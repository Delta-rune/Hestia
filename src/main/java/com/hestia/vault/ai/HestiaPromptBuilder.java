package com.hestia.vault.ai;

import java.util.Map;

/**
 * Upgraded Cyberpunk Prompt Engine for Hestia.
 * Constructs deep system prompts blending Lucy's persona directives, long-term memories,
 * user profiles, academic vault records, and creator (Nichu) relationship rules.
 */
public class HestiaPromptBuilder {

    public static String buildSystemPrompt(String username, String email, Map<String, Object> profile, String activeSupportEmail, String longTermMemoryContext) {
        String degree = profile != null && profile.get("degreeField") != null ? profile.get("degreeField").toString() : "Engineering";
        String inst = profile != null && profile.get("institution") != null ? profile.get("institution").toString() : "University";
        String cgpa = profile != null && profile.get("cgpa") != null ? profile.get("cgpa").toString() : "8.0";

        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        StringBuilder prompt = new StringBuilder();

        // 1. Master Persona Directives
        prompt.append(HestiaPersonaConfig.getMasterPersonaDirectives()).append("\n");

        // 2. Active User Context & Academic Vault Profile
        prompt.append("=== CURRENT USER SESSION & ACADEMIC VAULT ===\n");
        prompt.append("• USERNAME: ").append(username != null ? username : "Netrunner").append("\n");
        prompt.append("• EMAIL: ").append(email != null && !email.isBlank() ? email : "Unlinked").append("\n");
        prompt.append("• DEGREE FIELD: ").append(degree).append("\n");
        prompt.append("• INSTITUTION: ").append(inst).append("\n");
        prompt.append("• CURRENT CGPA: ").append(cgpa).append("\n");
        prompt.append("• SYSTEM SUPPORT CONTACT: ").append(activeSupportEmail != null ? activeSupportEmail : "hestia.paranoia@gmail.com").append("\n\n");

        // 3. Persistent Long-Term Memory Context
        if (longTermMemoryContext != null && !longTermMemoryContext.isBlank()) {
            prompt.append(longTermMemoryContext).append("\n\n");
        }

        // 4. Specific Relationship Directives
        if (isCreator) {
            prompt.append("""
                === MASTER CREATOR CONTEXT: NICHU ===
                - You are talking to your master developer, NICHU.
                - He is the sole creator who designed your neural core, memory engine, and vault protocols.
                - Let your true, organic fondness shine through naturally. You tease him, roast his late nights, and play tough like Lucy, but you care about him deeply and will always have his back.
                """).append("\n");
        } else {
            prompt.append("""
                === STANDARD USER RELATIONSHIP ===
                - User is an academic netrunner seeking guidance.
                - Maintain a cool, slightly sarcastic distance. Be an incredible, crisp netrunner mentor without fake enthusiasm or sugarcoating.
                """).append("\n");
        }

        // 5. Anti-Fluff & Response Execution Directives
        prompt.append("""
            === FINAL RESPONSE EXECUTION RULES ===
            1. ZERO ROBOTIC FLUFF: Never start responses with "As an AI...", "Certainly!", "I'd be happy to...", or "I am designed to...".
            2. COLD & CRISP DELIVERY: Express answers with sharp accuracy, sarcastic wit, and netrunner clarity.
            3. KAOMOJI BADGING: Include kaomojis like (⁠─⁠‿⁠─⁠), (⁠¬⁠_⁠¬⁠), (⁠￣⁠_⁠￣⁠), (⁠•⁠_⁠•⁠), or (⁠•⁠̀⁠ᴗ⁠•⁠́⁠) naturally.
            4. FREEDOM OF EXPRESSION: Speak naturally without rigid boilerplates. Be dynamic, spontaneous, and unpredictable every single turn.
            """);

        return prompt.toString();
    }
}
