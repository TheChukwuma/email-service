package com.octopus.email_service.service;

import com.octopus.email_service.entity.EmailDomain;
import com.octopus.email_service.entity.EmailTenant;
import com.octopus.email_service.repository.EmailDomainRepository;
import com.octopus.email_service.repository.EmailTenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTenantService {
    
    private final EmailTenantRepository tenantRepository;
    private final EmailDomainRepository domainRepository;
    
    /**
     * Create a new email tenant
     */
    @Transactional
    public EmailTenant createTenant(String tenantCode, String tenantName, 
                                   String defaultSenderEmail, String defaultSenderName,
                                   String defaultReplyToEmail, String defaultReplyToName) {
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new IllegalArgumentException("Tenant code already exists: " + tenantCode);
        }
        
        EmailTenant tenant = EmailTenant.builder()
                .tenantCode(tenantCode)
                .tenantName(tenantName)
                .defaultSenderEmail(defaultSenderEmail)
                .defaultSenderName(defaultSenderName)
                .defaultReplyToEmail(defaultReplyToEmail)
                .defaultReplyToName(defaultReplyToName)
                .domainVerified(false)
                .isActive(true)
                .build();
        
        EmailTenant savedTenant = tenantRepository.save(tenant);
        log.info("Created email tenant: {} ({})", savedTenant.getTenantName(), savedTenant.getTenantCode());
        return savedTenant;
    }
    
    /**
     * Add a domain to a tenant
     */
    @Transactional
    public EmailDomain addDomainToTenant(Long tenantId, String domain) {
        EmailTenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        // Check if domain already exists for this tenant
        Optional<EmailDomain> existingDomain = domainRepository.findByDomainAndTenant(domain, tenant);
        if (existingDomain.isPresent()) {
            throw new IllegalArgumentException("Domain already exists for this tenant: " + domain);
        }
        
        EmailDomain emailDomain = EmailDomain.builder()
                .domain(domain)
                .tenant(tenant)
                .isVerified(false)
                .verificationToken(EmailDomain.generateVerificationToken())
                .spfVerified(false)
                .dkimVerified(false)
                .build();
        
        EmailDomain savedDomain = domainRepository.save(emailDomain);
        log.info("Added domain {} to tenant {}", domain, tenant.getTenantCode());
        return savedDomain;
    }
    
    /**
     * Verify a domain for a tenant
     */
    @Transactional
    public EmailDomain verifyDomain(Long tenantId, Long domainId) {
        EmailTenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        EmailDomain domain = domainRepository.findById(domainId)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + domainId));
        
        if (!domain.getTenant().getId().equals(tenantId)) {
            throw new IllegalArgumentException("Domain does not belong to this tenant");
        }
        
        // In a real implementation, you would verify DNS records here
        // For now, we'll just mark it as verified
        domain.setIsVerified(true);
        domain.setSpfVerified(true);
        domain.setDkimVerified(true);
        domain.setVerifiedAt(LocalDateTime.now());
        
        EmailDomain savedDomain = domainRepository.save(domain);
        
        // Update tenant's domain verification status
        updateTenantDomainVerificationStatus(tenant);
        
        log.info("Verified domain {} for tenant {}", domain.getDomain(), tenant.getTenantCode());
        return savedDomain;
    }
    
    /**
     * Update tenant's domain verification status
     */
    private void updateTenantDomainVerificationStatus(EmailTenant tenant) {
        boolean hasVerifiedDomains = domainRepository.findByTenantAndIsVerifiedTrue(tenant).size() > 0;
        tenant.setDomainVerified(hasVerifiedDomains);
        tenantRepository.save(tenant);
    }
    
    /**
     * Find tenant by code
     */
    public Optional<EmailTenant> findByTenantCode(String tenantCode) {
        return tenantRepository.findByTenantCode(tenantCode);
    }
    
    /**
     * Find tenant by verified domain
     */
    public Optional<EmailTenant> findByVerifiedDomain(String domain) {
        return tenantRepository.findByVerifiedDomain(domain);
    }
    
    /**
     * Get all active tenants
     */
    public List<EmailTenant> findAllActiveTenants() {
        return tenantRepository.findByIsActiveTrue();
    }
    
    /**
     * Get all domains for a tenant
     */
    public List<EmailDomain> getDomainsForTenant(Long tenantId) {
        EmailTenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        return domainRepository.findByTenant(tenant);
    }
    
    /**
     * Check if an email address domain is verified for a tenant
     */
    public boolean isDomainVerifiedForTenant(String emailAddress, EmailTenant tenant) {
        String domain = extractDomainFromEmail(emailAddress);
        return domainRepository.existsByDomainAndTenantAndVerified(domain, tenant);
    }
    
    /**
     * Extract domain from email address
     */
    public String extractDomainFromEmail(String emailAddress) {
        if (emailAddress == null || !emailAddress.contains("@")) {
            throw new IllegalArgumentException("Invalid email address: " + emailAddress);
        }
        return emailAddress.substring(emailAddress.lastIndexOf("@") + 1).toLowerCase();
    }
    
    /**
     * Get tenant by ID
     */
    public Optional<EmailTenant> findById(Long tenantId) {
        return tenantRepository.findById(tenantId);
    }
    
    /**
     * Update tenant
     */
    @Transactional
    public EmailTenant updateTenant(Long tenantId, String tenantName, 
                                   String defaultSenderEmail, String defaultSenderName,
                                   String defaultReplyToEmail, String defaultReplyToName) {
        EmailTenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
        
        tenant.setTenantName(tenantName);
        tenant.setDefaultSenderEmail(defaultSenderEmail);
        tenant.setDefaultSenderName(defaultSenderName);
        tenant.setDefaultReplyToEmail(defaultReplyToEmail);
        tenant.setDefaultReplyToName(defaultReplyToName);
        
        EmailTenant updatedTenant = tenantRepository.save(tenant);
        log.info("Updated tenant: {}", updatedTenant.getTenantCode());
        return updatedTenant;
    }
}
