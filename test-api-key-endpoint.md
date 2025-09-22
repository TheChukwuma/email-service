# API Key Generation Endpoint Test

## Endpoint Added
- **POST** `/v1/auth/api-key` - Generate API key for authenticated user

## Test the Endpoint

### 1. Register a User
```bash
curl -X POST http://localhost:8080/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "isAdmin": false
  }'
```

### 2. Login to Get JWT Token (using username or email)
```bash
# Login with username
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'

# OR login with email
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "password123"
  }'
```

### 3. Generate API Key (using JWT token from step 2)
```bash
curl -X POST http://localhost:8080/v1/auth/api-key \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE" \
  -d '{
    "keyName": "Test API Key",
    "expiresAt": "2025-12-31T23:59:59"
  }'
```

### Expected Response
```json
{
  "success": true,
  "message": "API key generated successfully",
  "data": {
    "apiKey": "generated-api-key-string",
    "keyName": "Test API Key",
    "expiresAt": "2025-12-31T23:59:59",
    "createdAt": "2025-09-18T13:38:17.123456"
  }
}
```

### 4. Test API Key Authentication
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_GENERATED_API_KEY_HERE" \
  -d '{
    "from": "test@example.com",
    "to": "recipient@example.com",
    "subject": "Test Email",
    "body": "This is a test email using API key authentication."
  }'
```

## Security Notes
- The plain API key is only returned once during generation
- API keys are hashed and stored in the database
- API keys can have expiration dates
- Users can only generate API keys for themselves
- JWT authentication is required to generate API keys

## Postman Collection
The Postman collection has been updated to include the API key generation test in the Authentication folder.
