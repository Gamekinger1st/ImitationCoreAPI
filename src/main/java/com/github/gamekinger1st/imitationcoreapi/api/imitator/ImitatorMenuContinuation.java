package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface ImitatorMenuContinuation {
    void resume(ServerPlayer player, int selectedSlot);
}
