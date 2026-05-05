package com.chatbot.prompt;

import com.chatbot.config.BotProperties;
import com.chatbot.prompt.dto.TimeContext;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TimeContextProvider {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BotProperties botProperties;
    private final Clock clock;

    @Autowired
    public TimeContextProvider(BotProperties botProperties) {
        this(botProperties, Clock.systemUTC());
    }

    TimeContextProvider(BotProperties botProperties, Clock clock) {
        this.botProperties = botProperties;
        this.clock = clock;
    }

    public static TimeContextProvider forClock(BotProperties botProperties, Clock clock) {
        return new TimeContextProvider(botProperties, clock);
    }

    public TimeContext getCurrentTimeContext() {
        ZoneId zoneId = botProperties.promptZoneId();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), zoneId);
        String chineseDateText = now.getYear()
                + "年" + now.getMonthValue()
                + "月" + now.getDayOfMonth()
                + "日，" + toChineseWeekday(now.getDayOfWeek());
        String block = "[当前时间]\n"
                + "当前时间：" + now.format(DATE_TIME_FORMATTER) + '\n'
                + "当前时区：" + zoneId.getId() + '\n'
                + "今天是：" + chineseDateText;
        return new TimeContext(now, zoneId.getId(), chineseDateText, block);
    }

    private String toChineseWeekday(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "星期一";
            case TUESDAY -> "星期二";
            case WEDNESDAY -> "星期三";
            case THURSDAY -> "星期四";
            case FRIDAY -> "星期五";
            case SATURDAY -> "星期六";
            case SUNDAY -> "星期日";
        };
    }
}
