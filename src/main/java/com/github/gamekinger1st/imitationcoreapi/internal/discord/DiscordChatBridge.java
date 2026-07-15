package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelKind;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannels;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatEnvelope;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ServerChatDeliveryRegistration;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import net.minecraft.server.MinecraftServer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DiscordChatBridge implements AutoCloseable {
    private static final URI DISCORD_API = URI.create("https://discord.com/api/v10/");
    private static final int MAX_INBOUND_MESSAGES_PER_POLL = 25;
    private static final ConcurrentHashMap<MinecraftServer, DiscordChatBridge> BRIDGES = new ConcurrentHashMap<>();

    private final MinecraftServer server;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final ExecutorService outbound = Executors.newSingleThreadExecutor(runnable -> daemonThread(runnable, "ImitationCore-DiscordOutbound"));
    private final ScheduledExecutorService inbound = Executors.newSingleThreadScheduledExecutor(runnable -> daemonThread(runnable, "ImitationCore-DiscordInbound"));
    private final ServerChatDeliveryRegistration registration;
    private final DiscordBridgeConfig config;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean inboundInitialized;
    private volatile String lastInboundMessageId = "";

    private DiscordChatBridge(MinecraftServer server) {
        this.server = server;
        config = DiscordBridgeConfig.load();
        registration = ImitationApi.serverChatDeliveries().register(this::onServerChatDelivered);
    }

    public static void start(MinecraftServer server) {
        BRIDGES.computeIfAbsent(server, DiscordChatBridge::new).startInbound();
    }

    public static void stop(MinecraftServer server) {
        Optional.ofNullable(BRIDGES.remove(server)).ifPresent(DiscordChatBridge::close);
    }

    private void startInbound() {
        Optional<String> botToken = config.botToken();
        Optional<String> channelId = config.channelId();
        if (botToken.isEmpty() && channelId.isEmpty()) {
            return;
        }
        if (botToken.isEmpty() || channelId.isEmpty()) {
            ImitationCoreApi.LOGGER.warn("Discord inbound chat requires both discord.bot_token and discord.channel_id");
            return;
        }
        inbound.scheduleWithFixedDelay(this::pollInbound, 0L, config.pollIntervalSeconds(), TimeUnit.SECONDS);
        ImitationCoreApi.LOGGER.info("Discord inbound chat bridge enabled for channel {}", channelId.get());
    }

    private void onServerChatDelivered(ChatEnvelope envelope) {
        if (closed.get() || envelope.source() == ChatMessageSource.DISCORD_BRIDGE || !shouldRelay(envelope)) {
            return;
        }
        config.webhookUri().ifPresent(webhookUri -> {
            try {
                outbound.execute(() -> postWebhook(webhookUri, DiscordWebhookPayload.json(DiscordWebhookPayload.format(envelope))));
            } catch (RejectedExecutionException ignored) {
            }
        });
    }

    private boolean shouldRelay(ChatEnvelope envelope) {
        if (envelope.source() == ChatMessageSource.SERVER_SYSTEM) {
            return config.relaySystemMessages();
        }
        return switch (envelope.channelKind()) {
            case GLOBAL -> true;
            case LOCAL -> config.relayLocalMessages();
            case DIRECT -> config.relayDirectMessages();
            default -> false;
        };
    }

    private void postWebhook(URI webhookUri, String payload) {
        for (int attempt = 0; attempt < 2 && !closed.get(); attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(webhookUri)
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return;
                }
                if (response.statusCode() == 429 && attempt == 0 && waitForRetry(response)) {
                    continue;
                }
                ImitationCoreApi.LOGGER.warn("Discord webhook rejected a chat message with HTTP {}", response.statusCode());
                return;
            } catch (Exception exception) {
                if (!closed.get()) {
                    ImitationCoreApi.LOGGER.warn("Discord webhook delivery failed: {}", exception.getClass().getSimpleName());
                }
                return;
            }
        }
    }

    private boolean waitForRetry(HttpResponse<?> response) {
        Optional<String> retryAfter = response.headers().firstValue("Retry-After");
        if (retryAfter.isEmpty()) {
            return false;
        }
        try {
            long delayMillis = Math.max(1L, Math.round(Double.parseDouble(retryAfter.get()) * 1_000.0D));
            Thread.sleep(delayMillis);
            return !closed.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void pollInbound() {
        if (closed.get()) {
            return;
        }
        Optional<String> botToken = config.botToken();
        Optional<String> channelId = config.channelId();
        if (botToken.isEmpty() || channelId.isEmpty()) {
            return;
        }
        try {
            URI endpoint = channelMessagesEndpoint(channelId.get());
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bot " + normalizedToken(botToken.get()))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ImitationCoreApi.LOGGER.warn("Discord inbound chat poll failed with HTTP {}", response.statusCode());
                return;
            }
            List<DiscordInboundMessage> messages = DiscordMessageParser.parseChannelMessages(response.body(), channelId.get());
            Optional<String> latestMessageId = DiscordMessageParser.latestMessageId(response.body());
            if (!inboundInitialized) {
                latestMessageId.ifPresent(messageId -> lastInboundMessageId = messageId);
                inboundInitialized = true;
                return;
            }
            int processed = 0;
            for (DiscordInboundMessage message : messages) {
                if (!isNewer(message.messageId(), lastInboundMessageId)) {
                    continue;
                }
                relayInbound(message);
                lastInboundMessageId = message.messageId();
                if (++processed >= MAX_INBOUND_MESSAGES_PER_POLL) {
                    return;
                }
            }
            latestMessageId.filter(messageId -> isNewer(messageId, lastInboundMessageId)).ifPresent(messageId -> lastInboundMessageId = messageId);
        } catch (Exception exception) {
            if (!closed.get()) {
                ImitationCoreApi.LOGGER.warn("Discord inbound chat poll failed: {}", exception.getClass().getSimpleName());
            }
        }
    }

    private URI channelMessagesEndpoint(String channelId) {
        String query = lastInboundMessageId.isBlank() ? "?limit=100" : "?after=" + lastInboundMessageId + "&limit=100";
        return DISCORD_API.resolve("channels/" + channelId + "/messages" + query);
    }

    private void relayInbound(DiscordInboundMessage message) {
        String content = boundedChatText(message.content());
        if (content.isEmpty()) {
            return;
        }
        UUID senderId = UUID.nameUUIDFromBytes(("discord:" + message.authorId()).getBytes(StandardCharsets.UTF_8));
        String senderName = boundedSenderName("Discord: " + message.authorName());
        server.execute(() -> {
            if (!closed.get()) {
                ImitationCoreServices.chats(server).sendExternal(
                        ChatChannels.GLOBAL,
                        ChatChannelKind.GLOBAL,
                        ChatMessageSource.DISCORD_BRIDGE,
                        senderId,
                        senderName,
                        content,
                        server.getPlayerList().getPlayers()
                );
            }
        });
    }

    private static boolean isNewer(String candidate, String reference) {
        return reference.isBlank() || Long.compareUnsigned(Long.parseUnsignedLong(candidate), Long.parseUnsignedLong(reference)) > 0;
    }

    private static String normalizedToken(String token) {
        return token.startsWith("Bot ") ? token.substring("Bot ".length()).strip() : token;
    }

    private static String boundedChatText(String value) {
        String normalized = value.replaceAll("[\\p{Cntrl}\\r\\n]+", " ").strip();
        if (normalized.length() <= ChatEnvelope.MAX_MESSAGE_LENGTH) {
            return normalized;
        }
        return truncate(normalized, ChatEnvelope.MAX_MESSAGE_LENGTH - 1).stripTrailing() + "\u2026";
    }

    private static String boundedSenderName(String value) {
        String normalized = value.replaceAll("[\\p{Cntrl}\\r\\n]+", " ").strip();
        if (normalized.length() <= ChatEnvelope.MAX_SENDER_NAME_LENGTH) {
            return normalized;
        }
        return truncate(normalized, ChatEnvelope.MAX_SENDER_NAME_LENGTH).stripTrailing();
    }

    private static String truncate(String value, int maximumLength) {
        int end = maximumLength;
        if (end < value.length() && Character.isHighSurrogate(value.charAt(end - 1)) && Character.isLowSurrogate(value.charAt(end))) {
            end--;
        }
        return value.substring(0, end);
    }

    private static Thread daemonThread(Runnable runnable, String name) {
        Thread thread = new Thread(runnable, name);
        thread.setDaemon(true);
        return thread;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        registration.close();
        inbound.shutdownNow();
        outbound.shutdownNow();
    }
}
