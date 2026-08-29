package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public record ImitatorTransformationModifiers(ImitatorAutoJumpOverride autoJumpOverride) {
    public static final ImitatorTransformationModifiers DEFAULT = new ImitatorTransformationModifiers(ImitatorAutoJumpOverride.INHERIT);
    private static final String AUTO_JUMP_KEY = "auto_jump";

    public ImitatorTransformationModifiers {
        Objects.requireNonNull(autoJumpOverride, "autoJumpOverride");
    }

    public static ImitatorTransformationModifiers forceAutoJump(boolean enabled) {
        return new ImitatorTransformationModifiers(enabled ? ImitatorAutoJumpOverride.FORCE_ENABLED : ImitatorAutoJumpOverride.FORCE_DISABLED);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString(AUTO_JUMP_KEY, autoJumpOverride.name());
        return tag;
    }

    public static ImitatorTransformationModifiers fromTag(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        try {
            return new ImitatorTransformationModifiers(ImitatorAutoJumpOverride.valueOf(tag.getString(AUTO_JUMP_KEY)));
        } catch (IllegalArgumentException exception) {
            return DEFAULT;
        }
    }
}
