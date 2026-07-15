package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class RaceStatKeys {
    public static final ResourceLocation MIN_AURA = key("min_aura");
    public static final ResourceLocation MAX_AURA = key("max_aura");
    public static final ResourceLocation MIN_MAGICULE = key("min_magicule");
    public static final ResourceLocation MAX_MAGICULE = key("max_magicule");
    public static final ResourceLocation BASE_AURA_RANGE = key("base_aura_range");
    public static final ResourceLocation BASE_MAGICULE_RANGE = key("base_magicule_range");
    public static final ResourceLocation SIZE = key("size");
    public static final ResourceLocation MAX_HEALTH = key("max_health");
    public static final ResourceLocation MAX_SPIRITUAL_HEALTH = key("max_spiritual_health");
    public static final ResourceLocation ATTACK = key("attack");
    public static final ResourceLocation ATTACK_SPEED = key("attack_speed");
    public static final ResourceLocation KNOCKBACK_RESISTANCE = key("knockback_resistance");
    public static final ResourceLocation MOVEMENT_SPEED = key("movement_speed");
    public static final ResourceLocation SWIM_SPEED = key("swim_speed");
    public static final ResourceLocation ELEMENTAL_SPIRIT_CHANCE = key("elemental_spirit_chance");

    private RaceStatKeys() {
    }

    public static List<ResourceLocation> builtin() {
        return List.of(
                MIN_AURA,
                MAX_AURA,
                MIN_MAGICULE,
                MAX_MAGICULE,
                BASE_AURA_RANGE,
                BASE_MAGICULE_RANGE,
                SIZE,
                MAX_HEALTH,
                MAX_SPIRITUAL_HEALTH,
                ATTACK,
                ATTACK_SPEED,
                KNOCKBACK_RESISTANCE,
                MOVEMENT_SPEED,
                SWIM_SPEED,
                ELEMENTAL_SPIRIT_CHANCE
        );
    }

    private static ResourceLocation key(String path) {
        return ResourceLocation.fromNamespaceAndPath("imitationcoreapi", path);
    }
}
