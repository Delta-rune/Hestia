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
}
