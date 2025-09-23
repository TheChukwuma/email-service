# Enhanced Email Service Examples

This document demonstrates the enhanced email service functionality that automatically handles HTML/plain text bodies and processes attachments.

## Features Implemented

1. **Multiple Recipients**: Send emails to multiple TO addresses in a single request
2. **Automatic HTML Detection**: The service automatically detects if the email body contains HTML content
3. **Fallback Template for Plain Text**: For plain text emails, the service automatically applies the classic email template
4. **Attachment Processing**: Attachments are automatically processed and stored in Cloudinary or MinIO storage
5. **Content ID Support**: Inline attachments with content IDs are properly embedded in HTML emails

## Example 0: Send Email to Multiple Recipients

```json
{
  "from": "newsletter@example.com",
  "to": ["user1@example.com", "user2@example.com", "user3@example.com"],
  "cc": ["manager@example.com"],
  "bcc": ["audit@example.com"],
  "subject": "Monthly Newsletter - Multiple Recipients",
  "body": "<html><body><h1>Monthly Newsletter</h1><p>This email is being sent to multiple recipients using the new multiple TO addresses feature.</p><p>Primary Recipients: user1@example.com, user2@example.com, user3@example.com</p><p>CC: manager@example.com</p><p>BCC: audit@example.com</p></body></html>"
}
```

**What happens:**
- Service sends the email to all 3 primary recipients in the "to" array
- Manager receives a copy via CC
- Audit department receives a blind copy via BCC
- All recipients receive the same content
- Email tracking and delivery status are tracked per recipient

## Example 1: HTML Email with Attachments (Your Sample Request)

```json
{
  "from": "sender@example.com",
  "to": ["egwuonwuchukwuma74@gmail.com"],
  "subject": "Report with charts and data",
  "body": "<html><body><h1>Monthly Report</h1><p>Please find the attached report and see the chart below:</p><img src=\"cid:chart1\" alt=\"Sales Chart\"><p>Best regards,<br>Team</p></body></html>",
  "attachments": [
    {
      "base64Content": "JVBERi0xLjQKJcfsj6IKNSAwIG9iago8PAovVHlwZSAvUGFnZQovUGFyZW50IDMgMCBSCi9SZXNvdXJjZXMgPDwKL0ZvbnQgPDwKL0YxIDYgMCBSCj4+Cj4+Ci9NZWRpYUJveCBbMCAwIDU5NSA4NDJdCi9Db250ZW50cyA3IDAgUgo+PgplbmRvYmoKNiAwIG9iago8PAovVHlwZSAvRm9udAovU3VidHlwZSAvVHlwZTEKL0Jhc2VGb250IC9IZWx2ZXRpY2EKPj4KZW5kb2JqCjcgMCBvYmoKPDwKL0xlbmd0aCA0NAo+PgpzdHJlYW0KQlQKL0YxIDEyIFRmCjcyIDcyMCBUZAooSGVsbG8gV29ybGQpIFRqCkVUCmVuZHN0cmVhbQplbmRvYmoKMyAwIG9iago8PAovVHlwZSAvUGFnZXMKL0NvdW50IDEKL0tpZHMgWzUgMCBSXQo+PgplbmRvYmoKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMyAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL0V4dEdTdGF0ZQo+PgplbmRvYmoKeHJlZgowIDgKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAwMDAwMDExNSAwMDAwMCBuIAowMDAwMDAwMjQwIDAwMDAwIG4gCjAwMDAwMDAzNjUgMDAwMDAgbiAKMDAwMDAwMDQ5MCAwMDAwMCBuIAowMDAwMDAwNjQ1IDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgOAovUm9vdCAxIDAgUgo+PgpzdGFydHhyZWYKNzM5CiUlRU9G",
      "filename": "monthly-report.pdf",
      "mimeType": "application/pdf",
      "description": "Monthly sales report"
    },
    {
      "base64Content": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
      "filename": "sales-chart.png",
      "mimeType": "image/png",
      "inline": true,
      "cid": "chart1",
      "description": "Sales performance chart"
    }
  ]
}
```

**What happens:**
- Service detects HTML content (isHtmlBody = true)
- Attachments are processed and stored in Cloudinary/MinIO
- Inline image with cid "chart1" is embedded in the HTML
- Email is sent as HTML with proper attachment handling

## Example 2: Plain Text Email (Automatically Uses Classic Template)

```json
{
  "from": "support@example.com",
  "to": ["customer@example.com"],
  "subject": "Welcome to our service",
  "body": "Thank you for signing up! We're excited to have you on board. Please visit our website for more information and to complete your profile setup."
}
```

**What happens:**
- Service detects plain text content (isHtmlBody = false)
- Sets needsFallbackTemplate = true
- Automatically applies classic email template
- Plain text content is wrapped in the classic HTML template for better presentation

## Example 3: Plain Text with Template Variables

```json
{
  "from": "notifications@example.com",
  "to": ["john.doe@example.com"],
  "subject": "Account Update",
  "body": "Your account has been updated successfully. If you have any questions, please contact our support team.",
  "templateVars": {
    "userName": "John Doe",
    "companyName": "Acme Corp",
    "supportEmail": "support@example.com"
  }
}
```

**What happens:**
- Service detects plain text content
- Applies classic template with additional template variables
- Variables are available in the template context for dynamic content

## Example 4: Mixed Content with Various Attachments

```json
{
  "from": "team@example.com",
  "to": ["client@example.com"],
  "subject": "Project Documents",
  "body": "<div><h2>Project Update</h2><p>Please find the project documents attached. The main presentation is embedded below:</p><img src=\"cid:presentation\" alt=\"Presentation\"/><p>Best regards,<br/>Project Team</p></div>",
  "attachments": [
    {
      "base64Content": "...",
      "filename": "project-specification.pdf",
      "mimeType": "application/pdf",
      "description": "Technical specifications"
    },
    {
      "base64Content": "...",
      "filename": "presentation.png",
      "mimeType": "image/png",
      "inline": true,
      "cid": "presentation",
      "description": "Project presentation slide"
    },
    {
      "base64Content": "...",
      "filename": "budget.xlsx",
      "mimeType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "description": "Project budget spreadsheet"
    }
  ]
}
```

## Database Changes

The email table now includes:
- `is_html_body`: Boolean flag indicating if the email contains HTML content
- `needs_fallback_template`: Boolean flag indicating if the classic template should be applied

## Storage Integration

Attachments are automatically:
1. Validated for content type and size
2. Stored in either Cloudinary or MinIO based on configuration
3. Assigned unique storage paths and checksums
4. Associated with the email record for tracking

## Template Processing Logic

The enhanced EmailWorker now:
1. Checks if the email has a custom template (existing behavior)
2. If no template but HTML content detected: sends as HTML
3. If no template and plain text: applies classic fallback template
4. Processes inline attachments with content IDs for HTML emails
5. Handles regular attachments for all email types

## API Behavior

- **Backward Compatible**: Existing API calls work exactly as before
- **Automatic Enhancement**: New logic automatically improves plain text emails
- **Flexible**: Supports both simple plain text and complex HTML with attachments
- **Efficient**: Attachment processing and storage happens asynchronously

