package com.github.gamekinger1st.imitationcoreapi.api.persistence;

import net.minecraft.nbt.CompoundTag;

@FunctionalInterface
public interface SchemaMigration {
    CompoundTag migrate(CompoundTag source);
}
