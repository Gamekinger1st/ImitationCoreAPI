package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorFormProgressionStateTest {
    @Test
    void firstObservationOnlySeedsTheProgressionAnchor() {
        ImitatorFormProgressionState state = ImitatorFormProgressionState.empty(UUID.randomUUID());
        DisguiseAppraisalSnapshot current = new DisguiseAppraisalSnapshot(10F, 20F, 2, Optional.of(new TensuraVitals(100D, 40D, 60D, 12D)));

        ImitatorFormProgressionState observed = state.observe(current);

        assertTrue(state.lastDelta(current).isEmpty());
        assertEquals(Optional.of(current), observed.lastObservedAppraisal());
        assertTrue(observed.accumulatedDelta().isEmpty());
    }

    @Test
    void accumulatesOnlyPositiveProgressionDeltas() {
        UUID snapshotId = UUID.randomUUID();
        ImitatorFormProgressionState state = ImitatorFormProgressionState.empty(snapshotId)
                .observe(new DisguiseAppraisalSnapshot(10F, 20F, 2, Optional.of(new TensuraVitals(100D, 40D, 60D, 12D))));
        DisguiseAppraisalSnapshot current = new DisguiseAppraisalSnapshot(9F, 25F, 3, Optional.of(new TensuraVitals(125D, 45D, 80D, 11D)));

        ImitatorFormStatDelta delta = state.lastDelta(current);
        ImitatorFormProgressionState observed = state.observe(current);

        assertEquals(0F, delta.health());
        assertEquals(0F, delta.maxHealth());
        assertEquals(0, delta.armorValue());
        assertEquals(new TensuraVitals(25D, 5D, 20D, 0D), delta.tensuraVitals().orElseThrow());
        assertEquals(delta, observed.accumulatedDelta());
        assertEquals(observed, ImitatorFormProgressionState.fromTag(observed.toTag()));
    }

    @Test
    void doesNotTreatOrdinaryHealingAsPermanentFormProgression() {
        ImitatorFormProgressionState state = ImitatorFormProgressionState.empty(UUID.randomUUID())
                .observe(new DisguiseAppraisalSnapshot(5F, 20F, 2, Optional.empty()));

        ImitatorFormStatDelta delta = state.lastDelta(new DisguiseAppraisalSnapshot(20F, 20F, 2, Optional.empty()));

        assertTrue(delta.isEmpty());
    }

    @Test
    void doesNotRepeatIgnoredTransientChangesEveryTick() {
        ImitatorFormProgressionState state = ImitatorFormProgressionState.empty(UUID.randomUUID())
                .observe(new DisguiseAppraisalSnapshot(5F, 20F, 2, Optional.of(new TensuraVitals(100D, 40D, 60D, 5D))));
        DisguiseAppraisalSnapshot buffed = new DisguiseAppraisalSnapshot(20F, 40F, 20, Optional.of(new TensuraVitals(100D, 40D, 60D, 50D)));

        ImitatorFormProgressionState first = state.observe(buffed);
        ImitatorFormProgressionState second = first.observe(buffed);

        assertTrue(first.accumulatedDelta().isEmpty());
        assertTrue(second.accumulatedDelta().isEmpty());
    }
}
