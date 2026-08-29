package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;

import com.github.gamekinger1st.imitationcoreapi.api.network.PersonaChatPayload;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class PersonaChatBus {
    private final List<PersonaChatListener> listeners = new CopyOnWriteArrayList<>();

    public PersonaChatListenerRegistration register(PersonaChatListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(PersonaChatPayload payload) {
        Objects.requireNonNull(payload, "payload");
        for (PersonaChatListener listener : listeners) {
            try {
                listener.onPersonaChat(payload);
            } catch (RuntimeException | LinkageError exception) {
                ImitationCoreApi.LOGGER.error("An imitation persona chat listener failed", exception);
            }
        }
    }
}
