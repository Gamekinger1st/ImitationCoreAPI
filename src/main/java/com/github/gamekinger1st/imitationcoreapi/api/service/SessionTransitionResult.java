package com.github.gamekinger1st.imitationcoreapi.api.service;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;

import java.util.Objects;
import java.util.Optional;

public record SessionTransitionResult(boolean accepted, Optional<TransformationSession> session, String message) {
    public SessionTransitionResult {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(message, "message");
        if (accepted && session.isEmpty()) {
            throw new IllegalArgumentException("Accepted transitions require a session");
        }
    }

    public static SessionTransitionResult accepted(TransformationSession session) {
        return new SessionTransitionResult(true, Optional.of(session), "");
    }

    public static SessionTransitionResult rejected(String message) {
        return new SessionTransitionResult(false, Optional.empty(), message);
    }
}
