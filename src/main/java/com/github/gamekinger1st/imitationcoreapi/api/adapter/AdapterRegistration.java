package com.github.gamekinger1st.imitationcoreapi.api.adapter;

import net.minecraft.resources.ResourceLocation;

public interface AdapterRegistration {
    ResourceLocation id();

    boolean unregister();
}
