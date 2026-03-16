# Vault Service
 
## Overview
 
The Vault Service is a critical microservice within the Digital Personal Data Protection (DPDP) Consent Management System (CMS). This service provides secure cryptographic operations including encryption, decryption, digital signing, and verification capabilities for the DPDP CMS ecosystem. It acts as a secure gateway to HashiCorp Vault, managing cryptographic keys and operations while ensuring compliance with the DPDP Act, 2023 of India.
 
As part of the DPDP CMS, this service ensures that sensitive personal data and consent information are protected through industry-standard encryption and signing mechanisms, supporting the system's mission as a government Digital Public Good.
 
**Documentation**: See the [root README](../../README.md) for system overview and deployment.
 
## Table of Contents
 
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [API Documentation](#api-documentation)
- [Development Setup](#development-setup)
- [Building and Testing](#building-and-testing)
- [Deployment](#deployment)
- [Service Dependencies](#service-dependencies)
- [Security Considerations](#security-considerations)
- [Multi-tenancy Support](#multi-tenancy-support)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)
 
## Features
 
### Business Features
 
- **Client Certificate Onboarding**: Onboard and manage cryptographic certificates for tenants and businesses through Hashicorp vault
- **Data Encryption/Decryption**: Encrypt and decrypt sensitive data using AES-256-GCM encryption through Hashicorp vault
- **Payload Encryption with Reference IDs**: Encrypt JSON payloads and store them with reference IDs for later retrieval
- **JWT Signing and Verification**: Generate and verify JSON Web Tokens (JWT) for consent and grievance integrations. Signing and verify the signatures.
- **Multi-tenant Support**: Isolated cryptographic operations per tenant with tenant-specific databases
- **Key Management**: Automatic generation and management of RSA-4096 and AES-256 keys through HashiCorp Vault
 
### Technical Features
 
- **RESTful API**: Comprehensive REST API with OpenAPI/Swagger documentation
- **HashiCorp Vault Integration**: Secure integration with HashiCorp Vault for cryptographic operations
- **MongoDB Storage**: Tenant-isolated MongoDB databases for storing certificates and encrypted payloads
- **Request Validation**: Comprehensive header and body validation for all API endpoints
- **Error Handling**: Standardized error responses with specific error codes
- **Health Check Endpoint**: Service health monitoring and Vault connectivity checks
- **Spring Boot Framework**: Built on Spring Boot 3.5.7 for enterprise-grade reliability
 
## Architecture
 
### Service Type
 
The Vault Service is a **REST API microservice** that provides cryptographic operations to other services in the DPDP CMS ecosystem.
 
### Integration Points
 
- **HashiCorp Vault**: External service for cryptographic key management and operations
  - Transit key creation and management
  - Encryption/decryption operations
  - Digital signing operations
  - Health monitoring
 
- **MongoDB**: Database for storing:
  - Client public certificates (`ClientPublicCert` collection)
  - Encrypted payloads with metadata (`EncryptedPayload` collection)
  - Tenant-specific databases (format: `tenant_db_{tenantId}`)
 
### Service Communication
 
- **Inbound**: Receives REST API requests from other microservices in the DPDP CMS
- **Outbound**: Makes HTTP requests to HashiCorp Vault for cryptographic operations
 
### Database Schema
 
#### ClientPublicCert Collection
- `id`: Document ID
- `businessId`: Business identifier
- `tenantId`: Tenant identifier
- `keyId`: Unique key identifier (UUID)
- `publicKeyPem`: Public key in PEM format
- `certType`: Certificate type (default: "rsa-4096")
- `aesKey`: AES key identifier for encryption operations
 
#### EncryptedPayload Collection
- `id`: Document ID
- `uuid`: Reference ID for encrypted payload
- `tenantId`: Tenant identifier
- `businessId`: Business identifier
- `dataCategoryType`: Category of data (e.g., "CONSENT")
- `dataCategoryValue`: Category value identifier
- `encryptedString`: Encrypted payload string
- `createdTimeStamp`: Timestamp of creation
 
## Technology Stack
 
- **Java**: 17
- **Spring Boot**: 3.5.7
- **Spring Web**: REST API framework
- **Spring Data MongoDB**: MongoDB integration (reactive and non-reactive)
- **SpringDoc OpenAPI**: API documentation (Swagger UI)
- **Hibernate Validator**: Request validation
- **Lombok**: Code generation
- **iText PDF**: PDF processing (for future file signing capabilities)
- **MongoDB**: Document database
- **HashiCorp Vault**: Cryptographic operations backend
 
## Prerequisites
 
- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.6+ for building the project
- **MongoDB**: Version 4.4+ (accessible via connection string)
- **HashiCorp Vault**: Version 1.0+ with Transit secrets engine enabled
- **Network Access**: Connectivity to MongoDB and HashiCorp Vault instances
 
## Configuration
 
### Environment Variables
 
The service requires the following environment variables to be configured:
 
| Variable Name | Description | Required | Default Value | Security Sensitive |
|--------------|-------------|----------|---------------|-------------------|
| `SERVER_PORT` | Port number on which the service will run | Yes | - | No |
| `MONGODB_URI` | MongoDB connection string | Yes | - | Yes |
| `MONGODB_DATABASE` | Default MongoDB database name | Yes | - | No |
| `EXT_VAULT_URL` | HashiCorp Vault base URL | Yes | `http://<ip-or-host>:8200` | No |
| `ROOTKEY` | HashiCorp Vault authentication token | Yes | - | **Yes** |
 
### Application Properties
 
The service uses the following configuration properties (configurable via `application.properties`):
 
```properties
spring.application.name=vault
 
server.port=${SERVER_PORT}
 
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=${MONGODB_DATABASE}
 
vault.base-url=${EXT_VAULT_URL:http://<ip-or-host>:8200}
vault.encrypt-uri=/v1/transit/encrypt/
vault.decrypt-uri=/v1/transit/decrypt/
vault.sign-uri=/v1/transit/sign/test-key2/sha2-256
vault.verify-uri=/verify/test-key2
vault.token=${ROOTKEY:<HashiCorp-Root-Key>}
```
 
### Vault Configuration
 
The service integrates with HashiCorp Vault's Transit secrets engine. Ensure the following:
 
1. **Transit Engine**: The Transit secrets engine must be enabled at `/v1/transit/`
2. **Authentication**: The service uses token-based authentication
3. **Key Types Supported**:
   - RSA-4096 keys for signing operations
   - AES-256-GCM96 keys for encryption operations
 
### MongoDB Configuration
 
- **Connection**: MongoDB connection string must include authentication if required
- **Database Naming**: Tenant-specific databases are created dynamically as `tenant_db_{tenantId}`
- **Collections**:
  - `client_public_cert`: Stores client certificates
  - `encrypted_payload`: Stores encrypted payloads with reference IDs
 
## API Documentation
 
### Base URL
 
The service exposes REST APIs at the base path `/`. All client-related operations are under `/client`.
 
### Swagger UI
 
Interactive API documentation is available at:
- **Swagger UI**: `http://localhost:{SERVER_PORT}/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:{SERVER_PORT}/v3/api-docs`
 
### API Endpoints
 
#### Health Check
 
**GET** `/health`
 
Check service health and Vault connectivity.
 
**Response**: `200 OK`
```json
"Vault health okay"
```
 
---
 
#### Client Certificate Onboarding
 
**POST** `/client/key/onboard`
 
Onboard a new client certificate for a tenant and business combination.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier (UUID format)
- `Business-Id` (required): Business identifier (UUID format)
 
**Request Body** (optional):
```json
{
  "certType": "rsa-4096"
}
```
 
**Response**: `200 OK`
```json
{
  "tenantId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "businessId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "certType": "rsa-4096",
  "publicKeyPem": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
  "message": "The BusinessID has been onboarded Successfully"
}
```
 
**Error Responses**:
- `409 Conflict`: Certificate already exists for the tenant and business combination
 {
  "tenantId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "businessId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "certType": "rsa-4096",
  "publicKeyPem": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
  "message": " Certificate already exists for the tenant and business combination"
}
---
**Error Responses**:
400 Bad Request
 
#### Sign Payload
 
**POST** `/client/sign`
 
Sign a JSON payload and generate a JWT token.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
 
**Request Body**:
```json
{
  "payload": {
    "key": "value"
  }
}
```
 
**Response**: `200 OK`
```json
{
  "jwt": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
 
---
 
#### Verify JWT
 
**POST** `/client/verify`
 
Verify a signed JWT and extract the payload.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
 
**Request Body**:
```json
{
  "jwt": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
 
**Response**: `200 OK`
```json
{
  "valid": true,
  "payload": {
    "key": "value"
  }
}
```
 
**Error Responses**:
- `401 Unauthorized`: JWT validation failed or invalid signature
 
---
 
#### Get Key Information
 
**GET** `/client/getKey`
 
Retrieve key information (key ID and public key) for a tenant and business.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
 
**Response**: `200 OK`
```json
{
  "keyId": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "publicKeyPem": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
}
```
 
---
 
#### Encrypt Data
 
**POST** `/client/encrypt`
 
Encrypt base64-encoded data using AES encryption.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
 
**Request Body**:
```json
{
  "base64Text": "SGVsbG8gV29ybGQ="
}
```
 
**Response**: `200 OK`
```json
{
  "keyId": "aes-a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "ciphertext": "vault:v1:8SDd3WHDO5f..."
}
```
 
---
 
#### Decrypt Data
 
**POST** `/client/decrypt`
 
Decrypt ciphertext to retrieve the original base64-encoded data.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
 
**Request Body**:
```json
{
  "ciphertext": "vault:v1:8SDd3WHDO5f..."
}
```
 
**Response**: `200 OK`
```json
{
  "plaintext": "SGVsbG8gV29ybGQ=",
  "keyId": "a1b2c3d4-e5f6-7890-1234-567890abcdef"
}
```
 
---
 
#### Encrypt Payload
 
**POST** `/client/encryptPayload`
 
Encrypt a JSON payload and store it with a reference ID for later retrieval.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
- `Data-Category-Type` (required): Category of data (e.g., "consent")
- `Data-Category-Value` (required): Category value identifier
 
**Request Body** (JSON):
```json
{
  "string": "sample",
  "object": {
    "anynumber": 1234,
    "something": {
      "another string": "abcd"
    }
  }
}
```
 
**Response**: `200 OK`
```json
{
  "tenantId": "01d86a53-15ea-4952-be12-8f7a77c3a72c",
  "businessId": "01d86a53-15ea-4952-be12-8f7a77c3a72c",
  "dataCategoryType": "consent",
  "dataCategoryValue": "a1b2c3d4-e5f6-7890-1234-567890abcdef",
  "referenceId": "a35d3fac-c5a7-4c8c-a95c-a174e3f81b24",
  "encryptedString": "vault:v1:HP9gY7cWoHlFrv5YZAzVK2SCeODjLbsI6Pns+JQGHgJXtFD1tZ6mnyeVFonWGFBICelBtcRrYVSblXAKvfZr9I9ZiM22Q0tz51lVsLv40XZHZC3xJYD0oM3aHX+EHEw6SMWhAmWss26ilFdF+ZnXn/0Y0q/8gMXuzembCL4TKOZWBo3IVlgc20FoFLexrkBwU/vuKvpQtZAXUWUwJCeL4u14bpln4dW4ubRsO+Db8CoS8shef/uZlmN0zheo0uA/e6nky00c7Ecii+NBFvSO9rDFUTRXMlFWWPFcC8cC/DiOi2b+zYt1R1M+d56nnp+Bqxq1mv+2jLxDmiRHwxsAekOlm1YqsaSDtb2udW0ZpBUuVZZZy9tVI9vb+IyVeWws9wHARfa42rNJAjBPiHXtsdrpdQZaTuP4Cw0wKbrUKgTmzVF8zwtg4y5QzTdV5HyScki2KUpXARNLWwb52Va/uHY94brPmU/h43L+o69LHUS1lMiMTPTSg6vuO5v6hqRwMTmOdz8DZNxDTR+FCALLAcpNhg0L3BP7wB1X7lyxXYKlHcW/+5PT2lYSFOExZ7wDXS/hBIWPpf2rIpm6LS0GI2eDalmhEby6qZUj7LVvBzcH3isObXxyTWTVaz8+TSQlukjFz2qrsrkknHnqR8jsVXw+tCHnwn1zcM9/WRqcDDE=",
  "createdTimeStamp": "2025-11-04T20:14:09.253588800"
}
```
 
---
 
#### Decrypt Payload
 
**GET** `/client/decryptedPayload`
 
Retrieve and decrypt a stored payload using its reference ID.
 
**Headers**:
- `Tenant-Id` (required): Tenant identifier
- `Business-Id` (required): Business identifier
- `Reference-Id` (required): Reference ID of the encrypted payload
 
**Response**: `200 OK`
```json
{
  "referenceId": "a35d3fac-c5a7-4c8c-a95c-a174e3f81b24",
  "decryptedPayload": {
    "string": "sample",
    "object": {
      "anynumber": 1234,
      "something": {
        "another string": "abcd"
      }
    }
  }
}
```
 
**Error Responses**:
- `500 Internal Server Error`: Payload not found for the given reference ID
 
---
 
### Error Responses
 
All endpoints return standardized error responses in the following format:
 
```json
{
  "errorCode": "NV400",
  "errorMessage": "Invalid request, check headers & body"
}
```
 
#### Error Codes
 
| Error Code | Description |
|------------|-------------|
| `NV400` | Invalid request, check headers & body |
| `NV404` | Resource not found |
| `NV500` | Internal server error |
| `NVK404` | The Key does not exist |
| `NVK405` | The key does not match with Tenant-ID & Business-ID |
| `NVEn500` | Encryption Failed |
| `NVDe500` | Decryption Failed |
| `NVJWT400` | JWT validation failed |
| `NVT400` | Tenant ID Header is empty or not present |
| `NVB400` | Business ID Header is empty or not present |
| `NVK400` | Key ID Header is empty or not present |
| `NVC400` | Data category header is empty or not present |
 
## Development Setup
 
### Local Development
 
1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd vault-apis
   ```
 
2. **Set environment variables**:
   ```bash
   export SERVER_PORT=8080
   export MONGODB_URI=mongodb://localhost:27017
   export MONGODB_DATABASE=vault_db
   export EXT_VAULT_URL=http://localhost:8200
   export ROOTKEY=your-vault-token
   ```
 
3. **Build the project**:
   ```bash
   mvn clean install
   ```
 
4. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```
 
   Or run the JAR directly:
   ```bash
   java -jar target/vault-v1.0.jar
   ```
 
5. **Access Swagger UI**:
   Open your browser and navigate to:
   ```
   http://localhost:8080/swagger-ui.html
   ```
 
### Project Structure
 
```
vault-apis/
├── src/
│   ├── main/
│   │   ├── java/com/jio/vault/
│   │   │   ├── client/              # HashiCorp Vault client
│   │   │   ├── config/               # Configuration classes
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   └── VaultProperties.java
│   │   │   ├── constants/            # Constants and enums
│   │   │   │   ├── CollectionConstants.java
│   │   │   │   ├── ErrorCode.java
│   │   │   │   └── HeaderConstants.java
│   │   │   ├── controller/           # REST controllers
│   │   │   │   ├── ClientController.java
│   │   │   │   ├── FileSignController.java
│   │   │   │   └── VaultController.java
│   │   │   ├── documents/            # MongoDB document models
│   │   │   │   ├── ClientPublicCert.java
│   │   │   │   └── EncryptedPayload.java
│   │   │   ├── dto/                  # Data Transfer Objects
│   │   │   │   ├── cryptodto/
│   │   │   │   └── error/
│   │   │   ├── exception/            # Exception handling
│   │   │   │   ├── CustomException.java
│   │   │   │   └── GlobalExceptionHandler.java
│   │   │   ├── repository/           # Data access layer
│   │   │   │   ├── mongotemplate/
│   │   │   │   └── *RepositoryCustom.java
│   │   │   ├── service/              # Business logic
│   │   │   │   ├── PayloadCryptoService.java
│   │   │   │   ├── TenantValidationService.java
│   │   │   │   └── VaultService.java
│   │   │   ├── util/                 # Utility classes
│   │   │   │   ├── FileSigner.java
│   │   │   │   ├── JwtGenerator.java
│   │   │   │   └── JwtVerifierUtil.java
│   │   │   ├── validation/           # Validation classes
│   │   │   │   ├── ValidateBody.java
│   │   │   │   └── ValidateHeader.java
│   │   │   └── NegdApplication.java  # Main application class
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── applogconfig.xml
│   │       ├── auditlogconfig.xml
│   │       └── logback-spring.xml
│   └── test/
│       └── java/com/jio/negd/
│           └── NegdApplicationTests.java
├── deployment/
│   ├── Dockerfile
│   └── *.yaml                        # Kubernetes deployment files
├── pom.xml
├── README.md
└── azure-pipelines.yml
```
 
## Building and Testing
 
### Building the Project
 
```bash
# Clean and build
mvn clean install
 
# Skip tests during build
mvn clean install -DskipTests
 
# Build with specific profile
mvn clean install -P<profile-name>
```
 
 
# Run with coverage
mvn test jacoco:report
```
 

 
## Deployment
 
### Docker Deployment
 
The service includes a Dockerfile for containerized deployment:
 
```bash
# Build Docker image
docker build -f deployment/Dockerfile -t vault-service:v1.0 .
 
# Run container
docker run -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e MONGODB_URI=mongodb://mongodb:27017 \
  -e MONGODB_DATABASE=vault_db \
  -e EXT_VAULT_URL=http://vault:8200 \
  -e ROOTKEY=your-vault-token \
  vault-service:v1.0
```
 
### Kubernetes Deployment
 
Kubernetes deployment manifests are available in the `deployment/` directory:
- `deployment.yaml`: Production deployment configuration
- `deployment-sit.yaml`: SIT environment configuration
- `Public_deployment.yaml`: Public deployment configuration
 
### CI/CD
 
The project includes Azure Pipelines configuration (`azure-pipelines.yml`) for automated builds and deployments.
 
## Service Dependencies
 
### Required Services
 
1. **HashiCorp Vault**
   - **Purpose**: Cryptographic operations backend
   - **Version**: 1.0+
   - **Endpoints Used**:
     - `/v1/transit/keys/{keyName}` - Key creation
     - `/v1/transit/encrypt/{keyName}` - Encryption
     - `/v1/transit/decrypt/{keyName}` - Decryption
     - `/v1/transit/sign/{keyName}/sha2-256` - Signing
     - `/v1/sys/health` - Health check
   - **Authentication**: Token-based (X-Vault-Token header)
 
2. **MongoDB**
   - **Purpose**: Certificate and encrypted payload storage
   - **Version**: 4.4+
   - **Database Pattern**: `tenant_db_{tenantId}` (dynamic per tenant)
   - **Collections**: `client_public_cert`, `encrypted_payload`
 
### Optional Dependencies
 
- **Service Discovery**: If deployed in a service mesh environment
- **API Gateway**: For external API exposure
- **Monitoring**: Prometheus, Grafana, or similar for metrics
 
## Security Considerations
 
### Data Protection
 
- **Encryption at Rest**: All encrypted payloads stored in MongoDB are encrypted using AES-256-GCM
- **Encryption in Transit**: Use TLS/HTTPS for all API communications
- **Key Management**: All cryptographic keys are managed by HashiCorp Vault, never stored in application code
- **Token Security**: Vault tokens must be stored securely (use secrets management, never commit to version control)
 
### Validations

- **Header Validation**: All endpoints require `Tenant-Id` and `Business-Id` headers
- **Tenant Isolation**: Each tenant's data is stored in separate databases
- **Business Validation**: Business IDs are validated against tenant context
 
### Compliance
 
- **DPDP Act Compliance**: The service supports data protection requirements under the DPDP Act, 2023
- **Audit Logging**: All cryptographic operations should be logged for audit purposes using partenr portal APIs
- **Data Retention**: Encrypted payloads are stored with timestamps for compliance tracking
 
### Security Best Practices
 
1. **Never expose Vault tokens** in logs or error messages
2. **Use strong, unique tokens** for each environment
3. **Rotate Vault tokens** regularly
4. **Enable TLS** for all Vault communications
5. **Implement rate limiting** on API endpoints
6. **Monitor for suspicious activity** in cryptographic operations
7. **Regular security audits** of key management practices
 
## Multi-tenancy Support
 
The service implements multi-tenancy through:
 
1. **Tenant-Specific Databases**: Each tenant has an isolated MongoDB database (`tenant_db_{tenantId}`)
2. **Tenant Validation**: All operations validate tenant and business IDs
3. **Key Isolation**: Cryptographic keys are scoped to tenant and business combinations
4. **Header Requirements**: All API requests must include `Tenant-Id` and `Business-Id` headers
 
### Tenant Onboarding
 
When a new tenant is onboarded:
1. A tenant-specific database is created automatically on first operation
2. Client certificates are created per business within the tenant
3. Keys are generated in HashiCorp Vault with unique identifiers
 
## Troubleshooting
 
### Common Issues
 
#### Vault Connection Errors
 
**Problem**: Service cannot connect to HashiCorp Vault
 
**Solutions**:
- Verify `EXT_VAULT_URL` environment variable is correct
- Check network connectivity to Vault instance
- Verify `ROOTKEY` token is valid and has required permissions
- Check Vault health: `curl http://vault-url/v1/sys/health`
 
#### MongoDB Connection Errors
 
**Problem**: Service cannot connect to MongoDB
 
**Solutions**:
- Verify `MONGODB_URI` is correct and accessible
- Check MongoDB authentication credentials
- Ensure MongoDB instance is running and accessible
- Verify network connectivity
 
#### Certificate Not Found Errors
 
**Problem**: `NVK404` - Key does not exist
 
**Solutions**:
- Ensure client certificate is onboarded using `/client/key/onboard`
- Verify `Tenant-Id` and `Business-Id` headers match the onboarded certificate
- Check that the tenant database exists and contains the certificate
 
#### Encryption/Decryption Failures
 
**Problem**: `NVEn500` or `NVDe500` errors
 
**Solutions**:
- Verify the key exists in HashiCorp Vault
- Check that the ciphertext format is correct (must start with `vault:v1:`)
- Ensure the key used for encryption matches the key used for decryption
- Verify Vault token has permissions for transit operations
 
### Logging
 
The service uses Logback for logging. Log files are configured in:
- `src/main/resources/logback-spring.xml`
- `src/main/resources/applogconfig.xml`
- `src/main/resources/auditlogconfig.xml`
 
Logs are written to `/opt/logs` in containerized deployments (configurable via `-Dlog_path`).
 
### Health Check
 
Use the `/health` endpoint to verify:
- Service is running
- Vault connectivity
- Basic service health
 
```bash
curl http://localhost:8080/health
```
 
## Contributing
 
Please refer to the [CONTRIBUTING.md](CONTRIBUTING.md) file for guidelines on contributing to this project.
 
### Development Guidelines
 
1. Follow Java coding standards and Spring Boot best practices
2. Write unit tests for new features
3. Update API documentation (Swagger annotations) for new endpoints
4. Ensure all tests pass before submitting pull requests
5. Follow the existing code structure and naming conventions
 
## License
 
This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0) with additional terms specific to the DPDP CMS. See the [LICENSE](LICENSE) file for details.
 
---
 
**Copyright © 2025 National e-Governance Division (NeGD), Ministry of Electronics and Information Technology, Government of India**
 
For support and inquiries, please contact:
- **Email**: support@dpdp-cms.gov.in
- **Documentation**: https://docs.dpdp-cms.gov.in
- **Security Issues**: security@dpdp-cms.gov.in
 