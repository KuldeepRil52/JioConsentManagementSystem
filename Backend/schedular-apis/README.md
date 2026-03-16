# Schedular — DPDP Consent Management System

Short description: The `schedular` microservice performs periodic scheduled tasks for consent lifecycle management, retention, reminders and grievance escalation within the DPDP Consent Management System (DPDP CMS).

## Overview
- Service: `Schedular` (artifact `com.jio:schedular`)
- Role: Runs tenant-aware scheduled jobs that enforce consent expiry, consent-handle expiry, consent-preference reminders, artifact retention, cookie consent handling, grievance escalation and related housekeeping tasks required for DPDP Act compliance.
- Exposes a small REST surface for operational metrics and job run history and integrates with notification, audit and vault modules.

## Table of Contents
- Features
- Architecture
- Prerequisites
- Quick Start
- Configuration
- API
- Development
- Testing
- Deployment
- Monitoring
- Security & Compliance

## Features
- Tenant-aware scheduled job runner for consent lifecycle and retention.
- Jobs included: consent expiry, consent-handle expiry, consent-preference expiry reminders, cookie-consent expiry/retention, grievance escalation and retention, logs & data retention, consent artifact retention.
- Batch processing with configurable batch-size and thread-pool tuning per job.
- Integrates with `notification`, `audit` and `vault` modules for notifications, audit records and cryptographic operations.
- REST endpoints to query scheduler statistics and job run history.
- OpenAPI (Swagger) UI exposed for interactive API exploration.

## Architecture
**Service type:** Scheduled jobs processor + lightweight REST API

**Communication patterns:** REST between microservices; internal scheduled (cron) jobs. No Kafka producers/consumers found in this module.

**Database:** MongoDB (Spring Data MongoDB)

**Default port:** `9001` (overridable by `SERVER_PORT` environment variable)

**Integration points (found in code):**
- `notification.service.base.url` — Notification module base URL (used to trigger notification events)
- `audit.service.base.url` — Audit module base URL (used to write audit records)
- `vault.service.base.url` — Vault service (sign/encrypt payloads)

## Prerequisites
- Java 21 (project `pom.xml` sets `java.version=21`)
- Maven (the repo includes Maven wrapper `mvnw`/`mvnw.cmd`)
- MongoDB accessible to the service
- Optional: Docker for containerized runs

## Quick Start

### Local development (fast)
1. Build:
```powershell
./mvnw -DskipTests clean package
```
2. Run:
```powershell
./mvnw spring-boot:run
```
Service will start on `http://localhost:9001/schedular` by default.

### Using Docker
Build image (from repository root):
```powershell
docker build -f deployment/Dockerfile -t schedular:local .
```
Run container (example):
```powershell
docker run -e SERVER_PORT=9001 -e MONGODB_URI="mongodb://..." -p 9001:9001 schedular:local
```

### Using Docker Compose
If you have a compose file (not included), run:
```powershell
docker compose up --build
```

## Configuration
Configuration is controlled via `application.properties` and environment variables.

Key environment variables (name — default — description):
- `SERVER_PORT` — `9001` — HTTP port the app listens on (mapped to Spring Boot `server.port`).
- `MONGODB_URI` — `mongodb://<user>:<password>@<host>:27017/?authSource=admin` — MongoDB connection string.
- `MONGODB_DATABASE` — `cms_db_admin` — MongoDB database name.

Notification / Audit / Vault endpoints (defaults):
- `NOTIFICATION_MODULE_APIS_URL` — `http://notification-module-apis:9005`
- `AUDIT_MODULE_APIS_URL` — `http://audit-module-apis:9006`
- `VAULT_APIS_URL` — `http://<vault-host>:30010`

Scheduling job configuration (examples, set as env vars):
- `CONSENT_HANDLE_EXPIRY_CRON` — cron for consent-handle expiry (default `0 0 2 * * ?`).
- `CONSENT_EXPIRY_CRON` — cron for consent expiry (default `0 30 02 * * ?`).
- `CONSENT_PREFERENCE_EXPIRY_CRON` — cron for preference expiry reminder (default `0 15 02 * * ?`).
- For each job there are `*_BATCH_SIZE` and `*_THREAD_POOL_SIZE` environment variables; see `src/main/resources/application.properties` for full list and defaults.

Security-sensitive variables: `MONGODB_URI` (contains DB credentials), any secrets used to call external services — treat as sensitive.

## API
OpenAPI (springdoc) is enabled in the project. Default OpenAPI endpoint:
- OpenAPI JSON: `http://<host>:<port>/v3/api-docs`
- Swagger UI: `http://<host>:<port>/swagger-ui/index.html`

Key REST endpoints (from `SchedularJobController`)
- GET `/schedular/v1/stats` — Returns run info for all registered scheduler jobs.
  - Required header: `tenantId`
  - Optional headers: `businessId`, `startDate`, `endDate` (ISO-8601 or `yyyy-MM-dd`)

- GET `/schedular/v1/stats/{jobName}` — Returns run info for specific job.
  - Path param: `jobName`
  - Required header: `tenantId`
  - Optional headers: `businessId`, `startDate`, `endDate`

Authentication: Spring Security is present (`SecurityConfig`); the service expects authenticated requests in production. The project uses JWT libraries — confirm with your environment for the exact auth flow and token issuer.

Example curl:
```bash
curl -H "tenantId: tenant-abc" \
  "http://localhost:9001/schedular/v1/stats"
```

## Development

### Project structure (top-level)
```
src/main/java/com/jio/schedular/
  controller/       # REST controllers
  service/          # business logic and scheduled jobs
  jobs/             # individual job implementations
  repository/       # MongoDB repositories
  config/           # Spring configuration, security, OpenAPI
src/main/resources/
  application.properties
  logback-spring.xml
```

### Build from source
```powershell
./mvnw -DskipTests clean package
```

## Testing
- Run unit tests:
```powershell
./mvnw test
```

## Deployment
- Kubernetes manifests and Dockerfile are under `deployment/`.
- CI pipeline is configured in `azure-pipelines.yml` using shared pipeline templates.

## Monitoring
- Actuator endpoints are available via Spring Boot Actuator if enabled in production configuration.
- Health and metrics endpoints can be exposed and scraped by Prometheus/Grafana in platform deployments.

## Security & Compliance
- This service processes consent- and retention-related workflows; follow DPDP Act 2023 requirements for consent handling.
- Audit integration is enabled — audit events should be produced for sensitive actions.
- Secrets and DB credentials must be stored in a vault and not committed into repository.

## Where to find more info
- Design and API docs for specific scheduled jobs are in `docs/` (consent expiry, notification, grievance docs).

If you want, I can also generate a minimal `docker-compose.yml` and a starter `README` section for developers onboarding; tell me which you'd prefer next.

## Contact & Support
- **Developer / General support**: `dev@dpdp-cms.gov.in`
- **Security / Vulnerability reports**: `security@dpdp-cms.gov.in` (do NOT create public issues for vulnerabilities)
- **Code of Conduct reports**: `conduct@dpdp-cms.gov.in`

## License
This module is licensed under the GNU Lesser General Public License v3.0 (LGPL-3.0) with DPDP-specific additional terms. See the `LICENSE` file for full details.
