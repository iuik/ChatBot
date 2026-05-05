package com.chatbot.delivery;

import com.chatbot.config.BotProperties;
import com.chatbot.delivery.dto.DeliveryMessagePart;
import com.chatbot.onebot.OneBotApiClient;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ResponseDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ResponseDeliveryService.class);

    private final OneBotApiClient oneBotApiClient;
    private final ResponseSplitter responseSplitter;
    private final BotProperties botProperties;
    private final DelaySupport delaySupport;

    @Autowired
    public ResponseDeliveryService(OneBotApiClient oneBotApiClient,
                                   ResponseSplitter responseSplitter,
                                   BotProperties botProperties) {
        this(oneBotApiClient, responseSplitter, botProperties, new ThreadDelaySupport());
    }

    ResponseDeliveryService(OneBotApiClient oneBotApiClient,
                            ResponseSplitter responseSplitter,
                            BotProperties botProperties,
                            DelaySupport delaySupport) {
        this.oneBotApiClient = oneBotApiClient;
        this.responseSplitter = responseSplitter;
        this.botProperties = botProperties;
        this.delaySupport = delaySupport;
    }

    public Long deliver(Long qqId, String text, DeliveryMode mode) {
        if (!StringUtils.hasText(text)) {
            return null;
        }

        List<DeliveryMessagePart> parts = responseSplitter.split(text, mode);
        Long firstMessageId = null;

        for (int i = 0; i < parts.size(); i++) {
            if (i > 0 && shouldDelay(mode, parts.size())) {
                try {
                    delaySupport.sleep(nextDelayMillis());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("Delivery interrupted for qqId={}, mode={}", qqId, mode, ex);
                    break;
                }
            }

            DeliveryMessagePart part = parts.get(i);
            try {
                Long messageId = oneBotApiClient.sendPrivateMessage(qqId, part.content());
                if (firstMessageId == null) {
                    firstMessageId = messageId;
                }
                if (messageId == null) {
                    log.warn("OneBot returned null messageId for qqId={}, mode={}, part={}", qqId, mode, part.index());
                }
            } catch (Exception ex) {
                log.warn("Failed to deliver message for qqId={}, mode={}, part={}", qqId, mode, part.index(), ex);
            }
        }
        return firstMessageId;
    }

    private boolean shouldDelay(DeliveryMode mode, int totalParts) {
        return mode == DeliveryMode.DAILY_CHAT && totalParts > 1;
    }

    private long nextDelayMillis() {
        long min = Math.max(0, botProperties.getDelivery().getMinDelayMillis());
        long max = Math.max(min, botProperties.getDelivery().getMaxDelayMillis());
        if (min == max) {
            return min;
        }
        return ThreadLocalRandom.current().nextLong(min, max + 1);
    }

    interface DelaySupport {
        void sleep(long millis) throws InterruptedException;
    }

    static final class ThreadDelaySupport implements DelaySupport {
        @Override
        public void sleep(long millis) throws InterruptedException {
            Thread.sleep(millis);
        }
    }
}
