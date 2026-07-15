package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ImitatorSkillCopyExtensions {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "imitator_skill_copy");
    public static final int SCHEMA_VERSION = 1;
    private static final String BRIDGE_ID = "bridge_id";
    private static final String SKILLS = "skills";
    private static final String SKILL_ID = "skill_id";
    private static final String MASTERY = "mastery";

    private ImitatorSkillCopyExtensions() {
    }

    public static SnapshotExtension create(ImitatorSkillCopySnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        CompoundTag tag = new CompoundTag();
        tag.putString(BRIDGE_ID, snapshot.bridgeId().toString());
        tag.putInt("schema", snapshot.schemaVersion());
        ListTag skills = new ListTag();
        for (ImitatorCopiedSkill skill : snapshot.skills()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(SKILL_ID, skill.skillId().toString());
            entry.putDouble(MASTERY, skill.mastery());
            skills.add(entry);
        }
        tag.put(SKILLS, skills);
        return new SnapshotExtension(ID, SCHEMA_VERSION, tag);
    }

    public static Optional<ImitatorSkillCopySnapshot> find(List<SnapshotExtension> extensions) {
        Objects.requireNonNull(extensions, "extensions");
        return extensions.stream()
                .filter(extension -> extension.adapterId().equals(ID))
                .filter(extension -> extension.schemaVersion() == SCHEMA_VERSION)
                .findFirst()
                .flatMap(extension -> fromTag(extension.payload()));
    }

    private static Optional<ImitatorSkillCopySnapshot> fromTag(CompoundTag tag) {
        if (!tag.contains(BRIDGE_ID, Tag.TAG_STRING) || !tag.contains("schema", Tag.TAG_INT)) {
            return Optional.empty();
        }
        ResourceLocation bridgeId = ResourceLocation.tryParse(tag.getString(BRIDGE_ID));
        if (bridgeId == null) {
            return Optional.empty();
        }
        List<ImitatorCopiedSkill> skills = new ArrayList<>();
        ListTag entries = tag.getList(SKILLS, Tag.TAG_COMPOUND);
        if (entries.size() > ImitatorSkillCopySnapshot.MAX_SKILLS) {
            return Optional.empty();
        }
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.contains(SKILL_ID, Tag.TAG_STRING) || !entry.contains(MASTERY, Tag.TAG_DOUBLE)) {
                return Optional.empty();
            }
            ResourceLocation skillId = ResourceLocation.tryParse(entry.getString(SKILL_ID));
            if (skillId == null) {
                return Optional.empty();
            }
            skills.add(new ImitatorCopiedSkill(skillId, entry.getDouble(MASTERY)));
        }
        try {
            return Optional.of(new ImitatorSkillCopySnapshot(bridgeId, tag.getInt("schema"), skills));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
