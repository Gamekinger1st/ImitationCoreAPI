package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;

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
            try {
                listener.onFormLibrary(payload);
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("An imitation form library listener failed", exception);
            }
        }
    }
}
