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
}
