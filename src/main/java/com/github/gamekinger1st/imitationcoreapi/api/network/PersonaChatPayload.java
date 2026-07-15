package com.github.gamekinger1st.imitationcoreapi.api.network;

import com.github.gamekinger1st.imitationcoreapi.ImitationCoreApi;
import com.github.gamekinger1st.imitationcoreapi.api.chat.PersonaIdentity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record PersonaChatPayload(UUID messageId, UUID realSenderId, PersonaIdentity persona, String message) implements CustomPacketPayload {
    public static final int MAX_MESSAGE_LENGTH = 256;
    public static final Type<PersonaChatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ImitationCoreApi.MOD_ID, "persona_chat"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PersonaChatPayload> STREAM_CODEC = StreamCodec.of(PersonaChatPayload::encode, PersonaChatPayload::decode);

    public PersonaChatPayload {
        Objects.requireNonNull(messageId, "messageId");
        Objects.requireNonNull(realSenderId, "realSenderId");
        Objects.requireNonNull(persona, "persona");
        Objects.requireNonNull(message, "message");
        message = message.strip();
        if (message.isEmpty() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Persona chat message must contain between 1 and " + MAX_MESSAGE_LENGTH + " characters");
        }
    }

    public Component displayComponent() {
        return Component.literal("<").append(Component.literal(persona.displayName()).withStyle(ChatFormatting.LIGHT_PURPLE)).append("> ").append(Component.literal(message));
    }

    private static void encode(RegistryFriendlyByteBuf buffer, PersonaChatPayload payload) {
        buffer.writeUUID(payload.messageId);
        buffer.writeUUID(payload.realSenderId);
        buffer.writeUUID(payload.persona.personaId());
        buffer.writeUtf(payload.persona.displayName(), 256);
        buffer.writeBoolean(payload.persona.copiedPlayerId().isPresent());
        payload.persona.copiedPlayerId().ifPresent(buffer::writeUUID);
        buffer.writeUtf(payload.message, MAX_MESSAGE_LENGTH);
    }

    private static PersonaChatPayload decode(RegistryFriendlyByteBuf buffer) {
        UUID messageId = buffer.readUUID();
        UUID realSenderId = buffer.readUUID();
        UUID personaId = buffer.readUUID();
        String displayName = buffer.readUtf(256);
        Optional<UUID> copiedPlayerId = buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
        String message = buffer.readUtf(MAX_MESSAGE_LENGTH);
        return new PersonaChatPayload(messageId, realSenderId, new PersonaIdentity(personaId, displayName, copiedPlayerId), message);
    }

    @Override
    public Type<PersonaChatPayload> type() {
        return TYPE;
    }
}
