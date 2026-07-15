package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;

import java.util.Objects;
import java.util.Optional;

public record DisguiseAppraisalSnapshot(float health, float maxHealth, int armorValue, Optional<TensuraVitals> tensuraVitals) {
    public DisguiseAppraisalSnapshot {
        if (!Float.isFinite(health) || health < 0F) {
            throw new IllegalArgumentException("health must be finite and non-negative");
        }
        if (!Float.isFinite(maxHealth) || maxHealth < 0F) {
            throw new IllegalArgumentException("maxHealth must be finite and non-negative");
        }
        if (armorValue < 0) {
            throw new IllegalArgumentException("armorValue must be non-negative");
        }
        Objects.requireNonNull(tensuraVitals, "tensuraVitals");
    }
}
