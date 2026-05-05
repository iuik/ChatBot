package com.chatbot.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.chatbot.config.EmbeddingProperties;
import com.chatbot.config.QdrantProperties;
import com.chatbot.memory.dto.RetrievedMemory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class QdrantMemoryStore {

    private static final Logger log = LoggerFactory.getLogger(QdrantMemoryStore.class);

    private final RestClient restClient;
    private final QdrantProperties qdrantProperties;
    private volatile boolean collectionEnsured;

    public QdrantMemoryStore(RestClient.Builder restClientBuilder,
                             QdrantProperties qdrantProperties,
                             EmbeddingProperties embeddingProperties) {
        this.restClient = restClientBuilder.build();
        this.qdrantProperties = qdrantProperties;
        if (qdrantProperties.isEnabled()
                && embeddingProperties.isEnabled()
                && qdrantProperties.getVectorSize() != embeddingProperties.getDimension()) {
            throw new IllegalStateException("Qdrant vector size must match embedding dimension");
        }
        ensureCollectionExists();
    }

    public boolean isEnabled() {
        return qdrantProperties.isEnabled();
    }

    public void upsert(String pointId, List<Double> vector, Map<String, Object> payload) {
        if (!isEnabled()) {
            return;
        }
        if (!ensureCollectionExists()) {
            throw new IllegalStateException("Qdrant collection is unavailable");
        }
        RestClient.RequestBodySpec requestSpec = restClient.put()
                .uri(baseUrl() + "/collections/" + qdrantProperties.getCollection() + "/points")
                .contentType(MediaType.APPLICATION_JSON);
        applyAuth(requestSpec);
        requestSpec
                .body(Map.of(
                        "points", List.of(Map.of(
                                "id", pointId,
                                "vector", vector,
                                "payload", payload
                        ))
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public List<RetrievedMemory> search(String qqId, List<Double> vector, int limit) {
        if (!isEnabled()) {
            return List.of();
        }
        if (!ensureCollectionExists()) {
            return List.of();
        }

        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri(baseUrl() + "/collections/" + qdrantProperties.getCollection() + "/points/search")
                .contentType(MediaType.APPLICATION_JSON);
        applyAuth(requestSpec);
        JsonNode response = requestSpec
                .body(Map.of(
                        "vector", vector,
                        "limit", limit,
                        "with_payload", true,
                        "filter", Map.of(
                                "must", List.of(
                                        Map.of("key", "qq_id", "match", Map.of("value", qqId)),
                                        Map.of("key", "enabled", "match", Map.of("value", true))
                                )
                        )
                ))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || response.path("result").isMissingNode()) {
            return List.of();
        }

        List<RetrievedMemory> memories = new ArrayList<>();
        for (JsonNode item : response.path("result")) {
            JsonNode payload = item.path("payload");
            memories.add(new RetrievedMemory(
                    payload.path("memory_id").asLong(),
                    payload.path("content").asText(),
                    item.path("score").asDouble()
            ));
        }
        return List.copyOf(memories);
    }

    public void delete(String pointId) {
        if (!isEnabled() || !StringUtils.hasText(pointId)) {
            return;
        }
        if (!ensureCollectionExists()) {
            return;
        }
        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri(baseUrl() + "/collections/" + qdrantProperties.getCollection() + "/points/delete")
                .contentType(MediaType.APPLICATION_JSON);
        applyAuth(requestSpec);
        requestSpec
                .body(Map.of("points", List.of(pointId)))
                .retrieve()
                .toBodilessEntity();
    }

    private synchronized boolean ensureCollectionExists() {
        if (!isEnabled()) {
            return false;
        }
        if (collectionEnsured) {
            return true;
        }
        try {
            RestClient.RequestHeadersSpec<?> getSpec = restClient.get()
                    .uri(baseUrl() + "/collections/" + qdrantProperties.getCollection());
            applyAuth(getSpec);
            getSpec
                    .retrieve()
                    .toBodilessEntity();
            collectionEnsured = true;
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            try {
                RestClient.RequestBodySpec createSpec = restClient.put()
                        .uri(baseUrl() + "/collections/" + qdrantProperties.getCollection())
                        .contentType(MediaType.APPLICATION_JSON);
                applyAuth(createSpec);
                createSpec
                        .body(Map.of(
                                "vectors", Map.of(
                                        "size", qdrantProperties.getVectorSize(),
                                        "distance", "Cosine"
                                )
                        ))
                        .retrieve()
                        .toBodilessEntity();
                collectionEnsured = true;
                return true;
            } catch (Exception createEx) {
                log.warn("Failed to create Qdrant collection {}", qdrantProperties.getCollection(), createEx);
                return false;
            }
        } catch (Exception ex) {
            log.warn("Failed to access Qdrant collection {}", qdrantProperties.getCollection(), ex);
            return false;
        }
    }

    private void applyAuth(RestClient.RequestHeadersSpec<?> requestSpec) {
        if (StringUtils.hasText(qdrantProperties.getApiKey())) {
            requestSpec.header(HttpHeaders.AUTHORIZATION, "Bearer " + qdrantProperties.getApiKey().trim());
        }
    }

    private String baseUrl() {
        return "http://" + qdrantProperties.getHost() + ":" + qdrantProperties.getPort();
    }
}
