package com.octopus.email_service.dto;

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
public class DomainRequest {
    
    @NotBlank(message = "Domain is required")
    @Size(min = 3, max = 255, message = "Domain must be between 3 and 255 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]*\\.[a-zA-Z]{2,}$",
        message = "Domain must be a valid domain name (e.g., example.com)"
    )
    private String domain;
}

