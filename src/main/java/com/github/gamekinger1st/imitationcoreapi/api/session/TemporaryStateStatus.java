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
}
