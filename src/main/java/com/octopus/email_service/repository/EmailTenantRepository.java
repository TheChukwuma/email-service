package com.octopus.email_service.repository;

import com.octopus.email_service.entity.EmailTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTenantRepository extends JpaRepository<EmailTenant, Long> {
    
    /**
     * Find tenant by tenant code
     */
    Optional<EmailTenant> findByTenantCode(String tenantCode);
    
    /**
     * Find all active tenants
     */
    List<EmailTenant> findByIsActiveTrue();
    
    /**
     * Check if tenant code exists
     */
    boolean existsByTenantCode(String tenantCode);
    
    /**
     * Find tenants with verified domains
     */
    @Query("SELECT t FROM EmailTenant t WHERE t.domainVerified = true AND t.isActive = true")
    List<EmailTenant> findTenantsWithVerifiedDomains();
    
    /**
     * Find tenant by domain
     */
    @Query("SELECT t FROM EmailTenant t " +
           "JOIN t.allowedDomains d " +
           "WHERE d.domain = :domain AND d.isVerified = true AND t.isActive = true")
    Optional<EmailTenant> findByVerifiedDomain(@Param("domain") String domain);
}

