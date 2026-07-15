package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

public interface PersonaChatRegistration {
    ResourceLocation id();

    boolean unregister();
}
