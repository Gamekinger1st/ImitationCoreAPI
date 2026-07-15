package com.github.gamekinger1st.imitationcoreapi.api.race;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class RaceFunctionKeys {
    public static final ResourceLocation CAN_ACTIVATE_ABILITY = key("can_activate_ability");
    public static final ResourceLocation MAX_HELD_TIME = key("max_held_time");
    public static final ResourceLocation CAN_TICK = key("can_tick");
    public static final ResourceLocation ON_TICK = key("on_tick");
    public static final ResourceLocation ON_ACTIVATE_ABILITY = key("on_activate_ability");
    public static final ResourceLocation ON_HELD_ABILITY = key("on_held_ability");
    public static final ResourceLocation ON_RELEASE_ABILITY = key("on_release_ability");
    public static final ResourceLocation ON_RACE_SET = key("on_race_set");
    public static final ResourceLocation ON_EFFECT_ADDED = key("on_effect_added");
    public static final ResourceLocation ON_BEING_TARGETED = key("on_being_targeted");
    public static final ResourceLocation ON_ATTACK_ENTITY = key("on_attack_entity");
    public static final ResourceLocation ON_HURT = key("on_hurt");
    public static final ResourceLocation ON_DEATH = key("on_death");
    public static final ResourceLocation ON_RESPAWN = key("on_respawn");
    public static final ResourceLocation RESPAWN_DIMENSION = key("respawn_dimension");
    public static final ResourceLocation INTRINSIC_SKILLS = key("intrinsic_skills");
    public static final ResourceLocation LEARN_INTRINSIC_SKILLS = key("learn_intrinsic_skills");
    public static final ResourceLocation NEXT_EVOLUTIONS = key("next_evolutions");
    public static final ResourceLocation PREVIOUS_EVOLUTIONS = key("previous_evolutions");
    public static final ResourceLocation DEFAULT_EVOLUTION = key("default_evolution");
    public static final ResourceLocation EVOLUTION_PROGRESS = key("evolution_progress");
    public static final ResourceLocation ON_RACE_EVOLUTION = key("on_race_evolution");
    public static final ResourceLocation RESET_EXISTENCE_DATA = key("reset_existence_data");
    public static final ResourceLocation AWAKENING_EVOLUTION = key("awakening_evolution");
    public static final ResourceLocation HARVEST_FESTIVAL_EVOLUTION = key("harvest_festival_evolution");
    public static final ResourceLocation EVOLUTION_REQUIREMENTS = key("evolution_requirements");
    public static final ResourceLocation TRIGGER_EVOLUTION_REWARDS = key("trigger_evolution_rewards");
    public static final ResourceLocation ALIGNMENT = key("alignment");
    public static final ResourceLocation INTRINSIC_LEARNABLE = key("intrinsic_learnable");
    public static final ResourceLocation ELEMENTAL_SPIRIT_CHANCE = key("elemental_spirit_chance");

    private RaceFunctionKeys() {
    }

    public static List<ResourceLocation> builtin() {
        return List.of(
                CAN_ACTIVATE_ABILITY,
                MAX_HELD_TIME,
                CAN_TICK,
                ON_TICK,
                ON_ACTIVATE_ABILITY,
                ON_HELD_ABILITY,
                ON_RELEASE_ABILITY,
                ON_RACE_SET,
                ON_EFFECT_ADDED,
                ON_BEING_TARGETED,
                ON_ATTACK_ENTITY,
                ON_HURT,
                ON_DEATH,
                ON_RESPAWN,
                RESPAWN_DIMENSION,
                INTRINSIC_SKILLS,
                LEARN_INTRINSIC_SKILLS,
                NEXT_EVOLUTIONS,
                PREVIOUS_EVOLUTIONS,
                DEFAULT_EVOLUTION,
                EVOLUTION_PROGRESS,
                ON_RACE_EVOLUTION,
                RESET_EXISTENCE_DATA,
                AWAKENING_EVOLUTION,
                HARVEST_FESTIVAL_EVOLUTION,
                EVOLUTION_REQUIREMENTS,
                TRIGGER_EVOLUTION_REWARDS,
                ALIGNMENT,
                INTRINSIC_LEARNABLE,
                ELEMENTAL_SPIRIT_CHANCE
        );
    }

    private static ResourceLocation key(String path) {
        return ResourceLocation.fromNamespaceAndPath("imitationcoreapi", path);
    }
}
