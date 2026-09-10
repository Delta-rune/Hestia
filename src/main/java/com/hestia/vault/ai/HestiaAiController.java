package com.hestia.vault.ai;

import com.hestia.vault.model.HestiaMemoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HestiaAiController {

    @Autowired
    private HestiaAiService hestiaAiService;

    @Autowired(required = false)
    private HestiaMemoryService hestiaMemoryService;

    @PostMapping("/chat")
    public ResponseEntity<?> chatWithHestia(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        String username = (String) request.getOrDefault("username", "Friend");
        String email = (String) request.getOrDefault("email", "");
        List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");
        Map<String, Object> profile = (Map<String, Object>) request.get("profile");

        if (query == null) query = "";

        String reply = hestiaAiService.generateResponse(query, username, email, history, profile);

        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        int currentHour = java.time.LocalDateTime.now().getHour();
        HestiaPersonaConfig.MoodState mood = HestiaPersonaConfig.inferMoodFromQuery(query, isCreator, currentHour);

        Map<String, Object> responseMap = new LinkedHashMap<>();
        responseMap.put("status", "SUCCESS");
        responseMap.put("reply", reply);
        responseMap.put("mood", mood.name());
        responseMap.put("moodLabel", mood.getLabel());
        responseMap.put("moodDesc", mood.getDescription());
        responseMap.put("audioPitch", mood.getDefaultPitch());
        responseMap.put("audioRate", mood.getDefaultRate());
        responseMap.put("suggestedActions", List.of("Audit my academic vault", "Roast my student profile", "Tell me an engineering joke"));
        responseMap.put("typingSpeedMs", 22);
        responseMap.put("tone", isCreator ? "CREATOR_FONDNESS" : mood.name());
        responseMap.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(responseMap);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChatWithHestia(@RequestBody Map<String, Object> request) {
        SseEmitter emitter = new SseEmitter(60000L);
        CompletableFuture.runAsync(() -> {
            try {
                String query = (String) request.get("query");
                String username = (String) request.getOrDefault("username", "Friend");
                String email = (String) request.getOrDefault("email", "");
                List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");
                Map<String, Object> profile = (Map<String, Object>) request.get("profile");

                if (query == null) query = "";

                Map<String, Object> enriched = hestiaAiService.generateEnrichedResponse(query, username, email, history, profile);
                String reply = (String) enriched.getOrDefault("reply", "");

                // Send metadata event first
                emitter.send(SseEmitter.event().name("meta").data(Map.of(
                    "mood", enriched.get("mood"),
                    "moodLabel", enriched.get("moodLabel"),
                    "suggestedActions", enriched.get("suggestedActions"),
                    "typingSpeedMs", enriched.get("typingSpeedMs")
                )));

                // Stream word by word with human typing delay
                String[] words = reply.split("(?<=\\s)|(?=[\\n])");
                for (String word : words) {
                    emitter.send(SseEmitter.event().name("delta").data(word));
                    Thread.sleep(18);
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }

    @GetMapping("/hestia/memories")
    public ResponseEntity<?> getUserMemories(@RequestParam(value = "userIdentifier", required = false) String userIdentifier,
                                           @RequestParam(value = "email", required = false) String email) {
        String id = (email != null && !email.isBlank()) ? email : (userIdentifier != null ? userIdentifier : "Friend");
        List<HestiaMemoryEntity> memories = (hestiaMemoryService != null) ? hestiaMemoryService.getUserMemories(id) : List.of();
        
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "userIdentifier", id,
            "memoryCount", memories.size(),
            "memories", memories
        ));
    }

    @PostMapping("/hestia/memories/fact")
    public ResponseEntity<?> addMemoryFact(@RequestBody Map<String, String> request) {
        String userIdentifier = request.getOrDefault("userIdentifier", request.getOrDefault("email", "Friend"));
        String key = request.get("key");
        String value = request.get("value");
        String category = request.getOrDefault("category", "PREFERENCE");

        if (key == null || value == null || hestiaMemoryService == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "Missing key or value"));
        }

        HestiaMemoryEntity saved = hestiaMemoryService.saveOrUpdateMemory(userIdentifier, key, value, category);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "memory", saved));
    }

    @DeleteMapping("/hestia/memories")
    public ResponseEntity<?> clearMemories(@RequestParam("userIdentifier") String userIdentifier) {
        boolean success = (hestiaMemoryService != null) && hestiaMemoryService.clearUserMemoryAndHistory(userIdentifier);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "cleared", success));
    }

    @GetMapping("/hestia/status")
    public ResponseEntity<?> getHestiaStatus(@RequestParam(value = "email", required = false) String email,
                                             @RequestParam(value = "username", required = false) String username) {
        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        String id = (email != null && !email.isBlank()) ? email : (username != null ? username : "Friend");
        int memoryCount = (hestiaMemoryService != null) ? hestiaMemoryService.getUserMemories(id).size() : 0;

        return ResponseEntity.ok(Map.of(
            "name", "Hestia 2.0 Cognitive AI",
            "persona", "Hestia",
            "tone", isCreator ? "Master Creator Bond (Nichu)" : "Authentic Human Mentor & Companion",
            "isCreator", isCreator,
            "memoryEngineActive", (hestiaMemoryService != null),
            "storedMemoriesCount", memoryCount,
            "version", "2.0.0"
        ));
    }

    @GetMapping("/hestia/history")
    public ResponseEntity<?> getConversationHistory(@RequestParam(value = "userIdentifier", required = false) String userIdentifier,
                                                    @RequestParam(value = "email", required = false) String email,
                                                    @RequestParam(value = "limit", defaultValue = "30") int limit) {
        String id = (email != null && !email.isBlank()) ? email : (userIdentifier != null ? userIdentifier : "Friend");
        var history = (hestiaMemoryService != null) ? hestiaMemoryService.getRecentConversationHistory(id, limit) : List.of();
        List<Object> reversed = new ArrayList<>(history);
        Collections.reverse(reversed);
        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "userIdentifier", id,
            "count", history.size(),
            "history", reversed
        ));
    }
}
