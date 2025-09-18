package com.octopus.email_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attachments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;
    
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;
    
    @Column(name = "content_type", nullable = false)
    private String contentType;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "storage_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StorageType storageType;
    
    @Column(name = "storage_path", nullable = false)
    private String storagePath;
    
    @Column(name = "cloudinary_public_id")
    private String cloudinaryPublicId;
    
    @Column(name = "cloudinary_url")
    private String cloudinaryUrl;
    
    @Column(name = "minio_bucket")
    private String minioBucket;
    
    @Column(name = "minio_object_key")
    private String minioObjectKey;
    
    @Column(name = "checksum")
    private String checksum;
    
    @Column(name = "is_inline")
    private Boolean isInline;
    
    @Column(name = "content_id")
    private String contentId;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_processed", nullable = false)
    @Builder.Default
    private Boolean isProcessed = false;
    
    @Column(name = "processing_error")
    private String processingError;
    
    @Column(name = "created_by")
    private String createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    public enum StorageType {
        MINIO,
        CLOUDINARY,
        LOCAL
    }
}
