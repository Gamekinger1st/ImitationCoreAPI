package com.github.gamekinger1st.imitationcoreapi.internal.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticCategory;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorAction;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorActionResult;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.api.network.CommitImitatorRecordPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ActiveDisguisePayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ClearDisguisePayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ImitatorActionFeedbackPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ImitatorFormLibraryPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.OpenImitatorMenuPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.RequestSessionRevertPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.PersonaChatPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ChatEnvelopePayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ChatProtocolHelloPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.ChatProtocolPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.SendCoreChatPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.RequestImitatorFormLibraryPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.SelectImitatorFormPayload;
import com.github.gamekinger1st.imitationcoreapi.api.network.SessionStatePayload;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelRequest;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatEnvelope;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ClientChatStore;
import com.github.gamekinger1st.imitationcoreapi.internal.chat.ChatClientCapabilities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.Optional;
import java.util.Collection;

public final class ImitationCoreNetwork {
    private static final String PROTOCOL_VERSION = "11";

    private ImitationCoreNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ImitationCoreApi.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToServer(RequestImitatorFormLibraryPayload.TYPE, RequestImitatorFormLibraryPayload.STREAM_CODEC, ImitationCoreNetwork::handleFormLibraryRequest);
        registrar.playToServer(CommitImitatorRecordPayload.TYPE, CommitImitatorRecordPayload.STREAM_CODEC, ImitationCoreNetwork::handleRecordCommit);
        registrar.playToServer(SelectImitatorFormPayload.TYPE, SelectImitatorFormPayload.STREAM_CODEC, ImitationCoreNetwork::handleFormSelection);
        registrar.playToServer(RequestSessionRevertPayload.TYPE, RequestSessionRevertPayload.STREAM_CODEC, ImitationCoreNetwork::handleRevertRequest);
        registrar.playToClient(ActiveDisguisePayload.TYPE, ActiveDisguisePayload.STREAM_CODEC, ImitationCoreNetwork::handleActiveDisguise);
        registrar.playToClient(ClearDisguisePayload.TYPE, ClearDisguisePayload.STREAM_CODEC, ImitationCoreNetwork::handleClearDisguise);
        registrar.playToClient(ImitatorFormLibraryPayload.TYPE, ImitatorFormLibraryPayload.STREAM_CODEC, ImitationCoreNetwork::handleFormLibrary);
        registrar.playToClient(ImitatorActionFeedbackPayload.TYPE, ImitatorActionFeedbackPayload.STREAM_CODEC, ImitationCoreNetwork::handleActionFeedback);
        registrar.playToClient(OpenImitatorMenuPayload.TYPE, OpenImitatorMenuPayload.STREAM_CODEC, ImitationCoreNetwork::handleOpenImitatorMenu);
        registrar.playToClient(SessionStatePayload.TYPE, SessionStatePayload.STREAM_CODEC, ImitationCoreNetwork::handleSessionState);
        registrar.playToClient(PersonaChatPayload.TYPE, PersonaChatPayload.STREAM_CODEC, ImitationCoreNetwork::handlePersonaChat);
        PayloadRegistrar chatRegistrar = event.registrar(ImitationCoreApi.MOD_ID).versioned("1").optional();
        chatRegistrar.playToServer(ChatProtocolHelloPayload.TYPE, ChatProtocolHelloPayload.STREAM_CODEC, ImitationCoreNetwork::handleChatProtocolHello);
        chatRegistrar.playToServer(SendCoreChatPayload.TYPE, SendCoreChatPayload.STREAM_CODEC, ImitationCoreNetwork::handleCoreChat);
        chatRegistrar.playToClient(ChatProtocolPayload.TYPE, ChatProtocolPayload.STREAM_CODEC, ImitationCoreNetwork::handleChatProtocol);
        chatRegistrar.playToClient(ChatEnvelopePayload.TYPE, ChatEnvelopePayload.STREAM_CODEC, ImitationCoreNetwork::handleChatEnvelope);
    }

    public static void advertiseChat(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ChatProtocolPayload(ChatProtocolPayload.CURRENT_PROTOCOL_VERSION, ClientChatStore.MAX_HISTORY_SIZE));
    }

    public static void forgetChatClient(ServerPlayer player) {
        ChatClientCapabilities.clear(player.getUUID());
    }

    public static void deliverChat(Collection<ServerPlayer> recipients, ChatEnvelope envelope) {
        for (ServerPlayer recipient : recipients) {
            if (ChatClientCapabilities.supports(recipient.getUUID(), ChatProtocolPayload.CURRENT_PROTOCOL_VERSION)) {
                PacketDistributor.sendToPlayer(recipient, new ChatEnvelopePayload(envelope));
            } else {
                recipient.sendSystemMessage(envelope.vanillaFallbackComponent());
            }
        }
    }

    public static void syncToTrackingAndSelf(ServerPlayer subject, TransformationSession session) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(subject, SessionStatePayload.from(session));
        if (!session.scope().changesOwnerPresentation()) {
            return;
        }
        activeDisguise(subject, session).ifPresentOrElse(
                payload -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(subject, payload),
                () -> PacketDistributor.sendToPlayersTrackingEntityAndSelf(subject, new ClearDisguisePayload(subject.getId(), subject.getUUID()))
        );
    }

    public static void syncToPlayer(ServerPlayer player, TransformationSession session) {
        PacketDistributor.sendToPlayer(player, SessionStatePayload.from(session));
    }

    public static void syncToPlayer(ServerPlayer player, ServerPlayer subject, TransformationSession session) {
        syncToPlayer(player, session);
        if (!session.scope().changesOwnerPresentation()) {
            return;
        }
        activeDisguise(subject, session).ifPresentOrElse(
                payload -> PacketDistributor.sendToPlayer(player, payload),
                () -> PacketDistributor.sendToPlayer(player, new ClearDisguisePayload(subject.getId(), subject.getUUID()))
        );
    }

    public static void syncFormLibrary(ServerPlayer player) {
        TransformationService transformations = ImitationCoreServices.forServer(player.serverLevel().getServer());
        PacketDistributor.sendToPlayer(player, ImitatorFormLibraryPayload.from(ImitationCoreServices.imitatorHandlers(player).formsFor(player), transformations::snapshot));
    }

    public static void openImitatorMenu(ServerPlayer player, ImitatorMenuRequest request) {
        if (request != ImitatorMenuRequest.NONE) {
            PacketDistributor.sendToPlayer(player, new OpenImitatorMenuPayload(request));
        }
    }

    private static void handleFormLibraryRequest(RequestImitatorFormLibraryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> serverPlayer(context).ifPresent(ImitationCoreNetwork::syncFormLibrary));
    }

    private static void handleRecordCommit(CommitImitatorRecordPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> serverPlayer(context).ifPresent(player -> {
            ImitatorActionResult result = ImitationCoreServices.imitatorSkills(player).commitRecord(player, payload.slot());
            publishAction(player, ImitatorAction.COMMIT_RECORD, result);
        }));
    }

    private static void handleFormSelection(SelectImitatorFormPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> serverPlayer(context).ifPresent(player -> {
            ImitatorActionResult result = ImitationCoreServices.imitatorSkills(player).selectForm(player, payload.slot());
            publishAction(player, ImitatorAction.SELECT_FORM, result);
        }));
    }

    private static void handleRevertRequest(RequestSessionRevertPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
                return;
            }
            TransformationService service = ImitationCoreServices.forServer(level.getServer());
            Optional<TransformationSession> found = service.session(payload.sessionId());
            if (found.isEmpty()) {
                publishAction(player, ImitatorAction.REVERT, false, "The requested transformation session does not exist");
                return;
            }
            TransformationSession session = found.get();
            if (!session.ownerId().equals(player.getUUID()) || session.revision() != payload.expectedRevision()) {
                publishAction(player, ImitatorAction.REVERT, false, "The transformation session changed before reversion could be requested");
                return;
            }
            com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult result = ImitationCoreServices.imitatorHandlers(player)
                    .requestReversion(player, session.sessionId(), TransformationLifecycleReason.FORCE_REVERT);
            result.session().ifPresent(updated -> syncToTrackingAndSelf(player, updated));
            publishAction(player, ImitatorAction.REVERT, result.accepted(), result.accepted() ? "Transformation reversion requested" : result.message());
        });
    }

    private static void handleFormLibrary(ImitatorFormLibraryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImitationApi.imitatorFormLibraries().post(payload));
    }

    private static void handleActiveDisguise(ActiveDisguisePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImitationApi.clientDisguiseStates().postActivated(payload.toState()));
    }

    private static void handleClearDisguise(ClearDisguisePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImitationApi.clientDisguiseStates().postCleared(payload.entityId(), payload.ownerId()));
    }

    private static void handleActionFeedback(ImitatorActionFeedbackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.message().isEmpty()) {
                context.player().displayClientMessage(Component.literal(payload.message()).withStyle(payload.accepted() ? ChatFormatting.GRAY : ChatFormatting.RED), false);
            }
        });
    }

    private static void handleOpenImitatorMenu(OpenImitatorMenuPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImitationApi.imitatorMenus().post(payload.request()));
    }

    private static void handleSessionState(SessionStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ImitationApi.sessionStates().post(payload));
    }

    private static void handleChatProtocolHello(ChatProtocolHelloPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> serverPlayer(context).ifPresent(player -> ChatClientCapabilities.accept(player.getUUID(), payload.protocolVersion())));
    }

    private static void handleCoreChat(SendCoreChatPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> serverPlayer(context).ifPresent(player -> {
            if (!ChatClientCapabilities.supports(player.getUUID(), ChatProtocolPayload.CURRENT_PROTOCOL_VERSION)) {
                String message = "The Imitation Core chat protocol has not been negotiated";
                ImitationDiagnostics.rejected(player, ImitationDiagnosticCategory.CLIENT_PROTOCOL_MISMATCH, message);
                player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
                return;
            }
            ImitationCoreServices.chats(player)
                    .route(new ChatChannelRequest(player, payload.channelId(), payload.message(), payload.targetPlayerId(), ChatMessageSource.CORE_UNSIGNED_PLAYER))
                    .reason()
                    .ifPresent(reason -> player.sendSystemMessage(Component.literal(reason).withStyle(ChatFormatting.RED)));
        }));
    }

    private static void handleChatProtocol(ChatProtocolPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ImitationApi.clientChatProtocol().accept(payload);
            if (ImitationApi.clientChatProtocol().replacementEnabled()) {
                PacketDistributor.sendToServer(new ChatProtocolHelloPayload(payload.protocolVersion()));
            }
        });
    }

    private static void handleChatEnvelope(ChatEnvelopePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ImitationApi.chatMessages().post(payload.envelope());
            context.player().displayClientMessage(payload.envelope().displayComponent(), false);
        });
    }

    private static void handlePersonaChat(PersonaChatPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ImitationApi.personaChatMessages().post(payload);
            context.player().displayClientMessage(payload.displayComponent(), false);
        });
    }

    private static Optional<ServerPlayer> serverPlayer(IPayloadContext context) {
        return context.player() instanceof ServerPlayer player && player.level() instanceof ServerLevel ? Optional.of(player) : Optional.empty();
    }

    private static void publishAction(ServerPlayer player, ImitatorAction action, ImitatorActionResult result) {
        publishAction(player, action, result.accepted(), result.message());
        syncFormLibrary(player);
    }

    private static void publishAction(ServerPlayer player, ImitatorAction action, boolean accepted, String message) {
        if (!accepted) {
            ImitationDiagnostics.rejected(player, message);
        }
        PacketDistributor.sendToPlayer(player, new ImitatorActionFeedbackPayload(action, accepted, message));
    }

    private static Optional<ActiveDisguisePayload> activeDisguise(ServerPlayer subject, TransformationSession session) {
        if (session.state() != com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState.ACTIVE) {
            return Optional.empty();
        }
        if (!session.scope().changesOwnerPresentation()) {
            return Optional.empty();
        }
        return ImitationCoreServices.forServer(subject.serverLevel().getServer())
                .snapshot(session.snapshotId())
                .map(snapshot -> ActiveDisguisePayload.from(subject.getId(), session, snapshot));
    }
}
