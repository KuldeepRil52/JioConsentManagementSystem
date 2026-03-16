# Changelog

All notable changes to the **Jio Consent Management System (JCMS)** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


### Planned Features

#### System-Wide Enhancements
- Enhanced consent analytics dashboard with real-time metrics
- GraphQL API support for flexible consent querying
- Advanced consent versioning and history tracking
- Multi-language consent template management UI
- Real-time consent status change notifications via WebSocket
- Enhanced parental KYC workflow with multiple verification methods
- Consent data export and reporting capabilities
- Advanced consent search with full-text search support

#### Service-Specific Improvements

**Audit Core Service**
- PDF generation improvements (Section 65B template)
- Enhanced `PdfSignerService` integration for PDF signing
- Kafka producer improvements for audit event publishing
- API contract hardening and OpenAPI annotations

**Auth Service**
- Enhanced Redis caching support
- Additional authentication methods
- Rate limiting implementation
- Advanced session analytics
- Token refresh endpoint improvements

**Notification Module**
- Enhanced rate limiting capabilities with Redis backend
- Advanced template versioning system
- Multi-language template management UI
- Real-time notification delivery tracking dashboard

**Partner Portal**
- Enhanced dashboard analytics with real-time metrics
- Multi-language support for grievance templates
- Advanced search capabilities with full-text search
- Bulk operations for data processors and legal entities

**Scheduler Service**
- Multiple retention and expiry scheduled jobs optimization
- Enhanced integration hooks for notification, audit, and vault modules
- Improved scheduler run history and metrics

**Cookie Consent Scanner**
- Documentation refresh covering architecture and compliance policies
- Enhanced multi-tenant MongoDB configuration
- Improved security filters and deployment instructions

---

## [1.0.0] - 2025-11-26

### Overview

This is the **initial production release** of the Jio Consent Management System (JCMS), a comprehensive Digital Personal Data Protection (DPDP) Act 2023 compliant consent management platform. This release includes 10 integrated microservices providing end-to-end consent lifecycle management.

### System-Wide Features

#### Architecture & Infrastructure
- **Multi-Tenant Architecture**: Database-per-tenant isolation with automatic routing
- **Microservices Design**: 10 loosely coupled services with REST API communication
- **Security**: JWT/JWS authentication, RSA encryption, comprehensive audit trails
- **Monitoring**: Spring Boot Actuator, Prometheus metrics, structured JSON logging
- **Deployment**: Docker containerization, Kubernetes orchestration, CI/CD pipelines
- **Compliance**: DPDP Act 2023, TRAI DLT, CERT-In security guidelines

#### Technology Stack
- **Backend**: Java 21, Spring Boot 3.5.x, MongoDB 5.0+, Apache Kafka 2.8+
- **Frontend**: React, Redux, Node.js 14+, Parcel bundler, JDS Core components
- **Security**: Spring Security 6.5.7, Nimbus JOSE JWT, BCrypt hashing
- **Documentation**: OpenAPI 3.0, Swagger UI, comprehensive API documentation
- **Build & Deploy**: Maven 3.8+, Docker 20.10+, Kubernetes 1.21+

---

### Service-Specific Features

## 🏛️ Partner Portal Service (Port: 9002)

### Added
- **Tenant Management System**
  - Tenant onboarding with WSO2 integration
  - Tenant registry creation and management
  - Tenant-specific database initialization (tenant_db_{tenantId})
  - Session token generation and validation
  - Automatic TTL index creation for OTP and auth_secret collections

- **Legal Entity Management**
  - Legal entity creation with PAN validation
  - Company name and logo management
  - SPOC (Single Point of Contact) details management
  - Legal entity search and analytics

- **Data Processor Management**
  - Data processor onboarding with WSO2 credential generation
  - Consumer key/secret management from WSO2
  - Certificate and vendor risk document management
  - Cross-border data processing configuration
  - Onboard notification with WSO2 consumer credentials

- **Configuration Management**
  - Consent configuration with purpose/data type association
  - Grievance configuration with default communication settings
  - DPO (Data Protection Officer) configuration and dashboard
  - ROPA (Record of Processing Activities) management
  - Notification, SMTP, and SMSC configuration

- **User & Access Management**
  - User management with role-based access control
  - Role creation with component-based permissions
  - User dashboard theme customization
  - Authentication with OTP support (2FA)

