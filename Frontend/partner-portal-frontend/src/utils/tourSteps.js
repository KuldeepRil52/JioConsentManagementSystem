/**
 * Tour Steps Configuration
 * 
 * Define tour steps for different pages here.
 * Each step should have:
 * - target: CSS selector for the element to highlight
 * - title: Step title
 * - description: Step description
 * - position: 'top' | 'bottom' | 'left' | 'right' | 'center'
 * - image: (optional) Image URL
 */

// Dashboard Tour
export const dashboardTourSteps = [
  {
    target: null, // Center of screen
    title: 'Welcome to Jio Consent Management System! 👋',
    description: 'Let me guide you through the key features of this platform. This tour will help you understand how to manage consents, cookies, grievances, and more.',
    position: 'center',
  },
  {
    target: '.dashboard-btn',
    title: 'Quick Actions',
    description: 'Start by configuring your system settings or creating a business group. These are the foundational steps to get started.',
    position: 'top',
  },
  {
    target: '.sideBar-outer-container',
    title: 'Navigation Menu',
    description: 'Access all features from this sidebar - System Configuration, Consent, Cookies, Governance, Grievance Redressal, Administration, and Notifications.',
    position: 'right',
  },
  {
    target: '.avatar-container',
    title: 'User Profile',
    description: 'Manage your account settings, view notifications, and access help documentation from here.',
    position: 'left',
  },
];

// Master Setup Tour
export const masterSetupTourSteps = [
  {
    target: null,
    title: 'Master Setup 🔧',
    description: 'This is where you configure the foundation of your consent management system. Let\'s explore each section.',
    position: 'center',
  },
  {
    target: '[data-tour="pii-tab"]',
    title: 'PII (Personal Identifiable Information)',
    description: 'Define and manage data types and items that contain personal information. This helps classify and protect sensitive user data.',
    position: 'bottom',
  },
  {
    target: '[data-tour="data-processor-tab"]',
    title: 'Data Processors',
    description: 'Add third-party processors who handle your data. This is crucial for DPDP and other privacy compliance.',
    position: 'bottom',
  },
  {
    target: '[data-tour="processing-activity-tab"]',
    title: 'Processing Activities',
    description: 'Define how and why data is processed. Link processors with specific data items and purposes.',
    position: 'bottom',
  },
  {
    target: '[data-tour="purpose-tab"]',
    title: 'Purposes',
    description: 'Create clear purposes for data collection - like "Marketing", "Analytics", or "Service Improvement".',
    position: 'bottom',
  },
];

// Create Consent Template Tour
export const createConsentTourSteps = [
  {
    target: null,
    title: 'Create Consent Template 📝',
    description: 'Build beautiful, compliant consent forms that your users will see. Let\'s create your first template!',
    position: 'center',
  },
  {
    target: '[data-tour="template-name"]',
    title: 'Template Name',
    description: 'Give your template a meaningful name. This helps you identify it later when managing multiple templates.',
    position: 'bottom',
  },
  {
    target: '[data-tour="purpose-tab"]',
    title: 'Purpose Configuration',
    description: 'Add purposes for data collection. Each purpose should clearly explain why you need the data and link to processing activities.',
    position: 'bottom',
  },
  {
    target: '[data-tour="language-tab"]',
    title: 'Multi-language Support',
    description: 'Make your consent form accessible in multiple languages. Customize text for each language to reach a global audience.',
    position: 'bottom',
  },
  {
    target: '[data-tour="branding-tab"]',
    title: 'Branding & Customization',
    description: 'Upload your logo, choose colors, and customize the look and feel of your consent form to match your brand.',
    position: 'bottom',
  },
  {
    target: '[data-tour="accessibility-tab"]',
    title: 'Accessibility Options',
    description: 'Configure advanced options like parental controls, dark mode, and what information to display to users.',
    position: 'bottom',
  },
  {
    target: '.right-half',
    title: 'Live Preview',
    description: 'See your changes in real-time! Toggle between desktop/mobile views and light/dark themes to ensure your consent form looks perfect.',
    position: 'left',
  },
];

