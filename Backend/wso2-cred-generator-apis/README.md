# WSO2 Credentials Generation Service

## Overview

The **WSO2 Credentials Generation Service** is a critical microservice in the Digital Personal Data Protection (DPDP) Consent Management System (JCMS). It generates and manages WSO2 API Manager credentials: tenant registration, business onboarding, and data processor credential provisioning with idempotent operations and secure storage in tenant databases.

Part of the government Digital Public Good for DPDP Act 2023 compliance.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Configuration](#configuration)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [WSO2 Setup](#wso2-setup)
- [Deployment](#deployment)
- [License](#license)

## Features

- **Tenant registration** with WSO2 API Manager (`/registerTenant`)
- **Business onboarding** with OAuth2 credential generation
- **Data processor credential provisioning**
- **Idempotent operations** to avoid duplicate registrations
- **API subscription management**
- **Secure credential storage** in tenant MongoDB databases
- **REST API** with OpenAPI/Swagger

## Architecture

- **Service type**: REST API microservice
- **Context path**: `/api/v1`
- **Database**: MongoDB (e.g. `cms_db_admin`, tenant DBs)
- **Integration**: WSO2 API Manager (OAuth2, DevPortal APIs)

## Prerequisites

- Java 21
- Maven 3.8+
- MongoDB 5.0+
- WSO2 API Manager instance (admin and DevPortal access)
- **Required environment variables** (no default credentials in code):
  - `MONGODB_URI` — MongoDB connection string
  - `MONGODB_DATABASE` — Database name (e.g. `cms_db_admin`)
  - `WSO2_BASE_URL` — WSO2 base URL (e.g. `https://<wso2-host>:9443`)
  - `REGISTER_TOKEN` — Base64-encoded `username:password` (admin)
  - `TOKEN_AUTH` — Base64-encoded `clientId:clientSecret` (admin app)

## Configuration

| Variable | Description | Required |
|----------|-------------|----------|
| `SERVER_PORT` | HTTP port | No (deployment default: 9004) |
| `MONGODB_URI` | MongoDB connection URI | Yes |
| `MONGODB_DATABASE` | MongoDB database name | Yes |
| `WSO2_BASE_URL` | WSO2 API Manager base URL | Yes |
| `REGISTER_TOKEN` | Base64(admin username:password) | Yes |
| `TOKEN_AUTH` | Base64(clientId:clientSecret) | Yes |

**Security:** Do not commit `REGISTER_TOKEN` or `TOKEN_AUTH`. Use environment variables or Kubernetes Secrets.

## Quick Start

### 1. Set environment variables

```bash
export MONGODB_URI=mongodb://localhost:27017/?authSource=admin
export MONGODB_DATABASE=cms_db_admin
export WSO2_BASE_URL=https://<wso2-host>:9443
export REGISTER_TOKEN=<base64-username-password>
export TOKEN_AUTH=<base64-clientid-clientsecret>
```

### 2. Build and run

```bash
./mvnw clean package -DskipTests
./mvnw spring-boot:run
```

Service runs at `http://localhost:<SERVER_PORT>/api/v1`. Swagger UI: `http://localhost:<SERVER_PORT>/api/v1/swagger-ui.html`.

## API Documentation

- **OpenAPI JSON**: `/api/v1/v3/api-docs`
- **Swagger UI**: `/api/v1/swagger-ui.html`

Key operations (paths may vary; check Swagger):

- Tenant registration
- Business onboarding
- Data processor onboarding
- API subscription (multiple)

## WSO2 Setup

1. **Admin credentials**: Obtain WSO2 admin username and password.
2. **Admin application**: In WSO2 DevPortal, create an application and generate production keys to get `clientId` and `clientSecret`.
3. **Encode for env**:
   - `REGISTER_TOKEN`: `echo -n "username:password" | base64`
   - `TOKEN_AUTH`: `echo -n "clientId:clientSecret" | base64`
4. **API IDs**: Ensure required NeGD API IDs are present in the `wso2_availabe_apis` collection as documented in the root README.

## Deployment

- Kubernetes: use `deployment/deployment.yaml`. Replace ConfigMap placeholders (`<base64-username-password>`, `<base64-clientid-clientsecret>`, `<wso2-host>`) with real values or use Secrets and external config.
- Docker: build from repo root or this directory using the appropriate Dockerfile if present.

## License

See the root repository `LICENSE` and DPDP CMS terms.
