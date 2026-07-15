package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;

public record ChatChannelSelectionResult(boolean accepted, Optional<ResourceLocation> channelId, String message) {
    public ChatChannelSelectionResult {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(message, "message");
        message = message.strip();
        if (message.isEmpty() || message.length() > 256) {
            throw new IllegalArgumentException("Chat channel selection message is outside the supported bounds");
        }
        if (accepted != channelId.isPresent()) {
            throw new IllegalArgumentException("Accepted chat channel selections must include a channel");
        }
    }

    public static ChatChannelSelectionResult selected(ResourceLocation channelId) {
        return new ChatChannelSelectionResult(true, Optional.of(Objects.requireNonNull(channelId, "channelId")), "Active chat channel: " + channelId.getPath());
    }

    public static ChatChannelSelectionResult rejected(String message) {
        return new ChatChannelSelectionResult(false, Optional.empty(), message);
    }
}
