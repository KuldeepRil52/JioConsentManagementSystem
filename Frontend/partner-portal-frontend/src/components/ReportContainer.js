// src/components/ReportContainer.js
import React, { useState, useEffect, useMemo } from "react";
import { Text, Icon } from "../custom-components";
import { IcEditPen } from "../custom-components/Icon";
import { IcEditPen, IcChevronDown, IcChevronUp } from "../custom-components/Icon";
import { formatToIST } from "../utils/dateUtils";

/* ===========================
   Styles
   =========================== */
const css = `
.page-container {
  display:flex;
  flex-direction:column;
  min-height:100vh;
  background:#F5F5F5;
  font-family: Inter, "Helvetica Neue", Arial, sans-serif;
  color:#111827;
}

.center-wrap { width:100%; }

.report-card {
  background:#fff;
  border-radius:12px;
  border:1px solid #F1F5F9;
  padding:18px;
  box-sizing:border-box;
  overflow:hidden;
}

/* -----------------------------------------
   CATEGORY HEADER WITH CHEVRON (LEFT SIDE)
----------------------------------------- */
.collapsed-row {
  height:48px;
  display:flex;
  align-items:center;
  gap:10px;
  padding:0 12px;
  cursor:pointer;
  border-bottom:1px solid #F3F4F6;
  transition:background 0.2s ease;
}

.collapsed-row:hover { background:#f9fafb; }

.chevron-icon {
  display:flex;
  align-items:center;
  justify-content:center;
  opacity:0.75;
  transition:opacity 0.2s ease;
}

.chevron-icon:hover { opacity:1; }

.category-label {
  font-size:14px;
  font-weight:600;
  color:#111827;
}

/* -----------------------------------------
   COLLAPSIBLE BODY (NO GAP WHEN CLOSED)
----------------------------------------- */
.section-body {
  max-height:0;
  opacity:0;
  padding:0;
  overflow:hidden;
  transition:
    max-height 0.35s ease,
    opacity 0.25s ease,
    padding 0.25s ease;
}

.section-body.open {
  opacity:1;
  padding-top:12px;
  max-height:380px;
  overflow-y:auto;
}

/* Scrollbar */
.section-body::-webkit-scrollbar { width:4px; }
.section-body::-webkit-scrollbar-thumb {
  background:#c7c7c7;
  border-radius:4px;
}
.section-body::-webkit-scrollbar-track { background:transparent; }

/* -----------------------------------------
   TABLE STYLING
----------------------------------------- */
.table-wrap {
  background:#fff;
  border-radius:6px;
  overflow:hidden;
  border:1px solid #EEF2F7;
  margin-bottom:12px;
}

.purpose-table {
  width:100%;
  border-collapse:collapse;
  table-layout:fixed;
}

.purpose-table thead th {
  background:#F8FAFC;
  color:#6B7280;
  text-align:left;
  padding:10px 14px;
  font-size:12px;
  font-weight:600;
  border-bottom:1px solid #EEF2F7;
}

.purpose-table thead th:nth-child(1) { width:15%; }
.purpose-table thead th:nth-child(2) { width:18%; }
.purpose-table thead th:nth-child(3) { width:40%; }
.purpose-table thead th:nth-child(4) { width:15%; }
.purpose-table thead th:nth-child(5) { width:12%; }

.purpose-table td {
  padding:14px;
  font-size:13px;
  color:#111827;
  border-bottom:1px solid #F5F6F7;
  word-break:break-word;
  overflow-wrap:break-word;
  white-space:normal;
}

.icon-btn {
  background:transparent;
  border:none;
  cursor:pointer;
  padding:6px;
  border-radius:6px;
}

.icon-btn:hover { background:rgba(10,60,201,0.06); }

/* -----------------------------------------
   VENDOR LABEL (Necessary only)
----------------------------------------- */
.purpose-vendor {
  font-size:13px;
  font-weight:600;
  margin:8px 0 6px 2px;
  color:#374151;
}

/* -----------------------------------------
   FLASH HIGHLIGHT
----------------------------------------- */
.flash {
  box-shadow:0 0 0 2px #0A3CCE33,
              0 0 6px 2px #0A3CCE66 inset;
  border-radius:8px;
  transition:box-shadow 0.3s ease;
}

/* Responsive */
@media (max-width:768px) {
  .section-body.open { max-height:260px; }
}
`;

/* Format Date */
function formatDate(dateValue) {
  if (!dateValue) return "—";
  try {
    const date = new Date(dateValue);
    if (isNaN(date.getTime())) return "—";
    if (date.getFullYear() < 1970) return "Session";
    const istFormatted = formatToIST(dateValue);
    if (istFormatted === "N/A") return "—";
    const datePart = istFormatted.split(" ")[0];
    return datePart.replace(/-/g, "/");
  } catch {
    return "—";
  }
}

