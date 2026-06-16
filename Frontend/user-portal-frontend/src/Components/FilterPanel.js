import React, { useState } from "react";
import "../Styles/FilterPanel.css";
import { textStyle } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import { FaFilter, FaPlus, FaTimes } from "react-icons/fa";
import { FiSearch } from "react-icons/fi";

const FilterPanel = ({
  open,
  onClose,
  onApply,
  onClear,
  showStatus = true,
}) => {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState([]);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");

  // Toggle status selection
  const toggleStatus = (value) => {
    setStatus((prev) =>
      prev.includes(value) ? prev.filter((s) => s !== value) : [...prev, value],
    );
  };

  // Clear all filters
  const handleClear = () => {
    setSearch("");
    setStatus([]);
    setStartDate("");
    setEndDate("");
    onClear?.(); // Inform parent
  };

  // Apply filter
  const handleApply = () => {
    onApply?.({
      search: search.toLowerCase().trim().replace(/\s+/g, " "),
      status,
      startDate,
      endDate,
    });
  };

  return (
    <>
      <div
        className="filter-overlay"
        style={{ display: open ? "block" : "none" }}
        onClick={onClose}
      />
      <div className={`filter-panel ${open ? "open" : ""}`}>
        {/* Header */}
        <div className="filter-header">
          <div style={{ display: "flex", alignItems: "center", gap: "0.5rem" }}>
            <FaFilter size={ICON_SIZE} />
            <span
              id="filterPanelTitle"
              style={textStyle("heading-xs", "primary-grey-100")}
            >
              Filters
            </span>
          </div>
          <button
            aria-label="Close filter panel"
            onClick={onClose}
            style={{
              background: "transparent",
              border: "none",
              cursor: "pointer",
              padding: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
            }}
          >
            <FaTimes size={ICON_SIZE} color="#6B7280" />
          </button>
        </div>

        {/* Body */}
        <div className="filter-body">
          {/* Search */}
          <div className="search-input-wrapper">
            <FiSearch size={ICON_SIZE} />
            <input
              type="text"
              className="search-input"
              placeholder="Search data item, type and purpose"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              aria-label="Search data item, type and purpose"
            />
          </div>

          {/* Status */}
          <span
            className="filter-label"
            style={textStyle("body-s-bold", "primary-grey-80")}
          >
            Status
          </span>

          {showStatus && (
            <div className="status-options">
              <div
                className={`status-box ${
                  status.includes("ACTIVE") ? "selected" : ""
                }`}
                aria-label={
                  status.includes("ACTIVE")
                    ? "Remove Active filter"
                    : "Add Active filter"
                }
                aria-pressed={status.includes("ACTIVE")}
                onClick={() => toggleStatus("ACTIVE")}
              >
                {status.includes("ACTIVE") ? (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <span style={textStyle("body-s", "primary-grey-100")}>
                      Active
                    </span>
                    <FaTimes size={ICON_SIZE} />
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <span style={textStyle("body-s", "primary-grey-100")}>
                      Active
                    </span>
                    <FaPlus size={ICON_SIZE} />
                  </div>
                )}
              </div>

              <div
                className={`status-box ${
                  status.includes("WITHDRAWN") ? "selected" : ""
                }`}
                onClick={() => toggleStatus("WITHDRAWN")}
                aria-label={
                  status.includes("WITHDRAWN")
                    ? "Remove Withdrawn filter"
                    : "Add Withdrawn filter"
                }
                aria-pressed={status.includes("WITHDRAWN")}
              >
                {status.includes("WITHDRAWN") ? (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <span style={textStyle("body-s", "primary-grey-100")}>
                      Withdrawn
                    </span>
                    <FaTimes size={ICON_SIZE} />
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <span style={textStyle("body-s", "primary-grey-100")}>
                      Withdrawn
                    </span>
                    <FaPlus size={ICON_SIZE} />
                  </div>
                )}
              </div>

              <div
                className={`status-box ${
                  status.includes("EXPIRED") ? "selected" : ""
                }`}
                onClick={() => toggleStatus("EXPIRED")}
                aria-label={
                  status.includes("EXPIRED")
                    ? "Remove Expired filter"
                    : "Add Expired filter"
                }
                aria-pressed={status.includes("EXPIRED")}
              >
                {status.includes("EXPIRED") ? (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <span style={textStyle("body-s", "primary-grey-100")}>
                      Expired
                    </span>
                    <FaTimes size={ICON_SIZE} />
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <span style={textStyle("body-s", "primary-grey-100")}>
                      Expired
                    </span>
                    <FaPlus size={ICON_SIZE} />
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Date Range */}
          <br />
          <span
            className="filter-label"
            style={textStyle("body-s-bold", "primary-grey-80")}
          >
            Created Date Range
          </span>

          <div className="date-range-row">
            <input
              type="date"
              className="date-input"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              onFocus={(e) => e.target.showPicker?.()}
              aria-label="Start Date"
            />
            <input
              type="date"
              className="date-input"
              aria-label="End Date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              onFocus={(e) => e.target.showPicker?.()}
            />
          </div>
        </div>

        {/* Footer */}
        <div className="filter-footer">
          <button
            onClick={handleClear}
            ariaLabel="Clear All Filter"
            style={{
              background: "white",
              color: "#2563eb",
              border: "1px solid rgb(224, 224, 224)",
              padding: "10px 20px",
              borderRadius: "999px",
              fontSize: "11px",
              fontWeight: "600",
              cursor: "pointer",
              transition: "0.2s",
            }}
          >
            Clear All
          </button>
          <button
            onClick={handleApply}
            aria-label="Apply Filters"
            style={{
              backgroundColor: "#2563eb",
              color: "#fff",
              border: "none",
              padding: "10px 20px",
              borderRadius: "999px",
              fontSize: "11px",
              fontWeight: "600",
              cursor: "pointer",
              transition: "0.2s",
            }}
          >
            Apply Filters
          </button>
        </div>
      </div>
    </>
  );
};

export default FilterPanel;
