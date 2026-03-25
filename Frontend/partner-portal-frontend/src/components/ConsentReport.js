import React, { useState } from "react";
import { useSelector } from "react-redux";
import { Text, ActionButton, Button, InputFieldV2, Select, Spinner } from '../custom-components';
import { IcSearch, IcDownload } from '../custom-components/Icon';
import "../styles/consentReport.css";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import { formatToIST } from "../utils/dateUtils";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const ConsentReport = () => {
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const token = useSelector((state) => state.common.session_token);
  const companyLogo = useSelector((state) => state.common.companyLogo);

  const [searchType, setSearchType] = useState("mobile");
  const [searchValue, setSearchValue] = useState("");
  const [loading, setLoading] = useState(false);
  const [consentData, setConsentData] = useState(null);
  const [selectedConsent, setSelectedConsent] = useState(null);
  const [showPreview, setShowPreview] = useState(false);

  const searchOptions = [
    { value: "mobile", label: "Mobile Number" },
    { value: "email", label: "Email Address" },
    { value: "consentId", label: "Consent ID" },
  ];

  const handleSearch = async () => {
    if (!searchValue.trim()) {
      toast.error(
        <CustomToast type="error" message="Please enter a search value" />,
        { autoClose: 3000 }
      );
      return;
    }

    setLoading(true);
    try {
      const txnId = generateTransactionId();
      let searchUrl = "";

      if (searchType === "consentId") {
        searchUrl = `${config.consent_search}?consentId=${encodeURIComponent(searchValue)}`;
      } else {
        searchUrl = `${config.consent_search}?customerIdentifiers.value=${encodeURIComponent(searchValue)}`;
      }

      const response = await fetch(searchUrl, {
        method: "GET",
        headers: {
          Accept: "*/*",
          "tenant-id": tenantId,
          txn: txnId,
        },
      });

      if (response.ok) {
        const data = await response.json();
        const consents = data?.searchList || data?.data || [];

        if (consents.length === 0) {
          toast.info(
            <CustomToast type="info" message="No consents found for this identifier" />,
            { autoClose: 3000 }
          );
          setConsentData(null);
        } else {
          setConsentData(consents);
          toast.success(
            <CustomToast
              type="success"
              message={`Found ${consents.length} consent${consents.length > 1 ? "s" : ""}`}
            />,
            { autoClose: 2000 }
          );
        }
      } else {
        throw new Error(`API returned status ${response.status}`);
      }
    } catch (error) {
      console.error("Error fetching consent data:", error);
      toast.error(
        <CustomToast type="error" message="Failed to fetch consent data" />,
        { autoClose: 3000 }
      );
      setConsentData(null);
    } finally {
      setLoading(false);
    }
  };

  const handleGeneratePDF = (consent) => {
    setSelectedConsent(consent);
    setShowPreview(true);
  };

  const downloadPDF = () => {
    if (!selectedConsent) return;

    // Get the preview content
    const element = document.getElementById("pdf-content");
    if (!element) return;

    // Create a printable version
    const printWindow = window.open("", "_blank");
    
    printWindow.document.write(`
      <!DOCTYPE html>
      <html>
        <head>
          <title>Consent Report - ${selectedConsent.consentId}</title>
          <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            body {
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
              padding: 40px;
              background: white;
              color: #000;
            }
            .pdf-container {
              max-width: 800px;
              margin: 0 auto;
              background: white;
              padding: 40px;
              box-shadow: 0 0 10px rgba(0,0,0,0.1);
            }
            .header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              border-bottom: 3px solid #0066ff;
              padding-bottom: 20px;
              margin-bottom: 30px;
            }
            .logo {
              width: 120px;
              height: auto;
            }
            .title {
              text-align: center;
              flex: 1;
            }
            h1 {
              color: #0066ff;
              font-size: 24px;
              margin-bottom: 5px;
            }
            .subtitle {
              color: #666;
              font-size: 12px;
            }
            .document-info {
              background: #f5f5f5;
              padding: 15px;
              border-radius: 8px;
              margin-bottom: 20px;
              border-left: 4px solid #0066ff;
            }
            .section {
              margin-bottom: 25px;
            }
            .section-title {
              font-size: 16px;
              font-weight: 700;
              color: #0066ff;
              margin-bottom: 12px;
              padding-bottom: 8px;
              border-bottom: 1px solid #e0e0e0;
            }
            .info-grid {
              display: grid;
              grid-template-columns: 1fr 1fr;
              gap: 15px;
              margin-bottom: 15px;
            }
            .info-item {
              padding: 10px;
              background: #fafafa;
              border-radius: 4px;
            }
            .info-label {
              font-size: 11px;
              color: #666;
              margin-bottom: 4px;
              text-transform: uppercase;
              font-weight: 600;
            }
            .info-value {
              font-size: 13px;
              color: #000;
              font-weight: 500;
            }
            .status-badge {
              display: inline-block;
              padding: 4px 12px;
              border-radius: 4px;
              font-size: 11px;
              font-weight: 600;
              text-transform: uppercase;
            }
            .status-active {
              background: #e8f5e9;
              color: #2e7d32;
            }
            .status-revoked {
              background: #ffebee;
              color: #c62828;
            }
            .status-expired {
              background: #fff3e0;
              color: #e65100;
            }
            .preference-card {
              background: #f9f9f9;
              border: 1px solid #e0e0e0;
              border-radius: 6px;
              padding: 15px;
              margin-bottom: 15px;
            }
            .preference-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 10px;
              padding-bottom: 10px;
              border-bottom: 1px dashed #ccc;
            }
            .purpose-list {
              margin: 10px 0;
            }
            .purpose-item {
              padding: 8px;
              background: white;
              border-left: 3px solid #4CAF50;
              margin-bottom: 8px;
              border-radius: 4px;
            }
            .footer {
              margin-top: 40px;
              padding-top: 20px;
              border-top: 1px solid #e0e0e0;
              text-align: center;
              color: #666;
              font-size: 11px;
            }
            .legal-notice {
              background: #fff9c4;
              border-left: 4px solid #fbc02d;
              padding: 15px;
              margin: 20px 0;
              border-radius: 4px;
            }
            .legal-notice p {
              font-size: 11px;
              line-height: 1.6;
              color: #333;
            }
            @media print {
              body { padding: 0; }
              .pdf-container { box-shadow: none; }
            }
          </style>
        </head>
        <body>
          ${element.innerHTML}
        </body>
      </html>
    `);

    printWindow.document.close();
    
    // Wait for content to load then trigger print
    printWindow.onload = () => {
      setTimeout(() => {
        printWindow.print();
      }, 250);
    };
  };

  const getStatusClass = (status) => {
    const statusLower = status?.toLowerCase() || "active";
    if (statusLower === "active") return "status-active";
    if (statusLower === "revoked") return "status-revoked";
    if (statusLower === "expired") return "status-expired";
    return "status-active";
  };

  return (
    <div className="configurePage">
      <div className="consent-report-container">
        {/* Header */}
        <div className="report-header">
          <div className="report-header-content">
            <h1>Consent Report Generator</h1>
            <p>Generate legal proof of consent for compliance and record-keeping</p>
          </div>
        </div>

        {/* Search Section */}
        <div className="search-section">
          <div className="search-section-title">
            <IcSearch />
            <Text appearance="heading-xxs" color="primary-grey-100">
              Search for User Consent
            </Text>
          </div>

          <div className="search-form">
            <div className="search-type-selector">
              <Text appearance="body-xs" color="primary-grey-60" style={{ marginBottom: "8px" }}>
                Search By
              </Text>
              <div className="radio-group">
                {searchOptions.map((option) => (
                  <label key={option.value} className="radio-label">
                    <input
                      type="radio"
                      name="searchType"
                      value={option.value}
                      checked={searchType === option.value}
                      onChange={(e) => setSearchType(e.target.value)}
                    />
                    <span>{option.label}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="search-input-group">
              <InputFieldV2
                label={`Enter ${searchOptions.find((o) => o.value === searchType)?.label}`}
                placeholder={
                  searchType === "mobile"
                    ? "e.g., 9876543210"
                    : searchType === "email"
                    ? "e.g., user@example.com"
                    : "e.g., consent-id-123"
                }
                value={searchValue}
                onChange={(e) => setSearchValue(e.target.value)}
                size="medium"
                onKeyPress={(e) => e.key === "Enter" && handleSearch()}
              />
              <ActionButton
                kind="primary"
                size="medium"
                label={loading ? "Searching..." : "Search"}
                onClick={handleSearch}
                disabled={loading || !searchValue.trim()}
                iconLeft={<IcSearch />}
              />
            </div>
          </div>
        </div>

        {/* Loading State */}
        {loading && (
          <div className="loading-state">
            <Spinner size="medium" />
            <Text appearance="body-m" color="primary-grey-60">
              Searching for consents...
            </Text>
          </div>
        )}

        {/* Results Section */}
        {!loading && consentData && consentData.length > 0 && (
          <div className="results-section">
            <div className="results-section-header">
              <Text appearance="heading-xxs" color="primary-grey-100">
                Search Results
              </Text>
              <span className="results-count">
                {consentData.length} Consent{consentData.length > 1 ? "s" : ""} Found
              </span>
            </div>

            <div className="consent-list">
              {consentData.map((consent, index) => (
                <div key={consent.consentId || index} className="consent-card">
                  <div className="consent-card-header">
                    <div>
                      <Text appearance="body-m-bold" color="primary-grey-100">
                        {consent.templateName}
                      </Text>
                      <Text appearance="body-xs" color="primary-grey-60">
                        Consent ID: {consent.consentId}
                      </Text>
                    </div>
                    <div className={`consent-status-badge ${getStatusClass(consent.status)}`}>
                      {consent.status || "ACTIVE"}
                    </div>
                  </div>

                  <div className="consent-card-details">
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">
                        Customer ID
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {consent.customerIdentifiers?.value || "N/A"}
                      </Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">
                        Created
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {formatToIST(consent.startDate || consent.createdAt)}
                      </Text>
                    </div>
                    <div className="detail-item">
                      <Text appearance="body-xs" color="primary-grey-60">
                        Valid Until
                      </Text>
                      <Text appearance="body-s" color="primary-grey-100">
                        {formatToIST(consent.endDate || consent.expiryDate)}
                      </Text>
                    </div>
                  </div>

                  <div className="consent-card-actions">
                    <ActionButton
                      kind="primary"
                      size="small"
                      label="Generate PDF Report"
                      onClick={() => handleGeneratePDF(consent)}
                      iconLeft={<IcDownload />}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* PDF Preview Modal */}
        {showPreview && selectedConsent && (
          <div className="pdf-preview-modal">
            <div className="pdf-preview-container">
              <div className="pdf-preview-header">
                <Text appearance="heading-xs" color="primary-grey-100">
                  PDF Preview
                </Text>
                <div className="pdf-preview-actions">
                  <Button
                    kind="primary"
                    size="small"
                    label="Download/Print PDF"
                    onClick={downloadPDF}
                    iconLeft={<IcDownload />}
                  />
                  <Button
                    kind="secondary"
                    size="small"
                    label="Close"
                    onClick={() => {
                      setShowPreview(false);
                      setSelectedConsent(null);
                    }}
                  />
                </div>
              </div>

              <div className="pdf-preview-content" id="pdf-content">
                <div className="pdf-container">
                  {/* Header */}
                  <div className="header">
                    {companyLogo && (
                      <img src={companyLogo} alt="Company Logo" className="logo" />
                    )}
                    <div className="title">
                      <h1>CONSENT RECORD</h1>
                      <div className="subtitle">Legal Proof of User Consent</div>
                    </div>
                    <div className={`status-badge ${getStatusClass(selectedConsent.status)}`}>
                      {selectedConsent.status || "ACTIVE"}
                    </div>
                  </div>

                  {/* Document Info */}
                  <div className="document-info">
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                      <strong>Document ID:</strong>
                      <span>{selectedConsent.consentId}</span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between", marginBottom: "8px" }}>
                      <strong>Generated On:</strong>
                      <span>{formatToIST(new Date())}</span>
                    </div>
                    <div style={{ display: "flex", justifyContent: "space-between" }}>
                      <strong>Tenant ID:</strong>
                      <span>{tenantId}</span>
                    </div>
                  </div>

                  {/* Consent Information */}
                  <div className="section">
                    <div className="section-title">📋 Consent Information</div>
                    <div className="info-grid">
                      <div className="info-item">
                        <div className="info-label">Template Name</div>
                        <div className="info-value">{selectedConsent.templateName}</div>
                      </div>
                      <div className="info-item">
                        <div className="info-label">Template Version</div>
                        <div className="info-value">{selectedConsent.templateVersion}</div>
                      </div>
                      <div className="info-item">
                        <div className="info-label">Customer Identifier</div>
                        <div className="info-value">
                          {selectedConsent.customerIdentifiers?.type}:{" "}
                          {selectedConsent.customerIdentifiers?.value}
                        </div>
                      </div>
                      <div className="info-item">
                        <div className="info-label">Consent Status</div>
                        <div className="info-value">
                          <span className={`status-badge ${getStatusClass(selectedConsent.status)}`}>
                            {selectedConsent.status || "ACTIVE"}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* Timeline */}
                  <div className="section">
                    <div className="section-title">📅 Timeline</div>
                    <div className="info-grid">
                      <div className="info-item">
                        <div className="info-label">Created Date</div>
                        <div className="info-value">{formatToIST(selectedConsent.startDate || selectedConsent.createdAt)}</div>
                      </div>
                      <div className="info-item">
                        <div className="info-label">Updated At</div>
                        <div className="info-value">{formatToIST(selectedConsent.updatedAt)}</div>
                      </div>
                      <div className="info-item">
                        <div className="info-label">Valid Until</div>
                        <div className="info-value">{formatToIST(selectedConsent.endDate || selectedConsent.expiryDate)}</div>
                      </div>
                    </div>
                  </div>

                  {/* Preferences */}
                  {selectedConsent.preferences && selectedConsent.preferences.length > 0 && (
                    <div className="section">
                      <div className="section-title">🎯 Consent Preferences</div>
                      {selectedConsent.preferences.map((pref, index) => (
                        <div key={pref.preferenceId || index} className="preference-card">
                          <div className="preference-header">
                            <strong>
                              {pref.mandatory ? "🔒 Mandatory" : "⚙️ Optional"} - {pref.preferenceStatus}
                            </strong>
                            <span>
                              Valid for: {pref.preferenceValidity?.value} {pref.preferenceValidity?.unit}
                            </span>
                          </div>

                          {pref.purposeList && pref.purposeList.length > 0 && (
                            <div className="purpose-list">
                              <div className="info-label">Purposes:</div>
                              {pref.purposeList.map((purpose, idx) => (
                                <div key={purpose.purposeId || idx} className="purpose-item">
                                  <strong>{purpose.purposeInfo?.purposeName}</strong>
                                  <div style={{ fontSize: "11px", color: "#666", marginTop: "4px" }}>
                                    {purpose.purposeInfo?.purposeDescription}
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Legal Notice */}
                  <div className="legal-notice">
                    <p>
                      <strong>⚖️ LEGAL NOTICE:</strong> This document serves as an official record of user consent
                      collected in accordance with applicable data protection regulations. This consent was obtained
                      through a legally compliant process and has been securely stored in our system. This document
                      can be used for regulatory audits, compliance verification, and legal proceedings.
                    </p>
                    <p style={{ marginTop: "10px" }}>
                      The information contained in this document is confidential and intended solely for regulatory
                      and compliance purposes. Any unauthorized use, disclosure, or distribution is prohibited.
                    </p>
                  </div>

                  {/* Footer */}
                  <div className="footer">
                    <p>
                      <strong>Jio Consent Management System</strong>
                    </p>
                    <p>This is an auto-generated document. No signature required.</p>
                    <p style={{ marginTop: "10px" }}>
                      Document generated on {formatToIST(new Date())} | Business ID: {businessId}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      <ToastContainer
        position="bottom-left"
        autoClose={3000}
        hideProgressBar
        closeOnClick
        pauseOnHover
        draggable
        closeButton={false}
        toastClassName={() => "toast-wrapper"}
        transition={Slide}
      />
    </div>
  );
};

export default ConsentReport;

