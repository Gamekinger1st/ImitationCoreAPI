package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

public interface ChatModerationProvider {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    ChatModerationDecision evaluate(ChatModerationContext context);
}
