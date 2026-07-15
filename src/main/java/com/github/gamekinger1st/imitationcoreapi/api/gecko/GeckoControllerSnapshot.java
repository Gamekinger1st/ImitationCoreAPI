package com.github.gamekinger1st.imitationcoreapi.api.gecko;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record GeckoControllerSnapshot(
        String controllerName,
        String animationState,
        List<String> animationNames,
        List<String> triggerableAnimationNames,
        double animationSpeed,
        double transitionLength,
        String triggeredAnimationName,
        boolean playingTriggeredAnimation
) {
    public GeckoControllerSnapshot(String controllerName) {
        this(controllerName, "UNKNOWN", List.of(), List.of(), 1D, 0D, "", false);
    }

    public GeckoControllerSnapshot {
        Objects.requireNonNull(controllerName, "controllerName");
        Objects.requireNonNull(animationState, "animationState");
        Objects.requireNonNull(animationNames, "animationNames");
        Objects.requireNonNull(triggerableAnimationNames, "triggerableAnimationNames");
        Objects.requireNonNull(triggeredAnimationName, "triggeredAnimationName");
        controllerName = controllerName.strip();
        animationState = animationState.strip();
        triggeredAnimationName = triggeredAnimationName.strip();
        if (controllerName.isEmpty()) {
            throw new IllegalArgumentException("controllerName cannot be blank");
        }
        if (animationState.isEmpty()) {
            animationState = "UNKNOWN";
        }
        animationNames = normalize(animationNames);
        triggerableAnimationNames = normalize(triggerableAnimationNames);
        if (!Double.isFinite(animationSpeed)) {
            animationSpeed = 1D;
        }
        if (!Double.isFinite(transitionLength) || transitionLength < 0D) {
            transitionLength = 0D;
        }
    }

    public Optional<String> primaryAnimationName() {
        return animationNames.stream().findFirst();
    }

    public Optional<String> activeTriggeredAnimationName() {
        return triggeredAnimationName.isEmpty() ? Optional.empty() : Optional.of(triggeredAnimationName);
    }

    private static List<String> normalize(List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
