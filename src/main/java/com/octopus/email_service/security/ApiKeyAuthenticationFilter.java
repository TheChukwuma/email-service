package com.octopus.email_service.security;

import com.octopus.email_service.entity.ApiKey;
import com.octopus.email_service.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(apiKeyHeader);
        log.info("API Key header: {}, Value: {}", apiKeyHeader, apiKey);

        if (apiKey != null && !apiKey.isEmpty()) {
            try {
                Authentication authentication = getApiKeyAuthentication(apiKey);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("API Key authenticated for request {}", request.getRequestURI());
            } catch (Exception e) {
                log.warn("Invalid API Key authentication attempt: {}", e.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Error hashing API key", e);
        }
    }

    private Authentication getApiKeyAuthentication(String apiKey) {
        String hashedKey = hashApiKey(apiKey);
        Optional<ApiKey> apiKeyEntity = apiKeyRepository.findActiveByKeyHash(hashedKey, LocalDateTime.now());

        if (apiKeyEntity.isEmpty()) {
            throw new IllegalArgumentException("API Key not found");
        }

        ApiKey key = apiKeyEntity.get();
        if (!Boolean.TRUE.equals(key.getIsActive()) || key.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("API Key is expired or inactive");
        }

        // Update last used timestamp (non-blocking async could be used if high volume)
        apiKeyRepository.updateLastUsedAt(key.getId(), LocalDateTime.now());

        return new ApiKeyAuthentication(apiKey, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
