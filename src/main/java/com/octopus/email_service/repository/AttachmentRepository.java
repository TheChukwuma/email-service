package com.octopus.email_service.repository;

import com.octopus.email_service.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    
    Optional<Attachment> findByStoredFilename(String storedFilename);
    
    Optional<Attachment> findByCloudinaryPublicId(String cloudinaryPublicId);
    
    Optional<Attachment> findByMinioObjectKey(String minioObjectKey);
    
    List<Attachment> findByCreatedBy(String createdBy);
    
    List<Attachment> findByIsProcessed(Boolean isProcessed);
    
    List<Attachment> findByStorageType(Attachment.StorageType storageType);
    
    @Query("SELECT a FROM Attachment a WHERE a.expiresAt < :now AND a.expiresAt IS NOT NULL")
    List<Attachment> findExpiredAttachments(@Param("now") LocalDateTime now);
    
    @Query("SELECT a FROM Attachment a WHERE a.createdAt < :cutoffDate")
    List<Attachment> findOldAttachments(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.createdBy = :createdBy AND a.createdAt >= :since")
    Long countByCreatedBySince(@Param("createdBy") String createdBy, @Param("since") LocalDateTime since);
    
    @Query("SELECT SUM(a.fileSize) FROM Attachment a WHERE a.createdBy = :createdBy AND a.createdAt >= :since")
    Long sumFileSizeByCreatedBySince(@Param("createdBy") String createdBy, @Param("since") LocalDateTime since);
}
