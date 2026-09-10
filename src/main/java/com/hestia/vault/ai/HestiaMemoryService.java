package com.hestia.vault.ai;

import com.hestia.vault.model.HestiaConversationEntity;
import com.hestia.vault.model.HestiaMemoryEntity;
import com.hestia.vault.repository.HestiaConversationRepository;
import com.hestia.vault.repository.HestiaMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class HestiaMemoryService {

    @Autowired(required = false)
    private HestiaMemoryRepository memoryRepository;

    @Autowired(required = false)
    private HestiaConversationRepository conversationRepository;

    // Pattern matchers for automated fact extraction
    private static final List<FactExtractionPattern> PATTERNS = List.of(
        new FactExtractionPattern("tech_stack", "(?i)(?:my tech stack is|i work with|i use|i build with|my stack is|proficient in|learning)\\s+([A-Za-z0-9,\\s+#\\.-]{3,60})", "PREFERENCE"),
        new FactExtractionPattern("favorite_language", "(?i)(?:my favorite language is|i love coding in|i prefer|favorite programming language is)\\s+([A-Za-z0-9#\\+]+)", "PREFERENCE"),
        new FactExtractionPattern("career_goal", "(?i)(?:i want to become|my dream job is|aiming for|target role is|i work as a|my career goal is)\\s+([A-Za-z0-9\\s_-]{3,50})", "BACKGROUND"),
        new FactExtractionPattern("current_project", "(?i)(?:i am working on|my project is|building a|currently developing)\\s+([A-Za-z0-9\\s_-]{3,60})", "BACKGROUND"),
        new FactExtractionPattern("nickname", "(?i)(?:call me|my name is|my nickname is|you can refer to me as)\\s+([A-Za-z0-9_-]{2,30})", "PERSONALITY_TRAIT"),
        new FactExtractionPattern("hobby", "(?i)(?:in my free time|my hobby is|i enjoy|i like playing|i love reading)\\s+([A-Za-z0-9\\s_-]{3,50})", "PREFERENCE"),
        new FactExtractionPattern("user_promise", "(?i)(?:i promise|i will|i'll finish|i will complete|i'll deploy)\\s+([A-Za-z0-9\\s_-]{3,60})", "PROMISE")
    );

    private static class FactExtractionPattern {
        String key;
        Pattern pattern;
        String category;

        FactExtractionPattern(String key, String regex, String category) {
            this.key = key;
            this.pattern = Pattern.compile(regex);
            this.category = category;
        }
    }

    /**
     * Automatically extracts facts from user input and saves them to long-term memory.
     */
    @Transactional
    public List<HestiaMemoryEntity> autoExtractAndSaveFacts(String userIdentifier, String userMessage) {
        if (userMessage == null || userMessage.isBlank() || userIdentifier == null || userIdentifier.isBlank()) {
            return Collections.emptyList();
        }

        List<HestiaMemoryEntity> extracted = new ArrayList<>();
        String normalizedId = userIdentifier.toLowerCase().trim();

        for (FactExtractionPattern p : PATTERNS) {
            Matcher matcher = p.pattern.matcher(userMessage);
            if (matcher.find()) {
                String extractedVal = matcher.group(1).trim();
                // Clean trailing punctuation
                extractedVal = extractedVal.replaceAll("[\\.\\!\\?]+$", "");
                if (extractedVal.length() >= 2) {
                    HestiaMemoryEntity memory = saveOrUpdateMemory(normalizedId, p.key, extractedVal, p.category);
                    if (memory != null) {
                        extracted.add(memory);
                    }
                }
            }
        }
        return extracted;
    }

    @Transactional
    public HestiaMemoryEntity saveOrUpdateMemory(String userIdentifier, String memoryKey, String memoryValue, String category) {
        if (memoryRepository == null) return null;

        String normalizedId = userIdentifier.toLowerCase().trim();
        Optional<HestiaMemoryEntity> existingOpt = memoryRepository.findByUserIdentifierAndMemoryKey(normalizedId, memoryKey);

        HestiaMemoryEntity entity;
        if (existingOpt.isPresent()) {
            entity = existingOpt.get();
            entity.setMemoryValue(memoryValue);
            if (category != null) entity.setCategory(category);
        } else {
            entity = new HestiaMemoryEntity(normalizedId, memoryKey, memoryValue, category != null ? category : "PREFERENCE");
        }
        return memoryRepository.save(entity);
    }

    public List<HestiaMemoryEntity> getUserMemories(String userIdentifier) {
        if (memoryRepository == null || userIdentifier == null) return Collections.emptyList();
        return memoryRepository.findByUserIdentifierOrderByUpdatedAtDesc(userIdentifier.toLowerCase().trim());
    }

    public String buildMemoryPromptContext(String userIdentifier) {
        List<HestiaMemoryEntity> memories = getUserMemories(userIdentifier);
        if (memories.isEmpty()) {
            return "No persistent memories logged yet for this user.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== LONG-TERM MEMORY & RECORDED FACTS ABOUT USER ===\n");
        for (HestiaMemoryEntity mem : memories) {
            sb.append("• [").append(mem.getMemoryKey().toUpperCase()).append("]: ")
              .append(mem.getMemoryValue())
              .append(" (Category: ").append(mem.getCategory()).append(")\n");
        }
        return sb.toString();
    }

    @Transactional
    public void logConversationTurn(String userIdentifier, String sessionId, String userMessage, String hestiaReply, String tone) {
        if (conversationRepository == null || userIdentifier == null) return;

        String normalizedId = userIdentifier.toLowerCase().trim();
        String activeSession = (sessionId != null && !sessionId.isBlank()) ? sessionId : "default_session";

        if (userMessage != null && !userMessage.isBlank()) {
            conversationRepository.save(new HestiaConversationEntity(normalizedId, activeSession, "USER", userMessage, "NEUTRAL"));
        }

        if (hestiaReply != null && !hestiaReply.isBlank()) {
            conversationRepository.save(new HestiaConversationEntity(normalizedId, activeSession, "HESTIA", hestiaReply, tone));
        }
    }

    public List<HestiaConversationEntity> getRecentConversationHistory(String userIdentifier, int limit) {
        if (conversationRepository == null || userIdentifier == null) return Collections.emptyList();
        return conversationRepository.findByUserIdentifierOrderByTimestampDesc(userIdentifier.toLowerCase().trim(), PageRequest.of(0, limit));
    }

    @Transactional
    public boolean clearUserMemoryAndHistory(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.isBlank()) return false;
        String normalizedId = userIdentifier.toLowerCase().trim();
        boolean deleted = false;
        if (memoryRepository != null) {
            memoryRepository.deleteByUserIdentifier(normalizedId);
            deleted = true;
        }
        if (conversationRepository != null) {
            conversationRepository.deleteByUserIdentifier(normalizedId);
            deleted = true;
        }
        return deleted;
    }
}
