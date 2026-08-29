package com.github.gamekinger1st.imitationcoreapi.api.skill;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class SkillBridgeRegistry {
    private final Map<ResourceLocation, SkillBridge> bridges = new LinkedHashMap<>();

    public synchronized SkillBridgeRegistration register(SkillBridge bridge) {
        Objects.requireNonNull(bridge, "bridge");
        ResourceLocation id = Objects.requireNonNull(bridge.id(), "bridge.id");
        if (bridges.putIfAbsent(id, bridge) != null) {
            throw new IllegalArgumentException("A skill bridge is already registered for " + id);
        }
        return new RegisteredBridge(this, id, bridge);
    }

    public Optional<SkillSnapshot> capture(LivingEntity entity) {
        Objects.requireNonNull(entity, "entity");
        for (SkillBridge bridge : orderedBridges()) {
            try {
                if (!bridge.isAvailable()) {
                    continue;
                }
                Optional<SkillSnapshot> snapshot = Objects.requireNonNull(bridge.capture(entity), "bridge capture result");
                if (snapshot.isPresent()) {
                    return snapshot;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    public Optional<ResourceLocation> activeBridgeId() {
        return activeBridge().map(SkillBridge::id);
    }

    public SkillClassification classify(ResourceLocation bridgeId, ResourceLocation skillId) {
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(skillId, "skillId");
        SkillClassification classification = bridge(bridgeId)
                .map(bridge -> {
                    try {
                        return Objects.requireNonNullElse(bridge.classify(skillId), SkillClassification.UNKNOWN);
                    } catch (RuntimeException | LinkageError exception) {
                        return SkillClassification.UNKNOWN;
                    }
                })
                .orElse(SkillClassification.UNKNOWN);
        return ImitationApi.skillClassifications().resolve(bridgeId, skillId, classification);
    }

    public SkillOperationResult alterClassification(ResourceLocation skillId, SkillClassification classification) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(classification, "classification");
        return activeBridge().map(bridge -> invoke(() -> bridge.alterClassification(skillId, classification))).orElseGet(() -> SkillOperationResult.failure("No compatible skill bridge is loaded"));
    }

    public SkillOperationResult alterClassification(ResourceLocation bridgeId, ResourceLocation skillId, SkillClassification classification) {
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(classification, "classification");
        return bridge(bridgeId).map(bridge -> invoke(() -> bridge.alterClassification(skillId, classification))).orElseGet(() -> SkillOperationResult.failure("The requested skill bridge is not available"));
    }

    public SkillOperationResult update(LivingEntity entity, SkillUpdateRequest request) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(request, "request");
        return activeBridge().map(bridge -> invoke(() -> bridge.update(entity, request))).orElseGet(() -> SkillOperationResult.failure("No compatible skill bridge is loaded"));
    }

    public SkillOperationResult update(LivingEntity entity, ResourceLocation bridgeId, SkillUpdateRequest request) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(request, "request");
        return bridge(bridgeId).map(bridge -> invoke(() -> bridge.update(entity, request))).orElseGet(() -> SkillOperationResult.failure("The requested skill bridge is not available"));
    }

    public SkillOperationResult restore(LivingEntity entity, SkillSnapshot snapshot) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(snapshot, "snapshot");
        for (SkillBridge bridge : orderedBridges()) {
            try {
                if (bridge.id().equals(snapshot.bridgeId()) && bridge.isAvailable()) {
                    return invoke(() -> bridge.restore(entity, snapshot));
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return SkillOperationResult.failure("No compatible skill bridge is loaded for this snapshot");
    }

    public SkillOperationResult restoreSkill(LivingEntity entity, ResourceLocation bridgeId, SkillState state) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(state, "state");
        return bridge(bridgeId)
                .map(bridge -> invoke(() -> bridge.restoreSkill(entity, state)))
                .orElseGet(() -> SkillOperationResult.failure("The requested skill bridge is not available"));
    }

    public SkillOperationResult removeSkill(LivingEntity entity, ResourceLocation bridgeId, ResourceLocation skillId) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(bridgeId, "bridgeId");
        Objects.requireNonNull(skillId, "skillId");
        return bridge(bridgeId)
                .map(bridge -> invoke(() -> bridge.removeSkill(entity, skillId)))
                .orElseGet(() -> SkillOperationResult.failure("The requested skill bridge is not available"));
    }

    public SkillOperationResult grantTemporary(LivingEntity entity, ResourceLocation skillId, int removeTime, TemporarySkillOwnership ownership) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(ownership, "ownership");
        if (removeTime < 0) {
            throw new IllegalArgumentException("removeTime cannot be negative");
        }
        return activeBridge().map(bridge -> invoke(() -> bridge.grantTemporary(entity, skillId, removeTime, ownership))).orElseGet(() -> SkillOperationResult.failure("No compatible skill bridge is loaded"));
    }

    public SkillOperationResult grantTemporary(LivingEntity entity, ResourceLocation bridgeId, ResourceLocation skillId, int removeTime, TemporarySkillOwnership ownership) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(ownership, "ownership");
        if (removeTime < 0) {
            throw new IllegalArgumentException("removeTime cannot be negative");
        }
        return bridge(bridgeId).map(bridge -> invoke(() -> bridge.grantTemporary(entity, skillId, removeTime, ownership))).orElseGet(() -> SkillOperationResult.failure("The requested skill bridge is not available"));
    }

    public SkillOperationResult revokeTemporary(LivingEntity entity, ResourceLocation skillId, TemporarySkillOwnership ownership) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(ownership, "ownership");
        return activeBridge().map(bridge -> invoke(() -> bridge.revokeTemporary(entity, skillId, ownership))).orElseGet(() -> SkillOperationResult.failure("No compatible skill bridge is loaded"));
    }

    public SkillOperationResult revokeTemporary(LivingEntity entity, ResourceLocation bridgeId, ResourceLocation skillId, TemporarySkillOwnership ownership) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(ownership, "ownership");
        return bridge(bridgeId).map(bridge -> invoke(() -> bridge.revokeTemporary(entity, skillId, ownership))).orElseGet(() -> SkillOperationResult.failure("The requested skill bridge is not available"));
    }

    private Optional<SkillBridge> activeBridge() {
        for (SkillBridge bridge : orderedBridges()) {
            try {
                if (bridge.isAvailable()) {
                    return Optional.of(bridge);
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    private Optional<SkillBridge> bridge(ResourceLocation bridgeId) {
        Objects.requireNonNull(bridgeId, "bridgeId");
        for (SkillBridge bridge : orderedBridges()) {
            try {
                if (bridge.id().equals(bridgeId) && bridge.isAvailable()) {
                    return Optional.of(bridge);
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    private SkillOperationResult invoke(Supplier<SkillOperationResult> operation) {
        try {
            return Objects.requireNonNull(operation.get(), "bridge operation result");
        } catch (RuntimeException | LinkageError exception) {
            return SkillOperationResult.failure(exception.getClass().getSimpleName());
        }
    }

    private synchronized java.util.List<SkillBridge> orderedBridges() {
        return bridges.entrySet().stream()
                .sorted(Comparator.<Map.Entry<ResourceLocation, SkillBridge>>comparingInt(entry -> priority(entry.getValue()))
                        .reversed()
                        .thenComparing(entry -> entry.getKey().toString()))
                .map(Map.Entry::getValue)
                .toList();
    }

    private static int priority(SkillBridge bridge) {
        try {
            return bridge.priority();
        } catch (RuntimeException | LinkageError exception) {
            return Integer.MIN_VALUE;
        }
    }

    private synchronized boolean unregister(ResourceLocation id, SkillBridge bridge) {
        return bridges.remove(id, bridge);
    }

    private record RegisteredBridge(SkillBridgeRegistry registry, ResourceLocation id, SkillBridge bridge) implements SkillBridgeRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, bridge);
        }
    }
}
