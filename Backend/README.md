# JCMS Backend Services

Backend microservices for the **Jio Consent Management System (JCMS)** — DPDP Act 2023 compliant consent, grievance, audit, and credential management.

For high-level architecture, quick start, and deployment, see the [root README](../README.md).

## Services

| Service | Description | Default Port | README |
|---------|-------------|--------------|--------|
| **auth-apis** | JWT auth, token introspection/revocation, session & secure code | 9009 | [README](auth-apis/README.md) |
| **audit-module-apis** | Audit logging, Section 65B PDF certificates, Kafka audit events | 9006 | [README](audit-module-apis/README.md) |
| **consent-core-apis** | Consent lifecycle, templates, handles, validation | 9001 | [README](consent-core-apis/README.md) |
| **cookie-consent-apis** | Cookie consent and scanning | 9008 | [README](cookie-consent-apis/README.md) |
| **grivance-module-apis** | Grievance creation, tracking, Vault-signed JWTs | 9007 | [README](grivance-module-apis/README.md) |
| **notification-module-apis** | Notification triggering, templates, OTP, Kafka producer | 9005 | [README](notification-module-apis/README.md) |
| **notification-consumer-apis** | Kafka consumer, SMS/email/webhook delivery | 9020 | [README](notification-consumer-apis/README.md) |
| **partner-portal-apis** | Partner portal backend, WSO2 credential flow, vault, auth | 9002 | [README](partner-portal-apis/README.md) |
| **schedular-apis** | Scheduled jobs (consent expiry, retention, grievance escalation) | 9017 | [README](schedular-apis/README.md) |
| **system-registry-apis** | System/integration/dataset registry, consent withdraw API | 9011 | [README](system-registry-apis/README.md) |
| **translator-apis** | Bhashini translation, multi-language support | 9003 | [README](translator-apis/README.md) |
| **vault-apis** | Encryption, signing, HashiCorp Vault integration | 9010 | [README](vault-apis/README.md) |
| **wso2-cred-generator-apis** | WSO2 tenant/business/data processor credentials | 9004 | [README](wso2-cred-generator-apis/README.md) |


## Deployment Utilities

| File | Purpose |
|------|---------|
| `configmap.yaml` | Single Kubernetes ConfigMap for all 13 services. Populate placeholder values before applying. See the [Deployment Guide](../Installation%20and%20Configuration/JCMS_Deployment_Guide.docx) for details. |
| `backendDeploy.sh` | Automated build-and-deploy script. Builds Docker images, pushes to registry, and applies Kubernetes manifests. Run `./backendDeploy.sh --help` for usage. |

## Common

- **Java**: 21  
- **Build**: Maven (`./mvnw clean package`)  
- **Config**: Environment variables and optional `application*.yml` / `application.properties`; no hardcoded IPs or secrets.  
- **Deployment**: Per-service `deployment/` with Dockerfile and Kubernetes manifests where present.

## License

See [root LICENSE](../LICENSE) and DPDP CMS terms.
