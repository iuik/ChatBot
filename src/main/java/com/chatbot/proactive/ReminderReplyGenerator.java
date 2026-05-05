package com.chatbot.proactive;

import java.time.LocalDateTime;
import java.util.List;

public interface ReminderReplyGenerator {

    String generate(ReminderReplyAction action,
                    LocalDateTime scheduledAt,
                    String content,
                    List<String> candidates);
}
