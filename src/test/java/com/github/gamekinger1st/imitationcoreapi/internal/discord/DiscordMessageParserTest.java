package com.github.gamekinger1st.imitationcoreapi.internal.discord;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscordMessageParserTest {
    @Test
    void readsHumanMessagesInOldestFirstOrder() {
        String payload = """
                [
                  {"id":"2","channel_id":"42","content":"second","author":{"id":"22","username":"Second","bot":false}},
                  {"id":"1","channel_id":"42","content":"first","author":{"id":"11","username":"First","global_name":"First Display","bot":false}},
                  {"id":"3","channel_id":"42","content":"ignored bot","author":{"id":"33","username":"Bot","bot":true}},
                  {"id":"4","channel_id":"42","webhook_id":"99","content":"ignored webhook","author":{"id":"44","username":"Webhook","bot":true}},
                  {"id":"5","channel_id":"24","content":"ignored channel","author":{"id":"55","username":"Other","bot":false}}
                ]
                """;

        List<DiscordInboundMessage> messages = DiscordMessageParser.parseChannelMessages(payload, "42");

        assertEquals(List.of("1", "2"), messages.stream().map(DiscordInboundMessage::messageId).toList());
        assertEquals("First Display", messages.getFirst().authorName());
        assertEquals("second", messages.get(1).content());
        assertEquals("5", DiscordMessageParser.latestMessageId(payload).orElseThrow());
    }

    @Test
    void ordersSnowflakesAsUnsignedValues() {
        String payload = """
                [
                  {"id":"9223372036854775808","channel_id":"42","content":"newer","author":{"id":"22","username":"Second","bot":false}},
                  {"id":"9223372036854775807","channel_id":"42","content":"older","author":{"id":"11","username":"First","bot":false}}
                ]
                """;

        List<DiscordInboundMessage> messages = DiscordMessageParser.parseChannelMessages(payload, "42");

        assertEquals(List.of("9223372036854775807", "9223372036854775808"), messages.stream().map(DiscordInboundMessage::messageId).toList());
        assertEquals("9223372036854775808", DiscordMessageParser.latestMessageId(payload).orElseThrow());
    }

    @Test
    void relaysAttachmentsAndReplyContext() {
        String payload = """
                [{"id":"7","channel_id":"42","content":"look","author":{"id":"11","username":"First","bot":false},"attachments":[{"url":"https://cdn.discordapp.com/file.png"}],"referenced_message":{"content":"earlier","author":{"username":"Second"}}}]
                """;

        DiscordInboundMessage message = DiscordMessageParser.parseChannelMessages(payload, "42").getFirst();

        assertEquals("[reply to Second: earlier] look https://cdn.discordapp.com/file.png", message.content());
    }
}
