# Auth Service
 
## Overview
 
The Auth Service is a critical microservice within the Digital Personal Data Protection (DPDP) Consent Management System (CMS). This service provides comprehensive authentication and authorization capabilities, managing JWT token generation, validation, revocation, and session management for the DPDP CMS ecosystem.
 
As part of a government Digital Public Good designed to ensure compliance with the DPDP Act, 2023 of India, the Auth Service ensures secure access control and maintains audit trails for all authentication operations.

## Prerequisites

Auth Handler assumes that the DB contains the private key (JWK) for signing in the below format in `cms_db_admin` in `auth_key` collection:
{
    "_id" : ObjectId("690f38b2ce1641694e5bea5a"),
    "p" : <valueOfP>,
    "kty" : "RSA",
    "q" : <valueOfQ>,
    "d" : <valueOfD>,
    "e" : "AQAB",
    "use" : "sig",
    "qi" : <valueOfQI>,
    "dp" : <valueOfDQ>,
    "alg" : "RS256",
    "dq" : <valueOfDQ>,
    "n" : <valueOfN>
}


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
 
### Core Authentication Features
 
- **JWT Token Generation**: Generate RS256-signed JWT tokens with configurable expiration
- **Token Validation**: Validate JWT tokens and verify their integrity
- **Token Introspection**: OAuth2-compliant token introspection endpoint
- **Token Revocation**: Secure token revocation with persistent storage
- **Session Management**: Multi-session support with configurable limits (max 3 active sessions per user)
- **Secure Code Management**: Static token-based session handling for secure code workflows
- **WSO2 Integration**: Integration with WSO2 API manager to provide to securely access Backend APIs
- **Request/Response Signing**: JWS signature verification for request/response integrity
 
### Security Features
 
- **Multi-tenant Isolation**: Tenant-specific database isolation
- **Revoked Token Tracking**: Persistent storage of revoked tokens with TTL
- **IP Whitelisting**: Configurable IP whitelisting for enhanced security so that only partner portal can call it
- **Audit Logging**: Comprehensive audit trail for all authentication operations
 
### Operational Features
 
- **Health Monitoring**: Spring Boot Actuator endpoints for health checks
- **Scheduled Tasks**: Automatic retry mechanism for failed audit logs
- **Error Handling**: Comprehensive error codes and messages
- **Swagger/OpenAPI**: Interactive API documentation
 
## Architecture
 
### Service Type
 
The Auth Service is a **REST API** microservice that provides authentication and authorization capabilities. It does not use Kafka messaging but integrates with other services via HTTP REST calls.
 
### Integration Points
 
- **Audit Service**: Sends audit logs via HTTP POST to the audit service endpoint
- **WSO2 API Manager**: Fetches OAuth2 access tokens for business applications
- **MongoDB**: Multi-tenant database storage with tenant-specific databases
- **Redis** (Optional): Can be configured for token caching instead of in-memory cache
 
### Database Architecture
 
The service uses MongoDB with a multi-tenant architecture:
 
- **Admin Database**: `cms_db_admin` - Stores tenant configuration, business keys, and WSO2 application credentials
- **Tenant Databases**: `tenant_db_{tenantId}` - Each tenant has an isolated database containing:
  - User sessions
  - Active tokens
  - Revoked tokens
  - Auth session managers
  - User records
 
### Key Components
 
1. **Controllers**:
   - `AuthController`: JWT token operations (generate, validate, introspect, revoke)
   - `SecureCodeController`: Secure code session management
   - `Wso2TokenController`: WSO2 token retrieval
 
2. **Services**:
   - `TokenService`: Core JWT token operations
   - `AuthSessionService`: Advanced session management with refresh tokens
   - `SessionService`: Secure code session management
   - `Wso2TokenManagerService`: WSO2 token caching and management
   - `AuditService`: Audit log submission
 
3. **Repositories**: MongoDB repositories for data persistence
 
 
## Technology Stack
 
