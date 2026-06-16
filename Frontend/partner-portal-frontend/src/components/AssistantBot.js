import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Text, Icon, InputFieldV2, ActionButton } from '../custom-components';
import { IcClose, IcInfo } from '../custom-components/Icon';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { motion, AnimatePresence } from 'framer-motion';
import { canAccessRoute, hasFullAccess } from '../utils/permissions';
import '../styles/assistantBot.css';

const AssistantBot = ({ isOpen, onClose }) => {
  const [messages, setMessages] = useState([]);
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef(null);
  const textareaRef = useRef(null);
  const navigate = useNavigate();
  
  // Get user permissions from Redux
  const permissions = useSelector((state) => state.common.permissions);
  const userRole = useSelector((state) => state.common.userRole);

  // Page titles mapping from SideNav for better context
  const pageTitles = {
    '/dashboard': { title: 'Dashboard', section: 'Dashboard' },
    '/user-dashboard': { title: 'User Dashboard Theme', section: 'Dashboard' },
    '/systemConfiguration': { title: 'Configuration', section: 'System Configuration' },
    '/master': { title: 'Master Data', section: 'System Configuration' },
    '/templates': { title: 'Templates', section: 'Consent' },
    '/consent': { title: 'Configuration', section: 'Consent' },
    '/registercookies': { title: 'Templates', section: 'Cookies' },
    '/cookieslogs': { title: 'Cookies Logs', section: 'Cookies' },
    '/cookie-category': { title: 'Manage categories', section: 'Cookies' },
    '/dataProtectionOfficer': { title: 'Data protection officer', section: 'Governance' },
    '/ropa': { title: 'ROPA', section: 'Governance' },
    '/dpo-dashboard': { title: 'DPO Dashboard', section: 'Governance' },
    '/audit-compliance': { title: 'Audit & Compliance Report', section: 'Governance' },
    '/breach-notifications': { title: 'Breach Notifications', section: 'Governance' },
    '/grievance': { title: 'Configuration', section: 'Grievance Redressal' },
    '/grievanceFormTemplates': { title: 'Grievance Form', section: 'Grievance Redressal' },
    '/request': { title: 'Requests', section: 'Grievance Redressal' },
    '/business': { title: 'Business groups', section: 'Administration' },
    '/roles': { title: 'Roles', section: 'Administration' },
    '/users': { title: 'Users', section: 'Administration' },
    '/organization-map': { title: 'Organization Map', section: 'Administration' },
    '/notification': { title: 'Configuration', section: 'Notifications' },
    '/emailTemplate': { title: 'Consent Email template', section: 'Notifications' },
    '/smsTemplate': { title: 'Consent SMS template', section: 'Notifications' },
    '/grievanceEmailTemplate': { title: 'Grievance Email template', section: 'Notifications' },
    '/grievanceSmsTemplate': { title: 'Grievance SMS template', section: 'Notifications' },
  };

  // Knowledge base about the system
  const knowledgeBase = {
    // Greetings
    greeting: {
      keywords: ['hi', 'hello', 'hey', 'good morning', 'good afternoon', 'good evening', 'greetings', 'hola', 'namaste', 'hi there'],
      response: "Hello! 👋 I'm your CMS Assistant.\n\nI can help you with:\n✅ Consent Management (templates, configuration)\n🍪 Cookies Management (templates, logs, categories)\n🏢 Organization Map (business groups, users, roles)\n📝 Grievance Redressal (forms, requests)\n🛡️ Governance (DPO, ROPA, Audit, Breach notifications)\n📊 Dashboard & Reports\n⚙️ System Configuration\n📧 Notifications (Email/SMS templates)\n\nWhat would you like to know about?",
      action: null,
      path: null
    },
    
    // Dashboard
    dashboard: {
      keywords: ['dashboard', 'overview', 'main', 'home', 'statistics', 'metrics', 'main dashboard'],
      response: "📊 **Dashboard:**\n\nView system overview and key metrics:\n• Activity summary\n• Recent consents\n• Grievance statistics\n• User analytics\n• Quick access to features\n\nShall I take you to the Dashboard?",
      action: () => navigate('/dashboard'),
      path: '/dashboard'
    },
    
    userDashboard: {
      keywords: ['user dashboard', 'dashboard theme', 'user theme', 'customizable dashboard'],
      response: "🎨 **User Dashboard Theme:**\n\nCustomize your dashboard experience:\n• Personalized layout\n• Theme preferences\n• Widget configuration\n• Custom views\n\nWould you like to go to the User Dashboard Theme page?",
      action: () => navigate('/user-dashboard'),
      path: '/user-dashboard'
    },
    
    dpoDashboard: {
      keywords: ['dpo dashboard', 'data protection officer dashboard', 'dpo metrics'],
      response: "📊 **Governance > DPO Dashboard:**\n\nAccess Data Protection Officer metrics:\n• Compliance overview\n• DPDP compliance status\n• Data processing activities\n• Risk assessments\n• Audit reports\n\nShall I take you to the DPO Dashboard?",
      action: () => navigate('/dpo-dashboard'),
      path: '/dpo-dashboard'
    },
    
    // Consent Management

    
    consentTemplate: {
      keywords: ['consent template', 'create consent template', 'consent form template'],
      response: "📝 **Creating Consent Templates:**\n\n1. Navigate to **Consent > Templates**\n2. Click **Create Template**\n3. Configure template fields\n4. Set purposes and branding\n5. Save and activate\n\nShall I take you to the Templates page?",
      action: () => navigate('/templates'),
      path: '/templates'
    },
    
    createConsent: {
      keywords: ['create consent template', 'new consent template', 'add consent template', 'how to create consent template'],
      response: "📋 **To Create a Consent:**\n\n1. Go to **Consent > Templates**\n2. Click **Create Consent** or use existing template\n3. Fill in required details\n4. Configure purposes and data collection\n5. Set up branding and notifications\n6. Save and publish\n\nWould you like me to navigate you to the Templates page?",
      action: () => navigate('/createConsent'),
      path: '/createConsent'
    },
    
    // Cookies
    cookies: {
      keywords: ['cookie', 'cookies', 'cookie management', 'cookie template'],
      response: "🍪 **Cookies Section:**\n\n• **Cookies > Templates** - Register and manage cookie templates\n• **Cookies > Cookies Logs** - View cookie consent logs\n• **Cookies > Manage categories** - Organize cookies by categories\n\nCookies help you comply with privacy regulations. Where would you like to go?",
      action: () => navigate('/registercookies'),
      path: '/registercookies'
    },
    
    cookieLogs: {
      keywords: ['cookie log', 'cookie logs', 'cookies logs', 'cookie consent log'],
      response: "📊 **Cookies > Cookies Logs:**\n\nView and track cookie consent logs:\n• Cookie consent history\n• User cookie preferences\n• Consent timestamps\n• Cookie usage analytics\n\nShall I take you to the Cookies Logs page?",
      action: () => navigate('/cookieslogs'),
      path: '/cookieslogs'
    },
    
    cookieCategories: {
      keywords: ['cookie categor', 'manage categor', 'cookie classification'],
      response: "🗂️ **Cookies > Manage categories:**\n\nOrganize and manage cookie categories:\n• Essential cookies\n• Functional cookies\n• Analytics cookies\n• Marketing cookies\n• Define custom categories\n\nWould you like to go to Manage categories?",
      action: () => navigate('/cookie-category'),
      path: '/cookie-category'
    },
    
    // Grievance
    
    grievanceForm: {
      keywords: ['create grievance', 'grievance form template', 'new grievance'],
      response: "📄 **Creating Grievance Forms:**\n\n1. Navigate to **Grievance Redressal > Grievance Form**\n2. Click **Create Form Template**\n3. Design form fields\n4. Configure workflow\n5. Set notifications\n6. Publish form\n\nWould you like to navigate to the Grievance Form page?",
      action: () => navigate('/grievanceFormTemplates'),
      path: '/grievanceFormTemplates'
    },
    
    grievanceRequests: {
      keywords: ['grievance request', 'view request', 'request management', 'process request'],
      response: "📥 **Grievance Redressal > Requests:**\n\nManage grievance requests:\n• View all grievance submissions\n• Process pending requests\n• Track request status\n• Respond to grievances\n• Generate reports\n\nShall I take you to the Requests page?",
      action: () => navigate('/request'),
      path: '/request'
    },
    
    // Organization & Administration
    organization: {
      keywords: ['organization', 'org map', 'organization map', 'business structure', 'hierarchy'],
      response: "🏢 **Administration > Organization Map:**\n\nVisualize your organization structure:\n• Legal entities\n• Business groups\n• Data processors\n• Users and their roles\n• Tree, hierarchy, and grid views\n\nShall I take you to the Organization Map page?",
      action: () => navigate('/organization-map'),
      path: '/organization-map'
    },
    
    businessGroups: {
      keywords: ['business', 'business group', 'business groups', 'manage business'],
      response: "🏢 **Administration > Business groups:**\n\nManage your business entities:\n• Create business groups\n• Assign users and roles\n• Configure business settings\n• View business hierarchy\n\nWould you like to go to the Business groups page?",
      action: () => navigate('/business'),
      path: '/business'
    },
    
    users: {
      keywords: ['user', 'users', 'manage users', 'add user', 'user management'],
      response: "👥 **Administration > Users:**\n\n• View all users\n• Add new users\n• Assign roles and permissions\n• Update user information\n• Manage user access\n\nShall I navigate you to the Users page?",
      action: () => navigate('/users'),
      path: '/users'
    },
    
    roles: {
      keywords: ['role', 'roles', 'permissions', 'user role', 'manage roles'],
      response: "🔐 **Administration > Roles:**\n\n• Create custom roles\n• Assign permissions\n• Manage role hierarchy\n• Control feature access\n\nRoles determine what users can see and do. Want to go to the Roles page?",
      action: () => navigate('/roles'),
      path: '/roles'
    },
    
    // Governance
    governance: {
      keywords: ['governance', 'compliance governance'],
      response: "🛡️ **Governance Section:**\n\n• **Governance > Data protection officer** - DPO information\n• **Governance > ROPA** - Record of Processing Activities\n• **Governance > DPO Dashboard** - Compliance metrics\n• **Governance > Audit & Compliance Report** - Audit reports\n• **Governance > Breach Notifications** - Security breach management\n\nWhich area would you like to explore?",
      action: null,
      path: null
    },
    
    dpo: {
      keywords: ['dpo', 'data protection officer', 'data protection'],
      response: "👔 **Governance > Data protection officer:**\n\nManage DPO information:\n• Configure DPO details\n• Contact information\n• Responsibilities\n• Compliance oversight\n\nShall I take you to the Data protection officer page?",
      action: () => navigate('/dataProtectionOfficer'),
      path: '/dataProtectionOfficer'
    },
    
    ropa: {
      keywords: ['ropa', 'record of processing', 'processing activities'],
      response: "📋 **Governance > ROPA:**\n\nMaintain compliance records:\n• Document processing activities\n• Track data flows\n• Legal basis for processing\n• Data retention policies\n\nWould you like to go to the ROPA page?",
      action: () => navigate('/ropa'),
      path: '/ropa'
    },
    
    breach: {
      keywords: ['breach', 'breach notification', 'security breach', 'data breach'],
      response: "🚨 **Governance > Breach Notifications:**\n\nManage security incidents:\n• Report data breaches\n• Track breach status\n• Notify authorities\n• Document remediation\n\nShall I navigate you to the Breach Notifications page?",
      action: () => navigate('/breach-notifications'),
      path: '/breach-notifications'
    },
    
    audit: {
      keywords: ['audit', 'compliance', 'audit report', 'compliance report', 'audit & compliance'],
      response: "📋 **Governance > Audit & Compliance Report:**\n\nAccess audit and compliance information:\n• Compliance assessments\n• Audit trails\n• Regulatory compliance reports\n• Risk assessments\n• Generate compliance reports\n\nWould you like to go to the Audit & Compliance Report page?",
      action: () => navigate('/audit-compliance'),
      path: '/audit-compliance'
    },
    
    // System Configuration
    systemConfig: {
      keywords: ['system', 'configuration', 'system configuration', 'settings', 'setup'],
      response: "⚙️ **System Configuration Section:**\n\n• **System Configuration > Configuration** - System settings and integration\n• **System Configuration > Master Data** - Manage master data (purposes, PII, processors)\n\nConfigure core system functionality. Where would you like to go?",
      action: () => navigate('/systemConfiguration'),
      path: '/systemConfiguration'
    },
    
    masterData: {
      keywords: ['master', 'master data', 'purpose', 'pii', 'processor'],
      response: "📊 **System Configuration > Master Data:**\n\nManage foundational data:\n• **Purposes** - Data processing purposes\n• **PII Categories** - Personal information types\n• **Data Processors** - Third-party processors\n• **Processing Activities** - Activities catalog\n\nShall I take you to the Master Data page?",
      action: () => navigate('/master'),
      path: '/master'
    },
    
    // Notifications
    notifications: {
      keywords: ['notification', 'notifications', 'notification config'],
      response: "📧 **Notifications Section:**\n\n**Consent Notifications:**\n• **Notifications > Consent Email template**\n• **Notifications > Consent SMS template**\n\n**Grievance Notifications:**\n• **Notifications > Grievance Email template**\n• **Notifications > Grievance SMS template**\n\nCustomize notification messages. Which templates would you like to manage?",
      action: () => navigate('/notification'),
      path: '/notification'
    },
    
    consentEmailTemplate: {
      keywords: ['consent email', 'email template consent', 'consent email template'],
      response: "📧 **Notifications > Consent Email template:**\n\nManage consent email templates:\n• Create email templates\n• Customize email content\n• Set dynamic variables\n• Preview email templates\n• Manage consent notifications\n\nShall I take you to the Consent Email template page?",
      action: () => navigate('/emailTemplate'),
      path: '/emailTemplate'
    },
    
    consentSmsTemplate: {
      keywords: ['consent sms', 'sms template consent', 'consent sms template'],
      response: "📱 **Notifications > Consent SMS template:**\n\nManage consent SMS templates:\n• Create SMS templates\n• Customize SMS messages\n• Set dynamic variables\n• Preview SMS templates\n• Manage consent SMS notifications\n\nWould you like to go to the Consent SMS template page?",
      action: () => navigate('/smsTemplate'),
      path: '/smsTemplate'
    },
    
    grievanceEmailTemplate: {
      keywords: ['grievance email', 'email template grievance', 'grievance email template'],
      response: "📧 **Notifications > Grievance Email template:**\n\nManage grievance email templates:\n• Create email templates for grievances\n• Customize email content\n• Set dynamic variables\n• Preview email templates\n• Manage grievance notifications\n\nShall I take you to the Grievance Email template page?",
      action: () => navigate('/grievanceEmailTemplate'),
      path: '/grievanceEmailTemplate'
    },
    
    grievanceSmsTemplate: {
      keywords: ['grievance sms', 'sms template grievance', 'grievance sms template'],
      response: "📱 **Notifications > Grievance SMS template:**\n\nManage grievance SMS templates:\n• Create SMS templates for grievances\n• Customize SMS messages\n• Set dynamic variables\n• Preview SMS templates\n• Manage grievance SMS notifications\n\nWould you like to go to the Grievance SMS template page?",
      action: () => navigate('/grievanceSmsTemplate'),
      path: '/grievanceSmsTemplate'
    },
    
    // General queries
    profile: {
      keywords: ['profile', 'account', 'my profile', 'user profile', 'my account'],
      response: "👤 **Your Profile:**\n\n• View profile information\n• Update personal details\n• Manage account settings\n• Change preferences\n\nShall I take you to your Profile page?",
      action: () => navigate('/profile'),
      path: '/profile'
    },
    
    search: {
      keywords: ['search', 'find', 'look for', 'global search'],
      response: "🔍 **Global Search:**\n\nQuickly find anything in the system:\n• Search users, consents, grievances\n• Advanced filters\n• Quick navigation\n\nClick the **search icon (🔍)** in the header to use Global Search!",
      action: null,
      path: null
    },
    
    // How-to questions
    howTo: {
      keywords: ['how to', 'how do i', 'how can i', 'steps to', 'guide to', 'tutorial', 'show me'],
      response: "📚 **Quick Navigation Guide:**\n\n**Consent Management:**\n• **Consent > Templates** - Create consent templates\n• **Consent > Configuration** - Configure consent settings\n\n**User Management:**\n• **Administration > Users** - Manage users\n• **Administration > Roles** - Manage roles\n• **Administration > Organization Map** - View organization structure\n\n**Grievance Handling:**\n• **Grievance Redressal > Configuration** - Configure settings\n• **Grievance Redressal > Grievance Form** - Manage forms\n\n**Cookie Management:**\n• **Cookies > Templates** - Manage cookie templates\n\nWhat specific task would you like help with?",
      action: null,
      path: null
    },
    
    // General Help
    help: {
      keywords: ['help', 'assist', 'support', 'getting started', 'what can you do', 'need help'],
      response: "💡 **I'm here to help!**\n\nI can guide you through:\n✅ **Consent** - Templates, Configuration\n🍪 **Cookies** - Templates, Logs, Categories\n🏢 **Administration** - Users, Roles, Business groups, Organization Map\n📝 **Grievance Redressal** - Configuration, Forms, Requests\n🛡️ **Governance** - Data protection officer, ROPA, DPO Dashboard, Audit & Compliance, Breach Notifications\n⚙️ **System Configuration** - Configuration, Master Data\n📧 **Notifications** - Email/SMS templates\n📊 **Dashboard** - Main Dashboard, User Dashboard Theme, DPO Dashboard\n\nTry asking:\n• \"Navigate to Consent Templates\"\n• \"Show me the Organization Map\"\n• \"Take me to ROPA\"\n• \"How do I manage users?\"\n\nWhat would you like to know?",
      action: null,
      path: null
    },
    
    // Thank you
    thanks: {
      keywords: ['thank', 'thanks', 'thank you', 'thx', 'appreciate', 'awesome', 'great'],
      response: "You're very welcome! 😊 I'm always here to help.\n\nFeel free to ask me anything about the Consent Management System anytime!",
      action: null,
      path: null
    },
    
    // Features
    features: {
      keywords: ['features', 'what can i do', 'capabilities', 'functions', 'what does this do'],
      response: "🎯 **System Features & Pages:**\n\n**Dashboard:**\n📊 Dashboard, User Dashboard Theme, DPO Dashboard\n\n**System Configuration:**\n⚙️ Configuration, Master Data\n\n**Consent:**\n✅ Templates, Configuration\n\n**Cookies:**\n🍪 Templates, Cookies Logs, Manage categories\n\n**Governance:**\n🛡️ Data protection officer, ROPA, DPO Dashboard, Audit & Compliance Report, Breach Notifications\n\n**Grievance Redressal:**\n📝 Configuration, Grievance Form, Requests\n\n**Administration:**\n🏢 Business groups, Roles, Users, Organization Map\n\n**Notifications:**\n📧 Configuration, Email/SMS templates\n\nWhat would you like to explore?",
      action: null,
      path: null
    },
  };

  // Filter knowledge base based on user permissions
  const filteredKnowledgeBase = useMemo(() => {
    const filtered = {};
    
    for (const [key, data] of Object.entries(knowledgeBase)) {
      // If no path specified (general help/greeting/etc), always include
      if (!data.path) {
        filtered[key] = data;
        continue;
      }
      
      // Check if user has access to this route
      if (canAccessRoute(permissions, data.path, userRole)) {
        filtered[key] = data;
      }
    }
    
    return filtered;
  }, [permissions, userRole, knowledgeBase]);
  
  // Quick action suggestions - filtered by permissions
  const quickActions = useMemo(() => {
    const actions = [];
    
    // Organization Map - Administration
    if (canAccessRoute(permissions, '/organization-map', userRole)) {
      actions.push({ label: "Organization Map", icon: "🏢", query: "show me organization map", path: "/organization-map" });
    }
    
    // Create Consent Template - Consent
    if (canAccessRoute(permissions, '/templates', userRole)) {
      actions.push({ label: "Create Consent Template", icon: "📋", query: "how to create consent template", path: "/templates" });
    }
    
    // Manage Grievances - Grievance Redressal
    if (canAccessRoute(permissions, '/request', userRole)) {
      actions.push({ label: "Manage Grievances", icon: "📝", query: "tell me about grievance management", path: "/request" });
    }
    
    // ROPA - Governance
    if (canAccessRoute(permissions, '/ropa', userRole)) {
      actions.push({ label: "ROPA", icon: "🛡️", query: "show me ropa", path: "/ropa" });
    }
    
    // Master Data - System Configuration
    if (canAccessRoute(permissions, '/master', userRole)) {
      actions.push({ label: "Master Data", icon: "⚙️", query: "show me master data", path: "/master" });
    }
    
    // Cookies Management
    if (canAccessRoute(permissions, '/registercookies', userRole)) {
      actions.push({ label: "Cookie Templates", icon: "🍪", query: "show me cookies", path: "/registercookies" });
    }
    
    // Always show system features (general help)
    actions.push({ label: "System Features", icon: "✨", query: "what are the system features" });
    
    // Take first 4 actions
    return actions.slice(0, 4);
  }, [permissions, userRole]);

  // Helper function to remove markdown formatting
  const removeMarkdown = (text) => {
    if (!text) return text;
    // Remove ** for bold, * for italic, and other markdown formatting
    return text
      .replace(/\*\*/g, '') // Remove ** (bold)
      .replace(/\*/g, '')    // Remove * (italic)
      .replace(/__/g, '')    // Remove __ (bold)
      .replace(/_/g, '');    // Remove _ (italic)
  };

  // Initialize with welcome message - personalized based on role
  useEffect(() => {
    if (isOpen && messages.length === 0) {
      // Build personalized welcome based on accessible features
      const features = [];
      
      if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('consent') || kb.path?.includes('templates'))) {
        features.push('• Consent Management');
      }
      if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('cookie'))) {
        features.push('• Cookie Management');
      }
      if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('organization') || kb.path?.includes('business') || kb.path?.includes('users') || kb.path?.includes('roles'))) {
        features.push('• Organization & Administration');
      }
      if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('grievance') || kb.path?.includes('request'))) {
        features.push('• Grievance Redressal');
      }
      if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('ropa') || kb.path?.includes('dpo') || kb.path?.includes('audit') || kb.path?.includes('breach'))) {
        features.push('• Governance & Compliance');
      }
      if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('system') || kb.path?.includes('master'))) {
        features.push('• System Configuration');
      }
      
      const featuresText = features.length > 0 ? features.join('\n') : '• Dashboard\n• Profile Settings';
      
      const welcomeMessage = `👋 Hi! I'm your CMS Assistant.\n\nI can help you with:\n${featuresText}\n• System Navigation\n\nTry the quick actions below or ask me anything!`;
      
      setMessages([{
        type: 'bot',
        content: removeMarkdown(welcomeMessage),
        timestamp: new Date()
      }]);
    }
  }, [isOpen, filteredKnowledgeBase]);

  // Scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  // Find matching response from filtered knowledge base
  const findResponse = (query) => {
    const lowerQuery = query.toLowerCase();
    
    // Special handling for "features" query - build dynamic response
    if (['features', 'what can i do', 'capabilities', 'functions', 'what does this do'].some(keyword => lowerQuery.includes(keyword))) {
      const sections = {};
      
      // Group accessible pages by section
      Object.values(filteredKnowledgeBase).forEach(kb => {
        if (kb.path && pageTitles[kb.path]) {
          const section = pageTitles[kb.path].section;
          if (!sections[section]) {
            sections[section] = [];
          }
          if (!sections[section].includes(pageTitles[kb.path].title)) {
            sections[section].push(pageTitles[kb.path].title);
          }
        }
      });
      
      // Build features response
      let featuresResponse = "🎯 **Your Accessible Features:**\n\n";
      
      const sectionIcons = {
        'Dashboard': '📊',
        'System Configuration': '⚙️',
        'Consent': '✅',
        'Cookies': '🍪',
        'Governance': '🛡️',
        'Grievance Redressal': '📝',
        'Administration': '🏢',
        'Notifications': '📧'
      };
      
      Object.entries(sections).sort().forEach(([section, pages]) => {
        const icon = sectionIcons[section] || '•';
        featuresResponse += `**${icon} ${section}:**\n${pages.join(', ')}\n\n`;
      });
      
      featuresResponse += "What would you like to explore?";
      
      return {
        response: removeMarkdown(featuresResponse),
        action: null,
        path: null
      };
    }
    
    // Check each filtered knowledge base entry (only what user can access)
    for (const [key, data] of Object.entries(filteredKnowledgeBase)) {
      if (data.keywords.some(keyword => lowerQuery.includes(keyword))) {
        // Special handling for "greeting" query
        if (key === 'greeting') {
          const accessibleFeatures = [];
          
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('consent') || kb.path?.includes('templates'))) {
            accessibleFeatures.push('✅ Consent Management (templates, configuration)');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('cookie'))) {
            accessibleFeatures.push('🍪 Cookies Management (templates, logs, categories)');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('organization') || kb.path?.includes('business') || kb.path?.includes('users') || kb.path?.includes('roles'))) {
            accessibleFeatures.push('🏢 Organization Map (business groups, users, roles)');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('grievance') || kb.path?.includes('request'))) {
            accessibleFeatures.push('📝 Grievance Redressal (forms, requests)');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('ropa') || kb.path?.includes('dpo') || kb.path?.includes('audit') || kb.path?.includes('breach'))) {
            accessibleFeatures.push('🛡️ Governance (DPO, ROPA, Audit, Breach notifications)');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('system') || kb.path?.includes('master'))) {
            accessibleFeatures.push('⚙️ System Configuration');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('Template') || kb.path?.includes('notification'))) {
            accessibleFeatures.push('📧 Notifications (Email/SMS templates)');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('dashboard'))) {
            accessibleFeatures.push('📊 Dashboard & Reports');
          }
          
          const greetingResponse = `Hello! 👋 I'm your CMS Assistant.\n\nBased on your role, I can help you with:\n${accessibleFeatures.join('\n')}\n\nWhat would you like to know about?`;
          
          return {
            response: removeMarkdown(greetingResponse),
            action: null,
            path: null
          };
        }
        
        // Special handling for "help" query
        if (key === 'help') {
          const accessibleFeatures = [];
          
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('consent') || kb.path?.includes('templates'))) {
            accessibleFeatures.push('✅ **Consent** - Templates, Configuration');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('cookie'))) {
            accessibleFeatures.push('🍪 **Cookies** - Templates, Logs, Categories');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('organization') || kb.path?.includes('business') || kb.path?.includes('users') || kb.path?.includes('roles'))) {
            accessibleFeatures.push('🏢 **Administration** - Business groups, Users, Roles, Organization Map');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('grievance') || kb.path?.includes('request'))) {
            accessibleFeatures.push('📝 **Grievance Redressal** - Configuration, Forms, Requests');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('ropa') || kb.path?.includes('dpo') || kb.path?.includes('audit') || kb.path?.includes('breach'))) {
            accessibleFeatures.push('🛡️ **Governance** - Data Protection Officer, ROPA, DPO Dashboard, Audit & Compliance, Breach Notifications');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('system') || kb.path?.includes('master'))) {
            accessibleFeatures.push('⚙️ **System Configuration** - Configuration, Master Data');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('Template') || kb.path?.includes('notification'))) {
            accessibleFeatures.push('📧 **Notifications** - Email/SMS templates');
          }
          if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('dashboard'))) {
            accessibleFeatures.push('📊 **Dashboard** - Main Dashboard, User Dashboard, DPO Dashboard');
          }
          
          const helpResponse = `💡 **I'm here to help!**\n\nI can guide you through:\n${accessibleFeatures.join('\n')}\n\nTry asking:\n• \"What are my accessible features?\"\n• \"Navigate to Dashboard\"\n• \"Show me my profile\"\n\nWhat would you like to know?`;
          
          return {
            response: removeMarkdown(helpResponse),
            action: null,
            path: null
          };
        }
        
        return {
          ...data,
          response: removeMarkdown(data.response)
        };
      }
    }

    // Check if user asked about a feature they don't have access to
    let restrictedFeatureAsked = false;
    for (const [key, data] of Object.entries(knowledgeBase)) {
      if (data.path && data.keywords.some(keyword => lowerQuery.includes(keyword))) {
        // Found a match in full knowledge base but not in filtered (user doesn't have access)
        if (!filteredKnowledgeBase[key]) {
          restrictedFeatureAsked = true;
          return {
            response: removeMarkdown(`🔒 It looks like you're asking about a feature that's not accessible with your current role.\n\n**Your role may not have permission to:**\n• ${data.response.split('\n')[0].replace(/[*#]/g, '')}\n\n**What you can access:**\nTry asking "What are my accessible features?" to see what's available to you, or contact your administrator for additional permissions.`),
            action: null,
            path: null
          };
        }
      }
    }
    
    // Build dynamic default response based on what user can access
    const accessibleSections = [];
    
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.startsWith('/dashboard') || kb.path?.startsWith('/user-dashboard') || kb.path?.startsWith('/dpo-dashboard'))) {
      accessibleSections.push('📊 **Dashboard** - Main Dashboard, User Dashboard, DPO Dashboard');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.startsWith('/systemConfiguration') || kb.path?.startsWith('/master'))) {
      accessibleSections.push('⚙️ **System Configuration** - Configuration, Master Data');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.startsWith('/templates') || kb.path?.startsWith('/consent'))) {
      accessibleSections.push('✅ **Consent** - Templates, Configuration');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('cookie'))) {
      accessibleSections.push('🍪 **Cookies** - Templates, Logs, Categories');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.startsWith('/dataProtectionOfficer') || kb.path?.startsWith('/ropa') || kb.path?.startsWith('/dpo-dashboard') || kb.path?.startsWith('/audit') || kb.path?.startsWith('/breach'))) {
      accessibleSections.push('🛡️ **Governance** - Data Protection Officer, ROPA, DPO Dashboard, Audit & Compliance, Breach Notifications');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.startsWith('/grievance') || kb.path?.startsWith('/request'))) {
      accessibleSections.push('📝 **Grievance Redressal** - Configuration, Forms, Requests');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.startsWith('/business') || kb.path?.startsWith('/roles') || kb.path?.startsWith('/users') || kb.path?.startsWith('/organization'))) {
      accessibleSections.push('🏢 **Administration** - Business groups, Users, Roles, Organization Map');
    }
    if (Object.values(filteredKnowledgeBase).some(kb => kb.path?.includes('Template'))) {
      accessibleSections.push('📧 **Notifications** - Email/SMS templates');
    }
    
    const sectionsText = accessibleSections.length > 0 
      ? accessibleSections.join('\n')
      : '• Profile settings\n• Dashboard';

    // Default response if no match found
    return {
      response: removeMarkdown(`I'm not sure about that specific question, but I'd love to help! 🤔\n\n**Your accessible features:**\n\n${sectionsText}\n\n**Try asking:**\n• \"What are my available features?\"\n• \"Navigate to Dashboard\"\n• \"Show me my profile\"\n\nWhat would you like to know?`),
      action: null,
      path: null
    };
  };

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 120)}px`;
    }
  }, [inputValue]);

  // Handle sending message
  const handleSend = () => {
    if (!inputValue.trim()) return;

    // Add user message
    const userMessage = {
      type: 'user',
      content: inputValue,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputValue('');
    setIsTyping(true);
    
    // Reset textarea height
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }

    // Simulate bot thinking and respond
    setTimeout(() => {
      const matchedResponse = findResponse(inputValue);
      
      // Get page title and section for better context
      let pageInfo = null;
      if (matchedResponse.path && pageTitles[matchedResponse.path]) {
        pageInfo = pageTitles[matchedResponse.path];
      }
      
      const botMessage = {
        type: 'bot',
        content: matchedResponse.response,
        timestamp: new Date(),
        action: matchedResponse.action,
        pageInfo: pageInfo
      };

      setMessages(prev => [...prev, botMessage]);
      setIsTyping(false);
    }, 800);
  };

  // Handle quick action click
  const handleQuickAction = (query) => {
    setInputValue(query);
    setTimeout(() => handleSend(), 100);
  };

  // Handle action button click
  const handleActionClick = (action) => {
    if (action) {
      action();
      onClose();
    }
  };

  // Handle key press
  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
    // Shift+Enter will naturally create a new line in textarea
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <motion.div
        className="assistant-bot-overlay"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        onClick={onClose}
      >
        <motion.div
          className="assistant-bot-container"
          initial={{ x: 400, opacity: 0 }}
          animate={{ x: 0, opacity: 1 }}
          exit={{ x: 400, opacity: 0 }}
          transition={{ type: 'spring', damping: 25, stiffness: 300 }}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Header */}
          <div className="assistant-header">
            <div className="assistant-header-left">
              <div className="bot-avatar">
                <span className="bot-emoji">🤖</span>
              </div>
              <div>
                <Text appearance="body-m-bold" color="primary-grey-100">
                  JCMS-Bot
                </Text>
                <br></br>
                <Text appearance="body-xs" color="primary-grey-60">
                  Always here to help
                </Text>
              </div>
            </div>
            <button className="close-bot-btn" onClick={onClose}>
              <Icon ic={<IcClose />} size="md" color="primary-grey-80" />
            </button>
          </div>

          {/* Messages Area */}
          <div className="assistant-messages">
            {messages.length === 1 && (
              <div className="quick-actions-container">
                <Text appearance="body-xs" color="primary-grey-60">
                  Quick actions:
                </Text>
                <div className="quick-actions">
                  {quickActions.map((action, idx) => (
                    <button
                      key={idx}
                      className="quick-action-btn"
                      onClick={() => handleQuickAction(action.query)}
                    >
                      <span className="quick-action-icon">{action.icon}</span>
                      <Text appearance="body-xs" color="primary-grey-100">
                        {action.label}
                      </Text>
                    </button>
                  ))}
                </div>
              </div>
            )}

            {messages.map((message, idx) => (
              <motion.div
                key={idx}
                className={`message ${message.type}`}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3 }}
              >
                {message.type === 'bot' && (
                  <div className="message-avatar bot-msg-avatar">
                    <span className="bot-msg-emoji">🤖</span>
                  </div>
                )}
                <div className="message-content">
                  <Text
                    appearance="body-s"
                    color={message.type === 'user' ? 'white' : 'primary-grey-100'}
                  >
                    {message.content}
                  </Text>
                  {message.action && (
                    <button
                      className="action-navigate-btn"
                      onClick={() => handleActionClick(message.action)}
                    >
                      <Text appearance="body-xs-bold" color="primary-blue-100">
                        {message.pageInfo 
                          ? `Go to ${message.pageInfo.section} > ${message.pageInfo.title} →`
                          : 'Take me there →'
                        }
                      </Text>
                    </button>
                  )}
                </div>
                {/* {message.type === 'user' && (
                  <div className="message-avatar user-msg-avatar">
                    <Text appearance="body-xs-bold" color="white">

                    </Text>
                  </div>
                )} */}
              </motion.div>
            ))}

            {isTyping && (
              <motion.div
                className="message bot typing-indicator"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
              >
                <div className="message-avatar bot-msg-avatar">
                  <span className="bot-msg-emoji">🤖</span>
                </div>
                <div className="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </motion.div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="assistant-input-container">
            <div className="assistant-input-wrapper">
              <textarea
                ref={textareaRef}
                className="assistant-input"
                placeholder="Ask me anything about the system..."
                value={inputValue}
                onChange={(e) => setInputValue(e.target.value)}
                onKeyDown={handleKeyPress}
                rows={1}
                style={{ resize: 'none', overflow: 'hidden', minHeight: '24px', maxHeight: '120px' }}
              />
              <button
                className={`send-btn ${inputValue.trim() ? 'active' : ''}`}
                onClick={handleSend}
                disabled={!inputValue.trim()}
              >
                <span className="send-icon">{inputValue.trim() ? '➤' : '➤'}</span>
              </button>
            </div>
            <Text appearance="body-xxs" color="primary-grey-40" style={{ textAlign: 'center', marginTop: '8px' }}>
              Press Enter to send • Shift+Enter for new line
            </Text>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  );
};

export default AssistantBot;

