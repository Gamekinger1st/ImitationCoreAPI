package com.github.gamekinger1st.imitationcoreapi.api.tensura;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TensuraStateExtensionsTest {
    @Test
    void roundTripsAStateSnapshotThroughThePortableExtension() {
        CompoundTag race = new CompoundTag();
        race.putString("race", "slime");
        TensuraStateSnapshot state = new TensuraStateSnapshot(
                ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "test_tensura"),
                TensuraStateSnapshot.CURRENT_SCHEMA_VERSION,
                new TensuraVitals(100D, 50D, 40D, 20D),
                Map.of(TensuraStateSections.RACE, race)
        );

        SnapshotExtension extension = TensuraStateExtensions.create(state);

        assertEquals(state, TensuraStateExtensions.find(List.of(extension)).orElseThrow());
    }

    @Test
    void ignoresUnknownExtensions() {
        SnapshotExtension extension = new SnapshotExtension(ResourceLocation.withDefaultNamespace("unknown"), 1, new CompoundTag());

        assertTrue(TensuraStateExtensions.find(List.of(extension)).isEmpty());
    }

    @Test
    void ignoresMalformedPerfectFormStateExtensions() {
        CompoundTag malformed = new CompoundTag();
        malformed.putString("bridge", "not a valid id");
        malformed.putInt("schema", TensuraStateSnapshot.CURRENT_SCHEMA_VERSION);
        SnapshotExtension extension = new SnapshotExtension(TensuraStateExtensions.ID, TensuraStateExtensions.SCHEMA_VERSION, malformed);

        assertTrue(TensuraStateExtensions.find(List.of(extension)).isEmpty());
    }

    @Test
    void baselineAndTargetStateUseTheSamePortablePerfectFormContract() {
        CompoundTag baselineRace = new CompoundTag();
        baselineRace.putString("race", "human");
        TensuraStateSnapshot baseline = new TensuraStateSnapshot(
                ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "test_tensura"),
                TensuraStateSnapshot.CURRENT_SCHEMA_VERSION,
                new TensuraVitals(200D, 100D, 80D, 30D),
                Map.of(TensuraStateSections.RACE, baselineRace)
        );
        CompoundTag targetRace = new CompoundTag();
        targetRace.putString("race", "direwolf");
        TensuraStateSnapshot target = new TensuraStateSnapshot(
                ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "test_tensura"),
                TensuraStateSnapshot.CURRENT_SCHEMA_VERSION,
                new TensuraVitals(300D, 150D, 120D, 35D),
                Map.of(TensuraStateSections.RACE, targetRace)
        );

        assertEquals(baseline, TensuraStateExtensions.find(List.of(TensuraStateExtensions.create(baseline))).orElseThrow());
        assertEquals(target, TensuraStateExtensions.find(List.of(TensuraStateExtensions.create(target))).orElseThrow());
    }
}
