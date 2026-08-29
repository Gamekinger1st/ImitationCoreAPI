package com.github.gamekinger1st.imitationcoreapi.internal.physical;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhysicalFormApplicationAdapterTest {
    private static final ResourceLocation MOVEMENT_SPEED = ResourceLocation.withDefaultNamespace("generic.movement_speed");
    private static final ResourceLocation JUMP_STRENGTH = ResourceLocation.withDefaultNamespace("generic.jump_strength");

    @Test
    void convertsMobMovementSpeedToPlayerMovementScale() {
        assertEquals(0.15D, PhysicalFormApplicationAdapter.adaptedAttributeBase(MOVEMENT_SPEED, 0.3D, false), 0.0000001D);
    }

    @Test
    void convertsMobJumpStrengthWithoutDroppingBelowPlayerDefault() {
        assertEquals(0.6D, PhysicalFormApplicationAdapter.adaptedAttributeBase(JUMP_STRENGTH, 1.0D, false), 0.0000001D);
        assertEquals(0.42D, PhysicalFormApplicationAdapter.adaptedAttributeBase(JUMP_STRENGTH, 0.42D, false), 0.0000001D);
    }

    @Test
    void leavesPlayerLocomotionOnPlayerScale() {
        assertEquals(0.3D, PhysicalFormApplicationAdapter.adaptedAttributeBase(MOVEMENT_SPEED, 0.3D, true), 0.0000001D);
        assertEquals(1.0D, PhysicalFormApplicationAdapter.adaptedAttributeBase(JUMP_STRENGTH, 1.0D, true), 0.0000001D);
    }

    @Test
    void prefersCapturedEffectiveAttributeValuesAndSupportsLegacySnapshots() {
        CompoundTag current = new CompoundTag();
        current.putDouble("base", 0.2D);
        current.putDouble("value", 0.35D);
        CompoundTag legacy = new CompoundTag();
        legacy.putDouble("base", 0.2D);

        assertEquals(0.35D, PhysicalFormApplicationAdapter.copiedAttributeValue(current), 0.0000001D);
        assertEquals(0.2D, PhysicalFormApplicationAdapter.copiedAttributeValue(legacy), 0.0000001D);
    }
}
