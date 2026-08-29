package com.github.gamekinger1st.imitationcoreapi.internal.config;

import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImitationCoreConfigTest {
    @Test
    void exposesSafeDefaultsBeforeNeoForgeLoadsTheConfigFiles() {
        assertEquals(ChatChannels.GLOBAL, ImitationCoreConfig.defaultChatChannel());
        assertEquals(128D, ImitationCoreConfig.localChatRange());
        assertEquals(6, ImitationCoreConfig.chatRateLimit());
        assertEquals(10_000L, ImitationCoreConfig.chatRateWindowMillis());
        assertFalse(ImitationCoreConfig.logChatContent());
        assertFalse(ImitationCoreConfig.bypassChatRestrictions());
        assertTrue(ImitationCoreConfig.mobTargetingEnabled());
        assertEquals(10, ImitationCoreConfig.mobTargetingReconciliationInterval());
        assertEquals(32D, ImitationCoreConfig.mobTargetingReconciliationRange());
    }
}
