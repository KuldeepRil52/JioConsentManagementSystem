# Translator APIs Service

> Multi-tenant microservice providing translation capabilities for the Digital Personal Data Protection (DPDP) Consent Management System

## Overview

The **Translator APIs Service** is a multi-tenant microservice that provides fast and reliable language translation services across the Jio Consent Management System (JCMS). It enables multilingual support for different modules such as Consent Templates, Grievance Messages, Notifications, Cookie Banners, and User Portal content.

This service integrates with **Bhashini**, the Government of India's open-source language translation platform, to ensure high-quality translation across multiple Indian languages. The service manages translation configurations per business application, generates and caches authentication tokens, and provides text translation with automatic numeral conversion between language scripts.

## Quick Start

```bash
# Clone repository
git clone <repository-url>
cd translator-apis

# Set environment variables
export MONGODB_URI=mongodb://localhost:27017
export MONGODB_DATABASE=cms_db_admin

# Build and run
mvn clean install
mvn spring-boot:run

# Access Swagger UI
open http://localhost:9002/v1/swagger-ui.html
```

## Features

### Core Functionality
- ✅ **Real-time language translation** for UI and backend modules
- ✅ **Translation configuration management** (create, update, retrieve)
- ✅ **Text translation** with multiple input items
- ✅ **Automatic token generation and caching** for faster repeated translations
- ✅ **Numeral conversion** between different language scripts
- ✅ **Transaction tracking and audit trail**

### Multi-Tenancy & Credentials
- ✅ **Tenant-level translation settings** supported
- ✅ **Business-level credential override** to use custom translation keys
- ✅ **Credential hierarchy** with automatic fallback mechanism
- ✅ **Multi-tenant architecture** with isolated MongoDB databases

### Technical Features
- ✅ **RESTful API** with OpenAPI/Swagger documentation
- ✅ **Caching mechanism** for faster repeated translations
- ✅ **Fallback mechanism** when credentials are not present
- ✅ **Comprehensive input validation** and error handling

## Technology Stack

- **Framework**: Spring Boot 3.5.7
- **Java**: 21
- **Database**: MongoDB
- **Build Tool**: Maven
- **API Docs**: SpringDoc OpenAPI 3
- **HTTP Client**: Apache HttpClient 5

## Prerequisites

- Java 21 or higher
- MongoDB 4.4 or higher
- Maven 3.6+ (for building)
- Access to translation provider API (Bhashini or Microsoft)

## Configuration

### Environment Variables

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `SERVER_PORT` | Service port | No | `9002` |
| `MONGODB_URI` | MongoDB connection URI | Yes | - |
| `MONGODB_DATABASE` | Admin database name | Yes | - |
| `JAVA_OPTS` | JVM options | No | - |
| `proxy.enabled` | Enable HTTP proxy for external API calls | No | `false` |
| `proxy.host` | Proxy host (required if proxy.enabled=true) | No | - |
| `proxy.port` | Proxy port (required if proxy.enabled=true) | No | `0` |

### Application Properties

```properties
spring.application.name=multitranslator
server.port=9002
server.servlet.context-path=/v1
log_path=/log/consent
spring.data.mongodb.auto-index-creation=true
```

## API Documentation

### Base URLs
- **API**: `http://localhost:9002/v1`
- **Swagger UI**: `http://localhost:9002/v1/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:9002/v1/v3/api-docs`

### Required Headers
- `tenantid` (required): Tenant ID (UUID)
- `businessid` (required): Business ID (UUID)  
- `txn` (required): Transaction ID (UUID)

### Endpoints

#### 1. Create Translation Configuration
```http
POST /v1/translateConfig
```

**Headers**: `tenantid`, `businessid`, `txn`

**Request:**
```json
{
  "scopeLevel": "BUSINESS",
  "config": {
    "provider": "BHASHINI",
    "apiBaseUrl": "https://api.bhashini.gov.in",
    "modelPipelineEndpoint": "/pipeline",
    "userId": "user-id",
    "apiKey": "api-key",
    "pipelineId": "pipeline-id"
  }
}
```

