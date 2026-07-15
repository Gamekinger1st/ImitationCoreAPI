package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisguiseAnimationIntentTest {
    @Test
    void normalizesCustomTriggersAndClampsAnimationScalars() {
        DisguiseAnimationIntent intent = intent(false, false, false, false, false, false, false, false, false, 4F, 3F, -1F, List.of(" Roar ", "", "roar", "Cannon"));

        assertEquals(1F, intent.walkSpeed());
        assertEquals(1F, intent.attackAnimation());
        assertEquals(0F, intent.previousAttackAnimation());
        assertEquals(List.of("roar", "cannon"), intent.customTriggers());
        assertEquals(List.of("roar"), intent.triggerKeywordGroups().getFirst());
        assertEquals(List.of("cannon"), intent.triggerKeywordGroups().get(1));
    }

    @Test
    void ordersUrgentStatesBeforeMovementStates() {
        DisguiseAnimationIntent intent = intent(true, true, true, true, true, true, true, true, true, 0.5F, 0.5F, 0.25F, List.of());

        assertEquals(List.of("death", "dead", "die", "faint"), intent.triggerKeywordGroups().getFirst());
        assertEquals(List.of("hurt", "damage", "hit", "flinch"), intent.triggerKeywordGroups().get(1));
        assertEquals(List.of("attack", "bite", "claw", "tail", "roar", "shoot", "cannon", "slam", "spit", "leap"), intent.triggerKeywordGroups().get(2));
    }

    @Test
    void appendsAdditionalCustomTriggersWithoutDuplicatingNormalizedNames() {
        DisguiseAnimationIntent intent = intent(true, false, false, false, false, false, false, false, false, 0.5F, 0F, 0F, List.of("roar"))
                .withAdditionalCustomTriggers(List.of(" Roar ", "voice_cannon"));

        assertEquals(List.of("roar", "voice_cannon"), intent.customTriggers());
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
            boolean dying,
            float walkSpeed,
            float attackAnimation,
            float previousAttackAnimation,
            List<String> customTriggers
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
                walkSpeed,
                attackAnimation,
                previousAttackAnimation,
                0,
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
                customTriggers
        );
    }
}
