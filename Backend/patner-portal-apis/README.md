# Partner Portal Backend Service

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen.svg)](https://spring.io/projects/spring-boot)

[![License](https://img.shields.io/badge/License-LGPL%203.0-blue.svg)](LICENSE)

[![DPDP Act 2023](https://img.shields.io/badge/DPDP%20Act-2023%20Compliant-success.svg)](https://www.meity.gov.in/writereaddata/files/Digital%20Personal%20Data%20Protection%20Act%202023.pdf)

> REST API gateway for partner organization management, legal entity onboarding, data processor management, and DPDP compliance configuration in a multi-tenant system

The Partner Portal Backend Service is a critical microservice within the Digital Personal Data Protection (DPDP) Consent Management System, designed to ensure compliance with the DPDP Act, 2023 of India. This service provides a comprehensive REST API for managing partner organizations, legal entities, data processors, consent configurations, grievance handling, and other essential components required for DPDP Act compliance.

**Repository:** https://dev.azure.com/JPL-Limited/partner-portal-backend

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Dependencies](#dependencies)
- [Support](#support)
- [License](#license)

## Features

### Core Capabilities

- **Tenant Management** - Onboard and manage multiple tenants with WSO2 integration, tenant-specific database initialization, and session token generation
- **Legal Entity Management** - Create and manage legal entities with company details, PAN validation, SPOC information, and logo management
- **Data Processor Management** - Onboard data processors with WSO2 credential generation, certificate management, vendor risk assessment, document validation, and onboard notification with WSO2 consumer credentials (clientId and clientSecret)
- **Consent Configuration** - Configure consent management settings including purposes, data types, processing activities, and workflow stages
- **Grievance Management** - Set up grievance redressal mechanisms with workflow stages, SLA timelines, escalation policies, retention policies, and default communication preferences (SMS and email set to true if not provided)
- **DPO Configuration** - Configure Data Protection Officer (DPO) details, contact information, and designation tracking with dashboard support
- **ROPA Management** - Create and manage Records of Processing Activities (ROPA) for compliance tracking with data category and purpose association
- **Notification Configuration** - Configure notification channels, event types, and customer identifier mappings
- **SMTP/SMSC Configuration** - Configure email and SMS channels with server credentials and gateway settings
- **Purpose & Data Type Management** - Manage purposes and data types with name, code, description, and association capabilities
- **Processor Activity Management** - Track processor activities with data processor and data type linkage, including latest activity retrieval
- **System Configuration** - Manage system-wide settings and configurations with history tracking
- **Business Application Management** - Create and manage business applications with application details
- **Client Credentials Management** - Manage client credentials with consumer key/secret and certificate type management
- **User Management** - Create and manage users with role-based access control, profile management, and permission management
- **User Roles Management** - Manage user roles with component-based access control and role assignment
- **User Dashboard Theme** - Customize user dashboard themes and preferences
- **Retention Configuration** - Manage data retention policies and configurations
- **Document Management** - Base64 document upload, storage, metadata tracking, and versioning support with validation
- **DigiLocker Integration** - Manage DigiLocker credentials for document verification and credential status tracking
- **Data Breach Management** - Report and notify data breaches with breach details and notification status tracking
- **Consent Signing Key** - Generate consent signing keys for secure consent management
- **Subscription Management** - Enable, renew, and manage subscriptions
- **Dashboard Analytics** - Get dashboard statistics for data processors, users, and overall system metrics

### Technical Features

- **Multi-Tenant Architecture** - Complete tenant isolation with dedicated MongoDB databases (tenant_db_{tenantId})
- **RESTful API** - Comprehensive REST API with 28 controllers and 100+ endpoints with OpenAPI/Swagger documentation
- **JWT Authentication** - Secure token-based authentication with session management
- **2FA Support** - Optional two-factor authentication with OTP generation and validation
- **Property Encryption** - Jasypt-based encryption for sensitive configuration properties
- **Document Validation** - File size, content type, and format validation for attachments, certificates, and vendor risk documents
- **Retry Mechanism** - Automatic retry for failed external service calls with configurable retry count (max_retry = 3)
- **Async Processing** - Asynchronous notification handling for improved performance
- **Audit Logging** - Comprehensive audit trail via integration with audit microservice
- **Health Monitoring** - Spring Boot Actuator endpoints for health checks and metrics
- **CORS Support** - Configurable cross-origin resource sharing
- **Request/Response Logging** - Comprehensive request and response logging with correlation IDs
- **Config History Tracking** - Track configuration changes with operation history

## Architecture

### High-Level Architecture

```
┌─────────────────┐      ┌──────────────────────────┐      ┌─────────────────────┐
│  Client Apps    │─────▶│  Partner Portal Backend  │─────▶│  MongoDB            │
│  (REST API)     │      │  (Port 9002)             │      │  - tenant_db_{id}   │
└─────────────────┘      │                          │      │  - cms_db_admin     │
                         │  - Tenant Management     │      └─────────────────────┘
                         │  - Legal Entity Mgmt     │               │
                         │  - Data Processor Mgmt   │               │
                         │  - Consent Config        │               ▼
                         │  - Grievance Config      │      ┌─────────────────────┐
                         │  - DPO Config            │      │  External Services  │
                         │  - ROPA Management       │      │  - WSO2 Service     │
                         │  - User Management       │      │  - Vault Service    │
                         │  - Dashboard Analytics   │      │  - Auth Service     │
                         └──────────────────────────┘      │  - Notification Svc  │
                                   │                       │  - Audit Service    │
                                   │                       └─────────────────────┘
                                   ▼
                         ┌──────────────────────────┐
                         │  Multi-Tenant Routing    │
                         │  TenantMongoTemplate     │
                         │  Provider                │
                         └──────────────────────────┘
```

### Technology Stack

- **Framework:** Spring Boot 3.5.4
- **Language:** Java 21
- **Database:** MongoDB (multi-tenant architecture)
- **Security:** Spring Security 6.5.7
- **API Documentation:** OpenAPI 3.0 / Swagger UI (SpringDoc 2.8.11)
- **HTTP Client:** Apache HttpClient 5
- **JWT:** Nimbus JOSE + JWT 10.0.2
- **Encryption:** Jasypt 3.0.5
- **Logging:** Logback 1.5.21 with tenant-specific appenders
- **Container Runtime:** Docker with eclipse-temurin:21-jre
- **Orchestration:** Kubernetes
- **Build Tool:** Maven
- **Tomcat:** 10.1.49
- **Spring Framework:** 6.2.14
- **Commons Lang3:** 3.20.0

### Multi-Tenant Design

The service implements database-per-tenant isolation:

- Each tenant has a dedicated MongoDB database: `tenant_db_{tenantId}`
- Shared system configuration in `cms_db_admin` database
- Tenant identification via `tenant-id` header in all API requests
- Automatic database routing via `TenantMongoTemplateProvider`
- Tenant-specific log file segregation
- Complete data isolation at the database level
- Automatic TTL index creation for OTP and auth_secret collections

### Integration Points

The service integrates with the following external services:

1. **WSO2 Credential Generator Service** - Tenant registration (`/registerTenant`), business onboarding (`/onboardBusiness`), and data processor onboarding (`/onboardDataProcessor`)
2. **Vault Service** - Key onboarding (`/client/key/onboard`) and document signing (`/client/sign`)
3. **Auth Service** - OAuth2 token generation (`/oauth2/token`)
4. **Notification Service** - Notification setup (`/notification/v1/onboarding/setup`), event triggering (`/notification/v1/events/trigger`), system event triggering (`/notification/v1/system/events/trigger`), and OTP verification (`/notification/v1/system/otp/verify`)
5. **Audit Service** - Audit log management (`/audit/v1/audit`)

## Prerequisites

Before running the Partner Portal Backend Service, ensure you have:

### Required Software

- **Java Development Kit (JDK):** 21 or higher
- **Apache Maven:** 3.6+ (for building)
- **MongoDB:** 4.4 or higher
- **Docker:** 20.10+ (for containerized deployment, optional)
- **Kubernetes:** 1.21+ (for orchestration, optional)

### Required Services

- **MongoDB Server** - For tenant-specific and shared databases
- **WSO2 Credential Generator Service** - For tenant/business/data processor onboarding
- **Vault Service** - For key management and document signing
- **Auth Service** - For OAuth2 token generation
- **Notification Service** - For event triggering and notifications
- **Audit Service** - For audit trail logging

### Network Requirements

- MongoDB connection (localhost:27017 or cluster)
- Access to WSO2 Credential Generator Service (default: http://localhost:30004/api/v1)
- Access to Vault Service (default: http://localhost:30010)
- Access to Auth Service (default: http://localhost:30009)
- Access to Notification Service (default: http://localhost:30005)
- Access to Audit Service (default: http://localhost:30006)

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd partner-portal-backend
```

### 2. Configure Environment

Set required environment variables:

```bash
export MONGODB_URI=mongodb://localhost:27017/
export MONGODB_DATABASE=partnerportal
export SERVER_PORT=9002
export WSO2_CRED_GENERATOR_APIS_URL=http://localhost:30004/api/v1
export VAULT_APIS_URL=http://localhost:30010
export AUTH_APIS_URL=http://localhost:30009
export NOTIFICATION_MODULE_APIS_URL=http://localhost:30005
export AUDIT_MODULE_APIS_URL=http://localhost:30006
```

### 3. Build the Application

```bash
# Clean build with tests
mvn clean package

# Build without tests
mvn clean package -DskipTests
```

### 4. Run the Application

```bash
# Run locally
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run the JAR file
java -jar target/partnerportal-0.0.1-SNAPSHOT.jar
```

### 5. Verify the Application

```bash
# Check health
curl http://localhost:9002/partnerportal/actuator/health

# Access Swagger UI
open http://localhost:9002/partnerportal/swagger-ui.html
```

## Configuration

### Core Environment Variables

#### Server Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | HTTP server port | `9002` | No |
| `PROFILE` | Spring profile (dev/sit/prod) | `dev` | No |

#### MongoDB Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MONGODB_URI` | MongoDB connection URI | - | Yes |
| `MONGODB_DATABASE` | Shared database name | - | Yes |

#### WSO2 Service Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `WSO2_CRED_GENERATOR_APIS_URL` | WSO2 service base URL | `http://localhost:30004/api/v1` | No |
| `wso2.token.url` | WSO2 token URL | `https://<wso2-host>:8243/token` | No |

#### Vault Service Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `VAULT_APIS_URL` | Vault service base URL | `http://localhost:30010` | No |

#### Auth Service Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AUTH_APIS_URL` | Auth service base URL | `http://localhost:30009` | No |

#### Notification Service Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `NOTIFICATION_MODULE_APIS_URL` | Notification service base URL | `http://localhost:30005` | No |

#### Audit Service Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AUDIT_MODULE_APIS_URL` | Audit service base URL | `http://localhost:30006` | No |

#### Security Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `2FA` | Enable 2FA authentication | `false` | No |
| `JASYPT_ENCRYPTOR_PASSWORD` | Jasypt encryption password | (secure value) | Yes (if using encryption) |

#### Portal Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PORTAL_URL` | Portal frontend URL | `https://partnerportal.example.com` | No |

#### Search Parameters

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PROCESSOR_SEARCH_PARAMS` | Data processor search parameters | `processorActivityId` | No |

### Application Properties

Key configuration properties (from `application.properties`):

- **Server Configuration**:
    - `server.servlet.context-path=/partnerportal`
    - `server.port=${SERVER_PORT:9002}`
    - `spring.application.name=partnerportal`

- **MongoDB Configuration**:
    - `spring.data.mongodb.uri=${MONGODB_URI}`
    - `spring.data.mongodb.database=${MONGODB_DATABASE}`
    - `spring.data.mongodb.auto-index-creation=true`

- **Security Configuration**:
    - `security.2fa.enabled=${2FA:false}`
    - `jasypt.encryptor.algorithm=PBEWITHHMACSHA512ANDAES_256`
    - `x-session-token.enable=true`

- **Timeouts**:
    - `http.client.connect-timeout=5000`
    - `http.client.read-timeout=5000`

- **OTP Configuration**:
    - `otp.time.to.live=PT10M` (10 minutes)
    - `auth.secret.time.to.live=PT3M` (3 minutes)

- **Retry Configuration**:
    - `max_retry=3`

- **Search Parameters**: Configurable search parameters for each entity type (tenants, legal-entity, smtp, smsc, grievance, consent, dpo, ropa, purpose, dataType, dataProcessor, processorActivity, systemConfig, businessApplication, config-history, client-credentials, notification, digilocker.credential)

### Property Encryption

Sensitive properties can be encrypted using Jasypt:

```bash
java -cp jasypt-1.9.3.jar org.jasypt.intf.cli.JasyptPBEStringEncryptionCLI \
  input="your-secret-value" \
  password="<your-jasypt-encryptor-password>" \
  algorithm="PBEWITHHMACSHA512ANDAES_256"
```

Use the encrypted value in properties as: `ENC(encrypted-value)`

## API Documentation

### REST API Endpoints

The Partner Portal Backend Service exposes 28 controllers with 100+ REST API endpoints organized by functional domain:

#### Base URL

```
http://localhost:9002/partnerportal
```

### 1. Tenant Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/tenant/onboard` | Onboard a new tenant |
| `GET` | `/v1.0/tenant/search` | Search tenants |
| `GET` | `/v1.0/tenant/count` | Get tenant count |

### 2. Legal Entity Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `PUT` | `/v1.0/legal-entity/{legalEntityId}` | Update a legal entity |
| `GET` | `/v1.0/legal-entity/search` | Search legal entities |
| `GET` | `/v1.0/legal-entity/count` | Get legal entity count |

### 3. Data Processor Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/data-processor/create` | Create a data processor |
| `PUT` | `/v1.0/data-processor/update/{dataProcessorId}` | Update a data processor |
| `GET` | `/v1.0/data-processor/search` | Search data processors |
| `GET` | `/v1.0/data-processor/count` | Get data processor count |

### 4. Consent Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/consent/create` | Create consent configuration |
| `PUT` | `/v1.0/consent/update/{configId}` | Update consent configuration |
| `GET` | `/v1.0/consent/search` | Search consent configurations |
| `GET` | `/v1.0/consent/count` | Get consent configuration count |

### 5. Grievance Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/grievance/create` | Create grievance configuration |
| `PUT` | `/v1.0/grievance/update/{configId}` | Update grievance configuration |
| `GET` | `/v1.0/grievance/search` | Search grievance configurations |
| `GET` | `/v1.0/grievance/count` | Get grievance configuration count |

### 6. DPO Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/dpo/create` | Create DPO configuration |
| `PUT` | `/v1.0/dpo/update/{configId}` | Update DPO configuration |
| `GET` | `/v1.0/dpo/search` | Search DPO configurations |
| `GET` | `/v1.0/dpo/count` | Get DPO configuration count |
| `GET` | `/v1.0/dpo/dpodashboard` | Get DPO dashboard data |

### 7. ROPA Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/ropa/create` | Create ROPA record |
| `PUT` | `/v1.0/ropa/update/{ropaId}` | Update ROPA record |
| `GET` | `/v1.0/ropa/{ropaId}` | Get ROPA record by ID |
| `DELETE` | `/v1.0/ropa/delete/{ropaId}` | Delete ROPA record |
| `GET` | `/v1.0/ropa` | Search ROPA records |
| `GET` | `/v1.0/ropa/count` | Get ROPA record count |

### 8. Notification Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/notification/create` | Create notification configuration |
| `PUT` | `/v1.0/notification/update/{configId}` | Update notification configuration |
| `GET` | `/v1.0/notification/search` | Search notification configurations |
| `GET` | `/v1.0/notification/count` | Get notification configuration count |

### 9. SMTP Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/smtp/create` | Create SMTP configuration |
| `PUT` | `/v1.0/smtp/update/{configId}` | Update SMTP configuration |
| `GET` | `/v1.0/smtp/search` | Search SMTP configurations |
| `GET` | `/v1.0/smtp/count` | Get SMTP configuration count |

### 10. SMSC Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/smsc/create` | Create SMSC configuration |
| `PUT` | `/v1.0/smsc/update/{configId}` | Update SMSC configuration |
| `GET` | `/v1.0/smsc/search` | Search SMSC configurations |
| `GET` | `/v1.0/smsc/count` | Get SMSC configuration count |

### 11. Purpose Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/purpose/create` | Create a purpose |
| `PUT` | `/v1.0/purpose/update/{purposeId}` | Update a purpose |
| `GET` | `/v1.0/purpose/search` | Search purposes |
| `GET` | `/v1.0/purpose/count` | Get purpose count |

### 12. Data Type Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/data-type/create` | Create a data type |
| `PUT` | `/v1.0/data-type/update/{dataTypeId}` | Update a data type |
| `GET` | `/v1.0/data-type/search` | Search data types |
| `GET` | `/v1.0/data-type/count` | Get data type count |

### 13. Processor Activity Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/processor-activity/create` | Create a processor activity |
| `PUT` | `/v1.0/processor-activity/update/{processorActivityId}` | Update a processor activity |
| `GET` | `/v1.0/processor-activity/search` | Search processor activities |
| `GET` | `/v1.0/processor-activity/count` | Get processor activity count |
| `GET` | `/v1.0/processor-activity/get-latest` | Get latest processor activity |

### 14. System Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/system-config/create` | Create system configuration |
| `PUT` | `/v1.0/system-config/update/{configId}` | Update system configuration |
| `GET` | `/v1.0/system-config/search` | Search system configurations |
| `GET` | `/v1.0/system-config/count` | Get system configuration count |

### 15. Business Application Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/business-application/create` | Create a business application |
| `PUT` | `/v1.0/business-application/update/{businessId}` | Update a business application |
| `GET` | `/v1.0/business-application/search` | Search business applications |
| `GET` | `/v1.0/business-application/count` | Get business application count |

### 16. Client Credentials Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1.0/client-credential` | Get client credentials |

### 17. User Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/users/create` | Create a user |
| `PUT` | `/users/update/{userId}` | Update a user |
| `DELETE` | `/users/delete/{userId}` | Delete a user |
| `GET` | `/users/search` | Search users |
| `GET` | `/users/count` | Get user count |
| `GET` | `/users/list` | Get user list |
| `GET` | `/users/profile` | Get user profile |

### 18. User Roles Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/role/create` | Create a role |
| `PUT` | `/role/update` | Update a role |
| `DELETE` | `/role/delete` | Delete a role |
| `GET` | `/role/search` | Search roles |
| `GET` | `/role/count` | Get role count |
| `GET` | `/role/list` | Get role list |
| `GET` | `/role/component/list` | Get component list |

### 19. User Dashboard Theme APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/user-dashboard-theme/create` | Create user dashboard theme |
| `PUT` | `/v1.0/user-dashboard-theme/update` | Update user dashboard theme |
| `GET` | `/v1.0/user-dashboard-theme/get` | Get user dashboard theme |

### 20. Retention Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/retention/create` | Create retention configuration |
| `PUT` | `/retention/update` | Update retention configuration |
| `DELETE` | `/retention/delete` | Delete retention configuration |
| `GET` | `/retention/search` | Search retention configurations |

### 21. DigiLocker Credential APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/digilocker/credential/create` | Create DigiLocker credential |
| `PUT` | `/v1.0/digilocker/credential/update/{credentialId}` | Update DigiLocker credential |
| `GET` | `/v1.0/digilocker/credential/search` | Search DigiLocker credentials |
| `GET` | `/v1.0/digilocker/credential/count` | Get DigiLocker credential count |

### 22. Data Breach Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/data-breach/create` | Create data breach report |
| `PUT` | `/v1.0/data-breach/{id}` | Update data breach report |
| `GET` | `/v1.0/data-breach/{id}` | Get data breach report by ID |
| `GET` | `/v1.0/data-breach` | Get all data breach reports |
| `POST` | `/v1.0/data-breach/notify/{incidentId}` | Notify data breach |

### 23. Consent Signing Key APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/consent-signing-key/generate` | Generate consent signing key |

### 24. Subscription Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1.0/subscription/enable` | Enable subscription |
| `POST` | `/v1.0/subscription/renew` | Renew subscription |
| `GET` | `/v1.0/subscription/get` | Get subscription details |

### 25. Dashboard APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1.0/dashboard/data` | Get dashboard data |
| `GET` | `/v1.0/dashboard/data-processors` | Get data processor statistics |
| `GET` | `/v1.0/dashboard/users` | Get user statistics |

### 26. Authentication APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/otp/init` | Initialize OTP |
| `POST` | `/otp/validate` | Validate OTP |
| `POST` | `/tenant/otp/init` | Initialize tenant OTP |
| `POST` | `/tenant/otp/validate` | Validate tenant OTP |

### 27. Audit APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1.0/audit/search` | Search audit logs |

### Authentication & Headers

All API requests require the following headers:

```
txn: <uuid>                              # Required for request tracking
tenant-id: <tenant_identifier>          # Required for tenant-scoped operations
business-id: <business_identifier>      # Required for business-scoped operations
scope-level: TENANT|BUSINESS             # Required for configuration operations
x-session-token: Bearer <session-token>  # Required for authenticated endpoints
Content-Type: application/json           # For POST/PUT requests
```

### Response Format

**Success Response:**
`Depends on API http method.`

**Error Response:**
```json
{
  "errors": [
    {
      "errorCode": "JCMP1001",
      "errorMessage": "Error description",
      "parameter": "field-name (if applicable)",
      "header": "header-name (if applicable)"
    }
  ],
  "timestamp": "2025-01-15T10:30:00Z"
}
```

### OpenAPI / Swagger UI

Access the interactive API documentation:

- **Swagger UI:** http://localhost:9002/partnerportal/swagger-ui.html
- **OpenAPI JSON:** http://localhost:9002/partnerportal/v3/api-docs
- **OpenAPI YAML:** http://localhost:9002/partnerportal/v3/api-docs.yaml

**API Title:** Partner Portal APIs  
**API Version:** 1.0  
**Description:** List of all Partner Portal APIs

## Development

### Project Structure

```
partner-portal-backend/
├── src/
│   ├── main/
│   │   ├── java/com/jio/partnerportal/
│   │   │   ├── client/              # External service clients
│   │   │   │   ├── audit/          # Audit service client
│   │   │   │   ├── auth/           # Auth service client
│   │   │   │   ├── notification/   # Notification service client
│   │   │   │   ├── vault/          # Vault service client
│   │   │   │   └── wso2/           # WSO2 service client
│   │   │   ├── config/             # Spring configurations
│   │   │   ├── constant/           # Application constants
│   │   │   ├── controller/         # REST API controllers (28 controllers)
│   │   │   ├── dto/                # Data Transfer Objects
│   │   │   │   ├── request/        # Request DTOs
│   │   │   │   └── response/       # Response DTOs
│   │   │   ├── entity/             # MongoDB entities
│   │   │   ├── exception/          # Custom exceptions
│   │   │   ├── interceptor/        # Request interceptors
│   │   │   ├── multitenancy/       # Multi-tenant support
│   │   │   ├── repository/         # MongoDB repositories
│   │   │   ├── repositoryImpl/     # Custom repository implementations
│   │   │   ├── service/            # Business logic services (28 services)
│   │   │   └── util/               # Utility classes
│   │   └── resources/
│   │       ├── application.properties
│   │       └── logback-spring.xml
│   └── test/
│       └── java/                    # Unit and integration tests
├── deployment/
│   ├── Dockerfile
│   ├── jio-dl-deployment.yaml
│   ├── jio-dl-deployment-sit.yaml
│   └── Public_deployment.yaml
├── pom.xml
└── README.md
```

### Building from Source

```bash
# Clean build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build with code coverage
mvn clean test jacoco:report
```

### IDE Setup

#### IntelliJ IDEA

1. Import as Maven project
2. Set JDK to Java 21
3. Install Lombok plugin
4. Enable annotation processing

#### Eclipse

1. Import as existing Maven project
2. Configure Java 21 compiler
3. Install Lombok (https://projectlombok.org/setup/eclipse)

### Local Development

For local development:

```bash
# Use default profile
mvn spring-boot:run

# Or set environment variable
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DataProcessorServiceTest

# Run tests matching pattern
mvn test -Dtest=*ControllerTest

# Run with code coverage
mvn test jacoco:report
```

### Test Coverage

The project maintains test coverage for critical business logic:

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Test Structure

Tests are organized by type:
- **Unit Tests** - Service and utility class tests
- **Integration Tests** - Full Spring context tests with MongoDB
- **Controller Tests** - REST API endpoint tests

## Deployment

### Docker Deployment

#### Build Docker Image

```bash
# Build image
docker build -f deployment/Dockerfile -t partner-portal-backend:0.0.1-SNAPSHOT .

# Run container
docker run -d \
  --name partner-portal-backend \
  -p 9002:9002 \
  -e MONGODB_URI=mongodb://host:27017/ \
  -e MONGODB_DATABASE=partnerportal \
  -e WSO2_CRED_GENERATOR_APIS_URL=http://wso2:30004/api/v1 \
  partner-portal-backend:0.0.1-SNAPSHOT
```

#### Docker Image Details

- **Base Image:** `<docker-registry-host>/docker-virtual-all/eclipse-temurin:21-jre`
- **User:** `si_digigov` (non-root)
- **Working Directory:** `/opt`
- **Exposed Port:** 9002
- **Entry Point:** `java -Djasypt.encryptor.password=n&G$I)@rtN#rI)%R^@l -jar -Dspring.config.location=/opt/props/application.properties -Dpartnerportal_log_path=/opt/logs /opt/deployment/partnerportal-0.0.1-SNAPSHOT.jar`
- **Maintainer:** Kuldeep Prajapati <Kuldeep.Prajapati@ril.com>

### Kubernetes Deployment

#### Prerequisites

- Kubernetes cluster 1.21+
- kubectl configured
- ConfigMaps and Secrets created

#### Deployment Files

- **SIT Environment:** `deployment/jio-dl-deployment-sit.yaml`
- **Production:** `deployment/jio-dl-deployment.yaml`
- **Public Cloud:** `deployment/Public_deployment.yaml`

#### Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace dpdp-cms

# Create ConfigMap for application.properties
kubectl create configmap partner-portal-config \
  --from-file=application.properties=src/main/resources/application.properties \
  -n dpdp-cms

# Create Secret for sensitive values
kubectl create secret generic partner-portal-secret \
  --from-literal=mongodb-uri=mongodb://... \
  --from-literal=jasypt-password=... \
  -n dpdp-cms

# Deploy application
kubectl apply -f deployment/jio-dl-deployment-sit.yaml -n dpdp-cms

# Check deployment status
kubectl get pods -n dpdp-cms
kubectl logs -f deployment/partner-portal-backend -n dpdp-cms

# Access service
kubectl port-forward service/partner-portal-backend 9002:9002 -n dpdp-cms
```

#### Kubernetes Resources

The deployment includes:
- **Deployment** - Manages pod replicas
- **Service** - Internal ClusterIP service
- **ConfigMap** - Application configuration
- **Secret** - Sensitive credentials

#### Health Probes

```yaml
livenessProbe:
  httpGet:
    path: /partnerportal/actuator/health/liveness
    port: 9002
  initialDelaySeconds: 120
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /partnerportal/actuator/health/readiness
    port: 9002
  initialDelaySeconds: 60
  periodSeconds: 10
```

### Environment-Specific Configuration

| Environment | Namespace | MongoDB | WSO2 Service | Port |
|-------------|-----------|---------|---------------|------|
| Local | - | localhost:27017 | localhost:30004 | 9002 |
| SIT | dpdp-cms-sit | sit-mongo-cluster | sit-wso2-service | 9002 |
| Production | dpdp-cms | prod-mongo-cluster | prod-wso2-service | 9002 |

## Monitoring

### Health Checks

The service exposes Spring Boot Actuator endpoints for monitoring:

```bash
# Overall health
curl http://localhost:9002/partnerportal/actuator/health

# Detailed health
curl http://localhost:9002/partnerportal/actuator/health?details=true

# Readiness probe
curl http://localhost:9002/partnerportal/actuator/health/readiness

# Liveness probe
curl http://localhost:9002/partnerportal/actuator/health/liveness
```

### Application Information

```bash
# Application info
curl http://localhost:9002/partnerportal/actuator/info
```

### Logging

The service uses structured logging with tenant-specific log files:

**Log Levels:**
- `ERROR` - Critical failures requiring immediate attention
- `WARN` - Warning conditions (retries, degraded performance)
- `INFO` - Important business events (tenant onboarded, entity created)
- `DEBUG` - Detailed diagnostic information (disabled in production)

**Log Configuration:**
```bash
# Set log level via environment variable
export LOGGING_LEVEL_COM_JIO_PARTNERPORTAL=DEBUG
```

**Log Output:**
- **Console:** Standard output
- **File:** `logs/partnerportal/` (local) or `/opt/logs/` (container)
- **Tenant-specific:** Logs segregated by tenant ID

### Monitoring Best Practices

1. **Set up Prometheus + Grafana** - Visualize metrics and create dashboards
2. **Configure Alerts** - Alert on high error rates, latency spikes, or failures
3. **Centralized Logging** - Use ELK stack (Elasticsearch, Logstash, Kibana) or similar
4. **Distributed Tracing** - Integrate with Zipkin or Jaeger for request tracing
5. **APM Tools** - Consider Application Performance Monitoring tools

## Dependencies

### Core Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.4 | Framework foundation |
| Java | 21 | Runtime platform |
| MongoDB | 4.4+ | Database |

### Spring Boot Starters

- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-security` - Security support
- `spring-boot-starter-actuator` - Health and metrics
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-test` - Testing support

### Third-Party Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| springdoc-openapi | 2.8.11 | OpenAPI/Swagger documentation |
| lombok | 1.18.38 | Reduce boilerplate code |
| nimbus-jose-jwt | 10.0.2 | JWT token handling |
| httpclient5 | 5.x | HTTP client |
| jasypt-spring-boot-starter | 3.0.5 | Property encryption |
| commons-lang3 | 3.20.0 | Utility functions |
| logback-classic | 1.5.21 | Logging |
| logback-core | 1.5.21 | Logging |
| tomcat-embed-core | 10.1.49 | Embedded Tomcat |
| spring-retry | (Spring managed) | Retry mechanism |
| jackson-datatype-jsr310 | (Spring managed) | JSON date/time handling |
| jakarta.persistence-api | 3.1.0 | JPA API |
| jakarta.validation-api | 3.0.2 | Bean validation API |
| jakarta.jakartaee-api | 10.0.0 | Jakarta EE API |
| google.zxing | 3.5.3 | QR code generation |
| commons-codec | 1.18.0 | Codec utilities |
| android-json | 0.0.20131108.vaadin1 | JSON processing |

### Security Dependency Overrides

The project overrides versions for CVE fixes:

| Dependency | Version | CVE Fixed |
|------------|---------|-----------|
| tomcat-embed-core | 10.1.49 | Security vulnerabilities |
| logback-classic | 1.5.21 | CVE-2025-11226 |
| logback-core | 1.5.21 | CVE-2025-11226 |
| spring-security | 6.5.7 | Security patches |
| spring-framework | 6.2.14 | Security patches |
| commons-lang3 | 3.20.0 | Security updates |

### Version Compatibility

| Component | Minimum Version | Recommended Version |
|-----------|-----------------|---------------------|
| Java | 21 | 21 LTS |
| Maven | 3.6.0 | 3.9.x |
| MongoDB | 4.4 | 7.0+ |
| Docker | 20.10 | 24.x |
| Kubernetes | 1.21 | 1.28+ |

### Dependency Management

```bash
# Check for dependency updates
mvn versions:display-dependency-updates

# Display dependency tree
mvn dependency:tree

# Analyze dependencies
mvn dependency:analyze
```

## Support

### Getting Help

- **Documentation:** https://docs.dpdp-cms.gov.in (Update with actual URL)
- **Support Email:** Jio.ConsentSupport@ril.com
- **Issue Tracking:** [Update with issue tracking URL]

### Reporting Issues

When reporting issues, please include:

1. **Environment** - OS, Java version, deployment method
2. **Steps to Reproduce** - Clear steps to reproduce the issue
3. **Expected Behavior** - What you expected to happen
4. **Actual Behavior** - What actually happened
5. **Logs** - Relevant log excerpts (use DEBUG level if possible)
6. **Configuration** - Relevant environment variables and configuration

### Security Vulnerabilities

**DO NOT** report security vulnerabilities through public GitHub issues.

Please report security vulnerabilities to: **security@dpdp-cms.gov.in** (Update with actual email)

Include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Suggested fix (if available)

We will acknowledge receipt within 48 hours and provide a timeline for remediation.

### Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on:
- Code standards and style
- Development workflow
- Pull request process
- Testing requirements
- Documentation expectations

### Code of Conduct

This project adheres to a code of conduct. Please see [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details.

## License

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)** with additional DPDP-specific terms.

```
Copyright (c) 2025 Government of India

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

See [LICENSE](LICENSE) for complete terms and additional DPDP Act 2023 compliance requirements.

## Compliance

This software is designed to facilitate compliance with:

- **Digital Personal Data Protection Act, 2023 (DPDP Act)** - India's data protection law
- **Multi-tenant Data Isolation** - Complete tenant data separation for privacy compliance
- **Audit Trail Requirements** - Comprehensive audit logging for all operations
- **Data Breach Notification** - Support for data breach reporting and notification
- **ROPA Management** - Record of Processing Activities for compliance tracking
- **Grievance Redressal** - Support for grievance handling with SLA timelines and escalation policies

## Acknowledgments

This project is part of the **DPDP Consent Management System**, a Digital Public Good initiative designed to ensure compliance with India's DPDP Act, 2023.

**Developed for:** Ministry of Electronics and Information Technology (MeitY), Government of India

---

**Version:** 0.0.1-SNAPSHOT

**Last Updated:** 2025-01-21

**Maintained by:** DPDP CMS Team
