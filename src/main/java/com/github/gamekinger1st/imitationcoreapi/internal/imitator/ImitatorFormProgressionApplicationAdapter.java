package com.github.gamekinger1st.imitationcoreapi.internal.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormProgressionState;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormProgressionService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateOperationResult;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Optional;

public final class ImitatorFormProgressionApplicationAdapter implements TransformationApplicationAdapter {
    @Override
    public ResourceLocation id() {
        return ImitatorFormProgressionService.HANDLER_ID;
    }

    @Override
    public int priority() {
        return 200;
    }

    @Override
    public boolean appliesTo(TransformationScope scope) {
        return scope.changesOwnerPresentation();
    }

    @Override
    public Optional<String> validate(TransformationApplicationContext context) {
        return Optional.empty();
    }

    @Override
    public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
        return List.of(new TemporaryStateDefinition(TemporaryStateKinds.FORM_PROGRESSION, ImitatorFormProgressionState.empty(context.session().snapshotId()).toTag()));
    }

    @Override
    public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
    }

    @Override
    public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        if (!context.session().scope().appliesGameplayState()) {
            return;
        }
        var owner = context.owner().orElseThrow(() -> new IllegalStateException("Form progression reversion requires the owner to be online"));
        for (TemporaryStateReference reference : temporaryState) {
            if (!reference.kind().equals(TemporaryStateKinds.FORM_PROGRESSION)) {
                continue;
            }
            ImitatorFormProgressionState state = ImitatorFormProgressionState.fromTag(reference.payload());
            state.accumulatedDelta().tensuraVitals().ifPresent(delta -> {
                TensuraStateOperationResult result = ImitationApi.tensuraStates().addVitalsDelta(owner, delta);
                if (!result.successful()) {
                    throw new IllegalStateException(result.detail());
                }
            });
            state.accumulatedDelta().attributeBaseValues().forEach((id, delta) -> {
                if (id.equals(ResourceLocation.fromNamespaceAndPath("tensura", "max_magicule"))
                        || id.equals(ResourceLocation.fromNamespaceAndPath("tensura", "max_aura"))) {
                    return;
                }
                BuiltInRegistries.ATTRIBUTE.getHolder(id).map(owner::getAttribute).ifPresent(instance -> instance.setBaseValue(instance.getBaseValue() + delta));
            });
        }
    }
}
