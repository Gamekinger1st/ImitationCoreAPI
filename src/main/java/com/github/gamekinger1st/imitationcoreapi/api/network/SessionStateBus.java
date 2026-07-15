package com.github.gamekinger1st.imitationcoreapi.api.network;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SessionStateBus {
    private final List<SessionStateListener> listeners = new CopyOnWriteArrayList<>();

    public SessionStateRegistration register(SessionStateListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(SessionStatePayload payload) {
        Objects.requireNonNull(payload, "payload");
        for (SessionStateListener listener : listeners) {
            listener.onSessionState(payload);
        }
    }
}
