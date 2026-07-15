package com.github.gamekinger1st.imitationcoreapi.api.gecko;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeckoAnimationSnapshotTest {
    @Test
    void normalizesControllerNames() {
        GeckoAnimationSnapshot snapshot = new GeckoAnimationSnapshot(UUID.randomUUID(), ResourceLocation.withDefaultNamespace("zombie"), 4, List.of(" idle ", "walk", "idle", ""));

        assertEquals(List.of("idle", "walk"), snapshot.controllerNames());
        assertEquals(List.of("idle", "walk"), snapshot.controllers().stream().map(GeckoControllerSnapshot::controllerName).toList());
    }

    @Test
    void keepsControllerStateSerializable() {
        GeckoControllerSnapshot controller = new GeckoControllerSnapshot(" base ", " RUNNING ", List.of(" walk ", "walk", ""), List.of(" bite ", "bite"), Double.NaN, -4D, " attack ", true);
        GeckoAnimationSnapshot snapshot = new GeckoAnimationSnapshot(UUID.randomUUID(), ResourceLocation.withDefaultNamespace("zombie"), 4, List.of("fallback"), List.of(controller));

        assertEquals(List.of("base", "fallback"), snapshot.controllerNames());
        assertEquals(List.of("walk"), snapshot.controllers().get(0).animationNames());
        assertEquals(List.of("bite"), snapshot.controllers().get(0).triggerableAnimationNames());
        assertEquals(1D, snapshot.controllers().get(0).animationSpeed());
        assertEquals(0D, snapshot.controllers().get(0).transitionLength());
        assertEquals("attack", snapshot.controllers().get(0).triggeredAnimationName());
        assertTrue(snapshot.controllers().get(0).playingTriggeredAnimation());
    }
}
