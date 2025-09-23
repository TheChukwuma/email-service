package com.octopus.email_service.enums;

/**
 * User roles in the email service system
 */
public enum UserRole {
    /**
     * Third-party client admin users who manage their organization's email settings.
     * They can create templates, domains, and API keys for their tenant.
     */
    USER_TENANT,
    
    /**
     * System administrators who can manage user tenants and access most system features.
     * Cannot create or modify other admins or superadmins.
     */
    ADMIN,
    
    /**
     * Super administrators with full system access.
     * Can create and manage all user types including other admins.
     */
    SUPERADMIN
}
