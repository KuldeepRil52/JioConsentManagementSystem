# User Portal Frontend

## Overview

The **User Portal Frontend** is a React 18 / Parcel 2 application for data principals (end users) to view, manage, and withdraw their consents in line with the Digital Personal Data Protection (DPDP) Act 2023. It is part of the JCMS (Jio Consent Management System) suite.

Built with **React 18**, **Parcel 2**, **Redux Toolkit**, and **SASS**.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Available Scripts](#available-scripts)
- [Environment Configuration](#environment-configuration)
- [Build for Production](#build-for-production)
- [Contributing](#contributing)
- [License](#license)

## Features

- **View Active Consents** — Browse all consents currently granted to data fiduciaries
- **Withdraw Consent** — Revoke any previously granted consent with immediate effect
- **Consent History** — View a full audit trail of consent events and changes
- **OTP-Based Authentication** — Secure, passwordless login via one-time password
- **Multi-Language Support** — Localisation via the integrated translator service
- **Grievance Filing** — Raise and track grievance requests directly from the portal
- **Multi-Environment Builds** — Dev, nonprod, and prod configurations via env files

## Prerequisites

- **Node.js**: 18 LTS or 20 LTS
- **npm**: bundled with Node.js (v9+ recommended)
- **Git**: for version control

## Installation

```bash
cd Frontend/user-portal-frontend
npm install
```

## Available Scripts

| Script | Description |
|--------|-------------|
| `npm run start` | Start development server on port 3000 (no env file) |
| `npm run start:dev` | Start development server on port 4200 with `.env.dev` |
| `npm run start:nonprod` | Start development server on port 3000 with `.env.nonprod` |
| `npm run start:prod` | Start development server on port 3000 with `.env.prod` |
| `npm run build:dev` | Build for development environment |
| `npm run build:nonprod` | Build for non-production environment |
| `npm run build:prod` | Build for production environment |

## Environment Configuration

The project uses environment-specific `.env` files loaded by `env-cmd`:

| File | Purpose |
|------|---------|
| `.env.dev` | Local development |
| `.env.nonprod` | Staging / non-production |
| `.env.prod` | Production |

Copy `.env.example` to the appropriate file and fill in your values:

```bash
cp .env.example .env.dev
```

Key variables:

| Variable | Description |
|----------|-------------|
| `REACT_APP_API_URL` | Partner portal API base URL |
| `REACT_APP_CONSENT_URL` | Consent service base URL |
| `REACT_APP_GRIEVANCE_URL` | Grievance service base URL |
| `REACT_APP_TRANSLATE_URL` | Translator service base URL |
| `REACT_APP_META_URL` | User portal metadata service URL |
| `REACT_APP_BASE_URL` | Public base URL of this app |
| `REACT_APP_ENV` | Environment identifier (`dev`, `nonprod`, `prod`) |

Do **not** commit real `.env.*` files to version control — they are excluded by `.gitignore`.

## Build for Production

```bash
npm run build:prod
```

Output is written to `dist/`. Serve the contents of `dist/` from any static file host or CDN.

## Contributing

Please read the root `CONTRIBUTING.md` before submitting pull requests. Follow the branch strategy and commit conventions described there.

## License

See the root `LICENSE` file for licensing terms.
