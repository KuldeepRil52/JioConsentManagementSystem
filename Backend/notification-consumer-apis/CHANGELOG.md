# Changelog

All notable changes to the DPDP Notification Module will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Enhanced rate limiting capabilities with Redis backend
- GraphQL API support for flexible querying
- Advanced template versioning system
- Multi-language template management UI
- Real-time notification delivery tracking dashboard

## [1.0.0] - 2025-01-21

### Added
- **Core Notification System**
  - Multi-channel notification support (SMS, Email, Webhook/Callback)
  - Event-driven architecture with Kafka integration
  - REST API gateway with 65+ endpoints for notification management
  - Multi-tenant architecture with database-per-tenant isolation

- **Template Management**
  - Unified template creation and management
  - DigiGov Partner Portal integration for template onboarding
  - Template approval workflow automation
  - Dynamic argument substitution with master list support
  - Template lifecycle management (PENDING → ACTIVE → FAILED states)

- **Event Management**
  - Event triggering API with multi-recipient support
  - Event configuration with business-specific notification rules
  - Recipient type support (Data Principal, Data Fiduciary, Data Processor)
  - Event history tracking with pagination and filtering
  - Event statistics and count APIs

- **Master List System**
  - Hierarchical master label management
  - Master list configuration for dynamic data resolution
  - Event-to-label mapping system
  - MongoDB-based master data resolution
  - Master list activation and versioning

- **OTP Services**
  - OTP generation with RSA encryption
  - OTP verification with attempt tracking
  - Configurable OTP expiry and length
  - Rate limiting for OTP requests (5 requests per 10 minutes)
  - DigiGov G2C OTP Auth integration

- **Data Processor Management**
  - Third-party data processor registration
  - Processor-specific notification preferences
  - Webhook configuration for processor callbacks
  - Processor relationship management with consent data

- **Missed Notification APIs**
  - Missed notification tracking and retrieval
  - JWS signature verification for security
  - Notification ID listing with pagination
  - Bulk missed notification retrieval

- **Security Features**
  - JWS request/response signing for API security
  - Kerberos authentication for Kafka (SASL_PLAINTEXT)
  - RSA encryption for sensitive data (OTP)
  - JWT-based callback authentication
  - Tenant-based authorization and isolation
  - API signature verification

- **Integration & Infrastructure**
  - DigiGov Partner Portal integration (SMS/Email APIs)
  - Audit Module integration for comprehensive audit trails
  - Kafka producer for asynchronous event publishing
  - MongoDB multi-tenant database architecture
  - Caffeine in-memory cache for DigiGov tokens (55-minute TTL)
  - Spring Boot Actuator for health checks and monitoring
  - Prometheus metrics export

- **Monitoring & Observability**
  - Health check endpoints (liveness/readiness probes)
  - Prometheus metrics for JVM, HTTP, Kafka, and MongoDB
  - Custom business metrics (events triggered, templates created)
  - Structured JSON logging with Logstash encoder
  - Request correlation ID tracking
  - Cache hit/miss metrics

- **Testing & Quality**
  - Comprehensive unit test suite
  - Integration tests with embedded MongoDB
  - Kafka producer tests with Spring Kafka Test
  - 80% code coverage requirement with JaCoCo
  - Checkstyle integration for code quality
  - SonarQube integration for static analysis

- **Deployment & DevOps**
  - Docker containerization with eclipse-temurin:21-jre base image
  - Kubernetes deployment manifests (SIT, ST, Production)
  - ConfigMap and Secret management
  - Health probes for Kubernetes orchestration
  - Non-root container execution (si_digigov user)
  - Environment-specific configuration profiles

- **API Documentation**
  - OpenAPI 3.0 / Swagger UI integration
  - Interactive API documentation at `/swagger-ui.html`
  - Complete request/response schema definitions
  - API versioning support (v1)

- **Configuration Management**
  - Environment-based configuration profiles
  - External configuration via ConfigMaps
  - Secure secret management via Kubernetes Secrets
  - Property encryption with Jasypt
  - 100+ configurable environment variables

