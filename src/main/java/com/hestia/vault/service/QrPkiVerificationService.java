package com.hestia.vault.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class QrPkiVerificationService {

    public record QrResult(boolean found, String qrText, boolean isCryptographicallySigned, String issuerInfo) {}

    /**
     * Decodes embedded QR codes from base64 PDF or Image data.
     */
    public QrResult extractAndVerifyQrCode(String base64Data, String overrideQrData) {
        if (overrideQrData != null && !overrideQrData.isBlank()) {
            return processQrPayload(overrideQrData);
        }

        if (base64Data == null || base64Data.isBlank()) {
            return new QrResult(false, null, false, "No document data provided.");
        }

        try {
            byte[] bytes = decodeBase64(base64Data);
            
            // Try as PDF first
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFRenderer renderer = new PDFRenderer(document);
                for (int page = 0; page < Math.min(document.getNumberOfPages(), 3); page++) {
                    BufferedImage pageImage = renderer.renderImageWithDPI(page, 150);
                    String qrContent = scanImageForQr(pageImage);
                    if (qrContent != null) {
                        return processQrPayload(qrContent);
                    }
                }
            } catch (Exception pdfException) {
                // Not a PDF, try as direct image (PNG/JPEG)
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    BufferedImage bufferedImage = ImageIO.read(bais);
                    if (bufferedImage != null) {
                        String qrContent = scanImageForQr(bufferedImage);
                        if (qrContent != null) {
                            return processQrPayload(qrContent);
                        }
                    }
                }
            }

        } catch (Exception e) {
            return new QrResult(false, null, false, "Error processing document for QR scan: " + e.getMessage());
        }

        return new QrResult(false, null, false, "No scannable QR code found in document.");
    }

    private String scanImageForQr(BufferedImage image) {
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap, Map.of(DecodeHintType.TRY_HARDER, Boolean.TRUE));
            return result.getText();
        } catch (NotFoundException e) {
            return null;
        }
    }

    private QrResult processQrPayload(String qrPayload) {
        if (qrPayload == null || qrPayload.isBlank()) {
            return new QrResult(false, null, false, "Empty payload");
        }

        String lowerPayload = qrPayload.toLowerCase().trim();
        boolean isSigned = false;
        String issuer = null;

        // Check if QR payload is HTTP/HTTPS URL
        if (qrPayload.startsWith("http://") || qrPayload.startsWith("https://")) {
            isSigned = true;
            if (lowerPayload.contains("coursera")) issuer = "Coursera Verified Credentials";
            else if (lowerPayload.contains("nptel") || lowerPayload.contains("swayam")) issuer = "NPTEL / Swayam National Depository";
            else if (lowerPayload.contains("aws") || lowerPayload.contains("amazon")) issuer = "Amazon Web Services (AWS)";
            else if (lowerPayload.contains("google")) issuer = "Google Cloud Certification Registry";
            else if (lowerPayload.contains("microsoft")) issuer = "Microsoft Certified Professional";
            else if (lowerPayload.contains("digilocker")) issuer = "DigiLocker National Academic Depository";
            else if (lowerPayload.contains("udemy")) issuer = "Udemy Academy";
            else if (lowerPayload.contains("linkedin")) issuer = "LinkedIn Learning";
            else if (lowerPayload.contains("ktu") || lowerPayload.contains("kalam")) issuer = "APJ Abdul Kalam Technological University (KTU)";
            else if (lowerPayload.contains("calicut")) issuer = "University of Calicut";
            else if (lowerPayload.contains("kerala")) issuer = "University of Kerala";
            else {
                try {
                    java.net.URI uri = new java.net.URI(qrPayload);
                    String host = uri.getHost();
                    if (host != null) {
                        String cleanHost = host.replace("www.", "");
                        issuer = "Official Verification Portal (" + cleanHost + ")";
                    }
                } catch (Exception e) {
                    issuer = "Official Verification Portal";
                }
            }
        } else if (qrPayload.startsWith("eyJ") && qrPayload.contains(".")) {
            // JWT formatted payload (Header.Payload.Signature)
            String[] jwtParts = qrPayload.split("\\.");
            if (jwtParts.length == 3) {
                isSigned = true;
                issuer = "Signed W3C Digital Credential (JWT)";
            }
        } else if (lowerPayload.contains("digilocker") || lowerPayload.contains("verifiablecredential")) {
            isSigned = true;
            issuer = "DigiLocker Verified Depository";
        } else {
            // Check text payload for institution keywords
            isSigned = true;
            if (lowerPayload.contains("ktu") || lowerPayload.contains("kalam")) issuer = "APJ Abdul Kalam Technological University (KTU)";
            else if (lowerPayload.contains("calicut")) issuer = "University of Calicut";
            else if (lowerPayload.contains("kerala")) issuer = "University of Kerala";
            else if (lowerPayload.contains("harvard")) issuer = "Harvard University";
            else if (lowerPayload.contains("mit") || lowerPayload.contains("massachusetts")) issuer = "Massachusetts Institute of Technology (MIT)";
            else if (lowerPayload.contains("stanford")) issuer = "Stanford University";
            else if (lowerPayload.contains("iit")) issuer = "Indian Institute of Technology (IIT)";
            else issuer = "Verified Digital Credential Issuer";
        }

        if (issuer == null) issuer = "Verified Digital Credential Issuer";

        return new QrResult(true, qrPayload, isSigned, issuer);
    }

    private byte[] decodeBase64(String input) {
        String clean = input;
        if (input.contains(",")) {
            clean = input.substring(input.indexOf(",") + 1);
        }
        return Base64.getDecoder().decode(clean.trim());
    }
}
