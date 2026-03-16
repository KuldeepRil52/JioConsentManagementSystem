# DPDP Notification Module

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.7-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-LGPL%203.0-blue.svg)](LICENSE)
[![DPDP Act 2023](https://img.shields.io/badge/DPDP%20Act-2023%20Compliant-success.svg)](https://www.meity.gov.in/writereaddata/files/Digital%20Personal%20Data%20Protection%20Act%202023.pdf)

> REST API gateway for notification event triggering and template management in a multi-tenant DPDP compliance system

The DPDP Notification Module is a critical component of the Digital Personal Data Protection (DPDP) Consent Management System, designed to ensure compliance with the DPDP Act, 2023 of India. This microservice serves as the primary interface for triggering notification events, managing templates, and orchestrating multi-channel notifications (SMS, Email, Webhooks) across a multi-tenant architecture.

**Documentation:** https://docs.jio.com/dpdp-cms/
**Repository:** https://dev.azure.com/JPL-Limited/dpdp-notification-module

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

- **Event Triggering** - Trigger notification events with multi-channel delivery (SMS/Email/Callback) via REST API
- **Template Management** - Create, onboard, and manage notification templates with DigiGov integration and approval workflow
- **Event Configuration** - Configure business-specific notification rules and recipient types (Data Principal, Data Fiduciary, Data Processor)
- **Master List Management** - Dynamic data resolution with configurable master lists for template argument substitution
- **OTP Services** - OTP generation, verification, and lifecycle management with encryption and rate limiting
- **Multi-Tenant Support** - Complete tenant isolation with dedicated MongoDB databases (tenant_db_{tenantId})
- **Kafka Integration** - Publishes events to Kafka topics for asynchronous processing by notification consumers
- **DigiGov Integration** - Complete template onboarding, approval, and token management workflow with DigiGov Partner Portal
- **Missed Notification Tracking** - Track and retrieve missed notifications with JWS signature verification
- **Data Processor Management** - Manage third-party data processors and their notification preferences

### Technical Features

- **Master Label Management** - Hierarchical label system for dynamic template argument resolution from MongoDB
- **Rate Limiting** - OTP and event rate limiting to prevent abuse (configurable per event type)
- **Token Caching** - Caffeine-based in-memory caching for DigiGov OAuth2 tokens (55-minute TTL)
- **Audit Trail** - Comprehensive audit logging via integration with audit microservice
- **Request/Response Signing** - JWS signatures for API security and non-repudiation
- **Multi-Language Support** - Template support for multiple languages with dynamic argument substitution
- **Retry Mechanisms** - Configurable retry policies with exponential backoff for external API calls
- **DLT Compliance** - TRAI DLT entity and template ID validation for SMS notifications
- **Health Checks** - Spring Boot Actuator endpoints for health monitoring and Prometheus metrics

## Architecture

### High-Level Architecture

```
┌─────────────────┐      ┌──────────────────────┐      ┌─────────────────────┐
│  Client Apps    │─────▶│  Notification Module │─────▶│  Kafka Topics       │
│  (REST API)     │      │  (Port 9003)         │      │  - SMS              │
└─────────────────┘      │                      │      │  - Email            │
                         │  - Event Triggering  │      │  - Callback         │
                         │  - Template Mgmt     │      └─────────────────────┘
                         │  - OTP Services      │               │
                         │  - Master Lists      │               ▼
                         └──────────────────────┘      ┌─────────────────────┐
                                   │                   │  Notification       │
                                   │                   │  Consumer           │
                                   ▼                   │  (Delivery Engine)  │
┌──────────────────────────────────────────────┐      └─────────────────────┘
│  MongoDB (Multi-Tenant)                      │
│  - tenant_db_{tenantId} (per tenant)         │
│  - cms_db_admin (shared configuration)       │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│  External Integrations                       │
│  - DigiGov Partner Portal (Template Mgmt)    │
│  - Audit Module (Audit Trail)                │
└──────────────────────────────────────────────┘
```

### Technology Stack

- **Framework:** Spring Boot 3.5.7
- **Language:** Java 21
- **Database:** MongoDB 5.0+ (multi-tenant architecture)
- **Messaging:** Apache Kafka 2.8+ with SASL/Kerberos
- **Caching:** Caffeine (in-memory)
- **API Documentation:** OpenAPI 3.0 / Swagger UI
- **Container Runtime:** Docker with eclipse-temurin:21-jre
- **Orchestration:** Kubernetes
- **Monitoring:** Micrometer + Prometheus
- **Logging:** Logback with JSON encoding (Logstash format)

### Multi-Tenant Design

The service implements database-per-tenant isolation:
- Each tenant has a dedicated MongoDB database: `tenant_db_{tenantId}`
- Shared system configuration in `cms_db_admin` database
- Tenant identification via `X-Tenant-Id` header in all API requests
- Automatic database routing based on tenant context

### Kafka Topics (Published)

The module publishes notification events to the following Kafka topics:
- `DEV_CMS_NOTIFICATION_SMS` - SMS notification events
- `DEV_CMS_NOTIFICATION_EMAIL` - Email notification events
- `DEV_CMS_NOTIFICATION_CALLBACK` - Callback/webhook notification events

## Prerequisites

Before running the DPDP Notification Module, ensure you have:

### Required Software

- **Java Development Kit (JDK):** 21 or higher
- **Apache Maven:** 3.8+ (for building)
- **MongoDB:** 5.0 or higher
- **Apache Kafka:** 2.8 or higher with Kerberos (SASL_PLAINTEXT)
- **Docker:** 20.10+ (for containerized deployment)
- **Kubernetes:** 1.21+ (for orchestration)

### Required Services

- **MongoDB Server** - For tenant-specific and shared databases
- **Kafka Cluster** - For asynchronous message processing
- **DigiGov Partner Portal** - For template onboarding and notification delivery
- **Audit Module** - For audit trail logging (optional but recommended)

### Network Requirements

- Access to Kafka brokers (17 production servers or local Kafka)
- MongoDB connection (localhost:27017 or cluster)
- DigiGov Partner Portal API endpoint
- Kerberos KDC for Kafka authentication (production)

## Quick Start

### 1. Clone the Repository

```bash
git clone https://dev.azure.com/JPL-Limited/dpdp-notification-module
cd dpdp-notification-module
```

### 2. Configure Environment

Copy and edit the configuration file:

```bash
# Edit configuration for your environment
vi src/main/resources/application-local.yml
```

Set required environment variables (see [Configuration](#configuration) section for details):

```bash
export MONGODB_URI=mongodb://localhost:27017/
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export DIGIGOV_ADMIN_CLIENT_ID=your_client_id
export DIGIGOV_ADMIN_CLIENT_SECRET=your_client_secret
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
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Run the JAR file
java -jar target/notification-module-1.0.0.jar
```

### 5. Verify the Application

```bash
# Check health
curl http://localhost:9003/notification/actuator/health

# Access Swagger UI
open http://localhost:9003/notification/swagger-ui.html
```

## Configuration

### Core Environment Variables

#### Server Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SERVER_PORT` | HTTP server port | `9003` | No |
| `SERVER_SERVLET_CONTEXT_PATH` | Application context path | `/notification` | No |

#### MongoDB Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/` | Yes |
| `MONGODB_DATABASE` | Tenant database pattern | `tenant_db_{tenantId}` | Yes |
| `MONGODB_DEFAULT_DATABASE` | Shared database name | `cms_db_admin` | Yes |
| `MONGODB_AUTO_INDEX_CREATION` | Auto-create indexes | `true` | No |

#### Kafka Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses | `<kafka-host>:9092` (comma-separated for multiple) | Yes |
| `SMS_TOPIC` | SMS notification topic | `DEV_CMS_NOTIFICATION_SMS` | Yes |
| `EMAIL_TOPIC` | Email notification topic | `DEV_CMS_NOTIFICATION_EMAIL` | Yes |
| `CALLBACK_TOPIC` | Callback notification topic | `DEV_CMS_NOTIFICATION_CALLBACK` | Yes |
| `KAFKA_SECURITY_PROTOCOL` | Security protocol | `SASL_PLAINTEXT` | Yes (prod) |
| `KAFKA_SASL_MECHANISM` | SASL mechanism | `GSSAPI` (Kerberos) | Yes (prod) |
| `KRB5_CONFIG` | Kerberos config file | `/etc/krb5.conf` | Yes (prod) |
| `KEYTAB_CONFIG` | Keytab file path | `/path/to/keytab` | Yes (prod) |

#### DigiGov Integration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DIGIGOV_BASE_URL` | DigiGov Partner Portal base URL | `http://patner-portal-apis:9002/partnerportal` | Yes |
| `DIGIGOV_ADMIN_CLIENT_ID` | DigiGov OAuth2 client ID | (your client ID) | Yes |
| `DIGIGOV_ADMIN_CLIENT_SECRET` | DigiGov OAuth2 client secret | (secure value) | Yes |
| `DIGIGOV_SMS_API_URL` | SMS delivery endpoint | `${digigov.base-url}/sms/send` | Yes |
| `DIGIGOV_EMAIL_API_URL` | Email delivery endpoint | `${digigov.base-url}/email/send` | Yes |
| `DIGIGOV_SMS_API_TIMEOUT` | SMS API timeout (ms) | `60000` | No |
| `DIGIGOV_EMAIL_API_TIMEOUT` | Email API timeout (ms) | `90000` | No |

#### Audit Module Integration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AUDIT_MODULE_APIS_URL` | Audit module base URL | `http://audit-module-apis:9006/audit` | Yes |
| `AUDIT_ENABLED` | Enable audit logging | `true` | No |
| `AUDIT_CONNECTION_TIMEOUT` | Connection timeout (ms) | `5000` | No |
| `AUDIT_READ_TIMEOUT` | Read timeout (ms) | `10000` | No |

#### Cache Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `CACHE_ENABLED` | Enable caching | `true` | No |
| `CACHE_TYPE` | Cache provider | `caffeine` | No |
| `CACHE_CAFFEINE_TOKENS_MAX_SIZE` | Token cache max size | `5000` | No |
| `CACHE_CAFFEINE_TOKENS_EXPIRE` | Token cache expiration | `55m` | No |

#### OTP Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `OTP_DEFAULT_EXPIRY_MINUTES` | OTP expiry time | `5` | No |
| `OTP_DEFAULT_MAX_ATTEMPTS` | Max verification attempts | `3` | No |
| `OTP_DEFAULT_LENGTH` | OTP length | `6` | No |
| `OTP_ENCRYPTION_PUBLIC_KEY` | RSA public key (Base64) | (see config) | Yes |
| `OTP_ENCRYPTION_PRIVATE_KEY` | RSA private key (Base64) | (see config) | Yes |

#### Rate Limiting

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `RATE_LIMIT_OTP_MAX_REQUESTS` | Max OTP requests per window | `5` | No |
| `RATE_LIMIT_OTP_WINDOW_MINUTES` | Rate limit window | `10` | No |
| `RATE_LIMIT_OTP_ENABLED_EVENT_TYPES` | Event types to rate limit | `INIT_OTP` | No |

#### JWT Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JWT_CALLBACK_SECRET` | JWT signing secret (256-bit) | (secure value) | Yes |
| `JWT_CALLBACK_EXPIRATION` | JWT expiration (seconds) | `3600` | No |
| `JWT_CALLBACK_ISSUER` | JWT issuer | `dpdp-notification-service` | No |

#### Security Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `APP_SIGN_RESPONSE` | Enable response signing (JWS) | `true` | No |
| `APP_VERIFY_REQUEST` | Enable request verification (JWS) | `true` | No |
| `AUTHORIZATION_ENABLED` | Enable authorization checks | `true` | No |

#### Email System Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `SYSTEM_EMAIL_FROM` | System email sender | `Jio.ConsentSupport@ril.com` | No |
| `SYSTEM_EMAIL_REPLYTO` | System email reply-to | `support-consent@ril.com` | No |
| `SYSTEM_EMAIL_FROM_NAME` | System email sender name | `Jio Consent Management System` | No |
| `SYSTEM_SMS_FROM` | System SMS sender ID | `JIOCNF-S` | No |

### Configuration Files

- **application.yml** - Base configuration
- **application-local.yml** - Local development overrides
- **application-sit.yml** - SIT environment (Kubernetes ConfigMap)
- **application-st.yml** - ST environment (Kubernetes ConfigMap)
- **logback-simple.xml** - Logging configuration

## API Documentation

### REST API Endpoints

The DPDP Notification Module exposes 65+ REST API endpoints organized by functional domain:

#### Base URL

```
http://localhost:9003/notification
```

### 1. Event Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/events/trigger` | Trigger a notification event with multi-channel delivery |
| `GET` | `/v1/events` | Get all events with pagination and filtering |
| `GET` | `/v1/events/{eventId}` | Get event details by ID |
| `GET` | `/v1/events/count` | Get event statistics and counts |

**Example Request:**
```bash
curl -X POST http://localhost:9003/notification/v1/events/trigger \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant_001" \
  -H "X-Business-Id: business_001" \
  -d '{
    "eventType": "CONSENT_REQUEST",
    "recipientType": "DATA_PRINCIPAL",
    "recipients": [{"email": "user@example.com", "mobile": "+919876543210"}],
    "arguments": {"consentId": "CNS12345", "dpName": "Acme Corp"}
  }'
```

### 2. Template Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/v1/templates` | Create unified template (SMS + Email) |
| `GET` | `/v1/templates` | Get all templates with pagination |
| `GET` | `/v1/templates/{templateId}` | Get template by ID |
| `GET` | `/v1/templates/digigov/{id}` | Get template by DigiGov ID |
| `DELETE` | `/v1/templates/digigov/{id}` | Delete template by DigiGov ID |
| `GET` | `/v1/templates/count` | Get template counts by status |

### 3. Event Configuration APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/event-configurations` | Get all event configurations |
| `POST` | `/v1/event-configurations` | Create new event configuration |
| `GET` | `/v1/event-configurations/{configId}` | Get configuration by ID |
| `PUT` | `/v1/event-configurations/{configId}` | Update event configuration |
| `DELETE` | `/v1/event-configurations/{configId}` | Delete event configuration |
| `GET` | `/v1/event-configurations/event-type/{type}` | Get configurations by event type |
| `GET` | `/v1/event-configurations/count` | Get configuration counts |

### 4. Master List Management APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/master-lists/events` | Get all event types |
| `GET` | `/v1/master-lists/events/{eventType}` | Get labels for event type |
| `POST` | `/v1/master-lists/events/{eventType}/mappings` | Add event-label mappings |
| `PUT` | `/v1/master-lists/events/{eventType}/mappings` | Update event-label mappings |
| `DELETE` | `/v1/master-lists/events/{eventType}/mappings` | Remove event-label mappings |
| `POST` | `/v1/master-lists/labels` | Add master label |
| `GET` | `/v1/master-lists/labels/{labelName}` | Get master label details |
| `PUT` | `/v1/master-lists/labels/{labelName}` | Update master label |
| `DELETE` | `/v1/master-lists/labels/{labelName}` | Delete master label |
| `GET` | `/v1/master-lists/configs` | Get all master list configs |
| `POST` | `/v1/master-lists/configs` | Create master list config |
| `GET` | `/v1/master-lists/configs/{configId}` | Get config by ID |
| `PUT` | `/v1/master-lists/configs/{configId}` | Update master list config |
| `DELETE` | `/v1/master-lists/configs/{configId}` | Delete master list config |
| `POST` | `/v1/master-lists/configs/{configId}/activate` | Activate master list config |

### 5. Data Processor APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/data-processors` | Get all data processors |
| `POST` | `/v1/data-processors` | Create data processor |
| `GET` | `/v1/data-processors/{dataProcessorId}` | Get processor by ID |
| `PUT` | `/v1/data-processors/{dataProcessorId}` | Update data processor |
| `DELETE` | `/v1/data-processors/{dataProcessorId}` | Delete data processor |
| `GET` | `/v1/data-processors/count` | Get processor counts |

### 6. Missed Notification APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/v1/missed-notifications/count` | Get missed notification count |
| `GET` | `/v1/missed-notifications/list` | Get missed notification IDs |
| `GET` | `/v1/missed-notifications/{notificationId}` | Get missed notification by ID |
| `GET` | `/v1/missed-notifications/all` | Get all missed notifications |

### 7. OTP APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/otp/init` | Initialize OTP (legacy endpoint) |
| `POST` | `/otp/verify` | Verify OTP (legacy endpoint) |

### 8. Health & Monitoring APIs

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Application health check |
| `GET` | `/actuator/prometheus` | Prometheus metrics |
| `GET` | `/actuator/info` | Application information |

### Authentication & Headers

All API requests require the following headers:

```
X-Tenant-Id: <tenant_identifier>     # Required for tenant routing
X-Business-Id: <business_identifier>  # Required for business context
X-Transaction-Id: <uuid>              # Optional for request tracking
Content-Type: application/json        # For POST/PUT requests
```

For missed notification APIs with signature verification, additional headers may be required:

```
X-Signature: <jws_signature>          # JWS signature of request body
```

### OpenAPI / Swagger UI

Access the interactive API documentation:

- **Swagger UI:** http://localhost:9003/notification/swagger-ui.html
- **OpenAPI JSON:** http://localhost:9003/notification/v3/api-docs
- **OpenAPI YAML:** http://localhost:9003/notification/v3/api-docs.yaml

## Development

### Project Structure

```
dpdp-notification-module/
├── src/
│   ├── main/
│   │   ├── java/com/jio/digigov/notification/
│   │   │   ├── bootstrap/              # Application initialization
│   │   │   ├── config/                 # Spring configurations
│   │   │   │   ├── kafka/              # Kafka producer configs
│   │   │   │   └── properties/         # Configuration properties
│   │   │   ├── constant/               # Application constants
│   │   │   ├── controller/             # REST API controllers (23 controllers)
│   │   │   │   ├── v1/                 # V1 API endpoints
│   │   │   │   └── otp/                # OTP controllers
│   │   │   ├── dto/                    # Data Transfer Objects
│   │   │   │   ├── audit/
│   │   │   │   ├── cache/
│   │   │   │   ├── kafka/
│   │   │   │   ├── masterlist/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/                 # MongoDB entities
│   │   │   │   ├── event/
│   │   │   │   ├── notification/
│   │   │   │   ├── otp/
│   │   │   │   └── template/
│   │   │   ├── enums/                  # Enumerations
│   │   │   ├── exception/              # Custom exceptions
│   │   │   ├── mapper/                 # DTO-Entity mappers
│   │   │   ├── repository/             # MongoDB repositories
│   │   │   ├── service/                # Business logic services
│   │   │   └── util/                   # Utility classes
│   │   └── resources/
│   │       ├── application.yml         # Base configuration
│   │       ├── application-local.yml   # Local overrides
│   │       └── logback-simple.xml      # Logging configuration
│   └── test/
│       └── java/                       # Unit and integration tests
├── deployment/
│   ├── Dockerfile                      # Container image definition
│   ├── jio-dl-deployment-sit.yaml      # Kubernetes - SIT
│   ├── jio-dl-deployment-st.yaml       # Kubernetes - ST
│   └── Public_deployment.yaml          # Public cloud deployment
├── templates/                          # Documentation templates
├── pom.xml                             # Maven build configuration
├── checkstyle.xml                      # Code style rules
└── README.md                           # This file
```

### Building from Source

```bash
# Clean build
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build with code coverage
mvn clean test jacoco:report

# Build Docker image
docker build -t notification-module:1.0.0 -f deployment/Dockerfile .
```

### Code Quality

The project enforces code quality through:

- **Checkstyle** - Java code style validation (checkstyle.xml)
- **JaCoCo** - 80% minimum code coverage requirement
- **SonarQube** - Static code analysis
- **Maven Surefire** - Test execution

```bash
# Run checkstyle
mvn checkstyle:check

# Generate coverage report
mvn jacoco:report
# Report: target/site/jacoco/index.html

# Run SonarQube analysis
mvn sonar:sonar -Dsonar.host.url=http://sonarqube:9000
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

For local development without Kafka and MongoDB:

```bash
# Use local profile with embedded/mock services
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Or set environment variable
export SPRING_PROFILES_ACTIVE=local
mvn spring-boot:run
```

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=EventTriggerServiceTest

# Run tests matching pattern
mvn test -Dtest=*ControllerTest

# Run integration tests
mvn test -Dtest=*IntegrationTest

# Run with code coverage
mvn test jacoco:report
```

### Test Coverage

The project requires **80% minimum code coverage** (enforced by JaCoCo):

```bash
# Generate coverage report
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html
```

### Test Structure

Tests are organized by type:
- **Unit Tests** - Service and utility class tests (Mockito)
- **Integration Tests** - Full Spring context tests with MongoDB
- **Controller Tests** - REST API endpoint tests (MockMvc)
- **Kafka Tests** - Kafka producer tests (Spring Kafka Test)

### Test Configuration

Test-specific configuration is in `src/test/resources/application-test.yml`:
- Embedded MongoDB (Flapdoodle)
- In-memory Kafka (Spring Kafka Test)
- Disabled external integrations

## Deployment

### Docker Deployment

#### Build Docker Image

```bash
# Build image
docker build -t notification-module:1.0.0 -f deployment/Dockerfile .

# Run container
docker run -d \
  --name notification-module \
  -p 9003:9003 \
  -e MONGODB_URI=mongodb://host:27017/ \
  -e KAFKA_BOOTSTRAP_SERVERS=kafka:9092 \
  -e DIGIGOV_ADMIN_CLIENT_ID=your_client_id \
  -e DIGIGOV_ADMIN_CLIENT_SECRET=your_secret \
  notification-module:1.0.0
```

#### Docker Image Details

- **Base Image:** `<docker-registry-host>/docker-virtual-all/eclipse-temurin:21-jre`
- **User:** `si_digigov` (non-root)
- **Working Directory:** `/opt`
- **Exposed Port:** 9003
- **Entry Point:** `java $JAVA_OPTS -jar deployment/notification-module-1.0.0.jar`

### Kubernetes Deployment

#### Prerequisites

- Kubernetes cluster 1.21+
- kubectl configured
- Kerberos keytab for Kafka authentication
- ConfigMaps and Secrets created

#### Deployment Files

- **SIT Environment:** `deployment/jio-dl-deployment-sit.yaml`
- **ST Environment:** `deployment/jio-dl-deployment-st.yaml`
- **Public Cloud:** `deployment/Public_deployment.yaml`

#### Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace dpdp-cms

# Create ConfigMap for application.yml
kubectl create configmap notification-module-config \
  --from-file=application.yml=src/main/resources/application.yml \
  -n dpdp-cms

# Create Secret for sensitive values
kubectl create secret generic notification-module-secret \
  --from-literal=digigov-client-secret=your_secret \
  --from-literal=jwt-secret=your_jwt_secret \
  -n dpdp-cms

# Deploy application
kubectl apply -f deployment/jio-dl-deployment-sit.yaml -n dpdp-cms

# Check deployment status
kubectl get pods -n dpdp-cms
kubectl logs -f deployment/notification-module -n dpdp-cms

# Access service
kubectl port-forward service/notification-module 9003:9003 -n dpdp-cms
```

#### Kubernetes Resources

The deployment includes:
- **Deployment** - Manages pod replicas
- **Service** - Internal ClusterIP service
- **ConfigMap** - Application configuration
- **Secret** - Sensitive credentials
- **Ingress** - External access (if configured)
- **HPA** - Horizontal Pod Autoscaling (optional)

#### Health Probes

```yaml
livenessProbe:
  httpGet:
    path: /notification/actuator/health/liveness
    port: 9003
  initialDelaySeconds: 120
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /notification/actuator/health/readiness
    port: 9003
  initialDelaySeconds: 60
  periodSeconds: 10
```

### Environment-Specific Configuration

| Environment | Namespace | Kafka Brokers | MongoDB | DigiGov URL |
|-------------|-----------|---------------|---------|-------------|
| Local | - | localhost:9092 | localhost:27017 | Mock/Dev |
| SIT | dpdp-cms-sit | jpbsthdpdbs01-17:9092 | sit-mongo-cluster | SIT Portal |
| ST | dpdp-cms-st | jpbsthdpdbs01-17:9092 | st-mongo-cluster | ST Portal |
| Production | dpdp-cms | jpbsthdpdbs01-17:9092 | prod-mongo-cluster | Prod Portal |

## Monitoring

### Health Checks

The service exposes Spring Boot Actuator endpoints for monitoring:

```bash
# Overall health
curl http://localhost:9003/notification/actuator/health

# Detailed health (requires auth)
curl http://localhost:9003/notification/actuator/health?details=true

# Readiness probe
curl http://localhost:9003/notification/actuator/health/readiness

# Liveness probe
curl http://localhost:9003/notification/actuator/health/liveness
```

### Prometheus Metrics

Metrics are exposed for Prometheus scraping:

```bash
# Prometheus metrics endpoint
curl http://localhost:9003/notification/actuator/prometheus
```

**Available Metrics:**
- JVM memory and GC metrics
- HTTP request metrics (count, duration, status codes)
- Kafka producer metrics (records sent, errors, latency)
- MongoDB connection pool metrics
- Custom business metrics (events triggered, templates created)
- Cache hit/miss rates

### Logging

The service uses structured JSON logging (Logstash format) for centralized log aggregation:

**Log Levels:**
- `ERROR` - Critical failures requiring immediate attention
- `WARN` - Warning conditions (retries, degraded performance)
- `INFO` - Important business events (event triggered, template created)
- `DEBUG` - Detailed diagnostic information (disabled in production)

**Log Configuration:**
```bash
# Set log level via environment variable
export LOGGING_LEVEL_COM_JIO_DIGIGOV_NOTIFICATION=DEBUG

# Or in application.yml
logging:
  level:
    com.jio.digigov.notification: DEBUG
```

**Log Output:**
- **Console:** JSON format for container environments
- **File:** `/opt/logs/notification-module.log` (inside container)

### Application Information

```bash
# Application info
curl http://localhost:9003/notification/actuator/info

# Response includes:
# - Application name and version
# - Build timestamp
# - Git commit information
# - Java version
# - Spring Boot version
```

### Monitoring Best Practices

1. **Set up Prometheus + Grafana** - Visualize metrics and create dashboards
2. **Configure Alerts** - Alert on high error rates, latency spikes, or failures
3. **Centralized Logging** - Use ELK stack (Elasticsearch, Logstash, Kibana) or similar
4. **Distributed Tracing** - Integrate with Zipkin or Jaeger for request tracing
5. **APM Tools** - Consider Application Performance Monitoring tools like New Relic or Dynatrace

## Dependencies

### Core Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.7 | Framework foundation |
| Java | 21 | Runtime platform |
| MongoDB | 5.0+ | Database |
| Apache Kafka | 2.8+ | Message broker |

### Spring Boot Starters

- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-actuator` - Health and metrics
- `spring-boot-starter-cache` - Caching abstraction
- `spring-boot-starter-mail` - Email support
- `spring-kafka` - Kafka integration

### Third-Party Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| springdoc-openapi | 2.7.0 | OpenAPI/Swagger documentation |
| lombok | 1.18.30 | Reduce boilerplate code |
| jackson-databind | (Spring managed) | JSON serialization |
| logstash-logback-encoder | 7.4 | JSON logging |
| micrometer-prometheus | (Spring managed) | Prometheus metrics |
| caffeine | (Spring managed) | In-memory cache |
| jasypt | 3.0.5 | Property encryption |
| java-jwt | 4.4.0 | JWT token handling |
| httpclient5 | 5.x | HTTP client |

### Security Dependency Overrides

The project overrides versions for CVE fixes:

| Dependency | Version | CVE Fixed |
|------------|---------|-----------|
| commons-beanutils | 1.11.0 | Multiple CVEs |
| netty-bom | 4.1.128.Final | CVE-2024-xxxxx |
| commons-io | 2.18.0 | CVE-2024-xxxxx |
| tomcat-embed-core | 10.1.49 | CVE-2025-55752 |
| logback-classic | 1.5.19 | CVE-2025-11226 |
| logback-core | 1.5.19 | CVE-2025-11226 |

### Version Compatibility

| Component | Minimum Version | Recommended Version |
|-----------|-----------------|---------------------|
| Java | 21 | 21 LTS |
| Maven | 3.8.0 | 3.9.x |
| MongoDB | 5.0 | 7.0+ |
| Kafka | 2.8 | 3.x |
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

- **Documentation:** https://docs.jio.com/dpdp-cms/
- **Support Email:** Jio.ConsentSupport@ril.com
- **Issue Tracking:** https://dev.azure.com/JPL-Limited/dpdp-notification-module/issues

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

Please report security vulnerabilities to: **Jio.ConsentSupport@ril.com**

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
Copyright (c) 2025 Reliance Jio Infocomm Limited

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published
by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
```

See [LICENSE](LICENSE) for complete terms and additional DPDP Act 2023 compliance requirements.

## Compliance

This software is designed to facilitate compliance with:
- **Digital Personal Data Protection Act, 2023 (DPDP Act)** - India's data protection law
- **TRAI DLT Regulations** - Telecom Regulatory Authority of India's Distributed Ledger Technology requirements for SMS
- **CERT-In Guidelines** - Indian Computer Emergency Response Team security guidelines

## Acknowledgments

This project is part of the **DPDP Consent Management System**, a Digital Public Good initiative designed to ensure compliance with India's DPDP Act, 2023.

**Developed by:** Reliance Jio Infocomm Limited
**For:** Ministry of Electronics and Information Technology (MeitY), Government of India

---

**Version:** 1.0.0
**Last Updated:** 2025-01-21
**Maintained by:** Jio DPDP CMS Team
