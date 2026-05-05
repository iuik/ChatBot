package com.chatbot.memory;

import com.chatbot.config.BotProperties;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisProcessingLockManager implements ProcessingLockManager {

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    end
                    return 0
                    """,
            Long.class
    );

    private final StringRedisTemplate redisTemplate;
    private final Duration lockTtl;

    public RedisProcessingLockManager(StringRedisTemplate redisTemplate, BotProperties botProperties) {
        this.redisTemplate = redisTemplate;
        this.lockTtl = Duration.ofSeconds(botProperties.getChat().getQueue().getProcessingLockSeconds());
    }

    @Override
    public String tryLock(Long qqId) {
        String token = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(buildKey(qqId), token, lockTtl);
        if (Boolean.TRUE.equals(locked)) {
            return token;
        }
        return null;
    }

    @Override
    public void unlock(Long qqId, String token) {
        if (qqId == null || token == null) {
            return;
        }
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(buildKey(qqId)), token);
    }

    @Override
    public boolean isLocked(Long qqId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(buildKey(qqId)));
    }

    private String buildKey(Long qqId) {
        return "qqbot:processing:" + qqId;
    }
}
