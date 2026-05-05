package com.chatbot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bot")
public class BotProperties {

    private String ownerQq;

    private String timezone = "Asia/Shanghai";

    @Valid
    private final OneBotProperties onebot = new OneBotProperties();

    @Valid
    private final ChatProperties chat = new ChatProperties();

    @Valid
    private final MemoryProperties memory = new MemoryProperties();

    @Valid
    private final ProactiveProperties proactive = new ProactiveProperties();

    @Valid
    private final ReminderProperties reminder = new ReminderProperties();

    @Valid
    private final ChatPushProperties chatpush = new ChatPushProperties();

    @Valid
    private final PromptProperties prompt = new PromptProperties();

    @Valid
    private final DeliveryProperties delivery = new DeliveryProperties();

    public String getOwnerQq() {
        return ownerQq;
    }

    public void setOwnerQq(String ownerQq) {
        this.ownerQq = ownerQq;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public OneBotProperties getOnebot() {
        return onebot;
    }

    public ChatProperties getChat() {
        return chat;
    }

    public MemoryProperties getMemory() {
        return memory;
    }

    public ProactiveProperties getProactive() {
        return proactive;
    }

    public ReminderProperties getReminder() {
        return reminder;
    }

    public ChatPushProperties getChatpush() {
        return chatpush;
    }

    public PromptProperties getPrompt() {
        return prompt;
    }

    public DeliveryProperties getDelivery() {
        return delivery;
    }

    public ZoneId promptZoneId() {
        return ZoneId.of(resolveTimezone(null));
    }

    public ZoneId proactiveZoneId() {
        return ZoneId.of(resolveTimezone(proactive.getTimezone()));
    }

    public ZoneId chatPushZoneId() {
        return ZoneId.of(resolveTimezone(chatpush.getTimezone()));
    }

    public String resolveTimezone(String preferredTimezone) {
        if (StringUtils.hasText(preferredTimezone)) {
            return preferredTimezone.trim();
        }
        if (StringUtils.hasText(timezone)) {
            return timezone.trim();
        }
        return "Asia/Shanghai";
    }

    public static class OneBotProperties {

        @NotBlank
        private String apiBaseUrl;

        private String accessToken;

        public String getApiBaseUrl() {
            return apiBaseUrl;
        }

        public void setApiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class ChatProperties {

        private int timeoutSeconds = 20;

        private int maxReplyTokens = 500;

        private int maxContextMessages = 20;

        @Valid
        private final QueueProperties queue = new QueueProperties();

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxReplyTokens() {
            return maxReplyTokens;
        }

        public void setMaxReplyTokens(int maxReplyTokens) {
            this.maxReplyTokens = maxReplyTokens;
        }

        public int getMaxContextMessages() {
            return maxContextMessages;
        }

        public void setMaxContextMessages(int maxContextMessages) {
            this.maxContextMessages = maxContextMessages;
        }

        public QueueProperties getQueue() {
            return queue;
        }
    }

    public static class QueueProperties {

        private long debounceMillis = 1200;

        private int processingLockSeconds = 120;

        private int maxBatchSize = 8;

        private int maxMergedMessageChars = 4000;

        public long getDebounceMillis() {
            return debounceMillis;
        }

        public void setDebounceMillis(long debounceMillis) {
            this.debounceMillis = debounceMillis;
        }

        public int getProcessingLockSeconds() {
            return processingLockSeconds;
        }

        public void setProcessingLockSeconds(int processingLockSeconds) {
            this.processingLockSeconds = processingLockSeconds;
        }

        public int getMaxBatchSize() {
            return maxBatchSize;
        }

        public void setMaxBatchSize(int maxBatchSize) {
            this.maxBatchSize = maxBatchSize;
        }

        public int getMaxMergedMessageChars() {
            return maxMergedMessageChars;
        }

        public void setMaxMergedMessageChars(int maxMergedMessageChars) {
            this.maxMergedMessageChars = maxMergedMessageChars;
        }
    }

    public static class MemoryProperties {

        private boolean autoExtractEnabled = false;

        public boolean isAutoExtractEnabled() {
            return autoExtractEnabled;
        }

        public void setAutoExtractEnabled(boolean autoExtractEnabled) {
            this.autoExtractEnabled = autoExtractEnabled;
        }
    }

    public static class ProactiveProperties {

        private boolean enabled = true;

        private long scanIntervalMillis = 30000;

        private String quietHoursStart = "23:30";

        private String quietHoursEnd = "08:30";

        private int maxPerDay = 3;

        private String timezone = "Asia/Shanghai";

        private int deferMinutesWhenQuiet = 30;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getScanIntervalMillis() {
            return scanIntervalMillis;
        }

        public void setScanIntervalMillis(long scanIntervalMillis) {
            this.scanIntervalMillis = scanIntervalMillis;
        }

        public String getQuietHoursStart() {
            return quietHoursStart;
        }

        public void setQuietHoursStart(String quietHoursStart) {
            this.quietHoursStart = quietHoursStart;
        }

        public String getQuietHoursEnd() {
            return quietHoursEnd;
        }

        public void setQuietHoursEnd(String quietHoursEnd) {
            this.quietHoursEnd = quietHoursEnd;
        }

        public int getMaxPerDay() {
            return maxPerDay;
        }

        public void setMaxPerDay(int maxPerDay) {
            this.maxPerDay = maxPerDay;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public int getDeferMinutesWhenQuiet() {
            return deferMinutesWhenQuiet;
        }

        public void setDeferMinutesWhenQuiet(int deferMinutesWhenQuiet) {
            this.deferMinutesWhenQuiet = deferMinutesWhenQuiet;
        }
    }

    public static class ReminderProperties {

        private boolean naturalLanguageEnabled = true;

        private boolean aiIntentEnabled = true;

        private boolean aiReplyEnabled = true;

        private int maxCandidates = 5;

        public boolean isNaturalLanguageEnabled() {
            return naturalLanguageEnabled;
        }

        public void setNaturalLanguageEnabled(boolean naturalLanguageEnabled) {
            this.naturalLanguageEnabled = naturalLanguageEnabled;
        }

        public boolean isAiIntentEnabled() {
            return aiIntentEnabled;
        }

        public void setAiIntentEnabled(boolean aiIntentEnabled) {
            this.aiIntentEnabled = aiIntentEnabled;
        }

        public boolean isAiReplyEnabled() {
            return aiReplyEnabled;
        }

        public void setAiReplyEnabled(boolean aiReplyEnabled) {
            this.aiReplyEnabled = aiReplyEnabled;
        }

        public int getMaxCandidates() {
            return maxCandidates;
        }

        public void setMaxCandidates(int maxCandidates) {
            this.maxCandidates = maxCandidates;
        }
    }

    public static class ChatPushProperties {

        private boolean enabled = false;

        private long scanIntervalMillis = 600000;

        private int minIdleHours = 6;

        private int cooldownHours = 6;

        private int maxPerDay = 2;

        private String quietHoursStart = "23:30";

        private String quietHoursEnd = "08:30";

        private String timezone = "Asia/Shanghai";

        private int maxMessageLength = 120;

        private boolean useAiGenerator = true;

        private int minAllowedIdleMinutes = 10;

        private int minAllowedCooldownMinutes = 10;

        private int maxAllowedPerDay = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getScanIntervalMillis() {
            return scanIntervalMillis;
        }

        public void setScanIntervalMillis(long scanIntervalMillis) {
            this.scanIntervalMillis = scanIntervalMillis;
        }

        public int getMinIdleHours() {
            return minIdleHours;
        }

        public void setMinIdleHours(int minIdleHours) {
            this.minIdleHours = minIdleHours;
        }

        public int getCooldownHours() {
            return cooldownHours;
        }

        public void setCooldownHours(int cooldownHours) {
            this.cooldownHours = cooldownHours;
        }

        public int getMaxPerDay() {
            return maxPerDay;
        }

        public void setMaxPerDay(int maxPerDay) {
            this.maxPerDay = maxPerDay;
        }

        public String getQuietHoursStart() {
            return quietHoursStart;
        }

        public void setQuietHoursStart(String quietHoursStart) {
            this.quietHoursStart = quietHoursStart;
        }

        public String getQuietHoursEnd() {
            return quietHoursEnd;
        }

        public void setQuietHoursEnd(String quietHoursEnd) {
            this.quietHoursEnd = quietHoursEnd;
        }

        public String getTimezone() {
            return timezone;
        }

        public void setTimezone(String timezone) {
            this.timezone = timezone;
        }

        public int getMaxMessageLength() {
            return maxMessageLength;
        }

        public void setMaxMessageLength(int maxMessageLength) {
            this.maxMessageLength = maxMessageLength;
        }

        public boolean isUseAiGenerator() {
            return useAiGenerator;
        }

        public void setUseAiGenerator(boolean useAiGenerator) {
            this.useAiGenerator = useAiGenerator;
        }

        public int getMinAllowedIdleMinutes() {
            return minAllowedIdleMinutes;
        }

        public void setMinAllowedIdleMinutes(int minAllowedIdleMinutes) {
            this.minAllowedIdleMinutes = minAllowedIdleMinutes;
        }

        public int getMinAllowedCooldownMinutes() {
            return minAllowedCooldownMinutes;
        }

        public void setMinAllowedCooldownMinutes(int minAllowedCooldownMinutes) {
            this.minAllowedCooldownMinutes = minAllowedCooldownMinutes;
        }

        public int getMaxAllowedPerDay() {
            return maxAllowedPerDay;
        }

        public void setMaxAllowedPerDay(int maxAllowedPerDay) {
            this.maxAllowedPerDay = maxAllowedPerDay;
        }
    }

    public static class PromptProperties {

        private String profileFile;

        private String profileText;

        private String profileSourcePriority = "file-first";

        private boolean reloadFileEachRequest = false;

        private int maxProfileChars = 8000;

        private boolean includeTimeContext = true;

        public String getProfileFile() {
            return profileFile;
        }

        public void setProfileFile(String profileFile) {
            this.profileFile = profileFile;
        }

        public String getProfileText() {
            return profileText;
        }

        public void setProfileText(String profileText) {
            this.profileText = profileText;
        }

        public String getProfileSourcePriority() {
            return profileSourcePriority;
        }

        public void setProfileSourcePriority(String profileSourcePriority) {
            this.profileSourcePriority = profileSourcePriority;
        }

        public boolean isReloadFileEachRequest() {
            return reloadFileEachRequest;
        }

        public void setReloadFileEachRequest(boolean reloadFileEachRequest) {
            this.reloadFileEachRequest = reloadFileEachRequest;
        }

        public int getMaxProfileChars() {
            return maxProfileChars;
        }

        public void setMaxProfileChars(int maxProfileChars) {
            this.maxProfileChars = maxProfileChars;
        }

        public boolean isIncludeTimeContext() {
            return includeTimeContext;
        }

        public void setIncludeTimeContext(boolean includeTimeContext) {
            this.includeTimeContext = includeTimeContext;
        }
    }

    public static class DeliveryProperties {

        private boolean splitEnabled = true;

        private int maxParts = 3;

        private int maxPartChars = 80;

        private long minDelayMillis = 700;

        private long maxDelayMillis = 1800;

        private boolean splitDailyChatOnly = true;

        public boolean isSplitEnabled() {
            return splitEnabled;
        }

        public void setSplitEnabled(boolean splitEnabled) {
            this.splitEnabled = splitEnabled;
        }

        public int getMaxParts() {
            return maxParts;
        }

        public void setMaxParts(int maxParts) {
            this.maxParts = maxParts;
        }

        public int getMaxPartChars() {
            return maxPartChars;
        }

        public void setMaxPartChars(int maxPartChars) {
            this.maxPartChars = maxPartChars;
        }

        public long getMinDelayMillis() {
            return minDelayMillis;
        }

        public void setMinDelayMillis(long minDelayMillis) {
            this.minDelayMillis = minDelayMillis;
        }

        public long getMaxDelayMillis() {
            return maxDelayMillis;
        }

        public void setMaxDelayMillis(long maxDelayMillis) {
            this.maxDelayMillis = maxDelayMillis;
        }

        public boolean isSplitDailyChatOnly() {
            return splitDailyChatOnly;
        }

        public void setSplitDailyChatOnly(boolean splitDailyChatOnly) {
            this.splitDailyChatOnly = splitDailyChatOnly;
        }
    }
}
