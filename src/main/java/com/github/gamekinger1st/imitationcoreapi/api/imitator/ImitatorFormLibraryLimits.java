package com.github.gamekinger1st.imitationcoreapi.api.imitator;

public record ImitatorFormLibraryLimits(int slotCapacity, int maxSeenForms, long maxPendingDurationTicks) {
    public static final ImitatorFormLibraryLimits DEFAULT = new ImitatorFormLibraryLimits(32, 256, 12_000L);

    public ImitatorFormLibraryLimits {
        if (slotCapacity < 1 || slotCapacity > 256) {
            throw new IllegalArgumentException("slotCapacity must be between 1 and 256");
        }
        if (maxSeenForms < 1 || maxSeenForms > 4_096) {
            throw new IllegalArgumentException("maxSeenForms must be between 1 and 4096");
        }
        if (maxPendingDurationTicks < 1 || maxPendingDurationTicks > 1_200_000L) {
            throw new IllegalArgumentException("maxPendingDurationTicks must be between 1 and 1200000");
        }
    }
}
