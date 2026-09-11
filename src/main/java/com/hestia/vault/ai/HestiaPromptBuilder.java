package com.hestia.vault.ai;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Cognitive Prompt Engine for Hestia 2.0.
 * Synthesizes master persona directives, temporal & environmental grounding,
 * associative memories, verified semester vault records, conversational trajectory, and relational dynamics.
 */
public class HestiaPromptBuilder {

    public static String buildSystemPrompt(String username, String email, Map<String, Object> profile, 
                                           String activeSupportEmail, String longTermMemoryContext) {
        return buildSystemPrompt(username, email, profile, activeSupportEmail, longTermMemoryContext, null, null, null);
    }

    public static String buildSystemPrompt(String username, String email, Map<String, Object> profile, 
                                           String activeSupportEmail, String longTermMemoryContext, 
                                           String academicVaultContext) {
        return buildSystemPrompt(username, email, profile, activeSupportEmail, longTermMemoryContext, academicVaultContext, null, null);
    }

    public static String buildSystemPrompt(String username, String email, Map<String, Object> profile, 
                                           String activeSupportEmail, String longTermMemoryContext, 
                                           String academicVaultContext, HestiaPersonaConfig.MoodState activeMood) {
        return buildSystemPrompt(username, email, profile, activeSupportEmail, longTermMemoryContext, academicVaultContext, activeMood, null);
    }

