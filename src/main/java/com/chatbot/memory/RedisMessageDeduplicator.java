package com.chatbot.memory;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMessageDeduplicator implements MessageDeduplicator {

    private static final Duration DEDUP_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;

    public RedisMessageDeduplicator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isDuplicate(Long messageId) {
        if (messageId == null) {
            return false;
        }
        Boolean inserted = redisTemplate.opsForValue().setIfAbsent(buildKey(messageId), "1", DEDUP_TTL);
        return Boolean.FALSE.equals(inserted);
    }

    private String buildKey(Long messageId) {
        return "qqbot:dedup:" + messageId;
    }
}
