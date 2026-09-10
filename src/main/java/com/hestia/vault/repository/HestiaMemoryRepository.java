package com.hestia.vault.repository;

import com.hestia.vault.model.HestiaMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HestiaMemoryRepository extends JpaRepository<HestiaMemoryEntity, Long> {

    List<HestiaMemoryEntity> findByUserIdentifierOrderByUpdatedAtDesc(String userIdentifier);

    List<HestiaMemoryEntity> findByUserIdentifierAndCategory(String userIdentifier, String category);

    Optional<HestiaMemoryEntity> findByUserIdentifierAndMemoryKey(String userIdentifier, String memoryKey);

    @Query("SELECT m FROM HestiaMemoryEntity m WHERE m.userIdentifier = :userIdentifier AND (LOWER(m.memoryKey) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(m.memoryValue) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<HestiaMemoryEntity> searchUserMemories(@Param("userIdentifier") String userIdentifier, @Param("query") String query);

    void deleteByUserIdentifier(String userIdentifier);
}
