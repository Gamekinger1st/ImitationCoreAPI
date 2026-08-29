package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.ClientDisguiseState;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.DisguiseAppraisalSnapshot;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.PlayerDisguiseProfile;
import com.github.gamekinger1st.imitationcoreapi.api.disguise.PlayerDisguiseProfileExtensions;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorAutoJumpOverride;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorTransformationModifierState;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorTransformationModifiers;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.IdentitySnapshot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ActiveDisguisePayload(
        int entityId,
        UUID ownerId,
        UUID sessionId,
        UUID snapshotId,
        TransformationScope scope,
        ResourceLocation entityType,
        String displayName,
        CompoundTag entityData,
        CompoundTag visualData,
        CompatibilityLevel compatibility,
        long revision,
        Optional<PlayerDisguiseProfile> playerProfile,
        Optional<DisguiseAppraisalSnapshot> appraisal,
        ImitatorTransformationModifiers transformationModifiers
) implements CustomPacketPayload {
    public static final int MAX_ENTITY_DATA_BYTES = com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotLimits.DEFAULT.maxEntityDataBytes();
    public static final int MAX_VISUAL_DATA_BYTES = com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotLimits.DEFAULT.maxVisualDataBytes();
    public static final int MAX_DISPLAY_NAME_LENGTH = 256;
    public static final Type<ActiveDisguisePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "active_disguise"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ActiveDisguisePayload> STREAM_CODEC = StreamCodec.of(ActiveDisguisePayload::encode, ActiveDisguisePayload::decode);

    public ActiveDisguisePayload(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            TransformationScope scope,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision
    ) {
        this(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, Optional.empty(), Optional.empty(), ImitatorTransformationModifiers.DEFAULT);
    }

    public ActiveDisguisePayload(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            TransformationScope scope,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision,
            Optional<PlayerDisguiseProfile> playerProfile
    ) {
        this(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, playerProfile, Optional.empty(), ImitatorTransformationModifiers.DEFAULT);
    }

    public ActiveDisguisePayload(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision
    ) {
        this(entityId, ownerId, sessionId, snapshotId, TransformationScope.GAMEPLAY, entityType, displayName, entityData, visualData, compatibility, revision, Optional.empty(), Optional.empty(), ImitatorTransformationModifiers.DEFAULT);
    }

    public ActiveDisguisePayload(
            int entityId,
            UUID ownerId,
            UUID sessionId,
            UUID snapshotId,
            TransformationScope scope,
            ResourceLocation entityType,
            String displayName,
            CompoundTag entityData,
            CompoundTag visualData,
            CompatibilityLevel compatibility,
            long revision,
            Optional<PlayerDisguiseProfile> playerProfile,
            Optional<DisguiseAppraisalSnapshot> appraisal
    ) {
        this(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, playerProfile, appraisal, ImitatorTransformationModifiers.DEFAULT);
    }

    public ActiveDisguisePayload {
        if (entityId < 0 || revision < 0) {
            throw new IllegalArgumentException("entityId and revision cannot be negative");
        }
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(entityType, "entityType");
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(entityData, "entityData");
        Objects.requireNonNull(visualData, "visualData");
        Objects.requireNonNull(compatibility, "compatibility");
        Objects.requireNonNull(playerProfile, "playerProfile");
        Objects.requireNonNull(appraisal, "appraisal");
        Objects.requireNonNull(transformationModifiers, "transformationModifiers");
        displayName = displayName.strip();
        if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
            throw new IllegalArgumentException("Disguise display name exceeds the configured limit");
        }
        entityData = entityData.copy();
        visualData = visualData.copy();
        if (entityData.sizeInBytes() > MAX_ENTITY_DATA_BYTES || visualData.sizeInBytes() > MAX_VISUAL_DATA_BYTES) {
            throw new IllegalArgumentException("Disguise payload NBT exceeds the configured limit");
        }
    }

    public static ActiveDisguisePayload from(int entityId, TransformationSession session, IdentitySnapshot snapshot) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(snapshot, "snapshot");
        CompoundTag entityData = bounded(snapshot.entityData(), MAX_ENTITY_DATA_BYTES);
        CompoundTag visualData = bounded(snapshot.visualData(), MAX_VISUAL_DATA_BYTES);
        return new ActiveDisguisePayload(
                entityId,
                session.ownerId(),
                session.sessionId(),
                snapshot.snapshotId(),
                session.scope(),
                snapshot.entityType(),
                snapshot.displayName(),
                entityData,
                visualData,
                session.compatibility().level(),
                session.revision(),
                PlayerDisguiseProfileExtensions.find(snapshot.extensions()),
                DisguiseAppraisalExtensions.find(snapshot.extensions()),
                ImitatorTransformationModifierState.find(session)
        );
    }

    public ClientDisguiseState toState() {
        return new ClientDisguiseState(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, compatibility, revision, playerProfile, appraisal, transformationModifiers);
    }

    @Override
    public CompoundTag entityData() {
        return entityData.copy();
    }

    @Override
    public CompoundTag visualData() {
        return visualData.copy();
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ActiveDisguisePayload payload) {
        buffer.writeVarInt(payload.entityId);
        buffer.writeUUID(payload.ownerId);
        buffer.writeUUID(payload.sessionId);
        buffer.writeUUID(payload.snapshotId);
        buffer.writeVarInt(payload.scope.ordinal());
        buffer.writeResourceLocation(payload.entityType);
        buffer.writeUtf(payload.displayName, MAX_DISPLAY_NAME_LENGTH);
        buffer.writeNbt(payload.entityData);
        buffer.writeNbt(payload.visualData);
        buffer.writeVarInt(payload.compatibility.ordinal());
        buffer.writeVarLong(payload.revision);
        buffer.writeBoolean(payload.playerProfile.isPresent());
        payload.playerProfile.ifPresent(profile -> writePlayerProfile(buffer, profile));
        buffer.writeBoolean(payload.appraisal.isPresent());
        payload.appraisal.ifPresent(snapshot -> writeAppraisal(buffer, snapshot));
        buffer.writeVarInt(payload.transformationModifiers.autoJumpOverride().ordinal());
    }

    private static ActiveDisguisePayload decode(RegistryFriendlyByteBuf buffer) {
        int entityId = buffer.readVarInt();
        UUID ownerId = buffer.readUUID();
        UUID sessionId = buffer.readUUID();
        UUID snapshotId = buffer.readUUID();
        TransformationScope scope = enumAt(TransformationScope.values(), buffer.readVarInt(), "transformation scope");
        ResourceLocation entityType = buffer.readResourceLocation();
        String displayName = buffer.readUtf(MAX_DISPLAY_NAME_LENGTH);
        CompoundTag entityData = Objects.requireNonNull(buffer.readNbt(), "entityData");
        CompoundTag visualData = Objects.requireNonNull(buffer.readNbt(), "visualData");
        int compatibilityIndex = buffer.readVarInt();
        CompatibilityLevel[] levels = CompatibilityLevel.values();
        if (compatibilityIndex < 0 || compatibilityIndex >= levels.length) {
            throw new IllegalArgumentException("Invalid disguise compatibility level");
        }
        long revision = buffer.readVarLong();
        Optional<PlayerDisguiseProfile> playerProfile = buffer.readBoolean() ? Optional.of(readPlayerProfile(buffer)) : Optional.empty();
        Optional<DisguiseAppraisalSnapshot> appraisal = buffer.readBoolean() ? Optional.of(readAppraisal(buffer)) : Optional.empty();
        ImitatorAutoJumpOverride autoJumpOverride = enumAt(ImitatorAutoJumpOverride.values(), buffer.readVarInt(), "auto-jump override");
        return new ActiveDisguisePayload(entityId, ownerId, sessionId, snapshotId, scope, entityType, displayName, entityData, visualData, levels[compatibilityIndex], revision, playerProfile, appraisal, new ImitatorTransformationModifiers(autoJumpOverride));
    }

    private static <T> T enumAt(T[] values, int index, String name) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("Invalid " + name + " value");
        }
        return values[index];
    }

    private static CompoundTag bounded(CompoundTag data, int limit) {
        if (data.sizeInBytes() > limit) {
            throw new IllegalArgumentException("Disguise data exceeds the synchronized payload limit");
        }
        return data;
    }

    private static void writePlayerProfile(RegistryFriendlyByteBuf buffer, PlayerDisguiseProfile profile) {
        buffer.writeUUID(profile.playerId());
        buffer.writeUtf(profile.accountName(), PlayerDisguiseProfile.MAX_ACCOUNT_NAME_LENGTH);
        buffer.writeBoolean(profile.texturesValue().isPresent());
        profile.texturesValue().ifPresent(value -> buffer.writeUtf(value, PlayerDisguiseProfile.MAX_TEXTURE_VALUE_LENGTH));
        buffer.writeBoolean(profile.texturesSignature().isPresent());
        profile.texturesSignature().ifPresent(signature -> buffer.writeUtf(signature, PlayerDisguiseProfile.MAX_TEXTURE_SIGNATURE_LENGTH));
    }

    private static PlayerDisguiseProfile readPlayerProfile(RegistryFriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        String accountName = buffer.readUtf(PlayerDisguiseProfile.MAX_ACCOUNT_NAME_LENGTH);
        Optional<String> textureValue = buffer.readBoolean()
                ? Optional.of(buffer.readUtf(PlayerDisguiseProfile.MAX_TEXTURE_VALUE_LENGTH))
                : Optional.empty();
        Optional<String> textureSignature = buffer.readBoolean()
                ? Optional.of(buffer.readUtf(PlayerDisguiseProfile.MAX_TEXTURE_SIGNATURE_LENGTH))
                : Optional.empty();
        return new PlayerDisguiseProfile(playerId, accountName, textureValue, textureSignature);
    }

    private static void writeAppraisal(RegistryFriendlyByteBuf buffer, DisguiseAppraisalSnapshot appraisal) {
        buffer.writeFloat(appraisal.health());
        buffer.writeFloat(appraisal.maxHealth());
        buffer.writeVarInt(appraisal.armorValue());
        buffer.writeBoolean(appraisal.tensuraVitals().isPresent());
        appraisal.tensuraVitals().ifPresent(vitals -> {
            buffer.writeDouble(vitals.ep());
            buffer.writeDouble(vitals.magicule());
            buffer.writeDouble(vitals.aura());
            buffer.writeDouble(vitals.spiritualHealth());
        });
    }

    private static DisguiseAppraisalSnapshot readAppraisal(RegistryFriendlyByteBuf buffer) {
        float health = buffer.readFloat();
        float maxHealth = buffer.readFloat();
        int armor = buffer.readVarInt();
        Optional<com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals> vitals = buffer.readBoolean()
                ? Optional.of(new com.github.gamekinger1st.imitationcoreapi.api.tensura.TensuraVitals(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()))
                : Optional.empty();
        return new DisguiseAppraisalSnapshot(health, maxHealth, armor, vitals);
    }

    @Override
    public Type<ActiveDisguisePayload> type() {
        return TYPE;
    }
}
