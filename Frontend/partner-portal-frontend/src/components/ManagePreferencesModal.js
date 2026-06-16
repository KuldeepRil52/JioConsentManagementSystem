// src/components/ManagePreferencesModal.js
import React, { useState, useMemo, useEffect } from "react";
import {
  Text,
  InputToggle,
  ActionButton,
  Icon,
  
} from '../custom-components';

export default function ManagePreferencesModal({
  dark = false,
  darkMode = false,
  colors = {},
  darkColors = {},
  scanData = null,
  onClose,
  selectedLanguage = "ENGLISH",
  translations = {},
  fontStyles = { family: "JioType, sans-serif", size: "14px", weight: "400", style: "normal" },
}) {
  const [open, setOpen] = useState({
    required: true,
    functional: false,
    analytics: false,
    advertising: false,
    others: false,
  });

  // 🎨 Sync live branding colors
  const [theme, setTheme] = useState(darkMode || dark ? darkColors : colors);
  useEffect(() => {
    setTheme(darkMode || dark ? darkColors : colors);
  }, [colors, darkColors, darkMode, dark]);

  // 🌈 Centralized computed color palette
  const computedColors = {
    text: theme.cardFont || "#111827",
    subText: theme.cardFont ? `${theme.cardFont}CC` : "#4B5563", // lighter
    border: theme.linkFont
      ? `${theme.linkFont}33`
      : dark
      ? "rgba(255,255,255,0.1)"
      : "#E5E7EB",
    tableHeader: darkMode || dark ? "#0B1120" : "#F8FAFC",
    background: theme.cardBackground || (dark ? "#071026" : "#FFFFFF"),
    buttonBackground: theme.buttonBackground || "#0F3CC9",
    buttonFont: theme.buttonFont || "#FFFFFF",
    linkFont: theme.linkFont || "#0A2885",
  };

  // 🗣️ Get language-specific data
  const activeLang = selectedLanguage?.toUpperCase() || "ENGLISH";
  const langData = translations?.[activeLang] || {};
  const translatedCookies = langData?.scanTranslations || {};
  
  // ✅ Get translated title/description (support both pre-publish and post-publish formats)
  const translatedTitle = langData.managePrefTitle || langData.rightsText || "";
  const translatedDescription = langData.managePrefDescription || langData.permissionText || "";
  
  // ✅ Get translated category labels (if available)
  const categoryLabels = langData.categoryLabels || {};
  const categoryDescs = langData.categoryDescriptions || {};

  // 🧠 Group cookies from scanData by category
  const categories = useMemo(() => {
    const grouped = {
      required: {},
      functional: {},
      analytics: {},
      advertising: {},
      others: {},
    };
    if (!scanData?.subdomains) return grouped;

    scanData.subdomains.forEach((sub) => {
      sub.cookies.forEach((cookie) => {
        const cat = (cookie.category || "others").toLowerCase();
        let type;
        if (cat === "necessary" || cat === "required") type = "required";
        else if (cat === "functional") type = "functional";
        else if (cat === "analytics") type = "analytics";
        else if (
  cat === "advertising" ||
  cat === "advertisement" ||
  cat === "ads" ||
  cat === "ad" ||
  cat.includes("marketing")
) {
  type = "advertising";
}

        else type = "others";

        const vendor =
          cookie.domain?.replace(/^(\.)?/, "") ||
          sub.subdomainName ||
          "Unknown";
        if (!grouped[type][vendor]) grouped[type][vendor] = [];
        grouped[type][vendor].push(cookie);
      });
    });
    return grouped;
  }, [scanData]);

  const toggleSection = (key) =>
    setOpen((prev) => ({ ...prev, [key]: !prev[key] }));

  // 🧾 Table view for each cookie list
  const renderCookieTable = (rows) => (
   <div
    style={{
      maxHeight: 160,                // fixed scroll area height
      overflowY: "auto",             // always scrollable
      border: `1px solid ${computedColors.border}`,
      borderRadius: 6,
      marginBottom: 8,
      scrollbarWidth: "thin",
      scrollbarColor: `${computedColors.border} transparent`,
    }}
>

      <table
        style={{
          width: "100%",
          borderCollapse: "collapse",
          minWidth: "100%",
        }}
      >
        <thead>
          <tr
            style={{
              background: computedColors.tableHeader,
              color: computedColors.linkFont,
              textAlign: "left",
            }}
          >
            <th
              style={{
                padding: "10px 8px",
                fontSize: 13,
                fontWeight: 600,
                borderBottom: `1px solid ${computedColors.border}`,
                width: "40%",
              }}
            >
              Cookie name
            </th>
            <th
              style={{
                padding: "10px 8px",
                fontSize: 13,
                fontWeight: 600,
                borderBottom: `1px solid ${computedColors.border}`,
                width: "60%",
              }}
            >
              Description
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              <td
                style={{
                  padding: "10px 8px",
                  fontSize: 13,
                  color: computedColors.text,
                  borderBottom: `1px solid ${computedColors.border}`,
                }}
              >
                {translatedCookies[row.name]?.name || row.name}
              </td>
              <td
                style={{
                  padding: "10px 8px",
                  fontSize: 13,
                  color: computedColors.subText,
                  borderBottom: `1px solid ${computedColors.border}`,
                }}
              >
                {translatedCookies[row.name]?.description ||
                  row.description_gpt ||
                  row.description ||
                  "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );

  // 🔹 Category Accordion Section
  const renderCategory = (id, title, defaultDescription, lockToggle = false) => {
    const vendors = categories[id];
    const hasCookies =
      vendors && Object.values(vendors).some((arr) => arr.length > 0);

    const translatedDesc =
      categoryDescs[id] || langData[`desc_${id}`] || langData.categoryDescriptions?.[id];

    return (
      <div style={{ marginBottom: 20 }}>
        {/* Header */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 6,
            cursor: "pointer",
          }}
          onClick={() => toggleSection(id)}
        >
          <Text
            appearance="body-m-bold"
            style={{
              color: computedColors.text,
            }}
          >
            {title}
          </Text>
          <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
            <InputToggle
              label=""
              type="toggle"
              size="small"
              checked={id === "required"}
              disabled={lockToggle}
              style={{ width: 44, height: 24 }}
            />
            <Icon
            name="chevron-down"
              size={18}
              color={computedColors.text}
              style={{
                transform: open[id] ? "rotate(180deg)" : "rotate(0deg)",
                transition: "transform 0.3s ease",
              }}
            />
          </div>
        </div>

        {/* Description */}
        <Text
          appearance="body-s"
          style={{
            color: computedColors.subText,
          }}
        >
          {translatedDesc || defaultDescription}
        </Text>

        {/* Vendors + Cookies */}
        {open[id] &&
          (hasCookies ? (
            Object.entries(vendors).map(([vendor, list], i) => (
              <div key={i} style={{ marginBottom: 12 }}>
                <Text
                  appearance="body-s-bold"
                  style={{ color: computedColors.text }}
                >
                  {vendor}
                </Text>
                {list.length > 0 && renderCookieTable(list)}
              </div>
            ))
          ) : (
            <Text
              style={{
                fontSize: 13,
                color: computedColors.subText,
                paddingLeft: 6,
              }}
            >
              No cookies found in this category.
            </Text>
          ))}
      </div>
    );
  };

  // 🧩 Final Render
  return (
    <div
  style={{
    width: 584,
    maxHeight: "90vh",
    background: computedColors.background,
    borderRadius: 32,
    border: `1px solid ${computedColors.border}`,
    boxShadow: "0 8px 24px rgba(0,0,0,0.1)",
    position: "relative",
    overflow: "hidden",
    fontFamily: fontStyles.family,
    fontSize: fontStyles.size,
    fontWeight: fontStyles.weight,
    fontStyle: fontStyles.style,
    color: computedColors.text,
    transition: "all 0.3s ease",
    display: "flex",
    flexDirection: "column",
  }}
>
      <style>
        {`
          .manage-prefs-modal, .manage-prefs-modal * {
            font-family: ${fontStyles.family} !important;
            font-size: ${fontStyles.size} !important;
            font-weight: ${fontStyles.weight} !important;
            font-style: ${fontStyles.style} !important;
          }
          .manage-prefs-modal button, 
          .manage-prefs-modal input, 
          .manage-prefs-modal select, 
          .manage-prefs-modal textarea {
            font-family: inherit !important;
            font-size: inherit !important;
            font-weight: inherit !important;
            font-style: inherit !important;
          }
        `}
      </style>

      {/* ❌ Close Button */}
      <button
        onClick={onClose}
        style={{
          position: "absolute",
          top: 16,
          right: 20,
          fontSize: 22,
          background: "transparent",
          border: "none",
          color: computedColors.subText,
          cursor: "pointer",
        }}
      >
        ×
      </button>

      {/* Header */}
      <div
  style={{
    padding: "24px 24px 16px",
    display: "flex",
    flexDirection: "column",
    alignItems: "flex-start",
    gap: "4px",
  }}
>
  <Text
    appearance="heading-xs"
    style={{
      color: computedColors.text,
      marginBottom: 4,
    }}
  >
    {translatedTitle || "Manage preferences"}
  </Text>
  <Text
    appearance="body-s"
    style={{
      color: computedColors.subText,
      lineHeight: "20px",
    }}
  >
    {translatedDescription ||
      "Choose the cookies you allow. You can update your preferences anytime."}
  </Text>
</div>


      {/* Scrollable Body */}
<div
  style={{
    flex: 1,
    overflowY: "auto",
    padding: "0 24px",
    paddingBottom: "90px",
  }}
>

        {renderCategory(
          "required",
          categoryLabels.required || "Required Cookies",
          "These cookies are necessary to enable basic site features.",
          true
        )}
        {renderCategory(
          "functional",
          categoryLabels.functional || "Functional Cookies",
          "These cookies improve site performance and personalization."
        )}
        {renderCategory(
          "analytics",
          categoryLabels.analytics || "Analytics Cookies",
          "These cookies help measure site usage and improve our services."
        )}
        {renderCategory(
          "advertising",
          categoryLabels.advertising || "Advertising Cookies",
          "These cookies are used to show relevant ads and measure campaign effectiveness."
        )}
        {renderCategory(
          "others",
          categoryLabels.others || "Other Cookies",
          "These cookies have not yet been classified."
        )}
      </div>

      {/* Footer */}
      <div
        style={{
          position: "absolute",
          bottom: 0,
          width: "100%",
          padding: "16px 24px",
          background: computedColors.background,
          display: "flex",
          justifyContent: "flex-end",
          gap: 12,
        }}
      >
        
    
        <ActionButton
          kind="primary"
          size="medium"
          label={langData.saveButtonText || langData.managePrefs || "Save preferences"}
          onClick={() => alert("Preferences saved")}
          style={{
            background: computedColors.buttonBackground,
            color: computedColors.buttonFont,
          }}
        />
      </div>
    </div>
  );
}
