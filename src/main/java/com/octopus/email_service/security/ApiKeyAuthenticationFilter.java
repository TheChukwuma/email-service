package com.octopus.email_service.security;

import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.entity.User;
import com.octopus.email_service.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {
    
    private final ApiKeyRepository apiKeyRepository;
    
    @Value("${app.security.api-key-header:X-Api-Key}")
    private String apiKeyHeader;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String apiKey = request.getHeader(apiKeyHeader);
        log.info("API Key header: {}, Value: {}", apiKeyHeader, apiKey);
        
        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                String hashedKey = hashApiKey(apiKey);
                log.info("Hashed API key: {}", hashedKey);
                Optional<ApiKey> apiKeyEntity = apiKeyRepository.findActiveByKeyHash(hashedKey, LocalDateTime.now());
                
                if (apiKeyEntity.isPresent()) {
                    ApiKey key = apiKeyEntity.get();
                    User user = key.getUser();
                    log.info("Found API key for user: {}", user.getUsername());
                    
                    if (user.getIsActive()) {
                        // Update last used timestamp
                        apiKeyRepository.updateLastUsedAt(key.getId(), LocalDateTime.now());
                        
                        // Create authentication
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.info("API key authentication successful for user: {}", user.getUsername());
                    } else {
                        log.warn("Inactive user attempted to use API key: {}", user.getUsername());
                    }
                } else {
                    log.warn("Invalid API key provided - not found in database");
                }
            } catch (Exception e) {
                log.error("Error processing API key authentication", e);
            }
        } else {
            log.warn("No API key provided in request");
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String hashApiKey(String apiKey) throws NoSuchAlgorithmException {
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
    }
}
