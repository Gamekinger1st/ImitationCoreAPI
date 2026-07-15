package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraCopyPolicy;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorMirrorSyncPolicyTest {
    @Test
    void delegatesMirrorEligibilityToTheConfiguredTensuraPolicy() {
        ImitatorMirrorSyncPolicy policy = new ImitatorMirrorSyncPolicy(new TensuraCopyPolicy(1D, 0.85D, true));
        TensuraVitals imitator = new TensuraVitals(100D, 50D, 50D, 20D);

        assertTrue(policy.tensuraCopyPolicy().evaluate(imitator, new TensuraVitals(100D, 50D, 50D, 20D), 0.85D, true).accepted());
        assertFalse(policy.tensuraCopyPolicy().evaluate(imitator, new TensuraVitals(101D, 50D, 50D, 20D), 1D, true).accepted());
    }
}
