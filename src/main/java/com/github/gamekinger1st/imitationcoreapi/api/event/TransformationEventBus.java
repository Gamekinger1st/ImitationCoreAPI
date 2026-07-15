package com.github.gamekinger1st.imitationcoreapi.api.event;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TransformationEventBus {
    private final List<TransformationEventListener> listeners = new CopyOnWriteArrayList<>();

    public TransformationEventRegistration register(TransformationEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(TransformationEvent event) {
        Objects.requireNonNull(event, "event");
        for (TransformationEventListener listener : listeners) {
            listener.onTransformationEvent(event);
        }
    }
}
