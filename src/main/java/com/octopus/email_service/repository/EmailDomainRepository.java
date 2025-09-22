package com.octopus.email_service.repository;

import com.octopus.email_service.entity.EmailDomain;
import com.octopus.email_service.entity.EmailTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailDomainRepository extends JpaRepository<EmailDomain, Long> {
    
    /**
     * Find domain by domain name and tenant
     */
    Optional<EmailDomain> findByDomainAndTenant(String domain, EmailTenant tenant);
    
    /**
     * Find all domains for a tenant
     */
    List<EmailDomain> findByTenant(EmailTenant tenant);
    
    /**
     * Find all verified domains for a tenant
     */
    List<EmailDomain> findByTenantAndIsVerifiedTrue(EmailTenant tenant);
    
    /**
     * Find domain by verification token
     */
    Optional<EmailDomain> findByVerificationToken(String verificationToken);
    
    /**
     * Check if domain exists for any tenant
     */
    boolean existsByDomain(String domain);
    
    /**
     * Check if domain is verified for a specific tenant
     */
    @Query("SELECT COUNT(d) > 0 FROM EmailDomain d " +
           "WHERE d.domain = :domain AND d.tenant = :tenant AND d.isVerified = true")
    boolean existsByDomainAndTenantAndVerified(@Param("domain") String domain, 
                                               @Param("tenant") EmailTenant tenant);
    
    /**
     * Find all verified domains
     */
    List<EmailDomain> findByIsVerifiedTrue();
}

