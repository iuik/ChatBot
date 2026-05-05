package com.chatbot.memory;

import java.util.List;

public interface EmbeddingClient {

    boolean isEnabled();

    List<Double> embed(String text);
}
