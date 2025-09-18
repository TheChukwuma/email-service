package com.octopus.email_service.service;

import com.octopus.email_service.dto.ApiKeyRequest;
import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Transactional
    public String createApiKey(User user, ApiKeyRequest request) {
        // Generate a secure random API key
        String apiKey = generateSecureApiKey();
        String hashedKey = hashApiKey(apiKey);
        
        ApiKey apiKeyEntity = ApiKey.builder()
                .user(user)
                .keyHash(hashedKey)
                .keyName(request.getKeyName())
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .build();
        
        apiKeyRepository.save(apiKeyEntity);
        log.info("Created API key for user: {}", user.getUsername());
        
        // Return the plain text key only once
        return apiKey;
    }
    
    @Transactional
    public ApiKey generateApiKey(Long userId, ApiKeyRequest request) {
        // Generate a secure random API key
        String plainApiKey = generateSecureApiKey();
        String hashedKey = hashApiKey(plainApiKey);
        
        // Create a User object with just the ID for the relationship
        User user = new User();
        user.setId(userId);
        
        ApiKey apiKeyEntity = ApiKey.builder()
                .user(user)
                .keyHash(hashedKey)
                .keyName(request.getKeyName())
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .build();
        
        ApiKey savedApiKey = apiKeyRepository.save(apiKeyEntity);
        
        // Store the plain key temporarily for response (in production, this should be handled differently)
        savedApiKey.setPlainKey(plainApiKey);
        
        log.info("Generated API key for user ID: {} with name: {}", userId, request.getKeyName());
        
        return savedApiKey;
    }
    
    public List<ApiKey> getUserApiKeys(User user) {
        return apiKeyRepository.findByUserIdAndIsActiveTrue(user.getId());
    }
    
    @Transactional
    public void deactivateApiKey(Long apiKeyId, User user) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found: " + apiKeyId));
        
        if (!apiKey.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("API key does not belong to user");
        }
        
        apiKeyRepository.deactivateById(apiKeyId);
        log.info("Deactivated API key: {} for user: {}", apiKeyId, user.getUsername());
    }
    
    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return apiKeyRepository.findByKeyHash(keyHash);
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
