package com.github.gamekinger1st.imitationcoreapi.api.diagnostic;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ImitationDiagnosticBus {
    private final List<ImitationDiagnosticListener> listeners = new CopyOnWriteArrayList<>();

    public ImitationDiagnosticRegistration register(ImitationDiagnosticListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    public void post(ImitationDiagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        for (ImitationDiagnosticListener listener : listeners) {
            listener.onImitationDiagnostic(diagnostic);
        }
    }
}
