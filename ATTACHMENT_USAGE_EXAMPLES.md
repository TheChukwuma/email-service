# Email Attachment Usage Examples

This document provides examples of how to use the new EmailAttachmentDto for sending emails with attachments.

## Basic Email with Regular Attachment

```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "Email with PDF attachment",
  "body": "Please find the attached document.",
  "attachments": [
    {
      "base64Content": "JVBERi0xLjQKJcfsj6IKNSAwIG9iago8PAovVHlwZSAvUGFnZQovUGFyZW50IDMgMCBSCi9SZXNvdXJjZXMgPDwKL0ZvbnQgPDwKL0YxIDYgMCBSCj4+Cj4+Ci9NZWRpYUJveCBbMCAwIDU5NSA4NDJdCi9Db250ZW50cyA3IDAgUgo+PgplbmRvYmoKNiAwIG9iago8PAovVHlwZSAvRm9udAovU3VidHlwZSAvVHlwZTEKL0Jhc2VGb250IC9IZWx2ZXRpY2EKPj4KZW5kb2JqCjcgMCBvYmoKPDwKL0xlbmd0aCA0NAo+PgpzdHJlYW0KQlQKL0YxIDEyIFRmCjcyIDcyMCBUZAooSGVsbG8gV29ybGQpIFRqCkVUCmVuZHN0cmVhbQplbmRvYmoKMyAwIG9iago8PAovVHlwZSAvUGFnZXMKL0NvdW50IDEKL0tpZHMgWzUgMCBSXQo+PgplbmRvYmoKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMyAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL0V4dEdTdGF0ZQo+PgplbmRvYmoKeHJlZgowIDgKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDA5IDAwMDAwIG4gCjAwMDAwMDAwNTggMDAwMDAgbiAKMDAwMDAwMDExNSAwMDAwMCBuIAowMDAwMDAwMjQwIDAwMDAwIG4gCjAwMDAwMDAzNjUgMDAwMDAgbiAKMDAwMDAwMDQ5MCAwMDAwMCBuIAowMDAwMDAwNjQ1IDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgOAovUm9vdCAxIDAgUgo+PgpzdGFydHhyZWYKNzM5CiUlRU9G",
      "filename": "document.pdf",
      "mimeType": "application/pdf"
    }
  ]
}
```

## Email with Inline Image Attachment

```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "Email with inline image",
  "body": "<html><body><h1>Welcome!</h1><p>Here's our logo:</p><img src=\"cid:company-logo\" alt=\"Company Logo\"></body></html>",
  "attachments": [
    {
      "base64Content": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
      "filename": "logo.png",
      "mimeType": "image/png",
      "inline": true,
      "cid": "company-logo"
    }
  ]
}
```

## Email with Multiple Attachments (Regular + Inline)

```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
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

## Email with Optimized Image Attachment

```json
{
  "from": "sender@example.com",
  "to": "recipient@example.com",
  "subject": "Product photos",
  "body": "Please find the product photos attached.",
  "attachments": [
    {
      "base64Content": "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==",
      "filename": "product1.jpg",
      "mimeType": "image/jpeg",
      "optimizeImage": true,
      "generateThumbnail": true,
      "storageType": "CLOUDINARY"
    }
  ]
}
```

## EmailAttachmentDto Field Reference

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `base64Content` | String | Yes | Base64 encoded file content |
| `filename` | String | No | Original filename (defaults to "attachment") |
| `mimeType` | String | No | MIME type of the file |
| `inline` | Boolean | No | Whether this is an inline attachment (defaults to false) |
| `cid` | String | No | Content ID for inline attachments (required if inline=true) |
| `description` | String | No | Description of the attachment |
| `optimizeImage` | Boolean | No | Whether to optimize images (defaults to false) |
| `generateThumbnail` | Boolean | No | Whether to generate thumbnails (defaults to false) |
| `storageType` | String | No | Storage type: "MINIO", "CLOUDINARY", or "AUTO" (defaults to "AUTO") |

## Validation Rules

1. **Base64 Content**: Must be valid base64 encoded data
2. **File Size**: Maximum 10MB per attachment
3. **MIME Types**: Only allowed file types are accepted:
   - Images: `image/jpeg`, `image/png`, `image/gif`, `image/webp`
   - Documents: `application/pdf`, `text/plain`, `text/csv`
   - Office: `application/msword`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `application/vnd.ms-excel`, `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
4. **Content ID**: For inline attachments, must contain only alphanumeric characters, dots, underscores, and hyphens
5. **Inline Attachments**: If `inline=true`, `cid` is required

## Storage Behavior

- **AUTO**: System automatically chooses storage based on file type and size
  - Images < 5MB → Cloudinary
  - Other files → MinIO
- **CLOUDINARY**: Force storage in Cloudinary (good for images with optimization)
- **MINIO**: Force storage in MinIO (good for documents and large files)

## Processing Flow

1. **Validation**: File content, size, and type are validated
2. **Upload**: Base64 content is decoded and uploaded to chosen storage
3. **Database**: Attachment metadata is stored in database
4. **Email Processing**: During email sending, attachments are retrieved and added to email
5. **Cleanup**: Attachments expire after 24 hours by default

## Error Handling

- Invalid base64 content → 400 Bad Request
- File too large → 400 Bad Request  
- Unsupported file type → 400 Bad Request
- Invalid content ID format → 400 Bad Request
- Storage upload failure → 500 Internal Server Error


