package com.octopus.email_service.service;

import com.octopus.email_service.entity.User;
import com.octopus.email_service.enums.Permission;
import com.octopus.email_service.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Service to handle role-based authorization and permission checking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {
    
    // Permission mappings for each role
    private static final Set<Permission> SUPERADMIN_PERMISSIONS = EnumSet.allOf(Permission.class);
    
    private static final Set<Permission> ADMIN_PERMISSIONS = EnumSet.of(
        // Template permissions (all except delete - could be configurable)
        Permission.CREATE_TEMPLATE, Permission.EDIT_TEMPLATE, Permission.VIEW_TEMPLATE,
        
        // Domain permissions
        Permission.CREATE_DOMAIN, Permission.EDIT_DOMAIN, Permission.VIEW_DOMAIN,
        
        // API Key permissions
        Permission.CREATE_API_KEY, Permission.VIEW_API_KEY, Permission.REVOKE_API_KEY,
        
        // User management
        Permission.CREATE_USER_TENANT, Permission.VIEW_ALL_USERS, Permission.EDIT_USER_TENANT,
        
        // Email management
        Permission.VIEW_ALL_EMAILS, Permission.RETRY_EMAIL,
        
        // System access
        Permission.VIEW_SYSTEM_STATS, Permission.ACCESS_ALL_APIS,
        
        // Audit access
        Permission.VIEW_AUDIT_LOGS, Permission.VIEW_SYSTEM_METRICS
    );
    
    private static final Set<Permission> USER_TENANT_PERMISSIONS = EnumSet.of(
        // Template permissions (for their tenant only)
        Permission.CREATE_TEMPLATE, Permission.EDIT_TEMPLATE, Permission.VIEW_TEMPLATE,
        
        // Domain permissions (for their tenant only)
        Permission.CREATE_DOMAIN, Permission.EDIT_DOMAIN, Permission.VIEW_DOMAIN,
        
        // API Key permissions (for their tenant only)
        Permission.CREATE_API_KEY, Permission.VIEW_API_KEY,
        
        // Email permissions (for their tenant only)
        Permission.VIEW_TENANT_EMAILS,
        
        // User management (view their tenant users only)
        Permission.VIEW_TENANT_USERS
    );
    
    /**
     * Check if a user has a specific permission
     */
    public boolean hasPermission(User user, Permission permission) {
        if (user == null || user.getRole() == null) {
            return false;
        }
        
        Set<Permission> userPermissions = getPermissionsForRole(user.getRole());
        return userPermissions.contains(permission);
    }
    
    /**
     * Get all permissions for a specific role
     */
    public Set<Permission> getPermissionsForRole(UserRole role) {
        return switch (role) {
            case SUPERADMIN -> SUPERADMIN_PERMISSIONS;
            case ADMIN -> ADMIN_PERMISSIONS;
            case USER_TENANT -> USER_TENANT_PERMISSIONS;
        };
    }
    
    /**
     * Check if a user can manage another user (for user management operations)
     */
    public boolean canManageUser(User currentUser, User targetUser) {
        if (currentUser == null || targetUser == null) {
            return false;
        }
        
        UserRole currentRole = currentUser.getRole();
        UserRole targetRole = targetUser.getRole();
        
        return switch (currentRole) {
            case SUPERADMIN -> true; // Can manage anyone
            case ADMIN -> {
                // Can manage USER_TENANT users and other ADMIN users (but not SUPERADMIN)
                yield targetRole == UserRole.USER_TENANT || targetRole == UserRole.ADMIN;
            }
            case USER_TENANT -> {
                // Can only manage users in their own tenant (if we implement sub-users)
                yield false; // For now, USER_TENANT cannot manage other users
            }
        };
    }
    
    /**
     * Check if a user can access a resource belonging to a specific tenant
     */
    public boolean canAccessTenantResource(User user, Long tenantId) {
        if (user == null) {
            return false;
        }
        
        return switch (user.getRole()) {
            case SUPERADMIN, ADMIN -> true; // Can access any tenant's resources
            case USER_TENANT -> {
                // Can only access their own tenant's resources
                yield user.getTenant() != null && user.getTenant().getId().equals(tenantId);
            }
        };
    }
    
    /**
     * Check if a user can create users of a specific role
     */
    public boolean canCreateUserWithRole(User currentUser, UserRole targetRole) {
        if (currentUser == null) {
            return false;
        }
        
        return switch (currentUser.getRole()) {
            case SUPERADMIN -> true; // Can create any role
            case ADMIN -> targetRole == UserRole.USER_TENANT; // Can only create USER_TENANT
            case USER_TENANT -> false; // Cannot create any users
        };
    }
    
    /**
     * Validate that a user has the required permission, throw exception if not
     */
    public void requirePermission(User user, Permission permission) {
        if (!hasPermission(user, permission)) {
            throw new SecurityException(
                String.format("User %s does not have permission %s", 
                             user != null ? user.getUsername() : "null", permission));
        }
    }
    
    /**
     * Validate that a user can manage another user, throw exception if not
     */
    public void requireUserManagementPermission(User currentUser, User targetUser) {
        if (!canManageUser(currentUser, targetUser)) {
            throw new SecurityException(
                String.format("User %s cannot manage user %s", 
                             currentUser != null ? currentUser.getUsername() : "null",
                             targetUser != null ? targetUser.getUsername() : "null"));
        }
    }
    
    /**
     * Validate that a user can access tenant resources, throw exception if not
     */
    public void requireTenantAccess(User user, Long tenantId) {
        if (!canAccessTenantResource(user, tenantId)) {
            throw new SecurityException(
                String.format("User %s cannot access tenant %s resources", 
                             user != null ? user.getUsername() : "null", tenantId));
        }
    }
}
