package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscordBridgeConfigTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void createsAServerLocalTemplateWithoutCredentials() {
        Path path = temporaryDirectory.resolve(DiscordBridgeConfig.FILE_NAME);

        DiscordBridgeConfig config = DiscordBridgeConfig.load(path);

        assertTrue(Files.isRegularFile(path));
        assertTrue(config.webhookUri().isEmpty());
        assertTrue(config.botToken().isEmpty());
        assertTrue(config.channelId().isEmpty());
    }

    @Test
    void readsValidatedWebhookAndInboundBotSettings() throws IOException {
        Path path = temporaryDirectory.resolve(DiscordBridgeConfig.FILE_NAME);
        Files.writeString(path, """
                webhook_url=https://discord.com/api/webhooks/example-id/example-token
                bot_token=bot-token
                channel_id=1234567890
                poll_interval_seconds=999
                relay_local_messages=true
                relay_direct_messages=true
                relay_system_messages=false
                """);

        DiscordBridgeConfig config = DiscordBridgeConfig.load(path);

        assertEquals("https://discord.com/api/webhooks/example-id/example-token", config.webhookUri().orElseThrow().toString());
        assertEquals("bot-token", config.botToken().orElseThrow());
        assertEquals("1234567890", config.channelId().orElseThrow());
        assertEquals(60, config.pollIntervalSeconds());
        assertTrue(config.relayLocalMessages());
        assertTrue(config.relayDirectMessages());
        assertFalse(config.relaySystemMessages());
    }
}
