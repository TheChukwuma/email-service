package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.dto.SuperAdminSetupRequest;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.service.SuperAdminSetupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/setup")
@RequiredArgsConstructor
@Slf4j
public class SuperAdminSetupController {
    
    private final SuperAdminSetupService superAdminSetupService;
    
    /**
     * Get the current setup status
     * This endpoint helps administrators understand if setup is needed/allowed
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SuperAdminSetupService.SetupStatus>> getSetupStatus() {
        try {
            SuperAdminSetupService.SetupStatus status = superAdminSetupService.getSetupStatus();
            return ResponseEntity.ok(ApiResponse.success("Setup status retrieved", status));
        } catch (Exception e) {
            log.error("Failed to get setup status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get setup status: " + e.getMessage()));
        }
    }
    
    /**
     * Create the initial SuperAdmin user
     * This endpoint is only available during the setup window and with proper authentication
     */
    @PostMapping("/superadmin")
    public ResponseEntity<ApiResponse<Object>> createSuperAdmin(
            @Valid @RequestBody SuperAdminSetupRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIpAddress(httpRequest);
        
        try {
            // Security logging
            log.info("SuperAdmin setup attempt from IP: {} for username: {}", clientIp, request.getUsername());
            
            User superAdmin = superAdminSetupService.createSuperAdmin(request, clientIp);
            
            // Don't return the full user object for security
            Object response = new Object() {
                public final String username = superAdmin.getUsername();
                public final String email = superAdmin.getEmail();
                public final String role = superAdmin.getRole().name();
                public final String message = "SuperAdmin created successfully. You can now login with your credentials.";
            };
            
            log.info("SuperAdmin created successfully: {} from IP: {}", superAdmin.getUsername(), clientIp);
            
            return ResponseEntity.ok(ApiResponse.success("SuperAdmin created successfully", response));
            
        } catch (SecurityException e) {
            log.warn("SuperAdmin setup security violation from IP {}: {}", clientIp, e.getMessage());
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Setup not allowed: " + e.getMessage()));
                    
        } catch (IllegalArgumentException e) {
            log.warn("SuperAdmin setup validation error from IP {}: {}", clientIp, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Validation error: " + e.getMessage()));
                    
        } catch (Exception e) {
            log.error("SuperAdmin setup failed from IP {}: {}", clientIp, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Setup failed: " + e.getMessage()));
        }
    }
    
    /**
     * Check if SuperAdmin setup is needed
     * Convenience endpoint for external tools/scripts
     */
    @GetMapping("/required")
    public ResponseEntity<ApiResponse<Boolean>> isSetupRequired() {
        try {
            boolean required = superAdminSetupService.isSuperAdminSetupAllowed() && 
                              !superAdminSetupService.hasAnySuperAdmin();
            
            return ResponseEntity.ok(ApiResponse.success("Setup requirement status", required));
        } catch (Exception e) {
            log.error("Failed to check setup requirement", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to check setup requirement: " + e.getMessage()));
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
