package com.github.gamekinger1st.imitationcoreapi.internal.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.adapter.AdapterKind;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.PlayerDisguiseProfile;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.PlayerDisguiseProfileExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureAdapter;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotCaptureContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class PlayerProfileSnapshotCaptureAdapter implements SnapshotCaptureAdapter {
    @Override
    public ResourceLocation id() {
        return PlayerDisguiseProfileExtensions.ID;
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
        if (context.subject() instanceof Player player) {
            PlayerDisguiseProfile.from(player.getGameProfile())
                    .map(PlayerDisguiseProfileExtensions::create)
                    .ifPresent(builder::extension);
        }
    }
}
