# Changelog

All notable changes to the DPDP Consent Core Backend will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- Enhanced consent analytics dashboard with real-time metrics
- GraphQL API support for flexible consent querying
- Advanced consent versioning and history tracking
- Multi-language consent template management UI
- Real-time consent status change notifications via WebSocket
- Enhanced parental KYC workflow with multiple verification methods
- Consent data export and reporting capabilities
- Advanced consent search with full-text search support

## [0.0.1-SNAPSHOT] - 2025-01-21

### Added

- **Core Consent Management System**
    - Complete consent lifecycle management (Create, Read, Update, Validate)
    - Consent status tracking (Active, Inactive, Expired, Withdrawn)
    - Consent token generation and validation
    - Payload hash verification for consent integrity
    - Consent chain hash tracking for audit purposes
    - REST API gateway with 21+ endpoints for consent operations
    - Multi-tenant architecture with database-per-tenant isolation

- **Consent Handle Management**
    - Consent handle creation and management
    - Handle status tracking (PENDING, ACTIVE, EXPIRED, WITHDRAWN)
    - Handle-to-consent relationship management
    - Batch handle creation from templates
    - Handle search and filtering capabilities
    - Handle remarks and status tracking

- **Template Management**
    - Consent template creation and versioning
    - Multilingual template support with language-specific content
    - UI configuration for consent templates
    - Document metadata association with templates
    - Template status management (ACTIVE, INACTIVE)
    - Template search with version filtering
    - Template preference configuration

- **Consent Metadata Management**
    - Extended metadata storage for consents
    - Metadata retrieval by consent meta ID
    - Metadata association with consent lifecycle

- **Document Management**
    - PDF document upload and storage
    - Document metadata tracking
    - Document retrieval by ID
    - Document viewing with binary content delivery
    - File size validation (max 10MB)
    - Document type validation (PDF only)

- **Parental KYC Integration**
    - DigiLocker OAuth2 integration for parental consent
    - Parental KYC creation for users below 18 years
    - Parental reference ID tracking
    - DigiLocker token exchange and user data retrieval
    - Parental consent flag management

- **Advanced Search and Analytics**
    - Multi-parameter consent search with filtering
    - Consent handle search with status filtering
    - Template search with version and business filtering
    - Consent count APIs with parameter filtering
    - Consent status grouping and analytics
    - Customer identifier-based search (MOBILE, EMAIL)

- **Security Features**
    - JWS request/response signing for API security
    - JWT token validation support
    - RSA encryption for sensitive data through vault service
    - Payload hash verification (SHA-256)
    - Tenant-based authorization and isolation
    - API signature verification via vault service
    - Request/response encryption support

- **Integration & Infrastructure**
    - Notification Module integration for consent lifecycle events
    - Audit Module integration for comprehensive audit trails
    - Vault Service integration for encryption and digital signatures
    - DigiLocker API integration for parental KYC
    - MongoDB multi-tenant database architecture
    - Spring Boot Actuator for health checks and monitoring
    - Tenant-specific logging with automatic log rotation

- **Multi-Tenancy Support**
    - Database-per-tenant isolation (`tenant_db_{tenantId}`)
    - Automatic tenant database routing via `TenantMongoTemplateProvider`
    - Tenant registry management
    - Tenant-specific log file segregation
    - Tenant context propagation across all operations
    - Shared configuration database (`cms_db_admin`)

- **Monitoring & Observability**
    - Health check endpoints (liveness/readiness probes)
    - Spring Boot Actuator metrics for JVM, HTTP, and MongoDB
    - Custom business metrics (consents created, templates created)
    - Tenant-specific logging with INFO, DEBUG, and ERROR levels
    - Request correlation ID tracking via transaction ID
    - Log rotation with size and time-based policies

- **Testing & Quality**
    - Unit test suite for service layer
    - Integration tests with MongoDB
    - Controller tests with MockMvc
    - Test coverage reporting

- **Deployment & DevOps**
    - Docker containerization with eclipse-temurin:21-jre base image
    - Kubernetes deployment manifests (SIT, ST, Production, Public)
    - ConfigMap and Secret management
    - Health probes for Kubernetes orchestration
    - Non-root container execution (si_digigov user)
    - Environment-specific configuration profiles

- **API Documentation**
    - OpenAPI 3.0 / Swagger UI integration (springdoc-openapi 2.8.11)
    - Interactive API documentation at `/swagger-ui.html`
    - Complete request/response schema definitions
    - API versioning support (v1.0)
    - Comprehensive endpoint documentation with examples

