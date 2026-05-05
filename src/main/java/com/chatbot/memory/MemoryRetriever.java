package com.chatbot.memory;

import com.chatbot.memory.dto.RetrievedMemory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MemoryRetriever {

    private static final Logger log = LoggerFactory.getLogger(MemoryRetriever.class);

    private final EmbeddingClient embeddingClient;
    private final QdrantMemoryStore qdrantMemoryStore;

    public MemoryRetriever(EmbeddingClient embeddingClient, QdrantMemoryStore qdrantMemoryStore) {
        this.embeddingClient = embeddingClient;
        this.qdrantMemoryStore = qdrantMemoryStore;
    }

    public List<RetrievedMemory> retrieveRelevantMemories(Long qqId, String message) {
        if (qqId == null || !StringUtils.hasText(message)) {
            return List.of();
        }
        if (!embeddingClient.isEnabled() || !qdrantMemoryStore.isEnabled()) {
            return List.of();
        }
        try {
            List<Double> vector = embeddingClient.embed(message);
            return qdrantMemoryStore.search(String.valueOf(qqId), vector, 5);
        } catch (Exception ex) {
            log.warn("Failed to retrieve long term memory for qqId={}", qqId, ex);
            return List.of();
        }
    }
}
