# Email Service Integration Test Guide

This guide provides comprehensive instructions for running integration tests using the Postman collection for the Email Service API.

## Prerequisites

### 1. Service Setup
- Email Service running on `http://localhost:8080`
- PostgreSQL database running and accessible
- RabbitMQ running and accessible
- MinIO running and accessible
- Cloudinary configured (optional)

### 2. Postman Setup
- Import the collection: `Email_Service_Postman_Collection.json`
- Import the environment: `Postman_Test_Environment.json`
- Select the "Email Service Test Environment" in Postman

## Test Scenarios

### 1. Authentication Flow
**Purpose**: Test user registration, login, and API key generation

**Test Sequence**:
1. **Register User** - Creates a new test user
2. **Login User** - Authenticates and gets JWT token
3. **Generate API Key** - Creates API key for API authentication

**Expected Results**:
- User registration returns 201 with user ID
- Login returns 200 with JWT token
- API key generation returns 201 with API key

### 2. Basic Email Sending
**Purpose**: Test core email functionality

**Test Sequence**:
1. **Send Simple Email** - Basic email without attachments
2. **Get Email Status** - Verify email was processed

**Expected Results**:
- Email sent returns 200 with email ID
- Status check returns current email status

### 3. Email with Attachments
**Purpose**: Test attachment processing functionality

**Test Scenarios**:
- **Regular Attachment**: PDF document attachment
- **Inline Image**: Image embedded in HTML email
- **Multiple Attachments**: Both regular and inline attachments

**Expected Results**:
- All attachment types processed successfully
- Inline images display correctly in HTML emails
- Multiple attachments handled properly

### 4. Template System
**Purpose**: Test email templating functionality

**Test Sequence**:
1. **Create Template** - Create a new email template
2. **Send Email with Template** - Use template with variables

**Expected Results**:
- Template created successfully
- Templated email sent with variable substitution

### 5. Attachment Management
**Purpose**: Test standalone attachment operations

**Test Sequence**:
1. **Upload Attachment** - Upload file via base64
2. **Get My Attachments** - List user's attachments

**Expected Results**:
- Attachment uploaded successfully
- Attachment list returned correctly

### 6. Error Handling
**Purpose**: Test error scenarios and validation

**Test Scenarios**:
- **Invalid Base64**: Malformed base64 content
- **File Too Large**: Exceeding size limits
- **Unauthorized Access**: Missing authentication

**Expected Results**:
- Appropriate error codes (400, 401, 403)
- Descriptive error messages
- Proper validation responses

### 7. Admin Functions
**Purpose**: Test administrative operations

**Test Sequence**:
1. **Get System Statistics** - View system metrics
2. **Cleanup Expired Attachments** - Trigger cleanup process

**Expected Results**:
- Statistics returned successfully
- Cleanup process initiated

## Running the Tests

### Manual Testing
1. Open Postman
2. Import the collection and environment
3. Run requests individually or in sequence
4. Verify responses match expected results

### Automated Testing
1. Use Postman's Collection Runner
2. Select the entire collection or specific folders
3. Configure iterations and delays
4. Run and review test results

### Newman CLI Testing
```bash
# Install Newman
npm install -g newman

# Run the collection
newman run Email_Service_Postman_Collection.json \
  -e Postman_Test_Environment.json \
  --reporters cli,json \
  --reporter-json-export results.json
```

## Test Data

### Sample Base64 Content
The collection includes sample base64 content for:
- **PDF Document**: Simple "Hello World" PDF
- **PNG Image**: 1x1 transparent pixel
- **Invalid Base64**: Malformed content for error testing

### Test Users
- **Username**: testuser
- **Email**: test@example.com
- **Password**: password123

## Validation Points

### Response Validation
Each test includes assertions for:
- **Status Codes**: Correct HTTP response codes
- **Response Structure**: Proper JSON structure
- **Data Presence**: Required fields present
- **Data Types**: Correct data types

### Business Logic Validation
- **Email Processing**: Emails queued and processed
- **Attachment Handling**: Files uploaded and stored
- **Template Rendering**: Variables substituted correctly
- **Security**: Authentication and authorization working

## Troubleshooting

### Common Issues

#### 1. Authentication Failures
- **Problem**: 401 Unauthorized responses
- **Solution**: Ensure API key is set in environment variables
- **Check**: Verify user registration and login completed successfully

#### 2. Database Connection Issues
- **Problem**: 500 Internal Server Error
- **Solution**: Verify PostgreSQL is running and accessible
- **Check**: Database connection configuration

#### 3. File Upload Failures
- **Problem**: Attachment upload errors
- **Solution**: Check MinIO/Cloudinary configuration
- **Check**: Verify storage services are running

#### 4. Email Processing Issues
- **Problem**: Emails not being sent
- **Solution**: Verify RabbitMQ is running
- **Check**: Email worker processing logs

### Debug Information
- Check application logs for detailed error information
- Verify environment variables are set correctly
- Ensure all required services are running
- Check network connectivity between services

## Performance Testing

### Load Testing Scenarios
1. **Concurrent Email Sending**: Multiple emails sent simultaneously
2. **Large Attachment Handling**: Test with various file sizes
3. **Template Processing**: Multiple templated emails
4. **Database Performance**: High volume of email records

### Monitoring Points
- **Response Times**: API response latency
- **Memory Usage**: Application memory consumption
- **Database Performance**: Query execution times
- **Queue Processing**: Email processing throughput

## Security Testing

### Authentication Testing
- **Valid Credentials**: Proper authentication flow
- **Invalid Credentials**: Rejection of bad credentials
- **Token Expiration**: JWT token expiry handling
- **API Key Rotation**: Key management functionality

### Authorization Testing
- **User Isolation**: Users can only access their own data
- **Admin Functions**: Proper admin role enforcement
- **Resource Access**: Appropriate access controls

### Input Validation Testing
- **File Type Validation**: Only allowed file types accepted
- **Size Limits**: File size restrictions enforced
- **Content Validation**: Malicious content detection
- **SQL Injection**: Database injection prevention

## Continuous Integration

### CI/CD Integration
```yaml
# Example GitHub Actions workflow
name: Email Service Integration Tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Setup Java
        uses: actions/setup-java@v2
        with:
          java-version: '21'
      - name: Start Services
        run: docker-compose up -d
      - name: Run Tests
        run: newman run Email_Service_Postman_Collection.json
```

### Test Reporting
- **Pass/Fail Status**: Clear test results
- **Performance Metrics**: Response time tracking
- **Coverage Analysis**: API endpoint coverage
- **Trend Analysis**: Historical test results

## Maintenance

### Regular Updates
- **Test Data**: Update sample data regularly
- **Environment Variables**: Keep configuration current
- **Dependencies**: Update Postman and Newman versions
- **Test Scenarios**: Add new test cases for new features

### Documentation Updates
- **API Changes**: Update tests for API modifications
- **New Features**: Add tests for new functionality
- **Error Scenarios**: Update error handling tests
- **Performance Baselines**: Adjust performance expectations


