package com.chatbot.memory.dto;

public record EmbeddingRequest(
        String model,
        String input
) {
}
