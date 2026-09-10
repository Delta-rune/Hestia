package com.hestia.vault.ai;

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

    @PostMapping("/chat")
    public ResponseEntity<?> chatWithHestia(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        String username = (String) request.getOrDefault("username", "Friend");
        String email = (String) request.getOrDefault("email", "");
        List<Map<String, String>> history = (List<Map<String, String>>) request.get("history");
        Map<String, Object> profile = (Map<String, Object>) request.get("profile");

        if (query == null) query = "";

        String responseMessage = hestiaAiService.generateResponse(query, username, email, history, profile);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "reply", responseMessage,
            "timestamp", System.currentTimeMillis()
        ));
    }
}
