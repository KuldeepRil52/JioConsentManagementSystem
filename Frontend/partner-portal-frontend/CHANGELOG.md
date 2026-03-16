# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- PDF viewer component with route-based navigation for Terms of Use, Privacy Policy, and Annexure documents
- Support for opening PDFs via routes (`/pdf/terms-of-use`, `/pdf/privacy-policy`, `/pdf/annexure`)
- Hyperlink interception in PDF viewer to handle internal PDF links
- Full-screen PDF viewer mode without application header and sidebar
- Data Processor count card in DPO Dashboard
- Multilingual configuration warning modal for language dropdowns
- Date range filtering for DPO Dashboard API calls
- Latest version filtering for processing activities in Create Consent and Purpose Container
- Whitespace trimming for processor name inputs
- Language dropdown styling consistency across components

### Changed
- Updated DPO Dashboard to display "0" instead of blank for zero counts
- Refactored `getSystemConfig` API call to execute after `getUserProfile` to ensure `businessId` is available
- Modified consent pie chart calculation to exclude pending renewals and normalize percentages
- Updated SchedulerStats design to match Templates.js styling
- Improved PDF opening mechanism to use routes instead of direct file URLs
- Updated README.md with proper Git standards

### Fixed
- Fixed build error caused by missing closing div tag in Processing Activities card
- Fixed tab content not displaying in UserDashboardAccessibility component
- Fixed PDF hyperlinks opening blank tabs by implementing route-based navigation
- Fixed footer links opening same PDF in multiple tabs
- Fixed PDFs not opening after window.open interception changes
- Fixed hyperlinks within PDFs pointing to incorrect URLs
- Fixed date filter not affecting API calls in DPO Dashboard
- Fixed pie chart calculation issues (percentages exceeding 100%, single segment display)

## [1.0.0] - 2025-01-XX

### Added
- Initial release of Jio Consent Management System Frontend
- User authentication and authorization
- DPO Dashboard with statistics and charts
- Consent management functionality
- Purpose and Processing Activity management
- Data Processor management
- Grievance management
- Notification system
- Template management (Email, SMS, Forms)
- Cookie consent integration
- ROPA (Record of Processing Activities) management
- Audit and compliance features
- Breach notification system
- User and role management
- Business group management
- System configuration
- Multilingual support
- Accessibility features
- Responsive design

### Security
- Session management
- Protected routes
- RBAC (Role-Based Access Control)
- Input validation and sanitization

---

## Version History

### Version Format

We use [Semantic Versioning](https://semver.org/):
- **MAJOR** version when you make incompatible API changes
- **MINOR** version when you add functionality in a backward compatible manner
- **PATCH** version when you make backward compatible bug fixes

### Change Categories

- **Added** for new features
- **Changed** for changes in existing functionality
- **Deprecated** for soon-to-be removed features
- **Removed** for now removed features
- **Fixed** for any bug fixes
- **Security** for vulnerability fixes

---

## How to Update This Changelog

When making changes:

1. Add your changes under the `[Unreleased]` section
2. Use the appropriate category (Added, Changed, Fixed, etc.)
3. Write clear, concise descriptions
4. Reference issue numbers when applicable: `Closes #123`
5. When releasing a new version:
   - Move `[Unreleased]` items to a new version section
   - Update the date
   - Create a new `[Unreleased]` section

### Example Entry

```markdown
### Fixed
- Fixed issue where PDFs were not opening correctly in Safari browser
  - Resolved by implementing blob URL handling for cross-browser compatibility
  - Closes #456
```

---

## Links

- [Keep a Changelog](https://keepachangelog.com/)
- [Semantic Versioning](https://semver.org/)
- [GitHub Releases](https://github.com/your-org/JioConsentManagementSystem-Frontend/releases)

