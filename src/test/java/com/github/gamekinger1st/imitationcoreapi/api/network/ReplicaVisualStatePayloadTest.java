package com.github.gamekinger1st.imitationcoreapi.api.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReplicaVisualStatePayloadTest {
    @Test
    void validatesEntityIdAndCopiesEquipment() {
        CompoundTag equipment = new CompoundTag();
        equipment.putString("head", "minecraft:diamond_helmet");
        ReplicaVisualStatePayload payload = new ReplicaVisualStatePayload(42, equipment);
        equipment.putString("head", "minecraft:air");

        assertEquals("minecraft:diamond_helmet", payload.equipment().getString("head"));
        assertThrows(IllegalArgumentException.class, () -> new ReplicaVisualStatePayload(-1, new CompoundTag()));
    }
}
