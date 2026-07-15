package com.github.gamekinger1st.imitationcoreapi.api.gecko;

import net.minecraft.resources.ResourceLocation;

public interface GeckoAnimationRegistration {
    ResourceLocation id();

    boolean unregister();
}
