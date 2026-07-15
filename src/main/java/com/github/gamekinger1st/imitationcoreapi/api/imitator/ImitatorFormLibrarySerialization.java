package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigration;
import com.github.gamekinger1st.imitationcoreapi.api.persistence.SchemaMigrationRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class ImitatorFormLibrarySerialization {
    private static final SchemaMigrationRegistry MIGRATIONS = new SchemaMigrationRegistry();

    static {
        MIGRATIONS.register(1, source -> {
            source.putString("skill_mode", ImitatorSkillMode.RECORD.name());
            source.putBoolean("mirror_sync_enabled", false);
            return source;
        });
        MIGRATIONS.register(2, source -> {
            if (source.contains("pending_record", Tag.TAG_COMPOUND)) {
                CompoundTag pending = source.getCompound("pending_record");
                pending.putDouble("precision", ImitatorProgressionPolicy.DEFAULT.minimumPrecision());
                pending.putBoolean("mirror_sync_allowed", false);
                source.put("pending_record", pending);
            }
            return source;
        });
        MIGRATIONS.register(3, source -> {
            ListTag forms = source.getList("forms", Tag.TAG_COMPOUND);
            for (int index = 0; index < forms.size(); index++) {
                forms.getCompound(index).putBoolean("skill_copy_allowed", false);
            }
            if (source.contains("pending_record", Tag.TAG_COMPOUND)) {
                CompoundTag pending = source.getCompound("pending_record");
                pending.putBoolean("skill_copy_allowed", false);
                source.put("pending_record", pending);
            }
            source.put("forms", forms);
            return source;
        });
        MIGRATIONS.register(4, source -> source);
    }

    private ImitatorFormLibrarySerialization() {
    }

    public static CompoundTag toTag(ImitatorFormLibraryState library) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", library.schemaVersion());
        ListTag forms = new ListTag();
        library.forms().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            CompoundTag form = new CompoundTag();
            form.putInt("slot", entry.getKey());
            form.putUUID("snapshot_id", entry.getValue().snapshotId());
            form.putDouble("precision", entry.getValue().precision());
            form.putBoolean("perfect", entry.getValue().perfect());
            form.putBoolean("mirror_sync_allowed", entry.getValue().mirrorSyncAllowed());
            form.putBoolean("skill_copy_allowed", entry.getValue().skillCopyAllowed());
            if (!entry.getValue().stats().isEmpty()) {
                form.put("stats", entry.getValue().stats().toTag());
            }
            forms.add(form);
        });
        tag.put("forms", forms);
        if (library.selectedSlot().isPresent()) {
            tag.putInt("selected_slot", library.selectedSlot().getAsInt());
        }
        library.pendingRecord().ifPresent(record -> {
            CompoundTag pending = new CompoundTag();
            pending.putUUID("snapshot_id", record.snapshotId());
            pending.putLong("created_game_time", record.createdGameTime());
            pending.putLong("expires_game_time", record.expiresGameTime());
            pending.putDouble("precision", record.precision());
            pending.putBoolean("mirror_sync_allowed", record.mirrorSyncAllowed());
            pending.putBoolean("skill_copy_allowed", record.skillCopyAllowed());
            tag.put("pending_record", pending);
        });
        ListTag seen = new ListTag();
        for (UUID snapshotId : library.seenSnapshotIds()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("snapshot_id", snapshotId);
            seen.add(entry);
        }
        tag.put("seen_snapshots", seen);
        tag.putString("skill_mode", library.skillMode().name());
        tag.putBoolean("mirror_sync_enabled", library.mirrorSyncEnabled());
        return tag;
    }

    public static ImitatorFormLibraryState fromTag(CompoundTag source) {
        CompoundTag tag = migrate(source);
        int schema = requireSchema(tag);
        Map<Integer, ImitatorForm> forms = readForms(tag.getList("forms", Tag.TAG_COMPOUND));
        OptionalInt selected = tag.contains("selected_slot", Tag.TAG_INT) ? OptionalInt.of(tag.getInt("selected_slot")) : OptionalInt.empty();
        Optional<ImitatorPendingRecord> pending = tag.contains("pending_record", Tag.TAG_COMPOUND)
                ? Optional.of(readPending(tag.getCompound("pending_record")))
                : Optional.empty();
        List<UUID> seen = tag.getList("seen_snapshots", Tag.TAG_COMPOUND).stream()
                .map(value -> value instanceof CompoundTag entry ? requireUuid(entry, "snapshot_id") : null)
                .toList();
        ImitatorSkillMode skillMode = readMode(tag);
        return new ImitatorFormLibraryState(schema, forms, selected, pending, seen, skillMode, tag.getBoolean("mirror_sync_enabled"));
    }

    public static void registerMigration(int sourceVersion, SchemaMigration migration) {
        MIGRATIONS.register(sourceVersion, migration);
    }

    private static Map<Integer, ImitatorForm> readForms(ListTag tags) {
        Map<Integer, ImitatorForm> forms = new LinkedHashMap<>();
        for (int index = 0; index < tags.size(); index++) {
            CompoundTag tag = tags.getCompound(index);
            if (!tag.contains("slot", Tag.TAG_INT)) {
                throw new IllegalArgumentException("Missing form slot");
            }
            int slot = tag.getInt("slot");
            ImitatorForm form = new ImitatorForm(
                    requireUuid(tag, "snapshot_id"),
                    tag.getDouble("precision"),
                    tag.getBoolean("perfect"),
                    tag.getBoolean("mirror_sync_allowed"),
                    tag.getBoolean("skill_copy_allowed"),
                    tag.contains("stats", Tag.TAG_COMPOUND) ? ImitatorFormStats.fromTag(tag.getCompound("stats")) : ImitatorFormStats.empty()
            );
            if (forms.putIfAbsent(slot, form) != null) {
                throw new IllegalArgumentException("Duplicate form slot: " + slot);
            }
        }
        return forms;
    }

    private static ImitatorPendingRecord readPending(CompoundTag tag) {
        if (!tag.contains("created_game_time", Tag.TAG_LONG) || !tag.contains("expires_game_time", Tag.TAG_LONG)) {
            throw new IllegalArgumentException("Pending record is missing timing data");
        }
        if (!tag.contains("precision", Tag.TAG_DOUBLE)) {
            throw new IllegalArgumentException("Pending record is missing precision data");
        }
        return new ImitatorPendingRecord(requireUuid(tag, "snapshot_id"), tag.getLong("created_game_time"), tag.getLong("expires_game_time"), tag.getDouble("precision"), tag.getBoolean("mirror_sync_allowed"), tag.getBoolean("skill_copy_allowed"));
    }

    private static UUID requireUuid(CompoundTag tag, String key) {
        if (!tag.hasUUID(key)) {
            throw new IllegalArgumentException("Missing UUID field: " + key);
        }
        return tag.getUUID(key);
    }

    private static ImitatorSkillMode readMode(CompoundTag tag) {
        if (!tag.contains("skill_mode", Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing Imitator skill mode");
        }
        try {
            return ImitatorSkillMode.valueOf(tag.getString("skill_mode"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown Imitator skill mode");
        }
    }

    private static CompoundTag migrate(CompoundTag source) {
        if (!source.contains("schema", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing form library schema version");
        }
        int sourceVersion = source.getInt("schema");
        if (sourceVersion < 1 || sourceVersion > ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported form library schema version: " + sourceVersion);
        }
        if (sourceVersion == ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION) {
            return source.copy();
        }
        CompoundTag migrated = MIGRATIONS.migrate(source, sourceVersion, ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION);
        migrated.putInt("schema", ImitatorFormLibraryState.CURRENT_SCHEMA_VERSION);
        return migrated;
    }

    private static int requireSchema(CompoundTag tag) {
        if (!tag.contains("schema", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing form library schema version");
        }
        return tag.getInt("schema");
    }
}
