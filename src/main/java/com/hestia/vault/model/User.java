package com.hestia.vault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(name = "auth_provider", nullable = false)
    private String authProvider = "local";

    @Column(name = "linked_emails", columnDefinition = "TEXT")
    private String linkedEmails; // Comma-separated secondary linked email addresses

    @Column(name = "display_email")
    private String displayEmail; // Primary or professional email selected for display

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getHashedPassword() { return hashedPassword; }
    public void setHashedPassword(String hashedPassword) { this.hashedPassword = hashedPassword; }

    public String getAuthProvider() { return authProvider; }
    public void setAuthProvider(String authProvider) { this.authProvider = authProvider; }

    public String getLinkedEmails() { return linkedEmails; }
    public void setLinkedEmails(String linkedEmails) { this.linkedEmails = linkedEmails; }

    public String getDisplayEmail() { return displayEmail; }
    public void setDisplayEmail(String displayEmail) { this.displayEmail = displayEmail; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}