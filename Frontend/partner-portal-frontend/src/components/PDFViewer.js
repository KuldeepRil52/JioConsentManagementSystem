import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Text, ActionButton, Spinner } from '../custom-components';
import { IcClose } from '../custom-components/Icon';
import '../styles/pdfViewer.css';

// Import PDF files
let termsOfUseDoc, privacyPolicyDoc, annexureDoc;
try {
  termsOfUseDoc = new URL("../assets/TermsofUseCMS.pdf", import.meta.url).href;
} catch (e) {
  console.error("Error loading Terms of Use PDF:", e);
  termsOfUseDoc = null;
}

try {
  privacyPolicyDoc = new URL("../assets/PrivacyPracticeStatementCMS.pdf", import.meta.url).href;
} catch (e) {
  console.error("Error loading Privacy Policy PDF:", e);
  privacyPolicyDoc = null;
}

try {
  annexureDoc = new URL("../assets/Annexure.pdf", import.meta.url).href;
} catch (e) {
  console.error("Error loading Annexure PDF:", e);
  annexureDoc = null;
}

const PDFViewer = () => {
  const { documentType } = useParams();
  const navigate = useNavigate();
  const [pdfUrl, setPdfUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    // Determine which PDF to load based on route parameter
    let url = null;
    let title = '';

    if (documentType === 'terms-of-use' || documentType === 'terms') {
      url = termsOfUseDoc;
      title = 'Terms of Use';
    } else if (documentType === 'privacy-policy' || documentType === 'privacy') {
      url = privacyPolicyDoc;
      title = 'Privacy Policy';
    } else if (documentType === 'annexure') {
      url = annexureDoc;
      title = 'Annexure';
    }

    if (url) {
      setPdfUrl(url);
      setLoading(false);
    } else {
      setError('Document not found');
      setLoading(false);
    }
  }, [documentType]);

  // Intercept PDF hyperlinks within the PDF viewer
  useEffect(() => {
    // Store original window.open
    if (!window._originalWindowOpen) {
      window._originalWindowOpen = window.open.bind(window);
    }

    // Intercept window.open calls from within the PDF (iframe)
    const handlePDFLink = (url) => {
      if (url && typeof url === 'string') {
        // Check if it's already a route URL (handles both relative and absolute)
        if (url.includes('/pdf/privacy-policy') || url.includes('/pdf/terms-of-use') || url.includes('/pdf/annexure')) {
          // Extract the route and navigate
          if (url.includes('/pdf/privacy-policy')) {
            navigate('/pdf/privacy-policy');
            return true;
          }
          if (url.includes('/pdf/terms-of-use')) {
            navigate('/pdf/terms-of-use');
            return true;
          }
          if (url.includes('/pdf/annexure')) {
            navigate('/pdf/annexure');
            return true;
          }
        }
        
        // Check if it's a Privacy Policy link (old format)
        if (
          url.includes('PrivacyPracticeStatementCMS') ||
          url.includes('PrivacyPracticeStatement') ||
          url.endsWith('PrivacyPracticeStatementCMS.pdf') ||
          url.includes('/PrivacyPracticeStatementCMS.pdf')
        ) {
          navigate('/pdf/privacy-policy');
          return true; // Handled
        }
        // Check if it's a Terms of Use link (old format)
        if (
          url.includes('TermsofUseCMS') ||
          url.includes('TermsOfUse') ||
          url.endsWith('TermsofUseCMS.pdf') ||
          url.includes('/TermsofUseCMS.pdf')
        ) {
          navigate('/pdf/terms-of-use');
          return true; // Handled
        }
        // Check if it's an Annexure link (old format)
        if (
          url.includes('Annexure') ||
          url.endsWith('Annexure.pdf') ||
          url.includes('/Annexure.pdf')
        ) {
          navigate('/pdf/annexure');
          return true; // Handled
        }
      }
      return false; // Not handled
    };

    // Override window.open to intercept PDF links
    window.open = function(url, target, features) {
      if (handlePDFLink(url)) {
        return null; // Prevent default navigation
      }
      // For other URLs, use original window.open
      return window._originalWindowOpen.call(this, url, target, features);
    };

    // Cleanup: restore original window.open
    return () => {
      window.open = window._originalWindowOpen;
    };
  }, [navigate]);

  const handleClose = () => {
    navigate(-1); // Go back to previous page
  };

  if (loading) {
    return (
      <div className="pdf-viewer-container">
        <div className="pdf-viewer-loading">
          <Spinner size="large" />
          <Text appearance="body-m" color="primary-grey-60">
            Loading document...
          </Text>
        </div>
      </div>
    );
  }

  if (error || !pdfUrl) {
    return (
      <div className="pdf-viewer-container">
        <div className="pdf-viewer-error">
          <Text appearance="heading-s" color="primary-grey-100">
            Document Not Found
          </Text>
          <Text appearance="body-m" color="primary-grey-80">
            {error || 'The requested document could not be loaded.'}
          </Text>
          <ActionButton
            label="Go Back"
            onClick={handleClose}
            kind="primary"
            size="medium"
          />
        </div>
      </div>
    );
  }

  return (
    <div className="pdf-viewer-container">
      <div className="pdf-viewer-header">
        <Text appearance="heading-s" color="primary-grey-100">
          {documentType === 'terms-of-use' || documentType === 'terms' 
            ? 'Terms of Use' 
            : documentType === 'privacy-policy' || documentType === 'privacy'
            ? 'Privacy Policy'
            : documentType === 'annexure'
            ? 'Annexure'
            : 'Document'}
        </Text>
        <ActionButton
          icon={<IcClose />}
          onClick={handleClose}
          kind="tertiary"
          size="medium"
          label="Close"
        />
      </div>
      <div className="pdf-viewer-content">
        <iframe
          src={pdfUrl}
          title={documentType === 'terms-of-use' || documentType === 'terms' 
            ? 'Terms of Use' 
            : documentType === 'privacy-policy' || documentType === 'privacy'
            ? 'Privacy Policy'
            : documentType === 'annexure'
            ? 'Annexure'
            : 'Document'}
          className="pdf-iframe"
          style={{
            width: '100%',
            height: '100%',
            border: 'none',
          }}
        />
      </div>
    </div>
  );
};

export default PDFViewer;

