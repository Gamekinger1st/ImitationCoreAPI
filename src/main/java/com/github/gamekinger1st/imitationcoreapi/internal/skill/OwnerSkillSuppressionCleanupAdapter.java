package com.github.gamekinger1st.imitationcoreapi.internal.skill;

import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.skill.OwnerSkillSuppressionService;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class OwnerSkillSuppressionCleanupAdapter implements TransformationApplicationAdapter {
    @Override
    public ResourceLocation id() {
        return OwnerSkillSuppressionService.HANDLER_ID;
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public boolean appliesTo(TransformationScope scope) {
        return true;
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
    }
}
