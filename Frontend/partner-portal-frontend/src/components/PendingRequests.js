import React, { useEffect } from "react";
import "../styles/ConsentLogs.css";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Button, Spinner, Text } from "../custom-components";
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
  getPendingConsentsByTemplateId,
  getConsentCountsByTemplateId,
  getConsentHandleDetailsByConsentHandleId,
  getBusinessyDetails,
} from "../store/actions/CommonAction";
import { useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";

const PendingRequests = () => {
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
  const [consentDetails, setConsentDetails] = useState({});
  const [loadingDetails, setLoadingDetails] = useState({});

  const handleToggle = (rowId, consentHandleId) => {
    // If clicking on already expanded row, just collapse it
    if (expandedRow === rowId) {
      setExpandedRow(null);
      return;
    }

    // If details already fetched, just expand
    if (consentDetails[consentHandleId]) {
      setExpandedRow(rowId);
      return;
    }

    // Fetch details for this consent
    setLoadingDetails({ ...loadingDetails, [consentHandleId]: true });
    setExpandedRow(rowId);

    dispatch(getConsentHandleDetailsByConsentHandleId(consentHandleId))
      .then((response) => {
        console.log("Detailed consent data:", response);
        // Store the response directly - it's already the full consent handle object
        setConsentDetails({
          ...consentDetails,
          [consentHandleId]: response,
        });
        setLoadingDetails({ ...loadingDetails, [consentHandleId]: false });
      })
      .catch((err) => {
        console.log("Error fetching consent details:", err);
        setLoadingDetails({ ...loadingDetails, [consentHandleId]: false });
      });
  };

  const business_id = useSelector((state) => state.common.business_id);
  const { search } = useLocation();
  const queryParams = new URLSearchParams(search);
  const templateId = queryParams.get("templateId");
  useEffect(() => {
    dispatch(getPendingConsentsByTemplateId(templateId))
      .then((searchList) => {
        console.log("SEarchLisy", searchList);
        // Filter to show only pending status records
        const pendingRecords = searchList.filter(
          (consent) => consent.status === "PENDING"
        );
        setConsentsByTempId(pendingRecords);
        console.log("Counts", pendingRecords.length);
        setLoadLogs(false);
      })
      .catch((err) => {
        console.log("Error occured in getConsentsByTemplateId", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setConsentsByTempId([]);
        }

        setLoadLogs(false);
      });
  }, [dispatch, templateId]);

  useEffect(() => {
    dispatch(getBusinessyDetails(business_id))
      .then((response) => {
        console.log("Business Name:", response?.searchList?.[0]?.name);

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
        console.log("API returned:", inactiveCount);
      })
      .catch((err) => {
        console.log(err);
      });
  }, [dispatch, templateId]);
  useEffect(() => {
    if (consentCounts?.statusCounts) {
      console.log("Updated consentCounts:", consentCounts.statusCounts);
    }
  }, [consentCounts]);
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
                  Pending Consent Requests
                </Text>
              </div>

              {/* <span class="landmark-tag">
                {" "}
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  {businessDetails?.searchList?.[0]?.name}
                </Text>
              </span> */}
              <div style={{marginRight:'5%'}}>{"   "}</div>
              {/* <div className="dropdown-group-pl">
                <select
                  // value={language}
                  // onChange={handleLanguageChange}
                  id="grievance-type"
                >
                  <option value="" disabled>
                    Select time period
                  </option>
                  <option value="English">Today</option>
                  <option value="Hindi">Yesterday</option>
                  <option value="Marathi">Last 1 week</option>
                  <option value="Marathi">Last 1 month</option>
                  <option value="Marathi">Last 1 year</option>
                </select>
              </div> */}
            </div>
            {/* <div className="numbers-section">
              <div className="card success">
                <div>
                  <Icon
                    ic={<IcSuccessColored></IcSuccessColored>}
                    size="xtra-large"
                  ></Icon>
                </div>
                <div style={{ marginLeft: "10px" }}>
                  <Text appearance="heading-xxs" color="primary-grey-100">
                    {consentCounts?.statusCounts?.ACTIVE}
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
                    Consent revoked
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
            </div> */}
            <br></br>

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
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Request ID
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Request Date
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Request Expiry
                      </Text>
                      <Icon ic={<IcSort />} color="primary_60" size="small" />
                    </div>
                  </th>
                  <th className="parentheader">
                    <div className="header-with-icon">
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Request Type
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
                {loadLogs ? (
                  <tr>
                    <td
                      colSpan="6"
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
                ) : !Array.isArray(consentsByTempId) ||
                  consentsByTempId.length === 0 ? (
                  <tr>
                    <td
                      colSpan="6"
                      style={{ textAlign: "center", padding: "1rem" }}
                    >
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        No Data to Display
                      </Text>
                    </td>
                  </tr>
                ) : (
                  consentsByTempId.map((consent, index) => (
                    <React.Fragment key={consent.consentHandleId
                    }>
                      {/* Parent row */}
                      <tr
                        onClick={() => handleToggle(`row${index}`, consent.consentHandleId)}
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
                            {consent.consentHandleId}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            {consent.createdAt}
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            -
                          </Text>
                        </td>
                        <td>
                          <Text
                            appearance="body-xs-bold"
                            color="primary-grey-80"
                          >
                            Renewal request
                          </Text>
                        </td>
                        <td>
                          <div className="warningbadge" >
                            <p className="warningbadge-text" style={{ background: "#fff4ec", fontWeight: "700", fontFamily: "system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif", padding: "0.2rem", textAlign: "center", color: "#f06d0f"  }}>{consent.status}</p>
                          </div>
                        </td>
                      </tr>

                      {/* Expanded row */}
                      {expandedRow === `row${index}` && (
                        <tr className="expanded-row">
                          <td></td>
                          <td colSpan="5">
                            {loadingDetails[consent.consentHandleId] ? (
                              <div style={{ textAlign: "center", padding: "2rem" }}>
                                <Spinner
                                  kind="normal"
                                  labelPosition="right"
                                  size="small"
                                />
                                <Text appearance="body-xs" color="primary-grey-80" style={{ marginLeft: "8px" }}>
                                  Loading details...
                                </Text>
                              </div>
                            ) : consentDetails[consent.consentHandleId]?.preferences ? (
                              <table className="inner-table">
                                <colgroup>
                                  <col style={{ width: "25%" }} />{" "}
                                  <col style={{ width: "20%" }} />{" "}
                                  <col style={{ width: "20%" }} />{" "}
                                  <col style={{ width: "15%" }} />{" "}
                                  <col style={{ width: "15%" }} />{" "}
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
                                  {consentDetails[consent.consentHandleId].preferences.map((pref) =>
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
                            ) : (
                              <div style={{ textAlign: "center", padding: "2rem" }}>
                                <Text appearance="body-xs-bold" color="primary-grey-80">
                                  No details available
                                </Text>
                              </div>
                            )}
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

export default PendingRequests;
