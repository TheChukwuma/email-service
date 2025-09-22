package com.octopus.email_service.controller;

import com.octopus.email_service.dto.ApiResponse;
import com.octopus.email_service.dto.DomainRequest;
import com.octopus.email_service.dto.DomainResponse;
import com.octopus.email_service.dto.TenantRequest;
import com.octopus.email_service.dto.TenantResponse;
import com.octopus.email_service.entity.EmailDomain;
import com.octopus.email_service.entity.EmailTenant;
import com.octopus.email_service.service.EmailTenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/admin/tenants")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class TenantController {
    
    private final EmailTenantService tenantService;
    
    /**
     * Create a new email tenant
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TenantResponse>> createTenant(@Valid @RequestBody TenantRequest request) {
        try {
            EmailTenant tenant = tenantService.createTenant(
                request.getTenantCode(),
                request.getTenantName(),
                request.getDefaultSenderEmail(),
                request.getDefaultSenderName(),
                request.getDefaultReplyToEmail(),
                request.getDefaultReplyToName()
            );
            
            TenantResponse response = TenantResponse.fromEntity(tenant);
            return ResponseEntity.ok(ApiResponse.success("Tenant created successfully", response));
        } catch (Exception e) {
            log.error("Failed to create tenant: {}", request.getTenantCode(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create tenant: " + e.getMessage()));
        }
    }
    
    /**
     * Get all tenants
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TenantResponse>>> getAllTenants() {
        try {
            List<EmailTenant> tenants = tenantService.findAllActiveTenants();
            List<TenantResponse> responses = tenants.stream()
                    .map(TenantResponse::fromEntity)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("Failed to get all tenants", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get tenants: " + e.getMessage()));
        }
    }
    
    /**
     * Get tenant by ID
     */
    @GetMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable Long tenantId) {
        try {
            EmailTenant tenant = tenantService.findById(tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));
            
            TenantResponse response = TenantResponse.fromEntity(tenant);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get tenant: {}", tenantId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get tenant: " + e.getMessage()));
        }
    }
    
    /**
     * Update tenant
     */
    @PutMapping("/{tenantId}")
    public ResponseEntity<ApiResponse<TenantResponse>> updateTenant(
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantRequest request) {
        try {
            EmailTenant tenant = tenantService.updateTenant(
                tenantId,
                request.getTenantName(),
                request.getDefaultSenderEmail(),
                request.getDefaultSenderName(),
                request.getDefaultReplyToEmail(),
                request.getDefaultReplyToName()
            );
            
            TenantResponse response = TenantResponse.fromEntity(tenant);
            return ResponseEntity.ok(ApiResponse.success("Tenant updated successfully", response));
        } catch (Exception e) {
            log.error("Failed to update tenant: {}", tenantId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update tenant: " + e.getMessage()));
        }
    }
    
    /**
     * Add domain to tenant
     */
    @PostMapping("/{tenantId}/domains")
    public ResponseEntity<ApiResponse<DomainResponse>> addDomain(
            @PathVariable Long tenantId,
            @Valid @RequestBody DomainRequest request) {
        try {
            EmailDomain domain = tenantService.addDomainToTenant(tenantId, request.getDomain());
            DomainResponse response = DomainResponse.fromEntity(domain);
            
            return ResponseEntity.ok(ApiResponse.success("Domain added successfully", response));
        } catch (Exception e) {
            log.error("Failed to add domain {} to tenant {}", request.getDomain(), tenantId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to add domain: " + e.getMessage()));
        }
    }
    
    /**
     * Get domains for tenant
     */
    @GetMapping("/{tenantId}/domains")
    public ResponseEntity<ApiResponse<List<DomainResponse>>> getTenantDomains(@PathVariable Long tenantId) {
        try {
            List<EmailDomain> domains = tenantService.getDomainsForTenant(tenantId);
            List<DomainResponse> responses = domains.stream()
                    .map(DomainResponse::fromEntity)
                    .toList();
            
            return ResponseEntity.ok(ApiResponse.success(responses));
        } catch (Exception e) {
            log.error("Failed to get domains for tenant: {}", tenantId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get domains: " + e.getMessage()));
        }
    }
    
    /**
     * Verify domain for tenant
     */
    @PostMapping("/{tenantId}/domains/{domainId}/verify")
    public ResponseEntity<ApiResponse<DomainResponse>> verifyDomain(
            @PathVariable Long tenantId,
            @PathVariable Long domainId) {
        try {
            EmailDomain domain = tenantService.verifyDomain(tenantId, domainId);
            DomainResponse response = DomainResponse.fromEntity(domain);
            
            return ResponseEntity.ok(ApiResponse.success("Domain verified successfully", response));
        } catch (Exception e) {
            log.error("Failed to verify domain {} for tenant {}", domainId, tenantId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to verify domain: " + e.getMessage()));
        }
    }
    
    /**
     * Get tenant by code (for lookup)
     */
    @GetMapping("/lookup/{tenantCode}")
    public ResponseEntity<ApiResponse<TenantResponse>> getTenantByCode(@PathVariable String tenantCode) {
        try {
            EmailTenant tenant = tenantService.findByTenantCode(tenantCode)
                    .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantCode));
            
            TenantResponse response = TenantResponse.fromEntity(tenant);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Failed to get tenant by code: {}", tenantCode, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to get tenant: " + e.getMessage()));
        }
    }
}
