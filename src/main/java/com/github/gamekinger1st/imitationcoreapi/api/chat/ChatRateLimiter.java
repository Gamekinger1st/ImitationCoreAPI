package com.github.gamekinger1st.imitationcoreapi.api.chat;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatRateLimiter {
    public static final int MAX_MESSAGES_PER_WINDOW = 6;
    public static final long WINDOW_MILLIS = 10_000L;
    private final Map<UUID, Deque<Long>> messages = new ConcurrentHashMap<>();
    private final int maxMessages;
    private final long windowMillis;

    public ChatRateLimiter() {
        this(MAX_MESSAGES_PER_WINDOW, WINDOW_MILLIS);
    }

    public ChatRateLimiter(int maxMessages, long windowMillis) {
        if (maxMessages < 1 || windowMillis < 1L) {
            throw new IllegalArgumentException("Chat rate limits must be positive");
        }
        this.maxMessages = maxMessages;
        this.windowMillis = windowMillis;
    }

    public boolean tryAcquire(UUID playerId, long nowMillis) {
        Deque<Long> timestamps = messages.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            if (!timestamps.isEmpty() && nowMillis < timestamps.peekLast()) {
                timestamps.clear();
            }
            while (!timestamps.isEmpty() && nowMillis - timestamps.peekFirst() >= windowMillis) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= maxMessages) {
                return false;
            }
            timestamps.addLast(nowMillis);
            return true;
        }
    }

    public void clear(UUID playerId) {
        messages.remove(playerId);
    }
}
