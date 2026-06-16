import React, { useState } from "react";
import { Text } from "../custom-components";
import { Text, Button, ActionButton } from "../custom-components";
import { useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import PII from "./PII";
import DataProcessor from "./DataProcessor";
import ProcessingActivity from "./ProcessingActivity";
import Purpose from "./Purpose";
import "../styles/masterSetup.css";
import CustomToast from "./CustomToastContainer";
import { Slide, ToastContainer, toast } from "react-toastify";
import "../styles/toast.css";
import { languages } from "../utils/languages";



const MasterSetup = () => {
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const navigate = useNavigate();
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);
  const [activeTab, setActiveTab] = useState("PII");
  const [selectedLanguage, setSelectedLanguage] = useState(languages[0].value);

  const handleLanguageChange = (e) => {
    const selectedLang = e.target.value;
    
    // Allow English without config check
    if (selectedLang === "en") {
      setSelectedLanguage(selectedLang);
      return;
    }
    
    // Check if multilingual config is saved
    if (!multilingualConfigSaved) {
      setShowMultilingualWarningModal(true);
      // Reset dropdown to previous value
      e.target.value = selectedLanguage || "en";
      return;
    }
    
    setSelectedLanguage(selectedLang);
  };

  const renderContent = () => {
    switch (activeTab) {
      case "PII":
        return <PII selectedLanguage={selectedLanguage} />;
      case "DataProcessor":
        return <DataProcessor selectedLanguage={selectedLanguage} />;
      case "ProcessingActivity":
        return <ProcessingActivity selectedLanguage={selectedLanguage} />;
      case "Purpose":
        return <Purpose selectedLanguage={selectedLanguage} />;
      default:
        return null;
    }
  };

  return (
    <>
      <div className="configurePage">
        <div className="ms-page">
          {/* Heading and Badge  */}
          <div className="systemConfig-header-and-badge">
            <div style={{display:'flex', gap:'15px'}}>
              <Text appearance="heading-s" color="primary-grey-100">
                Master Data
              </Text>
              <div className="systemConfig-badge" style={{marginTop:'5px'}}>
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  System Configuration
                </Text>
              </div>
            </div>
            <div style={{ marginLeft: 'auto', marginRight: '50px', display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
              <Text appearance="body-s">Select language to view and edit translation</Text>
              <select
                value={selectedLanguage}
                onChange={handleLanguageChange}
                style={{
                  width: '320px',
                  padding: '8px',
                  borderRadius: '6px',
                  border: '1px solid #ccc',
                  marginTop: '8px',
                }}
              >
                {languages.map((lang) => (
                  <option key={lang.value} value={lang.value}>
                    {lang.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="mainDiv-table-and-container">
            {/* Tabs Header and Divider combined */}
            <div className="masterSetup-new-css-tabs-header">
              {[
                {
                  key: "PII",
                  label: "Personally Identifiable Information (PII)",
                },
                { key: "DataProcessor", label: "Data Processor" },
                { key: "ProcessingActivity", label: "Processing Activity" },
                { key: "Purpose", label: "Purpose" },
              ].map((tab) => (
                <div
                  key={tab.key}
                  className={`masterSetup-new-css-tab-item ${
                    activeTab === tab.key ? "active" : ""
                  }`}
                  onClick={() => setActiveTab(tab.key)}
                >
                  <Text appearance="body-s" color="primary-grey-80">
                    {tab.label}
                  </Text>
                  {activeTab === tab.key && (
                    <div className="masterSetup-new-css-underline"></div>
                  )}
                </div>
              ))}
            </div>

            {/* Tab Content */}
            <div className="masterSetup-new-css-tab-content">
              {renderContent()}
            </div>
          </div>
        </div>
      </div>
      {/* Multilingual Warning Modal */}
      {showMultilingualWarningModal && (
        <div className="modal-overlay" onClick={() => setShowMultilingualWarningModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Multilingual Support Required
              </h2>
              <button
                className="modal-close-btn"
                onClick={() => setShowMultilingualWarningModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666',
                }}
              >
                ×
              </button>
            </div>
            
            <div className="modal-body">
              <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: '24px' }}>
                You need to configure multilingual support in the consent configuration to enable the translate functionality.
              </Text>

              <div style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'flex-end',
              }}>
                <Button
                  kind="secondary"
                  size="medium"
                  label="Cancel"
                  onClick={() => setShowMultilingualWarningModal(false)}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label="Go to Configuration"
                  onClick={() => {
                    setShowMultilingualWarningModal(false);
                    navigate("/consent");
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

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
    </>
  );
};

export default MasterSetup;
