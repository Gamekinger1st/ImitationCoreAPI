package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientDisguiseStateBus {
    private final List<ClientDisguiseStateListener> listeners = new CopyOnWriteArrayList<>();

    public ClientDisguiseStateRegistration register(ClientDisguiseStateListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void postActivated(ClientDisguiseState state) {
        Objects.requireNonNull(state, "state");
        for (ClientDisguiseStateListener listener : listeners) {
            listener.onDisguiseActivated(state);
        }
    }

    public void postCleared(int entityId, UUID ownerId) {
        Objects.requireNonNull(ownerId, "ownerId");
        for (ClientDisguiseStateListener listener : listeners) {
            listener.onDisguiseCleared(entityId, ownerId);
        }
    }
}
