// src/components/FooterComponent.jsx
import React from "react";
// import "../jds-styles/main.scss"; // Commented out - using custom components only
import { ActionButton } from "../custom-components";
import { useLocation, useNavigate } from "react-router-dom";
import { Footer, TokenProvider } from "../custom-components";

// PDFs are now accessed via routes: /pdf/terms-of-use and /pdf/privacy-policy

const FooterComponent = () => {
  const location = useLocation();
  const navigate = useNavigate();

  // ✅ Only show Add Cookie button on report page
  const isReportPage = location.pathname.startsWith("/registercookies/report");

  // Add event listeners to footer links after component mounts
  React.useEffect(() => {
    // Store handlers so we can remove them later
    const handlers = new Map();
    
    const attachListeners = () => {
      // Find all footer links
      const footerLinks = document.querySelectorAll('footer a, .footer a, [class*="footer"] a');
      
      footerLinks.forEach((link) => {
        // Skip if already has our data attribute (already processed)
        if (link.dataset.pdfHandlerAttached === 'true') {
          return;
        }
        
        const linkText = link.textContent?.trim();
        const href = link.getAttribute('href');
        
        // Create handler function
        let handler = null;
        
        // Check if it's our privacy policy or terms link
        if (linkText === 'Privacy Policy' || href === '#privacy-policy') {
          handler = (e) => {
            e.preventDefault();
            e.stopPropagation();
            navigate("/pdf/privacy-policy");
          };
        } else if (linkText === 'Terms & Conditions' || href === '#terms-conditions') {
          handler = (e) => {
            e.preventDefault();
            e.stopPropagation();
            navigate("/pdf/terms-of-use");
          };
        }
        
        // Attach handler if found
        if (handler) {
          link.addEventListener('click', handler);
          link.dataset.pdfHandlerAttached = 'true';
          handlers.set(link, handler);
        }
      });
    };

    // Try immediately and also after a short delay to ensure footer is rendered
    attachListeners();
    const timeoutId = setTimeout(attachListeners, 100);

    // Cleanup - remove all event listeners
    return () => {
      clearTimeout(timeoutId);
      handlers.forEach((handler, link) => {
        link.removeEventListener('click', handler);
        link.dataset.pdfHandlerAttached = 'false';
      });
      handlers.clear();
    };
  }, [navigate]);

  return (
    <TokenProvider
      value={{
        theme: "JioBase",
        mode: "light",
      }}
    >
      <div className="footer-fixed">
        <Footer
          copyright={{
            href: "#",
            newTab: true,
            title:
              "Copyright © 2025 Jio Platforms Limited. All rights reserved.",
          }}
          logo="ic_jio_dot"
          bottomLinks={[
           {
            title: "Contact Us",
            href: "mailto:support-consent@ril.com",
            newTab: false,
           },
            {
              title: "Privacy Policy",
              href: "#privacy-policy",
              newTab: false,
            },
            {
              title: "Terms & Conditions",
              href: "#terms-conditions",
              newTab: false,
            },
          ]}
        />
      </div>
      {isReportPage && (
        <div
        style={{
          height: "20px",
          width: "100%",
          bottom: 0,
          padding: "2px",
        }}
      >
        <div style={{ display: "flex", justifyContent: "flex-end", padding: "1rem" }}>
          <ActionButton kind="primary" label="Add Cookie" />
        </div>
        </div>
      )}
    </TokenProvider>
  );

};

export default FooterComponent;






