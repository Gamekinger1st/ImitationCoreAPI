package com.github.gamekinger1st.imitationcoreapi.api.application;

import net.minecraft.resources.ResourceLocation;

public interface TransformationApplicationRegistration {
    ResourceLocation id();

    boolean unregister();
}
