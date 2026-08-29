package com.github.gamekinger1st.imitationcoreapi.api.skill;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TemporarySkillService {
    public static final ResourceLocation HANDLER_ID = ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "temporary_skill_cleanup");
    public static final String BRIDGE_ID_KEY = "bridge_id";
    public static final String SKILL_ID_KEY = "skill_id";
    public static final String SESSION_KEY = "imitationcoreapi_session";
    public static final String REFERENCE_KEY = "imitationcoreapi_reference";
    public static final String REPLACED_SKILL_KEY = "replaced_skill";
    private final TransformationService transformations;

    public TemporarySkillService(TransformationService transformations) {
        this.transformations = Objects.requireNonNull(transformations, "transformations");
    }

    public TemporarySkillGrantResult grant(LivingEntity owner, UUID sessionId, ResourceLocation skillId, int removeTime) {
        return grant(owner, sessionId, skillId, removeTime, Optional.empty());
    }

    public TemporarySkillGrantResult grant(LivingEntity owner, UUID sessionId, ResourceLocation skillId, int removeTime, double mastery) {
        if (!Double.isFinite(mastery) || mastery < 0D) {
            throw new IllegalArgumentException("mastery must be finite and non-negative");
        }
        return grant(owner, sessionId, skillId, removeTime, Optional.of(mastery));
    }

    private TemporarySkillGrantResult grant(LivingEntity owner, UUID sessionId, ResourceLocation skillId, int removeTime, Optional<Double> mastery) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(skillId, "skillId");
        if (removeTime < 0) {
            throw new IllegalArgumentException("removeTime cannot be negative");
        }
        Optional<TransformationSession> session = transformations.session(sessionId);
        if (session.isEmpty()) {
            return TemporarySkillGrantResult.failure("The requested transformation session does not exist");
        }
        if (!session.get().ownerId().equals(owner.getUUID())) {
            return TemporarySkillGrantResult.failure("The requested transformation session does not belong to this entity");
        }
        if (session.get().state() != TransformationState.APPLYING && session.get().state() != TransformationState.ACTIVE) {
            return TemporarySkillGrantResult.failure("Temporary skills can only be granted to an applying or active transformation");
        }
        Optional<ResourceLocation> bridgeId = ImitationApi.skillBridges().activeBridgeId();
        if (bridgeId.isEmpty()) {
            return TemporarySkillGrantResult.failure("No compatible skill bridge is loaded");
        }
        UUID referenceId = UUID.randomUUID();
        CompoundTag payload = new CompoundTag();
        payload.putString(BRIDGE_ID_KEY, bridgeId.get().toString());
        payload.putString(SKILL_ID_KEY, skillId.toString());
        payload.putInt("remove_time", removeTime);
        Optional<SkillState> replaced = ImitationApi.skillBridges().capture(owner)
                .filter(snapshot -> snapshot.bridgeId().equals(bridgeId.get()))
                .flatMap(snapshot -> snapshot.skills().stream().filter(state -> state.skillId().equals(skillId)).findFirst());
        replaced.ifPresent(state -> payload.put(REPLACED_SKILL_KEY, SkillSnapshotSerialization.toTag(new SkillSnapshot(bridgeId.get(), 1, java.util.List.of(state)))));
        TemporaryStateReference reference = new TemporaryStateReference(
                referenceId,
                sessionId,
                HANDLER_ID,
                TemporaryStateKinds.BORROWED_SKILL,
                payload,
                TemporaryStateStatus.PREPARED
        );
        long gameTime = owner.level().getGameTime();
        SessionTransitionResult added = transformations.addTemporaryState(sessionId, reference, gameTime);
        if (!added.accepted()) {
            return TemporarySkillGrantResult.failure(added.message());
        }
        if (replaced.isPresent()) {
            SkillOperationResult removed = ImitationApi.skillBridges().removeSkill(owner, bridgeId.get(), skillId);
            if (!removed.successful()) {
                transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.CLEANED, gameTime);
                return TemporarySkillGrantResult.failure("The original skill could not be isolated: " + removed.detail());
            }
        }
        SkillOperationResult granted = ImitationApi.skillBridges().grantTemporary(owner, bridgeId.get(), skillId, removeTime, new TemporarySkillOwnership(sessionId, referenceId));
        if (!granted.successful()) {
            replaced.ifPresent(state -> ImitationApi.skillBridges().restoreSkill(owner, bridgeId.get(), state));
            transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.CLEANED, gameTime);
            return TemporarySkillGrantResult.failure(granted.detail());
        }
        if (mastery.isPresent()) {
            SkillOperationResult configured = ImitationApi.skillBridges().update(owner, bridgeId.get(), new SkillUpdateRequest(skillId, mastery, Optional.of(false), Optional.empty(), Optional.empty()));
            if (!configured.successful()) {
                return rollback(owner, bridgeId.get(), skillId, sessionId, referenceId, gameTime, "The temporary skill could not be configured: " + configured.detail());
            }
        }
        SessionTransitionResult activated = transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.ACTIVE, gameTime);
        return activated.accepted()
                ? TemporarySkillGrantResult.success(referenceId)
                : rollback(owner, bridgeId.get(), skillId, sessionId, referenceId, gameTime, "The temporary skill could not be activated: " + activated.message());
    }

    private TemporarySkillGrantResult rollback(LivingEntity owner, ResourceLocation bridgeId, ResourceLocation skillId, UUID sessionId, UUID referenceId, long gameTime, String failure) {
        SkillOperationResult revoked = ImitationApi.skillBridges().revokeTemporary(owner, bridgeId, skillId, new TemporarySkillOwnership(sessionId, referenceId));
        if (!revoked.successful()) {
            return TemporarySkillGrantResult.failure(failure + "; cleanup failed: " + revoked.detail());
        }
        transformations.session(sessionId).flatMap(session -> session.temporaryState().stream()
                        .filter(reference -> reference.referenceId().equals(referenceId))
                        .findFirst())
                .flatMap(TemporarySkillService::replacedSkill)
                .ifPresent(state -> ImitationApi.skillBridges().restoreSkill(owner, bridgeId, state));
        SessionTransitionResult cleaned = transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.CLEANED, gameTime);
        return cleaned.accepted()
                ? TemporarySkillGrantResult.failure(failure)
                : TemporarySkillGrantResult.failure(failure + "; cleanup state could not be recorded: " + cleaned.message());
    }

    public static Optional<SkillState> replacedSkill(TemporaryStateReference reference) {
        CompoundTag payload = reference.payload();
        if (!payload.contains(REPLACED_SKILL_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return Optional.empty();
        }
        try {
            return SkillSnapshotSerialization.fromTag(payload.getCompound(REPLACED_SKILL_KEY)).skills().stream().findFirst();
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }
}
