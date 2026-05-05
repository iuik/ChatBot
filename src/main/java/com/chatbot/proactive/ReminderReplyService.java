package com.chatbot.proactive;

import com.chatbot.config.BotProperties;
import com.chatbot.repository.ProactiveTaskRecord;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ReminderReplyService {

    private static final Logger log = LoggerFactory.getLogger(ReminderReplyService.class);

    private final BotProperties botProperties;
    private final ReminderReplyGenerator reminderReplyGenerator;

    public ReminderReplyService(BotProperties botProperties,
                                @Nullable ReminderReplyGenerator reminderReplyGenerator) {
        this.botProperties = botProperties;
        this.reminderReplyGenerator = reminderReplyGenerator;
    }

    public String created(LocalDateTime scheduledAt, String content) {
        return generate(ReminderReplyAction.CREATED, scheduledAt, content, List.of(), templateCreated(scheduledAt, content));
    }

    public String deleted(ProactiveTaskRecord task) {
        return generate(ReminderReplyAction.DELETED, task.nextRunAt(), task.content(), List.of(), templateDeleted(task));
    }

    public String notFound() {
        return generate(ReminderReplyAction.NOT_FOUND, null, null, List.of(),
                "我没找到对应的提醒，你可以发 /reminders 看一下。");
    }

    public String ambiguous(List<ProactiveTaskRecord> candidates) {
        List<String> lines = candidates.stream()
                .map(task -> task.id() + ". " + formatAbsolute(task.nextRunAt()) + " " + task.content())
                .collect(Collectors.toList());
        String fallback = "我找到几个可能的提醒，你要删哪一个？\n"
                + String.join("\n", lines)
                + "\n你可以回复 /remind del 任务ID。";
        return generate(ReminderReplyAction.AMBIGUOUS, null, null, lines, fallback);
    }

    public String invalidTime() {
        return generate(ReminderReplyAction.INVALID_TIME, null, null, List.of(), "这个时间已经过去了，换一个时间吧。");
    }

    public String needTime() {
        return generate(ReminderReplyAction.NEED_TIME, null, null, List.of(), "你想让我什么时候提醒你？");
    }

    public String needContent() {
        return generate(ReminderReplyAction.NEED_CONTENT, null, null, List.of(), "要提醒你做什么？");
    }

    private String generate(ReminderReplyAction action,
                            LocalDateTime scheduledAt,
                            String content,
                            List<String> candidates,
                            String fallback) {
        if (!botProperties.getReminder().isAiReplyEnabled() || reminderReplyGenerator == null) {
            return fallback;
        }
        try {
            String reply = reminderReplyGenerator.generate(action, scheduledAt, content, candidates);
            return StringUtils.hasText(reply) ? reply.trim() : fallback;
        } catch (Exception ex) {
            log.warn("Failed to generate reminder reply with AI, fallback to template");
            return fallback;
        }
    }

    private String templateCreated(LocalDateTime scheduledAt, String content) {
        String timeText = formatFriendly(scheduledAt);
        if (content.startsWith("叫我")) {
            return "好，" + timeText + "我叫你" + content.substring(2) + "。";
        }
        if (content.startsWith("喊我")) {
            return "好，" + timeText + "我喊你" + content.substring(2) + "。";
        }
        return "好，" + timeText + "提醒你" + content + "。";
    }

    private String templateDeleted(ProactiveTaskRecord task) {
        return "好，" + formatFriendly(task.nextRunAt()) + "的" + task.content() + "提醒已经取消。";
    }

    private String formatFriendly(LocalDateTime dateTime) {
        ZoneId zoneId = botProperties.proactiveZoneId();
        LocalDate today = LocalDate.now(zoneId);
        LocalDate date = dateTime.toLocalDate();
        String dayText;
        if (date.equals(today)) {
            dayText = "今天";
        } else if (date.equals(today.plusDays(1))) {
            dayText = "明天";
        } else if (date.equals(today.plusDays(2))) {
            dayText = "后天";
        } else {
            dayText = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return dayText + periodText(dateTime) + clockText(dateTime);
    }

    private String formatAbsolute(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String periodText(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        if (hour < 6) {
            return "凌晨 ";
        }
        if (hour < 12) {
            return "早上 ";
        }
        if (hour == 12) {
            return "中午 ";
        }
        if (hour < 18) {
            return "下午 ";
        }
        return "晚上 ";
    }

    private String clockText(LocalDateTime dateTime) {
        if (dateTime.getMinute() == 0) {
            return dateTime.getHour() + " 点";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("H:mm"));
    }
}
