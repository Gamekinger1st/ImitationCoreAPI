package com.github.gamekinger1st.imitationcoreapi.internal.service;

import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationService;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatRateLimiter;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatService;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorFormService;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSynchronizer;
import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServiceProvider;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import com.github.gamekinger1st.imitationcoreapi.internal.network.ImitationCoreNetwork;
import com.github.gamekinger1st.imitationcoreapi.internal.persistence.ImitationCoreSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.time.Clock;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class DefaultImitationCoreServiceProvider implements ImitationCoreServiceProvider {
    private static final ImitatorSynchronizer SYNCHRONIZER = new ImitatorSynchronizer() {
        @Override
        public void syncFormLibrary(ServerPlayer player) {
            ImitationCoreNetwork.syncFormLibrary(player);
        }

        @Override
        public void syncSession(ServerPlayer player, com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession session) {
            ImitationCoreNetwork.syncToTrackingAndSelf(player, session);
        }

        @Override
        public void openMenu(ServerPlayer player, com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorMenuRequest request) {
            ImitationCoreNetwork.openImitatorMenu(player, request);
        }
    };

    private final Map<MinecraftServer, TransformationService> transformations = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<MinecraftServer, TransformationApplicationService> applications = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<MinecraftServer, ImitatorHandlerService> imitatorHandlers = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<MinecraftServer, ChatService> chats = Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public TransformationService transformations(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (transformations) {
            return transformations.computeIfAbsent(server, value -> new TransformationService(ImitationCoreSavedData.get(value)));
        }
    }

    @Override
    public TransformationApplicationService applications(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (applications) {
            return applications.computeIfAbsent(server, value -> new TransformationApplicationService(value, transformations(value), com.github.gamekinger1st.imitationcoreapi.api.ImitationApi.transformationApplications()));
        }
    }

    @Override
    public ImitatorHandlerService imitatorHandlers(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (imitatorHandlers) {
            return imitatorHandlers.computeIfAbsent(server, value -> new ImitatorHandlerService(transformations(value), new ImitatorFormService(ImitationCoreSavedData.get(value)), applications(value)));
        }
    }

    @Override
    public ChatService chats(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (chats) {
            return chats.computeIfAbsent(server, value -> new ChatService(value, ImitationCoreSavedData.get(value), new ChatRateLimiter(), Clock.systemUTC(), ImitationCoreConfig::defaultChatChannel, ImitationCoreNetwork::deliverChat));
        }
    }

    @Override
    public ImitatorSynchronizer imitatorSynchronizer() {
        return SYNCHRONIZER;
    }

    @Override
    public void release(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (transformations) {
            transformations.remove(server);
        }
        synchronized (imitatorHandlers) {
            imitatorHandlers.remove(server);
        }
        synchronized (applications) {
            applications.remove(server);
        }
        synchronized (chats) {
            chats.remove(server);
        }
    }
}
