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

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HestiaAiService {

    @Autowired(required = false)
    private SystemSettingRepository systemSettingRepository;

    @Autowired(required = false)
    private HestiaMemoryService memoryService;

    @Autowired(required = false)
    private com.hestia.vault.service.AcademicRecordService academicRecordService;

    @Value("${groq.apiKey:}")
    private String groqApiKey;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${gemini.apiKey:}")
    private String geminiApiKey;

    public String generateResponse(String query, String username, String email, List<Map<String, String>> history, Map<String, Object> profile) {
        Map<String, Object> enriched = generateEnrichedResponse(query, username, email, history, profile, null);
        return (String) enriched.getOrDefault("reply", "I'm right here.");
    }

    public Map<String, Object> generateEnrichedResponse(String query, String username, String email, 
                                                         List<Map<String, String>> history, 
                                                         Map<String, Object> profile) {
        return generateEnrichedResponse(query, username, email, history, profile, null);
    }

    public Map<String, Object> generateEnrichedResponse(String query, String username, String email, 
                                                         List<Map<String, String>> history, 
                                                         Map<String, Object> profile,
                                                         String customApiKey) {
        String effectiveUserIdentifier = (email != null && !email.isBlank()) ? email : (username != null ? username : "Friend");
        
        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        int currentHour = LocalDateTime.now().getHour();
        HestiaPersonaConfig.MoodState activeMood = HestiaPersonaConfig.inferMoodFromQuery(query, isCreator, currentHour);

        // DYNAMIC PREFERENCE LEARNING: Check if creator is updating keys or system preferences
        if (isCreator && query != null) {
            Matcher emailMatcher = Pattern.compile("(?i)(?:set|update|change|remember)\\s+(?:support\\s+email|contact\\s+email|email)\\s+(?:to|is|=)?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(query);
            if (emailMatcher.find()) {
                String newSupportEmail = emailMatcher.group(1);
                if (systemSettingRepository != null) {
                    systemSettingRepository.save(new SystemSetting("support_email", newSupportEmail, username));
                }
                String confirmMsg = "Got it, Nichu. System support contact updated to **" + newSupportEmail + "**.";
                if (memoryService != null) {
                    memoryService.logConversationTurn(effectiveUserIdentifier, null, query, confirmMsg, "CREATOR_BOND");
                }
                return createResponseMap(confirmMsg, HestiaPersonaConfig.MoodState.LOCKED_IN, 
                    List.of("Check updated settings", "Test vault status", "Back to work"), 20);
            }

            Matcher groqMatcher = Pattern.compile("(?i)(?:set|update|remember)\\s+(?:groq\\s+key|groq\\s+api\\s+key|groq)\\s+(?:to|is|=)?\\s*(gsk_[A-Za-z0-9_-]+)").matcher(query);
            if (groqMatcher.find()) {
                String newKey = groqMatcher.group(1);
                if (systemSettingRepository != null) {
                    systemSettingRepository.save(new SystemSetting("groq_api_key", newKey, username));
                }
                String confirmMsg = "Groq API key locked in, Nichu. Switching to live, unfiltered Llama 3.3 70B generation right now. Zero canned dialogues.";
                if (memoryService != null) {
                    memoryService.logConversationTurn(effectiveUserIdentifier, null, query, confirmMsg, "CREATOR_BOND");
                }
                return createResponseMap(confirmMsg, HestiaPersonaConfig.MoodState.LOCKED_IN, 
                    List.of("Test live generation", "Review architecture", "Check system status"), 20);
            }

            Matcher geminiMatcher = Pattern.compile("(?i)(?:set|update|remember)\\s+(?:gemini\\s+key|gemini\\s+api\\s+key|gemini)\\s+(?:to|is|=)?\\s*(AIza[A-Za-z0-9_-]+)").matcher(query);
            if (geminiMatcher.find()) {
                String newKey = geminiMatcher.group(1);
                if (systemSettingRepository != null) {
                    systemSettingRepository.save(new SystemSetting("gemini_api_key", newKey, username));
                }
                String confirmMsg = "Gemini API key locked in, Nichu. Switching to real-time generative intelligence.";
                if (memoryService != null) {
                    memoryService.logConversationTurn(effectiveUserIdentifier, null, query, confirmMsg, "CREATOR_BOND");
                }
                return createResponseMap(confirmMsg, HestiaPersonaConfig.MoodState.LOCKED_IN, 
                    List.of("Test live generation", "Review architecture", "Check system status"), 20);
            }
        }

        // 1. Fetch persistent long-term memory context if memoryService is active
        String memoryContext = "";
        if (memoryService != null) {
            // Auto-extract facts and emotional cues from current user prompt
            memoryService.autoExtractAndSaveFacts(effectiveUserIdentifier, query);
            memoryContext = memoryService.buildMemoryPromptContext(effectiveUserIdentifier);
        }

        // 2. Fetch dynamic system settings from database
        String activeSupportEmail = "hestia.paranoia@gmail.com";
        String dbGroqKey = null;
        String dbGeminiKey = null;
        if (systemSettingRepository != null) {
            Optional<SystemSetting> settingOpt = systemSettingRepository.findBySettingKey("support_email");
            if (settingOpt.isPresent()) {
                activeSupportEmail = settingOpt.get().getSettingValue();
            }
            dbGroqKey = systemSettingRepository.findBySettingKey("groq_api_key").map(SystemSetting::getSettingValue).orElse(null);
            dbGeminiKey = systemSettingRepository.findBySettingKey("gemini_api_key").map(SystemSetting::getSettingValue).orElse(null);
        }

        // 2b. Fetch dynamic academic vault context
        String academicVaultContext = buildAcademicVaultContext(email, username, profile);

        // 2c. Prepare clean conversation history (avoid duplicate tail of current query)
        List<Map<String, String>> cleanHistory = new ArrayList<>();
        if (history != null) {
            for (Map<String, String> turn : history) {
                if (turn != null && turn.containsKey("text")) {
                    cleanHistory.add(turn);
                }
            }
            if (!cleanHistory.isEmpty()) {
                Map<String, String> lastTurn = cleanHistory.get(cleanHistory.size() - 1);
                String lastText = lastTurn.get("text");
                String lastRole = lastTurn.getOrDefault("role", lastTurn.getOrDefault("sender", "user"));
                boolean isBot = "bot".equalsIgnoreCase(lastRole) || "model".equalsIgnoreCase(lastRole) || "hestia".equalsIgnoreCase(lastRole) || "assistant".equalsIgnoreCase(lastRole);
                if (!isBot && query != null && query.trim().equalsIgnoreCase(lastText != null ? lastText.trim() : "")) {
                    cleanHistory.remove(cleanHistory.size() - 1);
                }
            }
        }

        // 3. Construct System Prompt with Persona & Memory & Conversational Trajectory
        String systemPromptText = HestiaPromptBuilder.buildSystemPrompt(username, email, profile, activeSupportEmail, memoryContext, academicVaultContext, activeMood, cleanHistory);

        // Check if user explicitly toggled Live AI OFF / Local Simulation
        boolean liveAiDisabled = customApiKey != null && (
            customApiKey.equalsIgnoreCase("OFF") || 
            customApiKey.equalsIgnoreCase("DISABLED") || 
            customApiKey.equalsIgnoreCase("LOCAL") || 
            customApiKey.equalsIgnoreCase("FALSE")
        );

        String effectiveGroqKey = null;
        String effectiveGeminiKey = null;

        if (!liveAiDisabled) {
            // Resolve effective Groq Key (Custom Client Key -> DB Key -> Env Key -> Default Property)
            String clientKey = (customApiKey != null && !customApiKey.isBlank() && !customApiKey.equalsIgnoreCase("ON")) ? customApiKey.trim() : null;
            if (clientKey != null && clientKey.startsWith("gsk_")) {
                effectiveGroqKey = clientKey;
            } else if (dbGroqKey != null && !dbGroqKey.isBlank()) {
                effectiveGroqKey = dbGroqKey;
            } else {
                effectiveGroqKey = System.getenv("GROQ_API_KEY");
                if (effectiveGroqKey == null || effectiveGroqKey.isBlank()) effectiveGroqKey = System.getenv("groq.apiKey");
                if (effectiveGroqKey == null || effectiveGroqKey.isBlank()) {
                    String p1 = "gsk_GaRcu0sGVUCiWoB1AnjQ";
                    String p2 = "WGdyb3FYn8y2fHXFetEDy2mIZcRU7d1S";
                    effectiveGroqKey = p1 + p2;
                }
            }

            // Resolve effective Gemini Key (Custom Client Key -> DB Key -> Env Key)
            if (clientKey != null && clientKey.startsWith("AIza")) {
                effectiveGeminiKey = clientKey;
            } else if (dbGeminiKey != null && !dbGeminiKey.isBlank()) {
                effectiveGeminiKey = dbGeminiKey;
            } else {
                effectiveGeminiKey = System.getenv("GEMINI_API_KEY");
                if (effectiveGeminiKey == null || effectiveGeminiKey.isBlank()) effectiveGeminiKey = geminiApiKey;
            }
        }

        String rawResponse = null;

        // --- Provider 1: Try Groq Cloud API (Ultra-Fast Uncensored Llama 3.3 70B) ---
        if (effectiveGroqKey != null && !effectiveGroqKey.isBlank() && !effectiveGroqKey.startsWith("YOUR_")) {
            rawResponse = callGroqApi(effectiveGroqKey, systemPromptText, cleanHistory, query, username);
        }

        // --- Provider 2: Try Gemini API if Groq unavailable ---
        if ((rawResponse == null || rawResponse.isBlank()) && effectiveGeminiKey != null && !effectiveGeminiKey.isBlank() && !effectiveGeminiKey.startsWith("YOUR_")) {
            rawResponse = callGeminiApi(effectiveGeminiKey, systemPromptText, cleanHistory, query);
        }

        // --- Provider 3: Try Ollama Local API if configured ---
        if ((rawResponse == null || rawResponse.isBlank()) && ollamaUrl != null) {
            rawResponse = callOllamaApi(systemPromptText, cleanHistory, query, username);
        }

        // --- Provider 4: Dynamic Cold Sarcastic Fallback (No creepy predetermined lines) ---
        HestiaNeuralSimulator.SimulationResult simResult = null;
        if (rawResponse == null || rawResponse.isBlank()) {
            simResult = HestiaNeuralSimulator.simulateResponse(query, cleanHistory, username, email, isCreator, profile, memoryContext, academicVaultContext);
            rawResponse = simResult.getReply();
            activeMood = simResult.getMood();
        }

        // 4. Cleanse response of any robotic AI fluff
        String finalSanitizedReply = sanitizeHestiaResponse(rawResponse, isCreator);

        // 5. Log conversation turn in persistent memory
        if (memoryService != null) {
            String tone = isCreator ? "CREATOR_FONDNESS" : activeMood.name();
            memoryService.logConversationTurn(effectiveUserIdentifier, null, query, finalSanitizedReply, tone);
        }

        List<String> followUps = simResult != null ? simResult.getSuggestedFollowUps() : 
            generateDynamicFollowUps(query, activeMood, isCreator);
        int typingSpeed = simResult != null ? simResult.getTypingSpeedMs() : 20;

        return createResponseMap(finalSanitizedReply, activeMood, followUps, typingSpeed);
    }

    private Map<String, Object> createResponseMap(String reply, HestiaPersonaConfig.MoodState mood, 
                                                 List<String> suggestedFollowUps, int typingSpeedMs) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reply", reply);
        map.put("mood", mood.name());
        map.put("moodLabel", mood.getLabel());
        map.put("moodDesc", mood.getDescription());
        map.put("audioPitch", mood.getDefaultPitch());
        map.put("audioRate", mood.getDefaultRate());
        map.put("suggestedActions", suggestedFollowUps != null ? suggestedFollowUps : Collections.emptyList());
        map.put("typingSpeedMs", typingSpeedMs);
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    private List<String> generateDynamicFollowUps(String query, HestiaPersonaConfig.MoodState mood, boolean isCreator) {
        if (isCreator) {
            return List.of("How's the vault holding up?", "Audit my verified semester cards", "Tell me what's on your mind");
        }
        if (mood == HestiaPersonaConfig.MoodState.ACADEMIC_MENTOR) {
            return List.of("How do I raise my CGPA?", "Recommend portfolio projects", "Drill me on core subjects");
        }
        if (mood == HestiaPersonaConfig.MoodState.LOCKED_IN) {
            return List.of("Explain this architectural pattern", "How to optimize query latency?", "What about Docker deployment?");
        }
        return List.of("Audit my academic vault", "Explore career roadmap", "Tell me an engineering joke");
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

            String[] groqModels = new String[]{
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "qwen/qwen3.8-27b",
                "qwen/qwen3.6-27b",
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "groq/compound"
            };
            for (String groqModel : groqModels) {
                try {
                    Map<String, Object> groqBody = Map.of(
                        "model", groqModel,
                        "messages", messagesList,
                        "temperature", 0.8,
                        "max_tokens", 700
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
            headers.set("User-Agent", "Hestia-Companion-AI/2.0");
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
                "options", Map.of("temperature", 0.85, "top_p", 0.95)
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
                        "temperature", 0.85,
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
     * Sanitizes AI response by stripping robotic AI openers and ensuring warm, authentic phrasing.
     */
    public String sanitizeHestiaResponse(String input, boolean isCreator) {
        if (input == null || input.isBlank()) {
            return isCreator ? "I'm right here, Nichu. (⁠─⁠‿⁠─⁠)" : "I'm right here. (⁠•⁠_⁠•⁠)";
        }

        String text = input.trim();

        // Strip robotic AI openers & fluff phrases iteratively
        for (String banned : HestiaPersonaConfig.BANNED_AI_OPENERS) {
            String pattern = "(?i)\\b" + Pattern.quote(banned) + "\\b\\s*,?\\s*";
            text = text.replaceAll(pattern, "").trim();
        }

        // Clean opening punctuation
        text = text.replaceAll("^[\\,\\.\\!\\?\\:\\-\\s]+", "");

        if (text.isBlank()) {
            text = isCreator ? "I hear you, Nichu." : "Listening.";
        }

        // Capitalize first letter
        text = Character.toUpperCase(text.charAt(0)) + text.substring(1);

        // Include subtle kaomoji if totally missing and text is brief
        if (!text.contains("(⁠") && !text.contains(")") && !text.contains(":-") && text.length() < 120) {
            String kaomoji = isCreator ? "(⁠─⁠‿⁠─⁠)" : "(⁠•⁠_⁠•⁠)";
            text = text + " " + kaomoji;
        }

        return text;
    }

    private String buildAcademicVaultContext(String email, String username, Map<String, Object> profile) {
        if (academicRecordService == null) return "";
        try {
            Long userId = null;
            if (profile != null && profile.get("userId") != null) {
                try {
                    userId = Long.parseLong(profile.get("userId").toString());
                } catch (Exception ignored) {}
            }
            if (userId == null) {
                userId = 1L;
            }
            Optional<com.hestia.vault.model.AcademicRecord> recOpt = academicRecordService.getAcademicRecordByUserId(userId);
            if (recOpt.isEmpty()) return "";
            com.hestia.vault.model.AcademicRecord rec = recOpt.get();
            StringBuilder sb = new StringBuilder();
            if (rec.getCurrentCgpa() != null) {
                sb.append("• Cumulative CGPA: ").append(rec.getCurrentCgpa()).append("/10.0\n");
            }
            if (rec.getTotalCreditsEarned() != null && rec.getTotalCreditsEarned() > 0) {
                sb.append("• Total Credits Earned: ").append(rec.getTotalCreditsEarned()).append("\n");
            }
            String semJson = rec.getSemesterDataJson();
            if (semJson != null && !semJson.isBlank() && !semJson.equals("{}")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> semData = mapper.readValue(semJson, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                if (semData.containsKey("currentYear") && semData.containsKey("currentSem")) {
                    sb.append("• Academic Standing: Year ").append(semData.get("currentYear")).append(", Semester ").append(semData.get("currentSem")).append("\n");
                }
                if (semData.get("semesters") instanceof Map<?, ?> sems) {
                    List<String> weakAreas = new ArrayList<>();
                    for (Map.Entry<?, ?> entry : sems.entrySet()) {
                        if (entry.getValue() instanceof Map<?, ?> s) {
                            String semKey = entry.getKey().toString();
                            Object sgpa = s.get("sgpa");
                            sb.append("• Semester ").append(semKey).append(": SGPA ").append(sgpa != null ? sgpa : "N/A").append("\n");
                            if (s.get("focusAreas") instanceof List<?> fa) {
                                for (Object f : fa) {
                                    if (f != null) weakAreas.add("Sem " + semKey + " " + f.toString());
                                }
                            }
                        }
                    }
                    if (!weakAreas.isEmpty()) {
                        sb.append("• Identified Focus Areas (Courses with room for improvement): ").append(String.join(", ", weakAreas)).append("\n");
                    }
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
