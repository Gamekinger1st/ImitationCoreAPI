package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

record DiscordBridgeConfig(Optional<URI> webhookUri, Optional<String> botToken, Optional<String> channelId, int pollIntervalSeconds, boolean relayLocalMessages, boolean relayDirectMessages, boolean relaySystemMessages) {
    static final String FILE_NAME = "imitationcoreapi-discord.properties";
    static final String WEBHOOK_ENVIRONMENT = "IMITATIONCOREAPI_DISCORD_WEBHOOK_URL";
    static final String BOT_TOKEN_ENVIRONMENT = "IMITATIONCOREAPI_DISCORD_BOT_TOKEN";
    static final String CHANNEL_ID_ENVIRONMENT = "IMITATIONCOREAPI_DISCORD_CHANNEL_ID";
    private static final String DEFAULT_CONTENT = """
            webhook_url=
            bot_token=
            channel_id=
            poll_interval_seconds=3
            relay_local_messages=false
            relay_direct_messages=false
            relay_system_messages=true
            """;

    DiscordBridgeConfig {
        Objects.requireNonNull(webhookUri, "webhookUri");
        Objects.requireNonNull(botToken, "botToken");
        Objects.requireNonNull(channelId, "channelId");
        pollIntervalSeconds = Math.max(1, Math.min(60, pollIntervalSeconds));
    }

    static DiscordBridgeConfig load() {
        return load(FMLPaths.CONFIGDIR.get().resolve(FILE_NAME));
    }

    static DiscordBridgeConfig load(Path path) {
        Properties properties = new Properties();
        try {
            if (Files.notExists(path)) {
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(path, DEFAULT_CONTENT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            }
        } catch (IOException exception) {
            ImitationCoreApi.LOGGER.warn("Could not load the Discord bridge configuration at {}", path);
            return defaults();
        }
        return new DiscordBridgeConfig(
                webhookUri(secret(properties, "webhook_url", WEBHOOK_ENVIRONMENT)),
                secret(properties, "bot_token", BOT_TOKEN_ENVIRONMENT),
                secret(properties, "channel_id", CHANNEL_ID_ENVIRONMENT).filter(DiscordBridgeConfig::isSnowflake),
                integer(properties, "poll_interval_seconds", 3),
                bool(properties, "relay_local_messages", false),
                bool(properties, "relay_direct_messages", false),
                bool(properties, "relay_system_messages", true)
        );
    }

    private static DiscordBridgeConfig defaults() {
        return new DiscordBridgeConfig(Optional.empty(), Optional.empty(), Optional.empty(), 3, false, false, true);
    }

    private static Optional<URI> webhookUri(Optional<String> configured) {
        if (configured.isEmpty()) {
            return Optional.empty();
        }
        try {
            URI uri = new URI(configured.get());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || !(host.equals("discord.com") || host.endsWith(".discord.com"))) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (URISyntaxException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> value(Properties properties, String key) {
        return Optional.ofNullable(properties.getProperty(key)).map(String::strip).filter(configured -> !configured.isEmpty());
    }

    private static Optional<String> secret(Properties properties, String key, String environmentKey) {
        try {
            Optional<String> environmentValue = Optional.ofNullable(System.getenv(environmentKey)).map(String::strip).filter(configured -> !configured.isEmpty());
            if (environmentValue.isPresent()) {
                return environmentValue;
            }
        } catch (SecurityException ignored) {
        }
        return value(properties, key);
    }

    private static int integer(Properties properties, String key, int fallback) {
        try {
            return value(properties, key).map(Integer::parseInt).orElse(fallback);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static boolean bool(Properties properties, String key, boolean fallback) {
        return value(properties, key).map(Boolean::parseBoolean).orElse(fallback);
    }

    private static boolean isSnowflake(String value) {
        if (value.isEmpty() || value.length() > 20 || !value.chars().allMatch(Character::isDigit)) {
            return false;
        }
        try {
            Long.parseUnsignedLong(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
