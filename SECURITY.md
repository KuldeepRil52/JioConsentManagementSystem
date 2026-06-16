# Security Policy

This document describes the security policy for the **JCMS Consent Management System** — an implementation of India's Digital Personal Data Protection (DPDP) Act 2023, built on Spring Boot microservices and React frontends.

---

## Supported Versions

Only the versions listed below receive security updates. If you are running an unsupported version, please upgrade before reporting.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: Yes |
| < 1.0   | :x: No             |

---

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue in JCMS, **please do not open a public GitHub issue**. Instead, report it responsibly via one of the channels below.

### How to Report

**Email:** security@dpdp-cms.gov.in

Please encrypt your report using our PGP key if the information is highly sensitive (key available on request from the email above).

### What to Include

To help us triage and reproduce the issue quickly, your report should contain:

1. **Description** — A clear summary of the vulnerability, including the affected component (e.g., `consent-core-apis`, `auth-apis`, frontend portal name).
2. **Steps to Reproduce** — A minimal, reliable set of steps or a proof-of-concept (PoC) that demonstrates the issue.
3. **Expected vs. Actual Behaviour** — What the system should do versus what it actually does.
4. **Impact Assessment** — Your assessment of severity (e.g., data exposure, authentication bypass, privilege escalation) and the data subjects or data principals potentially affected.
5. **Affected Version** — The JCMS release version (e.g., `v1.0.0`) and, if known, the specific service or module.
6. **Environment** — Deployment type (Kubernetes, Docker Compose, bare-metal), Java version, and any relevant configuration details (without exposing actual secrets).
7. **Supporting Evidence** — Screenshots, HTTP request/response captures, or log excerpts that substantiate the finding (redact any personal data before attaching).

---

## Responsible Disclosure Policy

We follow a coordinated disclosure model:

| Commitment | Timeline |
| ---------- | -------- |
| Initial acknowledgement of your report | Within **72 hours** of receipt |
| Confirm whether the issue is valid | Within **7 days** |
| Provide a remediation timeline for valid critical/high issues | Within **14 days** of validation |
| Notify you when a fix is released | Before or at public disclosure |

We ask that you:

- **Do not publicly disclose** details of the vulnerability until we have released a patch and notified you.
- **Do not access, modify, or exfiltrate** data beyond what is strictly necessary to demonstrate the vulnerability.
- **Do not perform** denial-of-service attacks, social engineering, or physical security tests.
- Act in good faith — we will do the same.

We will credit researchers who report valid vulnerabilities in our release notes unless you prefer to remain anonymous.

---

## Out of Scope

The following are **not** eligible for vulnerability reports under this policy:

- **Third-party dependencies** (e.g., Spring Boot, Keycloak, WSO2, PostgreSQL, Kafka). Please report those issues to the respective upstream projects and inform us so we can track upgrade timelines.
- **Known issues** already listed in the project's open issues or this repository's security advisories.
- **Theoretical vulnerabilities** with no practical exploit path.
- Issues in unsupported versions (see table above).
- Self-XSS, clickjacking on pages with no sensitive actions, or CSRF on logout-only endpoints.
- Missing security headers with no demonstrated exploitable impact.
- Rate-limiting issues that require an unrealistic number of requests to have any effect.

---

## DPDP Act 2023 Security Considerations

JCMS is built to support organisations in complying with India's Digital Personal Data Protection Act 2023. The following architectural decisions are directly relevant to security reviewers:

- **Consent Lifecycle Integrity** — All consent grant, modification, and withdrawal events are immutable audit records. Any vulnerability that permits tampering with consent records is treated as critical severity.
- **Data Principal Authentication** — The `auth-apis` service mediates all data principal sessions. Authentication bypasses or session fixation issues are critical.
- **Least-Privilege Microservices** — Each backend service runs with scoped database credentials and limited inter-service trust. Lateral movement vulnerabilities between services are high severity.
- **Vault Integration** — Secrets are managed via `vault-apis`. Hard-coded credentials or secret-exposure bugs are critical by definition.
- **PII in Transit and at Rest** — Personal data is encrypted in transit (TLS) and at rest. Vulnerabilities exposing PII in logs, error messages, or API responses are high severity.
- **Grievance Module** — The `grivance-module-apis` service handles regulatory complaints. Tampering or denial-of-service against this module may constitute a regulatory breach.

When assessing severity, reviewers should consider the DPDP Act's obligations around breach notification (within 72 hours to the Data Protection Board) and the potential for significant financial penalties.

---

## Bug Bounty

JCMS does **not** currently operate a formal bug bounty programme. We are grateful to the security community for responsible disclosures and will acknowledge valid reports in our changelogs. A formal programme may be introduced in a future release.

---

## Contact

| Purpose | Contact |
| ------- | ------- |
| Security vulnerabilities | security@dpdp-cms.gov.in |
| General enquiries | See [README.md](README.md) |
| Code of Conduct matters | See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) |
