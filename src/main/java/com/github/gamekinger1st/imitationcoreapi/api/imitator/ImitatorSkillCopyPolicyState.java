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
    private static final String ACCESS_KEY = "copy_access";
    private static final String MASTERED_KEY = "mastered";

    private ImitatorSkillCopyPolicyState() {
    }

    public static TemporaryStateReference create(UUID sessionId, ImitatorSkillCopyPolicy policy) {
        return create(sessionId, policy, ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP, false);
    }

    public static TemporaryStateReference create(UUID sessionId, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access, boolean mastered) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        net.minecraft.nbt.CompoundTag payload = policy.toTag();
        payload.putString(ACCESS_KEY, access.name());
        payload.putBoolean(MASTERED_KEY, mastered);
        return new TemporaryStateReference(
                UUID.randomUUID(),
                sessionId,
                HANDLER_ID,
                TemporaryStateKinds.SKILL_COPY_POLICY,
                payload,
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

    public static ImitatorSkillCopyAccess access(TransformationSession session) {
        return reference(session)
                .map(TemporaryStateReference::payload)
                .map(payload -> parseAccess(payload.getString(ACCESS_KEY)))
                .orElse(ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP);
    }

    public static boolean mastered(TransformationSession session) {
        return reference(session).map(TemporaryStateReference::payload).map(payload -> payload.getBoolean(MASTERED_KEY)).orElse(false);
    }

    private static Optional<TemporaryStateReference> reference(TransformationSession session) {
        Objects.requireNonNull(session, "session");
        return session.temporaryState().stream()
                .filter(reference -> reference.handlerId().equals(HANDLER_ID))
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.SKILL_COPY_POLICY))
                .filter(reference -> reference.status() == TemporaryStateStatus.PREPARED || reference.status() == TemporaryStateStatus.ACTIVE)
                .findFirst();
    }

    private static ImitatorSkillCopyAccess parseAccess(String value) {
        try {
            return ImitatorSkillCopyAccess.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return ImitatorSkillCopyAccess.INFERIOR_OR_EQUAL_EP;
        }
    }
}
