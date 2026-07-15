package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;

import java.util.Objects;
import java.util.Optional;

public record ImitatorFormStatDelta(
        float health,
        float maxHealth,
        int armorValue,
        Optional<TensuraVitals> tensuraVitals
) {
    public static final ImitatorFormStatDelta EMPTY = new ImitatorFormStatDelta(0F, 0F, 0, Optional.empty());

    public ImitatorFormStatDelta {
        if (!Float.isFinite(health) || health < 0F) {
            throw new IllegalArgumentException("health delta must be finite and non-negative");
        }
        if (!Float.isFinite(maxHealth) || maxHealth < 0F) {
            throw new IllegalArgumentException("maxHealth delta must be finite and non-negative");
        }
        if (armorValue < 0) {
            throw new IllegalArgumentException("armor delta cannot be negative");
        }
        Objects.requireNonNull(tensuraVitals, "tensuraVitals");
        tensuraVitals = tensuraVitals.filter(vitals -> !vitals.isZero());
    }

    public static ImitatorFormStatDelta positiveBetween(DisguiseAppraisalSnapshot previous, DisguiseAppraisalSnapshot current) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        Optional<TensuraVitals> tensuraDelta = previous.tensuraVitals().flatMap(previousVitals ->
                current.tensuraVitals().map(currentVitals -> currentVitals.positiveDeltaSince(previousVitals))
        );
        return new ImitatorFormStatDelta(
                Math.max(0F, current.health() - previous.health()),
                Math.max(0F, current.maxHealth() - previous.maxHealth()),
                Math.max(0, current.armorValue() - previous.armorValue()),
                tensuraDelta
        );
    }

    public ImitatorFormStatDelta plus(ImitatorFormStatDelta other) {
        Objects.requireNonNull(other, "other");
        Optional<TensuraVitals> mergedVitals = merge(tensuraVitals, other.tensuraVitals);
        return new ImitatorFormStatDelta(health + other.health, maxHealth + other.maxHealth, armorValue + other.armorValue, mergedVitals);
    }

    public boolean isEmpty() {
        return health == 0F && maxHealth == 0F && armorValue == 0 && tensuraVitals.isEmpty();
    }

    private static Optional<TensuraVitals> merge(Optional<TensuraVitals> first, Optional<TensuraVitals> second) {
        if (first.isEmpty()) {
            return second;
        }
        if (second.isEmpty()) {
            return first;
        }
        return Optional.of(first.get().plus(second.get()));
    }
}
