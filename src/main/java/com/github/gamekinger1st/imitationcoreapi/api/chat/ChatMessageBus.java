package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ChatMessageBus {
    private final List<ChatMessageListener> listeners = new CopyOnWriteArrayList<>();

    public ChatMessageRegistration register(ChatMessageListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(ChatEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        for (ChatMessageListener listener : listeners) {
            listener.onChatMessage(envelope);
        }
    }
}
