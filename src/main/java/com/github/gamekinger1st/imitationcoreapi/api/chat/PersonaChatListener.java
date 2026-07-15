package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.api.network.PersonaChatPayload;

@FunctionalInterface
public interface PersonaChatListener {
    void onPersonaChat(PersonaChatPayload payload);
}
