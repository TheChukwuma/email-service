# Migration Guide: Single to Multiple TO Addresses

## Overview

This guide helps you migrate from the old single TO address system to the new multiple TO addresses system introduced in the latest version of the email service.

## What Changed

### API Request Format

**Old Format (Single TO Address):**
```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "Test Email",
  "body": "Email content"
}
```

**New Format (Multiple TO Addresses):**
```json
{
  "from": "sender@example.com",
  "to": ["recipient@example.com"],
  "subject": "Test Email",
  "body": "Email content"
}
```

### Database Schema Changes

The `emails` table has been updated:

**Old Schema:**
```sql
to_address VARCHAR(255) NOT NULL
```

**New Schema:**
```sql
to_addresses TEXT[] NOT NULL
```

Additional helper table for JPA mapping:
```sql
CREATE TABLE email_to_addresses (
    email_id BIGINT NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    to_address VARCHAR(255) NOT NULL
);
```

### API Response Changes

**Old Response:**
```json
{
  "id": 123,
  "toAddress": "recipient@example.com",
  "status": "SENT"
}
```

**New Response:**
```json
{
  "id": 123,
  "toAddresses": ["recipient@example.com"],
  "status": "SENT"
}
```

## Migration Steps

### 1. Update Client Code

#### Java/Spring Boot Clients
```java
// Old way
EmailRequest request = EmailRequest.builder()
    .from("sender@example.com")
    .to("recipient@example.com")  // String
    .subject("Test")
    .body("Content")
    .build();

// New way  
EmailRequest request = EmailRequest.builder()
    .from("sender@example.com")
    .to(List.of("recipient@example.com"))  // List<String>
    .subject("Test")
    .body("Content")
    .build();
```

#### JavaScript/Node.js Clients
```javascript
// Old way
const emailData = {
  from: "sender@example.com",
  to: "recipient@example.com",  // string
  subject: "Test",
  body: "Content"
};

// New way
const emailData = {
  from: "sender@example.com", 
  to: ["recipient@example.com"],  // array
  subject: "Test",
  body: "Content"
};
```

#### Python Clients
```python
# Old way
email_data = {
    "from": "sender@example.com",
    "to": "recipient@example.com",  # string
    "subject": "Test",
    "body": "Content"
}

# New way
email_data = {
    "from": "sender@example.com",
    "to": ["recipient@example.com"],  # list
    "subject": "Test", 
    "body": "Content"
}
```

### 2. Update Response Parsing

#### Java/Spring Boot
```java
// Old way
String toAddress = response.getToAddress();

// New way
List<String> toAddresses = response.getToAddresses();
String firstRecipient = toAddresses.get(0);
```

#### JavaScript/Node.js
```javascript
// Old way
const recipient = response.toAddress;

// New way
const recipients = response.toAddresses;
const firstRecipient = recipients[0];
```

#### Python
```python
# Old way
recipient = response["toAddress"]

# New way
recipients = response["toAddresses"]
first_recipient = recipients[0]
```

### 3. Database Migration

The database migration is handled automatically by Flyway when you restart the application. However, if you have existing data, you may need to handle the migration manually.

#### Manual Data Migration (if needed)
```sql
-- Backup existing data
CREATE TABLE emails_backup AS SELECT * FROM emails;

-- The migration will automatically convert existing single TO addresses to arrays
-- No manual intervention needed for most cases
```

### 4. Testing Migration

#### Test Single Recipient (Backward Compatibility)
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "from": "test@example.com",
    "to": ["recipient@example.com"],
    "subject": "Migration Test",
    "body": "Testing single recipient with new format"
  }'
```

#### Test Multiple Recipients (New Feature)
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "from": "test@example.com",
    "to": ["user1@example.com", "user2@example.com", "user3@example.com"],
    "cc": ["manager@example.com"],
    "bcc": ["audit@example.com"],
    "subject": "Migration Test - Multiple Recipients", 
    "body": "Testing multiple recipients feature"
  }'
```

## New Features Available

### 1. Multiple Primary Recipients
Send to multiple people in the TO field:
```json
{
  "to": ["user1@example.com", "user2@example.com", "user3@example.com"]
}
```

### 2. Enhanced CC/BCC Support
Better support for carbon copy and blind carbon copy:
```json
{
  "to": ["primary@example.com"],
  "cc": ["manager@example.com", "supervisor@example.com"],
  "bcc": ["audit@example.com", "compliance@example.com"]
}
```

### 3. Improved Email Tracking
Each recipient is tracked individually for opens, clicks, and delivery status.

### 4. Better Analytics
- Per-recipient delivery statistics
- Individual bounce tracking
- Separate click/open tracking per recipient

## Common Issues and Solutions

### Issue 1: "to field must be an array" Error
**Problem:** Still sending TO address as string instead of array.

**Solution:** Update your request to use array format:
```json
// Wrong
"to": "user@example.com"

// Correct  
"to": ["user@example.com"]
```

### Issue 2: Response parsing errors
**Problem:** Code expects `toAddress` field but response has `toAddresses`.

**Solution:** Update response parsing to use the new field name and handle array:
```javascript
// Old
const recipient = response.toAddress;

// New
const recipients = response.toAddresses;
```

### Issue 3: Database query errors
**Problem:** Custom queries still reference the old `to_address` column.

**Solution:** Update queries to use new `to_addresses` array field:
```sql
-- Old
SELECT * FROM emails WHERE to_address = 'user@example.com';

-- New
SELECT * FROM emails WHERE 'user@example.com' = ANY(to_addresses);
```

## Rollback Plan

If you need to rollback to the previous version:

1. **Stop the application**
2. **Restore database from backup**
3. **Deploy previous version of the application**
4. **Update client code to use old format**

**Note:** Rolling back will lose any emails sent to multiple recipients, as the old system doesn't support this feature.

## Support

If you encounter issues during migration:

1. Check the application logs for specific error messages
2. Verify your API requests match the new format
3. Test with a single recipient first before trying multiple recipients
4. Review the Postman collection for updated examples

## Benefits of Migration

After migration, you'll have access to:

- **Multi-recipient emails**: Send to multiple people at once
- **Better performance**: Reduced API calls for bulk sending  
- **Enhanced tracking**: Individual recipient analytics
- **Improved reliability**: Better error handling per recipient
- **Future features**: Upcoming features will build on this foundation

The migration ensures your email service is more scalable and feature-rich while maintaining backward compatibility for single recipient emails.
