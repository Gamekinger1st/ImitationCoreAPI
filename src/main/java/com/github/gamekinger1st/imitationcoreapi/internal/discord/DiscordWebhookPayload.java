package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatEnvelope;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;

final class DiscordWebhookPayload {
    private DiscordWebhookPayload() {
    }

    static String format(ChatEnvelope envelope) {
        String prefix = "[" + envelope.channelId().getPath() + "] ";
        if (envelope.source() == ChatMessageSource.SERVER_SYSTEM) {
            return prefix + envelope.message();
        }
        String realName = envelope.realSenderName().orElse("Unknown");
        String sender = envelope.persona().map(persona -> persona.displayName() + " via " + realName).orElse(realName);
        return prefix + "<" + sender + "> " + envelope.message();
    }

    static String json(String content) {
        return "{\"username\":\"Minecraft\",\"allowed_mentions\":{\"parse\":[]},\"content\":\"" + escape(content) + "\"}";
    }

    private static String escape(String value) {
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
