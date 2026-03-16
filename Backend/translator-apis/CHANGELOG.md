# Changelog

All notable changes to the Translator APIs Service will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial service implementation with multi-tenant translation support
- Integration with Bhashini translation API
- Translation configuration management (create, update, retrieve)
- Text translation with multiple input items support
- Automatic token generation and caching mechanism
- Numeral conversion between different language scripts
- Transaction tracking and audit trail
- Multi-tenant MongoDB database support
- Credential hierarchy (Business-level and Tenant-level)
- RESTful API with OpenAPI/Swagger documentation
- Health check endpoints via Spring Boot Actuator
- Comprehensive input validation and error handling
- CORS configuration for cross-origin requests

### Security
- Spring Security integration for API protection
- Secure credential storage in database
- Input validation on all endpoints
- Header validation for tenant and transaction tracking

## [0.0.1-SNAPSHOT] - 2024-01-01

### Added
- Initial release of Translator APIs Service
- Multi-tenant architecture with MongoDB
- Translation configuration management endpoints
- Text translation service with Bhashini integration
- Token caching for improved performance
- Numeral conversion service
- Transaction logging for audit purposes

### Changed
- Updated deployment configurations for Kubernetes
- Enhanced error handling and validation
- Improved logging configuration

### Fixed
- Fortify security fixes (commits: a1342ab, 6f1bc90)
- Deployment configuration updates
- Sonar project properties configuration

### Security
- Security vulnerability fixes identified by Fortify scans
- Enhanced input validation
- Secure error handling to prevent information leakage

---

## Change Categories

- **Added** - New features
- **Changed** - Changes in existing functionality
- **Deprecated** - Soon-to-be removed features
- **Removed** - Removed features
- **Fixed** - Bug fixes
- **Security** - Security improvements and vulnerability fixes

---

## Version History

| Version | Date | Description |
|---------|------|-------------|
| 0.0.1-SNAPSHOT | 2026-11-XX | Initial development version |

---

## Notes

- This service is part of the Digital Personal Data Protection (DPDP) Consent Management System
- All changes are tracked for compliance and audit purposes
- Breaking changes will be clearly marked in future releases
- Security fixes are prioritized and released as needed