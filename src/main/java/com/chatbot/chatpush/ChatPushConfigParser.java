package com.chatbot.chatpush;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatPushConfigParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([mh])$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");

    public ParsedCommand parse(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return ParsedCommand.unmatched();
        }
        String text = rawText.trim().replace('\u3000', ' ');
        if ("/chatpush config".equals(text)) {
            return new ParsedCommand(true, CommandType.CONFIG, null, null, null, null, null, null);
        }
        if ("/chatpush reset".equals(text)) {
            return new ParsedCommand(true, CommandType.RESET, null, null, null, null, null, null);
        }
        if (!text.startsWith("/chatpush set ")) {
            return ParsedCommand.unmatched();
        }

        String body = text.substring("/chatpush set ".length()).trim();
        if (body.startsWith("idle ")) {
            Integer minutes = parseDurationMinutes(body.substring("idle ".length()).trim());
            return new ParsedCommand(true, CommandType.SET_IDLE, minutes, null, null, null, null, null);
        }
        if (body.startsWith("cooldown ")) {
            Integer minutes = parseDurationMinutes(body.substring("cooldown ".length()).trim());
            return new ParsedCommand(true, CommandType.SET_COOLDOWN, minutes, null, null, null, null, null);
        }
        if (body.startsWith("quiet ")) {
            String quietBody = body.substring("quiet ".length()).trim();
            if ("off".equalsIgnoreCase(quietBody)) {
                return new ParsedCommand(true, CommandType.SET_QUIET_OFF, null, null, null, null, null, null);
            }
            if ("on".equalsIgnoreCase(quietBody)) {
                return new ParsedCommand(true, CommandType.SET_QUIET_ON, null, null, null, null, null, null);
            }
            String[] parts = quietBody.split("\\s+");
            if (parts.length != 2) {
                return new ParsedCommand(true, CommandType.INVALID, null, null, null, null, null, Usage.QUIET);
            }
            String start = normalizeTime(parts[0]);
            String end = normalizeTime(parts[1]);
            if (start == null || end == null) {
                return new ParsedCommand(true, CommandType.INVALID, null, null, null, null, null, Usage.QUIET);
            }
            return new ParsedCommand(true, CommandType.SET_QUIET_TIME, null, null, start, end, null, null);
        }
        if (body.startsWith("max ")) {
            String maxBody = body.substring("max ".length()).trim().toLowerCase(Locale.ROOT);
            if ("off".equals(maxBody)) {
                return new ParsedCommand(true, CommandType.SET_MAX_OFF, null, null, null, null, null, null);
            }
            if ("on".equals(maxBody)) {
                return new ParsedCommand(true, CommandType.SET_MAX_ON, null, null, null, null, null, null);
            }
            try {
                Integer value = Integer.valueOf(maxBody);
                return new ParsedCommand(true, CommandType.SET_MAX_VALUE, null, value, null, null, null, null);
            } catch (NumberFormatException ex) {
                return new ParsedCommand(true, CommandType.INVALID, null, null, null, null, null, Usage.MAX);
            }
        }
        return new ParsedCommand(true, CommandType.INVALID, null, null, null, null, null, Usage.GENERAL);
    }

    private Integer parseDurationMinutes(String value) {
        Matcher matcher = DURATION_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        int amount = Integer.parseInt(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        return "h".equals(unit) ? amount * 60 : amount;
    }

    private String normalizeTime(String value) {
        String normalized = value.replace('：', ':');
        return TIME_PATTERN.matcher(normalized).matches() ? normalized : null;
    }

    public enum CommandType {
        CONFIG,
        RESET,
        SET_IDLE,
        SET_COOLDOWN,
        SET_QUIET_TIME,
        SET_QUIET_OFF,
        SET_QUIET_ON,
        SET_MAX_VALUE,
        SET_MAX_OFF,
        SET_MAX_ON,
        INVALID,
        UNMATCHED
    }

    public record ParsedCommand(
            boolean handled,
            CommandType type,
            Integer durationMinutes,
            Integer maxPerDay,
            String quietStart,
            String quietEnd,
            String field,
            String usage
    ) {

        static ParsedCommand unmatched() {
            return new ParsedCommand(false, CommandType.UNMATCHED, null, null, null, null, null, null);
        }
    }

    public static final class Usage {
        public static final String GENERAL = "用法：/chatpush config | /chatpush set idle 6h | /chatpush set cooldown 30m | /chatpush set quiet 23:30 08:30 | /chatpush set quiet on|off | /chatpush set max 2|on|off | /chatpush reset";
        public static final String QUIET = "用法：/chatpush set quiet 23:30 08:30，或 /chatpush set quiet on，或 /chatpush set quiet off";
        public static final String MAX = "用法：/chatpush set max 2，或 /chatpush set max on，或 /chatpush set max off";

        private Usage() {
        }
    }
}
