package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImitatorFormStatsTest {
    @Test
    void appliesPositiveDeltasToAppraisalAndTensuraVitals() {
        ImitatorFormStats stats = ImitatorFormStats.fromAppraisal(new DisguiseAppraisalSnapshot(10F, 20F, 2, Optional.of(new TensuraVitals(100D, 40D, 60D, 12D))));
        ImitatorFormStatDelta delta = new ImitatorFormStatDelta(1F, 3F, 4, Optional.of(new TensuraVitals(25D, 10D, 15D, 2D)));

        DisguiseAppraisalSnapshot updated = stats.apply(delta).appraisal().orElseThrow();

        assertEquals(11F, updated.health());
        assertEquals(23F, updated.maxHealth());
        assertEquals(6, updated.armorValue());
        assertEquals(new TensuraVitals(125D, 50D, 75D, 14D), updated.tensuraVitals().orElseThrow());
    }

    @Test
    void mergesUpdatedAppraisalAndVitalsIntoStoredSnapshot() {
        ResourceLocation bridge = ResourceLocation.fromNamespaceAndPath("test", "bridge");
        CompoundTag race = new CompoundTag();
        race.putString("value", "preserved");
        IdentitySnapshot snapshot = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 4L)
                .snapshotId(java.util.UUID.randomUUID())
                .displayName("Zombie")
                .entityData(new CompoundTag())
                .visualData(new CompoundTag())
                .extension(DisguiseAppraisalExtensions.create(new DisguiseAppraisalSnapshot(10F, 20F, 2, Optional.of(new TensuraVitals(100D, 40D, 60D, 12D)))))
                .extension(TensuraStateExtensions.create(new TensuraStateSnapshot(bridge, TensuraStateSnapshot.CURRENT_SCHEMA_VERSION, new TensuraVitals(100D, 40D, 60D, 12D), Map.of(ResourceLocation.fromNamespaceAndPath("test", "race"), race))))
                .build();
        ImitatorFormStats stats = ImitatorFormStats.fromAppraisal(new DisguiseAppraisalSnapshot(12F, 24F, 5, Optional.of(new TensuraVitals(140D, 55D, 85D, 15D))));

        IdentitySnapshot updated = stats.mergeInto(snapshot);

        assertEquals(new TensuraVitals(140D, 55D, 85D, 15D), DisguiseAppraisalExtensions.find(updated.extensions()).orElseThrow().tensuraVitals().orElseThrow());
        TensuraStateSnapshot tensura = TensuraStateExtensions.find(updated.extensions()).orElseThrow();
        assertEquals(new TensuraVitals(140D, 55D, 85D, 15D), tensura.vitals());
        assertEquals("preserved", tensura.sections().get(ResourceLocation.fromNamespaceAndPath("test", "race")).getString("value"));
    }
}
