package com.hestia.vault.dto;

public class VerificationRequest {

    private Long userId;
    private String email;
    private String institution;
    private String studentIdNumber;
    private Double claimedCgpa;
    private String documentBase64; // Base64 encoded PDF or Image
    private String documentFileName;
    private String qrDataOverride; // Optional manual QR string payload if scanned client-side

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getStudentIdNumber() {
        return studentIdNumber;
    }

    public void setStudentIdNumber(String studentIdNumber) {
        this.studentIdNumber = studentIdNumber;
    }

    public Double getClaimedCgpa() {
        return claimedCgpa;
    }

    public void setClaimedCgpa(Double claimedCgpa) {
        this.claimedCgpa = claimedCgpa;
    }

    public String getDocumentBase64() {
        return documentBase64;
    }

    public void setDocumentBase64(String documentBase64) {
        this.documentBase64 = documentBase64;
    }

    public String getDocumentFileName() {
        return documentFileName;
    }

    public void setDocumentFileName(String documentFileName) {
        this.documentFileName = documentFileName;
    }

    public String getQrDataOverride() {
        return qrDataOverride;
    }

    public void setQrDataOverride(String qrDataOverride) {
        this.qrDataOverride = qrDataOverride;
    }
}
