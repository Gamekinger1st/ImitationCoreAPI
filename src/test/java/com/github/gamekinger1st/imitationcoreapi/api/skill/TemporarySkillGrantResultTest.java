package com.github.gamekinger1st.imitationcoreapi.api.skill;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemporarySkillGrantResultTest {
    @Test
    void requiresLedgerOwnershipForSuccessfulTemporaryGrants() {
        assertTrue(TemporarySkillGrantResult.success(UUID.randomUUID()).operation().successful());
        assertFalse(TemporarySkillGrantResult.failure("Rejected").operation().successful());
        assertThrows(IllegalArgumentException.class, () -> new TemporarySkillGrantResult(SkillOperationResult.success(), Optional.empty()));
    }
}
