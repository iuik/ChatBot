package com.chatbot.memory;

import com.chatbot.config.EmbeddingProperties;
import com.chatbot.memory.dto.EmbeddingRequest;
import com.chatbot.memory.dto.EmbeddingResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleEmbeddingClient.class);

    private final RestClient restClient;
    private final EmbeddingProperties embeddingProperties;

    public OpenAiCompatibleEmbeddingClient(RestClient.Builder restClientBuilder, EmbeddingProperties embeddingProperties) {
        this.embeddingProperties = embeddingProperties;
        Duration timeout = Duration.ofSeconds(embeddingProperties.getTimeoutSeconds());
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) timeout.toMillis());
        requestFactory.setReadTimeout((int) timeout.toMillis());
        this.restClient = restClientBuilder.requestFactory(requestFactory).build();
    }

    @Override
    public boolean isEnabled() {
        return embeddingProperties.isEnabled()
                && StringUtils.hasText(embeddingProperties.getBaseUrl())
                && StringUtils.hasText(embeddingProperties.getApiKey())
                && StringUtils.hasText(embeddingProperties.getModel());
    }

    @Override
    public List<Double> embed(String text) {
        if (!isEnabled()) {
            throw new IllegalStateException("Embedding client is disabled or not configured");
        }
        EmbeddingResponse response = restClient.post()
                .uri(normalizeBaseUrl(embeddingProperties.getBaseUrl()) + "/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + embeddingProperties.getApiKey().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new EmbeddingRequest(embeddingProperties.getModel(), text))
                .retrieve()
                .body(EmbeddingResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            log.warn("Embedding response is empty");
            throw new IllegalStateException("Embedding response is empty");
        }
        List<Double> vector = response.data().get(0).embedding();
        if (vector == null || vector.isEmpty()) {
            throw new IllegalStateException("Embedding vector is empty");
        }
        return List.copyOf(vector);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
