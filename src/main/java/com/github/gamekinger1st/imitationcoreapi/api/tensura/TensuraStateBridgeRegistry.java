package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class TensuraStateBridgeRegistry {
    private final Map<ResourceLocation, TensuraStateBridge> bridges = new LinkedHashMap<>();

    public synchronized TensuraStateBridgeRegistration register(TensuraStateBridge bridge) {
        Objects.requireNonNull(bridge, "bridge");
        ResourceLocation id = Objects.requireNonNull(bridge.id(), "bridge.id");
        if (bridges.putIfAbsent(id, bridge) != null) {
            throw new IllegalArgumentException("A Tensura state bridge is already registered for " + id);
        }
        return new RegisteredBridge(this, id, bridge);
    }

    public Optional<TensuraStateSnapshot> capture(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        for (TensuraStateBridge bridge : orderedBridges()) {
            try {
                if (!bridge.isAvailable()) {
                    continue;
                }
                Optional<TensuraStateSnapshot> snapshot = Objects.requireNonNull(bridge.capture(entity), "bridge capture result");
                if (snapshot.isPresent()) {
                    return snapshot;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    public TensuraStateOperationResult restore(LivingEntity entity, TensuraStateSnapshot snapshot) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(snapshot, "snapshot");
        return bridge(snapshot.bridgeId())
                .map(bridge -> invoke(() -> bridge.restore(entity, snapshot)))
                .orElseGet(() -> TensuraStateOperationResult.failure("No compatible Tensura state bridge is loaded for this snapshot"));
    }

    public TensuraStateOperationResult restoreScaled(LivingEntity entity, TensuraStateSnapshot snapshot, double scale) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(snapshot, "snapshot");
        if (!Double.isFinite(scale) || scale < 0D || scale > 1D) {
            throw new IllegalArgumentException("scale must be between zero and one");
        }
        return bridge(snapshot.bridgeId())
                .map(bridge -> invoke(() -> bridge.restoreScaled(entity, snapshot, scale)))
                .orElseGet(() -> TensuraStateOperationResult.failure("No compatible Tensura state bridge is loaded for this snapshot"));
    }

    public TensuraStateOperationResult chargeMagicule(LivingEntity entity, double amount) {
        Objects.requireNonNull(entity, "entity");
        if (!Double.isFinite(amount) || amount < 0D) {
            throw new IllegalArgumentException("amount must be finite and non-negative");
        }
        return activeBridge().map(bridge -> invoke(() -> bridge.chargeMagicule(entity, amount))).orElseGet(() -> TensuraStateOperationResult.failure("No compatible Tensura state bridge is loaded"));
    }

    public TensuraStateOperationResult addVitalsDelta(LivingEntity entity, TensuraVitals delta) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(delta, "delta");
        if (delta.isZero()) {
            return TensuraStateOperationResult.success();
        }
        return activeBridge().map(bridge -> invoke(() -> bridge.addVitalsDelta(entity, delta))).orElseGet(() -> TensuraStateOperationResult.failure("No compatible Tensura state bridge is loaded"));
    }

    private Optional<TensuraStateBridge> activeBridge() {
        for (TensuraStateBridge bridge : orderedBridges()) {
            try {
                if (bridge.isAvailable()) {
                    return Optional.of(bridge);
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    private Optional<TensuraStateBridge> bridge(ResourceLocation bridgeId) {
        for (TensuraStateBridge bridge : orderedBridges()) {
            try {
                if (bridge.id().equals(bridgeId) && bridge.isAvailable()) {
                    return Optional.of(bridge);
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    private TensuraStateOperationResult invoke(java.util.function.Supplier<TensuraStateOperationResult> operation) {
        try {
            return Objects.requireNonNull(operation.get(), "bridge operation result");
        } catch (RuntimeException | LinkageError exception) {
            return TensuraStateOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    private synchronized java.util.List<TensuraStateBridge> orderedBridges() {
        return bridges.values().stream().sorted(Comparator.comparingInt(TensuraStateBridge::priority).reversed().thenComparing(bridge -> bridge.id().toString())).toList();
    }

    private synchronized boolean unregister(ResourceLocation id, TensuraStateBridge bridge) {
        return bridges.remove(id, bridge);
    }

    private record RegisteredBridge(TensuraStateBridgeRegistry registry, ResourceLocation id, TensuraStateBridge bridge) implements TensuraStateBridgeRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, bridge);
        }
    }
}
