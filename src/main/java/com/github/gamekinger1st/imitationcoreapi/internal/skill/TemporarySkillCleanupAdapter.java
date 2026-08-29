package com.github.gamekinger1st.imitationcoreapi.internal.skill;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillOperationResult;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillOwnership;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Optional;

public final class TemporarySkillCleanupAdapter implements TransformationApplicationAdapter {
    @Override
    public ResourceLocation id() {
        return TemporarySkillService.HANDLER_ID;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public Optional<String> validate(TransformationApplicationContext context) {
        return Optional.empty();
    }

    @Override
    public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
        return List.of();
    }

    @Override
    public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
    }

    @Override
    public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        List<TemporaryStateReference> borrowedSkills = temporaryState.stream()
                .filter(reference -> reference.kind().equals(TemporaryStateKinds.BORROWED_SKILL))
                .toList();
        if (borrowedSkills.isEmpty()) {
            return;
        }
        LivingEntity owner = context.owner().orElseThrow(() -> new IllegalStateException("Temporary skill cleanup requires the session owner to be online"));
        for (TemporaryStateReference reference : borrowedSkills) {
            ResourceLocation skillId = ResourceLocation.tryParse(reference.payload().getString(TemporarySkillService.SKILL_ID_KEY));
            if (skillId == null) {
                throw new IllegalStateException("Temporary skill cleanup reference has an invalid skill id");
            }
            ResourceLocation bridgeId = ResourceLocation.tryParse(reference.payload().getString(TemporarySkillService.BRIDGE_ID_KEY));
            SkillOperationResult result = bridgeId == null
                    ? ImitationApi.skillBridges().revokeTemporary(owner, skillId, new TemporarySkillOwnership(reference.sessionId(), reference.referenceId()))
                    : ImitationApi.skillBridges().revokeTemporary(owner, bridgeId, skillId, new TemporarySkillOwnership(reference.sessionId(), reference.referenceId()));
            if (!result.successful()) {
                throw new IllegalStateException(result.detail());
            }
            Optional<com.github.gamekinger1st.imitationcoreapi.api.skill.SkillState> replaced = TemporarySkillService.replacedSkill(reference);
            if (replaced.isPresent()) {
                if (bridgeId == null) {
                    throw new IllegalStateException("Temporary skill replacement is missing its bridge id");
                }
                SkillOperationResult restored = ImitationApi.skillBridges().restoreSkill(owner, bridgeId, replaced.get());
                if (!restored.successful()) {
                    throw new IllegalStateException(restored.detail());
                }
            }
        }
    }
}
