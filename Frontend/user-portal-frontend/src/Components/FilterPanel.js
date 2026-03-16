import React, { useState } from "react";
import "../Styles/FilterPanel.css";
import { ActionButton, Icon, Text } from "@jds/core";
import { IcAdd, IcClose, IcFilter, IcSearch } from "@jds/core-icons";

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
      prev.includes(value) ? prev.filter((s) => s !== value) : [...prev, value]
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
            <Icon size="m" ic={<IcFilter />} color="primary_grey_100" />
            <Text
              appearance="heading-xs"
              color="primary_grey_100"
              id="filterPanelTitle"
            >
              Filters
            </Text>
          </div>
          <ActionButton
            ariaLabel="Close filter panel"
            onClick={onClose}
            kind="tertiary"
            icon={<Icon ic={<IcClose />} size="l" color="primary_grey_100" />}
            size="medium"
          />
        </div>

        {/* Body */}
        <div className="filter-body">
          {/* Search */}
          <div className="search-input-wrapper">
            <Icon ic={<IcSearch />} size="m" color="primary_grey_100" />
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
          <Text
            appearance="body-s-bold"
            color="primary_grey_80"
            className="filter-label"
          >
            Status
          </Text>

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
                    <Text appearance="body-s" color="primary_grey_100">
                      Active
                    </Text>
                    <IcClose height={20} width={20}></IcClose>
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <Text appearance="body-s" color="primary_grey_100">
                      Active
                    </Text>
                    <IcAdd height={20} width={20}></IcAdd>
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
                    <Text appearance="body-s" color="primary_grey_100">
                      Withdrawn
                    </Text>
                    <IcClose height={20} width={20}></IcClose>
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <Text appearance="body-s" color="primary_grey_100">
                      Withdrawn
                    </Text>
                    <IcAdd height={20} width={20}></IcAdd>
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
                    <Text appearance="body-s" color="primary_grey_100">
                      Expired
                    </Text>
                    <IcClose height={20} width={20}></IcClose>
                  </div>
                ) : (
                  <div
                    style={{
                      display: "flex",
                      gap: "0.5rem",
                      alignItems: "center",
                    }}
                  >
                    <Text appearance="body-s" color="primary_grey_100">
                      Expired
                    </Text>
                    <IcAdd height={20} width={20}></IcAdd>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Date Range */}
          <br />
          <Text
            appearance="body-s-bold"
            color="primary_grey_80"
            className="filter-label"
          >
            Created Date Range
          </Text>

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
          <ActionButton
            kind="secondary"
            label="Clear All"
            onClick={handleClear}
            ariaLabel="Clear All Filter"
          />
          <ActionButton
            kind="primary"
            label="Apply"
            onClick={handleApply}
            ariaLabel="Apply filters"
          />
        </div>
      </div>
    </>
  );
};

export default FilterPanel;
