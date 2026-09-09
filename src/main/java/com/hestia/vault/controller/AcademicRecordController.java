package com.hestia.vault.controller;

import com.hestia.vault.model.AcademicRecord;
import com.hestia.vault.service.AcademicRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/academic-records")
@CrossOrigin(origins = "*")
public class AcademicRecordController {

    private final AcademicRecordService academicRecordService;

    @Autowired
    public AcademicRecordController(AcademicRecordService academicRecordService) {
        this.academicRecordService = academicRecordService;
    }

    /**
     * POST /api/academic-records
     * Saves or updates a manually entered academic record.
     */
    @PostMapping
    public ResponseEntity<?> saveAcademicRecord(@RequestBody AcademicRecord record) {
        if (record.getUserId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "userId is required."));
        }

        // Validate APAAR ID format (12-digit format)
        if (record.getApaarId() != null && !record.getApaarId().isBlank()) {
            String digitsOnly = record.getApaarId().replaceAll("[^0-9]", "");
            if (digitsOnly.length() != 12) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "APAAR ID must follow the standard 12-digit format."));
            }
            // Standardize format or store raw clean digit string
            record.setApaarId(digitsOnly);
        }

        // Validate CGPA (0.0 to 10.0)
        if (record.getCurrentCgpa() != null) {
            if (record.getCurrentCgpa() < 0.0 || record.getCurrentCgpa() > 10.0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "CGPA must be between 0.0 and 10.0."));
            }
        }

        AcademicRecord savedRecord = academicRecordService.saveAcademicRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRecord);
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr == null || userIdStr.isBlank()) return 1L;
        try {
            return Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            String digits = userIdStr.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                try {
                    return Long.parseLong(digits.substring(0, Math.min(15, digits.length())));
                } catch (NumberFormatException nfe) {
                    return 1L;
                }
            }
            return (long) Math.abs(userIdStr.hashCode());
        }
    }

    /**
     * GET /api/academic-records/{userIdStr}
     * Retrieves the saved academic record for the specified userId.
     */
    @GetMapping("/{userIdStr}")
    public ResponseEntity<?> getAcademicRecord(@PathVariable String userIdStr) {
        Long userId = parseUserId(userIdStr);
        Optional<AcademicRecord> recordOpt = academicRecordService.getAcademicRecordByUserId(userId);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No academic record found for userId: " + userIdStr));
        }

        return ResponseEntity.ok(recordOpt.get());
    }

    /**
     * GET /api/academic-records/{userIdStr}/semesters
     * Retrieves semester JSON data for specified userId.
     */
    @GetMapping("/{userIdStr}/semesters")
    public ResponseEntity<?> getSemesterRecords(@PathVariable String userIdStr) {
        Long userId = parseUserId(userIdStr);
        Optional<AcademicRecord> recordOpt = academicRecordService.getAcademicRecordByUserId(userId);
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No academic record found for userId: " + userIdStr));
        }

        AcademicRecord record = recordOpt.get();
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "studentStatus", record.getStudentStatus() != null ? record.getStudentStatus() : "PRE_GRADUATE",
            "semesterDataJson", record.getSemesterDataJson() != null ? record.getSemesterDataJson() : "[]"
        ));
    }
}
