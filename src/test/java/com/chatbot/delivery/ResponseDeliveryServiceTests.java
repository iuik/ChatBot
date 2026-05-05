package com.chatbot.delivery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatbot.config.BotProperties;
import com.chatbot.onebot.OneBotApiClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseDeliveryServiceTests {

    private OneBotApiClient oneBotApiClient;
    private ResponseDeliveryService responseDeliveryService;
    private List<Long> recordedSleeps;

    @BeforeEach
    void setUp() {
        oneBotApiClient = org.mockito.Mockito.mock(OneBotApiClient.class);
        BotProperties botProperties = new BotProperties();
        botProperties.getDelivery().setSplitEnabled(true);
        botProperties.getDelivery().setMaxParts(3);
        botProperties.getDelivery().setMaxPartChars(5);
        botProperties.getDelivery().setMinDelayMillis(5);
        botProperties.getDelivery().setMaxDelayMillis(5);

        ResponseSplitter responseSplitter = new ResponseSplitter(botProperties);
        recordedSleeps = new ArrayList<>();
        responseDeliveryService = new ResponseDeliveryService(
                oneBotApiClient,
                responseSplitter,
                botProperties,
                recordedSleeps::add
        );
    }

    @Test
    void shouldDeliverPartsInOrder() {
        String text = "在呢。在看。你继续说。";
        when(oneBotApiClient.sendPrivateMessage(123456789L, "在呢。")).thenReturn(11L);
        when(oneBotApiClient.sendPrivateMessage(123456789L, "在看。")).thenReturn(12L);
        when(oneBotApiClient.sendPrivateMessage(123456789L, "你继续说。")).thenReturn(13L);

        Long messageId = responseDeliveryService.deliver(123456789L, text, DeliveryMode.DAILY_CHAT);

        assertThat(messageId).isEqualTo(11L);
        verify(oneBotApiClient).sendPrivateMessage(123456789L, "在呢。");
        verify(oneBotApiClient).sendPrivateMessage(123456789L, "在看。");
        verify(oneBotApiClient).sendPrivateMessage(123456789L, "你继续说。");
        assertThat(recordedSleeps).containsExactly(5L, 5L);
    }
}
