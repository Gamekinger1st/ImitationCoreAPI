package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorProgressionPolicyTest {
    @Test
    void calculatesInitialPrecisionWithinConfiguredBounds() {
        ImitatorProgressionPolicy policy = ImitatorProgressionPolicy.DEFAULT;

        assertEquals(policy.maximumInitialPrecision(), policy.initialPrecision(ImitatorRecordingContext.DEFAULT));
        assertEquals(policy.minimumPrecision(), policy.initialPrecision(new ImitatorRecordingContext(1D, 1D, 0D, 0D, false)));
        assertEquals(policy.maximumInitialPrecision(), policy.initialPrecision(new ImitatorRecordingContext(0D, 0D, 1D, 1D, true)));
        assertFalse(policy.initialForm(UUID.randomUUID(), ImitatorRecordingContext.DEFAULT).skillCopyAllowed());
        assertTrue(policy.initialForm(UUID.randomUUID(), new ImitatorRecordingContext(0D, 0D, 1D, 0D, true)).skillCopyAllowed());
    }

    @Test
    void refinesFormsIntoPerfectAndMirrorEligibleStates() {
        ImitatorProgressionPolicy policy = ImitatorProgressionPolicy.DEFAULT;
        ImitatorForm form = new ImitatorForm(UUID.randomUUID(), 0.83D, false, false);

        ImitatorFormProgression first = policy.refine(form, ImitatorProgressionAction.TRANSFORM);
        ImitatorFormProgression perfect = policy.refine(new ImitatorForm(form.snapshotId(), 0.99D, false, true), ImitatorProgressionAction.TRANSFORM);

        assertEquals(0.85D, first.currentForm().precision());
        assertTrue(first.currentForm().mirrorSyncAllowed());
        assertFalse(first.currentForm().skillCopyAllowed());
        assertTrue(policy.allowsMirrorSync(first.currentForm()));
        assertTrue(perfect.currentForm().perfect());
        assertTrue(perfect.becamePerfect());
    }

    @Test
    void appliesPerfectCostsAndPowerLimitedReproduction() {
        ImitatorProgressionPolicy policy = ImitatorProgressionPolicy.DEFAULT;
        ImitatorForm ordinary = new ImitatorForm(UUID.randomUUID(), 0.9D, false, true);
        ImitatorForm perfect = new ImitatorForm(UUID.randomUUID(), 1D, true, true);

        assertEquals(1_000L, policy.adjustedResourceCost(1_000L, ordinary));
        assertEquals(500L, policy.adjustedResourceCost(1_000L, perfect));
        assertEquals(0.25D, policy.reproductionScale(0D, 1_000D, false));
        assertEquals(0.65D, policy.reproductionScale(500D, 1_000D, true));
        assertEquals(15, policy.masteryReward(ImitatorProgressionAction.TRANSFORM, true));
        assertFalse(policy.refine(perfect, ImitatorProgressionAction.TRANSFORM).becamePerfect());
    }
}
