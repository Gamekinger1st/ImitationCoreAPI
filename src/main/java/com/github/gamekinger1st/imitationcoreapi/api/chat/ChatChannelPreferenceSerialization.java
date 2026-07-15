package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public final class ChatChannelPreferenceSerialization {
    private ChatChannelPreferenceSerialization() {
    }

    public static CompoundTag toTag(UUID playerId, ResourceLocation channelId) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("player", Objects.requireNonNull(playerId, "playerId"));
        tag.putString("channel", Objects.requireNonNull(channelId, "channelId").toString());
        return tag;
    }

    public static Entry fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.hasUUID("player") || !tag.contains("channel", Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Chat channel preference is missing required fields");
        }
        ResourceLocation channelId = ResourceLocation.tryParse(tag.getString("channel"));
        if (channelId == null) {
            throw new IllegalArgumentException("Chat channel preference contains an invalid channel id");
        }
        return new Entry(tag.getUUID("player"), channelId);
    }

    public record Entry(UUID playerId, ResourceLocation channelId) {
        public Entry {
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(channelId, "channelId");
        }
    }
}
