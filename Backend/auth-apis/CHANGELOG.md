# Changelog

All notable changes to the JCMS Auth APIs will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2025-11-26

### Added

- RSA JWK-based JWT issuance with public key endpoint for downstream service verification
- Multi-tenant token support with tenant-scoped claims and database-per-tenant isolation
- DPDP Act 2023 compliance controls including consent-linked token metadata and audit trail
- Spring Security integration for request authentication, authorization, and secure-code flows
- Audit event publishing to Kafka for all authentication and token lifecycle events
- HashiCorp Vault integration for RSA key signing of issued JWT tokens
