package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkillSnapshotSerializationTest {
    @Test
    void roundTripsSkillStateForPersistentBridgeUse() {
        CompoundTag data = new CompoundTag();
        data.putString("source", "test");
        SkillSnapshot snapshot = new SkillSnapshot(
                ResourceLocation.fromNamespaceAndPath("imitationcoreapi", "manas_tensura_reflective"),
                1,
                List.of(new SkillState(ResourceLocation.withDefaultNamespace("strength"), data, 12.5D, true, List.of(4, 8), true))
        );

        SkillSnapshot loaded = SkillSnapshotSerialization.fromTag(SkillSnapshotSerialization.toTag(snapshot));

        assertEquals(snapshot, loaded);
    }
}
