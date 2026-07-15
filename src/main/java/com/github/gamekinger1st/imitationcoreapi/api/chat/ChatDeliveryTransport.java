package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

@FunctionalInterface
public interface ChatDeliveryTransport {
    void deliver(Collection<ServerPlayer> recipients, ChatEnvelope envelope);
}