### Security
- Implemented CVE fixes for dependencies:
  - `commons-beanutils` → 1.11.0
  - `netty-bom` → 4.1.128.Final
  - `commons-io` → 2.18.0
  - `tomcat-embed-core` → 10.1.49 (fixes CVE-2025-55752)
  - `logback-classic` and `logback-core` → 1.5.19 (fixes CVE-2025-11226)

### Technical Stack
- **Framework:** Spring Boot 3.5.7
- **Language:** Java 21
- **Database:** MongoDB 5.0+
- **Messaging:** Apache Kafka 2.8+ with Kerberos
- **Cache:** Caffeine (in-memory)
- **Documentation:** OpenAPI 3.0 / Swagger UI (springdoc-openapi 2.7.0)
- **Logging:** Logback with Logstash encoder 7.4
- **Metrics:** Micrometer + Prometheus
- **Build Tool:** Maven 3.8+
- **Container Runtime:** Docker 20.10+
- **Orchestration:** Kubernetes 1.21+

### Dependencies
- Spring Boot Starters: web, data-mongodb, validation, actuator, cache, mail, kafka
- springdoc-openapi: 2.7.0
- lombok: 1.18.30
- logstash-logback-encoder: 7.4
- jasypt-spring-boot-starter: 3.0.5
- java-jwt: 4.4.0
- httpclient5: 5.x
- caffeine: (Spring managed)
- jackson-databind: (Spring managed)

### Compliance
- DPDP Act 2023 (Digital Personal Data Protection Act, India) compliance features
- TRAI DLT (Distributed Ledger Technology) validation for SMS
- CERT-In security guideline adherence
- Multi-tenant data isolation for privacy compliance

### Documentation
- Comprehensive README.md with setup, configuration, and API documentation
- CONTRIBUTING.md with development guidelines
- SECURITY.md with vulnerability reporting procedures
- CODE_OF_CONDUCT.md for community standards
- GOVERNANCE.md for project governance
- LICENSE (LGPL 3.0 + DPDP-specific addendum)

## Version History Legend

### Types of Changes
- **Added** - New features or functionality
- **Changed** - Changes to existing functionality
- **Deprecated** - Features that will be removed in future versions
- **Removed** - Features that have been removed
- **Fixed** - Bug fixes
- **Security** - Security vulnerability fixes and improvements

## Release Notes

### v1.0.0 Release Highlights

This is the initial production release of the DPDP Notification Module, providing a comprehensive notification management system for the DPDP Consent Management System.

**Key Capabilities:**
- Complete notification lifecycle management
- Multi-tenant architecture with data isolation
- Integration with DigiGov Partner Portal
- Support for SMS, Email, and Webhook notifications
- Dynamic template management with approval workflows
- OTP generation and verification
- Comprehensive audit trails
- Production-ready deployment configurations

**Deployment:**
- Kubernetes deployment to SIT/ST/Production environments
- Kerberos-authenticated Kafka integration with 17 production brokers
- MongoDB multi-tenant database architecture
- Docker containerization with non-root execution

**Compliance:**
- DPDP Act 2023 compliance features
- TRAI DLT validation for SMS
- Comprehensive security controls
- Audit logging for all operations

## Upgrade Guide

### Upgrading to 1.0.0

This is the initial release. For fresh installations:

1. Review the [README.md](README.md) for prerequisites
2. Configure required environment variables
3. Set up MongoDB and Kafka infrastructure
4. Deploy using Docker or Kubernetes
5. Verify health endpoints and connectivity

## Support

For questions, issues, or feature requests:

- **Documentation:** https://docs.jio.com/dpdp-cms/
- **Support Email:** Jio.ConsentSupport@ril.com
- **Issue Tracking:** https://dev.azure.com/JPL-Limited/dpdp-notification-module/issues
- **Security Issues:** Jio.ConsentSupport@ril.com

---

**Note:** This changelog follows semantic versioning. Breaking changes will result in major version bumps, new features in minor version bumps, and bug fixes in patch version bumps.
