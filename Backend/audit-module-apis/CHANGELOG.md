# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
- Implemented PDF generation improvements (Section 65B template).
- Integrate `PdfSignerService` to sign generated PDFs.
- Kafka producer to publish audit events (`audit-events`).
- Add multi-tenant header handling (`X-Tenant-Id`, `X-Business-Id`, `X-Transaction-Id`).
- Ongoing: API contract hardening and OpenAPI annotations.

## [0.1.0] - 2025-11-21
### Added
- Initial public implementation of `dpdp-audit-module`.
- REST endpoints to create and fetch audit records.
- PDF generation with Apache PDFBox (`SignPdfServiceImpl`).
- Kafka producer and configuration skeleton.
- Basic MongoDB repository layer.
### Fixed
- N/A
### Security
- N/A
