# Grievance Service (DPDP CMS)

> Multi-tenant grievance lifecycle APIs that power the Digital Personal Data Protection (DPDP) Consent Management System.  
> Central DPDP CMS documentation: https://dev.azure.com/DPDP-Consent-Core/_wiki/wikis/CMS-Docs/1 (internal access).

## Service Role within DPDP CMS

The grievance service collects, tracks, and resolves citizen or partner grievances that arise during consent lifecycle events. It enforces tenant and business isolation, issues signed grievance tokens for downstream verification, and publishes audit & notification signals so that compliance, CX, and analytics teams can observe the full grievance journey mandated by the DPDP Act 2023.

| Aspect | Details |
| --- | --- |
| **Service type** | REST API (Spring Boot 3.5) backed by multi-tenant MongoDB |
| **Inbound clients** | Consent orchestration UI, partner onboarding apps, regulatory back-office tooling |
| **Outbound integrations** | Notification module, Audit module, DigiGov Vault signing service |
| **Runtime** | Java 21, packaged as container image via `deployment/Dockerfile` |
| **Context path & port** | `/grievance` on port `9006` (configurable) |

## Core Capabilities

- Tenant-aware grievance CRUD with full history, attachments metadata, and customer feedback management.
- Dynamic template library supporting multilingual payload definitions, UI config, and publish/version workflows.
- Catalogues for grievance types, user types, and user detail blueprints reused across tenants.
- Notification fan-out using configurable email/SMS toggles stored in `grievance_configurations`.
- Immutable audit trail emission to the DPDP Audit service with IP address capture for every mutation.
- Vault-signed JWT issuance so that other microservices can independently validate grievance payloads.
- Request correlation via `X-Tenant-ID`, `X-Business-ID`, and `X-Transaction-ID` headers to support cross-service tracing.

## Architecture Overview

1. **Inbound REST** (controllers in `com.jio.digigov.grievance.controller`) validate headers, perform schema validation, and delegate to services.
2. **Service layer** (`service.impl`) applies business rules (status transitions, versioning) and populates `TenantContextHolder` for the current tenant.
3. **Tenant-aware persistence**: repositories operate on MongoDB collections that are dynamically selected via `TenantAwareMongoTemplate`. Shared metadata (templates/config) sits in `tenant_db_shared`, while live grievances live in databases named `tenant_db_<tenantId>`.
4. **Outbound flows**: after mutating data, services call `NotificationEventService` and `AuditEventService` (HTTP clients configured in `integration/`) and optionally sign payloads with the Vault service.
5. **Observability & compliance**: OpenAPI 3 documentation is published via SpringDoc; logback writes JSON-friendly logs to both console and rotating files for SIEM ingestion.

## Integrations

| Integration | Protocol | Purpose | Config |
| --- | --- | --- | --- |
| Notification Module | REST POST `/notification/v1/events/trigger` | Email/SMS workflow triggered per grievance status | `NOTIFICATION_MODULE_APIS_URL`, tenant headers forwarded |
| Audit Module | REST POST `/audit/v1/audit` | Immutable audit records for grievances and templates | `AUDIT_MODULE_APIS_URL`, includes IP & actor role metadata |
| DigiGov Vault | REST POST `/client/sign` | Issues JWT (`grievanceJwtToken`) for downstream verification | `VAULT_APIS_URL` |
| MongoDB | Native driver | Per-tenant storage of `grievances`, `grievance_types`, `user_types`, etc. | `MONGODB_URI`, `multi-tenant.*` |


## Configuration Reference

| Variable | Description | Default | Required | Sensitive |
| --- | --- | --- | --- | --- |
| `SERVER_PORT` | HTTP listener port. Must align with service mesh and ingress. | none (`9006` in k8s config) | Yes | No |
| `MONGODB_URI` | Full MongoDB connection string **including** default database. Also reused as `spring.data.mongodb.database`, so include `/tenant_db_shared`. | none | Yes | Yes |
| `LOG_PATH` | Directory for logback appenders (`grievance-app*.log`). | `logs` | No | No |
| `NOTIFICATION_MODULE_APIS_URL` | Base URL for notification module APIs. | `http://notification-module-apis:9005` | No | No |
| `AUDIT_MODULE_APIS_URL` | Base URL for audit module APIs. | `http://audit-module-apis:9006` | No | No |
| `VAULT_APIS_URL` | DigiGov Vault URL used for signing grievance payloads. | `https://<vault-host>:8443/negdvault` | No | Yes (targets critical infra) |
| `PROFILE` | Optional Spring profile (e.g., `public`, `sit`). Used by deployment manifests. | `public` | No | No |
| `JAVA_OPTS` | JVM flags injected by container entrypoint (heap, GC, tracing). | empty | No | Potentially |
| `KRB5_CONFIG` / `KEYTAB_CONFIG` | Optional Kerberos paths when running in clusters that mandate mTLS/Kerberos for egress. | set via manifests | No | Yes |

