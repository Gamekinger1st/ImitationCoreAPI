package com.github.gamekinger1st.imitationcoreapi.api.session;

public enum TransformationScope {
    SURFACE,
    GAMEPLAY,
    REPLICA;

    public boolean appliesGameplayState() {
        return this == GAMEPLAY;
    }

    public boolean changesOwnerPresentation() {
        return this == SURFACE || this == GAMEPLAY;
    }
}
