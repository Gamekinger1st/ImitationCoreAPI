package com.github.gamekinger1st.imitationcoreapi.api.disguise;

import java.util.UUID;

public interface ClientDisguiseStateListener {
    void onDisguiseActivated(ClientDisguiseState state);

    void onDisguiseCleared(int entityId, UUID ownerId);
}
