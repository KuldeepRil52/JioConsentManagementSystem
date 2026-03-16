import React from "react";

const AccManagePreviewRight = ({ tab = "semantics" }) => {
  // Badge styles based on tab type
  const semanticsBadge = (label) => ({
    background: "#9333ea",
    color: "#fff",
    fontSize: 10,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 6px",
    marginRight: 6,
    display: "inline-block",
  });

  const keyboardBadge = {
    background: "#7B1F3A",
    color: "#fff",
    fontSize: 9,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 5px",
    display: "inline-flex",
    alignItems: "center",
    flexDirection: "column",
    lineHeight: 1.2,
  };

  const screenReaderBadge = {
    background: "#7B5C00",
    color: "#fff",
    fontSize: 9,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 5px",
    display: "inline-flex",
    alignItems: "center",
    gap: 2,
  };

  const noteBadge = {
    background: "#6c757d",
    color: "#fff",
    fontSize: 9,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 5px",
  };

  const fieldsetBadge = {
    background: "#166534",
    color: "#fff",
    fontSize: 9,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 5px",
  };

  const buttonBadge = {
    background: "#166534",
    color: "#fff",
    fontSize: 9,
    fontWeight: 700,
    borderRadius: 4,
    padding: "2px 5px",
  };

  // Render badge based on tab and type
  const renderBadge = (type, number) => {
    if (tab === "semantics") {
      if (type === "h2" || type === "h3" || type === "h4") {
        return <span style={semanticsBadge(type)}>{type}</span>;
      } else if (type === "note") {
        return <span style={noteBadge}>Note</span>;
      } else if (type === "fieldset") {
        return <span style={fieldsetBadge}>{number} Fieldset</span>;
      } else if (type === "button") {
        return <span style={buttonBadge}>{number} Button</span>;
      }
    } else if (tab === "focus") {
      return (
        <span style={keyboardBadge}>
          <span style={{ fontSize: 8 }}>Tab</span>
          <span>→ #{number}</span>
        </span>
      );
    } else if (tab === "screen") {
      return <span style={screenReaderBadge}>🔊 #{number}</span>;
    }
    return null;
  };

  const toggle = (checked = false) => (
    <div
      style={{
        width: 36,
        height: 20,
        borderRadius: 12,
        background: checked ? "#0A3CCE" : "#9CA3AF",
        display: "flex",
        alignItems: "center",
        justifyContent: checked ? "flex-end" : "flex-start",
        padding: 2,
        cursor: "pointer",
        flexShrink: 0,
      }}
    >
      <div
        style={{
          width: 16,
          height: 16,
          borderRadius: "50%",
          background: "#fff",
        }}
      />
    </div>
  );

  // Border colors based on tab
  const getBorderColor = () => {
    if (tab === "semantics") return "#9333ea";
    if (tab === "focus") return "#7B1F3A";
    if (tab === "screen") return "#7B5C00";
    return "#9333ea";
  };

  const box = {
    border: `2px dashed ${getBorderColor()}`,
    borderRadius: 8,
    padding: 10,
    marginBottom: 10,
    position: "relative",
  };

  const fieldsetBox = {
    border: "2px dashed #166534",
    borderRadius: 6,
    padding: 8,
    marginTop: 8,
    position: "relative",
  };

  return (
    <div
      style={{
        position: "relative",
        width: "100%",
        maxWidth: 480,
        background: "#FFFFFF",
        color: "#111827",
        borderRadius: 12,
        border: `2px dashed ${getBorderColor()}`,
        padding: 16,
        margin: "20px auto",
        boxSizing: "border-box",
        fontSize: 12,
        maxHeight: 600,
        overflowY: "auto",
      }}
    >
      {/* Close Button */}
      <div style={{ position: "absolute", top: 12, right: 12, display: "flex", alignItems: "center", gap: 4 }}>
        {tab === "focus" && renderBadge("button", 11)}
        <button
          style={{
            background: "transparent",
            border: "1px solid #D1D5DB",
            borderRadius: "50%",
            width: 24,
            height: 24,
            cursor: "pointer",
            fontSize: 14,
          }}
        >
          ✕
        </button>
      </div>

      {/* Header */}
      <div style={{ marginBottom: 8, display: "flex", alignItems: "center", gap: 6, paddingRight: 40 }}>
        {renderBadge("h2", 1)}
        <h2 style={{ margin: 0, fontWeight: 800, fontSize: 16 }}>Manage preferences</h2>
      </div>

      {/* Description */}
      <div style={{ marginBottom: 12, display: "flex", alignItems: "flex-start", gap: 6 }}>
        {renderBadge("note", 2)}
        <p style={{ margin: 0, fontSize: 12, color: "#4B5563", lineHeight: 1.4 }}>
          Choose the cookies you allow. You can update your preferences anytime.
        </p>
      </div>

      {/* Required Cookies */}
      <div style={box}>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 6 }}>
          {renderBadge("h3", 3)}
          <h3 style={{ margin: 0, fontSize: 13, fontWeight: 700, flex: 1 }}>Required Cookies</h3>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            {tab === "focus" && renderBadge("toggle", 1)}
            {toggle(true)}
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 8 }}>
          {renderBadge("note", 4)}
          <p style={{ margin: 0, fontSize: 11, color: "#4B5563", lineHeight: 1.3 }}>
            These Cookies are necessary to enable the basic features of this site to function, such as providing secure log-in, allowing images to load, or allowing you to select your cookie preferences.
          </p>
        </div>

        <div style={fieldsetBox}>
          {tab === "semantics" && (
            <div style={{ position: "absolute", top: -8, left: 8, background: "#fff", padding: "0 4px" }}>
              {renderBadge("fieldset", 1)}
            </div>
          )}
          
          {/* Meta Platforms */}
          <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 6 }}>
            {renderBadge("h4", 5)}
            <h4 style={{ margin: 0, fontSize: 12, fontWeight: 600 }}>▾ Meta Platforms, Inc</h4>
            {tab === "focus" && renderBadge("button", 2)}
          </div>
          
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: 10, marginBottom: 8 }}>
            <thead>
              <tr style={{ borderBottom: "1px solid #E5E7EB", background: "#F9FAFB" }}>
                <th style={{ textAlign: "left", padding: 4 }}>Cookie name</th>
                <th style={{ textAlign: "left", padding: 4 }}>Description</th>
              </tr>
            </thead>
            <tbody>
              <tr style={{ borderBottom: "1px solid #E5E7EB" }}>
                <td style={{ padding: 4 }}>lastExternalReferrer1</td>
                <td style={{ padding: 4 }}>Detects how the user reached the website by registering their last URL-address.</td>
              </tr>
              <tr style={{ borderBottom: "1px solid #E5E7EB" }}>
                <td style={{ padding: 4 }}>lastExternalReferrer2</td>
                <td style={{ padding: 4 }}>Detects how the user reached the website by registering their last URL-address.</td>
              </tr>
            </tbody>
          </table>

          {/* Facebook */}
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {renderBadge("h4", 7)}
            <h4 style={{ margin: 0, fontSize: 12, fontWeight: 600 }}>▾ Facebook, Inc</h4>
            {tab === "focus" && renderBadge("button", 3)}
          </div>
        </div>
      </div>

      {/* Functional Cookies */}
      <div style={box}>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 6 }}>
          {renderBadge("h3", 8)}
          <h3 style={{ margin: 0, fontSize: 13, fontWeight: 700, flex: 1 }}>Functional Cookies</h3>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            {tab === "focus" && renderBadge("toggle", 4)}
            {toggle(false)}
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 8 }}>
          {renderBadge("note", 9)}
          <p style={{ margin: 0, fontSize: 11, color: "#4B5563", lineHeight: 1.3 }}>
            These Cookies allow us to analyze your use of the site to evaluate and improve its performance. They may also be used to provide a better customer experience on this site.
          </p>
        </div>

        <div style={fieldsetBox}>
          {tab === "semantics" && (
            <div style={{ position: "absolute", top: -8, left: 8, background: "#fff", padding: "0 4px" }}>
              {renderBadge("fieldset", 2)}
            </div>
          )}
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {renderBadge("h4", 10)}
            <h4 style={{ margin: 0, fontSize: 12, fontWeight: 600 }}>▾ Google Analytics</h4>
            {tab === "focus" && renderBadge("button", 5)}
          </div>
        </div>
      </div>

      {/* Advertising Cookies */}
      <div style={box}>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 6 }}>
          {renderBadge("h3", 11)}
          <h3 style={{ margin: 0, fontSize: 13, fontWeight: 700, flex: 1 }}>Advertising Cookies</h3>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            {tab === "focus" && renderBadge("toggle", 6)}
            {toggle(false)}
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 6, marginBottom: 8 }}>
          {renderBadge("note", 12)}
          <p style={{ margin: 0, fontSize: 11, color: "#4B5563", lineHeight: 1.3 }}>
            These Cookies are used to show you ads that are more relevant to you. We may share this information with advertisers or use it to understand your interests.
          </p>
        </div>

        <div style={fieldsetBox}>
          {tab === "semantics" && (
            <div style={{ position: "absolute", top: -8, left: 8, background: "#fff", padding: "0 4px" }}>
              {renderBadge("fieldset", 3)}
            </div>
          )}
          <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
            {renderBadge("h4", 13)}
            <h4 style={{ margin: 0, fontSize: 12, fontWeight: 600 }}>▾ Google Ads</h4>
            {tab === "focus" && renderBadge("button", 7)}
          </div>
        </div>
      </div>

      {/* Analytics and customization */}
      <div style={box}>
        <div style={{ display: "flex", alignItems: "center", gap: 6, marginBottom: 6 }}>
          {renderBadge("h3", 14)}
          <h3 style={{ margin: 0, fontSize: 13, fontWeight: 700, flex: 1 }}>Analytics and customization</h3>
          <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
            {tab === "focus" && renderBadge("toggle", 8)}
            {toggle(false)}
          </div>
        </div>
        <div style={{ display: "flex", alignItems: "flex-start", gap: 6 }}>
          {renderBadge("note", 15)}
          <p style={{ margin: 0, fontSize: 11, color: "#4B5563", lineHeight: 1.3 }}>
            These Cookies allow us to analyze your use of the site to evaluate and improve its performance. They may also be used to provide a better customer experience on this site.
          </p>
        </div>
      </div>
    </div>
  );
};

export default AccManagePreviewRight;
