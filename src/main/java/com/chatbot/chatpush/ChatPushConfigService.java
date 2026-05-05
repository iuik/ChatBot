package com.chatbot.chatpush;

import com.chatbot.chatpush.dto.ChatPushConfig;
import com.chatbot.config.BotProperties;
import com.chatbot.repository.ChatPushConfigRecord;
import com.chatbot.repository.ChatPushConfigRepository;
import java.util.function.UnaryOperator;
import org.springframework.stereotype.Service;

@Service
public class ChatPushConfigService {

    private final ChatPushConfigRepository repository;
    private final BotProperties botProperties;

    public ChatPushConfigService(ChatPushConfigRepository repository, BotProperties botProperties) {
        this.repository = repository;
        this.botProperties = botProperties;
    }

    public ChatPushConfig getEffectiveConfig(Long qqId) {
        return repository.findByQqId(String.valueOf(qqId))
                .map(this::toConfig)
                .orElseGet(this::defaultConfig);
    }

    public void updateEnabled(Long qqId, boolean enabled) {
        save(qqId, config -> config.withEnabled(enabled));
    }

    public void updateMinIdleMinutes(Long qqId, int minIdleMinutes) {
        save(qqId, config -> config.withMinIdleMinutes(minIdleMinutes));
    }

    public void updateCooldownMinutes(Long qqId, int cooldownMinutes) {
        save(qqId, config -> config.withCooldownMinutes(cooldownMinutes));
    }

    public void updateQuietTime(Long qqId, String quietStart, String quietEnd) {
        save(qqId, config -> config.withQuiet(true, quietStart, quietEnd));
    }

    public void updateQuietEnabled(Long qqId, boolean quietEnabled) {
        ChatPushConfig defaults = defaultConfig();
        save(qqId, config -> config.withQuiet(
                quietEnabled,
                config.quietStart() == null ? defaults.quietStart() : config.quietStart(),
                config.quietEnd() == null ? defaults.quietEnd() : config.quietEnd()
        ));
    }

    public void updateMaxPerDay(Long qqId, int maxPerDay) {
        save(qqId, config -> config.withMax(maxPerDay, true));
    }

    public void updateMaxPerDayEnabled(Long qqId, boolean enabled) {
        save(qqId, config -> config.withMax(config.maxPerDay(), enabled));
    }

    public void reset(Long qqId) {
        repository.deleteByQqId(String.valueOf(qqId));
    }

    public ChatPushConfig defaultConfig() {
        BotProperties.ChatPushProperties properties = botProperties.getChatpush();
        return new ChatPushConfig(
                properties.isEnabled(),
                properties.getMinIdleHours() * 60,
                properties.getCooldownHours() * 60,
                true,
                properties.getQuietHoursStart(),
                properties.getQuietHoursEnd(),
                properties.getMaxPerDay(),
                true
        );
    }

    public int minAllowedIdleMinutes() {
        return botProperties.getChatpush().getMinAllowedIdleMinutes();
    }

    public int minAllowedCooldownMinutes() {
        return botProperties.getChatpush().getMinAllowedCooldownMinutes();
    }

    public int maxAllowedPerDay() {
        return botProperties.getChatpush().getMaxAllowedPerDay();
    }

    private void save(Long qqId, UnaryOperator<ChatPushConfig> updater) {
        ChatPushConfig updated = updater.apply(getEffectiveConfig(qqId));
        repository.save(toRecord(String.valueOf(qqId), updated));
    }

    private ChatPushConfig toConfig(ChatPushConfigRecord record) {
        return new ChatPushConfig(
                record.enabled(),
                record.minIdleMinutes(),
                record.cooldownMinutes(),
                record.quietEnabled(),
                record.quietStart(),
                record.quietEnd(),
                record.maxPerDay(),
                record.maxPerDayEnabled()
        );
    }

    private ChatPushConfigRecord toRecord(String qqId, ChatPushConfig config) {
        return new ChatPushConfigRecord(
                null,
                qqId,
                config.enabled(),
                config.minIdleMinutes(),
                config.cooldownMinutes(),
                config.quietEnabled(),
                config.quietStart(),
                config.quietEnd(),
                config.maxPerDay(),
                config.maxPerDayEnabled(),
                null,
                null
        );
    }
}
