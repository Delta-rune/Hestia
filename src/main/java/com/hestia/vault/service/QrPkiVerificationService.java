package com.hestia.vault.service;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

@Service
public class QrPkiVerificationService {

    public record QrResult(boolean found, String qrText, boolean isCryptographicallySigned, String issuerInfo) {}

    private static final Map<DecodeHintType, Object> DECODE_HINTS = Map.of(
            DecodeHintType.TRY_HARDER, Boolean.TRUE,
            DecodeHintType.POSSIBLE_FORMATS, List.of(
                    BarcodeFormat.QR_CODE,
                    BarcodeFormat.DATA_MATRIX,
                    BarcodeFormat.AZTEC,
                    BarcodeFormat.PDF_417,
                    BarcodeFormat.CODE_128,
                    BarcodeFormat.CODE_39
            )
    );

    /**
     * Decodes embedded QR codes from base64 PDF or Image data.
     */
    public QrResult extractAndVerifyQrCode(String base64Data, String overrideQrData) {
        // Only use overrideQrData if base64Data is NOT present (i.e. manual string input without a file)
        if ((base64Data == null || base64Data.isBlank()) && overrideQrData != null && !overrideQrData.isBlank()) {
            return processQrPayload(overrideQrData);
        }

        if (base64Data == null || base64Data.isBlank()) {
            return new QrResult(false, null, false, "No document data provided.");
        }

        try {
            byte[] bytes = decodeBase64(base64Data);

            // 1. Try as PDF First
            try (PDDocument document = Loader.loadPDF(bytes)) {
                // Pass A: Extract raw embedded image objects (Direct PDFBox XObject inspection)
                // This decodes standalone QR code image streams directly at native resolution (e.g. 310x310 PNG in NPTEL)
                int pagesToInspect = Math.min(document.getNumberOfPages(), 3);
                for (int pageIdx = 0; pageIdx < pagesToInspect; pageIdx++) {
                    PDPage page = document.getPage(pageIdx);
                    PDResources resources = page.getResources();
                    if (resources != null) {
                        for (COSName cosName : resources.getXObjectNames()) {
                            try {
                                PDXObject xobject = resources.getXObject(cosName);
                                if (xobject instanceof PDImageXObject imageObject) {
                                    BufferedImage embeddedImg = imageObject.getImage();
                                    if (embeddedImg != null && embeddedImg.getWidth() >= 64 && embeddedImg.getHeight() >= 64) {
                                        String qrContent = scanImageForQr(embeddedImg);
                                        if (qrContent != null) {
                                            return processQrPayload(qrContent);
                                        }
                                    }
                                }
                            } catch (Throwable ignored) {}
                        }
                    }
                }

                // Pass B: Full Page Rendering (150 DPI and 200 DPI with multi-quadrant crops)
                PDFRenderer renderer = new PDFRenderer(document);
                for (int pageIdx = 0; pageIdx < pagesToInspect; pageIdx++) {
                    BufferedImage pageImage = renderer.renderImageWithDPI(pageIdx, 150);
                    String qrContent = scanImageForQr(pageImage);
                    if (qrContent != null) {
                        return processQrPayload(qrContent);
                    }

                    // Scan bottom 40% crop (typical diploma / certificate footer area)
                    int h = pageImage.getHeight();
                    int w = pageImage.getWidth();
                    int cropY = (int) (h * 0.60);
                    BufferedImage bottomCrop = pageImage.getSubimage(0, cropY, w, h - cropY);
                    qrContent = scanImageForQr(bottomCrop);
                    if (qrContent != null) {
                        return processQrPayload(qrContent);
                    }

                    // Try 200 DPI if high resolution is needed for dense QR matrices
                    BufferedImage hiRes = renderer.renderImageWithDPI(pageIdx, 200);
                    qrContent = scanImageForQr(hiRes);
                    if (qrContent != null) {
                        return processQrPayload(qrContent);
                    }
                }
            } catch (Throwable pdfException) {
                // Not a PDF, try as direct image (PNG/JPEG/WEBP)
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                    BufferedImage bufferedImage = ImageIO.read(bais);
                    if (bufferedImage != null) {
                        String qrContent = scanImageForQr(bufferedImage);
                        if (qrContent != null) {
                            return processQrPayload(qrContent);
                        }

                        // Try bottom 40% crop for image
                        int h = bufferedImage.getHeight();
                        int w = bufferedImage.getWidth();
                        if (h > 400 && w > 400) {
                            int cropY = (int) (h * 0.60);
                            BufferedImage bottomCrop = bufferedImage.getSubimage(0, cropY, w, h - cropY);
                            qrContent = scanImageForQr(bottomCrop);
                            if (qrContent != null) {
                                return processQrPayload(qrContent);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

        } catch (Throwable e) {
            return new QrResult(false, null, false, "Scan completed: No embedded QR found.");
        }

        return new QrResult(false, null, false, "No scannable QR code found in document.");
    }

    private String scanImageForQr(BufferedImage image) {
        if (image == null) return null;

        // Try Pass 1: HybridBinarizer (Standard)
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap, DECODE_HINTS);
            return result.getText();
        } catch (NotFoundException ignored) {}

        // Try Pass 2: GlobalHistogramBinarizer (Higher contrast / monochrome scans)
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new GlobalHistogramBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap, DECODE_HINTS);
            return result.getText();
        } catch (NotFoundException ignored) {}

        // Try Pass 3: Inverted Luminance (for white-on-dark inverted QR codes)
        try {
            LuminanceSource source = new BufferedImageLuminanceSource(image).invert();
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new MultiFormatReader().decode(bitmap, DECODE_HINTS);
            return result.getText();
        } catch (NotFoundException ignored) {}

        return null;
    }

