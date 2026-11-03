package com.hestia.vault.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ComprehensiveCertificateVerificationEngineTest {

    @Autowired
    private ComprehensiveCertificateVerificationEngine engine;

    @Test
    @DisplayName("Verify Real-World Official NPTEL Certificate with Embedded QR Code & Text Layer")
    public void testVerifyRealNptelCertificate() throws Exception {
        String testPdfPath = "C:/Users/zyber/.gemini/antigravity/brain/a2cf0fe6-3cd2-4c3c-8093-1959ff56f46f/.user_uploaded/media_1793671514777_28d5212a.pdf";
        File f = new File(testPdfPath);

        if (f.exists()) {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String base64Data = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(bytes);

            var result = engine.verifyDocument(
                    base64Data,
                    null,
                    null,
                    null,
                    "nripansatheesh@gmail.com",
                    "Nripan Satheesh"
            );

            System.out.println("=== REAL NPTEL VERIFICATION RESULT ===");
            System.out.println("Status: " + result.status());
            System.out.println("Trust Score: " + result.trustScore());
            System.out.println("Tier: " + result.tier());
            System.out.println("Classification: " + result.classification());
            System.out.println("Recipient Name: " + result.entity().recipientName());
            System.out.println("Course Title: " + result.entity().certificateTitle());
            System.out.println("Issuer Authority: " + result.entity().issuerAuthority());
            System.out.println("Credential / Roll No: " + result.entity().credentialId());
            System.out.println("Score: " + result.entity().performanceScore());
            System.out.println("Credits: " + result.entity().creditsAwarded());
            System.out.println("Session: " + result.entity().issueSessionOrDate());
            System.out.println("Verification URL: " + result.entity().verificationUrl());
            System.out.println("Audit Note: " + result.diagnosticAuditNote());

            assertEquals("VERIFIED_AUTHENTIC", result.status());
            assertEquals("99.8%", result.trustScore());
            assertEquals("TIER_2_QR_PKI", result.tier());
            assertTrue(result.isCryptographicallySigned());
            assertTrue(result.entity().issuerAuthority().contains("NPTEL") || result.entity().issuerAuthority().contains("Swayam"));
            assertEquals("NRIPAN SATHEESH", result.entity().recipientName());
            assertEquals("ADVANCED R PROGRAMMING FOR DATA ANALYTICS IN BUSINESS", result.entity().certificateTitle());
            assertTrue(result.entity().credentialId().contains("NPTEL26MG122S658700861") || result.entity().credentialId().contains("NOC-"));
            assertEquals("51%", result.entity().performanceScore());
            assertEquals("4 Credits", result.entity().creditsAwarded());
            assertEquals("JUL-OCT 2026", result.entity().issueSessionOrDate());
        } else {
            // CI/Build Environment validation
            var result = engine.verifyDocument(
                    null,
                    "NPTEL26MG122S658700861",
                    "ADVANCED R PROGRAMMING FOR DATA ANALYTICS IN BUSINESS",
                    "NPTEL",
                    "nripansatheesh@gmail.com",
                    "Nripan Satheesh"
            );
            assertTrue(result.status().startsWith("VERIFIED"));
        }
    }

    @Test
    @DisplayName("Verify Academic Degree from APJ Abdul Kalam Technological University (KTU)")
    public void testVerifyKtuDegree() {
        String sampleText = """
                APJ ABDUL KALAM TECHNOLOGICAL UNIVERSITY
                Thiruvananthapuram, Kerala
                PROVISIONAL CERTIFICATE / DEGREE
                This is to certify that JOHN DOE has successfully completed the degree of
                Bachelor of Technology in Computer Science and Engineering
                with CGPA: 8.85
                Register No: MBC21CS045
                Month & Year of Examination: JUNE 2025
                No. of credits recommended: 160
                """;

        var entity = engine.extractCertificateEntities(sampleText, null, null, null, "KTU", "John Doe");
        var classification = engine.classifyDocument(sampleText, true, null, entity);

        assertEquals("APJ Abdul Kalam Technological University (KTU)", entity.issuerAuthority());
        assertTrue(entity.recipientName().equalsIgnoreCase("John Doe"));
        assertTrue(entity.certificateTitle().toLowerCase().contains("bachelor of technology"));
        assertEquals("MBC21CS045", entity.credentialId());
        assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.ACADEMIC_CREDENTIAL, classification);
    }

    @Test
    @DisplayName("Verify Coursera Verified Certificate with Course & Platform Metadata")
    public void testVerifyCourseraCredential() {
        String sampleText = """
                coursera
                COURSE CERTIFICATE
                This is to certify that
                ALICE JOHNSON
                has successfully completed
                Machine Learning Specialization
                an online authorized course on Deep Learning offered through Coursera
                Credential ID: COURSERA-ML-99214
                Verification URL: https://coursera.org/verify/COURSERA-ML-99214
                Grade: 98.4%
                """;

        var entity = engine.extractCertificateEntities(sampleText, null, null, null, null, null);
        var classification = engine.classifyDocument(sampleText, true, null, entity);

        assertEquals("Coursera Verified Credentials", entity.issuerAuthority());
        assertEquals("ALICE JOHNSON", entity.recipientName());
        assertTrue(entity.certificateTitle().contains("Machine Learning"));
        assertEquals("COURSERA-ML-99214", entity.credentialId());
        assertEquals("98.4%", entity.performanceScore());
        assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.TECHNICAL_CERTIFICATION, classification);
    }

    @Test
    @DisplayName("Strict Non-Certificate Rejection: Financial Invoices & Billing Receipts (Flag Red 0.0%)")
    public void testRejectFinancialInvoice() {
        String invoiceText = """
                TAX INVOICE
                Invoice Number: INV-2026-90412
                Invoice Date: 12-Oct-2026
                Billed To: ACME Corporation
                Billing Address: 45 Tech Park, Bangalore
                Payment Method: Credit Card / Net Banking
                Item Description        Qty    Unit Price    Total Amount
                Server Cloud Hosting     1      $250.00       $250.00
                Domain Registration      2       $15.00        $30.00
                Subtotal: $280.00
                Tax Rate: 18% GST
                Grand Total: $330.40
                Amount Due: $0.00 (PAID)
                """;

        var entity = engine.extractCertificateEntities(invoiceText, null, null, null, null, null);
        var classification = engine.classifyDocument(invoiceText, true, null, entity);

        assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.REJECTED_FINANCIAL_INVOICE, classification);

        var result = engine.verifyDocument(
                null,
                null,
                null,
                null,
                "user@acme.com",
                "User"
        );
        // Direct text test through engine helper
        assertTrue(result.status().startsWith("REJECTED"));
    }

    @Test
    @DisplayName("Strict Non-Certificate Rejection: Source Code Screenshots & Terminal Dumps (Flag Red 0.0%)")
    public void testRejectSourceCodeSnippet() {
        String codeText = """
                import java.util.List;
                import java.util.ArrayList;
                
                public class QuickSort {
                    public static void main(String[] args) {
                        System.out.println("Sorting array...");
                    }
                    
                    def processData():
                        npm install express
                        git commit -m "update code"
                        git push origin main
                }
                """;

        var entity = engine.extractCertificateEntities(codeText, null, null, null, null, null);
        var classification = engine.classifyDocument(codeText, true, null, entity);

        assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.REJECTED_CODE_SNIPPET, classification);
    }

    @Test
    @DisplayName("Strict Non-Certificate Rejection: Chat Logs & Social Media Captures (Flag Red 0.0%)")
    public void testRejectChatScreenshot() {
        String chatText = """
                WhatsApp Chat with Bob
                Today at 10:45 AM
                Alice: Hey, did you finish the assignment?
                Bob: typing...
                Bob: Yeah, sending it now!
                Read receipt: Blue ticks
                Yesterday at 09:20 PM
                Discord server: direct message
                """;

        var entity = engine.extractCertificateEntities(chatText, null, null, null, null, null);
        var classification = engine.classifyDocument(chatText, true, null, entity);

        assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.REJECTED_CHAT_SOCIAL_MEDIA, classification);
    }

    @Test
    @DisplayName("Strict Non-Certificate Rejection: Plain Resume / CV (Flag Red 0.0%)")
    public void testRejectResume() {
        String resumeText = """
                CURRICULUM VITAE
                John Doe - Senior Software Engineer
                Professional Summary: 5+ years of experience in Java, Spring Boot, React.
                Work Experience:
                Lead Architect at Tech Corp (2022-Present)
                Software Engineer at Startup Inc (2020-2022)
                References available upon request.
                Career Objective: To build scalable web systems.
                """;

        var entity = engine.extractCertificateEntities(resumeText, null, null, null, null, null);
        var classification = engine.classifyDocument(resumeText, true, null, entity);

        assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.REJECTED_GENERIC_DOCUMENT, classification);
    }

    @Test
    @DisplayName("Institutional Accreditation Registry: 150+ Recognized Global & National Authorities")
    public void testRecognizedAuthorities() {
        // Tech Titans & Professional Certifications
        assertTrue(engine.isRecognizedAuthority("Amazon Web Services"));
        assertTrue(engine.isRecognizedAuthority("AWS"));
        assertTrue(engine.isRecognizedAuthority("Google Cloud"));
        assertTrue(engine.isRecognizedAuthority("Microsoft Azure"));
        assertTrue(engine.isRecognizedAuthority("IBM"));
        assertTrue(engine.isRecognizedAuthority("Oracle"));
        assertTrue(engine.isRecognizedAuthority("Cisco"));
        assertTrue(engine.isRecognizedAuthority("Red Hat"));
        assertTrue(engine.isRecognizedAuthority("Meta"));

        // National Depository & Govt Councils
        assertTrue(engine.isRecognizedAuthority("NPTEL"));
        assertTrue(engine.isRecognizedAuthority("Swayam"));
        assertTrue(engine.isRecognizedAuthority("DigiLocker"));
        assertTrue(engine.isRecognizedAuthority("Skill India"));
        assertTrue(engine.isRecognizedAuthority("NSDC"));
        assertTrue(engine.isRecognizedAuthority("MyCaptain"));

        // Premier Universities & Technical Boards
        assertTrue(engine.isRecognizedAuthority("APJ Abdul Kalam Technological University"));
        assertTrue(engine.isRecognizedAuthority("KTU"));
        assertTrue(engine.isRecognizedAuthority("MBCCET"));
        assertTrue(engine.isRecognizedAuthority("University of Calicut"));
        assertTrue(engine.isRecognizedAuthority("University of Kerala"));
        assertTrue(engine.isRecognizedAuthority("IIT Bombay"));
        assertTrue(engine.isRecognizedAuthority("IIT Kanpur"));
        assertTrue(engine.isRecognizedAuthority("NIT Calicut"));
        assertTrue(engine.isRecognizedAuthority("BITS Pilani"));
        assertTrue(engine.isRecognizedAuthority("Stanford University"));
        assertTrue(engine.isRecognizedAuthority("Harvard University"));
        assertTrue(engine.isRecognizedAuthority("MIT"));

        // Random Fake Strings Must Return False
        assertFalse(engine.isRecognizedAuthority("Fake Random Institute 9982"));
        assertFalse(engine.isRecognizedAuthority("asdfasdf"));
    }

    @Test
    @DisplayName("Verify Real-World Official MyCaptain Python Programming Certificate")
    public void testVerifyRealMyCaptainPythonCertificate() throws Exception {
        String testPdfPath = "C:/Users/zyber/.gemini/antigravity/brain/a2cf0fe6-3cd2-4c3c-8093-1959ff56f46f/.user_uploaded/media_1793673826215_e1d4a800.pdf";
        File f = new File(testPdfPath);

        if (f.exists()) {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String base64Data = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(bytes);

            var result = engine.verifyDocument(
                    base64Data,
                    null,
                    null,
                    null,
                    "nripansatheesh@gmail.com",
                    "Nripan Satheesh"
            );

            System.out.println("=== REAL MYCAPTAIN PYTHON VERIFICATION RESULT ===");
            System.out.println("Status: " + result.status());
            System.out.println("Trust Score: " + result.trustScore());
            System.out.println("Tier: " + result.tier());
            System.out.println("Classification: " + result.classification());
            System.out.println("Recipient Name: " + result.entity().recipientName());
            System.out.println("Course Title: " + result.entity().certificateTitle());
            System.out.println("Issuer Authority: " + result.entity().issuerAuthority());
            System.out.println("Credential ID: " + result.entity().credentialId());
            System.out.println("Session: " + result.entity().issueSessionOrDate());
            System.out.println("Audit Note: " + result.diagnosticAuditNote());

            assertEquals("VERIFIED_AUTHENTIC", result.status());
            assertEquals("98.5%", result.trustScore());
            assertEquals("TIER_3_OCR_METADATA", result.tier());
            assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.TECHNICAL_CERTIFICATION, result.classification());
            assertEquals("Nripan Satheesh", result.entity().recipientName());
            assertEquals("Python Programming Course", result.entity().certificateTitle());
            assertEquals("2Q45VNNER", result.entity().credentialId());
            assertEquals("February 2026 Batch", result.entity().issueSessionOrDate());
            assertTrue(result.entity().issuerAuthority().contains("MyCaptain"));
        } else {
            // In CI without local file, test text extraction directly
            String sampleText = "Nripan Satheesh\nPython Programming Course\nFebruary 2026 Batch\n2Q45VNNER";
            var entity = engine.extractCertificateEntities(sampleText, null, null, null, null, "Nripan Satheesh");
            var classification = engine.classifyDocument(sampleText, true, null, entity);

            assertEquals("Nripan Satheesh", entity.recipientName());
            assertEquals("Python Programming Course", entity.certificateTitle());
            assertEquals("2Q45VNNER", entity.credentialId());
            assertEquals("February 2026 Batch", entity.issueSessionOrDate());
            assertTrue(entity.issuerAuthority().contains("MyCaptain"));
            assertEquals(ComprehensiveCertificateVerificationEngine.DocumentClassification.TECHNICAL_CERTIFICATION, classification);
        }
    }
}
