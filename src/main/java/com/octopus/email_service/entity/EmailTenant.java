package com.octopus.email_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "email_tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailTenant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_code", unique = true, nullable = false, length = 50)
    private String tenantCode; // e.g., "unionbank", "gtbank"
    
    @Column(name = "tenant_name", nullable = false)
    private String tenantName; // e.g., "Union Bank of Nigeria"
    
    @Column(name = "default_sender_email")
    private String defaultSenderEmail; // e.g., "no-reply@unionbank.com"
    
    @Column(name = "default_sender_name")
    private String defaultSenderName; // e.g., "Union Bank"
    
    @Column(name = "default_reply_to_email")
    private String defaultReplyToEmail; // e.g., "support@unionbank.com"
    
    @Column(name = "default_reply_to_name")
    private String defaultReplyToName; // e.g., "Union Bank Support"
    
    @Column(name = "domain_verified")
    @Builder.Default
    private Boolean domainVerified = false;
    
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailDomain> allowedDomains;
    
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ApiKey> apiKeys;
    
    /**
     * Get the formatted default sender address
     * @return Formatted sender address like "Union Bank <no-reply@unionbank.com>"
     */
    public String getFormattedDefaultSender() {
        if (defaultSenderName != null && !defaultSenderName.trim().isEmpty() && 
            defaultSenderEmail != null && !defaultSenderEmail.trim().isEmpty()) {
            return String.format("%s <%s>", defaultSenderName, defaultSenderEmail);
        }
        return defaultSenderEmail;
    }
    
    /**
     * Get the formatted default reply-to address
     * @return Formatted reply-to address like "Union Bank Support <support@unionbank.com>"
     */
    public String getFormattedDefaultReplyTo() {
        if (defaultReplyToName != null && !defaultReplyToName.trim().isEmpty() && 
            defaultReplyToEmail != null && !defaultReplyToEmail.trim().isEmpty()) {
            return String.format("%s <%s>", defaultReplyToName, defaultReplyToEmail);
        }
        return defaultReplyToEmail;
    }
    
    /**
     * Check if a domain is verified for this tenant
     * @param domain The domain to check
     * @return true if the domain is verified for this tenant
     */
    public boolean isDomainVerified(String domain) {
        if (allowedDomains == null) {
            return false;
        }
        return allowedDomains.stream()
                .anyMatch(d -> d.getDomain().equalsIgnoreCase(domain) && d.getIsVerified());
    }
    
    /**
     * Check if this tenant has any verified domains
     * @return true if at least one domain is verified
     */
    public boolean hasVerifiedDomains() {
        if (allowedDomains == null) {
            return false;
        }
        return allowedDomains.stream().anyMatch(EmailDomain::getIsVerified);
    }
}
