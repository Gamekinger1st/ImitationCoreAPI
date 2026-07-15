package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

final class DiscordMessageParser {
    private static final Comparator<String> SNOWFLAKE_ORDER = (left, right) -> Long.compareUnsigned(Long.parseUnsignedLong(left), Long.parseUnsignedLong(right));

    private DiscordMessageParser() {
    }

    static List<DiscordInboundMessage> parseChannelMessages(String payload, String expectedChannelId) {
        Optional<JsonArray> messages = messages(payload);
        if (messages.isEmpty()) {
            return List.of();
        }
        List<DiscordInboundMessage> parsed = new ArrayList<>();
        for (JsonElement messageElement : messages.get()) {
            if (!messageElement.isJsonObject()) {
                continue;
            }
            parseMessage(messageElement.getAsJsonObject(), expectedChannelId).ifPresent(parsed::add);
        }
        parsed.sort(Comparator.comparing(DiscordInboundMessage::messageId, SNOWFLAKE_ORDER));
        return List.copyOf(parsed);
    }

    static Optional<String> latestMessageId(String payload) {
        return messages(payload)
                .stream()
                .flatMap(array -> StreamSupport.stream(array.spliterator(), false))
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .flatMap(message -> string(message, "id").stream())
                .filter(DiscordMessageParser::isSnowflake)
                .max(SNOWFLAKE_ORDER);
    }

    private static Optional<JsonArray> messages(String payload) {
        JsonElement root = JsonParser.parseString(payload);
        return root.isJsonArray() ? Optional.of(root.getAsJsonArray()) : Optional.empty();
    }

    private static Optional<DiscordInboundMessage> parseMessage(JsonObject message, String expectedChannelId) {
        if (!expectedChannelId.equals(string(message, "channel_id").orElse(null)) || message.has("webhook_id")) {
            return Optional.empty();
        }
        JsonElement authorElement = message.get("author");
        if (authorElement == null || !authorElement.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject author = authorElement.getAsJsonObject();
        if (booleanValue(author, "bot")) {
            return Optional.empty();
        }
        Optional<String> messageId = string(message, "id").filter(DiscordMessageParser::isSnowflake);
        Optional<String> authorId = string(author, "id").filter(DiscordMessageParser::isSnowflake);
        Optional<String> authorName = string(author, "global_name").or(() -> string(author, "username"));
        Optional<String> content = string(message, "content");
        if (messageId.isEmpty() || authorId.isEmpty() || authorName.isEmpty() || content.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DiscordInboundMessage(messageId.get(), authorId.get(), authorName.get(), content.get()));
    }

    private static Optional<String> string(JsonObject object, String member) {
        JsonElement element = object.get(member);
        if (element == null || element.isJsonNull() || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return Optional.empty();
        }
        String value = element.getAsString().strip();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    private static boolean booleanValue(JsonObject object, String member) {
        JsonElement element = object.get(member);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }

    private static boolean isSnowflake(String value) {
        if (value.isEmpty() || value.length() > 20 || !value.chars().allMatch(Character::isDigit)) {
            return false;
        }
        try {
            Long.parseUnsignedLong(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
