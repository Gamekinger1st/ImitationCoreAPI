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
        SkillOperationResult granted = ImitationApi.skillBridges().grantTemporary(owner, bridgeId.get(), skillId, removeTime, new TemporarySkillOwnership(sessionId, referenceId));
        if (!granted.successful()) {
            transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.CLEANED, gameTime);
            return TemporarySkillGrantResult.failure(granted.detail());
        }
        if (mastery.isPresent()) {
            SkillOperationResult configured = ImitationApi.skillBridges().update(owner, bridgeId.get(), new SkillUpdateRequest(skillId, mastery, Optional.of(false), Optional.empty(), Optional.empty()));
            if (!configured.successful()) {
                SkillOperationResult revoked = ImitationApi.skillBridges().revokeTemporary(owner, bridgeId.get(), skillId, new TemporarySkillOwnership(sessionId, referenceId));
                if (revoked.successful()) {
                    transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.CLEANED, gameTime);
                }
                return TemporarySkillGrantResult.failure("The temporary skill could not be configured: " + configured.detail());
            }
        }
        SessionTransitionResult activated = transformations.updateTemporaryState(sessionId, referenceId, TemporaryStateStatus.ACTIVE, gameTime);
        return activated.accepted() ? TemporarySkillGrantResult.success(referenceId) : TemporarySkillGrantResult.failure(activated.message());
    }
}
