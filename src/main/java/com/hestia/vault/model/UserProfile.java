package com.hestia.vault.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(name = "academic_level")
    private String academicLevel; // Pre-Graduation, Post-Graduation, Alumni

    @Column(name = "degree_field")
    private String degreeField; // e.g. Computer Science, Medicine, Civil Engineering

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "institution")
    private String institution; // e.g. Harvard, IIT, KTU, Stanford

    private Double cgpa; // e.g. 8.5 (out of 10) or 3.7 (out of 4.0)

    @Column(name = "academic_credit_id")
    private String academicCreditId; // National / Universal Credit ID

    @Column(name = "apaar_id")
    private String apaarId; // 12-digit One Nation One Student ID (APAAR/ABC)

    @Column(name = "college_name")
    private String collegeName; // Specific affiliated college/department name

    @Column(name = "date_of_birth")
    private String dateOfBirth; // YYYY-MM-DD format

    private Integer calculatedAge; // Derived age from DOB

    @Column(name = "abc_credits")
    private Integer abcCredits; // Accumulated credits in Academic Bank of Credits

    @Column(length = 4000)
    private String certificates; // List or JSON of verified certificates

    @Column(columnDefinition = "TEXT")
    private String certificateImageData; // Base64 image representation of uploaded certificate

    @Column(name = "verification_status")
    private String verificationStatus = "UNVERIFIED"; // VERIFIED, PENDING, UNVERIFIED, SUSPICIOUS

    @Column(name = "verification_tier")
    private String verificationTier; // TIER_1_EMAIL, TIER_2_QR_PKI, TIER_3_OCR_METADATA

    @Column(name = "verification_confidence_score")
    private Integer verificationConfidenceScore = 0; // 0-100 score

    @Column(name = "verification_audit_log", columnDefinition = "TEXT")
    private String verificationAuditLog; // Audit trail of checks

    @Column(name = "is_edu_email_verified")
    private Boolean isEduEmailVerified = false;

    @Column(length = 2000)
    private String projects; // Summary of key projects

    private LocalDateTime updatedAt = LocalDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getAcademicLevel() { return academicLevel; }
    public void setAcademicLevel(String academicLevel) { this.academicLevel = academicLevel; }

    public String getDegreeField() { return degreeField; }
    public void setDegreeField(String degreeField) { this.degreeField = degreeField; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }

    public Double getCgpa() { return cgpa; }
    public void setCgpa(Double cgpa) { this.cgpa = cgpa; }

    public String getAcademicCreditId() { return academicCreditId; }
    public void setAcademicCreditId(String academicCreditId) { this.academicCreditId = academicCreditId; }

    public String getApaarId() { return apaarId; }
    public void setApaarId(String apaarId) { this.apaarId = apaarId; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public Integer getCalculatedAge() { return calculatedAge; }
    public void setCalculatedAge(Integer calculatedAge) { this.calculatedAge = calculatedAge; }

    public Integer getAbcCredits() { return abcCredits; }
    public void setAbcCredits(Integer abcCredits) { this.abcCredits = abcCredits; }

    public String getCertificates() { return certificates; }
    public void setCertificates(String certificates) { this.certificates = certificates; }

    public String getCertificateImageData() { return certificateImageData; }
    public void setCertificateImageData(String certificateImageData) { this.certificateImageData = certificateImageData; }

    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String verificationStatus) { this.verificationStatus = verificationStatus; }

    public String getVerificationTier() { return verificationTier; }
    public void setVerificationTier(String verificationTier) { this.verificationTier = verificationTier; }

    public Integer getVerificationConfidenceScore() { return verificationConfidenceScore; }
    public void setVerificationConfidenceScore(Integer verificationConfidenceScore) { this.verificationConfidenceScore = verificationConfidenceScore; }

    public String getVerificationAuditLog() { return verificationAuditLog; }
    public void setVerificationAuditLog(String verificationAuditLog) { this.verificationAuditLog = verificationAuditLog; }

    public Boolean getIsEduEmailVerified() { return isEduEmailVerified; }
    public void setIsEduEmailVerified(Boolean isEduEmailVerified) { this.isEduEmailVerified = isEduEmailVerified; }

    public String getProjects() { return projects; }
    public void setProjects(String projects) { this.projects = projects; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
