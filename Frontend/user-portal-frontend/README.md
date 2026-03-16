# User Portal Frontend (Data Principal Portal)

## Overview

The **User Portal Frontend** is the data principal (citizen) facing application of the Jio Consent Management System (JCMS). It allows users to view and manage their consents, submit grievances, track integration and system requests, and access parental consent flows in line with the DPDP Act 2023.

Built with **React**, **Redux**, **Parcel**, and **JDS (Jio Design System)** components.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Environment Configuration](#environment-configuration)
- [Build and Test](#build-and-test)
- [Contributing](#contributing)
- [License](#license)

## Features

- **Consent management** — View and manage consent preferences
- **Grievance** — Submit and track grievance requests
- **Integration / system requests** — Create and track integration and fiduciary requests
- **Parental consent** — Flows for minor data principals
- **Multi-language support** — Translation and accessibility
- **Responsive UI** — JDS Core components and modern UX

## Prerequisites

- **Node.js**: v14.x or higher
- **npm**: v6.x or higher
- **Git**: Version control

## Installation

1. **Clone the repository** (or use the parent repo and navigate to this folder):

   ```bash
   cd Frontend/user-portal-frontend
   ```

2. **Install dependencies**

   ```bash
   npm install
   ```

3. **Configure environment**

   - Copy `.env.dev` (or `.env.nonprod` / `.env.prod`) and set API base URLs and other variables as per your environment. See [Environment Configuration](#environment-configuration).

## Getting Started

### Development

```bash
# Default (port 3000)
npm run start

# With .env.dev (port 4200)
npm run start:dev

# Non-production env
npm run start:nonprod

# Production-like env
npm run start:prod
```

### Build

```bash
npm run build:dev      # using .env.dev
npm run build:nonprod  # using .env.nonprod
npm run build:prod     # using .env.prod
```

## Environment Configuration

Use environment-specific files:

- `.env.dev` — Development
- `.env.nonprod` — Non-production / staging
- `.env.prod` — Production

Set at least:

- API base URLs (consent, grievance, auth, etc.)
- Any feature flags or tenant identifiers required by your deployment

Do not commit real secrets or internal URLs. See project root or `ENV_SETUP.md` in partner-portal-frontend for patterns.

## Build and Test

- **Build**: `npm run build:dev` (or `build:nonprod` / `build:prod`)
- **Test**: Use the project’s test script if defined (e.g. `npm test`)

## Contributing

See the root repository CONTRIBUTING and CODE_OF_CONDUCT guidelines.

## License

See the root repository LICENSE and DPDP CMS terms.
