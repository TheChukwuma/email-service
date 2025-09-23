-- Create email tenants table
CREATE TABLE IF NOT EXISTS email_tenants (
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

-- Create email domains table for tenant domain verification
CREATE TABLE IF NOT EXISTS email_domains (
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

-- Add foreign key constraints for tenant relationships (fields already exist in V1)
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES email_tenants(id) ON DELETE SET NULL;
ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_tenant FOREIGN KEY (tenant_id) REFERENCES email_tenants(id) ON DELETE SET NULL;
ALTER TABLE templates ADD CONSTRAINT fk_templates_tenant FOREIGN KEY (tenant_id) REFERENCES email_tenants(id) ON DELETE CASCADE;

-- Create indexes for better performance
CREATE INDEX idx_tenant_code ON email_tenants(tenant_code);
CREATE INDEX idx_tenant_active ON email_tenants(is_active);
CREATE UNIQUE INDEX idx_tenant_domain ON email_domains(domain, tenant_id);
CREATE INDEX idx_domain_verified ON email_domains(is_verified);
CREATE INDEX idx_api_key_tenant ON api_keys(tenant_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_templates_tenant ON templates(tenant_id);

-- Insert some sample tenants for testing
INSERT INTO email_tenants (tenant_code, tenant_name, default_sender_email, default_sender_name, default_reply_to_email, default_reply_to_name, is_active) VALUES
('unionbank', 'Union Bank of Nigeria', 'no-reply@unionbank.com', 'Union Bank', 'support@unionbank.com', 'Union Bank Support', true),
('gtbank', 'Guaranty Trust Bank', 'noreply@gtbank.com', 'GT Bank', 'customercare@gtbank.com', 'GT Bank Customer Care', true),
('zenithbank', 'Zenith Bank Plc', 'alerts@zenithbank.com', 'Zenith Bank', 'help@zenithbank.com', 'Zenith Bank Help', true);

-- Add comments for documentation
COMMENT ON TABLE email_tenants IS 'Stores tenant organizations that can send emails through the service';
COMMENT ON TABLE email_domains IS 'Stores verified domains for each tenant';
COMMENT ON COLUMN email_tenants.tenant_code IS 'Unique identifier for the tenant (e.g., unionbank, gtbank)';
COMMENT ON COLUMN email_tenants.domain_verified IS 'Whether the tenant has at least one verified domain';
COMMENT ON COLUMN email_domains.verification_token IS 'Token used for domain ownership verification';
COMMENT ON COLUMN email_domains.spf_verified IS 'Whether SPF record is correctly configured';
COMMENT ON COLUMN email_domains.dkim_verified IS 'Whether DKIM is properly set up';
