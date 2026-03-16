# Changelog

All notable changes to the Partner Portal Backend Service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- Enhanced dashboard analytics with real-time metrics
- GraphQL API support for flexible querying
- Multi-language support for grievance templates
- Advanced search capabilities with full-text search
- Bulk operations for data processors and legal entities

## [0.0.1-SNAPSHOT] - 2025-01-21

### Added

#### Core Partner Portal System

- Complete partner organization management system
- REST API gateway with 28 controllers and 100+ endpoints for partner operations
- Multi-tenant architecture with database-per-tenant isolation
- Tenant-specific database routing via TenantMongoTemplateProvider
- Shared configuration database (cms_db_admin) for cross-tenant data
- Tenant registry management with automatic database initialization
- Session token generation and validation
- Comprehensive error handling with custom error codes

#### Tenant Management

- Tenant onboarding with WSO2 integration
- Tenant registry creation and management
- Tenant-specific database initialization (tenant_db_{tenantId})
- Tenant search with PAN and tenant ID filtering
- Tenant count and analytics
- Automatic TTL index creation for OTP and auth_secret collections
- Tenant-specific log file segregation

#### Legal Entity Management

- Legal entity creation and management
- PAN (Permanent Account Number) validation and storage
- Company name and logo management
- SPOC (Single Point of Contact) details management
- Legal entity update and modification
- Legal entity search with multiple parameters (legalEntityId, companyName)
- Legal entity count tracking

#### Data Processor Management

- Data processor onboarding with WSO2 credential generation
- Consumer key and secret management from WSO2
- Data processor unique ID assignment
- Certificate management (upload, storage, validation)
- Vendor risk document management with validation
- Attachment and document metadata tracking
- Data processor SPOC (Single Point of Contact) management
- Cross-border data processing flag
- Data processor status tracking (ACTIVE, INACTIVE)
- Data processor search and filtering
- Onboard notification with WSO2 consumer credentials (clientId and clientSecret) in event payload
- Document validation for attachments, vendor risk documents, and certificates
- Certificate modification tracking

#### Consent Configuration Management

- Consent configuration creation and versioning
- Purpose and data type association
- Processing activity configuration
- Consent template management
- Consent workflow configuration
- Consent status tracking
- Multi-parameter consent configuration search
- Consent configuration count with filtering
- Configuration history tracking

#### Grievance Management

- Grievance configuration creation with default CommunicationConfig (SMS and email set to true if not provided)
- Grievance types management (ACCESS, CORRECTION, etc.)
- Endpoint URL configuration for grievance submission
- Intake methods configuration (WEB_FORM, MOBILE_APP, etc.)
- Workflow stages definition
- SLA timeline configuration
- Escalation policy setup
- Retention policy management
- Communication configuration with default SMS and email settings
- Grievance configuration search and filtering
- Grievance configuration count tracking
- Configuration history tracking

#### DPO (Data Protection Officer) Configuration

- DPO details creation and management
- DPO contact information (email, phone)
- DPO name and designation tracking
- DPO configuration search
- DPO configuration update
- DPO configuration count
- DPO dashboard with statistics

#### ROPA (Record of Processing Activities) Management

- ROPA record creation
- Processing activity documentation
- Data category tracking
- Purpose association
- Data processor linkage
- Retention period management
- ROPA record update and deletion
- ROPA search with business ID and ROPA ID
- ROPA count tracking

#### Notification Configuration

- Notification configuration creation
- Event type management
- Customer identifier configuration
- Notification channel setup
- Notification configuration search
- Notification configuration update
- Notification trigger management

#### SMTP Configuration

- SMTP server configuration
- Email server credentials management
- SMTP configuration creation and update
- SMTP configuration search with businessId, scopeLevel, configId
- SMTP configuration count

#### SMSC Configuration

- SMSC provider configuration
- SMS gateway setup
- SMSC configuration creation and update
- SMSC configuration search with businessId, scopeLevel, configId
- SMSC configuration count

#### Purpose Management

- Purpose creation with name, code, and description
- Purpose update and modification
- Purpose search capabilities
- Purpose count tracking
- Purpose association with consent configurations

#### Data Type Management

- Data type creation
- Data type name and description management
- Data type update
- Data type search
- Data type count tracking
- Data type association with purposes

#### Processor Activity Management

- Processor activity creation
- Activity name and description management
- Data processor association
- Data type linkage
- Processor activity update
- Processor activity search
- Processor activity count
- Latest processor activity retrieval

#### System Configuration

- System-wide configuration management
- Configuration creation and update
- System configuration search with businessId, scopeLevel, configId
- System configuration count
- Configuration history tracking

#### Business Application Management

- Business application creation
- Application details management
- Business application update
- Business application search with businessId
- Business application count

#### Client Credentials Management

- Client credential retrieval
- Consumer key and secret management
- Certificate type management
- Client credential search with businessId, businessUniqueId, consumerKey, scopeLevel, certType

#### User Management

- User creation with role assignment
- User details management
- User update and deletion
- User search capabilities
- User count tracking
- User list retrieval
- User profile management

#### User Roles Management

