package com.github.gamekinger1st.imitationcoreapi.internal.skill;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.adapter.AdapterKind;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorCopiedSkill;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillCopyExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillCopySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ImitatorSkillCopySnapshotCaptureAdapter implements SnapshotCaptureAdapter {
    @Override
    public ResourceLocation id() {
        return ImitatorSkillCopyExtensions.ID;
    }

    @Override
    public AdapterKind kind() {
        return AdapterKind.SNAPSHOT;
    }

    @Override
    public CompatibilityAssessment assess(IdentitySnapshot snapshot) {
        return CompatibilityAssessment.full();
    }

    @Override
    public void capture(SnapshotCaptureContext context, IdentitySnapshot.Builder builder) {
        if (!(context.subject() instanceof LivingEntity living)) {
            return;
        }
        ImitationApi.skillBridges().capture(living)
                .map(ImitatorSkillCopySnapshotCaptureAdapter::copySnapshot)
                .map(ImitatorSkillCopyExtensions::create)
                .ifPresent(builder::extension);
    }

    private static ImitatorSkillCopySnapshot copySnapshot(SkillSnapshot snapshot) {
        Map<ResourceLocation, Double> masteryBySkill = new LinkedHashMap<>();
        snapshot.skills().stream()
                .filter(skill -> !skill.temporary())
                .forEach(skill -> masteryBySkill.merge(skill.skillId(), skill.mastery(), Math::max));
        return new ImitatorSkillCopySnapshot(
                snapshot.bridgeId(),
                ImitatorSkillCopySnapshot.CURRENT_SCHEMA_VERSION,
                masteryBySkill.entrySet().stream()
                        .map(entry -> new ImitatorCopiedSkill(entry.getKey(), entry.getValue()))
                        .sorted(Comparator.comparing(skill -> skill.skillId().toString()))
                        .limit(ImitatorSkillCopySnapshot.MAX_SKILLS)
                        .toList()
        );
    }
}
