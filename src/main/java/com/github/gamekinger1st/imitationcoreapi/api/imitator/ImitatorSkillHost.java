package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.server.level.ServerPlayer;

public interface ImitatorSkillHost {
    long mastery(ServerPlayer player);

    boolean canUse(ServerPlayer player, ImitatorSkillMode mode, ImitatorSkillCost cost);

    void onUseAccepted(ServerPlayer player, ImitatorSkillMode mode, ImitatorSkillCost cost);
}
