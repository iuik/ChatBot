package com.chatbot.delivery;

import static org.assertj.core.api.Assertions.assertThat;

import com.chatbot.config.BotProperties;
import com.chatbot.delivery.dto.DeliveryMessagePart;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResponseSplitterTests {

    private ResponseSplitter responseSplitter;

    @BeforeEach
    void setUp() {
        BotProperties botProperties = new BotProperties();
        botProperties.getDelivery().setSplitEnabled(true);
        botProperties.getDelivery().setMaxParts(3);
        botProperties.getDelivery().setMaxPartChars(18);
        botProperties.getDelivery().setSplitDailyChatOnly(true);
        responseSplitter = new ResponseSplitter(botProperties);
    }

    @Test
    void dailyChatShortTextShouldNotSplit() {
        List<DeliveryMessagePart> parts = responseSplitter.split("在呢。", DeliveryMode.DAILY_CHAT);

        assertThat(parts).extracting(DeliveryMessagePart::content).containsExactly("在呢。");
    }

    @Test
    void dailyChatLongTextShouldSplitByChinesePunctuation() {
        List<DeliveryMessagePart> parts = responseSplitter.split(
                "在呢在呢，刚刚看你一直在忙，就没打扰你嘛……璃璃在这里陪着。",
                DeliveryMode.DAILY_CHAT
        );

        assertThat(parts).hasSizeBetween(2, 3);
        assertThat(parts.get(0).content()).contains("在呢在呢");
        assertThat(parts.get(parts.size() - 1).content()).contains("璃璃在这里陪着。");
        assertThat(String.join("", parts.stream().map(DeliveryMessagePart::content).toList()))
                .isEqualTo("在呢在呢，刚刚看你一直在忙，就没打扰你嘛……璃璃在这里陪着。");
    }

    @Test
    void dailyChatShouldRespectMaxParts() {
        BotProperties botProperties = new BotProperties();
        botProperties.getDelivery().setSplitEnabled(true);
        botProperties.getDelivery().setMaxParts(3);
        botProperties.getDelivery().setMaxPartChars(4);
        botProperties.getDelivery().setSplitDailyChatOnly(true);
        responseSplitter = new ResponseSplitter(botProperties);

        List<DeliveryMessagePart> parts = responseSplitter.split(
                "第一句。第二句。第三句。第四句。第五句。",
                DeliveryMode.DAILY_CHAT
        );

        assertThat(parts).hasSize(3);
        assertThat(parts.get(2).content()).contains("第三句。").contains("第四句。").contains("第五句。");
    }

    @Test
    void dailyChatWithCodeBlockShouldNotSplit() {
        List<DeliveryMessagePart> parts = responseSplitter.split(
                "给你示例：\n```json\n{\"a\":1}\n```",
                DeliveryMode.DAILY_CHAT
        );

        assertThat(parts).extracting(DeliveryMessagePart::content)
                .containsExactly("给你示例：\n```json\n{\"a\":1}\n```");
    }

    @Test
    void commandModeShouldNotSplit() {
        List<DeliveryMessagePart> parts = responseSplitter.split(
                "第一句。第二句。第三句。第四句。",
                DeliveryMode.COMMAND
        );

        assertThat(parts).extracting(DeliveryMessagePart::content)
                .containsExactly("第一句。第二句。第三句。第四句。");
    }

    @Test
    void reminderModeShouldNotSplit() {
        List<DeliveryMessagePart> parts = responseSplitter.split(
                "提醒：晚上九点记得收衣服。顺便把明天要带的东西看一下。",
                DeliveryMode.REMINDER
        );

        assertThat(parts).extracting(DeliveryMessagePart::content)
                .containsExactly("提醒：晚上九点记得收衣服。顺便把明天要带的东西看一下。");
    }

    @Test
    void chatPushModeShouldNotSplitByDefault() {
        List<DeliveryMessagePart> parts = responseSplitter.split(
                "来继续收尾主动话题模块？顺手把测试也补一下？",
                DeliveryMode.CHATPUSH
        );

        assertThat(parts).extracting(DeliveryMessagePart::content)
                .containsExactly("来继续收尾主动话题模块？顺手把测试也补一下？");
    }
}
