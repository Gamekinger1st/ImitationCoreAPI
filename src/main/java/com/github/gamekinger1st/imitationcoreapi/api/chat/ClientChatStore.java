package com.github.gamekinger1st.imitationcoreapi.api.chat;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientChatStore implements ChatMessageListener {
    public static final int MAX_HISTORY_SIZE = 1_000;
    private final Map<UUID, ChatEnvelope> messages = new LinkedHashMap<>();
    private final Map<ResourceLocation, Integer> unread = new ConcurrentHashMap<>();

    public synchronized Collection<ChatEnvelope> history() {
        return java.util.List.copyOf(messages.values());
    }

    public synchronized Collection<ChatEnvelope> history(ResourceLocation channelId) {
        return messages.values().stream().filter(envelope -> envelope.channelId().equals(channelId)).toList();
    }

    public int unread(ResourceLocation channelId) {
        return unread.getOrDefault(channelId, 0);
    }

    public void markRead(ResourceLocation channelId) {
        unread.remove(channelId);
    }

    public synchronized void clearAll() {
        messages.clear();
        unread.clear();
    }

    @Override
    public synchronized void onChatMessage(ChatEnvelope envelope) {
        if (messages.putIfAbsent(envelope.messageId(), envelope) != null) {
            return;
        }
        while (messages.size() > MAX_HISTORY_SIZE) {
            UUID oldest = messages.keySet().iterator().next();
            ChatEnvelope removed = messages.remove(oldest);
            unread.computeIfPresent(removed.channelId(), (channelId, count) -> count > 1 ? count - 1 : null);
        }
        unread.merge(envelope.channelId(), 1, Integer::sum);
    }
}
