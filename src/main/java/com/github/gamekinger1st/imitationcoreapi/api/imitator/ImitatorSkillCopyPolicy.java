package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillClassification;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public record ImitatorSkillCopyPolicy(
        int maximumCopiedSkills,
        double minimumSourceMastery,
        double masteryMultiplier,
        int temporaryRemoveTime,
        Set<ResourceLocation> deniedSkills,
        boolean allowUnclassifiedSkills,
        boolean allowUniqueSkills,
        boolean allowUltimateSkills
) {
    public static final ImitatorSkillCopyPolicy DISABLED = new ImitatorSkillCopyPolicy(0, 0D, 0D, 0, Set.of(), false, false, false);
    public static final ImitatorSkillCopyPolicy DEFAULT_FORM_ABILITY_POLICY = new ImitatorSkillCopyPolicy(0, 0D, 1D, 0, Set.of(), false, true, true);
    private static final String MAXIMUM_COPIED_SKILLS_KEY = "maximum_copied_skills";
    private static final String MINIMUM_SOURCE_MASTERY_KEY = "minimum_source_mastery";
    private static final String MASTERY_MULTIPLIER_KEY = "mastery_multiplier";
    private static final String TEMPORARY_REMOVE_TIME_KEY = "temporary_remove_time";
    private static final String DENIED_SKILLS_KEY = "denied_skills";
    private static final String ALLOW_UNCLASSIFIED_SKILLS_KEY = "allow_unclassified_skills";
    private static final String ALLOW_UNIQUE_SKILLS_KEY = "allow_unique_skills";
    private static final String ALLOW_ULTIMATE_SKILLS_KEY = "allow_ultimate_skills";

    public ImitatorSkillCopyPolicy(int maximumCopiedSkills, double minimumSourceMastery, double masteryMultiplier, int temporaryRemoveTime, Set<ResourceLocation> deniedSkills) {
        this(maximumCopiedSkills, minimumSourceMastery, masteryMultiplier, temporaryRemoveTime, deniedSkills, false, false, false);
    }

    public ImitatorSkillCopyPolicy {
        if (maximumCopiedSkills < 0 || maximumCopiedSkills > ImitatorSkillCopySnapshot.MAX_SKILLS) {
            throw new IllegalArgumentException("maximumCopiedSkills must be between zero and " + ImitatorSkillCopySnapshot.MAX_SKILLS);
        }
        if (!Double.isFinite(minimumSourceMastery) || minimumSourceMastery < 0D) {
            throw new IllegalArgumentException("minimumSourceMastery must be finite and non-negative");
        }
        if (!Double.isFinite(masteryMultiplier) || masteryMultiplier < 0D || masteryMultiplier > 1D) {
            throw new IllegalArgumentException("masteryMultiplier must be between zero and one");
        }
        if (temporaryRemoveTime < 0) {
            throw new IllegalArgumentException("temporaryRemoveTime cannot be negative");
        }
        Objects.requireNonNull(deniedSkills, "deniedSkills");
        deniedSkills = Set.copyOf(deniedSkills);
        for (ResourceLocation skillId : deniedSkills) {
            Objects.requireNonNull(skillId, "denied skill id");
        }
        if (maximumCopiedSkills > 0 && masteryMultiplier == 0D) {
            throw new IllegalArgumentException("Enabled skill copying requires a positive mastery multiplier");
        }
    }

    public boolean enabled() {
        return maximumCopiedSkills > 0;
    }

    public List<ImitatorCopiedSkill> select(ImitatorSkillCopySnapshot snapshot, ResourceLocation imitatorSkillId) {
        return select(snapshot, imitatorSkillId, ignored -> SkillClassification.STANDARD);
    }

    public List<ImitatorCopiedSkill> select(ImitatorSkillCopySnapshot snapshot, ResourceLocation imitatorSkillId, Function<ResourceLocation, SkillClassification> classificationResolver) {
        return select(snapshot, imitatorSkillId, classificationResolver, ImitatorSkillCopyAccess.POLICY);
    }

    public List<ImitatorCopiedSkill> select(ImitatorSkillCopySnapshot snapshot, ResourceLocation imitatorSkillId, Function<ResourceLocation, SkillClassification> classificationResolver, ImitatorSkillCopyAccess access) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(imitatorSkillId, "imitatorSkillId");
        Objects.requireNonNull(classificationResolver, "classificationResolver");
        Objects.requireNonNull(access, "access");
        if (!enabled()) {
            return List.of();
        }
        return snapshot.skills().stream()
                .filter(skill -> skill.mastery() >= minimumSourceMastery)
                .filter(skill -> !skill.skillId().equals(imitatorSkillId))
                .filter(skill -> !deniedSkills.contains(skill.skillId()))
                .filter(skill -> allows(Objects.requireNonNullElse(classificationResolver.apply(skill.skillId()), SkillClassification.UNKNOWN), access))
                .sorted(Comparator.comparingDouble(ImitatorCopiedSkill::mastery).reversed().thenComparing(skill -> skill.skillId().toString()))
                .limit(maximumCopiedSkills)
                .map(skill -> new ImitatorCopiedSkill(skill.skillId(), skill.mastery() * masteryMultiplier))
                .toList();
    }

    public boolean allows(SkillClassification classification) {
        return switch (Objects.requireNonNull(classification, "classification")) {
            case STANDARD, RESISTANCE, INTRINSIC, COMMON, EXTRA -> true;
            case UNIQUE -> allowUniqueSkills;
            case ULTIMATE -> allowUltimateSkills;
            case UNKNOWN -> allowUnclassifiedSkills;
        };
    }

    public boolean allows(SkillClassification classification, ImitatorSkillCopyAccess access) {
        Objects.requireNonNull(classification, "classification");
        return switch (Objects.requireNonNull(access, "access")) {
            case POLICY -> allows(classification);
            case SUPERIOR_EP -> classification == SkillClassification.UNKNOWN ? allowUnclassifiedSkills : true;
            case INFERIOR_OR_EQUAL_EP -> switch (classification) {
                case ULTIMATE, INTRINSIC -> false;
                case UNKNOWN -> allowUnclassifiedSkills;
                default -> true;
            };
        };
    }

    public boolean allows(ImitatorFormAbility ability, IdentitySnapshot snapshot, ImitatorSkillCopyAccess access) {
        Objects.requireNonNull(ability, "ability");
        Objects.requireNonNull(snapshot, "snapshot");
        SkillClassification classification;
        try {
            classification = Objects.requireNonNullElse(ability.classification(snapshot), SkillClassification.UNKNOWN);
        } catch (RuntimeException | LinkageError exception) {
            classification = SkillClassification.UNKNOWN;
        }
        return allows(classification, access);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(MAXIMUM_COPIED_SKILLS_KEY, maximumCopiedSkills);
        tag.putDouble(MINIMUM_SOURCE_MASTERY_KEY, minimumSourceMastery);
        tag.putDouble(MASTERY_MULTIPLIER_KEY, masteryMultiplier);
        tag.putInt(TEMPORARY_REMOVE_TIME_KEY, temporaryRemoveTime);
        ListTag denied = new ListTag();
        deniedSkills.stream().map(ResourceLocation::toString).sorted().map(StringTag::valueOf).forEach(denied::add);
        tag.put(DENIED_SKILLS_KEY, denied);
        tag.putBoolean(ALLOW_UNCLASSIFIED_SKILLS_KEY, allowUnclassifiedSkills);
        tag.putBoolean(ALLOW_UNIQUE_SKILLS_KEY, allowUniqueSkills);
        tag.putBoolean(ALLOW_ULTIMATE_SKILLS_KEY, allowUltimateSkills);
        return tag;
    }

    public static ImitatorSkillCopyPolicy fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        Builder builder = builder()
                .maximumCopiedSkills(tag.getInt(MAXIMUM_COPIED_SKILLS_KEY))
                .minimumSourceMastery(tag.getDouble(MINIMUM_SOURCE_MASTERY_KEY))
                .masteryMultiplier(tag.contains(MASTERY_MULTIPLIER_KEY) ? tag.getDouble(MASTERY_MULTIPLIER_KEY) : 1D)
                .temporaryRemoveTime(tag.getInt(TEMPORARY_REMOVE_TIME_KEY))
                .allowUnclassifiedSkills(tag.getBoolean(ALLOW_UNCLASSIFIED_SKILLS_KEY))
                .allowUniqueSkills(tag.getBoolean(ALLOW_UNIQUE_SKILLS_KEY))
                .allowUltimateSkills(tag.getBoolean(ALLOW_ULTIMATE_SKILLS_KEY));
        ListTag denied = tag.getList(DENIED_SKILLS_KEY, Tag.TAG_STRING);
        for (int index = 0; index < denied.size(); index++) {
            ResourceLocation skillId = ResourceLocation.tryParse(denied.getString(index));
            if (skillId != null) {
                builder.denySkill(skillId);
            }
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int maximumCopiedSkills;
        private double minimumSourceMastery;
        private double masteryMultiplier = 1D;
        private int temporaryRemoveTime;
        private final Set<ResourceLocation> deniedSkills = new LinkedHashSet<>();
        private boolean allowUnclassifiedSkills;
        private boolean allowUniqueSkills;
        private boolean allowUltimateSkills;

        public Builder maximumCopiedSkills(int value) {
            maximumCopiedSkills = value;
            return this;
        }

        public Builder minimumSourceMastery(double value) {
            minimumSourceMastery = value;
            return this;
        }

        public Builder masteryMultiplier(double value) {
            masteryMultiplier = value;
            return this;
        }

        public Builder temporaryRemoveTime(int value) {
            temporaryRemoveTime = value;
            return this;
        }

        public Builder denySkill(ResourceLocation skillId) {
            deniedSkills.add(Objects.requireNonNull(skillId, "skillId"));
            return this;
        }

        public Builder deniedSkills(Set<ResourceLocation> skillIds) {
            deniedSkills.clear();
            deniedSkills.addAll(Objects.requireNonNull(skillIds, "skillIds"));
            return this;
        }

        public Builder allowUnclassifiedSkills(boolean value) {
            allowUnclassifiedSkills = value;
            return this;
        }

        public Builder allowUniqueSkills(boolean value) {
            allowUniqueSkills = value;
            return this;
        }

        public Builder allowUltimateSkills(boolean value) {
            allowUltimateSkills = value;
            return this;
        }

        public ImitatorSkillCopyPolicy build() {
            return new ImitatorSkillCopyPolicy(
                    maximumCopiedSkills,
                    minimumSourceMastery,
                    masteryMultiplier,
                    temporaryRemoveTime,
                    deniedSkills,
                    allowUnclassifiedSkills,
                    allowUniqueSkills,
                    allowUltimateSkills
            );
        }
    }
}
