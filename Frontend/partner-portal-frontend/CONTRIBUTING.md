# Contributing to Jio Consent Management System

First off, thank you for considering contributing to the Jio Consent Management System! It's people like you that make this project better.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Pull Requests](#pull-requests)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Testing Guidelines](#testing-guidelines)
- [Code Review Process](#code-review-process)
- [Project Structure](#project-structure)

## Code of Conduct

This project adheres to a Code of Conduct that all contributors are expected to follow. Please read the [Code of Conduct](CODE_OF_CONDUCT.md) so that you can understand what actions will and will not be tolerated.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the issue list as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

**How Do I Submit a (Good) Bug Report?**

Bugs are tracked as [GitHub issues](https://github.com/your-org/JioConsentManagementSystem-Frontend/issues). After you've determined which repository your bug is related to, create an issue on that repository and provide the following information:

1. **Use a clear and descriptive title** for the issue to identify the problem.
2. **Describe the exact steps to reproduce the problem** in as many details as possible.
3. **Describe the behavior you observed** after following the steps and point out what exactly is the problem with that behavior.
4. **Explain which behavior you expected to see** instead and why.
5. **Include screenshots and animated GIFs** if applicable, which show you following the described steps and clearly demonstrate the problem.
6. **Include error messages and stack traces** if applicable.
7. **Include your environment details:**
   - Operating System and version
   - Browser and version
   - Node.js version
   - Package manager version (npm/yarn)

**Example:**

```markdown
**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '....'
3. Scroll down to '....'
4. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Screenshots**
If applicable, add screenshots to help explain your problem.

**Environment:**
- OS: [e.g., Windows 10]
- Browser: [e.g., Chrome 120]
- Node.js: [e.g., 18.17.0]
- npm: [e.g., 9.6.7]

**Additional context**
Add any other context about the problem here.
```

### Suggesting Enhancements

Enhancement suggestions are tracked as [GitHub issues](https://github.com/your-org/JioConsentManagementSystem-Frontend/issues). When creating an enhancement suggestion, please include:

1. **Use a clear and descriptive title** for the issue to identify the suggestion.
2. **Provide a step-by-step description** of the suggested enhancement in as many details as possible.
3. **Provide specific examples** to demonstrate the steps.
4. **Describe the current behavior** and explain which behavior you expected to see instead and why.
5. **Explain why this enhancement would be useful** to most users.
6. **List some other applications where this enhancement exists** if applicable.

### Pull Requests

- Fill in the required template
- Do not include issue numbers in the PR title
- Include screenshots and animated GIFs in your pull request whenever possible
- Follow the [Coding Standards](#coding-standards)
- Include thoughtfully-worded, well-structured tests
- Document new code based on the [Documentation Styleguide](#documentation-styleguide)
- End all files with a newline
- Place imports in the following order:
  1. External dependencies
  2. Internal modules
  3. Relative imports
- Place class properties in the following order:
  1. Static methods
  2. Constructor
  3. Lifecycle methods
  4. Event handlers
  5. Render methods

## Development Setup

### Prerequisites

- Node.js (v18.x or higher)
- npm (v9.x or higher) or yarn
- Git

### Installation

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/your-username/JioConsentManagementSystem-Frontend.git
   cd JioConsentManagementSystem-Frontend
   ```

3. Install dependencies:
   ```bash
   npm install
   # or
   yarn install
   ```

4. Create a `.env` file in the root directory (see `.env.example` for reference):
   ```bash
   cp .env.example .env
   ```

5. Start the development server:
   ```bash
   npm start
   # or
   yarn start
   ```

### Development Workflow

1. Create a branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/your-bug-fix
   ```

2. Make your changes and test them thoroughly

3. Commit your changes following the [Commit Message Guidelines](#commit-message-guidelines)

4. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```

5. Create a Pull Request on GitHub

## Coding Standards

### JavaScript/React

- Use functional components with hooks
- Follow the existing code style
- Use meaningful variable and function names
- Keep functions small and focused
- Avoid deep nesting (max 3 levels)
- Use ES6+ features
- Prefer `const` over `let`, avoid `var`
- Use arrow functions for callbacks
- Destructure props and state when appropriate

### Code Style

- Use 2 spaces for indentation
- Use single quotes for strings
- Add trailing commas in multi-line objects/arrays
- Use semicolons
- Maximum line length: 100 characters
- Use meaningful comments to explain "why", not "what"

### File Naming

- Components: PascalCase (e.g., `UserDashboard.js`)
- Utilities: camelCase (e.g., `formatDate.js`)
- Constants: UPPER_SNAKE_CASE (e.g., `API_ENDPOINTS.js`)
- Styles: kebab-case (e.g., `user-dashboard.css`)

### Component Structure

```javascript
// 1. Imports
import React, { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';

// 2. Component
const ComponentName = () => {
  // 3. Hooks
  const dispatch = useDispatch();
  const [state, setState] = useState(null);

  // 4. Effects
  useEffect(() => {
    // Effect logic
  }, []);

  // 5. Handlers
  const handleClick = () => {
    // Handler logic
  };

  // 6. Render
  return (
    <div>
      {/* JSX */}
    </div>
  );
};

// 7. Export
export default ComponentName;
```

### Redux

- Keep actions, reducers, and selectors in separate files
- Use action creators for all actions
- Keep state normalized
- Use Redux Toolkit when possible
- Avoid mutating state directly

### CSS/Styling

- Use CSS modules or styled-components for component-specific styles
- Follow BEM naming convention when using regular CSS
- Keep styles close to components
- Use CSS variables for theming
- Ensure responsive design

## Commit Message Guidelines

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: A new feature
- `fix`: A bug fix
- `docs`: Documentation only changes
- `style`: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- `refactor`: A code change that neither fixes a bug nor adds a feature
- `perf`: A code change that improves performance
- `test`: Adding missing tests or correcting existing tests
- `chore`: Changes to the build process or auxiliary tools and libraries

### Examples

```
feat(dashboard): add data processor count card

Add a new card to display the count of data processors in the DPO dashboard.
The card shows the count from the API response field "dataProcessor".

Closes #123
```

```
fix(signup): prevent PDF opening in multiple tabs

Fix issue where clicking footer links opened the same PDF in multiple tabs
by implementing proper event listener cleanup.

Fixes #456
```

## Testing Guidelines

- Write tests for new features and bug fixes
- Maintain or improve code coverage
- Test user interactions, not implementation details
- Use descriptive test names
- Follow the AAA pattern (Arrange, Act, Assert)

### Running Tests

```bash
npm test
# or
yarn test
```

### Test Coverage

```bash
npm run test:coverage
# or
yarn test:coverage
```

## Code Review Process

1. All code submissions require review
2. Address review comments promptly
3. Be respectful and constructive in reviews
4. Approve only if you've tested the changes
5. Request changes if the code doesn't meet standards

### Review Checklist

- [ ] Code follows the project's style guidelines
- [ ] Tests are included and passing
- [ ] Documentation is updated
- [ ] No console.logs or debug code
- [ ] No commented-out code
- [ ] Error handling is appropriate
- [ ] Performance considerations are addressed
- [ ] Accessibility standards are met
- [ ] Security best practices are followed

## Project Structure

```
src/
├── components/          # React components
├── routes/             # Route definitions
├── store/              # Redux store configuration
│   ├── actions/        # Action creators
│   ├── reducers/       # Redux reducers
│   └── constants/      # Action constants
├── utils/              # Utility functions
├── styles/             # Global styles
├── assets/             # Static assets
└── jds-styles/         # JDS component styles
```

## Documentation Styleguide

- Use JSDoc comments for functions and components
- Document complex logic
- Keep README.md updated
- Update CHANGELOG.md for user-facing changes

## Questions?

If you have any questions, please:
- Open an issue with the `question` label
- Contact the maintainers
- Check existing documentation

Thank you for contributing! 🎉

