package com.github.gamekinger1st.imitationcoreapi.api.service;

import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorHandlerService;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorSkillController;
import com.github.gamekinger1st.imitationcoreapi.api.skill.OwnerSkillSuppressionService;
import com.github.gamekinger1st.imitationcoreapi.api.skill.TemporarySkillService;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatService;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

public final class ImitationCoreServices {
    private static volatile ImitationCoreServiceProvider provider;

    private ImitationCoreServices() {
    }

    public static synchronized void initialize(ImitationCoreServiceProvider serviceProvider) {
        Objects.requireNonNull(serviceProvider, "serviceProvider");
        if (provider != null) {
            throw new IllegalStateException("Imitation Core services are already initialized");
        }
        provider = serviceProvider;
    }

    public static TransformationService forServer(MinecraftServer server) {
        return provider().transformations(Objects.requireNonNull(server, "server"));
    }

    public static ImitatorHandlerService imitatorHandlers(MinecraftServer server) {
        return provider().imitatorHandlers(Objects.requireNonNull(server, "server"));
    }

    public static TransformationApplicationService applications(MinecraftServer server) {
        return provider().applications(Objects.requireNonNull(server, "server"));
    }

    public static ImitatorHandlerService imitatorHandlers(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return provider().imitatorHandlers(player);
    }

    public static ImitatorSkillController imitatorSkills(MinecraftServer server) {
        return new ImitatorSkillController(imitatorHandlers(Objects.requireNonNull(server, "server")), provider().imitatorSynchronizer());
    }

    public static ImitatorSkillController imitatorSkills(ServerPlayer player) {
        return new ImitatorSkillController(imitatorHandlers(Objects.requireNonNull(player, "player")), provider().imitatorSynchronizer());
    }

    public static TemporarySkillService temporarySkills(MinecraftServer server) {
        return new TemporarySkillService(forServer(Objects.requireNonNull(server, "server")));
    }

    public static TemporarySkillService temporarySkills(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!(player.level() instanceof ServerLevel level)) {
            throw new IllegalStateException("Temporary skills are only available on the server");
        }
        return temporarySkills(level.getServer());
    }

    public static OwnerSkillSuppressionService ownerSkillSuppressions(MinecraftServer server) {
        return new OwnerSkillSuppressionService(forServer(Objects.requireNonNull(server, "server")));
    }

    public static OwnerSkillSuppressionService ownerSkillSuppressions(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!(player.level() instanceof ServerLevel level)) {
            throw new IllegalStateException("Owner skill suppression is only available on the server");
        }
        return ownerSkillSuppressions(level.getServer());
    }

    public static ChatService chats(MinecraftServer server) {
        return provider().chats(Objects.requireNonNull(server, "server"));
    }

    public static ChatService chats(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return provider().chats(player);
    }

    public static void release(MinecraftServer server) {
        provider().release(Objects.requireNonNull(server, "server"));
    }

    private static ImitationCoreServiceProvider provider() {
        ImitationCoreServiceProvider current = provider;
        if (current == null) {
            throw new IllegalStateException("Imitation Core services are not initialized");
        }
        return current;
    }
}
