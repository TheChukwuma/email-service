# Role-Based Access Control (RBAC) System

## Overview

The email service now implements a comprehensive role-based access control system with three distinct user roles, each with specific permissions and access levels.

## User Roles

### 1. SUPERADMIN
- **Purpose**: System superadministrators with full system access
- **Capabilities**: 
  - Create and manage all user types (ADMIN, USER_TENANT)
  - Access all system APIs and resources
  - View system-wide audit logs and metrics
  - Manage system settings
- **Restrictions**: None
- **Tenant Association**: None (operates at system level)

### 2. ADMIN
- **Purpose**: System administrators who manage tenant users and system operations
- **Capabilities**:
  - Create and edit USER_TENANT users
  - View all users in the system
  - Access all templates and domains
  - Generate API keys for any tenant
  - View system statistics and audit logs
- **Restrictions**: 
  - Cannot create or modify other ADMIN or SUPERADMIN users
  - Cannot delete users (only deactivate)
- **Tenant Association**: None (operates at system level)

### 3. USER_TENANT
- **Purpose**: Third-party client administrators who manage their organization's email settings
- **Capabilities**:
  - Create, edit, and view templates for their tenant
  - Create, edit, and view domains for their tenant
  - Generate API keys for their tenant
  - View emails sent by their tenant
  - View users within their tenant organization
- **Restrictions**: 
  - Can only access resources belonging to their tenant
  - Cannot create other users
  - Cannot access system-wide settings or logs
- **Tenant Association**: Must be associated with an EmailTenant

## SuperAdmin Setup Security

### Security Measures

The SuperAdmin setup process implements multiple security layers:

1. **Environment Secret**: Requires a secure setup secret configured via environment variable
2. **Time Window**: Setup is only allowed within 30 minutes of application startup
3. **IP Whitelisting**: By default, only allows localhost connections
4. **One-Time Setup**: After the first SuperAdmin is created, the endpoint is permanently disabled
5. **Audit Logging**: All setup attempts are logged with IP addresses and details

### Setup Process

1. **Configure Environment Variables**:
   ```bash
   # Required: Setup secret (minimum 32 characters recommended)
   SUPERADMIN_SETUP_SECRET=your-very-secure-secret-key-here
   
   # Optional: Allowed IP addresses (defaults to localhost)
   SUPERADMIN_SETUP_ALLOWED_IPS=127.0.0.1,::1,192.168.1.100
   
   # Optional: Setup window in minutes (default: 30)
   SUPERADMIN_SETUP_WINDOW_MINUTES=30
   ```

2. **Start the Application**: The setup window begins when the application starts

3. **Check Setup Status**:
   ```bash
   curl http://localhost:8080/v1/setup/status
   ```

4. **Create SuperAdmin**:
   ```bash
   curl -X POST http://localhost:8080/v1/setup/superadmin \
     -H "Content-Type: application/json" \
     -d '{
       "username": "superadmin",
       "email": "admin@yourcompany.com", 
       "password": "SecurePassword123!",
       "setupSecret": "your-very-secure-secret-key-here",
       "organizationName": "Your Company",
       "adminName": "Admin Name"
     }'
   ```

### Production Deployment Recommendations

1. **Use Strong Secrets**: Generate a cryptographically secure setup secret
2. **Restrict Network Access**: Use firewalls to limit setup endpoint access
3. **Container Orchestration**: For Docker/K8s, consider init containers for setup
4. **Monitor Setup Attempts**: Watch audit logs for unauthorized setup attempts
5. **Secure Secret Storage**: Use secret management systems (Vault, K8s secrets, etc.)

## API Authentication

### JWT Authentication
- Used for user operations (templates, user management, etc.)
- Contains user role and tenant information
- Required for all authenticated endpoints

### API Key Authentication  
- Used for email sending operations (backward compatible)
- API keys are tenant-scoped for USER_TENANT users
- No changes to existing email sending workflow

## Permission System

### Permission Matrix

| Permission | SUPERADMIN | ADMIN | USER_TENANT |
|------------|------------|-------|-------------|
| CREATE_TEMPLATE | ✅ | ✅ | ✅ (own tenant) |
| EDIT_TEMPLATE | ✅ | ✅ | ✅ (own tenant) |
| VIEW_TEMPLATE | ✅ | ✅ | ✅ (own tenant) |
| DELETE_TEMPLATE | ✅ | ❌ | ❌ |
| CREATE_DOMAIN | ✅ | ✅ | ✅ (own tenant) |
| EDIT_DOMAIN | ✅ | ✅ | ✅ (own tenant) |
| CREATE_API_KEY | ✅ | ✅ | ✅ (own tenant) |
| CREATE_USER_TENANT | ✅ | ✅ | ❌ |
| CREATE_ADMIN | ✅ | ❌ | ❌ |
| VIEW_ALL_USERS | ✅ | ✅ | ❌ |
| VIEW_TENANT_USERS | ✅ | ✅ | ✅ |
| VIEW_SYSTEM_STATS | ✅ | ✅ | ❌ |
| VIEW_AUDIT_LOGS | ✅ | ✅ | ❌ |

