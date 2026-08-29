package com.github.gamekinger1st.imitationcoreapi.api.chat;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ChatService {
    private final MinecraftServer server;
    private final ChatRateLimiter rateLimiter;
    private final ChatChannelPreferenceRepository preferences;
    private final Clock clock;
    private final Supplier<ResourceLocation> defaultChannel;
    private final ChatDeliveryTransport transport;

    public ChatService(MinecraftServer server, ChatChannelPreferenceRepository preferences, ChatRateLimiter rateLimiter, Clock clock, Supplier<ResourceLocation> defaultChannel, ChatDeliveryTransport transport) {
        this.server = Objects.requireNonNull(server, "server");
        this.preferences = Objects.requireNonNull(preferences, "preferences");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.defaultChannel = Objects.requireNonNull(defaultChannel, "defaultChannel");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public ResourceLocation activeChannel(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return preferences.activeChatChannel(player.getUUID())
                .filter(this::isSelectable)
                .orElseGet(this::defaultActiveChannel);
    }

    public ChatChannelSelectionResult selectChannel(ServerPlayer player, ResourceLocation channelId) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(channelId, "channelId");
        if (!isSelectable(channelId)) {
            return ChatChannelSelectionResult.rejected("That chat channel cannot be selected as an active channel");
        }
        preferences.saveActiveChatChannel(player.getUUID(), channelId);
        return ChatChannelSelectionResult.selected(channelId);
    }

    public ChatDeliveryResult route(ChatChannelRequest request) {
        Objects.requireNonNull(request, "request");
        if (!rateLimiter.tryAcquire(request.sender().getUUID(), clock.millis())) {
            return reject(request, "You are sending chat messages too quickly");
        }
        Optional<ChatDelivery> delivery = ImitationApi.chatChannels().route(request);
        if (delivery.isEmpty() || delivery.get().recipients().isEmpty()) {
            return reject(request, "That chat channel is unavailable or has no recipients");
        }
        PersonaChatDecision personaDecision = ImitationApi.personaChats().resolve(request.sender(), request.message());
        if (personaDecision.disposition() == PersonaChatDisposition.BLOCK) {
            audit(UUID.randomUUID(), Instant.now(clock), request, Optional.empty(), delivery.get().recipients(), false, personaDecision.reason());
            return ChatDeliveryResult.rejected(personaDecision.reason().orElse("This chat message was blocked"));
        }
        Optional<PersonaIdentity> persona = personaDecision.persona();
        ChatModerationDecision moderation = ImitationApi.chatModeration().evaluate(new ChatModerationContext(request, delivery.get(), persona));
        if (!moderation.allowed()) {
            audit(UUID.randomUUID(), Instant.now(clock), request, persona, delivery.get().recipients(), false, moderation.reason());
            return ChatDeliveryResult.rejected(moderation.reason().orElse("This chat message was blocked"));
        }
        ChatEnvelope envelope = new ChatEnvelope(
                UUID.randomUUID(),
                Instant.now(clock),
                request.channelId(),
                delivery.get().channelKind(),
                request.source(),
                Optional.of(request.sender().getUUID()),
                Optional.of(request.sender().getGameProfile().getName()),
                persona,
                request.message()
        );
        transport.deliver(delivery.get().recipients(), envelope);
        audit(envelope.messageId(), envelope.sentAt(), request, persona, delivery.get().recipients(), true, Optional.empty());
        ImitationApi.serverChatDeliveries().post(envelope);
        return ChatDeliveryResult.accepted(envelope);
    }

    public ChatEnvelope sendSystem(ResourceLocation channelId, ChatChannelKind kind, String message, Collection<ServerPlayer> recipients) {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(recipients, "recipients");
        ChatEnvelope envelope = new ChatEnvelope(UUID.randomUUID(), Instant.now(clock), channelId, kind, ChatMessageSource.SERVER_SYSTEM, Optional.empty(), Optional.empty(), Optional.empty(), message);
        transport.deliver(recipients, envelope);
        ImitationApi.chatAudit().post(new ChatAuditEntry(envelope.messageId(), envelope.sentAt(), channelId, ChatMessageSource.SERVER_SYSTEM, Optional.empty(), Optional.empty(), Optional.empty(), recipients.stream().map(ServerPlayer::getUUID).toList(), true, Optional.empty()));
        ImitationApi.serverChatDeliveries().post(envelope);
        return envelope;
    }

    public ChatEnvelope sendExternal(ResourceLocation channelId, ChatChannelKind kind, ChatMessageSource source, UUID senderId, String senderName, String message, Collection<ServerPlayer> recipients) {
        Objects.requireNonNull(channelId, "channelId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(senderId, "senderId");
        Objects.requireNonNull(senderName, "senderName");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(recipients, "recipients");
        if (source != ChatMessageSource.DISCORD_BRIDGE) {
            throw new IllegalArgumentException("External messages must use an explicit external message source");
        }
        ChatEnvelope envelope = new ChatEnvelope(UUID.randomUUID(), Instant.now(clock), channelId, kind, source, Optional.of(senderId), Optional.of(senderName), Optional.empty(), message);
        transport.deliver(recipients, envelope);
        ImitationApi.chatAudit().post(new ChatAuditEntry(envelope.messageId(), envelope.sentAt(), channelId, source, Optional.of(senderId), Optional.of(senderName), Optional.empty(), recipients.stream().map(ServerPlayer::getUUID).toList(), true, Optional.empty()));
        ImitationApi.serverChatDeliveries().post(envelope);
        return envelope;
    }

    public void clearPlayer(UUID playerId) {
        rateLimiter.clear(playerId);
    }

    private ResourceLocation defaultActiveChannel() {
        ResourceLocation configured = Objects.requireNonNull(defaultChannel.get(), "defaultChannel.get()");
        return isSelectable(configured) ? configured : ChatChannels.GLOBAL;
    }

    private boolean isSelectable(ResourceLocation channelId) {
        Optional<ChatChannelProvider> provider = ImitationApi.chatChannels().provider(channelId);
        if (provider.isEmpty()) {
            return false;
        }
        try {
            return provider.get().acceptsPlayerMessages() && provider.get().supportsActiveSelection();
        } catch (RuntimeException | LinkageError exception) {
            ImitationCoreApi.LOGGER.error("Chat channel provider {} failed during selection", provider.get().id(), exception);
            return false;
        }
    }

    private ChatDeliveryResult reject(ChatChannelRequest request, String reason) {
        audit(UUID.randomUUID(), Instant.now(clock), request, Optional.empty(), java.util.List.of(), false, Optional.of(reason));
        return ChatDeliveryResult.rejected(reason);
    }

    private void audit(UUID messageId, Instant timestamp, ChatChannelRequest request, Optional<PersonaIdentity> persona, Collection<ServerPlayer> recipients, boolean delivered, Optional<String> moderationReason) {
        ImitationApi.chatAudit().post(new ChatAuditEntry(
                messageId,
                timestamp,
                request.channelId(),
                request.source(),
                Optional.of(request.sender().getUUID()),
                Optional.of(request.sender().getGameProfile().getName()),
                persona,
                recipients.stream().map(ServerPlayer::getUUID).toList(),
                delivered,
                moderationReason
        ));
    }
}
