package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ChatChannelRequest(ServerPlayer sender, ResourceLocation channelId, String message, Optional<UUID> targetPlayerId, ChatMessageSource source) {
    public ChatChannelRequest {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        Objects.requireNonNull(source, "source");
        message = message.strip();
        if (message.isEmpty() || message.length() > ChatEnvelope.MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Chat message must contain between 1 and " + ChatEnvelope.MAX_MESSAGE_LENGTH + " characters");
        }
        if (source == ChatMessageSource.SERVER_SYSTEM) {
            throw new IllegalArgumentException("Player chat requests cannot use the system source");
        }
    }
}
