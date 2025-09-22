package com.octopus.email_service.service;

import com.octopus.email_service.dto.ApiKeyRequest;
import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.entity.EmailTenant;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.repository.ApiKeyRepository;
import com.octopus.email_service.repository.EmailTenantRepository;
import com.octopus.email_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final EmailTenantRepository tenantRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ApiKey createApiKey(String username, ApiKeyRequest request) {
        // Generate a secure random API key
        String plainApiKey = generateSecureApiKey();
        String hashedKey = hashApiKey(plainApiKey);

        // Get tenant if specified
        EmailTenant tenant = null;
        if (request.getTenantId() != null) {
            tenant = tenantRepository.findById(request.getTenantId())
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + request.getTenantId()));
        }

        ApiKey apiKeyEntity = ApiKey.builder()
                .clientId(request.getClientId())
                .clientApplication(request.getClientApplication())
                .keyHash(hashedKey)
                .keyName(request.getKeyName())
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .createdBy(username)
                .tenant(tenant)
                .build();

        ApiKey savedApiKey = apiKeyRepository.save(apiKeyEntity);

        // Store the plain key temporarily for response (in production, this should be handled differently)
        savedApiKey.setPlainKey(plainApiKey);

        log.info("Generated API key for user ID: {} with name: {}", username, request.getKeyName());

        return savedApiKey;
    }

    public List<ApiKey> getUserApiKeys(String username) {
        return apiKeyRepository.findByCreatedByAndIsActiveTrue(username);
    }

    @Transactional
    public void deactivateApiKey(Long apiKeyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));

//        if (!apiKey.getUser().getId().equals(user.getId())) {
//            throw new IllegalArgumentException("API key does not belong to user");
//        }

        apiKeyRepository.deactivateById(apiKeyId);
        log.info("Deactivated API key: {} for user: {}", apiKeyId, user.getUsername());
    }

    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return apiKeyRepository.findByKeyHash(keyHash);
    }
    
    /**
     * Validate API key and return the ApiKey entity with tenant information
     */
    public ApiKey validateApiKey(String plainApiKey) {
        if (plainApiKey == null || plainApiKey.trim().isEmpty()) {
            throw new SecurityException("API key is required");
        }
        
        String hashedKey = hashApiKey(plainApiKey);
        ApiKey apiKey = apiKeyRepository.findByKeyHash(hashedKey)
                .orElseThrow(() -> new SecurityException("Invalid API key"));
        
        if (!Boolean.TRUE.equals(apiKey.getIsActive())) {
            throw new SecurityException("API key is inactive");
        }
        
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new SecurityException("API key has expired");
        }
        
        // Update last used timestamp
        apiKey.setLastUsedAt(java.time.LocalDateTime.now());
        apiKeyRepository.save(apiKey);
        
        log.debug("Validated API key for client: {} with tenant: {}", 
                 apiKey.getClientId(), 
                 apiKey.getTenant() != null ? apiKey.getTenant().getTenantCode() : "none");
        
        return apiKey;
    }

    private String generateSecureApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
