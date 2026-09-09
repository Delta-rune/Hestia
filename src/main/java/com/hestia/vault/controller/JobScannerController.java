package com.hestia.vault.controller;

import com.hestia.vault.dto.JobDTO;
import com.hestia.vault.service.JobScannerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobScannerController {

    private final JobScannerService jobScannerService;

    @Autowired
    public JobScannerController(JobScannerService jobScannerService) {
        this.jobScannerService = jobScannerService;
    }

    /**
     * GET /api/jobs/scan
     * Endpoint to scan local and remote job opportunities.
     * Parameters:
     * - location (String)
     * - jobType (String: Full-Time, Part-Time, Internship, etc.)
     * - qualifications (String: derived from user's degree/skills or search input)
     * - userId (Long: optional, used to auto-derive qualifications from saved AcademicRecord)
     */
    @GetMapping("/scan")
    public ResponseEntity<List<JobDTO>> scanJobs(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String jobType,
            @RequestParam(required = false) String qualifications,
            @RequestParam(required = false) Long userId) {

        List<JobDTO> jobs = jobScannerService.scanJobs(location, jobType, qualifications, userId);
        return ResponseEntity.ok(jobs);
    }
}
