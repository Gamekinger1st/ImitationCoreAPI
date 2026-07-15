package com.github.gamekinger1st.imitationcoreapi.api.imitator;

import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import net.minecraft.server.level.ServerPlayer;

public interface ImitatorSynchronizer {
    ImitatorSynchronizer NO_OP = new ImitatorSynchronizer() {
        @Override
        public void syncFormLibrary(ServerPlayer player) {
        }

        @Override
        public void syncSession(ServerPlayer player, TransformationSession session) {
        }

        @Override
        public void openMenu(ServerPlayer player, ImitatorMenuRequest request) {
        }
    };

    void syncFormLibrary(ServerPlayer player);

    void syncSession(ServerPlayer player, TransformationSession session);

    void openMenu(ServerPlayer player, ImitatorMenuRequest request);
}
