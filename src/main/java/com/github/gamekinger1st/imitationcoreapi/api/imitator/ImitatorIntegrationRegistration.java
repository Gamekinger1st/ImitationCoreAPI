package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.resources.ResourceLocation;

public interface ImitatorIntegrationRegistration {
    ResourceLocation id();

    boolean unregister();
}
