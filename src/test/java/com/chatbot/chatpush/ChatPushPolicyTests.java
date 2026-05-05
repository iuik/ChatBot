package com.chatbot.chatpush;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.chatbot.config.BotProperties;
import com.chatbot.repository.ChatPushConfigRecord;
import com.chatbot.repository.ChatPushConfigRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatPushPolicyTests {

    private ChatPushConfigRepository repository;
    private ChatPushPolicy policy;

    @BeforeEach
    void setUp() {
        BotProperties botProperties = new BotProperties();
        botProperties.getChatpush().setEnabled(false);
        botProperties.getChatpush().setMinIdleHours(6);
        repository = org.mockito.Mockito.mock(ChatPushConfigRepository.class);
        ChatPushConfigService configService = new ChatPushConfigService(repository, botProperties);
        ChatPushStateStore stateStore = org.mockito.Mockito.mock(ChatPushStateStore.class);
        policy = new ChatPushPolicy(botProperties, configService, stateStore);
    }

    @Test
    void shouldPreferMysqlUserConfigOverApplicationDefaults() {
        when(repository.findByQqId("123456789"))
                .thenReturn(Optional.of(new ChatPushConfigRecord(
                        1L,
                        "123456789",
                        true,
                        30,
                        45,
                        false,
                        "23:30",
                        "08:30",
                        3,
                        false,
                        LocalDateTime.now(),
                        LocalDateTime.now()
                )));

        assertThat(policy.isEnabled(123456789L)).isTrue();
        assertThat(policy.getEffectiveConfig(123456789L).minIdleMinutes()).isEqualTo(30);
        assertThat(policy.getEffectiveConfig(123456789L).cooldownMinutes()).isEqualTo(45);
        assertThat(policy.getEffectiveConfig(123456789L).maxPerDayEnabled()).isFalse();
    }
}
