package com.chatbot.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryRetrieverTests {

    private EmbeddingClient embeddingClient;
    private QdrantMemoryStore qdrantMemoryStore;
    private MemoryRetriever memoryRetriever;

    @BeforeEach
    void setUp() {
        embeddingClient = org.mockito.Mockito.mock(EmbeddingClient.class);
        qdrantMemoryStore = org.mockito.Mockito.mock(QdrantMemoryStore.class);
        memoryRetriever = new MemoryRetriever(embeddingClient, qdrantMemoryStore);
    }

    @Test
    void shouldReturnEmptyWhenQdrantDisabled() {
        when(embeddingClient.isEnabled()).thenReturn(true);
        when(qdrantMemoryStore.isEnabled()).thenReturn(false);

        assertThat(memoryRetriever.retrieveRelevantMemories(123456789L, "hello")).isEmpty();
        verify(embeddingClient, never()).embed("hello");
    }

    @Test
    void shouldReturnEmptyWhenEmbeddingDisabled() {
        when(embeddingClient.isEnabled()).thenReturn(false);

        assertThat(memoryRetriever.retrieveRelevantMemories(123456789L, "hello")).isEmpty();
        verify(qdrantMemoryStore, never()).search("123456789", List.of(), 5);
    }
}
