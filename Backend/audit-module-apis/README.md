# dpdp-audit-module

## Overview
The `dpdp-audit-module` provides audit recording, PDF certificate generation (Section 65B style), and publishing audit events to Kafka for the DPDP Consent Management System. It is a Spring Boot microservice implemented in Java (Maven). Repository: `git@ssh.dev.azure.com:v3/JPL-Limited/DPDP-Easy-Gov/dpdp-audit-module`.

## Role in DPDP CMS
- Captures audit records for consent and related transactions.
- Generates a Section 65B-like PDF certificate and applies external PAdES signing.
- Publishes audit events to Kafka for downstream processing and indexing.
- Enforces multi-tenancy via `X-Tenant-Id` header.

## Features
- REST API to create and query audit records.
- PDF generation using Apache PDFBox (templated sections, JSON appendix).
- PDF signing integration via `PdfSignerService`.
- Kafka producer to publish audit events (topic configurable).
- Tenant-aware operations (header-driven).
- JSON serialization with Jackson.

## Architecture
- Spring Boot REST service + Kafka producer.
- Key packages: `controller`, `service`, `repository`, `kafka`, `config`.
- PDF generation implementation: `SignPdfServiceImpl` (uses PDFBox, loads fonts from `src/main/resources/fonts/`).
- Kafka producer/service: publishes audit payloads after creation (`audit-events` default topic).
- Data store: MongoDB (tenant-isolated databases recommended).

## Configuration
Configuration is read from Spring properties / environment variables. Important properties:

| Environment Variable / Property | Default | Description | Sensitive |
|---|---:|---|---|
| `SERVER_PORT` / `server.port` | `8080` | HTTP port | no |
| `SPRING_PROFILES_ACTIVE` / `spring.profiles.active` | `default` | Active profile | no |
| `SPRING_DATA_MONGODB_URI` / `spring.data.mongodb.uri` | `mongodb://localhost:27017/auditdb` | MongoDB connection URI | yes |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` / `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka brokers | no |
| `AUDIT_KAFKA_TOPIC` / `audit.kafka.topic` | `audit-events` | Kafka topic used for audit events | no |
| `PDF_SIGNER_KEYSTORE_PATH` | `classpath:keystore.p12` | Keystore used by external signer integration | yes |
| `LOG_LEVEL` | `INFO` | Logging level | no |
| `FONT_PATH` | `classpath:fonts/` | Path to bundled fonts | no |
| `JWT_PUBLIC_KEY` | - | Public key for JWT verification (if used) | yes |

Place sensitive secrets in environment variables or secret stores. Verify exact property names in `src/main/resources/application*.yml` or `application.properties`.

## API
Primary endpoints are implemented in `AuditController`. Examples below reflect typical operations; verify exact mappings in the controller source.

- Create audit
    - POST `/api/audits` (controller-level path may vary)
    - Headers: `X-Tenant-Id`, `X-Business-Id`, `X-Transaction-Id`
    - Body: `AuditRequest` JSON
    - Response: `201 Created` with `AuditResponse` on success

- Retrieve audit
    - GET `/api/audits/{id}`
    - Headers: `X-Tenant-Id`
    - Response: `200 OK` with `AuditResponse`

Authentication: If JWT/OAuth is enabled, requests must include `Authorization: Bearer <token>`. Check security configuration for exact requirements.

Swagger UI:
- Available when `springdoc`/`springfox` enabled. Typical path: `/swagger-ui.html` or `/swagger-ui/index.html`. Confirm via running app.

Example curl (adjust path to match controller mapping):
curl -X POST http://localhost:8080/api/audits
-H "Content-Type: application/json"
-H "X-Tenant-Id: tenant1"
-H "X-Business-Id: business1"
-H "X-Transaction-Id: tx-123"
-d '{ "referenceId":"ref-1", "payload": { /.../ } }'

## Development
Build and test:

- Build: `mvn -e -B clean package`
- Run tests: `mvn test`
- Run locally: `java -jar target/dpdp-audit-module-*.jar`
- Docker (example):
    - `docker build -t dpdp-audit-module:latest .`
    - `docker run -e SPRING_DATA_MONGODB_URI="mongodb://host:27017/auditdb" -p 8080:8080 dpdp-audit-module:latest`

Project layout (partial):
src/main/java/com/jio/digigov/auditmodule/ controller/ service/ service/impl/ kafka/ repository/ src/main/resources/ application.yml fonts/ pom.xml Dockerfile
## Dependencies
- Spring Boot (version as in `pom.xml`)
- Apache PDFBox
- Jackson (databind)
- Spring Kafka
- Spring Data MongoDB
- Lombok (compile-time)
  Confirm exact versions in `pom.xml`.

## Runtime / Deployment Notes
- Ensure Kafka and MongoDB are reachable.
- Configure font files in `src/main/resources/fonts/` or provide alternatives.
- The service reserves bottom area of generated PDFs for external signature; signing is performed by `PdfSignerService`.
- Monitor and secure `spring.data.mongodb.uri` and keystore secrets.

## Troubleshooting
- PDF rendering errors: verify font files exist and are readable.
- Kafka errors: confirm `spring.kafka.bootstrap-servers`.
- MongoDB errors: verify `spring.data.mongodb.uri` and tenant database creation permissions.

## Contacts
- Repository: `git@ssh.dev.azure.com:v3/JPL-Limited/DPDP-Easy-Gov/dpdp-audit-module`
- Maintainers: `ops@jpl-limited.example` (operational), `security@jpl-limited.example` (security)
