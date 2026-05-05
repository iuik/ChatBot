package com.chatbot.memory.dto;

public record RetrievedMemory(
        Long id,
        String content,
        double score
) {
}
