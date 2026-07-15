package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;

public interface DisguiseAnimationRegistration {
    ResourceLocation id();

    boolean unregister();
}
