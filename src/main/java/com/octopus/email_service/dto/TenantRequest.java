package com.octopus.email_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantRequest {
    
    @NotBlank(message = "Tenant code is required")
    @Size(min = 2, max = 50, message = "Tenant code must be between 2 and 50 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Tenant code must contain only lowercase letters, numbers and hyphens")
    private String tenantCode;
    
    @NotBlank(message = "Tenant name is required")
    @Size(min = 2, max = 255, message = "Tenant name must be between 2 and 255 characters")
    private String tenantName;
    
    @Email(message = "Default sender email must be a valid email address")
    private String defaultSenderEmail;
    
    @Size(max = 255, message = "Default sender name must not exceed 255 characters")
    private String defaultSenderName;
    
    @Email(message = "Default reply-to email must be a valid email address")
    private String defaultReplyToEmail;
    
    @Size(max = 255, message = "Default reply-to name must not exceed 255 characters")
    private String defaultReplyToName;
}