- **Document & Data Management**
  - Base64 document upload with validation
  - Document metadata tracking and versioning
  - DigiLocker integration for credential management
  - Data breach reporting and notification

- **Analytics & Monitoring**
  - Dashboard statistics and analytics
  - Audit logging with search capabilities
  - Configuration history tracking
  - Multi-parameter search across all entities

### Security
- JWT token-based authentication
- Property encryption with Jasypt (PBEWITHHMACSHA512ANDAES_256)
- Document validation with magic bytes verification
- Comprehensive audit trails
- CORS configuration and request/response logging

---

## 🔐 Auth Service (Port: 9009)

### Added
- **JWT Token Management**
  - RS256-signed JWT token generation with configurable expiration
  - Token validation, introspection, and revocation
  - OAuth2-compliant token introspection endpoint
  - Persistent revoked token tracking with TTL

- **Session Management**
  - Multi-session support (max 3 active sessions per user)
  - 7-day absolute session expiry
  - IP address and user agent tracking
  - Secure code session workflows

- **WSO2 Integration**
  - WSO2 API Manager token caching and management
  - OAuth2 access token retrieval for business applications
  - Token cache with 60-minute expiration

- **Security Features**
  - RSA key-based JWT signing (JWK format)
  - BCrypt hashing for refresh tokens
  - Request/response JWS signature verification
  - IP whitelisting support
  - Tenant secret code validation

- **Multi-Tenancy**
  - Database-per-tenant isolation (tenant_db_{tenantId})
  - Admin database (cms_db_admin) for shared configuration
  - Tenant and business verification

### Security
- RSA private key storage in JWK format
- BCrypt token hashing
- Comprehensive audit logging with retry mechanism
- Tenant isolation at database level

---

## 🎯 Consent Core Service (Port: 9001)

### Added
- **Consent Lifecycle Management**
  - Complete consent CRUD operations (Create, Read, Update, Validate)
  - Consent status tracking (Active, Inactive, Expired, Withdrawn)
  - Consent token generation and validation
  - Payload hash verification (SHA-256) for integrity
  - Consent chain hash tracking for audit purposes

- **Consent Handle Management**
  - Handle creation and status management
  - Handle-to-consent relationship tracking
  - Batch handle creation from templates
  - Handle search and filtering capabilities

- **Template Management**
  - Template creation and versioning
  - Multilingual template support
  - UI configuration for templates
  - Template preference configuration
  - Template status management (ACTIVE, INACTIVE)

- **Document Management**
  - PDF document upload and storage (max 10MB)
  - Document metadata tracking
  - Document viewing with binary content delivery
  - File size and type validation

- **Parental KYC Integration**
  - DigiLocker OAuth2 integration
  - Parental consent for users below 18 years
  - Parental reference ID tracking
  - Token exchange and user data retrieval

- **Advanced Search & Analytics**
  - Multi-parameter search with filtering
  - Consent count APIs with parameter filtering
  - Status grouping and analytics
  - Customer identifier-based search (MOBILE, EMAIL)

### Security
- JWS request/response signing via vault service
- JWT token validation support
- RSA encryption through vault service
- Payload hash verification
- Tenant-based authorization and isolation

---

## 📧 Notification Module (Port: 9003)

### Added
- **Multi-Channel Notification System**
  - SMS, Email, and Webhook/Callback support
  - Event-driven architecture with Kafka integration
  - Multi-recipient notification support
  - Event triggering API with business-specific rules

- **Template Management**
  - Unified template creation and management
  - DigiGov Partner Portal integration
  - Template approval workflow automation
  - Dynamic argument substitution with master lists
  - Template lifecycle management (PENDING → ACTIVE → FAILED)

- **Event Management**
  - Event configuration with recipient type support
  - Event history tracking with pagination
  - Event statistics and count APIs
  - Recipient type support (Data Principal, Fiduciary, Processor)

- **Master List System**
  - Hierarchical master label management
  - Master list configuration for dynamic data resolution
  - Event-to-label mapping system
  - MongoDB-based master data resolution

- **OTP Services**
  - OTP generation with RSA encryption
  - OTP verification with attempt tracking
  - Configurable OTP expiry and length
  - Rate limiting (5 requests per 10 minutes)

- **Data Processor Management**
  - Third-party processor registration
  - Processor-specific notification preferences
  - Webhook configuration for callbacks

- **Missed Notification APIs**
  - Missed notification tracking and retrieval
  - JWS signature verification for security
  - Bulk notification retrieval

