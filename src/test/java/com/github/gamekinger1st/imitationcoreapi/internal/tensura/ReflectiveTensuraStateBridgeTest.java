package com.github.gamekinger1st.imitationcoreapi.internal.tensura;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectiveTensuraStateBridgeTest {
    @Test
    void excludesOwnerAndProgressionDataFromExistenceForms() {
        CompoundTag source = new CompoundTag();
        source.putDouble("magicule", 400D);
        source.putDouble("aura", 200D);
        source.putBoolean("trueDemonLord", true);
        source.putDouble("gainedEP", 900D);
        source.putInt("soulPoint", 100);
        source.putUUID("permanentOwner", java.util.UUID.randomUUID());
        source.put("neutralList", new ListTag());

        CompoundTag sanitized = ReflectiveTensuraStateBridge.sanitizeExistenceSection(source);

        assertEquals(400D, sanitized.getDouble("magicule"));
        assertEquals(200D, sanitized.getDouble("aura"));
        assertTrue(sanitized.getBoolean("trueDemonLord"));
        assertFalse(sanitized.contains("gainedEP"));
        assertFalse(sanitized.contains("soulPoint"));
        assertFalse(sanitized.contains("permanentOwner"));
        assertFalse(sanitized.contains("neutralList"));
        assertTrue(source.contains("permanentOwner"));
    }

    @Test
    void spiritFormsKeepLevelsWithoutCopyingLabyrinthProgress() {
        CompoundTag source = new CompoundTag();
        source.put("spiritList", new ListTag());
        source.putInt("cooldown", 12);
        source.putBoolean("colossusWon", true);

        CompoundTag sanitized = ReflectiveTensuraStateBridge.sanitizeSpiritSection(source);

        assertTrue(sanitized.contains("spiritList"));
        assertFalse(sanitized.contains("cooldown"));
        assertFalse(sanitized.contains("colossusWon"));
    }
}
