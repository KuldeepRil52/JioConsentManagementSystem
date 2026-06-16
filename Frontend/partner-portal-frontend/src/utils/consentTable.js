import { ActionButton, Text, Icon, BadgeV2 } from "../custom-components";
import { IcEditPen, IcSort, IcSwap, IcSmartSwitchPlug, IcWhatsapp } from "../custom-components/Icon";
import { useNavigate, Link } from "react-router-dom";
import "../styles/EmailTemplate.css";
import "../styles/ConsentLogs.css";
import React from "react";

const ConsentTable = ({ consentData = [] }) => {
  const navigate = useNavigate();

  // Helper function to extract data from consent object
  const extractConsentInfo = (consent) => {
    const templateName = consent.templateName || "N/A";
    const templateId = consent.templateId || "N/A";
    const startDate = consent.startDate || "N/A";
    const endDate = consent.endDate || "N/A";
    
    // Extract data items
    const dataItems = [];
    if (consent.preferences) {
      consent.preferences.forEach((pref) => {
        if (pref.processorActivityList) {
          pref.processorActivityList.forEach((activity) => {
            if (activity.processActivityInfo?.dataTypesList) {
              activity.processActivityInfo.dataTypesList.forEach((dataType) => {
                if (dataType.dataItems && Array.isArray(dataType.dataItems)) {
                  dataItems.push(...dataType.dataItems);
                }
              });
            }
          });
        }
      });
    }
    const dataItemsStr = [...new Set(dataItems)].join(", ") || "N/A";
    
    // Extract data types (use dataTypeName instead of dataType)
    const dataTypes = [];
    if (consent.preferences) {
      consent.preferences.forEach((pref) => {
        if (pref.processorActivityList) {
          pref.processorActivityList.forEach((activity) => {
            if (activity.processActivityInfo?.dataTypesList) {
              activity.processActivityInfo.dataTypesList.forEach((dataType) => {
                if (dataType.dataTypeName) {
                  dataTypes.push(dataType.dataTypeName);
                }
              });
            }
          });
        }
      });
    }
    const dataTypesStr = [...new Set(dataTypes)].join(", ") || "N/A";
    
    // Extract purposes
    const purposes = [];
    if (consent.preferences) {
      consent.preferences.forEach((pref) => {
        if (pref.purposeList) {
          pref.purposeList.forEach((purpose) => {
            if (purpose.purposeInfo?.purposeName) {
              purposes.push(purpose.purposeInfo.purposeName);
            }
          });
        }
      });
    }
    const purposesStr = [...new Set(purposes)].join(", ") || "N/A";
    
    const status = consent.status || "N/A";
    
    // Extract customer identifier
    const customerIdentifier = consent.customerIdentifiers 
      ? `${consent.customerIdentifiers.type}: ${consent.customerIdentifiers.value}`
      : "N/A";
    
    // Format dates
    const formatDate = (dateStr) => {
      if (dateStr === "N/A" || !dateStr) return "N/A";
      try {
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-US', { 
          year: 'numeric', 
          month: 'short', 
          day: 'numeric' 
        });
      } catch (e) {
        return dateStr;
      }
    };
    
    return {
      templateName,
      customerIdentifier,
      startDate: formatDate(startDate),
      endDate: formatDate(endDate),
      dataItems: dataItemsStr,
      dataTypes: dataTypesStr,
      purpose: purposesStr,
      status,
    };
  };

  const tableHeaders = [
    "Template Name",
    "Customer Identifier",
    "Start Date",
    "End Date",
    "Data Items",
    "Data Types",
    "Purpose",
    "Status",
  ];

  const tableRows = Array.isArray(consentData) 
    ? consentData.map((consent) => extractConsentInfo(consent))
    : [];

  return (
    <div style={{ marginTop: "12px" }}>
      <table className="consentLog-table">
        <thead>
          <tr>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Customer Identifier
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Start Date
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  End Date
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Data Items
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Data Types
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Purpose
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
            <th className="parentheader">
              <div className="header-with-icon">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Status
                </Text>
                <Icon ic={<IcSort />} color="primary_60" size="small" />
              </div>
            </th>
          </tr>
        </thead>
        <tbody>
          {tableRows.length > 0 ? (
            tableRows.map((row, index) => (
              <tr key={index}>
                <td>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    {row.customerIdentifier}
                  </Text>
                </td>
                <td>
                  <Text appearance="body-xs" color="primary-grey-80">
                    {row.startDate}
                  </Text>
                </td>
                <td>
                  <Text appearance="body-xs" color="primary-grey-80">
                    {row.endDate}
                  </Text>
                </td>
                <td>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    {row.dataItems}
                  </Text>
                </td>
                <td>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    {row.dataTypes}
                  </Text>
                </td>
                <td>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    {row.purpose}
                  </Text>
                </td>
                <td>
                  <div className="activebadge">
                    <p className="activebadge-text">{row.status}</p>
                  </div>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan="7" style={{ textAlign: "center", padding: "1rem" }}>
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  No consent data available
                </Text>
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
};

export default ConsentTable;
