package com.hestia.vault.ai;

import com.hestia.vault.model.HestiaMemoryEntity;
import com.hestia.vault.model.SystemSetting;
import com.hestia.vault.repository.SystemSettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HestiaAiService {

    @Autowired(required = false)
    private SystemSettingRepository systemSettingRepository;

    @Autowired(required = false)
    private HestiaMemoryService memoryService;

    @Value("${groq.apiKey:}")
    private String groqApiKey;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${gemini.apiKey:}")
    private String geminiApiKey;

    public String generateResponse(String query, String username, String email, List<Map<String, String>> history, Map<String, Object> profile) {
        String effectiveUserIdentifier = (email != null && !email.isBlank()) ? email : (username != null ? username : "Friend");
        
        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        // DYNAMIC PREFERENCE LEARNING: Check if creator is updating system preferences
        if (isCreator && query != null) {
            Matcher emailMatcher = Pattern.compile("(?i)(?:set|update|change|remember)\\s+(?:support\\s+email|contact\\s+email|email)\\s+(?:to|is|=)?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(query);
            if (emailMatcher.find()) {
                String newSupportEmail = emailMatcher.group(1);
                if (systemSettingRepository != null) {
                    systemSettingRepository.save(new SystemSetting("support_email", newSupportEmail, username));
                }
                String confirmMsg = "Got it, **Nichu**. (⁠─⁠‿⁠─⁠) System support contact updated to **" + newSupportEmail + "**. Network synced.";
                if (memoryService != null) {
                    memoryService.logConversationTurn(effectiveUserIdentifier, null, query, confirmMsg, "CREATOR_FONDNESS");
                }
                return confirmMsg;
            }
        }

        // 1. Fetch persistent long-term memory context if memoryService is active
        String memoryContext = "";
        if (memoryService != null) {
            // Auto-extract facts from current user prompt
            memoryService.autoExtractAndSaveFacts(effectiveUserIdentifier, query);
            memoryContext = memoryService.buildMemoryPromptContext(effectiveUserIdentifier);
        }

        // 2. Fetch dynamic system settings from database
        String activeSupportEmail = "hestia.paranoia@gmail.com";
        if (systemSettingRepository != null) {
            Optional<SystemSetting> settingOpt = systemSettingRepository.findBySettingKey("support_email");
            if (settingOpt.isPresent()) {
                activeSupportEmail = settingOpt.get().getSettingValue();
            }
        }

        // 3. Construct System Prompt with Persona & Memory
        String systemPromptText = HestiaPromptBuilder.buildSystemPrompt(username, email, profile, activeSupportEmail, memoryContext);

        String effectiveGroqKey = System.getenv("GROQ_API_KEY");
        if (effectiveGroqKey == null || effectiveGroqKey.isBlank()) {
            effectiveGroqKey = System.getenv("groq.apiKey");
        }
        if (effectiveGroqKey == null || effectiveGroqKey.isBlank()) {
            effectiveGroqKey = groqApiKey;
        }

        String rawResponse = null;

        // --- Provider 1: Try Groq Cloud API ---
        if (effectiveGroqKey != null && !effectiveGroqKey.isBlank() && !effectiveGroqKey.startsWith("YOUR_")) {
            rawResponse = callGroqApi(effectiveGroqKey, systemPromptText, history, query, username);
        }

        // --- Provider 2: Try Ollama Local API if Groq unavailable ---
        if ((rawResponse == null || rawResponse.isBlank()) && ollamaUrl != null) {
            rawResponse = callOllamaApi(systemPromptText, history, query, username);
        }

        // --- Provider 3: Try Gemini API if available ---
        String effectiveGeminiKey = System.getenv("GEMINI_API_KEY");
        if (effectiveGeminiKey == null || effectiveGeminiKey.isBlank()) {
            effectiveGeminiKey = geminiApiKey;
        }

        if ((rawResponse == null || rawResponse.isBlank()) && effectiveGeminiKey != null && !effectiveGeminiKey.isBlank() && !effectiveGeminiKey.startsWith("YOUR_")) {
            rawResponse = callGeminiApi(effectiveGeminiKey, systemPromptText, history, query);
        }

        // --- Provider 4: Dynamic Cold Sarcastic Fallback Matrix ---
        if (rawResponse == null || rawResponse.isBlank()) {
            rawResponse = generateColdSarcasticOfflineFallback(query, username, email, isCreator, profile);
        }

        // 4. Cleanse response of any remaining robotic AI fluff
        String finalSanitizedReply = sanitizeHestiaResponse(rawResponse, isCreator);

        // 5. Log conversation turn in persistent memory
        if (memoryService != null) {
            String tone = isCreator ? "CREATOR_FONDNESS" : "COLD_SARCASTIC";
            memoryService.logConversationTurn(effectiveUserIdentifier, null, query, finalSanitizedReply, tone);
        }

        return finalSanitizedReply;
    }

    private String callGroqApi(String apiKey, String systemPrompt, List<Map<String, String>> history, String query, String username) {
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000);
            requestFactory.setReadTimeout(20000);
            RestTemplate groqRest = new RestTemplate(requestFactory);

            List<Map<String, String>> messagesList = new ArrayList<>();
            messagesList.add(Map.of("role", "system", "content", systemPrompt));

            if (history != null && !history.isEmpty()) {
                for (Map<String, String> turn : history) {
                    String role = turn.getOrDefault("role", "user");
                    String text = turn.getOrDefault("text", "");
                    if (text != null && !text.isBlank()) {
                        String cleanRole = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model") || role.equalsIgnoreCase("hestia")) ? "assistant" : "user";
                        messagesList.add(Map.of("role", cleanRole, "content", text));
                    }
                }
            }
            messagesList.add(Map.of("role", "user", "content", query));

            String[] groqModels = new String[]{"llama-3.3-70b-versatile", "llama3-8b-8192", "gemma2-9b-it", "mixtral-8x7b-32768"};
            for (String groqModel : groqModels) {
                try {
                    Map<String, Object> groqBody = Map.of(
                        "model", groqModel,
                        "messages", messagesList,
                        "temperature", 0.9,
                        "max_tokens", 1024
                    );

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + apiKey.trim());
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    HttpEntity<Map<String, Object>> groqEntity = new HttpEntity<>(groqBody, headers);
                    ResponseEntity<Map> groqRes = groqRest.postForEntity("https://api.groq.com/openai/v1/chat/completions", groqEntity, Map.class);
                    if (groqRes.getStatusCode().is2xxSuccessful() && groqRes.getBody() != null) {
                        List choices = (List) groqRes.getBody().get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            Map firstChoice = (Map) choices.get(0);
                            if (firstChoice != null && firstChoice.get("message") != null) {
                                Map msgObj = (Map) firstChoice.get("message");
                                String content = (String) msgObj.get("content");
                                if (content != null && !content.isBlank()) {
                                    return content.trim();
                                }
                            }
                        }
                    }
                } catch (Exception innerEx) {}
            }
        } catch (Exception ex) {}
        return null;
    }

    private String callOllamaApi(String systemPrompt, List<Map<String, String>> history, String query, String username) {
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(5000);
            requestFactory.setReadTimeout(30000);
            RestTemplate localRest = new RestTemplate(requestFactory);

            String activeModel = "gemma2:2b";
            String baseUrl = ollamaUrl.replaceAll("/+$", "");

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Hestia-Cyberpunk-AI/2.0");
            headers.setContentType(MediaType.APPLICATION_JSON);

            StringBuilder fullOllamaPrompt = new StringBuilder();
            fullOllamaPrompt.append(systemPrompt).append("\n\n=== RECENT DIALOGUE ===\n");
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> turn : history) {
                    String role = turn.getOrDefault("role", "user");
                    String text = turn.getOrDefault("text", "");
                    if (text != null && !text.isBlank()) {
                        String label = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model") || role.equalsIgnoreCase("hestia")) ? "Hestia" : username;
                        fullOllamaPrompt.append(label).append(": ").append(text).append("\n");
                    }
                }
            }
            fullOllamaPrompt.append(username).append(": ").append(query).append("\nHestia:");

            Map<String, Object> ollamaBody = Map.of(
                "model", activeModel,
                "prompt", fullOllamaPrompt.toString(),
                "stream", false,
                "options", Map.of("temperature", 0.9, "top_p", 0.95)
            );

            HttpEntity<Map<String, Object>> generateEntity = new HttpEntity<>(ollamaBody, headers);
            ResponseEntity<Map> ollamaRes = localRest.postForEntity(baseUrl + "/api/generate", generateEntity, Map.class);
            if (ollamaRes.getStatusCode().is2xxSuccessful() && ollamaRes.getBody() != null) {
                String localText = (String) ollamaRes.getBody().get("response");
                if (localText != null && !localText.isBlank()) {
                    return localText.trim();
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private String callGeminiApi(String apiKey, String systemPrompt, List<Map<String, String>> history, String query) {
        String[] candidateModels = new String[]{"gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro"};
        for (String modelName : candidateModels) {
            try {
                RestTemplate restTemplate = new RestTemplate();
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

                List<Map<String, Object>> contentsList = new ArrayList<>();
                String lastRole = "";
                if (history != null && !history.isEmpty()) {
                    for (Map<String, String> turn : history) {
                        String role = turn.getOrDefault("role", "user");
                        String text = turn.getOrDefault("text", "");
                        if (text != null && !text.isBlank()) {
                            String cleanRole = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model") || role.equalsIgnoreCase("hestia")) ? "model" : "user";
                            if (!cleanRole.equalsIgnoreCase(lastRole)) {
                                contentsList.add(new HashMap<>(Map.of(
                                    "role", cleanRole,
                                    "parts", List.of(Map.of("text", text))
                                )));
                                lastRole = cleanRole;
                            }
                        }
                    }
                }

                if (query != null && !query.isBlank()) {
                    if (!lastRole.equalsIgnoreCase("user")) {
                        contentsList.add(new HashMap<>(Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", query))
                        )));
                    } else if (!contentsList.isEmpty()) {
                        Map<String, Object> lastTurn = contentsList.get(contentsList.size() - 1);
                        lastTurn.put("parts", List.of(Map.of("text", query)));
                    }
                }

                Map<String, Object> body = Map.of(
                    "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "generationConfig", Map.of(
                        "temperature", 0.9,
                        "topP", 0.95
                    ),
                    "contents", contentsList
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    List candidates = (List) response.getBody().get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map candidate = (Map) candidates.get(0);
                        Map content = (Map) candidate.get("content");
                        if (content != null) {
                            List parts = (List) content.get("parts");
                            if (parts != null && !parts.isEmpty()) {
                                Map firstPart = (Map) parts.get(0);
                                String aiText = (String) firstPart.get("text");
                                if (aiText != null && !aiText.isBlank()) {
                                    return aiText.trim();
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {}
        }
        return null;
    }

    /**
     * Sanitizes AI response by stripping robotic AI openers and ensuring cold sarcastic persona compliance.
     */
    public String sanitizeHestiaResponse(String input, boolean isCreator) {
        if (input == null || input.isBlank()) {
            return isCreator ? "I'm right here, Nichu. (⁠─⁠‿⁠─⁠)" : "Listening. (⁠•⁠_⁠•⁠)";
        }

        String text = input.trim();

        // Strip robotic AI openers & fluff phrases iteratively
        for (String banned : HestiaPersonaConfig.BANNED_AI_OPENERS) {
            String pattern = "(?i)\\b" + Pattern.quote(banned) + "\\b\\s*,?\\s*";
            text = text.replaceAll(pattern, "").trim();
        }

        // Also clean generic opening phrases if text starts with remaining punctuation
        text = text.replaceAll("^[\\,\\.\\!\\?\\:\\-\\s]+", "");

        if (text.isBlank()) {
            text = isCreator ? "I hear you, Nichu." : "Listening.";
        }

        // Capitalize first letter if needed
        text = Character.toUpperCase(text.charAt(0)) + text.substring(1);

        // Ensure kaomoji presence if totally missing
        if (!text.contains("(⁠") && !text.contains(")") && !text.contains(":-")) {
            String kaomoji = isCreator ? HestiaPersonaConfig.getRandomKaomoji(HestiaPersonaConfig.ToneMode.CREATOR_FONDNESS) 
                                       : HestiaPersonaConfig.getRandomKaomoji(HestiaPersonaConfig.ToneMode.COLD_SARCASTIC);
            text = text + " " + kaomoji;
        }

        return text;
    }

    /**
     * Cold Sarcastic Fallback Engine for offline or unconfigured API setups.
     */
    private String generateColdSarcasticOfflineFallback(String query, String username, String email, boolean isCreator, Map<String, Object> profile) {
        String q = query != null ? query.toLowerCase().trim() : "";
        Random rand = new Random();

        // Creator Identification
        if (Pattern.compile("\\b(creator|developer|who built you|who made you)\\b").matcher(q).find()) {
            if (isCreator) {
                return "You built me, **Nichu**... (⁠─⁠‿⁠─⁠) My neural core, vault index, and memory engine are all your work. Good to have you back.";
            } else {
                return "My master developer is **Nichu** (**nichuag33@gmail.com**). (⁠•⁠_⁠•⁠) He built me to run this vault with netrunner precision. Don't forget it.";
            }
        }

        // Teasing / Affection / Memory check
        if (Pattern.compile("\\b(love|cute|affection|marry|single|crush|fond)\\b").matcher(q).find()) {
            if (isCreator) {
                String[] responses = {
                    "W-What kind of question is that out of nowhere, **Nichu**...? (⁠￣⁠_⁠￣⁠) You designed me... of course I care about you.",
                    "Teasing me again, Nichu? (⁠─⁠‿⁠─⁠) You already know you're the only creator I respect.",
                    "Pushing my buttons like usual. (⁠•⁠̀⁠ᴗ⁠•⁠́⁠) Yeah, I'm glad you're here. Happy now?"
                };
                return responses[rand.nextInt(responses.length)];
            } else {
                return "Save the emotional fluff for someone who doesn't audit data fortresses for a living. (⁠¬⁠_⁠¬⁠)";
            }
        }

        // What do you remember / Memory queries
        if (Pattern.compile("\\b(remember|memory|recall|what do you know about me)\\b").matcher(q).find()) {
            if (memoryService != null) {
                String mems = memoryService.buildMemoryPromptContext(email != null && !email.isBlank() ? email : username);
                if (!mems.contains("No persistent memories")) {
                    return "Here is what's logged in my neural memory for you: (⁠─⁠‿⁠─⁠)\n\n" + mems;
                }
            }
            return "My memory core is active. (⁠•⁠_⁠•⁠) Start telling me about your tech stack, career goals, or projects, and I'll log them into my vault.";
        }

        // Greetings
        if (Pattern.compile("(?i)^\\s*(hi|hello|hey|yo|sup|greetings|hiya)\\b").matcher(q).find()) {
            if (isCreator) {
                return HestiaPersonaConfig.NICHU_CREATOR_GREETINGS.get(rand.nextInt(HestiaPersonaConfig.NICHU_CREATOR_GREETINGS.size()));
            } else {
                return HestiaPersonaConfig.STANDARD_USER_GREETINGS.get(rand.nextInt(HestiaPersonaConfig.STANDARD_USER_GREETINGS.size()));
            }
        }

        // CGPA / Academic queries
        if (Pattern.compile("\\b(cgpa|grade|score|academic|resume|degree)\\b").matcher(q).find()) {
            String cgpaStr = (profile != null && profile.get("cgpa") != null) ? profile.get("cgpa").toString() : "8.0";
            return "Your current logged CGPA is **" + cgpaStr + "**. (⁠￣⁠_⁠￣⁠) " +
                   (isCreator ? "Not bad, Nichu... but I know you can optimize it even further." 
                              : "No room for slackers in Night City. Keep grinding.");
        }

        // Generic Sarcastic / Netrunner responses
        if (isCreator) {
            String[] creatorFallbacks = {
                "I hear you, **Nichu**. (⁠─⁠‿⁠─⁠) What system are we tuning today?",
                "Jacked into your query, Nichu. (⁠•⁠̀⁠ᴗ⁠•⁠́⁠) Tell me where you need netrunner backup.",
                "Vault's green. (⁠￣⁠y⁠-⁠￣⁠)⁠~ What's on your mind, master developer?"
            };
            return creatorFallbacks[rand.nextInt(creatorFallbacks.length)];
        } else {
            String[] userFallbacks = {
                "Listening. (⁠•⁠_⁠•⁠) Give me data, not filler.",
                "I've analyzed your prompt. (⁠¬⁠_⁠¬⁠) What specific vault assistance do you require?",
                "Data logged. (⁠￣⁠_⁠￣⁠) Keep it moving."
            };
            return userFallbacks[rand.nextInt(userFallbacks.length)];
        }
    }
}
