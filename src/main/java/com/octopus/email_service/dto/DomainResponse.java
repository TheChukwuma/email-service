package com.octopus.email_service.dto;

import com.octopus.email_service.entity.EmailDomain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainResponse {
    
    private Long id;
    private String domain;
    private String domainReplyToEmail;
    private String domainReplyToName;
    private Boolean isVerified;
    private String verificationToken;
    private Boolean spfVerified;
    private Boolean dkimVerified;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private String verificationStatus;
    
    public static DomainResponse fromEntity(EmailDomain domain) {
        return DomainResponse.builder()
                .id(domain.getId())
                .domain(domain.getDomain())
                .domainReplyToEmail(domain.getDomainReplyToEmail())
                .domainReplyToName(domain.getDomainReplyToName())
                .isVerified(domain.getIsVerified())
                .verificationToken(domain.getVerificationToken())
                .spfVerified(domain.getSpfVerified())
                .dkimVerified(domain.getDkimVerified())
                .verifiedAt(domain.getVerifiedAt())
                .createdAt(domain.getCreatedAt())
                .verificationStatus(domain.getVerificationStatus())
                .build();
    }
}
