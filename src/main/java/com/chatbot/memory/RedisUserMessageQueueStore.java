package com.chatbot.memory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.chatbot.chat.QueuedUserMessage;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisUserMessageQueueStore implements UserMessageQueueStore {

    private static final Duration QUEUE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisUserMessageQueueStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void enqueue(Long qqId, QueuedUserMessage message) {
        String key = buildKey(qqId);
        redisTemplate.opsForList().rightPush(key, serialize(message));
        redisTemplate.expire(key, QUEUE_TTL);
    }

    @Override
    public List<QueuedUserMessage> drain(Long qqId, int maxBatchSize) {
        List<QueuedUserMessage> messages = new ArrayList<>();
        String key = buildKey(qqId);
        for (int i = 0; i < maxBatchSize; i++) {
            String serialized = redisTemplate.opsForList().leftPop(key);
            if (serialized == null) {
                break;
            }
            messages.add(deserialize(serialized));
        }
        return List.copyOf(messages);
    }

    @Override
    public boolean hasMessages(Long qqId) {
        Long size = redisTemplate.opsForList().size(buildKey(qqId));
        return size != null && size > 0;
    }

    private String serialize(QueuedUserMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize queued user message", ex);
        }
    }

    private QueuedUserMessage deserialize(String serialized) {
        try {
            return objectMapper.readValue(serialized, QueuedUserMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize queued user message", ex);
        }
    }

    private String buildKey(Long qqId) {
        return "qqbot:queue:" + qqId;
    }
}
