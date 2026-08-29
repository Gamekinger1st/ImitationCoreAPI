package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public record RaceFunctionResult(
        boolean handled,
        Optional<Boolean> booleanValue,
        OptionalInt intValue,
        OptionalDouble doubleValue,
        Optional<ResourceLocation> resourceValue,
        List<ResourceLocation> resourceList,
        Optional<Component> lineValue,
        Optional<Object> rawValue,
        CompoundTag data
) {
    public RaceFunctionResult {
        Objects.requireNonNull(booleanValue, "booleanValue");
        Objects.requireNonNull(intValue, "intValue");
        Objects.requireNonNull(doubleValue, "doubleValue");
        Objects.requireNonNull(resourceValue, "resourceValue");
        Objects.requireNonNull(resourceList, "resourceList");
        Objects.requireNonNull(lineValue, "lineValue");
        Objects.requireNonNull(rawValue, "rawValue");
        Objects.requireNonNull(data, "data");
        resourceList = List.copyOf(resourceList);
        data = data.copy();
    }

    public static RaceFunctionResult pass() {
        return new RaceFunctionResult(false, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult handledResult() {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult booleanValue(boolean value) {
        return new RaceFunctionResult(true, Optional.of(value), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult intValue(int value) {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.of(value), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult doubleValue(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Race function double values must be finite");
        }
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.of(value), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult resourceValue(ResourceLocation value) {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.of(Objects.requireNonNull(value, "value")), List.of(), Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult resourceList(List<ResourceLocation> value) {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), value, Optional.empty(), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult lineValue(Component value) {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.of(Objects.requireNonNull(value, "value")), Optional.empty(), new CompoundTag());
    }

    public static RaceFunctionResult data(CompoundTag value) {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.empty(), value);
    }

    public static RaceFunctionResult rawValue(Object value) {
        return new RaceFunctionResult(true, Optional.empty(), OptionalInt.empty(), OptionalDouble.empty(), Optional.empty(), List.of(), Optional.empty(), Optional.of(Objects.requireNonNull(value, "value")), new CompoundTag());
    }
}
