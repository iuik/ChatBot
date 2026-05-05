package com.chatbot.memory.dto;

public record MemoryCandidate(
        String content,
        Long sourceMessageId,
        int importance
) {
}
