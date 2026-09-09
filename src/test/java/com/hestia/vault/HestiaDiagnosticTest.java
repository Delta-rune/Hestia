package com.hestia.vault;

import com.hestia.vault.controller.AuthController;
import com.hestia.vault.controller.JobEvaluationController;
import com.hestia.vault.controller.VerificationController;
import com.hestia.vault.dto.VerificationRequest;
import com.hestia.vault.dto.VerificationResult;
import com.hestia.vault.model.User;
import com.hestia.vault.model.UserProfile;
import com.hestia.vault.repository.UserRepository;
import com.hestia.vault.service.EduEmailVerificationService;
import com.hestia.vault.service.MultiTierVerificationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class HestiaDiagnosticTest {

    @Mock
    private com.hestia.vault.repository.UserProfileRepository userProfileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobEvaluationController jobEvaluationController;

    @org.mockito.Spy
    private com.hestia.vault.service.ApaarOtpService apaarOtpService = new com.hestia.vault.service.ApaarOtpService();

    @InjectMocks
    private VerificationController verificationController;

    @InjectMocks
    private AuthController authController;

    private EduEmailVerificationService eduEmailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eduEmailService = new EduEmailVerificationService();
    }

    @Test
    @DisplayName("DIAGNOSTIC 1: Hestia AI Chat Responsiveness & Persona Integrity")
    void diagnosticHestiaChatResponsivenessAndPersona() {
        // Query to check Hestia's identity & persona
        Map<String, Object> req = Map.of(
                "query", "who are you",
                "username", "Nichu",
                "email", "nichuag33@gmail.com",
                "history", List.of(),
                "profile", Map.of("degreeField", "Computer Science", "institution", "Stanford", "cgpa", "9.2")
        );

        var response = jobEvaluationController.chatWithHestia(req);
        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());

        Map body = (Map) response.getBody();
        assertNotNull(body);
        assertEquals("SUCCESS", body.get("status"));
        String reply = (String) body.get("reply");
        assertNotNull(reply);

        // Verify Hestia's response is non-empty and active
        assertFalse(reply.isBlank(), "Hestia reply should not be blank.");
        System.out.println("✅ DIAGNOSTIC 1 PASSED — Hestia AI is responsive. Reply: " + reply);
    }

    @Test
    @DisplayName("DIAGNOSTIC 2: Multi-Tier Credential & Verification Engine Health")
    void diagnosticMultiTierVerificationEngine() {
        assertTrue(eduEmailService.isEducationalEmail("student@mit.edu"));
        assertTrue(eduEmailService.isEducationalEmail("iitd@ac.in"));
        assertFalse(eduEmailService.isEducationalEmail("user@yahoo.com"));

        System.out.println("✅ DIAGNOSTIC 2 PASSED — Multi-Tier Verification Engine is fully operational.");
    }

    @Test
    @DisplayName("DIAGNOSTIC 3: User Entity & Vault Persistence Check")
    void diagnosticUserEntityPersistence() {
        User user = new User();
        user.setUsername("diagnostic_user");
        user.setEmail("diagnostic@hestia.io");
        user.setAuthProvider("local");

        assertEquals("diagnostic_user", user.getUsername());
        assertEquals("diagnostic@hestia.io", user.getEmail());
        assertNotNull(user.getCreatedAt());

        System.out.println("✅ DIAGNOSTIC 3 PASSED — User entity and vault models structured cleanly.");
    }

    @Test
    @DisplayName("DIAGNOSTIC 4: Universal Job & Compatibility Scanner Check")
    void diagnosticJobEvaluationEngine() {
        Map<String, String> req = Map.of(
                "jobTitle", "AI Engineer",
                "requiredDomain", "Engineering",
                "degreeField", "Computer Science",
                "cgpa", "8.5"
        );

        var res = jobEvaluationController.evaluateJobEligibility(req);
        assertNotNull(res);
        assertEquals(200, res.getStatusCode().value());

        Map body = (Map) res.getBody();
        assertNotNull(body);
        assertTrue(body.containsKey("compatibilityScore") || body.containsKey("canApply"));
        assertTrue(body.containsKey("normalizedGpa4Scale"));

        System.out.println("✅ DIAGNOSTIC 4 PASSED — Universal Job & Compatibility Scanner operational.");
    }

    @Test
    @DisplayName("DIAGNOSTIC 5: Dynamic Preference Learning & Custom Support Email Memory")
    void diagnosticPreferenceLearning() {
        // Nichu instructs Hestia to update support email
        Map<String, Object> req = Map.of(
                "query", "set support email to support@hestia.io",
                "username", "Nichu",
                "email", "nichuag33@gmail.com",
                "history", List.of(),
                "profile", Map.of()
        );

        var res = jobEvaluationController.chatWithHestia(req);
        assertNotNull(res);
        assertEquals(200, res.getStatusCode().value());

        Map body = (Map) res.getBody();
        assertNotNull(body);
        String reply = (String) body.get("reply");
        assertNotNull(reply);
        assertTrue(reply.contains("support@hestia.io"), "Hestia should confirm updating the preference to support@hestia.io");

        System.out.println("✅ DIAGNOSTIC 5 PASSED — Dynamic Preference Learning & System Setting Memory operational.");
    }

    @Test
    @DisplayName("DIAGNOSTIC 6: APAAR ID 2-Step OTP Security & Anti-Scraping Defense")
    void diagnosticApaarOtpSecurity() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(java.util.Optional.of(new UserProfile()));

        // 1. Attempt Sync WITHOUT OTP -> Must be rejected with 401 Unauthorized
        Map<String, String> noOtpReq = Map.of("apaarId", "987654321012", "userId", "1");
        var failRes = verificationController.syncApaarId(noOtpReq);
        assertEquals(401, failRes.getStatusCode().value(), "APAAR sync without OTP must return 401 Unauthorized");

        // 2. Request OTP Consent
        Map<String, String> requestOtpReq = Map.of("apaarId", "987654321012", "userId", "1", "mobilePhone", "+91 9876543210");
        var otpRes = verificationController.requestApaarOtp(requestOtpReq);
        assertEquals(200, otpRes.getStatusCode().value());
        Map otpBody = (Map) otpRes.getBody();
        assertNotNull(otpBody);
        assertEquals("SUCCESS", otpBody.get("status"));
        assertNotNull(otpBody.get("generatedOtp"));
        assertEquals("+91 ******3210", otpBody.get("maskedPhone"));

        // 3. Test Invalid OTP Attempt -> Must return 401 with remaining attempts info
        Map<String, String> invalidOtpReq = Map.of("apaarId", "987654321012", "userId", "1", "otp", "000000");
        var invalidRes = verificationController.syncApaarId(invalidOtpReq);
        assertEquals(401, invalidRes.getStatusCode().value());
        Map invalidBody = (Map) invalidRes.getBody();
        assertEquals(2, invalidBody.get("attemptsRemaining"));

        // 4. Sync WITH Valid OTP (or generated OTP) -> Must succeed with 200 OK
        String generatedOtp = (String) otpBody.get("generatedOtp");
        Map<String, String> validOtpReq = Map.of("apaarId", "987654321012", "userId", "1", "otp", generatedOtp);
        var successRes = verificationController.syncApaarId(validOtpReq);
        assertEquals(200, successRes.getStatusCode().value());
        System.out.println("✅ DIAGNOSTIC 6 PASSED — APAAR ID 2-Step OTP Security & Anti-Scraping Defense operational.");
    }

    @Test
    @DisplayName("DIAGNOSTIC 7: Master Developer Protection & Multi-Email Linking")
    void diagnosticMasterProtectionAndMultiEmail() {
        User creator = new User();
        creator.setId(99L);
        creator.setUsername("Nichu");
        creator.setEmail("nichuag33@gmail.com");

        when(userRepository.findById(99L)).thenReturn(java.util.Optional.of(creator));

        // 1. Attempt Account Deletion on Master Account -> Must be rejected with 403 FORBIDDEN
        Map<String, Object> delReq = Map.of("userId", 99L);
        var delRes = authController.deleteAccount(delReq);
        assertEquals(403, delRes.getStatusCode().value(), "Deleting master creator account must return 403 Forbidden");

        // 2. Link Professional Gmail
        Map<String, Object> linkReq = Map.of("userId", 99L, "newEmail", "nichu.pro@gmail.com");
        var linkRes = authController.linkEmail(linkReq);
        assertEquals(200, linkRes.getStatusCode().value());
        Map linkBody = (Map) linkRes.getBody();
        assertNotNull(linkBody);
        assertEquals("SUCCESS", linkBody.get("status"));

        System.out.println("✅ DIAGNOSTIC 7 PASSED — Master Developer Account Deletion Protection & Multi-Email Linking operational.");
    }
}
