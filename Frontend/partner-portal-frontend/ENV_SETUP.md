# Environment Variables Setup

## Required Environment Variables

Create the following files in the root directory:
- `.env.dev` (for development)
- `.env.nonprod` (for non-production)
- `.env.prod` (for production)

## Environment Variables Template

Copy the following into your environment files and update the values as needed:

```bash
# API Base URLs - Replace <api-host> with your API gateway host
REACT_APP_API_URL=https://<api-host>:8443/partnerportal
REACT_APP_CONSENT_URL=https://<api-host>:8443/negd/consent
REACT_APP_GRIEVANCE_URL=https://<api-host>:8443/grievance
REACT_APP_COOKIE_URL=https://<api-host>:8443
REACT_APP_AUDIT_URL=https://<api-host>:8443
REACT_APP_TRANSLATOR_URL=https://<api-host>:8443/translator
REACT_APP_ENCRYPT_URL=YOUR_ENCRYPTION_URL_HERE
REACT_APP_INTEGRATION_BASE_URL=http://<integration-host>:9012/integrationCreateRequest
```

## Usage

These environment variables are used in `src/utils/config.js` to configure all API endpoints.

## Notes

- Never commit `.env.*` files to version control
- Ensure these are listed in `.gitignore`
- Update values per environment (dev/nonprod/prod)