### Security
- JWS request/response signing
- Kerberos authentication for Kafka (SASL_PLAINTEXT)
- RSA encryption for sensitive data (OTP)
- JWT-based callback authentication
- Tenant-based authorization

---

## 📨 Notification Consumer (Port: 9020)

### Added
- **Multi-Channel Delivery Engine**
  - SMS, Email, and Webhook delivery
  - Kafka consumer with retry mechanisms (3 attempts)
  - Template resolution with dynamic substitution
  - Status tracking and delivery confirmation

- **Reliability Features**
  - Exponential backoff retry with configurable attempts
  - Manual acknowledgment for reliable message processing
  - Circuit breaker pattern for external API failures
  - DLT compliance for SMS notifications

- **Integration**
  - DigiGov SMS/Email API integration
  - Token management with OAuth2
  - Delivery status tracking
  - Multi-tenant MongoDB routing

### Security
- DigiGov OAuth2 token caching and refresh
- Secure webhook payload delivery
- TRAI DLT entity and template validation

---

## ⚖️ Grievance Service (Port: 9006)

### Added
- **Grievance Management System**
  - Tenant-aware grievance CRUD with full history
  - Dynamic template library with multilingual support
  - Grievance types and user type catalogues
  - Customer feedback management (1-5 rating)

- **Workflow Management**
  - Workflow stages definition and tracking
  - SLA timeline configuration
  - Escalation policy setup
  - Status transitions (NEW, IN_PROGRESS, RESOLVED, etc.)

- **Integration Features**
  - Notification integration for status updates
  - Vault-signed JWT issuance for verification
  - Audit service integration for compliance
  - IP address capture for mutations

### Security
- Vault-signed JWT tokens for downstream verification
- Tenant isolation via TenantAwareMongoTemplate
- Comprehensive audit trails
- Request correlation via tenant/business/transaction headers

---

## 📊 Audit Core Service (Port: 8080)

### Added
- **Audit Recording System**
  - REST API for audit record creation and retrieval
  - Section 65B-style PDF certificate generation
  - PAdES signing integration via PdfSignerService
  - Multi-tenant header handling (X-Tenant-Id, X-Business-Id, X-Transaction-Id)

- **PDF Generation**
  - Apache PDFBox implementation (SignPdfServiceImpl)
  - Templated sections with JSON appendix
  - Font loading from src/main/resources/fonts/
  - External signature space reservation

- **Kafka Integration**
  - Kafka producer for audit event publishing
  - Configurable topic (audit-events default)
  - Event publishing after audit creation

### Security
- Tenant-aware operations with header validation
- PDF signing integration
- Secure audit event publishing

---

## 🔧 WSO2 Credentials Generation Service (Port: 9007)

### Added
- **WSO2 Integration**
  - Tenant registration with WSO2 API Manager
  - Business application onboarding
  - Data processor credential provisioning
  - Idempotent operations preventing duplicates

- **Credential Management**
  - OAuth2 consumer key/secret generation
  - API subscription management
  - Secure credential storage in tenant databases
  - Automatic subscription to configured APIs

- **Database Management**
  - Multi-tenant credential storage
  - Collections: wso2_tenants, wso2_business_applications, wso2_data_processor
  - wso2_available_apis collection for API ID management

### Security
- SSL/TLS communication with WSO2 API Manager
- Secure credential storage
- Base64 encoded authentication tokens

---

## ⏰ Scheduler Service (Port: 9001)

### Added
- **Scheduled Job Management**
  - Tenant-aware consent expiry processing
  - Consent-handle expiry management
  - Consent-preference reminder scheduling
  - Cookie consent expiry/retention
  - Grievance escalation automation
  - Data retention and cleanup jobs

- **Job Configuration**
  - Configurable cron expressions per job
  - Batch processing with configurable batch sizes
  - Thread pool tuning per job
  - Job run history and metrics

- **Integration**
  - Notification module integration
  - Audit service integration
  - Vault service integration

### Features
- REST endpoints for scheduler statistics
- Job run history tracking
- Multi-tenant job execution

---

## 🍪 Cookie Consent Scanner (Port: TBD)

### Added
- **Cookie Scanning System**
  - Playwright 1.45.0 based scanning pipeline
  - Subdomain-aware cookie capture
  - Cookie categorization with retries
  - Tenant-aware persistence

- **Consent Management**
  - Cookie consent handle management
  - Consent template integration
  - Consent lifecycle tracking
  - Dashboard analytics

