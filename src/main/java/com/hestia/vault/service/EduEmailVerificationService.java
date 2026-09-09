package com.hestia.vault.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class EduEmailVerificationService {

    // Regex matching standard educational top-level & sub-domains (.edu, .ac.uk, .edu.in, .ac.in, .edu.au, etc.)
    private static final Pattern EDU_DOMAIN_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@([A-Za-z0-9.-]+\\.)?(edu|ac\\.[a-z]{2}|edu\\.[a-z]{2}|sch\\.[a-z]{2})$",
            Pattern.CASE_INSENSITIVE
    );

    // Known university domain whitelist (can be expanded)
    private static final List<String> RECOGNIZED_UNIS = List.of(
            "stanford.edu", "harvard.edu", "mit.edu", "ox.ac.uk", "cam.ac.uk",
            "iitd.ac.in", "iitb.ac.in", "ktu.edu.in", "berkeley.edu", "caltech.edu"
    );

    /**
     * Checks whether an email address belongs to a recognized educational domain.
     */
    public boolean isEducationalEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        String cleanEmail = email.trim().toLowerCase();

        if (EDU_DOMAIN_PATTERN.matcher(cleanEmail).matches()) {
            return true;
        }

        String domain = getDomainFromEmail(cleanEmail);
        return RECOGNIZED_UNIS.contains(domain);
    }

    /**
     * Extracts institution name candidate from domain.
     */
    public String extractInstitutionFromEmail(String email) {
        String domain = getDomainFromEmail(email);
        if (domain == null) return "Unknown Institution";
        String[] parts = domain.split("\\.");
        if (parts.length > 0) {
            String name = parts[0];
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return domain;
    }

    private String getDomainFromEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }
}
