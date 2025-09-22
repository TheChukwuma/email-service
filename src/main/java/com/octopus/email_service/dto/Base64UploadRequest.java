package com.octopus.email_service.dto;

public class Base64UploadRequest {
    private String base64Content;
    private String filename;
    private String contentType;
    private AttachmentUploadRequest.StorageType storageType = AttachmentUploadRequest.StorageType.AUTO;
    private java.time.LocalDateTime expiresAt;
    private Boolean generateThumbnail = false;
    private Boolean optimizeImage = false;

    // Getters and setters
    public String getBase64Content() { return base64Content; }
    public void setBase64Content(String base64Content) { this.base64Content = base64Content; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public AttachmentUploadRequest.StorageType getStorageType() { return storageType; }
    public void setStorageType(AttachmentUploadRequest.StorageType storageType) { this.storageType = storageType; }

    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public Boolean getGenerateThumbnail() { return generateThumbnail; }
    public void setGenerateThumbnail(Boolean generateThumbnail) { this.generateThumbnail = generateThumbnail; }

    public Boolean getOptimizeImage() { return optimizeImage; }
    public void setOptimizeImage(Boolean optimizeImage) { this.optimizeImage = optimizeImage; }
}