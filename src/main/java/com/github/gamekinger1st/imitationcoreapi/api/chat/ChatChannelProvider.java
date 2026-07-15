package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface ChatChannelProvider {
    ResourceLocation id();

    ChatChannelKind kind();

    default int priority() {
        return 0;
    }

    default boolean acceptsPlayerMessages() {
        return true;
    }

    default boolean supportsActiveSelection() {
        return kind() != ChatChannelKind.DIRECT && kind() != ChatChannelKind.SYSTEM && kind() != ChatChannelKind.ACTION_BAR;
    }

    Optional<ChatDelivery> route(ChatChannelRequest request);
}
