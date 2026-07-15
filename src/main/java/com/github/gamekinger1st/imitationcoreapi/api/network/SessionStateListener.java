package com.github.gamekinger1st.imitationcoreapi.api.network;

@FunctionalInterface
public interface SessionStateListener {
    void onSessionState(SessionStatePayload payload);
}
