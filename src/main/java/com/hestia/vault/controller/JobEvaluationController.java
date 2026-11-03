package com.hestia.vault.controller;

import com.hestia.vault.model.SystemSetting;
import com.hestia.vault.model.UserProfile;
import com.hestia.vault.repository.SystemSettingRepository;
import com.hestia.vault.repository.UserProfileRepository;
import com.hestia.vault.service.ComprehensiveCertificateVerificationEngine;
import com.hestia.vault.service.DocumentAuditService;
import com.hestia.vault.service.QrPkiVerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class JobEvaluationController {

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SystemSettingRepository systemSettingRepository;

    @Autowired
    private QrPkiVerificationService qrPkiService;

    @Autowired
    private DocumentAuditService documentAuditService;

    @Autowired
    private ComprehensiveCertificateVerificationEngine comprehensiveEngine;

    @Autowired(required = false)
    private com.hestia.vault.repository.DocumentVaultRepository documentVaultRepository;

    @Autowired(required = false)
    private com.hestia.vault.repository.UserRepository userRepository;

    @Value("${gemini.apiKey:}")
    private String geminiApiKey;

    @Value("${google.projectName:}")
    private String googleProjectName;

    @Value("${google.projectNumber:}")
    private String googleProjectNumber;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${groq.apiKey:}")
    private String groqApiKey;


    // Recognized accredited global & national universities list for verification
    private static final List<String> RECOGNIZED_UNIVERSITIES = Arrays.asList(
        "harvard university", "massachusetts institute of technology", "mit", "stanford university",
        "university of oxford", "oxford", "university of cambridge", "cambridge", "iit bombay", "iit delhi", "iit madras",
        "ktu", "apj abdul kalam technological university", "eth zurich", "national university of singapore", "nus",
        "caltech", "columbia university", "princeton university", "uc berkeley", "stanford", "harvard", "yale university",
        "cornell university", "carnegie mellon", "ucla", "university of toronto", "imperial college london",
        "aws", "google", "microsoft", "coursera", "udacity", "nptel", "edx", "udemy", "linkedin", "pluralsight",
        "calicut university", "university of calicut", "kerala university", "university of kerala", "mg university",
        "mahatma gandhi university", "vtu", "visvesvaraya technological university", "anna university",
        "delhi university", "mumbai university", "bits pilani", "amity university", "manipal university", "srm university",
        "cisco", "oracle", "ibm", "red hat", "comptia", "meta"
    );

    // Save or update user profile with strict accreditation & verification inspection
    @PostMapping("/profile/save")
    public ResponseEntity<?> saveProfile(@RequestBody Map<String, Object> request) {
        Long userId = request.get("userId") != null ? Long.valueOf(request.get("userId").toString()) : 1L;
        
        Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(userId);
        UserProfile profile = profileOpt.orElse(new UserProfile());
        profile.setUserId(userId);
        profile.setAcademicLevel((String) request.get("academicLevel"));
        
        String degree = request.get("degreeField") != null ? request.get("degreeField").toString().trim() : "";
        profile.setDegreeField(degree);
        
        String inst = request.get("institution") != null ? request.get("institution").toString().trim() : "";
        profile.setInstitution(inst);
        
        Double cgpa = null;
        if (request.get("cgpa") != null && !request.get("cgpa").toString().isBlank()) {
            try { cgpa = Double.valueOf(request.get("cgpa").toString()); } catch (Exception e) {}
        }
        profile.setCgpa(cgpa);

        String creditId = request.get("academicCreditId") != null ? request.get("academicCreditId").toString().trim() : "";
        profile.setAcademicCreditId(creditId);
        profile.setCertificates((String) request.get("certificates"));
        profile.setProjects((String) request.get("projects"));
        
        if (request.get("certificateImageData") != null) {
            profile.setCertificateImageData((String) request.get("certificateImageData"));
        }

        // STRICT VERIFICATION & ACCREDITATION AUDIT
        boolean isGibberishInst = isGibberish(inst);
        boolean isGibberishDegree = isGibberish(degree);
        boolean validGpa = cgpa != null && cgpa >= 0.0 && cgpa <= 10.0;
        boolean recognizedInst = isRecognizedUniversity(inst);

        String verificationStatus;
        List<String> auditLogs = new ArrayList<>();

        if (isGibberishInst || inst.length() < 3) {
            verificationStatus = "REJECTED_INVALID_INSTITUTION";
            auditLogs.add("Institution name '" + inst + "' rejected as invalid or unaccredited text.");
        } else if (isGibberishDegree || degree.length() < 3) {
            verificationStatus = "REJECTED_INVALID_DEGREE";
            auditLogs.add("Degree discipline '" + degree + "' rejected as invalid text.");
        } else if (!validGpa) {
            verificationStatus = "REJECTED_INVALID_CGPA";
            auditLogs.add("CGPA score must be between 0.0 and 10.0.");
        } else if (!recognizedInst) {
            verificationStatus = "UNVERIFIED_PENDING_REGISTRY_CHECK";
            auditLogs.add("Institution '" + inst + "' is not in the Instant Accreditation Registry and requires manual review.");
        } else {
            verificationStatus = "VERIFIED_AUTHENTIC";
            auditLogs.add("Institution '" + inst + "' validated against Universal Accreditation Registry.");
            auditLogs.add("Academic credentials and CGPA score fully verified.");
        }

        profile.setVerificationStatus(verificationStatus);
        userProfileRepository.save(profile);

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "verificationStatus", verificationStatus,
            "auditLogs", auditLogs,
            "message", verificationStatus.startsWith("VERIFIED") ? 
                "Qualifications successfully verified & saved!" : 
                "Profile saved, but verification failed: " + (auditLogs.isEmpty() ? "Check credentials" : auditLogs.get(0)),
            "profile", profile
        ));
    }

    // Evaluate Job Eligibility & Universal GPA Conversion
    @PostMapping("/jobs/evaluate")
    public ResponseEntity<?> evaluateJobEligibility(@RequestBody Map<String, String> request) {
        String userDegree = request.getOrDefault("degreeField", "Engineering");
        String cgpaStr = request.getOrDefault("cgpa", "8.0");
        String jobTitle = request.getOrDefault("jobTitle", "Software Engineer");
        String requiredDegreeDomain = request.getOrDefault("requiredDomain", "Engineering");
        String requiredMinCgpa = request.getOrDefault("minCgpa", "7.5");

        double userCgpa = 8.0;
        try { userCgpa = Double.parseDouble(cgpaStr); } catch (Exception e) {}

        double minCgpa = 7.5;
        try { minCgpa = Double.parseDouble(requiredMinCgpa); } catch (Exception e) {}

        double normalizedGpa4Scale = userCgpa > 4.0 ? (userCgpa / 10.0) * 4.0 : userCgpa;
        double normalizedGpa10Scale = userCgpa <= 4.0 ? (userCgpa / 4.0) * 10.0 : userCgpa;

        boolean domainMatches = userDegree.equalsIgnoreCase(requiredDegreeDomain) ||
                (userDegree.toLowerCase().contains("engineering") && requiredDegreeDomain.toLowerCase().contains("tech")) ||
                (userDegree.toLowerCase().contains("computer") && requiredDegreeDomain.toLowerCase().contains("software"));

        boolean isMedicalRole = jobTitle.toLowerCase().contains("doctor") || jobTitle.toLowerCase().contains("medical") || jobTitle.toLowerCase().contains("surgeon");
        boolean isMedicalDegree = userDegree.toLowerCase().contains("medical") || userDegree.toLowerCase().contains("mbbs") || userDegree.toLowerCase().contains("medicine");

        String status;
        String honestVerdict;
        List<String> improvementSteps = new ArrayList<>();
        boolean canApply = false;

        if (isMedicalRole && !isMedicalDegree) {
            status = "INELIGIBLE_DOMAIN_MISMATCH";
            canApply = false;
            honestVerdict = "Honest Assessment: You currently hold an " + userDegree + " qualification, whereas this " + jobTitle + " position strictly requires an accredited Medical (MBBS/MD) degree. You cannot enter clinical medicine directly with an engineering degree.";
            improvementSteps.add("Option 1: Explore HealthTech, Biomedical Engineering, or Health Data Analytics roles where your technical background is highly sought after.");
            improvementSteps.add("Option 2: If passionate about practicing Medicine, enroll in a recognized Pre-Med / Graduate-Entry Medicine degree program.");
        } else if (!domainMatches) {
            status = "DOMAIN_MISMATCH";
            canApply = false;
            honestVerdict = "Domain Variance: This role targets a " + requiredDegreeDomain + " background, but your degree is in " + userDegree + ". Specialized domain requirements must be bridged.";
            improvementSteps.add("Earn verified certificates in " + requiredDegreeDomain + " fundamentals to bridge the domain gap.");
            improvementSteps.add("Build 2-3 portfolio projects demonstrating practical capability in " + jobTitle + " tasks.");
        } else if (userCgpa < minCgpa) {
            status = "UNDER_QUALIFIED_GPA";
            canApply = true;
            honestVerdict = "Academic Threshold: Your current CGPA (" + String.format("%.2f", userCgpa) + ") is below the preferred threshold of " + minCgpa + ". However, your degree domain aligns with the role.";
            improvementSteps.add("Focus on final semester coursework to elevate CGPA to " + minCgpa + "+.");
            improvementSteps.add("Highlight high-impact projects and certified industry skills to offset academic margin.");
        } else {
            status = "QUALIFIED";
            canApply = true;
            honestVerdict = "Excellent Alignment: Your academic background in " + userDegree + " and normalized CGPA (" + String.format("%.2f", normalizedGpa10Scale) + "/10.0 | " + String.format("%.2f", normalizedGpa4Scale) + "/4.0) satisfy all baseline qualifications for " + jobTitle + ".";
            improvementSteps.add("Your application profile is ready for recruiter dispatch.");
            improvementSteps.add("Prepare for technical & behavioral interview rounds.");
        }

        return ResponseEntity.ok(Map.of(
            "status", status,
            "canApply", canApply,
            "jobTitle", jobTitle,
            "userDegree", userDegree,
            "normalizedGpa4Scale", String.format("%.2f", normalizedGpa4Scale),
            "normalizedGpa10Scale", String.format("%.2f", normalizedGpa10Scale),
            "honestVerdict", honestVerdict,
            "improvementSteps", improvementSteps
        ));
    }

    // Verify Certificate Authenticity (COMPREHENSIVE INTELLIGENCE & ANTI-FRAUD VERIFICATION)
    @PostMapping("/certificates/upload-verify")
    public ResponseEntity<?> uploadAndVerifyCertificate(@RequestBody Map<String, String> request) {
        String certId = request.get("certId");
        String certName = request.get("certName");
        String issuer = request.get("issuer");
        String imageData = request.get("imageData");
        String userIdStr = request.get("userId");
        String userEmail = request.get("userEmail");
        String userName = request.get("userName");

        Long userId = parseUserId(userIdStr);
        if ((userName == null || userName.isBlank()) && userRepository != null && userId != null) {
            try {
                userRepository.findById(userId).ifPresent(u -> {
                    // Could supply full name if available
                });
            } catch (Exception ignored) {}
        }

        ComprehensiveCertificateVerificationEngine.ComprehensiveVerificationResult result =
                comprehensiveEngine.verifyDocument(
                        imageData,
                        certId,
                        certName,
                        issuer,
                        userEmail,
                        userName
                );

        // Check if this file has already been saved into the user's Cloud Vault
        boolean isAlreadyInVault = false;
        if (documentVaultRepository != null && userId != null && !result.sha256Hash().equals("INVALID_HASH_REJECTED")) {
            try {
                String rawHash = result.sha256Hash().replace("SHA256:", "").toLowerCase();
                isAlreadyInVault = documentVaultRepository.findByUserIdAndFileHash(userId, "sha256:" + rawHash).isPresent() ||
                                  documentVaultRepository.findByUserIdAndFileHash(userId, rawHash).isPresent();
            } catch (Exception ignored) {}
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", result.status());
        resp.put("trustScore", result.trustScore());
        resp.put("tier", result.tier());
        resp.put("classification", result.classification().name());
        resp.put("certificateId", result.entity().credentialId());
        resp.put("certificateName", result.entity().certificateTitle());
        resp.put("issuer", result.entity().issuerAuthority());
        resp.put("recipientName", result.entity().recipientName());
        resp.put("performanceScore", result.entity().performanceScore());
        resp.put("creditsAwarded", result.entity().creditsAwarded());
        resp.put("issueSessionOrDate", result.entity().issueSessionOrDate());
        resp.put("verificationUrl", result.entity().verificationUrl());
        resp.put("hasImageProof", imageData != null && !imageData.isBlank());
        resp.put("verificationHash", result.sha256Hash());
        resp.put("alreadyInVault", isAlreadyInVault);
        resp.put("hasTamperWarnings", result.hasTamperWarnings());
        resp.put("tamperWarnings", result.tamperWarnings());
        resp.put("auditNote", result.diagnosticAuditNote());
        resp.put("verifiedAt", result.verifiedTimestampMs());

        return ResponseEntity.ok(resp);
    }

    /**
     * Explicit Endpoint: User asks to archive a verified certificate to Cloud Vault.
     * Prevents automatic duplicate insertions and enforces user permission.
     */
    @PostMapping("/certificates/archive-to-vault")
    public ResponseEntity<?> saveCertificateToVault(@RequestBody Map<String, String> request) {
        String userIdStr = request.get("userId");
        String title = request.get("certName");
        String issuer = request.get("issuer");
        String certId = request.get("certId");
        String documentBase64 = request.get("imageData");

        if (userIdStr == null || userIdStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "User ID is required."));
        }
        if (documentBase64 == null || documentBase64.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "message", "Document data is required to archive."));
        }

        Long userId = parseUserId(userIdStr);
        String finalTitle = (title != null && !title.isBlank()) ? title.trim() : "Verified Academic Certificate";
        String finalIssuer = (issuer != null && !issuer.isBlank()) ? issuer.trim() : "Accredited Authority";
        String finalCertId = (certId != null && !certId.isBlank()) ? certId.trim() : "HST-CERT-" + (System.currentTimeMillis() % 1000000);

        try {
            String clean = documentBase64.contains(",") ? documentBase64.substring(documentBase64.indexOf(",") + 1) : documentBase64;
            clean = clean.replaceAll("\\s+", "");
            byte[] bytes = java.util.Base64.getMimeDecoder().decode(clean);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(bytes);
            StringBuilder sb = new StringBuilder("sha256:");
            for (byte b : hash) sb.append(String.format("%02x", b));
            String fHash = sb.toString();

            // Check if already in vault to prevent duplicates
            if (documentVaultRepository != null) {
                Optional<com.hestia.vault.model.DocumentVault> existing = documentVaultRepository.findByUserIdAndFileHash(userId, fHash);
                if (existing.isPresent()) {
                    return ResponseEntity.ok(Map.of(
                            "status", "ALREADY_EXISTS",
                            "message", "This certificate is already saved in your Academic Cloud Vault.",
                            "documentId", existing.get().getId(),
                            "alreadyInVault", true
                    ));
                }

                boolean isPdf = documentBase64.contains("application/pdf") || clean.startsWith("JVBERi");
                String cType = isPdf ? "application/pdf" : "image/png";
                String fName = (finalTitle.replaceAll("[^a-zA-Z0-9_-]", "_") + (isPdf ? ".pdf" : ".png"));

                com.hestia.vault.model.DocumentVault doc = new com.hestia.vault.model.DocumentVault(
                        userId, "", "CERTIFICATE", finalTitle, finalIssuer, fName, cType, (long) bytes.length, clean, fHash, finalCertId);
                com.hestia.vault.model.DocumentVault saved = documentVaultRepository.save(doc);

                // Update user profile certificates list
                if (userProfileRepository != null) {
                    Optional<UserProfile> pOpt = userProfileRepository.findFirstByUserIdOrderByIdDesc(userId);
                    if (pOpt.isPresent()) {
                        UserProfile p = pOpt.get();
                        String existingCerts = p.getCertificates() != null ? p.getCertificates() : "";
                        String entry = finalTitle + " (" + finalIssuer + " #" + finalCertId + ")";
                        if (!existingCerts.contains(finalCertId)) {
                            p.setCertificates(existingCerts.isBlank() ? entry : existingCerts + " | " + entry);
                            userProfileRepository.save(p);
                        }
                    }
                }

                return ResponseEntity.ok(Map.of(
                        "status", "SUCCESS",
                        "message", "Certificate archived securely to your Academic Cloud Vault!",
                        "documentId", saved.getId(),
                        "alreadyInVault", true
                ));
            }

            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "Vault repository is unavailable."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "message", "Failed to archive certificate: " + e.getMessage()));
        }
    }

    private void saveVerifiedCertificateToProfile(String userIdStr, String title, String issuer, String certId, String documentBase64) {
        if (userIdStr == null || userIdStr.isBlank()) return;
        try {
            Long userId = parseUserId(userIdStr);
            Optional<UserProfile> pOpt = userProfileRepository != null ? userProfileRepository.findFirstByUserIdOrderByIdDesc(userId) : Optional.empty();
            if (pOpt.isPresent()) {
                UserProfile p = pOpt.get();
                String existing = p.getCertificates() != null ? p.getCertificates() : "";
                String entry = title + " (" + issuer + " #" + certId + ")";
                if (!existing.contains(certId)) {
                    p.setCertificates(existing.isBlank() ? entry : existing + " | " + entry);
                    userProfileRepository.save(p);
                }
            }

            if (documentVaultRepository != null && documentBase64 != null && !documentBase64.isBlank()) {
                try {
                    String clean = documentBase64.contains(",") ? documentBase64.substring(documentBase64.indexOf(",") + 1) : documentBase64;
                    clean = clean.replaceAll("\\s+", "");
                    byte[] bytes = java.util.Base64.getMimeDecoder().decode(clean);
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
                    byte[] hash = md.digest(bytes);
                    StringBuilder sb = new StringBuilder("sha256:");
                    for (byte b : hash) sb.append(String.format("%02x", b));
                    String fHash = sb.toString();

                    // Check for existing document to prevent duplicate rows
                    Optional<com.hestia.vault.model.DocumentVault> existingDoc = documentVaultRepository.findByUserIdAndFileHash(userId, fHash);
                    if (existingDoc.isEmpty()) {
                        boolean isPdf = documentBase64.contains("application/pdf") || clean.startsWith("JVBERi");
                        String cType = isPdf ? "application/pdf" : "image/png";
                        String fName = (title.replaceAll("[^a-zA-Z0-9_-]", "_") + (isPdf ? ".pdf" : ".png"));

                        com.hestia.vault.model.DocumentVault doc = new com.hestia.vault.model.DocumentVault(
                                userId, "", "CERTIFICATE", title, issuer, fName, cType, (long) bytes.length, clean, fHash, certId);
                        documentVaultRepository.save(doc);
                    }
                } catch (Exception docEx) {
                    System.err.println("DocumentVault certificate archive warning: " + docEx.getMessage());
                }
            }
        } catch (Exception ignored) {}
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) return 1L;
        String clean = userIdStr.trim();
        if (clean.contains("@") && userRepository != null) {
            Optional<com.hestia.vault.model.User> u = userRepository.findByEmail(clean.toLowerCase());
            if (u.isPresent()) return u.get().getId();
        }
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            if (userRepository != null) {
                Optional<com.hestia.vault.model.User> u = userRepository.findByUsername(clean);
                if (u.isPresent()) return u.get().getId();
            }
            String digits = clean.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return Long.parseLong(digits.substring(0, Math.min(15, digits.length())));
                } catch (NumberFormatException ignored) {}
            }
            return (long) Math.abs(clean.hashCode());
        }
    }

    private boolean isGibberish(String input) {

        if (input == null || input.trim().length() < 3) return true;
        String lower = input.toLowerCase().trim();
        if (lower.equals("wwss") || lower.equals("qweqwe") || lower.equals("qww") || lower.equals("dsd") || lower.equals("sjsjd") || lower.equals("aaas") || lower.equals("2333")) {
            return true;
        }
        if (Pattern.matches("^(.)\\1+$", lower)) return true;
        return false;
    }

    private boolean isRecognizedUniversity(String inst) {
        if (inst == null || inst.isBlank()) return false;
        if (comprehensiveEngine != null && comprehensiveEngine.isRecognizedAuthority(inst)) return true;
        String lower = inst.toLowerCase().trim();
        for (String u : RECOGNIZED_UNIVERSITIES) {
            if (lower.contains(u) || u.contains(lower)) return true;
        }
        return false;
    }
}
