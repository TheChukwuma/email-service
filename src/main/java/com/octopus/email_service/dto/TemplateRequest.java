package com.octopus.email_service.dto;

import com.octopus.email_service.enums.BodyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRequest {
    
    @NotBlank(message = "Template name is required")
    private String name;
    
    @NotBlank(message = "Subject template is required")
    private String subjectTemplate;
    
    @NotBlank(message = "Body template is required")
    private String bodyTemplate;
    
    @NotNull(message = "Body type is required")
    private BodyType bodyType;
    
    private Boolean isActive;
}