## Audit Logging

### Automatic Auditing
All user actions are automatically logged using AOP (Aspect-Oriented Programming):

- **User Information**: Username, role, tenant
- **Action Details**: Operation performed, resource affected
- **Request Context**: IP address, user agent, timestamp
- **Success/Failure**: Result and error messages if applicable

### Audited Actions
- User creation, modification, deletion
- Template operations
- Domain management  
- API key generation
- System configuration changes

### Audit Log Access
- **SUPERADMIN**: Full access to all audit logs
- **ADMIN**: Access to system audit logs
- **USER_TENANT**: No access to audit logs

## Database Schema Changes

### New Tables
- `audit_logs`: Comprehensive audit trail
- `system_settings`: System configuration
- Role field added to `users` table
- Tenant relationship added to `users` table
- Tenant relationship added to `templates` table

### Migration
- Run `V1.3__Add_user_roles_and_audit_system.sql` migration
- Existing users will need role assignment
- Existing templates become tenant-scoped

## API Endpoints

### SuperAdmin Setup
- `GET /v1/setup/status` - Check setup status
- `POST /v1/setup/superadmin` - Create SuperAdmin (time-limited)
- `GET /v1/setup/required` - Check if setup is needed

### User Management
- `POST /v1/admin/users` - Create users (ADMIN, SUPERADMIN only)
- `GET /v1/admin/users` - List users (role-filtered)
- `PUT /v1/admin/users/{id}` - Update users
- `DELETE /v1/admin/users/{id}` - Deactivate users

### Authentication
- `POST /v1/auth/login` - Login (returns role information)
- `POST /v1/auth/register` - Public registration (USER_TENANT only)

## Usage Examples

### 1. Initial Setup
```bash
# Start application with setup secret
export SUPERADMIN_SETUP_SECRET="my-super-secure-secret-key-123"
./run-application.sh

# Check if setup is needed
curl http://localhost:8080/v1/setup/status

# Create SuperAdmin
curl -X POST http://localhost:8080/v1/setup/superadmin \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","email":"admin@company.com","password":"SecurePass123!","setupSecret":"my-super-secure-secret-key-123"}'
```

### 2. SuperAdmin Creates Admin
```bash
# Login as SuperAdmin
TOKEN=$(curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"superadmin","password":"SecurePass123!"}' | jq -r '.data.token')

# Create Admin user
curl -X POST http://localhost:8080/v1/admin/users \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin1",
    "email": "admin1@company.com",
    "password": "AdminPass123!",
    "role": "ADMIN"
  }'
```

### 3. Admin Creates Tenant User
```bash
# Login as Admin
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin1","password":"AdminPass123!"}' | jq -r '.data.token')

# Create Tenant User
curl -X POST http://localhost:8080/v1/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "bankuser",
    "email": "admin@bank.com", 
    "password": "BankPass123!",
    "role": "USER_TENANT",
    "tenantId": 1
  }'
```

### 4. Tenant User Operations
```bash
# Login as Tenant User
TENANT_TOKEN=$(curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"bankuser","password":"BankPass123!"}' | jq -r '.data.token')

# Create Template (scoped to tenant)
curl -X POST http://localhost:8080/v1/templates \
  -H "Authorization: Bearer $TENANT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "welcome-email",
    "subjectTemplate": "Welcome {{name}}!",
    "bodyTemplate": "<h1>Welcome {{name}}!</h1><p>Thanks for joining {{company}}</p>",
    "bodyType": "HTML"
  }'
```

## Backward Compatibility

### Existing API Keys
- Continue to work unchanged for email sending
- Existing API keys remain functional
- No impact on current email operations

### Migration Strategy
1. Deploy new code with role system
2. Run database migration
3. Assign roles to existing users
4. Update client applications to use new user management APIs
5. Gradually migrate to tenant-scoped operations

## Security Best Practices

### For Production Deployment
1. **Secure Secrets**: Use strong, unique setup secrets
2. **Network Security**: Restrict setup endpoint access
3. **Monitoring**: Monitor setup attempts and failures
4. **Backup**: Ensure database backups before migration
5. **Testing**: Test role permissions thoroughly in staging

### For Ongoing Operations
1. **Principle of Least Privilege**: Assign minimal required roles
2. **Regular Auditing**: Review audit logs regularly
3. **Access Reviews**: Periodically review user access
4. **Secure Communication**: Use HTTPS in production
5. **Token Management**: Implement proper JWT refresh and expiration
