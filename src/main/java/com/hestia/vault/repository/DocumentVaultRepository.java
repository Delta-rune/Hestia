package com.hestia.vault.repository;

import com.hestia.vault.model.DocumentVault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVaultRepository extends JpaRepository<DocumentVault, Long> {

    List<DocumentVault> findByUserIdOrderByUploadedAtDesc(Long userId);

    List<DocumentVault> findByUserEmailOrderByUploadedAtDesc(String userEmail);

    List<DocumentVault> findByUserIdAndDocumentTypeOrderByUploadedAtDesc(Long userId, String documentType);

    Optional<DocumentVault> findByUserIdAndFileHash(Long userId, String fileHash);
}
