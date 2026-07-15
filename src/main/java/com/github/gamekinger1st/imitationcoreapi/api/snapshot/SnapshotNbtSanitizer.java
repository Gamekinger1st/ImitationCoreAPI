package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.nbt.CompoundTag;

import java.util.Set;

public final class SnapshotNbtSanitizer {
    private static final Set<String> EXCLUDED_ENTITY_KEYS = Set.of(
            "UUID", "UUIDMost", "UUIDLeast", "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround",
            "PortalCooldown", "Invulnerable", "Passengers", "Leash", "Brain", "Tags", "CommandTags", "Owner", "OwnerUUID",
            "Thrower", "Inventory", "ArmorItems", "HandItems", "ActiveEffects", "Attributes"
    );

    private SnapshotNbtSanitizer() {
    }

    public static CompoundTag sanitizeEntityData(CompoundTag source) {
        CompoundTag sanitized = source.copy();
        EXCLUDED_ENTITY_KEYS.forEach(sanitized::remove);
        return sanitized;
    }

    public static CompoundTag sanitizeVisualData(CompoundTag source) {
        return source.copy();
    }
}