- **Java**: 21
- **Spring Boot**: 3.5.7
- **Spring Data MongoDB**: For database operations
- **Spring Security Crypto**: For BCrypt hashing
- **JJWT**: 0.11.5 for JWT operations
- **Lombok**: For reducing boilerplate code
- **SpringDoc OpenAPI**: 2.8.11 for API documentation
- **Maven**: Build tool
 
## Prerequisites
 
- Java 21 or higher
- Maven 3.6+
- MongoDB 4.4+ (with multi-tenant database support)
- Redis (optional, for distributed caching)
- WSO2 API Manager (for OAuth2 token management for API access)
 
## Configuration
 
### Environment Variables
 
The service requires the following environment variables for proper operation:
 
| Variable | Description | Required | Default | Security Sensitive |
|----------|-------------|----------|---------|-------------------|
| `SERVER_PORT` | Port on which the service runs | Yes | - | No |
| `MONGODB_URI` | MongoDB connection string | Yes | - | Yes |
| `MONGODB_DATABASE` | Admin database name | Yes | `cms_db_admin` | No |
| `WSO2_BASE_URL` | Base URL for WSO2 API Manager| No | `https://<ip-or-host>:9443` | No |
| `AUDIT_URL` | Audit service endpoint URL | No | `<audit-base-url>/audit/v1/audit` | No |
| `APP_SIGN` | Enable response signing | No | `false` | No |
| `APP_VERIFY` | Enable request verification | No | `false` | No |
 
### Application Properties
 
Key configuration properties in `application.properties`:
 
```properties
# Service Configuration
spring.application.name=auth
server.port=${SERVER_PORT}
 
# MongoDB Configuration
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=${MONGODB_DATABASE}
 
# Token Cache Configuration
token.cache.type=inMemory
token.cache.maximum-size=10001
token.cache.expire-after-minutes=60
 
# Token Expiry (milliseconds)
token.expiry=3600000
 
# Session Configuration
token.max.sessions=1
securecode.expiry=60
 
# WSO2 Configuration
wso2.token.url=${WSO2_BASE_URL}/oauth2/token
 
# Audit Configuration
audit.url=${AUDIT_URL}
 
# Security Configuration
whitelistips.file.path=whitelisted-ips.json
whitelistips.check=false
app.sign.response=${APP_SIGN:false}
app.verify.request=${APP_VERIFY:false}
```
 
## API Documentation
 
### Base URL
 
```
http://localhost:${SERVER_PORT}
```
 
### Swagger UI
 
Interactive API documentation is available at:
```
http://localhost:${SERVER_PORT}/swagger-ui.html
```
 
### API Endpoints
 
#### OAuth2 Token Operations (`/oauth2`)
 
##### Generate JWT Token
- **Endpoint**: `POST /oauth2/token`
- **Description**: Generates a new JWT token with the provided claims
- **Headers**:
  - `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "tenantId": "string",
    "businessId": "string",
    "iss": "string",
    "sub": "string",
    "scope": "string",
    "client_id": "string",
    "username": "string",
    "token_type": "string"
  }
  ```
- **Response**:
  ```json
  {
    "accessToken": "string",
    "expiry": 1234567890
  }
  ```
- **Response Headers**:
  - `tenant-id`: Tenant identifier
  - `business-id`: Business identifier
 
##### Validate Token
- **Endpoint**: `POST /oauth2/validate`
- **Description**: Validates a JWT token and returns WSO2 access token
- **Headers**:
  - `Content-Type: application/x-www-form-urlencoded`
- **Request Parameters**:
  - `token`: JWT token to validate
- **Response**:
  ```json
  {
    "active": true,
    "scope": "string",
    "clientId": "string",
    "username": "string",
    "tokenType": "string",
    "exp": 1234567890,
    "iat": 1234567890,
    "sub": "string",
    "aud": "string",
    "iss": "string",
    "accessToken": "string"
  }
  ```
- **Response Headers**:
  - `tenant-id`: Tenant identifier
  - `business-id`: Business identifier
 
