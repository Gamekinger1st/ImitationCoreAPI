package com.github.gamekinger1st.imitationcoreapi.api.gecko;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record GeckoAnimationSnapshot(UUID entityId, ResourceLocation entityType, long entityTick, List<String> controllerNames, List<GeckoControllerSnapshot> controllers) {
    public GeckoAnimationSnapshot(UUID entityId, ResourceLocation entityType, long entityTick, List<String> controllerNames) {
        this(entityId, entityType, entityTick, controllerNames, List.of());
    }

    public GeckoAnimationSnapshot {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(controllerNames, "controllerNames");
        Objects.requireNonNull(controllers, "controllers");
        if (entityTick < 0) {
            throw new IllegalArgumentException("entityTick cannot be negative");
        }
        controllers = normalizeControllers(controllerNames, controllers);
        controllerNames = controllers.stream().map(GeckoControllerSnapshot::controllerName).distinct().sorted().toList();
    }

    private static List<GeckoControllerSnapshot> normalizeControllers(List<String> controllerNames, List<GeckoControllerSnapshot> controllers) {
        Map<String, GeckoControllerSnapshot> normalized = new LinkedHashMap<>();
        for (String controllerName : controllerNames) {
            if (controllerName == null) {
                continue;
            }
            String normalizedName = controllerName.strip();
            if (!normalizedName.isEmpty()) {
                normalized.putIfAbsent(normalizedName, new GeckoControllerSnapshot(normalizedName));
            }
        }
        for (GeckoControllerSnapshot controller : controllers) {
            if (controller != null) {
                normalized.put(controller.controllerName(), controller);
            }
        }
        return new ArrayList<>(normalized.values()).stream()
                .sorted(java.util.Comparator.comparing(GeckoControllerSnapshot::controllerName))
                .toList();
    }
}
