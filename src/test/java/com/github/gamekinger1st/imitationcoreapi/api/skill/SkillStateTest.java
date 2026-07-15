package com.github.gamekinger1st.imitationcoreapi.api.skill;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillStateTest {
    @Test
    void defensivelyCopiesSerializedSkillState() {
        CompoundTag data = new CompoundTag();
        data.putInt("mode", 2);
        SkillState state = new SkillState(ResourceLocation.withDefaultNamespace("example"), data, 10, true, List.of(5, 10), false);
        data.putInt("mode", 4);

        assertEquals(2, state.serializedData().getInt("mode"));
        assertEquals(List.of(5, 10), state.cooldowns());
    }

    @Test
    void rejectsInvalidMastery() {
        assertThrows(IllegalArgumentException.class, () -> new SkillState(ResourceLocation.withDefaultNamespace("example"), new CompoundTag(), -1, false, List.of(), false));
    }
}
