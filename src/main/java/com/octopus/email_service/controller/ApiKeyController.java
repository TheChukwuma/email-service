package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiKeyRequest;
import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.service.ApiKeyService;
import com.octopus.email_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
@Slf4j
public class ApiKeyController {
    
    private final UserService userService;
    private final ApiKeyService apiKeyService;

    @PostMapping("/api-key")
    public ResponseEntity<ApiResponse<Map<String, String>>> generateApiKey(
            @Valid @RequestBody ApiKeyRequest request,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
            
            ApiKey apiKey = apiKeyService.createApiKey(user.getUsername(), request);
            
            Map<String, String> response = new HashMap<>();
            response.put("apiKey", apiKey.getPlainKey()); // Return the plain key only once
            response.put("keyName", apiKey.getKeyName());
            response.put("expiresAt", apiKey.getExpiresAt() != null ? apiKey.getExpiresAt().toString() : null);
            response.put("createdAt", apiKey.getCreatedAt().toString());
            
            log.info("API key generated for user: {} with name: {}", username, request.getKeyName());
            
            return ResponseEntity.ok(ApiResponse.success("API key generated successfully", response));
            
        } catch (Exception e) {
            log.error("API key generation failed for user: {}", authentication.getName(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API key generation failed: " + e.getMessage()));
        }
    }

}