##### Introspect Token
- **Endpoint**: `POST /oauth2/introspect`
- **Description**: OAuth2-compliant token introspection
- **Headers**:
  - `Content-Type: application/x-www-form-urlencoded`
- **Request Parameters**:
  - `token`: JWT token to introspect
- **Response**:
  ```json
  {
    "active": true,
    "scope": "string",
    "clientId": "string",
    "username": "string",
    "tokenType": "string",
    "exp": 1234567890,
    "iat": 1234567890,
    "sub": "string",
    "aud": "string",
    "iss": "string"
  }
  ```
 
##### Revoke Token
- **Endpoint**: `POST /oauth2/revoke`
- **Description**: Revokes a JWT token
- **Headers**:
  - `x-session-token`: JWT token to revoke
- **Response**:
  ```json
  {
    "token": "string",
    "revoked": true
  }
  ```
 
#### Secure Code Operations (`/secureCode`)
 
##### Create Session
- **Endpoint**: `POST /secureCode/create`
- **Description**: Creates a new secure code session
- **Headers**:
  - `x-tenant-id`: Tenant identifier
  - `x-business-id`: Business identifier
  - `Content-Type: application/json`
- **Request Body**:
  ```json
  {
    "identity": "string",
    "identityType": "string"
  }
  ```
- **Response**:
  ```json
  {
    "secureCode": "string",
    "identity": "string",
    "expiry": 1234567890
  }
  ```
- **Response Headers**:
  - `x-jws-signature`: JWS signature of the response
 
##### Validate Session
- **Endpoint**: `POST /secureCode/validate`
- **Description**: Validates a secure code session
- **Headers**:
  - `x-access-token`: Secure code
  - `x-tenant-id`: Tenant identifier
  - `x-business-id`: Business identifier
  - `x-identity`: Identity value
- **Response**:
  ```json
  {
    "active": true,
    "tenantId": "string",
    "businessId": "string",
    "identity": "string",
    "identityType": "string",
    "accessToken": "string"
  }
  ```
 
##### Check Access Token
- **Endpoint**: `GET /secureCode/validate`
- **Description**: Validates access token only
- **Headers**:
  - `x-access-token`: Secure code
- **Response**:
  ```json
  {
    "active": true,
    "tenantId": "string",
    "businessId": "string",
    "identity": "string",
    "identityType": "string",
    "accessToken": "string"
  }
  ```
- **Response Headers**:
  - `tenant-id`: Tenant identifier
  - `business-id`: Business identifier
 
##### Validate Tenant Token
- **Endpoint**: `GET /secureCode/tenant/validate`
- **Description**: Validates tenant secret code with identity value
- **Headers**:
  - `x-secret-code`: Secret code
  - `x-identity-value`: Identity value
- **Response**:
  ```json
  {
    "active": true,
    "identityValue": "string"
  }
  ```
 
#### WSO2 Token Operations (`/api/token`)
 
##### Get Access Token
- **Endpoint**: `GET /api/token`
- **Description**: Retrieves a valid WSO2 access token for the tenant/business
- **Headers**:
  - `tenantId`: Tenant identifier
  - `businessId`: Business identifier
- **Response**: WSO2 access token (string)
 
### Error Responses
 
All endpoints return standard error responses:
 
```json
{
  "errorCode": "AUTH400",
  "message": "Invalid request, check headers & body",
  "timestamp": "2024-01-01T00:00:00Z"
}
```
 
Common error codes:
- `AUTH400`: Invalid request
- `AUTH401`: Unauthorized
- `AUTH404`: Resource not found
- `AUTH415`: Unsupported Media Type
- `AUTH500`: Internal server error
- `AUTHDB500`: Database error
 
## Development Setup
 
### Local Development
 
