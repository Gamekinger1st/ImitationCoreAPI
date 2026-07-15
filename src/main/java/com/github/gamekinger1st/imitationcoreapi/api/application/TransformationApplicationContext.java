package com.github.gamekinger1st.imitationcoreapi.api.application;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public record TransformationApplicationContext(
        MinecraftServer server,
        ServerPlayer owner,
        TransformationSession session,
        IdentitySnapshot snapshot,
        long gameTime
) {
    public TransformationApplicationContext {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(snapshot, "snapshot");
        if (gameTime < 0) {
            throw new IllegalArgumentException("gameTime cannot be negative");
        }
    }
}
