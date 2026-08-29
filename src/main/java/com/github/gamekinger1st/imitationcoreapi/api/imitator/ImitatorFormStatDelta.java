package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;

import java.util.Objects;
import java.util.Optional;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public record ImitatorFormStatDelta(
        float health,
        float maxHealth,
        int armorValue,
        Optional<TensuraVitals> tensuraVitals,
        Map<ResourceLocation, Double> attributeBaseValues
) {
    public static final ImitatorFormStatDelta EMPTY = new ImitatorFormStatDelta(0F, 0F, 0, Optional.empty(), Map.of());

    public ImitatorFormStatDelta(float health, float maxHealth, int armorValue, Optional<TensuraVitals> tensuraVitals) {
        this(health, maxHealth, armorValue, tensuraVitals, Map.of());
    }

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
        Objects.requireNonNull(attributeBaseValues, "attributeBaseValues");
        java.util.LinkedHashMap<ResourceLocation, Double> sanitized = new java.util.LinkedHashMap<>();
        attributeBaseValues.forEach((id, value) -> {
            Objects.requireNonNull(id, "attribute id");
            if (value == null || !Double.isFinite(value) || value < 0D) {
                throw new IllegalArgumentException("attribute deltas must be finite and non-negative");
            }
            if (value > 0D) {
                sanitized.put(id, value);
            }
        });
        attributeBaseValues = Map.copyOf(sanitized);
    }

    public static ImitatorFormStatDelta positiveBetween(DisguiseAppraisalSnapshot previous, DisguiseAppraisalSnapshot current) {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(current, "current");
        Optional<TensuraVitals> tensuraDelta = previous.tensuraVitals().flatMap(previousVitals ->
                current.tensuraVitals().map(currentVitals -> {
                    TensuraVitals delta = currentVitals.positiveDeltaSince(previousVitals);
                    return new TensuraVitals(delta.ep(), delta.magicule(), delta.aura(), 0D);
                })
        );
        return new ImitatorFormStatDelta(
                0F,
                0F,
                0,
                tensuraDelta,
                Map.of()
        );
    }

    public static ImitatorFormStatDelta positiveBetween(
            DisguiseAppraisalSnapshot previous,
            DisguiseAppraisalSnapshot current,
            Map<ResourceLocation, Double> previousAttributes,
            Map<ResourceLocation, Double> currentAttributes
    ) {
        ImitatorFormStatDelta appraisal = positiveBetween(previous, current);
        java.util.LinkedHashMap<ResourceLocation, Double> attributes = new java.util.LinkedHashMap<>();
        currentAttributes.forEach((id, value) -> {
            double delta = value - previousAttributes.getOrDefault(id, value);
            if (Double.isFinite(delta) && delta > 0D) {
                attributes.put(id, delta);
            }
        });
        float maxHealth = attributes.getOrDefault(ResourceLocation.withDefaultNamespace("generic.max_health"), 0D).floatValue();
        int armor = (int)Math.floor(attributes.getOrDefault(ResourceLocation.withDefaultNamespace("generic.armor"), 0D));
        return new ImitatorFormStatDelta(0F, maxHealth, armor, appraisal.tensuraVitals(), attributes);
    }

    public ImitatorFormStatDelta plus(ImitatorFormStatDelta other) {
        Objects.requireNonNull(other, "other");
        Optional<TensuraVitals> mergedVitals = merge(tensuraVitals, other.tensuraVitals);
        java.util.LinkedHashMap<ResourceLocation, Double> attributes = new java.util.LinkedHashMap<>(attributeBaseValues);
        other.attributeBaseValues.forEach((id, value) -> attributes.merge(id, value, Double::sum));
        return new ImitatorFormStatDelta(health + other.health, maxHealth + other.maxHealth, armorValue + other.armorValue, mergedVitals, attributes);
    }

    public boolean isEmpty() {
        return health == 0F && maxHealth == 0F && armorValue == 0 && tensuraVitals.isEmpty() && attributeBaseValues.isEmpty();
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
