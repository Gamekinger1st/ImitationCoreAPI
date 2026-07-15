package com.github.gamekinger1st.imitationcoreapi.api.targeting;

import net.minecraft.resources.ResourceLocation;

public interface MobFactionRegistration {
    ResourceLocation id();

    boolean unregister();
}
