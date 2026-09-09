package com.hestia.vault.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentAuditService {

    public record DocumentAuditResult(
            boolean isPdf,
            boolean metadataTamperWarning,
            List<String> detectedEditingTools,
            String extractedText,
            boolean studentIdMatched,
            boolean cgpaMatched,
            int auditScore
    ) {}

    private static final List<String> EDITING_TOOLS = List.of(
            "photoshop", "gimp", "canva", "pdfedit", "inkscape", "illustrator", "paint.net"
    );

    public DocumentAuditResult auditDocument(String base64Data, String claimedStudentId, Double claimedCgpa, String claimedInstitution) {
        if (base64Data == null || base64Data.isBlank()) {
            return new DocumentAuditResult(false, false, List.of(), "", false, false, 0);
        }

        byte[] data;
        try {
            data = decodeBase64(base64Data);
        } catch (Exception e) {
            return new DocumentAuditResult(false, false, List.of("Invalid Base64 Data"), "", false, false, 0);
        }

        List<String> warnings = new ArrayList<>();
        boolean isPdf = false;
        boolean tamperWarning = false;
        String extractedText = "";

        try (PDDocument pdfDocument = Loader.loadPDF(data)) {
            isPdf = true;
            PDDocumentInformation info = pdfDocument.getDocumentInformation();
            
            String creator = info.getCreator() != null ? info.getCreator().toLowerCase() : "";
            String producer = info.getProducer() != null ? info.getProducer().toLowerCase() : "";
            
            for (String tool : EDITING_TOOLS) {
                if (creator.contains(tool) || producer.contains(tool)) {
                    tamperWarning = true;
                    warnings.add("Document created or modified using image editing software: " + tool);
                }
            }

            PDFTextStripper stripper = new PDFTextStripper();
            extractedText = stripper.getText(pdfDocument);

        } catch (Exception pdfException) {
            // Document might be a plain image, not a PDF
            isPdf = false;
        }

        // Match claims against extracted text
        boolean studentIdMatched = false;
        if (claimedStudentId != null && !claimedStudentId.isBlank() && !extractedText.isBlank()) {
            studentIdMatched = extractedText.toLowerCase().contains(claimedStudentId.trim().toLowerCase());
        }

        boolean cgpaMatched = false;
        if (claimedCgpa != null && !extractedText.isBlank()) {
            String cgpaStr = String.valueOf(claimedCgpa);
            if (extractedText.contains(cgpaStr)) {
                cgpaMatched = true;
            } else {
                // Try matching with regex for CGPA / Grade patterns
                Pattern pattern = Pattern.compile("(?i)(cgpa|gpa|marks|grade)\\s*[:=]?\\s*([0-9]+\\.[0-9]+)");
                Matcher matcher = pattern.matcher(extractedText);
                if (matcher.find()) {
                    try {
                        double foundCgpa = Double.parseDouble(matcher.group(2));
                        if (Math.abs(foundCgpa - claimedCgpa) < 0.1) {
                            cgpaMatched = true;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }

        // Calculate audit confidence score
        int score = 50; // base score for uploading a readable document
        if (isPdf) score += 15;
        if (!tamperWarning) score += 20; else score -= 30;
        if (studentIdMatched) score += 15;
        if (cgpaMatched) score += 10;

        score = Math.max(0, Math.min(100, score));

        return new DocumentAuditResult(isPdf, tamperWarning, warnings, extractedText, studentIdMatched, cgpaMatched, score);
    }

    private byte[] decodeBase64(String input) {
        String clean = input;
        if (input.contains(",")) {
            clean = input.substring(input.indexOf(",") + 1);
        }
        return Base64.getDecoder().decode(clean.trim());
    }
}
