package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotSerializationTest {
    @Test
    void preservesIdentityAndBaselineSchemasAcrossRoundTrips() {
        CompoundTag entityData = new CompoundTag();
        entityData.putString("variant", "blue");
        CompoundTag visualData = new CompoundTag();
        visualData.putString("pose", "STANDING");
        SnapshotExtension extension = new SnapshotExtension(ResourceLocation.withDefaultNamespace("example"), 1, new CompoundTag());
        IdentitySnapshot identity = IdentitySnapshot.builder(ResourceLocation.withDefaultNamespace("zombie"), 42L)
                .displayName("Example")
                .entityData(entityData)
                .visualData(visualData)
                .extension(extension)
                .build();
        BaselineSnapshot baseline = new BaselineSnapshot(1, entityData, java.util.List.of(extension));

        IdentitySnapshot loadedIdentity = SnapshotSerialization.identityFromTag(SnapshotSerialization.identityToTag(identity));
        BaselineSnapshot loadedBaseline = SnapshotSerialization.baselineFromTag(SnapshotSerialization.baselineToTag(baseline));

        assertEquals(identity, loadedIdentity);
        assertEquals(baseline, loadedBaseline);
    }

    @Test
    void blueprintSanitizationRemovesTransientRuntimeState() {
        CompoundTag source = new CompoundTag();
        source.putUUID("UUID", java.util.UUID.randomUUID());
        source.putString("Brain", "targeting data");
        source.putString("OwnerUUID", java.util.UUID.randomUUID().toString());
        source.putString("Motion", "moving");
        source.putString("Passengers", "mounted");
        source.putString("ActiveEffects", "temporary effects");
        source.putString("active_effects", "modern temporary effects");
        source.putInt("HurtTime", 8);
        source.putInt("DeathTime", 2);
        source.putFloat("Health", 5F);
        source.putBoolean("NoAI", true);
        source.putBoolean("PersistenceRequired", true);
        source.putString("Pose", "SLEEPING");
        source.putString("equipment", "real item state");
        source.putString("Variant", "safe");

        CompoundTag sanitized = SnapshotNbtSanitizer.sanitizeEntityData(source);

        assertFalse(sanitized.hasUUID("UUID"));
        assertFalse(sanitized.contains("Brain"));
        assertFalse(sanitized.contains("OwnerUUID"));
        assertFalse(sanitized.contains("Motion"));
        assertFalse(sanitized.contains("Passengers"));
        assertFalse(sanitized.contains("ActiveEffects"));
        assertFalse(sanitized.contains("active_effects"));
        assertFalse(sanitized.contains("HurtTime"));
        assertFalse(sanitized.contains("DeathTime"));
        assertFalse(sanitized.contains("Health"));
        assertFalse(sanitized.contains("NoAI"));
        assertFalse(sanitized.contains("PersistenceRequired"));
        assertFalse(sanitized.contains("Pose"));
        assertFalse(sanitized.contains("equipment"));
        assertEquals("safe", sanitized.getString("Variant"));
        assertEquals("targeting data", source.getString("Brain"));
    }

    @Test
    void playerBlueprintSanitizationKeepsOnlyRenderSafeEntityData() {
        CompoundTag source = new CompoundTag();
        source.putString("CustomName", "Copied Player");
        source.putBoolean("CustomNameVisible", true);
        source.putBoolean("Glowing", true);
        source.putString("Inventory", "oversized inventory data");
        source.putString("recipeBook", "oversized recipe data");
        source.putString("abilities", "player abilities");
        source.putString("EnderItems", "ender chest data");
        source.putString("SelectedItem", "held item data");

        CompoundTag sanitized = SnapshotNbtSanitizer.sanitizePlayerEntityData(source);

        assertEquals("Copied Player", sanitized.getString("CustomName"));
        assertEquals(true, sanitized.getBoolean("CustomNameVisible"));
        assertEquals(true, sanitized.getBoolean("Glowing"));
        assertFalse(sanitized.contains("Inventory"));
        assertFalse(sanitized.contains("recipeBook"));
        assertFalse(sanitized.contains("abilities"));
        assertFalse(sanitized.contains("EnderItems"));
        assertFalse(sanitized.contains("SelectedItem"));
        assertEquals("oversized inventory data", source.getString("Inventory"));
    }

    @Test
    void rejectsMalformedStoredIdentityData() {
        CompoundTag missingSchema = new CompoundTag();
        assertThrows(IllegalArgumentException.class, () -> SnapshotSerialization.identityFromTag(missingSchema));

        CompoundTag invalidResource = new CompoundTag();
        invalidResource.putUUID("id", java.util.UUID.randomUUID());
        invalidResource.putInt("schema", IdentitySnapshot.CURRENT_SCHEMA_VERSION);
        invalidResource.putString("entity_type", "not a valid id");
        invalidResource.putString("display_name", "Bad");
        invalidResource.put("entity_data", new CompoundTag());
        invalidResource.put("visual_data", new CompoundTag());
        assertThrows(IllegalArgumentException.class, () -> SnapshotSerialization.identityFromTag(invalidResource));
    }
}
