# Jio Consent Management System - Frontend

A comprehensive frontend application for managing consent templates, processing activities, data processors, and compliance reporting. Built with React, Redux, and JDS Core components.

## 📋 Table of Contents

- [About](#about)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Getting Started](#getting-started)
- [Available Scripts](#available-scripts)
- [Environment Configuration](#environment-configuration)
- [Project Structure](#project-structure)
- [Build and Test](#build-and-test)
- [Git Guidelines](#git-guidelines)
- [Contributing](#contributing)
- [License](#license)

## 🎯 About

The Jio Consent Management System Frontend is a React-based application that provides a user-friendly interface for managing consent templates, processing activities, data processors, grievance management, and compliance reporting. The application follows modern web development practices and uses Redux for state management.

## ✨ Features

- **Consent Template Management**: Create, edit, and manage consent templates
- **Data Processing Register**: Track purposes, data types, processing activities, and data processors
- **Grievance Management**: Handle grievance requests with SLA tracking
- **Cookie Management**: Manage cookie consents and tracking
- **Audit & Compliance**: Generate compliance reports and audit logs
- **Multi-language Support**: Support for multiple languages with translation capabilities
- **Accessibility**: Built with accessibility features and WCAG compliance
- **Dashboard Analytics**: Comprehensive dashboard with statistics and visualizations

## 📦 Prerequisites

Before you begin, ensure you have the following installed:

- **Node.js**: v14.x or higher
- **npm**: v6.x or higher (comes with Node.js)
- **Git**: For version control

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone https://JPL-Limited@dev.azure.com/JPL-Limited/Jio%20Consent%20Manager/_git/NEGD_FrontEnd_Code
   cd JioConsentManagementSystem-Frontend
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Set up environment variables**
   - Copy `.env.dev` to create your environment configuration
   - Update the environment variables as needed (see [Environment Configuration](#environment-configuration))

## 🏃 Getting Started

### Development Mode

To start the development server:

```bash
npm run start
```

Or for specific environments:

```bash
# Development environment
npm run start:dev

# Non-production environment
npm run start:nonprod

# Production environment
npm run start:prod
```

The application will be available at `http://localhost:1234` (default Parcel port).

## 📜 Available Scripts

| Script | Description |
|--------|-------------|
| `npm run start` | Start development server with default configuration |
| `npm run start:dev` | Start development server with dev environment variables |
| `npm run start:nonprod` | Start development server with nonprod environment variables |
| `npm run start:prod` | Start development server with production environment variables |
| `npm run build` | Build the application for production |
| `npm run build:dev` | Build for development environment |
| `npm run build:nonprod` | Build for non-production environment |
| `npm run build:prod` | Build for production environment |
| `npm test` | Run test suite (Jest) |

## 🔧 Environment Configuration

The project uses environment-specific configuration files:

- `.env.dev` - Development environment
- `.env.nonprod` - Non-production environment
- `.env.prod` - Production environment

Key environment variables include:
- API base URLs
- Tenant configuration
- Session management settings

For detailed environment setup, refer to `ENV_SETUP.md`.

## 📁 Project Structure

```
JioConsentManagementSystem-Frontend/
├── src/
│   ├── components/          # React components
│   ├── store/              # Redux store configuration
│   │   ├── actions/        # Redux actions
│   │   ├── reducers/       # Redux reducers
│   │   └── constants/      # Redux constants
│   ├── styles/             # CSS/SCSS stylesheets
│   ├── utils/              # Utility functions
│   │   ├── config.js       # API configuration
│   │   └── ...
│   └── assets/             # Static assets
├── .env.dev                # Development environment variables
├── .env.nonprod            # Non-production environment variables
├── .env.prod               # Production environment variables
├── .gitignore              # Git ignore rules
├── package.json            # Project dependencies and scripts
└── README.md               # This file
```

## 🏗️ Build and Test

### Building for Production

```bash
# Build for specific environment
npm run build:dev
npm run build:nonprod
npm run build:prod
```

The build output will be in the `dist/` directory.

### Running Tests

```bash
npm test
```

## 📝 Git Guidelines

### Excluded Files and Directories

The following files and directories are excluded from Git tracking (already in `.gitignore`):

- `node_modules/` - Dependencies (install via `npm install`)
- `.parcel-cache/` - Parcel build cache
- `dist/` - Build output directory

**Important**: Do not commit these directories to Git. They are automatically excluded via `.gitignore`.

### Commit Guidelines

- Use clear, descriptive commit messages
- Follow conventional commit format when possible
- Keep commits focused on a single feature or fix
- Test your changes before committing

### Branch Strategy

- `main` or `master` - Production-ready code
- `develop` - Development branch
- Feature branches - `feature/feature-name`
- Bug fix branches - `fix/bug-description`

## 🤝 Contributing

We welcome contributions! Please follow these guidelines:

1. **Fork the repository** and create a feature branch
2. **Make your changes** following the project's coding standards
3. **Test your changes** thoroughly
4. **Commit your changes** with clear, descriptive messages
5. **Push to your fork** and create a Pull Request
6. **Ensure your PR** includes:
   - Clear description of changes
   - Reference to related issues (if any)
   - Updated documentation if needed

### Code Style

- Follow existing code patterns and conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Maintain consistent formatting

## 📄 License

This project is licensed under the ISC License.

## 👥 Authors

- Development Team - JPL Limited

## 🙏 Acknowledgments

- JDS Core Components
- React Community
- All contributors to this project

---

**Note**: For detailed documentation on specific features, refer to the documentation files in the project root:
- `ENV_SETUP.md` - Environment setup guide
- `SANDBOX_MODE_GUIDE.md` - Sandbox mode documentation
- `GUIDED_TOUR_DOCUMENTATION.md` - Guided tour feature documentation
