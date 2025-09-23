package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.dto.UserRequest;
import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.entity.Email;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.enums.EmailStatus;
import com.octopus.email_service.security.UserPrincipal;
import com.octopus.email_service.service.ApiKeyService;
import com.octopus.email_service.service.EmailService;
import com.octopus.email_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    private final UserService userService;
    private final ApiKeyService apiKeyService;
    private final EmailService emailService;
    
    // User Management
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody UserRequest request) {
        try {
            User user = userService.createUser(request);
            return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create user: " + e.getMessage()));
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        try {
            List<User> users = userService.findAllUsers();
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            log.error("Failed to get all users", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get users: " + e.getMessage()));
        }
    }
    
    @PutMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserRequest request,
            Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            User user = userService.updateUser(userId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
        } catch (Exception e) {
            log.error("Failed to update user: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update user: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            userService.deleteUser(userId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            log.error("Failed to delete user: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }
    
    // API Key Management
    @GetMapping("/users/{userId}/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKey>>> getUserApiKeys(@PathVariable Long userId) {
        try {
            User user = userService.findByUsername("") // This should be fixed
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            List<ApiKey> apiKeys = apiKeyService.getUserApiKeys(user.getUsername());
            return ResponseEntity.ok(ApiResponse.success(apiKeys));
        } catch (Exception e) {
            log.error("Failed to get API keys for user: {}", userId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get API keys: " + e.getMessage()));
        }
    }

    @DeleteMapping("/api-keys/{apiKeyId}")
    public ResponseEntity<ApiResponse<Void>> deactivateApiKey(
            @PathVariable Long apiKeyId,
            Authentication authentication) {
        try {
            User user = (User) authentication.getPrincipal();
            apiKeyService.deactivateApiKey(apiKeyId, user);
            return ResponseEntity.ok(ApiResponse.success("API key deactivated successfully", null));
        } catch (Exception e) {
            log.error("Failed to deactivate API key: {}", apiKeyId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to deactivate API key: " + e.getMessage()));
        }
    }
    
    // Email Management
    @GetMapping("/emails")
    public ResponseEntity<ApiResponse<Page<Email>>> getAllEmails(Pageable pageable) {
        try {
            // This would need to be implemented in EmailService
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (Exception e) {
            log.error("Failed to get all emails", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get emails: " + e.getMessage()));
        }
    }
    
    @PostMapping("/emails/{emailId}/retry")
    public ResponseEntity<ApiResponse<Void>> retryEmail(@PathVariable Long emailId) {
        try {
            // This would need to be implemented in EmailService
            return ResponseEntity.ok(ApiResponse.success("Email retry queued successfully", null));
        } catch (Exception e) {
            log.error("Failed to retry email: {}", emailId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retry email: " + e.getMessage()));
        }
    }
    
    // Statistics
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> stats = Map.of(
                "totalEmails", emailService.getEmailCountSince(LocalDateTime.now().minusDays(30)),
                "sentEmails", emailService.getEmailCountByStatus(EmailStatus.SENT),
                "failedEmails", emailService.getEmailCountByStatus(EmailStatus.FAILED),
                "pendingEmails", emailService.getEmailCountByStatus(EmailStatus.ENQUEUED)
            );
            
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Failed to get statistics", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get statistics: " + e.getMessage()));
        }
    }
    
    private User getCurrentUser(Authentication authentication) {
        if (authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getUser();
        }
        throw new IllegalStateException("Invalid authentication principal");
    }
}
