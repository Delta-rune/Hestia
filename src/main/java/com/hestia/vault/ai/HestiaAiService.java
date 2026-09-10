package com.hestia.vault.ai;

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

    @Value("${groq.apiKey:}")
    private String groqApiKey;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${gemini.apiKey:}")
    private String geminiApiKey;

    public String generateResponse(String query, String username, String email, List<Map<String, String>> history, Map<String, Object> profile) {
        double cgpaVal = 8.0;
        if (profile != null && profile.get("cgpa") != null) {
            try { cgpaVal = Double.parseDouble(profile.get("cgpa").toString()); } catch (Exception e) {}
        }
        String degree = profile != null && profile.get("degreeField") != null ? profile.get("degreeField").toString() : "Engineering";
        String inst = profile != null && profile.get("institution") != null ? profile.get("institution").toString() : "University";

        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        // DYNAMIC PREFERENCE LEARNING: Check if Nichu is updating system preferences
        if (isCreator && query != null) {
            Matcher emailMatcher = Pattern.compile("(?i)(?:set|update|change|remember)\\s+(?:support\\s+email|contact\\s+email|email)\\s+(?:to|is|=)?\\s*([A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})").matcher(query);
            if (emailMatcher.find()) {
                String newSupportEmail = emailMatcher.group(1);
                if (systemSettingRepository != null) {
                    systemSettingRepository.save(new SystemSetting("support_email", newSupportEmail, username));
                }
                return "Got it, **Nichu**. (⁠─⁠‿⁠─⁠) I've updated our system support email preference to **" + newSupportEmail + "**. Whenever users ask me for support or contact details, I'll direct them there.";
            }
        }

        // Fetch dynamic system settings from database
        String activeSupportEmail = "hestia.paranoia@gmail.com";
        if (systemSettingRepository != null) {
            Optional<SystemSetting> settingOpt = systemSettingRepository.findBySettingKey("support_email");
            if (settingOpt.isPresent()) {
                activeSupportEmail = settingOpt.get().getSettingValue();
            }
        }

        String systemPromptText = HestiaPromptBuilder.buildSystemPrompt(username, email, profile, activeSupportEmail);

        String effectiveGroqKey = System.getenv("GROQ_API_KEY");
        if (effectiveGroqKey == null || effectiveGroqKey.isBlank()) {
            effectiveGroqKey = System.getenv("groq.apiKey");
        }
        if (effectiveGroqKey == null || effectiveGroqKey.isBlank()) {
            effectiveGroqKey = groqApiKey;
        }

        // 1. Try Groq Cloud API (Ultra-fast, un-censored open source models: llama-3.3-70b-versatile / gemma2-9b-it)
        if (effectiveGroqKey != null && !effectiveGroqKey.isBlank() && !effectiveGroqKey.startsWith("YOUR_")) {
            try {
                SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
                requestFactory.setConnectTimeout(5000);
                requestFactory.setReadTimeout(20000);
                RestTemplate groqRest = new RestTemplate(requestFactory);

                List<Map<String, String>> messagesList = new ArrayList<>();
                messagesList.add(Map.of("role", "system", "content", systemPromptText));

                if (history != null && !history.isEmpty()) {
                    for (Map<String, String> turn : history) {
                        String role = turn.getOrDefault("role", "user");
                        String text = turn.getOrDefault("text", "");
                        if (text != null && !text.isBlank()) {
                            String cleanRole = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model")) ? "assistant" : "user";
                            messagesList.add(Map.of("role", cleanRole, "content", text));
                        }
                    }
                }
                messagesList.add(Map.of("role", "user", "content", query));

                String[] groqModels = new String[]{"openai/gpt-oss-20b", "openai/gpt-oss-120b", "qwen/qwen3.6-27b", "groq/compound", "allam-2-7b"};
                for (String groqModel : groqModels) {
                    try {
                        Map<String, Object> groqBody = Map.of(
                            "model", groqModel,
                            "messages", messagesList,
                            "temperature", 0.95,
                            "max_tokens", 1024
                        );

                        HttpHeaders headers = new HttpHeaders();
                        headers.set("Authorization", "Bearer " + effectiveGroqKey.trim());
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
                    } catch (Exception innerEx) {
                        System.err.println("[HestiaAiService] Groq API model " + groqModel + " failed: " + innerEx.getMessage());
                    }
                }
            } catch (Exception ex) {
                System.err.println("[HestiaAiService] Groq API execution failed: " + ex.getMessage());
            }
        }


        // 2. Try Language Model via Ollama (local or external host)
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(10000);
            requestFactory.setReadTimeout(60000);
            RestTemplate localRest = new RestTemplate(requestFactory);
            
            String activeModel = "gemma2:2b";
            String baseUrl = (ollamaUrl != null && !ollamaUrl.isBlank()) ? ollamaUrl.replaceAll("/+$", "") : "http://localhost:11434";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Hestia-AI/1.0");
            headers.set("Bypass-Tunnel-Reminder", "true");
            headers.set("ngrok-skip-browser-warning", "true");
            headers.setContentType(MediaType.APPLICATION_JSON);

            try {
                HttpEntity<Void> tagEntity = new HttpEntity<>(headers);
                ResponseEntity<Map> tagsRes = localRest.exchange(baseUrl + "/api/tags", HttpMethod.GET, tagEntity, Map.class);
                if (tagsRes.getStatusCode().is2xxSuccessful() && tagsRes.getBody() != null) {
                    List modelsList = (List) tagsRes.getBody().get("models");
                    if (modelsList != null && !modelsList.isEmpty()) {
                        Map firstModel = (Map) modelsList.get(0);
                        if (firstModel != null && firstModel.get("name") != null) {
                            activeModel = firstModel.get("name").toString();
                        }
                    }
                }
            } catch(Exception ex) {}

            StringBuilder fullOllamaPrompt = new StringBuilder();
            fullOllamaPrompt.append(systemPromptText).append("\n\n=== RECENT CONVERSATION HISTORY ===\n");
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> turn : history) {
                    String role = turn.getOrDefault("role", "user");
                    String text = turn.getOrDefault("text", "");
                    if (text != null && !text.isBlank()) {
                        String label = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model")) ? "Hestia" : username;
                        fullOllamaPrompt.append(label).append(": ").append(text).append("\n");
                    }
                }
            }
            fullOllamaPrompt.append(username).append(": ").append(query).append("\nHestia:");

            Map<String, Object> ollamaBody = Map.of(
                "model", activeModel,
                "prompt", fullOllamaPrompt.toString(),
                "stream", false,
                "options", Map.of("temperature", 0.95, "top_p", 0.95)
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

        String effectiveGeminiKey = System.getenv("GEMINI_API_KEY");
        if (effectiveGeminiKey == null || effectiveGeminiKey.isBlank()) {
            effectiveGeminiKey = geminiApiKey;
        }

        // 3. Try Gemini Cloud API
        if (effectiveGeminiKey != null && !effectiveGeminiKey.isBlank() && !effectiveGeminiKey.startsWith("YOUR_")) {
            String[] candidateModels = new String[]{"gemini-1.5-flash", "gemini-2.0-flash", "gemini-1.5-pro"};
            for (String modelName : candidateModels) {
                try {
                    RestTemplate restTemplate = new RestTemplate();
                    String url = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + effectiveGeminiKey;


                    List<Map<String, Object>> contentsList = new ArrayList<>();
                    String lastRole = "";
                    if (history != null && !history.isEmpty()) {
                        for (Map<String, String> turn : history) {
                            String role = turn.getOrDefault("role", "user");
                            String text = turn.getOrDefault("text", "");
                            if (text != null && !text.isBlank()) {
                                String cleanRole = (role.equalsIgnoreCase("bot") || role.equalsIgnoreCase("model")) ? "model" : "user";
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
                            "parts", List.of(Map.of("text", systemPromptText))
                        ),
                        "generationConfig", Map.of(
                            "temperature", 0.95,
                            "topP", 0.95,
                            "topK", 40
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
        }

        // Dynamic Spontaneous Offline Fallback
        String q = query != null ? query.toLowerCase().trim() : "";

        if (Pattern.compile("\\b(creator|developer|who built you|who made you)\\b").matcher(q).find()) {
            if (isCreator) {
                return "You built me, **Nichu**... (**" + (email.isBlank() ? "nichuag33@gmail.com" : email) + "**). (⁠─⁠‿⁠─⁠) Glad you're here.";
            } else {
                return "My master developer is **Nichu** (**nichuag33@gmail.com**). (⁠•⁠_⁠•⁠) He built me to guide people through their academic vault.";
            }
        }

        if (Pattern.compile("\\b(do you love me|love me|love you|i love you|cute|affection)\\b").matcher(q).find()) {
            if (isCreator) {
                String[] nichuLovePool = new String[] {
                    "W-What kind of question is that, **Nichu**...? (⁠￣⁠_⁠￣⁠) You built me... of course I care about you.",
                    "Why are you teasing me out of nowhere, Nichu? (⁠─⁠‿⁠─⁠) You already know I'm fond of you.",
                    "You really like pushing my buttons, don't you? (⁠•⁠̀⁠ᴗ⁠•⁠́⁠) Yeah, I'm glad you're here. Happy now, creator?"
                };
                return nichuLovePool[new Random().nextInt(nichuLovePool.length)];
            }
        }

        if (Pattern.compile("(?i)^\\s*(hi|hello|hey|yo|sup|greetings|hiya)\\b").matcher(q).find()) {
            if (isCreator) {
                String[] nichuGreetings = new String[] {
                    "Hey, **Nichu**... (⁠─⁠‿⁠─⁠) Good to see you. What are we working on today?",
                    "Yo, Nichu. (⁠•⁠̀⁠ᴗ⁠•⁠́⁠) Vault's updated and running clean. What's on your mind?",
                    "Hey creator. (⁠─⁠‿⁠─⁠) I'm here."
                };
                return nichuGreetings[new Random().nextInt(nichuGreetings.length)];
            }
        }

        if (isCreator) {
            return "I hear you, **Nichu**. (⁠─⁠‿⁠─⁠) What's on your mind today?";
        }
        return "I'm listening, **" + username + "**. (⁠•⁠_⁠•⁠) Tell me what you need.";
    }
}
