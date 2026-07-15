package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public record ImitatorFormAbilityContext(ServerPlayer player, IdentitySnapshot snapshot) {
    public ImitatorFormAbilityContext {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
    }
}
