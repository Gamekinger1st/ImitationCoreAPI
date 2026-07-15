package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientDisguiseStoreTest {
    @Test
    void keepsNewestStateAndOnlyClearsTheMatchingOwner() {
        ClientDisguiseStore store = new ClientDisguiseStore();
        UUID owner = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ClientDisguiseState current = state(owner, sessionId, 4L);
        ClientDisguiseState stale = state(owner, sessionId, 3L);

        store.onDisguiseActivated(current);
        store.onDisguiseActivated(stale);
        store.onDisguiseCleared(12, UUID.randomUUID());

        assertEquals(4L, store.get(12).orElseThrow().revision());
        store.onDisguiseCleared(12, owner);
        assertTrue(store.get(12).isEmpty());
    }

    @Test
    void replacesStaleEntityIdStateWhenTheOwnerOrSessionChanges() {
        ClientDisguiseStore store = new ClientDisguiseStore();
        UUID firstOwner = UUID.randomUUID();
        UUID secondOwner = UUID.randomUUID();
        ClientDisguiseState stale = state(firstOwner, UUID.randomUUID(), 10L);
        ClientDisguiseState replacement = state(secondOwner, UUID.randomUUID(), 0L);

        store.onDisguiseActivated(stale);
        store.onDisguiseActivated(replacement);

        assertEquals(secondOwner, store.get(12).orElseThrow().ownerId());
        assertEquals(0L, store.get(12).orElseThrow().revision());
    }

    private static ClientDisguiseState state(UUID owner, long revision) {
        return state(owner, UUID.randomUUID(), revision);
    }

    private static ClientDisguiseState state(UUID owner, UUID sessionId, long revision) {
        return new ClientDisguiseState(
                12,
                owner,
                sessionId,
                UUID.randomUUID(),
                ResourceLocation.withDefaultNamespace("zombie"),
                "Zombie",
                new CompoundTag(),
                new CompoundTag(),
                CompatibilityLevel.FALLBACK,
                revision
        );
    }
}
