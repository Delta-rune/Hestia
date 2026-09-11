package com.hestia.vault.controller;

import com.hestia.vault.model.DocumentVault;
import com.hestia.vault.model.User;
import com.hestia.vault.repository.DocumentVaultRepository;
import com.hestia.vault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentVaultController {

    private final DocumentVaultRepository documentVaultRepository;
    private final UserRepository userRepository;

    @Autowired
    public DocumentVaultController(DocumentVaultRepository documentVaultRepository,
                                   UserRepository userRepository) {
        this.documentVaultRepository = documentVaultRepository;
        this.userRepository = userRepository;
    }

    /**
     * POST /api/documents/upload
     * Upload and permanently store a Certificate or Grade Card PDF/image.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestBody Map<String, Object> request) {
        try {
            String userIdStr = request.get("userId") != null ? request.get("userId").toString() : null;
            String userEmail = request.get("userEmail") != null ? request.get("userEmail").toString() : "";
            if ((userIdStr == null || userIdStr.isBlank()) && !userEmail.isBlank()) {
                userIdStr = userEmail;
            }
            Long userId = parseUserId(userIdStr != null ? userIdStr : "1");

            String documentType = request.get("documentType") != null ? request.get("documentType").toString() : "CERTIFICATE";
            String title = request.get("title") != null ? request.get("title").toString() : "Verified Document";
            String issuer = request.get("issuer") != null ? request.get("issuer").toString() : "Academic Authority";
            String fileName = request.get("fileName") != null ? request.get("fileName").toString() : "document.pdf";
            String contentType = request.get("contentType") != null ? request.get("contentType").toString() : "application/pdf";
            String fileData = (String) request.get("fileData");
            String credentialId = request.get("credentialId") != null ? request.get("credentialId").toString() : null;

            if (fileData == null || fileData.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File data is required."));
            }

            // Clean Base64 data
            String cleanData = fileData;
            if (cleanData.contains(",")) {
                cleanData = cleanData.substring(cleanData.indexOf(",") + 1);
            }
            cleanData = cleanData.replaceAll("\\s+", "");

            byte[] decodedBytes;
            try {
                decodedBytes = Base64.getMimeDecoder().decode(cleanData);
            } catch (Exception e) {
                decodedBytes = Base64.getDecoder().decode(cleanData);
            }

            // Compute SHA-256 fingerprint
            String fileHash = computeSha256(decodedBytes);

            // Calculate file size
            long sizeBytes = decodedBytes.length;

            // Check if this file hash was already uploaded by user
            Optional<DocumentVault> existingOpt = documentVaultRepository.findByUserIdAndFileHash(userId, fileHash);
            DocumentVault doc;
            if (existingOpt.isPresent()) {
                doc = existingOpt.get();
                doc.setTitle(title);
                doc.setIssuer(issuer);
                doc.setFileName(fileName);
                doc.setContentType(contentType);
                doc.setFileSizeBytes(sizeBytes);
                doc.setFileData(cleanData);
                if (credentialId != null) doc.setCredentialId(credentialId);
                doc = documentVaultRepository.save(doc);
            } else {
                doc = new DocumentVault(userId, userEmail, documentType, title, issuer,
                        fileName, contentType, sizeBytes, cleanData, fileHash, credentialId);
                doc = documentVaultRepository.save(doc);
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Document stored securely in Cloud Vault.");
            response.put("documentId", doc.getId());
            response.put("title", doc.getTitle());
            response.put("issuer", doc.getIssuer());
            response.put("fileName", doc.getFileName());
            response.put("fileSizeBytes", doc.getFileSizeBytes());
            response.put("fileHash", doc.getFileHash());
            response.put("credentialId", doc.getCredentialId());
            response.put("uploadedAt", doc.getUploadedAt());
            response.put("viewUrl", "/api/documents/file/" + doc.getId());

            return ResponseEntity.ok(response);
        } catch (Throwable t) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to store document in cloud vault: " + t.getMessage()));
        }
    }

    /**
     * GET /api/documents?userId=...&email=...&type=...
     * Lists all documents stored for the given user.
     */
    @GetMapping
    public ResponseEntity<?> listDocuments(@RequestParam(required = false) String userId,
                                           @RequestParam(required = false) String email,
                                           @RequestParam(required = false) String type) {
        try {
            Long parsedUserId = parseUserId(userId != null ? userId : (email != null ? email : "1"));
            List<DocumentVault> docs;
            if (type != null && !type.isBlank()) {
                docs = documentVaultRepository.findByUserIdAndDocumentTypeOrderByUploadedAtDesc(parsedUserId, type.toUpperCase());
            } else {
                docs = documentVaultRepository.findByUserIdOrderByUploadedAtDesc(parsedUserId);
            }

            List<Map<String, Object>> result = new ArrayList<>();
            for (DocumentVault d : docs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", d.getId());
                item.put("userId", d.getUserId());
                item.put("documentType", d.getDocumentType());
                item.put("title", d.getTitle());
                item.put("issuer", d.getIssuer());
                item.put("fileName", d.getFileName());
                item.put("contentType", d.getContentType());
                item.put("fileSizeBytes", d.getFileSizeBytes());
                item.put("fileHash", d.getFileHash());
                item.put("credentialId", d.getCredentialId());
                item.put("verificationStatus", d.getVerificationStatus());
                item.put("uploadedAt", d.getUploadedAt());
                item.put("viewUrl", "/api/documents/file/" + d.getId());
                result.add(item);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }

    /**
     * GET /api/documents/{id}
     * Retrieves metadata for a specific document.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDocumentMetadata(@PathVariable Long id) {
        return documentVaultRepository.findById(id)
                .map(d -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", d.getId());
                    item.put("title", d.getTitle());
                    item.put("issuer", d.getIssuer());
                    item.put("fileName", d.getFileName());
                    item.put("contentType", d.getContentType());
                    item.put("fileSizeBytes", d.getFileSizeBytes());
                    item.put("fileHash", d.getFileHash());
                    item.put("credentialId", d.getCredentialId());
                    item.put("uploadedAt", d.getUploadedAt());
                    item.put("viewUrl", "/api/documents/file/" + d.getId());
                    return ResponseEntity.ok((Object) item);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Document not found.")));
    }

    /**
     * GET /api/documents/file/{id}
     * Streams the decoded PDF or image directly for inline viewing or download in browser.
     */
    @GetMapping("/file/{id}")
    public ResponseEntity<byte[]> viewDocumentFile(@PathVariable Long id) {
        Optional<DocumentVault> docOpt = documentVaultRepository.findById(id);
        if (docOpt.isEmpty() || docOpt.get().getFileData() == null) {
            return ResponseEntity.notFound().build();
        }

        DocumentVault doc = docOpt.get();
        try {
            byte[] bytes = Base64.getMimeDecoder().decode(doc.getFileData());
            MediaType mediaType;
            try {
                mediaType = MediaType.parseMediaType(doc.getContentType() != null ? doc.getContentType() : "application/pdf");
            } catch (Exception ignored) {
                mediaType = MediaType.APPLICATION_PDF;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(mediaType);
            headers.setContentLength(bytes.length);
            String safeFileName = doc.getFileName() != null ? doc.getFileName().replace("\"", "") : "document.pdf";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeFileName + "\"");

            return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/documents/{id}
     * Deletes a document from the cloud vault.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        if (documentVaultRepository.existsById(id)) {
            documentVaultRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Document removed from cloud vault."));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Document not found."));
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) return 1L;
        String clean = userIdStr.trim();
        if (clean.contains("@") && userRepository != null) {
            Optional<User> u = userRepository.findByEmail(clean.toLowerCase());
            if (u.isPresent()) return u.get().getId();
        }
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            if (userRepository != null) {
                Optional<User> u = userRepository.findByUsername(clean);
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

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return "sha256:" + hexString.toString();
        } catch (Exception e) {
            return "sha256:" + UUID.randomUUID();
        }
    }
}
