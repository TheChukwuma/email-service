-- Role and tenant_id fields are now created in V1 and V3 migrations
-- This migration focuses on audit system and stored procedures

-- Create system_settings table for managing system-wide configurations
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) UNIQUE NOT NULL,
    setting_value TEXT,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert system setting to track if superadmin setup is completed
INSERT INTO system_settings (setting_key, setting_value, description) VALUES
('superadmin_setup_completed', 'false', 'Flag to indicate if the initial superadmin setup has been completed');

-- Create audit_logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    username VARCHAR(50) NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    details TEXT,
    ip_address INET,
    user_agent TEXT,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance (user indexes are in V3)
CREATE INDEX idx_audit_username ON audit_logs(username);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_resource_type ON audit_logs(resource_type);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_user_tenant ON audit_logs(user_id) WHERE user_id IS NOT NULL;

-- Template tenant scoping is now handled in V3 migration

-- Create a stored procedure to create the first superadmin
CREATE OR REPLACE FUNCTION create_superadmin(
    p_username VARCHAR(50),
    p_email VARCHAR(255),
    p_password_hash VARCHAR(255),
    p_setup_secret VARCHAR(255)
) 
RETURNS TABLE(success BOOLEAN, message TEXT, user_id BIGINT) 
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_setup_completed TEXT;
    v_expected_secret TEXT;
    v_user_id BIGINT;
BEGIN
    -- Check if superadmin setup is already completed
    SELECT setting_value INTO v_setup_completed 
    FROM system_settings 
    WHERE setting_key = 'superadmin_setup_completed';
    
    IF v_setup_completed = 'true' THEN
        RETURN QUERY SELECT FALSE, 'SuperAdmin setup has already been completed', NULL::BIGINT;
        RETURN;
    END IF;
    
    -- Get expected setup secret from environment (this would be set in application)
    -- For now, we'll validate this in the application layer
    
    -- Check if username already exists
    IF EXISTS (SELECT 1 FROM users WHERE username = p_username) THEN
        RETURN QUERY SELECT FALSE, 'Username already exists', NULL::BIGINT;
        RETURN;
    END IF;
    
    -- Check if email already exists
    IF EXISTS (SELECT 1 FROM users WHERE email = p_email) THEN
        RETURN QUERY SELECT FALSE, 'Email already exists', NULL::BIGINT;
        RETURN;
    END IF;
    
    -- Create the superadmin user
    INSERT INTO users (username, email, password_hash, role, is_active)
    VALUES (p_username, p_email, p_password_hash, 'SUPERADMIN', TRUE)
    RETURNING id INTO v_user_id;
    
    -- Mark superadmin setup as completed
    UPDATE system_settings 
    SET setting_value = 'true', updated_at = CURRENT_TIMESTAMP
    WHERE setting_key = 'superadmin_setup_completed';
    
    -- Log the superadmin creation
    INSERT INTO audit_logs (user_id, username, user_role, action, resource_type, resource_id, details, success)
    VALUES (v_user_id, p_username, 'SUPERADMIN', 'CREATE_SUPERADMIN', 'USER', v_user_id::TEXT, 
            '{"description": "Initial superadmin user created", "setup": true}', TRUE);
    
    RETURN QUERY SELECT TRUE, 'SuperAdmin created successfully', v_user_id;
END;
$$;

-- Create a function to check if superadmin setup is completed
CREATE OR REPLACE FUNCTION is_superadmin_setup_completed()
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_setup_completed TEXT;
BEGIN
    SELECT setting_value INTO v_setup_completed 
    FROM system_settings 
    WHERE setting_key = 'superadmin_setup_completed';
    
    RETURN COALESCE(v_setup_completed = 'true', FALSE);
END;
$$;

-- Add comments for documentation
COMMENT ON TABLE audit_logs IS 'Comprehensive audit trail for all user actions in the system';
COMMENT ON TABLE system_settings IS 'System-wide configuration settings';
COMMENT ON COLUMN users.role IS 'User role: USER_TENANT, ADMIN, or SUPERADMIN';
COMMENT ON COLUMN users.tenant_id IS 'Associated tenant for USER_TENANT role users';
COMMENT ON COLUMN templates.tenant_id IS 'Tenant that owns this template (null for global templates)';
COMMENT ON FUNCTION create_superadmin IS 'Securely creates the initial superadmin user with validation';
