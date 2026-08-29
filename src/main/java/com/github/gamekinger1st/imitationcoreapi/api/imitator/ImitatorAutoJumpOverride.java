package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import java.util.Optional;

public enum ImitatorAutoJumpOverride {
    INHERIT,
    FORCE_ENABLED,
    FORCE_DISABLED;

    public Optional<Boolean> forcedValue() {
        return switch (this) {
            case INHERIT -> Optional.empty();
            case FORCE_ENABLED -> Optional.of(true);
            case FORCE_DISABLED -> Optional.of(false);
        };
    }

    public boolean resolve(boolean configuredValue) {
        return forcedValue().orElse(configuredValue);
    }
}
