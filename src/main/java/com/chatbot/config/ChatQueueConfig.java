package com.chatbot.config;

import com.chatbot.chat.QueueProcessingPauser;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class ChatQueueConfig {

    @Bean
    public Executor chatQueueTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("chat-queue-");
        executor.setConcurrencyLimit(8);
        return executor;
    }

    @Bean
    public QueueProcessingPauser queueProcessingPauser() {
        return Thread::sleep;
    }
}
