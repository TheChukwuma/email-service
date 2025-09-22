package com.octopus.email_service.dto;

import com.octopus.email_service.entity.EmailTenant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponse {
    
    private Long id;
    private String tenantCode;
    private String tenantName;
    private String defaultSenderEmail;
    private String defaultSenderName;
    private String defaultReplyToEmail;
    private String defaultReplyToName;
    private Boolean domainVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DomainResponse> allowedDomains;
    
    public static TenantResponse fromEntity(EmailTenant tenant) {
        List<DomainResponse> domains = null;
        if (tenant.getAllowedDomains() != null) {
            domains = tenant.getAllowedDomains().stream()
                    .map(DomainResponse::fromEntity)
                    .toList();
        }
        
        return TenantResponse.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .tenantName(tenant.getTenantName())
                .defaultSenderEmail(tenant.getDefaultSenderEmail())
                .defaultSenderName(tenant.getDefaultSenderName())
                .defaultReplyToEmail(tenant.getDefaultReplyToEmail())
                .defaultReplyToName(tenant.getDefaultReplyToName())
                .domainVerified(tenant.getDomainVerified())
                .isActive(tenant.getIsActive())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .allowedDomains(domains)
                .build();
    }
}
