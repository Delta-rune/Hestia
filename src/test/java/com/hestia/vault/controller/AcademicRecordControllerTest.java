package com.hestia.vault.controller;

import com.hestia.vault.model.AcademicRecord;
import com.hestia.vault.repository.AcademicRecordRepository;
import com.hestia.vault.service.AcademicRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AcademicRecordControllerTest {

    private AcademicRecordRepository academicRecordRepository;
    private AcademicRecordService academicRecordService;
    private AcademicRecordController academicRecordController;

    @BeforeEach
    void setUp() {
        academicRecordRepository = Mockito.mock(AcademicRecordRepository.class);
        academicRecordService = new AcademicRecordService(academicRecordRepository);
        academicRecordController = new AcademicRecordController(academicRecordService);
    }

    @Test
    void testSaveAcademicRecordSuccess() {
        AcademicRecord record = new AcademicRecord(
                101L,
                "987654321012",
                "APJ Abdul Kalam Technological University",
                "B.Tech Computer Science and Engineering",
                8.75,
                160
        );

        when(academicRecordRepository.findByUserId(101L)).thenReturn(Optional.empty());
        when(academicRecordRepository.save(any(AcademicRecord.class))).thenAnswer(i -> i.getArgument(0));

        ResponseEntity<?> response = academicRecordController.saveAcademicRecord(record);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody() instanceof AcademicRecord);
        AcademicRecord saved = (AcademicRecord) response.getBody();
        assertEquals("987654321012", saved.getApaarId());
        assertEquals("APJ Abdul Kalam Technological University", saved.getInstitutionName());
        assertEquals(8.75, saved.getCurrentCgpa());
        assertEquals(160, saved.getTotalCreditsEarned());
    }

    @Test
    void testGetAcademicRecordSuccess() {
        AcademicRecord record = new AcademicRecord(
                101L,
                "987654321012",
                "APJ Abdul Kalam Technological University",
                "B.Tech Computer Science and Engineering",
                8.75,
                160
        );

        when(academicRecordRepository.findByUserId(101L)).thenReturn(Optional.of(record));

        ResponseEntity<?> response = academicRecordController.getAcademicRecord("101");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof AcademicRecord);
        AcademicRecord fetched = (AcademicRecord) response.getBody();
        assertEquals(101L, fetched.getUserId());
        assertEquals("987654321012", fetched.getApaarId());
    }

    @Test
    void testSaveAcademicRecordInvalidCgpa() {
        AcademicRecord invalidRecord = new AcademicRecord(
                102L,
                "123456789012",
                "Some University",
                "B.Tech",
                11.5, // CGPA > 10.0
                120
        );

        ResponseEntity<?> response = academicRecordController.saveAcademicRecord(invalidRecord);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("CGPA must be between 0.0 and 10.0.", body.get("error"));
    }

    @Test
    void testSaveAcademicRecordInvalidApaarId() {
        AcademicRecord invalidRecord = new AcademicRecord(
                103L,
                "12345", // Length < 12 digits
                "Some University",
                "B.Tech",
                8.0,
                120
        );

        ResponseEntity<?> response = academicRecordController.saveAcademicRecord(invalidRecord);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("APAAR ID must follow the standard 12-digit format.", body.get("error"));
    }

    @Test
    void testGetNonExistentAcademicRecord() {
        when(academicRecordRepository.findByUserId(999999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = academicRecordController.getAcademicRecord("999999");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testParseKtuGradeCardTextExactUserExample() {
        String ocrText = """
                July-2026
                No.GC/2026/02/S2/4691
                APJ Abdul Kalam Technological University
                Semester Grade Card
                Name of Candidate NRIPAN SATHEESH Register No MBC25CA063
                Name of College
                MAR BASELIOS CHRISTIAN
                COLLEGE OF ENGINEERING &
                TECHNOLOGY
                Branch Computer Science and Engineering
                (Artificial Intelligence)
                Semester S2 Programme B.Tech
                Course Name Code Grade Credits
                Earned
                Month & Year
                of Examination
                MATHEMATICS FOR INFORMATION SCIENCE
                - 2
                GAMAT201 D 3.0 April 2026
                CHEMISTRY FOR INFORMATION SCIENCE /
                ELECTRICAL SCIENCE
                GXCYT122 D 4.0 April 2026
                FOUNDATIONS OF COMPUTING: FROM
                HARDWARE ESSENTIALS TO WEB DESIGN
                GXEST203 A+ 3.0 April 2026
                PROGRAMMING IN C GXEST204 P 4.0 April 2026
                DISCRETE MATHEMATICS PCCST205 P 4.0 April 2026
                ENGINEERING ENTREPRENEURSHIP & IPR UCEST206 B 3.0 April 2026
                IT WORKSHOP GXESL208 A+ 1.0 April 2026
                LIFE SKILLS AND PROFESSIONAL
                COMMUNICATION
                UCHUT128 PASS 1.0 April 2026
                SKILL ENHANCEMENT COURSE:DIGITAL 101 UCSEM129 PASS 1.0 July 2026
                Total Credits Earned 24
                Total Credits in the Semester 24
                SGPA 6.57
                CGPA 6.49
                Controller of Examinations*
                 Generated on: 31/07/2026 03:55 PM
                *This is a computer system generated Grade Card.Hence no need for a physical signature
                """;

        Map<String, Object> result = academicRecordController.parseGradeCardText(ocrText, 2);

        assertEquals("NRIPAN SATHEESH", result.get("studentName"));
        assertEquals("MBC25CA063", result.get("registerNo"));
        assertEquals("MAR BASELIOS CHRISTIAN COLLEGE OF ENGINEERING & TECHNOLOGY", result.get("collegeName"));
        assertEquals("Computer Science and Engineering (Artificial Intelligence)", result.get("branch"));
        assertEquals(24, result.get("credits"));
        assertEquals(24, result.get("semesterCredits"));
        assertEquals(6.57, (Double) result.get("sgpa"), 0.001);
        assertEquals(6.49, (Double) result.get("cgpa"), 0.001);

        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> courses = (java.util.List<Map<String, Object>>) result.get("courses");
        assertEquals(9, courses.size());

        // Verify specific courses
        assertEquals("GAMAT201", courses.get(0).get("code"));
        assertEquals("MATHEMATICS FOR INFORMATION SCIENCE - 2", courses.get(0).get("name"));
        assertEquals(3, courses.get(0).get("credits"));
        assertEquals("D", courses.get(0).get("grade"));

        assertEquals("GXCYT122", courses.get(1).get("code"));
        assertEquals("CHEMISTRY FOR INFORMATION SCIENCE / ELECTRICAL SCIENCE", courses.get(1).get("name"));
        assertEquals(4, courses.get(1).get("credits"));
        assertEquals("D", courses.get(1).get("grade"));

        assertEquals("UCSEM129", courses.get(8).get("code"));
        assertEquals("SKILL ENHANCEMENT COURSE:DIGITAL 101", courses.get(8).get("name"));
        assertEquals(1, courses.get(8).get("credits"));
        assertEquals("PASS", courses.get(8).get("grade"));
    }
}
