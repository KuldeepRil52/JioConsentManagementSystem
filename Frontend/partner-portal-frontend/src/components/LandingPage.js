import React, { useState } from "react";
import "../styles/landingPage.css";
import { Text, TokenProvider, Button, Icon, InputFieldV2, ActionButton } from "../custom-components";
import { IcFavorite, IcQrCode, IcClose, IcLanguage, IcRechargeNow, IcStatusFail, IcSubscriptions } from "../custom-components/Icon";
import { useNavigate } from "react-router-dom";
import { enableSandboxMode } from "../utils/sandboxMode";
import { toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const LandingPage = () => {
  const navigate = useNavigate();
  const [activeIndex, setActiveIndex] = useState(null);
  const [showSandboxModal, setShowSandboxModal] = useState(false);
  const [sandboxName, setSandboxName] = useState("");
  const [sandboxEmail, setSandboxEmail] = useState("");
  const [nameError, setNameError] = useState("");
  const [emailError, setEmailError] = useState("");
  
  // Helper to check if form is valid
  const isFormValid = () => {
    const nameValidation = validateName(sandboxName);
    const emailValidation = validateEmail(sandboxEmail);
    return nameValidation.isValid && emailValidation.isValid;
  };

  const handleClick = () => {
    navigate("/signup");
  };

  // Input sanitization function to prevent XSS
  const sanitizeInput = (input) => {
    if (!input) return "";
    
    // Remove any HTML tags
    let sanitized = input.replace(/<[^>]*>/g, '');
    
    // Remove script-related content
    sanitized = sanitized.replace(/javascript:/gi, '');
    sanitized = sanitized.replace(/on\w+\s*=/gi, '');
    
    // Remove special characters that could be used for injection
    sanitized = sanitized.replace(/[<>\"'`]/g, '');
    
    return sanitized.trim();
  };

  // Validate name - only letters, spaces, hyphens, and apostrophes
  const validateName = (name) => {
    const sanitized = sanitizeInput(name);
    
    if (!sanitized) {
      return { isValid: false, error: "Name is required", sanitized: "" };
    }
    
    if (sanitized.length < 2) {
      return { isValid: false, error: "Name must be at least 2 characters", sanitized };
    }
    
    if (sanitized.length > 50) {
      return { isValid: false, error: "Name must not exceed 50 characters", sanitized };
    }
    
    // Allow only letters, spaces, hyphens, apostrophes, and dots
    const namePattern = /^[a-zA-Z][a-zA-Z\s\-'\.]*$/;
    if (!namePattern.test(sanitized)) {
      return { isValid: false, error: "Name can only contain letters, spaces, hyphens, and apostrophes", sanitized };
    }
    
    // Check for suspicious patterns
    const suspiciousPatterns = /(\bscript\b|\balert\b|\beval\b|\bdocument\b|\bwindow\b)/i;
    if (suspiciousPatterns.test(sanitized)) {
      return { isValid: false, error: "Invalid characters detected", sanitized };
    }
    
    return { isValid: true, error: "", sanitized };
  };

  // Validate email with comprehensive checks
  const validateEmail = (email) => {
    const sanitized = sanitizeInput(email);
    
    if (!sanitized || sanitized.trim() === '') {
      return { isValid: false, error: "Email is required", sanitized: "" };
    }
    
    if (sanitized.length > 100) {
      return { isValid: false, error: "Email must not exceed 100 characters", sanitized };
    }
    
    // RFC 5322 compliant email regex (simplified but strict)
    const emailPattern = /^[a-zA-Z0-9]([a-zA-Z0-9._-])*@[a-zA-Z0-9]([a-zA-Z0-9.-])*\.[a-zA-Z]{2,}$/;
    if (!emailPattern.test(sanitized)) {
      return { isValid: false, error: "Please enter a valid email address", sanitized };
    }
    
    // Additional checks for suspicious patterns
    const suspiciousPatterns = /(\bscript\b|\balert\b|\beval\b|\bdocument\b|\bwindow\b|javascript:|data:)/i;
    if (suspiciousPatterns.test(sanitized)) {
      return { isValid: false, error: "Invalid email format", sanitized };
    }
    
    // Check for multiple @ symbols
    if ((sanitized.match(/@/g) || []).length !== 1) {
      return { isValid: false, error: "Invalid email format", sanitized };
    }
    
    // Ensure there's text before and after @
    const parts = sanitized.split('@');
    if (parts.length !== 2 || !parts[0] || !parts[1] || !parts[1].includes('.')) {
      return { isValid: false, error: "Please enter a valid email address", sanitized };
    }
    
    return { isValid: true, error: "", sanitized };
  };

  // Handle name input with validation
  const handleNameChange = (e) => {
    const value = e.target.value;
    const validation = validateName(value);
    
    setSandboxName(validation.sanitized);
    setNameError(validation.error);
  };

  // Handle email input with validation
  const handleEmailChange = (e) => {
    const value = e.target.value;
    const validation = validateEmail(value);
    
    setSandboxEmail(validation.sanitized);
    setEmailError(validation.error);
    
    // Clear error if email becomes valid
    if (validation.isValid && emailError) {
      setEmailError("");
    }
  };

const toggleFaq = (index) => {
  setActiveIndex(activeIndex === index ? null : index);
};

const handleSandboxEntry = () => {
  // Validate name
  const nameValidation = validateName(sandboxName);
  if (!nameValidation.isValid) {
    setNameError(nameValidation.error);
    toast.error(
      (props) => (
        <CustomToast
          {...props}
          type="error"
          message={nameValidation.error || "Please enter a valid name"}
        />
      ),
      { icon: false }
    );
    return;
  }
  
  // Validate email
  const emailValidation = validateEmail(sandboxEmail);
  if (!emailValidation.isValid) {
    setEmailError(emailValidation.error);
    toast.error(
      (props) => (
        <CustomToast
          {...props}
          type="error"
          message={emailValidation.error || "Please enter a valid email address"}
        />
      ),
      { icon: false }
    );
    return;
  }
  
  // Clear any previous errors
  setNameError("");
  setEmailError("");
  
  // Enable sandbox mode with sanitized user data
  enableSandboxMode({ 
    name: nameValidation.sanitized, 
    email: emailValidation.sanitized 
  });
  
  // Open sandbox in new tab
  window.open('/dpo-dashboard?sandbox=true', '_blank');
  
  // Close modal and reset
  setShowSandboxModal(false);
  setSandboxName("");
  setSandboxEmail("");
  setNameError("");
  setEmailError("");
};

    const banner = new URL("../assets/Banner.svg", import.meta.url).href;
    const consentImg = new URL("../assets/consentImg.svg", import.meta.url).href;
    const cookieConsent = new URL("../assets/cookieConsent.svg", import.meta.url).href;
    const logos = new URL("../assets/logos.png", import.meta.url).href;
    const newdsar = new URL("../assets/newdsar.svg", import.meta.url).href;

    const faqList = [
      {
        question: "What is a Consent Management System (CMS)?",
        answer:
          "A CMS is a platform that enables organizations (Data Fiduciaries) to collect, manage, and track consents from individuals (Data Principals) in compliance with data protection laws like the DPDP Act 2023, DPDP, etc. It ensures that consents are explicit, auditable, and revocable.",
      },
      {
        question: "Why is consent management important under the DPDP Act 2023?",
        answer:
          "The DPDP Act requires Data Fiduciaries to process personal data only with valid, informed, and freely given consent. A CMS ensures compliance by capturing explicit consent, providing withdrawal and update mechanisms, and maintaining audit trails for proof of consent.",
      },
      {
        question: "How are consents stored in the CMS?",
        answer:
          "Consents are stored as digitally signed artifacts (JWS tokens) that are tamper-proof, immutable, version-controlled for updates and renewals, and stored in a WORM-compliant repository for legal audits.",
      },
      {
        question: "Can a Data Fiduciary update a consent once it has been given?",
        answer:
          "Yes, but only through an update request, such as adding a new purpose. The Data Principal must explicitly re-consent to accept these changes. Until then, the previous consent remains valid.",
      },
      {
        question: "What happens if a Data Principal withdraws consent?",
        answer:
          "The consent artifact status becomes WITHDRAWN. The Data Fiduciary must immediately stop processing the data. CMS triggers notifications to both DF and DP and records this event in audit logs.",
      },
      {
        question: "How are grievances handled in the CMS?",
        answer:
          "Data Principals can raise grievances via an embedded grievance form. CMS creates a grievance ticket, enforces SLA timelines, and escalates automatically to the Data Protection Officer (DPO) if unresolved. Status updates are notified through callbacks.",
      },
      {
        question: "Does the CMS support multi-language consent collection?",
        answer:
          "Yes. CMS supports 22 Indian languages (as per the 8th Schedule) and ensures WCAG accessibility compliance, enabling inclusivity for diverse users.",
      },
      {
        question: "How does CMS handle cookie consent?",
        answer:
          "The system includes a cookie consent module that detects and categorizes cookies (Essential, Analytics, Marketing), displays customizable banners, and stores user preferences with full audit trails.",
      },
      
      {
        question: "What are the benefits of using CMS for organizations?",
        answer:
          "CMS ensures compliance with the DPDP Act and global laws, increases user trust, automates grievance redressal and notifications, enables tenant isolation, and supports billing, reporting, and monitoring for legal entities.",
      },
      {
        question: "What is a Consent Artifact and how is it secured?",
        answer:
          "A Consent Artifact is a digitally signed JWS token containing consent metadata such as purpose, data items, processors, and validity. It is signed with the CMS private key and verifiable with its public key for tamper-proof authenticity.",
      },
      
      {
        question: "What happens when consent validity expires?",
        answer:
          "CMS automatically marks the consent as EXPIRED and triggers notifications to both Data Fiduciary and Data Principal. A renewal flow is initiated, and processing cannot continue until renewal.",
      },
      
      {
        question: "Can CMS support offline consent collection?",
        answer:
          "Yes. CMS supports offline consent workflows via SMS, WhatsApp, and email notifications. Businesses like hotels or showrooms can trigger consent links at the point of sale, enabling secure digital consent.",
      },
      {
        question: "How does CMS handle grievance escalation?",
        answer:
          "If a grievance is unresolved within SLA, CMS escalates automatically: first to the Grievance Officer, then to the Data Protection Officer (DPO). All escalations are logged and notified to concerned parties.",
      },
      {
        question: "How are logs and audit records stored?",
        answer:
          "CMS uses WORM (Write Once Read Many) compliant storage for logs, ensuring immutability and encryption. Logs capture who, when, and what action was taken, and are retained as per configurable retention policies.",
      },
      {
        question: "How does CMS handle accessibility requirements?",
        answer:
          "The CMS UI complies with WCAG 2.1 standards and supports multi-language popups, screen readers, color contrast controls, and keyboard navigation for accessible user experience.",
      },
      
     
    ];
    

  return (
     <TokenProvider
          value={{
            theme: "JioBase",
            mode: "light",
          }}
        >
    <div className="landing-page">

      {/* Hero Section */}
        <section className="hero-container">
            <div className="hero-left">
              <div className="main-heading">
                <Text appearance='heading-m' color='primary_grey_100'>
                Manage
                </Text>
                <Text appearance='heading-m' color='primary_50' style={{color: '#0f3cc9'}}>
                 consent
                </Text>
                <Text appearance='heading-m' color='primary_grey_100'>
                 without
                </Text>
                </div>
                <br></br>
                <Text appearance='heading-m' color='primary_grey_100'>
                any hassle.
                </Text>
                
                <br></br>
                <div className="hero-subs">
                    <Text appearance='body-m' color='primary_grey_80'>
                    Stay compliant with India’s DPDP Act, 2023 and give
                    <br></br>
                    users control over their data — all in one secure platform.
                    </Text>
                </div>
                <div className="hero-buttons" style={{ display: "flex", gap: "15px", flexWrap: "wrap" }}>
                <Button
                    ariaControls="Button Clickable"
                    ariaDescribedby="Button"
                    ariaExpanded="Expanded"
                    ariaLabel="Button"
                    className="Button"
                    icon=""
                    iconAriaLabel="Icon Favorite"
                    iconLeft=""
                    kind="primary"
                    label="Get started"
                    onClick={handleClick}
                    size="medium"
                    state="normal"
                    />
                {/* <Button
                    ariaControls="Sandbox Button"
                    ariaDescribedby="Sandbox"
                    ariaExpanded="Expanded"
                    ariaLabel="Try Sandbox"
                    className="Button"
                    kind="secondary"
                    label="Try Sandbox"
                    onClick={() => setShowSandboxModal(true)}
                    size="medium"
                    state="normal"
                    /> */}
                </div>
            </div>

            <div className="hero-right">
                <img src={banner} alt="Hero Dashboard" className="hero-image" />
            </div>
        </section>


          {/* Feature Cards */}
      <section className="feature-cards">
        
        <Text appearance='body-xs-bold' color='primary-50'>
            ONE STOP SOLUTION
            </Text>
            <br></br>
            <Text appearance='heading-s' color='primary_grey_100'>
            We have everything you need
                </Text>
        <div className="cards">

          <div className="card-landing">
              <div className="jds-icons">
                        <Icon
                    color="primary"
                    ic={<IcSubscriptions></IcSubscriptions>}
                    kind="background"
                    onClick={function noRefCheck() {}}
                    size='xtra-xtra-large'
                style={{marginRight: '5px'}}
                  />
              </div>
          
                <Text appearance='heading-xs' color='primary_grey_100' 
                style={{marginRight: '10px'}}
                >
                Secure audit <br></br> logs
                </Text>
                
                <div style={{marginTop: '10px', marginBottom: '12px'}}>
                    <Text appearance='body-m' color='primary_grey_80'>
                    Always be ready for compliance checks.
                    </Text>
                </div>

          </div>


          <div className="card-landing">
            {/* <Icon size='small' color='primary-20' ic={IcLanguage}></Icon> */}
              <div className="jds-icons">
                    <Icon
                color="primary"
                ic={<IcLanguage></IcLanguage>}
                kind="background"
                onClick={function noRefCheck() {}}
                size='xtra-xtra-large'
                style={{marginRight: '5px'}}
              />
              </div>
                <Text appearance='heading-xs' color='primary_grey_100' 
                style={{marginRight: '10px'}}
                
                >
                Multi-language <br></br> support
                </Text>
                
                <div style={{marginTop: '10px', marginBottom: '12px'}}>
                    <Text appearance='body-m' color='primary_grey_80'>
                    Communicate in your users' preferred language.
                    </Text>
                </div>
          </div>


          <div className="card-landing">

                <div className="jds-icons">
                        <Icon
                    color="primary"
                    ic={<IcRechargeNow></IcRechargeNow>}
                    kind="background"
                    onClick={function noRefCheck() {}}
                    size='xtra-xtra-large'
                    style={{marginRight: '5px'}}

                  />
                </div>


                <Text appearance='heading-xs' color='primary_grey_100' style={{marginRight: '10px'}}
                >
                Cross-platform <br></br> integration
                </Text>
                
                <div style={{marginTop: '10px', marginBottom: '12px'}}>
                    <Text appearance='body-m' color='primary_grey_80'>
                    Works across websites, apps, and digital touchpoints.
                    </Text>
                </div>
          </div>
          <div className="card-landing">
          <div className="jds-icons">
                        <Icon
                    color="primary"
                    ic={<IcStatusFail></IcStatusFail>}
                    kind="background"
                    onClick={function noRefCheck() {}}
                    size='xtra-xtra-large'
                    style={{marginRight: '5px'}}

                  />
              </div>
          <Text appearance='heading-xs' color='primary_grey_100' style={{marginRight: '10px'}}
          >
                Consent withdrawal <br></br> anytime
                </Text>
                
                <div style={{marginTop: '10px', marginBottom: '12px'}}>
                    <Text appearance='body-m' color='primary_grey_80'>
                    Let users change their mind in seconds.
                    </Text>
                </div>
          </div>
        </div>
        
      </section>

      {/* Features Section */}
      <section className="features">
  <Text appearance="heading-s" color="primary_grey_100">
    Why Jio Consent Management is right fit for you
  </Text>

  <div className="features-grid">
    {/* Consent management */}
    {/* <div className="feature-item">
      <div className="feature-text" style={{ marginTop: "7%"}}>
        <div className="heading-feature-1">
          <Text appearance="heading-xs" color="primary_grey_100">
            Consent management
          </Text>
        </div>
        <div className="feature-list">
            <Text appearance='body-s' color='primary-grey-80'>
                  <ul>
                    <li>Deploy banners, forms, and pop-ups — no coding required.</li>
                    <li>Let users give, withdraw, or modify consent anytime.</li>
                    <li>Keep time-stamped, tamper-proof consent records.</li>
                    <li>Multi-language support to engage users in their preferred language.</li>
                  </ul>
            </Text>
        </div>
      </div>
      <div className="feature-image">
        <img src={consentImg} alt="Consent management" />
      </div>
    </div> */}
    <div className="feature-1" style={{display: "flex", flexDirection: "row", justifyContent: "centre", alignItems: "center", marginRight:'5%'}}>
      <div className="content" style={{display:'flex', flexDirection:'column', paddingLeft:'8%', paddingRight:'2%'}}>
        <Text appearance="heading-xs" color="primary_grey_100">
            Consent management
        </Text>
        <br></br>
        <div className="points">
            <Text appearance='body-s' color='primary-grey-80'>
                  <ul>
                    <li>Deploy banners, forms, and pop-ups — no coding required.</li>
                    <li>Let users give, withdraw, or modify consent anytime.</li>
                    <li>Keep time-stamped, tamper-proof consent records.</li>
                    <li>Multi-language support to engage users in their preferred language.</li>
                  </ul>
            </Text>
        </div>
      </div>
      <div className="feature-image-new">
      <img src={consentImg} alt="Consent management" />
      </div>
    </div>

    {/* Cookie consent */}
    {/* <div className="feature-item">
      <div className="feature-image-2">
          <img src={cookieConsent} alt="Cookie consent" />
        </div>
        <div className="feature-text" style={{ marginTop: "7%"}}>
        <div className="heading-feature-2">
          <Text appearance="heading-xs" color="primary_grey_100">
            Cookie consent
          </Text>
        </div>
          <div className="feature-list">
          <Text appearance='body-s' color='primary-grey-80'>
                  <ul>
                      <li>Configure and display customizable cookie banners.</li>
                      <li>Let users choose their tracking preferences simply.</li>
                      <li>Automatically block non-essential cookies until consent is given.</li>
                      <li>Cross-platform support: websites, mobile apps, and kiosks.</li>
                  </ul>
                  </Text>
                  </div>
        </div>
      
    </div> */}

    <div className="feature-1" style={{display: "flex", flexDirection: "row", justifyContent: 'center', alignItems: "center", marginRight:'5%'}}>
      <div className="feature-image-new">
      <img src={cookieConsent} alt="Cookie consent" />
      </div>
      <div className="content" style={{display:'flex', flexDirection:'column', paddingLeft:'2%'}}>
        <Text appearance="heading-xs" color="primary_grey_100">
          Cookie consent
        </Text>
        <br></br>
        <div className="points">
            <Text appearance='body-s' color='primary-grey-80'>
            <ul>
                    <li>Configure and display customizable cookie banners.</li>
                    <li>Let users choose their tracking preferences simply.</li>
                    <li>Automatically block non-essential cookies until consent is given.</li>
                    <li>Cross-platform support: websites, mobile apps, and kiosks.</li>
                </ul>
            </Text>
        </div>
      </div>
      
    </div>

    {/* DSAR and grievance management */}
    {/* <div className="feature-item">
      <div className="feature-text" style={{ marginTop: "7%"}}>

          <div className="heading-feature-3">
            <Text appearance="heading-xs" color="primary_grey_100">
            DSAR and grievance management
            </Text>
          </div>
        
        <div className="feature-list">
                <Text appearance='body-s' color='primary-grey-80'>
                <ul>
                    <li>Let Data Principals raise requests (DSARs) securely.</li>
                    <li>Provide a dedicated grievance portal with status tracking.</li>
                    <li>Automate response workflows to save time and ensure SLA compliance.</li>
                    <li>Generate reports for audits or internal reviews instantly.</li>
                </ul>
                </Text>
                </div>
      </div>
      <div className="feature-image-3">
        <img src={newdsar} alt="DSAR and grievance management" />
      </div>
    </div> */}

    <div className="feature-1" style={{display: "flex", flexDirection: "row", justifyContent: 'center', alignItems: "center", marginRight:'5%'}}>
      <div className="content" style={{display:'flex', flexDirection:'column', paddingLeft:'8%', paddingRight:'2%'}}>
        <Text appearance="heading-xs" color="primary_grey_100">
            Grievance Redressal Management
        </Text>
        <br></br>
        <div className="points">
            <Text appearance='body-s' color='primary-grey-80'>
            <ul>
                    <li>Let Data Principals raise grievances securely.</li>
                    <li>Provide a dedicated grievance portal with status tracking.</li>
                    <li>Automate response workflows to save time and ensure SLA compliance.</li>
                    <li>Generate reports for audits or internal reviews instantly.</li>
                </ul>
            </Text>
        </div>
      </div>
      <div className="feature-image-new">
      <img src={newdsar} alt="DSAR and grievance management" />
      </div>
    </div>

  </div>
</section>


      {/* Stats Section */}
        <section className="stats">
            <Text appearance='heading-s' color='primary-inverse'>
                Compliance solution you can trust
            </Text>
            <div className="stats-row">
                <div className="stat-item">
                    <Text appearance="heading-s" color="primary-inverse">
                        30+ Million
                    </Text>
                <br></br>
                <div className="stat-subitem">
                    <Text appearance="body-m" color="primary_grey_60">
                        Consents created seamlessly
                    </Text>
                </div>
                </div>

                <div className="stat-item">
                <Text appearance="heading-s" color="primary-inverse">
                    2+ Lakh
                </Text>
                <br></br>
                <div className="stat-subitem">
                <Text appearance="body-m" color="primary_grey_60">
                    Consents revoked transparently by users
                </Text>
                </div>
                </div>

                <div className="stat-item">
                <Text appearance="heading-s" color="primary-inverse">
                  800+ Million
                </Text>
                <br></br>
                <div className="stat-subitem">
                <Text appearance="body-m" color="primary_grey_60">
                    Consents validated & tracked securely
                </Text>
                </div>
                </div>
            </div>
        </section>

      {/* Testimonials */}
      <section className="testimonials">
        <div className="logos">
            <Text appearance='heading-s' color='primary_grey_100'>
                Any platform, any device
            </Text>
            <br></br>
            <div className="framework">
                <Text appearance='body-m' color='primary_grey_80'>
                    Frameworks and platforms supported
                </Text>
            </div>
            <div className="framework-icons">
                
                    <img src={logos} alt="Consent management" />
                
            </div>
        </div>
      </section>

      {/* FAQ */}
      
      <section className="faq">
  <Text appearance="heading-s" color="primary_grey_100">
    Frequently asked questions
  </Text>

  <div className="faq-wrapper">
    {faqList.map((item, index) => (
      <div key={index} className="faq-item">
        <div className="faq-question" onClick={() => toggleFaq(index)}>
          <Text appearance="body-s-bold" color="primary_grey_100">
            {item.question}
          </Text>
          <ActionButton
            appearance="normal"
            ariaDescribedby="Button"
            ariaHidden="None"
            ariaLabel="Button"
            icon={activeIndex === index ? "ic_minus" : "ic_add"}
            iconAriaLabel="icon"
            iconPosition="left"
            kind="tertiary"
            label=""
            size="medium"
            state="normal"
          />
        </div>

        {activeIndex === index && (
          <div className="faq-answer">
            <Text appearance="body-s" color="primary_grey_80">
              {item.answer || "This is a placeholder answer for the FAQ."}
            </Text>
          </div>
        )}
      </div>
    ))}
  </div>
</section>

      

      {/* Final CTA */}
      <section className="final-cta">
        <Text appearance="heading-s" color='primary-inverse'>
        Ready to get started?
        </Text>
        <div className="cta-buttons">
            <button className="btn-white" onClick={handleClick}>Get started</button>
            {/* <button className="btn-link" onClick={() => setShowSandboxModal(true)}>Try Sandbox</button> */}
            
        </div>
      </section>

      {/* Sandbox Entry Modal */}
      {showSandboxModal && (
        <div style={{
          position: "fixed",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          backgroundColor: "rgba(0, 0, 0, 0.5)",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          zIndex: 10000
        }}>
          <div style={{
            backgroundColor: "white",
            borderRadius: "12px",
            padding: "30px",
            maxWidth: "450px",
            width: "90%",
            boxShadow: "0 10px 40px rgba(0, 0, 0, 0.2)"
          }}>
            <div style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              marginBottom: "20px"
            }}>
              <Text appearance="heading-xs" color="primary-grey-100">
                Try Sandbox Mode
              </Text>
              <ActionButton
                onClick={() => setShowSandboxModal(false)}
                icon={<IcClose />}
                kind="tertiary"
              />
            </div>

            <Text appearance="body-s" color="primary-grey-80" style={{ display: "block", marginBottom: "20px" }}>
              Enter your details to explore our Consent Management System with dummy data. No real account needed!
            </Text>

            <div style={{ marginBottom: "15px" }}>
              <InputFieldV2
                label="Your Name"
                value={sandboxName}
                placeholder="Enter your name (e.g., John Doe)"
                size="medium"
                onChange={handleNameChange}
                maxLength={50}
                required
              />
              {nameError && (
                <Text 
                  appearance="body-xxs" 
                  color="primary-red-100" 
                  style={{ display: "block", marginTop: "4px" }}
                >
                  {nameError}
                </Text>
              )}
            </div>

            <div style={{ marginBottom: "25px" }}>
              <InputFieldV2
                label="Your Email"
                value={sandboxEmail}
                placeholder="Enter your email (e.g., john@example.com)"
                size="medium"
                type="email"
                onChange={handleEmailChange}
                maxLength={100}
                required
              />
              {emailError && (
                <Text 
                  appearance="body-xxs" 
                  color="primary-red-100" 
                  style={{ display: "block", marginTop: "4px" }}
                >
                  {emailError}
                </Text>
              )}
            </div>

            <div style={{ display: "flex", gap: "10px", justifyContent: "flex-end" }}>
              <Button
                kind="secondary"
                label="Cancel"
                onClick={() => {
                  setShowSandboxModal(false);
                  setSandboxName("");
                  setSandboxEmail("");
                  setNameError("");
                  setEmailError("");
                }}
                size="medium"
              />
              <Button
                kind="primary"
                label="Launch Sandbox"
                onClick={handleSandboxEntry}
                size="medium"
                disabled={!isFormValid()}
              />
            </div>

            <div style={{
              marginTop: "20px",
              padding: "12px",
              backgroundColor: "#F0F7FF",
              borderRadius: "8px"
            }}>
              <Text appearance="body-xxs" color="primary-grey-80" style={{ display: "block" }}>
                ℹ️ Sandbox mode uses test data only. No actual data is saved or processed.
              </Text>
            </div>
          </div>
        </div>
      )}
 
    </div>
    </TokenProvider>
  );
};

export default LandingPage;
