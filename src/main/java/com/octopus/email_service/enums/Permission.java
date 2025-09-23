package com.octopus.email_service.enums;

/**
 * System permissions that can be assigned to user roles
 */
public enum Permission {
    // Template Management
    CREATE_TEMPLATE,
    EDIT_TEMPLATE,
    VIEW_TEMPLATE,
    DELETE_TEMPLATE,
    
    // Domain Management  
    CREATE_DOMAIN,
    EDIT_DOMAIN,
    VIEW_DOMAIN,
    DELETE_DOMAIN,
    
    // API Key Management
    CREATE_API_KEY,
    VIEW_API_KEY,
    REVOKE_API_KEY,
    
    // User Management
    CREATE_USER_TENANT,
    CREATE_ADMIN,
    VIEW_ALL_USERS,
    VIEW_TENANT_USERS,
    EDIT_USER_TENANT,
    EDIT_ADMIN,
    DELETE_USER,
    
    // Email Management
    VIEW_ALL_EMAILS,
    VIEW_TENANT_EMAILS,
    RETRY_EMAIL,
    
    // System Management
    VIEW_SYSTEM_STATS,
    ACCESS_ALL_APIS,
    MANAGE_SYSTEM_SETTINGS,
    
    // Audit and Monitoring
    VIEW_AUDIT_LOGS,
    VIEW_SYSTEM_METRICS
}
