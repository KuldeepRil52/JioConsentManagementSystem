# Contributing Guidelines

## For Jio Consent Management System(JCMS)

Thank you for considering contributing to the JCMP Platform.

This document outlines the standards, workflows, and guidelines for contributing to both backend (Java/Spring Boot) and frontend (React/JavaScript) modules.

## 📚 Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [How Can I Contribute?](#how-can-i-contribute)
   - [Reporting Bugs](#reporting-bugs)
   - [Suggesting Enhancements](#suggesting-enhancements)
3. [Development Setup](#development-setup)
   - [Backend Setup](#backend-setup)
   - [Frontend Setup](#frontend-setup)
4. [Coding Standards](#coding-standards)
   - [Backend (Java)](#backend-java)
   - [Frontend (React/JS)](#frontend-reactjs)
5. [Commit Message Guidelines](#commit-message-guidelines)
6. [Testing Guidelines](#testing-guidelines)
7. [Pull Request Process](#pull-request-process)
8. [Code Review Process](#code-review-process)
9. [Project Structure](#project-structure)
10. [Security Considerations](#security-considerations)
11. [Getting Help](#getting-help)

## 1. Code of Conduct

This project follows the organizational Code of Conduct.

Please review it before contributing.

➡️ Refer to [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## 2. How Can I Contribute?

### 🐞 Reporting Bugs

Before creating a bug report:

- Check existing issues (Azure DevOps / GitHub).
- Verify if the bug already exists in the latest build.

#### ✔️ Good Bug Report Checklist

- Clear and descriptive title
- Steps to reproduce
- Expected behavior
- Actual behavior
- Logs, stack traces, console output
- Screenshots or GIFs (if applicable)
- Environment details:
  - OS
  - Browser (for frontend)
  - Java version / Node.js version
  - Deployment mode (Local / Dev / QA / UAT / Prod)

#### 📝 Bug Report Template

```markdown
### Describe the bug
A concise description of the issue.

### Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Scroll to '...'

### Expected Behavior
What should happen?

### Actual Behavior
What actually happened?

### Environment
- OS:
- Browser:
- Java Version:
- Node Version:
- Deployment Mode:

### Additional context
Add any other context about the problem.
```

### 💡 Suggesting Enhancements

Enhancement requests must include:

- Clear title
- Detailed proposal
- Why the feature is needed
- Alternatives considered
- Impact on existing modules
- References (if any)

## 3. Development Setup

### 🛠 Backend (Java / Spring Boot)

#### Prerequisites

- Java 21
- Maven 3.8+
- MongoDB 5+
- Kafka 2.8+ (Kerberos-enabled)
- Docker 20.10+
- IntelliJ IDEA (recommended)

#### Setup Steps

```bash
git clone <repo-url>
cd backend
cp src/main/resources/application.yml src/main/resources/application-local.yml
vi src/main/resources/application-local.yml
```

#### Start Required Services

```bash
docker run -d --name mongodb -p 27017:27017 mongo:5.0
```

#### Build & Run

```bash
mvn clean install
mvn test
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

#### Health Check

```bash
curl http://localhost:<port>/actuator/health
```

### 💻 Frontend (React / JavaScript)

#### Prerequisites

- Node.js 18.x+
- npm 9.x+ / yarn
- Git

#### Setup Steps

```bash
git clone <repo-url>
cd frontend
npm install
cp .env.example .env
npm start
```

## 4. Coding Standards

### 🧩 Backend (Java + Spring Boot)

#### Java Best Practices

Use Java 21 features:
- Records
- Pattern matching
- Sealed classes

- DTOs must be immutable
- Use Lombok responsibly
- Maintain a consistent exception hierarchy
- Follow layered architecture: Controller → Service → Repository → DTO
- Structured logs with correlation IDs (tenant + request tracking)

#### Formatting Rules

- 4-space indentation
- 120-character line limit
- Braces required for all conditionals
- Import order:
  - java.*
  - javax.*
  - Third-party
  - com.jio.*

#### Naming Conventions

| Type | Convention |
|------|------------|
| Classes | PascalCase |
| Methods | camelCase |
| Variables | camelCase |
| Constants | UPPER_SNAKE_CASE |
| Packages | lowercase |
| Tests | *Test.java |

### 🎨 Frontend (React + JavaScript)

#### Standards

- Functional components with Hooks
- ES6+ syntax
- Meaningful variable names
- Avoid deep nesting (max depth: 3 levels)
- End all files with newline
- Types (if using TS) must be strongly typed

#### Import Order

- External libraries
- Internal modules
- Relative imports

#### Component Layout Order

- Static metadata
- Hooks
- Event handlers
- Render logic

## 5. Commit Message Guidelines

We follow Conventional Commits format:

```
<type>(<scope>): <short summary>

<body>

<footer>
```

### Commit Types

- **feat** — New feature
- **fix** — Bug fix
- **docs** — Documentation updates
- **style** — Non-breaking formatting updates
- **refactor** — Code rework without feature change
- **test** — Unit/Integration test additions
- **chore** — CI, build, tooling updates

### Examples

```bash
feat(api): add pagination to event listing
fix(kafka): resolve null pointer during consumer start
```

## 6. Testing Guidelines

### Backend

- Minimum 80% test coverage
- Use:
  - JUnit 5
  - Mockito

Structure:
```
src/test/java/
├── controller
├── service
├── repository
└── integration
```

### Frontend

Use:
- Jest
- React Testing Library

Must include:
- Component tests
- API mocks
- Edge case tests
- Error-state scenarios

## 7. Pull Request Process

### ✔ Before Creating a PR

- All tests must pass
- Coverage must be ≥ 80%
- Lint checks must pass
- Documentation updated
- CHANGELOG.md updated

### ✔ PR Must Include

- Summary of changes
- Why the changes are needed
- Screenshots/GIFs (UI changes)
- Tests added/updated
- No commented-out code
- No hardcoded secrets

## 8. Code Review Process

Reviewers will validate:

- Code quality & formatting
- Architecture alignment
- Multi-tenant compliance
- Security impacts
- Performance implications
- Logging & error structures

After approval:
- PR must be Squash merged
- Feature branch must be deleted

## 9. Project Structure (Common)

```
platform/
├── backend/
│   ├── controller
│   ├── service
│   ├── repository
│   ├── dto
│   ├── entity
│   ├── config
│   ├── util
│   └── tests
└── frontend/
    ├── src/components
    ├── src/pages
    ├── src/hooks
    ├── src/services
    ├── public/
    └── tests
```

## 10. Security Considerations

- No secrets in codebase
- Strict input validation
- Use encrypted credentials
- Do not log sensitive data
- Mandatory tenant-ID validation
- Schema validation on all API payloads
- Follow principle of least privilege
- Regular dependency audits

### Backend:
```bash
mvn dependency-check:check
```

### Frontend:
```bash
npm audit
```

## 11. Getting Help

- **Documentation**: Internal Confluence / Project Docs
- **Support Email**: Jio.ConsentSupport@ril.com
- **Issue Tracking**: Azure DevOps / GitHub
- **Architecture Docs**: docs/architecture/

## Contributors

- **Raviraj Mishra** — UI/UX Lead

## ✅ Thank You for Contributing!

**This document is submitted only for Code for Consent Challenge, organised by NeGD (a part of MeitY) and shall not be used for any other purpose.**