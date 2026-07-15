package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.resources.ResourceLocation;

public interface SkillBridgeRegistration {
    ResourceLocation id();

    boolean unregister();
}
