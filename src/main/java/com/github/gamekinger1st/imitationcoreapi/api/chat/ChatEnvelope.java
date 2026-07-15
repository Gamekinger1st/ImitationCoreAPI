package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ChatEnvelope(
        UUID messageId,
        Instant sentAt,
        ResourceLocation channelId,
        ChatChannelKind channelKind,
        ChatMessageSource source,
        Optional<UUID> realSenderId,
        Optional<String> realSenderName,
        Optional<PersonaIdentity> persona,
        String message
) {
    public static final int MAX_MESSAGE_LENGTH = 256;
    public static final int MAX_SENDER_NAME_LENGTH = 64;

    public ChatEnvelope {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(sentAt, "sentAt");
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(channelKind, "channelKind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(realSenderId, "realSenderId");
        Objects.requireNonNull(realSenderName, "realSenderName");
        Objects.requireNonNull(persona, "persona");
        Objects.requireNonNull(message, "message");
        message = message.strip();
        if (message.isEmpty() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Chat message must contain between 1 and " + MAX_MESSAGE_LENGTH + " characters");
        }
        realSenderName = realSenderName.map(String::strip).filter(value -> !value.isEmpty());
        if (realSenderName.isPresent() && realSenderName.get().length() > MAX_SENDER_NAME_LENGTH) {
            throw new IllegalArgumentException("Chat sender name exceeds the configured limit");
        }
        if (source == ChatMessageSource.SERVER_SYSTEM && realSenderId.isPresent()) {
            throw new IllegalArgumentException("System messages cannot have a real player sender");
        }
        if (source != ChatMessageSource.SERVER_SYSTEM && (realSenderId.isEmpty() || realSenderName.isEmpty())) {
            throw new IllegalArgumentException("Player messages must retain their real sender identity");
        }
    }

    public Component displayComponent() {
        MutableComponent prefix = Component.literal("[" + channelId.getPath() + "] ").withStyle(ChatFormatting.DARK_GRAY);
        if (source == ChatMessageSource.SERVER_SYSTEM) {
            return prefix.append(Component.literal(message).withStyle(ChatFormatting.GRAY));
        }
        String renderedName = persona.map(PersonaIdentity::displayName).orElseGet(() -> realSenderName.orElse("Unknown"));
        ChatFormatting color = persona.isPresent() ? ChatFormatting.LIGHT_PURPLE : source == ChatMessageSource.DISCORD_BRIDGE ? ChatFormatting.AQUA : ChatFormatting.WHITE;
        return prefix.append(Component.literal("<")).append(Component.literal(renderedName).withStyle(color)).append(Component.literal("> ")).append(Component.literal(message));
    }

    public Component vanillaFallbackComponent() {
        if (source == ChatMessageSource.SERVER_SYSTEM) {
            return displayComponent();
        }
        String realName = realSenderName.orElse("Unknown");
        String renderedName = persona.map(PersonaIdentity::displayName).orElse(realName);
        if (persona.isEmpty()) {
            return displayComponent();
        }
        return Component.literal("[" + channelId.getPath() + "] <" + renderedName + " via " + realName + "> " + message).withStyle(ChatFormatting.GRAY);
    }
}