- **Resilience Features**
  - Rate limiting with Bucket4j
  - Circuit breakers for scan services
  - Multi-tenant MongoDB filtering

### Security
- Request/response signing via vault service
- Strict error serialization
- Hardened security headers

---

## 🖥️ Frontend Application (Port: 1234)

### Added
- **React-Based Interface**
  - Consent template management interface
  - Data processing register with visual workflows
  - Grievance management dashboard with SLA tracking
  - Cookie consent management interface

- **Dashboard & Analytics**
  - Audit and compliance reporting dashboards
  - Multi-language support with accessibility features
  - Redux state management with JDS Core components
  - Guided tour feature for user onboarding

- **User Experience**
  - Responsive design with modern UX practices
  - Environment-specific configurations (.env.dev, .env.nonprod, .env.prod)
  - Code splitting and lazy loading
  - WCAG accessibility compliance

### Features
- Parcel bundler for optimized builds
- Environment-specific builds and configurations
- Integration with all backend services
- Comprehensive error handling and validation

---

## Security Enhancements (All Services)

### CVE Fixes and Security Updates
- `tomcat-embed-core` → 10.1.49 (fixes multiple security vulnerabilities)
- `logback-classic` and `logback-core` → 1.5.19/1.5.21 (fixes CVE-2025-11226)
- `spring-security` → 6.5.7 (security patches)
- `spring-framework` → 6.2.14 (security patches)
- `commons-lang3` → 3.20.0 (security updates)
- `commons-beanutils` → 1.11.0 (multiple CVE fixes)
- `netty-bom` → 4.1.128.Final (security updates)
- `commons-io` → 2.18.0 (security fixes)

### Security Features
- Multi-factor authentication (2FA) support
- JWS signature validation across services
- RSA encryption for sensitive data
- Comprehensive audit logging
- IP whitelisting capabilities
- CORS configuration and validation
- Request/response correlation tracking

---

## Integration & Compliance

### DPDP Act 2023 Compliance
- Complete consent lifecycle management per DPDP Act requirements
- Multi-tenant data isolation for privacy compliance
- Parental consent support for users below 18 years
- Comprehensive audit trails for compliance reporting
- Grievance redressal mechanism configuration
- Data breach reporting and notification
- DPO (Data Protection Officer) configuration
- ROPA (Record of Processing Activities) management

### External Integrations
- **WSO2 API Manager**: OAuth2 credential management
- **DigiGov Partner Portal**: SMS/Email delivery, template onboarding
- **DigiLocker**: Parental KYC verification
- **Vault Service**: Encryption, digital signatures, key management
- **TRAI DLT**: SMS delivery compliance
- **Apache Kafka**: Asynchronous message processing

### Monitoring & Observability
- Spring Boot Actuator health checks across all services
- Prometheus metrics export
- Structured JSON logging with Logstash encoder
- Request correlation ID tracking
- Tenant-specific log segregation
- Health probes for Kubernetes orchestration

---

## Deployment & DevOps

### Containerization
- Docker images with eclipse-temurin:21-jre base
- Non-root container execution (si_digigov user)
- Multi-stage builds for optimization
- Environment-specific configurations

### Kubernetes Support
- Deployment manifests for SIT/ST/Production/Public environments
- ConfigMap and Secret management
- Health probes (liveness/readiness)
- Horizontal Pod Autoscaling support
- Network policies for service isolation

### CI/CD
- Azure DevOps pipelines
- Maven-based builds for backend services
- npm-based builds for frontend
- Automated testing and quality gates
- Security scanning with BlackDuck and Fortify
- Code coverage reporting with JaCoCo

---

## Documentation & API

### API Documentation
- OpenAPI 3.0 specifications across all services
- Interactive Swagger UI at service endpoints
- Comprehensive request/response schemas
- API versioning support (v1.0)
- 100+ REST endpoints across the platform

### Project Documentation
- Comprehensive README.md with setup guides
- Service-specific configuration documentation
- Deployment guides for Docker and Kubernetes
- Troubleshooting guides and FAQ
- Contributing guidelines and code standards

---

## Version History Legend

### Types of Changes
- **Added** - New features or functionality
- **Changed** - Changes to existing functionality  
- **Deprecated** - Features that will be removed in future versions
- **Removed** - Features that have been removed
- **Fixed** - Bug fixes
- **Security** - Security vulnerability fixes and improvements

