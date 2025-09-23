package com.octopus.email_service.service;

import com.octopus.email_service.dto.SuperAdminSetupRequest;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.enums.UserRole;
import com.octopus.email_service.repository.SystemSettingRepository;
import com.octopus.email_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminSetupService {
    
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.security.superadmin-setup-secret:}")
    private String configuredSetupSecret;
    
    @Value("${app.security.superadmin-setup-allowed-ips:}")
    private List<String> allowedIPs;
    
    @Value("${app.security.superadmin-setup-window-minutes:30}")
    private int setupWindowMinutes;
    
    private final LocalDateTime applicationStartTime = LocalDateTime.now();
    
    public boolean isSuperAdminSetupCompleted() {
        try {
            return Boolean.TRUE.equals(systemSettingRepository.isSuperAdminSetupCompleted());
        } catch (Exception e) {
            log.warn("Could not check superadmin setup status", e);
            return false;
        }
    }
    
    public boolean isSuperAdminSetupAllowed() {
        return !isSuperAdminSetupCompleted();
    }
    
    public boolean isSetupWindowOpen() {
        LocalDateTime cutoffTime = applicationStartTime.plusMinutes(setupWindowMinutes);
        return LocalDateTime.now().isBefore(cutoffTime);
    }
    
    public boolean isIpAddressAllowed(String clientIp) {
        if (allowedIPs == null || allowedIPs.isEmpty()) {
            // If no IPs configured, allow localhost only
            return "127.0.0.1".equals(clientIp) || "::1".equals(clientIp) || "0:0:0:0:0:0:0:1".equals(clientIp);
        }
        
        return allowedIPs.contains(clientIp);
    }
    
    @Transactional
    public User createSuperAdmin(SuperAdminSetupRequest request, String clientIp) {
        // Security validations
        validateSetupRequest(request, clientIp);
        
        // Check if any superadmin already exists
        if (userRepository.existsByRole(UserRole.SUPERADMIN)) {
            throw new SecurityException("SuperAdmin already exists in the system");
        }
        
        // Validate setup secret
        if (configuredSetupSecret.isEmpty()) {
            throw new SecurityException("SuperAdmin setup secret not configured");
        }
        
        if (!configuredSetupSecret.equals(request.getSetupSecret())) {
            log.warn("Invalid superadmin setup attempt from IP: {} with secret: {}", 
                    clientIp, request.getSetupSecret().substring(0, Math.min(4, request.getSetupSecret().length())) + "***");
            throw new SecurityException("Invalid setup secret");
        }
        
        // Check for duplicate username/email
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        // Create the superadmin user
        User superAdmin = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.SUPERADMIN)
                .isActive(true)
                .tenant(null) // SuperAdmin is not associated with any tenant
                .build();
        
        User savedUser = userRepository.save(superAdmin);
        
        // Mark setup as completed
        markSetupAsCompleted();
        
        log.info("SuperAdmin created successfully: {} from IP: {}", savedUser.getUsername(), clientIp);
        
        return savedUser;
    }
    
    private void validateSetupRequest(SuperAdminSetupRequest request, String clientIp) {
        if (!isSuperAdminSetupAllowed()) {
            throw new SecurityException("SuperAdmin setup has already been completed");
        }
        
        if (!isSetupWindowOpen()) {
            throw new SecurityException("SuperAdmin setup window has expired. Please restart the application to open a new setup window.");
        }
        
        if (!isIpAddressAllowed(clientIp)) {
            log.warn("SuperAdmin setup attempt from unauthorized IP: {}", clientIp);
            throw new SecurityException("SuperAdmin setup not allowed from this IP address");
        }
    }
    
    private void markSetupAsCompleted() {
        try {
            systemSettingRepository.findBySettingKey("superadmin_setup_completed")
                    .ifPresent(setting -> {
                        setting.setSettingValue("true");
                        systemSettingRepository.save(setting);
                    });
        } catch (Exception e) {
            log.error("Failed to mark superadmin setup as completed", e);
        }
    }
    
    public boolean hasAnySuperAdmin() {
        return userRepository.existsByRole(UserRole.SUPERADMIN);
    }
    
    public SetupStatus getSetupStatus() {
        return SetupStatus.builder()
                .setupCompleted(isSuperAdminSetupCompleted())
                .setupWindowOpen(isSetupWindowOpen())
                .hasExistingSuperAdmin(hasAnySuperAdmin())
                .applicationStartTime(applicationStartTime)
                .setupWindowExpiresAt(applicationStartTime.plusMinutes(setupWindowMinutes))
                .build();
    }
    
    @lombok.Data
    @lombok.Builder
    public static class SetupStatus {
        private boolean setupCompleted;
        private boolean setupWindowOpen;
        private boolean hasExistingSuperAdmin;
        private LocalDateTime applicationStartTime;
        private LocalDateTime setupWindowExpiresAt;
    }
}
