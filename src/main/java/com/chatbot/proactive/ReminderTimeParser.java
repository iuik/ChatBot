package com.chatbot.proactive;

import com.chatbot.proactive.dto.CreateReminderCommand;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReminderTimeParser {

    private static final Pattern FULL_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\s+(\\d{1,2}:\\d{2})\\s+(.+)$");
    private static final Pattern TOMORROW_PATTERN = Pattern.compile("^(?:tomorrow|明天)\\s+(\\d{1,2}:\\d{2})\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TIME_ONLY_PATTERN = Pattern.compile("^(\\d{1,2}:\\d{2})\\s+(.+)$");
    private static final Pattern FULL_DATE_TIME_TEXT_PATTERN =
            Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}");
    private static final Pattern ENGLISH_TOMORROW_TIME_PATTERN =
            Pattern.compile("tomorrow\\s+\\d{1,2}:\\d{2}", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHINESE_TIME_PATTERN =
            Pattern.compile("(下周[一二三四五六日天]|后天|明天|今天|今晚)(凌晨|早上|上午|中午|下午|晚上)?\\s*\\d{1,2}(?::\\d{2})?(?:点|时)?");
    private static final Pattern CHINESE_PERIOD_PATTERN =
            Pattern.compile("(下周[一二三四五六日天]|后天|明天|今天)(凌晨|早上|上午|中午|下午|晚上)");
    private static final Pattern TIME_ONLY_TEXT_PATTERN =
            Pattern.compile("\\b\\d{1,2}:\\d{2}\\b");
    private static final Pattern CHINESE_HOUR_ONLY_PATTERN =
            Pattern.compile("(下周[一二三四五六日天]|后天|明天|今天|今晚)(凌晨|早上|上午|中午|下午|晚上)?\\s*\\d{1,2}(?:点|时)");

    private final Clock clock;
    private final ReminderTextNormalizer textNormalizer;

    @Autowired
    public ReminderTimeParser(ReminderTextNormalizer textNormalizer) {
        this(Clock.systemDefaultZone(), textNormalizer);
    }

    ReminderTimeParser(Clock clock, ReminderTextNormalizer textNormalizer) {
        this.clock = clock;
        this.textNormalizer = textNormalizer;
    }

    public CreateReminderCommand parse(String input, ZoneId zoneId) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        String normalized = textNormalizer.normalize(input);
        Matcher fullMatcher = FULL_PATTERN.matcher(normalized);
        if (fullMatcher.matches()) {
            LocalDate date = parseDate(fullMatcher.group(1));
            LocalTime time = parseClockTime(fullMatcher.group(2));
            if (date == null || time == null) {
                return null;
            }
            LocalDateTime scheduledAt = LocalDateTime.of(date, time);
            if (scheduledAt.isBefore(now(zoneId))) {
                return null;
            }
            return new CreateReminderCommand(scheduledAt, fullMatcher.group(3).trim());
        }

        Matcher tomorrowMatcher = TOMORROW_PATTERN.matcher(normalized);
        if (tomorrowMatcher.matches()) {
            LocalTime time = parseClockTime(tomorrowMatcher.group(1));
            if (time == null) {
                return null;
            }
            LocalDate tomorrow = now(zoneId).toLocalDate().plusDays(1);
            return new CreateReminderCommand(LocalDateTime.of(tomorrow, time), tomorrowMatcher.group(2).trim());
        }

        Matcher timeOnlyMatcher = TIME_ONLY_PATTERN.matcher(normalized);
        if (timeOnlyMatcher.matches()) {
            LocalTime time = parseClockTime(timeOnlyMatcher.group(1));
            if (time == null) {
                return null;
            }
            LocalDateTime now = now(zoneId);
            LocalDate targetDate = now.toLocalDate();
            if (!time.isAfter(now.toLocalTime())) {
                targetDate = targetDate.plusDays(1);
            }
            return new CreateReminderCommand(LocalDateTime.of(targetDate, time), timeOnlyMatcher.group(2).trim());
        }

        return null;
    }

    public LocalDateTime parseDateTimeText(String text, ZoneId zoneId) {
        ReminderTimeReference reference = parseTimeReference(text, zoneId);
        if (reference == null || !reference.exact()) {
            return null;
        }
        return reference.scheduledAt();
    }

    public ReminderTimeReference parseTimeReference(String text, ZoneId zoneId) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = textNormalizer.normalize(text);
        LocalDateTime fullDateTime = parseFullDateTimeText(normalized);
        if (fullDateTime != null) {
            return ReminderTimeReference.exact(fullDateTime);
        }

        LocalDateTime tomorrowTime = parseTomorrowTimeText(normalized, zoneId);
        if (tomorrowTime != null) {
            return ReminderTimeReference.exact(tomorrowTime);
        }

        LocalDateTime chineseNaturalTime = parseChineseNaturalTime(normalized, zoneId);
        if (chineseNaturalTime != null) {
            return ReminderTimeReference.exact(chineseNaturalTime);
        }

        ReminderTimeReference periodReference = parseChinesePeriodReference(normalized, zoneId);
        if (periodReference != null) {
            return periodReference;
        }

        if (TIME_ONLY_TEXT_PATTERN.matcher(normalized).matches()) {
            LocalTime time = parseClockTime(normalized);
            if (time == null) {
                return null;
            }
            LocalDateTime now = now(zoneId);
            LocalDate targetDate = now.toLocalDate();
            if (!time.isAfter(now.toLocalTime())) {
                targetDate = targetDate.plusDays(1);
            }
            return ReminderTimeReference.exact(LocalDateTime.of(targetDate, time));
        }

        return null;
    }

    public String extractFirstTimeExpression(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = textNormalizer.normalize(text);
        String[] orderedMatches = {
                find(FULL_DATE_TIME_TEXT_PATTERN, normalized),
                find(ENGLISH_TOMORROW_TIME_PATTERN, normalized),
                find(CHINESE_TIME_PATTERN, normalized),
                find(CHINESE_HOUR_ONLY_PATTERN, normalized),
                find(CHINESE_PERIOD_PATTERN, normalized),
                find(TIME_ONLY_TEXT_PATTERN, normalized)
        };
        for (String match : orderedMatches) {
            if (StringUtils.hasText(match)) {
                return match.trim();
            }
        }
        return null;
    }

    public String normalize(String text) {
        return textNormalizer.normalize(text);
    }

    private LocalDateTime parseFullDateTimeText(String normalized) {
        Matcher matcher = FULL_DATE_TIME_TEXT_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String[] parts = matcher.group().split("\\s+");
        LocalDate date = parseDate(parts[0]);
        LocalTime time = parseClockTime(parts[1]);
        if (date == null || time == null) {
            return null;
        }
        return LocalDateTime.of(date, time);
    }

    private LocalDateTime parseTomorrowTimeText(String normalized, ZoneId zoneId) {
        Matcher matcher = ENGLISH_TOMORROW_TIME_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String[] parts = matcher.group().split("\\s+");
        LocalTime time = parseClockTime(parts[1]);
        if (time == null) {
            return null;
        }
        return LocalDateTime.of(now(zoneId).toLocalDate().plusDays(1), time);
    }

    private LocalDateTime parseChineseNaturalTime(String normalized, ZoneId zoneId) {
        String match = find(CHINESE_TIME_PATTERN, normalized);
        if (!StringUtils.hasText(match)) {
            match = find(CHINESE_HOUR_ONLY_PATTERN, normalized);
        }
        if (!StringUtils.hasText(match)) {
            return null;
        }
        Matcher matcher = Pattern.compile("(下周[一二三四五六日天]|后天|明天|今天|今晚)(凌晨|早上|上午|中午|下午|晚上)?\\s*(\\d{1,2})(?::(\\d{2}))?(?:点|时)?")
                .matcher(match);
        if (!matcher.matches()) {
            return null;
        }
        LocalDate date = resolveDateToken(matcher.group(1), zoneId);
        if (date == null) {
            return null;
        }
        LocalTime time = resolveNaturalTime(
                matcher.group(2),
                Integer.parseInt(matcher.group(3)),
                matcher.group(4) == null ? 0 : Integer.parseInt(matcher.group(4))
        );
        if (time == null) {
            return null;
        }
        return LocalDateTime.of(date, time);
    }

    private ReminderTimeReference parseChinesePeriodReference(String normalized, ZoneId zoneId) {
        Matcher matcher = CHINESE_PERIOD_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        LocalDate date = resolveDateToken(matcher.group(1), zoneId);
        if (date == null) {
            return null;
        }
        String period = matcher.group(2);
        LocalTime[] range = periodRange(period);
        return ReminderTimeReference.range(LocalDateTime.of(date, range[0]), LocalDateTime.of(date, range[1]));
    }

    private LocalDate resolveDateToken(String token, ZoneId zoneId) {
        LocalDate today = now(zoneId).toLocalDate();
        return switch (token) {
            case "今天", "今晚" -> today;
            case "明天" -> today.plusDays(1);
            case "后天" -> today.plusDays(2);
            default -> {
                if (token.startsWith("下周")) {
                    yield nextWeekday(today, token.substring(2));
                }
                yield null;
            }
        };
    }

    private LocalDate nextWeekday(LocalDate today, String dayToken) {
        DayOfWeek dayOfWeek = switch (dayToken) {
            case "一" -> DayOfWeek.MONDAY;
            case "二" -> DayOfWeek.TUESDAY;
            case "三" -> DayOfWeek.WEDNESDAY;
            case "四" -> DayOfWeek.THURSDAY;
            case "五" -> DayOfWeek.FRIDAY;
            case "六" -> DayOfWeek.SATURDAY;
            case "日", "天" -> DayOfWeek.SUNDAY;
            default -> null;
        };
        if (dayOfWeek == null) {
            return null;
        }
        return today.with(TemporalAdjusters.next(dayOfWeek));
    }

    private LocalTime resolveNaturalTime(String period, int hour, int minute) {
        if (minute < 0 || minute > 59) {
            return null;
        }
        int normalizedHour = hour;
        if ("下午".equals(period) || "晚上".equals(period)) {
            if (hour < 12) {
                normalizedHour = hour + 12;
            }
        } else if ("中午".equals(period)) {
            normalizedHour = hour == 0 ? 12 : hour;
            if (normalizedHour < 11) {
                normalizedHour += 12;
            }
        }
        if (normalizedHour < 0 || normalizedHour > 23) {
            return null;
        }
        return LocalTime.of(normalizedHour, minute);
    }

    private LocalTime[] periodRange(String period) {
        return switch (period) {
            case "凌晨" -> new LocalTime[]{LocalTime.of(0, 0), LocalTime.of(5, 59)};
            case "早上", "上午" -> new LocalTime[]{LocalTime.of(6, 0), LocalTime.of(10, 0)};
            case "中午" -> new LocalTime[]{LocalTime.of(11, 0), LocalTime.of(12, 59)};
            case "下午" -> new LocalTime[]{LocalTime.of(13, 0), LocalTime.of(17, 59)};
            case "晚上" -> new LocalTime[]{LocalTime.of(18, 0), LocalTime.of(23, 59)};
            default -> new LocalTime[]{LocalTime.MIN, LocalTime.MAX};
        };
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalTime parseClockTime(String value) {
        try {
            return LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm"));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private LocalDateTime now(ZoneId zoneId) {
        return LocalDateTime.now(clock.withZone(zoneId));
    }

    public record ReminderTimeReference(
            LocalDateTime scheduledAt,
            LocalDateTime windowStart,
            LocalDateTime windowEnd,
            boolean exact
    ) {

        static ReminderTimeReference exact(LocalDateTime scheduledAt) {
            return new ReminderTimeReference(scheduledAt, scheduledAt, scheduledAt, true);
        }

        static ReminderTimeReference range(LocalDateTime windowStart, LocalDateTime windowEnd) {
            return new ReminderTimeReference(null, windowStart, windowEnd, false);
        }
    }
}
