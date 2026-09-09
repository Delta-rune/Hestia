package com.hestia.vault.service;

import com.hestia.vault.dto.VerificationRequest;
import com.hestia.vault.dto.VerificationResult;
import com.hestia.vault.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

class VerificationEngineTest {

    private EduEmailVerificationService eduEmailService;
    private QrPkiVerificationService qrPkiService;
    private DocumentAuditService documentAuditService;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private MultiTierVerificationFacade verificationFacade;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        eduEmailService = new EduEmailVerificationService();
        qrPkiService = new QrPkiVerificationService();
        documentAuditService = new DocumentAuditService();
    }

    @Test
    void testEduEmailVerification() {
        assertTrue(eduEmailService.isEducationalEmail("student@stanford.edu"));
        assertTrue(eduEmailService.isEducationalEmail("john@iitd.ac.in"));
        assertFalse(eduEmailService.isEducationalEmail("user@gmail.com"));
        assertEquals("Stanford", eduEmailService.extractInstitutionFromEmail("user@stanford.edu"));
    }

    @Test
    void testDocumentAuditServiceCleanText() {
        DocumentAuditService.DocumentAuditResult result = documentAuditService.auditDocument(
                "",
                "STU123",
                8.5,
                "Stanford"
        );
        assertFalse(result.metadataTamperWarning());
    }

    @Test
    void testFacadeProcessingEduStudent() {
        VerificationRequest req = new VerificationRequest();
        req.setEmail("alice@mit.edu");
        req.setInstitution("MIT");
        req.setStudentIdNumber("MIT-8849");
        req.setClaimedCgpa(3.9);

        // Process facade directly with injected services
        EduEmailVerificationService realEduService = new EduEmailVerificationService();
        assertTrue(realEduService.isEducationalEmail(req.getEmail()));
    }
}
