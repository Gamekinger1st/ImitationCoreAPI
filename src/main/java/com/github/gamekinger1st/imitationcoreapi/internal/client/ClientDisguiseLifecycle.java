package com.github.gamekinger1st.imitationcoreapi.internal.client;

import com.github.gamekinger1st.imitationcoreapi.api.ImitationApi;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseEntityFactory;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseQueries;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseStateListener;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguisePresentation;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseRenderContext;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticCategory;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnosticSeverity;
import com.github.gamekinger1st.imitationcoreapi.api.diagnostic.ImitationDiagnostics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientDisguiseLifecycle {
    private static boolean registered;
    private static final ClientDisguiseEntityFactory ENTITY_FACTORY = new ClientDisguiseEntityFactory();
    private static final Set<UUID> PRESENTATION_DIAGNOSTICS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> ENTITY_FACTORY_DIAGNOSTICS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> RENDERER_DIAGNOSTICS = ConcurrentHashMap.newKeySet();

    private ClientDisguiseLifecycle() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onLogout);
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onRenderLiving);
        ImitationApi.imitatorMenus().register(ClientImitatorFormMenus::open);
        ImitationApi.clientDisguiseStates().register(new ClientDisguiseStateListener() {
            @Override
            public void onDisguiseActivated(ClientDisguiseState state) {
            }

            @Override
            public void onDisguiseCleared(int entityId, java.util.UUID ownerId) {
                ENTITY_FACTORY.clearByOwner(ownerId);
            }
        });
        registered = true;
    }

    private static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ImitationApi.clientDisguiseStore().get(event.getEntity().getId()).ifPresent(state -> {
                ENTITY_FACTORY.clear(state.sessionId());
                clearDiagnosticSession(state.sessionId());
            });
            ImitationApi.clientDisguiseStore().clearEntity(event.getEntity().getId());
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ImitationApi.clientDisguiseStore().clearAll();
        ImitationApi.clientChatStore().clearAll();
        ImitationApi.clientChatProtocol().clear();
        ImitationApi.clientImitatorFormLibrary().clear();
        ENTITY_FACTORY.clearAll();
        PRESENTATION_DIAGNOSTICS.clear();
        ENTITY_FACTORY_DIAGNOSTICS.clear();
        RENDERER_DIAGNOSTICS.clear();
    }

    private static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        ClientDisguiseQueries.state(event.getEntity())
                .ifPresent(state -> renderDisguise(event, state));
    }

    private static void renderDisguise(RenderLivingEvent.Pre<?, ?> event, ClientDisguiseState state) {
        DisguisePresentation presentation = ImitationApi.disguisePresentations().resolve(event.getEntity(), state);
        if (presentation.renderMode() == DisguisePresentation.RenderMode.FALLBACK) {
            diagnoseOnce(PRESENTATION_DIAGNOSTICS, state, ImitationDiagnosticCategory.UNSUPPORTED_RENDERER, ImitationDiagnosticSeverity.WARNING, "No disguise presentation adapter accepted the copied form; using the original entity renderer");
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity imitation = ENTITY_FACTORY.createOrUpdate(minecraft.level, event.getEntity(), state).orElse(null);
        if (imitation == null) {
            diagnoseOnce(ENTITY_FACTORY_DIAGNOSTICS, state, ImitationDiagnosticCategory.INVALID_SNAPSHOT, ImitationDiagnosticSeverity.ERROR, "The copied form could not be reconstructed on the client");
            return;
        }
        ImitationApi.disguiseAnimations().synchronize(imitation, event.getEntity(), state, event.getPartialTick());
        DisguiseRenderContext context = new DisguiseRenderContext(event.getEntity(), imitation, state, event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        if (!ImitationApi.disguiseRenderers().render(context)) {
            diagnoseOnce(RENDERER_DIAGNOSTICS, state, ImitationDiagnosticCategory.UNSUPPORTED_RENDERER, ImitationDiagnosticSeverity.WARNING, "No disguise render adapter handled the copied form; using the entity renderer fallback");
            EntityRenderer<? super Entity> renderer = minecraft.getEntityRenderDispatcher().getRenderer(imitation);
            renderer.render(imitation, event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        }
        event.setCanceled(true);
    }

    private static void diagnoseOnce(Set<UUID> sessions, ClientDisguiseState state, ImitationDiagnosticCategory category, ImitationDiagnosticSeverity severity, String message) {
        if (!sessions.add(state.sessionId())) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long gameTime = minecraft.level == null ? 0L : minecraft.level.getGameTime();
        ImitationDiagnostics.publish(
                category,
                severity,
                message,
                Optional.of(state.ownerId()),
                Optional.of(state.sessionId()),
                Optional.of(state.snapshotId()),
                Optional.of(state.entityType()),
                gameTime
        );
    }

    private static void clearDiagnosticSession(UUID sessionId) {
        PRESENTATION_DIAGNOSTICS.remove(sessionId);
        ENTITY_FACTORY_DIAGNOSTICS.remove(sessionId);
        RENDERER_DIAGNOSTICS.remove(sessionId);
    }
}
