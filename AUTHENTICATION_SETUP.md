# Authentication Setup

## Overview
The Email Service API uses different authentication methods for different endpoints to provide optimal security and user experience.

## Authentication Methods

### 1. API Key Authentication
**Used for**: `/v1/emails/**` endpoints only
**Header**: `X-Api-Key: your-api-key-here`
**Purpose**: Client applications and integrations

### 2. JWT Authentication  
**Used for**: All other authenticated endpoints
**Header**: `Authorization: Bearer your-jwt-token-here`
**Purpose**: User-facing operations and admin functions

## Endpoint Authentication Matrix

| Endpoint Category | Authentication Method | Header | Purpose |
|------------------|----------------------|--------|---------|
| `/v1/emails/**` | API Key | `X-Api-Key` | Email sending and management |
| `/v1/auth/**` | None (Public) | - | Registration, login, API key generation |
| `/v1/templates/**` | JWT | `Authorization: Bearer` | Template management |
| `/v1/attachments/**` | JWT | `Authorization: Bearer` | Attachment management |
| `/v1/admin/**` | JWT (Admin Role) | `Authorization: Bearer` | Administrative functions |
| `/track/**` | None (Public) | - | Email tracking (pixels, clicks) |
| `/actuator/**` | None (Public) | - | Health checks and monitoring |

## Security Filter Chains

### API Key Filter Chain
- **Scope**: `/v1/emails/**` only
- **Filter**: `ApiKeyAuthenticationFilter`
- **Purpose**: Validates API keys for email operations

### JWT Filter Chain  
- **Scope**: All other authenticated endpoints
- **Filter**: `JwtAuthenticationFilter`
- **Purpose**: Validates JWT tokens for user operations

## Usage Examples

### Email Operations (API Key)
```bash
# Send email
curl -X POST http://localhost:8080/v1/emails/send \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: your-api-key-here" \
  -d '{
    "from": "sender@example.com",
    "to": "recipient@example.com",
    "subject": "Test Email",
    "body": "Hello World"
  }'

# Get email status
curl -X GET http://localhost:8080/v1/emails/123 \
  -H "X-Api-Key: your-api-key-here"
```

### User Operations (JWT)
```bash
# Create template
curl -X POST http://localhost:8080/v1/templates \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "name": "welcome-template",
    "subjectTemplate": "Welcome {{name}}!",
    "bodyTemplate": "<h1>Welcome {{name}}!</h1>",
    "bodyType": "HTML"
  }'

# Upload attachment
curl -X POST http://localhost:8080/v1/attachments/upload-base64 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -d '{
    "base64Content": "base64-encoded-content",
    "filename": "document.pdf",
    "contentType": "application/pdf"
  }'
```

### Admin Operations (JWT + Admin Role)
```bash
# Get system statistics
curl -X GET http://localhost:8080/v1/admin/statistics \
  -H "Authorization: Bearer admin-jwt-token-here"
```

## Authentication Flow

### For Client Applications
1. **Register User** (Public endpoint)
2. **Login User** (Public endpoint) → Get JWT token
3. **Generate API Key** (JWT required) → Get API key
4. **Use API Key** for email operations

### For User Applications
1. **Register User** (Public endpoint)
2. **Login User** (Public endpoint) → Get JWT token
3. **Use JWT Token** for templates, attachments, admin functions

## Benefits of This Setup

### Security
- **Separation of Concerns**: Different auth methods for different use cases
- **API Key Isolation**: Email operations isolated with API keys
- **JWT for User Operations**: User-facing operations use JWT
- **Role-Based Access**: Admin functions require admin role

### User Experience
- **Client Integration**: API keys are perfect for client applications
- **User Interface**: JWT tokens work well for user-facing applications
- **Flexibility**: Users can choose the appropriate authentication method

### Performance
- **Optimized Filters**: Each filter chain only processes relevant requests
- **Reduced Overhead**: No unnecessary authentication processing
- **Scalable**: Can handle high-volume email operations with API keys

## Error Responses

### Invalid API Key
```json
{
  "timestamp": "2025-09-18T14:27:15.123456",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid API key",
  "path": "/v1/emails/send"
}
```

### Invalid JWT Token
```json
{
  "timestamp": "2025-09-18T14:27:15.123456",
  "status": 401,
  "error": "Unauthorized", 
  "message": "Invalid JWT token",
  "path": "/v1/templates"
}
```

### Insufficient Privileges
```json
{
  "timestamp": "2025-09-18T14:27:15.123456",
  "status": 403,
  "error": "Forbidden",
  "message": "Access denied. Admin role required.",
  "path": "/v1/admin/statistics"
}
```

## Postman Collection Updates

The Postman collection has been updated to reflect the new authentication setup:

- **Email Sending**: Uses API key authentication
- **Email Management**: Uses API key authentication  
- **Template Management**: Uses JWT authentication
- **Attachment Management**: Uses JWT authentication
- **Admin Functions**: Uses JWT authentication with admin role
- **Authentication**: Public endpoints for registration, login, API key generation

## Migration Notes

### Breaking Changes
- **Email endpoints** now require API key authentication instead of JWT
- **Template/Attachment endpoints** now require JWT authentication instead of API key

### Migration Steps
1. **Update Client Applications**: Use API keys for email operations
2. **Update User Applications**: Use JWT tokens for template/attachment operations
3. **Update Tests**: Ensure tests use correct authentication methods
4. **Update Documentation**: Update API documentation to reflect new auth requirements


