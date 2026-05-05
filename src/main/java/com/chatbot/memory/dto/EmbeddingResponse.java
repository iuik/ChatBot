package com.chatbot.memory.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EmbeddingResponse(List<EmbeddingData> data) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddingData(List<Double> embedding) {
    }
}
