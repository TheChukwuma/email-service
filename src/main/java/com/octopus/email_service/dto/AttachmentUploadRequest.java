package com.octopus.email_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUploadRequest {
    
    @NotNull(message = "Storage type is required")
    private StorageType storageType;
    
    private String description;
    
    private LocalDateTime expiresAt;
    
    private Boolean generateThumbnail;
    
    private Boolean optimizeImage;
    
    public enum StorageType {
        MINIO,
        CLOUDINARY,
        AUTO // Let the system decide based on file type and size
    }
}