- **Configuration Management**
    - Environment-based configuration via properties
    - External configuration via environment variables
    - Secure secret management via Kubernetes Secrets
    - Configurable search parameters per entity type
    - Batch processing configuration
    - JWS signature enable/disable configuration


### Security

- Implemented JWS signature validation (x-jws-signature header)
- Enhanced request/response signing capabilities via vault service
- Security vulnerability fixes in dependencies:
    - `tomcat-embed-core` → 10.1.49 (fixes security vulnerabilities)
    - `logback-classic` and `logback-core` → 1.5.21 (fixes CVE-2025-11226)
    - `spring-security` → 6.5.7 (security patches)
    - `spring-framework` → 6.2.14 (security patches)
    - `commons-lang3` → 3.20.0 (security updates)
- Resolved BlackDuck security vulnerabilities
- Enhanced JWT and JWS signature handling

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

### Dependencies

- Spring Boot Starters: web, data-mongodb, validation, actuator, security, test
- springdoc-openapi: 2.8.11
- lombok: 1.18.38
- gson: 2.13.0
- nimbus-jose-jwt: 10.0.2
- java-jwt: 4.4.0
- httpclient5: 5.x
- commons-lang3: 3.20.0
- jackson-datatype-jsr310: (Spring managed)
- jakarta.persistence-api: 3.1.0
- jakarta.validation-api: 3.0.2

### Compliance

- DPDP Act 2023 (Digital Personal Data Protection Act, India) compliance features
- Multi-tenant data isolation for privacy compliance
- Comprehensive audit logging for all consent operations
- Consent lifecycle tracking for compliance reporting
- Parental consent support for users below 18 years (DPDP Act requirement)
- Payload integrity verification for consent data

### Documentation

- Comprehensive README.md with setup, configuration, and API documentation
- CHANGELOG.md with version history and change tracking
- LICENSE (LGPL 3.0 + DPDP-specific addendum)
- OpenAPI/Swagger interactive API documentation

## Version History Legend

### Types of Changes

- **Added** - New features or functionality
- **Changed** - Changes to existing functionality
- **Deprecated** - Features that will be removed in future versions
- **Removed** - Features that have been removed
- **Fixed** - Bug fixes
- **Security** - Security vulnerability fixes and improvements

## Release Notes

### v0.0.1-SNAPSHOT Release Highlights

This is the current development version of the DPDP Consent Core Backend, providing comprehensive consent management capabilities for the DPDP Consent Management System.

**Key Capabilities:**

- Complete consent lifecycle management (Create, Update, Validate, Withdraw)
- Multi-tenant architecture with complete data isolation
- Integration with notification, audit, and vault services
- Template management with multilingual support
- Parental KYC integration with DigiLocker
- Advanced search and analytics capabilities
- Production-ready deployment configurations

**Deployment:**

- Kubernetes deployment to SIT/ST/Production/Public environments
- MongoDB multi-tenant database architecture
- Docker containerization with non-root execution
- Health probes and monitoring support

**Compliance:**

- DPDP Act 2023 compliance features
- Comprehensive security controls
- Audit logging for all operations
- Multi-tenant data isolation

**Recent Improvements:**

- JWS signature validation for enhanced security
- Payload hash verification for consent integrity
- Batch processing for template handle creation
- Enhanced search capabilities with multiple filter parameters
- Consent status analytics and grouping

## Upgrade Guide

### Upgrading to 0.0.1-SNAPSHOT

For fresh installations:

1. Review the [README.md](README.md) for prerequisites
2. Configure required environment variables (MONGODB_URI, MONGODB_DATABASE, etc.)
3. Set up MongoDB infrastructure with tenant databases
4. Configure integration service URLs (Notification, Audit, Vault)
5. Deploy using Docker or Kubernetes
6. Verify health endpoints and connectivity
7. Register tenants in tenant_registry collection

### Migration Notes

- Ensure MongoDB indexes are created (auto-index-creation is enabled by default)
- Configure tenant-specific databases in the format `tenant_db_{tenantId}`
- Set up log directories with proper permissions for tenant-specific logging
- Configure JWS signature if required (set `JWS_SIGNATURE_ENABLED=true`)
- Ensure vault service is accessible if JWS or encryption features are enabled

## Support

For questions, issues, or feature requests:

- **Documentation:** https://docs.jio.com/dpdp-cms/
- **Support Email:** Jio.ConsentSupport@ril.com
- **Issue Tracking:** https://dev.azure.com/JPL-Limited/DPDP-Consent-Core/issues
- **Security Issues:** Jio.ConsentSupport@ril.com

---

**Note:** This changelog follows semantic versioning. Breaking changes will result in major version bumps, new features in minor version bumps, and bug fixes in patch version bumps.
