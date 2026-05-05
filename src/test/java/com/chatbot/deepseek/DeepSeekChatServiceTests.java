package com.chatbot.deepseek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.chatbot.chat.ChatMessageContext;
import com.chatbot.config.BotProperties;
import com.chatbot.config.DeepSeekProperties;
import com.chatbot.prompt.PromptContextBuilder;
import com.chatbot.prompt.PromptProfileProvider;
import com.chatbot.prompt.TimeContextProvider;
import com.chatbot.prompt.dto.PromptProfile;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DeepSeekChatServiceTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldReturnAssistantReply() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        AtomicReference<JsonNode> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(objectMapper.readTree(exchange.getRequestBody()));
            respond(exchange, 200, """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "assistant reply"
                          }
                        }
                      ]
                    }
                    """);
        });
        server.start();

        DeepSeekChatService deepSeekChatService = createService("http://127.0.0.1:" + server.getAddress().getPort());

        String reply = deepSeekChatService.chat(List.of(
                new ChatMessageContext("user", "hello"),
                new ChatMessageContext("assistant", "previous reply")
        ));

        assertThat(reply).isEqualTo("assistant reply");
        assertThat(authorizationHeader.get()).isEqualTo("Bearer test-key");
        assertThat(requestBody.get().path("messages")).hasSize(3);
        assertThat(requestBody.get().path("messages").get(0).path("role").asText()).isEqualTo("system");
        assertThat(requestBody.get().path("messages").get(0).path("content").asText()).contains("[当前时间]");
        assertThat(requestBody.get().path("messages").get(1).path("role").asText()).isEqualTo("user");
        assertThat(requestBody.get().path("messages").get(1).path("content").asText()).isEqualTo("hello");
        assertThat(requestBody.get().path("messages").get(2).path("role").asText()).isEqualTo("assistant");
    }

    @Test
    void shouldThrowWhenServerFails() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/chat/completions", exchange -> respond(exchange, 500, """
                {
                  "error": {
                    "message": "server error"
                  }
                }
                """));
        server.start();

        DeepSeekChatService deepSeekChatService = createService("http://127.0.0.1:" + server.getAddress().getPort());

        assertThatThrownBy(() -> deepSeekChatService.chat(List.of(new ChatMessageContext("user", "hello"))))
                .isInstanceOf(DeepSeekChatException.class);
    }

    private DeepSeekChatService createService(String baseUrl) {
        DeepSeekProperties deepSeekProperties = new DeepSeekProperties();
        deepSeekProperties.setApiKey("test-key");
        deepSeekProperties.setBaseUrl(baseUrl);
        deepSeekProperties.setModel("deepseek-chat");

        BotProperties botProperties = new BotProperties();
        botProperties.setTimezone("Asia/Shanghai");
        botProperties.getChat().setTimeoutSeconds(20);
        botProperties.getChat().setMaxReplyTokens(500);

        PromptProfileProvider provider = new PromptProfileProvider() {
            @Override
            public PromptProfile getProfile() {
                return new PromptProfile(PromptProfile.Source.DEFAULT, "persona", 7, false);
            }

            @Override
            public PromptProfile reload() {
                return getProfile();
            }
        };
        TimeContextProvider timeContextProvider = TimeContextProvider.forClock(
                botProperties,
                Clock.fixed(Instant.parse("2026-05-04T16:03:21Z"), ZoneId.of("UTC"))
        );
        PromptContextBuilder promptContextBuilder = new PromptContextBuilder(provider, timeContextProvider, botProperties);

        return new DeepSeekChatService(RestClient.builder(), deepSeekProperties, botProperties, promptContextBuilder);
    }

    private void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bodyBytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bodyBytes);
        } finally {
            exchange.close();
        }
    }
}
