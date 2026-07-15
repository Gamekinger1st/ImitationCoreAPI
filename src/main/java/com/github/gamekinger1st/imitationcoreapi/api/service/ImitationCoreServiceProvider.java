package com.github.gamekinger1st.imitationcoreapi.api.service;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatService;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSynchronizer;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public interface ImitationCoreServiceProvider {
    TransformationService transformations(MinecraftServer server);

    TransformationApplicationService applications(MinecraftServer server);

    ImitatorHandlerService imitatorHandlers(MinecraftServer server);

    ChatService chats(MinecraftServer server);

    ImitatorSynchronizer imitatorSynchronizer();

    void release(MinecraftServer server);

    default ImitatorHandlerService imitatorHandlers(ServerPlayer player) {
        return imitatorHandlers(player.serverLevel().getServer());
    }

    default ChatService chats(ServerPlayer player) {
        return chats(player.serverLevel().getServer());
    }
}
