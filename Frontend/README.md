# JCMS Frontend Applications

Frontend applications for the **Jio Consent Management System (JCMS)** — DPDP Act 2023 compliant consent and grievance UX.

For system overview and API configuration, see the [root README](../README.md).

## Applications

| Application | Description | README |
|-------------|-------------|--------|
| **partner-portal-frontend** | Partner/DPO portal — consent templates, data processors, grievance, cookie management, audit | [README](partner-portal-frontend/README.md) |
| **user-portal-frontend** | Data principal portal — consents, grievances, integration/system requests, parental consent | [README](user-portal-frontend/README.md) |
| **consent-popup-frontend** | Cookie consent popup and redirect-based consent flows | [README](consent-popup-frontend/README.md) |

## Common

- **Stack**: React, Parcel (or as per app), JDS (Jio Design System) where used  
- **Env**: Use `.env.dev`, `.env.nonprod`, `.env.prod` per app; do not commit secrets or internal URLs.  
- **Run**: `npm install` then `npm run start` or `npm run start:dev` (see each app’s README).

## License

See [root LICENSE](../LICENSE) and DPDP CMS terms.
