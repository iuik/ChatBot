package com.chatbot.chatpush;

import com.chatbot.config.BotProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatPushScheduler {

    private final BotProperties botProperties;
    private final ChatPushService chatPushService;

    public ChatPushScheduler(BotProperties botProperties, ChatPushService chatPushService) {
        this.botProperties = botProperties;
        this.chatPushService = chatPushService;
    }

    @Scheduled(fixedDelayString = "${bot.chatpush.scan-interval-millis:600000}")
    public void scanAndSend() {
        Long ownerQq = parseOwnerQq();
        if (ownerQq == null) {
            return;
        }
        chatPushService.trySendChatPush(ownerQq, false);
    }

    private Long parseOwnerQq() {
        if (!StringUtils.hasText(botProperties.getOwnerQq())) {
            return null;
        }
        try {
            return Long.parseLong(botProperties.getOwnerQq());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
