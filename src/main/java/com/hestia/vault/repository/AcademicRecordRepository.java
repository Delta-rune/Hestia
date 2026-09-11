package com.hestia.vault.repository;

import com.hestia.vault.model.AcademicRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicRecordRepository extends JpaRepository<AcademicRecord, Long> {
    
    Optional<AcademicRecord> findByUserId(Long userId);
    Optional<AcademicRecord> findFirstByUserIdOrderByIdDesc(Long userId);
}
