package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillClassification;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorFormAbilityRegistryTest {
    private static final ResourceLocation ENTITY = ResourceLocation.fromNamespaceAndPath("test", "entity");
    private static final ResourceLocation OTHER_ENTITY = ResourceLocation.fromNamespaceAndPath("test", "other_entity");

    @Test
    void activeAbilitiesAreFilteredAndPriorityOrdered() {
        ImitatorFormAbilityRegistry registry = new ImitatorFormAbilityRegistry();
        registry.register(new TestAbility(ResourceLocation.fromNamespaceAndPath("test", "low"), 0, ENTITY, true, false));
        registry.register(new TestAbility(ResourceLocation.fromNamespaceAndPath("test", "high"), 10, ENTITY, true, false));
        registry.register(new TestAbility(ResourceLocation.fromNamespaceAndPath("test", "other"), 20, OTHER_ENTITY, true, false));

        List<ResourceLocation> ids = registry.activeAbilities(snapshot(ENTITY)).stream().map(ImitatorFormAbility::id).toList();

        assertEquals(List.of(ResourceLocation.fromNamespaceAndPath("test", "high"), ResourceLocation.fromNamespaceAndPath("test", "low")), ids);
    }

    @Test
    void tickingAbilitiesAreSeparateFromActiveAbilities() {
        ImitatorFormAbilityRegistry registry = new ImitatorFormAbilityRegistry();
        ResourceLocation tickId = ResourceLocation.fromNamespaceAndPath("test", "tick");
        registry.register(new TestAbility(tickId, 0, ENTITY, false, true));

        assertTrue(registry.activeAbilities(snapshot(ENTITY)).isEmpty());
        assertEquals(List.of(tickId), registry.tickingAbilities(snapshot(ENTITY)).stream().map(ImitatorFormAbility::id).toList());
    }

    @Test
    void policyFiltersClassifiedAbilitiesByEpAccess() {
        ImitatorFormAbilityRegistry registry = new ImitatorFormAbilityRegistry();
        ResourceLocation standard = ResourceLocation.fromNamespaceAndPath("test", "standard");
        ResourceLocation ultimate = ResourceLocation.fromNamespaceAndPath("test", "ultimate");
        registry.register(new TestAbility(standard, 0, ENTITY, true, true, SkillClassification.STANDARD));
        registry.register(new TestAbility(ultimate, 10, ENTITY, true, true, SkillClassification.ULTIMATE));
        ImitatorSkillCopyPolicy policy = ImitatorSkillCopyPolicy.builder()
                .maximumCopiedSkills(1)
                .masteryMultiplier(1D)
                .temporaryRemoveTime(Integer.MAX_VALUE)
                .build();

        assertEquals(List.of(ultimate, standard), registry.activeAbilities(snapshot(ENTITY), policy, ImitatorSkillCopyAccess.SUPERIOR_EP).stream().map(ImitatorFormAbility::id).toList());
        assertEquals(List.of(standard), registry.activeAbilities(snapshot(ENTITY), policy, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP).stream().map(ImitatorFormAbility::id).toList());
        assertEquals(List.of(standard), registry.tickingAbilities(snapshot(ENTITY), policy, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP).stream().map(ImitatorFormAbility::id).toList());
    }

    @Test
    void rejectsDuplicateAbilityIds() {
        ImitatorFormAbilityRegistry registry = new ImitatorFormAbilityRegistry();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "duplicate");
        registry.register(new TestAbility(id, 0, ENTITY, true, false));

        assertThrows(IllegalArgumentException.class, () -> registry.register(new TestAbility(id, 1, ENTITY, true, false)));
    }

    @Test
    void registrationCanUnregister() {
        ImitatorFormAbilityRegistry registry = new ImitatorFormAbilityRegistry();
        ImitatorFormAbilityRegistration registration = registry.register(new TestAbility(ResourceLocation.fromNamespaceAndPath("test", "temporary"), 0, ENTITY, true, false));

        assertTrue(registration.unregister());
        assertTrue(registry.activeAbilities(snapshot(ENTITY)).isEmpty());
    }

    @Test
    void brokenAddonAbilitiesDoNotBlockOtherAbilitiesFromBeingDiscovered() {
        ImitatorFormAbilityRegistry registry = new ImitatorFormAbilityRegistry();
        ResourceLocation stable = ResourceLocation.fromNamespaceAndPath("test", "stable");
        registry.register(new ThrowingAbility(ResourceLocation.fromNamespaceAndPath("test", "broken_support"), 20, true));
        registry.register(new ThrowingAbility(ResourceLocation.fromNamespaceAndPath("test", "broken_active"), 10, false));
        registry.register(new TestAbility(stable, 0, ENTITY, true, true));

        assertEquals(List.of(stable), registry.activeAbilities(snapshot(ENTITY)).stream().map(ImitatorFormAbility::id).toList());
        assertEquals(List.of(stable), registry.tickingAbilities(snapshot(ENTITY)).stream().map(ImitatorFormAbility::id).toList());
    }

    private static IdentitySnapshot snapshot(ResourceLocation entityType) {
        return IdentitySnapshot.builder(entityType, 0L).displayName("Test").build();
    }

    private record TestAbility(ResourceLocation id, int priority, ResourceLocation entityType, boolean active, boolean ticking, SkillClassification classification) implements ImitatorFormAbility {
        private TestAbility(ResourceLocation id, int priority, ResourceLocation entityType, boolean active, boolean ticking) {
            this(id, priority, entityType, active, ticking, SkillClassification.STANDARD);
        }

        @Override
        public boolean supports(IdentitySnapshot snapshot) {
            return entityType.equals(snapshot.entityType());
        }

        @Override
        public boolean hasActiveAbility(IdentitySnapshot snapshot) {
            return active && supports(snapshot);
        }

        @Override
        public boolean hasTickAbility(IdentitySnapshot snapshot) {
            return ticking && supports(snapshot);
        }

        @Override
        public SkillClassification classification(IdentitySnapshot snapshot) {
            return classification;
        }
    }

    private record ThrowingAbility(ResourceLocation id, int priority, boolean throwsInSupports) implements ImitatorFormAbility {
        @Override
        public boolean supports(IdentitySnapshot snapshot) {
            if (throwsInSupports) {
                throw new IllegalStateException("broken supports");
            }
            return true;
        }

        @Override
        public boolean hasActiveAbility(IdentitySnapshot snapshot) {
            throw new IllegalStateException("broken active check");
        }

        @Override
        public boolean hasTickAbility(IdentitySnapshot snapshot) {
            throw new IllegalStateException("broken tick check");
        }
    }
}
