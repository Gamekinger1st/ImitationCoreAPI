package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImitatorSkillDefinitionTest {
    @Test
    void retainsTheTensuraFacingCostsOutsideCoreGameplayState() {
        ImitatorReplicaPolicy replicaPolicy = new ImitatorReplicaPolicy(1_200, 2D, 32D, true, true, true, true, true, true, true, "Replica");
        ImitatorSkillDefinition definition = new ImitatorSkillDefinition(
                ResourceLocation.fromNamespaceAndPath("troverhaul", "imitator"),
                "Imitator",
                "Copies recorded forms",
                100_000L,
                500L,
                1_500L,
                Map.of(
                        ImitatorSkillMode.RECORD, new ImitatorSkillCost(100L, 5, 0L),
                        ImitatorSkillMode.TRANSFORM, new ImitatorSkillCost(500L, 30, 100L),
                        ImitatorSkillMode.REPLICA, new ImitatorSkillCost(1_000L, 60, 500L)
                ),
                ImitatorProgressionPolicy.DEFAULT,
                ImitatorMirrorSyncPolicy.DEFAULT,
                ImitatorSkillCopyPolicy.DISABLED,
                replicaPolicy,
                ImitatorTransformDurationPolicy.minutes(10),
                new ImitatorFormLibraryLimits(8, 64, 1_200L)
        );

        assertEquals(500L, definition.cost(ImitatorSkillMode.TRANSFORM).resourceCost());
        assertEquals(replicaPolicy, definition.replicaPolicy());
        assertEquals(10, definition.transformDurationPolicy().durationMinutes());
        assertEquals(8, definition.formLibraryLimits().slotCapacity());
        assertEquals(ImitatorSkillMode.TRANSFORM, ImitatorSkillMode.RECORD.next());
        assertEquals(ImitatorSkillMode.RECORD, ImitatorSkillMode.REPLICA.next());
    }

    @Test
    void buildsImitatorLikeDefinitionsForAddonSkillAuthors() {
        ImitatorReplicaPolicy replicaPolicy = new ImitatorReplicaPolicy(1_800, 4D, 64D, true, true, false, true, true, false, false, "Echo");
        ImitatorSkillCopyPolicy copiedSkillPolicy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(4)
                .allowUniqueSkills(true)
                .allowUltimateSkills(false)
                .build();
        ImitatorFormLibraryLimits limits = new ImitatorFormLibraryLimits(12, 128, 2_400L);

        ImitatorSkillDefinition definition = ImitatorSkillDefinition.builder(ResourceLocation.fromNamespaceAndPath("addon", "echo_form"), "Echo Form", "Copies forms with addon-owned tuning")
                .learningCost(25_000L)
                .mastery(100L, 2_000L)
                .recordCost(25L, 20, 0L)
                .transformCost(100L, 40, 100L)
                .replicaCost(400L, 80, 500L)
                .progressionPolicy(ImitatorProgressionPolicy.DEFAULT)
                .formLibraryLimits(limits)
                .copiedSkillPolicy(copiedSkillPolicy)
                .transformDurationMinutes(5)
                .perfectFormPolicy(ImitatorMirrorSyncPolicy.DEFAULT)
                .replicaPolicy(replicaPolicy)
                .build();

        assertEquals(ResourceLocation.fromNamespaceAndPath("addon", "echo_form"), definition.skillId());
        assertEquals(100L, definition.cost(ImitatorSkillMode.TRANSFORM).resourceCost());
        assertEquals(40, definition.cost(ImitatorSkillMode.TRANSFORM).cooldownTicks());
        assertEquals(5, definition.transformDurationPolicy().durationMinutes());
        assertEquals(limits, definition.formLibraryLimits());
        assertEquals(copiedSkillPolicy, definition.skillCopyPolicy());
        assertEquals(replicaPolicy, definition.replicaPolicy());
    }

    @Test
    void clonesDefinitionsThroughTheBuilder() {
        ImitatorSkillDefinition original = ImitatorSkillDefinition.builder(ResourceLocation.fromNamespaceAndPath("addon", "first_form"), "First Form", "First")
                .maximumMastery(1_000L)
                .transformDurationMinutes(3)
                .build();

        ImitatorSkillDefinition clone = ImitatorSkillDefinitionBuilder.from(original)
                .displayName("Second Form")
                .build();

        assertEquals("Second Form", clone.displayName());
        assertEquals(original.skillId(), clone.skillId());
        assertEquals(original.transformDurationPolicy(), clone.transformDurationPolicy());
        assertEquals(original.formLibraryLimits(), clone.formLibraryLimits());
    }

    @Test
    void requiresEveryCoreOwnedSkillModeToHaveAnAdapterCost() {
        assertThrows(IllegalArgumentException.class, () -> new ImitatorSkillDefinition(
                ResourceLocation.fromNamespaceAndPath("troverhaul", "imitator"),
                "Imitator",
                "Copies recorded forms",
                0L,
                0L,
                0L,
                Map.of(ImitatorSkillMode.RECORD, ImitatorSkillCost.free())
        ));
    }
}
