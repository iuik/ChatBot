package com.chatbot.memory.dto;

public record MemoryItem(
        Long id,
        String qqId,
        String content,
        int importance,
        boolean enabled
) {
}