**Response:** `201 Created`
```json
{
  "status": "SUCCESS",
  "configId": "uuid-generated-config-id",
  "message": "Translator configuration saved successfully"
}
```

#### 2. Update Translation Configuration
```http
PUT /v1/updateTranslateConfig
```

**Headers**: `tenantid`, `businessid`, `txn`

**Request Body**: Same as Create Translation Configuration

**Response:** `200 OK`
```json
{
  "status": "SUCCESS",
  "configId": "existing-config-id",
  "message": "Translator configuration updated successfully"
}
```

#### 3. Get Translation Configuration
```http
GET /v1/getConfig?tenantId={tenantId}&businessId={businessId}&provider={provider}
```

**Query Parameters:**
- `tenantId` (required): Tenant ID (UUID)
- `businessId` (optional): Business ID (UUID)
- `provider` (optional): Provider name (BHASHINI or MICROSOFT)

**Response:** `200 OK`
```json
[
  {
    "configId": "config-id",
    "tenantId": "tenant-id",
    "businessId": "business-id",
    "scopeLevel": "BUSINESS",
    "config": {
      "provider": "BHASHINI",
      "apiBaseUrl": "https://api.bhashini.gov.in",
      "modelPipelineEndpoint": "/pipeline",
      "userId": "user-id",
      "apiKey": "api-key",
      "pipelineId": "pipeline-id"
    },
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
]
```

#### 4. Get Configuration Count
```http
GET /v1/count
```

**Headers**: `tenantid`, `txn`

**Response:** `200 OK`
```json
{
  "count": 5
}
```

#### 5. Translate Text
```http
POST /v1/translate
```

**Headers**: `tenantid`, `businessid`, `txn`

**Request:**
```json
{
  "provider": "BHASHINI",
  "source": "API",
  "language": {
    "sourceLanguage": "en",
    "targetLanguage": "hi"
  },
  "input": [
    {
      "id": "1",
      "source": "Hello, how are you?"
    },
    {
      "id": "2",
      "source": "This is a test message."
    }
  ]
}
```

**Response:** `200 OK`
```json
{
  "TXN": "transaction-id",
  "status": "SUCCESS",
  "message": "Translated Successfully",
  "output": [
    {
      "id": "1",
      "source": "Hello, how are you?",
      "target": "नमस्ते, आप कैसे हैं?"
    },
    {
      "id": "2",
      "source": "This is a test message.",
      "target": "यह एक परीक्षण संदेश है।"
    }
  ]
}
```

**Error Response:** `400 Bad Request`
```json
{
  "TXN": "transaction-id",
  "status": "FAILED",
  "message": "Configuration not found for given Business Id"
}
```

## Development

### Local Setup

1. **Clone repository:**
   ```bash
   git clone <repository-url>
   cd translator-apis
   ```

2. **Configure MongoDB:**
   - Start MongoDB locally or use remote instance
   - Create admin database (`cms_db_admin`)
   - Create tenant registry collection

3. **Set environment variables:**
   ```bash
   export MONGODB_URI=mongodb://localhost:27017
   export MONGODB_DATABASE=cms_db_admin
   export SERVER_PORT=9002
   ```

4. **Build and run:**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

### Build Commands

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Build JAR
mvn clean package

# Install to local repository
mvn clean install
```

## Deployment

### Docker

**Build:**
```bash
docker build -f deployment/Dockerfile -t translator-apis:latest .
```

**Run:**
```bash
docker run -d \
  --name translator-apis \
  -p 9002:9002 \
  -e MONGODB_URI=mongodb://host:27017 \
  -e MONGODB_DATABASE=cms_db_admin \
  translator-apis:latest
