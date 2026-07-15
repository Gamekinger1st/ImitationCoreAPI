package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class SkillSnapshotSerialization {
    private SkillSnapshotSerialization() {
    }

    public static CompoundTag toTag(SkillSnapshot snapshot) {
        CompoundTag tag = new CompoundTag();
        tag.putString("bridge", snapshot.bridgeId().toString());
        tag.putInt("schema", snapshot.schemaVersion());
        ListTag skills = new ListTag();
        for (SkillState state : snapshot.skills()) {
            CompoundTag skill = new CompoundTag();
            skill.putString("id", state.skillId().toString());
            skill.put("data", state.serializedData());
            skill.putDouble("mastery", state.mastery());
            skill.putBoolean("toggled", state.toggled());
            skill.putBoolean("temporary", state.temporary());
            ListTag cooldowns = new ListTag();
            for (int cooldown : state.cooldowns()) {
                CompoundTag cooldownTag = new CompoundTag();
                cooldownTag.putInt("value", cooldown);
                cooldowns.add(cooldownTag);
            }
            skill.put("cooldowns", cooldowns);
            skills.add(skill);
        }
        tag.put("skills", skills);
        return tag;
    }

    public static SkillSnapshot fromTag(CompoundTag tag) {
        ResourceLocation bridgeId = resourceLocation(tag, "bridge");
        if (!tag.contains("schema", Tag.TAG_INT)) {
            throw new IllegalArgumentException("Missing skill snapshot schema version");
        }
        List<SkillState> skills = new ArrayList<>();
        ListTag tags = tag.getList("skills", Tag.TAG_COMPOUND);
        for (int index = 0; index < tags.size(); index++) {
            CompoundTag skill = tags.getCompound(index);
            if (!skill.contains("data", Tag.TAG_COMPOUND)) {
                throw new IllegalArgumentException("Missing serialized skill data");
            }
            List<Integer> cooldowns = new ArrayList<>();
            ListTag cooldownTags = skill.getList("cooldowns", Tag.TAG_COMPOUND);
            for (int cooldownIndex = 0; cooldownIndex < cooldownTags.size(); cooldownIndex++) {
                cooldowns.add(cooldownTags.getCompound(cooldownIndex).getInt("value"));
            }
            skills.add(new SkillState(
                    resourceLocation(skill, "id"),
                    skill.getCompound("data"),
                    skill.getDouble("mastery"),
                    skill.getBoolean("toggled"),
                    cooldowns,
                    skill.getBoolean("temporary")
            ));
        }
        return new SkillSnapshot(bridgeId, tag.getInt("schema"), skills);
    }

    private static ResourceLocation resourceLocation(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            throw new IllegalArgumentException("Missing resource location field: " + key);
        }
        ResourceLocation value = ResourceLocation.tryParse(tag.getString(key));
        if (value == null) {
            throw new IllegalArgumentException("Invalid resource location field: " + key);
        }
        return value;
    }
}
