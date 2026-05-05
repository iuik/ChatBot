package com.chatbot.chatpush;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chatbot.memory.MemoryRetriever;
import com.chatbot.repository.ChatMessageRecord;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.LongTermMemoryRecord;
import com.chatbot.repository.LongTermMemoryRepository;
import com.chatbot.repository.ProactiveTaskRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChatPushTopicSourceTests {

    @Test
    void shouldFallbackToMysqlMemoriesWhenRetrieverReturnsEmpty() {
        ProactiveTaskRepository proactiveTaskRepository = org.mockito.Mockito.mock(ProactiveTaskRepository.class);
        MemoryRetriever memoryRetriever = org.mockito.Mockito.mock(MemoryRetriever.class);
        LongTermMemoryRepository longTermMemoryRepository = org.mockito.Mockito.mock(LongTermMemoryRepository.class);
        ChatMessageRepository chatMessageRepository = org.mockito.Mockito.mock(ChatMessageRepository.class);
        ChatPushTopicSource source = new ChatPushTopicSource(
                proactiveTaskRepository,
                memoryRetriever,
                longTermMemoryRepository,
                chatMessageRepository
        );
        when(proactiveTaskRepository.findUpcoming("123456789", 3)).thenReturn(List.of());
        when(memoryRetriever.retrieveRelevantMemories(123456789L, "项目 学习 计划 进度 提醒")).thenReturn(List.of());
        when(longTermMemoryRepository.findEnabledByQqId("123456789", 3)).thenReturn(List.of(
                new LongTermMemoryRecord(1L, "123456789", null, "OneBot + Spring Boot QQ AI 助手项目", null, 5, true, null,
                        Instant.now(), Instant.now())
        ));
        when(chatMessageRepository.findRecentByQqId(123456789L, 10)).thenReturn(List.of(
                new ChatMessageRecord(1L, 123456789L, "private:123456789", "user", "最近在做主动话题模块", null, LocalDateTime.now())
        ));

        var context = source.buildContext(123456789L);

        assertThat(context.memories()).contains("OneBot + Spring Boot QQ AI 助手项目");
        assertThat(context.candidate().source()).isEqualTo("MEMORY");
    }
}