- Role creation with component-based access control
- Role update and deletion
- Role search capabilities
- Role count tracking
- Role list retrieval
- Component list management

#### User Dashboard Theme

- User dashboard theme creation
- Theme customization and preferences
- Theme update and retrieval

#### Retention Configuration

- Retention configuration creation
- Retention policy management
- Retention configuration update and deletion
- Retention configuration search

#### Document Management

- Base64 document upload and storage
- Document metadata tracking (name, size, content type, tag)
- Document retrieval by ID
- Document versioning support
- File size validation
- Content type validation with magic bytes verification
- Document tag management
- Support for multiple file types (PDF, images, documents, certificates)
- Certificate format validation (CER, CRT, PEM, DER, P12, PFX)

#### DigiLocker Integration

- DigiLocker credential management
- Credential ID tracking
- Client ID association
- Credential status management
- DigiLocker credential search with credentialId, clientId, businessId, scopeLevel, status
- DigiLocker credential count

#### Data Breach Management

- Data breach report creation
- Data breach notification management
- Breach details tracking
- Breach notification status
- Data breach update and retrieval

#### Consent Signing Key

- Consent signing key generation
- Secure key management

#### Subscription Management

- Subscription enablement
- Subscription renewal
- Subscription details retrieval

#### Dashboard & Analytics

- Dashboard statistics retrieval
- Data processor statistics
- User statistics
- Entity count aggregation
- Status-based analytics
- Multi-parameter search capabilities

#### Authentication & Authorization

- OTP-based authentication (init and validate)
- Tenant OTP initialization and validation
- Session token generation and validation
- JWT token support
- Role-based token generation
- Two-factor authentication (2FA) support
- OTP expiry management (10 minutes TTL)
- Auth secret management (3 minutes TTL)

#### Audit Logging

- Comprehensive audit trail for all operations
- Audit log search with multiple parameters
- Operation type tracking (CREATE, UPDATE, DELETE)
- Actor and resource tracking
- Context information (IP address, transaction ID)
- Audit component categorization
- Integration with audit microservice

#### Configuration History

- Configuration change tracking
- Operation history (CREATE, UPDATE)
- Performed by tracking
- Config type management (GRIEVANCE, CONSENT, DPO, etc.)
- Configuration history search with configType, businessId, operation, performedBy

### Security

- JWT token-based authentication
- Session token generation and validation
- Property encryption for sensitive data (Jasypt with PBEWITHHMACSHA512ANDAES_256)
- Input validation and sanitization
- Secure document storage with base64 encoding
- Audit trail for all operations
- CORS configuration
- Request/response logging with correlation IDs
- Document validation with magic bytes verification
- Certificate format validation

### Integration & Infrastructure

- **WSO2 Credential Generator Service** integration for tenant/business/data processor onboarding
- **Vault Service** integration for key management and document signing
- **Auth Service** integration for OAuth2 token generation
- **Notification Service** integration for event triggering and OTP verification
- **Audit Service** integration for comprehensive audit trails
- MongoDB multi-tenant database architecture
- Spring Boot Actuator for health checks and monitoring
- Tenant-specific logging with automatic log rotation
- Request correlation ID tracking via transaction ID
- Retry mechanism for external service calls (max_retry = 3)
- Async notification processing

### Multi-Tenancy Support

- Database-per-tenant isolation (tenant_db_{tenantId})
- Automatic tenant database routing via TenantMongoTemplateProvider
- Tenant registry management
- Tenant-specific log file segregation
- Tenant context propagation across all operations
- Shared configuration database (cms_db_admin)
- Automatic TTL index creation for tenant-specific collections

### Monitoring & Observability

- Health check endpoints (liveness/readiness probes)
- Spring Boot Actuator metrics for JVM, HTTP, and MongoDB
- Custom business metrics
- Tenant-specific logging with INFO, DEBUG, and ERROR levels
- Request correlation ID tracking via transaction ID
- Log rotation with size and time-based policies

### Testing & Quality

- Unit test suite for service layer
- Integration tests with MongoDB
- Controller tests with MockMvc
- Test coverage reporting

### Deployment & DevOps

- Docker containerization with eclipse-temurin:21-jre base image
- Kubernetes deployment manifests (SIT, Production, Public)
- ConfigMap and Secret management
- Health probes for Kubernetes orchestration
- Non-root container execution (si_digigov user)
- Environment-specific configuration profiles
- Azure DevOps CI/CD pipeline integration

### API Documentation

- OpenAPI 3.0 / Swagger UI integration (springdoc-openapi 2.8.11)
- Interactive API documentation at /swagger-ui.html
- Complete request/response schema definitions
- API versioning support (v1.0)
- Comprehensive endpoint documentation with examples
- API title: "Partner Portal APIs"
- API version: 1.0

### Configuration Management

- Environment-based configuration via properties
- External configuration via environment variables
- Secure secret management via Kubernetes Secrets
- Configurable search parameters per entity type
- Retry configuration (max_retry = 3)
- OTP configuration (10 minutes TTL)
- Auth secret configuration (3 minutes TTL)
- HTTP client timeout configuration (connect: 5000ms, read: 5000ms)
- Property encryption with Jasypt

