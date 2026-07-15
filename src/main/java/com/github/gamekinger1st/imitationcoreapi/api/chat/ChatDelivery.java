package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Objects;

public record ChatDelivery(ChatChannelKind channelKind, Collection<ServerPlayer> recipients) {
    public ChatDelivery {
        Objects.requireNonNull(channelKind, "channelKind");
        Objects.requireNonNull(recipients, "recipients");
        recipients = java.util.List.copyOf(recipients);
        if (recipients.size() > 1024) {
            throw new IllegalArgumentException("Chat delivery recipient count exceeds the configured limit");
        }
    }
}
