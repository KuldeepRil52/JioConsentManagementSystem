// src/components/Branding.js
import React, { useState, useEffect } from "react";
import { Text, InputToggle, ActionButton } from "../custom-components";
import "../styles/branding.css";

const DEFAULT_LIGHT_COLORS = {
  cardBackground: "#FFFFFF",
  cardFont: "#111827",
  buttonBackground: "#0F3CC9",
  buttonFont: "#FFFFFF",
  linkFont: "#0A2885",
};

const DEFAULT_DARK_COLORS = {
  cardBackground: "#071026",
  cardFont: "#E6EEF5",
  buttonBackground: "#0F3CC9",
  buttonFont: "#FFFFFF",
  linkFont: "#6AB0FF",
};

export default function BrandingTab({
  colors,
  setColors,
  darkColors,
  setDarkColors,
  darkMode,
  setDarkMode,
  consentBanner,
  setConsentBanner,
  declineBtn,
  setDeclineBtn,
  managePrefsBtn,
  setManagePrefsBtn,
  acceptAllBtn,
  setAcceptAllBtn,
  acceptNecessaryBtn,
  setAcceptNecessaryBtn,
  onOpenManagePreferences,
  activePreview,
  setActivePreview,

  // 👇 preferences states are now received from parent
  prefColors,
  setPrefColors,
  prefDarkColors,
  setPrefDarkColors,
  prefDarkMode,
  setPrefDarkMode,
}) {
  const [activeSubTab, setActiveSubTab] = useState(
    sessionStorage.getItem("branding_tab_sub") || "cookie"
  );

  // ⏺ On mount — restore last active tab and control preview panel
  useEffect(() => {
    const savedTab = sessionStorage.getItem("branding_tab_sub") || "cookie";
    setActiveSubTab(savedTab);
    const isPref = savedTab === "preferences";
    onOpenManagePreferences?.(isPref);
    setActivePreview?.(isPref ? "preferences" : "cookie");
  }, [onOpenManagePreferences, setActivePreview]);

  // 💾 Persist cookie tab colors
  useEffect(() => {
    sessionStorage.setItem("brandingColors", JSON.stringify(colors));
    sessionStorage.setItem("brandingDarkColors", JSON.stringify(darkColors));
  }, [colors, darkColors]);

  // 💾 Persist preferences tab colors
  useEffect(() => {
    sessionStorage.setItem("brandingPrefColors", JSON.stringify(prefColors));
    sessionStorage.setItem("brandingPrefDarkColors", JSON.stringify(prefDarkColors));
    sessionStorage.setItem("brandingPrefDarkMode", JSON.stringify(prefDarkMode));
  }, [prefColors, prefDarkColors, prefDarkMode]);

  // 🧹 Reset everything
  const handleCancel = () => {
    setColors({ ...DEFAULT_LIGHT_COLORS });
    setDarkColors({ ...DEFAULT_DARK_COLORS });
    setPrefColors({ ...DEFAULT_LIGHT_COLORS });
    setPrefDarkColors({ ...DEFAULT_DARK_COLORS });
    setDarkMode(false);
    setPrefDarkMode(false);
    setConsentBanner(true);
    setDeclineBtn(true);
    setManagePrefsBtn(true);
    setAcceptAllBtn(true);
    setAcceptNecessaryBtn(true);
    sessionStorage.clear();
  };

  // 💾 Save explicitly
  const handleSave = () => {
    sessionStorage.setItem("brandingColors", JSON.stringify(colors));
    sessionStorage.setItem("brandingDarkColors", JSON.stringify(darkColors));
    sessionStorage.setItem("brandingPrefColors", JSON.stringify(prefColors));
    sessionStorage.setItem("brandingPrefDarkColors", JSON.stringify(prefDarkColors));
    sessionStorage.setItem("brandingPrefDarkMode", JSON.stringify(prefDarkMode));

  };

  const colorInput = (label, value, onChange) => (
    <div style={{ marginBottom: 12 }}>
      <Text appearance="body-xs" color="primary-grey-80">
        {label}
      </Text>
      <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
        <input
          type="color"
          value={value}
          onChange={onChange}
          style={{ width: 36, height: 36, border: "none", cursor: "pointer" }}
        />
        <input
          type="text"
          value={value}
          readOnly
          style={{
            flex: 1,
            height: 40,
            borderRadius: 8,
            border: "1px solid #E5E7EB",
            padding: "8px 12px",
            background: "#fff",
            fontSize: 13,
          }}
        />
      </div>
    </div>
  );

  const ColorThemeSection = ({ isDark, setDark, light, setLight, dark, setDarkColors }) => (
    <>
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginBottom: 18 }}>
        <div>
          {colorInput("Card background", light.cardBackground, (e) =>
            setLight((p) => ({ ...p, cardBackground: e.target.value }))
          )}
          {colorInput("Button background", light.buttonBackground, (e) =>
            setLight((p) => ({ ...p, buttonBackground: e.target.value }))
          )}
          {colorInput("Link font", light.linkFont, (e) =>
            setLight((p) => ({ ...p, linkFont: e.target.value }))
          )}
        </div>
        <div>
          {colorInput("Card font", light.cardFont, (e) =>
            setLight((p) => ({ ...p, cardFont: e.target.value }))
          )}
          {colorInput("Button font", light.buttonFont, (e) =>
            setLight((p) => ({ ...p, buttonFont: e.target.value }))
          )}
        </div>
      </div>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        <Text appearance="heading-xxs" color="primary-grey-80">
          Theme (Dark mode)
        </Text>
        <InputToggle
          checked={!!isDark}
          onChange={() => setDark((p) => !p)}
          size="medium"
          type="toggle"
        />
      </div>

      {isDark && (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginTop: 20 }}>
          <div>
            {colorInput("Card background (Dark)", dark.cardBackground, (e) =>
              setDarkColors((p) => ({ ...p, cardBackground: e.target.value }))
            )}
            {colorInput("Button background (Dark)", dark.buttonBackground, (e) =>
              setDarkColors((p) => ({ ...p, buttonBackground: e.target.value }))
            )}
            {colorInput("Link font (Dark)", dark.linkFont, (e) =>
              setDarkColors((p) => ({ ...p, linkFont: e.target.value }))
            )}
          </div>
          <div>
            {colorInput("Card font (Dark)", dark.cardFont, (e) =>
              setDarkColors((p) => ({ ...p, cardFont: e.target.value }))
            )}
            {colorInput("Button font (Dark)", dark.buttonFont, (e) =>
              setDarkColors((p) => ({ ...p, buttonFont: e.target.value }))
            )}
          </div>
        </div>
      )}
    </>
  );

  return (
    <div style={{ padding: 20, background: "#fff", borderRadius: 12 }}>
      {/* Sub Tabs */}
      <div style={{ display: "flex", gap: 32, marginBottom: 30 }}>
        {[
          { key: "cookie", label: "Cookie" },
          { key: "preferences", label: "Manage preferences" },
        ].map((tab) => (
          <button
            key={tab.key}
            onClick={() => {
              setActiveSubTab(tab.key);
              sessionStorage.setItem("branding_tab_sub", tab.key);
              const isPref = tab.key === "preferences";
              onOpenManagePreferences?.(isPref);
              setActivePreview?.(isPref ? "preferences" : "cookie");
            }}
            style={{
              padding: "10px 20px",
              border: "none",
              borderBottom:
                activeSubTab === tab.key
                  ? "3px solid #0052CC"
                  : "3px solid transparent",
              background: "none",
              cursor: "pointer",
              fontWeight: activeSubTab === tab.key ? 600 : 500,
            }}
          >
            <Text
              appearance="body_s"
              color={
                activeSubTab === tab.key
                  ? "primary-brand"
                  : "primary-grey-100"
              }
            >
              {tab.label}
            </Text>
          </button>
        ))}
      </div>

      {/* COOKIE SECTION */}
      {activeSubTab === "cookie" && (
        <>
          <ColorThemeSection
            isDark={darkMode}
            setDark={setDarkMode}
            light={colors}
            setLight={setColors}
            dark={darkColors}
            setDarkColors={setDarkColors}
          />
          <Text appearance="heading-xxs" color="primary-grey-80" style={{ marginTop: 20 }}>
            Cookie Buttons
          </Text>
          {[
            { label: "Decline button", state: declineBtn, setter: setDeclineBtn },
            { label: "Accept all button", state: acceptAllBtn, setter: setAcceptAllBtn },
            { label: "Accept necessary button", state: acceptNecessaryBtn, setter: setAcceptNecessaryBtn },
          ].map((opt) => (
            <div key={opt.label} style={{ marginTop: 14 }}>
              <Text appearance="body-s" color="primary-grey-80">{opt.label}?</Text>
              <div style={{ display: "flex", gap: 16, marginTop: 6 }}>
                <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <input type="radio" checked={opt.state === true} onChange={() => opt.setter(true)} />
                  <span style={{ color: "#0A3CCE", fontWeight: 600 }}>Yes</span>
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <input type="radio" checked={opt.state === false} onChange={() => opt.setter(false)} />
                  <span>No</span>
                </label>
              </div>
            </div>
          ))}
        </>
      )}

      {/* MANAGE PREFERENCES SECTION */}
      {activeSubTab === "preferences" && (
        <>
          <ColorThemeSection
            isDark={prefDarkMode}
            setDark={setPrefDarkMode}
            light={prefColors}
            setLight={setPrefColors}
            dark={prefDarkColors}
            setDarkColors={setPrefDarkColors}
          />
          <Text appearance="heading-xxs" color="primary-grey-80" style={{ marginTop: 20 }}>
            Manage Preferences Buttons
          </Text>
          {[
            { label: "Enable consent banner", state: consentBanner, setter: setConsentBanner },
            { label: "Manage preferences button", state: managePrefsBtn, setter: setManagePrefsBtn },
          ].map((opt) => (
            <div key={opt.label} style={{ marginTop: 14 }}>
              <Text appearance="body-s" color="primary-grey-80">{opt.label}?</Text>
              <div style={{ display: "flex", gap: 16, marginTop: 6 }}>
                <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <input type="radio" checked={opt.state === true} onChange={() => opt.setter(true)} />
                  <span style={{ color: "#0A3CCE", fontWeight: 600 }}>Yes</span>
                </label>
                <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                  <input type="radio" checked={opt.state === false} onChange={() => opt.setter(false)} />
                  <span>No</span>
                </label>
              </div>
            </div>
          ))}
        </>
      )}

      {/* Footer */}
      <div style={{ marginTop: 24, display: "flex", gap: 12 }}>
        <ActionButton kind="primary" size="small" label="Save" onClick={handleSave} />
        <ActionButton kind="secondary" size="small" label="Cancel" onClick={handleCancel} />
      </div>
    </div>
  );
}
