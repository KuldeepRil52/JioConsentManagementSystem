# Changelog

All notable changes to the Cookie Consent Scanner service will be documented in this file. The format loosely follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and semantic versioning once the service leaves `-SNAPSHOT` state.

## [Unreleased]

### Added
- Documentation refresh covering architecture, APIs, configuration, compliance policies, and contribution processes.
- GNU LGPL v3 licensing with DPDP-specific additional terms.

### Changed
- Clarified multi-tenant MongoDB configuration, security filters, and deployment instructions in `README.md`.

### Security
- Documented the rate-limit, vault signing, and detached JWS verification features that protect DPDP CMS traffic.

## [0.0.1-SNAPSHOT] - 2025-11-24

### Added
- Spring Boot 3.4.1, MongoDB, Playwright 1.45.0 based service skeleton (`pom.xml`).
- Comprehensive `ScanService` pipeline with subdomain-aware cookie capture, categorisation retries, tenant-aware persistence, and audit hooks.
- Consent handle, consent template, consent lifecycle, dashboard, and cookie category REST endpoints.
- External integrations: Audit service, Vault signing/verification/encryption, secure code API, notification API.
- Resilience features: `RateLimitInterceptor` (Bucket4j), Resilience4j circuit breakers for scan and categorisation services, multi-tenant Mongo filters.
- Docker and Kubernetes deployment assets plus Azure Pipelines CI definition.

### Fixed
- Upgraded embedded Tomcat, Logback, and `json-smart` library versions to remediate known CVEs (`pom.xml` overrides).

### Security
- Added request/response signing via `RequestResponseSignatureService`, strict error serialization, and hardened `application.properties` defaults (content negotiation, session cookies, security headers).


