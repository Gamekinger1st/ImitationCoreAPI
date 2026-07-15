package com.github.gamekinger1st.imitationcoreapi.internal.race;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalDouble;

public final class ReflectiveRaceRuntimeHooks {
    public static final ResourceLocation BRIDGE_ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "manas_tensura_race_reflective");

    private ReflectiveRaceRuntimeHooks() {
    }

    public static Optional<Component> line(Object race, ResourceLocation lineKey) {
        return raceId(race).flatMap(raceId -> ImitationApi.raceEdits().line(BRIDGE_ID, raceId, lineKey));
    }

    public static OptionalDouble stat(Object race, ResourceLocation statKey) {
        Optional<Double> value = raceId(race).flatMap(raceId -> ImitationApi.raceEdits().stat(BRIDGE_ID, raceId, statKey));
        return value.map(OptionalDouble::of).orElseGet(OptionalDouble::empty);
    }

    public static Optional<ResourceLocation> raceId(Object race) {
        if (race == null) {
            return Optional.empty();
        }
        try {
            Method method = race.getClass().getMethod("getRegistryName");
            Object id = method.invoke(race);
            return id instanceof ResourceLocation resourceLocation ? Optional.of(resourceLocation) : Optional.empty();
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            return Optional.empty();
        }
    }
}
