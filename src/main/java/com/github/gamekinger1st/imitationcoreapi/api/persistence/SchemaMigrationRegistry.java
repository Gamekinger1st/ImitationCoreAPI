package com.github.gamekinger1st.imitationcoreapi.api.persistence;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class SchemaMigrationRegistry {
    private final Map<Integer, SchemaMigration> migrations = new HashMap<>();

    public synchronized void register(int sourceVersion, SchemaMigration migration) {
        if (sourceVersion < 1) {
            throw new IllegalArgumentException("sourceVersion must be positive");
        }
        Objects.requireNonNull(migration, "migration");
        if (migrations.putIfAbsent(sourceVersion, migration) != null) {
            throw new IllegalArgumentException("A migration is already registered for schema version " + sourceVersion);
        }
    }

    public synchronized CompoundTag migrate(CompoundTag source, int sourceVersion, int targetVersion) {
        Objects.requireNonNull(source, "source");
        if (sourceVersion < 1 || targetVersion < sourceVersion) {
            throw new IllegalArgumentException("Invalid migration range");
        }
        CompoundTag result = source.copy();
        for (int version = sourceVersion; version < targetVersion; version++) {
            SchemaMigration migration = migrations.get(version);
            if (migration == null) {
                throw new IllegalStateException("No migration is registered from schema version " + version);
            }
            result = Objects.requireNonNull(migration.migrate(result.copy()), "migration result").copy();
        }
        return result;
    }
}
