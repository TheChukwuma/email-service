package com.octopus.email_service.dto;

import com.octopus.email_service.entity.Attachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    
    private UUID id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private Attachment.StorageType storageType;
    private String downloadUrl;
    private String cloudinaryUrl;
    private String checksum;
    private Boolean isInline;
    private String contentId;
    private String description;
    private Boolean isProcessed;
    private String processingError;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    
    public static AttachmentResponse fromEntity(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .originalFilename(attachment.getOriginalFilename())
                .contentType(attachment.getContentType())
                .fileSize(attachment.getFileSize())
                .storageType(attachment.getStorageType())
                .cloudinaryUrl(attachment.getCloudinaryUrl())
                .checksum(attachment.getChecksum())
                .isInline(attachment.getIsInline())
                .contentId(attachment.getContentId())
                .description(attachment.getDescription())
                .isProcessed(attachment.getIsProcessed())
                .processingError(attachment.getProcessingError())
                .createdBy(attachment.getCreatedBy())
                .createdAt(attachment.getCreatedAt())
                .updatedAt(attachment.getUpdatedAt())
                .expiresAt(attachment.getExpiresAt())
                .build();
    }
}
