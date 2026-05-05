package com.chatbot.proactive;

import com.chatbot.config.BotProperties;
import com.chatbot.proactive.ReminderTimeParser.ReminderTimeReference;
import com.chatbot.proactive.dto.CreateReminderCommand;
import com.chatbot.proactive.dto.ProactiveStatus;
import com.chatbot.repository.ProactiveTaskRecord;
import com.chatbot.repository.ProactiveTaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ProactiveTaskService {

    private static final Logger log = LoggerFactory.getLogger(ProactiveTaskService.class);

    private static final String REMINDER_USAGE =
            "用法：\n"
                    + "/remind 2026-05-04 09:00 提醒内容\n"
                    + "/remind 09:00 提醒内容\n"
                    + "/remind tomorrow 09:00 提醒内容\n"
                    + "/reminders\n"
                    + "/remind del 任务ID";
    private static final String PROACTIVE_OFF_REPLY =
            "已关闭主动消息。你主动发消息时我仍会正常回复。";
    private static final String PROACTIVE_ON_REPLY =
            "已开启主动消息。";

    private final ProactiveTaskRepository proactiveTaskRepository;
    private final ReminderTimeParser reminderTimeParser;
    private final ProactivePolicy proactivePolicy;
    private final BotProperties botProperties;
    private final NaturalReminderIntentDetector naturalReminderIntentDetector;
    private final ReminderReplyService reminderReplyService;

    public ProactiveTaskService(ProactiveTaskRepository proactiveTaskRepository,
                                ReminderTimeParser reminderTimeParser,
                                ProactivePolicy proactivePolicy,
                                BotProperties botProperties,
                                NaturalReminderIntentDetector naturalReminderIntentDetector,
                                ReminderReplyService reminderReplyService) {
        this.proactiveTaskRepository = proactiveTaskRepository;
        this.reminderTimeParser = reminderTimeParser;
        this.proactivePolicy = proactivePolicy;
        this.botProperties = botProperties;
        this.naturalReminderIntentDetector = naturalReminderIntentDetector;
        this.reminderReplyService = reminderReplyService;
    }

    public String handleCommand(Long qqId, String text) {
        if (text.startsWith("/reminders")) {
            return listReminders(qqId);
        }
        if (text.startsWith("/remind del ")) {
            return deleteReminderById(qqId, text.substring("/remind del ".length()).trim());
        }
        if (text.equals("/remind") || text.startsWith("/remind ")) {
            return createReminder(qqId, text.equals("/remind") ? "" : text.substring("/remind ".length()).trim());
        }
        if (text.equals("/proactive status")) {
            return renderStatus(qqId);
        }
        if (text.equals("/proactive on")) {
            proactivePolicy.setUserEnabled(qqId, true);
            return PROACTIVE_ON_REPLY;
        }
        if (text.equals("/proactive off")) {
            proactivePolicy.setUserEnabled(qqId, false);
            return PROACTIVE_OFF_REPLY;
        }
        return null;
    }

    public String handleNaturalMessage(Long qqId, String text) {
        ReminderIntent intent = naturalReminderIntentDetector.detect(text);
        if (intent.type() == ReminderIntentType.UNKNOWN) {
            return null;
        }
        if (intent.type() == ReminderIntentType.LIST_REMINDER) {
            return listReminders(qqId);
        }
        if (intent.type() == ReminderIntentType.CREATE_REMINDER) {
            return createReminderFromIntent(qqId, intent);
        }
        if (intent.type() == ReminderIntentType.DELETE_REMINDER) {
            return deleteReminderFromIntent(qqId, intent);
        }
        return null;
    }

    public ProactiveStatus getStatus(Long qqId) {
        Integer sentToday = proactivePolicy.getDailySentCount(qqId, LocalDate.now(proactivePolicy.zoneId()));
        return new ProactiveStatus(
                proactivePolicy.isUserEnabled(qqId),
                proactivePolicy.quietHoursText(),
                sentToday,
                botProperties.getProactive().getMaxPerDay(),
                proactiveTaskRepository.countPending(String.valueOf(qqId))
        );
    }

    private String createReminder(Long qqId, String args) {
        CreateReminderCommand command = reminderTimeParser.parse(args, proactivePolicy.zoneId());
        if (command == null || !StringUtils.hasText(command.content())) {
            return REMINDER_USAGE;
        }
        return saveReminder(qqId, command.scheduledAt(), command.content(), false);
    }

    private String createReminderFromIntent(Long qqId, ReminderIntent intent) {
        if (!StringUtils.hasText(intent.timeText())) {
            return reminderReplyService.needTime();
        }
        if (!StringUtils.hasText(intent.content())) {
            return reminderReplyService.needContent();
        }
        LocalDateTime scheduledAt = reminderTimeParser.parseDateTimeText(intent.timeText(), proactivePolicy.zoneId());
        if (scheduledAt == null) {
            return reminderReplyService.invalidTime();
        }
        if (scheduledAt.isBefore(LocalDateTime.now(proactivePolicy.zoneId()))) {
            return reminderReplyService.invalidTime();
        }
        return saveReminder(qqId, scheduledAt, intent.content(), true);
    }

    private String saveReminder(Long qqId, LocalDateTime scheduledAt, String content, boolean naturalReply) {
        try {
            Long id = proactiveTaskRepository.save(
                    String.valueOf(qqId),
                    "REMINDER",
                    null,
                    content,
                    scheduledAt,
                    scheduledAt
            );
            if (naturalReply) {
                return reminderReplyService.created(scheduledAt, content);
            }
            return "已创建提醒 " + id + "："
                    + scheduledAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                    + " " + content;
        } catch (Exception ex) {
            log.error("Failed to create reminder for qqId={}", qqId, ex);
            return "创建提醒失败，请稍后再试。";
        }
    }

    private String listReminders(Long qqId) {
        List<ProactiveTaskRecord> tasks = proactiveTaskRepository.findUpcoming(String.valueOf(qqId), 10);
        if (tasks.isEmpty()) {
            return "当前没有待提醒任务。";
        }
        StringBuilder builder = new StringBuilder("待提醒任务：\n");
        for (ProactiveTaskRecord task : tasks) {
            builder.append(task.id())
                    .append(". ")
                    .append(task.nextRunAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                    .append(" ")
                    .append(task.content())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private String deleteReminderById(Long qqId, String idText) {
        Long id;
        try {
            id = Long.parseLong(idText);
        } catch (Exception ex) {
            return REMINDER_USAGE;
        }
        Optional<ProactiveTaskRecord> existing = proactiveTaskRepository.findByIdAndQqId(id, String.valueOf(qqId));
        if (existing.isEmpty()) {
            return "未找到该提醒任务。";
        }
        proactiveTaskRepository.cancel(id, String.valueOf(qqId));
        return "已取消提醒 " + id;
    }

    private String deleteReminderFromIntent(Long qqId, ReminderIntent intent) {
        List<ProactiveTaskRecord> tasks = proactiveTaskRepository.findUpcoming(String.valueOf(qqId), 100);
        List<ProactiveTaskRecord> matches = tasks.stream()
                .filter(task -> matchesTime(task, intent.timeText()))
                .filter(task -> matchesContent(task, intent.content()))
                .limit(botProperties.getReminder().getMaxCandidates() + 1L)
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            return reminderReplyService.notFound();
        }
        if (matches.size() == 1) {
            ProactiveTaskRecord task = matches.get(0);
            proactiveTaskRepository.cancel(task.id(), String.valueOf(qqId));
            return reminderReplyService.deleted(task);
        }
        int maxCandidates = botProperties.getReminder().getMaxCandidates();
        return reminderReplyService.ambiguous(matches.subList(0, Math.min(matches.size(), maxCandidates)));
    }

    private boolean matchesTime(ProactiveTaskRecord task, String timeText) {
        if (!StringUtils.hasText(timeText)) {
            return true;
        }
        ReminderTimeReference reference = reminderTimeParser.parseTimeReference(timeText, proactivePolicy.zoneId());
        if (reference == null) {
            return false;
        }
        if (reference.exact()) {
            return task.nextRunAt().withSecond(0).withNano(0).equals(reference.scheduledAt().withSecond(0).withNano(0));
        }
        return !task.nextRunAt().isBefore(reference.windowStart()) && !task.nextRunAt().isAfter(reference.windowEnd());
    }

    private boolean matchesContent(ProactiveTaskRecord task, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String normalizedKeyword = normalizeKeyword(keyword);
        String normalizedContent = normalizeKeyword(task.content());
        return normalizedContent.contains(normalizedKeyword) || normalizedKeyword.contains(normalizedContent);
    }

    private String normalizeKeyword(String text) {
        return reminderTimeParser.normalize(text)
                .replace("提醒我", "")
                .replace("提醒你", "")
                .replace("叫我", "")
                .replace("叫你", "")
                .replace("喊我", "")
                .replace("喊你", "")
                .replace("那个", "")
                .replace("任务", "")
                .replace("提醒", "")
                .replace(" ", "")
                .trim();
    }

    private String renderStatus(Long qqId) {
        ProactiveStatus status = getStatus(qqId);
        String sentToday = status.sentToday() == null ? "未知" : status.sentToday().toString();
        return "主动消息：" + (status.enabled() ? "开启" : "关闭")
                + "\n安静时间：" + status.quietHours()
                + "\n今日已发送：" + sentToday + " / " + status.maxPerDay()
                + "\n待提醒任务：" + status.pendingTasks() + " 条";
    }

    public LocalDateTime deferToAfterQuietHours(LocalDateTime now) {
        return now.plusMinutes(botProperties.getProactive().getDeferMinutesWhenQuiet());
    }
}
