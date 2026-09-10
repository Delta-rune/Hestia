package com.hestia.vault.repository;

import com.hestia.vault.model.HestiaConversationEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HestiaConversationRepository extends JpaRepository<HestiaConversationEntity, Long> {

    List<HestiaConversationEntity> findByUserIdentifierOrderByTimestampDesc(String userIdentifier, Pageable pageable);

    List<HestiaConversationEntity> findBySessionIdOrderByTimestampAsc(String sessionId);

    void deleteByUserIdentifier(String userIdentifier);
}
