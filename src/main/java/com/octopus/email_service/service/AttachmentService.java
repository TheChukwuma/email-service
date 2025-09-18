package com.octopus.email_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {
    
    private final MinioClient minioClient;
    private final Cloudinary cloudinary;
    private final Tika tika;
    
    @Value("${minio.bucket-name}")
    private String bucketName;
    
    @Value("${minio.endpoint}")
    private String minioEndpoint;
    
    private final Set<String> allowedMimeTypes;
    private final long maxFileSizeBytes;
    
    public String uploadToMinIO(MultipartFile file) throws Exception {
        validateFile(file);
        
        String fileName = generateUniqueFileName(file.getOriginalFilename());
        String contentType = file.getContentType();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
            
            String fileUrl = minioEndpoint + "/" + bucketName + "/" + fileName;
            log.info("File uploaded to MinIO: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }
    
    public String uploadToCloudinary(MultipartFile file) throws Exception {
        validateFile(file);
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "resource_type", "auto",
                    "folder", "email-attachments"
                )
            );
            
            String fileUrl = (String) uploadResult.get("secure_url");
            log.info("File uploaded to Cloudinary: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }
    
    public String uploadBase64ToMinIO(String base64Content, String fileName, String contentType) throws Exception {
        byte[] content = Base64.getDecoder().decode(base64Content);
        
        if (content.length > maxFileSizeBytes) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size");
        }
        
        // Validate content type using Tika
        String detectedType = tika.detect(content);
        if (!allowedMimeTypes.contains(detectedType)) {
            throw new IllegalArgumentException("File type not allowed: " + detectedType);
        }
        
        String uniqueFileName = generateUniqueFileName(fileName);
        
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(uniqueFileName)
                    .stream(inputStream, content.length, -1)
                    .contentType(contentType)
                    .build()
            );
            
            String fileUrl = minioEndpoint + "/" + bucketName + "/" + uniqueFileName;
            log.info("Base64 file uploaded to MinIO: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("Failed to upload base64 file to MinIO", e);
            throw new RuntimeException("Failed to upload base64 file to MinIO", e);
        }
    }
    
    public InputStream downloadFromMinIO(String fileName) throws Exception {
        try {
            return minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", fileName, e);
            throw new RuntimeException("Failed to download file from MinIO", e);
        }
    }
    
    public void deleteFromMinIO(String fileName) throws Exception {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build()
            );
            log.info("File deleted from MinIO: {}", fileName);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", fileName, e);
            throw new RuntimeException("Failed to delete file from MinIO", e);
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
    
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
}
