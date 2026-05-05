package com.chatbot.chatpush.dto;

public record ChatPushConfig(
        boolean enabled,
        int minIdleMinutes,
        int cooldownMinutes,
        boolean quietEnabled,
        String quietStart,
        String quietEnd,
        int maxPerDay,
        boolean maxPerDayEnabled
) {

    public ChatPushConfig withEnabled(boolean value) {
        return new ChatPushConfig(value, minIdleMinutes, cooldownMinutes, quietEnabled, quietStart, quietEnd, maxPerDay, maxPerDayEnabled);
    }

    public ChatPushConfig withMinIdleMinutes(int value) {
        return new ChatPushConfig(enabled, value, cooldownMinutes, quietEnabled, quietStart, quietEnd, maxPerDay, maxPerDayEnabled);
    }

    public ChatPushConfig withCooldownMinutes(int value) {
        return new ChatPushConfig(enabled, minIdleMinutes, value, quietEnabled, quietStart, quietEnd, maxPerDay, maxPerDayEnabled);
    }

    public ChatPushConfig withQuiet(boolean enabledValue, String startValue, String endValue) {
        return new ChatPushConfig(enabled, minIdleMinutes, cooldownMinutes, enabledValue, startValue, endValue, maxPerDay, maxPerDayEnabled);
    }

    public ChatPushConfig withMax(int value, boolean enabledValue) {
        return new ChatPushConfig(enabled, minIdleMinutes, cooldownMinutes, quietEnabled, quietStart, quietEnd, value, enabledValue);
    }
}
