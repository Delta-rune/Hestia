
package com.hestia.vault.controller;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hestia.vault.model.User;
import com.hestia.vault.repository.UserRepository;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.hestia.vault.repository.UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${google.clientId:698778630407-97c2j8jdlbp1hobctslv80f6735oakg8.apps.googleusercontent.com}")
    private String googleClientId;

    /**
     * Update Display Username Endpoint.
     */
    @PostMapping("/update-username")
    public ResponseEntity<?> updateUsername(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        String newUsername = (String) request.get("newUsername");

        if (userId == null || newUsername == null || newUsername.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Valid User ID and new username are required."));
        }

        String cleaned = newUsername.trim();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        User user = userOpt.get();
        if (!user.getUsername().equalsIgnoreCase(cleaned) && userRepository.existsByUsername(cleaned)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Username already taken. Please choose another."));
        }

        user.setUsername(cleaned);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Username updated successfully.", "user", user));
    }

    /**
     * Set / Change Password Endpoint (Supports local and Google SSO accounts).
     */
    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        String newPassword = (String) request.get("newPassword");

        if (userId == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Password must be at least 6 characters long."));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        User user = userOpt.get();
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password created/updated successfully. You can now log in using password."));
    }

    /**
     * Permanent Account Deletion & Vault Data Wipe Endpoint ("Right to be Forgotten").
     * Protected: Master Developer Account (nichuag33@gmail.com) CANNOT be deleted.
     */
    @PostMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;

        if (userId == null) {
            return ResponseEntity.badRequest().body(Map.of("detail", "User ID is required for account deletion."));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        User user = userOpt.get();
        // MASTER DEVELOPER PROTECTION CHECK
        if (user.getEmail() != null && (user.getEmail().equalsIgnoreCase("nichuag33@gmail.com") || 
                                        user.getEmail().equalsIgnoreCase("nichuag35@gmail.com") || 
                                        user.getUsername().equalsIgnoreCase("nichuag33") ||
                                        user.getUsername().equalsIgnoreCase("Nichu"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "PROTECTED",
                "detail", "Deletion Blocked: Master Developer Account (nichuag33@gmail.com) is permanently protected from deletion."
            ));
        }

        // Delete UserProfile
        Optional<com.hestia.vault.model.UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        profileOpt.ifPresent(profile -> userProfileRepository.delete(profile));

        // Delete User
        userRepository.deleteById(userId);

        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Account and all associated academic vault records permanently deleted."));
    }

    /**
     * Multi-Gmail Linking Endpoint: Link professional/additional emails to one account.
     */
    @PostMapping("/link-email")
    public ResponseEntity<?> linkEmail(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        String newEmail = (String) request.get("newEmail");

        if (userId == null || newEmail == null || !newEmail.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Valid User ID and email address are required."));
        }

        String cleaned = newEmail.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        User user = userOpt.get();
        String currentLinked = user.getLinkedEmails() != null ? user.getLinkedEmails() : "";
        List<String> list = new ArrayList<>(Arrays.asList(currentLinked.split(",")));
        list.removeIf(String::isBlank);

        if (!list.contains(cleaned) && !cleaned.equalsIgnoreCase(user.getEmail())) {
            list.add(cleaned);
        }

        String updatedLinked = String.join(",", list);
        user.setLinkedEmails(updatedLinked);
        if (user.getDisplayEmail() == null || user.getDisplayEmail().isBlank()) {
            user.setDisplayEmail(cleaned);
        }
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Email linked successfully! Professional Gmail added to account.",
            "primaryEmail", user.getEmail(),
            "linkedEmails", updatedLinked,
            "displayEmail", user.getDisplayEmail() != null ? user.getDisplayEmail() : user.getEmail()
        ));
    }

    /**
     * Select Display Email Endpoint: Choose which linked email to display publicly on profile.
     */
    @PostMapping("/set-display-email")
    public ResponseEntity<?> setDisplayEmail(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : null;
        String selectedEmail = (String) request.get("selectedEmail");

        if (userId == null || selectedEmail == null || selectedEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Valid User ID and selected email are required."));
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        User user = userOpt.get();
        user.setDisplayEmail(selectedEmail.trim().toLowerCase());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Display email preference updated.",
            "displayEmail", user.getDisplayEmail()
        ));
    }

    // Standard local registration endpoint
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        if (username == null || email == null || password == null || username.isBlank() || email.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Username, email, and password are required."));
        }

        String cleanedEmail = email.trim().toLowerCase();
        String cleanedUsername = username.trim();

        if (userRepository.existsByEmail(cleanedEmail)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Email address already registered. Please log in instead."));
        }

        if (userRepository.existsByUsername(cleanedUsername)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Username already taken. Please choose another."));
        }

        User user = new User();
        user.setUsername(cleanedUsername);
        user.setEmail(cleanedEmail);
        user.setHashedPassword(passwordEncoder.encode(password));
        user.setAuthProvider("local");
        user.setCreatedAt(LocalDateTime.now());

        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "status", "Registration successful",
            "message", "User account registered successfully.",
            "user", user
        ));
    }

    // Standard local login endpoint
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> request) {
        String usernameOrEmail = request.get("usernameOrEmail");
        String password = request.get("password");

        if (usernameOrEmail == null || password == null || usernameOrEmail.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Please provide email/username and password."));
        }

        String input = usernameOrEmail.trim().toLowerCase();

        Optional<User> userOpt = userRepository.findByEmail(input);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(usernameOrEmail.trim());
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "User account not found. Please sign up first."));
        }

        User user = userOpt.get();
        if (user.getHashedPassword() != null && !passwordEncoder.matches(password, user.getHashedPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("detail", "Incorrect password. Please try again."));
        }

        return ResponseEntity.ok(Map.of(
            "status", "Login successful",
            "message", "Authentication verified.",
            "user", user
        ));
    }

    // OFFICIAL GOOGLE OAUTH 2.0 SINGLE SIGN-ON (SSO) ENDPOINT
    @PostMapping("/google")
    public ResponseEntity<?> googleAuthenticate(@RequestBody Map<String, String> request) {
        String idTokenString = request.get("idToken");
        String fallbackEmail = request.get("email");
        String fallbackName = request.get("name");

        String email = null;
        String name = null;

        // Try verifying Google ID Token with GoogleIdTokenVerifier & configured Client ID
        if (idTokenString != null && !idTokenString.isBlank()) {
            try {
                GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(Collections.singletonList(googleClientId))
                        .build();

                GoogleIdToken idToken = verifier.verify(idTokenString);
                if (idToken != null) {
                    GoogleIdToken.Payload payload = idToken.getPayload();
                    email = payload.getEmail();
                    name = (String) payload.get("name");
                }
            } catch (Exception e) {
                // Fallback JWT Base64 payload decoding
                try {
                    String[] parts = idTokenString.split("\\.");
                    if (parts.length >= 2) {
                        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
                        if (payloadJson.contains("\"email\":\"")) {
                            int start = payloadJson.indexOf("\"email\":\"") + 9;
                            int end = payloadJson.indexOf("\"", start);
                            email = payloadJson.substring(start, end);
                        }
                        if (payloadJson.contains("\"name\":\"")) {
                            int start = payloadJson.indexOf("\"name\":\"") + 8;
                            int end = payloadJson.indexOf("\"", start);
                            name = payloadJson.substring(start, end);
                        }
                    }
                } catch (Exception ex) {}
            }
        }

        if (email == null || email.isBlank()) {
            email = fallbackEmail != null ? fallbackEmail : "user@gmail.com";
        }
        if (name == null || name.isBlank()) {
            name = fallbackName != null ? fallbackName : email.split("@")[0];
        }

        String cleanedEmail = email.trim().toLowerCase();

        // Database Logic: Search UserRepository by email
        Optional<User> userOpt = userRepository.findByEmail(cleanedEmail);
        User user;
        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            // Auto-register new Google user with authProvider = "google", hashedPassword = null
            user = new User();
            user.setUsername(name.replaceAll("\\s+", "_").toLowerCase());
            user.setEmail(cleanedEmail);
            user.setAuthProvider("google");
            user.setHashedPassword(null);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of(
            "status", "Google authentication successful",
            "email", cleanedEmail,
            "user", user
        ));
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("Auth Service Online");
    }
}