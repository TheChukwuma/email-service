# Email Service

A robust, scalable email service built with Spring Boot 3, featuring template-based email sending, queue processing, attachment handling, and comprehensive tracking capabilities.

## Features

- **Email Sending**: Send templated or raw emails with support for HTML and text formats
- **Template Management**: Create, update, and manage email templates with variable substitution
- **Queue Processing**: Asynchronous email processing using RabbitMQ with retry logic
- **Attachment Support**: Secure file upload with MinIO and Cloudinary integration
- **Email Tracking**: Track email opens and clicks with pixel tracking and link rewriting
- **Authentication**: API key-based authentication for clients, JWT for admin operations
- **Monitoring**: Prometheus metrics and Grafana dashboards
- **Database**: PostgreSQL with Flyway migrations
- **Security**: File upload validation, rate limiting, and comprehensive security measures

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST API      │    │   RabbitMQ      │    │   Email Worker  │
│   (Spring Boot) │───▶│   (Queue)       │───▶│   (Processing)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostgreSQL    │    │   MinIO         │    │   SMTP Server   │
│   (Database)    │    │   (Attachments) │    │   (GreenMail)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Quick Start

### Prerequisites

- Java 21+
- Docker and Docker Compose
- Maven 3.8+

### 1. Start Infrastructure Services

```bash
docker-compose up -d postgres rabbitmq minio greenmail redis prometheus grafana
```

### 2. Configure Environment Variables

Create a `.env` file in the project root:

```env
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/email_service
SPRING_DATASOURCE_USERNAME=email_user
SPRING_DATASOURCE_PASSWORD=email_password

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=email-attachments

# Cloudinary (optional)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# JWT
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=86400000

# Email
SPRING_MAIL_HOST=localhost
SPRING_MAIL_PORT=1025
```

### 3. Build and Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

The application will be available at `http://localhost:8080`

## API Endpoints

### Authentication

- `POST /v1/auth/register` - Register a new user
- `POST /v1/auth/login` - Login and get JWT token

### Email Operations

- `POST /v1/emails/send` - Send an email (requires API key)
- `GET /v1/emails/{id}` - Get email status by ID
- `GET /v1/emails/uuid/{uuid}` - Get email status by UUID
- `GET /v1/emails/status/{status}` - Get emails by status
- `GET /v1/emails/recipient/{email}` - Get emails by recipient

### Template Management

- `POST /v1/templates` - Create a template
- `GET /v1/templates` - Get all templates
- `GET /v1/templates/{name}` - Get template by name
- `PUT /v1/templates/{name}` - Update template
- `DELETE /v1/templates/{name}` - Delete template

### Admin Operations (requires admin role)

- `POST /v1/admin/users` - Create user
- `GET /v1/admin/users` - Get all users
- `POST /v1/admin/users/{userId}/api-keys` - Create API key for user
- `GET /v1/admin/stats` - Get system statistics

### Tracking

- `GET /track/open/{emailUuid}` - Email open tracking pixel
- `GET /track/click/{emailUuid}?to={url}` - Email click tracking

## Usage Examples

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

### 2. Login and Get JWT Token

```bash
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 3. Create an API Key (Admin only)

```bash
curl -X POST http://localhost:8080/v1/admin/users/1/api-keys \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "keyName": "My API Key",
    "expiresAt": "2024-12-31T23:59:59"
  }'
```

### 4. Create a Template

```bash
curl -X POST http://localhost:8080/v1/templates \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "name": "welcome",
    "subjectTemplate": "Welcome {{name}}!",
    "bodyTemplate": "<h1>Welcome {{name}}!</h1><p>Thank you for joining us.</p>",
    "bodyType": "HTML"
  }'
```

### 5. Send an Email

```bash
curl -X POST http://localhost:8080/v1/emails/send \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "from": "noreply@example.com",
    "to": "user@example.com",
    "templateName": "welcome",
    "templateVars": {
      "name": "John Doe"
    }
  }'
```

## Monitoring

### Prometheus Metrics

Access Prometheus at `http://localhost:9090`

Key metrics:
- `emails_sent_total` - Total emails sent
- `emails_failed_total` - Total failed emails
- `emails_opened_total` - Total email opens tracked
- `queue_depth` - Current queue depth
- `worker_throughput` - Email processing rate

### Grafana Dashboards

Access Grafana at `http://localhost:3000` (admin/admin)

Pre-configured dashboards for:
- Email service metrics
- Queue monitoring
- System health
- Performance metrics

## Development

### Running Tests

```bash
mvn test
```

### Database Migrations

Flyway migrations are automatically applied on startup. Migration files are located in `src/main/resources/db/migration/`.

### Adding New Features

1. Create entity classes in `com.octopus.email_service.entity`
2. Create repository interfaces in `com.octopus.email_service.repository`
3. Create service classes in `com.octopus.email_service.service`
4. Create controller classes in `com.octopus.email_service.controller`
5. Add database migrations if needed

## Security Considerations

- All file uploads are validated for type and size
- API keys are hashed before storage
- JWT tokens have configurable expiration
- Rate limiting is implemented for API endpoints
- CORS is configured for cross-origin requests
- Input validation is enforced on all endpoints

## Production Deployment

For production deployment:

1. Use a proper SMTP provider (SendGrid, Mailgun, etc.)
2. Configure proper DNS records (SPF, DKIM, DMARC)
3. Use environment-specific configuration
4. Set up proper monitoring and alerting
5. Configure backup strategies for database and attachments
6. Use HTTPS for all communications
7. Implement proper secret management

## Troubleshooting

### Common Issues

1. **Database Connection Failed**: Ensure PostgreSQL is running and accessible
2. **Queue Processing Stopped**: Check RabbitMQ status and logs
3. **File Upload Fails**: Verify MinIO is running and accessible
4. **Email Not Sending**: Check SMTP configuration and GreenMail status

### Logs

Application logs are available in the console. For production, configure proper logging to files or centralized logging system.

## License

This project is licensed under the MIT License.