```

### Kubernetes

```bash
kubectl apply -f deployment/jio-dl-deployment.yaml
```

**Deployment Configuration:**
- Port: 9003 (configurable via ConfigMap)
- Resources: 200m CPU / 512Mi Memory (requests), 500m CPU / 1Gi Memory (limits)
- Health checks: Liveness and readiness probes at `/v1/actuator/health`

## Multi-Tenancy

The service implements multi-tenancy with tenant-specific MongoDB databases:

- **Admin Database** (`cms_db_admin`): Stores tenant registry in `tenant_registry` collection
- **Tenant Databases**: Named `tenant_db_{tenantId}` containing:
  - `multitranslateconfig` - Translation configurations
  - `TranslateToken` - Cached authentication tokens
  - `TranslateTranscation` - Translation transaction logs

**Tenant Setup:**
```javascript
// MongoDB admin database
db.tenant_registry.insertOne({
  "tenantId": "tenant-uuid-here"
});
```

## Credential Hierarchy

The Translator Service uses a layered credential system to maintain security and flexibility. The service determines which credentials to use in the following order of priority:

### Priority 1: Business Credentials
If a business (Data Fiduciary) has configured its own Bhashini credentials, these credentials will **always be used** for that business's translation requests.

### Priority 2: Tenant Credentials
If business credentials are not available, **tenant-level credentials** are used as fallback. This ensures translation continues to work even if individual businesses haven't configured their own credentials.

### Priority 3: No Credentials → Translation Disabled
If neither business nor tenant credentials exist, the service returns the **original English text** without translation.

**Benefits:**
- Businesses can use their own credentials for isolation and control
- Tenants can provide default credentials for all businesses
- Graceful degradation when credentials are missing

## How It Works

### Translation Flow

1. **Request Received**: Service receives translation request from calling module (Consent, Cookies, Notifications, etc.)

2. **Credential Resolution**:
   - Service checks if **Business Credentials** exist for the given `businessId`
   - If yes → use business credentials
   - If no → check **Tenant Credentials** (using `tenantId` as `businessId`)
   - If tenant credentials exist → use tenant credentials
   - If neither exists → return original text

3. **Token Management**:
   - Service checks for cached authentication token in database
   - If token exists and valid → use cached token
   - If token missing or expired → generate new token from Bhashini API
   - Cache token for future requests

4. **Translation Execution**:
   - Service calls Bhashini API with cached token
   - Processes translated text with automatic numeral conversion
   - Returns translated content to calling module

5. **Transaction Logging**: All translation operations are logged in `TranslateTranscation` collection for audit purposes

### Initial Setup

Before using the Translator Service, you must configure your Bhashini credentials:

1. **Tenant Admin Setup**: Tenant admin sets Bhashini API key and secret once at tenant level
2. **Business Setup (Optional)**: Businesses can optionally configure their own credentials for isolation
3. **No Further Configuration Required**: After initial setup, no further configuration is needed unless credentials change

## Security

- Input validation on all endpoints
- Header validation for tenant and transaction tracking
- Secure error handling
- CORS configuration enabled
- API keys stored in database (should be encrypted at rest)

## Monitoring

### Health Checks
- **Health**: `GET /v1/actuator/health`
- **Info**: `GET /v1/actuator/info`
- **Metrics**: `GET /v1/actuator/metrics`

### Logging
Logs are written to the directory specified by `log_path` (default: `/log/consent`).

## Use Cases

The Translator Service is used across multiple JCMS modules:

- **Consent Templates**: Translate consent request templates to user's preferred language
- **Grievance Messages**: Translate grievance-related messages and responses
- **Notifications**: Translate notification content sent to data principals
- **Cookie Banners**: Translate cookie consent banners and messages
- **User Portal**: Translate user portal content and interface elements

## Error Scenarios

| Scenario | Behavior |
|----------|----------|
| **Missing Business + Tenant credentials** | Translation disabled, original English text returned |
| **Invalid Bhashini key/secret** | Bhashini API error returned with appropriate error message |
| **Bhashini service unavailable** | Translator returns fallback original text, error logged |
| **Token generation failed** | Returns `FAILED` status with "Token Generation Failed" message |
| **Configuration not found** | Returns `FAILED` status with "Configuration not found for given Business Id" message |

## Best Practices

### Credential Management
- ✅ **Encourage businesses to configure their own credentials** for better isolation and control
- ✅ **Maintain tenant-level credentials as fallback** to ensure translation works for all businesses
- ✅ **Rotate Bhashini credentials periodically** for security
- ✅ **Use strong, unique API keys** for each business/tenant

### Performance Optimization
- ✅ **Monitor translation API latency** using application logs
- ✅ **Leverage token caching** - tokens are automatically cached to reduce API calls
- ✅ **Batch translation requests** when possible to improve efficiency
- ✅ **Monitor cache hit rates** to ensure optimal performance

### Security
- ✅ **Encrypt API keys at rest** in the database
- ✅ **Use TLS/SSL** for all external communications
- ✅ **Implement authentication** at API gateway level
- ✅ **Regular security audits** of stored credentials

### Monitoring
- ✅ **Track translation success/failure rates** via transaction logs
- ✅ **Monitor Bhashini API response times** for performance issues
- ✅ **Set up alerts** for credential failures or service unavailability
- ✅ **Review audit logs** regularly for compliance

## Troubleshooting

### Common Issues

**MongoDB Connection Failed**
- Verify `MONGODB_URI` is correct and accessible
- Check network connectivity to MongoDB instance
- Ensure MongoDB is running

**Translation API Failing**
- Verify translation provider configuration (API keys, endpoints)
- Check network connectivity to external translation services
- Review token generation logic in logs
- Verify Bhashini service is available and responding

**Tenant Not Found**
- Ensure tenant is registered in `tenant_registry` collection in admin database
- Verify `tenantid` header is set correctly in requests
- Check tenant database exists: `tenant_db_{tenantId}`

**Configuration Not Found**
- Verify translation configuration exists for the business ID
- Check if tenant-level configuration exists (using tenantId as businessId)
- Check provider type matches the configuration
- Ensure tenant ID and business ID are correct

**Credential Issues**
- Verify business credentials are correctly configured
- Check tenant-level credentials exist as fallback
- Ensure API keys and secrets are valid and not expired
- Review credential hierarchy resolution in logs

**Token Generation Failed**
- Check Bhashini API credentials are valid
- Verify network connectivity to Bhashini API
- Review API endpoint configuration
- Check Bhashini service status

**Enable Debug Logging**
```properties
logging.level.com.jio.multitranslator=DEBUG
```

## Integration with JCMS Modules

The Translator Service integrates seamlessly with various JCMS modules:

### Consent Management Module
- Translates consent request templates
- Translates consent withdrawal messages
- Supports multi-language consent forms

### Grievance Management Module
- Translates grievance submission forms
- Translates grievance response messages
- Multi-language grievance tracking

### Notification Service
- Translates notification content
- Supports SMS, Email, and Push notification translation
- Language-specific notification templates

### Cookie Management
- Translates cookie consent banners
- Multi-language cookie policy content
- Cookie preference center translations

### User Portal
- Translates portal interface elements
- Dynamic content translation
- User preference-based language selection

## Conclusion

The Translator Service plays a crucial role in enabling accessibility across different languages in the JCMS ecosystem. With its credential hierarchy, integration with Bhashini, and robust caching mechanism, it ensures flexible, scalable, and secure multilingual support. Businesses and tenants must ensure their credentials are properly configured to fully utilize multilingual capabilities and provide a seamless experience to data principals in their preferred language.

## Dependencies

### External Services
- **Bhashini API**: Government of India's translation platform

### Database
- **MongoDB**: Multi-tenant database architecture
  - Admin database: `cms_db_admin` (tenant registry)
  - Tenant databases: `tenant_db_{tenantId}` (per-tenant data)

### Integration Points
This service is called by other DPDP CMS microservices via REST API:
- Consent Management Service
- Grievance Management Service
- Notification Service
- Cookie Management Service
- User Portal

## License

This project is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0) with additional terms for the DPDP CMS project. See [LICENSE](LICENSE) for details.

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version history and changes.

---

**Note**: This service is part of the Digital Personal Data Protection (DPDP) Consent Management System, a government Digital Public Good designed to ensure compliance with the DPDP Act, 2023 of India.