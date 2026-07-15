package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record RaceEditProfile(
        ResourceLocation raceId,
        Map<ResourceLocation, Double> stats,
        Map<ResourceLocation, Component> lines,
        Map<ResourceLocation, CompoundTag> data
) {
    public RaceEditProfile {
        Objects.requireNonNull(raceId, "raceId");
        stats = copyStats(stats);
        lines = copyLines(lines);
        data = copyData(data);
    }

    public Optional<Double> stat(ResourceLocation key) {
        return Optional.ofNullable(stats.get(Objects.requireNonNull(key, "key")));
    }

    public Optional<Component> line(ResourceLocation key) {
        return Optional.ofNullable(lines.get(Objects.requireNonNull(key, "key")));
    }

    public Optional<CompoundTag> data(ResourceLocation key) {
        CompoundTag tag = data.get(Objects.requireNonNull(key, "key"));
        return tag == null ? Optional.empty() : Optional.of(tag.copy());
    }

    public static RaceEditBuilder builder(ResourceLocation raceId) {
        return new RaceEditBuilder(raceId);
    }

    private static Map<ResourceLocation, Double> copyStats(Map<ResourceLocation, Double> values) {
        Objects.requireNonNull(values, "stats");
        LinkedHashMap<ResourceLocation, Double> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            Objects.requireNonNull(key, "stat key");
            Objects.requireNonNull(value, "stat value");
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Race stat values must be finite");
            }
            copy.put(key, value);
        });
        return Map.copyOf(copy);
    }

    private static Map<ResourceLocation, Component> copyLines(Map<ResourceLocation, Component> values) {
        Objects.requireNonNull(values, "lines");
        LinkedHashMap<ResourceLocation, Component> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "line key"), Objects.requireNonNull(value, "line value")));
        return Map.copyOf(copy);
    }

    private static Map<ResourceLocation, CompoundTag> copyData(Map<ResourceLocation, CompoundTag> values) {
        Objects.requireNonNull(values, "data");
        LinkedHashMap<ResourceLocation, CompoundTag> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key, "data key"), Objects.requireNonNull(value, "data value").copy()));
        return Map.copyOf(copy);
    }
}
