# 🏢 Tenant-Based Email System Testing Guide

## Overview
This guide demonstrates how to test the multi-tenant email system with custom sender addresses and reply-to functionality.

## 🚀 Quick Start

### 1. Start the Application
```bash
./mvnw spring-boot:run
```

### 2. Create Admin User (First Time Setup)
```bash
curl -X POST http://localhost:8080/v1/admin/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@example.com",
    "password": "admin123",
    "isAdmin": true
  }'
```

### 3. Login and Get JWT Token
```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "admin123"
  }'
```

---

## 🏦 Scenario 1: UnionBank Setup

### Step 1: Create UnionBank Tenant
```bash
curl -X POST http://localhost:8080/v1/admin/tenants \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantCode": "unionbank",
    "tenantName": "Union Bank of Nigeria",
    "defaultSenderEmail": "no-reply@unionbank.com",
    "defaultSenderName": "Union Bank",
    "defaultReplyToEmail": "support@unionbank.com",
    "defaultReplyToName": "Union Bank Support"
  }'
```

### Step 2: Add Domain to UnionBank
```bash
curl -X POST http://localhost:8080/v1/admin/tenants/1/domains \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "domain": "unionbank.com"
  }'
```

### Step 3: Verify Domain
```bash
curl -X POST http://localhost:8080/v1/admin/tenants/1/domains/1/verify \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Step 4: Create API Key for UnionBank
```bash
curl -X POST http://localhost:8080/v1/admin/api-keys \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "keyName": "UnionBank Mobile App",
    "clientId": "unionbank-mobile",
    "clientApplication": "UnionBank Mobile Banking",
    "tenantId": 1
  }'
```

**Save the returned API key for testing!**

---

## 📧 Email Testing Scenarios

### Scenario A: UnionBank with Verified Domain ✅

**Request:**
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "X-Api-Key: YOUR_UNIONBANK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "alerts@unionbank.com",
    "to": "customer@gmail.com",
    "subject": "Transaction Alert - Union Bank",
    "body": "<html><body><h2>Transaction Successful</h2><p>Your transfer of ₦50,000 was successful.</p><p>Best regards,<br>Union Bank Team</p></body></html>"
  }'
```

**Expected Result:**
- ✅ **From**: `alerts@unionbank.com` (verified domain, used as requested)
- ✅ **Reply-To**: `Union Bank Support <support@unionbank.com>` (tenant default)

---

### Scenario B: UnionBank with Unverified Domain ⚠️

**Request:**
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "X-Api-Key: YOUR_UNIONBANK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "test@random-domain.com",
    "to": "customer@gmail.com",
    "subject": "Transaction Alert - Union Bank",
    "body": "Your transaction was successful. Thank you for banking with Union Bank."
  }'
```

**Expected Result:**
- ✅ **From**: `Union Bank <no-reply@unionbank.com>` (fallback to tenant default)
- ✅ **Reply-To**: `Union Bank Support <support@unionbank.com>`
- ✅ **Body**: Wrapped in classic HTML template (plain text fallback)

---

### Scenario C: Individual User (No Tenant) 👤

**Request:**
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "Content-Type: application/json" \
  -d '{
    "from": "john@personal.com",
    "to": "friend@gmail.com",
    "subject": "Personal Email",
    "body": "Hey! How are you doing?"
  }'
```

**Expected Result:**
- ✅ **From**: `john@personal.com` (no tenant restrictions)
- ✅ **Reply-To**: Not set (individual user)
- ✅ **Body**: Wrapped in classic HTML template

---

## 🏦 Multi-Tenant Comparison

### Create GTBank Tenant
```bash
curl -X POST http://localhost:8080/v1/admin/tenants \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantCode": "gtbank",
    "tenantName": "Guaranty Trust Bank",
    "defaultSenderEmail": "noreply@gtbank.com",
    "defaultSenderName": "GT Bank",
    "defaultReplyToEmail": "customercare@gtbank.com",
    "defaultReplyToName": "GT Bank Customer Care"
  }'
```

