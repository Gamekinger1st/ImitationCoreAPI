package com.github.gamekinger1st.imitationcoreapi.api;

import com.github.gamekinger1st.imitationcoreapi.api.adapter.ImitationAdapterRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseStateBus;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseStore;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAnimationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguisePresentationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseRenderRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticBus;
import com.github.gamekinger1st.imitationcoreapi.api.event.TransformationEventBus;
import com.github.gamekinger1st.imitationcoreapi.api.network.SessionStateBus;
import com.github.gamekinger1st.imitationcoreapi.api.network.ImitatorFormLibraryBus;
import com.github.gamekinger1st.imitationcoreapi.api.network.ClientImitatorFormLibraryStore;
import com.github.gamekinger1st.imitationcoreapi.api.network.ImitatorMenuBus;
import com.github.gamekinger1st.imitationcoreapi.api.race.RaceEditRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaChatBus;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaChatRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatAuditBus;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageBus;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatModerationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ClientChatStore;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ClientChatProtocolState;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ServerChatDeliveryBus;
import com.github.gamekinger1st.imitationcoreapi.api.gecko.GeckoAnimationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillBridgeRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.skill.SkillClassificationOverrideRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormAbilityRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorIntegrationRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraStateBridgeRegistry;
import com.github.gamekinger1st.imitationcoreapi.api.targeting.MobFactionRegistry;

public final class ImitationApi {
    private static final ImitationAdapterRegistry ADAPTERS = new ImitationAdapterRegistry();
    private static final TransformationApplicationRegistry TRANSFORMATION_APPLICATIONS = new TransformationApplicationRegistry();
    private static final ClientDisguiseStateBus CLIENT_DISGUISE_STATES = new ClientDisguiseStateBus();
    private static final ClientDisguiseStore CLIENT_DISGUISE_STORE = new ClientDisguiseStore();
    private static final DisguiseAnimationRegistry DISGUISE_ANIMATIONS = new DisguiseAnimationRegistry();
    private static final DisguisePresentationRegistry DISGUISE_PRESENTATIONS = new DisguisePresentationRegistry();
    private static final DisguiseRenderRegistry DISGUISE_RENDERERS = new DisguiseRenderRegistry();
    private static final ImitationDiagnosticBus DIAGNOSTICS = new ImitationDiagnosticBus();
    private static final TransformationEventBus EVENTS = new TransformationEventBus();
    private static final SessionStateBus SESSION_STATES = new SessionStateBus();
    private static final ImitatorFormLibraryBus IMITATOR_FORM_LIBRARIES = new ImitatorFormLibraryBus();
    private static final ClientImitatorFormLibraryStore CLIENT_IMITATOR_FORM_LIBRARY = new ClientImitatorFormLibraryStore();
    private static final ImitatorMenuBus IMITATOR_MENUS = new ImitatorMenuBus();
    private static final PersonaChatRegistry PERSONA_CHAT = new PersonaChatRegistry();
    private static final PersonaChatBus PERSONA_CHAT_MESSAGES = new PersonaChatBus();
    private static final ChatChannelRegistry CHAT_CHANNELS = new ChatChannelRegistry();
    private static final ChatModerationRegistry CHAT_MODERATION = new ChatModerationRegistry();
    private static final ChatAuditBus CHAT_AUDIT = new ChatAuditBus();
    private static final ChatMessageBus CHAT_MESSAGES = new ChatMessageBus();
    private static final ServerChatDeliveryBus SERVER_CHAT_DELIVERIES = new ServerChatDeliveryBus();
    private static final ClientChatStore CLIENT_CHAT_STORE = new ClientChatStore();
    private static final ClientChatProtocolState CLIENT_CHAT_PROTOCOL = new ClientChatProtocolState();
    private static final GeckoAnimationRegistry GECKO_ANIMATIONS = new GeckoAnimationRegistry();
    private static final SkillBridgeRegistry SKILL_BRIDGES = new SkillBridgeRegistry();
    private static final SkillClassificationOverrideRegistry SKILL_CLASSIFICATIONS = new SkillClassificationOverrideRegistry();
    private static final ImitatorFormAbilityRegistry IMITATOR_FORM_ABILITIES = new ImitatorFormAbilityRegistry();
    private static final ImitatorIntegrationRegistry IMITATOR_INTEGRATIONS = new ImitatorIntegrationRegistry();
    private static final TensuraStateBridgeRegistry TENSURA_STATES = new TensuraStateBridgeRegistry();
    private static final RaceEditRegistry RACE_EDITS = new RaceEditRegistry();
    private static final MobFactionRegistry MOB_FACTIONS = new MobFactionRegistry();

