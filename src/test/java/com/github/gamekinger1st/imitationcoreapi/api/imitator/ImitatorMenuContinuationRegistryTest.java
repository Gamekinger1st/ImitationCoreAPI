package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorMenuContinuationRegistryTest {
    @Test
    void consumesMatchingContinuationOnlyOnce() {
        ImitatorMenuContinuationRegistry registry = new ImitatorMenuContinuationRegistry();
        UUID playerId = UUID.randomUUID();
        registry.stage(playerId, ImitatorMenuRequest.SELECT_TRANSFORM_FORM, ImitatorSkillMode.TRANSFORM, 100L, (player, slot) -> {
        });

        assertTrue(registry.take(playerId, ImitatorSkillMode.TRANSFORM, 101L).isPresent());
        assertFalse(registry.take(playerId, ImitatorSkillMode.TRANSFORM, 101L).isPresent());
    }

    @Test
    void rejectsChangedModeAndExpiredContinuation() {
        ImitatorMenuContinuationRegistry registry = new ImitatorMenuContinuationRegistry();
        UUID changedMode = UUID.randomUUID();
        UUID expired = UUID.randomUUID();
        registry.stage(changedMode, ImitatorMenuRequest.SELECT_TRANSFORM_FORM, ImitatorSkillMode.REPLICA, 100L, (player, slot) -> {
        });
        registry.stage(expired, ImitatorMenuRequest.SELECT_TRANSFORM_FORM, ImitatorSkillMode.TRANSFORM, 100L, (player, slot) -> {
        });

        assertFalse(registry.take(changedMode, ImitatorSkillMode.TRANSFORM, 101L).isPresent());
        assertFalse(registry.take(expired, ImitatorSkillMode.TRANSFORM, 100L + ImitatorMenuContinuationRegistry.MAX_WAIT_TICKS + 1L).isPresent());
        assertEquals(0, registry.pendingCount(Long.MAX_VALUE));
    }

    @Test
    void acceptsOnlyTransformAndReplicaSelectionMenus() {
        ImitatorMenuContinuationRegistry registry = new ImitatorMenuContinuationRegistry();
        UUID playerId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> registry.stage(playerId, ImitatorMenuRequest.COMMIT_RECORD, ImitatorSkillMode.TRANSFORM, 0L, (player, slot) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> registry.stage(playerId, ImitatorMenuRequest.SELECT_TRANSFORM_FORM, ImitatorSkillMode.RECORD, 0L, (player, slot) -> {
        }));
    }
}
