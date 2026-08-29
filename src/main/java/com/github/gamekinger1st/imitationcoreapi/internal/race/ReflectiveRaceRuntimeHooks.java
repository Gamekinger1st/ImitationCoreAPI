package com.github.gamekinger1st.imitationcoreapi.internal.race;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceFunctionContext;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceFunctionResult;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public static RaceFunctionResult function(Object race, ResourceLocation functionKey, LivingEntity entity, CompoundTag data) {
        return raceId(race)
                .map(raceId -> ImitationApi.raceEdits().handleFunction(new RaceFunctionContext(BRIDGE_ID, raceId, functionKey, Optional.ofNullable(entity), data)))
                .orElseGet(RaceFunctionResult::pass);
    }

    public static List<Object> races(List<ResourceLocation> ids) {
        return registryEntries("io.github.manasmods.manascore.race.api.RaceAPI", "getRaceRegistry", ids);
    }

    public static List<Object> skills(List<ResourceLocation> ids) {
        return registryEntries("io.github.manasmods.manascore.skill.api.SkillAPI", "getSkillRegistry", ids);
    }

    public static Optional<Object> enumValue(String className, ResourceLocation id) {
        try {
            Class<?> type = Class.forName(className, false, ReflectiveRaceRuntimeHooks.class.getClassLoader());
            if (!type.isEnum()) {
                return Optional.empty();
            }
            for (Object constant : type.getEnumConstants()) {
                if (((Enum<?>)constant).name().equalsIgnoreCase(id.getPath())) {
                    return Optional.of(constant);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
        }
        return Optional.empty();
    }

    public static void applyAttributeStats(Object race, Map<Object, Object> attributeModifiers) {
        if (race == null || attributeModifiers == null || attributeModifiers.isEmpty()) {
            return;
        }
        Map<Object, Object> replacements = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : attributeModifiers.entrySet()) {
            ResourceLocation statKey = attributeStatKey(entry.getKey()).orElse(null);
            OptionalDouble override = statKey == null ? OptionalDouble.empty() : stat(race, statKey);
            if (override.isEmpty()) {
                continue;
            }
            recreateAttributeTemplate(entry.getValue(), override.getAsDouble()).ifPresent(value -> replacements.put(entry.getKey(), value));
        }
        attributeModifiers.putAll(replacements);
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

    private static List<Object> registryEntries(String apiClassName, String registryMethod, List<ResourceLocation> ids) {
        try {
            Class<?> api = Class.forName(apiClassName, false, ReflectiveRaceRuntimeHooks.class.getClassLoader());
            Object registry = api.getMethod(registryMethod).invoke(null);
            Method get = registry.getClass().getMethod("get", ResourceLocation.class);
            List<Object> values = new ArrayList<>();
            for (ResourceLocation id : ids) {
                Object value = get.invoke(registry, id);
                if (value != null) {
                    values.add(value);
                }
            }
            return List.copyOf(values);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return List.of();
        }
    }

    private static Optional<ResourceLocation> attributeStatKey(Object holder) {
        if (!(holder instanceof net.minecraft.core.Holder<?> value)) {
            return Optional.empty();
        }
        Optional<ResourceLocation> id = value.unwrapKey().map(net.minecraft.resources.ResourceKey::location);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return switch (id.get().toString()) {
            case "minecraft:generic.scale", "minecraft:scale" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.SIZE);
            case "minecraft:generic.max_health", "minecraft:max_health" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.MAX_HEALTH);
            case "minecraft:generic.attack_damage", "minecraft:attack_damage" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.ATTACK);
            case "minecraft:generic.attack_speed", "minecraft:attack_speed" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.ATTACK_SPEED);
            case "minecraft:generic.knockback_resistance", "minecraft:knockback_resistance" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.KNOCKBACK_RESISTANCE);
            case "minecraft:generic.movement_speed", "minecraft:movement_speed" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.MOVEMENT_SPEED);
            case "manascore:swim_speed_multiplier" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.SWIM_SPEED);
            case "tensura:max_spiritual_health" -> Optional.of(com.github.gamekinger1st.imitationcoreapi.api.race.RaceStatKeys.MAX_SPIRITUAL_HEALTH);
            default -> Optional.empty();
        };
    }

    private static Optional<Object> recreateAttributeTemplate(Object template, double amount) {
        if (template == null) {
            return Optional.empty();
        }
        try {
            Object id = template.getClass().getMethod("id").invoke(template);
            Object operation = template.getClass().getMethod("operation").invoke(template);
            for (Constructor<?> constructor : template.getClass().getDeclaredConstructors()) {
                if (constructor.getParameterCount() == 3) {
                    constructor.setAccessible(true);
                    return Optional.of(constructor.newInstance(id, amount, operation));
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
        }
        return Optional.empty();
    }
}
