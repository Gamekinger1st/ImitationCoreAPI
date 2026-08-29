package com.github.gamekinger1st.imitationcoreapi.api.event;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;

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
            try {
                listener.onTransformationEvent(event);
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("An imitation transformation event listener failed", exception);
            }
        }
    }
}
