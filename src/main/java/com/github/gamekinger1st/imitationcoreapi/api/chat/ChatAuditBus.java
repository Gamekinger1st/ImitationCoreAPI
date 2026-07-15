package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ChatAuditBus {
    private final List<ChatAuditListener> listeners = new CopyOnWriteArrayList<>();

    public ChatAuditRegistration register(ChatAuditListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(ChatAuditEntry entry) {
        Objects.requireNonNull(entry, "entry");
        for (ChatAuditListener listener : listeners) {
            listener.onChatAudit(entry);
        }
    }
}
