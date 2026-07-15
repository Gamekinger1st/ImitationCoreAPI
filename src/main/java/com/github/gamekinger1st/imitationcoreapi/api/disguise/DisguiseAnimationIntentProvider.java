package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.List;

public interface DisguiseAnimationIntentProvider {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    boolean supports(Entity imitation, Entity subject, ClientDisguiseState state, DisguiseAnimationIntent baseIntent);

    List<String> customTriggers(Entity imitation, Entity subject, ClientDisguiseState state, DisguiseAnimationIntent baseIntent);
}
