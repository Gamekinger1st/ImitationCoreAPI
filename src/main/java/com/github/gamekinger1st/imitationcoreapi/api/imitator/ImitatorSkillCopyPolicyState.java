package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateStatus;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ImitatorSkillCopyPolicyState {
    public static final ResourceLocation HANDLER_ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "imitator_skill_copy_policy");

    private ImitatorSkillCopyPolicyState() {
    }

    public static TemporaryStateReference create(UUID sessionId, ImitatorSkillCopyPolicy policy) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(policy, "policy");
        return new TemporaryStateReference(
                UUID.randomUUID(),
                sessionId,
                HANDLER_ID,
                TemporaryStateKinds.SKILL_COPY_POLICY,
                policy.toTag(),
                TemporaryStateStatus.ACTIVE
        );
    }

    public static Optional<ImitatorSkillCopyPolicy> find(TransformationSession session) {
        Objects.requireNonNull(session, "session");
        return session.temporaryState().stream()
                .filter(reference -> reference.handlerId().equals(HANDLER_ID))
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.SKILL_COPY_POLICY))
                .filter(reference -> reference.status() == TemporaryStateStatus.PREPARED || reference.status() == TemporaryStateStatus.ACTIVE)
                .findFirst()
                .map(reference -> ImitatorSkillCopyPolicy.fromTag(reference.payload()));
    }
}
