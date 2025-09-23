# Login with Email or Username

## Overview
The login endpoint now supports authentication using either username or email in the `username` field. The system will automatically detect whether the provided value is a username or email and authenticate accordingly.

## API Endpoint
- **POST** `/v1/auth/login`
- **Content-Type**: `application/json`

## Request Format
```json
{
  "username": "username_or_email",
  "password": "password"
}
```

## Examples

### 1. Login with Username
```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 2. Login with Email
```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "password123"
  }'
```

## Response Format
Both login methods return the same response format:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "testuser"
  }
}
```

## How It Works

### Authentication Flow
1. **Username First**: System first tries to find user by username
2. **Email Fallback**: If username not found, tries to find user by email
3. **Password Verification**: Validates password against found user
4. **JWT Generation**: Generates JWT token for successful authentication

### Implementation Details
- **CustomUserDetailsService**: Updated to support both username and email lookup
- **UserRepository**: Uses existing `findActiveByUsername` and `findActiveByEmail` methods
- **Fallback Logic**: Uses Java Optional's `or()` method for clean fallback
- **Error Handling**: Clear error message when neither username nor email is found

## Security Considerations
- **Active Users Only**: Only active users can login (both methods check `isActive = true`)
- **Password Security**: Same password validation for both authentication methods
- **JWT Token**: Same token generation regardless of login method
- **User Identification**: Response always returns the actual username, not the email used for login

## Testing

### Postman Collection
The Postman collection includes two test cases:
1. **Login User with Username** - Tests username-based authentication
2. **Login User with Email** - Tests email-based authentication

### Test Scenarios
- ✅ Login with valid username
- ✅ Login with valid email
- ✅ Login with invalid username/email
- ✅ Login with wrong password
- ✅ Login with inactive user account

## Error Responses

### User Not Found
```json
{
  "success": false,
  "message": "Invalid credentials"
}
```

### Wrong Password
```json
{
  "success": false,
  "message": "Invalid credentials"
}
```

### Inactive User
```json
{
  "success": false,
  "message": "Invalid credentials"
}
```

## Benefits
- **User Flexibility**: Users can login with either username or email
- **Backward Compatibility**: Existing username-based logins continue to work
- **No Breaking Changes**: Request format remains the same
- **Better UX**: Users don't need to remember which identifier to use
- **Consistent API**: Same endpoint and response format for both methods


