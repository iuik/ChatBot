package com.chatbot.memory;

import com.chatbot.memory.dto.MemoryCandidate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MemoryExtractor {

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "\u8bb0\u4f4f\uff1a",
            "\u8bf7\u8bb0\u4f4f",
            "\u4f60\u8981\u8bb0\u4f4f",
            "\u5e2e\u6211\u8bb0\u4f4f",
            "\u6211\u7684\u504f\u597d\u662f",
            "\u4ece\u73b0\u5728\u5f00\u59cb\u4f60\u8981",
            "\u4ee5\u540e\u8bf7\u4f60",
            "\u4ee5\u540e\u56de\u7b54\u6211\u65f6\u8bf7"
    );

    public Optional<MemoryCandidate> extract(String text, Long sourceMessageId) {
        if (!StringUtils.hasText(text)) {
            return Optional.empty();
        }

        String normalized = text.trim();
        if (looksLikeQuestion(normalized)) {
            return Optional.empty();
        }

        for (String prefix : ALLOWED_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                return Optional.of(new MemoryCandidate(normalized, sourceMessageId, 7));
            }
        }
        return Optional.empty();
    }

    private boolean looksLikeQuestion(String text) {
        return text.contains("?")
                || text.contains("\uff1f")
                || text.startsWith("\u4f60\u89c9\u5f97")
                || text.startsWith("\u4f60\u8ba4\u4e3a")
                || text.endsWith("\u5417")
                || text.endsWith("\u5462")
                || text.endsWith("\u4e48")
                || text.contains("\u600e\u4e48\u529e")
                || text.contains("\u4e3a\u4ec0\u4e48");
    }
}
