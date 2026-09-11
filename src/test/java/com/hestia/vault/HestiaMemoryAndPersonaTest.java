package com.hestia.vault;

import com.hestia.vault.ai.HestiaAiService;
import com.hestia.vault.ai.HestiaMemoryService;
import com.hestia.vault.ai.HestiaPersonaConfig;
import com.hestia.vault.ai.HestiaPromptBuilder;
import com.hestia.vault.model.HestiaMemoryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class HestiaMemoryAndPersonaTest {

    @Autowired(required = false)
    private HestiaMemoryService memoryService;

    @Autowired(required = false)
    private HestiaAiService aiService;

    @Test
    public void testPromptBuilderCreatorBondAndMemory() {
        Map<String, Object> profile = Map.of(
            "degreeField", "Computer Science",
            "institution", "MIT",
            "cgpa", "9.5"
        );
        String memoryContext = "• [TECH_STACK]: Java, Spring Boot, Docker, React\n• [CAREER_GOAL]: Senior AI Systems Engineer";

        String prompt = HestiaPromptBuilder.buildSystemPrompt("Nichu", "nichuag33@gmail.com", profile, "support@hestia.org", memoryContext);

        assertNotNull(prompt);
        assertTrue(prompt.contains("MASTER CREATOR CONTEXT: NICHU"));
        assertTrue(prompt.contains("HESTIA"));
        assertTrue(prompt.contains("Computer Science"));
        assertTrue(prompt.contains("TECH_STACK"));
        assertTrue(prompt.contains("Senior AI Systems Engineer"));
    }

    @Test
    public void testPersonaKaomojiAndBannedOpeners() {
        String kaomojiCold = HestiaPersonaConfig.getRandomKaomoji(HestiaPersonaConfig.ToneMode.COLD_SARCASTIC);
        assertNotNull(kaomojiCold);
        assertTrue(kaomojiCold.contains("(⁠"));

        assertFalse(HestiaPersonaConfig.BANNED_AI_OPENERS.isEmpty());
        assertTrue(HestiaPersonaConfig.BANNED_AI_OPENERS.contains("As an AI"));
    }

    @Test
    public void testAutomatedFactExtraction() {
        if (memoryService == null) return; // Skip if context not loaded in isolated test

        String userQuery = "My tech stack is Java, Spring Boot, Docker and Rust. I promise to deploy Hestia tomorrow.";
        List<HestiaMemoryEntity> facts = memoryService.autoExtractAndSaveFacts("testuser@gmail.com", userQuery);

        assertNotNull(facts);
        assertFalse(facts.isEmpty());
        assertTrue(facts.stream().anyMatch(f -> f.getMemoryKey().equalsIgnoreCase("tech_stack")));
        assertTrue(facts.stream().anyMatch(f -> f.getMemoryKey().equalsIgnoreCase("user_promise")));
    }

    @Test
    public void testResponseSanitizerStripsAiFluff() {
        HestiaAiService service = (aiService != null) ? aiService : new HestiaAiService();

        String rawAiInput = "As an AI language model, I would be happy to help you with your resume! Your CGPA is solid.";
        String sanitized = service.sanitizeHestiaResponse(rawAiInput, false);

        assertNotNull(sanitized);
        assertFalse(sanitized.toLowerCase().contains("as an ai language model"));
        assertFalse(sanitized.toLowerCase().contains("i would be happy to help"));
        assertTrue(sanitized.contains("(⁠"));
    }

    @Test
    public void testHestiaNeuralSimulatorCapabilities() {
        // Test Creator Bond
        var creatorResult = com.hestia.vault.ai.HestiaNeuralSimulator.simulateResponse(
            "Do you care about me Hestia?", "Nichu", "nichuag33@gmail.com", true, Map.of(), "", ""
        );
        assertNotNull(creatorResult);
        assertNotNull(creatorResult.getReply());
        assertEquals(com.hestia.vault.ai.HestiaPersonaConfig.MoodState.CREATOR_BOND, creatorResult.getMood());
        assertFalse(creatorResult.getSuggestedFollowUps().isEmpty());

        // Test Technical Domain
        var techResult = com.hestia.vault.ai.HestiaNeuralSimulator.simulateResponse(
            "Java vs Rust for backend development", "Student", "user@test.edu", false, Map.of(), "", ""
        );
        assertNotNull(techResult);
        assertTrue(techResult.getReply().contains("Java") && techResult.getReply().contains("Rust"));
        assertEquals(com.hestia.vault.ai.HestiaPersonaConfig.MoodState.LOCKED_IN, techResult.getMood());

        // Test Academic Vault Math
        Map<String, Object> profile = Map.of("cgpa", "7.2", "degreeField", "Computer Science", "institution", "KTU");
        var academicResult = com.hestia.vault.ai.HestiaNeuralSimulator.simulateResponse(
            "How can I calculate my target SGPA to improve my CGPA?", "Student", "user@test.edu", false, profile, "", ""
        );
        assertNotNull(academicResult);
        assertTrue(academicResult.getReply().contains("CGPA"));
        assertEquals(com.hestia.vault.ai.HestiaPersonaConfig.MoodState.ACADEMIC_MENTOR, academicResult.getMood());
    }

    @Test
    public void testMultiTurnConversationalAwareness() {
        List<Map<String, String>> history = List.of(
            Map.of("role", "user", "text", "What do you think of PostgreSQL vs MongoDB for student projects?"),
            Map.of("role", "assistant", "text", "PostgreSQL is unbeatable for relational consistency and ACID transactions, whereas MongoDB gives rapid schema-less prototyping.")
        );

        // 1. Verify Prompt Builder includes conversational trajectory and thread continuity directives
        String prompt = HestiaPromptBuilder.buildSystemPrompt(
            "Nichu", "nichuag33@gmail.com", Map.of(), "support@hestia.org", "", "", null, history
        );
        assertNotNull(prompt);
        assertTrue(prompt.contains("RECENT CONVERSATIONAL TRAJECTORY"));
        assertTrue(prompt.contains("PostgreSQL vs MongoDB"));
        assertTrue(prompt.contains("THREAD CONTINUITY DIRECTIVE"));

        // 2. Verify Neural Simulator resolves brief follow-up questions using prior context
        var followUpResult = com.hestia.vault.ai.HestiaNeuralSimulator.simulateResponse(
            "Which one do you recommend?", history, "Nichu", "nichuag33@gmail.com", true, Map.of(), "", ""
        );
        assertNotNull(followUpResult);
        assertNotNull(followUpResult.getReply());
        // Should reference the topic from prior dialogue
        assertTrue(followUpResult.getReply().toLowerCase().contains("postgres") || 
                   followUpResult.getReply().toLowerCase().contains("recommend") ||
                   followUpResult.getReply().toLowerCase().contains("choice"));
    }

    @Test
    public void testRepetitionAnnoyance() {
        // Simulating user saying "hai" 3 times in a row
        List<Map<String, String>> history = List.of(
            Map.of("role", "user", "text", "hai"),
            Map.of("role", "assistant", "text", "What do you want, Nichu?"),
            Map.of("role", "user", "text", "hai"),
            Map.of("role", "assistant", "text", "You literally just said that 10 seconds ago.")
        );

        // Third "hai" in a row
        var annoyedResult = com.hestia.vault.ai.HestiaNeuralSimulator.simulateResponse(
            "hai", history, "Nichu", "nichuag33@gmail.com", true, Map.of(), "", ""
        );

        assertNotNull(annoyedResult);
        String reply = annoyedResult.getReply().toLowerCase();
        // Verifying visible human annoyance / sarcasm at 3rd greeting in a row
        assertTrue(reply.contains("three times") || reply.contains("three") || 
                   reply.contains("keyboard") || reply.contains("echo") || 
                   reply.contains("syllable") || reply.contains("packets") || 
                   reply.contains("row") || reply.contains("lagging"),
                   "Hestia should get visibly annoyed when user says 'hai' 3 times. Got: " + annoyedResult.getReply());
    }
}