### GTBank API Key
```bash
curl -X POST http://localhost:8080/v1/admin/api-keys \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "keyName": "GTBank Mobile App",
    "clientId": "gtbank-mobile", 
    "clientApplication": "GTBank Mobile App",
    "tenantId": 2
  }'
```

### GTBank Email Test
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "X-Api-Key: YOUR_GTBANK_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "alerts@gtbank.com",
    "to": "customer@gmail.com",
    "subject": "Account Statement - GT Bank",
    "body": "Your monthly statement is ready for download."
  }'
```

**Expected Result:**
- ✅ **From**: `GT Bank <noreply@gtbank.com>` (tenant default, domain not verified)
- ✅ **Reply-To**: `GT Bank Customer Care <customercare@gtbank.com>`

---

## 🔍 Verification Steps

### 1. Check Database Records
```sql
-- View tenants
SELECT * FROM email_tenants;

-- View domains  
SELECT * FROM email_domains;

-- View API keys with tenants
SELECT ak.*, et.tenant_code 
FROM api_keys ak 
LEFT JOIN email_tenants et ON ak.tenant_id = et.id;

-- View sent emails
SELECT id, from_address, reply_to_address, to_address, subject, status 
FROM emails 
ORDER BY created_at DESC 
LIMIT 10;
```

### 2. Check Application Logs
Look for these log entries:
```
✅ "Validated API key for client: unionbank-mobile with tenant: unionbank"
✅ "Using requested sender address alerts@unionbank.com for tenant unionbank"  
✅ "Using tenant default reply-to for unionbank: Union Bank Support <support@unionbank.com>"
✅ "Email enqueued with ID: X for recipient: customer@gmail.com from tenant: unionbank"
```

### 3. Verify Email Headers
In the received email, check headers:
```
From: alerts@unionbank.com
Reply-To: Union Bank Support <support@unionbank.com>
To: customer@gmail.com
Subject: Transaction Alert - Union Bank
```

---

## 🏗️ Domain-Specific Reply-To (Advanced)

### Add Domain-Specific Reply-To
Currently, this would require direct database update:

```sql
UPDATE email_domains 
SET domain_reply_to_email = 'alerts-support@unionbank.com',
    domain_reply_to_name = 'Union Bank Alerts Team'
WHERE domain = 'unionbank.com' AND tenant_id = 1;
```

Then test the same UnionBank email - it should now use the domain-specific reply-to.

---

## 🚨 Error Testing

### Invalid API Key
```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "X-Api-Key: invalid-key" \
  -H "Content-Type: application/json" \
  -d '{
    "from": "test@test.com",
    "to": "user@gmail.com", 
    "subject": "Test",
    "body": "Test"
  }'
```

**Expected**: `401 Unauthorized - Invalid API key`

### Tenant Without Default Sender
This shouldn't happen with proper validation, but would fall back gracefully.

---

## 📊 Performance Testing

### Bulk Email Test
```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/v1/emails/send \
    -H "X-Api-Key: YOUR_UNIONBANK_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{
      \"from\": \"alerts@unionbank.com\",
      \"to\": \"customer$i@gmail.com\",
      \"subject\": \"Bulk Test Email $i\",
      \"body\": \"This is bulk test email number $i\"
    }" &
done
wait
```

---

## 🎯 Success Criteria

✅ **Tenant Isolation**: Each tenant's emails use their configured sender addresses  
✅ **Domain Verification**: Only verified domains allow custom sender addresses  
✅ **Fallback Logic**: Unverified domains fall back to tenant defaults  
✅ **Reply-To Hierarchy**: Domain-specific > Tenant default > None  
✅ **Individual Users**: Work without tenant restrictions  
✅ **API Security**: Tenant-specific API keys function correctly  
✅ **Template Handling**: Plain text gets classic template wrapper  
✅ **Error Handling**: Invalid configurations fail gracefully  

---

## 📞 Support

For issues or questions about the tenant email system:
1. Check application logs for detailed error messages
2. Verify database constraints and foreign keys
3. Ensure API keys are properly associated with tenants
4. Test with individual user emails to isolate tenant-specific issues

The system is designed to be highly configurable while maintaining security and proper fallbacks for all scenarios.

