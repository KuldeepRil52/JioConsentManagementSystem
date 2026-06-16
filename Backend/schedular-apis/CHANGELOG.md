# CHANGELOG

All notable changes to this project will be documented in this file. This project adheres to Semantic Versioning.

## [Unreleased]

### Added
- Multiple retention and expiry scheduled jobs (consent-handles, consent-preference reminders, cookie consent, artifact retention, logs/data retention).
- Integration hooks for `notification`, `audit`, and `vault` modules.
- `stats` REST endpoints to expose scheduler run history and metrics.

### Changed
- Configuration improvements: added per-job cron/batch/thread-pool environment properties and defaults.
- Separated enums from DTOs to improve maintainability.

### Fixed
- Several bug fixes and vulnerability mitigations in `pom.xml` and dependency updates.
- Fixes for notification payloads and chaining/hashing code in consent expiry flows.

---

## [0.0.1] - 2025-10-27

### Added
- Initial public release (baseline) with core scheduled jobs for consent expiry and retention, and basic REST metrics endpoints.

---