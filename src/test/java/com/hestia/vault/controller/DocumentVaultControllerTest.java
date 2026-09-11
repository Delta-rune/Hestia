package com.hestia.vault.controller;

import com.hestia.vault.model.DocumentVault;
import com.hestia.vault.repository.DocumentVaultRepository;
import com.hestia.vault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DocumentVaultControllerTest {

    @Mock
    private DocumentVaultRepository documentVaultRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentVaultController documentVaultController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUploadDocumentSuccess() {
        String base64Data = Base64.getEncoder().encodeToString("Sample Certificate Content".getBytes());
        Map<String, Object> req = new HashMap<>();
        req.put("userId", "1");
        req.put("userEmail", "student@ktu.edu");
        req.put("documentType", "CERTIFICATE");
        req.put("title", "Google Cloud Associate Engineer");
        req.put("issuer", "Google");
        req.put("fileName", "gcp_cert.pdf");
        req.put("contentType", "application/pdf");
        req.put("fileData", base64Data);
        req.put("credentialId", "GCP-102938");

        DocumentVault savedDoc = new DocumentVault(1L, "student@ktu.edu", "CERTIFICATE",
                "Google Cloud Associate Engineer", "Google", "gcp_cert.pdf", "application/pdf",
                25L, base64Data, "sha256:abc12345", "GCP-102938");
        savedDoc.setId(10L);

        when(documentVaultRepository.findByUserIdAndFileHash(anyLong(), anyString())).thenReturn(Optional.empty());
        when(documentVaultRepository.save(any(DocumentVault.class))).thenReturn(savedDoc);

        ResponseEntity<?> response = documentVaultController.uploadDocument(req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("SUCCESS", body.get("status"));
        assertEquals(10L, body.get("documentId"));
    }

    @Test
    void testListDocuments() {
        DocumentVault doc = new DocumentVault(1L, "student@ktu.edu", "GRADE_CARD",
                "Semester 1 Grade Card", "KTU", "S1.pdf", "application/pdf",
                1024L, "dummy", "sha256:1122", "KTU-1234");
        doc.setId(1L);

        when(documentVaultRepository.findByUserIdOrderByUploadedAtDesc(1L)).thenReturn(List.of(doc));

        ResponseEntity<?> response = documentVaultController.listDocuments("1", null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof List);
        List<?> list = (List<?>) response.getBody();
        assertEquals(1, list.size());
    }

    @Test
    void testViewDocumentFile() {
        String content = "%PDF-1.4 Mock PDF Content";
        String base64 = Base64.getEncoder().encodeToString(content.getBytes());
        DocumentVault doc = new DocumentVault(1L, "student@ktu.edu", "CERTIFICATE",
                "Test Cert", "Google", "cert.pdf", "application/pdf",
                (long) content.getBytes().length, base64, "sha256:999", "CR-123");
        doc.setId(5L);

        when(documentVaultRepository.findById(5L)).thenReturn(Optional.of(doc));

        ResponseEntity<byte[]> response = documentVaultController.viewDocumentFile(5L);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(content, new String(response.getBody()));
    }
}
