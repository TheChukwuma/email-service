package com.octopus.email_service.repository;

import com.octopus.email_service.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    
    Optional<ApiKey> findByKeyHash(String keyHash);
    
    List<ApiKey> findByCreatedByAndIsActiveTrue(String createdBy);
    
    @Query("SELECT ak FROM ApiKey ak WHERE ak.keyHash = :keyHash AND ak.isActive = true AND (ak.expiresAt IS NULL OR ak.expiresAt > :now)")
    Optional<ApiKey> findActiveByKeyHash(@Param("keyHash") String keyHash, @Param("now") LocalDateTime now);

    @Transactional
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.lastUsedAt = :lastUsedAt WHERE ak.id = :id")
    void updateLastUsedAt(@Param("id") Long id, @Param("lastUsedAt") LocalDateTime lastUsedAt);

    @Transactional
    @Modifying
    @Query("UPDATE ApiKey ak SET ak.isActive = false WHERE ak.id = :id")
    void deactivateById(@Param("id") Long id);
    
    boolean existsByKeyHash(String keyHash);
}
