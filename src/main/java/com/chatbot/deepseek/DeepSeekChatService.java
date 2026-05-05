package com.chatbot.deepseek;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.chatbot.chat.ChatMessageContext;
import com.chatbot.config.BotProperties;
import com.chatbot.config.DeepSeekProperties;
import com.chatbot.memory.dto.RetrievedMemory;
import com.chatbot.prompt.PromptContextBuilder;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class DeepSeekChatService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatService.class);

    private final RestClient restClient;
    private final DeepSeekProperties deepSeekProperties;
    private final BotProperties botProperties;
    private final PromptContextBuilder promptContextBuilder;

    public DeepSeekChatService(RestClient.Builder restClientBuilder,
                               DeepSeekProperties deepSeekProperties,
                               BotProperties botProperties,
                               PromptContextBuilder promptContextBuilder) {
        this.deepSeekProperties = deepSeekProperties;
        this.botProperties = botProperties;
        this.promptContextBuilder = promptContextBuilder;
        Duration timeout = Duration.ofSeconds(botProperties.getChat().getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    public String chat(List<ChatMessageContext> contextMessages) {
        return chat(promptContextBuilder.buildSystemPrompt(), contextMessages);
    }

    public String chat(List<ChatMessageContext> contextMessages, String userMessage) {
        return chat(promptContextBuilder.buildSystemPrompt(), contextMessages, userMessage, List.of());
    }

    public String chat(List<ChatMessageContext> contextMessages,
                       String userMessage,
                       List<RetrievedMemory> retrievedMemories) {
        return chat(promptContextBuilder.buildSystemPrompt(), contextMessages, userMessage, retrievedMemories);
    }

    public String chat(String systemPrompt, List<ChatMessageContext> contextMessages) {
        return sendChatRequest(systemPrompt, List.of(), contextMessages);
    }

    public String chat(String systemPrompt,
                       List<ChatMessageContext> contextMessages,
                       String userMessage,
                       List<RetrievedMemory> retrievedMemories) {
        List<ChatMessageContext> messages = new ArrayList<>(contextMessages);
        if (StringUtils.hasText(userMessage)) {
            messages.add(new ChatMessageContext("user", userMessage));
        }
        return sendChatRequest(systemPrompt, retrievedMemories, messages);
    }

    private String sendChatRequest(String systemPrompt,
                                   List<RetrievedMemory> retrievedMemories,
                                   List<ChatMessageContext> contextMessages) {
        if (!StringUtils.hasText(deepSeekProperties.getApiKey())) {
            log.error("DeepSeek API key is missing");
            throw new DeepSeekChatException("DeepSeek API key is missing");
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        if (!retrievedMemories.isEmpty()) {
            messages.add(new ChatMessage("system", buildMemoryPrompt(retrievedMemories)));
        }
        for (ChatMessageContext contextMessage : contextMessages) {
            if (contextMessage == null
                    || !StringUtils.hasText(contextMessage.role())
                    || !StringUtils.hasText(contextMessage.content())) {
                continue;
            }
            messages.add(new ChatMessage(contextMessage.role(), contextMessage.content()));
        }

        DeepSeekChatRequest request = new DeepSeekChatRequest(
                deepSeekProperties.getModel(),
                messages,
                botProperties.getChat().getMaxReplyTokens(),
                0.7
        );

        try {
            DeepSeekChatResponse response = restClient.post()
                    .uri(normalizeBaseUrl(deepSeekProperties.getBaseUrl()) + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + deepSeekProperties.getApiKey().trim())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(DeepSeekChatResponse.class);

            String reply = extractReply(response);
            if (!StringUtils.hasText(reply)) {
                log.error("DeepSeek returned an empty reply");
                throw new DeepSeekChatException("DeepSeek returned an empty reply");
            }
            return reply.trim();
        } catch (HttpClientErrorException.Unauthorized ex) {
            log.error("DeepSeek request failed with 401 unauthorized");
            throw new DeepSeekChatException("DeepSeek unauthorized", ex);
        } catch (HttpClientErrorException.TooManyRequests ex) {
            log.error("DeepSeek request failed with 429 too many requests");
            throw new DeepSeekChatException("DeepSeek rate limited", ex);
        } catch (HttpServerErrorException ex) {
            log.error("DeepSeek request failed with server error status={}", ex.getStatusCode().value());
            throw new DeepSeekChatException("DeepSeek server error", ex);
        } catch (ResourceAccessException ex) {
            log.error("DeepSeek request failed due to network or timeout");
            throw new DeepSeekChatException("DeepSeek network or timeout error", ex);
        } catch (RestClientResponseException ex) {
            log.error("DeepSeek request failed with status={}", ex.getStatusCode().value());
            throw new DeepSeekChatException("DeepSeek response error", ex);
        } catch (DeepSeekChatException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("DeepSeek request failed unexpectedly");
            throw new DeepSeekChatException("DeepSeek unexpected error", ex);
        }
    }

    private String buildMemoryPrompt(List<RetrievedMemory> retrievedMemories) {
        StringBuilder builder = new StringBuilder("[长期记忆]\n");
        for (RetrievedMemory retrievedMemory : retrievedMemories) {
            builder.append("- ").append(retrievedMemory.content()).append('\n');
        }
        return builder.toString().trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private String extractReply(DeepSeekChatResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        DeepSeekChoice choice = response.choices().get(0);
        if (choice == null || choice.message() == null) {
            return null;
        }
        return choice.message().content();
    }

    private record DeepSeekChatRequest(
            String model,
            List<ChatMessage> messages,
            int max_tokens,
            double temperature
    ) {
    }

    private record ChatMessage(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepSeekChatResponse(List<DeepSeekChoice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepSeekChoice(DeepSeekResponseMessage message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DeepSeekResponseMessage(String content) {
    }
}
