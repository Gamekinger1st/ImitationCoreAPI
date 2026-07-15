package com.github.gamekinger1st.imitationcoreapi.api.skill;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record TemporarySkillGrantResult(SkillOperationResult operation, Optional<UUID> referenceId) {
    public TemporarySkillGrantResult {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(referenceId, "referenceId");
        if (operation.successful() != referenceId.isPresent()) {
            throw new IllegalArgumentException("Successful temporary skill grants require a ledger reference");
        }
    }

    public static TemporarySkillGrantResult success(UUID referenceId) {
        return new TemporarySkillGrantResult(SkillOperationResult.success(), Optional.of(referenceId));
    }

    public static TemporarySkillGrantResult failure(String detail) {
        return new TemporarySkillGrantResult(SkillOperationResult.failure(detail), Optional.empty());
    }
}
