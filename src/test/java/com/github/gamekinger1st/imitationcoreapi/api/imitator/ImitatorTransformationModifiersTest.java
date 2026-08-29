package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityAssessment;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.BaselineSnapshot;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitatorTransformationModifiersTest {
    @Test
    void resolvesAllAutoJumpOverrideModes() {
        assertFalse(ImitatorAutoJumpOverride.INHERIT.resolve(false));
        assertTrue(ImitatorAutoJumpOverride.INHERIT.resolve(true));
        assertTrue(ImitatorAutoJumpOverride.FORCE_ENABLED.resolve(false));
        assertFalse(ImitatorAutoJumpOverride.FORCE_DISABLED.resolve(true));
    }

    @Test
    void serializesAndFindsModifiersFromTheTransformationSession() {
        ImitatorTransformationModifiers modifiers = ImitatorTransformationModifiers.forceAutoJump(true);
        CompoundTag playerData = new CompoundTag();
        ImitatorTransformationModifierState.store(playerData, modifiers);
        TransformationSession session = TransformationSession.begin(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BaselineSnapshot(BaselineSnapshot.CURRENT_SCHEMA_VERSION, playerData, List.of()),
                CompatibilityAssessment.full(),
                1L
        );

        assertEquals(modifiers, ImitatorTransformationModifierState.find(session));
        assertEquals(modifiers, ImitatorTransformationModifiers.fromTag(modifiers.toTag()));
    }

    @Test
    void invalidSerializedModifiersFallBackToInheritance() {
        CompoundTag invalid = new CompoundTag();
        invalid.putString("auto_jump", "INVALID");

        assertEquals(ImitatorTransformationModifiers.DEFAULT, ImitatorTransformationModifiers.fromTag(invalid));
    }
}
