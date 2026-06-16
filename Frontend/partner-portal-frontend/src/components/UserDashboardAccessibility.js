import React, { useState, useEffect } from "react";
import { Text, Icon } from '../custom-components';
import { IcCode, IcKeyboard, IcVoice } from '../custom-components/Icon';
import "../styles/createConsent.css";

const UserDashboardAccessibility = ({ onTabChange, activeAccessibilityTab }) => {

   const [activeTab, setActiveTab] = useState(activeAccessibilityTab || "semantics");

  // Sync with parent's activeAccessibilityTab prop
  useEffect(() => {
    if (activeAccessibilityTab !== undefined) {
      setActiveTab(activeAccessibilityTab);
    }
  }, [activeAccessibilityTab]);

  // Notify parent component when tab changes
  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (onTabChange) {
      onTabChange(tab);
    }
  };

  // console.log("Active Tab:", activeTab);

  const renderContent = () => {
    switch (activeTab) {
      case "semantics":
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
              marginTop: "20px",
            }}
          >
            <div className="landmark-card">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge">
                  <span className="landmark-number">1</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Landmark Details
                </Text>
              </div>
              <div className="landmark-body">
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;header&gt;</span>
              </div>
            </div>
            <div className="landmark-card">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge">
                  <span className="landmark-number">2</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Landmark Details
                </Text>
              </div>
              <div className="landmark-body">
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;main&gt;</span>
              </div>
            </div>
            <div className="landmark-card-2">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-2">
                  <span className="landmark-number">1</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Image Details
                </Text>
              </div>
              <div className="landmark-body">
                <Text appearance="body-s" color="primary-grey-100">
                  accessible name
                </Text>
                <span className="landmark-tag">
                  Logo image is decorative. It will not be focussable
                </span>
              </div>
            </div>
            <div className="landmark-card-3">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-3">
                  <span className="landmark-number">h2</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Heading &lt;h2&gt;
                </Text>
              </div>
              <div
                className="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;h2&gt;</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  accessible label
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  User Dashboard
                </Text>
              </div>
            </div>
            <div className="landmark-card-3">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-3">
                  <span className="landmark-number">h2</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Heading &lt;h2&gt;
                </Text>
              </div>
              <div
                className="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;h2&gt;</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  accessible label
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  Granted consents
                </Text>
              </div>
            </div>
            <div className="landmark-card-4">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-4">
                  <span className="landmark-number">1</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Note
                </Text>
              </div>
              <div
                className="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;p&gt;</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  Paragraph content
                </Text>
                <div style={{ width: "50%" }}>
                  <Text appearance="body-xxs" color="primary-grey-80">
                    View and respond to your consent requests awaiting your action.
                  </Text>
                </div>
              </div>
            </div>
            <div className="landmark-card-6">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-6">
                  <span className="landmark-number">1</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Button details
                </Text>
              </div>
              <div
                className="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;button&gt;</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  accessible label
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  View consent details
                </Text>
              </div>
            </div>
            <div className="landmark-card-6">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-6">
                  <span className="landmark-number">2</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Button details
                </Text>
              </div>
              <div
                className="landmark-body"
                style={{ borderBottom: "solid 1px #e0e0e0" }}
              >
                <Text appearance="body-s" color="primary-grey-100">
                  element
                </Text>
                <span className="landmark-tag">&lt;button&gt;</span>
              </div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  justifyContent: "space-between",
                  width: "100%",
                  padding: "5px",
                }}
              >
                <Text appearance="body-xs" color="primary-grey-80">
                  accessible label
                </Text>
                <Text appearance="body-xxs" color="primary-grey-80">
                  Edit consent details
                </Text>
              </div>
            </div>
          </div>
        );
      case "focus":
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
              marginTop: "20px",
            }}
          >
            <div className="landmark-card-7">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-7">
                  <span className="landmark-number">1</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Focus Order Details
                </Text>
              </div>
              <div className="landmark-body-2">
                <Text appearance="body-s" color="primary-grey-80">
                  {" "}
                  • When the user is navigating with a keyboard using the tab
                  key, the focus will be on elements that are interactible - eg.
                  buttons, links, checkboxes, etc.{" "}
                </Text>
                <br></br>
                <Text appearance="body-s" color="primary-grey-80">
                  • They will be taken through these elements in the order
                  listed on the preview screen
                </Text>
              </div>
            </div>
          </div>
        );
      case "screen":
        return (
          <div
            style={{
              display: "flex",
              flexDirection: "column",
              gap: "20px",
              marginTop: "20px",
            }}
          >
            <div className="landmark-card-8">
              <div
                style={{
                  display: "flex",
                  flexDirection: "row",
                  alignItems: "center",
                }}
              >
                <div className="landmark-badge-8">
                  <span className="landmark-number">1</span>
                  <span className="landmark-icon"></span>
                </div>
                <Text appearance="body-xs-bold" color="primary-grey-100">
                  Reading Order Details
                </Text>
              </div>
              <div className="landmark-body-2">
                <Text appearance="body-s" color="primary-grey-80">
                  {" "}
                  • When user is navigating using a screen reader, it will be
                  reading the content of the screen in the order listed on the
                  preview.
                </Text>
                <br></br>
                <Text appearance="body-s" color="primary-grey-80">
                  • The screen reader will be reading out the accessible names,
                  and ARIA labels as mentioned on the HTML semantics page for
                  each element.{" "}
                </Text>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };


  return (
    <div className="acc-con">
      <div style={{ marginTop: "20px", padding: "15px 20px" }}>
        <Text appearance="heading-xxs" color="primary-grey-80">
          Element properties
        </Text>
        <br></br>
        <Text appearance="body-xs" color="primary-grey-80">
          Accessibility properties of elements and preview how
          they will be experienced by the user
        </Text>
      </div>
      <div className="tabs-container">
        <div className="tabs">
          <button
            className={`tab-btn ${activeTab === "semantics" ? "active" : ""
              }`}
            onClick={() => handleTabChange("semantics")}
          >
            <IcCode height={25} width={25} />
            <Text
              appearance="body-xs"
              color={
                activeTab === "semantics"
                  ? "primary-inverse"
                  : "primary-60"
              }
            >
              HTML semantics
            </Text>
          </button>
          <button
            className={`tab-btn ${activeTab === "focus" ? "active" : ""
              }`}
            onClick={() => handleTabChange("focus")}
          >
            <IcKeyboard height={25} width={25} />{" "}
            <Text
              appearance="body-xs"
              color={
                activeTab === "focus"
                  ? "primary-inverse"
                  : "primary-60"
              }
            >
              Keyboard focus order
            </Text>
          </button>
          <button
            className={`tab-btn ${activeTab === "screen" ? "active" : ""
              }`}
            onClick={() => handleTabChange("screen")}
          >
            <IcVoice height={25} width={25} />
            <Text
              appearance="body-xxs"
              color={
                activeTab === "screen"
                  ? "primary-inverse"
                  : "primary-60"
              }
            >
              Screen reader reading order
            </Text>
          </button>
        </div>
      </div>
      
      {/* Render the content based on active tab */}
      <div className="tab-content">
        {renderContent()}
      </div>
    </div>
  );
};

export default UserDashboardAccessibility;

