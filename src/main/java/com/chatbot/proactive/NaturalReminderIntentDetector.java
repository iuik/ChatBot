package com.chatbot.proactive;

import com.chatbot.config.BotProperties;
import java.util.Locale;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class NaturalReminderIntentDetector {

    private static final String[] CREATE_VERBS = {
            "提醒我", "记得叫我", "叫我", "喊我", "到时候提醒", "别忘了提醒我"
    };
    private static final String[] DELETE_MARKERS = {
            "删了", "删除", "取消", "不要了", "别提醒我"
    };
    private static final String[] LIST_MARKERS = {
            "提醒列表", "看看提醒", "查看提醒", "我的提醒", "有哪些提醒"
    };

    private final BotProperties botProperties;
    private final ReminderTimeParser reminderTimeParser;
    private final ReminderAiIntentService reminderAiIntentService;

    public NaturalReminderIntentDetector(BotProperties botProperties,
                                         ReminderTimeParser reminderTimeParser,
                                         @Nullable ReminderAiIntentService reminderAiIntentService) {
        this.botProperties = botProperties;
        this.reminderTimeParser = reminderTimeParser;
        this.reminderAiIntentService = reminderAiIntentService;
    }

    public ReminderIntent detect(String text) {
        if (!botProperties.getReminder().isNaturalLanguageEnabled() || !StringUtils.hasText(text)) {
            return ReminderIntent.unknown(text);
        }
        String normalized = reminderTimeParser.normalize(text);
        if (normalized.startsWith("/")) {
            return ReminderIntent.unknown(text);
        }

        ReminderIntent ruleIntent = detectByRule(normalized);
        if (ruleIntent.type() != ReminderIntentType.UNKNOWN) {
            return ruleIntent;
        }
        if (!botProperties.getReminder().isAiIntentEnabled() || reminderAiIntentService == null) {
            return ruleIntent;
        }
        ReminderIntent aiIntent = reminderAiIntentService.detect(normalized);
        return validateAiIntent(aiIntent, normalized);
    }

    private ReminderIntent detectByRule(String normalized) {
        if (isLikelyQuestion(normalized)) {
            return ReminderIntent.unknown(normalized);
        }
        for (String listMarker : LIST_MARKERS) {
            if (normalized.contains(listMarker)) {
                return new ReminderIntent(ReminderIntentType.LIST_REMINDER, null, null, 1.0, normalized);
            }
        }
        ReminderIntent deleteIntent = detectDelete(normalized);
        if (deleteIntent.type() != ReminderIntentType.UNKNOWN) {
            return deleteIntent;
        }
        ReminderIntent createIntent = detectCreate(normalized);
        if (createIntent.type() != ReminderIntentType.UNKNOWN) {
            return createIntent;
        }
        return ReminderIntent.unknown(normalized);
    }

    private ReminderIntent detectCreate(String normalized) {
        String matchedVerb = firstMatch(normalized, CREATE_VERBS);
        if (matchedVerb == null) {
            return ReminderIntent.unknown(normalized);
        }
        int verbIndex = normalized.indexOf(matchedVerb);
        String beforeVerb = normalized.substring(0, verbIndex).trim();
        String afterVerb = normalized.substring(verbIndex + matchedVerb.length()).trim();
        String timeText = reminderTimeParser.extractFirstTimeExpression(beforeVerb);
        if (!StringUtils.hasText(timeText) && StringUtils.hasText(afterVerb)) {
            timeText = reminderTimeParser.extractFirstTimeExpression(afterVerb);
            if (StringUtils.hasText(timeText)) {
                afterVerb = removeFirst(afterVerb, timeText).trim();
            }
        }
        String content = afterVerb;
        if ("叫我".equals(matchedVerb) || "记得叫我".equals(matchedVerb) || "喊我".equals(matchedVerb)) {
            content = matchedVerb.replace("记得", "") + afterVerb;
        }
        if (StringUtils.hasText(timeText)) {
            beforeVerb = removeFirst(beforeVerb, timeText).trim();
        }
        if (!StringUtils.hasText(content) && StringUtils.hasText(beforeVerb) && !StringUtils.hasText(timeText)) {
            content = beforeVerb;
        }
        if (!StringUtils.hasText(content)) {
            content = cleanupKeyword(afterVerb);
        }
        return new ReminderIntent(ReminderIntentType.CREATE_REMINDER, timeText, cleanupKeyword(content), 0.95, normalized);
    }

    private ReminderIntent detectDelete(String normalized) {
        String marker = firstMatch(normalized, DELETE_MARKERS);
        if (marker == null) {
            return ReminderIntent.unknown(normalized);
        }
        String body = normalized;
        if (normalized.startsWith("取消") || normalized.startsWith("删除")) {
            body = normalized.substring(2).trim();
        } else if (normalized.contains("删了")) {
            body = normalized.substring(0, normalized.indexOf("删了")).trim();
        } else if (normalized.contains("不要了")) {
            body = normalized.substring(0, normalized.indexOf("不要了")).trim();
        } else if (normalized.startsWith("别提醒我")) {
            body = normalized.substring("别提醒我".length()).trim();
        }
        String timeText = reminderTimeParser.extractFirstTimeExpression(body);
        if (StringUtils.hasText(timeText)) {
            body = removeFirst(body, timeText).trim();
        }
        String content = cleanupKeyword(body
                .replace("把", "")
                .replace("那个提醒", "")
                .replace("那个任务", "")
                .replace("的提醒", "")
                .replace("提醒", "")
                .replace("任务", "")
                .replace("别", ""));
        if (normalized.startsWith("别提醒我")) {
            content = cleanupKeyword(body);
        }
        return new ReminderIntent(ReminderIntentType.DELETE_REMINDER, timeText, content, 0.95, normalized);
    }

    private ReminderIntent validateAiIntent(ReminderIntent aiIntent, String normalized) {
        if (aiIntent == null || aiIntent.type() == null || aiIntent.confidence() < 0.75) {
            return ReminderIntent.unknown(normalized);
        }
        if (aiIntent.type() == ReminderIntentType.CREATE_REMINDER) {
            if (!StringUtils.hasText(aiIntent.timeText())
                    || !StringUtils.hasText(aiIntent.content())
                    || reminderTimeParser.parseDateTimeText(aiIntent.timeText(), defaultZone()) == null) {
                return ReminderIntent.unknown(normalized);
            }
        }
        if (aiIntent.type() == ReminderIntentType.DELETE_REMINDER) {
            if (!StringUtils.hasText(aiIntent.timeText()) && !StringUtils.hasText(aiIntent.content())) {
                return ReminderIntent.unknown(normalized);
            }
        }
        return new ReminderIntent(
                aiIntent.type(),
                aiIntent.timeText(),
                aiIntent.content(),
                aiIntent.confidence(),
                normalized
        );
    }

    private java.time.ZoneId defaultZone() {
        return botProperties.proactiveZoneId();
    }

    private boolean isLikelyQuestion(String normalized) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        return normalized.contains("?")
                || normalized.contains("？")
                || lower.startsWith("你觉得")
                || lower.startsWith("你认为")
                || lower.contains("应该")
                || lower.contains("要不要")
                || lower.contains("会不会")
                || lower.endsWith("吗")
                || lower.endsWith("呢")
                || lower.endsWith("怎么办")
                || lower.endsWith("为什么");
    }

    private String firstMatch(String text, String[] candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String removeFirst(String text, String fragment) {
        int index = text.indexOf(fragment);
        if (index < 0) {
            return text;
        }
        return (text.substring(0, index) + text.substring(index + fragment.length())).trim();
    }

    private String cleanupKeyword(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.replace("一下", "")
                .replace("记得", "")
                .replace("到时候", "")
                .replace("了", "")
                .replace("的", "")
                .trim();
    }
}
