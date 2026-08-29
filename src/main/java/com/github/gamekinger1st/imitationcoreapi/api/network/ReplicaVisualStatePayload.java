package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.snapshot.SnapshotLimits;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ReplicaVisualStatePayload(int entityId, CompoundTag equipment) implements CustomPacketPayload {
    public static final Type<ReplicaVisualStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "replica_visual_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ReplicaVisualStatePayload> STREAM_CODEC = StreamCodec.of(ReplicaVisualStatePayload::encode, ReplicaVisualStatePayload::decode);
    public static final int MAX_EQUIPMENT_BYTES = SnapshotLimits.DEFAULT.maxVisualDataBytes();

    public ReplicaVisualStatePayload {
        if (entityId < 0) {
            throw new IllegalArgumentException("entityId cannot be negative");
        }
        Objects.requireNonNull(equipment, "equipment");
        equipment = equipment.copy();
        if (equipment.sizeInBytes() > MAX_EQUIPMENT_BYTES) {
            throw new IllegalArgumentException("Replica visual equipment exceeds the synchronized payload limit");
        }
    }

    @Override
    public CompoundTag equipment() {
        return equipment.copy();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ReplicaVisualStatePayload payload) {
        buffer.writeVarInt(payload.entityId());
        buffer.writeNbt(payload.equipment());
    }

    private static ReplicaVisualStatePayload decode(RegistryFriendlyByteBuf buffer) {
        return new ReplicaVisualStatePayload(buffer.readVarInt(), Objects.requireNonNull(buffer.readNbt(), "equipment"));
    }
}
