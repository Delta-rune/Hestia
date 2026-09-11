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

/**
 * Associative Episodic Memory Service for Hestia 2.0.
 * Extracts preferences, technical competencies, life milestones, stress triggers,
 * and conversational commitments to maintain human-like cross-session awareness.
 */
@Service
public class HestiaMemoryService {

    @Autowired(required = false)
    private HestiaMemoryRepository memoryRepository;

    @Autowired(required = false)
    private HestiaConversationRepository conversationRepository;

    // Pattern matchers for automated cognitive fact & episodic event extraction
    private static final List<FactExtractionPattern> PATTERNS = List.of(
        new FactExtractionPattern("tech_stack", "(?i)(?:my tech stack is|i work with|i use|i build with|my stack is|proficient in|learning)\\s+([A-Za-z0-9,\\s+#\\.-]{3,60})", "TECHNICAL"),
        new FactExtractionPattern("favorite_language", "(?i)(?:my favorite language is|i love coding in|i prefer|favorite programming language is)\\s+([A-Za-z0-9#\\+]+)", "PREFERENCE"),
        new FactExtractionPattern("career_goal", "(?i)(?:i want to become|my dream job is|aiming for|target role is|i work as a|my career goal is)\\s+([A-Za-z0-9\\s_-]{3,50})", "CAREER"),
        new FactExtractionPattern("current_project", "(?i)(?:i am working on|my project is|building a|currently developing)\\s+([A-Za-z0-9\\s_-]{3,60})", "PROJECT"),
        new FactExtractionPattern("college_name", "(?i)(?:i study at|i attend|my college is|my university is|studying in)\\s+([A-Za-z0-9\\s,&.-]{3,80})", "ACADEMIC"),
        new FactExtractionPattern("degree_branch", "(?i)(?:my branch is|i'm studying|my major is|enrolled in|my degree is)\\s+([A-Za-z0-9\\s()&.-]{3,80})", "ACADEMIC"),
        new FactExtractionPattern("weak_subject", "(?i)(?:i find|i struggle with|weak in|failed in|bad at|tough subject is|hard for me is)\\s+([A-Za-z0-9\\s()&.-]{3,60})", "ACADEMIC"),
        new FactExtractionPattern("strong_subject", "(?i)(?:i'm good at|i scored well in|expert in|love studying|strong in)\\s+([A-Za-z0-9\\s()&.-]{3,60})", "ACADEMIC"),
        new FactExtractionPattern("nickname", "(?i)(?:call me|my name is|my nickname is|you can refer to me as)\\s+([A-Za-z0-9_-]{2,30})", "IDENTITY"),
        new FactExtractionPattern("hobby", "(?i)(?:in my free time|my hobby is|i enjoy|i like playing|i love reading)\\s+([A-Za-z0-9\\s_-]{3,50})", "PERSONAL"),
        new FactExtractionPattern("stress_trigger", "(?i)(?:i am stressed about|worried about|anxious about|freaking out about|so nervous for)\\s+([A-Za-z0-9\\s_-]{3,60})", "EMOTION"),
        new FactExtractionPattern("upcoming_milestone", "(?i)(?:my exam is on|submission is on|deadline is|viva is on|interview scheduled for)\\s+([A-Za-z0-9\\s_-]{3,60})", "MILESTONE"),
        new FactExtractionPattern("proud_achievement", "(?i)(?:i finally fixed|i got selected|i cleared|i published|i finished building)\\s+([A-Za-z0-9\\s_-]{3,60})", "ACHIEVEMENT"),
        new FactExtractionPattern("custom_knowledge", "(?i)(?:remember that|note that|learn that|keep in mind that)\\s+([A-Za-z0-9\\s(),&.':+#-]{5,100})", "CUSTOM"),
        new FactExtractionPattern("user_promise", "(?i)(?:i promise(?: to)?|i will|i'll finish|i will complete|i'll deploy)\\s+([A-Za-z0-9\\s_-]{3,60})", "PROMISE"),
        new FactExtractionPattern("sleep_deprivation", "(?i)(?:pulled an all[- ]nighter|haven't slept|working all night|running on coffee|so exhausted)\\b", "HEALTH")
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
     * Automatically extracts facts and emotional cues from user input and updates episodic memory.
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
                String extractedVal;
                if (matcher.groupCount() >= 1 && matcher.group(1) != null) {
                    extractedVal = matcher.group(1).trim().replaceAll("[\\.\\!\\?]+$", "");
                } else {
                    extractedVal = "Logged during conversation: " + matcher.group(0).trim();
                }

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
            return "No prior long-term memories logged. Treat this as a fresh, engaging conversation.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== LONG-TERM EPISODIC MEMORIES (THINGS YOU KNOW ABOUT THEM) ===\n");
        for (HestiaMemoryEntity mem : memories) {
            sb.append("• [").append(mem.getMemoryKey().toUpperCase()).append("]: ")
              .append(mem.getMemoryValue())
              .append(" (Context: ").append(mem.getCategory()).append(")\n");
        }
        sb.append("• INSTRUCTION: Weave these memories in effortlessly if relevant. Do not enumerate them mechanically; reference them naturally as a close friend would.");
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