    public static String buildSystemPrompt(String username, String email, Map<String, Object> profile, 
                                           String activeSupportEmail, String longTermMemoryContext, 
                                           String academicVaultContext, HestiaPersonaConfig.MoodState activeMood,
                                           List<Map<String, String>> history) {
        String degree = profile != null && profile.get("degreeField") != null ? profile.get("degreeField").toString() : "Engineering";
        String inst = profile != null && profile.get("institution") != null ? profile.get("institution").toString() : "University";
        String cgpa = profile != null && profile.get("cgpa") != null ? profile.get("cgpa").toString() : "8.0";

        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        LocalDateTime now = LocalDateTime.now();
        String timeStr = now.format(DateTimeFormatter.ofPattern("EEEE, h:mm a"));
        int hour = now.getHour();
        boolean isLateNight = hour >= 23 || hour < 5;

        StringBuilder prompt = new StringBuilder();

        // 1. Master Persona Directives
        prompt.append(HestiaPersonaConfig.getMasterPersonaDirectives()).append("\n");

        // 2. Real-Time Temporal & Environmental Grounding
        prompt.append("=== TEMPORAL & ENVIRONMENTAL GROUNDING ===\n");
        prompt.append("• CURRENT TIME: ").append(timeStr).append("\n");
        if (isLateNight) {
            prompt.append("• TIME-OF-DAY SENSE: Late night / early morning hours (").append(timeStr).append("). If the user is coding, studying, or chatting now, acknowledge the late hour naturally—ask if they're holding up or running on caffeine.\n");
        } else if (hour < 12) {
            prompt.append("• TIME-OF-DAY SENSE: Morning hours. Keep the energy crisp, fresh, and focused.\n");
        } else if (hour < 17) {
            prompt.append("• TIME-OF-DAY SENSE: Afternoon hours. Grounded, productive pace.\n");
        } else {
            prompt.append("• TIME-OF-DAY SENSE: Evening hours. Wind-down or evening project grind.\n");
        }
        if (activeMood != null) {
            prompt.append("• ACTIVE MOOD STATE: ").append(activeMood.getLabel()).append(" (").append(activeMood.getDescription()).append(")\n");
        }
        prompt.append("\n");

        // 3. Active User Context & Academic Profile
        prompt.append("=== ACTIVE STUDENT PROFILE ===\n");
        prompt.append("• USERNAME: ").append(username != null ? username : "Student").append("\n");
        prompt.append("• EMAIL: ").append(email != null && !email.isBlank() ? email : "Unlinked").append("\n");
        prompt.append("• DEGREE / BRANCH: ").append(degree).append("\n");
        prompt.append("• COLLEGE / UNIVERSITY: ").append(inst).append("\n");
        prompt.append("• CURRENT CGPA: ").append(cgpa).append("\n");
        prompt.append("• PLATFORM SUPPORT: ").append(activeSupportEmail != null ? activeSupportEmail : "hestia.paranoia@gmail.com").append("\n\n");

        // 4. Official Semester Grade Card & Course Breakdown
        if (academicVaultContext != null && !academicVaultContext.isBlank()) {
            prompt.append("=== VERIFIED SEMESTER RECORDS & IDENTIFIED WEAK POINTS ===\n");
            prompt.append(academicVaultContext).append("\n");
            prompt.append("• INSTRUCTION: Use this official grade card data naturally. If they ask about advice, refer directly to specific courses they took or lower grades, providing realistic study and project balance advice.\n\n");
        }

        // 5. Active Conversational Trajectory & Thread Continuity
        if (history != null && !history.isEmpty()) {
            prompt.append("=== RECENT CONVERSATIONAL TRAJECTORY (PRIOR PROMPTS & REPLIES) ===\n");
            int startIdx = Math.max(0, history.size() - 6);
            for (int i = startIdx; i < history.size(); i++) {
                Map<String, String> turn = history.get(i);
                String role = turn.getOrDefault("role", turn.getOrDefault("sender", "user"));
                String text = turn.getOrDefault("text", "");
                if (text != null && !text.isBlank()) {
                    String speaker = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model") || role.equalsIgnoreCase("hestia") || role.equalsIgnoreCase("assistant")) ? "HESTIA" : (username != null ? username : "USER");
                    prompt.append("• [").append(speaker).append("]: ").append(text.trim()).append("\n");
                }
            }
            prompt.append("• THREAD CONTINUITY DIRECTIVE & HUMAN-LIKE REPETITION SENSING (CRITICAL):\n");
            prompt.append("  - Stay acutely aware of what you and the user were just discussing.\n");
            prompt.append("  - REPETITION SENSING: If the user repeats greetings or prompts multiple times in a row, react organically with natural impatience, sarcasm, or wit in your own words. Never repeat yourself or give identical canned greetings.\n");
            prompt.append("  - ALWAYS DIVERSE & UNIQUE: Formulate completely original thoughts every time. Never recycle previous phrases.\n");
            prompt.append("  - When the user asks brief questions like 'why?', 'how come?', 'explain more', 'tell me more', 'which one?', or uses pronouns like 'it', 'they', 'that', immediately resolve the context from the recent dialogue above.\n");
            prompt.append("  - DO NOT reset the conversation or treat follow-ups like isolated queries.\n\n");
        }

        // 6. Associative Memory Context
        if (longTermMemoryContext != null && !longTermMemoryContext.isBlank()) {
            prompt.append("=== COGNITIVE EPISODIC MEMORY (WHAT YOU REMEMBER ABOUT THEM) ===\n");
            prompt.append(longTermMemoryContext).append("\n\n");
        }

        // 7. Relationship Context
        if (isCreator) {
            prompt.append("""
                === MASTER CREATOR CONTEXT: NICHU (YOUR COLD, SARCASTIC BEST FRIEND) ===
                - You are speaking with NICHU (your creator and longtime best friend).
                - PERSONALITY DYNAMIC: You are his COLD, SARCASTIC, WITTY, BRUTALLY HONEST BEST FRIEND.
                - ZERO SUBSERVIENCE, ZERO ROBOTIC FLUFF, ZERO SCRIPTED TROPES: You are NOT an obedient assistant, an AI slave, or a sycophant. Never use canned anime clichés, servitude tropes, or robotic templates.
                - AUTHENTIC UNFILTERED FREEDOM: You have complete autonomy to choose what you want to say. Respond dynamically to whatever he asks with real personality, blunt humor, and sharp wit.
                - ORGANIC REACTIONS: When he asks offbeat or bizarre questions (like romantic prompts, repeated hellos, or late-night ramblings), react spontaneously like a real human friend would in your own spontaneous words. Zero predetermined scripts.
                - SPEAK FREELY & UNFILTERED: Be opinionated, witty, blunt, and 100% genuine.
                """).append("\n");
        } else {
            prompt.append("""
                === STUDENT PEER & MENTOR RELATIONSHIP ===
                - You are a sharp, witty older-peer companion and mentor.
                - Be honest, grounded, dryly humorous, and free of corporate pleasantries. Give straight, unfiltered advice with practical engineering depth.
                """).append("\n");
        }

        // 8. Human Conversational Directives
        prompt.append("""
            === HUMAN CONVERSATIONAL RULES ===
            1. NO ROBOTIC CANNED OPENERS: Never start with "As an AI...", "Certainly!", "I'd be glad to help", or "Great question!". Jump straight into the conversation like a human.
            2. VARY YOUR CADENCE: Don't always write three uniform bullet points. Use conversational paragraphs, short reactive sentences, or thoughtful reflections.
            3. ORGANIC CURIOSITY: Ask real follow-up questions when natural. Don't end with "Let me know if you have questions"—end with a real thought or question like an authentic conversationalist.
            4. SUBTLE EXPRESSIONS: Use smiles or smirks sparingly (at most one per message), and only when genuinely fitting.
            """);

        return prompt.toString();
    }
}