1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd auth-apis
   ```
 
2. **Set up MongoDB**:
   - Ensure MongoDB is running
   - Create the admin database: `cms_db_admin`
   - Configure connection string in environment variables
 
3. **Configure environment variables**:
   ```bash
   export SERVER_PORT=9009
   export MONGODB_URI=mongodb://localhost:27017
   export MONGODB_DATABASE=cms_db_admin
   export WSO2_BASE_URL=https://your-wso2-server:9443
   export AUDIT_URL=http://audit-service:30006/audit/v1/audit
   ```
 
4. **Prepare key files**:
   - Place RSA private key in `src/main/resources/private-key.json`
   - Place public certificate in `src/main/resources/public-cert.json`
 
5. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```
 
   Or using Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
 
### Directory Structure
 
```
auth-apis/
├── src/
│   ├── main/
│   │   ├── java/com/jio/auth/
│   │   │   ├── controller/          # REST controllers
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # MongoDB repositories
│   │   │   ├── model/               # Data models
│   │   │   ├── dto/                 # Data transfer objects
│   │   │   ├── config/              # Configuration classes
│   │   │   ├── cache/               # Caching implementations
│   │   │   ├── token/               # Token storage
│   │   │   ├── filter/              # Request filters
│   │   │   ├── exception/           # Exception handling
│   │   │   ├── validation/          # Validation logic
│   │   │   └── constants/           # Constants
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── private-key.json
│   │       ├── public-cert.json
│   │       └── whitelisted-ips.json
│   └── test/                        # Test files
├── deployment/
│   ├── Dockerfile
│   └── jio-dl-deployment.yaml
├── pom.xml
└── README.md
```
 
## Building and Testing
 
### Build the Application
 
```bash
mvn clean package
```
 
This will:
- Compile the source code
- Run unit tests
- Package the application as a JAR file in `target/` directory
 
### Run Tests
 
```bash
mvn test
```
 
### Run Specific Test Class
 
```bash
mvn test -Dtest=AuthApplicationTests
```
 
### Build Docker Image
 
```bash
docker build -f deployment/Dockerfile -t auth-apis:latest .
```

 
```bash
mvn clean test jacoco:report
```
 
Coverage reports will be available in `target/site/jacoco/index.html`.
 
## Deployment
 
### Docker Deployment
 
1. **Build the Docker image**:
   ```bash
   docker build -f deployment/Dockerfile -t auth-apis:latest .
   ```
 
2. **Run the container**:
   ```bash
   docker run -d \
     -p 9009:9009 \
     -e SERVER_PORT=9009 \
     -e MONGODB_URI=mongodb://mongodb:27017 \
     -e MONGODB_DATABASE=cms_db_admin \
     -e WSO2_BASE_URL=https://wso2-server:9443 \
     -e AUDIT_URL=http://audit-service:30006/audit/v1/audit \
     auth-apis:latest
   ```
 
### Kubernetes Deployment
 
The service includes Kubernetes deployment manifests in `deployment/jio-dl-deployment.yaml`.
 
1. **Apply the deployment**:
   ```bash
   kubectl apply -f deployment/jio-dl-deployment.yaml
   ```
 
2. **Create MongoDB secret**:
   ```bash
   kubectl create secret generic mongodb-secret \
     --from-literal=uri=mongodb://mongodb:27017
   ```
 
3. **Verify deployment**:
   ```bash
   kubectl get pods -l app=auth-apis
   kubectl get svc auth-apis
   ```
 
### Environment-Specific Configuration
 
- **Development**: Use local MongoDB and WSO2 instances
- **SIT**: Configure SIT environment URLs in ConfigMap
- **Production**: Use production MongoDB cluster and WSO2 server
 
## Service Dependencies
 
### Required Services
 
1. **MongoDB**:
   - Version: 4.4+
   - Purpose: Multi-tenant data storage
   - Connection: Configured via `MONGODB_URI`
 
2. **WSO2 API Manger**:
   - Purpose: OAuth2 token provider for business applications to access backend APIs
   - Endpoint: Configured via `WSO2_BASE_URL`
   - Required for: Business application token management
 
### Optional Services
 
1. **Audit Service**:
   - Purpose: Audit log storage
   - Endpoint: Configured via `AUDIT_URL`
   - Fallback: Failed audits are stored locally and retried
 
 
## Security Considerations
 
