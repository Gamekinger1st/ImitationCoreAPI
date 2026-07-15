package com.github.gamekinger1st.imitationcoreapi.api.imitator;

public record ImitatorActionPolicy(double maxRecordDistance, boolean allowPlayerTargets, boolean requireLineOfSight) {
    public static final ImitatorActionPolicy DEFAULT = new ImitatorActionPolicy(16D, true, true);

    public ImitatorActionPolicy(double maxRecordDistance, boolean allowPlayerTargets) {
        this(maxRecordDistance, allowPlayerTargets, true);
    }

    public ImitatorActionPolicy {
        if (!Double.isFinite(maxRecordDistance) || maxRecordDistance <= 0D || maxRecordDistance > 256D) {
            throw new IllegalArgumentException("maxRecordDistance must be between zero and 256");
        }
    }

    public double maxRecordDistanceSqr() {
        return maxRecordDistance * maxRecordDistance;
    }
}