// Create Grievance Template Tour
export const createGrievanceTourSteps = [
  {
    target: null,
    title: 'Create Grievance Form 📋',
    description: 'Set up a grievance management system to handle user complaints and requests effectively.',
    position: 'center',
  },
  {
    target: '[data-tour="template-name"]',
    title: 'Form Name',
    description: 'Name your grievance form. You can create different forms for different types of complaints.',
    position: 'bottom',
  },
  {
    target: '[data-tour="form-config-tab"]',
    title: 'Form Configuration',
    description: 'Define grievance categories, user types, and custom fields to collect the right information.',
    position: 'bottom',
  },
  {
    target: '[data-tour="language-tab"]',
    title: 'Language Support',
    description: 'Translate your grievance form into multiple languages for better user accessibility.',
    position: 'bottom',
  },
  {
    target: '[data-tour="branding-tab"]',
    title: 'Branding',
    description: 'Customize the appearance with your brand colors and logo.',
    position: 'bottom',
  },
  {
    target: '.right-half',
    title: 'Preview Your Form',
    description: 'Test your grievance form before publishing. Try different device views and themes.',
    position: 'left',
  },
];

// Cookie Management Tour
export const cookieTourSteps = [
  {
    target: null,
    title: 'Cookie Management 🍪',
    description: 'Scan, categorize, and manage cookies on your website to ensure compliance with privacy regulations.',
    position: 'center',
  },
  {
    target: '[data-tour="scan-website"]',
    title: 'Scan Website',
    description: 'Start by scanning your website to discover all cookies being used. We\'ll automatically categorize them for you.',
    position: 'bottom',
  },
  {
    target: '[data-tour="cookie-categories"]',
    title: 'Cookie Categories',
    description: 'Organize cookies into categories like "Essential", "Analytics", "Marketing", etc. This helps users make informed choices.',
    position: 'bottom',
  },
  {
    target: '[data-tour="cookie-templates"]',
    title: 'Cookie Templates',
    description: 'Create cookie consent banners that appear on your website. Customize the design and behavior.',
    position: 'bottom',
  },
];

// System Configuration Tour
export const systemConfigTourSteps = [
  {
    target: null,
    title: 'System Configuration ⚙️',
    description: 'Configure global settings for your consent management system.',
    position: 'center',
  },
  {
    target: '[data-tour="dpo-config"]',
    title: 'Data Protection Officer',
    description: 'Set up your DPO contact information. This is required for DPDP compliance and builds trust with users.',
    position: 'bottom',
  },
  {
    target: '[data-tour="system-config"]',
    title: 'System Settings',
    description: 'Configure default settings, notification preferences, and other system-wide options.',
    position: 'bottom',
  },
  {
    target: '[data-tour="grievance-config"]',
    title: 'Grievance Settings',
    description: 'Set up SLA timelines, escalation policies, and retention periods for grievances.',
    position: 'bottom',
  },
  {
    target: '[data-tour="consent-config"]',
    title: 'Consent Settings',
    description: 'Configure how long consents are valid, renewal policies, and notification preferences.',
    position: 'bottom',
  },
];

// Templates Page Tour
export const templatesTourSteps = [
  {
    target: null,
    title: 'Templates Overview 📄',
    description: 'Manage all your consent templates from one place. Create, edit, publish, and track template performance.',
    position: 'center',
  },
  {
    target: '[data-tour="create-template"]',
    title: 'Create New Template',
    description: 'Click here to create a new consent template. You can create multiple templates for different use cases.',
    position: 'bottom',
  },
  {
    target: '[data-tour="template-stats"]',
    title: 'Template Statistics',
    description: 'View quick stats about your templates - total created, published, drafts, and inactive templates.',
    position: 'top',
  },
  {
    target: '[data-tour="template-list"]',
    title: 'Template Management',
    description: 'View, edit, publish, or delete templates. Click on any template to see detailed analytics and consent logs.',
    position: 'top',
  },
];

