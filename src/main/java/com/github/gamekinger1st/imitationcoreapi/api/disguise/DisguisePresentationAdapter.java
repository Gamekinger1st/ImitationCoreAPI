package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public interface DisguisePresentationAdapter {
    ResourceLocation id();

    int priority();

    boolean supports(Entity subject, ClientDisguiseState state);

    DisguisePresentation presentation(Entity subject, ClientDisguiseState state);
}
