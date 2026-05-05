package com.chatbot.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.config.BotProperties;
import com.chatbot.memory.dto.MemoryItem;
import com.chatbot.repository.LongTermMemoryRecord;
import com.chatbot.repository.LongTermMemoryRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LongTermMemoryServiceTests {

    private LongTermMemoryRepository repository;
    private EmbeddingClient embeddingClient;
    private QdrantMemoryStore qdrantMemoryStore;
    private LongTermMemoryService longTermMemoryService;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(LongTermMemoryRepository.class);
        embeddingClient = org.mockito.Mockito.mock(EmbeddingClient.class);
        qdrantMemoryStore = org.mockito.Mockito.mock(QdrantMemoryStore.class);
        BotProperties botProperties = new BotProperties();
        botProperties.getMemory().setAutoExtractEnabled(false);
        longTermMemoryService = new LongTermMemoryService(
                repository,
                embeddingClient,
                qdrantMemoryStore,
                new MemoryExtractor(),
                botProperties
        );
    }

    @Test
    void memoryAddShouldSaveMysql() {
        when(repository.save(anyString(), anyString(), anyString(), any(), anyInt(), any()))
                .thenReturn(1L);
        when(embeddingClient.isEnabled()).thenReturn(false);

        String reply = longTermMemoryService.handleCommand(123456789L, "/memory add remember this");

        verify(repository).save(anyString(), isNull(), anyString(), any(), anyInt(), any());
        assertThat(reply).contains("已保存记忆");
    }

    @Test
    void memoryCommandShouldListEnabledMemories() {
        when(repository.findEnabledByQqId("123456789", 10)).thenReturn(List.of(
                record(1L, "123456789", "first"),
                record(2L, "123456789", "second")
        ));

        String reply = longTermMemoryService.handleCommand(123456789L, "/memory");

        assertThat(reply).contains("1. first");
        assertThat(reply).contains("2. second");
    }

    @Test
    void memoryDeleteShouldDisableMemory() {
        when(repository.findByIdAndQqId(1L, "123456789")).thenReturn(Optional.of(record(1L, "123456789", "first")));

        String reply = longTermMemoryService.handleCommand(123456789L, "/memory del 1");

        verify(repository).disable(1L, "123456789");
        assertThat(reply).contains("已删除记忆 1");
    }

    @Test
    void tooLongMemoryShouldReturnFriendlyMessage() {
        String reply = longTermMemoryService.handleCommand(123456789L, "/memory add " + "x".repeat(1001));

        assertThat(reply).contains("1000");
        verify(repository, never()).save(anyString(), anyString(), anyString(), any(), anyInt(), any());
    }

    @Test
    void getEnabledMemoriesShouldMapRecords() {
        when(repository.findEnabledByQqId("123456789", 5)).thenReturn(List.of(record(1L, "123456789", "first")));

        List<MemoryItem> items = longTermMemoryService.getEnabledMemories(123456789L, 5);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).content()).isEqualTo("first");
    }

    @Test
    void autoExtractDisabledShouldNotSaveOrdinaryQuestion() {
        longTermMemoryService.extractAndStore(123456789L, "你觉得我希望你以后怎么回答我？", 1L);
        longTermMemoryService.extractAndStore(123456789L, "以后怎么办？", 2L);
        longTermMemoryService.extractAndStore(123456789L, "我以后想学习 Java", 3L);

        verify(repository, never()).save(anyString(), any(), anyString(), any(), anyInt(), any());
    }

    private LongTermMemoryRecord record(Long id, String qqId, String content) {
        return new LongTermMemoryRecord(
                id,
                qqId,
                null,
                content,
                null,
                5,
                true,
                "point-" + id,
                Instant.now(),
                Instant.now()
        );
    }
}
