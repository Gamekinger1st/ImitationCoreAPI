package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

public record ChatModerationContext(ChatChannelRequest request, ChatDelivery delivery, Optional<PersonaIdentity> persona) {
    public ChatModerationContext {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(delivery, "delivery");
        Objects.requireNonNull(persona, "persona");
    }

    public ServerPlayer sender() {
        return request.sender();
    }
}
