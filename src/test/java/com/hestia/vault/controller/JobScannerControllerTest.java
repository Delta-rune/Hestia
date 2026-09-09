package com.hestia.vault.controller;

import com.hestia.vault.dto.JobDTO;
import com.hestia.vault.model.AcademicRecord;
import com.hestia.vault.repository.AcademicRecordRepository;
import com.hestia.vault.service.AcademicRecordService;
import com.hestia.vault.service.JobScannerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class JobScannerControllerTest {

    private AcademicRecordRepository academicRecordRepository;
    private AcademicRecordService academicRecordService;
    private JobScannerService jobScannerService;
    private JobScannerController jobScannerController;

    @BeforeEach
    void setUp() {
        academicRecordRepository = Mockito.mock(AcademicRecordRepository.class);
        academicRecordService = new AcademicRecordService(academicRecordRepository);
        jobScannerService = new JobScannerService(academicRecordService);
        jobScannerController = new JobScannerController(jobScannerService);
    }

    @Test
    void testScanJobsWithExplicitParameters() {
        ResponseEntity<List<JobDTO>> response = jobScannerController.scanJobs(
                "San Francisco, CA",
                "Full-Time",
                "Computer Science Software Engineer",
                101L
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());

        JobDTO job = response.getBody().get(0);
        assertNotNull(job.getJobTitle());
        assertNotNull(job.getCompanyName());
        assertNotNull(job.getApplyUrl());
        assertTrue(job.getApplyUrl().startsWith("http"));
    }

    @Test
    void testScanJobsWithSavedAcademicRecordMatching() {
        AcademicRecord record = new AcademicRecord(
                101L,
                "987654321012",
                "APJ Abdul Kalam Technological University",
                "Computer Science Engineering",
                8.8,
                140
        );

        when(academicRecordRepository.findByUserId(101L)).thenReturn(Optional.of(record));

        // Pass null qualifications to trigger automatic academic record matching logic
        ResponseEntity<List<JobDTO>> response = jobScannerController.scanJobs(
                "New York, NY",
                "Internship",
                null,
                101L
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isEmpty());

        // Ensure derived keyword matching worked
        String derivedKeywords = jobScannerService.deriveKeywordsFromAcademicRecord(record);
        assertTrue(derivedKeywords.contains("Computer Science"));
        assertTrue(derivedKeywords.contains("Software Developer"));
    }
}
