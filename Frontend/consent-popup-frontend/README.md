# Consent Popup Frontend

## Overview

The **Consent Popup Frontend** is a lightweight React application used for consent and redirect flows in the Jio Consent Management System (JCMS). It supports cookie consent UX, redirect-based integrations, and embedded consent experiences in line with the DPDP Act 2023.

Built with **React**, **Parcel**, and **JDS (Jio Design System)** (or custom components where applicable).

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Environment Configuration](#environment-configuration)
- [Build](#build)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Cookie / consent popup** — Banner and preference UI for cookie consent
- **Redirect flows** — Integration and redirect-based consent capture
- **Embeddable** — Can be used in iframe or redirect flows
- **Multi-environment** — Dev, nonprod, and prod builds via env files
- **Lightweight** — Parcel-based build for fast load

## Prerequisites

- **Node.js**: v14.x or higher
- **npm**: v6.x or higher
- **Git**: Version control

## Installation

1. **Navigate to the project**

   ```bash
   cd Frontend/consent-popup-frontend
   ```

2. **Install dependencies**

   ```bash
   npm install
   ```

3. **Configure environment**

   - Copy `.env.dev` (or `.env.nonprod` / `.env.prod`) and set API base URLs and any tenant/site identifiers. Do not commit secrets.

## Getting Started

### Development

```bash
# Default
npm run start

# With .env.dev (e.g. port 3051)
npm run start:dev

# Non-production
npm run start:nonprod

# Production-like
npm run start:prod
```

### Build

```bash
npm run build:dev
npm run build:nonprod
npm run build:prod
```

Output is in `dist/` (or as configured by Parcel).

## Environment Configuration

- `.env.dev` — Development
- `.env.nonprod` — Non-production
- `.env.prod` — Production

Configure API URLs and any keys/identifiers required for cookie/consent and redirect APIs. Keep secrets out of version control.

## Build

Use the appropriate build script for your target environment. Ensure the correct env file is used (e.g. `env-cmd -f .env.prod` for production).

## Contributing

See the root repository CONTRIBUTING and CODE_OF_CONDUCT guidelines.

## License

See the root repository LICENSE and DPDP CMS terms.
