package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.compat.CompatibilityLevel;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationSession;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationScope;
import com.github.gamekinger1st.imitationcoreapi.api.session.TransformationState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SessionStatePayload(
        UUID sessionId,
        UUID ownerId,
        UUID snapshotId,
        TransformationScope scope,
        TransformationState state,
        CompatibilityLevel compatibility,
        long revision,
        List<String> reasons
) implements CustomPacketPayload {
    private static final int MAX_REASONS = 8;
    private static final int MAX_REASON_LENGTH = 256;
    public static final Type<SessionStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "session_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SessionStatePayload> STREAM_CODEC = StreamCodec.of(
            SessionStatePayload::encode,
            SessionStatePayload::decode
    );

    public SessionStatePayload(
            UUID sessionId,
            UUID ownerId,
            UUID snapshotId,
            TransformationState state,
            CompatibilityLevel compatibility,
            long revision,
            List<String> reasons
    ) {
        this(sessionId, ownerId, snapshotId, TransformationScope.GAMEPLAY, state, compatibility, revision, reasons);
    }

    public SessionStatePayload {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(snapshotId, "snapshotId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(compatibility, "compatibility");
        Objects.requireNonNull(reasons, "reasons");
        if (revision < 0) {
            throw new IllegalArgumentException("Invalid session state payload bounds");
        }
        reasons = reasons.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(reason -> !reason.isEmpty())
                .map(reason -> reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason)
                .limit(MAX_REASONS)
                .toList();
    }

    public static SessionStatePayload from(TransformationSession session) {
        return new SessionStatePayload(
                session.sessionId(),
                session.ownerId(),
                session.snapshotId(),
                session.scope(),
                session.state(),
                session.compatibility().level(),
                session.revision(),
                session.compatibility().reasons()
        );
    }

    private static void encode(RegistryFriendlyByteBuf buffer, SessionStatePayload payload) {
        buffer.writeUUID(payload.sessionId);
        buffer.writeUUID(payload.ownerId);
        buffer.writeUUID(payload.snapshotId);
        buffer.writeVarInt(payload.scope.ordinal());
        buffer.writeVarInt(payload.state.ordinal());
        buffer.writeVarInt(payload.compatibility.ordinal());
        buffer.writeVarLong(payload.revision);
        buffer.writeVarInt(payload.reasons.size());
        for (String reason : payload.reasons) {
            buffer.writeUtf(reason, MAX_REASON_LENGTH);
        }
    }

    private static SessionStatePayload decode(RegistryFriendlyByteBuf buffer) {
        UUID sessionId = buffer.readUUID();
        UUID ownerId = buffer.readUUID();
        UUID snapshotId = buffer.readUUID();
        TransformationScope scope = enumAt(TransformationScope.values(), buffer.readVarInt(), "transformation scope");
        TransformationState state = enumAt(TransformationState.values(), buffer.readVarInt(), "transformation state");
        CompatibilityLevel compatibility = enumAt(CompatibilityLevel.values(), buffer.readVarInt(), "compatibility level");
        long revision = buffer.readVarLong();
        int reasonCount = buffer.readVarInt();
        if (reasonCount < 0 || reasonCount > MAX_REASONS) {
            throw new IllegalArgumentException("Invalid session-state reason count");
        }
        java.util.ArrayList<String> reasons = new java.util.ArrayList<>(reasonCount);
        for (int index = 0; index < reasonCount; index++) {
            reasons.add(buffer.readUtf(MAX_REASON_LENGTH));
        }
        return new SessionStatePayload(sessionId, ownerId, snapshotId, scope, state, compatibility, revision, reasons);
    }

    private static <T> T enumAt(T[] values, int index, String name) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("Invalid " + name + " value");
        }
        return values[index];
    }

    @Override
    public Type<SessionStatePayload> type() {
        return TYPE;
    }
}
