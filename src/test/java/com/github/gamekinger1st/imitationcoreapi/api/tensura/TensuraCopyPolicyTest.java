package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraCopyPolicyTest {
    @Test
    void permitsVisualOnlyFormsWithoutMirrorSync() {
        TensuraCopyPolicy policy = new TensuraCopyPolicy(1D, 0.75D, false);

        TensuraCopyPolicy.TensuraCopyPolicyDecision decision = policy.evaluate(new TensuraVitals(10D, 10D, 10D, 10D), new TensuraVitals(1_000D, 10D, 10D, 10D), 0D, false);

        assertTrue(decision.accepted());
        assertEquals(0D, decision.scale());
    }

    @Test
    void enforcesPrecisionAndEpLimitsForMirrorSync() {
        TensuraCopyPolicy policy = new TensuraCopyPolicy(1.5D, 0.75D, true);
        TensuraVitals imitator = new TensuraVitals(100D, 10D, 10D, 10D);

        assertFalse(policy.evaluate(imitator, new TensuraVitals(50D, 10D, 10D, 10D), 0.5D, true).accepted());
        assertFalse(policy.evaluate(imitator, new TensuraVitals(151D, 10D, 10D, 10D), 1D, true).accepted());
        assertTrue(policy.evaluate(imitator, new TensuraVitals(150D, 10D, 10D, 10D), 0.75D, true).accepted());
    }

    @Test
    void scalesCopiedPowerToTheLowerOfPrecisionAndOwnerPower() {
        TensuraCopyPolicy policy = new TensuraCopyPolicy(1.5D, 0.5D, true);
        TensuraVitals imitator = new TensuraVitals(100D, 10D, 10D, 10D);
        TensuraVitals target = new TensuraVitals(150D, 10D, 10D, 10D);

        assertEquals(2D / 3D, policy.powerRatio(imitator, target));
        assertEquals(2D / 3D, policy.evaluate(imitator, target, 0.9D, true).scale());
    }
}
