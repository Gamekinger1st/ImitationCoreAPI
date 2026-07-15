package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.imitator.ImitatorAction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public record ImitatorActionFeedbackPayload(ImitatorAction action, boolean accepted, String message) implements CustomPacketPayload {
    public static final int MAX_MESSAGE_LENGTH = 256;
    public static final Type<ImitatorActionFeedbackPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "imitator_action_feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImitatorActionFeedbackPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.action.ordinal());
                buffer.writeBoolean(payload.accepted);
                buffer.writeUtf(payload.message, MAX_MESSAGE_LENGTH);
            },
            buffer -> new ImitatorActionFeedbackPayload(actionAt(buffer.readVarInt()), buffer.readBoolean(), buffer.readUtf(MAX_MESSAGE_LENGTH))
    );

    public ImitatorActionFeedbackPayload {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(message, "message");
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Action feedback message exceeds the configured limit");
        }
    }

    @Override
    public Type<ImitatorActionFeedbackPayload> type() {
        return TYPE;
    }

    private static ImitatorAction actionAt(int index) {
        ImitatorAction[] values = ImitatorAction.values();
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("Invalid Imitator action");
        }
        return values[index];
    }
}
