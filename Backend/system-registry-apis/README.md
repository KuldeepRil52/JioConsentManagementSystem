# System Registry APIs (NeGD Fides Module)

## Overview

The **System Registry** service (NeGD Fides Module) is part of the Digital Personal Data Protection (DPDP) Consent Management System (CMS). It provides registry and integration capabilities for **systems**, **integrations**, and **datasets**, and supports consent withdrawal flows used by the Scheduler and other microservices.

**Key responsibilities:**

- Tenant and system registry management
- Integration and dataset registry
- Consent withdraw API (`/registry/api/v1/consents/{consentId}/withdraw`) for downstream services (e.g. Scheduler, Notification Consumer)
- Audit and notification integration for registry events

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Deployment](#deployment)
- [License](#license)

## Features

- **Multi-tenant registry**: Tenant-aware MongoDB with shared and tenant-prefixed databases
- **Consent withdraw**: REST endpoint for consent withdrawal used by Scheduler and other services
- **Integration & dataset registry**: System, integration, and dataset metadata storage
- **Audit & notification**: Triggers audit and notification services for relevant operations
- **REST API**: Spring Boot REST with OpenAPI/Swagger at `/registry/swagger-ui.html`
- **Security**: Spring Security; configurable for production

## Architecture

- **Service type**: REST API microservice
- **Default port**: `9011` (overridable via `SERVER_PORT`)
- **Context path**: `/registry`
- **Database**: MongoDB (`cms_db_admin`, tenant databases)
- **Integration**: Notification Module (trigger events), Audit Module (audit logs)

## Prerequisites

- Java 21
- Maven 3.8+
- MongoDB 5.0+
- Access to Notification and Audit services (or use defaults for local runs)

## Configuration

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | HTTP port | `9011` |
| `MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/?authSource=admin` |
| `NOTIFICATION_MODULE_APIS_URL` | Notification service base URL | `http://localhost:30005` |
| `AUDIT_MODULE_APIS_URL` | Audit service base URL | `http://localhost:30006` |

## Quick Start

### Build

```bash
./mvnw clean package -DskipTests
```

### Run

```bash
./mvnw spring-boot:run
```

Service will be available at `http://localhost:9011/registry`.

### Swagger UI

- OpenAPI: `http://localhost:9011/registry/v3/api-docs`
- Swagger UI: `http://localhost:9011/registry/swagger-ui.html`

## API Documentation

Key area:

- **Consent withdraw**: `POST /registry/api/v1/consents/{consentId}/withdraw` — used by Scheduler and other services to withdraw consent and trigger notifications/audit.

Other endpoints for system, integration, and dataset registry are documented in Swagger.

## Deployment

- Kubernetes manifests and Dockerfile are under `deployment/`.
- Set `MONGODB_URI`, `NOTIFICATION_MODULE_APIS_URL`, and `AUDIT_MODULE_APIS_URL` via ConfigMap or Secrets for your environment.

## License

See the root repository `LICENSE` file and DPDP CMS terms.
