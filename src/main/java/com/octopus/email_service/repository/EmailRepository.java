package com.octopus.email_service.repository;

import com.octopus.email_service.entity.Email;
import com.octopus.email_service.enums.EmailStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    
    Optional<Email> findByUuid(UUID uuid);
    
    @Query("SELECT e FROM Email e WHERE e.status = :status AND e.attempts < e.maxAttempts AND (e.scheduledAt IS NULL OR e.scheduledAt <= :now)")
    List<Email> findEmailsForProcessing(@Param("status") EmailStatus status, @Param("now") LocalDateTime now);
    
    @Query("SELECT e FROM Email e WHERE e.status = :status")
    Page<Email> findByStatus(@Param("status") EmailStatus status, Pageable pageable);
    
    @Query("SELECT e FROM Email e WHERE e.toAddress = :toAddress")
    Page<Email> findByToAddress(@Param("toAddress") String toAddress, Pageable pageable);
    
    @Query("SELECT e FROM Email e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    Page<Email> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      Pageable pageable);
    
    @Modifying
    @Query("UPDATE Email e SET e.status = :status, e.attempts = e.attempts + 1, e.lastError = :error WHERE e.id = :id")
    void updateEmailStatusAndAttempts(@Param("id") Long id, 
                                     @Param("status") EmailStatus status, 
                                     @Param("error") String error);
    
    @Modifying
    @Query("UPDATE Email e SET e.status = :status, e.sentAt = :sentAt WHERE e.id = :id")
    void markAsSent(@Param("id") Long id, 
                   @Param("status") EmailStatus status, 
                   @Param("sentAt") LocalDateTime sentAt);
    
    @Modifying
    @Query("UPDATE Email e SET e.status = :status, e.deliveredAt = :deliveredAt WHERE e.id = :id")
    void markAsDelivered(@Param("id") Long id, 
                        @Param("status") EmailStatus status, 
                        @Param("deliveredAt") LocalDateTime deliveredAt);
    
    @Query("SELECT COUNT(e) FROM Email e WHERE e.status = :status")
    long countByStatus(@Param("status") EmailStatus status);
    
    @Query("SELECT COUNT(e) FROM Email e WHERE e.createdAt >= :since")
    long countCreatedSince(@Param("since") LocalDateTime since);
}