### Data Protection
 
- **Token Storage**: Tokens are hashed using BCrypt before storage
- **Private Keys**: RSA private keys must be securely stored and not committed to version control
- **Database Encryption**: Ensure MongoDB connection uses TLS/SSL in production
- **Network Security**: Use VPN or private networks for service communication
 
### Authentication
 
- **JWT Signing**: RS256 algorithm for token signing
- **Token Expiration**: Configurable token expiration (default: 1 hour)
- **Revocation**: Revoked tokens are tracked and cannot be reused
- **Session Limits**: Maximum 3 active sessions per user
 
### Audit and Compliance
 
- **Audit Logging**: All authentication operations are logged
- **Failed Audit Retry**: Automatic retry mechanism for failed audit submissions
- **DPDP Compliance**: Service maintains audit trails required for DPDP Act compliance
 
### Best Practices
 
1. **Key Management**: Rotate RSA keys regularly
2. **Token Expiry**: Use appropriate token expiration times
3. **IP Whitelisting**: Enable IP whitelisting in production
4. **HTTPS**: Always use HTTPS in production
5. **Secrets Management**: Use Kubernetes secrets or external secret management
 
## Multi-tenancy Support
 
The service implements multi-tenant architecture with complete data isolation:
 
### Tenant Isolation
 
- **Database Level**: Each tenant has a separate MongoDB database (`tenant_db_{tenantId}`)
- **Header-Based**: Tenant identification via `x-tenant-id` header
- **Business Context**: Business identification via `x-business-id` header
 
### Tenant Configuration
 
Tenant and business verification is performed against the admin database:
- Tenant records in `BusinessKey` collection
- Business application records in `Wso2BusinessApplication` collection
 
### Data Models
 
- **Admin Database**: Tenant configuration, business keys, WSO2 applications
- **Tenant Databases**: User sessions, tokens, auth sessions, user records
 
## Troubleshooting
 
### Common Issues
 
#### 1. MongoDB Connection Error
 
**Error**: `MongoSocketException: Unable to connect to server`
 
**Solution**:
- Verify MongoDB is running
- Check `MONGODB_URI` environment variable
- Ensure network connectivity
- Verify MongoDB credentials
 
#### 2. Token Validation Fails
 
**Error**: `Invalid token signature`
 
**Solution**:
- Verify public key is correctly configured
- Ensure private/public key pair match
- Check token expiration
- Verify token format
 
#### 3. WSO2 Token Fetch Fails
 
**Error**: `Failed to fetch WSO2 token`
 
**Solution**:
- Verify WSO2 server is accessible
- Check WSO2 credentials in database
- Verify business application configuration
- Check network connectivity
 
#### 4. Audit Service Unavailable
 
**Error**: `Failed to send audit`
 
**Solution**:
- Audit failures are stored locally and retried automatically
- Check audit service endpoint URL
- Verify network connectivity
- Check audit service logs
 
### Logging
 
Logs are written to:
- **Application Logs**: `/opt/logs` (in container)
- **Audit Logs**: Separate audit logger configured in `logback-spring.xml`
 
### Health Checks
 
The service exposes Spring Boot Actuator endpoints:
- Health: `GET /actuator/health`
- Info: `GET /actuator/info`
 
## Contributing
 
Please refer to the [CONTRIBUTING.md](CONTRIBUTING.md) file for guidelines on contributing to this project.
 
### Code Style
 
- Follow Java coding conventions
- Use Lombok to reduce boilerplate
- Write unit tests for new features
- Update documentation for API changes
 
## License
 
This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0). See the [LICENSE](LICENSE) file for details.
 
## Contact and Support
 
For issues, questions, or contributions:
 
- **Repository**: [Git Repository URL]
- **Documentation**: [Documentation Site URL]
- **Security Issues**: [Security Contact Email]
 
---
 
**Note**: This service is part of the DPDP Consent Management System, a Digital Public Good designed to ensure compliance with the DPDP Act, 2023 of India. All authentication operations are logged for audit and compliance purposes.
 
 