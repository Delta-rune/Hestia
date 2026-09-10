package com.hestia.vault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hestia_conversations", indexes = {
    @Index(name = "idx_hestia_conv_user", columnList = "userIdentifier"),
    @Index(name = "idx_hestia_conv_session", columnList = "sessionId")
})
public class HestiaConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userIdentifier;

    private String sessionId;

    @Column(nullable = false)
    private String senderRole; // "USER" or "HESTIA"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String messageText;

    private String emotionalTone; // e.g. "COLD_SARCASTIC", "CREATOR_FONDNESS", "NEUTRAL"

    private LocalDateTime timestamp;

    public HestiaConversationEntity() {
        this.timestamp = LocalDateTime.now();
    }

    public HestiaConversationEntity(String userIdentifier, String sessionId, String senderRole, String messageText, String emotionalTone) {
        this();
        this.userIdentifier = userIdentifier;
        this.sessionId = sessionId;
        this.senderRole = senderRole;
        this.messageText = messageText;
        this.emotionalTone = emotionalTone;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getEmotionalTone() { return emotionalTone; }
    public void setEmotionalTone(String emotionalTone) { this.emotionalTone = emotionalTone; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
