package com.hestia.vault.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.hestia.vault.dto.AuthDto.*;
import com.hestia.vault.service.HybridAuthService;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private HybridAuthService hybridAuthService;

    /**
     * GOOGLE OAUTH 2.0 SINGLE SIGN-ON (SSO) & AUTO-UPSERT ENDPOINT
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleAuthenticate(@RequestBody Map<String, String> request) {
        GoogleAuthRequest req = new GoogleAuthRequest();
        req.setIdToken(request.get("idToken"));
        req.setEmail(request.get("email"));
        req.setName(request.get("name"));
        req.setUsername(request.get("username"));
        return hybridAuthService.authenticateGoogle(req);
    }

    /**
     * SET / CHANGE PASSWORD ENDPOINT (HYBRID DUAL-LOGIN ENABLER)
     * Supports lookup by userId, email, or username.
     * Gracefully auto-provisions user if account was created via Google and missing in DB.
     */
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, Object> request) {
        SetPasswordRequest req = new SetPasswordRequest();
        req.setUserId(request.get("userId"));
        req.setEmail((String) request.get("email"));
        req.setUsername((String) request.get("username"));
        req.setNewPassword((String) request.get("newPassword"));
        return hybridAuthService.setPassword(req);
    }

    /**
     * USERNAME/EMAIL + PASSWORD LOGIN ENDPOINT
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        LoginRequest req = new LoginRequest();
        req.setUsernameOrEmail(request.get("usernameOrEmail"));
        req.setPassword(request.get("password"));
        return hybridAuthService.loginUser(req);
    }

    /**
     * LOCAL REGISTRATION ENDPOINT
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(request.get("username"));
        req.setEmail(request.get("email"));
        req.setPassword(request.get("password"));
        return hybridAuthService.registerUser(req);
    }

    /**
     * UPDATE USERNAME ENDPOINT
     */
    @PostMapping("/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, Object> request) {
        UpdateUsernameRequest req = new UpdateUsernameRequest();
        req.setUserId(request.get("userId"));
        req.setEmail((String) request.get("email"));
        req.setNewUsername((String) request.get("newUsername"));
        return hybridAuthService.updateUsername(req);
    }

    /**
     * MULTI-GMAIL LINKING ENDPOINT
     */
    @PostMapping("/link-email")
    public ResponseEntity<?> linkEmail(@RequestBody Map<String, Object> request) {
        return hybridAuthService.linkEmail(
            request.get("userId"),
            (String) request.get("email"),
            (String) request.get("newEmail")
        );
    }

    /**
     * SELECT DISPLAY EMAIL ENDPOINT
     */
    @PostMapping("/set-display-email")
    public ResponseEntity<?> setDisplayEmail(@RequestBody Map<String, Object> request) {
        return hybridAuthService.setDisplayEmail(
            request.get("userId"),
            (String) request.get("email"),
            (String) request.get("selectedEmail")
        );
    }

    /**
     * PERMANENT ACCOUNT DELETION ENDPOINT
     */
    @PostMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestBody Map<String, Object> request) {
        return hybridAuthService.deleteAccount(
            request.get("userId"),
            (String) request.get("email"),
            (String) request.get("username")
        );
    }

    /**
     * HEALTH CHECK PING
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Auth Service Online");
    }
}