package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillClassification;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorSkillCopyPolicyTest {
    @Test
    void retainsOnlyBoundedNonDeniedSkillsAtScaledMastery() {
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation imitator = ResourceLocation.fromNamespaceAndPath("test", "imitator");
        ResourceLocation denied = ResourceLocation.fromNamespaceAndPath("test", "denied");
        ImitatorSkillCopySnapshot snapshot = new ImitatorSkillCopySnapshot(
                bridge,
                ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION,
                List.of(
                        new ImitatorCopiedSkill(imitator, 100D),
                        new ImitatorCopiedSkill(denied, 90D),
                        new ImitatorCopiedSkill(ResourceLocation.fromNamespaceAndPath("test", "high"), 80D),
                        new ImitatorCopiedSkill(ResourceLocation.fromNamespaceAndPath("test", "low"), 5D),
                        new ImitatorCopiedSkill(ResourceLocation.fromNamespaceAndPath("test", "middle"), 40D)
                )
        );
        ImitatorSkillCopyPolicy policy = new ImitatorSkillCopyPolicy(2, 10D, 0.5D, Integer.MAX_VALUE, java.util.Set.of(denied));

        assertEquals(
                List.of(
                        new ImitatorCopiedSkill(ResourceLocation.fromNamespaceAndPath("test", "high"), 40D),
                        new ImitatorCopiedSkill(ResourceLocation.fromNamespaceAndPath("test", "middle"), 20D)
                ),
                policy.select(snapshot, imitator, skillId -> SkillClassification.STANDARD)
        );
    }

    @Test
    void makesUniqueAndUltimateEligibilityAnExplicitPerSkillChoice() {
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation imitator = ResourceLocation.fromNamespaceAndPath("test", "imitator");
        ResourceLocation unique = ResourceLocation.fromNamespaceAndPath("test", "unique");
        ResourceLocation ultimate = ResourceLocation.fromNamespaceAndPath("test", "ultimate");
        ResourceLocation standard = ResourceLocation.fromNamespaceAndPath("test", "standard");
        ResourceLocation unknown = ResourceLocation.fromNamespaceAndPath("test", "unknown");
        ImitatorSkillCopySnapshot snapshot = new ImitatorSkillCopySnapshot(
                bridge,
                ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION,
                List.of(
                        new ImitatorCopiedSkill(unique, 100D),
                        new ImitatorCopiedSkill(ultimate, 90D),
                        new ImitatorCopiedSkill(standard, 80D),
                        new ImitatorCopiedSkill(unknown, 70D)
                )
        );
        ImitatorSkillCopyPolicy defaultPolicy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(4)
                .masteryMultiplier(1D)
                .temporaryRemoveTime(Integer.MAX_VALUE)
                .build();

        assertEquals(
                List.of(new ImitatorCopiedSkill(standard, 80D)),
                defaultPolicy.select(snapshot, imitator, skillId -> {
                    if (skillId.equals(unique)) return SkillClassification.UNIQUE;
                    if (skillId.equals(ultimate)) return SkillClassification.ULTIMATE;
                    if (skillId.equals(unknown)) return SkillClassification.UNKNOWN;
                    return SkillClassification.STANDARD;
                })
        );

        ImitatorSkillCopyPolicy enabledPolicy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(4)
                .masteryMultiplier(1D)
                .temporaryRemoveTime(Integer.MAX_VALUE)
                .allowUniqueSkills(true)
                .allowUltimateSkills(true)
                .allowUnclassifiedSkills(true)
                .build();

        assertEquals(
                List.of(
                        new ImitatorCopiedSkill(unique, 100D),
                        new ImitatorCopiedSkill(ultimate, 90D),
                        new ImitatorCopiedSkill(standard, 80D),
                        new ImitatorCopiedSkill(unknown, 70D)
                ),
                enabledPolicy.select(snapshot, imitator, skillId -> {
                    if (skillId.equals(unique)) return SkillClassification.UNIQUE;
                    if (skillId.equals(ultimate)) return SkillClassification.ULTIMATE;
                    if (skillId.equals(unknown)) return SkillClassification.UNKNOWN;
                    return SkillClassification.STANDARD;
                })
        );
    }

    @Test
    void treatsTensuraStandardTiersAsDefaultCopyableSkills() {
        ImitatorSkillCopyPolicy policy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(1)
                .masteryMultiplier(1D)
                .temporaryRemoveTime(Integer.MAX_VALUE)
                .build();

        assertTrue(policy.allows(SkillClassification.STANDARD));
        assertTrue(policy.allows(SkillClassification.RESISTANCE));
        assertTrue(policy.allows(SkillClassification.INTRINSIC));
        assertTrue(policy.allows(SkillClassification.COMMON));
        assertTrue(policy.allows(SkillClassification.EXTRA));
    }

    @Test
    void appliesEpBasedImitatorCopyRules() {
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        ResourceLocation imitator = ResourceLocation.fromNamespaceAndPath("test", "imitator");
        ResourceLocation intrinsic = ResourceLocation.fromNamespaceAndPath("test", "intrinsic");
        ResourceLocation ultimate = ResourceLocation.fromNamespaceAndPath("test", "ultimate");
        ResourceLocation unique = ResourceLocation.fromNamespaceAndPath("test", "unique");
        ResourceLocation extra = ResourceLocation.fromNamespaceAndPath("test", "extra");
        ImitatorSkillCopySnapshot snapshot = new ImitatorSkillCopySnapshot(
                bridge,
                ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION,
                List.of(
                        new ImitatorCopiedSkill(intrinsic, 100D),
                        new ImitatorCopiedSkill(ultimate, 90D),
                        new ImitatorCopiedSkill(unique, 80D),
                        new ImitatorCopiedSkill(extra, 70D)
                )
        );
        ImitatorSkillCopyPolicy policy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(4)
                .masteryMultiplier(1D)
                .temporaryRemoveTime(Integer.MAX_VALUE)
                .allowUniqueSkills(false)
                .allowUltimateSkills(false)
                .build();

        assertEquals(
                List.of(
                        new ImitatorCopiedSkill(intrinsic, 100D),
                        new ImitatorCopiedSkill(extra, 70D)
                ),
                policy.select(snapshot, imitator, skillId -> {
                    if (skillId.equals(intrinsic)) return SkillClassification.INTRINSIC;
                    if (skillId.equals(ultimate)) return SkillClassification.ULTIMATE;
                    if (skillId.equals(unique)) return SkillClassification.UNIQUE;
                    return SkillClassification.EXTRA;
                }, ImitatorSkillCopyAccess.SUPERIOR_EP)
        );
        assertEquals(
                List.of(
                        new ImitatorCopiedSkill(extra, 70D)
                ),
                policy.select(snapshot, imitator, skillId -> {
                    if (skillId.equals(intrinsic)) return SkillClassification.INTRINSIC;
                    if (skillId.equals(ultimate)) return SkillClassification.ULTIMATE;
                    if (skillId.equals(unique)) return SkillClassification.UNIQUE;
                    return SkillClassification.EXTRA;
                }, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP)
        );
    }

    @Test
    void appliesEpBasedRulesToClassifiedFormAbilities() {
        ImitatorSkillCopyPolicy policy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(1)
                .masteryMultiplier(1D)
                .temporaryRemoveTime(Integer.MAX_VALUE)
                .build();
        ImitatorFormAbility ultimateAbility = new ClassifiedAbility(SkillClassification.ULTIMATE);
        ImitatorFormAbility intrinsicAbility = new ClassifiedAbility(SkillClassification.INTRINSIC);
        ImitatorFormAbility uniqueAbility = new ClassifiedAbility(SkillClassification.UNIQUE);
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.fromNamespaceAndPath("test", "entity"), 0L).build();

        assertFalse(policy.allows(ultimateAbility, snapshot, ImitatorSkillCopyAccess.SUPERIOR_EP));
        assertTrue(policy.allows(intrinsicAbility, snapshot, ImitatorSkillCopyAccess.SUPERIOR_EP));
        assertFalse(policy.allows(uniqueAbility, snapshot, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP));
        assertFalse(policy.allows(ultimateAbility, snapshot, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP));
        assertFalse(policy.allows(intrinsicAbility, snapshot, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP));
    }

    @Test
    void roundTripsSkillCopyPolicyStatePayload() {
        ResourceLocation denied = ResourceLocation.fromNamespaceAndPath("test", "denied");
        ImitatorSkillCopyPolicy policy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(5)
                .minimumSourceMastery(12.5D)
                .masteryMultiplier(0.75D)
                .temporaryRemoveTime(90)
                .denySkill(denied)
                .allowUniqueSkills(true)
                .allowUltimateSkills(true)
                .allowUnclassifiedSkills(true)
                .build();

        CompoundTag tag = policy.toTag();
        ImitatorSkillCopyPolicy restored = ImitatorSkillCopyPolicy.fromTag(tag);

        assertEquals(policy, restored);
    }

    @Test
    void roundTripsTheSanitizedSkillCopyExtension() {
        ImitatorSkillCopySnapshot snapshot = new ImitatorSkillCopySnapshot(
                ResourceLocation.fromNamespaceAndPath("test", "bridge"),
                ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION,
                List.of(new ImitatorCopiedSkill(ResourceLocation.fromNamespaceAndPath("test", "copied"), 20D))
        );
        SnapshotExtension extension = ImitatorSkillCopyExtensions.create(snapshot);

        assertEquals(Optional.of(snapshot), ImitatorSkillCopyExtensions.find(List.of(extension)));
        assertTrue(ImitatorSkillCopyPolicy.DISABLED.select(snapshot, ResourceLocation.fromNamespaceAndPath("test", "imitator")).isEmpty());
    }

    @Test
    void rejectsMalformedCopiedSkillExtensionsWithoutThrowing() {
        CompoundTag missingBridge = new CompoundTag();
        missingBridge.putInt("schema", ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION);
        SnapshotExtension missingBridgeExtension = new SnapshotExtension(ImitatorSkillCopyExtensions.ID, ImitatorSkillCopyExtensions.SCHEMA_VERSION, missingBridge);
        assertTrue(ImitatorSkillCopyExtensions.find(List.of(missingBridgeExtension)).isEmpty());

        CompoundTag duplicatePayload = new CompoundTag();
        duplicatePayload.putString("bridge_id", ResourceLocation.fromNamespaceAndPath("test", "bridge").toString());
        duplicatePayload.putInt("schema", ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION);
        ListTag skills = new ListTag();
        CompoundTag first = new CompoundTag();
        first.putString("skill_id", ResourceLocation.fromNamespaceAndPath("test", "same").toString());
        first.putDouble("mastery", 1D);
        CompoundTag second = first.copy();
        skills.add(first);
        skills.add(second);
        duplicatePayload.put("skills", skills);
        SnapshotExtension duplicateExtension = new SnapshotExtension(ImitatorSkillCopyExtensions.ID, ImitatorSkillCopyExtensions.SCHEMA_VERSION, duplicatePayload);

        assertTrue(ImitatorSkillCopyExtensions.find(List.of(duplicateExtension)).isEmpty());
    }

    private record ClassifiedAbility(SkillClassification classification) implements ImitatorFormAbility {
        @Override
        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath("test", classification.name().toLowerCase(java.util.Locale.ROOT));
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public boolean supports(com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot snapshot) {
            return true;
        }

        @Override
        public SkillClassification classification(IdentitySnapshot snapshot) {
            return classification;
        }
    }
}
