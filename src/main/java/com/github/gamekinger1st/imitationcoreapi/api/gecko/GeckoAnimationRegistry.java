package com.github.gamekinger1st.imitationcoreapi.api.gecko;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class GeckoAnimationRegistry {
    private final Map<ResourceLocation, GeckoAnimationBridge> bridges = new LinkedHashMap<>();

    public synchronized GeckoAnimationRegistration register(GeckoAnimationBridge bridge) {
        Objects.requireNonNull(bridge, "bridge");
        ResourceLocation id = Objects.requireNonNull(bridge.id(), "bridge.id");
        if (bridges.putIfAbsent(id, bridge) != null) {
            throw new IllegalArgumentException("A Gecko animation bridge is already registered for " + id);
        }
        return new RegisteredBridge(this, id, bridge);
    }

    public Optional<GeckoAnimationSnapshot> capture(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        for (GeckoAnimationBridge bridge : orderedBridges()) {
            try {
                if (bridge.supports(entity)) {
                    Optional<GeckoAnimationSnapshot> snapshot = Objects.requireNonNull(bridge.capture(entity), "bridge capture result");
                    if (snapshot.isPresent()) {
                        return snapshot;
                    }
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    public boolean trigger(Entity entity, String controllerName, String animationName) {
        return invoke(entity, controllerName, animationName, true);
    }

    public boolean stop(Entity entity, String controllerName, String animationName) {
        return invoke(entity, controllerName, animationName, false);
    }

    public boolean mirror(Entity imitation, Entity subject) {
        Objects.requireNonNull(imitation, "imitation");
        Objects.requireNonNull(subject, "subject");
        for (GeckoAnimationBridge bridge : orderedBridges()) {
            try {
                if (bridge.supports(imitation) && bridge.supports(subject) && bridge.mirror(imitation, subject)) {
                    return true;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return false;
    }

    public Optional<Boolean> isPlaying(Entity entity, String controllerName, String animationName) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(controllerName, "controllerName");
        Objects.requireNonNull(animationName, "animationName");
        for (GeckoAnimationBridge bridge : orderedBridges()) {
            try {
                if (bridge.supports(entity)) {
                    Optional<Boolean> playing = Objects.requireNonNull(bridge.isPlaying(entity, controllerName, animationName), "bridge playing result");
                    if (playing.isPresent()) {
                        return playing;
                    }
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return Optional.empty();
    }

    private boolean invoke(Entity entity, String controllerName, String animationName, boolean trigger) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(controllerName, "controllerName");
        Objects.requireNonNull(animationName, "animationName");
        for (GeckoAnimationBridge bridge : orderedBridges()) {
            try {
                if (bridge.supports(entity) && (trigger ? bridge.trigger(entity, controllerName, animationName) : bridge.stop(entity, controllerName, animationName))) {
                    return true;
                }
            } catch (RuntimeException | LinkageError exception) {
            }
        }
        return false;
    }

    private synchronized java.util.List<GeckoAnimationBridge> orderedBridges() {
        return bridges.values().stream().sorted(Comparator.comparingInt(GeckoAnimationBridge::priority).reversed().thenComparing(bridge -> bridge.id().toString())).toList();
    }

    private synchronized boolean unregister(ResourceLocation id, GeckoAnimationBridge bridge) {
        return bridges.remove(id, bridge);
    }

    private record RegisteredBridge(GeckoAnimationRegistry registry, ResourceLocation id, GeckoAnimationBridge bridge) implements GeckoAnimationRegistration {
        @Override
        public boolean unregister() {
            return registry.unregister(id, bridge);
        }
    }
}
