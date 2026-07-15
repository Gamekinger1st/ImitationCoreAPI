package com.github.gamekinger1st.imitationcoreapi.internal.server;

import com.github.gamekinger1st.imitationcoreapi.api.service.ImitationCoreServices;
import com.github.gamekinger1st.imitationcoreapi.api.application.TransformationApplicationService;
import com.github.gamekinger1st.imitationcoreapi.api.service.TransformationService;
import com.github.gamekinger1st.imitationcoreapi.api.service.SessionTransitionResult;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.internal.network.ImitationCoreNetwork;
import com.github.gamekinger1st.imitationcoreapi.internal.discord.DiscordChatBridge;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationLifecycleReason;
import com.github.gamekinger1st.imitationcoreapi.api.targeting.MobImitationTargetingService;
import com.github.gamekinger1st.imitationcoreapi.api.replica.ReplicaEntityTags;
import com.github.gamekinger1st.imitationcoreapi.internal.config.ImitationCoreConfig;
import com.github.gamekinger1st.imitationcoreapi.internal.replica.ImitatorReplicaApplicationAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class ImitationCoreServerEvents {
    private static boolean registered;

    private ImitationCoreServerEvents() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onServerStopping);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onPlayerLogout);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onPlayerLogin);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onDimensionChange);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onStartTracking);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onMobChangeTarget);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ImitationCoreServerEvents::onTransformedPlayerLethalDamage);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ImitationCoreServerEvents::onTransformedPlayerDeath);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onReplicaDeath);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onReplicaDrops);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onReplicaExperience);
        NeoForge.EVENT_BUS.addListener(ImitationCoreServerEvents::onPlayerTick);
        registered = true;
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ImitationCoreServices.applications(server).recoverInterruptedSessions(server.overworld().getGameTime());
        DiscordChatBridge.start(server);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        ImitationCoreServices.applications(server).requestReversionForAll(TransformationLifecycleReason.SERVER_STOPPING, server.overworld().getGameTime());
        DiscordChatBridge.stop(server);
        ImitationCoreServices.release(server);
    }

    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        serverPlayer(event.getEntity()).ifPresent(player -> {
            ImitationCoreNetwork.forgetChatClient(player);
            ImitationCoreServices.chats(player).clearPlayer(player.getUUID());
        });
        applications(event.getEntity()).ifPresent(service -> {
            java.util.List<SessionTransitionResult> results = service.requestReversionForOwner(serverPlayer(event.getEntity()), event.getEntity().getUUID(), TransformationLifecycleReason.LOGOUT, gameTime(event.getEntity()));
            syncLifecycleResults(serverPlayer(event.getEntity()), results);
        });
    }

    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        serverPlayer(event.getEntity()).ifPresent(ImitationCoreNetwork::advertiseChat);
        applications(event.getEntity()).ifPresent(service -> {
            service.requestReversionForOwner(serverPlayer(event.getEntity()), event.getEntity().getUUID(), TransformationLifecycleReason.RECONNECT, gameTime(event.getEntity()));
            serverPlayer(event.getEntity()).ifPresent(ImitationCoreServerEvents::syncActiveDisguises);
        });
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        TransformationLifecycleReason reason = event.isWasDeath() ? TransformationLifecycleReason.DEATH : TransformationLifecycleReason.CLONE;
        applications(event.getEntity()).ifPresent(service -> {
            java.util.List<SessionTransitionResult> results = service.requestReversionForOwner(serverPlayer(event.getEntity()), event.getOriginal().getUUID(), reason, gameTime(event.getEntity()));
            syncLifecycleResults(serverPlayer(event.getEntity()), results);
        });
    }

    private static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        applications(event.getEntity()).ifPresent(service -> {
            java.util.List<SessionTransitionResult> results = service.requestReversionForOwner(serverPlayer(event.getEntity()), event.getEntity().getUUID(), TransformationLifecycleReason.DIMENSION_CHANGE, gameTime(event.getEntity()));
            syncLifecycleResults(serverPlayer(event.getEntity()), results);
        });
    }

    private static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer tracker) || !(event.getTarget() instanceof ServerPlayer subject)) {
            return;
        }
        ImitationCoreServices.forServer(tracker.serverLevel().getServer())
                .activeSessionForOwner(subject.getUUID())
                .ifPresent(session -> ImitationCoreNetwork.syncToPlayer(tracker, subject, session));
    }

    private static void onMobChangeTarget(LivingChangeTargetEvent event) {
        if (!ImitationCoreConfig.mobTargetingEnabled()
                || !(event.getEntity() instanceof Mob mob)
                || !(event.getNewAboutToBeSetTarget() instanceof ServerPlayer target)) {
            return;
        }
        MobImitationTargetingService targeting = new MobImitationTargetingService(ImitationCoreServices.forServer(target.serverLevel().getServer()));
        if (targeting.shouldSuppress(mob, target)) {
            event.setNewAboutToBeSetTarget(null);
            targeting.clearSuppressedTarget(mob, target);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ImitationCoreServices.imitatorSkills(player).tickFormTraits(player);
            reconcileReplicas(player);
        }
        if (!ImitationCoreConfig.mobTargetingEnabled()
                || !(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % ImitationCoreConfig.mobTargetingReconciliationInterval() != 0) {
            return;
        }
        new MobImitationTargetingService(ImitationCoreServices.forServer(player.serverLevel().getServer()))
                .clearExistingTargets(player, ImitationCoreConfig.mobTargetingReconciliationRange());
    }

    private static void onTransformedPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        activePresentationSession(player).ifPresent(session -> {
            event.setCanceled(true);
            player.setHealth(TransformedPlayerDeathProtection.survivalHealth(player.getMaxHealth()));
            SessionTransitionResult result = requestFormDeathReversion(player, session);
            if (!result.accepted()) {
                event.setCanceled(false);
            }
            syncLifecycleResults(java.util.Optional.of(player), java.util.List.of(result));
        });
    }

    private static void onTransformedPlayerLethalDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !TransformedPlayerDeathProtection.isLethal(player.getHealth(), player.getAbsorptionAmount(), event.getNewDamage())) {
            return;
        }
        activePresentationSession(player).ifPresent(session -> {
            SessionTransitionResult result = requestFormDeathReversion(player, session);
            if (result.accepted()) {
                event.setNewDamage(0.0F);
            }
            syncLifecycleResults(java.util.Optional.of(player), java.util.List.of(result));
        });
    }

    private static void onReplicaDrops(LivingDropsEvent event) {
        if (ReplicaEntityTags.suppressDrops(event.getEntity())) {
            event.getDrops().clear();
        }
    }

    private static void onReplicaDeath(LivingDeathEvent event) {
        if (!ReplicaEntityTags.isReplica(event.getEntity()) || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        ReplicaEntityTags.sessionId(event.getEntity()).ifPresent(sessionId -> {
            ServerPlayer owner = ReplicaEntityTags.ownerId(event.getEntity())
                    .map(id -> level.getServer().getPlayerList().getPlayer(id))
                    .orElse(null);
            SessionTransitionResult result = ImitationCoreServices.applications(level.getServer())
                    .requestReversion(java.util.Optional.ofNullable(owner), sessionId, TransformationLifecycleReason.REPLICA_REMOVED, gameTime(event.getEntity()));
            if (owner != null) {
                syncLifecycleResults(java.util.Optional.of(owner), java.util.List.of(result));
            }
        });
    }

    private static void onReplicaExperience(LivingExperienceDropEvent event) {
        if (ReplicaEntityTags.suppressExperience(event.getEntity())) {
            event.setDroppedExperience(0);
        }
    }

    private static java.util.Optional<TransformationApplicationService> applications(Player player) {
        if (player.level() instanceof ServerLevel serverLevel) {
            return java.util.Optional.of(ImitationCoreServices.applications(serverLevel.getServer()));
        }
        return java.util.Optional.empty();
    }

    private static java.util.Optional<ServerPlayer> serverPlayer(Player player) {
        return player instanceof ServerPlayer serverPlayer ? java.util.Optional.of(serverPlayer) : java.util.Optional.empty();
    }

    private static long gameTime(Entity entity) {
        return entity.level() instanceof ServerLevel serverLevel ? serverLevel.getGameTime() : 0L;
    }

    private static void syncActiveDisguises(ServerPlayer player) {
        TransformationService service = ImitationCoreServices.forServer(player.serverLevel().getServer());
        for (TransformationSession session : service.activeSessions()) {
            if (!session.scope().changesOwnerPresentation()) {
                continue;
            }
            ServerPlayer subject = player.serverLevel().getServer().getPlayerList().getPlayer(session.ownerId());
            if (subject != null) {
                ImitationCoreNetwork.syncToPlayer(player, subject, session);
            }
        }
    }

    private static void reconcileReplicas(ServerPlayer player) {
        if (player.tickCount % 40 != 0) {
            return;
        }
        MinecraftServer server = player.serverLevel().getServer();
        TransformationService transformations = ImitationCoreServices.forServer(server);
        TransformationApplicationService applications = ImitationCoreServices.applications(server);
        for (TransformationSession session : transformations.sessionsForOwner(player.getUUID())) {
            if (session.scope() != TransformationScope.REPLICA || !session.state().requiresRecovery()) {
                continue;
            }
            boolean cleanup = session.temporaryState().stream()
                    .anyMatch(reference -> ImitatorReplicaApplicationAdapter.shouldCleanup(server, reference, gameTime(player), player));
            if (!cleanup) {
                continue;
            }
            SessionTransitionResult result = applications.requestReversion(java.util.Optional.of(player), session.sessionId(), TransformationLifecycleReason.REPLICA_EXPIRED, gameTime(player));
            syncLifecycleResults(java.util.Optional.of(player), java.util.List.of(result));
        }
    }

    private static java.util.Optional<TransformationSession> activePresentationSession(ServerPlayer player) {
        return ImitationCoreServices.forServer(player.serverLevel().getServer())
                .activeSessionForOwner(player.getUUID())
                .filter(session -> session.scope().changesOwnerPresentation());
    }

    private static SessionTransitionResult requestFormDeathReversion(ServerPlayer player, TransformationSession session) {
        return ImitationCoreServices.applications(player.serverLevel().getServer())
                .requestReversion(java.util.Optional.of(player), session.sessionId(), TransformationLifecycleReason.FORM_DEATH_AVOIDED, gameTime(player));
    }

    private static void syncLifecycleResults(java.util.Optional<ServerPlayer> player, java.util.List<SessionTransitionResult> results) {
        player.ifPresent(subject -> results.stream()
                .flatMap(result -> result.session().stream())
                .forEach(session -> ImitationCoreNetwork.syncToTrackingAndSelf(subject, session)));
    }
}
