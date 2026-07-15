package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ChatAuditEntry(
        UUID messageId,
        Instant timestamp,
        ResourceLocation channelId,
        ChatMessageSource source,
        Optional<UUID> realSenderId,
        Optional<String> realSenderName,
        Optional<PersonaIdentity> persona,
        Collection<UUID> recipientIds,
        boolean delivered,
        Optional<String> moderationReason
) {
    public ChatAuditEntry {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(realSenderId, "realSenderId");
        Objects.requireNonNull(realSenderName, "realSenderName");
        Objects.requireNonNull(persona, "persona");
        Objects.requireNonNull(recipientIds, "recipientIds");
        Objects.requireNonNull(moderationReason, "moderationReason");
        recipientIds = java.util.List.copyOf(recipientIds);
        moderationReason = moderationReason.map(String::strip).filter(value -> !value.isEmpty());
    }
}
