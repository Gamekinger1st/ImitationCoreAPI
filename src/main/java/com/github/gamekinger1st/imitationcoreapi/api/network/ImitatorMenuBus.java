package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ImitatorMenuBus {
    private final List<ImitatorMenuListener> listeners = new CopyOnWriteArrayList<>();

    public ImitatorMenuRegistration register(ImitatorMenuListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(ImitatorMenuRequest request) {
        Objects.requireNonNull(request, "request");
        if (request == ImitatorMenuRequest.NONE) {
            return;
        }
        for (ImitatorMenuListener listener : listeners) {
            listener.onImitatorMenuRequested(request);
        }
    }
}
