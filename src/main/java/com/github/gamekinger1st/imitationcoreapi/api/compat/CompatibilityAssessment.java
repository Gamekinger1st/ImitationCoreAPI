package com.github.gamekinger1st.imitationcoreapi.api.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CompatibilityAssessment(CompatibilityLevel level, List<String> reasons) {
    public CompatibilityAssessment {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(reasons, "reasons");
        reasons = reasons.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(reason -> !reason.isEmpty())
                .map(reason -> reason.length() > 512 ? reason.substring(0, 512) : reason)
                .toList();
    }

    public static CompatibilityAssessment full() {
        return new CompatibilityAssessment(CompatibilityLevel.FULL, List.of());
    }

    public static CompatibilityAssessment visual(String reason) {
        return new CompatibilityAssessment(CompatibilityLevel.VISUAL, List.of(reason));
    }

    public static CompatibilityAssessment fallback(String reason) {
        return new CompatibilityAssessment(CompatibilityLevel.FALLBACK, List.of(reason));
    }

    public static CompatibilityAssessment unsupported(String reason) {
        return new CompatibilityAssessment(CompatibilityLevel.UNSUPPORTED, List.of(reason));
    }

    public CompatibilityAssessment combine(CompatibilityAssessment other) {
        Objects.requireNonNull(other, "other");
        List<String> combinedReasons = new ArrayList<>(reasons);
        combinedReasons.addAll(other.reasons);
        return new CompatibilityAssessment(level.combine(other.level), combinedReasons);
    }
}
