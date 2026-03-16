# Cookie Consent Scanner Service

## DPDP Consent Management System

### Overview

The **Cookie Consent Scanner Service** is a core microservice within the Digital Personal Data Protection (DPDP) Consent Management System. It provides comprehensive cookie scanning, categorization, and consent lifecycle management capabilities to ensure compliance with the **DPDP Act, 2023** of India.

This service enables organizations (Data Fiduciaries) to scan websites for cookies, categorize them, create consent templates, manage consent handles, and maintain a full audit trail of user consent decisions — all within a secure, multi-tenant architecture.

> **Part of the DPDP CMS Ecosystem** — For complete system documentation, visit the [DPDP CMS Documentation Portal](https://dpdp-cms-docs.gov.in).

---

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
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)
- [Support](#support)

---

## Features

### Core Capabilities

- **Website Cookie Scanning** — Automated cookie detection using Playwright (Chromium) browser automation with subdomain scanning support
- **AI-Powered Cookie Categorization** — Intelligent cookie classification via external AI/ML API with CSV-based fallback
- **Consent Template Management** — Create, version, and publish consent templates linked to completed scans
- **Consent Handle Lifecycle** — Generate time-limited, secure consent handles (configurable expiry, default 15 minutes)
- **Consent Creation & Versioning** — Full consent lifecycle management with immutable audit trail and version history
- **Dashboard Analytics** — Consent data grouped by template with date range, version, and business ID filtering
- **Cookie Category Management** — CRUD operations for managing cookie categories per tenant

### Technical Features

- **Multi-Tenant Architecture** — Complete tenant isolation with per-tenant MongoDB databases (`X-Tenant-ID` header)
- **Rate Limiting** — Configurable request throttling using Bucket4j (default: (false) / 100 requests/min)
- **Circuit Breaker** — Resilience4j circuit breakers for scan and categorization services
- **JWS Token Signing & Verification** — Consent integrity via Vault-based JWS signatures
- **Payload Encryption** — Consent data encryption through Vault service integration
- **Retry Mechanism** — Spring Retry with configurable exponential backoff
- **Path Traversal Protection** — Multi-layer security filters against path traversal attacks
- **Async Processing** — Asynchronous scan execution with configurable thread pools
- **Graceful Shutdown** — Clean shutdown with 30-second timeout per phase
- **OpenAPI/Swagger Documentation** — Interactive API documentation via SpringDoc

### Scanning Features

- **Subdomain Discovery** — Automatic subdomain detection with configurable patterns
- **Consent Banner Handling** — Automated consent popup detection and interaction
- **iFrame Processing** — Cross-frame cookie detection with configurable timeouts
- **Third-Party Resource Tracking** — Detection of analytics cookies (Google, Facebook, Adobe, Mixpanel, Hotjar)
- **Network Interception** — HTTP header and JavaScript-based cookie monitoring
- **Progressive Navigation** — Fallback strategy with networkidle → DOMContentLoaded → load

---

## Architecture

### Service Context

| Property            | Value                                                   |
|---------------------|---------------------------------------------------------|
| **Service Type**    | REST API / Async Processor                              |
| **Framework**       | Spring Boot 3.4.3                                       |
| **Language**        | Java 17                                                 |
| **Database**        | MongoDB (Multi-Tenant)                                  |
| **Browser Engine**  | Playwright 1.45.0 (Chromium)                            |
| **Default Port**    | `9008`                                                  |
| **NodePort**        | `30008`                                                 |
| **Application Name**| `protected-cookie-scanner-multitenant`                  |

### Integration Points

| Service                      | Purpose                                          | Default URL                        |
|------------------------------|--------------------------------------------------|------------------------------------|
| **Audit Module APIs**        | Audit logging for consent operations             | `http://audit-module-apis:9006`    |
| **Vault APIs**               | JWS signing, verification, payload encryption    | `http://vault-apis:9010`           |
| **Auth APIs**                | Secure code generation                           | `http://auth-apis:9009`            |
| **Notification Module APIs** | Event-driven notifications                       | `http://notification-module-apis:9005` |
| **Cookie Categorization API**| AI-powered cookie categorization                 | `http://mypredictservice:port`|

### MongoDB Collections

| Collection                    | Purpose                              |
|-------------------------------|--------------------------------------|
| `cookie_scan_results`         | Website scan results and cookies     |
| `cookie_consent_templates`    | Consent template versions            |
| `cookie_consent_handles`      | Time-limited consent handles         |
| `cookie_consents`             | User consent records and versions    |
| `cookie_category_master`      | Cookie category definitions          |

---

## Prerequisites

| Requirement         | Version / Details                     |
|---------------------|---------------------------------------|
| **Java**            | JDK 17 or higher                      |
| **Maven**           | 3.8+                                  |
| **MongoDB**         | 5.0+ (Replica Set recommended)        |
| **Docker**          | 20.10+ (for containerized deployment) |
| **Playwright**      | 1.45.0 (bundled via Maven dependency) |
| **Chromium**        | Installed via Playwright              |

---

## Quick Start

### Local Development

```bash
# Clone the repository
git clone <repository-url> cookie-consent-scanner-java
cd cookie-consent-scanner-java

# Install Playwright browsers
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"

# Build the project
mvn clean package -DskipTests

# Run the application
java -jar target/url-cookie-scanner-playwright-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:9008`.

### Using Docker (Public Build)

```bash
# Build the Docker image (multi-stage build)
docker build -f deployment/Public_Dockerfile -t cookie-consent-apis:latest .

# Run the container
docker run -d \
  --name cookie-consent-apis \
  -p 9008:9008 \
  -e MONGODB_URI="mongodb://user:password@host:27017/?replicaSet=rs0&authSource=admin" \
  -e SERVER_PORT=9008 \
  -e USE_AI=true \
  cookie-consent-apis:latest
```

### Using Docker (Internal Build)

```bash
# Build the JAR first
mvn clean package -DskipTests

# Build internal Docker image
docker build -f deployment/Dockerfile -t cookie-consent-apis:latest .

# Run with environment configuration
docker run -d \
  --name cookie-consent-apis \
  -p 9008:9008 \
  -e MONGODB_URI="mongodb://user:password@host:27017/?replicaSet=rs0&authSource=admin" \
  -e AUDIT_MODULE_APIS_URL="http://audit-module-apis:9006" \
  -e VAULT_APIS_URL="http://vault-apis:9010" \
  -e AUTH_APIS_URL="http://auth-apis:9009" \
  -e NOTIFICATION_MODULE_APIS_URL="http://notification-module-apis:9005" \
  cookie-consent-apis:latest
```

---

## Configuration

### Custom Application Properties

The following properties are **custom configurations defined by the application** in `application.properties`. These properties control multi-tenant behavior, scanner execution, cookie categorization, and external service integrations.

#### Multi-Tenant Configuration

| Property | Description |
|--------|-------------|
| `multi-tenant.shared-database` | Shared MongoDB database used across tenants |
| `multi-tenant.tenant-database-prefix` | Prefix used when creating tenant specific databases |
| `multi-tenant.default-tenant-id` | Default tenant identifier if tenant header is not provided |
| `multi-tenant.enable-tenant-isolation` | Enables strict isolation between tenant databases |
| `multi-tenant.tenant-header-name` | HTTP header used to identify tenant in API requests |

#### Scanner Configuration

| Property | Description |
|--------|-------------|
| `scanner.maxRedirects` | Maximum redirects allowed during website scanning |
| `scanner.navTimeoutSeconds` | Timeout for page navigation during scanning |
| `scanner.navigationWait` | Page load wait strategy used by the scanner |
| `scanner.userAgent` | Browser user agent used by the scanner |

#### Cookie Categorization

| Property | Description |
|--------|-------------|
| `cookie.categorization.api.url` | External API endpoint used for cookie categorization |
| `cookie.categorization.use-external-api` | Enables AI-based cookie categorization |
| `cookie.categorization.cache.enabled` | Enables caching of categorized cookies |
| `cookie.categorization.cache.ttl.minutes` | Cache duration for categorized cookies |

#### Rate Limiting

| Property | Description |
|--------|-------------|
| `rate-limit.requests-per-minute` | Maximum API requests allowed per minute |
| `rate-limit.burst-capacity` | Extra burst capacity allowed for rate limiting |
| `rate-limit.enabled` | Enables or disables rate limiting |

#### Consent Configuration

| Property | Description |
|--------|-------------|
| `consent.handle.expiry.minutes` | Expiry time of generated consent handles |

#### External Service Configuration

| Property | Description |
|--------|-------------|
| `audit.service.base-url` | Base URL of the Audit Service used for recording audit events |
| `notification.service.base.url` | Base URL of the Notification Service used for triggering notification events |
| `vault.service.base-url` | Base URL of the Vault service used for cryptographic operations |
| `vault.service.sign-endpoint` | API endpoint used for generating digital signatures for responses |
| `vault.service.verify-endpoint` | API endpoint used for verifying signed requests or tokens |
| `vault.service.endpoints.encryptPayload` | API endpoint used to encrypt payloads using Vault |
| `secure.code.api.base-url` | Base URL of the Secure Code Service used to generate secure consent codes |

### Scanner Proxy Configuration

| Property | Description |
|--------|-------------|
| `scanner.use.proxy` | It is a boolean value to consider if the proxy should be used or not.(Default false) |
| `scanner.proxy.url` | It is a proxy url (http://url.com:port) |

### Environment Variables

| Variable                           | Description                                 | Default                             | Required |
|------------------------------------|---------------------------------------------|-------------------------------------|----------|
| `SERVER_PORT`                      | Application server port                     | `9008`                              | No       |
| `MONGODB_URI`                      | MongoDB connection URI                      | *(see application.properties)*      | **Yes**  |
| `AUDIT_MODULE_APIS_URL`            | Audit service base URL                      | `http://audit-module-apis:9006`     | No       |
| `VAULT_APIS_URL`                   | Vault service base URL                      | `http://vault-apis:9010`            | No       |
| `AUTH_APIS_URL`                    | Auth service base URL                       | `http://auth-apis:9009`             | No       |
| `NOTIFICATION_MODULE_APIS_URL`     | Notification service base URL               | `http://notification-module-apis:9005` | No    |
| `USE_AI`                           | Enable AI-powered cookie categorization     | `true`                              | No       |
| `JAVA_OPTS`                        | JVM options for runtime tuning              | *(empty)*                           | No       |

### Key Application Properties

| Property                                         | Description                                | Default        |
|--------------------------------------------------|--------------------------------------------|----------------|
| `multi-tenant.shared-database`                   | Shared MongoDB database name               | `shared_cookie_scanner` |
| `multi-tenant.tenant-database-prefix`            | Prefix for tenant databases                | `tenant_db_`   |
| `multi-tenant.tenant-header-name`                | HTTP header for tenant identification      | `X-Tenant-ID`  |
| `consent.handle.expiry.minutes`                  | Consent handle expiry in minutes           | `15`           |
| `rate-limit.requests-per-minute`                 | Rate limit per minute                      | `100`          |
| `rate-limit.burst-capacity`                      | Burst capacity for rate limiter            | `10`           |
| `rate-limit.enabled`                             | Enable/disable rate limiting               | `false`        |
| `cookie.categorization.use-external-api`         | Use external AI for categorization         | `false`         |
| `cookie.categorization.api.url`                  | AI categorization API endpoint             | `http://mypredictservice.com:port` |
| `scanner.browser.pool.size`                      | Browser instance pool size                 | `1`            |
| `scanner.navTimeoutSeconds`                      | Navigation timeout in seconds              | `15`           |
| `app.sign.response`                              | Enable response signing                    | `false`        |
| `app.verify.request`                             | Enable request verification                | `false`        |

### Configuration Files

- **Main configuration**: `src/main/resources/application.properties`
- **Cookie database**: `src/main/resources/open-cookie-database.csv` (fallback categorization)
- **JWK keys**: `src/main/resources/jwt-set.json`

---

## API Documentation

### Swagger UI

Interactive API documentation is available at:

```
http://<host>:9008/swagger-ui.html
```

OpenAPI JSON specification:

```
http://<host>:9008/api-docs
```

### API Endpoints

All endpoints require the `X-Tenant-ID` header for multi-tenant isolation.

#### Cookie Scanner APIs

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| POST   | `/scan`                                       | Start website cookie scan                |
| GET    | `/status/{transactionId}`                     | Get scan status and results              |
| PUT    | `/transaction/{transactionId}/cookie`         | Update cookie information                |
| POST   | `/transaction/{transactionId}/cookies`        | Add cookie to scan transaction           |
| GET    | `/health`                                     | Enhanced health check                    |

#### Consent Management APIs

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| POST   | `/consent/create`                             | Create consent by consent handle ID      |
| PUT    | `/consent/{consentId}/update`                 | Update consent (creates new version)     |
| GET    | `/consent/{consentId}/history`                | Get consent version history              |
| GET    | `/consent/{consentId}/versions/{version}`     | Get specific consent version             |
| GET    | `/consent/validate-token`                     | Validate consent token with JWS          |
| GET    | `/consent/check`                              | Check consent status                     |

#### Consent Handle APIs

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| POST   | `/consent-handle/create`                      | Create a new consent handle              |
| GET    | `/consent-handle/get/{consentHandleId}`       | Get consent handle by ID                 |
| POST   | `/consent-handle/handle-code`                 | Create handle and retrieve secure code   |
| POST   | `/consent-handle/create-handle-code`          | Create handle with active template       |

#### Consent Template APIs

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| POST   | `/cookie-templates`                           | Create consent template                  |
| GET    | `/cookie-templates/tenant`                    | Get templates with filters               |
| PUT    | `/cookie-templates/{templateId}/update`       | Update template (creates new version)    |
| GET    | `/cookie-templates/{templateId}/history`      | Get template version history             |
| GET    | `/cookie-templates/{templateId}/versions/{version}` | Get specific template version      |

#### Cookie Category APIs

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| POST   | `/category`                                   | Add a new cookie category                |
| PUT    | `/category`                                   | Update an existing cookie category       |
| GET    | `/category`                                   | Fetch all cookie categories              |

#### Dashboard APIs

| Method | Endpoint                                      | Description                              |
|--------|-----------------------------------------------|------------------------------------------|
| GET    | `/dashboard/{tenantId}`                       | Get consent dashboard data               |

### Authentication

All API requests require the following headers:

| Header           | Required | Description                                              |
|------------------|----------|----------------------------------------------------------|
| `X-Tenant-ID`    | Yes      | Tenant identifier for multi-tenant isolation             |
| `business-id`    | Varies   | Business identifier (required for template/handle APIs)  |
| `x-jws-signature`| Varies  | JWS token for request verification                       |
| `consent-token`  | Varies   | Consent JWT token (for validation endpoint)              |

---

## Development

### Project Structure

```
cookie-consent-scanner-java/
├── deployment/
│   ├── Dockerfile                          # Internal Docker build
│   ├── Public_Dockerfile                   # Public multi-stage Docker build
│   ├── jio-dl-deployment.yaml              # Production K8s deployment
│   ├── jio-dl-deployment-sit.yaml          # SIT K8s deployment
│   └── Public_deployment.yaml              # Public K8s deployment
├── src/
│   └── main/
│       ├── java/com/example/scanner/
│       │   ├── ScannerApplication.java     # Spring Boot entry point
│       │   ├── client/                     # External service clients
│       │   │   ├── AuditClient.java        # Audit service integration
│       │   │   ├── VaultClient.java        # Vault service integration
│       │   │   └── notification/           # Notification service integration
│       │   ├── config/                     # Configuration classes
│       │   │   ├── MultiTenantMongoConfig.java
│       │   │   ├── TenantAwareMongoDbFactory.java
│       │   │   ├── RateLimitingConfig.java
│       │   │   ├── SwaggerConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   ├── PathTraversalSecurityFilter.java
│       │   │   └── ...
│       │   ├── constants/                  # Error codes and constants
│       │   ├── controller/                 # REST controllers
│       │   │   ├── ScanController.java
│       │   │   ├── ConsentController.java
│       │   │   ├── ConsentHandleController.java
│       │   │   ├── ConsentTemplateController.java
│       │   │   ├── CategoryController.java
│       │   │   ├── DashboardController.java
│       │   │   └── CustomErrorController.java
│       │   ├── dto/                        # Data transfer objects
│       │   │   ├── request/                # Request DTOs
│       │   │   └── response/               # Response DTOs
│       │   ├── entity/                     # MongoDB document entities
│       │   │   ├── ScanResultEntity.java
│       │   │   ├── CookieEntity.java
│       │   │   ├── CookieConsent.java
│       │   │   ├── ConsentTemplate.java
│       │   │   ├── CookieConsentHandle.java
│       │   │   └── CookieCategory.java
│       │   ├── enums/                      # Enumeration types
│       │   ├── exception/                  # Custom exceptions and handlers
│       │   ├── mapper/                     # Object mappers
│       │   ├── repository/                 # MongoDB repositories
│       │   ├── service/                    # Business logic services
│       │   └── util/                       # Utility classes
│       └── resources/
│           ├── application.properties      # Application configuration
│           ├── open-cookie-database.csv    # Cookie categorization database
│           └── jwt-set.json                # JWK key set
├── pom.xml                                 # Maven build configuration
├── azure-pipelines.yml                     # CI/CD pipeline
├── sonar-project.properties                # SonarQube configuration
└── README.md                               # This file
```

### Building from Source

```bash
# Development build (with tests)
mvn clean package

# Production build (skip tests)
mvn clean package -DskipTests

# Run with specific profile
java -jar target/url-cookie-scanner-playwright-0.0.1-SNAPSHOT.jar --spring.profiles.active=sit

# Run with custom config location
java -jar target/url-cookie-scanner-playwright-0.0.1-SNAPSHOT.jar --spring.config.location=/opt/props/
```

---

## Deployment

### Kubernetes Deployment

```bash
# Apply production deployment
kubectl apply -f deployment/jio-dl-deployment.yaml

# Apply SIT deployment
kubectl apply -f deployment/jio-dl-deployment-sit.yaml

# Check deployment status
kubectl get pods -l app=cookie-consent-apis
kubectl logs -l app=cookie-consent-apis --tail=100

# Check service
kubectl get svc cookie-consent-apis
```

### Resource Allocation

| Environment | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-------------|-------------|-----------|----------------|--------------|
| Production  | 200m        | 1000m     | 512Mi          | 2Gi          |
| SIT         | 200m        | 500m      | 512Mi          | 1Gi          |

### CI/CD Pipeline

The service uses **Azure DevOps Pipelines** for continuous integration and deployment:

- **Pipeline Template**: `DPDP-Consent-Core/pipeline-templates` → `maven-main.yml`
- **Trigger**: `main` branch
- **Security Scans**: Enabled by default (configurable via `RUN_SECURITY_SCANS` parameter)

---

## Monitoring

### Health Check Endpoints

| Endpoint                  | Description                                    |
|---------------------------|------------------------------------------------|
| `/health`                 | Custom health check with feature status        |
| `/actuator/health`        | Spring Boot Actuator health endpoint           |
| `/actuator/info`          | Application information                        |
| `/actuator/metrics`       | Micrometer metrics                             |
| `/actuator/prometheus`    | Prometheus-compatible metrics export           |
| `/actuator/circuitbreakers` | Circuit breaker status                       |

### Exposed Management Endpoints

```
health, info, metrics, prometheus, circuitbreakers, ratelimiters, env, loggers, tenants
```

### Log Format

```
yyyy-MM-dd HH:mm:ss.SSS [thread] LEVEL [traceId,spanId,tenantId] logger - message
```

### Key Metrics

- HTTP request latency percentiles (p50, p90, p95, p99)
- Circuit breaker states (`scanService`, `categorizationService`)
- JVM and system metrics
- Custom application metrics tagged with `application` and `environment`

---

## Dependencies

### Internal Service Dependencies

| Service                       | Required | Port  | Purpose                            |
|-------------------------------|----------|-------|------------------------------------|
| **Audit Module APIs**         | Optional | 9006  | Audit trail logging                |
| **Vault APIs**                | Optional | 9010  | JWS signing, verification, encryption |
| **Auth APIs**                 | Optional | 9009  | Secure code generation             |
| **Notification Module APIs**  | Optional | 9005  | Notification event triggering      |
| **Cookie Categorization API** | Optional | 9014  | AI-powered cookie categorization   |

### Key Library Dependencies

| Library                  | Version   | Purpose                              |
|--------------------------|-----------|--------------------------------------|
| Spring Boot              | 3.4.3     | Application framework                |
| Spring Data MongoDB      | *(managed)* | MongoDB data access               |
| Playwright               | 1.45.0    | Browser automation for scanning      |
| Resilience4j             | 2.1.0     | Circuit breaker, rate limiter        |
| Bucket4j                 | 7.6.0     | Token-bucket rate limiting           |
| SpringDoc OpenAPI        | 2.7.0     | API documentation                    |
| Nimbus JOSE JWT          | 10.5      | JWT/JWS processing                   |
| Auth0 Java JWT           | 4.5.0     | JWT token handling                   |
| Apache Tika              | 2.9.2     | Content type detection               |
| Guava                    | 33.0.0    | Utility library                      |
| Lombok                   | *(managed)* | Boilerplate reduction             |

---

## Contributing

We welcome contributions to the Cookie Consent Scanner Service. Please read our [Contributing Guidelines](CONTRIBUTING.md) and [Code of Conduct](CODE_OF_CONDUCT.md) before submitting changes.

For detailed contribution process, branching strategy, and coding standards, refer to the central [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Security

The Cookie Consent Scanner Service implements multiple security layers:

- **Path Traversal Protection** — Multi-layer URL validation and sanitization
- **Invalid Character Filtering** — Request sanitization against injection attacks
- **Rate Limiting** — Configurable request throttling (Bucket4j)
- **Circuit Breaker** — Fault isolation for external dependencies (Resilience4j)
- **Tenant Isolation** — Strict database-level multi-tenant separation
- **JWS Verification** — Cryptographic request/response integrity
- **Payload Encryption** — Vault-based consent data encryption
- **Session Security** — HTTP-only, secure, SameSite=Strict cookies
- **Non-Root Container** — Runs as `si_digigov` user (UID 1000)
- **Security Context** — Kubernetes `seccompProfile: RuntimeDefault`, no privilege escalation

For vulnerability reporting, please refer to [SECURITY.md](SECURITY.md).

---

## License

This project is licensed under the **GNU Lesser General Public License v3.0 (LGPL-3.0)**. See the [LICENSE](LICENSE) file for the full license text.

Copyright (C) 2026 Jio Platforms Limited / Ministry of Electronics and Information Technology (MeitY), Government of India.

---

## Support

| Channel                | Link                                                    |
|------------------------|---------------------------------------------------------|
| **Documentation**      | [DPDP CMS Docs](https://dpdp-cms-docs.gov.in)          |
| **Issue Tracker**      | [Azure DevOps Boards](https://dev.azure.com/DPDP-Consent-Core) |
| **Email**              | JIOCONSENT@RIL.COM                                      |
| **Security Issues**    | See [SECURITY.md](SECURITY.md)                          |

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: 2026-03-12  
**Maintained by**: Jio Consent Management System Team
