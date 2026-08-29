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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    private static final ConcurrentHashMap<UUID, UUID> DIAGNOSTIC_OWNERS = new ConcurrentHashMap<>();

    private ClientDisguiseLifecycle() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onLogout);
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onRenderLiving);
        NeoForge.EVENT_BUS.addListener(ClientDisguiseLifecycle::onEntitySize);
        ImitationApi.imitatorMenus().register(ClientImitatorFormMenus::open);
        ImitationApi.clientDisguiseStates().register(new ClientDisguiseStateListener() {
            @Override
            public void onDisguiseActivated(ClientDisguiseState state) {
            }

            @Override
            public void onDisguiseCleared(int entityId, java.util.UUID ownerId) {
                ENTITY_FACTORY.sessionIdsForOwner(ownerId).forEach(sessionId -> {
                    ClientDisguiseLifecycle.clearDiagnosticSession(sessionId);
                    ImitationApi.disguiseAnimations().clearSession(sessionId);
                });
                DIAGNOSTIC_OWNERS.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(ownerId))
                        .map(java.util.Map.Entry::getKey)
                        .toList()
                        .forEach(ClientDisguiseLifecycle::clearDiagnosticSession);
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
                ImitationApi.disguiseAnimations().clearSession(state.sessionId());
            });
            ImitationApi.clientDisguiseStore().clearEntity(event.getEntity().getId());
            ImitationApi.clientReplicaVisuals().clearEntity(event.getEntity().getId());
        }
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ChatDraftStore.discard();
        ImitationApi.clientDisguiseStore().clearAll();
        ImitationApi.clientReplicaVisuals().clearAll();
        ImitationApi.clientChatStore().clearAll();
        ImitationApi.clientChatProtocol().clear();
        ImitationApi.clientImitatorFormLibrary().clear();
        ImitationApi.disguiseAnimations().clearAllSessions();
        ENTITY_FACTORY.clearAll();
        PRESENTATION_DIAGNOSTICS.clear();
        ENTITY_FACTORY_DIAGNOSTICS.clear();
        RENDERER_DIAGNOSTICS.clear();
        DIAGNOSTIC_OWNERS.clear();
    }

    private static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        applyReplicaVisualEquipment(event.getEntity());
        ClientDisguiseQueries.state(event.getEntity())
                .ifPresent(state -> renderDisguise(event, state));
    }

    private static void applyReplicaVisualEquipment(LivingEntity replica) {
        ImitationApi.clientReplicaVisuals().get(replica.getId()).ifPresent(equipment -> {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!equipment.contains(slot.getName(), Tag.TAG_COMPOUND)) {
                    replica.setItemSlot(slot, ItemStack.EMPTY);
                    continue;
                }
                CompoundTag item = equipment.getCompound(slot.getName());
                replica.setItemSlot(slot, visualStack(replica, item));
            }
        });
    }

    private static ItemStack visualStack(LivingEntity replica, CompoundTag item) {
        if (item.contains("stack", Tag.TAG_COMPOUND)) {
            ItemStack decoded = ItemStack.parseOptional(replica.registryAccess(), item.getCompound("stack"));
            if (!decoded.isEmpty()) {
                return decoded;
            }
        }
        ResourceLocation itemId = ResourceLocation.tryParse(item.getString("item"));
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        return BuiltInRegistries.ITEM.getOptional(itemId)
                .map(value -> new ItemStack(value, Math.max(1, Math.min(99, item.getInt("count")))))
                .orElse(ItemStack.EMPTY);
    }

    private static void onEntitySize(net.neoforged.neoforge.event.entity.EntityEvent.Size event) {
        ImitationApi.clientDisguiseStore().get(event.getEntity().getId())
                .filter(state -> state.scope().appliesGameplayState())
                .ifPresent(state -> applyDimensions(event, state.visualData()));
    }

    private static void applyDimensions(net.neoforged.neoforge.event.entity.EntityEvent.Size event, CompoundTag visualData) {
        if (!visualData.contains("bb_width", Tag.TAG_FLOAT) || !visualData.contains("bb_height", Tag.TAG_FLOAT)) {
            return;
        }
        float width = visualData.getFloat("bb_width");
        float height = visualData.getFloat("bb_height");
        if (Float.isFinite(width) && Float.isFinite(height) && width >= 0.01F && width <= 64F && height >= 0.01F && height <= 64F) {
            event.setNewSize(net.minecraft.world.entity.EntityDimensions.scalable(width, height));
        }
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
        try {
            ImitationApi.disguiseAnimations().synchronize(imitation, event.getEntity(), state, event.getPartialTick());
            DisguiseRenderContext context = new DisguiseRenderContext(event.getEntity(), imitation, state, event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            if (!ImitationApi.disguiseRenderers().render(context)) {
                diagnoseOnce(RENDERER_DIAGNOSTICS, state, ImitationDiagnosticCategory.UNSUPPORTED_RENDERER, ImitationDiagnosticSeverity.WARNING, "No disguise render adapter handled the copied form; using the entity renderer fallback");
                EntityRenderer<? super Entity> renderer = minecraft.getEntityRenderDispatcher().getRenderer(imitation);
                renderer.render(imitation, event.getEntity().getYRot(), event.getPartialTick(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
            }
        } catch (RuntimeException | LinkageError exception) {
            diagnoseOnce(RENDERER_DIAGNOSTICS, state, ImitationDiagnosticCategory.UNSUPPORTED_RENDERER, ImitationDiagnosticSeverity.ERROR, "The copied form renderer failed; the original player renderer was preserved");
            return;
        }
        event.setCanceled(true);
    }

    private static void diagnoseOnce(Set<UUID> sessions, ClientDisguiseState state, ImitationDiagnosticCategory category, ImitationDiagnosticSeverity severity, String message) {
        if (!sessions.add(state.sessionId())) {
            return;
        }
        DIAGNOSTIC_OWNERS.put(state.sessionId(), state.ownerId());
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
        DIAGNOSTIC_OWNERS.remove(sessionId);
    }
}
