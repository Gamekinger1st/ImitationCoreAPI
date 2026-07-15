package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class ImitatorFormAbilities {
    private ImitatorFormAbilities() {
    }

    public static ImitatorFormAbilityRegistry registry() {
        return ImitationApi.imitatorFormAbilities();
    }

    public static ImitatorActionResult activate(ServerPlayer player, IdentitySnapshot snapshot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        return registry().activate(player, snapshot);
    }

    public static ImitatorActionResult activate(ServerPlayer player, IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        return registry().activate(player, snapshot, policy, access);
    }

    public static void tick(ServerPlayer player, IdentitySnapshot snapshot) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        if (player.isSpectator()) {
            return;
        }
        registry().tick(player, snapshot);
    }

    public static void tick(ServerPlayer player, IdentitySnapshot snapshot, ImitatorSkillCopyPolicy policy, ImitatorSkillCopyAccess access) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(access, "access");
        if (player.isSpectator()) {
            return;
        }
        registry().tick(player, snapshot, policy, access);
    }
}