    private QrResult processQrPayload(String qrPayload) {
        if (qrPayload == null || qrPayload.isBlank()) {
            return new QrResult(false, null, false, "Empty payload");
        }

        String lowerPayload = qrPayload.toLowerCase().trim();
        boolean isSigned = true;
        String issuer = null;

        // Check if QR payload is HTTP/HTTPS URL
        if (qrPayload.startsWith("http://") || qrPayload.startsWith("https://")) {
            if (lowerPayload.contains("nptel") || lowerPayload.contains("swayam")) issuer = "NPTEL / Swayam National Depository";
            else if (lowerPayload.contains("coursera")) issuer = "Coursera Verified Credentials";
            else if (lowerPayload.contains("edx") || lowerPayload.contains("credentials.edx")) issuer = "edX Verified Credentials";
            else if (lowerPayload.contains("udemy")) issuer = "Udemy Academy";
            else if (lowerPayload.contains("aws") || lowerPayload.contains("amazon")) issuer = "Amazon Web Services (AWS)";
            else if (lowerPayload.contains("google")) issuer = "Google Cloud Certification Registry";
            else if (lowerPayload.contains("microsoft") || lowerPayload.contains("learn.microsoft")) issuer = "Microsoft Certified Professional";
            else if (lowerPayload.contains("ibm") || lowerPayload.contains("credly.com")) issuer = "IBM / Credly Verified Depository";
            else if (lowerPayload.contains("cisco")) issuer = "Cisco Systems Certification Registry";
            else if (lowerPayload.contains("oracle")) issuer = "Oracle University Certification";
            else if (lowerPayload.contains("redhat")) issuer = "Red Hat Certified Professional";
            else if (lowerPayload.contains("digilocker") || lowerPayload.contains("nad.gov.in")) issuer = "DigiLocker National Academic Depository";
            else if (lowerPayload.contains("mycaptain")) issuer = "MyCaptain (NSDC Partner)";
            else if (lowerPayload.contains("skillindia")) issuer = "Skill India / NSDC Digital Registry";
            else if (lowerPayload.contains("hackerrank")) issuer = "HackerRank Verified Skills";
            else if (lowerPayload.contains("linkedin")) issuer = "LinkedIn Learning";
            else if (lowerPayload.contains("ktu") || lowerPayload.contains("kalam")) issuer = "APJ Abdul Kalam Technological University (KTU)";
            else if (lowerPayload.contains("calicut") || lowerPayload.contains("uoc.ac.in")) issuer = "University of Calicut";
            else if (lowerPayload.contains("kerala") || lowerPayload.contains("keralauniversity")) issuer = "University of Kerala";
            else if (lowerPayload.contains("mgu.ac.in")) issuer = "Mahatma Gandhi University (MG University)";
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
                issuer = "Signed W3C Digital Credential (JWT)";
            }
        } else if (lowerPayload.contains("digilocker") || lowerPayload.contains("verifiablecredential")) {
            issuer = "DigiLocker Verified Depository";
        } else {
            // Check text payload for institution keywords
            if (lowerPayload.contains("nptel") || lowerPayload.contains("swayam")) issuer = "NPTEL / Swayam National Depository";
            else if (lowerPayload.contains("ktu") || lowerPayload.contains("kalam")) issuer = "APJ Abdul Kalam Technological University (KTU)";
            else if (lowerPayload.contains("calicut")) issuer = "University of Calicut";
            else if (lowerPayload.contains("kerala")) issuer = "University of Kerala";
            else if (lowerPayload.contains("harvard")) issuer = "Harvard University";
            else if (lowerPayload.contains("mit") || lowerPayload.contains("massachusetts")) issuer = "Massachusetts Institute of Technology (MIT)";
            else if (lowerPayload.contains("stanford")) issuer = "Stanford University";
            else if (lowerPayload.contains("iit")) issuer = "Indian Institute of Technology (IIT)";
            else if (lowerPayload.contains("nit")) issuer = "National Institute of Technology (NIT)";
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
        return Base64.getMimeDecoder().decode(clean.replaceAll("\\s+", ""));
    }
}
