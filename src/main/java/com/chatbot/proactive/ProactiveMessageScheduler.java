package com.chatbot.proactive;

import com.chatbot.delivery.DeliveryMode;
import com.chatbot.delivery.ResponseDeliveryService;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.ProactiveTaskRecord;
import com.chatbot.repository.ProactiveTaskRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProactiveMessageScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProactiveMessageScheduler.class);

    private final ProactiveTaskRepository proactiveTaskRepository;
    private final ProactiveTaskService proactiveTaskService;
    private final ProactivePolicy proactivePolicy;
    private final ResponseDeliveryService responseDeliveryService;
    private final ChatMessageRepository chatMessageRepository;

    public ProactiveMessageScheduler(ProactiveTaskRepository proactiveTaskRepository,
                                     ProactiveTaskService proactiveTaskService,
                                     ProactivePolicy proactivePolicy,
                                     ResponseDeliveryService responseDeliveryService,
                                     ChatMessageRepository chatMessageRepository) {
        this.proactiveTaskRepository = proactiveTaskRepository;
        this.proactiveTaskService = proactiveTaskService;
        this.proactivePolicy = proactivePolicy;
        this.responseDeliveryService = responseDeliveryService;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Scheduled(fixedDelayString = "${bot.proactive.scan-interval-millis:30000}")
    public void scanAndSend() {
        if (!proactivePolicy.isGloballyEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(proactivePolicy.zoneId());
        try {
            List<ProactiveTaskRecord> dueTasks = proactiveTaskRepository.findDueTasks(now, 20);
            for (ProactiveTaskRecord task : dueTasks) {
                processTask(task, now);
            }
        } catch (Exception ex) {
            log.error("Failed to scan proactive tasks", ex);
        }
    }

    void processTask(ProactiveTaskRecord task, LocalDateTime now) {
        try {
            Long qqId = Long.parseLong(task.qqId());
            if (!proactivePolicy.isUserEnabled(qqId)) {
                return;
            }
            if (proactivePolicy.isQuietTime(now)) {
                proactiveTaskRepository.defer(task.id(), proactiveTaskService.deferToAfterQuietHours(now));
                return;
            }
            if (proactivePolicy.exceededDailyLimit(qqId, now.toLocalDate())) {
                proactiveTaskRepository.defer(task.id(), nextDayResume(now));
                return;
            }

            String message = "\u63d0\u9192\uff1a" + task.content();
            Long oneBotMessageId = responseDeliveryService.deliver(qqId, message, DeliveryMode.REMINDER);
            chatMessageRepository.save(qqId, "proactive", "assistant", message, oneBotMessageId);
            proactiveTaskRepository.markSent(task.id(), now);
            proactivePolicy.incrementDailySentCount(qqId, LocalDate.now(proactivePolicy.zoneId()));
        } catch (Exception ex) {
            log.error("Failed to process proactive task id={}", task.id(), ex);
            try {
                proactiveTaskRepository.markFailed(task.id(), now);
            } catch (Exception updateEx) {
                log.error("Failed to mark proactive task {} as failed", task.id(), updateEx);
            }
        }
    }

    private LocalDateTime nextDayResume(LocalDateTime now) {
        return now.toLocalDate().plusDays(1).atTime(proactivePolicy.quietHoursEnd());
    }
}
