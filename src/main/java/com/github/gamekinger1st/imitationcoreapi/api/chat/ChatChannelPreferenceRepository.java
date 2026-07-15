package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.UUID;

public interface ChatChannelPreferenceRepository {
    Optional<ResourceLocation> activeChatChannel(UUID playerId);

    void saveActiveChatChannel(UUID playerId, ResourceLocation channelId);
}
