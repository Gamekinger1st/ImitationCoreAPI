package com.github.gamekinger1st.imitationcoreapi.internal.tensura;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TensuraEnergyTransitionServiceTest {
    @Test
    void lowersEnergyByTensurasConfiguredStepShape() {
        assertEquals(95.0D, TensuraEnergyTransitionService.nextDownwardValue(100.0D, 20.0D, 5.0D));
    }

    @Test
    void stopsExactlyAtTheCopiedFormsMaximum() {
        assertEquals(20.0D, TensuraEnergyTransitionService.nextDownwardValue(22.0D, 20.0D, 5.0D));
        assertEquals(20.0D, TensuraEnergyTransitionService.nextDownwardValue(20.0D, 20.0D, 5.0D));
        assertEquals(10.0D, TensuraEnergyTransitionService.nextDownwardValue(10.0D, 20.0D, 5.0D));
    }

    @Test
    void rejectsInvalidTransitionSteps() {
        assertThrows(IllegalArgumentException.class,
                () -> TensuraEnergyTransitionService.nextDownwardValue(100.0D, 20.0D, 0.0D));
    }
}
