package com.github.gamekinger1st.imitationcoreapi.api.imitator;

public record ImitatorRecordingContext(double distanceRatio, double targetMotionRatio, double powerRatio, double analysisBonus, boolean mastered) {
    public static final ImitatorRecordingContext DEFAULT = new ImitatorRecordingContext(0D, 0D, 1D, 0D, false);

    public ImitatorRecordingContext {
        validateRatio(distanceRatio, "distanceRatio");
        validateRatio(targetMotionRatio, "targetMotionRatio");
        validateRatio(powerRatio, "powerRatio");
        validateRatio(analysisBonus, "analysisBonus");
    }

    private static void validateRatio(double value, String name) {
        if (!Double.isFinite(value) || value < 0D || value > 1D) {
            throw new IllegalArgumentException(name + " must be between zero and one");
        }
    }
}
