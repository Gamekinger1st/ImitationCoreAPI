package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TensuraStateSnapshotSerializationTest {
    @Test
    void roundTripsBoundedTensuraState() {
        CompoundTag existence = new CompoundTag();
        existence.putDouble("magicule", 500D);
        TensuraStateSnapshot snapshot = new TensuraStateSnapshot(
                ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "tensura_state_reflective"),
                TensuraStateSnapshot.CURRENT_SCHEMA_VERSION,
                new TensuraVitals(1_000D, 500D, 250D, 20D),
                Map.of(TensuraStateSections.EXISTENCE, existence)
        );

        TensuraStateSnapshot loaded = TensuraStateSnapshotSerialization.fromTag(TensuraStateSnapshotSerialization.toTag(snapshot));

        assertEquals(snapshot, loaded);
    }
}
