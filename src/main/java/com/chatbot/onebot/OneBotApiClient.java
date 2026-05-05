package com.chatbot.onebot;

import com.chatbot.config.BotProperties;
import com.chatbot.onebot.dto.OneBotSendPrivateMessageRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class OneBotApiClient {

    private static final Logger log = LoggerFactory.getLogger(OneBotApiClient.class);

    private final RestClient restClient;
    private final BotProperties botProperties;

    public OneBotApiClient(RestClient.Builder restClientBuilder, BotProperties botProperties) {
        this.botProperties = botProperties;
        this.restClient = restClientBuilder.build();
    }

    public Long sendPrivateMessage(Long userId, String message) {
        OneBotSendPrivateMessageRequest request = new OneBotSendPrivateMessageRequest(userId, message);

        try {
            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri(normalizeBaseUrl(botProperties.getOnebot().getApiBaseUrl()) + "/send_private_msg")
                    .contentType(MediaType.APPLICATION_JSON);

            String accessToken = botProperties.getOnebot().getAccessToken();
            if (StringUtils.hasText(accessToken)) {
                requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.trim());
            }

            Map<?, ?> response = requestSpec.body(request).retrieve().body(Map.class);
            return extractMessageId(response);
        } catch (Exception ex) {
            log.error("Failed to send OneBot private message to userId={}", userId, ex);
            return null;
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    private Long extractMessageId(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            return null;
        }
        Object messageId = dataMap.get("message_id");
        if (messageId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
