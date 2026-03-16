import React, { useEffect } from "react";
import "../styles/ConsentLogs.css";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Spinner, Text } from "../custom-components";
import { IcDownloads } from '../custom-components/Icon';
import {
  IcError,
  IcSuccessColored,
  IcWarningColored,
  IcSort,
  IcChevronDown,
  IcChevronUp,
} from "../custom-components/Icon";
import { Icon } from "../custom-components";
import {
  IcArrowDown,
  IcArrowUp,
  IcRefresh,
  IcTimezone,
  IcUpdate,
} from "../custom-components/Icon";
import { useState } from "react";
import {
  getConsentsByTemplateId,
  getConsentCountsByTemplateId,
  getBusinessyDetails,
} from "../store/actions/CommonAction";
import { useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { generateTransactionId } from "../utils/transactionId";
import config from "../utils/config";
import { toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

import { formatToIST } from "../utils/dateUtils.js";

const ConsentLogs = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  var res = "";
  const handleBack = () => {
    navigate("/templates");
  };

  const [consentsByTempId, setConsentsByTempId] = useState([]);

  const [expandedRow, setExpandedRow] = useState(null);
  const [consentCounts, setConsentCounts] = useState({});
  const [inactiveCount, setInactiveCount] = useState(0);
  const [businessDetails, setBusinessDetails] = useState({});
  const [loadLogs, setLoadLogs] = useState(true);
  const [selectedPeriod, setSelectedPeriod] = useState("Last 1 month");
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  const handleToggle = (rowId) => {
    setExpandedRow(expandedRow === rowId ? null : rowId);
  };

  const handlePeriodChange = (e) => {
    setSelectedPeriod(e.target.value);
  };
  // Helper function to filter consents based on selected period
  const filterConsentsByPeriod = (consents, period) => {
    if (!consents || consents.length === 0) return [];

    const now = new Date();
    const currentDate = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    return consents.filter((consent) => {
      if (!consent.createdAt) return true; // Include if no date

      const consentDate = new Date(consent.createdAt);
      const consentDateOnly = new Date(
        consentDate.getFullYear(),
        consentDate.getMonth(),
        consentDate.getDate()
      );

      switch (period) {
        case "Today":
          return consentDateOnly.getTime() === currentDate.getTime();
        case "Yesterday":
          const yesterday = new Date(currentDate);
          yesterday.setDate(yesterday.getDate() - 1);
          return consentDateOnly.getTime() === yesterday.getTime();
        case "Last 1 week":
          const weekAgo = new Date(currentDate);
          weekAgo.setDate(weekAgo.getDate() - 7);
          return consentDateOnly >= weekAgo;
        case "Last 1 month":
          const monthAgo = new Date(currentDate);
          monthAgo.setMonth(monthAgo.getMonth() - 1);
          return consentDateOnly >= monthAgo;
        case "Last 1 year":
          const yearAgo = new Date(currentDate);
          yearAgo.setFullYear(yearAgo.getFullYear() - 1);
          return consentDateOnly >= yearAgo;
        default:
          return true;
      }
    });
  };

  const business_id = useSelector((state) => state.common.business_id);
  const tenant_id = useSelector((state) => state.common.tenant_id);
  const session_token = useSelector((state) => state.common.session_token);
  const { search } = useLocation();
  const queryParams = new URLSearchParams(search);
  const templateId = queryParams.get("templateId");

  // Function to download Form 65B PDF
  const downloadForm65B = async (consentId) => {

    try {
      const txnId = generateTransactionId();

      const response = await fetch(`${config.check_integrity}`, {
        method: 'GET',
        headers: {
          'txn': txnId,
          'X-Tenant-ID': tenant_id,
          'X-Business-id': business_id,
          'consentId': consentId,
          'x-session-token': session_token,
          'Content-Type': 'application/json'
        }
      });

      if (!response.ok) {
        throw new Error(`API returned status ${response.status}`);
      }

      const data = await response.json();


      // Extract base64 string from response - correct field name is pdfBase64
      const base64String = data.pdfBase64 || data.data || data.base64;

      if (!base64String) {
        throw new Error('No PDF data received from server');
      }


      // Clean the base64 string (remove whitespace including newlines)
      const cleanedBase64 = base64String.replace(/\s/g, '');

      // Validate and convert base64 to blob
      const byteCharacters = atob(cleanedBase64);

      // Check PDF signature
      if (!byteCharacters.startsWith('%PDF')) {
        throw new Error('Invalid PDF data received');
      }

      // Convert to Uint8Array
      const byteNumbers = new Array(byteCharacters.length);
      for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
      }
      const byteArray = new Uint8Array(byteNumbers);

      // Create blob
      const blob = new Blob([byteArray], { type: 'application/pdf' });

      // Generate filename with timestamp and status
      const timestamp = new Date().toISOString().replace(/[:.]/g, '-').slice(0, -5);
      const status = data.overallStatus ? `_${data.overallStatus.toLowerCase()}` : '';
      const filename = `Form65B_${consentId.substring(0, 8)}${status}_${timestamp}.pdf`;

      // Create download link
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();

      // Cleanup with slight delay
      setTimeout(() => {
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
      }, 100);

      toast.success(
        (props) => (
          <CustomToast
            {...props}
            type="success"
            message="Form 65B downloaded successfully!"
          />
        ),
        { autoClose: 2000 }
      );
    } catch (error) {
      console.error('❌ Error downloading Form 65B:', error);
      console.error('Error details:', error.message, error.stack);

      let errorMessage = 'Failed to download Form 65B. Please try again.';
      if (error.message.includes('Invalid PDF')) {
        errorMessage = 'Invalid PDF data received from server.';
      } else if (error.message.includes('No PDF data')) {
        errorMessage = 'No PDF data available for this consent.';
      } else if (error.message.includes('status')) {
        errorMessage = `API Error: ${error.message}`;
      }

      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={errorMessage}
          />
        ),
        { autoClose: 4000 }
      );
    }
  };

  useEffect(() => {
    dispatch(getConsentsByTemplateId(templateId))
      .then((searchList) => {
        setConsentsByTempId(searchList);
        setLoadLogs(false);
      })
      .catch((err) => {
        if (err?.[0]?.errorCode === "JCMP3001") {
          setConsentsByTempId([]);
        }

        setLoadLogs(false);
      });
  }, [dispatch, templateId]);

  useEffect(() => {
    dispatch(getBusinessyDetails(business_id))
      .then((response) => {

        setBusinessDetails(response);
      })
      .catch((err) => {
        console.log(err);
      });
  }, [dispatch, templateId]);

  useEffect(() => {
    dispatch(getConsentCountsByTemplateId(templateId))
      .then((data) => {
        setConsentCounts(data);
        setInactiveCount(data?.statusCounts?.INACTIVE);
      })
      .catch((err) => {
        console.log(err);
      });
  }, [dispatch, templateId]);
  useEffect(() => {
    if (consentCounts?.statusCounts) {
    }
  }, [consentCounts]);

  // Apply the filter to the consents
  const filteredConsents = filterConsentsByPeriod(consentsByTempId, selectedPeriod);

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const renderSortIcon = (column) => {
    return <Icon ic={<IcSort />} size="small" color="black" />;
  };

  const sortedConsents = React.useMemo(() => {
    if (!sortColumn) return filteredConsents;

    return [...filteredConsents].sort((a, b) => {
      const aValue = a[sortColumn] || '';
      const bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [filteredConsents, sortColumn, sortDirection]);


  return (
    <div className="page-container-cl">
      <div className="main-content-cl">
        <div className="consentLog-container">
          <div>
            <Button
              ariaControls="Button Clickable"
              ariaDescribedby="Button"
              ariaExpanded="Expanded"
              ariaLabel="Button"
              className="Button"
              icon="ic_back"
              iconAriaLabel="Icon Favorite"
              iconLeft="ic_back"
              kind="secondary"
              onClick={handleBack}
              size="medium"
              state="normal"
            />
          </div>

          <div style={{ flex: 1 }}>
            <div className="header">
              <div>
                <Text appearance="heading-m" color="primary-grey-100">
                  Consent log
                </Text>
              </div>

              {/* <span class="landmark-tag">
                {" "}
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  {businessDetails?.searchList?.[0]?.name}
                </Text>
              </span> */}
              <div style={{ marginRight: '5%' }}>{"   "}</div>
              <div className="dropdown-group-pl">
                <select
                  value={selectedPeriod}
                  onChange={handlePeriodChange}
                  id="grievance-type"
                >
                  <option value="Today">Today</option>
                  <option value="Yesterday">Yesterday</option>
                  <option value="Last 1 week">Last 1 week</option>
                  <option value="Last 1 month">Last 1 month</option>
                  <option value="Last 1 year">Last 1 year</option>
                </select>
              </div>
            </div>
            <div className="numbers-section">
              <div className="card success">
                <div>
                  <Icon
                    ic={<IcSuccessColored></IcSuccessColored>}
                    size="xtra-large"
                  ></Icon>
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {consentCounts?.statusCounts?.ACTIVE || "0"}
                  </Text>
                  <br></br>
                  <Text appearance="body-xs" color="primary-grey-80">
                    Consent given
                  </Text>
                </div>
              </div>
              <div className="card warning">
                <div>
                  <Icon
                    ic={<IcTimezone></IcTimezone>}
                    size="xtra-large"
                    color="feedback_warning_50"
                  ></Icon>
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {String(consentCounts?.statusCounts?.INACTIVE)}
                  </Text>
                  <br></br>
                  <Text appearance="body-xs" color="primary-grey-80">
                    Consent pending
                  </Text>
                </div>
              </div>
              <div className="card danger">
                <div>
                  <Icon
                    ic={<IcError></IcError>}
                    size="xtra-large"
                    color="feedback_error_50"
                  ></Icon>
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {String(consentCounts?.statusCounts?.WITHDRAWN)}
                  </Text>
                  <br></br>
                  <Text appearance="body-xs" color="primary-grey-80">
                    {/* Consent revoked */}
                    Consent withdrawn
                  </Text>
                </div>
              </div>

              <div className="card info">
                <div>
                  <Icon
                    ic={<IcWarningColored></IcWarningColored>}
                    size="xtra-large"
                    color="feedback_error_50"
                  ></Icon>
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {String(consentCounts?.statusCounts?.EXPIRED)}
                  </Text>
                  <br></br>
                  <Text appearance="body-xs" color="primary-grey-80">
                    Consent Expired
                  </Text>
                </div>
              </div>
            </div>

            <table className="consentLog-table">
              <colgroup>
                <col style={{ width: "3rem" }} />
                <col style={{ width: "25%" }} />
                <col style={{ width: "20%" }} />
                <col style={{ width: "20%" }} />
                <col style={{ width: "15%" }} />
                <col style={{ width: "15%" }} />
              </colgroup>
              <thead>
                <tr>
                  <th></th>
                  <th className="parentheader" onClick={() => handleSort('consentId')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Consent ID
                      </Text>
                      {renderSortIcon('consentId')}
                    </div>
                  </th>
                  <th className="parentheader" onClick={() => handleSort('customerIdentifiers.value')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Data principle ID
                      </Text>
                      {renderSortIcon('customerIdentifiers.value')}
                    </div>
                  </th>
                  <th className="parentheader" onClick={() => handleSort('templateName')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Template name
                      </Text>
                      {renderSortIcon('templateName')}
                    </div>
                  </th>
                  <th className="parentheader" onClick={() => handleSort('templateVersion')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Created At
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Updated At
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Encrypted
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Digitally Signed
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Immutable Metadata
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Version
                      </Text>
                      {renderSortIcon('templateVersion')}
                    </div>
                  </th>
                  <th className="parentheader" onClick={() => handleSort('status')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Status
                      </Text>
                      {renderSortIcon('status')}
                    </div>
                  </th>
                  <th className="parentheader" onClick={() => handleSort('isParentalConsent')} style={{ cursor: 'pointer' }}>
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Parental Consent
                      </Text>
                      {renderSortIcon('isParentalConsent')}
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Form 65B
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                </tr>
              </thead>

              <tbody>
                {loadLogs ? (
                  <tr>
                    <td
                      colSpan="13"
                      style={{ textAlign: "center", padding: "1rem" }}
                    >
                      <div className="customerActivityLoader">
                        <Spinner
                          kind="normal"
                          labelPosition="right"
                          size="small"
                        />
                      </div>
                    </td>
                  </tr>
                ) : !Array.isArray(sortedConsents) ||
                  sortedConsents.length === 0 ? (
                  <tr>
                    <td
                      colSpan="13"
                      style={{ textAlign: "center", padding: "1rem" }}
                    >
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        No Data to Display
                      </Text>
                    </td>
                  </tr>
                ) : (
                  sortedConsents.map((consent, index) => (
                    <React.Fragment key={consent.consentId}>
                      {/* Parent row */}
                      <tr
                        onClick={() => handleToggle(`row${index}`)}
                        className={
                          expandedRow === `row${index}`
                            ? "expanded-row-parent"
                            : ""
                        }
                      >
                        <td className="chevron-cell">
                          <div className="chevron-wrapper">
                            <Text appearance="body-xs" color="primary-grey-100">
                              {expandedRow === `row${index}` ? (
                                <IcChevronUp height={25} width={25} />
                              ) : (
                                <IcChevronDown height={25} width={25} />
                              )}
                            </Text>
                          </div>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            {consent.consentId}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            {consent.customerIdentifiers?.value}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            {consent.templateName}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs"
                            color="primary-grey-80"
                          >
                            {formatToIST(consent.createdAt)}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs"
                            color="primary-grey-80"
                          >
                            {formatToIST(consent.updatedAt)}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            Yes
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            Yes
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            Yes
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            {consent.templateVersion}
                          </Text>
                        </td>
                        <td>
                          <div className="activebadge">
                            <p className="activebadge-text">{consent.status}</p>
                          </div>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            {consent.isParentalConsent === true ? "True" : "False"}
                          </Text>
                        </td>
                        <td>
                          <div
                            onClick={(e) => {
                              e.stopPropagation();
                              downloadForm65B(consent.consentId);
                            }}
                            style={{ cursor: 'pointer', display: 'inline-block' }}
                            title="Download Form 65B"
                          >
                            <Icon ic={<IcDownloads />} color="#0f3cc9" />
                          </div>
                        </td>
                      </tr>

                      {/* Expanded row */}
                      {expandedRow === `row${index}` && (
                        <tr className="expanded-row">
                          <td></td>
                          <td colSpan="12">
                            <table className="inner-table">
                              <colgroup>
                                <col style={{ width: "20%" }} />
                                <col style={{ width: "15%" }} />
                                <col style={{ width: "35%" }} />
                                <col style={{ width: "15%" }} />
                                <col style={{ width: "15%" }} />
                              </colgroup>

                              <thead>
                                <tr>
                                  <th>
                                    <Text
                                      appearance="body-xs-bold"
                                      color="primary-grey-80"
                                    >
                                      Purpose Name
                                    </Text>
                                  </th>
                                  <th>
                                    <Text
                                      appearance="body-xs-bold"
                                      color="primary-grey-80"
                                    >
                                      Purpose Code
                                    </Text>
                                  </th>
                                  <th>
                                    <Text
                                      appearance="body-xs-bold"
                                      color="primary-grey-80"
                                    >
                                      Data Item
                                    </Text>
                                  </th>
                                  <th>
                                    <Text
                                      appearance="body-xs-bold"
                                      color="primary-grey-80"
                                    >
                                      Mandatory/Optional
                                    </Text>
                                  </th>
                                  <th>
                                    <Text
                                      appearance="body-xs-bold"
                                      color="primary-grey-80"
                                    >
                                      Status
                                    </Text>
                                  </th>
                                </tr>
                              </thead>

                              <tbody>
                                {consent.preferences.map((pref) =>
                                  pref.purposeList.map((purpose) => (
                                    <tr key={purpose.purposeId}>
                                      <td>
                                        <Text
                                          appearance="body-xs-bold"
                                          color="primary-grey-80"
                                        >
                                          {purpose.purposeInfo.purposeName}
                                        </Text>
                                      </td>
                                      <td>
                                        <Text
                                          appearance="body-xs-bold"
                                          color="primary-grey-80"
                                        >
                                          {purpose.purposeInfo.purposeCode}
                                        </Text>
                                      </td>
                                      <td>
                                        <Text
                                          appearance="body-xs-bold"
                                          color="primary-grey-80"
                                        >
                                          {pref.processorActivityList
                                            .flatMap((pa) =>
                                              pa.processActivityInfo.dataTypesList.flatMap(
                                                (dt) => dt.dataItems
                                              )
                                            )
                                            .join(", ")}
                                        </Text>
                                      </td>
                                      <td>
                                        <Text
                                          appearance="body-xs-bold"
                                          color="primary-grey-80"
                                        >
                                          {pref.mandatory
                                            ? "Mandatory"
                                            : "Optional"}
                                        </Text>
                                      </td>
                                      <td>
                                        <span
                                          className={
                                            pref.preferenceStatus === "ACCEPTED"
                                              ? "accepted"
                                              : "not-accepted"
                                          }
                                        >
                                          <Text
                                            appearance="body-xs-bold"
                                            color="primary-grey-80"
                                          >
                                            {pref.preferenceStatus}
                                          </Text>
                                        </span>
                                      </td>
                                    </tr>
                                  ))
                                )}
                              </tbody>
                            </table>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConsentLogs;
