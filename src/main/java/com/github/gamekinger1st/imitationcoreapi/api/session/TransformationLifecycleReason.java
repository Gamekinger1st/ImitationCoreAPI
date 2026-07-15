package com.github.gamekinger1st.imitationcoreapi.api.session;

public enum TransformationLifecycleReason {
    LOGOUT,
    CLONE,
    DEATH,
    DIMENSION_CHANGE,
    RECONNECT,
    SERVER_STARTUP_RECOVERY,
    SERVER_STOPPING,
    WORLD_UNLOAD,
    SKILL_REMOVED,
    REPLICA_EXPIRED,
    DURATION_EXPIRED,
    REPLICA_REMOVED,
    FORM_DEATH_AVOIDED,
    FORCE_REVERT,
    APPLY_FAILURE,
    REVERT_FAILURE
}
