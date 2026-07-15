package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface PersonaChatProvider {
    ResourceLocation id();

    int priority();

    PersonaChatDecision resolve(ServerPlayer sender, String rawText);
}