// DPO Dashboard Tour - Comprehensive Workflow Guide
export const dpoDashboardTourSteps = [
  {
    target: null,
    title: 'Welcome to Jio Consent Management System! 🎉',
    description: 'This platform helps you manage user consents, cookies, and grievances in compliance with DPDP and data privacy regulations. Let me guide you through the complete setup and workflow!',
    position: 'center',
  },
  {
    target: null,
    title: 'Getting Started - The Setup Journey 🚀',
    description: 'Before you can start managing consents, you need to complete a few essential setup steps. Don\'t worry, I\'ll walk you through each one!',
    position: 'center',
  },
  {
    target: '[data-tour="sidebar-menu"]',
    title: 'Navigation Menu 🧭',
    description: 'All features are accessible through this sidebar. Let me show you the key sections you\'ll need for your setup journey.',
    position: 'right',
  },
  {
    target: '[data-tour="menu-system-configuration"]',
    title: 'Step 1: System Configuration ⚙️',
    description: 'START HERE! Click on "System Configuration" to configure your DPO details, SSL certificates, base URL, and notification settings. This is the foundation of your consent management system.',
    position: 'right',
    action: (node) => {
      if (node && node.click) {
        setTimeout(() => node.click(), 100);
      }
    },
  },
  {
    target: '[data-tour="menu-system-configuration"]',
    title: 'Step 2: Master Data Setup 🔧',
    description: 'After system config, click on "Master Data" inside this section to define your data types (PII), purposes, data processors, and processing activities. These are the building blocks for your templates.',
    position: 'right',
  },
  {
    target: '[data-tour="menu-consent"]',
    title: 'Step 3: Create Consent Templates 📝',
    description: 'Now you\'re ready! Click on "Consent" section and then go to "Templates" to build beautiful consent forms that users will see when agreeing to data collection.',
    position: 'right',
    action: (node) => {
      if (node && node.click) {
        setTimeout(() => node.click(), 100);
      }
    },
  },
  {
    target: '[data-tour="menu-cookies"]',
    title: 'Step 4: Cookie Management 🍪',
    description: 'Click on "Cookies" section to set up cookie consent. You can scan your website, categorize cookies, and create cookie consent banners for compliance.',
    position: 'right',
    action: (node) => {
      if (node && node.click) {
        setTimeout(() => node.click(), 100);
      }
    },
  },
  {
    target: '[data-tour="menu-grievance-redressal"]',
    title: 'Step 5: Grievance Setup 📋',
    description: 'Click on "Grievance Redressal" to configure your grievance handling system. This allows users to submit complaints, data deletion requests, and other privacy-related inquiries.',
    position: 'right',
    action: (node) => {
      if (node && node.click) {
        setTimeout(() => node.click(), 100);
      }
    },
  },
  {
    target: '[data-tour="stats-section"]',
    title: 'Dashboard Overview - Master Data 📊',
    description: 'Once configured, this section shows your data processing register: total purposes, data types, processing activities, and retention policies. This is your compliance foundation!',
    position: 'bottom',
  },
  {
    target: '[data-tour="consent-section"]',
    title: 'Monitor Consent Performance 📈',
    description: 'After publishing consent templates, track them here! See published templates, total consents collected, active consents, withdrawn consents, and expired consents. Monitor user consent preferences in real-time.',
    position: 'top',
  },
  {
    target: '[data-tour="cookies-section"]',
    title: 'Cookie Compliance Tracking 🍪',
    description: 'View your cookie consent statistics: published cookie banners, user responses (all accepted, partially accepted, rejected, no action). Ensure your website stays compliant with cookie laws.',
    position: 'top',
  },
  {
    target: '[data-tour="grievance-section"]',
    title: 'Grievance Management 📮',
    description: 'Track all user requests: total grievances, resolved cases, in-progress cases, escalated cases, and rejected requests. Monitor SLA compliance and response times to maintain user trust.',
    position: 'top',
  },
  {
    target: '[data-tour="notifications-section"]',
    title: 'Communication Tracking 📧',
    description: 'Monitor all notifications sent to users via email and SMS. Track delivery rates and ensure users are properly informed about consent expiry, renewals, and grievance updates.',
    position: 'top',
  },
  {
    target: '[data-tour="menu-governance"]',
    title: 'Access Detailed Reports 📊',
    description: 'Click on "Governance" to access "Audit & Compliance Report", "ROPA", and "Breach Notifications". View detailed analytics, export compliance reports, and track your privacy program\'s effectiveness.',
    position: 'right',
    action: (node) => {
      if (node && node.click) {
        setTimeout(() => node.click(), 100);
      }
    },
  },
];

// Help function to get tour steps by page
export const getTourSteps = (pageName) => {
  const tours = {
    dashboard: dashboardTourSteps,
    dpoDashboard: dpoDashboardTourSteps,
    masterSetup: masterSetupTourSteps,
    createConsent: createConsentTourSteps,
    createGrievance: createGrievanceTourSteps,
    cookie: cookieTourSteps,
    systemConfig: systemConfigTourSteps,
    templates: templatesTourSteps,
  };
  
  return tours[pageName] || [];
};

