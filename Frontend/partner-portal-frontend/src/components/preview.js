// src/components/Preview.js
import React from "react";
import PropTypes from "prop-types";
import "./preview.css";

/**
 * Preview component
 * Props:
 *  - title, body, actions (array), view ('desktop'|'mobile'), dark (bool)
 *  - onAction(id), onViewChange(view), onDarkChange(bool)
 */
export default function Preview({
  title = "This site uses cookies to make your experience better",
  body = `We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking "Accept", you agree to our website's cookie use as described in our cookie policy.`,
  actions = [
    { id: "prefs", label: "Manage preferences", variant: "secondary" },
    { id: "reject", label: "Reject all", variant: "secondary" },
    { id: "necessary", label: "Accept necessary cookies", variant: "secondary" },
    { id: "accept_all", label: "Accept all", variant: "primary" },
  ],
  view = "desktop",
  dark = false,
  onAction = () => {},
  onViewChange = () => {},
  onDarkChange = () => {},
}) {
  const isMobile = view === "mobile";
  const innerFrameClass = `pv-inner-frame ${dark ? "pv-dark" : ""} ${isMobile ? "pv-mobile" : "pv-desktop"}`;
  const bannerClass = `pv-banner ${dark ? "pv-banner-dark" : "pv-banner-light"}`;

  return (
    <div className="pv-canvas" role="region" aria-label="Preview canvas">
     

      <div className="pv-stage">
        <div className={innerFrameClass} aria-hidden>
          <div className="pv-inner-topbar">
            <span className="pv-dot pv-dot-1" />
            <span className="pv-dot pv-dot-2" />
            <span className="pv-dot pv-dot-3" />
          </div>

          <div className="pv-inner-content" />

          {/* Banner inside frame pinned to bottom */}
          <div className={bannerClass} role="region" aria-live="polite">
            <div className="pv-banner-text">
              <div className="pv-banner-title">{title}</div>
              <div className="pv-banner-body">{body}</div>
            </div>

            <div className="pv-banner-actions">
              <div style={{ flex: "1 1 auto" }} />
              {actions.map((a) => (
                <button
                  key={a.id}
                  className={`pv-btn ${a.variant === "primary" ? "pv-btn-primary" : "pv-btn-secondary"}`}
                  onClick={() => onAction(a.id)}
                  type="button"
                >
                  {a.label}
                </button>
              ))}
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}

Preview.propTypes = {
  title: PropTypes.string,
  body: PropTypes.string,
  actions: PropTypes.array,
  view: PropTypes.oneOf(["desktop", "mobile"]),
  dark: PropTypes.bool,
  onAction: PropTypes.func,
  onViewChange: PropTypes.func,
  onDarkChange: PropTypes.func,
};
