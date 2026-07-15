package com.github.gamekinger1st.imitationcoreapi.api.service;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TransformationRepository {
    Optional<IdentitySnapshot> snapshot(UUID snapshotId);

    void saveSnapshot(IdentitySnapshot snapshot);

    Optional<TransformationSession> session(UUID sessionId);

    Collection<TransformationSession> sessionsForOwner(UUID ownerId);

    Collection<TransformationSession> sessions();

    void saveSession(TransformationSession session);
}
