package com.octopus.email_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachmentDto {
    
    @NotBlank(message = "Base64 content is required")
    private String base64Content;
    
    private String filename;
    
    private String mimeType;
    
    private Boolean inline;
    
    private String cid; // Content ID for inline attachments
    
    private String description;
    
    private Boolean optimizeImage;
    
    private Boolean generateThumbnail;
    
    private AttachmentUploadRequest.StorageType storageType;
}