    private ImitationApi() {
    }

    static {
        CLIENT_DISGUISE_STATES.register(CLIENT_DISGUISE_STORE);
        CHAT_MESSAGES.register(CLIENT_CHAT_STORE);
        IMITATOR_FORM_LIBRARIES.register(CLIENT_IMITATOR_FORM_LIBRARY);
    }

    public static ApiVersion version() {
        return ApiVersion.CURRENT;
    }

    public static ImitationAdapterRegistry adapters() {
        return ADAPTERS;
    }

    public static TransformationApplicationRegistry transformationApplications() {
        return TRANSFORMATION_APPLICATIONS;
    }

    public static ClientDisguiseStateBus clientDisguiseStates() {
        return CLIENT_DISGUISE_STATES;
    }

    public static ClientDisguiseStore clientDisguiseStore() {
        return CLIENT_DISGUISE_STORE;
    }

    public static DisguiseAnimationRegistry disguiseAnimations() {
        return DISGUISE_ANIMATIONS;
    }

    public static DisguisePresentationRegistry disguisePresentations() {
        return DISGUISE_PRESENTATIONS;
    }

    public static DisguiseRenderRegistry disguiseRenderers() {
        return DISGUISE_RENDERERS;
    }

    public static ImitationDiagnosticBus diagnostics() {
        return DIAGNOSTICS;
    }

    public static TransformationEventBus events() {
        return EVENTS;
    }

    public static SessionStateBus sessionStates() {
        return SESSION_STATES;
    }

    public static ImitatorFormLibraryBus imitatorFormLibraries() {
        return IMITATOR_FORM_LIBRARIES;
    }

    public static ClientImitatorFormLibraryStore clientImitatorFormLibrary() {
        return CLIENT_IMITATOR_FORM_LIBRARY;
    }

    public static ImitatorMenuBus imitatorMenus() {
        return IMITATOR_MENUS;
    }

    public static PersonaChatRegistry personaChats() {
        return PERSONA_CHAT;
    }

    public static PersonaChatBus personaChatMessages() {
        return PERSONA_CHAT_MESSAGES;
    }

    public static ChatChannelRegistry chatChannels() {
        return CHAT_CHANNELS;
    }

    public static ChatModerationRegistry chatModeration() {
        return CHAT_MODERATION;
    }

    public static ChatAuditBus chatAudit() {
        return CHAT_AUDIT;
    }

    public static ChatMessageBus chatMessages() {
        return CHAT_MESSAGES;
    }

    public static ServerChatDeliveryBus serverChatDeliveries() {
        return SERVER_CHAT_DELIVERIES;
    }

    public static ClientChatStore clientChatStore() {
        return CLIENT_CHAT_STORE;
    }

    public static ClientChatProtocolState clientChatProtocol() {
        return CLIENT_CHAT_PROTOCOL;
    }

    public static GeckoAnimationRegistry geckoAnimations() {
        return GECKO_ANIMATIONS;
    }

    public static SkillBridgeRegistry skillBridges() {
        return SKILL_BRIDGES;
    }

    public static SkillClassificationOverrideRegistry skillClassifications() {
        return SKILL_CLASSIFICATIONS;
    }

    public static ImitatorFormAbilityRegistry imitatorFormAbilities() {
        return IMITATOR_FORM_ABILITIES;
    }

    public static ImitatorIntegrationRegistry imitatorIntegrations() {
        return IMITATOR_INTEGRATIONS;
    }

    public static TensuraStateBridgeRegistry tensuraStates() {
        return TENSURA_STATES;
    }

    public static RaceEditRegistry raceEdits() {
        return RACE_EDITS;
    }

    public static MobFactionRegistry mobFactions() {
        return MOB_FACTIONS;
    }
}
