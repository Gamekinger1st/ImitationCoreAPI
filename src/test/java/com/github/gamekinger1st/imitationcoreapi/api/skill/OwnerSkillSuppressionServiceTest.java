package com.github.gamekinger1st.imitationcoreapi.api.skill;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnerSkillSuppressionServiceTest {
    private static final ResourceLocation IMITATOR = ResourceLocation.fromNamespaceAndPath("troverhaul", "imitator");
    private static final ResourceLocation OWNER_SKILL = ResourceLocation.fromNamespaceAndPath("troverhaul", "owner_skill");
    private static final ResourceLocation COPIED_SKILL = ResourceLocation.fromNamespaceAndPath("tensura", "copied_skill");

    @Test
    void allowsSkillsWhenSessionHasNoSuppressionMarker() {
        TransformationSession session = session(List.of());

        assertTrue(OwnerSkillSuppressionService.evaluate(session, OWNER_SKILL, Optional.empty()).allowed());
    }

    @Test
    void deniesOriginalOwnerSkillsWhenSuppressionIsActive() {
        TransformationSession session = session(List.of(suppressionMarker()));

        assertFalse(OwnerSkillSuppressionService.evaluate(session, OWNER_SKILL, Optional.empty()).allowed());
    }

    @Test
    void allowsControllerSkillWhileSuppressionIsActive() {
        TransformationSession session = session(List.of(suppressionMarker()));

        assertTrue(OwnerSkillSuppressionService.evaluate(session, IMITATOR, Optional.empty()).allowed());
    }

    @Test
    void allowsAnAlreadyOwnedSkillWhenTheCopiedFormAlsoProvidesIt() {
        TransformationSession session = session(List.of(suppressionMarker(IMITATOR, COPIED_SKILL)));

        assertTrue(OwnerSkillSuppressionService.evaluate(session, COPIED_SKILL, Optional.empty()).allowed());
    }

    @Test
    void allowsCopiedTemporarySkillsOwnedByTheActiveSession() {
        UUID referenceId = UUID.randomUUID();
        TransformationSession session = session(List.of(suppressionMarker(), borrowedSkill(referenceId)));

        assertTrue(OwnerSkillSuppressionService.evaluate(session, COPIED_SKILL, Optional.of(new TemporarySkillOwnership(session.sessionId(), referenceId))).allowed());
    }

    @Test
    void deniesTemporarySkillsThatAreNotOwnedByTheActiveSession() {
        UUID referenceId = UUID.randomUUID();
        TransformationSession session = session(List.of(suppressionMarker(), borrowedSkill(referenceId)));

        assertFalse(OwnerSkillSuppressionService.evaluate(session, COPIED_SKILL, Optional.of(new TemporarySkillOwnership(UUID.randomUUID(), referenceId))).allowed());
    }

    private static TransformationSession session(List<TemporaryStateReference> references) {
        UUID sessionId = UUID.randomUUID();
        return new TransformationSession(
                sessionId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                TransformationScope.SURFACE,
                0D,
                BaselineSnapshot.empty(),
                CompatibilityAssessment.visual("test"),
                TransformationState.ACTIVE,
                1L,
                1L,
                0L,
                Optional.empty(),
                references.stream()
                        .map(reference -> new TemporaryStateReference(reference.referenceId(), sessionId, reference.handlerId(), reference.kind(), reference.payload(), reference.status()))
                        .toList()
        );
    }

    private static TemporaryStateReference suppressionMarker() {
        return suppressionMarker(IMITATOR);
    }

    private static TemporaryStateReference suppressionMarker(ResourceLocation... controllerSkills) {
        CompoundTag payload = new CompoundTag();
        ListTag skills = new ListTag();
        java.util.Arrays.stream(controllerSkills).map(ResourceLocation::toString).map(StringTag::valueOf).forEach(skills::add);
        payload.put(OwnerSkillSuppressionService.CONTROLLER_SKILLS_KEY, skills);
        return new TemporaryStateReference(UUID.randomUUID(), UUID.randomUUID(), OwnerSkillSuppressionService.HANDLER_ID, TemporaryStateKinds.OWNER_SKILL_SUPPRESSION, payload, TemporaryStateStatus.ACTIVE);
    }

    private static TemporaryStateReference borrowedSkill(UUID referenceId) {
        CompoundTag payload = new CompoundTag();
        payload.putString(TemporarySkillService.SKILL_ID_KEY, COPIED_SKILL.toString());
        return new TemporaryStateReference(referenceId, UUID.randomUUID(), TemporarySkillService.HANDLER_ID, TemporaryStateKinds.BORROWED_SKILL, payload, TemporaryStateStatus.ACTIVE);
    }
}
