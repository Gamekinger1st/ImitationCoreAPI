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

    public boolean tryAcquire(UUID playerId, long nowMillis) {
        Deque<Long> timestamps = messages.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && nowMillis - timestamps.peekFirst() >= WINDOW_MILLIS) {
                timestamps.removeFirst();
            }
            if (timestamps.size() >= MAX_MESSAGES_PER_WINDOW) {
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
