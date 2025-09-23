# Database Schema Documentation

## Overview

The email service uses PostgreSQL with Flyway migrations. The schema supports multi-tenancy, role-based access control, email tracking, and audit logging.

## Migration Files

- `V1.0__Base_Script.sql` - Base migration setup
- `V1.1__Create_initial_tables.sql` - Core tables for users, emails, templates, API keys, attachments, and events
- `V1.2__Create_tenant_email_tables.sql` - Tenant system and email domains
- `V1.3__Add_user_roles_and_audit_system.sql` - Audit logging and system settings

## Core Tables

### users
Stores user accounts with role-based access control.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER_TENANT' 
        CHECK (role IN ('USER_TENANT', 'ADMIN', 'SUPERADMIN')),
    tenant_id BIGINT REFERENCES email_tenants(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Key Features:**
- Three-tier role system: USER_TENANT, ADMIN, SUPERADMIN
- Tenant association for scoped access
- Soft delete via `is_active` flag

### emails
Stores email records with support for multiple recipients.

```sql
CREATE TABLE emails (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID UNIQUE NOT NULL DEFAULT gen_random_uuid(),
    from_address VARCHAR(255) NOT NULL,
    reply_to_address VARCHAR(255),
    to_addresses TEXT[] NOT NULL,  -- Multiple recipients support
    cc_addresses TEXT[],
    bcc_addresses TEXT[],
    subject VARCHAR(500) NOT NULL,
    template_name VARCHAR(100) REFERENCES templates(name),
    template_vars TEXT,
    body TEXT,
    attachments VARCHAR(50000),
    is_html_body BOOLEAN DEFAULT FALSE,
    needs_fallback_template BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) DEFAULT 'ENQUEUED' 
        CHECK (status IN ('ENQUEUED', 'SENDING', 'SENT', 'FAILED', 'BOUNCED', 'DELIVERED')),
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 5,
    last_error TEXT,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Key Features:**
- `to_addresses` array supports multiple recipients
- Email status tracking with retry logic
- Template variable support
- Attachment metadata storage

### email_to_addresses
Helper table for JPA @ElementCollection mapping.

```sql
CREATE TABLE email_to_addresses (
    email_id BIGINT NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    to_address VARCHAR(255) NOT NULL
);
```

### templates
Email templates with tenant scoping.

```sql
CREATE TABLE templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    subject_template TEXT NOT NULL,
    body_template TEXT NOT NULL,
    body_type VARCHAR(20) DEFAULT 'html' CHECK (body_type IN ('html', 'text')),
    created_by BIGINT REFERENCES users(id),
    tenant_id BIGINT REFERENCES email_tenants(id) ON DELETE CASCADE,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Key Features:**
- Tenant-scoped templates
- Support for both HTML and text templates
- Template variable substitution

### api_keys
API key management with tenant association.

```sql
CREATE TABLE api_keys (
    id BIGSERIAL PRIMARY KEY,
    key_hash VARCHAR(255) UNIQUE NOT NULL,
    key_name VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL,
    client_application VARCHAR(100) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    tenant_id BIGINT REFERENCES email_tenants(id) ON DELETE SET NULL,
    is_active BOOLEAN DEFAULT true,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Multi-Tenancy Tables

### email_tenants
Tenant organizations that can send emails.

```sql
CREATE TABLE email_tenants (
    id BIGSERIAL PRIMARY KEY,
    tenant_code VARCHAR(50) UNIQUE NOT NULL,
    tenant_name VARCHAR(255) NOT NULL,
    default_sender_email VARCHAR(255),
    default_sender_name VARCHAR(255),
    default_reply_to_email VARCHAR(255),
    default_reply_to_name VARCHAR(255),
    domain_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### email_domains
Domain verification for tenants.

```sql
CREATE TABLE email_domains (
    id BIGSERIAL PRIMARY KEY,
    domain VARCHAR(255) NOT NULL,
    tenant_id BIGINT REFERENCES email_tenants(id) ON DELETE CASCADE,
    domain_reply_to_email VARCHAR(255),
    domain_reply_to_name VARCHAR(255),
    is_verified BOOLEAN DEFAULT false,
    verification_token VARCHAR(255),
    spf_verified BOOLEAN DEFAULT false,
    dkim_verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Audit and System Tables

### audit_logs
Comprehensive audit trail for all user actions.

```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255),
    description TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSONB,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### system_settings
System-wide configuration settings.

```sql
CREATE TABLE system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Email Tracking Tables

### email_events
Email lifecycle events tracking.

```sql
CREATE TABLE email_events (
    id BIGSERIAL PRIMARY KEY,
    email_id BIGINT NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL 
        CHECK (event_type IN ('ENQUEUED', 'SENDING', 'SENT', 'DELIVERED', 'BOUNCED', 
                              'OPEN', 'CLICK', 'SOFT_BOUNCE', 'HARD_BOUNCE', 'FAILED')),
    detail JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### blacklist
Email address blacklist management.

```sql
CREATE TABLE blacklist (
    id BIGSERIAL PRIMARY KEY,
    email_address VARCHAR(255) UNIQUE NOT NULL,
    reason VARCHAR(100),
    blacklist_type VARCHAR(20) DEFAULT 'HARD_BOUNCE' 
        CHECK (blacklist_type IN ('HARD_BOUNCE', 'COMPLAINT', 'MANUAL')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## Attachment Storage

### attachments
File attachment metadata and storage information.

```sql
CREATE TABLE attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    original_filename VARCHAR(500) NOT NULL,
    stored_filename VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_type VARCHAR(20) NOT NULL CHECK (storage_type IN ('MINIO', 'CLOUDINARY', 'LOCAL')),
    storage_path VARCHAR(1000) NOT NULL,
    cloudinary_public_id VARCHAR(500),
    cloudinary_url VARCHAR(1000),
    minio_bucket VARCHAR(100),
    minio_object_key VARCHAR(500),
    checksum VARCHAR(500),
    is_inline BOOLEAN DEFAULT FALSE,
    content_id VARCHAR(100),
    description TEXT,
    is_processed BOOLEAN NOT NULL DEFAULT FALSE,
    processing_error TEXT,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);
```

## Key Indexes

### Performance Indexes
```sql
-- Email querying and processing
CREATE INDEX idx_emails_status ON emails(status);
CREATE INDEX idx_emails_created_at ON emails(created_at);
CREATE INDEX idx_emails_uuid ON emails(uuid);
CREATE INDEX idx_emails_to_addresses ON emails USING GIN(to_addresses);

-- Email recipients (for JPA collection)
CREATE INDEX idx_email_to_addresses_email_id ON email_to_addresses(email_id);
CREATE INDEX idx_email_to_addresses_address ON email_to_addresses(to_address);

-- Event tracking
CREATE INDEX idx_email_events_email_id ON email_events(email_id);
CREATE INDEX idx_email_events_event_type ON email_events(event_type);

-- User and tenant relationships
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_templates_tenant ON templates(tenant_id);
CREATE INDEX idx_api_key_tenant ON api_keys(tenant_id);

-- Audit logging
CREATE INDEX idx_audit_username ON audit_logs(username);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
```

## Foreign Key Constraints

```sql
-- Tenant relationships
ALTER TABLE users ADD CONSTRAINT fk_users_tenant 
    FOREIGN KEY (tenant_id) REFERENCES email_tenants(id) ON DELETE SET NULL;
    
ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_tenant 
    FOREIGN KEY (tenant_id) REFERENCES email_tenants(id) ON DELETE SET NULL;
    
ALTER TABLE templates ADD CONSTRAINT fk_templates_tenant 
    FOREIGN KEY (tenant_id) REFERENCES email_tenants(id) ON DELETE CASCADE;
```

## Data Types and Constraints

### Email Address Arrays
- `to_addresses`, `cc_addresses`, `bcc_addresses` use PostgreSQL `TEXT[]` arrays
- Supports multiple email addresses per field
- GIN indexes for efficient array searches

### JSON Storage
- `audit_logs.details` uses JSONB for structured audit data
- `email_events.detail` uses JSONB for event metadata
- Supports efficient querying with PostgreSQL JSON operators

### UUID Usage
- Email UUIDs for public API exposure
- Attachment UUIDs for file identification
- Prevents ID enumeration attacks

## Security Considerations

1. **Password Storage**: BCrypt hashed passwords in `password_hash`
2. **API Key Storage**: Hashed API keys in `key_hash`
3. **Soft Deletes**: `is_active` flags prevent data loss
4. **Audit Trail**: Complete action logging in `audit_logs`
5. **Tenant Isolation**: Foreign key constraints ensure data isolation
6. **UUID Exposure**: Internal IDs hidden, UUIDs used for external APIs

## Migration Strategy

The database uses Flyway for version control:

1. **V1.0**: Base migration setup
2. **V1.1**: Core email functionality and user system
3. **V1.2**: Multi-tenancy and domain verification
4. **V1.3**: Audit logging and system administration

All migrations are backward compatible and can be applied incrementally.