### Technical Stack

- **Framework:** Spring Boot 3.5.4
- **Language:** Java 21
- **Database:** MongoDB (with Spring Data MongoDB)
- **Build Tool:** Maven
- **Security:** Spring Security 6.5.7
- **API Documentation:** SpringDoc OpenAPI 2.8.11
- **JSON Processing:** Jackson with JSR310 date/time support
- **HTTP Client:** Apache HttpClient 5
- **JWT:** Nimbus JOSE + JWT 10.0.2
- **Logging:** Logback 1.5.21 with tenant-specific appenders
- **Container:** Docker (Eclipse Temurin 21 JRE base image)
- **Tomcat:** 10.1.49
- **Spring Framework:** 6.2.14
- **Commons Lang3:** 3.20.0
- **Encryption:** Jasypt 3.0.5
- **QR Code:** Google ZXing 3.5.3
- **Codec:** Commons Codec 1.18.0

### Dependencies

#### Spring Boot Starters
- `spring-boot-starter-web` - REST API support
- `spring-boot-starter-data-mongodb` - MongoDB integration
- `spring-boot-starter-security` - Security support
- `spring-boot-starter-actuator` - Health and metrics
- `spring-boot-starter-validation` - Bean validation
- `spring-boot-starter-test` - Testing support

#### Key Dependencies
- **springdoc-openapi**: 2.8.11
- **lombok**: 1.18.38
- **nimbus-jose-jwt**: 10.0.2
- **httpclient5**: 5.x
- **commons-lang3**: 3.20.0
- **jackson-datatype-jsr310**: (Spring managed)
- **jakarta.persistence-api**: 3.1.0
- **jakarta.validation-api**: 3.0.2
- **jakarta.jakartaee-api**: 10.0.0
- **jasypt-spring-boot-starter**: 3.0.5
- **commons-codec**: 1.18.0
- **spring-retry**: (Spring managed)
- **google.zxing**: 3.5.3
- **android-json**: 0.0.20131108.vaadin1

### Compliance

- **DPDP Act 2023** (Digital Personal Data Protection Act, India) compliance features
- Multi-tenant data isolation for privacy compliance
- Comprehensive audit logging for all consent operations
- Consent lifecycle tracking for compliance reporting
- Grievance redressal mechanism configuration
- DPO (Data Protection Officer) configuration support
- ROPA (Record of Processing Activities) management
- Data breach reporting and notification
- Document integrity verification
- Configuration history tracking

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

This is the current development version of the Partner Portal Backend Service, providing comprehensive partner organization and DPDP compliance management capabilities for the DPDP Consent Management System.

#### Key Capabilities

- Complete tenant and legal entity lifecycle management
- Multi-tenant architecture with complete data isolation
- Integration with WSO2, Vault, Auth, Notification, and Audit services
- Configuration management for consent, grievance, DPO, and system settings
- Data processor onboarding with WSO2 credential generation
- Document management with validation
- User and role management with RBAC
- Comprehensive audit logging for all operations
- Production-ready deployment configurations

#### Deployment

- Kubernetes deployment to SIT/Production/Public environments
- MongoDB multi-tenant database architecture
- Docker containerization with non-root execution
- Health probes and monitoring support

#### Compliance

- DPDP Act 2023 compliance features
- Comprehensive security controls
- Audit logging for all operations
- Multi-tenant data isolation
- Grievance redressal mechanism
- ROPA management for compliance tracking

#### Recent Improvements

- Default CommunicationConfig for grievance creation (SMS and email set to true)
- WSO2 consumer credentials (clientId and clientSecret) in data processor onboard notifications
- Document validation for attachments, vendor risk documents, and certificates
- Enhanced data processor onboarding workflow

## Upgrade Guide

### Upgrading to 0.0.1-SNAPSHOT

For fresh installations:

1. Review the README.md for prerequisites
2. Configure required environment variables (MONGODB_URI, MONGODB_DATABASE, etc.)
3. Set up MongoDB infrastructure with tenant databases
4. Configure integration service URLs (WSO2, Vault, Auth, Notification, Audit)
5. Deploy using Docker or Kubernetes
6. Verify health endpoints and connectivity
7. Register tenants in tenant_registry collection

### Migration Notes

- Ensure MongoDB indexes are created (auto-index-creation is enabled by default)
- Configure tenant-specific databases in the format `tenant_db_{tenantId}`
- Set up log directories with proper permissions for tenant-specific logging
- Configure Jasypt encryption password if using property encryption
- Ensure all external services (WSO2, Vault, Auth, Notification, Audit) are accessible
- Review and configure search parameters for each entity type

## Support

For questions, issues, or feature requests:

- **Documentation:** https://docs.jio.com/dpdp-cms/
- **Support Email:** Jio.ConsentSupport@ril.com
- **Issue Tracking:** https://dev.azure.com/JPL-Limited/DPDP-Consent-Core/issues
- **Security Issues:** Jio.ConsentSupport@ril.com

**Note:** This changelog follows semantic versioning. Breaking changes will result in major version bumps, new features in minor version bumps, and bug fixes in patch version bumps.
