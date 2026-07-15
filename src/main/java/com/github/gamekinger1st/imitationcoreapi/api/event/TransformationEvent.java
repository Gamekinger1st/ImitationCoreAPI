package com.github.gamekinger1st.imitationcoreapi.api.event;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TransformationEvent(
        TransformationEventType type,
        Optional<UUID> ownerId,
        Optional<IdentitySnapshot> snapshot,
        Optional<TransformationSession> session,
        Optional<TransformationLifecycleReason> lifecycleReason
) {
    public TransformationEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(lifecycleReason, "lifecycleReason");
    }
}
