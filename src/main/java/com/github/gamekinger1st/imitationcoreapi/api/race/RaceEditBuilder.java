package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RaceEditBuilder {
    private final ResourceLocation raceId;
    private final Map<ResourceLocation, Double> stats = new LinkedHashMap<>();
    private final Map<ResourceLocation, Component> lines = new LinkedHashMap<>();
    private final Map<ResourceLocation, CompoundTag> data = new LinkedHashMap<>();

    RaceEditBuilder(ResourceLocation raceId) {
        this.raceId = Objects.requireNonNull(raceId, "raceId");
    }

    public RaceEditBuilder stat(ResourceLocation key, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Race stat values must be finite");
        }
        stats.put(Objects.requireNonNull(key, "key"), value);
        return this;
    }

    public RaceEditBuilder line(ResourceLocation key, Component value) {
        lines.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        return this;
    }

    public RaceEditBuilder data(ResourceLocation key, CompoundTag value) {
        data.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value").copy());
        return this;
    }

    public RaceEditBuilder merge(RaceEditProfile profile) {
        Objects.requireNonNull(profile, "profile");
        if (!raceId.equals(profile.raceId())) {
            throw new IllegalArgumentException("Cannot merge a profile for another race");
        }
        stats.putAll(profile.stats());
        lines.putAll(profile.lines());
        profile.data().forEach((key, value) -> data.put(key, value.copy()));
        return this;
    }

    public RaceEditProfile build() {
        return new RaceEditProfile(raceId, stats, lines, data);
    }
}
