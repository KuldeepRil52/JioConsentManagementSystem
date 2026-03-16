// Comprehensive mock data for sandbox mode
export const mockData = {
  // Login & Auth
  loginInitOtp: {
    status: 200,
    data: {
      txnId: "sandbox-txn-123",
      message: "OTP sent successfully"
    }
  },

  loginValidateOtp: {
    status: 200,
    data: {
      tenantId: "sandbox-tenant-id",
      userId: "sandbox-user-id",
      xsessionToken: "sandbox-session-token-12345"
    }
  },

  userProfile: {
    status: 200,
    data: {
      userId: "sandbox-user-id",
      username: "Demo User",
      email: "demo@sandboxcms.com",
      designation: "Administrator",
      roles: ["ADMIN", "DPO"],
      tenantId: "sandbox-tenant-id",
      businessId: "sandbox-business-id"
    }
  },

  // Business Applications
  businessApplications: {
    status: 200,
    data: {
      searchList: [
        {
          businessId: "sandbox-business-id",
          name: "Sandbox Demo Corp",
          description: "Demo business for sandbox environment",
          createdAt: "2024-01-15T10:00:00Z"
        },
        {
          businessId: "sandbox-business-id-2",
          name: "Sandbox Tech Industries",
          description: "Technology business unit",
          createdAt: "2024-02-20T10:00:00Z"
        }
      ]
    }
  },

  // DPO Configuration
  dpoDetails: {
    status: 200,
    data: {
      configId: "dpo-config-123",
      configurationJson: {
        name: "John Doe",
        email: "dpo@sandboxcms.com",
        mobile: "+91 9876543210",
        address: "123 Privacy Street, Data City, DC 100001"
      }
    }
  },

  // System Configuration
  systemConfig: {
    status: 200,
    data: {
      searchList: [
        {
          configId: "sandbox-config-id-123",
          businessId: "sandbox-business-id",
          scopeLevel: "TENANT",
          configurationJson: {
            sslCertificate: "data:application/x-x509-ca-cert;base64,LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSURYVENDQWtXZ0F3SUJBZ0lKQUtaLi4uKERlbW8gQ2VydGlmaWNhdGUgRGF0YSkKLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLQo=",
            sslCertificateMeta: {
              documentId: "sandbox-cert-doc-id",
              name: "sandbox-cert.pem",
              contentType: "application/x-x509-ca-cert",
              size: 7418,
              tag: {}
            },
            logo: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
            logoMeta: {
              documentId: "sandbox-logo-doc-id",
              name: "sandbox-logo.png",
              contentType: "image/png",
              size: 27655,
              tag: {}
            },
            baseUrl: "https://sandbox.demo.com/api",
            defaultConsentExpiryDays: 365,
            jwtTokenTTLMinutes: 60,
            signedArtifactExpiryDays: 30,
            dataRetention: {
              value: 5,
              unit: "YEARS"
            },
            clientId: "SANDBOX123",
            clientSecret: "SECRET999"
          },
          createdAt: "2024-01-01T00:00:00.000",
          updatedAt: "2024-11-17T11:31:54.451"
        }
      ]
    }
  },

  // Purposes
  purposes: {
    status: 200,
    data: {
      searchList: [
        {
          purposeId: "purpose-1",
          purposeCode: "MARKETING",
          purposeName: "Marketing Communications",
          purposeDescription: "To send promotional and marketing materials"
        },
        {
          purposeId: "purpose-2",
          purposeCode: "ANALYTICS",
          purposeName: "Analytics & Insights",
          purposeDescription: "To analyze user behavior and improve services"
        },
        {
          purposeId: "purpose-3",
          purposeCode: "PERSONALIZATION",
          purposeName: "Personalization",
          purposeDescription: "To personalize user experience"
        },
        {
          purposeId: "purpose-4",
          purposeCode: "CUSTOMER_SUPPORT",
          purposeName: "Customer Support",
          purposeDescription: "To provide customer support and assistance"
        }
      ]
    }
  },

  // Data Types (PII)
  dataTypes: {
    status: 200,
    data: {
      searchList: [
        {
          createdAt: "2024-11-14T14:09:32.995",
          updatedAt: "2024-11-14T14:09:32.995",
          dataTypeId: "a1b2c3d4-e5f6-7890-1234-567890abcdef",
          dataTypeName: "Identity Data",
          dataItems: [
            "Full Name",
            "Date of Birth / Age",
            "Gender",
            "Photograph",
            "Signature",
            "National Identifier (PAN, Aadhaar, Passport, Voter ID, Driving License)",
            "Employee ID / Student ID"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.051",
          updatedAt: "2024-11-14T14:09:33.051",
          dataTypeId: "b2c3d4e5-f678-9012-3456-7890abcdef12",
          dataTypeName: "Contact Data",
          dataItems: [
            "Email Address",
            "Phone Number (Mobile / Landline)",
            "Residential Address",
            "Office Address",
            "Emergency Contact Details"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.083",
          updatedAt: "2024-11-14T14:09:33.083",
          dataTypeId: "c3d4e5f6-7890-1234-5678-90abcdef1234",
          dataTypeName: "Financial Data",
          dataItems: [
            "Bank Account Number",
            "IFSC Code",
            "Credit/Debit Card Details (masked storage)",
            "UPI ID",
            "Income Details",
            "Tax Identifier (PAN/TAN/GSTIN)",
            "Loan/EMI Records",
            "Insurance Policy Numbers"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.088",
          updatedAt: "2024-11-14T14:09:33.088",
          dataTypeId: "d4e5f678-9012-3456-7890-abcdef123456",
          dataTypeName: "Authentication & Security Data",
          dataItems: [
            "Username",
            "Password (hashed only)",
            "PIN",
            "Security Questions/Answers",
            "OTP / Token",
            "Biometric (Fingerprint, Face ID, Iris scan)"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.092",
          updatedAt: "2024-11-14T14:09:33.092",
          dataTypeId: "e5f67890-1234-5678-90ab-cdef12345678",
          dataTypeName: "Health & Sensitive Personal Data",
          dataItems: [
            "Medical Records",
            "Health Insurance Details",
            "Disability Information",
            "Prescription History",
            "Lab Test Reports",
            "Genetic / Biometric Health Data"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.096",
          updatedAt: "2024-11-14T14:09:33.096",
          dataTypeId: "f67890ab-cdef-1234-5678-90abcdef1234",
          dataTypeName: "Employment & Professional Data",
          dataItems: [
            "Employer Name",
            "Job Title / Designation",
            "Employee Code",
            "Work Email",
            "Work Phone",
            "Salary / Compensation Data",
            "Performance Records"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.113",
          updatedAt: "2024-11-14T14:09:33.113",
          dataTypeId: "7890abcd-ef12-3456-7890-abcdef123456",
          dataTypeName: "Educational Data",
          dataItems: [
            "School/University Name",
            "Enrollment/Student ID",
            "Marksheets / Transcripts",
            "Certifications",
            "Degrees/Diplomas"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.131",
          updatedAt: "2024-11-14T14:09:33.131",
          dataTypeId: "90abcdef-1234-5678-90ab-cdef12345678",
          dataTypeName: "Digital Interaction Data",
          dataItems: [
            "IP Address",
            "Device ID",
            "Browser Fingerprint",
            "Cookies / Tracking IDs",
            "Location Data (GPS, Cell Tower)",
            "App Usage Data",
            "Log Files"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.136",
          updatedAt: "2024-11-14T14:09:33.136",
          dataTypeId: "abcdef12-3456-7890-abcd-ef1234567890",
          dataTypeName: "Transactional Data",
          dataItems: [
            "Purchase History",
            "Payment History",
            "Subscription Details",
            "Service Usage Records"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        },
        {
          createdAt: "2024-11-14T14:09:33.140",
          updatedAt: "2024-11-14T14:09:33.140",
          dataTypeId: "bcdef123-4567-890a-bcde-f12345678901",
          dataTypeName: "Grievance & Communication Data",
          dataItems: [
            "Grievance Ticket ID",
            "Description of Issue",
            "Supporting Documents (Base64 encoded)",
            "Communication Channel (Email, SMS, Phone, WhatsApp)",
            "Resolution Notes"
          ],
          businessId: "sandbox-business-id",
          scopeType: "TENANT",
          status: "ACTIVE"
        }
      ]
    }
  },

  // Translations
  translations: {
    status: 200,
    data: {
      output: []  // Will be populated dynamically based on input
    }
  },

  // Processors
  processors: {
    status: 200,
    data: {
      searchList: [
        {
          dataProcessorId: "proc-1",
          dataProcessorName: "Email Service Provider",
          callbackUrl: "https://email-service.sandbox.com/callback",
          details: "Handles email communications",
          identityType: "EMAIL",
          isCrossBordered: false,
          spoc: {
            name: "Email SPOC",
            email: "spoc@emailservice.com",
            mobile: "+91 9876543210"
          }
        },
        {
          dataProcessorId: "proc-2",
          dataProcessorName: "Analytics Platform",
          callbackUrl: "https://analytics.sandbox.com/callback",
          details: "Processes analytics data",
          identityType: "EMAIL",
          isCrossBordered: true,
          spoc: {
            name: "Analytics SPOC",
            email: "spoc@analytics.com",
            mobile: "+91 9876543211"
          }
        },
        {
          dataProcessorId: "proc-3",
          dataProcessorName: "SMS Gateway",
          callbackUrl: "https://sms.sandbox.com/callback",
          details: "Handles SMS notifications",
          identityType: "MOBILE",
          isCrossBordered: false,
          spoc: {
            name: "SMS SPOC",
            email: "spoc@sms.com",
            mobile: "+91 9876543212"
          }
        }
      ]
    }
  },

  // Processing Activities
  processingActivities: {
    status: 200,
    data: {
      searchList: [
        {
          processorActivityId: "pa-1",
          activityName: "Email Campaign Processing",
          processorName: "Email Service Provider",
          status: "ACTIVE",
          details: "Processing emails for marketing campaigns",
          dataTypesList: [
            {
              dataTypeId: "dt-1",
              dataTypeName: "Personal Information",
              dataItems: ["Name", "Email"]
            }
          ]
        },
        {
          processorActivityId: "pa-2",
          activityName: "User Behavior Analytics",
          processorName: "Analytics Platform",
          status: "ACTIVE",
          details: "Analyzing user behavior patterns",
          dataTypesList: [
            {
              dataTypeId: "dt-3",
              dataTypeName: "Behavioral Data",
              dataItems: ["Browsing History", "Click Patterns"]
            },
            {
              dataTypeId: "dt-4",
              dataTypeName: "Device Information",
              dataItems: ["IP Address", "Device ID"]
            }
          ]
        },
        {
          processorActivityId: "pa-3",
          activityName: "SMS Notifications",
          processorName: "SMS Gateway",
          status: "ACTIVE",
          details: "Sending SMS notifications to users",
          dataTypesList: [
            {
              dataTypeId: "dt-1",
              dataTypeName: "Personal Information",
              dataItems: ["Phone Number", "Name"]
            }
          ]
        }
      ]
    }
  },

  // Consent Templates
  templates: {
    status: 200,
    data: {
      searchList: [
        {
          templateId: "template-1",
          templateName: "Marketing Consent Form",
          version: "1.0",
          status: "PUBLISHED",
          createdAt: "2024-01-15T10:00:00Z",
          updatedAt: "2024-01-15T10:00:00Z",
          channelType: "POPUP",
          templateType: "CONSENT",
          createdBy: "admin@sandbox.com",
          preferences: [
            {
              purposeIds: ["purpose-1", "purpose-2"],
              processorActivityIds: ["activity-1", "activity-2"]
            }
          ]
        },
        {
          templateId: "template-2",
          templateName: "Service Agreement",
          version: "2.0",
          status: "DRAFT",
          createdAt: "2024-02-20T10:00:00Z",
          updatedAt: "2024-02-20T10:00:00Z",
          channelType: "FORM",
          templateType: "CONSENT",
          createdBy: "dpo@sandbox.com",
          preferences: [
            {
              purposeIds: ["purpose-3", "purpose-4"],
              processorActivityIds: ["activity-3"]
            }
          ]
        },
        {
          templateId: "template-3",
          templateName: "Analytics Consent",
          version: "1.5",
          status: "PUBLISHED",
          createdAt: "2024-03-10T10:00:00Z",
          updatedAt: "2024-03-10T10:00:00Z",
          channelType: "BANNER",
          templateType: "CONSENT",
          createdBy: "admin@sandbox.com",
          preferences: [
            {
              purposeIds: ["purpose-2", "purpose-5"],
              processorActivityIds: ["activity-2", "activity-4"]
            }
          ]
        },
        {
          templateId: "template-4",
          templateName: "Newsletter Subscription",
          version: "1.0",
          status: "DRAFT",
          createdAt: "2024-04-05T12:00:00Z",
          updatedAt: "2024-04-05T12:00:00Z",
          channelType: "POPUP",
          templateType: "CONSENT",
          createdBy: "admin@sandbox.com",
          preferences: [
            {
              purposeIds: ["purpose-1"],
              processorActivityIds: ["activity-1"]
            }
          ]
        },
        {
          templateId: "template-5",
          templateName: "Account Registration Consent",
          version: "3.0",
          status: "DRAFT",
          createdAt: "2024-05-12T08:30:00Z",
          updatedAt: "2024-05-12T08:30:00Z",
          channelType: "FORM",
          templateType: "CONSENT",
          createdBy: "dpo@sandbox.com",
          preferences: [
            {
              purposeIds: ["purpose-4", "purpose-3"],
              processorActivityIds: ["activity-2", "activity-3"]
            }
          ]
        }
      ]
    }
  },

  // Template Details - wrapped in searchList for consistency
  templateDetails: {
    status: 200,
    data: {
      searchList: [{
        templateId: "template-1",
        templateName: "Marketing Consent Form",
        version: "1.0",
        channelType: "POPUP",
        status: "PUBLISHED",

        // Document metadata
        documentMeta: {
          name: "consent_document.pdf",
          size: "125KB",
          uploadDate: "2024-11-15"
        },

        // Multilingual support
        multilingual: {
          supportedLanguages: ["ENGLISH", "HINDI"],
          languageSpecificContentMap: {
            ENGLISH: {
              label: "Required",
              description: "While using Sandbox Demo Corp, your activities create data which will be used with your consent to offer customised services. Details of data usage are provided below.",
              rightsText: "To withdraw your consent, exercise your rights, or file complaints with the Board click here.",
              permissionText: "By clicking 'Allow all' or 'Save my choices', you are providing your consent to Reliance Medlab using your data as outlined above.",
              title: "Manage Consent",
              cid: "Customer ID",
              age: "I'm below 18 years of age.",
              purposeHeading: "Purpose: ",
              processingHeading: "Processing activity ",
              usedByHeading: "Used By: ",
              durationHeading: "Duration: ",
              dataItemHeading: "Data item: ",
              buttonText: "Allow all",
              saveButtonText: "Save my choices",
              others: "Others",
              allPurpose_desc_0: "To send you promotional emails and special offers",
              purpose_desc_0: "Marketing Communications - We will send you updates about new products, services, and special offers via email."
            },
            HINDI: {
              label: "आवश्यक",
              description: "Sandbox Demo Corp का उपयोग करते समय, आपकी गतिविधियाँ डेटा बनाती हैं जिसका उपयोग आपकी सहमति से अनुकूलित सेवाएं प्रदान करने के लिए किया जाएगा। डेटा उपयोग का विवरण नीचे प्रदान किया गया है।",
              rightsText: "अपनी सहमति वापस लेने, अपने अधिकारों का प्रयोग करने, या बोर्ड के साथ शिकायत दर्ज करने के लिए यहां क्लिक करें।",
              permissionText: "'सभी को अनुमति दें' या 'मेरी पसंद सहेजें' पर क्लिक करके, आप रिलायंस मेडलैब को ऊपर बताए अनुसार अपने डेटा का उपयोग करने की सहमति दे रहे हैं।",
              title: "सहमति प्रबंधित करें",
              cid: "ग्राहक आईडी",
              age: "मैं 18 वर्ष से कम उम्र का हूं।",
              purposeHeading: "उद्देश्य: ",
              processingHeading: "प्रसंस्करण गतिविधि ",
              usedByHeading: "द्वारा उपयोग किया गया: ",
              durationHeading: "अवधि: ",
              dataItemHeading: "डेटा आइटम: ",
              buttonText: "सभी को अनुमति दें",
              saveButtonText: "मेरी पसंद सहेजें",
              others: "अन्य"
            }
          }
        },

        // UI Configuration (Branding)
        uiConfig: {
          theme: btoa(JSON.stringify({
            light: JSON.stringify({
              cardBackground: "#FFFFFF",
              cardFont: "#000000",
              buttonBackground: "#0F3CC9",
              buttonFont: "#FFFFFF",
              linkFont: "#0A2885"
            }),
            dark: JSON.stringify({
              cardBackground: "#000000",
              cardFont: "#FFFFFF",
              buttonBackground: "#FFFFFF",
              buttonFont: "#0F3CC9",
              linkFont: "#0A2885"
            })
          })),
          logo: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
          darkMode: true,
          mobileView: true,
          parentalControl: false,
          dataTypeToBeShown: true,
          dataItemToBeShown: true,
          processActivityNameToBeShown: true,
          processorNameToBeShown: true,
          validitytoBeShown: true
        },

        // Preferences/Purposes
        preferences: [
          {
            purpose: "Marketing Communications",
            purposeId: "purpose-1",
            purposeIds: ["purpose-1"],
            isMandatory: "Optional",
            preferenceValidity: {
              value: 365,
              unit: "DAYS"
            },
            autoRenew: "No",
            processorActivityIds: ["pa-1", "pa-3"],
            processingActivities: [
              {
                id: "pa-1",
                name: "Email Campaign Processing",
                description: "Processing customer data for email marketing campaigns"
              },
              {
                id: "pa-3",
                name: "SMS Notifications",
                description: "Sending promotional SMS messages"
              }
            ],
            usedBy: [
              {
                id: "processor-1",
                name: "Email Service Provider",
                description: "Third-party email delivery service"
              },
              {
                id: "processor-2",
                name: "SMS Gateway",
                description: "Third-party SMS delivery service"
              }
            ],
            dataItems: [
              {
                dataTypeId: "dt-1",
                dataTypeName: "Contact Information",
                items: ["Email Address", "Phone Number", "Full Name"]
              },
              {
                dataTypeId: "dt-2",
                dataTypeName: "Preferences",
                items: ["Communication Preferences", "Product Interests"]
              }
            ]
          },
          {
            purpose: "Analytics",
            purposeId: "purpose-2",
            purposeIds: ["purpose-2"],
            isMandatory: "Mandatory",
            preferenceValidity: {
              value: 730,
              unit: "DAYS"
            },
            autoRenew: "Yes",
            processorActivityIds: ["pa-2"],
            processingActivities: [
              {
                id: "pa-2",
                name: "User Behavior Analytics",
                description: "Tracking user behavior on website"
              }
            ],
            usedBy: [
              {
                id: "processor-3",
                name: "Analytics Platform",
                description: "Third-party analytics service"
              }
            ],
            dataItems: [
              {
                dataTypeId: "dt-3",
                dataTypeName: "Usage Data",
                items: ["Page Views", "Click Events", "Session Duration"]
              }
            ]
          }
        ]
      }]
    }
  },

  // Consent Logs - List of consents by template
  consentLogs: {
    status: 200,
    data: {
      searchList: [
        {
          consentId: "consent-001",
          customerIdentifiers: {
            type: "EMAIL",
            value: "user1@sandboxdemo.com"
          },
          templateName: "Marketing Consent Form",
          templateId: "template-1",
          templateVersion: "1.0",
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          encrypted: true,
          digitallySigned: true,
          immutableMetadata: true,
          status: "PUBLISHED",
          consentStatus: "ACTIVE",
          preferences: [
            {
              mandatory: false,
              preferenceStatus: "ACCEPTED",
              purposeList: [
                {
                  purposeId: "purpose-1",
                  purposeInfo: {
                    purposeName: "Marketing Communications",
                    purposeCode: "MKTG-001"
                  }
                }
              ],
              processorActivityList: [
                {
                  processActivityInfo: {
                    activityName: "Email Campaign Processing",
                    dataTypesList: [
                      {
                        dataTypeName: "Personal Information",
                        dataItems: ["Email", "Name"]
                      }
                    ]
                  }
                }
              ]
            }
          ]
        },
        {
          consentId: "consent-002",
          customerIdentifiers: {
            type: "EMAIL",
            value: "user2@sandboxdemo.com"
          },
          templateName: "Marketing Consent Form",
          templateId: "template-1",
          templateVersion: "1.0",
          createdAt: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
          updatedAt: new Date(Date.now() - 86400000).toISOString(),
          encrypted: true,
          digitallySigned: true,
          immutableMetadata: true,
          status: "PUBLISHED",
          consentStatus: "ACTIVE",
          preferences: [
            {
              mandatory: false,
              preferenceStatus: "ACCEPTED",
              purposeList: [
                {
                  purposeId: "purpose-1",
                  purposeInfo: {
                    purposeName: "Marketing Communications",
                    purposeCode: "MKTG-001"
                  }
                }
              ],
              processorActivityList: [
                {
                  processActivityInfo: {
                    activityName: "Email Campaign Processing",
                    dataTypesList: [
                      {
                        dataTypeName: "Personal Information",
                        dataItems: ["Email", "Name", "Phone"]
                      }
                    ]
                  }
                }
              ]
            },
            {
              mandatory: true,
              preferenceStatus: "ACCEPTED",
              purposeList: [
                {
                  purposeId: "purpose-2",
                  purposeInfo: {
                    purposeName: "Analytics",
                    purposeCode: "ANLYT-001"
                  }
                }
              ],
              processorActivityList: [
                {
                  processActivityInfo: {
                    activityName: "Website Analytics",
                    dataTypesList: [
                      {
                        dataTypeName: "Usage Data",
                        dataItems: ["Page Views", "Click Events"]
                      }
                    ]
                  }
                }
              ]
            }
          ]
        },
        {
          consentId: "consent-003",
          customerIdentifiers: {
            type: "PHONE",
            value: "+91-9876543210"
          },
          templateName: "Marketing Consent Form",
          templateId: "template-1",
          templateVersion: "1.0",
          createdAt: new Date(Date.now() - 172800000).toISOString(), // 2 days ago
          updatedAt: new Date(Date.now() - 172800000).toISOString(),
          encrypted: true,
          digitallySigned: true,
          immutableMetadata: true,
          status: "PUBLISHED",
          consentStatus: "WITHDRAWN",
          preferences: [
            {
              mandatory: false,
              preferenceStatus: "WITHDRAWN",
              purposeList: [
                {
                  purposeId: "purpose-1",
                  purposeInfo: {
                    purposeName: "Marketing Communications",
                    purposeCode: "MKTG-001"
                  }
                }
              ],
              processorActivityList: [
                {
                  processActivityInfo: {
                    activityName: "SMS Marketing",
                    dataTypesList: [
                      {
                        dataTypeName: "Contact Information",
                        dataItems: ["Phone Number"]
                      }
                    ]
                  }
                }
              ]
            }
          ]
        },
        {
          consentId: "consent-004",
          customerIdentifiers: {
            type: "EMAIL",
            value: "user3@sandboxdemo.com"
          },
          templateName: "Marketing Consent Form",
          templateId: "template-1",
          templateVersion: "1.0",
          createdAt: new Date(Date.now() - 604800000).toISOString(), // 1 week ago
          updatedAt: new Date(Date.now() - 604800000).toISOString(),
          encrypted: true,
          digitallySigned: true,
          immutableMetadata: true,
          status: "PUBLISHED",
          consentStatus: "EXPIRED",
          preferences: [
            {
              mandatory: false,
              preferenceStatus: "EXPIRED",
              purposeList: [
                {
                  purposeId: "purpose-1",
                  purposeInfo: {
                    purposeName: "Marketing Communications",
                    purposeCode: "MKTG-001"
                  }
                }
              ],
              processorActivityList: [
                {
                  processActivityInfo: {
                    activityName: "Email Campaign Processing",
                    dataTypesList: [
                      {
                        dataTypeName: "Personal Information",
                        dataItems: ["Email", "Name"]
                      }
                    ]
                  }
                }
              ]
            }
          ]
        },
        {
          consentId: "consent-005",
          customerIdentifiers: {
            type: "EMAIL",
            value: "user4@sandboxdemo.com"
          },
          templateName: "Marketing Consent Form",
          templateId: "template-1",
          templateVersion: "1.0",
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          encrypted: true,
          digitallySigned: true,
          immutableMetadata: true,
          status: "PUBLISHED",
          consentStatus: "INACTIVE",
          preferences: [
            {
              mandatory: false,
              preferenceStatus: "PENDING",
              purposeList: [
                {
                  purposeId: "purpose-1",
                  purposeInfo: {
                    purposeName: "Marketing Communications",
                    purposeCode: "MKTG-001"
                  }
                }
              ],
              processorActivityList: [
                {
                  processActivityInfo: {
                    activityName: "Email Campaign Processing",
                    dataTypesList: [
                      {
                        dataTypeName: "Personal Information",
                        dataItems: ["Email"]
                      }
                    ]
                  }
                }
              ]
            }
          ]
        }
      ]
    }
  },

  // Consent Counts - Status counts by template
  consentCounts: {
    status: 200,
    data: {
      statusCounts: {
        ACTIVE: 12,
        INACTIVE: 5,
        WITHDRAWN: 3,
        EXPIRED: 2
      },
      totalCount: 22,
      templateId: "template-1"
    }
  },

  // Grievance Templates
  grievanceTemplates: {
    status: 200,
    data: [
      {
        grievanceTemplateId: "grv-template-1",
        grievanceTemplateName: "General Grievance Form",
        version: "1.0",
        status: "PUBLISHED",
        createdAt: "2024-01-15T10:00:00Z",
        updatedAt: "2024-11-01T10:00:00Z",
        description: "Standard form for submitting general grievances"
      },
      {
        grievanceTemplateId: "grv-template-2",
        grievanceTemplateName: "Data Deletion Request",
        version: "1.0",
        status: "PUBLISHED",
        createdAt: "2024-02-10T10:00:00Z",
        updatedAt: "2024-11-05T10:00:00Z",
        description: "Form to request deletion of personal data"
      },
      {
        grievanceTemplateId: "grv-template-3",
        grievanceTemplateName: "Privacy Concern",
        version: "2.0",
        status: "DRAFT",
        createdAt: "2024-03-20T10:00:00Z",
        updatedAt: "2024-11-10T10:00:00Z",
        description: "Form for reporting privacy-related concerns"
      }
    ]
  },

  // Grievance Template Details (for editing)
  grievanceTemplateDetails: {
    status: 200,
    data: {
      grievanceTemplateId: "grv-template-1",
      grievanceTemplateName: "General Grievance Form",
      version: "1.0",
      status: "PUBLISHED",
      description: "Standard form for submitting general grievances",
      grievanceTypes: ["DATA_ACCESS", "DATA_DELETION", "DATA_CORRECTION"],
      formFields: [
        {
          fieldId: "field-1",
          fieldName: "name",
          fieldLabel: "Full Name",
          fieldType: "TEXT",
          required: true,
          order: 1
        },
        {
          fieldId: "field-2",
          fieldName: "email",
          fieldLabel: "Email Address",
          fieldType: "EMAIL",
          required: true,
          order: 2
        },
        {
          fieldId: "field-3",
          fieldName: "grievanceType",
          fieldLabel: "Type of Request",
          fieldType: "SELECT",
          required: true,
          options: ["Data Access", "Data Deletion", "Data Correction"],
          order: 3
        },
        {
          fieldId: "field-4",
          fieldName: "description",
          fieldLabel: "Description",
          fieldType: "TEXTAREA",
          required: true,
          order: 4
        }
      ],
      multilingual: {
        supportedLanguages: ["ENGLISH", "HINDI"],
        languageSpecificContentMap: {
          ENGLISH: {
            title: "Submit Your Request",
            description: "Fill in the details below to submit your grievance request. We will respond within 72 hours.",
            submitButtonText: "Submit Request",
            cancelButtonText: "Cancel",
            thankYouMessage: "Thank you for your submission. We have received your request.",
            fields: {
              name: "Full Name",
              email: "Email Address",
              grievanceType: "Type of Request",
              description: "Describe your request"
            }
          },
          HINDI: {
            title: "अपना अनुरोध जमा करें",
            description: "अपना शिकायत अनुरोध सबमिट करने के लिए नीचे विवरण भरें। हम 72 घंटों के भीतर जवाब देंगे।",
            submitButtonText: "अनुरोध सबमिट करें",
            cancelButtonText: "रद्द करें",
            thankYouMessage: "आपके सबमिशन के लिए धन्यवाद। हमें आपका अनुरोध प्राप्त हो गया है।",
            fields: {
              name: "पूरा नाम",
              email: "ईमेल पता",
              grievanceType: "अनुरोध का प्रकार",
              description: "अपने अनुरोध का वर्णन करें"
            }
          }
        }
      },
      branding: {
        logo: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
        primaryColor: "#0F3CC9",
        secondaryColor: "#FFFFFF",
        fontFamily: "Arial, sans-serif"
      },
      notifications: {
        emailNotification: true,
        smsNotification: false,
        autoAcknowledgement: true
      },
      slaConfig: {
        responseTimeHours: 72,
        escalationEnabled: true,
        escalationTimeHours: 120
      },
      createdAt: "2024-01-15T10:00:00Z",
      updatedAt: "2024-11-01T10:00:00Z"
    }
  },

  // Consents (Logs)
  consents: {
    status: 200,
    data: {
      searchList: Array.from({ length: 50 }, (_, i) => {
        const statuses = ['ACTIVE', 'WITHDRAWN', 'EXPIRED', 'PENDING'];
        const templates = ['Marketing Consent Form', 'Service Agreement', 'Analytics Consent', 'Newsletter Subscription', 'Account Registration Consent'];
        const names = ['Alice Johnson', 'Bob Smith', 'Carol White', 'David Brown', 'Emma Davis', 'Frank Miller', 'Grace Lee', 'Henry Wilson', 'Iris Chen', 'Jack Taylor'];
        const status = statuses[i % 4];
        const daysAgo = Math.floor(Math.random() * 365);
        const createdDate = new Date(Date.now() - daysAgo * 24 * 60 * 60 * 1000);
        const expiryDate = new Date(createdDate.getTime() + 365 * 24 * 60 * 60 * 1000);

        return {
          consentId: `consent-${i + 1}`,
          consentHandleId: `handle-${i + 1}`,
          templateId: `template-${(i % 5) + 1}`,
          templateName: templates[i % templates.length],
          dataSubjectId: `user-${1000 + i}`,
          dataSubjectName: names[i % names.length],
          dataSubjectEmail: `${names[i % names.length].toLowerCase().replace(' ', '.')}@example.com`,
          status: status,
          createdAt: createdDate.toISOString(),
          expiryDate: status !== 'WITHDRAWN' ? expiryDate.toISOString() : null,
          withdrawnAt: status === 'WITHDRAWN' ? new Date(createdDate.getTime() + Math.random() * 180 * 24 * 60 * 60 * 1000).toISOString() : null,
          channel: ['WEB', 'MOBILE', 'EMAIL', 'SMS'][i % 4],
          ipAddress: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
          consentVersion: `${Math.floor(Math.random() * 3) + 1}.0`
        };
      })
    }
  },

  // Consent Handle Details
  consentHandleDetails: {
    status: 200,
    data: {
      consentHandleId: "handle-1",
      consentId: "consent-1",
      templateName: "Marketing Consent Form",
      dataSubjectId: "user-123",
      dataSubjectName: "Alice Johnson",
      dataSubjectEmail: "alice.johnson@example.com",
      status: "ACTIVE",
      createdAt: "2024-11-01T10:30:00Z",
      expiryDate: "2025-11-01T10:30:00Z",
      purposes: [
        {
          purposeName: "Marketing Communications",
          isMandatory: "Optional",
          consentGiven: true,
          validityPeriod: "365 DAYS"
        }
      ],
      artifact: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.sandbox.artifact"
    }
  },

  // Grievances
  grievances: {
    status: 200,
    data: Array.from({ length: 25 }, (_, i) => {
      const types = ['DATA_ACCESS', 'DATA_DELETION', 'DATA_CORRECTION', 'DATA_PORTABILITY', 'CONSENT_WITHDRAWAL', 'COMPLAINT'];
      const statuses = ['NEW', 'INPROCESS', 'RESOLVED']; // Using correct status values
      const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
      const names = ['Emma Davis', 'Frank Miller', 'Grace Lee', 'Henry Wilson', 'Iris Chen', 'Jack Taylor', 'Kate Anderson', 'Liam Brown', 'Mia Garcia', 'Noah Martinez'];
      const subjects = [
        'Request for Data Access',
        'Request to Delete My Data',
        'Incorrect Email Address',
        'Update Personal Information',
        'Withdraw Consent',
        'Data Breach Concern',
        'Privacy Policy Complaint',
        'Request for Data Export',
        'Unauthorized Data Processing',
        'Marketing Email Issue'
      ];

      const daysAgo = Math.floor(Math.random() * 90);
      const createdDate = new Date(Date.now() - daysAgo * 24 * 60 * 60 * 1000);

      // Assign status with weighted distribution: 5 NEW, 8 INPROCESS, 12 RESOLVED
      let status;
      if (i < 5) status = 'NEW';
      else if (i < 13) status = 'INPROCESS';
      else status = 'RESOLVED';

      const resolvedDate = (status === 'RESOLVED')
        ? new Date(createdDate.getTime() + Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString()
        : null;

      return {
        grievanceId: `GRV-2024-${String(i + 1).padStart(4, '0')}`,
        grievanceTemplateId: "grv-template-1", // Link to template
        grievanceType: types[i % types.length],
        grievanceDetail: types[i % types.length], // Add grievanceDetail field
        subject: subjects[i % subjects.length],
        description: `This is a detailed description of the grievance request. The user has raised concerns about their data privacy rights and requires assistance with ${subjects[i % subjects.length].toLowerCase()}.`,
        grievanceDescription: `This is a detailed description of the grievance request. The user has raised concerns about their data privacy rights and requires assistance with ${subjects[i % subjects.length].toLowerCase()}.`,
        status: status,
        priority: priorities[i % priorities.length],
        createdAt: createdDate.toISOString(),
        updatedAt: new Date(createdDate.getTime() + Math.random() * 5 * 24 * 60 * 60 * 1000).toISOString(),
        resolvedAt: resolvedDate,
        userDetails: {
          NAME: names[i % names.length],
          Name: names[i % names.length],
          email: `${names[i % names.length].toLowerCase().replace(' ', '.')}@sandboxdemo.com`,
          "Email address": `${names[i % names.length].toLowerCase().replace(' ', '.')}@sandboxdemo.com`,
          MOBILE: `+91 ${9000000000 + i}`,
          "Mobile Number": `+91 ${9000000000 + i}`
        },
        assignedTo: status !== 'NEW' ? ['DPO Officer', 'Admin User', 'Support Agent'][i % 3] : null,
        slaDeadline: new Date(createdDate.getTime() + 72 * 60 * 60 * 1000).toISOString(),
        responseCount: status === 'NEW' ? 0 : Math.floor(Math.random() * 5) + 1,
        remarks: status === 'RESOLVED'
          ? 'Issue resolved successfully. User data has been processed according to request.'
          : status === 'INPROCESS'
            ? 'Currently reviewing the request. Will update within 48 hours.'
            : 'New request received. Awaiting review.',
        attachments: i % 3 === 0 ? ['document1.pdf', 'screenshot.png'] : [],
        supportingDocs: i % 3 === 0 ? [
          { name: 'document1.pdf', url: `https://sandbox.example.com/attachments/GRV-2024-${String(i + 1).padStart(4, '0')}/document1.pdf`, size: '100KB', uploadedAt: createdDate.toISOString() },
          { name: 'screenshot.png', url: `https://sandbox.example.com/attachments/GRV-2024-${String(i + 1).padStart(4, '0')}/screenshot.png`, size: '250KB', uploadedAt: createdDate.toISOString() }
        ] : []
      };
    })
  },

  // Roles
  roles: {
    status: 200,
    data: {
      searchList: [
        {
          roleId: "role-1",
          role: "ADMIN",
          description: "Administrator with full access",
          permissions: ["READ", "WRITE", "DELETE", "MANAGE_USERS", "MANAGE_ROLES"]
        },
        {
          roleId: "role-2",
          role: "DPO",
          description: "Data Protection Officer",
          permissions: ["READ", "WRITE", "MANAGE_CONSENTS", "MANAGE_GRIEVANCES"]
        },
        {
          roleId: "role-3",
          role: "VIEWER",
          description: "Read-only access",
          permissions: ["READ"]
        }
      ]
    }
  },

  // Users
  users: {
    status: 200,
    data: {
      searchList: [
        {
          userId: "user-1",
          username: "Admin User",
          email: "admin@sandboxcms.com",
          designation: "System Administrator",
          roles: ["ADMIN"],
          status: "ACTIVE"
        },
        {
          userId: "user-2",
          username: "DPO Officer",
          email: "dpo@sandboxcms.com",
          designation: "Data Protection Officer",
          roles: ["DPO"],
          status: "ACTIVE"
        },
        {
          userId: "user-3",
          username: "Viewer User",
          email: "viewer@sandboxcms.com",
          designation: "Analyst",
          roles: ["VIEWER"],
          status: "ACTIVE"
        }
      ]
    }
  },

  // Cookie Categories
  cookieCategories: {
    status: 200,
    data: {
      searchList: [
        {
          categoryId: "cat-1",
          category: "Essential",
          description: "Necessary cookies for site functionality. These cookies enable core functionality such as security, network management, and accessibility."
        },
        {
          categoryId: "cat-2",
          category: "Analytics",
          description: "Cookies for tracking and analytics. These cookies help us understand how visitors interact with our website by collecting and reporting information anonymously."
        },
        {
          categoryId: "cat-3",
          category: "Marketing",
          description: "Cookies for advertising purposes. These cookies are used to make advertising messages more relevant to you and your interests."
        },
        {
          categoryId: "cat-4",
          category: "Functional",
          description: "Functional cookies enable enhanced functionality and personalization. They may be set by us or by third-party providers whose services we have added to our pages."
        },
        {
          categoryId: "cat-5",
          category: "Performance",
          description: "Performance cookies allow us to count visits and traffic sources so we can measure and improve the performance of our site."
        },
        {
          categoryId: "cat-6",
          category: "Social Media",
          description: "Social media cookies are set by a range of social media services that we have added to the site to enable you to share our content with your friends and networks."
        },
        {
          categoryId: "cat-7",
          category: "Targeting",
          description: "Targeting cookies may be set through our site by our advertising partners. They may be used to build a profile of your interests and show you relevant adverts on other sites."
        },
        {
          categoryId: "cat-8",
          category: "Strictly Necessary",
          description: "These cookies are essential for the website to function and cannot be switched off in our systems. They are usually only set in response to actions made by you."
        }
      ]
    }
  },

  // User Types
  userTypes: {
    status: 200,
    data: [
      { userTypeId: "ut-1", name: "Customer", description: "Regular customer" },
      { userTypeId: "ut-2", name: "Employee", description: "Company employee" },
      { userTypeId: "ut-3", name: "Partner", description: "Business partner" },
      { userTypeId: "ut-4", name: "Vendor", description: "External vendor" },
      { userTypeId: "ut-5", name: "Guest", description: "Guest user" }
    ]
  },

  // Roles
  roles: {
    status: 200,
    data: [
      {
        roleId: "role-1",
        role: "Administrator",
        description: "Full system access with all permissions",
        permissions: [
          { componentName: "Dashboard", accessType: "READ_WRITE" },
          { componentName: "User Management", accessType: "READ_WRITE" },
          { componentName: "Consent Management", accessType: "READ_WRITE" },
          { componentName: "Template Management", accessType: "READ_WRITE" },
          { componentName: "Reports", accessType: "READ_WRITE" }
        ],
        createdAt: "2024-01-15T10:00:00Z",
        updatedAt: "2024-01-15T10:00:00Z"
      },
      {
        roleId: "role-2",
        role: "Data Protection Officer",
        description: "DPO role with consent and data management permissions",
        permissions: [
          { componentName: "Dashboard", accessType: "READ" },
          { componentName: "Consent Management", accessType: "READ_WRITE" },
          { componentName: "Data Processing Register", accessType: "READ_WRITE" },
          { componentName: "ROPA", accessType: "READ_WRITE" },
          { componentName: "Reports", accessType: "READ" }
        ],
        createdAt: "2024-01-16T10:00:00Z",
        updatedAt: "2024-01-16T10:00:00Z"
      },
      {
        roleId: "role-3",
        role: "Consent Manager",
        description: "Manages consent templates and user consents",
        permissions: [
          { componentName: "Dashboard", accessType: "READ" },
          { componentName: "Consent Management", accessType: "READ_WRITE" },
          { componentName: "Template Management", accessType: "READ_WRITE" },
          { componentName: "Reports", accessType: "READ" }
        ],
        createdAt: "2024-01-17T10:00:00Z",
        updatedAt: "2024-01-17T10:00:00Z"
      },
      {
        roleId: "role-4",
        role: "Viewer",
        description: "Read-only access to view reports and dashboards",
        permissions: [
          { componentName: "Dashboard", accessType: "READ" },
          { componentName: "Reports", accessType: "READ" }
        ],
        createdAt: "2024-01-18T10:00:00Z",
        updatedAt: "2024-01-18T10:00:00Z"
      },
      {
        roleId: "role-5",
        role: "Grievance Handler",
        description: "Manages grievance requests and resolutions",
        permissions: [
          { componentName: "Dashboard", accessType: "READ" },
          { componentName: "Grievance Management", accessType: "READ_WRITE" },
          { componentName: "Reports", accessType: "READ" }
        ],
        createdAt: "2024-01-19T10:00:00Z",
        updatedAt: "2024-01-19T10:00:00Z"
      }
    ]
  },

  // Users
  users: {
    status: 200,
    data: [
      {
        userId: "user-1",
        username: "John Smith",
        email: "john.smith@sandboxcms.com",
        mobile: "+91 9876543210",
        designation: "Administrator",
        roles: [
          {
            roleId: "role-1",
            businessId: "sandbox-business-id",
            businessName: "Sandbox Demo Corp",
            roleName: "Administrator"
          }
        ],
        createdAt: "2024-01-15T10:00:00Z",
        updatedAt: "2024-01-15T10:00:00Z"
      },
      {
        userId: "user-2",
        username: "Sarah Johnson",
        email: "sarah.johnson@sandboxcms.com",
        mobile: "+91 9876543211",
        designation: "Data Protection Officer",
        roles: [
          {
            roleId: "role-2",
            businessId: "sandbox-business-id",
            businessName: "Sandbox Demo Corp",
            roleName: "Data Protection Officer"
          }
        ],
        createdAt: "2024-01-16T10:00:00Z",
        updatedAt: "2024-01-16T10:00:00Z"
      },
      {
        userId: "user-3",
        username: "Michael Chen",
        email: "michael.chen@sandboxcms.com",
        mobile: "+91 9876543212",
        designation: "Consent Manager",
        roles: [
          {
            roleId: "role-3",
            businessId: "sandbox-business-id",
            businessName: "Sandbox Demo Corp",
            roleName: "Consent Manager"
          },
          {
            roleId: "role-3",
            businessId: "sandbox-business-id-2",
            businessName: "Sandbox Tech Industries",
            roleName: "Consent Manager"
          }
        ],
        createdAt: "2024-01-17T10:00:00Z",
        updatedAt: "2024-01-17T10:00:00Z"
      },
      {
        userId: "user-4",
        username: "Emily Davis",
        email: "emily.davis@sandboxcms.com",
        mobile: "+91 9876543213",
        designation: "Analyst",
        roles: [
          {
            roleId: "role-4",
            businessId: "sandbox-business-id",
            businessName: "Sandbox Demo Corp",
            roleName: "Viewer"
          }
        ],
        createdAt: "2024-01-18T10:00:00Z",
        updatedAt: "2024-01-18T10:00:00Z"
      },
      {
        userId: "user-5",
        username: "David Wilson",
        email: "david.wilson@sandboxcms.com",
        mobile: "+91 9876543214",
        designation: "Grievance Officer",
        roles: [
          {
            roleId: "role-5",
            businessId: "sandbox-business-id",
            businessName: "Sandbox Demo Corp",
            roleName: "Grievance Handler"
          }
        ],
        createdAt: "2024-01-19T10:00:00Z",
        updatedAt: "2024-01-19T10:00:00Z"
      },
      {
        userId: "user-6",
        username: "Lisa Anderson",
        email: "lisa.anderson@sandboxcms.com",
        mobile: "+91 9876543215",
        designation: "Senior DPO",
        roles: [
          {
            roleId: "role-2",
            businessId: "sandbox-business-id-2",
            businessName: "Sandbox Tech Industries",
            roleName: "Data Protection Officer"
          }
        ],
        createdAt: "2024-01-20T10:00:00Z",
        updatedAt: "2024-01-20T10:00:00Z"
      }
    ]
  },

  // User Details
  userDetails: {
    status: 200,
    data: [
      { detailId: "ud-1", name: "Name", description: "Full name of user" },
      { detailId: "ud-2", name: "Email", description: "Email address" },
      { detailId: "ud-3", name: "Mobile Number", description: "Mobile phone number" },
      { detailId: "ud-4", name: "Address", description: "Physical address" },
      { detailId: "ud-5", name: "Date of Birth", description: "Date of birth" },
      { detailId: "ud-6", name: "PAN", description: "PAN card number" },
      { detailId: "ud-7", name: "Aadhaar", description: "Aadhaar number" }
    ]
  },

  // Grievance Types
  grievanceTypes: {
    status: 200,
    data: [
      {
        grievanceTypeId: "gt-1",
        grievanceType: "DATA_ACCESS",
        grievanceItem: ["Access Request", "Data Export", "Data Summary"],
        description: "Request to access personal data"
      },
      {
        grievanceTypeId: "gt-2",
        grievanceType: "DATA_DELETION",
        grievanceItem: ["Deletion Request", "Account Deletion", "Data Removal"],
        description: "Request to delete personal data"
      },
      {
        grievanceTypeId: "gt-3",
        grievanceType: "DATA_CORRECTION",
        grievanceItem: ["Correction Request", "Update Information", "Modify Data"],
        description: "Request to correct personal data"
      },
      {
        grievanceTypeId: "gt-4",
        grievanceType: "DATA_PORTABILITY",
        grievanceItem: ["Data Transfer", "Export Data", "Port Data"],
        description: "Request to transfer personal data"
      },
      {
        grievanceTypeId: "gt-5",
        grievanceType: "CONSENT_WITHDRAWAL",
        grievanceItem: ["Withdraw Consent", "Revoke Consent", "Opt Out"],
        description: "Request to withdraw consent"
      }
    ]
  },

  // Components (Permissions)
  components: {
    status: 200,
    data: {
      searchList: [
        { componentId: "comp-1", name: "Dashboard", permissions: ["READ"] },
        { componentId: "comp-2", name: "Consent Management", permissions: ["READ", "WRITE"] },
        { componentId: "comp-3", name: "User Management", permissions: ["READ", "WRITE", "DELETE"] },
        { componentId: "comp-4", name: "Reports", permissions: ["READ", "EXPORT"] }
      ]
    }
  },

  // Client Credentials
  clientCredentials: {
    status: 200,
    data: {
      consumerKey: "SANDBOX123",
      consumerSecret: "SECRET999",
      createdAt: "2024-01-01T00:00:00Z"
    }
  },

  // Consent Configuration
  consentConfiguration: {
    status: 200,
    data: {
      configId: "consent-config-1",
      slaHours: 48,
      autoEscalate: true,
      notificationEnabled: true
    }
  },

  // Grievance Configuration
  grievanceConfiguration: {
    status: 200,
    data: {
      configId: "grievance-config-1",
      slaHours: 72,
      autoEscalate: true,
      escalationLevels: ["Officer", "DPO"],
      notificationEnabled: true
    }
  },

  // Notification Configuration
  notificationConfiguration: {
    status: 200,
    data: {
      searchList: [
        {
          configId: "notif-config-1",
          businessId: "sandbox-business-id",
          scopeLevel: "TENANT",
          configurationJson: {
            clientId: "sandbox-client-id-123",
            clientSecret: "sandbox-client-secret-456",
            sid: "sandbox-sid-789",
            baseUrl: "https://api.sandbox.notification.com/v1",
            callbackUrl: "https://callback.sandbox.notification.com/webhook",
            networkType: "INTERNET",
            mutualCertificate: null,
            mutualCertificateMeta: null
          },
          createdAt: "2024-01-15T10:00:00Z",
          updatedAt: "2024-01-15T10:00:00Z"
        }
      ]
    }
  },

  // SMTP Configuration
  smtpConfiguration: {
    status: 200,
    data: {
      searchList: [
        {
          configId: "smtp-config-1",
          businessId: "sandbox-business-id",
          scopeLevel: "TENANT",
          smtpDetails: {
            serverAddress: "smtp.sandbox.example.com",
            port: 587,
            fromEmail: "noreply@sandbox.example.com",
            username: "smtp-user@sandbox.example.com",
            password: "smtp-password-123",
            tlsSsl: "TLS",
            senderDisplayName: "Sandbox CMS",
            connectionTimeout: 5000,
            smtpAuthEnabled: true,
            smtpConnectionTimeout: 5000,
            smtpSocketTimeout: 5000,
            replyTo: "support@sandbox.example.com",
            testEmail: null
          },
          createdAt: "2024-01-15T10:00:00Z",
          updatedAt: "2024-01-15T10:00:00Z"
        }
      ]
    }
  },

  // Digilocker Configuration
  digilockerConfiguration: {
    status: 200,
    data: {
      configId: "digi-config-1",
      enabled: true,
      clientId: "digilocker-client-id"
    }
  },

  // Audit Reports
  auditReports: {
    status: 200,
    data: {
      data: Array.from({ length: 25 }, (_, i) => {
        const modules = ['System Configuration', 'Consent Management', 'Cookie Management', 'Data Breach', 'ROPA', 'Master Data', 'User Management', 'Reports'];
        const components = ['DPO Settings', 'Consent Template', 'Cookie Banner', 'Breach Report', 'ROPA Entry', 'Data Type', 'User Profile', 'Compliance Report'];
        const actions = ['CREATE', 'UPDATE', 'DELETE', 'VIEW', 'EXPORT', 'APPROVE', 'REJECT'];
        const users = ['John Smith', 'Sarah Johnson', 'Michael Chen', 'Emily Davis', 'David Wilson'];
        const roles = ['DPO', 'Admin', 'Manager', 'Auditor', 'Operator'];
        
        const timestamp = new Date(Date.now() - (i * 1000 * 60 * 60 * 2)).toISOString(); // 2 hours apart
        
        return {
          auditId: `AUD-${String(i + 1).padStart(6, '0')}`,
          id: `audit-record-${i + 1}`,
          businessId: 'BUS-001',
          businessName: 'Jio Consent Management System - Sandbox Demo',
          timestamp: timestamp,
          createdAt: timestamp,
          updatedAt: timestamp,
          group: modules[i % modules.length],
          component: components[i % components.length],
          actionType: actions[i % actions.length],
          initiator: users[i % users.length],
          actor: {
            id: `actor-${i + 1}`,
            role: roles[i % roles.length],
            type: 'User'
          },
          resource: {
            id: `resource-${i + 1}`,
            type: components[i % components.length]
          },
          context: {
            txnId: `TXN-${String(i + 1).padStart(10, '0')}`,
            ipAddress: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`
          },
          payloadHash: `sha256:${Array.from({ length: 64 }, () => 
            Math.floor(Math.random() * 16).toString(16)).join('')}`,
          status: 'SUCCESS'
        };
      }),
      totalElements: 25,
      totalPages: 3,
      currentPage: 0,
      pageSize: 10
    }
  },

  // Scheduler Stats
  schedulerStats: {
    status: 200,
    data: [
      // Consent Expiry Notification - Multiple executions
      {
        id: "692464e0e7f4d24bdbdabf4a",
        runId: "a162e00e-a9a9-4ca2-9c20-47319a69e31a",
        jobName: "CONSENT_EXPIRY_NOTIFICATION",
        resources: [{ type: "CONSENT_ID", id: "consent-1", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "EXPIRY_NOTIFICATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 5,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T08:30:00.123",
        durationMillis: 245
      },
      {
        id: "692464e0e7f4d24bdbdabf4b",
        runId: "a162e00e-a9a9-4ca2-9c20-47319a69e31b",
        jobName: "CONSENT_EXPIRY_NOTIFICATION",
        resources: [{ type: "CONSENT_ID", id: "consent-2", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "EXPIRY_NOTIFICATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 3,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T09:15:00.456",
        durationMillis: 198
      },
      // Consent Renewal Reminder
      {
        id: "692464e0e7f4d24bdbdabf4c",
        runId: "b262e00e-a9a9-4ca2-9c20-47319a69e31c",
        jobName: "CONSENT_RENEWAL_REMINDER",
        resources: [{ type: "CONSENT_ID", id: "consent-3", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "RENEWAL_REMINDER",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 8,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T10:00:00.789",
        durationMillis: 312
      },
      // Grievance Escalation
      {
        id: "692464e0e7f4d24bdbdabf4d",
        runId: "c362e00e-a9a9-4ca2-9c20-47319a69e31d",
        jobName: "GRIEVANCE_ESCALATION_JOB",
        resources: [{ type: "GRIEVANCE_ID", id: "grievance-1", businessId: "sandbox-business-id" }],
        group: "GRIEVANCE",
        action: "GRIEVANCE_ESCALATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 1,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T11:30:00.234",
        durationMillis: 289
      },
      // Data Retention Cleanup - with one failure
      {
        id: "692464e0e7f4d24bdbdabf4e",
        runId: "d462e00e-a9a9-4ca2-9c20-47319a69e31e",
        jobName: "DATA_RETENTION_CLEANUP",
        resources: [{ type: "DATA_ID", id: "data-1", businessId: "sandbox-business-id" }],
        group: "DATA_MANAGEMENT",
        action: "RETENTION_CLEANUP",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 12,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T06:00:00.567",
        durationMillis: 421
      },
      {
        id: "692464e0e7f4d24bdbdabf4f",
        runId: "e562e00e-a9a9-4ca2-9c20-47319a69e31f",
        jobName: "DATA_RETENTION_CLEANUP",
        resources: [{ type: "DATA_ID", id: "data-2", businessId: "sandbox-business-id" }],
        group: "DATA_MANAGEMENT",
        action: "RETENTION_CLEANUP",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 0,
        errorCount: 1,
        lastError: "Database connection timeout",
        status: "FAILED",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T06:30:00.890",
        durationMillis: 5002
      },
      // Email Notification Job - currently running
      {
        id: "692464e0e7f4d24bdbdabf50",
        runId: "f662e00e-a9a9-4ca2-9c20-47319a69e320",
        jobName: "EMAIL_NOTIFICATION_JOB",
        resources: [{ type: "EMAIL_BATCH", id: "batch-1", businessId: "sandbox-business-id" }],
        group: "NOTIFICATION",
        action: "SEND_EMAIL",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 0,
        errorCount: 0,
        lastError: null,
        status: "RUNNING",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T14:45:00.123",
        durationMillis: 0
      },
      // Pending job
      {
        id: "692464e0e7f4d24bdbdabf51",
        runId: "g762e00e-a9a9-4ca2-9c20-47319a69e321",
        jobName: "CONSENT_EXPIRY_NOTIFICATION",
        resources: [{ type: "CONSENT_ID", id: "consent-4", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "EXPIRY_NOTIFICATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 0,
        errorCount: 0,
        lastError: null,
        status: "PENDING",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T15:00:00.000",
        durationMillis: 0
      },
      // Additional scheduler jobs for more comprehensive data
      {
        id: "692464e0e7f4d24bdbdabf52",
        runId: "h862e00e-a9a9-4ca2-9c20-47319a69e322",
        jobName: "CONSENT_EXPIRY_NOTIFICATION",
        resources: [{ type: "CONSENT_ID", id: "consent-5", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "EXPIRY_NOTIFICATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 12,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T16:00:00.000",
        durationMillis: 267
      },
      {
        id: "692464e0e7f4d24bdbdabf53",
        runId: "i962e00e-a9a9-4ca2-9c20-47319a69e323",
        jobName: "CONSENT_RENEWAL_REMINDER",
        resources: [{ type: "CONSENT_ID", id: "consent-6", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "RENEWAL_REMINDER",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 15,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T17:00:00.000",
        durationMillis: 345
      },
      {
        id: "692464e0e7f4d24bdbdabf54",
        runId: "j062e00e-a9a9-4ca2-9c20-47319a69e324",
        jobName: "DATA_RETENTION_CLEANUP",
        resources: [{ type: "DATA_ID", id: "data-3", businessId: "sandbox-business-id" }],
        group: "DATA_MANAGEMENT",
        action: "RETENTION_CLEANUP",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 25,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T18:00:00.000",
        durationMillis: 512
      },
      {
        id: "692464e0e7f4d24bdbdabf55",
        runId: "k162e00e-a9a9-4ca2-9c20-47319a69e325",
        jobName: "GRIEVANCE_ESCALATION_JOB",
        resources: [{ type: "GRIEVANCE_ID", id: "grievance-2", businessId: "sandbox-business-id" }],
        group: "GRIEVANCE",
        action: "GRIEVANCE_ESCALATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 2,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T19:00:00.000",
        durationMillis: 189
      },
      {
        id: "692464e0e7f4d24bdbdabf56",
        runId: "l262e00e-a9a9-4ca2-9c20-47319a69e326",
        jobName: "EMAIL_NOTIFICATION_JOB",
        resources: [{ type: "EMAIL_BATCH", id: "batch-2", businessId: "sandbox-business-id" }],
        group: "NOTIFICATION",
        action: "SEND_EMAIL",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 50,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T20:00:00.000",
        durationMillis: 678
      },
      {
        id: "692464e0e7f4d24bdbdabf57",
        runId: "m362e00e-a9a9-4ca2-9c20-47319a69e327",
        jobName: "SMS_NOTIFICATION_JOB",
        resources: [{ type: "SMS_BATCH", id: "batch-3", businessId: "sandbox-business-id" }],
        group: "NOTIFICATION",
        action: "SEND_SMS",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 30,
        errorCount: 0,
        lastError: null,
        status: "SUCCESS",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T21:00:00.000",
        durationMillis: 423
      },
      {
        id: "692464e0e7f4d24bdbdabf58",
        runId: "n462e00e-a9a9-4ca2-9c20-47319a69e328",
        jobName: "CONSENT_EXPIRY_NOTIFICATION",
        resources: [{ type: "CONSENT_ID", id: "consent-7", businessId: "sandbox-business-id" }],
        group: "CONSENT",
        action: "EXPIRY_NOTIFICATION",
        summaryType: "TENANT_SUMMARY",
        totalAffected: 0,
        errorCount: 1,
        lastError: "Network timeout",
        status: "FAILED",
        details: { batchSize: 500 },
        timestamp: "2025-11-24T22:00:00.000",
        durationMillis: 3000
      }
    ]
  },

  // Retention Policies
  retentionPolicies: {
    status: 200,
    data: {
      retentionId: "ret-1",
      retentions: {
        consent_artifact_retention: {
          value: 7,
          unit: "YEARS"
        },
        cookie_consent_artifact_retention: {
          value: 7,
          unit: "YEARS"
        },
        grievance_retention: {
          value: 7,
          unit: "YEARS"
        },
        logs_retention: {
          value: 7,
          unit: "YEARS"
        },
        data_retention: {
          value: 7,
          unit: "YEARS"
        }
      },
      createdAt: "2024-01-01T00:00:00Z",
      updatedAt: "2024-11-15T00:00:00Z"
    }
  },

  // Organization Map
  organizationMap: {
    status: 200,
    data: {
      dashboardData: {
        "Sandbox Demo Corp": {
          businessInfo: {
            businessId: "sandbox-business-id",
            name: "Sandbox Demo Corp",
            description: "Main business unit for sandbox environment",
            scopeLevel: "TENANT"
          },
          processors: [
            {
              processorId: "processor-1",
              processorName: "Cloud Services Provider",
              processorType: "THIRD_PARTY",
              contactEmail: "cloud@provider.com",
              contactPhone: "+91 9876543210",
              address: "123 Cloud Street, Tech City",
              status: "ACTIVE"
            },
            {
              processorId: "processor-2",
              processorName: "Analytics Platform Inc",
              processorType: "THIRD_PARTY",
              contactEmail: "analytics@platform.com",
              contactPhone: "+91 9876543211",
              address: "456 Data Avenue, Analytics Hub",
              status: "ACTIVE"
            },
            {
              processorId: "processor-3",
              processorName: "Payment Gateway Services",
              processorType: "THIRD_PARTY",
              contactEmail: "payments@gateway.com",
              contactPhone: "+91 9876543212",
              address: "789 Finance Road, Payment Center",
              status: "ACTIVE"
            }
          ],
          users: [
            {
              userId: "user-1",
              username: "John Smith",
              email: "john.smith@sandboxcms.com",
              designation: "Administrator",
              roles: [
                {
                  roleId: "role-1",
                  role: "Administrator",
                  businessId: "sandbox-business-id"
                }
              ]
            },
            {
              userId: "user-2",
              username: "Sarah Johnson",
              email: "sarah.johnson@sandboxcms.com",
              designation: "Data Protection Officer",
              roles: [
                {
                  roleId: "role-2",
                  role: "Data Protection Officer",
                  businessId: "sandbox-business-id"
                }
              ]
            },
            {
              userId: "user-3",
              username: "Michael Chen",
              email: "michael.chen@sandboxcms.com",
              designation: "Consent Manager",
              roles: [
                {
                  roleId: "role-3",
                  role: "Consent Manager",
                  businessId: "sandbox-business-id"
                }
              ]
            }
          ]
        },
        "Sandbox Tech Industries": {
          businessInfo: {
            businessId: "sandbox-business-id-2",
            name: "Sandbox Tech Industries",
            description: "Technology division for sandbox environment",
            scopeLevel: "BUSINESS"
          },
          processors: [
            {
              processorId: "processor-4",
              processorName: "AI Solutions Ltd",
              processorType: "THIRD_PARTY",
              contactEmail: "ai@solutions.com",
              contactPhone: "+91 9876543213",
              address: "321 AI Boulevard, Innovation Park",
              status: "ACTIVE"
            },
            {
              processorId: "processor-5",
              processorName: "Database Management Corp",
              processorType: "THIRD_PARTY",
              contactEmail: "db@management.com",
              contactPhone: "+91 9876543214",
              address: "654 Storage Lane, Data Center",
              status: "ACTIVE"
            }
          ],
          users: [
            {
              userId: "user-4",
              username: "Emily Davis",
              email: "emily.davis@sandboxcms.com",
              designation: "Analyst",
              roles: [
                {
                  roleId: "role-4",
                  role: "Viewer",
                  businessId: "sandbox-business-id-2"
                }
              ]
            },
            {
              userId: "user-6",
              username: "Lisa Anderson",
              email: "lisa.anderson@sandboxcms.com",
              designation: "Senior DPO",
              roles: [
                {
                  roleId: "role-2",
                  role: "Data Protection Officer",
                  businessId: "sandbox-business-id-2"
                }
              ]
            }
          ]
        }
      }
    }
  },

  // Consent Report
  consentReport: {
    status: 200,
    data: {
      totalConsents: 1250,
      activeConsents: 950,
      withdrawnConsents: 180,
      expiredConsents: 120,
      monthlyTrend: [
        { month: "Jan", count: 100 },
        { month: "Feb", count: 120 },
        { month: "Mar", count: 135 },
        { month: "Apr", count: 145 },
        { month: "May", count: 160 },
        { month: "Jun", count: 175 },
        { month: "Jul", count: 185 },
        { month: "Aug", count: 195 },
        { month: "Sep", count: 210 },
        { month: "Oct", count: 225 },
        { month: "Nov", count: 250 }
      ]
    }
  },

  // Email Templates
  emailTemplates: {
    status: 200,
    data: {
      data: [
        {
          templateId: "email-template-1",
          templateName: "Consent Confirmation Email",
          eventType: "CONSENT_COLLECTED",
          channel: "EMAIL",
          emailConfig: {
            subject: "Your Consent Has Been Recorded",
            body: "<html><body><h1>Thank you for providing your consent</h1><p>We have successfully recorded your consent preferences.</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-01-15T10:00:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "email-template-2",
          templateName: "Consent Withdrawal Notification",
          eventType: "CONSENT_WITHDRAWN",
          channel: "EMAIL",
          emailConfig: {
            subject: "Consent Withdrawal Confirmation",
            body: "<html><body><h1>Your consent has been withdrawn</h1><p>We have processed your consent withdrawal request.</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-02-10T14:30:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "email-template-3",
          templateName: "Consent Expiry Reminder",
          eventType: "CONSENT_EXPIRING",
          channel: "EMAIL",
          emailConfig: {
            subject: "Your Consent is About to Expire",
            body: "<html><body><h1>Consent Expiry Notice</h1><p>Your consent will expire in 30 days. Please renew if you wish to continue.</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-03-05T09:15:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "email-template-4",
          templateName: "Grievance Received Email",
          eventType: "GRIEVANCE_RECEIVED",
          channel: "EMAIL",
          emailConfig: {
            subject: "Your Grievance Has Been Received",
            body: "<html><body><h1>Grievance Request Received</h1><p>We have received your grievance request and will respond within 72 hours.</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-04-10T11:00:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "email-template-5",
          templateName: "Grievance Resolution Email",
          eventType: "GRIEVANCE_RESOLVED",
          channel: "EMAIL",
          emailConfig: {
            subject: "Your Grievance Has Been Resolved",
            body: "<html><body><h1>Grievance Resolved</h1><p>Your grievance has been successfully resolved. Details: {{RESOLUTION_DETAILS}}</p></body></html>"
          },
          status: "PUBLISHED",
          createdAt: "2024-04-15T14:20:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "email-template-6",
          templateName: "Grievance Update Email",
          eventType: "GRIEVANCE_IN_PROGRESS",
          channel: "EMAIL",
          emailConfig: {
            subject: "Update on Your Grievance",
            body: "<html><body><h1>Grievance Status Update</h1><p>Your grievance is currently in progress. Status: {{STATUS}}</p></body></html>"
          },
          status: "DRAFT",
          createdAt: "2024-04-20T16:45:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "email-template-7",
          templateName: "Consent Renewal Reminder",
          eventType: "CONSENT_RENEWAL_REMINDER",
          channel: "EMAIL",
          emailConfig: {
            subject: "Renew Your Consent",
            body: "<html><body><h1>Consent Renewal Reminder</h1><p>Your consent is due for renewal. Please review and update your preferences to continue using our services.</p><p>Renew now: {{RENEWAL_LINK}}</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-05-10T09:00:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "email-template-8",
          templateName: "Data Breach Notification",
          eventType: "DATA_BREACH",
          channel: "EMAIL",
          emailConfig: {
            subject: "Important: Data Security Notice",
            body: "<html><body><h1>Data Security Notice</h1><p>We are writing to inform you about a data security incident that may have affected your personal information.</p><p>Incident Details: {{INCIDENT_DETAILS}}</p><p>Recommended Actions: {{RECOMMENDED_ACTIONS}}</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-06-01T10:00:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "email-template-9",
          templateName: "Account Verification Email",
          eventType: "ACCOUNT_VERIFICATION",
          channel: "EMAIL",
          emailConfig: {
            subject: "Verify Your Account",
            body: "<html><body><h1>Account Verification Required</h1><p>Please verify your account by clicking the link below:</p><p><a href='{{VERIFICATION_LINK}}'>Verify Account</a></p><p>This link will expire in 24 hours.</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-06-15T11:00:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "email-template-10",
          templateName: "Password Reset Email",
          eventType: "PASSWORD_RESET",
          channel: "EMAIL",
          emailConfig: {
            subject: "Reset Your Password",
            body: "<html><body><h1>Password Reset Request</h1><p>You have requested to reset your password. Click the link below to proceed:</p><p><a href='{{RESET_LINK}}'>Reset Password</a></p><p>If you did not request this, please ignore this email.</p></body></html>"
          },
          status: "ACTIVE",
          createdAt: "2024-07-01T12:00:00Z",
          createdBy: "admin@sandbox.com"
        }
      ]
    }
  },

  // SMS Templates
  smsTemplates: {
    status: 200,
    data: {
      data: [
        {
          templateId: "sms-template-1",
          templateName: "Consent OTP",
          template: "Your consent verification OTP is: {{OTP}}. Valid for 10 minutes.",
          eventType: "CONSENT_OTP",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567890"
          },
          status: "ACTIVE",
          createdAt: "2024-01-20T11:00:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "sms-template-2",
          templateName: "Consent Confirmation SMS",
          template: "Your consent has been successfully recorded. Thank you - {{COMPANY_NAME}}",
          eventType: "CONSENT_COLLECTED",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567891"
          },
          status: "ACTIVE",
          createdAt: "2024-02-15T13:45:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "sms-template-3",
          templateName: "Consent Withdrawal SMS",
          template: "Your consent withdrawal has been processed. Ref: {{REF_NUMBER}} - {{COMPANY_NAME}}",
          eventType: "CONSENT_WITHDRAWN",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567892"
          },
          status: "ACTIVE",
          createdAt: "2024-03-10T10:30:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "sms-template-4",
          templateName: "Grievance Received SMS",
          template: "Your grievance request has been received. Ref: {{REF_NUMBER}}. We'll respond within 72 hours. Track status: {{TRACK_LINK}} - {{COMPANY_NAME}}",
          eventType: "GRIEVANCE_RECEIVED",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567893"
          },
          status: "ACTIVE",
          createdAt: "2024-04-12T09:30:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "sms-template-5",
          templateName: "Grievance Resolved SMS",
          template: "Your grievance (Ref: {{REF_NUMBER}}) has been resolved. Details: {{RESOLUTION_SUMMARY}}. Contact us for more info. - {{COMPANY_NAME}}",
          eventType: "GRIEVANCE_RESOLVED",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567894"
          },
          status: "ACTIVE",
          createdAt: "2024-04-18T14:00:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "sms-template-6",
          templateName: "Consent Expiry Reminder",
          template: "Your consent expires in {{DAYS_REMAINING}} days. Renew now: {{RENEWAL_LINK}} to continue services. - {{COMPANY_NAME}}",
          eventType: "CONSENT_EXPIRING",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567895"
          },
          status: "ACTIVE",
          createdAt: "2024-05-05T09:30:00Z",
          createdBy: "dpo@sandbox.com"
        },
        {
          templateId: "sms-template-7",
          templateName: "Consent Renewal Reminder",
          template: "Renew your consent to continue using our services. Review preferences: {{RENEWAL_LINK}} - {{COMPANY_NAME}}",
          eventType: "CONSENT_RENEWAL_REMINDER",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567896"
          },
          status: "ACTIVE",
          createdAt: "2024-05-12T10:15:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "sms-template-8",
          templateName: "Grievance Status Update",
          template: "Grievance (Ref: {{REF_NUMBER}}) status updated: {{STATUS}}. Check details: {{TRACK_LINK}} - {{COMPANY_NAME}}",
          eventType: "GRIEVANCE_STATUS_UPDATE",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567897"
          },
          status: "ACTIVE",
          createdAt: "2024-05-20T11:45:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "sms-template-9",
          templateName: "Account Verification SMS",
          template: "Your verification code is: {{VERIFICATION_CODE}}. Valid for 10 minutes. Do not share. - {{COMPANY_NAME}}",
          eventType: "ACCOUNT_VERIFICATION",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567898"
          },
          status: "ACTIVE",
          createdAt: "2024-06-10T12:00:00Z",
          createdBy: "admin@sandbox.com"
        },
        {
          templateId: "sms-template-10",
          templateName: "Password Reset SMS",
          template: "Your password reset code is: {{RESET_CODE}}. Valid for 15 minutes. If not requested, ignore this SMS. - {{COMPANY_NAME}}",
          eventType: "PASSWORD_RESET",
          channel: "SMS",
          smsConfig: {
            dltTemplateId: "1107168761234567899"
          },
          status: "ACTIVE",
          createdAt: "2024-06-25T13:30:00Z",
          createdBy: "admin@sandbox.com"
        }
      ]
    }
  },

  // Dashboard Statistics (DPO Dashboard)
  dashboardStats: {
    status: 200,
    data: {
      // Purpose and Data Management
      totalPurposeCreated: 12,
      dataTypes: 8,
      processingActivities: 15,
      dataProcessor: 6,
      
      // Retention Periods (in days) - converted to readable format by formatRetentionPeriod
      dataretentionPeriod: 2555, // 7 years (7 * 365 = 2555)
      logsretentionPeriod: 2555, // 7 years (7 * 365 = 2555)
      consentArtefactsretentionPeriod: 2555, // 7 years (7 * 365 = 2555)
      
      // Consents - Enhanced values
      totalConsents: 2847,
      activeConsents: 2156,
      revokedConsents: 342,
      expiredConsents: 289,
      autorenewalConsents: 156,
      pendingRenewal: 60,
      
      // Templates
      publishedTemp: 18,
      pendingRenewal: 60,
      
      // Cookies
      cookiesPublished: 24,
      cookiesDraft: 8,
      cookiesInactive: 4,
      cookiesTotal: 36,
      cookiesAllAccepted: 1850,
      cookiesPartiallyAccepted: 623,
      cookiesAllRejected: 214,
      cookiesNoAction: 160,
      
      // Logs and Artefacts
      logs: 15420,
      consentArtefacts: 2847,
      
      // Notifications
      notificationSent: 3420,
      notificationEmail: 2156,
      notificationSms: 1264,
      
      // Grievances
      grievanceTotalRequests: 127,
      grievanceResolved: 89,
      grievanceInProgress: 23,
      grievanceEscalated: 8,
      grievanceRejected: 7,
      resolvedSla: 82,
      exceededSla: 7,
      grievanceSms: 45,
      grievanceEmail: 82,
      
      // Trends (for charts)
      consentTrend: [
        { month: 'Jan', total: 100, active: 85, withdrawn: 10, expired: 5 },
        { month: 'Feb', total: 120, active: 100, withdrawn: 12, expired: 8 },
        { month: 'Mar', total: 135, active: 115, withdrawn: 10, expired: 10 },
        { month: 'Apr', total: 145, active: 125, withdrawn: 11, expired: 9 },
        { month: 'May', total: 160, active: 140, withdrawn: 12, expired: 8 },
        { month: 'Jun', total: 175, active: 152, withdrawn: 13, expired: 10 },
        { month: 'Jul', total: 185, active: 162, withdrawn: 14, expired: 9 },
        { month: 'Aug', total: 195, active: 172, withdrawn: 13, expired: 10 },
        { month: 'Sep', total: 210, active: 185, withdrawn: 15, expired: 10 },
        { month: 'Oct', total: 225, active: 198, withdrawn: 16, expired: 11 },
        { month: 'Nov', total: 250, active: 220, withdrawn: 18, expired: 12 }
      ],
      grievanceTrend: [
        { month: 'Jan', open: 3, resolved: 5 },
        { month: 'Feb', open: 4, resolved: 6 },
        { month: 'Mar', open: 2, resolved: 8 },
        { month: 'Apr', open: 5, resolved: 7 },
        { month: 'May', open: 3, resolved: 9 },
        { month: 'Jun', open: 4, resolved: 8 },
        { month: 'Jul', open: 6, resolved: 10 },
        { month: 'Aug', open: 3, resolved: 11 },
        { month: 'Sep', open: 5, resolved: 9 },
        { month: 'Oct', open: 4, resolved: 12 },
        { month: 'Nov', open: 7, resolved: 10 }
      ],
      
      // Additional stats
      totalGrievances: 127,
      openGrievances: 23,
      inProgressGrievances: 23,
      resolvedGrievances: 89,
      totalTemplates: 18,
      activeTemplates: 18,
      totalUsers: 45,
      activeUsers: 42
    }
  },

  // ROPA (Record of Processing Activities)
  ropaEntries: {
    status: 200,
    data: {
      searchList: [
        {
          ropaId: "ropa-1",
          processOverview: {
            businessFunction: "Human Resources",
            department: "HR Operations",
            processingActivityName: "Employee Recruitment",
            purposeForProcessing: "To hire and recruit qualified employees for the organization",
            processOwner: {
              name: "John Smith",
              mobile: "+1-555-0100",
              email: "john.smith@sandbox-demo.com"
            }
          },
          categoriesOfPersonalData: ["Name", "Email", "Phone Number", "Resume", "Work History"],
          categoriesOfSpecialNature: ["Health Information", "Background Check Results"],
          sourceOfPersonalData: ["Recruitment Agencies", "Direct Applications", "LinkedIn"],
          categoryOfIndividual: ["Employment Candidates", "Job Applicants"],
          activityReason: "Legitimate Interest - Employment",
          additionalCondition: "Explicit consent obtained from candidates",
          caseOrPurposeForExemption: "N/A",
          dpiaReference: "DPIA-HR-2024-001",
          status: "ACTIVE",
          createdAt: "2024-01-10T10:00:00Z"
        },
        {
          ropaId: "ropa-2",
          processOverview: {
            businessFunction: "Sales & Marketing",
            department: "Marketing",
            processingActivityName: "Customer Marketing Communications",
            purposeForProcessing: "To send promotional materials and product updates to customers",
            processOwner: {
              name: "Sarah Johnson",
              mobile: "+1-555-0200",
              email: "sarah.johnson@sandbox-demo.com"
            }
          },
          categoriesOfPersonalData: ["Email Address", "Name", "Phone Number", "Purchase History"],
          categoriesOfSpecialNature: [],
          sourceOfPersonalData: ["Website Forms", "Customer Registrations", "Purchase Records"],
          categoryOfIndividual: ["Customers", "Newsletter Subscribers"],
          activityReason: "Consent - Marketing",
          additionalCondition: "Right to withdraw consent at any time",
          caseOrPurposeForExemption: "N/A",
          dpiaReference: "DPIA-MKT-2024-002",
          status: "ACTIVE",
          createdAt: "2024-02-15T11:30:00Z"
        },
        {
          ropaId: "ropa-3",
          processOverview: {
            businessFunction: "Finance",
            department: "Accounts Payable",
            processingActivityName: "Vendor Payment Processing",
            purposeForProcessing: "To process payments to vendors and service providers",
            processOwner: {
              name: "Michael Chen",
              mobile: "+1-555-0300",
              email: "michael.chen@sandbox-demo.com"
            }
          },
          categoriesOfPersonalData: ["Vendor Name", "Bank Account Details", "Tax ID", "Contact Information"],
          categoriesOfSpecialNature: [],
          sourceOfPersonalData: ["Vendor Registration Forms", "Contracts", "Invoice Documents"],
          categoryOfIndividual: ["Vendors", "Contractors", "Service Providers"],
          activityReason: "Contractual Obligation",
          additionalCondition: "Compliance with financial regulations",
          caseOrPurposeForExemption: "N/A",
          dpiaReference: "DPIA-FIN-2024-003",
          status: "ACTIVE",
          createdAt: "2024-03-20T09:00:00Z"
        },
        {
          ropaId: "ropa-4",
          processOverview: {
            businessFunction: "Customer Support",
            department: "Service Desk",
            processingActivityName: "Customer Support Ticketing",
            purposeForProcessing: "To provide customer support and resolve service issues",
            processOwner: {
              name: "Emily Davis",
              mobile: "+1-555-0400",
              email: "emily.davis@sandbox-demo.com"
            }
          },
          categoriesOfPersonalData: ["Customer Name", "Email", "Phone", "Support Queries", "Product Information"],
          categoriesOfSpecialNature: [],
          sourceOfPersonalData: ["Support Portal", "Email", "Phone Calls", "Chat"],
          categoryOfIndividual: ["Customers", "Product Users"],
          activityReason: "Legitimate Interest - Customer Service",
          additionalCondition: "Data processed only for support resolution",
          caseOrPurposeForExemption: "N/A",
          dpiaReference: "DPIA-CS-2024-004",
          status: "ACTIVE",
          createdAt: "2024-04-10T14:30:00Z"
        },
        {
          ropaId: "ropa-5",
          processOverview: {
            businessFunction: "IT Security",
            department: "Information Security",
            processingActivityName: "Security Monitoring & Incident Response",
            purposeForProcessing: "To monitor systems for security threats and respond to incidents",
            processOwner: {
              name: "David Wilson",
              mobile: "+1-555-0500",
              email: "david.wilson@sandbox-demo.com"
            }
          },
          categoriesOfPersonalData: ["User IDs", "IP Addresses", "Access Logs", "System Activity"],
          categoriesOfSpecialNature: [],
          sourceOfPersonalData: ["System Logs", "Network Monitoring", "Security Tools"],
          categoryOfIndividual: ["Employees", "System Users", "Contractors"],
          activityReason: "Legitimate Interest - Security",
          additionalCondition: "Data retention per security policy",
          caseOrPurposeForExemption: "Legal Obligation - Security Standards",
          dpiaReference: "DPIA-SEC-2024-005",
          status: "ACTIVE",
          createdAt: "2024-05-05T10:15:00Z"
        }
      ]
    }
  },

  // Cookie Logs
  cookieLogs: {
    status: 200,
    data: {
      searchList: Array.from({ length: 20 }, (_, i) => ({
        logId: `cookie-log-${i + 1}`,
        userId: `user-${1000 + i}`,
        userEmail: `user${i + 1}@example.com`,
        cookiePreferences: {
          essential: true,
          analytics: i % 2 === 0,
          marketing: i % 3 === 0,
          functional: i % 4 === 0
        },
        ipAddress: `192.168.${Math.floor(Math.random() * 255)}.${Math.floor(Math.random() * 255)}`,
        userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        timestamp: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString()
      }))
    }
  },

  // Pending Requests
  pendingRequests: {
    status: 200,
    data: {
      searchList: Array.from({ length: 15 }, (_, i) => ({
        requestId: `req-${i + 1}`,
        requestType: ['CONSENT_UPDATE', 'CONSENT_WITHDRAWAL', 'DATA_UPDATE'][i % 3],
        userName: `User ${i + 1}`,
        userEmail: `user${i + 1}@example.com`,
        status: 'PENDING',
        requestDate: new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000).toISOString(),
        priority: ['LOW', 'MEDIUM', 'HIGH'][i % 3]
      }))
    }
  },

  // Generic success responses
  createSuccess: {
    status: 201,
    data: {
      message: "Created successfully",
      id: "generated-id-" + Date.now()
    }
  },

  updateSuccess: {
    status: 200,
    data: {
      message: "Updated successfully"
    }
  },

  deleteSuccess: {
    status: 200,
    data: {
      message: "Deleted successfully"
    }
  },

  // Data Breach Notifications
  dataBreachNotifications: {
    status: 200,
    data: {
      searchList: [
        {
          id: "breach-001",
          incidentId: "BR-1",
          status: "INVESTIGATION",
          incidentDetails: {
            discoveryDateTime: "2024-11-11T09:00:00Z",
            occurrenceDateTime: "2024-11-10T10:30:00Z",
            breachType: "UNAUTHORIZED_ACCESS",
            briefDescription: "Unauthorized access detected in the customer database. Immediate action taken to secure the system and prevent further unauthorized access.",
            affectedSystemOrService: ["Customer Database", "User Management System", "Authentication Service"]
          },
          dataInvolved: {
            personalDataCategories: ["Email", "Phone Number", "Full Name", "User ID"],
            affectedDataPrincipalsCount: 1500,
            dataEncryptedOrProtected: false,
            potentialImpactDescription: "High risk of identity theft and phishing attacks targeting affected customers. Personal contact information may be misused for unauthorized communications."
          },
          notificationDetails: {
            dpbiNotificationDate: "2024-11-12T10:00:00Z",
            dpbiAcknowledgementId: "DPBI-ACK-2024-001",
            dataPrincipalNotificationDate: "2024-11-13T08:30:00Z",
            channels: [
              { notificationChannel: "Email" },
              { notificationChannel: "SMS" },
              { notificationChannel: "In-App Notification" }
            ]
          },
          createdBy: "security@sandboxcms.com",
          createdAt: "2024-11-11T09:00:00Z",
          updatedAt: "2024-11-13T15:20:00Z"
        },
        {
          id: "breach-002",
          incidentId: "BR-2",
          status: "RESOLVED",
          incidentDetails: {
            discoveryDateTime: "2024-10-26T08:00:00Z",
            occurrenceDateTime: "2024-10-25T14:20:00Z",
            breachType: "PHISHING_ATTACK",
            briefDescription: "Targeted phishing attack on employee email accounts. All affected accounts have been secured and passwords reset. Enhanced security training implemented.",
            affectedSystemOrService: ["Employee Email System", "Internal Portal"]
          },
          dataInvolved: {
            personalDataCategories: ["Email", "Employee ID", "Department Information"],
            affectedDataPrincipalsCount: 50,
            dataEncryptedOrProtected: true,
            potentialImpactDescription: "Medium risk - Limited exposure of internal employee information. All affected credentials have been reset and accounts secured."
          },
          notificationDetails: {
            dpbiNotificationDate: "2024-10-27T09:00:00Z",
            dpbiAcknowledgementId: "DPBI-ACK-2024-002",
            dataPrincipalNotificationDate: "2024-10-27T10:00:00Z",
            channels: [
              { notificationChannel: "Email" },
              { notificationChannel: "Direct Communication" }
            ]
          },
          createdBy: "security@sandboxcms.com",
          createdAt: "2024-10-26T08:00:00Z",
          updatedAt: "2024-10-30T10:00:00Z"
        },
        {
          id: "breach-003",
          incidentId: "BR-3",
          status: "RESOLVED",
          incidentDetails: {
            discoveryDateTime: "2024-09-16T10:30:00Z",
            occurrenceDateTime: "2024-09-15T16:45:00Z",
            breachType: "API_MISCONFIGURATION",
            briefDescription: "API misconfiguration led to temporary data exposure through public endpoints. Issue was identified and fixed within 2 hours. Comprehensive security audit completed.",
            affectedSystemOrService: ["REST API", "Mobile App Backend", "Customer Portal API"]
          },
          dataInvolved: {
            personalDataCategories: ["Email", "Phone Number", "Address", "Date of Birth", "Account Status"],
            affectedDataPrincipalsCount: 3000,
            dataEncryptedOrProtected: false,
            potentialImpactDescription: "Critical risk - Extensive personal data was temporarily exposed. High potential for identity theft, fraud, and privacy violations. Comprehensive remediation measures implemented."
          },
          notificationDetails: {
            dpbiNotificationDate: "2024-09-17T08:00:00Z",
            dpbiAcknowledgementId: "DPBI-ACK-2024-003",
            dataPrincipalNotificationDate: "2024-09-18T09:00:00Z",
            channels: [
              { notificationChannel: "Email" },
              { notificationChannel: "SMS" },
              { notificationChannel: "In-App Notification" },
              { notificationChannel: "Official Website Notice" }
            ]
          },
          createdBy: "dpo@sandboxcms.com",
          createdAt: "2024-09-16T10:30:00Z",
          updatedAt: "2024-09-25T14:00:00Z"
        }
      ]
    }
  },

  // Data Compliance Report - Callbacks
  complianceCallbacks: {
    success: true,
    code: "JDNM0000",
    message: "Callback statistics retrieved successfully",
    data: {
      stats: {
        totalCallbacks: 800,
        successfulCallbacks: 760,
        failedCallbacks: 40,
        successPercentage: 95.0,
        failurePercentage: 5.0,
        byEventType: [
          {
            eventType: "CONSENT_CREATED",
            totalCallbacks: 250,
            successfulCallbacks: 245,
            failedCallbacks: 5,
            successPercentage: 98.0,
            failurePercentage: 2.0
          },
          {
            eventType: "CONSENT_WITHDRAWN",
            totalCallbacks: 200,
            successfulCallbacks: 190,
            failedCallbacks: 10,
            successPercentage: 95.0,
            failurePercentage: 5.0
          },
          {
            eventType: "CONSENT_EXPIRED",
            totalCallbacks: 180,
            successfulCallbacks: 170,
            failedCallbacks: 10,
            successPercentage: 94.4,
            failurePercentage: 5.6
          },
          {
            eventType: "GRIEVANCE_LODGED",
            totalCallbacks: 100,
            successfulCallbacks: 95,
            failedCallbacks: 5,
            successPercentage: 95.0,
            failurePercentage: 5.0
          },
          {
            eventType: "GRIEVANCE_RESOLVED",
            totalCallbacks: 70,
            successfulCallbacks: 60,
            failedCallbacks: 10,
            successPercentage: 85.7,
            failurePercentage: 14.3
          }
        ]
      },
      dataFiduciary: {
        "8ddc8cf9-7382-4125-83a0-8832bd029ac8": {
          name: "Jio Corporation",
          totalCallbacks: 450,
          successfulCallbacks: 430,
          failedCallbacks: 20,
          successPercentage: 95.6,
          failurePercentage: 4.4,
          byEventType: [
            {
              eventType: "CONSENT_CREATED",
              totalCallbacks: 150,
              successfulCallbacks: 148,
              failedCallbacks: 2,
              successPercentage: 98.7,
              failurePercentage: 1.3
            },
            {
              eventType: "CONSENT_WITHDRAWN",
              totalCallbacks: 120,
              successfulCallbacks: 115,
              failedCallbacks: 5,
              successPercentage: 95.8,
              failurePercentage: 4.2
            },
            {
              eventType: "CONSENT_EXPIRED",
              totalCallbacks: 100,
              successfulCallbacks: 95,
              failedCallbacks: 5,
              successPercentage: 95.0,
              failurePercentage: 5.0
            },
            {
              eventType: "GRIEVANCE_LODGED",
              totalCallbacks: 50,
              successfulCallbacks: 47,
              failedCallbacks: 3,
              successPercentage: 94.0,
              failurePercentage: 6.0
            },
            {
              eventType: "GRIEVANCE_RESOLVED",
              totalCallbacks: 30,
              successfulCallbacks: 25,
              failedCallbacks: 5,
              successPercentage: 83.3,
              failurePercentage: 16.7
            }
          ]
        },
        "9bbc9cf9-8493-5236-94b1-9943ce140bd9": {
          name: "Reliance Digital",
          totalCallbacks: 200,
          successfulCallbacks: 190,
          failedCallbacks: 10,
          successPercentage: 95.0,
          failurePercentage: 5.0,
          byEventType: [
            {
              eventType: "CONSENT_CREATED",
              totalCallbacks: 60,
              successfulCallbacks: 58,
              failedCallbacks: 2,
              successPercentage: 96.7,
              failurePercentage: 3.3
            },
            {
              eventType: "CONSENT_WITHDRAWN",
              totalCallbacks: 50,
              successfulCallbacks: 48,
              failedCallbacks: 2,
              successPercentage: 96.0,
              failurePercentage: 4.0
            },
            {
              eventType: "CONSENT_EXPIRED",
              totalCallbacks: 50,
              successfulCallbacks: 47,
              failedCallbacks: 3,
              successPercentage: 94.0,
              failurePercentage: 6.0
            },
            {
              eventType: "GRIEVANCE_LODGED",
              totalCallbacks: 25,
              successfulCallbacks: 23,
              failedCallbacks: 2,
              successPercentage: 92.0,
              failurePercentage: 8.0
            },
            {
              eventType: "GRIEVANCE_RESOLVED",
              totalCallbacks: 15,
              successfulCallbacks: 14,
              failedCallbacks: 1,
              successPercentage: 93.3,
              failurePercentage: 6.7
            }
          ]
        }
      },
      dataProcessor: {
        "7aac8bf8-6271-4014-72af-7721ac918a7": {
          name: "Analytics Processor Ltd",
          totalCallbacks: 150,
          successfulCallbacks: 140,
          failedCallbacks: 10,
          successPercentage: 93.3,
          failurePercentage: 6.7,
          byEventType: [
            {
              eventType: "CONSENT_CREATED",
              totalCallbacks: 40,
              successfulCallbacks: 39,
              failedCallbacks: 1,
              successPercentage: 97.5,
              failurePercentage: 2.5
            },
            {
              eventType: "CONSENT_WITHDRAWN",
              totalCallbacks: 30,
              successfulCallbacks: 27,
              failedCallbacks: 3,
              successPercentage: 90.0,
              failurePercentage: 10.0
            },
            {
              eventType: "CONSENT_EXPIRED",
              totalCallbacks: 30,
              successfulCallbacks: 28,
              failedCallbacks: 2,
              successPercentage: 93.3,
              failurePercentage: 6.7
            },
            {
              eventType: "GRIEVANCE_LODGED",
              totalCallbacks: 25,
              successfulCallbacks: 25,
              failedCallbacks: 0,
              successPercentage: 100.0,
              failurePercentage: 0.0
            },
            {
              eventType: "GRIEVANCE_RESOLVED",
              totalCallbacks: 25,
              successfulCallbacks: 21,
              failedCallbacks: 4,
              successPercentage: 84.0,
              failurePercentage: 16.0
            }
          ]
        }
      }
    },
    timestamp: "2025-11-19T19:38:42.456Z",
    path: "/notification/v1/notifications/callbacks/stats",
    error: false,
    transactionId: "d0040682-6a34-413e-9b17-8cc6ef62ebb6"
  },

  // Data Compliance Report - Data Deletion/Purge
  complianceDeletions: {
    success: true,
    code: "JDNM0000",
    message: "Callback purge statistics retrieved successfully",
    data: {
      stats: {
        totalEvents: 56,
        purgedEvents: 52,
        pendingEvents: 4,
        overdueEvents: 4,
        purgePercentage: 92.9,
        pendingPercentage: 7.1,
        overduePercentage: 7.1,
        byEventType: [
          {
            eventType: "CONSENT_WITHDRAWN",
            totalEvents: 30,
            purgedEvents: 28,
            pendingEvents: 2,
            overdueEvents: 2
          },
          {
            eventType: "CONSENT_EXPIRED",
            totalEvents: 26,
            purgedEvents: 24,
            pendingEvents: 2,
            overdueEvents: 2
          }
        ]
      },
      dataFiduciary: {
        "8ddc8cf9-7382-4125-83a0-8832bd029ac8": {
          name: "Jio Corporation",
          totalEvents: 25,
          purgedEvents: 25,
          pendingEvents: 0,
          overdueEvents: 0,
          byEventType: [
            {
              eventType: "CONSENT_WITHDRAWN",
              consentCount: 10,
              dateOfEvent: "01 Nov 2025",
              slaDays: 7,
              piiDataType: "Email + Contact No",
              totalRecords: 10,
              recordsPurged: 10,
              pendingRecords: 0,
              daysOverdue: 0,
              status: "Completed"
            },
            {
              eventType: "CONSENT_WITHDRAWN",
              consentCount: 15,
              dateOfEvent: "05 Nov 2025",
              slaDays: 7,
              piiDataType: "Purchase History",
              totalRecords: 15,
              recordsPurged: 15,
              pendingRecords: 0,
              daysOverdue: 0,
              status: "Completed"
            }
          ]
        },
        "9bbc9cf9-8493-5236-94b1-9943ce140bd9": {
          name: "Reliance Digital",
          totalEvents: 13,
          purgedEvents: 11,
          pendingEvents: 2,
          overdueEvents: 2,
          byEventType: [
            {
              eventType: "CONSENT_WITHDRAWN",
              consentCount: 5,
              dateOfEvent: "03 Nov 2025",
              slaDays: 7,
              piiDataType: "Web Behavior Logs",
              totalRecords: 5,
              recordsPurged: 4,
              pendingRecords: 1,
              daysOverdue: 3,
              status: "Overdue"
            },
            {
              eventType: "CONSENT_EXPIRED",
              consentCount: 8,
              dateOfEvent: "28 Oct 2025",
              slaDays: 7,
              piiDataType: "User Profile Records",
              totalRecords: 8,
              recordsPurged: 7,
              pendingRecords: 1,
              daysOverdue: 6,
              status: "Overdue"
            }
          ]
        }
      },
      dataProcessor: {
        "7aac8bf8-6271-4014-72af-7721ac918a7": {
          name: "Analytics Processor Ltd",
          totalEvents: 18,
          purgedEvents: 16,
          pendingEvents: 2,
          overdueEvents: 2,
          byEventType: [
            {
              eventType: "CONSENT_EXPIRED",
              consentCount: 12,
              dateOfEvent: "02 Nov 2025",
              slaDays: 7,
              piiDataType: "Transaction + Location Data",
              totalRecords: 12,
              recordsPurged: 11,
              pendingRecords: 1,
              daysOverdue: 4,
              status: "Overdue"
            },
            {
              eventType: "CONSENT_EXPIRED",
              consentCount: 6,
              dateOfEvent: "30 Oct 2025",
              slaDays: 7,
              piiDataType: "Device Information",
              totalRecords: 6,
              recordsPurged: 5,
              pendingRecords: 1,
              daysOverdue: 5,
              status: "Overdue"
            }
          ]
        }
      }
    },
    timestamp: "2025-11-19T20:30:26.986Z",
    path: "/notification/v1/notifications/callbacks/purge-stats",
    error: false,
    transactionId: "ecec7668-d661-4fe7-a4f3-929e5f1beeb8"
  },

  // Cookie Templates for RegisteredCookies
  cookieTemplates: {
    status: 200,
    data: [
      {
        scanId: "scan-1",
        templateId: "cookie-tpl-1",
        templateName: "https://sandbox.jio.com",
        url: "https://sandbox.jio.com",
        domain: "sandbox.jio.com",
        status: "ACTIVE",
        templateStatus: "PUBLISHED",
        version: "1.0",
        createdAt: "2024-01-15T10:00:00Z",
        updatedAt: new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString(), // 7 days ago
        documentMeta: {
          name: "Cookie Policy v1.0.pdf",
          size: "245KB",
          uploadedAt: "2024-01-15T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-1",
            mandatory: true,
            cookies: [
              {
                cookieName: "_session",
                cookieId: "cook-1",
                duration: "Session",
                expiry: "Session",
                purpose: "Manages user session and authentication state",
                domain: "sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_csrf",
                cookieId: "cook-2",
                duration: "Session",
                expiry: "Session",
                purpose: "Cross-site request forgery protection token",
                domain: "sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "AUTH_TOKEN",
                cookieId: "cook-3",
                duration: "24 hours",
                expiry: "1 day",
                purpose: "User authentication token",
                domain: "sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Analytics",
            preferenceId: "pref-analytics-1",
            mandatory: false,
            cookies: [
              {
                cookieName: "_ga",
                cookieId: "cook-4",
                duration: "2 years",
                expiry: "730 days",
                purpose: "Google Analytics - Distinguishes unique users",
                domain: ".jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_gid",
                cookieId: "cook-5",
                duration: "24 hours",
                expiry: "1 day",
                purpose: "Google Analytics - Distinguishes users",
                domain: ".jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_gat",
                cookieId: "cook-6",
                duration: "1 minute",
                expiry: "1 minute",
                purpose: "Google Analytics - Throttles request rate",
                domain: ".jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Marketing",
            preferenceId: "pref-marketing-1",
            mandatory: false,
            cookies: [
              {
                cookieName: "_fbp",
                cookieId: "cook-7",
                duration: "3 months",
                expiry: "90 days",
                purpose: "Facebook Pixel - Tracks conversions and user behavior",
                domain: ".jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_gcl_au",
                cookieId: "cook-8",
                duration: "3 months",
                expiry: "90 days",
                purpose: "Google AdSense - Stores conversion data",
                domain: ".jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-2",
        templateId: "cookie-tpl-2",
        templateName: "https://api.sandbox.jio.com",
        url: "https://api.sandbox.jio.com",
        domain: "api.sandbox.jio.com",
        status: "ACTIVE",
        templateStatus: "PUBLISHED",
        version: "1.0",
        createdAt: "2024-02-20T10:00:00Z",
        updatedAt: new Date(Date.now() - 14 * 24 * 60 * 60 * 1000).toISOString(), // 14 days ago
        documentMeta: {
          name: "API Cookie Policy.pdf",
          size: "128KB",
          uploadedAt: "2024-02-20T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-2",
            mandatory: true,
            cookies: [
              {
                cookieName: "_token",
                cookieId: "cook-api-1",
                duration: "Session",
                expiry: "Session",
                purpose: "API authentication and session management",
                domain: "api.sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_security",
                cookieId: "cook-api-2",
                duration: "Session",
                expiry: "Session",
                purpose: "Security headers and CORS validation",
                domain: "api.sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "API_KEY",
                cookieId: "cook-api-3",
                duration: "7 days",
                expiry: "7 days",
                purpose: "API key validation",
                domain: "api.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-3",
        templateId: "cookie-tpl-3",
        templateName: "https://www.sandbox.jio.com",
        url: "https://www.sandbox.jio.com",
        domain: "www.sandbox.jio.com",
        status: "DRAFT",
        templateStatus: "DRAFT",
        version: "2.0",
        createdAt: "2024-10-01T10:00:00Z",
        updatedAt: new Date(Date.now() - 2 * 24 * 60 * 60 * 1000).toISOString(),
        documentMeta: {
          name: "Cookie Policy Draft v2.0.pdf",
          size: "312KB",
          uploadedAt: "2024-10-01T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-3",
            mandatory: true,
            cookies: [
              {
                cookieName: "_secure_session",
                cookieId: "cook-w-1",
                duration: "Session",
                expiry: "Session",
                purpose: "Secure session management",
                domain: "www.sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_xsrf",
                cookieId: "cook-w-2",
                duration: "Session",
                expiry: "Session",
                purpose: "CSRF protection",
                domain: "www.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Analytics",
            preferenceId: "pref-analytics-3",
            mandatory: false,
            cookies: [
              {
                cookieName: "_ga",
                cookieId: "cook-w-3",
                duration: "2 years",
                expiry: "730 days",
                purpose: "Google Analytics tracking",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_gid",
                cookieId: "cook-w-4",
                duration: "24 hours",
                expiry: "1 day",
                purpose: "Google Analytics user ID",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "mixpanel",
                cookieId: "cook-w-5",
                duration: "1 year",
                expiry: "365 days",
                purpose: "Mixpanel analytics",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Marketing",
            preferenceId: "pref-marketing-3",
            mandatory: false,
            cookies: [
              {
                cookieName: "_fbp",
                cookieId: "cook-w-6",
                duration: "3 months",
                expiry: "90 days",
                purpose: "Facebook Pixel",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "_gcl_au",
                cookieId: "cook-w-7",
                duration: "3 months",
                expiry: "90 days",
                purpose: "Google AdSense",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Preferences",
            preferenceId: "pref-prefs-3",
            mandatory: false,
            cookies: [
              {
                cookieName: "theme",
                cookieId: "cook-w-8",
                duration: "1 year",
                expiry: "365 days",
                purpose: "User interface theme preference",
                domain: "www.sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "language",
                cookieId: "cook-w-9",
                duration: "1 year",
                expiry: "365 days",
                purpose: "User language preference",
                domain: "www.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-4",
        templateId: "cookie-tpl-4",
        templateName: "https://app.sandbox.jio.com",
        url: "https://app.sandbox.jio.com",
        domain: "app.sandbox.jio.com",
        status: "INACTIVE",
        templateStatus: "INACTIVE",
        version: "1.0",
        createdAt: "2023-12-10T10:00:00Z",
        updatedAt: new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString(),
        documentMeta: {
          name: "App Cookie Policy.pdf",
          size: "156KB",
          uploadedAt: "2023-12-10T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-4",
            mandatory: true,
            cookies: [
              {
                cookieName: "app_session",
                cookieId: "cook-app-1",
                duration: "Session",
                expiry: "Session",
                purpose: "Application session tracking",
                domain: "app.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-5",
        templateId: "cookie-tpl-5",
        templateName: "https://portal.sandbox.jio.com",
        url: "https://portal.sandbox.jio.com",
        domain: "portal.sandbox.jio.com",
        status: "ACTIVE",
        templateStatus: "PUBLISHED",
        version: "1.0",
        createdAt: "2024-03-10T10:00:00Z",
        updatedAt: new Date(Date.now() - 5 * 24 * 60 * 60 * 1000).toISOString(), // 5 days ago
        documentMeta: {
          name: "Portal Cookie Policy.pdf",
          size: "189KB",
          uploadedAt: "2024-03-10T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-5",
            mandatory: true,
            cookies: [
              {
                cookieName: "portal_session",
                cookieId: "cook-portal-1",
                duration: "Session",
                expiry: "Session",
                purpose: "Portal session management",
                domain: "portal.sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "user_pref",
                cookieId: "cook-portal-2",
                duration: "30 days",
                expiry: "30 days",
                purpose: "User preferences storage",
                domain: "portal.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Analytics",
            preferenceId: "pref-analytics-5",
            mandatory: false,
            cookies: [
              {
                cookieName: "_ga",
                cookieId: "cook-portal-3",
                duration: "2 years",
                expiry: "730 days",
                purpose: "Google Analytics",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-6",
        templateId: "cookie-tpl-6",
        templateName: "https://store.sandbox.jio.com",
        url: "https://store.sandbox.jio.com",
        domain: "store.sandbox.jio.com",
        status: "ACTIVE",
        templateStatus: "PUBLISHED",
        version: "1.0",
        createdAt: "2024-04-05T10:00:00Z",
        updatedAt: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000).toISOString(), // 10 days ago
        documentMeta: {
          name: "Store Cookie Policy.pdf",
          size: "267KB",
          uploadedAt: "2024-04-05T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-6",
            mandatory: true,
            cookies: [
              {
                cookieName: "cart_id",
                cookieId: "cook-store-1",
                duration: "7 days",
                expiry: "7 days",
                purpose: "Shopping cart identifier",
                domain: "store.sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "checkout_session",
                cookieId: "cook-store-2",
                duration: "Session",
                expiry: "Session",
                purpose: "Checkout session tracking",
                domain: "store.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Marketing",
            preferenceId: "pref-marketing-6",
            mandatory: false,
            cookies: [
              {
                cookieName: "_fbp",
                cookieId: "cook-store-3",
                duration: "3 months",
                expiry: "90 days",
                purpose: "Facebook Pixel",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "affiliate_id",
                cookieId: "cook-store-4",
                duration: "30 days",
                expiry: "30 days",
                purpose: "Affiliate tracking",
                domain: "store.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-7",
        templateId: "cookie-tpl-7",
        templateName: "https://support.sandbox.jio.com",
        url: "https://support.sandbox.jio.com",
        domain: "support.sandbox.jio.com",
        status: "DRAFT",
        templateStatus: "DRAFT",
        version: "1.0",
        createdAt: "2024-05-12T10:00:00Z",
        updatedAt: new Date(Date.now() - 1 * 24 * 60 * 60 * 1000).toISOString(), // 1 day ago
        documentMeta: {
          name: "Support Cookie Policy.pdf",
          size: "198KB",
          uploadedAt: "2024-05-12T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-7",
            mandatory: true,
            cookies: [
              {
                cookieName: "support_ticket",
                cookieId: "cook-support-1",
                duration: "Session",
                expiry: "Session",
                purpose: "Support ticket tracking",
                domain: "support.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      },
      {
        scanId: "scan-8",
        templateId: "cookie-tpl-8",
        templateName: "https://blog.sandbox.jio.com",
        url: "https://blog.sandbox.jio.com",
        domain: "blog.sandbox.jio.com",
        status: "ACTIVE",
        templateStatus: "PUBLISHED",
        version: "1.0",
        createdAt: "2024-06-20T10:00:00Z",
        updatedAt: new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString(), // 3 days ago
        documentMeta: {
          name: "Blog Cookie Policy.pdf",
          size: "145KB",
          uploadedAt: "2024-06-20T10:00:00Z"
        },
        preferencesWithCookies: [
          {
            preferenceName: "Strictly Necessary",
            preferenceId: "pref-necessary-8",
            mandatory: true,
            cookies: [
              {
                cookieName: "reading_pref",
                cookieId: "cook-blog-1",
                duration: "1 year",
                expiry: "365 days",
                purpose: "Reading preferences",
                domain: "blog.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          },
          {
            preferenceName: "Analytics",
            preferenceId: "pref-analytics-8",
            mandatory: false,
            cookies: [
              {
                cookieName: "_ga",
                cookieId: "cook-blog-2",
                duration: "2 years",
                expiry: "730 days",
                purpose: "Google Analytics",
                domain: ".sandbox.jio.com",
                type: "HTTP"
              },
              {
                cookieName: "page_views",
                cookieId: "cook-blog-3",
                duration: "1 year",
                expiry: "365 days",
                purpose: "Page view tracking",
                domain: "blog.sandbox.jio.com",
                type: "HTTP"
              }
            ]
          }
        ]
      }
    ]
  },

  // Cookie Scan Results
  cookieScanResult: {
    status: 200,
    data: {
      scanId: "scan-new-123",
      domain: "example.com",
      totalCookies: 12,
      categories: {
        necessary: 4,
        analytics: 5,
        marketing: 3
      },
      message: "Scan completed successfully"
    }
  }
};

// Helper function to get mock data based on URL pattern
export const getMockDataByUrl = (url, method = 'GET', body = {}) => {
  // Authentication
  if (url.includes('login/init-otp')) {
    return mockData.loginInitOtp;
  }
  if (url.includes('login/validate-otp')) {
    return mockData.loginValidateOtp;
  }
  if (url.includes('users/profile')) {
    return mockData.userProfile;
  }

  // Specific endpoint checks - MUST come before generic Business check to avoid matching businessId query param

  // Master Data
  if (url.includes('purpose') && url.includes('search')) {
    return mockData.purposes;
  }
  if (url.includes('data-type') && url.includes('search')) {
    return mockData.dataTypes;
  }
  if (url.includes('data-type') && url.includes('create')) {
    return mockData.createSuccess;
  }
  if (url.includes('data-type') && url.includes('update')) {
    return mockData.updateSuccess;
  }
  // Check processor-activity BEFORE processor (more specific first)
  if (url.includes('processor-activity') && url.includes('search') || url.includes('processing-activity') && url.includes('search')) {
    return mockData.processingActivities;
  }
  if (url.includes('data-processor') && url.includes('search') || url.includes('processor') && url.includes('search')) {
    return mockData.processors;
  }

  // Templates
  if (url.includes('template') && url.includes('search') && !url.includes('grievance') && !url.includes('email') && !url.includes('sms') && !url.includes('cookie')) {
    // Check if it's fetching a specific template by ID
    const templateIdMatch = url.match(/templateId=([^&]+)/);
    if (templateIdMatch) {
      const templateId = decodeURIComponent(templateIdMatch[1]);
      return mockData.templateDetails;
    }
    return mockData.templates;
  }
  if (url.includes('templates/') && !url.includes('search') && !url.includes('grievance') && !url.includes('email') && !url.includes('sms') && !url.includes('cookie')) {
    // Template detail by ID in URL path
    return mockData.templateDetails;
  }
  if (url.includes('template') && url.includes('details') && !url.includes('cookie')) {
    return mockData.templateDetails;
  }

  // Consent Logs - search consents by template ID
  if (url.includes('consent') && url.includes('search') && url.includes('templateId')) {
    return mockData.consentLogs;
  }

  // Consent Counts - status counts by template ID
  if (url.includes('consent') && url.includes('count-status-by-params') && url.includes('templateId')) {
    return mockData.consentCounts;
  }

  if (url.includes('grievance') && url.includes('template')) {
    return mockData.grievanceTemplates;
  }
  
  // Notification templates - check for notification/templates endpoint
  // Check for /v1/templates first (most specific)
  if (url.includes('/v1/templates') || url.includes('v1/templates')) {
    // Handle notification templates (could be email or SMS, consent or grievance)
    const isEmail = url.includes('channel=EMAIL') || url.includes('channel=email') || (url.toLowerCase().includes('email') && !url.toLowerCase().includes('sms'));
    const isSMS = url.includes('channel=SMS') || url.includes('channel=sms') || (url.toLowerCase().includes('sms') && !url.toLowerCase().includes('email'));
    
    console.log('🔍 Notification templates URL match:', { url, isEmail, isSMS });
    
    // Return appropriate templates based on channel
    if (isSMS) {
      console.log('📱 Returning SMS templates:', mockData.smsTemplates?.data?.data?.length || 0);
      return mockData.smsTemplates;
    } else if (isEmail) {
      console.log('📧 Returning Email templates:', mockData.emailTemplates?.data?.data?.length || 0);
      return mockData.emailTemplates;
    } else {
      // Default to email templates if channel not specified
      console.log('📧 Returning default Email templates:', mockData.emailTemplates?.data?.data?.length || 0);
      return mockData.emailTemplates;
    }
  }
  
  // Also check for notification/templates pattern (for relative URLs)
  if ((url.includes('notification') && url.includes('template')) || url.includes('notification/templates')) {
    // Handle notification templates (could be email or SMS, consent or grievance)
    const isEmail = url.includes('channel=EMAIL') || url.includes('channel=email') || (url.toLowerCase().includes('email') && !url.toLowerCase().includes('sms'));
    const isSMS = url.includes('channel=SMS') || url.includes('channel=sms') || (url.toLowerCase().includes('sms') && !url.toLowerCase().includes('email'));
    
    console.log('🔍 Notification templates URL match (relative):', { url, isEmail, isSMS });
    
    // Return appropriate templates based on channel
    if (isSMS) {
      console.log('📱 Returning SMS templates:', mockData.smsTemplates?.data?.data?.length || 0);
      return mockData.smsTemplates;
    } else if (isEmail) {
      console.log('📧 Returning Email templates:', mockData.emailTemplates?.data?.data?.length || 0);
      return mockData.emailTemplates;
    } else {
      // Default to email templates if channel not specified
      console.log('📧 Returning default Email templates:', mockData.emailTemplates?.data?.data?.length || 0);
      return mockData.emailTemplates;
    }
  }
  if (url.includes('email') && url.includes('template') && !url.includes('notification')) {
    return mockData.emailTemplates;
  }
  if (url.includes('sms') && url.includes('template') && !url.includes('notification')) {
    return mockData.smsTemplates;
  }

  // Configuration & Specific Endpoints - MUST come BEFORE Business checks to avoid false matches on businessId query params
  // Retention Policies - check for search, create, update endpoints
  if (url.includes('retention')) {
    // For search endpoint, return existing policy
    if (url.includes('search')) {
      return mockData.retentionPolicies;
    }
    // For create/update endpoints, return success with retentionId
    if (url.includes('create') || url.includes('update') || (method === 'POST' || method === 'PUT')) {
      return {
        status: 200,
        data: {
          ...mockData.retentionPolicies.data,
          businessId: body?.businessId || 'sandbox-business-id',
          message: 'Retention policy saved successfully'
        }
      };
    }
    return mockData.retentionPolicies;
  }
  if (url.includes('data-protection-officer')) {
    return mockData.dpoDetails;
  }
  if (url.includes('system-config') || url.includes('system-configuration')) {
    return mockData.systemConfig;
  }
  if (url.includes('consent') && url.includes('config') && !url.includes('grievance')) {
    return mockData.consentConfiguration;
  }
  if (url.includes('grievance') && url.includes('config')) {
    return mockData.grievanceConfiguration;
  }
  // Notification endpoints
  if (url.includes('notification') && url.includes('search') && method === 'GET') {
    return mockData.notificationConfiguration;
  }
  if (url.includes('notification') && url.includes('create') && method === 'POST') {
    return {
      status: 200,
      data: {
        configId: `notif-config-${Date.now()}`,
        businessId: body?.businessId || 'sandbox-business-id',
        message: 'Notification configuration created successfully'
      }
    };
  }
  if (url.includes('notification') && url.includes('update') && method === 'PUT') {
    return {
      status: 200,
      data: {
        configId: body?.configId || 'notif-config-1',
        message: 'Notification configuration updated successfully'
      }
    };
  }
  if (url.includes('notification') && url.includes('config')) {
    return mockData.notificationConfiguration;
  }
  
  // SMTP endpoints
  if (url.includes('smtp') && url.includes('search') && method === 'GET') {
    return mockData.smtpConfiguration;
  }
  if (url.includes('smtp') && url.includes('create') && method === 'POST') {
    return {
      status: 200,
      data: {
        configId: `smtp-config-${Date.now()}`,
        businessId: body?.businessId || 'sandbox-business-id',
        message: 'SMTP configuration created successfully'
      }
    };
  }
  if (url.includes('smtp') && url.includes('update') && method === 'PUT') {
    return {
      status: 200,
      data: {
        configId: body?.configId || 'smtp-config-1',
        message: 'SMTP configuration updated successfully'
      }
    };
  }
  if (url.includes('digilocker')) {
    return mockData.digilockerConfiguration;
  }

  // Business - Keep at end since it's generic and can match businessId query params
  // Business Application - create endpoint
  if (url.includes('business-application/create') && method === 'POST') {
    return {
      status: 200,
      data: {
        businessId: `sandbox-business-id-${Date.now()}`,
        name: body?.name || 'New Business Group',
        description: body?.description || 'Business group description',
        createdAt: new Date().toISOString(),
        message: 'Business group created successfully'
      }
    };
  }
  
  if (url.includes('business-application/search')) {
    return mockData.businessApplications;
  }
  if (url.includes('business') && url.includes('search')) {
    return mockData.businessApplications;
  }
  
  // Roles endpoints
  if (url.includes('/role/list') && method === 'GET') {
    return mockData.roles;
  }
  if (url.includes('/role/search') && method === 'GET') {
    // If roleId is in query params, return specific role
    const roleIdMatch = url.match(/roleId=([^&]+)/);
    if (roleIdMatch) {
      const roleId = roleIdMatch[1];
      const role = mockData.roles.data.find(r => r.roleId === roleId);
      if (role) {
        return { status: 200, data: role };
      }
    }
    return mockData.roles;
  }
  if (url.includes('/role/create') && method === 'POST') {
    return {
      status: 200,
      data: {
        roleId: `role-${Date.now()}`,
        role: body?.role || 'New Role',
        description: body?.description || 'Role description',
        permissions: body?.permissions || [],
        message: 'Role created successfully'
      }
    };
  }
  if (url.includes('/role/delete') && method === 'DELETE') {
    return { status: 200, data: { message: 'Role deleted successfully' } };
  }
  
  // Users endpoints
  if (url.includes('/users/list') && method === 'GET') {
    return mockData.users;
  }
  if (url.includes('/users/search') && method === 'GET') {
    // If userId is in query params, return specific user
    const userIdMatch = url.match(/userId=([^&]+)/);
    if (userIdMatch) {
      const userId = userIdMatch[1];
      const user = mockData.users.data.find(u => u.userId === userId);
      if (user) {
        return { status: 200, data: user };
      }
    }
    return mockData.users;
  }
  if (url.includes('/users/create') && method === 'POST') {
    return {
      status: 200,
      data: {
        userId: `user-${Date.now()}`,
        username: body?.name || 'New User',
        email: body?.email || 'user@sandboxcms.com',
        mobile: body?.mobile || '',
        designation: body?.designation || 'User',
        roles: body?.roles || [],
        message: 'User created successfully'
      }
    };
  }
  if (url.includes('/users/delete') && method === 'DELETE') {
    return { status: 200, data: { message: 'User deleted successfully' } };
  }

  // Consents - generic search (only if not already matched by template-specific above)
  if (url.includes('consent') && url.includes('search') && !url.includes('templateId')) {
    return mockData.consents;
  }
  if (url.includes('consent-handle')) {
    return mockData.consentHandleDetails;
  }
  if (url.includes('consent-report')) {
    return mockData.consentReport;
  }

  // Data Compliance Report
  if (url.includes('compliance/callbacks')) {
    return mockData.complianceCallbacks;
  }
  if (url.includes('compliance/deletions') || url.includes('purge-stats')) {
    return mockData.complianceDeletions;
  }

  // Grievances
  // Grievance Templates - create/update
  if (url.includes('grievance-templates') && (method === 'POST' || method === 'PUT')) {
    // Create or update grievance template
    const templateId = body?.grievanceTemplateId || `grv-template-${Date.now()}`;
    return {
      status: 200,
      data: {
        grievanceTemplateId: templateId,
        grievanceTemplateName: body?.grievanceTemplateName || 'New Grievance Template',
        version: body?.version || '1.0',
        status: body?.status || 'DRAFT',
        businessId: body?.businessId || 'sandbox-business-id',
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        message: method === 'POST' ? 'Grievance template created successfully' : 'Grievance template updated successfully'
      }
    };
  }
  if (url.includes('grievance-templates') && url.match(/\/grv-template-\d+/) && method === 'GET') {
    // getGrievnaceTemplateData - returns template details for editing
    return mockData.grievanceTemplateDetails;
  }
  if (url.includes('grievance') && url.match(/\/grv-template-\d+/) && method === 'GET') {
    // getGrievanceByTemplateId - returns grievances for a specific template
    return mockData.grievances;
  }
  // Grievance Details by ID - must come before generic grievance search
  if (url.includes('/api/v1/grievances/') && method === 'GET') {
    // Extract grievance ID from URL
    const grievanceIdMatch = url.match(/\/api\/v1\/grievances\/([^/?]+)/);
    if (grievanceIdMatch) {
      const grievanceId = decodeURIComponent(grievanceIdMatch[1]);
      // Find the grievance in the mock data
      const grievances = Array.isArray(mockData.grievances.data) 
        ? mockData.grievances.data 
        : mockData.grievances.data?.searchList || [];
      
      const grievance = grievances.find(g => g.grievanceId === grievanceId);
      
      if (grievance) {
        // Return detailed grievance with all required fields
        return {
          status: 200,
          data: {
            ...grievance,
            grievanceDescription: grievance.description || grievance.grievanceDescription || 'Grievance description',
            grievanceDetail: grievance.grievanceDetail || grievance.grievanceType || 'N/A',
            supportingDocs: grievance.attachments?.map((att, idx) => ({
              name: att,
              url: `https://sandbox.example.com/attachments/${grievanceId}/${att}`,
              size: '100KB',
              uploadedAt: grievance.createdAt
            })) || [],
            history: grievance.status === 'RESOLVED' ? [
              {
                newStatus: 'INPROCESS',
                updatedAt: new Date(new Date(grievance.createdAt).getTime() + 24 * 60 * 60 * 1000).toISOString(),
                updatedBy: 'DPO Officer',
                remarks: 'Request assigned for review'
              },
              {
                newStatus: 'RESOLVED',
                updatedAt: grievance.resolvedAt || grievance.updatedAt,
                updatedBy: 'DPO Officer',
                remarks: grievance.remarks || 'Issue resolved successfully'
              }
            ] : grievance.status === 'INPROCESS' ? [
              {
                newStatus: 'INPROCESS',
                updatedAt: new Date(new Date(grievance.createdAt).getTime() + 24 * 60 * 60 * 1000).toISOString(),
                updatedBy: 'DPO Officer',
                remarks: 'Request assigned for review'
              }
            ] : []
          }
        };
      }
      
      // If not found, return a default grievance
      return {
        status: 200,
        data: {
          grievanceId: grievanceId,
          grievanceType: 'DATA_ACCESS',
          grievanceDescription: 'Grievance request details',
          grievanceDetail: 'DATA_ACCESS',
          status: 'NEW',
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          userDetails: {
            Name: 'John Doe',
            'Mobile Number': '+91 9876543210',
            'Email address': 'john.doe@example.com'
          },
          supportingDocs: [],
          history: []
        }
      };
    }
  }
  
  if (url.includes('grievance') && (url.includes('search') || url.includes('/api/v1/grievances')) && method === 'GET') {
    return mockData.grievances;
  }
  // Grievance Types - must come before generic grievance check
  if ((url.includes('grievance-type') || url.includes('grievance-types')) && method === 'GET') {
    return mockData.grievanceTypes;
  }

  // Users & Roles
  if (url.includes('users') && url.includes('profile')) {
    return mockData.userProfile;
  }
  if (url.includes('role') && (url.includes('list') || url.includes('search'))) {
    return mockData.roles;
  }
  // User Types - must come before generic user check to avoid false matches
  if (url.includes('user-type') || url.includes('user-types')) {
    if (method === 'GET') {
      return mockData.userTypes;
    }
  }
  // User Details - must come before generic user check to avoid false matches
  if (url.includes('user-detail') || url.includes('user-details')) {
    if (method === 'GET') {
      return mockData.userDetails;
    }
  }
  if (url.includes('user') && (url.includes('list') || url.includes('search')) && !url.includes('user-type') && !url.includes('user-detail')) {
    return mockData.users;
  }

  // Cookies - cookie templates
  if (url.includes('cookie-templates') || url.includes('cookie/templates')) {
    // Check if scanId is in the URL - if so, return detailed data for that scan
    const scanIdMatch = url.match(/scanId=([^&]+)/);
    if (scanIdMatch) {
      const scanId = decodeURIComponent(scanIdMatch[1]);

      // Find the template with this scanId
      const template = mockData.cookieTemplates.data.find(t => t.scanId === scanId);
      if (template) {
        return {
          status: 200,
          data: {
            ...template,
            // Add more detailed information for the detail view
            cookiesDetails: template.preferencesWithCookies || template.categories || [],
            multilingual: {
              languageSpecificContentMap: {
                ENGLISH: {
                  label: "Cookie Consent",
                  description: "We use cookies to enhance your browsing experience.",
                  acceptAll: "Accept All",
                  rejectAll: "Reject All",
                  savePreferences: "Save Preferences"
                }
              }
            }
          }
        };
      }
      // If scanId not found, return empty
      return { status: 200, data: {} };
    }

    return mockData.cookieTemplates;
  }
  if (url.includes('cookie') && url.includes('scan')) {
    return mockData.cookieScanResult;
  }
  // Cookie Categories - check for /category endpoint
  if (url.includes('/category') && !url.includes('data-type') && !url.includes('purpose')) {
    // For GET requests, return the list of categories
    if (method === 'GET') {
      return mockData.cookieCategories;
    }
    // For POST requests (create), return success with the new category
    if (method === 'POST') {
      return {
        status: 200,
        data: {
          categoryId: `cat-${Date.now()}`,
          category: body?.category || 'New Category',
          description: body?.description || 'Category description',
          message: 'Category created successfully'
        }
      };
    }
    // For PUT requests (update), return success with updated category
    if (method === 'PUT') {
      return {
        status: 200,
        data: {
          categoryId: body?.categoryId || 'cat-1',
          category: body?.category || 'Updated Category',
          description: body?.description || 'Updated description',
          message: 'Category updated successfully'
        }
      };
    }
    return mockData.cookieCategories;
  }
  if (url.includes('cookie-category')) {
    return mockData.cookieCategories;
  }
  if (url.includes('cookie') && url.includes('log')) {
    return mockData.cookieLogs;
  }

  // Data Breach
  if (url.includes('data-breach')) {
    // Check if requesting a specific breach by ID (e.g., /data-breach/BR-1)
    const breachIdMatch = url.match(/data-breach\/([^?&/]+)/);
    if (breachIdMatch) {
      const breachId = breachIdMatch[1];
      // Find the specific breach
      const breach = mockData.dataBreachNotifications.data.searchList.find(
        b => b.incidentId === breachId || b.id === breachId
      );
      if (breach) {
        return {
          status: 200,
          data: breach
        };
      }
      // If breach not found, return 404
      return {
        status: 404,
        data: { message: `Breach with ID ${breachId} not found` }
      };
    }
    // Return full list if no specific ID
    return mockData.dataBreachNotifications;
  }

  // Other
  if (url.includes('component')) {
    return mockData.components;
  }
  if (url.includes('client-credential')) {
    return mockData.clientCredentials;
  }
  if (url.includes('audit')) {
    return mockData.auditReports;
  }
  // Scheduler Stats - check for scheduler/stats or schedular/v1/stats
  if (url.includes('scheduler') || url.includes('schedular')) {
    return mockData.schedulerStats;
  }

  // DPO Dashboard - check this BEFORE general dashboard
  if (url.includes('dpo') && url.includes('dashboard')) {
    return mockData.dashboardStats;
  }

  // Dashboard data endpoint
  if (url.includes('dashboard') && url.includes('data')) {
    return mockData.organizationMap;
  }

  // Cookie dashboard/logs
  if (url.includes('cookie') && url.includes('dashboard')) {
    return mockData.cookieLogs;
  }

  // Organization map
  if (url.includes('organization')) {
    return mockData.organizationMap;
  }

  if (url.includes('ropa')) {
    return mockData.ropaEntries;
  }
  if (url.includes('pending') && url.includes('request')) {
    return mockData.pendingRequests;
  }

  // Generic dashboard stats (last resort)
  if (url.includes('dashboard') && url.includes('stat')) {
    return mockData.dashboardStats;
  }

  // Translations
  if (url.includes('translate') || url.includes('translation')) {
    // For sandbox, just return the input as output (no actual translation)
    return {
      status: 200,
      data: {
        output: mockData.translations.data.output
      }
    };
  }

  // Default responses for create/update/delete
  if (url.includes('POST')) return mockData.createSuccess;
  if (url.includes('PUT')) return mockData.updateSuccess;
  if (url.includes('DELETE')) return mockData.deleteSuccess;

  return mockData.createSuccess; // Fallback
};

