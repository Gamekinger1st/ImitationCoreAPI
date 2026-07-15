package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ServerChatDeliveryBus {
    private final List<ServerChatDeliveryListener> listeners = new CopyOnWriteArrayList<>();

    public ServerChatDeliveryRegistration register(ServerChatDeliveryListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(ChatEnvelope envelope) {
        Objects.requireNonNull(envelope, "envelope");
        for (ServerChatDeliveryListener listener : listeners) {
            listener.onServerChatDelivered(envelope);
        }
    }
}
