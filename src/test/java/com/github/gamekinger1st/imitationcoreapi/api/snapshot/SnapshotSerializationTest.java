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
        source.putString("Variant", "safe");

        CompoundTag sanitized = SnapshotNbtSanitizer.sanitizeEntityData(source);

        assertFalse(sanitized.hasUUID("UUID"));
        assertFalse(sanitized.contains("Brain"));
        assertFalse(sanitized.contains("OwnerUUID"));
        assertFalse(sanitized.contains("Motion"));
        assertFalse(sanitized.contains("Passengers"));
        assertFalse(sanitized.contains("ActiveEffects"));
        assertEquals("safe", sanitized.getString("Variant"));
        assertEquals("targeting data", source.getString("Brain"));
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
