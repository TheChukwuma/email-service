package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.dto.EmailRequest;
import com.octopus.email_service.dto.EmailResponse;
import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.enums.EmailStatus;
import com.octopus.email_service.service.ApiKeyService;
import com.octopus.email_service.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {
    
    private final EmailService emailService;
    private final ApiKeyService apiKeyService;
    
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<EmailResponse>> sendEmail(
            @Valid @RequestBody EmailRequest request,
            @RequestHeader(value = "X-Api-Key", required = false) String apiKey) {
        
        try {
            EmailResponse response;
            
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                // Validate API key and get tenant information
                ApiKey validatedApiKey = apiKeyService.validateApiKey(apiKey);
                log.info("Received email send request from client: {} with tenant: {}", 
                        validatedApiKey.getClientId(),
                        validatedApiKey.getTenant() != null ? validatedApiKey.getTenant().getTenantCode() : "none");
                
                // Use tenant-aware email service
                response = emailService.sendEmail(request, validatedApiKey.getTenant());
            } else {
                // Fallback to regular email service for individual users
                log.info("Received email send request without API key (individual user)");
                response = emailService.sendEmail(request);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Email queued successfully", response));
        } catch (SecurityException e) {
            log.error("API key validation failed", e);
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Authentication failed: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to send email", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailResponse>> getEmailById(
            @PathVariable Long id,
            Authentication authentication) {
        
        try {
            return emailService.getEmailById(id)
                    .map(email -> ResponseEntity.ok(ApiResponse.success(email)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get email by ID: {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get email: " + e.getMessage()));
        }
    }
    
    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<ApiResponse<EmailResponse>> getEmailByUuid(
            @PathVariable UUID uuid,
            Authentication authentication) {
        
        try {
            return emailService.getEmailByUuid(uuid)
                    .map(email -> ResponseEntity.ok(ApiResponse.success(email)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Failed to get email by UUID: {}", uuid, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get email: " + e.getMessage()));
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<EmailResponse>>> getEmailsByStatus(
            @PathVariable EmailStatus status,
            Pageable pageable,
            Authentication authentication) {
        
        try {
            Page<EmailResponse> emails = emailService.getEmailsByStatus(status, pageable);
            return ResponseEntity.ok(ApiResponse.success(emails));
        } catch (Exception e) {
            log.error("Failed to get emails by status: {}", status, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get emails: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recipient/{toAddress}")
    public ResponseEntity<ApiResponse<Page<EmailResponse>>> getEmailsByRecipient(
            @PathVariable String toAddress,
            Pageable pageable,
            Authentication authentication) {
        
        try {
            Page<EmailResponse> emails = emailService.getEmailsByRecipient(toAddress, pageable);
            return ResponseEntity.ok(ApiResponse.success(emails));
        } catch (Exception e) {
            log.error("Failed to get emails by recipient: {}", toAddress, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get emails: " + e.getMessage()));
        }
    }
    
    @GetMapping("/date-range")
    public ResponseEntity<ApiResponse<Page<EmailResponse>>> getEmailsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable,
            Authentication authentication) {
        
        try {
            Page<EmailResponse> emails = emailService.getEmailsByDateRange(startDate, endDate, pageable);
            return ResponseEntity.ok(ApiResponse.success(emails));
        } catch (Exception e) {
            log.error("Failed to get emails by date range", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get emails: " + e.getMessage()));
        }
    }
}
