package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.adapter.AdapterKind;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class DisguiseAppraisalSnapshotCaptureAdapter implements SnapshotCaptureAdapter {
    @Override
    public ResourceLocation id() {
        return DisguiseAppraisalExtensions.ID;
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
        DisguiseAppraisalSnapshot snapshot = new DisguiseAppraisalSnapshot(
                living.getHealth(),
                living.getMaxHealth(),
                living.getArmorValue(),
                ImitationApi.tensuraStates().capture(living).map(state -> state.vitals())
        );
        builder.extension(DisguiseAppraisalExtensions.create(snapshot));
    }
}
