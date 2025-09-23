package com.octopus.email_service.repository;

import com.octopus.email_service.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByUsername(String username, Pageable pageable);
    
    Page<AuditLog> findByUserRole(String userRole, Pageable pageable);
    
    Page<AuditLog> findByAction(String action, Pageable pageable);
    
    Page<AuditLog> findByResourceType(String resourceType, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :startDate AND :endDate")
    Page<AuditLog> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                  @Param("endDate") LocalDateTime endDate, 
                                  Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.user.tenant.id = :tenantId")
    Page<AuditLog> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.createdAt >= :since")
    long countActionsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.success = false AND a.createdAt >= :since")
    long countFailedActionsSince(@Param("since") LocalDateTime since);
}
