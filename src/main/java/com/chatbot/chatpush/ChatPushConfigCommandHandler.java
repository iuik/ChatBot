package com.chatbot.chatpush;

import com.chatbot.chatpush.dto.ChatPushConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ChatPushConfigCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatPushConfigCommandHandler.class);

    private final ChatPushConfigParser parser;
    private final ChatPushConfigService configService;

    public ChatPushConfigCommandHandler(ChatPushConfigParser parser, ChatPushConfigService configService) {
        this.parser = parser;
        this.configService = configService;
    }

    public ChatPushCommandResult handle(Long qqId, String text) {
        ChatPushConfigParser.ParsedCommand command = parser.parse(text);
        if (!command.handled()) {
            return ChatPushCommandResult.unhandled();
        }
        try {
            return switch (command.type()) {
                case CONFIG -> ChatPushCommandResult.replied(null);
                case RESET -> {
                    configService.reset(qqId);
                    yield ChatPushCommandResult.replied("已恢复 ChatPush 默认配置。");
                }
                case SET_IDLE -> handleSetIdle(qqId, command.durationMinutes());
                case SET_COOLDOWN -> handleSetCooldown(qqId, command.durationMinutes());
                case SET_QUIET_TIME -> handleSetQuietTime(qqId, command.quietStart(), command.quietEnd());
                case SET_QUIET_OFF -> {
                    configService.updateQuietEnabled(qqId, false);
                    yield ChatPushCommandResult.replied("已关闭安静时间限制。");
                }
                case SET_QUIET_ON -> {
                    configService.updateQuietEnabled(qqId, true);
                    yield ChatPushCommandResult.replied("已开启安静时间限制。");
                }
                case SET_MAX_VALUE -> handleSetMax(qqId, command.maxPerDay());
                case SET_MAX_OFF -> {
                    configService.updateMaxPerDayEnabled(qqId, false);
                    yield ChatPushCommandResult.replied("已关闭每日次数限制。仍会受空闲时间和冷却时间限制。");
                }
                case SET_MAX_ON -> {
                    configService.updateMaxPerDayEnabled(qqId, true);
                    yield ChatPushCommandResult.replied("已开启每日次数限制。");
                }
                case INVALID -> ChatPushCommandResult.replied(command.usage());
                case UNMATCHED -> ChatPushCommandResult.unhandled();
            };
        } catch (Exception ex) {
            log.warn("Failed to save chatpush config for qqId={}", qqId, ex);
            return ChatPushCommandResult.replied("配置保存失败了，稍后再试。");
        }
    }

    private ChatPushCommandResult handleSetIdle(Long qqId, Integer minutes) {
        if (minutes == null) {
            return ChatPushCommandResult.replied("用法：/chatpush set idle 6h 或 /chatpush set idle 30m");
        }
        if (minutes < configService.minAllowedIdleMinutes()) {
            return ChatPushCommandResult.replied("空闲时间太短了，至少设置为 10m。");
        }
        configService.updateMinIdleMinutes(qqId, minutes);
        return ChatPushCommandResult.replied("已设置：你 " + formatMinutes(minutes) + " 没聊天后，我才会主动发起话题。");
    }

    private ChatPushCommandResult handleSetCooldown(Long qqId, Integer minutes) {
        if (minutes == null) {
            return ChatPushCommandResult.replied("用法：/chatpush set cooldown 6h 或 /chatpush set cooldown 30m");
        }
        if (minutes < configService.minAllowedCooldownMinutes()) {
            return ChatPushCommandResult.replied("冷却时间太短了，至少设置为 10m。");
        }
        configService.updateCooldownMinutes(qqId, minutes);
        return ChatPushCommandResult.replied("已设置：两次主动话题至少间隔 " + formatMinutes(minutes) + "。");
    }

    private ChatPushCommandResult handleSetQuietTime(Long qqId, String quietStart, String quietEnd) {
        configService.updateQuietTime(qqId, quietStart, quietEnd);
        return ChatPushCommandResult.replied("已设置安静时间：" + quietStart + " - " + quietEnd + "。");
    }

    private ChatPushCommandResult handleSetMax(Long qqId, Integer maxPerDay) {
        if (maxPerDay == null || maxPerDay <= 0) {
            return ChatPushCommandResult.replied("每日次数必须是正整数。");
        }
        if (maxPerDay > configService.maxAllowedPerDay()) {
            return ChatPushCommandResult.replied("每日次数太高了，最多设置为 10。");
        }
        configService.updateMaxPerDay(qqId, maxPerDay);
        return ChatPushCommandResult.replied("已设置：每天最多主动发起 " + maxPerDay + " 次。");
    }

    private String formatMinutes(int minutes) {
        if (minutes % 60 == 0) {
            return (minutes / 60) + " 小时";
        }
        return minutes + " 分钟";
    }
}
