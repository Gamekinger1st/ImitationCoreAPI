package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ImitatorReplicaOutcome(SessionTransitionResult transition, Optional<ImitatorFormProgression> progression, Optional<UUID> replicaId) {
    public ImitatorReplicaOutcome {
        Objects.requireNonNull(transition, "transition");
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(replicaId, "replicaId");
    }

    public ImitatorReplicaOutcome(SessionTransitionResult transition, Optional<ImitatorFormProgression> progression) {
        this(transition, progression, Optional.empty());
    }
}