---

## Release Highlights

### Platform Overview
The **Jio Consent Management System v1.0.0** represents a comprehensive DPDP Act 2023 compliant consent management platform with the following capabilities:

#### **Scale & Performance**
- **10 Microservices**: Complete backend and frontend ecosystem
- **100+ REST APIs**: Comprehensive functionality coverage
- **Multi-Tenant**: Database-per-tenant isolation
- **High Availability**: Kubernetes orchestration with health probes
- **Scalability**: Horizontal scaling support

#### **Compliance & Security**
- **DPDP Act 2023**: Full compliance with Indian data protection regulations
- **TRAI DLT**: SMS delivery compliance
- **Security**: JWT/JWS authentication, RSA encryption, audit trails
- **Multi-Factor**: 2FA support with OTP
- **Tenant Isolation**: Complete data separation

#### **Integration & Ecosystem**
- **WSO2 Integration**: OAuth2 credential management
- **DigiGov Portal**: Government service integration
- **Apache Kafka**: Event-driven architecture
- **MongoDB**: Multi-tenant database architecture
- **External APIs**: DigiLocker, TRAI DLT, Vault services

#### **Developer Experience**
- **Modern Stack**: Java 21, Spring Boot 3.5, React, MongoDB
- **Documentation**: Comprehensive API docs and guides
- **Testing**: Unit, integration, and end-to-end tests
- **CI/CD**: Automated pipelines with quality gates
- **Monitoring**: Health checks, metrics, structured logging

---

## Upgrade Guide

### Fresh Installation
This is the initial release. For new installations:

1. **Prerequisites Setup**
   - Install Java 21, MongoDB 5.0+, Kafka 2.8+
   - Set up WSO2 API Manager if using credential management
   - Configure network access between services

2. **Database Setup**
   - Create admin database: `cms_db_admin`
   - Configure tenant-specific databases: `tenant_db_{tenantId}`
   - Set up required collections and indexes

3. **Service Deployment**
   - Deploy services in dependency order (Auth → WSO2 Creds → Partner Portal → etc.)
   - Configure environment variables per service
   - Verify health endpoints after each service deployment

4. **Frontend Deployment**
   - Configure environment-specific settings
   - Build and deploy React application
   - Configure API endpoints and integrations

5. **Verification**
   - Test service-to-service communication
   - Verify tenant onboarding workflow
   - Validate consent lifecycle operations

### Migration Notes
- Ensure all external services (WSO2, DigiGov, Vault) are accessible
- Configure proper network policies in Kubernetes
- Set up monitoring and alerting
- Review security configurations and certificates

---

## Known Issues & Limitations

### Current Limitations
- **Caching**: Some services use in-memory caching (Redis support available but not default)
- **Search**: Full-text search capabilities limited to basic parameter matching
- **Real-time**: WebSocket support planned for future releases
- **Languages**: Multi-language support in development for some components

### Planned Improvements
- Enhanced caching with Redis backend
- GraphQL API support
- Real-time notifications via WebSocket
- Advanced analytics and reporting
- Multi-language UI support

---

## Support & Community

### Getting Help
- **Technical Support**: Jio.ConsentSupport@ril.com
- **Documentation**: Service-specific README files in each module
- **API Documentation**: Swagger UI available at `/swagger-ui.html` for each service
- **Issue Reporting**: Use repository issue tracker with detailed information

### Contributing
This is a government Digital Public Good. Contributions should follow:
1. Fork repository and create feature branch
2. Follow coding standards and write tests
3. Update documentation for changes
4. Ensure DPDP compliance is maintained
5. Submit pull request with clear description

### Code Review Process
- All changes require review and approval
- Automated quality gates must pass
- Security implications must be considered
- Documentation must be updated accordingly

---

## Legal & Compliance

### License
This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0) with additional terms specific to DPDP Act 2023 compliance.

### Government Digital Public Good
This system is developed as a Digital Public Good for the Government of India's DPDP Act 2023 compliance framework, organized by NeGD (National e-Governance Division), MeitY.

### Data Protection
The system implements comprehensive data protection measures in accordance with:
- Digital Personal Data Protection Act, 2023 (India)
- TRAI DLT regulations for SMS communications
- CERT-In security guidelines
- ISO 27001 security best practices

---

**This document is submitted only for Code for Consent Challenge, organised by NeGD (a part of MeitY) and shall not be used for any other purpose.**
