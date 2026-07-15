package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImitatorReplicaPolicyTest {
    @Test
    void serializesReplicaPolicyForSessionUse() {
        ImitatorReplicaPolicy policy = new ImitatorReplicaPolicy(1_200, 2.5D, 48D, false, true, false, true, false, true, false, "Echo ");

        ImitatorReplicaPolicy loaded = ImitatorReplicaPolicy.fromTag(policy.toTag());

        assertEquals(policy, loaded);
    }

    @Test
    void usesDefaultsForOlderReplicaSessions() {
        assertEquals(ImitatorReplicaPolicy.DEFAULT, ImitatorReplicaPolicy.fromTag(new CompoundTag()));
    }

    @Test
    void rejectsUnsafeReplicaBounds() {
        assertThrows(IllegalArgumentException.class, () -> new ImitatorReplicaPolicy(0, 3D, 96D, true, true, true, true, true, true, true, "Replica"));
        assertThrows(IllegalArgumentException.class, () -> new ImitatorReplicaPolicy(20, 33D, 96D, true, true, true, true, true, true, true, "Replica"));
        assertThrows(IllegalArgumentException.class, () -> new ImitatorReplicaPolicy(20, 3D, 0D, true, true, true, true, true, true, true, "Replica"));
    }
}
