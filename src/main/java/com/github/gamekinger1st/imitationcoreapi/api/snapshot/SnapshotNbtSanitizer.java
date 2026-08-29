package com.github.gamekinger1st.imitationcoreapi.api.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Set;

public final class SnapshotNbtSanitizer {
    private static final Set<String> EXCLUDED_ENTITY_KEYS = Set.of(
            "UUID", "UUIDMost", "UUIDLeast", "Pos", "Motion", "Rotation", "FallDistance", "Fire", "Air", "OnGround",
            "PortalCooldown", "Invulnerable", "Passengers", "Leash", "Brain", "Tags", "CommandTags", "Owner", "OwnerUUID",
            "Thrower", "Inventory", "ArmorItems", "HandItems", "ActiveEffects", "Attributes", "active_effects", "attributes",
            "equipment", "drop_chances", "HandDropChances", "ArmorDropChances", "body_armor_item", "body_armor_drop_chance",
            "Health", "HurtTime", "HurtByTimestamp", "DeathTime", "AttackTime", "AbsorptionAmount", "absorption_amount",
            "Pose", "ForcedPose", "SleepingX", "SleepingY", "SleepingZ", "Sleeping", "InSittingPose", "Sitting",
            "FallFlying", "FallFlyingTicks", "AngerTime", "AngryAt", "LastHurtByPlayer", "DeathLootTable", "DeathLootTableSeed",
            "CanPickUpLoot", "PersistenceRequired", "NoAI", "PatrolTarget", "Patrolling", "PatrolLeader", "RaidId", "Wave",
            "CanJoinRaid", "InLove", "LoveCause"
    );
    private static final Set<String> RETAINED_PLAYER_ENTITY_KEYS = Set.of(
            "CustomName", "CustomNameVisible", "Silent", "NoGravity", "Glowing", "TicksFrozen"
    );

    private SnapshotNbtSanitizer() {
    }

    public static CompoundTag sanitizeEntityData(CompoundTag source) {
        CompoundTag sanitized = source.copy();
        EXCLUDED_ENTITY_KEYS.forEach(sanitized::remove);
        return sanitized;
    }

    public static CompoundTag sanitizePlayerEntityData(CompoundTag source) {
        CompoundTag sanitized = new CompoundTag();
        RETAINED_PLAYER_ENTITY_KEYS.forEach(key -> copyIfPresent(source, sanitized, key));
        return sanitized;
    }

    public static CompoundTag sanitizeVisualData(CompoundTag source) {
        return source.copy();
    }

    private static void copyIfPresent(CompoundTag source, CompoundTag target, String key) {
        Tag value = source.get(key);
        if (value != null) {
            target.put(key, value.copy());
        }
    }
}