> Header Requirements: every API call must send `X-Tenant-ID`, `X-Business-ID`, and `X-Transaction-ID`; catalog endpoints also require `X-Scope-Level: TENANT|BUSINESS`. Missing headers raise `IllegalArgumentException`.


## API Surface (excerpt)

| Method & Path (all prefixed with `/grievance`) | Summary | Required Headers | Body |
| --- | --- | --- | --- |
| `POST /api/v1/grievances` | Create grievance, persist attachments metadata, issue Vault-signed JWT, trigger notification/audit. | `X-Tenant-ID`, `X-Business-ID`, `X-Transaction-ID`, `X-GRIEVANCE-TEMPLATE-ID` | `GrievanceCreateRequest` |
| `GET /api/v1/grievances` | Paginated listing filtered by tenant/business. | `X-Tenant-ID`, `X-Business-ID`, `X-Transaction-ID` | – |
| `GET /api/v1/grievances/search` | Dynamic search across nested JSON fields. Accepts `page`/`size` query params. | `X-Tenant-ID`, `X-Business-ID` | – |
| `GET /api/v1/grievances/{id}` | Fetch details plus timeline/history. Generates transaction ID if absent. | `X-Tenant-ID`, `X-Business-ID` | – |
| `PUT /api/v1/grievances/{id}` | Update status/remarks; re-signs payload and appends history. | `X-Tenant-ID`, `X-Business-ID` | `GrievanceUpdateRequest` |
| `PUT /api/v1/grievances/{id}/feedback` | Set citizen feedback rating (1–5). | `X-Tenant-ID`, `X-Business-ID` | `{ "feedback": 4 }` |
| `GET /api/v1/grievances/count` | Returns `{ "total": <count> }` for applied filters. | `X-Tenant-ID`, `X-Business-ID` | – |
| `POST /api/v1/grievance-templates` | Create or publish template with multilingual config and versioning. | `X-Tenant-ID`, `X-Business-ID`, `X-Transaction-ID` | `GrievanceTemplateRequest` |
| `GET /api/v1/grievance-templates/search` | Search templates by status/component. | same as above | – |
| `PUT /api/v1/grievance-templates/{id}` | Update template metadata and UI config. | same as above | payload |
| `POST /api/v1/grievance-types` | Manage grievance type catalog scoped by business and `X-Scope-Level`. | `X-Tenant-ID`, `X-Business-ID`, `X-Transaction-ID`, `X-Scope-Level` | `GrievanceTypeCreateRequest` |
| `POST /api/v1/user-details` | Store reusable user detail blueprints; duplicates rejected. | same as above | `UserDetailCreateRequest` |
| `POST /api/v1/user-types` | Maintain user type taxonomy. | same as above | `UserTypeCreateRequest` |

> Every controller is annotated with `@Tag`, `@Operation`, and `@ApiResponses`; Swagger UI is published at `https://<host>:<port>/grievance/swagger-ui/index.html`.

## Domain Model

- `grievances`: one document per grievance, including feedback, history, JWT, attachments metadata, and `status` (enum `NEW`, `IN_PROGRESS`, `RESOLVED`, etc.).
- `grievance_templates`: definitions of UI layout, multilingual content, and configuration toggles per business.
- `grievance_types`, `user_types`, `user_details`: lookup collections filtered by tenant, scope level, and business.
- `grievance_configurations`: stores notification channel toggles, SLA timelines, and escalation chain consumed while dispatching notifications.

Timestamps (`createdAt`, `updatedAt`) are automatically maintained through `BaseEntity` auditing annotations.


## Security, Privacy & Compliance Highlights

