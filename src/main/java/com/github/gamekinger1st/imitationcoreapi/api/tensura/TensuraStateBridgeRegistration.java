package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.resources.ResourceLocation;

public interface TensuraStateBridgeRegistration {
    ResourceLocation id();

    boolean unregister();
}
