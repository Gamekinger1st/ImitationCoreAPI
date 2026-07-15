package com.github.gamekinger1st.imitationcoreapi.api.chat;

@FunctionalInterface
public interface ChatAuditListener {
    void onChatAudit(ChatAuditEntry entry);
}
