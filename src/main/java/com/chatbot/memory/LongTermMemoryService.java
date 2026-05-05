package com.chatbot.memory;

import com.chatbot.config.BotProperties;
import com.chatbot.memory.dto.MemoryCandidate;
import com.chatbot.memory.dto.MemoryItem;
import com.chatbot.repository.LongTermMemoryRecord;
import com.chatbot.repository.LongTermMemoryRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LongTermMemoryService {

    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryService.class);

    private static final int MAX_MEMORY_LIST = 10;
    private static final int MAX_MEMORY_CHARS = 1000;

    private final LongTermMemoryRepository longTermMemoryRepository;
    private final EmbeddingClient embeddingClient;
    private final QdrantMemoryStore qdrantMemoryStore;
    private final MemoryExtractor memoryExtractor;
    private final boolean autoExtractEnabled;

    public LongTermMemoryService(LongTermMemoryRepository longTermMemoryRepository,
                                 EmbeddingClient embeddingClient,
                                 QdrantMemoryStore qdrantMemoryStore,
                                 MemoryExtractor memoryExtractor,
                                 BotProperties botProperties) {
        this.longTermMemoryRepository = longTermMemoryRepository;
        this.embeddingClient = embeddingClient;
        this.qdrantMemoryStore = qdrantMemoryStore;
        this.memoryExtractor = memoryExtractor;
        this.autoExtractEnabled = botProperties.getMemory().isAutoExtractEnabled();
    }

    public String handleCommand(Long qqId, String text) {
        if ("/memory".equals(text.trim())) {
            return listMemories(qqId);
        }
        if (text.startsWith("/memory add ")) {
            return addMemory(qqId, text.substring("/memory add ".length()).trim(), null);
        }
        if (text.startsWith("/memory del ")) {
            return deleteMemory(qqId, text.substring("/memory del ".length()).trim());
        }
        return null;
    }

    public void extractAndStore(Long qqId, String mergedUserMessage, Long sourceMessageId) {
        if (!autoExtractEnabled) {
            return;
        }
        Optional<MemoryCandidate> candidate = memoryExtractor.extract(mergedUserMessage, sourceMessageId);
        candidate.ifPresent(memoryCandidate -> {
            try {
                saveMemoryInternal(qqId, memoryCandidate.content(), memoryCandidate.sourceMessageId(), memoryCandidate.importance());
            } catch (Exception ex) {
                log.warn("Failed to auto extract long term memory for qqId={}", qqId, ex);
            }
        });
    }

    public List<MemoryItem> getEnabledMemories(Long qqId, int limit) {
        return longTermMemoryRepository.findEnabledByQqId(String.valueOf(qqId), limit).stream()
                .map(record -> new MemoryItem(record.id(), record.qqId(), record.content(), record.importance(), record.enabled()))
                .toList();
    }

    private String listMemories(Long qqId) {
        List<MemoryItem> memories = getEnabledMemories(qqId, MAX_MEMORY_LIST);
        if (memories.isEmpty()) {
            return "当前没有长期记忆。";
        }
        StringBuilder builder = new StringBuilder("当前长期记忆：\n");
        for (MemoryItem memory : memories) {
            builder.append(memory.id()).append(". ").append(memory.content()).append("\n");
        }
        return builder.toString().trim();
    }

    private String addMemory(Long qqId, String content, Long sourceMessageId) {
        if (!StringUtils.hasText(content)) {
            return "请提供要保存的记忆内容。";
        }
        if (content.length() > MAX_MEMORY_CHARS) {
            return "记忆内容过长，请控制在 1000 字以内。";
        }
        if (!embeddingClient.isEnabled() || !qdrantMemoryStore.isEnabled()) {
            saveMemoryInternal(qqId, content, sourceMessageId, 8);
            return "已保存记忆：" + content;
        }
        SaveMemoryResult result = saveMemoryInternal(qqId, content, sourceMessageId, 8);
        if (result.indexed()) {
            return "已保存记忆：" + content;
        }
        return "记忆已保存，但向量索引失败";
    }

    private String deleteMemory(Long qqId, String idText) {
        Long memoryId;
        try {
            memoryId = Long.parseLong(idText.trim());
        } catch (Exception ex) {
            return "记忆 id 格式不正确。";
        }

        Optional<LongTermMemoryRecord> memory = longTermMemoryRepository.findByIdAndQqId(memoryId, String.valueOf(qqId));
        if (memory.isEmpty() || !memory.get().enabled()) {
            return "未找到该记忆。";
        }

        longTermMemoryRepository.disable(memoryId, String.valueOf(qqId));
        try {
            qdrantMemoryStore.delete(memory.get().qdrantPointId());
        } catch (Exception ex) {
            log.warn("Failed to delete Qdrant memory point for memoryId={}", memoryId, ex);
        }
        return "已删除记忆 " + memoryId;
    }

    private SaveMemoryResult saveMemoryInternal(Long qqId, String content, Long sourceMessageId, int importance) {
        Long id = longTermMemoryRepository.save(String.valueOf(qqId), null, content, sourceMessageId, importance, null);
        if (!embeddingClient.isEnabled() || !qdrantMemoryStore.isEnabled()) {
            return new SaveMemoryResult(id, false);
        }

        String pointId = UUID.randomUUID().toString();
        try {
            List<Double> vector = embeddingClient.embed(content);
            qdrantMemoryStore.upsert(pointId, vector, Map.of(
                    "memory_id", id,
                    "qq_id", String.valueOf(qqId),
                    "content", content,
                    "enabled", true,
                    "importance", importance
            ));
            longTermMemoryRepository.updateQdrantPointId(id, pointId);
            return new SaveMemoryResult(id, true);
        } catch (Exception ex) {
            log.warn("Failed to build long term memory index for memoryId={}", id, ex);
            return new SaveMemoryResult(id, false);
        }
    }

    private record SaveMemoryResult(Long id, boolean indexed) {
    }
}
