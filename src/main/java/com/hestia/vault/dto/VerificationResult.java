package com.hestia.vault.dto;

import java.util.ArrayList;
import java.util.List;

public class VerificationResult {

    private String status; // VERIFIED, PENDING_REVIEW, SUSPICIOUS, UNVERIFIED
    private String primaryTier; // TIER_1_EMAIL, TIER_2_QR_PKI, TIER_3_OCR_METADATA
    private int confidenceScore; // 0 to 100
    private boolean isStudentVerified;
    private boolean isCertificateVerified;
    private List<String> auditLogs = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPrimaryTier() {
        return primaryTier;
    }

    public void setPrimaryTier(String primaryTier) {
        this.primaryTier = primaryTier;
    }

    public int getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(int confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public boolean isStudentVerified() {
        return isStudentVerified;
    }

    public void setStudentVerified(boolean studentVerified) {
        isStudentVerified = studentVerified;
    }

    public boolean isCertificateVerified() {
        return isCertificateVerified;
    }

    public void setCertificateVerified(boolean certificateVerified) {
        isCertificateVerified = certificateVerified;
    }

    public List<String> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<String> auditLogs) {
        this.auditLogs = auditLogs;
    }

    public void addAuditLog(String log) {
        this.auditLogs.add(log);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
