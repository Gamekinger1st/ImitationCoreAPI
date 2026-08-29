package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

public interface DisguiseAnimationAdapter {
    ResourceLocation id();

    default int priority() {
        return 0;
    }

    boolean supports(Entity imitation, Entity subject, ClientDisguiseState state);

    void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick);

    default void synchronize(Entity imitation, Entity subject, ClientDisguiseState state, float partialTick, DisguiseAnimationIntent intent) {
        synchronize(imitation, subject, state, partialTick);
    }

    default void clearSession(UUID sessionId) {
    }

    default void clearAllSessions() {
    }
}
