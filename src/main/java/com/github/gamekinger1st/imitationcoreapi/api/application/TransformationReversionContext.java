package com.github.gamekinger1st.imitationcoreapi.api.application;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.Optional;

public record TransformationReversionContext(
        MinecraftServer server,
        Optional<ServerPlayer> owner,
        TransformationSession session,
        Optional<IdentitySnapshot> snapshot,
        TransformationLifecycleReason reason,
        long gameTime
) {
    public TransformationReversionContext {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(reason, "reason");
        if (gameTime < 0) {
            throw new IllegalArgumentException("gameTime cannot be negative");
        }
    }
}