- **Access control**: Spring Security currently permits all requests. Deployments must front the service with API Gateway / WAF that enforces IAM, JWT verification, and rate limiting.
- **Tenant isolation**: `TenantContextHolder` ensures the correct Mongo database (`tenant_db_<tenantId>`) is selected per request; attempts without tenant context fall back to the shared DB and are logged.
- **Data protection**: `VaultManager` signs grievance payloads so that downstream services can verify tampering. Attachments are stored elsewhere; only metadata lives here.
- **Auditability**: Every create/update/delete operation triggers the Audit module with actor role, transaction ID, and client IP, satisfying DPDP retention clauses.
- **Observability**: Logback writes structured logs to both console and rotating files; include `X-Transaction-ID` in each request for correlation.


## Local Development

1. **Prerequisites**: Java 21, Maven 3.9+, Docker (optional), access to MongoDB (local or Atlas), and an HTTP client (Hoppscotch/Postman).
2. **Bootstrap Mongo**: Provide a URI that includes the default DB, e.g. `mongodb://localhost:27017/tenant_db_shared`.
3. **Run the app**:

```bash
mvn clean spring-boot:run \
  -Dspring-boot.run.jvmArguments="--add-opens java.base/java.lang=ALL-UNNAMED" \
  -DMONGODB_URI=mongodb://localhost:27017/tenant_db_shared \
  -DSERVER_PORT=9006
```

4. **Sample request**:

```bash
curl -X POST http://localhost:9006/grievance/api/v1/grievances \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant001" \
  -H "X-Business-ID: biz001" \
  -H "X-Transaction-ID: txn-123" \
  -H "X-GRIEVANCE-TEMPLATE-ID: tpl-01" \
  -d '{ "grievanceTemplateId": "tpl-01", "grievanceType": "DATA_CORRECTION", "userDetails": { "email": "citizen@example.com" }, "grievanceDescription": "Incorrect consent status" }'
```


## Testing

- `mvn test` (runs Spring Boot starter tests). Current repository contains minimal automated tests; please add unit tests alongside new services or controllers.
- For integration tests, spin up MongoDB via Docker (`docker run -p 27017:27017 mongo:7`) and use dedicated tenant IDs to avoid clashing with local data.
- CI (Azure Pipelines) executes Maven build, dependency scanning (Synopsys Black Duck), and Fortify SAST as defined in `azure-pipelines.yml`.


## Packaging & Deployment

- **Jar**: `mvn clean package` outputs `target/grievance-module-1.0.0.jar`.
- **Container**:
  - Internal multi-stage image: `docker build -f deployment/Public_Dockerfile -t grivance-module-apis:latest .`
  - Runtime uses non-root user `si_digigov` and expects props mounted under `/opt/props`.
- **Kubernetes**: `deployment/Public_deployment.yaml` deploys the service into `dpdp-consent-public` namespace, exposes NodePort `30006`, and mounts Kerberos artifacts plus log directories.
- **Helm/Other**: align env vars and secrets from ConfigMaps/Secrets as shown in the manifest.


## Directory Layout

```
.
├── pom.xml
├── azure-pipelines.yml
├── deployment/
│   ├── Dockerfile
│   ├── Public_Dockerfile
│   └── *.yaml (Kubernetes specs)
├── src/
│   ├── main/java/com/jio/digigov/grievance
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── integration/       # Notification & audit clients
│   │   ├── config/            # Multi-tenant Mongo, security, OpenAPI
│   │   └── entity|dto|mapper  # Mongo models & converters
│   └── main/resources/
│       ├── application.yml
│       └── logback-*.xml
└── README.md (this file)
```


## Troubleshooting

- **`IllegalStateException: Tenant ID cannot be null`** – verify `X-Tenant-ID` header is present; gateway rewrites sometimes strip mixed-case headers, prefer canonical casing.
- **`Failed to sign JWT`** – confirm `VAULT_APIS_URL` is reachable from the pod and the Vault client credentials stored in Vault are still valid.
- **`Notification config enabled but no valid contact`** – user detail documents must include either `email` or `mobile` field names for notification dispatch.
- **Mongo authentication** – because `spring.data.mongodb.database` equals the URI by default, always include the DB segment in your URI or override the property explicitly.


## Support

- **Product & engineering**: dpdp-support@ril.com
- **Security disclosures**: dpdp-security@ril.com (also see `SECURITY.md`)
- **Maintainers**: DigiGov DPDP Core Team (Jio Digital Government Solutions)

Contributions, bug reports, and feature proposals follow the workflow described in `CONTRIBUTING.md`. For policy or escalation questions, refer to `GOVERNANCE.md`.