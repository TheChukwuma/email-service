package com.octopus.email_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_domains")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailDomain {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String domain; // e.g., "unionbank.com"
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private EmailTenant tenant;
    
    @Column(name = "domain_reply_to_email")
    private String domainReplyToEmail; // Domain-specific reply-to email
    
    @Column(name = "domain_reply_to_name")
    private String domainReplyToName; // Domain-specific reply-to name
    
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;
    
    @Column(name = "verification_token")
    private String verificationToken;
    
    @Column(name = "spf_verified")
    @Builder.Default
    private Boolean spfVerified = false;
    
    @Column(name = "dkim_verified")
    @Builder.Default
    private Boolean dkimVerified = false;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * Check if the domain is fully verified (both SPF and DKIM)
     * @return true if domain is fully verified
     */
    public boolean isFullyVerified() {
        return Boolean.TRUE.equals(isVerified) && 
               Boolean.TRUE.equals(spfVerified) && 
               Boolean.TRUE.equals(dkimVerified);
    }
    
    /**
     * Get verification status as a string
     * @return Human-readable verification status
     */
    public String getVerificationStatus() {
        if (Boolean.TRUE.equals(isVerified)) {
            if (isFullyVerified()) {
                return "Fully Verified";
            } else {
                return "Partially Verified";
            }
        }
        return "Not Verified";
    }
    
    /**
     * Get the formatted domain-specific reply-to address if available, otherwise tenant default
     * @return Formatted reply-to address
     */
    public String getFormattedReplyTo() {
        if (domainReplyToName != null && !domainReplyToName.trim().isEmpty() && 
            domainReplyToEmail != null && !domainReplyToEmail.trim().isEmpty()) {
            return String.format("%s <%s>", domainReplyToName, domainReplyToEmail);
        }
        return domainReplyToEmail;
    }
    
    /**
     * Generate a verification token for domain ownership verification
     * @return A new verification token
     */
    public static String generateVerificationToken() {
        return "email-verify-" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
}
