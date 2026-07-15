package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImitatorTransformDurationPolicyTest {
    @Test
    void zeroMinutesMeansUnlimited() {
        ImitatorTransformDurationPolicy policy = ImitatorTransformDurationPolicy.minutes(0);

        assertEquals(OptionalLong.empty(), policy.expiresFrom(100L));
        assertEquals(0L, policy.durationTicks());
    }

    @Test
    void positiveMinutesBecomeServerGameTimeDeadlines() {
        ImitatorTransformDurationPolicy policy = ImitatorTransformDurationPolicy.minutes(3);

        assertEquals(3_600L, policy.durationTicks());
        assertEquals(OptionalLong.of(3_700L), policy.expiresFrom(100L));
    }

    @Test
    void rejectsNegativeMinutes() {
        assertThrows(IllegalArgumentException.class, () -> ImitatorTransformDurationPolicy.minutes(-1));
    }
}
