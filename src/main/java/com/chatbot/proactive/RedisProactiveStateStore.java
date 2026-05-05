package com.chatbot.proactive;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisProactiveStateStore implements ProactiveStateStore {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Duration DAILY_TTL = Duration.ofDays(2);

    private final StringRedisTemplate redisTemplate;

    public RedisProactiveStateStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean isUserEnabled(Long qqId) {
        String value = redisTemplate.opsForValue().get(enabledKey(qqId));
        return value == null || Boolean.parseBoolean(value);
    }

    @Override
    public void setUserEnabled(Long qqId, boolean enabled) {
        redisTemplate.opsForValue().set(enabledKey(qqId), String.valueOf(enabled));
    }

    @Override
    public Integer getDailySentCount(Long qqId, LocalDate date) {
        String value = redisTemplate.opsForValue().get(dailyCountKey(qqId, date));
        return value == null ? 0 : Integer.parseInt(value);
    }

    @Override
    public long incrementDailySentCount(Long qqId, LocalDate date) {
        String key = dailyCountKey(qqId, date);
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, DAILY_TTL);
        return count == null ? 0L : count;
    }

    private String enabledKey(Long qqId) {
        return "qqbot:proactive:enabled:" + qqId;
    }

    private String dailyCountKey(Long qqId, LocalDate date) {
        return "qqbot:proactive:" + qqId + ":date:" + DATE_FORMATTER.format(date);
    }
}
