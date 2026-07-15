package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import java.util.Objects;
import java.util.OptionalDouble;

public record DisguisePresentation(RenderMode renderMode, boolean replaceNameTag, OptionalDouble eyeHeight) {
    public DisguisePresentation {
        Objects.requireNonNull(renderMode, "renderMode");
        Objects.requireNonNull(eyeHeight, "eyeHeight");
        if (eyeHeight.isPresent() && (!Double.isFinite(eyeHeight.getAsDouble()) || eyeHeight.getAsDouble() < 0D || eyeHeight.getAsDouble() > 64D)) {
            throw new IllegalArgumentException("eyeHeight must be between zero and 64");
        }
    }

    public static DisguisePresentation fallback() {
        return new DisguisePresentation(RenderMode.FALLBACK, true, OptionalDouble.empty());
    }

    public enum RenderMode {
        FAKE_ENTITY,
        FAKE_PLAYER,
        GECKO,
        FALLBACK
    }
}
