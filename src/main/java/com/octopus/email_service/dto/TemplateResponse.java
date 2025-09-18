package com.octopus.email_service.dto;

import com.octopus.email_service.entity.Template;
import com.octopus.email_service.enums.BodyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateResponse {
    
    private Long id;
    private String name;
    private String subjectTemplate;
    private String bodyTemplate;
    private BodyType bodyType;
    private Long createdBy;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static TemplateResponse fromEntity(Template template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .subjectTemplate(template.getSubjectTemplate())
                .bodyTemplate(template.getBodyTemplate())
                .bodyType(template.getBodyType())
                .createdBy(template.getCreatedBy() != null ? template.getCreatedBy().getId() : null)
                .isActive(template.getIsActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}
