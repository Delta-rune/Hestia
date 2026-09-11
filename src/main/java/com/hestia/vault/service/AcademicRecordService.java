package com.hestia.vault.service;

import com.hestia.vault.model.AcademicRecord;
import com.hestia.vault.repository.AcademicRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AcademicRecordService {

    private final AcademicRecordRepository academicRecordRepository;

    @Autowired
    public AcademicRecordService(AcademicRecordRepository academicRecordRepository) {
        this.academicRecordRepository = academicRecordRepository;
    }

    /**
     * Saves or updates an academic record for a user.
     * If a record already exists for the given userId, update its fields.
     */
    public AcademicRecord saveAcademicRecord(AcademicRecord record) {
        if (record.getUserId() == null) {
            throw new IllegalArgumentException("User ID is required to save academic record.");
        }

        Optional<AcademicRecord> existingOpt = academicRecordRepository.findFirstByUserIdOrderByIdDesc(record.getUserId());
        if (existingOpt.isEmpty()) {
            existingOpt = academicRecordRepository.findByUserId(record.getUserId());
        }
        if (existingOpt.isPresent()) {
            AcademicRecord existing = existingOpt.get();
            existing.setApaarId(record.getApaarId());
            existing.setInstitutionName(record.getInstitutionName());
            existing.setDegreeName(record.getDegreeName());
            existing.setCurrentCgpa(record.getCurrentCgpa());
            existing.setTotalCreditsEarned(record.getTotalCreditsEarned());
            if (record.getStudentStatus() != null) existing.setStudentStatus(record.getStudentStatus());
            if (record.getSemesterDataJson() != null) existing.setSemesterDataJson(record.getSemesterDataJson());
            return academicRecordRepository.save(existing);
        }

        return academicRecordRepository.save(record);
    }

    /**
     * Retrieves the self-reported academic record for a specific userId.
     */
    public Optional<AcademicRecord> getAcademicRecordByUserId(Long userId) {
        Optional<AcademicRecord> opt = academicRecordRepository.findFirstByUserIdOrderByIdDesc(userId);
        if (opt.isPresent()) return opt;
        return academicRecordRepository.findByUserId(userId);
    }
}
