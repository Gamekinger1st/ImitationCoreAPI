package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import java.util.Objects;

record DiscordInboundMessage(String messageId, String authorId, String authorName, String content) {
    DiscordInboundMessage {
        messageId = requireNonBlank(messageId, "messageId");
        authorId = requireNonBlank(authorId, "authorId");
        authorName = requireNonBlank(authorName, "authorName");
        content = requireNonBlank(content, "content");
    }

    private static String requireNonBlank(String value, String name) {
        Objects.requireNonNull(value, name);
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return normalized;
    }
}
