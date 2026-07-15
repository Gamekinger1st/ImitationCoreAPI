package com.github.gamekinger1st.imitationcoreapi.api.persistence;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchemaMigrationRegistryTest {
    @Test
    void appliesEachRegisteredMigrationInOrder() {
        SchemaMigrationRegistry registry = new SchemaMigrationRegistry();
        registry.register(1, tag -> {
            tag.putInt("value", tag.getInt("value") + 1);
            return tag;
        });
        registry.register(2, tag -> {
            tag.putInt("value", tag.getInt("value") * 2);
            return tag;
        });
        CompoundTag source = new CompoundTag();
        source.putInt("value", 3);

        CompoundTag migrated = registry.migrate(source, 1, 3);

        assertEquals(8, migrated.getInt("value"));
        assertEquals(3, source.getInt("value"));
    }

    @Test
    void rejectsMissingMigrationSteps() {
        SchemaMigrationRegistry registry = new SchemaMigrationRegistry();
        assertThrows(IllegalStateException.class, () -> registry.migrate(new CompoundTag(), 1, 2));
    }
}
