package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigration;
import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigrationRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SnapshotSerialization {
    private static final SchemaMigrationRegistry IDENTITY_MIGRATIONS = new SchemaMigrationRegistry();
    private static final SchemaMigrationRegistry BASELINE_MIGRATIONS = new SchemaMigrationRegistry();

    private SnapshotSerialization() {
    }

    public static CompoundTag identityToTag(IdentitySnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", snapshot.snapshotId());
        tag.putInt("schema", snapshot.schemaVersion());
        tag.putString("entity_type", snapshot.entityType().toString());
        tag.putString("display_name", snapshot.displayName());
        tag.put("entity_data", snapshot.entityData());
        tag.put("visual_data", snapshot.visualData());
        tag.put("extensions", extensionsToTag(snapshot.extensions()));
        tag.putLong("captured_game_time", snapshot.capturedGameTime());
        return tag;
    }

    public static IdentitySnapshot identityFromTag(CompoundTag tag) {
        tag = migrate(tag, "schema", IdentitySnapshot.CURRENT_SCHEMA_VERSION, IDENTITY_MIGRATIONS, "identity snapshot");
        UUID id = requireUuid(tag, "id");
        int schemaVersion = requireSchema(tag, "schema", IdentitySnapshot.CURRENT_SCHEMA_VERSION, "identity snapshot");
        ResourceLocation entityType = requireResourceLocation(tag, "entity_type");
        String displayName = tag.getString("display_name");
        CompoundTag entityData = requireCompound(tag, "entity_data");
        CompoundTag visualData = requireCompound(tag, "visual_data");
        List<SnapshotExtension> extensions = extensionsFromTag(tag.getList("extensions", Tag.TAG_COMPOUND));
        long capturedGameTime = tag.getLong("captured_game_time");
        return new IdentitySnapshot(id, schemaVersion, entityType, displayName, entityData, visualData, extensions, capturedGameTime);
    }

    public static CompoundTag baselineToTag(BaselineSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", snapshot.schemaVersion());
        tag.put("player_data", snapshot.playerData());
        tag.put("extensions", extensionsToTag(snapshot.extensions()));
        return tag;
    }

    public static BaselineSnapshot baselineFromTag(CompoundTag tag) {
        tag = migrate(tag, "schema", BaselineSnapshot.CURRENT_SCHEMA_VERSION, BASELINE_MIGRATIONS, "baseline snapshot");
        int schemaVersion = requireSchema(tag, "schema", BaselineSnapshot.CURRENT_SCHEMA_VERSION, "baseline snapshot");
        CompoundTag playerData = requireCompound(tag, "player_data");
        List<SnapshotExtension> extensions = extensionsFromTag(tag.getList("extensions", Tag.TAG_COMPOUND));
        return new BaselineSnapshot(schemaVersion, playerData, extensions);
    }

    public static void registerIdentityMigration(int sourceVersion, SchemaMigration migration) {
        IDENTITY_MIGRATIONS.register(sourceVersion, migration);
    }

    public static void registerBaselineMigration(int sourceVersion, SchemaMigration migration) {
        BASELINE_MIGRATIONS.register(sourceVersion, migration);
    }

    public static CompoundTag extensionToTag(SnapshotExtension extension) {
        CompoundTag tag = new CompoundTag();
        tag.putString("adapter", extension.adapterId().toString());
        tag.putInt("schema", extension.schemaVersion());
        tag.put("payload", extension.payload());
        return tag;
    }

    public static SnapshotExtension extensionFromTag(CompoundTag tag) {
        ResourceLocation adapterId = requireResourceLocation(tag, "adapter");
        int schemaVersion = requireSchema(tag, "schema", Integer.MAX_VALUE, "snapshot extension");
        CompoundTag payload = requireCompound(tag, "payload");
        return new SnapshotExtension(adapterId, schemaVersion, payload);
    }

    private static ListTag extensionsToTag(List<SnapshotExtension> extensions) {
        ListTag tags = new ListTag();
        for (SnapshotExtension extension : extensions) {
            tags.add(extensionToTag(extension));
        }
        return tags;
    }

    private static List<SnapshotExtension> extensionsFromTag(ListTag tags) {
        List<SnapshotExtension> extensions = new ArrayList<>(tags.size());
        for (int index = 0; index < tags.size(); index++) {
            extensions.add(extensionFromTag(tags.getCompound(index)));
        }
        return extensions;
    }

    private static UUID requireUuid(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) {
            throw new IllegalArgumentException("Missing UUID field: " + key);
        }
        return tag.getUUID(key);
    }

    private static CompoundTag migrate(CompoundTag source, String schemaKey, int currentVersion, SchemaMigrationRegistry migrations, String subject) {
        if (!source.contains(schemaKey, Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing schema field for " + subject);
        }
        int sourceVersion = source.getInt(schemaKey);
        if (sourceVersion < 1 || sourceVersion > currentVersion) {
            throw new IllegalArgumentException("Unsupported " + subject + " schema version: " + sourceVersion);
        }
        if (sourceVersion == currentVersion) {
            return source.copy();
        }
        CompoundTag migrated = migrations.migrate(source, sourceVersion, currentVersion);
        migrated.putInt(schemaKey, currentVersion);
        return migrated;
    }

    private static int requireSchema(CompoundTag tag, String key, int maximum, String subject) {
        if (!tag.contains(key, Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing schema field for " + subject);
        }
        int schema = tag.getInt(key);
        if (schema < 1 || schema > maximum) {
            throw new IllegalArgumentException("Unsupported " + subject + " schema version: " + schema);
        }
        return schema;
    }

    private static ResourceLocation requireResourceLocation(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing resource location field: " + key);
        }
        ResourceLocation value = ResourceLocation.tryParse(tag.getString(key));
        if (value == null) {
            throw new IllegalArgumentException("Invalid resource location field: " + key);
        }
        return value;
    }

    private static CompoundTag requireCompound(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Missing compound field: " + key);
        }
        return tag.getCompound(key);
    }
}