/* Real Domain Helper */
function getRealDomain(c) {
  const d = c.domain?.toLowerCase();

  if (d === "first party" || d === "third party") {
    if (c.host) return c.host;

    if (c.url) {
      return c.url
        .replace(/https?:\/\//, "") // ← FIXED REGEX
        .split("/")[0];
    }

    return "Unknown Domain";
  }

  return c.domain || c.host || "Unknown Domain";
}


const Table = ({ rows = [], onEdit }) => (
  <div className="table-wrap">
    <table className="purpose-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Domain</th>
          <th>Description</th>
          <th>Expires</th>
          <th>Action</th>
        </tr>
      </thead>

      <tbody>
        {rows.length === 0 ? (
          <tr>
            <td colSpan="5" style={{ textAlign: "center", padding: 14 }}>
              No cookies found
            </td>
          </tr>
        ) : (
          rows.map((c, i) => (
            <tr key={i}>
              <td>{c.name}</td>
              <td>{getRealDomain(c)}</td>
              <td>{c.description || "—"}</td>
              <td>{formatDate(c.expires)}</td>
              <td>
                <button className="icon-btn" onClick={() => onEdit(c)}>
                  <Icon ic={<IcEditPen />} size="medium" />
                </button>
              </td>
            </tr>
          ))
        )}
      </tbody>
    </table>
  </div>
);

export default function ReportContainer({
  initialData = null,
  onEdit = null,
  highlightCategory = null,
  apiCategories = [],
}) {
  const [data, setData] = useState(initialData || null);

  const [open, setOpen] = useState({
    necessary: true,
    functional: false,
    analytics: false,
    advertisement: false,
    others: false,
  });

  /* Sync initial data */
  useEffect(() => {
    if (initialData) setData(initialData);
  }, [initialData]);

  /* Highlight category on navigate */
  useEffect(() => {
    if (!highlightCategory) return;
    const key = highlightCategory.toLowerCase();
    setOpen((prev) => ({ ...prev, [key]: true }));
    const el = document.querySelector(`#cat-${key}`);
    if (el) {
      el.scrollIntoView({ behavior: "smooth", block: "center" });
      el.classList.add("flash");
      setTimeout(() => el.classList.remove("flash"), 2000);
    }
  }, [highlightCategory]);

  /* Standard category names (lowercased) – used to filter API categories */
  const STANDARD_CATS = useMemo(() => new Set([
    "necessary", "required", "essential",
    "functional",
    "analytics",
    "advertisement", "advertising",
    "others", "unclassified",
  ]), []);

  /* Build dynamic category entries from scan data + API categories list */
  const dynamicEntries = useMemo(() => {
    const entries = {};

    // 1. Add categories that already have cookies from the scan
    if (data?.dynamicCategories) {
      Object.entries(data.dynamicCategories).forEach(([label, arr]) => {
        const key = label.toLowerCase().replace(/\s+/g, "_").trim();
        entries[key] = { key, label: label.trim(), rows: arr || [] };
      });
    }

    // 2. Add custom categories from the API that are NOT one of the 5 standard ones
    if (Array.isArray(apiCategories)) {
      apiCategories.forEach((cat) => {
        const catName = (cat.category || "").trim();
        if (!catName) return;
        const catLower = catName.toLowerCase().trim();
        if (STANDARD_CATS.has(catLower)) return; // skip standard categories
        const key = catLower.replace(/\s+/g, "_");
        if (!entries[key]) {
          // Category from API with no cookies yet – show as empty section
          entries[key] = { key, label: catName, rows: [] };
        }
      });
    }

    return Object.values(entries);
  }, [data, apiCategories, STANDARD_CATS]);

  /* Count per category */
  const counts = useMemo(() => {
    const result = {
      necessary: Object.values(data?.required || {}).reduce(
        (s, arr) => s + (Array.isArray(arr) ? arr.length : 0),
        0
      ),
      functional: (data?.functional || []).length,
      analytics: (data?.analytics || []).length,
      advertisement: (data?.advertising || []).length,
      others: (data?.unclassified || []).length,
    };
    dynamicEntries.forEach(({ key, rows }) => {
      result[key] = (rows || []).length;
    });
    return result;
  }, [data, dynamicEntries]);

  /* All category sections: standard + dynamic */
  const allSections = useMemo(() => [
    { key: "necessary", label: "Necessary", rows: data?.required || {} },
    { key: "functional", label: "Functional", rows: data?.functional || [] },
    { key: "analytics", label: "Analytics", rows: data?.analytics || [] },
    { key: "advertisement", label: "Advertisement", rows: data?.advertising || [] },
    ...dynamicEntries,
    { key: "others", label: "Others", rows: data?.unclassified || [] },
  ], [data, dynamicEntries]);

  const toggle = (k) => setOpen((prev) => ({ ...prev, [k]: !prev[k] }));

  const handleEdit = (cookie) => onEdit && onEdit(cookie);

  return (
    <div className="page-container">
      <style>{css}</style>

      <div className="center-wrap">
        <div className="report-card">
          <div className="purpose-con">
            {allSections.map(({ key, label, rows }) => (
              <div key={key} id={`cat-${key}`} className="category-section">

                {/* Header */}
                <div className="collapsed-row" onClick={() => toggle(key)}>
                  <div className="chevron-icon">
                    <Icon
                      ic={open[key] ? <IcChevronUp /> : <IcChevronDown />}
                      size="small"
                    />
                  </div>
                  <div className="category-label">
                    {label} ({counts[key] || 0})
                  </div>
                </div>

                {/* Body */}
                <div className={`section-body ${open[key] ? "open" : ""}`}>
                  {key === "necessary" && Object.keys(rows).length > 0 ? (
                    Object.entries(rows).map(([vendor, cookies], i) => (
                      <div key={i}>
                        <div className="purpose-vendor">
                          {cookies[0]?.domain || vendor}
                        </div>
                        <Table rows={cookies} onEdit={handleEdit} />
                      </div>
                    ))
                  ) : (
                    <Table
                      rows={Array.isArray(rows) ? rows : []}
                      onEdit={handleEdit}
                    />
                  )}
                </div>

              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
