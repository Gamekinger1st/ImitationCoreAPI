package com.github.gamekinger1st.imitationcoreapi.api.session;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.UUID;

public record TemporaryStateReference(
        UUID referenceId,
        UUID sessionId,
        ResourceLocation handlerId,
        ResourceLocation kind,
        CompoundTag payload,
        TemporaryStateStatus status
) {
    public TemporaryStateReference {
        Objects.requireNonNull(referenceId, "referenceId");
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(handlerId, "handlerId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(payload, "payload");
        Objects.requireNonNull(status, "status");
        payload = payload.copy();
    }

    public TemporaryStateReference(UUID referenceId, UUID sessionId, ResourceLocation kind, CompoundTag payload, TemporaryStateStatus status) {
        this(referenceId, sessionId, TemporaryStateKinds.UNASSIGNED_HANDLER, kind, payload, status);
    }

    @Override
    public CompoundTag payload() {
        return payload.copy();
    }

    public TemporaryStateReference withStatus(TemporaryStateStatus status) {
        return new TemporaryStateReference(referenceId, sessionId, handlerId, kind, payload, status);
    }

    public TemporaryStateReference withPayload(CompoundTag payload) {
        return new TemporaryStateReference(referenceId, sessionId, handlerId, kind, payload, status);
    }
}
