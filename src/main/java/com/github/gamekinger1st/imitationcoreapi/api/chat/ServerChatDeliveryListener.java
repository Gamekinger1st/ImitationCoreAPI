package com.github.gamekinger1st.imitationcoreapi.api.chat;

@FunctionalInterface
public interface ServerChatDeliveryListener {
    void onServerChatDelivered(ChatEnvelope envelope);
}
