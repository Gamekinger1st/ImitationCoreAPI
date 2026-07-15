package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatChannelKind;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatEnvelope;
import com.github.gamekinger1st.imitationcoreapi.api.chat.ChatMessageSource;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaIdentity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record ChatEnvelopePayload(ChatEnvelope envelope) implements CustomPacketPayload {
    public static final Type<ChatEnvelopePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "chat_envelope"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChatEnvelopePayload> STREAM_CODEC = StreamCodec.of(ChatEnvelopePayload::encode, ChatEnvelopePayload::decode);

    public ChatEnvelopePayload {
        Objects.requireNonNull(envelope, "envelope");
    }

    private static void encode(RegistryFriendlyByteBuf buffer, ChatEnvelopePayload payload) {
        ChatEnvelope envelope = payload.envelope;
        buffer.writeUUID(envelope.messageId());
        buffer.writeLong(envelope.sentAt().toEpochMilli());
        buffer.writeResourceLocation(envelope.channelId());
        buffer.writeVarInt(envelope.channelKind().ordinal());
        buffer.writeVarInt(envelope.source().ordinal());
        buffer.writeBoolean(envelope.realSenderId().isPresent());
        envelope.realSenderId().ifPresent(buffer::writeUUID);
        buffer.writeBoolean(envelope.realSenderName().isPresent());
        envelope.realSenderName().ifPresent(name -> buffer.writeUtf(name, ChatEnvelope.MAX_SENDER_NAME_LENGTH));
        buffer.writeBoolean(envelope.persona().isPresent());
        if (envelope.persona().isPresent()) {
            PersonaIdentity persona = envelope.persona().get();
            buffer.writeUUID(persona.personaId());
            buffer.writeUtf(persona.displayName(), 256);
            buffer.writeBoolean(persona.copiedPlayerId().isPresent());
            persona.copiedPlayerId().ifPresent(buffer::writeUUID);
        }
        buffer.writeUtf(envelope.message(), ChatEnvelope.MAX_MESSAGE_LENGTH);
    }

    private static ChatEnvelopePayload decode(RegistryFriendlyByteBuf buffer) {
        UUID messageId = buffer.readUUID();
        Instant sentAt = Instant.ofEpochMilli(buffer.readLong());
        ResourceLocation channelId = buffer.readResourceLocation();
        ChatChannelKind channelKind = enumValue(ChatChannelKind.values(), buffer.readVarInt(), "chat channel kind");
        ChatMessageSource source = enumValue(ChatMessageSource.values(), buffer.readVarInt(), "chat message source");
        Optional<UUID> realSenderId = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        Optional<String> realSenderName = buffer.readBoolean() ? Optional.of(buffer.readUtf(ChatEnvelope.MAX_SENDER_NAME_LENGTH)) : Optional.empty();
        Optional<PersonaIdentity> persona = Optional.empty();
        if (buffer.readBoolean()) {
            UUID personaId = buffer.readUUID();
            String displayName = buffer.readUtf(256);
            Optional<UUID> copiedPlayerId = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
            persona = Optional.of(new PersonaIdentity(personaId, displayName, copiedPlayerId));
        }
        return new ChatEnvelopePayload(new ChatEnvelope(messageId, sentAt, channelId, channelKind, source, realSenderId, realSenderName, persona, buffer.readUtf(ChatEnvelope.MAX_MESSAGE_LENGTH)));
    }

    private static <T> T enumValue(T[] values, int index, String name) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("Invalid " + name);
        }
        return values[index];
    }

    @Override
    public Type<ChatEnvelopePayload> type() {
        return TYPE;
    }
}
