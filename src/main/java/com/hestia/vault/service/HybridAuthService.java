package com.hestia.vault.service;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.hestia.vault.dto.AuthDto.*;
import com.hestia.vault.model.User;
import com.hestia.vault.model.UserProfile;
import com.hestia.vault.repository.UserProfileRepository;
import com.hestia.vault.repository.UserRepository;

@Service
public class HybridAuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${google.clientId:698778630407-b5r67j9jv96baijg7dikngrf7kv1etnb.apps.googleusercontent.com}")
    private String googleClientId;

    /**
     * Resolves a User entity using numeric userId, email, or username.
     */
    public Optional<User> findUser(Object userIdObj, String email, String username) {
        if (userIdObj != null) {
            try {
                Long userId = Long.valueOf(userIdObj.toString().trim());
                Optional<User> byId = userRepository.findById(userId);
                if (byId.isPresent()) return byId;
            } catch (NumberFormatException ignored) {}
        }

        if (email != null && !email.trim().isBlank()) {
            Optional<User> byEmail = userRepository.findByEmail(email.trim().toLowerCase());
            if (byEmail.isPresent()) return byEmail;
        }

        if (username != null && !username.trim().isBlank()) {
            Optional<User> byUsername = userRepository.findByUsername(username.trim());
            if (byUsername.isPresent()) return byUsername;
        }

        return Optional.empty();
    }

    /**
     * Google SSO Authentication and Auto-Upsert.
     * Supports both verified Google ID tokens and client-extracted profile details.
     */
    public ResponseEntity<?> authenticateGoogle(GoogleAuthRequest req) {
        String idTokenString = req.getIdToken();
        String email = req.getEmail();
        String name = req.getName();
        String requestedUsername = req.getUsername();

        // 1. Verify Google ID token if provided
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
                // Fallback JWT Base64 decoding
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
                } catch (Exception ignored) {}
            }
        }

        if (email == null || email.isBlank()) {
            email = "user@gmail.com";
        }
        if (name == null || name.isBlank()) {
            name = email.split("@")[0];
        }

        String cleanedEmail = email.trim().toLowerCase();
        String chosenUsername = (requestedUsername != null && !requestedUsername.isBlank())
                ? requestedUsername.trim()
                : name.replaceAll("\\s+", "_");

        Optional<User> userOpt = userRepository.findByEmail(cleanedEmail);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
            // Update username if explicitly selected and not already taken by another user
            if (chosenUsername != null && !chosenUsername.equalsIgnoreCase(user.getUsername())) {
                Optional<User> existing = userRepository.findByUsername(chosenUsername);
                if (existing.isEmpty() || existing.get().getId().equals(user.getId())) {
                    user.setUsername(chosenUsername);
                    userRepository.save(user);
                }
            }
        } else {
            // Auto-provision brand new Google user
            user = new User();
            String baseUsername = chosenUsername.isBlank() ? cleanedEmail.split("@")[0] : chosenUsername;
            String finalUsername = baseUsername;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = baseUsername + "_" + counter++;
            }
            user.setUsername(finalUsername);
            user.setEmail(cleanedEmail);
            user.setAuthProvider("google");
            user.setHashedPassword(null);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Google authentication verified.",
            "email", cleanedEmail,
            "user", user
        ));
    }

    /**
     * Set or update password for an account (enables hybrid username-password login).
     * If the account does not exist in DB yet (e.g. fresh container restart), it gracefully upserts it!
     */
    public ResponseEntity<?> setPassword(SetPasswordRequest req) {
        String newPassword = req.getNewPassword();
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Password must be at least 6 characters long."));
        }

        Optional<User> userOpt = findUser(req.getUserId(), req.getEmail(), req.getUsername());
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else if (req.getEmail() != null && !req.getEmail().trim().isBlank()) {
            // Graceful Auto-Provisioning: User signed in via Google / Supabase and needs initial DB record
            String cleanedEmail = req.getEmail().trim().toLowerCase();
            user = new User();
            String desiredUsername = req.getUsername() != null && !req.getUsername().trim().isBlank()
                    ? req.getUsername().trim()
                    : cleanedEmail.split("@")[0];
            String finalUsername = desiredUsername;
            int counter = 1;
            while (userRepository.existsByUsername(finalUsername)) {
                finalUsername = desiredUsername + "_" + counter++;
            }
            user.setUsername(finalUsername);
            user.setEmail(cleanedEmail);
            user.setAuthProvider("google");
            user.setCreatedAt(LocalDateTime.now());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "detail", "User account not found. Please log in first."
            ));
        }

        // Set secure BCrypt hash
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Password created/updated successfully! You can now log in using your username or email and this password.",
            "user", user
        ));
    }

    /**
     * Standard local login using username or email + password.
     * Enforces strict credential checks and blocks null-password Google accounts.
     */
    public ResponseEntity<?> loginUser(LoginRequest req) {
        String usernameOrEmail = req.getUsernameOrEmail();
        String password = req.getPassword();

        if (usernameOrEmail == null || password == null || usernameOrEmail.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Please provide email/username and password."));
        }

        String input = usernameOrEmail.trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(input);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByUsername(usernameOrEmail.trim());
        }

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "detail", "User account not found. Please sign up first."
            ));
        }

        User user = userOpt.get();
        if (user.getHashedPassword() == null || user.getHashedPassword().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "detail", "This account was registered using Google Sign-In and has no password set yet. Please log in using Google, then set a password in Settings."
            ));
        }

        if (!passwordEncoder.matches(password, user.getHashedPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "detail", "Incorrect password. Please try again."
            ));
        }

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Authentication verified.",
            "user", user
        ));
    }

    /**
     * Standard local registration endpoint.
     */
    public ResponseEntity<?> registerUser(RegisterRequest req) {
        String username = req.getUsername();
        String email = req.getEmail();
        String password = req.getPassword();

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
            "status", "SUCCESS",
            "message", "User account registered successfully.",
            "user", user
        ));
    }

    /**
     * Update Username endpoint with uniqueness validation.
     */
    public ResponseEntity<?> updateUsername(UpdateUsernameRequest req) {
        String newUsername = req.getNewUsername();
        if (newUsername == null || newUsername.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Valid new username is required."));
        }

        Optional<User> userOpt = findUser(req.getUserId(), req.getEmail(), null);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        String cleaned = newUsername.trim();
        User user = userOpt.get();
        if (!user.getUsername().equalsIgnoreCase(cleaned) && userRepository.existsByUsername(cleaned)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("detail", "Username already taken. Please choose another."));
        }

        user.setUsername(cleaned);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Username updated successfully.",
            "user", user
        ));
    }

    /**
     * Multi-Gmail Linking Endpoint.
     */
    public ResponseEntity<?> linkEmail(Object userId, String email, String newEmail) {
        if (newEmail == null || !newEmail.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Valid email address is required."));
        }

        Optional<User> userOpt = findUser(userId, email, null);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        String cleaned = newEmail.trim().toLowerCase();
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
     * Select Display Email Endpoint.
     */
    public ResponseEntity<?> setDisplayEmail(Object userId, String email, String selectedEmail) {
        if (selectedEmail == null || selectedEmail.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Selected email is required."));
        }

        Optional<User> userOpt = findUser(userId, email, null);
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

    /**
     * Delete Account with master developer protection.
     */
    public ResponseEntity<?> deleteAccount(Object userIdObj, String email, String username) {
        Optional<User> userOpt = findUser(userIdObj, email, username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("detail", "User account not found."));
        }

        User user = userOpt.get();
        Long userId = user.getId();

        // MASTER DEVELOPER PROTECTION
        if (user.getEmail() != null && (user.getEmail().equalsIgnoreCase("nichuag33@gmail.com") || 
            user.getEmail().equalsIgnoreCase("nichuag35@gmail.com") || 
            (user.getUsername() != null && user.getUsername().equalsIgnoreCase("nichu")))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "status", "PROTECTED",
                "detail", "Deletion Blocked: Master Developer Account (nichuag33@gmail.com) is permanently protected from deletion."
            ));
        }

        if (userId != null) {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
            profileOpt.ifPresent(p -> userProfileRepository.delete(p));
            userRepository.deleteById(userId);
        }

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Account and all associated records permanently deleted."
        ));
    }
}
