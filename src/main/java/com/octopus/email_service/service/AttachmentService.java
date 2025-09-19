package com.octopus.email_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.octopus.email_service.dto.AttachmentResponse;
import com.octopus.email_service.dto.AttachmentUploadRequest;
import com.octopus.email_service.entity.Attachment;
import com.octopus.email_service.repository.AttachmentRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {
    
    private final MinioClient minioClient;
    private final Cloudinary cloudinary;
    private final Tika tika;
    private final AttachmentRepository attachmentRepository;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    @Value("${app.attachment.max-file-size:10485760}") // 10MB default
    private long maxFileSizeBytes;
    
    @Value("${app.attachment.allowed-mime-types}")
    private Set<String> allowedMimeTypes;
    
    @Value("${app.attachment.default-expiry-hours:24}")
    private int defaultExpiryHours;
    
    @Transactional
    public AttachmentResponse uploadAttachment(MultipartFile file, AttachmentUploadRequest request, String createdBy) {
        try {
            // Validate file
            validateFile(file);
            
            // Generate unique filename and metadata
            String storedFilename = generateUniqueFileName(file.getOriginalFilename());
            String contentType = detectContentType(file);
            String checksum = calculateChecksum(file.getBytes());
            
            // Determine storage type
            Attachment.StorageType storageType = determineStorageType(request.getStorageType(), file);
            
            // Create attachment record
            Attachment attachment = Attachment.builder()
                    .originalFilename(file.getOriginalFilename())
                    .storedFilename(storedFilename)
                    .contentType(contentType)
                    .fileSize(file.getSize())
                    .storageType(storageType)
                    .checksum(checksum)
                    .createdBy(createdBy)
                    .expiresAt(request.getExpiresAt() != null ? 
                        request.getExpiresAt() : 
                        LocalDateTime.now().plusHours(defaultExpiryHours))
                    .build();
            
            // Upload to storage
            String storagePath = uploadToStorage(file, attachment, request);
            attachment.setStoragePath(storagePath);
            
            // Save to database
            attachment = attachmentRepository.save(attachment);
            
            log.info("Attachment uploaded successfully: {} by user: {}", attachment.getId(), createdBy);
            return AttachmentResponse.fromEntity(attachment);
            
        } catch (Exception e) {
            log.error("Failed to upload attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload attachment: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    public AttachmentResponse uploadBase64Attachment(String base64Content, String filename, 
                                                   String contentType, AttachmentUploadRequest request, String createdBy) {
        try {
            byte[] content = Base64.getDecoder().decode(base64Content);
            
            if (content.length > maxFileSizeBytes) {
                throw new IllegalArgumentException("File size exceeds maximum allowed size");
            }
            
            // Validate content type
            String detectedType = tika.detect(content);
            if (!allowedMimeTypes.contains(detectedType)) {
                throw new IllegalArgumentException("File type not allowed: " + detectedType);
            }
            
            // Generate metadata
            String storedFilename = generateUniqueFileName(filename);
            String checksum = calculateChecksum(content);
            
            // Determine storage type
            Attachment.StorageType storageType = determineStorageType(request.getStorageType(), content.length);
            
            // Create attachment record
            Attachment attachment = Attachment.builder()
                    .originalFilename(filename)
                    .storedFilename(storedFilename)
                    .contentType(detectedType)
                    .fileSize((long) content.length)
                    .storageType(storageType)
                    .checksum(checksum)
                    .createdBy(createdBy)
                    .expiresAt(request.getExpiresAt() != null ? 
                        request.getExpiresAt() : 
                        LocalDateTime.now().plusHours(defaultExpiryHours))
                    .build();
            
            // Upload to storage
            String storagePath = uploadBase64ToStorage(content, attachment, request);
            attachment.setStoragePath(storagePath);
            
            // Save to database
            attachment = attachmentRepository.save(attachment);
            
            log.info("Base64 attachment uploaded successfully: {} by user: {}", attachment.getId(), createdBy);
            return AttachmentResponse.fromEntity(attachment);
            
        } catch (Exception e) {
            log.error("Failed to upload base64 attachment: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload base64 attachment: " + e.getMessage(), e);
        }
    }
    
    public InputStream downloadAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        
        try {
            switch (attachment.getStorageType()) {
                case MINIO:
                    return minioClient.getObject(
                        GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(attachment.getMinioObjectKey())
                            .build()
                    );
                case CLOUDINARY:
                    // For Cloudinary, we would typically redirect to the URL
                    // This is a simplified implementation
                    throw new UnsupportedOperationException("Direct download from Cloudinary not implemented");
                default:
                    throw new IllegalArgumentException("Unsupported storage type: " + attachment.getStorageType());
            }
        } catch (Exception e) {
            log.error("Failed to download attachment: {}", attachmentId, e);
            throw new RuntimeException("Failed to download attachment", e);
        }
    }
    
    public AttachmentResponse getAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        
        return AttachmentResponse.fromEntity(attachment);
    }
    
    @Transactional
    public void deleteAttachment(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
        
        try {
            // Delete from storage
            deleteFromStorage(attachment);
            
            // Delete from database
            attachmentRepository.delete(attachment);
            
            log.info("Attachment deleted successfully: {}", attachmentId);
        } catch (Exception e) {
            log.error("Failed to delete attachment: {}", attachmentId, e);
            throw new RuntimeException("Failed to delete attachment", e);
        }
    }
    
    @Async
    @Transactional
    public CompletableFuture<Void> cleanupExpiredAttachments() {
        try {
            List<Attachment> expiredAttachments = attachmentRepository.findExpiredAttachments(LocalDateTime.now());
            
            for (Attachment attachment : expiredAttachments) {
                try {
                    deleteFromStorage(attachment);
                    attachmentRepository.delete(attachment);
                    log.info("Expired attachment cleaned up: {}", attachment.getId());
                } catch (Exception e) {
                    log.error("Failed to cleanup expired attachment: {}", attachment.getId(), e);
                }
            }
            
            log.info("Cleanup completed. Processed {} expired attachments", expiredAttachments.size());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.error("Failed to cleanup expired attachments", e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    public List<AttachmentResponse> getUserAttachments(String createdBy) {
        return attachmentRepository.findByCreatedBy(createdBy)
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
    }
    
    public Long getUserAttachmentCount(String createdBy, LocalDateTime since) {
        return attachmentRepository.countByCreatedBySince(createdBy, since);
    }
    
    public Long getUserAttachmentSize(String createdBy, LocalDateTime since) {
        return attachmentRepository.sumFileSizeByCreatedBySince(createdBy, since);
    }
    
    private String uploadToStorage(MultipartFile file, Attachment attachment, AttachmentUploadRequest request) throws Exception {
        return switch (attachment.getStorageType()) {
            case MINIO -> uploadToMinIO(file, attachment);
            case CLOUDINARY -> uploadToCloudinary(file, attachment, request);
            default -> throw new IllegalArgumentException("Unsupported storage type: " + attachment.getStorageType());
        };
    }
    
    private String uploadBase64ToStorage(byte[] content, Attachment attachment, AttachmentUploadRequest request) throws Exception {
        return switch (attachment.getStorageType()) {
            case MINIO -> uploadBase64ToMinIO(content, attachment);
            case CLOUDINARY -> uploadBase64ToCloudinary(content, attachment, request);
            default -> throw new IllegalArgumentException("Unsupported storage type: " + attachment.getStorageType());
        };
    }
    
    private String uploadToMinIO(MultipartFile file, Attachment attachment) throws Exception {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(attachment.getStoredFilename())
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(attachment.getContentType())
                    .build()
            );
            
            attachment.setMinioBucket(bucketName);
            attachment.setMinioObjectKey(attachment.getStoredFilename());
            
            return minioEndpoint + "/" + bucketName + "/" + attachment.getStoredFilename();
        }
    }
    
    private String uploadBase64ToMinIO(byte[] content, Attachment attachment) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(attachment.getStoredFilename())
                    .stream(inputStream, content.length, -1)
                    .contentType(attachment.getContentType())
                    .build()
            );
            
            attachment.setMinioBucket(bucketName);
            attachment.setMinioObjectKey(attachment.getStoredFilename());
            
            return minioEndpoint + "/" + bucketName + "/" + attachment.getStoredFilename();
        }
    }
    
    private String uploadToCloudinary(MultipartFile file, Attachment attachment, AttachmentUploadRequest request) throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "auto");
        options.put("folder", "email-attachments");
        
        if (request.getOptimizeImage() != null && request.getOptimizeImage()) {
            options.put("quality", "auto");
            options.put("fetch_format", "auto");
        }
        
        if (request.getGenerateThumbnail() != null && request.getGenerateThumbnail()) {
            options.put("transformation", "w_150,h_150,c_fill");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(options)
        );
        
        String publicId = (String) uploadResult.get("public_id");
        String secureUrl = (String) uploadResult.get("secure_url");
        
        attachment.setCloudinaryPublicId(publicId);
        attachment.setCloudinaryUrl(secureUrl);
        
        return secureUrl;
    }
    
    private String uploadBase64ToCloudinary(byte[] content, Attachment attachment, AttachmentUploadRequest request) throws Exception {
        Map<String, Object> options = new HashMap<>();
        options.put("resource_type", "auto");
        options.put("folder", "email-attachments");
        
        if (request.getOptimizeImage() != null && request.getOptimizeImage()) {
            options.put("quality", "auto");
            options.put("fetch_format", "auto");
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
            content,
            ObjectUtils.asMap(options)
        );
        
        String publicId = (String) uploadResult.get("public_id");
        String secureUrl = (String) uploadResult.get("secure_url");
        
        attachment.setCloudinaryPublicId(publicId);
        attachment.setCloudinaryUrl(secureUrl);
        
        return secureUrl;
    }
    
    private void deleteFromStorage(Attachment attachment) throws Exception {
        switch (attachment.getStorageType()) {
            case MINIO:
                if (attachment.getMinioObjectKey() != null) {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(attachment.getMinioObjectKey())
                            .build()
                    );
                }
                break;
            case CLOUDINARY:
                if (attachment.getCloudinaryPublicId() != null) {
                    cloudinary.uploader().destroy(attachment.getCloudinaryPublicId(), ObjectUtils.emptyMap());
                }
                break;
            default:
                log.warn("Unknown storage type for deletion: {}", attachment.getStorageType());
        }
    }
    
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        if (file.getSize() > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size: " + maxFileSizeBytes);
        }
        
        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
        
        // Additional validation using Tika to detect actual file type
        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!allowedMimeTypes.contains(detectedType)) {
                throw new IllegalArgumentException("File type mismatch. Detected: " + detectedType + ", Expected: " + contentType);
            }
        } catch (IOException e) {
            log.error("Failed to detect file type", e);
            throw new IllegalArgumentException("Failed to validate file type");
        }
    }
    
    private String detectContentType(MultipartFile file) {
        try {
            return tika.detect(file.getInputStream());
        } catch (IOException e) {
            log.warn("Failed to detect content type, using provided type: {}", file.getContentType());
            return file.getContentType();
        }
    }
    
    private String calculateChecksum(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            return null;
        }
    }
    
    private Attachment.StorageType determineStorageType(AttachmentUploadRequest.StorageType requestedType, MultipartFile file) {
        if (requestedType == AttachmentUploadRequest.StorageType.AUTO) {
            // Auto-determine based on file type and size
            if (isImageFile(file.getContentType()) && file.getSize() < 5 * 1024 * 1024) { // 5MB
                return Attachment.StorageType.CLOUDINARY;
            } else {
                return Attachment.StorageType.MINIO;
            }
        }
        
        return switch (requestedType) {
            case MINIO -> Attachment.StorageType.MINIO;
            case CLOUDINARY -> Attachment.StorageType.CLOUDINARY;
            default -> Attachment.StorageType.MINIO;
        };
    }
    
    private Attachment.StorageType determineStorageType(AttachmentUploadRequest.StorageType requestedType, long fileSize) {
        if (requestedType == AttachmentUploadRequest.StorageType.AUTO) {
            // Auto-determine based on file size
            if (fileSize < 5 * 1024 * 1024) { // 5MB
                return Attachment.StorageType.CLOUDINARY;
            } else {
                return Attachment.StorageType.MINIO;
            }
        }
        
        return switch (requestedType) {
            case MINIO -> Attachment.StorageType.MINIO;
            case CLOUDINARY -> Attachment.StorageType.CLOUDINARY;
            default -> Attachment.StorageType.MINIO;
        };
    }
    
    private boolean isImageFile(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}
