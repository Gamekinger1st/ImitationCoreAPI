package com.github.gamekinger1st.imitationcoreapi.api.chat;

@FunctionalInterface
public interface ServerChatDeliveryRegistration extends AutoCloseable {
    @Override
    void close();
}
