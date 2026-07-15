package com.github.gamekinger1st.imitationcoreapi.api.network;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ImitatorFormLibraryBus {
    private final List<ImitatorFormLibraryListener> listeners = new CopyOnWriteArrayList<>();

    public ImitatorFormLibraryRegistration register(ImitatorFormLibraryListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(ImitatorFormLibraryPayload payload) {
        Objects.requireNonNull(payload, "payload");
        for (ImitatorFormLibraryListener listener : listeners) {
            listener.onFormLibrary(payload);
        }
    }
}
