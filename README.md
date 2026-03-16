# JCMS - Jio Consent Management System

## Overview

The **Jio Consent Management System (JCMS)** is a comprehensive, government Digital Public Good designed to ensure compliance with the **Digital Personal Data Protection (DPDP) Act, 2023 of India**. This microservices-based platform provides end-to-end consent management capabilities across the entire data lifecycle, from consent creation and validation to audit trails and grievance resolution.

## Table of Contents

- [System Architecture](#system-architecture)
- [Microservices Overview](#microservices-overview)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development](#development)
- [Testing](#testing)
- [Deployment](#deployment)
- [Monitoring](#monitoring)
- [Security & Compliance](#security--compliance)
- [Contributing](#contributing)
- [Support](#support)
- [Related Documentation](#related-documentation)
- [License](#license)

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           JCMS - Consent Management System                          │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │   Partner       │  │   Consent       │  │   Notification  │  │   Grievance    │  │
│  │   Portal        │  │   Core          │  │   Module        │  │   Service      │  │
│  │   Port: 9002    │  │   Port: 9001    │  │   Port: 9005    │  │   Port: 9007   │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  └────────────────┘  │
│           │                     │                     │                     │         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │   Notification  │  │   Audit         │  │   Scheduler     │  │   WSO2 Cred    │  │
│  │   Consumer      │  │   Module        │  │   Service       │  │   Generator    │  │
│  │   Port: 9020    │  │   Port: 9006    │  │   Port: 9001    │  │   Port: 9004   │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  └────────────────┘  │
│           │                     │                     │                     │         │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌────────────────┐  │
│  │   Auth Service  │  │   Frontends     │  │   Vault Service │  │   System       │  │
│  │   Port: 9009    │  │   (see below)   │  │   Port: 9010    │  │   Port: 9011   │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  └────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                    ┌───────────────────┼───────────────────┐
                    │                   │                   │
          ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
          │   MongoDB       │  │   Apache        │  │   WSO2 API      │
          │   Multi-Tenant  │  │   Kafka         │  │   Manager       │
          │   Databases     │  │   Messaging     │  │   OAuth2        │
          └─────────────────┘  └─────────────────┘  └─────────────────┘
```

### Service Communication Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  Frontend   │───▶│   Partner   │───▶│   Consent   │───▶│   Audit     │
│  React App  │    │   Portal    │    │   Core      │    │   Core      │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   │
┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │
│ Auth Service│    │WSO2 Cred    │    │  Grievance  │             │
│   OAuth2    │    │ Generator   │    │   Service   │             │
└─────────────┘    └─────────────┘    └─────────────┘             │
       │                   │                   │                   │
       │                   ▼                   ▼                   │
       │            ┌─────────────┐    ┌─────────────┐             │
       │            │WSO2 API     │    │Notification │             │
       │            │Manager      │    │   Module    │             │
       │            └─────────────┘    └─────────────┘             │
       │                                      │                   │
       │                                      ▼                   │
       │                              ┌─────────────┐             │
       │                              │   Kafka     │             │
       │                              │  Messaging  │             │
       │                              └─────────────┘             │
       │                                      │                   │
       │                                      ▼                   │
       │                              ┌─────────────┐             │
       │                              │ Notification│             │
       │                              │  Consumer   │             │
       │                              └─────────────┘             │
       │                                                          ▼
       └─────────────────────────────────────────────────┌─────────────┐
                                                         │ Scheduler   │
                                                         │ Service     │
                                                         └─────────────┘
```

## Microservices Overview

### 1. Partner Portal Service (Port: 9002)

**Purpose**: Comprehensive REST API for managing partner organizations, legal entities, data processors, and system configurations.

**Key Features**:
- Tenant and business management with WSO2 integration
- Legal entity and data processor onboarding
- Consent and grievance configuration management
- User management with role-based access control
- Document management with validation
- Dashboard analytics and reporting

**Database**: MongoDB (tenant_db_{tenantId}, cms_db_admin)

### 2. Consent Core Service (Port: 9001)

**Purpose**: Core engine for managing consent lifecycle, templates, and consent handles with multi-tenant architecture.

**Key Features**:
- Consent lifecycle management (Create, Update, Validate, Withdraw)
- Consent handle and template management
- Parental KYC integration via DigiLocker
- JWT and JWS signature support
- Payload hash verification
- Advanced search and analytics

**Database**: MongoDB (tenant_db_{tenantId})

### 3. Notification Module (Port: 9005)

**Purpose**: Primary interface for triggering notification events and managing multi-channel notifications (SMS, Email, Webhooks).

**Key Features**:
- Event triggering with multi-channel delivery
- Template management with DigiGov integration
- Master list management for dynamic data resolution
- OTP services with encryption and rate limiting
- Kafka event publishing
- Token caching and management

**Messaging**: Apache Kafka Producer

### 4. Notification Consumer (Port: 9020)

**Purpose**: Delivery engine for consuming notification events from Kafka and delivering them through multiple channels.

**Key Features**:
- Multi-channel delivery (SMS, Email, Webhook)
- Kafka consumer with retry mechanisms
- Template resolution with dynamic substitution
- Status tracking and delivery confirmation
- DLT compliance for SMS notifications

**Messaging**: Apache Kafka Consumer

### 5. Grievance Service (Port: 9007)

**Purpose**: Collect, track, and resolve citizen or partner grievances with comprehensive workflow management.

**Key Features**:
- Tenant-aware grievance CRUD with full history
- Dynamic template library with multilingual support
- Catalogues for grievance types and user types
- Vault-signed JWT issuance for verification
- Notification integration for status updates

**Database**: MongoDB (tenant_db_{tenantId}, tenant_db_shared)

### 6. Audit Module Service (Port: 9006)

**Purpose**: Comprehensive audit recording, PDF certificate generation, and publishing audit events to Kafka.

**Key Features**:
- Section 65B-style PDF certificate generation
- PAdES signing integration
- Kafka audit event publishing
- Tenant-aware operations
- JSON serialization with Jackson

**Messaging**: Kafka Producer (audit-events topic)

### 7. Scheduler Service (Port: 9001)

**Purpose**: Runs tenant-aware scheduled jobs for consent lifecycle management and system maintenance.

**Key Features**:
- Consent and consent-handle expiry processing
- Preference reminder scheduling
- Data retention and cleanup jobs
- Grievance escalation automation
- Batch processing with configurable parameters

**Database**: MongoDB (tenant databases)

### 8. WSO2 Credentials Generation Service (Port: 9004)

**Purpose**: Critical microservice for generating and managing WSO2 API Manager credentials, handling tenant registration and credential provisioning.

**Key Features**:
- Tenant registration with WSO2 API Manager
- Business onboarding with OAuth2 credential generation
- Data processor credential provisioning
- Idempotent operations preventing duplicate registrations
- API subscription management
- Secure credential storage in tenant databases

**Integration**: WSO2 API Manager OAuth2, MongoDB (tenant_db_{tenantId})

### 9. Auth Service (Port: 9009)

**Purpose**: Comprehensive authentication and authorization service managing JWT tokens, session management, and secure access control.

**Key Features**:
- JWT token generation with RS256 signing
- Token validation, introspection, and revocation
- Session management with configurable limits (max 3 sessions)
- WSO2 token integration for API access
- Secure code management for session workflows
- Multi-tenant authentication isolation

**Database**: MongoDB (cms_db_admin, tenant_db_{tenantId})
**Security**: RSA key-based JWT signing, BCrypt token hashing

### 10. Cookie Consent Service (Port: 9008)

**Purpose**: Cookie consent and scanning APIs for DPDP-compliant cookie banners and preference management.

**Key Features**: Cookie scanning, consent capture, integration with consent core and vault.

**Database**: MongoDB (multi-tenant). See [Backend/cookie-consent-apis](Backend/cookie-consent-apis/README.md).

### 11. Translator Service (Port: configurable, default 9002 in module)

**Purpose**: Bhashini-based translation for multi-language support across the platform.

**Key Features**: Business/tenant credential hierarchy, token caching, multiple Indian languages. See [Backend/translator-apis](Backend/translator-apis/README.md).

### 12. Vault Service (Port: 9010)

**Purpose**: Encryption, decryption, signing, and verification via HashiCorp Vault integration.

**Key Features**: JWT signing/verification, payload encryption, certificate onboarding. See [Backend/vault-apis](Backend/vault-apis/README.md).

### 13. System Registry Service (Port: 9011)

**Purpose**: System, integration, and dataset registry; consent withdraw API for scheduler and other consumers.

**Key Features**: Consent withdraw endpoint, audit and notification integration. See [Backend/system-registry-apis](Backend/system-registry-apis/README.md).

### 14. Frontend Applications

**Purpose**: React-based user interfaces for consent management, grievance, and compliance.

| Application | Description | README |
|-------------|-------------|--------|
| **Partner Portal Frontend** | Partner/DPO portal — templates, data processors, grievance, cookie management | [Frontend/partner-portal-frontend](Frontend/partner-portal-frontend/README.md) |
| **User Portal Frontend** | Data principal portal — consents, grievances, requests | [Frontend/user-portal-frontend](Frontend/user-portal-frontend/README.md) |
| **Consent Popup Frontend** | Cookie consent popup and redirect flows | [Frontend/consent-popup-frontend](Frontend/consent-popup-frontend/README.md) |

**Technology**: React, Redux or state management, Parcel bundler, JDS Core UI components where used.

## Technology Stack

### Core Technologies
- **Java**: 21 LTS
- **Framework**: Spring Boot 3.5.x
- **Build Tool**: Maven 3.8+
- **Database**: MongoDB 5.0+ (Multi-tenant)
- **Messaging**: Apache Kafka 2.8+
- **Container**: Docker with Eclipse Temurin 21 JRE
- **Orchestration**: Kubernetes 1.21+

### Key Libraries
- **Spring Security**: 6.5.7
- **SpringDoc OpenAPI**: 2.8.11
- **MongoDB Driver**: Spring Data MongoDB
- **Kafka Client**: Spring Kafka
- **JWT/JWS**: Nimbus JOSE JWT 10.0.2
- **Logging**: Logback with JSON encoding
- **Caching**: Caffeine
- **PDF Generation**: Apache PDFBox

## Prerequisites

### Software Requirements
- **Java Development Kit (JDK)**: 21 or higher
- **Apache Maven**: 3.8+ (for building)
- **MongoDB**: 5.0 or higher
- **Apache Kafka**: 2.8 or higher
- **Docker**: 20.10+ (for containerized deployment)
- **Kubernetes**: 1.21+ (for orchestration)

### External Services
- **WSO2 API Manager** - OAuth2 credential management and API access control
- **Vault Service** - Key management, document signing, and encryption
- **DigiGov Partner Portal** - Template onboarding and notification delivery
- **DigiLocker API** - Parental KYC verification and document verification

### Development Tools
- **Node.js**: 14.x or higher (for frontend development)
- **npm**: 6.x or higher (frontend package management)
- **Git**: Version control and repository management

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd <repository-root>
```

### 2. Configure Environment

Set up the required environment variables. **Do not commit secrets or internal URLs.** Use environment variables or Kubernetes Secrets. Example for local development:

```bash
# Database Configuration
export MONGODB_URI=mongodb://localhost:27017/?authSource=admin
export MONGODB_DATABASE=cms_db_admin

# Kafka Configuration
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Service URLs (adjust host/port for your setup)
export WSO2_CRED_GENERATOR_APIS_URL=http://localhost:30004/api/v1
export VAULT_APIS_URL=http://localhost:30010
export NOTIFICATION_MODULE_APIS_URL=http://localhost:9005
export AUDIT_MODULE_APIS_URL=http://localhost:9006
export DIGIGOV_BASE_URL=http://localhost:9002

# Security (set these; no defaults in code)
export DIGIGOV_ADMIN_CLIENT_ID=<your_client_id>
export DIGIGOV_ADMIN_CLIENT_SECRET=<your_client_secret>
export JASYPT_ENCRYPTOR_PASSWORD=<your_encryption_password>

# WSO2 Cred Generator (required; no defaults)
export WSO2_BASE_URL=https://<wso2-host>:9443
export REGISTER_TOKEN=<base64-username-password>
export TOKEN_AUTH=<base64-clientid-clientsecret>
```

### 3. Build Backend Services

Each backend service is in `Backend/<service-name>/` with its own `pom.xml`. Build from each directory or use Maven from the repo root:

```bash
# Build all backend services (from repository root)
cd Backend
for dir in */; do (cd "$dir" && ./mvnw -q -DskipTests clean package 2>/dev/null) || true; done

# Or build individual services
cd Backend/patner-portal-apis && ./mvnw clean package -DskipTests
cd Backend/consent-core-apis && ./mvnw clean package -DskipTests
cd Backend/notification-module-apis && ./mvnw clean package -DskipTests
# See Backend/README.md for full list.
```

### 4. Start Services

Start each service from its directory (or use your deployment method). Example JAR names and paths (actual artifact names may vary; check `target/` after build):

```bash
# Example: start from each Backend service directory
java -jar target/*.jar

# Start frontends (from Frontend/<app>)
cd Frontend/partner-portal-frontend && npm install && npm run start:dev
# Or: user-portal-frontend, consent-popup-frontend (see Frontend/README.md)
```

### 5. Verify Installation

```bash
# Backend health (adjust if using different ports)
curl http://localhost:9006/audit/actuator/health          # Audit Module
curl http://localhost:9004/api/v1/swagger-ui.html         # WSO2 Credentials Generator
curl http://localhost:9009/actuator/health               # Auth Service
curl http://localhost:9002/partnerportal/actuator/health  # Partner Portal
curl http://localhost:9001/consent/actuator/health        # Consent Core
curl http://localhost:9005/notification/actuator/health  # Notification Module
curl http://localhost:9020/notification-consumer/actuator/health
curl http://localhost:9007/grievance/actuator/health    # Grievance Service
curl http://localhost:9010/actuator/health                # Vault (if enabled)
curl http://localhost:9011/registry/actuator/health      # System Registry

# Swagger UIs (examples)
# Partner Portal: http://localhost:9002/partnerportal/swagger-ui.html
# Consent Core:   http://localhost:9001/consent/swagger-ui.html
# Notification:   http://localhost:9005/notification/swagger-ui.html
# WSO2 Cred:      http://localhost:9004/api/v1/swagger-ui.html
# Auth:           http://localhost:9009/swagger-ui.html
```

## Configuration

**Important:** This repository does not contain hardcoded secrets, passwords, or internal IPs. Set all sensitive values (e.g. `MONGODB_URI`, `REGISTER_TOKEN`, `TOKEN_AUTH`, `WSO2_BASE_URL`, `ROOTKEY`, DigiGov client credentials) via environment variables or Kubernetes Secrets. See each service's README and deployment YAMLs for placeholders and required variables.

### Environment Variables Summary

| Service | Port | Key Environment Variables |
|---------|------|---------------------------|
| Partner Portal | 9002 | `MONGODB_URI`, `WSO2_CRED_GENERATOR_APIS_URL`, `VAULT_APIS_URL`, `AUTH_APIS_URL` |
| Consent Core | 9001 | `MONGODB_URI`, `NOTIFICATION_MODULE_APIS_URL`, `AUDIT_MODULE_APIS_URL`, `VAULT_APIS_URL` |
| Notification Module | 9005 | `MONGODB_URI`, `KAFKA_BOOTSTRAP_SERVERS`, `DIGIGOV_*`, `EMAIL_FROM` |
| Notification Consumer | 9020 | `MONGODB_URI`, `KAFKA_BOOTSTRAP_SERVERS`, `SYSTEM_REGISTRY_APIS_URL` |
| Grievance Service | 9007 | `MONGODB_URI`, `NOTIFICATION_MODULE_APIS_URL`, `AUDIT_MODULE_APIS_URL`, `VAULT_APIS_URL` |
| Audit Module | 9006 | `MONGODB_URI`, `KAFKA_BOOTSTRAP_SERVERS`, `VAULT_APIS_URL`, `NOTIFICATION_MODULE_APIS_URL` |
| Scheduler | 9001 | `MONGODB_URI`, `NOTIFICATION_MODULE_APIS_URL`, `AUDIT_MODULE_APIS_URL`, `VAULT_APIS_URL`, `SYSTEM_REGISTRY_APIS_URL` |
| WSO2 Credentials Generator | 9004 | `MONGODB_URI`, `WSO2_BASE_URL`, `REGISTER_TOKEN`, `TOKEN_AUTH` (all required) |
| Auth Service | 9009 | `MONGODB_URI`, `WSO2_BASE_URL`, `AUDIT_MODULE_APIS_URL`, `APP_SIGN`, `APP_VERIFY` |
| Cookie Consent | 9008 | `MONGODB_URI`, `AUDIT_MODULE_APIS_URL`, `VAULT_APIS_URL`, `AUTH_APIS_URL` |
| Vault | 9010 | `MONGODB_URI`, `EXT_VAULT_URL`, `ROOTKEY` (Vault token) |
| System Registry | 9011 | `MONGODB_URI`, `NOTIFICATION_MODULE_APIS_URL`, `AUDIT_MODULE_APIS_URL` |
| Translator | configurable | `MONGODB_URI`, `PROXY_HOST` (if behind proxy) |
| Frontends | various | `REACT_APP_*` API URLs; see [Frontend/README.md](Frontend/README.md) and each app's README |

### Multi-Tenant Configuration

All services implement database-per-tenant isolation:
- Each tenant has a dedicated MongoDB database: `tenant_db_{tenantId}`
- Shared system configuration in `cms_db_admin` database
- Tenant identification via `X-Tenant-Id` header in all API requests
- Automatic database routing based on tenant context

## API Documentation

### Service Endpoints

| Service | Base URL | Swagger UI |
|---------|----------|------------|
| Partner Portal | `http://localhost:9002/partnerportal` | `/swagger-ui.html` |
| Consent Core | `http://localhost:9001/consent` | `/swagger-ui.html` |
| Notification Module | `http://localhost:9005/notification` | `/swagger-ui.html` |
| Grievance Service | `http://localhost:9007/grievance` | `/swagger-ui/index.html` |
| Audit Module | `http://localhost:9006/audit` | `/swagger-ui.html` or `/swagger-ui/index.html` |
| Scheduler | `http://localhost:9001/schedular` | `/swagger-ui/index.html` |
| WSO2 Credentials Generator | `http://localhost:9004/api/v1` | `/swagger-ui.html` |
| Auth Service | `http://localhost:9009` | `/swagger-ui.html` |
| Cookie Consent | `http://localhost:9008` | See service README |
| Vault | `http://localhost:9010` | See service README |
| System Registry | `http://localhost:9011/registry` | `/swagger-ui.html` |
| Translator | `http://localhost:<port>/v1` | See [Backend/translator-apis](Backend/translator-apis/README.md) |
| Frontends | Per-app (see [Frontend/README.md](Frontend/README.md)) | N/A (React apps) |

### Common Headers

All API requests require the following headers:

```
X-Tenant-Id: <tenant_identifier>       # Required for tenant routing
X-Business-Id: <business_identifier>   # Required for business context  
X-Transaction-Id: <uuid>                # Optional for request tracking
Content-Type: application/json          # For POST/PUT requests
```

### Key API Operations

#### Tenant Onboarding
```bash
POST /partnerportal/v1.0/tenant/onboard
```

#### Consent Management
```bash
POST /consent/v1.0/consent-handle/create    # Create consent handle
POST /consent/v1.0/consent/create           # Create consent
POST /consent/v1.0/consent/validate-token   # Validate consent
```

#### Notification Triggering
```bash
POST /notification/v1/events/trigger        # Trigger notification event
```

#### Grievance Creation
```bash
POST /grievance/api/v1/grievances          # Create grievance
```

#### WSO2 Credential Management
```bash
POST /api/v1/registerTenant               # Register tenant with WSO2
POST /api/v1/onboardBusiness               # Onboard business application
POST /api/v1/onboardDataProcessor          # Onboard data processor
```

#### Authentication & Authorization
```bash
POST /oauth2/token                         # Generate JWT token
POST /oauth2/validate                      # Validate JWT token
POST /oauth2/introspect                    # Introspect token (OAuth2 compliant)
POST /oauth2/revoke                        # Revoke token
POST /secureCode/create                    # Create secure code session
POST /secureCode/validate                  # Validate secure session
GET  /api/token                            # Get WSO2 access token
```

## Development

### Project Structure

```
<repository-root>/
├── Backend/                          # Backend microservices
│   ├── auth-apis/                    # Auth Service (9009)
│   ├── audit-module-apis/            # Audit Module (9006)
│   ├── consent-core-apis/            # Consent Core (9001)
│   ├── cookie-consent-apis/          # Cookie Consent (9008)
│   ├── grivance-module-apis/         # Grievance Service (9007)
│   ├── notification-module-apis/     # Notification Module (9005)
│   ├── notification-consumer-apis/   # Notification Consumer (9020)
│   ├── patner-portal-apis/           # Partner Portal (9002)
│   ├── schedular-apis/               # Scheduler Service (9001)
│   ├── system-registry-apis/         # System Registry (9011)
│   ├── translator-apis/              # Translator Service
│   ├── vault-apis/                   # Vault Service (9010)
│   ├── wso2-cred-generator-apis/     # WSO2 Credentials (9004)
│   └── README.md                     # Backend index
├── Frontend/                         # Frontend applications
│   ├── partner-portal-frontend/      # Partner/DPO portal
│   ├── user-portal-frontend/         # Data principal portal
│   ├── consent-popup-frontend/       # Cookie consent popup
│   └── README.md                     # Frontend index
├── README.md                         # This file
├── CONTRIBUTING.md
├── CODE_OF_CONDUCT.md
└── CHANGELOG.md
```

Each backend service contains `src/`, `pom.xml`, and optionally `deployment/` (Dockerfile, Kubernetes manifests). Each frontend contains `src/`, `package.json`, and env files (`.env.dev`, `.env.nonprod`, `.env.prod`). See [Backend/README.md](Backend/README.md) and [Frontend/README.md](Frontend/README.md) for details.

### Building from Source

#### Backend Services (Java/Maven)

Each service has a Maven wrapper (`mvnw`). Build from the service directory:

```bash
# Build a specific service
cd Backend/patner-portal-apis
./mvnw clean package -DskipTests

# Build with tests
./mvnw clean package

# Run
java -jar target/<artifact>-*.jar
```

Docker images: use each service's `deployment/Dockerfile` (e.g. `docker build -f Backend/patner-portal-apis/deployment/Dockerfile -t partner-portal .` from an appropriate context).

#### Frontend Applications (Node.js/npm)

```bash
# Partner portal
cd Frontend/partner-portal-frontend
npm install
npm run build:dev    # or build:nonprod, build:prod
npm run start:dev

# User portal / Consent popup: same pattern; see Frontend/README.md
```

#### Build All Services

```bash
# Backend: build each service in Backend/
for d in Backend/*/; do (cd "$d" && ./mvnw -q -DskipTests clean package); done

# Frontends: build each app in Frontend/
cd Frontend/partner-portal-frontend && npm ci && npm run build:prod
```

### Code Quality Standards

Where configured per service:

- **Checkstyle** - Java code style validation
- **JaCoCo** - Code coverage reporting
- **SonarQube** - Static code analysis (if configured)
- **SpotBugs** - Bug pattern detection

```bash
# From a Backend service directory, e.g. Backend/consent-core-apis
./mvnw checkstyle:check
./mvnw jacoco:report
./mvnw sonar:sonar   # if Sonar is configured
```

## Testing

### Running Tests

```bash
# Run tests for a specific service (from that service directory)
cd Backend/consent-core-apis
./mvnw test

# Run integration tests
./mvnw test -Dtest=*IntegrationTest

# Generate test reports
./mvnw test jacoco:report
```

Repeat from each `Backend/<service>/` directory as needed. Frontend apps may have their own test scripts (see each app's README).

### Test Categories

- **Unit Tests** - Service and utility class tests
- **Integration Tests** - Full Spring context tests with MongoDB
- **Controller Tests** - REST API endpoint tests
- **End-to-End Tests** - Cross-service integration tests

## Deployment

### Docker Deployment

Each backend service that has a Dockerfile can be built from its directory or with a build context that includes the repo:

```bash
# Example: build Partner Portal image
docker build -f Backend/patner-portal-apis/deployment/Dockerfile -t partner-portal:latest .

# Example: build Consent Core
docker build -f Backend/consent-core-apis/deployment/Dockerfile -t consent-core:latest .
```

There is no single root `docker-compose.yml` in the repository; use your own Compose file or Kubernetes for multi-service runs. Per-service deployment manifests are under `Backend/<service>/deployment/`.

### Kubernetes Deployment

#### Prerequisites
- Kubernetes cluster 1.21+
- kubectl configured
- Persistent volumes for MongoDB and Kafka
- ConfigMaps and Secrets created

#### Deploy to Kubernetes

Kubernetes manifests are per service under `Backend/<service>/deployment/`. Replace placeholder values in ConfigMaps/Secrets (e.g. `<wso2-host>`, `<vault-token>`) with real values or external config before applying.

```bash
# Create namespace
kubectl create namespace dpdp-cms

# Deploy a service (example)
kubectl apply -f Backend/consent-core-apis/deployment/ -n dpdp-cms

# Check status
kubectl get pods -n dpdp-cms
kubectl get services -n dpdp-cms
```

Deploy MongoDB, Kafka, and other infrastructure per your environment; the repo does not include a single top-level `deployment/` folder with all manifests.

#### Environment-Specific Configurations

| Environment | Namespace | Description |
|-------------|-----------|-------------|
| Development | dpdp-cms-dev | Local development with minimal resources |
| SIT | dpdp-cms-sit | System Integration Testing environment |
| UAT | dpdp-cms-uat | User Acceptance Testing environment |
| Production | dpdp-cms | Production environment with full resources |

#### Service-Specific Deployment Considerations

**WSO2 Credentials Generation Service**:
- Requires network access to WSO2 API Manager (port 9443)
- Needs persistent MongoDB storage for credential caching
- Deploy after WSO2 API Manager is fully operational

**Auth Service**:
- Requires RSA key pair setup before deployment
- Needs IP whitelisting configuration for security
- Should be deployed early as other services depend on it
- Configure proper session storage with MongoDB TTL indexes

**Frontend Application**:
- Deploy behind CDN for better performance
- Configure environment-specific API URLs
- Set up proper cache headers for static assets
- Use HTTPS in production environments

#### Container Resource Requirements

| Service | Memory (Mi) | CPU (m) | Replicas |
|---------|-------------|---------|----------|
| Auth Service | 1024 | 500 | 2-3 |
| WSO2 Credentials Generator | 512 | 300 | 1-2 (port 9004) |
| Frontend | 256 | 200 | 2-3 |

## Monitoring

### Health Checks

All services expose Spring Boot Actuator endpoints:

```bash
# Check individual service health
curl http://localhost:9002/partnerportal/actuator/health
curl http://localhost:9001/consent/actuator/health

# Kubernetes readiness/liveness probes are configured
```

### Metrics and Observability

- **Prometheus** - Metrics collection from `/actuator/prometheus`
- **Grafana** - Visualization dashboards
- **ELK Stack** - Centralized logging (Elasticsearch, Logstash, Kibana)
- **Jaeger/Zipkin** - Distributed tracing

### Logging

All services use structured JSON logging:
- **Console**: JSON format for container environments
- **Files**: Tenant-segregated log files where applicable
- **Correlation IDs**: Request tracking across services

## Service-Specific Configuration

### WSO2 Credentials Generation Service

#### Database Prerequisites

The service assumes the database contains API IDs of onboarded NeGD APIs in the `wso2_availabe_apis` collection:

```json
{
    "_id": ObjectId("bda997a3dce443dda2bdee3b"),
    "apiId": "bda997a3-dce4-43dd-a2bd-ee3b4c3fdd7a"
}
```

#### How to Get API IDs

1. Log in to the **WSO2 Publisher Portal**
2. Go to the **APIs** list
3. Click on the desired API
4. Check the **browser URL**: `https://<host>/publisher/apis/<API_ID>/overview`
5. Extract the API_ID from the URL

#### WSO2 Client Credentials Setup

To generate `clientId:clientSecret` pair using admin credentials in WSO2 API Manager:

1. **Log in to Developer Portal**: `https://<host>/devportal`
2. **Create Application**: Applications → Create Application
3. **Generate Keys**: Production Keys → Generate Keys
4. **Configure Environment Variables**:
   - `TOKEN_AUTH`: Base64 encoded `clientId:clientSecret`
   - `REGISTER_TOKEN`: Base64 encoded `username:password`

### Auth Service

#### Database Prerequisites

The Auth Service requires an RSA private key (JWK format) in the `cms_db_admin` database, `auth_key` collection:

```json
{
    "_id": ObjectId("690f38b2ce1641694e5bea5a"),
    "p": "<valueOfP>",
    "kty": "RSA",
    "q": "<valueOfQ>",
    "d": "<valueOfD>",
    "e": "AQAB",
    "use": "sig",
    "qi": "<valueOfQI>",
    "dp": "<valueOfDQ>",
    "alg": "RS256",
    "dq": "<valueOfDQ>",
    "n": "<valueOfN>"
}
```

#### Key Management

- Store RSA private key in `src/main/resources/private-key.json`
- Store public certificate in `src/main/resources/public-cert.json`
- Configure IP whitelisting in `whitelisted-ips.json`

### Frontend Applications

There are three frontend applications: **Partner Portal**, **User Portal**, and **Consent Popup**. See [Frontend/README.md](Frontend/README.md) and each app's README for details.

#### Environment Configuration

Each frontend uses environment-specific files:

- `.env.dev` — Development
- `.env.nonprod` — Non-production
- `.env.prod` — Production

Do not commit real API URLs or secrets; use placeholders in templates and set values per environment.

#### Key Environment Variables (examples)

```bash
# API base URLs (set per environment)
REACT_APP_API_URL=<partner-portal-api-base>
REACT_APP_CONSENT_URL=<consent-api-base>
REACT_APP_GRIEVANCE_URL=<grievance-api-base>
REACT_APP_AUDIT_URL=<audit-api-base>
REACT_APP_TRANSLATOR_URL=<translator-api-base>
# See Frontend/partner-portal-frontend/ENV_SETUP.md and each app's README
```

#### Development Scripts

```bash
npm run start:dev       # Development
npm run start:nonprod   # Non-production
npm run start:prod      # Production
npm run build:dev       # Build for development
npm run build:nonprod   # Build for non-production
npm run build:prod      # Build for production
```

## Security & Compliance

### Security Features

- **JWT Authentication** - Token-based authentication across services
- **JWS Signatures** - Request/response signing for non-repudiation
- **Tenant Isolation** - Complete data isolation between tenants
- **Vault Integration** - Secure key management and encryption
- **Audit Logging** - Comprehensive audit trails for compliance
- **Input Validation** - Jakarta Validation for request validation
- **CORS Configuration** - Cross-origin resource sharing controls

### DPDP Act Compliance

- **Consent Lifecycle Management** - Complete consent management per DPDP Act
- **Data Processor Registration** - Third-party data processor onboarding
- **Grievance Redressal** - Systematic grievance handling and resolution
- **Audit Trails** - Immutable audit records for all operations
- **Data Retention** - Automated data retention and cleanup policies
- **Parental Consent** - Special handling for users below 18 years

### Security Best Practices

1. Use HTTPS in production environments
2. Store sensitive configuration in Kubernetes secrets
3. Enable JWS signature validation for production
4. Regularly rotate JWT signing keys and certificates
5. Monitor audit logs for suspicious activities
6. Implement rate limiting at API gateway level
7. Use network policies in Kubernetes for service isolation

## Contributing

This is a government Digital Public Good developed for the Code for Consent Challenge, organized by NeGD (a part of MeitY). Contributions should follow the project's contribution guidelines.

### Development Guidelines

1. **Follow Java coding standards** - Use consistent formatting and naming conventions
2. **Write comprehensive tests** - Unit tests, integration tests, and documentation
3. **Update API documentation** - Keep OpenAPI specifications current
4. **Ensure multi-tenancy** - All new features must support tenant isolation
5. **Add audit logging** - Log all significant operations for compliance
6. **Update documentation** - Keep README and service docs updated
7. **Security first** - Consider security implications in all changes

### Code Review Process

1. Create feature branch from `main`
2. Implement changes with tests
3. Ensure all quality gates pass
4. Submit pull request with detailed description
5. Address review feedback
6. Merge after approval and CI/CD success

## Support

### Contact Information

- **Support**: See project maintainers or the contact details in [CONTRIBUTING.md](CONTRIBUTING.md).
- **Documentation**: Each service has a README in [Backend/](Backend/README.md) and [Frontend/](Frontend/README.md). Use the repository's **Issues** and **Discussions** (if enabled) for bug reports and feature requests.

### Troubleshooting

#### Common Issues

1. **Service Discovery Issues**
   - Verify service URLs and network connectivity
   - Check Kubernetes service DNS resolution

2. **Database Connection Errors**
   - Verify MongoDB URI and credentials
   - Check network policies and firewall rules

3. **Kafka Integration Issues**
   - Verify Kafka broker connectivity
   - Check SASL/Kerberos configuration for production

4. **Tenant Routing Problems**
   - Ensure `X-Tenant-Id` header is present and valid
   - Verify tenant registration in system

5. **WSO2 Integration Issues**
   - **Error**: `WSO2 API Manager connection failed`
   - **Solution**: 
     - Verify WSO2_BASE_URL is accessible
     - Check TOKEN_AUTH and REGISTER_TOKEN are correctly base64 encoded
     - Ensure WSO2 API Manager is running and accessible
     - Verify client credentials are valid in WSO2 Developer Portal

6. **Auth Service Token Issues**
   - **Error**: `Invalid token signature`
   - **Solution**:
     - Verify RSA private key is correctly configured in database
     - Check private-key.json and public-cert.json files exist
     - Ensure key pair matches and is in correct JWK format
     - Verify token has not expired

7. **Frontend Application Issues**
   - **Error**: `API connection failed`
   - **Solution**:
     - Check backend services are running on configured ports
     - Verify REACT_APP_API_BASE_URL points to correct backend
     - Ensure CORS is configured correctly on backend services
     - Check browser network tab for specific API errors

8. **Missing API IDs in Database**
   - **Error**: `WSO2 API subscription failed`
   - **Solution**:
     - Add required API IDs to `wso2_availabe_apis` collection
     - Verify API IDs exist in WSO2 Publisher Portal
     - Check API is published and available for subscription

### Performance Optimization

- **Database Indexing** - Ensure proper indexes on frequently queried fields
- **Connection Pooling** - Configure appropriate connection pool sizes
- **Caching** - Utilize Redis or in-memory caching for frequently accessed data
- **Load Balancing** - Distribute traffic across multiple service instances

#### Service-Specific Optimizations

**WSO2 Credentials Generation Service**:
- Cache WSO2 access tokens to reduce API calls
- Use connection pooling for WSO2 API Manager requests
- Implement retry mechanisms with exponential backoff

**Auth Service**:
- Configure token cache size (`token.cache.maximum-size=10001`)
- Set appropriate token expiry times (`token.expiry=3600000`)
- Use BCrypt with appropriate rounds for password hashing
- Implement session limits (`token.max.sessions=3`)

**Frontend Application**:
- Enable code splitting for faster initial load times
- Implement lazy loading for heavy components
- Use React.memo and useMemo for expensive computations
- Optimize bundle size with Parcel tree shaking
- Configure appropriate cache headers for static assets

## Related Documentation

- [DPDP Act, 2023](https://www.meity.gov.in/data-protection-framework)
- [Backend services index](Backend/README.md) — list of microservices, ports, and links to each service README
- [Frontend applications index](Frontend/README.md) — list of frontend apps and links to each README
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution and development guidelines
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — community standards
- [CHANGELOG.md](CHANGELOG.md) — release history

---

## License

This project was developed for the **Code for Consent Challenge**, organised by NeGD (National e-Governance Division, Ministry of Electronics and Information Technology, Government of India). See the [LICENSE](LICENSE.md) file in the repository for full terms and conditions.

