package com.github.gamekinger1st.imitationcoreapi.api.chat;

@FunctionalInterface
public interface ChatMessageListener {
    void onChatMessage(ChatEnvelope envelope);
}
