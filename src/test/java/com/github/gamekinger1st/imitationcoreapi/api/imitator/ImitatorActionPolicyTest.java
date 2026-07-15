package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorActionPolicyTest {
    @Test
    void twoArgumentPolicyDefaultsToRequiringLineOfSight() {
        ImitatorActionPolicy policy = new ImitatorActionPolicy(12D, true);

        assertTrue(policy.requireLineOfSight());
        assertTrue(policy.allowPlayerTargets());
    }

    @Test
    void policyCanAllowNonVisibleTargetsWhenAnAddonExplicitlyChoosesIt() {
        ImitatorActionPolicy policy = new ImitatorActionPolicy(12D, false, false);

        assertFalse(policy.requireLineOfSight());
        assertFalse(policy.allowPlayerTargets());
    }
}
