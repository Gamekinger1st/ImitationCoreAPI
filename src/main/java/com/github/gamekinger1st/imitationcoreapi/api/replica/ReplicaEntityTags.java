package com.github.gamekinger1st.imitationcoreapi.api.replica;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import java.util.Optional;
import java.util.UUID;

public final class ReplicaEntityTags {
    public static final String REPLICA = "imitationcoreapi.replica";
    public static final String OWNER = "imitationcoreapi.replica_owner";
    public static final String SESSION = "imitationcoreapi.replica_session";
    public static final String EXPIRES = "imitationcoreapi.replica_expires";
    public static final String SUPPRESS_DROPS = "imitationcoreapi.replica_suppress_drops";
    public static final String SUPPRESS_EXPERIENCE = "imitationcoreapi.replica_suppress_experience";
    public static final String VISUAL_EQUIPMENT = "imitationcoreapi.replica_visual_equipment";

    private ReplicaEntityTags() {
    }

    public static void mark(Entity entity, UUID ownerId, UUID sessionId, long expiresGameTime, boolean suppressDrops, boolean suppressExperience) {
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(REPLICA, true);
        data.putUUID(OWNER, ownerId);
        data.putUUID(SESSION, sessionId);
        data.putLong(EXPIRES, expiresGameTime);
        data.putBoolean(SUPPRESS_DROPS, suppressDrops);
        data.putBoolean(SUPPRESS_EXPERIENCE, suppressExperience);
    }

    public static boolean isReplica(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(REPLICA);
    }

    public static Optional<UUID> ownerId(Entity entity) {
        return uuid(entity, OWNER);
    }

    public static Optional<UUID> sessionId(Entity entity) {
        return uuid(entity, SESSION);
    }

    public static long expiresGameTime(Entity entity) {
        return entity == null ? 0L : entity.getPersistentData().getLong(EXPIRES);
    }

    public static boolean suppressDrops(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(SUPPRESS_DROPS);
    }

    public static boolean suppressExperience(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(SUPPRESS_EXPERIENCE);
    }

    public static void setVisualEquipment(Entity entity, CompoundTag equipment) {
        entity.getPersistentData().put(VISUAL_EQUIPMENT, equipment.copy());
    }

    public static CompoundTag visualEquipment(Entity entity) {
        return entity == null ? new CompoundTag() : entity.getPersistentData().getCompound(VISUAL_EQUIPMENT).copy();
    }

    private static Optional<UUID> uuid(Entity entity, String key) {
        if (entity == null || !entity.getPersistentData().hasUUID(key)) {
            return Optional.empty();
        }
        return Optional.of(entity.getPersistentData().getUUID(key));
    }
}
