package com.github.gamekinger1st.imitationcoreapi.api.skill;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class OwnerSkillSuppressionService {
    public static final ResourceLocation HANDLER_ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "owner_skill_suppression");
    public static final String CONTROLLER_SKILLS_KEY = "controller_skills";
    private static final String DENIAL = "The owner's original skills are suppressed by the active copied form";
    private final TransformationService transformations;

    public OwnerSkillSuppressionService(TransformationService transformations) {
        this.transformations = Objects.requireNonNull(transformations, "transformations");
    }

    public SessionTransitionResult suppressOriginalSkills(LivingEntity owner, UUID sessionId, Collection<ResourceLocation> controllerSkillIds) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(controllerSkillIds, "controllerSkillIds");
        Optional<TransformationSession> found = transformations.session(sessionId);
        if (found.isEmpty()) {
            return SessionTransitionResult.rejected("The requested transformation session does not exist");
        }
        TransformationSession session = found.get();
        if (!session.ownerId().equals(owner.getUUID())) {
            return SessionTransitionResult.rejected("The requested transformation session does not belong to this entity");
        }
        if (session.state() != TransformationState.APPLYING && session.state() != TransformationState.ACTIVE) {
            return SessionTransitionResult.rejected("Owner skill suppression can only be attached to an applying or active transformation");
        }
        Set<ResourceLocation> controllers = sanitize(controllerSkillIds);
        if (hasSuppressionMarker(session, controllers)) {
            return SessionTransitionResult.accepted(session);
        }
        TemporaryStateReference reference = new TemporaryStateReference(
                UUID.randomUUID(),
                sessionId,
                HANDLER_ID,
                TemporaryStateKinds.OWNER_SKILL_SUPPRESSION,
                payload(controllers),
                TemporaryStateStatus.ACTIVE
        );
        return transformations.addTemporaryState(sessionId, reference, owner.level().getGameTime());
    }

    public OwnerSkillUseDecision evaluate(LivingEntity owner, ResourceLocation skillId, Optional<TemporarySkillOwnership> ownership) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(ownership, "ownership");
        return transformations.activeSessionForOwner(owner.getUUID())
                .map(session -> evaluate(session, skillId, ownership))
                .orElseGet(OwnerSkillUseDecision::allow);
    }

    public static OwnerSkillUseDecision evaluate(TransformationSession session, ResourceLocation skillId, Optional<TemporarySkillOwnership> ownership) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(skillId, "skillId");
        Objects.requireNonNull(ownership, "ownership");
        if (suppressionReferences(session).isEmpty()) {
            return OwnerSkillUseDecision.allow();
        }
        if (suppressionReferences(session).stream().anyMatch(reference -> controllerSkills(reference).contains(skillId))) {
            return OwnerSkillUseDecision.allow();
        }
        if (ownership.filter(value -> isBorrowedSkillOwnedBy(session, skillId, value)).isPresent()) {
            return OwnerSkillUseDecision.allow();
        }
        return OwnerSkillUseDecision.denied(DENIAL);
    }

    private static boolean hasSuppressionMarker(TransformationSession session, Set<ResourceLocation> controllers) {
        return suppressionReferences(session).stream().anyMatch(reference -> controllerSkills(reference).equals(controllers));
    }

    private static boolean isBorrowedSkillOwnedBy(TransformationSession session, ResourceLocation skillId, TemporarySkillOwnership ownership) {
        if (!session.sessionId().equals(ownership.sessionId())) {
            return false;
        }
        return session.temporaryState().stream()
                .filter(reference -> reference.referenceId().equals(ownership.referenceId()))
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.BORROWED_SKILL))
                .filter(reference -> reference.status() == TemporaryStateStatus.PREPARED || reference.status() == TemporaryStateStatus.ACTIVE)
                .anyMatch(reference -> skillId.toString().equals(reference.payload().getString(TemporarySkillService.SKILL_ID_KEY)));
    }

    private static Set<ResourceLocation> sanitize(Collection<ResourceLocation> skillIds) {
        LinkedHashSet<ResourceLocation> sanitized = new LinkedHashSet<>();
        for (ResourceLocation skillId : skillIds) {
            sanitized.add(Objects.requireNonNull(skillId, "controller skill id"));
        }
        if (sanitized.size() > 32) {
            throw new IllegalArgumentException("At most 32 controller skills can bypass owner skill suppression");
        }
        return Set.copyOf(sanitized);
    }

    private static CompoundTag payload(Set<ResourceLocation> controllerSkillIds) {
        CompoundTag payload = new CompoundTag();
        ListTag skills = new ListTag();
        controllerSkillIds.stream().map(ResourceLocation::toString).sorted().map(StringTag::valueOf).forEach(skills::add);
        payload.put(CONTROLLER_SKILLS_KEY, skills);
        return payload;
    }

    private static Set<ResourceLocation> controllerSkills(TemporaryStateReference reference) {
        LinkedHashSet<ResourceLocation> result = new LinkedHashSet<>();
        ListTag tags = reference.payload().getList(CONTROLLER_SKILLS_KEY, Tag.TAG_STRING);
        for (int index = 0; index < tags.size(); index++) {
            ResourceLocation skillId = ResourceLocation.tryParse(tags.getString(index));
            if (skillId != null) {
                result.add(skillId);
            }
        }
        return Set.copyOf(result);
    }

    private static java.util.List<TemporaryStateReference> suppressionReferences(TransformationSession session) {
        return session.temporaryState().stream()
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.OWNER_SKILL_SUPPRESSION))
                .filter(reference -> reference.status() == TemporaryStateStatus.PREPARED || reference.status() == TemporaryStateStatus.ACTIVE)
                .toList();
    }
}
