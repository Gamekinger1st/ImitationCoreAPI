package com.github.gamekinger1st.imitationcoreapi.api.skill;

import java.util.Objects;
import java.util.UUID;

public record TemporarySkillOwnership(UUID sessionId, UUID referenceId) {
    public TemporarySkillOwnership {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(referenceId, "referenceId");
    }
}
