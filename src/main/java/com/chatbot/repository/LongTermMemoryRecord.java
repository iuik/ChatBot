package com.chatbot.repository;

import java.time.Instant;

public record LongTermMemoryRecord(
        Long id,
        String qqId,
        String memoryKey,
        String content,
        Long sourceMessageId,
        int importance,
        boolean enabled,
        String qdrantPointId,
        Instant createdAt,
        Instant updatedAt
) {
}
