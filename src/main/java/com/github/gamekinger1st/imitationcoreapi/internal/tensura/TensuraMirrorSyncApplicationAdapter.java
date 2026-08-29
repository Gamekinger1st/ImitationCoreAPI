package com.github.gamekinger1st.imitationcoreapi.internal.tensura;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.application.TemporaryStateDefinition;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationContext;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationReversionContext;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateKinds;
import com.github.gamekinger1st.imitationcoreapi.api.session.TemporaryStateReference;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateOperationResult;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.internal.physical.PhysicalFormApplicationAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

public final class TensuraMirrorSyncApplicationAdapter implements TransformationApplicationAdapter {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "tensura_mirror_sync");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public Optional<String> validate(TransformationApplicationContext context) {
        if (!perfectForm(context)) {
            return Optional.empty();
        }
        Optional<TensuraStateSnapshot> target = TensuraStateExtensions.find(context.snapshot().extensions());
        if (target.isEmpty()) {
            return Optional.empty();
        }
        return TensuraStateExtensions.find(context.session().baseline().extensions()).isPresent()
                ? Optional.empty()
                : Optional.of("Perfect Form cannot restore the owner's Tensura baseline");
    }

    @Override
    public List<TemporaryStateDefinition> prepare(TransformationApplicationContext context) {
        if (!perfectForm(context)) {
            return List.of();
        }
        Optional<TensuraStateSnapshot> target = TensuraStateExtensions.find(context.snapshot().extensions());
        if (target.isEmpty()) {
            return List.of();
        }
        CompoundTag payload = new CompoundTag();
        payload.putString("bridge", target.get().bridgeId().toString());
        payload.putDouble("scale", perfectFormScale(context));
        return List.of(new TemporaryStateDefinition(TemporaryStateKinds.STAT, payload));
    }

    @Override
    public void apply(TransformationApplicationContext context, List<TemporaryStateReference> temporaryState) {
        if (temporaryState.stream().noneMatch(reference -> reference.kind().equals(TemporaryStateKinds.STAT))) {
            return;
        }
        TensuraStateSnapshot target = TensuraStateExtensions.find(context.snapshot().extensions())
                .orElseThrow(() -> new IllegalStateException("Perfect Form target state is unavailable"));
        TensuraStateOperationResult result = ImitationApi.tensuraStates().restoreScaled(context.owner(), target, perfectFormScale(context));
        if (!result.successful()) {
            throw new IllegalStateException(result.detail());
        }
        if (context.snapshot().entityType().equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("player"))) {
            PhysicalFormApplicationAdapter.reapplyCopiedHealth(context.owner(), context.snapshot().visualData(), perfectFormScale(context));
        } else {
            PhysicalFormApplicationAdapter.reapplyMobLocomotion(context.owner(), context.snapshot().visualData(), perfectFormScale(context));
        }
    }

    private static boolean perfectForm(TransformationApplicationContext context) {
        return context.session().baseline().playerData().getBoolean(ImitatorHandlerService.PERFECT_FORM_BASELINE_KEY);
    }

    private static double perfectFormScale(TransformationApplicationContext context) {
        return context.session().baseline().playerData().getDouble(ImitatorHandlerService.PERFECT_FORM_SCALE_BASELINE_KEY);
    }

    @Override
    public void revert(TransformationReversionContext context, List<TemporaryStateReference> temporaryState) {
        if (temporaryState.stream().noneMatch(reference -> reference.kind().equals(TemporaryStateKinds.STAT))) {
            return;
        }
        TensuraStateSnapshot baseline = TensuraStateExtensions.find(context.session().baseline().extensions())
                .orElseThrow(() -> new IllegalStateException("Perfect Form baseline state is unavailable"));
        var owner = context.owner().orElseThrow(() -> new IllegalStateException("Perfect Form reversion requires the owner to be online"));
        TensuraStateOperationResult result = ImitationApi.tensuraStates().restore(owner, baseline);
        if (!result.successful()) {
            throw new IllegalStateException(result.detail());
        }
    }
}
