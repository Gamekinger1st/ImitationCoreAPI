package com.github.gamekinger1st.imitationcoreapi.api.chat;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatRateLimiterTest {
    @Test
    void rejectsBurstsAndRecoversAfterTheWindow() {
        ChatRateLimiter limiter = new ChatRateLimiter();
        UUID playerId = UUID.randomUUID();
        for (int index = 0; index < ChatRateLimiter.MAX_MESSAGES_PER_WINDOW; index++) {
            assertTrue(limiter.tryAcquire(playerId, 1_000L));
        }
        assertFalse(limiter.tryAcquire(playerId, 1_000L));
        assertTrue(limiter.tryAcquire(playerId, 1_000L + ChatRateLimiter.WINDOW_MILLIS));
    }
}
