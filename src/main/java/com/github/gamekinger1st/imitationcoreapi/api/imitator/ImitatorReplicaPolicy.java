package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public record ImitatorReplicaPolicy(
        int lifetimeTicks,
        double spawnDistance,
        double cleanupDistance,
        boolean fallbackPlayerForms,
        boolean copyEntityNbt,
        boolean copyTensuraState,
        boolean suppressDrops,
        boolean suppressExperience,
        boolean persistentMob,
        boolean targetOwner,
        String namePrefix,
        boolean copyRecordedSkills
) {
    public static final ImitatorReplicaPolicy DEFAULT = new ImitatorReplicaPolicy(
            3_600,
            3D,
            96D,
            true,
            true,
            true,
            true,
            true,
            false,
            false,
            "Replica: ",
            true
    );

    public ImitatorReplicaPolicy(int lifetimeTicks, double spawnDistance, double cleanupDistance, boolean fallbackPlayerForms, boolean copyEntityNbt, boolean copyTensuraState, boolean suppressDrops, boolean suppressExperience, boolean persistentMob, boolean targetOwner, String namePrefix) {
        this(lifetimeTicks, spawnDistance, cleanupDistance, fallbackPlayerForms, copyEntityNbt, copyTensuraState, suppressDrops, suppressExperience, persistentMob, targetOwner, namePrefix, true);
    }

    public ImitatorReplicaPolicy {
        if (lifetimeTicks <= 0 || lifetimeTicks > 72_000) {
            throw new IllegalArgumentException("Replica lifetime must be between one tick and one hour");
        }
        if (!Double.isFinite(spawnDistance) || spawnDistance < 0D || spawnDistance > 32D) {
            throw new IllegalArgumentException("Replica spawn distance must be between zero and thirty-two blocks");
        }
        if (!Double.isFinite(cleanupDistance) || cleanupDistance < 1D || cleanupDistance > 512D) {
            throw new IllegalArgumentException("Replica cleanup distance must be between one and five-hundred-twelve blocks");
        }
        Objects.requireNonNull(namePrefix, "namePrefix");
        namePrefix = namePrefix.strip();
        if (namePrefix.length() > 64) {
            throw new IllegalArgumentException("Replica name prefix is too long");
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("lifetime_ticks", lifetimeTicks);
        tag.putDouble("spawn_distance", spawnDistance);
        tag.putDouble("cleanup_distance", cleanupDistance);
        tag.putBoolean("fallback_player_forms", fallbackPlayerForms);
        tag.putBoolean("copy_entity_nbt", copyEntityNbt);
        tag.putBoolean("copy_tensura_state", copyTensuraState);
        tag.putBoolean("suppress_drops", suppressDrops);
        tag.putBoolean("suppress_experience", suppressExperience);
        tag.putBoolean("persistent_mob", persistentMob);
        tag.putBoolean("target_owner", targetOwner);
        tag.putString("name_prefix", namePrefix);
        tag.putBoolean("copy_recorded_skills", copyRecordedSkills);
        return tag;
    }

    public static ImitatorReplicaPolicy fromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return DEFAULT;
        }
        return new ImitatorReplicaPolicy(
                tag.contains("lifetime_ticks") ? tag.getInt("lifetime_ticks") : DEFAULT.lifetimeTicks,
                tag.contains("spawn_distance") ? tag.getDouble("spawn_distance") : DEFAULT.spawnDistance,
                tag.contains("cleanup_distance") ? tag.getDouble("cleanup_distance") : DEFAULT.cleanupDistance,
                tag.contains("fallback_player_forms") ? tag.getBoolean("fallback_player_forms") : DEFAULT.fallbackPlayerForms,
                tag.contains("copy_entity_nbt") ? tag.getBoolean("copy_entity_nbt") : DEFAULT.copyEntityNbt,
                tag.contains("copy_tensura_state") ? tag.getBoolean("copy_tensura_state") : DEFAULT.copyTensuraState,
                tag.contains("suppress_drops") ? tag.getBoolean("suppress_drops") : DEFAULT.suppressDrops,
                tag.contains("suppress_experience") ? tag.getBoolean("suppress_experience") : DEFAULT.suppressExperience,
                tag.contains("persistent_mob") ? tag.getBoolean("persistent_mob") : DEFAULT.persistentMob,
                tag.contains("target_owner") ? tag.getBoolean("target_owner") : DEFAULT.targetOwner,
                tag.contains("name_prefix") ? tag.getString("name_prefix") : DEFAULT.namePrefix,
                tag.contains("copy_recorded_skills") ? tag.getBoolean("copy_recorded_skills") : DEFAULT.copyRecordedSkills
        );
    }
}
