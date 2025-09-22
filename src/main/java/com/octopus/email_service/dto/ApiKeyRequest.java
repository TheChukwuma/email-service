package com.octopus.email_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyRequest {
    
    @NotBlank(message = "Key name is required")
    @Size(min = 1, max = 100, message = "Key name must be between 1 and 100 characters")
    private String keyName;
    private String clientId; //username of client
    private String clientApplication; //application/service of client
    private LocalDateTime expiresAt;
    private Long tenantId; // Optional tenant ID for tenant-specific API keys
}
