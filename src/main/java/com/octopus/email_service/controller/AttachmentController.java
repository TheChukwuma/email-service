package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.dto.AttachmentResponse;
import com.octopus.email_service.dto.AttachmentUploadRequest;
import com.octopus.email_service.dto.Base64UploadRequest;
import com.octopus.email_service.service.AttachmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {
    
    private final AttachmentService attachmentService;
    
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "storageType", defaultValue = "AUTO") String storageType,
            @RequestParam(value = "expiresAt", required = false) String expiresAt,
            @RequestParam(value = "generateThumbnail", defaultValue = "false") Boolean generateThumbnail,
            @RequestParam(value = "optimizeImage", defaultValue = "false") Boolean optimizeImage,
            Authentication authentication) {
        
        try {
            String createdBy = authentication.getName();
            
            AttachmentUploadRequest request = AttachmentUploadRequest.builder()
                    .storageType(AttachmentUploadRequest.StorageType.valueOf(storageType.toUpperCase()))
                    .generateThumbnail(generateThumbnail)
                    .optimizeImage(optimizeImage)
                    .build();
            
            AttachmentResponse response = attachmentService.uploadAttachment(file, request, createdBy);
            
            return ResponseEntity.ok(ApiResponse.<AttachmentResponse>builder()
                    .success(true)
                    .message("Attachment uploaded successfully")
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to upload attachment", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AttachmentResponse>builder()
                            .success(false)
                            .message("Failed to upload attachment: " + e.getMessage())
                            .build());
        }
    }
    
    @PostMapping("/upload-base64")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadBase64Attachment(
            @Valid @RequestBody Base64UploadRequest request,
            Authentication authentication) {
        
        try {
            String createdBy = authentication.getName();
            
            AttachmentUploadRequest uploadRequest = AttachmentUploadRequest.builder()
                    .storageType(request.getStorageType())
                    .expiresAt(request.getExpiresAt())
                    .generateThumbnail(request.getGenerateThumbnail())
                    .optimizeImage(request.getOptimizeImage())
                    .build();
            
            AttachmentResponse response = attachmentService.uploadBase64Attachment(
                    request.getBase64Content(),
                    request.getFilename(),
                    request.getContentType(),
                    uploadRequest,
                    createdBy
            );
            
            return ResponseEntity.ok(ApiResponse.<AttachmentResponse>builder()
                    .success(true)
                    .message("Base64 attachment uploaded successfully")
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to upload base64 attachment", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AttachmentResponse>builder()
                            .success(false)
                            .message("Failed to upload base64 attachment: " + e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<AttachmentResponse>> getAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication) {
        
        try {
            AttachmentResponse response = attachmentService.getAttachment(attachmentId);
            
            // Check if user has access to this attachment
            if (!response.getCreatedBy().equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.<AttachmentResponse>builder()
                                .success(false)
                                .message("Access denied to this attachment")
                                .build());
            }
            
            return ResponseEntity.ok(ApiResponse.<AttachmentResponse>builder()
                    .success(true)
                    .data(response)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get attachment: {}", attachmentId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<AttachmentResponse>builder()
                            .success(false)
                            .message("Failed to get attachment: " + e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication,
            HttpServletRequest request) {
        
        try {
            AttachmentResponse attachment = attachmentService.getAttachment(attachmentId);
            
            // Check if user has access to this attachment
            if (!attachment.getCreatedBy().equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            InputStream inputStream = attachmentService.downloadAttachment(attachmentId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + attachment.getOriginalFilename() + "\"");
            headers.add(HttpHeaders.CONTENT_TYPE, attachment.getContentType());
            headers.add(HttpHeaders.CONTENT_LENGTH, attachment.getFileSize().toString());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(new InputStreamResource(inputStream));
                    
        } catch (Exception e) {
            log.error("Failed to download attachment: {}", attachmentId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID attachmentId,
            Authentication authentication) {
        
        try {
            AttachmentResponse attachment = attachmentService.getAttachment(attachmentId);
            
            // Check if user has access to this attachment
            if (!attachment.getCreatedBy().equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.<Void>builder()
                                .success(false)
                                .message("Access denied to this attachment")
                                .build());
            }
            
            attachmentService.deleteAttachment(attachmentId);
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Attachment deleted successfully")
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to delete attachment: {}", attachmentId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to delete attachment: " + e.getMessage())
                            .build());
        }
    }
    
    @GetMapping("/my-attachments")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getMyAttachments(
            Authentication authentication) {
        
        try {
            String createdBy = authentication.getName();
            List<AttachmentResponse> attachments = attachmentService.getUserAttachments(createdBy);
            
            return ResponseEntity.ok(ApiResponse.<List<AttachmentResponse>>builder()
                    .success(true)
                    .data(attachments)
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to get user attachments", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<AttachmentResponse>>builder()
                            .success(false)
                            .message("Failed to get attachments: " + e.getMessage())
                            .build());
        }
    }
    
    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<Void>> cleanupExpiredAttachments() {
        try {
            attachmentService.cleanupExpiredAttachments();
            
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Cleanup process started")
                    .build());
                    
        } catch (Exception e) {
            log.error("Failed to start cleanup process", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("Failed to start cleanup: " + e.getMessage())
                            .build());
        }
    }

}
