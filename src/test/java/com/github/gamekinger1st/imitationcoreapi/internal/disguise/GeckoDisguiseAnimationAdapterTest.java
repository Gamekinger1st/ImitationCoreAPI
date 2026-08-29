package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoControllerSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAnimationIntent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeckoDisguiseAnimationAdapterTest {
    private static final GeckoControllerSnapshot ATTACKING = new GeckoControllerSnapshot(
            "main",
            "RUNNING",
            List.of("attack"),
            List.of("attack"),
            1D,
            0D,
            "attack",
            true
    );

    @Test
    void ignoresPlaybackStateStoredInAFormSnapshot() {
        assertTrue(GeckoDisguiseAnimationAdapter.playbackTrigger(ATTACKING, false).isEmpty());
    }

    @Test
    void preservesPlaybackStateFromALiveGeckoEntity() {
        assertEquals("attack", GeckoDisguiseAnimationAdapter.playbackTrigger(ATTACKING, true).orElseThrow());
    }

    @Test
    void mapsLiveIntentToRecordedControllerCapabilities() {
        GeckoControllerSnapshot controller = new GeckoControllerSnapshot(
                "main",
                "RUNNING",
                List.of("idle", "walk", "attack"),
                List.of("idle", "walk", "attack", "hurt", "death"),
                1D,
                0D,
                "attack",
                true
        );
        DisguiseAnimationIntent intent = new DisguiseAnimationIntent(
                true,
                false,
                false,
                false,
                false,
                false,
                true,
                true,
                true,
                true,
                0.5F,
                0.5F,
                0F,
                0,
                4,
                4,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                List.of()
        );

        assertEquals("death", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent).orElseThrow());
    }

    @Test
    void customIntentTriggersCanOverrideBuiltInMovement() {
        GeckoControllerSnapshot controller = new GeckoControllerSnapshot(
                "main",
                "RUNNING",
                List.of("walk", "voice_cannon"),
                List.of("walk", "voice_cannon"),
                1D,
                0D,
                "",
                false
        );
        DisguiseAnimationIntent intent = new DisguiseAnimationIntent(
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                true,
                0.5F,
                0F,
                0F,
                0,
                0,
                0,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                List.of("voice_cannon")
        );

        assertEquals("voice_cannon", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent).orElseThrow());
    }

    @Test
    void retainsOneShotAnimationsBeforeReturningToMovement() {
        assertTrue(GeckoDisguiseAnimationAdapter.shouldRetainOneShot("attack", "walk", 24, 10));
        assertTrue(!GeckoDisguiseAnimationAdapter.shouldRetainOneShot("attack", "hurt", 24, 10));
        assertTrue(!GeckoDisguiseAnimationAdapter.shouldRetainOneShot("attack", "walk", 9, 10));
    }

    @Test
    void mapsCrouchUseSprintAndIdleIntentToRecordedTriggersInPriorityOrder() {
        GeckoControllerSnapshot controller = new GeckoControllerSnapshot(
                "main",
                "RUNNING",
                List.of("idle", "sprint", "crouch", "cast"),
                List.of("idle", "sprint", "crouch", "cast"),
                1D,
                0D,
                "",
                false
        );

        assertEquals("cast", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(false, false, false, true, false)).orElseThrow());
        assertEquals("crouch", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(false, false, true, false, false)).orElseThrow());
        assertEquals("sprint", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, true, false, false, false)).orElseThrow());
        assertEquals("idle", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(false, false, false, false, false)).orElseThrow());
    }

    @Test
    void mapsCombatAndMovementIntentToRecordedTriggersInStrictPriorityOrder() {
        GeckoControllerSnapshot controller = new GeckoControllerSnapshot(
                "main",
                "RUNNING",
                List.of("idle", "walk", "run", "swim", "attack", "hurt", "death"),
                List.of("idle", "walk", "run", "swim", "attack", "hurt", "death"),
                1D,
                0D,
                "",
                false
        );

        assertEquals("death", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, true, false, true, true, true, true, true, true)).orElseThrow());
        assertEquals("hurt", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, true, false, true, true, true, true, true, false)).orElseThrow());
        assertEquals("attack", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, true, false, true, true, true, true, false, false)).orElseThrow());
        assertEquals("swim", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, true, false, true, false, false, false, false, false)).orElseThrow());
        assertEquals("run", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, true, false, false, false, false, false, false, false)).orElseThrow());
        assertEquals("walk", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(true, false, false, false, false, false, false, false, false)).orElseThrow());
        assertEquals("idle", GeckoDisguiseAnimationAdapter.inferredTrigger(controller, intent(false, false, false, false, false, false, false, false, false)).orElseThrow());
    }

    private static DisguiseAnimationIntent intent(boolean moving, boolean sprinting, boolean crouching, boolean usingItem, boolean attacking) {
        return intent(moving, sprinting, crouching, false, false, usingItem, attacking, false, false);
    }

    private static DisguiseAnimationIntent intent(
            boolean moving,
            boolean sprinting,
            boolean crouching,
            boolean swimming,
            boolean fallFlying,
            boolean usingItem,
            boolean attacking,
            boolean hurt,
            boolean dying
    ) {
        return new DisguiseAnimationIntent(
                moving,
                sprinting,
                crouching,
                swimming,
                fallFlying,
                usingItem,
                attacking,
                hurt,
                dying,
                true,
                moving ? 0.5F : 0F,
                attacking ? 0.5F : 0F,
                0F,
                attacking ? 1 : 0,
                hurt ? 4 : 0,
                dying ? 4 : 0,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                0F,
                List.of()
        );
    }
}
