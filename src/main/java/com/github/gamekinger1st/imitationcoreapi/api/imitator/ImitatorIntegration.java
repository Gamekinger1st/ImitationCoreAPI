package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaIdentity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public interface ImitatorIntegration {
    ResourceLocation id();

    int priority();

    Optional<UUID> activeSession(ServerPlayer player);

    Optional<PersonaIdentity> activePersona(ServerPlayer player);
}
