package com.hestia.vault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hestia_memories", indexes = {
    @Index(name = "idx_hestia_mem_user", columnList = "userIdentifier"),
    @Index(name = "idx_hestia_mem_key", columnList = "memoryKey")
})
public class HestiaMemoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userIdentifier; // username or email

    @Column(nullable = false)
    private String memoryKey; // e.g. "tech_stack", "favorite_language", "career_goal", "academic_degree", "custom_fact"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String memoryValue; // e.g. "Java, Spring Boot, React", "Wants to become a Senior Cloud Architect"

    private String category; // "PREFERENCE", "BACKGROUND", "PROMISE", "PERSONALITY_TRAIT", "ACADEMIC"

    private double confidenceScore; // 0.0 to 1.0

    private boolean isPinned; // User pinned fact

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public HestiaMemoryEntity() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.confidenceScore = 0.9;
        this.isPinned = false;
        this.category = "PREFERENCE";
    }

    public HestiaMemoryEntity(String userIdentifier, String memoryKey, String memoryValue, String category) {
        this();
        this.userIdentifier = userIdentifier;
        this.memoryKey = memoryKey;
        this.memoryValue = memoryValue;
        this.category = category;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }

    public String getMemoryKey() { return memoryKey; }
    public void setMemoryKey(String memoryKey) { this.memoryKey = memoryKey; }

    public String getMemoryValue() { return memoryValue; }
    public void setMemoryValue(String memoryValue) { this.memoryValue = memoryValue; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
