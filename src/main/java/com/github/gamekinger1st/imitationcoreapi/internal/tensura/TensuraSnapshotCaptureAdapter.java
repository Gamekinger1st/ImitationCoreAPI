package com.github.gamekinger1st.imitationcoreapi.internal.tensura;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.adapter.AdapterKind;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureContext;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class TensuraSnapshotCaptureAdapter implements SnapshotCaptureAdapter {
    @Override
    public ResourceLocation id() {
        return TensuraStateExtensions.ID;
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
        if (context.subject() instanceof LivingEntity living) {
            ImitationApi.tensuraStates().capture(living).map(TensuraStateExtensions::create).ifPresent(builder::extension);
        }
    }
}
