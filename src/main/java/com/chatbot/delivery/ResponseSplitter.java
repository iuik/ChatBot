package com.chatbot.delivery;

import com.chatbot.config.BotProperties;
import com.chatbot.delivery.dto.DeliveryMessagePart;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ResponseSplitter {

    private static final String CODE_BLOCK_MARKER = "```";
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("(?s)^\\s*[\\[{].*[\\]}]\\s*$");
    private static final Pattern SQL_LINE_PATTERN = Pattern.compile("(?im)^\\s*(select|insert|update|delete|create|alter|drop|with)\\b");
    private static final Pattern COMMAND_LINE_PATTERN = Pattern.compile("(?m)^\\s*([/$]|[a-zA-Z]:\\\\|\\.\\\\|\\.\\./|npm\\s|mvn\\s|gradle\\s|git\\s|redis-cli\\s|curl\\s)");

    private final BotProperties botProperties;

    public ResponseSplitter(BotProperties botProperties) {
        this.botProperties = botProperties;
    }

    public List<DeliveryMessagePart> split(String text, DeliveryMode mode) {
        String normalized = normalize(text);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        if (!shouldSplit(mode, normalized)) {
            return List.of(new DeliveryMessagePart(1, normalized));
        }

        int maxPartChars = Math.max(1, botProperties.getDelivery().getMaxPartChars());
        List<String> parts = splitInternal(normalized, maxPartChars, Math.max(1, botProperties.getDelivery().getMaxParts()));
        if (parts.isEmpty()) {
            return List.of(new DeliveryMessagePart(1, normalized));
        }

        List<DeliveryMessagePart> messageParts = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            messageParts.add(new DeliveryMessagePart(i + 1, parts.get(i)));
        }
        return List.copyOf(messageParts);
    }

    private boolean shouldSplit(DeliveryMode mode, String text) {
        if (!botProperties.getDelivery().isSplitEnabled()) {
            return false;
        }
        if (botProperties.getDelivery().isSplitDailyChatOnly() && mode != DeliveryMode.DAILY_CHAT) {
            return false;
        }
        if (mode != DeliveryMode.DAILY_CHAT) {
            return false;
        }
        if (containsProtectedContent(text)) {
            return false;
        }
        return text.length() > botProperties.getDelivery().getMaxPartChars();
    }

    private boolean containsProtectedContent(String text) {
        if (text.contains(CODE_BLOCK_MARKER)) {
            return true;
        }
        if (!text.contains("\n")) {
            return false;
        }
        if (JSON_BLOCK_PATTERN.matcher(text).matches()) {
            return true;
        }
        if (SQL_LINE_PATTERN.matcher(text).find()) {
            return true;
        }
        return COMMAND_LINE_PATTERN.matcher(text).find();
    }

    private List<String> splitInternal(String text, int maxPartChars, int maxParts) {
        List<String> semanticSegments = splitByNewline(text);
        semanticSegments = expandLongSegments(semanticSegments, maxPartChars, true);
        semanticSegments = expandLongSegments(semanticSegments, maxPartChars, false);
        semanticSegments = expandFallbackSegments(semanticSegments, maxPartChars);
        return mergeOverflow(packSegments(semanticSegments, maxPartChars), maxParts);
    }

    private List<String> splitByNewline(String text) {
        List<String> segments = new ArrayList<>();
        for (String line : text.split("\n")) {
            String normalizedLine = normalize(line);
            if (StringUtils.hasText(normalizedLine)) {
                segments.add(normalizedLine);
            }
        }
        return segments.isEmpty() ? List.of(text) : List.copyOf(segments);
    }

    private List<String> expandLongSegments(List<String> segments, int maxPartChars, boolean strongPunctuation) {
        List<String> expanded = new ArrayList<>();
        for (String segment : segments) {
            if (segment.length() <= maxPartChars) {
                expanded.add(segment);
                continue;
            }
            List<String> splitParts = splitByPunctuation(segment, strongPunctuation);
            if (splitParts.size() == 1) {
                expanded.add(segment);
                continue;
            }
            expanded.addAll(splitParts);
        }
        return List.copyOf(expanded);
    }

    private List<String> expandFallbackSegments(List<String> segments, int maxPartChars) {
        List<String> expanded = new ArrayList<>();
        for (String segment : segments) {
            if (segment.length() <= maxPartChars) {
                expanded.add(segment);
                continue;
            }
            String remaining = segment;
            while (remaining.length() > maxPartChars) {
                int splitIndex = findWhitespaceSplitIndex(remaining, maxPartChars);
                if (splitIndex <= 0) {
                    splitIndex = maxPartChars;
                }
                expanded.add(normalize(remaining.substring(0, splitIndex)));
                remaining = normalize(remaining.substring(splitIndex));
            }
            if (StringUtils.hasText(remaining)) {
                expanded.add(remaining);
            }
        }
        return List.copyOf(expanded);
    }

    private List<String> splitByPunctuation(String text, boolean strongPunctuation) {
        List<String> segments = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            builder.append(current);
            if (matchesEllipsis(text, i)) {
                i++;
                builder.append(text.charAt(i));
                flushSegment(segments, builder);
                continue;
            }
            if (matchesAsciiEllipsis(text, i)) {
                builder.append(text.charAt(++i));
                builder.append(text.charAt(++i));
                flushSegment(segments, builder);
                continue;
            }
            if (isSplitPunctuation(current, strongPunctuation)) {
                flushSegment(segments, builder);
            }
        }
        flushSegment(segments, builder);
        return segments.isEmpty() ? List.of(text) : List.copyOf(segments);
    }

    private boolean isSplitPunctuation(char current, boolean strongPunctuation) {
        if (strongPunctuation) {
            return current == '。' || current == '？' || current == '！';
        }
        return current == '，' || current == '、' || current == '；';
    }

    private boolean matchesEllipsis(String text, int index) {
        return index + 1 < text.length() && text.charAt(index) == '…' && text.charAt(index + 1) == '…';
    }

    private boolean matchesAsciiEllipsis(String text, int index) {
        return index + 2 < text.length()
                && text.charAt(index) == '.'
                && text.charAt(index + 1) == '.'
                && text.charAt(index + 2) == '.';
    }

    private void flushSegment(List<String> segments, StringBuilder builder) {
        String segment = normalize(builder.toString());
        if (StringUtils.hasText(segment)) {
            segments.add(segment);
        }
        builder.setLength(0);
    }

    private List<String> packSegments(List<String> segments, int maxPartChars) {
        List<String> packed = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String segment : segments) {
            if (current.length() == 0) {
                current.append(segment);
                continue;
            }
            if (current.length() + segment.length() <= maxPartChars) {
                current.append(segment);
                continue;
            }
            packed.add(current.toString());
            current.setLength(0);
            current.append(segment);
        }
        if (current.length() > 0) {
            packed.add(current.toString());
        }
        return List.copyOf(packed);
    }

    private int findWhitespaceSplitIndex(String text, int maxPartChars) {
        for (int i = Math.min(maxPartChars - 1, text.length() - 1); i >= 0; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i + 1;
            }
        }
        return -1;
    }

    private List<String> mergeOverflow(List<String> parts, int maxParts) {
        if (parts.size() <= maxParts) {
            return List.copyOf(parts);
        }
        List<String> merged = new ArrayList<>(parts.subList(0, maxParts - 1));
        StringBuilder lastPart = new StringBuilder();
        for (int i = maxParts - 1; i < parts.size(); i++) {
            if (lastPart.length() > 0) {
                lastPart.append('\n');
            }
            lastPart.append(parts.get(i));
        }
        merged.add(lastPart.toString());
        return List.copyOf(merged);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").trim();
    }
}
