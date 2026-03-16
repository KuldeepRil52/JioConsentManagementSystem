# Changelog

All notable changes to `grievance-module` will be documented here, following [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- Documentation refresh (README, governance files, LGPL license) aligned with DPDP CMS standards.
- Guidance for configuration, deployment, and compliance alignment.

### Changed
- No code changes yet; future fixes must be listed here before release tagging.

## [1.0.0] - 2025-11-24
### Added
- Initial public repository import of the DPDP CMS Grievance Service.
- Multi-tenant MongoDB persistence layer via `TenantAwareMongoTemplate`.
- REST APIs for grievances, templates, types, user types, and user details.
- Notification, audit, and Vault integrations for compliance and traceability.
- Azure Pipelines CI with Maven build plus Blackduck and Fortify scans.

### Security
- Spring Security baseline configuration with CSRF disabled (requires perimeter enforcement).

[1.0.0]: https://dev.azure.com/DPDP-Consent-Core/grievance-module/_git/grievance-module?version=GC1.0.0

