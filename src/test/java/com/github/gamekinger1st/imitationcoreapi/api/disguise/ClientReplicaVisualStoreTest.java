package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ClientReplicaVisualStoreTest {
    @Test
    void storesDefensiveCopiesAndBoundsReplicaCount() {
        ClientReplicaVisualStore store = new ClientReplicaVisualStore();
        CompoundTag equipment = new CompoundTag();
        equipment.putString("head", "original");
        store.put(0, equipment);
        equipment.putString("head", "changed");
        assertEquals("original", store.get(0).orElseThrow().getString("head"));

        for (int entityId = 1; entityId <= ClientReplicaVisualStore.MAX_REPLICAS; entityId++) {
            store.put(entityId, new CompoundTag());
        }

        assertFalse(store.get(0).isPresent());
    }
}
