# Consent Core Backend

## Overview

The **Consent Core Backend** is a critical microservice within the Digital Personal Data Protection (DPDP) Consent Management System (CMS). This service is a government Digital Public Good designed to ensure compliance with the DPDP Act, 2023 of India. It serves as the core engine for managing consent lifecycle, templates, consent handles, and related metadata in a multi-tenant architecture.

This service provides RESTful APIs for creating, managing, validating, and querying consents, templates, and consent handles while maintaining strict data isolation across tenants and ensuring compliance with DPDP Act requirements.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development Setup](#development-setup)
- [Building and Testing](#building-and-testing)
- [Deployment](#deployment)
- [Multi-Tenancy](#multi-tenancy)
- [Security](#security)
- [Dependencies](#dependencies)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

## Features

### Business Features

- **Consent Lifecycle Management**: Create, update, validate, and manage consent records throughout their lifecycle (Active, Inactive, Expired, Withdrawn)
- **Consent Handle Management**: Create and manage consent handles that serve as references for consent requests
- **Template Management**: Create and version consent templates with multilingual support and UI configuration
- **Consent Metadata Management**: Store and retrieve additional metadata associated with consents
- **Document Management**: Upload, store, and retrieve consent-related documents (PDF format)
- **Parental KYC Integration**: Support for parental consent verification for users below 18 years through DigiLocker integration
- **Consent Token Validation**: Validate consent tokens with optional JWS signature verification
- **Payload Hash Verification**: Verify integrity of consent payloads using hash verification
- **Advanced Search and Filtering**: Search consents, templates, and handles with multiple filter criteria
- **Consent Status Analytics**: Get counts and status-wise grouping of consents

### Technical Features

- **Multi-Tenant Architecture**: Complete tenant isolation with tenant-specific MongoDB databases
- **RESTful API**: Comprehensive REST APIs with OpenAPI/Swagger documentation
- **JWT Authentication**: Support for JWT-based authentication and authorization
- **JWS Signature Support**: Optional request/response signing using JWS (JSON Web Signature)
- **Audit Trail Integration**: Automatic audit logging for all consent operations
- **Notification Integration**: Event-driven notifications for consent lifecycle events
- **Vault Integration**: Integration with vault service for encryption and digital signatures
- **Async Processing**: Asynchronous processing support for long-running operations
- **Batch Processing**: Batch creation of consent handles from templates
- **Comprehensive Logging**: Tenant-specific logging with INFO, DEBUG, and ERROR levels
- **Health Monitoring**: Spring Boot Actuator endpoints for health checks and metrics

## Architecture

### Service Type

This is a **REST API service** that provides HTTP endpoints for consent management operations. It does not use Kafka for messaging but integrates with other microservices via HTTP REST calls.

### Integration Points

The service integrates with the following microservices:

1. **Notification Service** (`notification-module-apis`)
    - Endpoint: `/notification/v1/events/trigger`
    - Purpose: Send notifications for consent lifecycle events (created, updated, withdrawn, expired, etc.)

2. **Audit Service** (`audit-module-apis`)
    - Endpoint: `/audit/v1/audit`
    - Purpose: Log audit trails for all consent operations

3. **Vault Service** (`vault-apis`)
    - Endpoints:
        - `/client/sign`: Digital signature generation
        - `/client/verify`: Signature verification
        - `/client/encryptPayload`: Payload encryption
    - Purpose: Cryptographic operations for consent security

4. **DigiLocker API** (External)
    - Base URL: `https://api.digitallocker.gov.in`
    - Purpose: Parental KYC verification for users below 18 years

### Database

- **Database Type**: MongoDB
- **Multi-Tenancy**: Each tenant has a dedicated database named `tenant_db_{tenantId}`
- **Main Collections**:
    - `consents`: Consent records
    - `consent_handles`: Consent handle references
    - `templates`: Consent templates
    - `consent_meta`: Consent metadata
    - `documents`: Document storage
    - `parental_kyc`: Parental KYC records
    - `tenant_registry`: Tenant configuration

### Data Flow

1. **Consent Creation Flow**:
    - Template created → Consent Handle created → Consent created from handle
    - Each step triggers notifications and audit logs

2. **Consent Validation Flow**:
    - Token validation → Signature verification (if enabled) → Consent retrieval

3. **Multi-Tenant Data Isolation**:
    - All requests require `tenant-id` header
    - Data is automatically routed to tenant-specific database
    - Logs are segregated by tenant

## Technology Stack

- **Java**: 21
- **Framework**: Spring Boot 3.5.4
- **Database**: MongoDB (with Spring Data MongoDB)
- **Build Tool**: Maven
- **Security**: Spring Security 6.5.7
- **API Documentation**: SpringDoc OpenAPI 2.8.11
- **JSON Processing**: Gson 2.13.0, Jackson
- **HTTP Client**: Apache HttpClient 5
- **JWT**: Nimbus JOSE JWT 10.0.2, Auth0 JWT 4.4.0
- **Logging**: Logback with tenant-specific appenders
- **Container**: Docker (Eclipse Temurin 21 JRE base image)

## Prerequisites

- **Java Development Kit (JDK):** 21 or higher
- **Apache Maven:** 3.8+ (for building)
- **MongoDB:** 5.0 or higher
- **Docker:** 20.10+ (for containerized deployment)
- **Kubernetes:** 1.21+ (for orchestration)

## Configuration

### Environment Variables

The service requires the following environment variables for configuration:

#### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `MONGODB_URI` | MongoDB connection string | `mongodb://localhost:27017` |
| `MONGODB_DATABASE` | Default MongoDB database name | `cms_db_admin` |
| `SERVER_PORT` | Server port (default: 9001) | `9001` |
| `PROFILE` | Spring profile (dev/sit/prod) | `dev` |

#### Optional Variables

| Variable | Description | Default | Security Sensitive |
|----------|-------------|---------|-------------------|
| `NOTIFICATION_MODULE_APIS_URL` | Notification service base URL | - | No |
| `AUDIT_MODULE_APIS_URL` | Audit service base URL | - | No |
| `VAULT_APIS_URL` | Vault service base URL | - | No |
| `TEMPLATE_HANDLE_BATCH_SIZE` | Batch size for template handle creation | `1000` | No |
| `JWS_SIGNATURE_ENABLED` | Enable JWS signature validation | `false` | No |

## API Documentation

### Base URL

- **Context Path**: `/consent`
- **API Version**: `/v1.0`
- **Swagger UI**: Available at `/consent/swagger-ui.html` when running

### Authentication

All API endpoints require the following headers:

- **`tenant-id`** (required): UUID identifying the tenant
- **`txn`** (required): Transaction ID (UUID) for request tracking
- **`business-id`** (required for some endpoints): Business identifier
- **`x-jws-signature`**: JWS signature for request validation (if JWS is enabled applicable only for create handle API and validate consent API)
- **`requestor-type`**: Type of requestor entity (e.g., `DATA_FIDUCIARY`, applicable only for create handle API and validate consent API)

### API Endpoints

#### Consent Management (`/v1.0/consent`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/create` | Create a consent from consent handle ID |
| `POST` | `/validate-token` | Validate a consent token |
| `PUT` | `/update/{consentId}` | Update consent by data principle |
| `GET` | `/search` | Search consents with filters |
| `GET` | `/count` | Get total count of consents |
| `GET` | `/count-by-params` | Get count of consents by parameters |
| `GET` | `/count-status-by-params` | Get count grouped by consent status |
| `GET` | `/verify-payload-hash/{consentId}` | Verify payload hash for a consent |

#### Consent Handle Management (`/v1.0/consent-handle`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/create` | Create a new consent handle |
| `GET` | `/get/{consentHandleId}` | Get consent handle by ID |
| `GET` | `/search` | Search consent handles with filters |
| `GET` | `/count` | Get total count of consent handles |

#### Template Management (`/v1.0/templates`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/create` | Create a new consent template |
| `PUT` | `/update/{templateId}` | Update an existing template |
| `GET` | `/search` | Search templates with filters |
| `GET` | `/count` | Get total count of templates |

#### Consent Metadata (`/v1.0/consent-meta`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/create` | Create consent metadata |
| `GET` | `/{consentMetaId}` | Get consent metadata by ID |

#### Document Management (`/v1.0/documents`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/view-document/{documentId}` | View document content |
| `GET` | `/get-document/{documentId}` | Get document metadata, base64 document content |

#### Parental KYC (`/api/v1/parental-consent`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/kyc` | Create parental KYC for users below 18 years |

### Health & Monitoring APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Application health check |
| `GET` | `/actuator/prometheus` | Prometheus metrics |
| `GET` | `/actuator/info` | Application information |

### Search Parameters
`It can we configured through properties file.`

#### Consent Search (default params)
- `consentId`, `status`, `businessId`, `templateId`, `consentHandleId`, `customerIdentifiers.type`, `customerIdentifiers.value`

#### Consent Handle Search (default params)
- `templateId`, `templateVersion`, `status`, `customerIdentifiers.type`, `customerIdentifiers.value`

#### Template Search (default params)
- `templateId`, `version`, `businessId`

### Example API Calls

#### Create Consent Handle

```bash
curl -X POST "http://localhost:9001/consent/v1.0/consent-handle/create" \
  -H "Content-Type: application/json" \
  -H "tenant-id: a1b2c3d4-e5f6-7890-1234-567890abcdef" \
  -H "txn: b2c3d4e5-f6a7-8901-2345-678901bcdefg" \
  -H "business-id: business-123" \
  -d '{
    "templateId": "template-uuid",
    "customerIdentifiers": {
      "type": "MOBILE",
      "value": "9818123456"
    }
  }'
```

#### Validate Consent Token

```bash
curl -X POST "http://localhost:9001/consent/v1.0/consent/validate-token" \
  -H "Content-Type: application/json" \
  -H "tenant-id: a1b2c3d4-e5f6-7890-1234-567890abcdef" \
  -H "txn: b2c3d4e5-f6a7-8901-2345-678901bcdefg" \
  -d '{
    "token": "consent-token-string"
  }'
```

## Development Setup

### Local Development

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd consent-core-backend
   ```

2. **Set up MongoDB**:
    - Ensure MongoDB is running and accessible
    - Create a database or use the default

3. **Configure environment variables**:
   ```bash
   export MONGODB_URI="mongodb://localhost:27017"
   export MONGODB_DATABASE="cms_db_admin"
   export SERVER_PORT=9001
   export PROFILE=dev
   export NOTIFICATION_MODULE_APIS_URL="http://localhost:9005"
   export AUDIT_MODULE_APIS_URL="http://localhost:9006"
   export VAULT_APIS_URL="http://localhost:9010"
   ```

4. **Build the project**:
   ```bash
   ./mvnw clean install
   ```

5. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

   Or using the JAR:
   ```bash
   java -jar target/consent-0.0.1-SNAPSHOT.jar
   ```

6. **Access Swagger UI**:
    - Open browser: `http://localhost:9001/consent/swagger-ui.html`

### Directory Structure

```
consent-core-backend/
├── src/
│   ├── main/
│   │   ├── java/com/jio/consent/
│   │   │   ├── client/          # External service clients
│   │   │   │   ├── audit/       # Audit service client
│   │   │   │   ├── notification/# Notification service client
│   │   │   │   └── vault/       # Vault service client
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── constant/        # Constants
│   │   │   ├── controller/     # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # MongoDB entities
│   │   │   ├── exception/       # Exception handlers
│   │   │   ├── interceptor/     # Request interceptors
│   │   │   ├── multitenancy/    # Multi-tenant support
│   │   │   ├── repository/      # Data repositories
│   │   │   ├── repositoryImpl/  # Custom repository implementations
│   │   │   ├── service/         # Business logic services
│   │   │   └── utils/           # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       └── java/                # Test classes
├── deployment/                  # Deployment configurations
│   ├── Dockerfile
│   └── *.yaml                   # Kubernetes manifests
├── pom.xml
└── README.md
```

## Building and Testing

### Build

```bash
# Clean and build
./mvnw clean install

# Skip tests
./mvnw clean install -DskipTests

# Build Docker image
docker build -f deployment/Dockerfile -t consent-core-backend:latest .
```

### Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=ConsentApplicationTests

# Generate test coverage report
./mvnw test jacoco:report
```

### Test Coverage

The project includes unit tests and integration tests. Test coverage reports can be generated using Maven plugins.

## Deployment

### Docker Deployment

1. **Build the Docker image**:
   ```bash
   docker build -f deployment/Dockerfile -t consent-core-backend:latest .
   ```

2. **Run the container**:
   ```bash
   docker run -d \
     -p 9001:9001 \
     -e MONGODB_URI="mongodb://mongodb:27017" \
     -e MONGODB_DATABASE="cms_db_admin" \
     -e SERVER_PORT=9001 \
     -e PROFILE=dev \
     consent-core-backend:latest
   ```

### Kubernetes Deployment

The service includes Kubernetes deployment manifests in the `deployment/` directory:

- `jio-dl-deployment.yaml`: Standard deployment configuration
- `jio-dl-deployment-sit.yaml`: SIT environment configuration
- `Public_deployment.yaml`: Public deployment configuration

**Deploy to Kubernetes**:
```bash
kubectl apply -f deployment/jio-dl-deployment.yaml
```

### Environment-Specific Configuration

- **Development**: Profile `dev`, port `9001`
- **SIT**: Profile `sit`, port `9001`
- **Production**: Profile `prod`, port `9006` (for public deployment)

## Multi-Tenancy

### Tenant Isolation

The service implements complete tenant isolation:

1. **Database Isolation**: Each tenant has a dedicated MongoDB database named `tenant_db_{tenantId}`
2. **Request Routing**: All requests must include `tenant-id` header
3. **Log Isolation**: Logs are segregated by tenant in separate directories
4. **Data Access**: Tenant-specific `MongoTemplate` instances ensure data isolation

### Tenant Configuration

Tenants are registered in the `tenant_registry` collection in the admin database. The `TenantMongoTemplateProvider` automatically creates tenant-specific database connections.

### Tenant Headers

All API requests require:
- **`tenant-id`**: UUID of the tenant (mandatory)
- **`txn`**: Transaction ID for request tracking (mandatory)

## Security

### Security Features

- **JWT Authentication**: Support for JWT token validation
- **JWS Signature**: Optional request/response signing using JSON Web Signature
- **Data Encryption**: Integration with vault service for payload encryption
- **Tenant Isolation**: Complete data isolation between tenants
- **Audit Logging**: All operations are audited
- **Input Validation**: Comprehensive request validation using Jakarta Validation
- **CORS Configuration**: Configurable CORS policies
- **Security Headers**: Standard security headers support

### JWS Signature

When `JWS_SIGNATURE_ENABLED=true`:

- Requests with `x-jws-signature` header are validated
- Responses are signed and include `x-jws-signature` header

### Security Best Practices

1. Always use HTTPS in production
2. Store sensitive configuration in Kubernetes secrets
3. Enable JWS signature validation for production
4. Regularly rotate JWT signing keys
5. Monitor audit logs for suspicious activities
6. Implement rate limiting at API gateway level

## Dependencies

### External Service Dependencies

1. **MongoDB**: Required for data persistence
2. **Notification Service**: Required for sending notifications
3. **Audit Service**: Required for audit logging
4. **Vault Service**: Required for encryption and signatures (if JWS enabled)
5. **DigiLocker API**: Required for parental KYC (external service)

### Service Dependencies

The service depends on other microservices in the DPDP CMS ecosystem:

- `notification-module-apis`: For event notifications
- `audit-module-apis`: For audit trail logging
- `vault-apis`: For cryptographic operations

**Note**: The service can start without these dependencies, but related features will fail. Ensure all dependent services are running for full functionality.

## Troubleshooting

### Common Issues

1. **MongoDB Connection Error**:
    - Verify `MONGODB_URI` is correct
    - Check MongoDB is accessible
    - Verify network connectivity

2. **Tenant Not Found**:
    - Ensure tenant is registered in `tenant_registry`
    - Verify `tenant-id` header is correct UUID format

3. **Service Integration Failures**:
    - Check dependent service URLs are correct
    - Verify services are running and accessible
    - Check network connectivity

4. **JWS Signature Validation Fails**:
    - Verify vault service is accessible
    - Check JWS signature format
    - Ensure `JWS_SIGNATURE_ENABLED` is set correctly

5. **Log Files Not Created**:
    - Verify `log_path` directory exists and is writable
    - Check file permissions
    - Verify tenant-id is present in request headers

### Logging

Logs are written to:
- Console: Standard output
- Files: `{log_path}/{tenantId}/consentcore_{Level}.log`
    - `consentcore_Info.log`: INFO level logs
    - `consentcore_Debug.log`: DEBUG level logs (dev/sit only)
    - `consentcore_Error.log`: ERROR level logs

Log rotation:
- Max file size: 10MB
- Max history: 30 days
- Pattern: `{level}-{date}.{index}.log`

## Contributing

This is a government Digital Public Good. Contributions should follow the project's contribution guidelines. Please refer to the central repository's `CONTRIBUTING.md` for details.

### Development Guidelines

1. Follow Java coding standards
2. Write unit tests for new features
3. Update API documentation
4. Ensure multi-tenancy is maintained
5. Add audit logging for new operations
6. Update this README for significant changes

## License

This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0) with additional terms for DPDP CMS. See [LICENSE](LICENSE) file for details.

## Contact and Support

For issues, questions, or contributions:

- **Repository**: [Git Repository URL]
- **Documentation**: [Documentation Site URL]
- **Security Issues**: [Security Contact Email]
- **Support**: Jio.ConsentSupport@ril.com

## Related Documentation

- [DPDP Act, 2023](https://www.meity.gov.in/data-protection-framework)
- [Central DPDP CMS Documentation]([Documentation Site URL])
- [API Reference]([API Documentation URL])

---

**Note**: This service is part of the DPDP Consent Management System, a critical infrastructure for ensuring compliance with the Digital Personal Data Protection Act, 2023 of India. All operations must maintain strict compliance with DPDP Act requirements.
