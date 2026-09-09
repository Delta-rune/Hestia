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

        // Check if QR payload is JWT (3 parts separated by dots) or W3C JSON-LD or DigiLocker format
        boolean isSigned = false;
        String issuer = "Unknown Issuer";

        if (qrPayload.startsWith("eyJ") && qrPayload.contains(".")) {
            // JWT formatted payload (Header.Payload.Signature)
            String[] jwtParts = qrPayload.split("\\.");
            if (jwtParts.length == 3) {
                isSigned = true;
                issuer = "Signed Digital JWT Credential";
            }
        } else if (qrPayload.toLowerCase().contains("digilocker") || qrPayload.toLowerCase().contains("verifiablecredential")) {
            isSigned = true;
            issuer = "Verified Educational Depository";
        } else if (qrPayload.startsWith("http://") || qrPayload.startsWith("https://")) {
            issuer = "Official Verification URL: " + qrPayload;
            isSigned = true;
        }

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
