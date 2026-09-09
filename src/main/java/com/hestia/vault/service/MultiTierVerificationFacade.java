package com.hestia.vault.service;

import com.hestia.vault.dto.VerificationRequest;
import com.hestia.vault.dto.VerificationResult;
import com.hestia.vault.model.UserProfile;
import com.hestia.vault.repository.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MultiTierVerificationFacade {

    @Autowired
    private EduEmailVerificationService eduEmailService;

    @Autowired
    private QrPkiVerificationService qrPkiService;

    @Autowired
    private DocumentAuditService documentAuditService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    public VerificationResult processVerification(VerificationRequest request) {
        VerificationResult result = new VerificationResult();
        int confidenceScore = 0;

        // TIER 1: Edu Email Check
        boolean isEduEmail = eduEmailService.isEducationalEmail(request.getEmail());
        if (isEduEmail) {
            result.setStudentVerified(true);
            result.addAuditLog("TIER 1 SUCCESS: Verified educational email domain (" + request.getEmail() + ").");
            confidenceScore += 40;
        } else {
            result.addAuditLog("TIER 1 INFO: Email (" + request.getEmail() + ") is non-educational domain.");
        }

        // TIER 2: QR Code / PKI Signature Extraction
        QrPkiVerificationService.QrResult qrResult = qrPkiService.extractAndVerifyQrCode(
                request.getDocumentBase64(),
                request.getQrDataOverride()
        );

        if (qrResult.found()) {
            result.addAuditLog("TIER 2 SUCCESS: Decoded embedded QR code payload. Issuer: " + qrResult.issuerInfo());
            if (qrResult.isCryptographicallySigned()) {
                result.setCertificateVerified(true);
                result.setPrimaryTier("TIER_2_QR_PKI");
                confidenceScore += 50;
                result.addAuditLog("TIER 2 CRYPTO VERIFIED: Certificate signature payload is valid.");
            }
        } else {
            result.addAuditLog("TIER 2 INFO: No scannable QR code found in uploaded document.");
        }

        // TIER 3: Document Metadata & Text Audit
        DocumentAuditService.DocumentAuditResult auditResult = documentAuditService.auditDocument(
                request.getDocumentBase64(),
                request.getStudentIdNumber(),
                request.getClaimedCgpa(),
                request.getInstitution()
        );

        if (auditResult.isPdf()) {
            result.addAuditLog("TIER 3 AUDIT: Document format verified as valid PDF structure.");
            if (auditResult.metadataTamperWarning()) {
                result.setPrimaryTier("TIER_3_OCR_METADATA");
                result.addWarning("CRITICAL WARNING: PDF metadata indicates editing software usage.");
                for (String warning : auditResult.detectedEditingTools()) {
                    result.addWarning(warning);
                }
            } else {
                result.addAuditLog("TIER 3 AUDIT: PDF metadata clean, no image manipulation software flags detected.");
            }

            if (auditResult.studentIdMatched()) {
                result.addAuditLog("TIER 3 MATCH: Claimed Student ID (" + request.getStudentIdNumber() + ") matched document text.");
                confidenceScore += 10;
            }
            if (auditResult.cgpaMatched()) {
                result.addAuditLog("TIER 3 MATCH: Claimed CGPA (" + request.getClaimedCgpa() + ") matched document text.");
                confidenceScore += 10;
            }
        }

        // Final Confidence Calculation and Status Determination
        confidenceScore = Math.min(100, Math.max(0, confidenceScore));
        result.setConfidenceScore(confidenceScore);

        if (auditResult.metadataTamperWarning()) {
            result.setStatus("SUSPICIOUS");
        } else if (result.isCertificateVerified() || (result.isStudentVerified() && confidenceScore >= 70)) {
            result.setStatus("VERIFIED");
            if (result.getPrimaryTier() == null) {
                result.setPrimaryTier("TIER_1_EMAIL");
            }
        } else if (confidenceScore >= 40) {
            result.setStatus("PENDING_REVIEW");
            if (result.getPrimaryTier() == null) {
                result.setPrimaryTier("TIER_3_OCR_METADATA");
            }
        } else {
            result.setStatus("UNVERIFIED");
        }

        // Persist updates to UserProfile if userId is provided
        if (request.getUserId() != null) {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(request.getUserId());
            UserProfile profile = profileOpt.orElseGet(() -> {
                UserProfile p = new UserProfile();
                p.setUserId(request.getUserId());
                return p;
            });

            profile.setVerificationStatus(result.getStatus());
            profile.setVerificationTier(result.getPrimaryTier() != null ? result.getPrimaryTier() : "TIER_3_OCR_METADATA");
            profile.setVerificationConfidenceScore(result.getConfidenceScore());
            profile.setVerificationAuditLog(String.join("\n", result.getAuditLogs()));
            profile.setIsEduEmailVerified(result.isStudentVerified());
            if (request.getInstitution() != null) profile.setInstitution(request.getInstitution());
            if (request.getClaimedCgpa() != null) profile.setCgpa(request.getClaimedCgpa());
            if (request.getStudentIdNumber() != null) profile.setAcademicCreditId(request.getStudentIdNumber());
            if (request.getDocumentBase64() != null) profile.setCertificateImageData(request.getDocumentBase64());
            profile.setUpdatedAt(LocalDateTime.now());

            userProfileRepository.save(profile);
        }

        return result;
    }
}
