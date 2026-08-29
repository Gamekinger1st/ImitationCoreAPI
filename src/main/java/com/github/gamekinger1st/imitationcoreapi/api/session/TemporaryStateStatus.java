package com.github.gamekinger1st.imitationcoreapi.api.session;

public enum TemporaryStateStatus {
    PREPARED,
    ACTIVE,
    CLEANUP_REQUESTED,
    CLEANED,
    QUARANTINED;

    public boolean requiresReconciliation() {
        return this != CLEANED;
    }

    public boolean canTransitionTo(TemporaryStateStatus target) {
        return this == target || switch (this) {
            case PREPARED -> target == ACTIVE || target == CLEANUP_REQUESTED || target == CLEANED || target == QUARANTINED;
            case ACTIVE -> target == CLEANUP_REQUESTED || target == CLEANED || target == QUARANTINED;
            case CLEANUP_REQUESTED -> target == CLEANED || target == QUARANTINED;
            case QUARANTINED -> target == CLEANED;
            case CLEANED -> false;
        };
    }
}
