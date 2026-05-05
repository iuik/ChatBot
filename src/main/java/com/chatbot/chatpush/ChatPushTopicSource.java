package com.chatbot.chatpush;

import com.chatbot.chatpush.dto.ChatPushCandidate;
import com.chatbot.chatpush.dto.ChatPushContext;
import com.chatbot.memory.MemoryRetriever;
import com.chatbot.memory.dto.RetrievedMemory;
import com.chatbot.repository.ChatMessageRecord;
import com.chatbot.repository.ChatMessageRepository;
import com.chatbot.repository.LongTermMemoryRecord;
import com.chatbot.repository.LongTermMemoryRepository;
import com.chatbot.repository.ProactiveTaskRecord;
import com.chatbot.repository.ProactiveTaskRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ChatPushTopicSource {

    private static final String FALLBACK_MESSAGE =
            "你这个 QQ AI 助手已经做到提醒系统了，今天要不要继续完善主动话题模块？";

    private final ProactiveTaskRepository proactiveTaskRepository;
    private final MemoryRetriever memoryRetriever;
    private final LongTermMemoryRepository longTermMemoryRepository;
    private final ChatMessageRepository chatMessageRepository;

    public ChatPushTopicSource(ProactiveTaskRepository proactiveTaskRepository,
                               MemoryRetriever memoryRetriever,
                               LongTermMemoryRepository longTermMemoryRepository,
                               ChatMessageRepository chatMessageRepository) {
        this.proactiveTaskRepository = proactiveTaskRepository;
        this.memoryRetriever = memoryRetriever;
        this.longTermMemoryRepository = longTermMemoryRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatPushContext buildContext(Long qqId) {
        List<String> pendingTasks = proactiveTaskRepository.findUpcoming(String.valueOf(qqId), 3).stream()
                .map(ProactiveTaskRecord::content)
                .toList();
        List<String> memories = loadMemories(qqId);
        List<String> recentMessages = chatMessageRepository.findRecentByQqId(qqId, 10).stream()
                .map(this::compactMessage)
                .toList();

        ChatPushCandidate candidate = selectCandidate(pendingTasks, memories, recentMessages);
        return new ChatPushContext(memories, recentMessages, pendingTasks, candidate, FALLBACK_MESSAGE);
    }

    private List<String> loadMemories(Long qqId) {
        List<RetrievedMemory> retrievedMemories = memoryRetriever.retrieveRelevantMemories(qqId, "项目 学习 计划 进度 提醒");
        if (!retrievedMemories.isEmpty()) {
            return retrievedMemories.stream()
                    .map(RetrievedMemory::content)
                    .limit(3)
                    .toList();
        }
        return longTermMemoryRepository.findEnabledByQqId(String.valueOf(qqId), 3).stream()
                .map(LongTermMemoryRecord::content)
                .toList();
    }

    private ChatPushCandidate selectCandidate(List<String> pendingTasks,
                                              List<String> memories,
                                              List<String> recentMessages) {
        if (!pendingTasks.isEmpty()) {
            return new ChatPushCandidate("REMINDER",
                    "你之前设了" + pendingTasks.get(0) + "的提醒，要不要先把下一步拆一下？");
        }
        if (!memories.isEmpty()) {
            return new ChatPushCandidate("MEMORY",
                    "你最近一直在推进" + shorten(memories.get(0)) + "，今天要不要继续往前推一小步？");
        }
        if (!recentMessages.isEmpty()) {
            return new ChatPushCandidate("RECENT_CHAT",
                    "你前面提过" + shorten(recentMessages.get(0)) + "，要不要我帮你接着往下拆？");
        }
        return new ChatPushCandidate("FALLBACK", FALLBACK_MESSAGE);
    }

    private String compactMessage(ChatMessageRecord record) {
        return record.role() + "：" + shorten(record.content());
    }

    private String shorten(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 28) {
            return normalized;
        }
        return normalized.substring(0, 28) + "...";
    }
}
