package com.hestia.vault.ai;

import com.hestia.vault.model.HestiaMemoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

        String responseMessage = hestiaAiService.generateResponse(query, username, email, history, profile);

        boolean isCreator = (email != null && email.equalsIgnoreCase("nichuag33@gmail.com")) || 
                            (email != null && email.equalsIgnoreCase("nichuag35@gmail.com")) || 
                            (username != null && username.equalsIgnoreCase("nichuag33")) ||
                            (username != null && username.equalsIgnoreCase("Nichu"));

        String toneMode = isCreator ? "CREATOR_FONDNESS" : "COLD_SARCASTIC";

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "reply", responseMessage,
            "tone", toneMode,
            "timestamp", System.currentTimeMillis()
        ));
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
            "name", "Hestia Cyberpunk AI",
            "persona", "Lucy (Cyberpunk Edgerunners)",
            "tone", isCreator ? "Deep Organic Fondness (Creator Nichu)" : "Cold Sarcastic Netrunner Mentor",
            "isCreator", isCreator,
            "memoryEngineActive", (hestiaMemoryService != null),
            "storedMemoriesCount", memoryCount,
            "sarcasmIndex", 9.5
        ));
    }
}
