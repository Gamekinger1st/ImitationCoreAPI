package com.github.gamekinger1st.imitationcoreapi.internal.imitator;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaIdentity;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.PlayerDisguiseProfileExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorIntegration;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.UUID;

public final class CoreImitatorIntegration implements ImitatorIntegration {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "core_imitator");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public int priority() {
        return Integer.MIN_VALUE;
    }

    @Override
    public Optional<UUID> activeSession(ServerPlayer player) {
        return ImitationCoreServices.forServer(player.serverLevel().getServer()).activeSessionForOwner(player.getUUID()).map(session -> session.sessionId());
    }

    @Override
    public Optional<PersonaIdentity> activePersona(ServerPlayer player) {
        return ImitationCoreServices.forServer(player.serverLevel().getServer()).activeSessionForOwner(player.getUUID())
                .flatMap(session -> ImitationCoreServices.forServer(player.serverLevel().getServer()).snapshot(session.snapshotId()))
                .map(snapshot -> new PersonaIdentity(
                        snapshot.snapshotId(),
                        snapshot.displayName(),
                        PlayerDisguiseProfileExtensions.find(snapshot.extensions()).map(profile -> profile.playerId())
                ));
}
}
