package com.chatbot.prompt.dto;

public record PromptProfile(
        Source source,
        String text,
        int length,
        boolean truncated
) {

    public enum Source {
        FILE,
        ENV,
        DEFAULT
    }
}
