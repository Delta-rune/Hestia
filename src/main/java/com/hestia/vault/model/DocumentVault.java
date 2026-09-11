package com.hestia.vault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "document_vault")
public class DocumentVault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "document_type", nullable = false)
    private String documentType; // CERTIFICATE, GRADE_CARD, TRANSCRIPT

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType; // application/pdf, image/png, image/jpeg

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Lob
    @Column(name = "file_data", columnDefinition = "TEXT")
    private String fileData; // Base64 encoded file data

    @Column(name = "file_hash")
    private String fileHash; // SHA-256 fingerprint

    @Column(name = "credential_id")
    private String credentialId;

    @Column(name = "verification_status")
    private String verificationStatus = "VERIFIED";

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now();

    public DocumentVault() {
    }

    public DocumentVault(Long userId, String userEmail, String documentType, String title, String issuer,
                         String fileName, String contentType, Long fileSizeBytes, String fileData,
                         String fileHash, String credentialId) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.documentType = documentType;
        this.title = title;
        this.issuer = issuer;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.fileData = fileData;
        this.fileHash = fileHash;
        this.credentialId = credentialId;
        this.uploadedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getFileData() { return fileData; }
    public void setFileData(String fileData) { this.fileData = fileData; }

    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
