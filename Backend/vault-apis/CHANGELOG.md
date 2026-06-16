# Changelog

All notable changes to the JCMS Vault APIs will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2025-11-26

### Added

- HashiCorp Vault integration for RSA key management including key creation, rotation, and deletion
- Digital signature generation for consent artifacts using RSA private keys stored in Vault
- PKCS#8/X.509 key lifecycle management for signing and verification operations
- MongoDB-backed key metadata storage for key versioning and tenant association
- Multi-tenant key isolation ensuring each tenant's cryptographic material is independently managed
