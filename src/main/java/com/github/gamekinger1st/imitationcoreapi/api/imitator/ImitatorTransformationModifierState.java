package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.Objects;

public final class ImitatorTransformationModifierState {
    private static final String BASELINE_KEY = "imitationcoreapi_transformation_modifiers";

    private ImitatorTransformationModifierState() {
    }

    public static ImitatorTransformationModifiers find(TransformationSession session) {
        Objects.requireNonNull(session, "session");
        CompoundTag playerData = session.baseline().playerData();
        return playerData.contains(BASELINE_KEY, Tag.TAG_COMPOUND)
                ? ImitatorTransformationModifiers.fromTag(playerData.getCompound(BASELINE_KEY))
                : ImitatorTransformationModifiers.DEFAULT;
    }

    static void store(CompoundTag playerData, ImitatorTransformationModifiers modifiers) {
        Objects.requireNonNull(playerData, "playerData");
        playerData.put(BASELINE_KEY, Objects.requireNonNull(modifiers, "modifiers").toTag());
    }
}
