import { ActionButton, Text, Icon, BadgeV2, SearchBox } from '../custom-components';
import {
  IcEditPen,
  IcError,
  IcSort,
  IcSuccessColored,
  IcSwap,
  IcWarningColored,
} from '../custom-components/Icon';
import { useLocation, useNavigate } from "react-router-dom";
import { Button, Spinner, Text } from '../custom-components';

import "../styles/EmailTemplate.css";
import {
  IcRequest,
  IcSmartSwitchPlug,
  IcTimezone,
  IcWhatsapp,
} from '../custom-components/Icon';
import { Link } from "react-router-dom";
import React, { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { useSelector } from "react-redux";
import { getProcessor, getGrievances, getGrievanceByTemplateId, getBusinessyDetails } from "../store/actions/CommonAction";
import { GRIEVANCE_STATUS } from "../utils/constant";

const GrievanceLogs = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  var res = "";
  const handleBack = () => {
    navigate("/grievanceFormTemplates");
  };

  // const [consentsByTempId, setConsentsByTempId] = useState([]);

  // const [expandedRow, setExpandedRow] = useState(null);
  // const [consentCounts, setConsentCounts] = useState({});
  // const [inactiveCount, setInactiveCount] = useState(0);
  // const [businessDetails, setBusinessDetails] = useState({});
  // const [loadLogs, setLoadLogs] = useState(true);

  // const handleToggle = (rowId) => {
  //   setExpandedRow(expandedRow === rowId ? null : rowId);
  // };

  // const business_id = useSelector((state) => state.common.business_id);
  // const { search } = useLocation();
  // const [requests, setRequests] = useState([]);

  // const tenantId = useSelector((state) => state.common.tenant_id);
  // const businessId = useSelector((state) => state.common.business_id);

  // const formatDateTime = (dateString) => {
  //   const date = new Date(dateString);
  //   return date.toLocaleDateString("en-IN") + " at " + date.toLocaleTimeString("en-IN", {
  //     hour: "2-digit",
  //     minute: "2-digit",
  //   });
  // };


  // // const handleClick = () => {
  // //   navigate("/editRequest");
  // // };

  // const handleClick = (reqId) => {
  //   navigate(`/editRequest/${reqId}`);
  // };

  // useEffect(() => {
  //   const fetchData = async () => {
  //     const res = await dispatch(getGrievances(tenantId, businessId));

  //     if (res?.data && Array.isArray(res.data)) {
  //       const formatted = res.data.map((item) => ({
  //         id: item.grievanceId,
  //         type: item.grievanceType,
  //         date: formatDateTime(item.createdAt),
  //         principal: item.userDetails?.MOBILE ?? "-",
  //         status: item.status,
  //       }));

  //       setRequests(formatted);
  //     }
  //   };

  //   fetchData();
  // }, [dispatch, businessId]);



  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const [consentsByTempId, setConsentsByTempId] = useState([]);
  const [requests, setRequests] = useState([]);

  const [expandedRow, setExpandedRow] = useState(null);
  const [consentCounts, setConsentCounts] = useState({});
  const [inactiveCount, setInactiveCount] = useState(0);
  const [businessDetails, setBusinessDetails] = useState({});
  const [loadLogs, setLoadLogs] = useState(true);
  const [resolvedCount, setResolvedCount] = useState(0);
  const [inProgressCount, setInProgressCount] = useState(0);
  const [newCount, setNewCount] = useState(0);
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  const handleToggle = (rowId) => {
    setExpandedRow(expandedRow === rowId ? null : rowId);
  };

  const business_id = useSelector((state) => state.common.business_id);
  const { search } = useLocation();
  const queryParams = new URLSearchParams(search);
  const templateId = queryParams.get("templateId");
  useEffect(() => {
    if (!templateId) return;

    dispatch(getGrievances(tenantId, businessId))
      .then((searchList) => {

        // ✅ Safely extract data array from API response
        const allData = searchList?.data || [];

        // ✅ Filter by current templateId
        const filteredData = allData.filter(
          (item) => item.grievanceTemplateId === templateId
        );

        // ✅ Update requests with filtered data
        setRequests(filteredData);

        // ✅ Compute counts by status
        const resolved = filteredData.filter(
          (item) => item.status === GRIEVANCE_STATUS.GRIEVANCE_RESOLVED
        ).length;

        const inProgress = filteredData.filter(
          (item) => item.status === GRIEVANCE_STATUS.GRIEVANCE_INPROCESS
        ).length;

        const newReqCount = filteredData.filter(
          (item) => item.status === GRIEVANCE_STATUS.NEW
        ).length;

        // ✅ Update counters
        setResolvedCount(resolved);
        setInProgressCount(inProgress);
        setNewCount(newReqCount);
      })
      .catch((err) => {
        console.error("Error occurred in getGrievances:", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setRequests([]);
        }
      })
      .finally(() => {
        setLoadLogs(false);
      });
  }, [dispatch, tenantId]);


  useEffect(() => {
    dispatch(getBusinessyDetails(business_id))
      .then((response) => {

        setBusinessDetails(response);
      })
      .catch((err) => {
        console.log(err);
      });
  }, [dispatch, templateId]);

  // useEffect(() => {
  //   dispatch(getConsentCountsByTemplateId(templateId))
  //     .then((data) => {
  //       setConsentCounts(data);
  //       setInactiveCount(data?.statusCounts?.INACTIVE);
  //       console.log("API returned:", inactiveCount);
  //     })
  //     .catch((err) => {
  //       console.log(err);
  //     });
  // }, [dispatch, templateId]);
  useEffect(() => {
    if (consentCounts?.statusCounts) {
      console.log("Updated consentCounts:", consentCounts.statusCounts);
    }
  }, [consentCounts]);

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

  const sortedRequests = React.useMemo(() => {
    if (!sortColumn) return requests;

    return [...requests].sort((a, b) => {
      const aValue = a[sortColumn] || '';
      const bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [requests, sortColumn, sortDirection]);

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
            <div className="">
              <div className="emailTemplate-Heading-Button">
                <div className="emailTemplate-Heading-badge">
                  <Text appearance="heading-s" color="primary-grey-100">
                    Request
                  </Text>
                  <div className="systemConfig-badge">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Grievance Redressal
                    </Text>
                  </div>
                </div>
              </div>
              <br></br>

              <div className="container-wrapper">
                <div className="numbers-section">
                  <div className="card total">
                    <div>
                      <Icon
                        ic={<IcRequest />}
                        size="xtra-large"
                        color="#555555"
                      />
                    </div>
                    <div style={{ marginLeft: "10px" }}>
                      <Text appearance="heading-xxs" color="primary-grey-100">
                        {newCount || "0"}
                      </Text>
                      <br />
                      <Text appearance="body-xs" color="primary-grey-80">
                        New
                      </Text>
                    </div>
                  </div>

                  <div className="card inProgress">
                    <div>
                      <Icon
                        ic={<IcRequest />}
                        size="xtra-large"
                        color="primary-grey-20"
                      />
                    </div>
                    <div style={{ marginLeft: "10px" }}>
                      <Text appearance="heading-xxs" color="primary-grey-100">
                        {inProgressCount || "0"}
                      </Text>
                      <br />
                      <Text appearance="body-xs" color="primary-grey-80">
                        In Process
                      </Text>
                    </div>
                  </div>

                  <div className="card success">
                    <div>
                      <Icon ic={<IcSuccessColored />} size="xtra-large" />
                    </div>
                    <div style={{ marginLeft: "10px" }}>
                      <Text appearance="heading-xxs" color="primary-grey-100">
                        {resolvedCount || "0"}
                      </Text>
                      <br />
                      <Text appearance="body-xs" color="primary-grey-80">
                        Resolved
                      </Text>
                    </div>
                  </div>
                </div>

                <div className="search-wrapper" style={{ padding: "1%" }}>
                  <SearchBox
                    kind="normal"
                    label="Search Request Id"
                    // onChange={(e) => {
                    //    console.log("Searching for:", e.target.value);
                    // }}
                    prefix="ic_search"
                  />
                </div>
              </div>

              <div>
                <div className="emailTemplate-custom-table-outer-div">
                  <table className="emailTempalte-custom-table">
                    <thead>
                      <tr>
                        <th style={{ width: "25%", cursor: 'pointer' }} onClick={() => handleSort('grievanceId')}>
                          <div className="emailTemplate-table-header-icon">
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              Request ID
                            </Text>
                            {renderSortIcon('grievanceId')}
                          </div>
                        </th>
                        <th style={{ width: "15%", cursor: 'pointer' }} onClick={() => handleSort('grievanceType')}>
                          <div className="emailTemplate-table-header-icon">
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              Request Type
                            </Text>
                            {renderSortIcon('grievanceType')}
                          </div>
                        </th>
                        <th style={{ width: "20%", cursor: 'pointer' }} onClick={() => handleSort('updatedAt')} >
                          <div className="emailTemplate-table-header-icon">
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              Received On (date/time)
                            </Text>
                            {renderSortIcon('updatedAt')}
                          </div>
                        </th>
                        <th style={{ width: "15%", cursor: 'pointer' }} onClick={() => handleSort('grievanceDescription')} >
                          <div className="emailTemplate-table-header-icon">
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              Remarks
                            </Text>
                            {renderSortIcon('grievanceDescription')}
                          </div>
                        </th>

                        <th style={{ width: "15%", cursor: 'pointer' }} onClick={() => handleSort('status')} >
                          <div className="emailTemplate-table-header-icon">
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              Status
                            </Text>
                            {renderSortIcon('status')}
                          </div>
                        </th>
                      </tr>
                    </thead>
                    {/* <tbody>
                  {requests.map((req, idx) => (
                    <tr key={idx}>
                      <td>
                        <span
                          style={{ color: "#0066cc", cursor: "pointer" }}
                          onClick={handleClick}
                        >
                          <Text appearance="body-xs-bold" color="black">
                            {req.id}
                          </Text>
                        </span>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.type}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.date}
                        </Text>
                      </td>
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.principal}
                        </Text>
                      </td>
                     
                      <td>
                        <Text appearance="body-xs-bold" color="black">
                          {req.status}
                        </Text>
                      </td>
                    </tr>
                  ))}
                </tbody> */}
                    {/* <tbody>
                  {requests.map((req, idx) => (
                    <tr key={idx}>
                      <td>
                        <span style={{ color: "#0066cc", cursor: "pointer" }} onClick={handleClick}>
                          <Text appearance="body-xs-bold" color="black">{req.id}</Text>
                        </span>
                      </td>
                      <td><Text appearance="body-xs-bold" color="black">{req.type}</Text></td>
                      <td><Text appearance="body-xs-bold" color="black">{req.date}</Text></td>
                      <td><Text appearance="body-xs-bold" color="black">{req.principal}</Text></td>
                      <td><Text appearance="body-xs-bold" color="black">{req.status}</Text></td>
                    </tr>
                  ))}
                </tbody> */}
                    <tbody>
                      {sortedRequests.length > 0 ? (
                        sortedRequests.map((req, idx) => (
                          <tr key={idx}>
                            <td>
                              <span
                                style={{ color: "#0066cc", cursor: "pointer" }}
                              // onClick={() => handleClick(req.id)}
                              >
                                <Text appearance="body-xs-bold" color="black">
                                  {req.grievanceId || ""}
                                </Text>
                              </span>
                            </td>
                            <td>
                              <Text appearance="body-xs-bold" color="black">
                                {req.grievanceType || ""}
                              </Text>
                            </td>
                            <td>
                              <Text appearance="body-xs-bold" color="black">
                                {req.updatedAt || ""}
                              </Text>
                            </td>
                            <td>
                              <Text appearance="body-xs-bold" color="black">
                                {req.grievanceDescription || ""}
                              </Text>
                            </td>
                            <td>
                              <Text appearance="body-xs-bold" color="black">
                                {req.status}
                              </Text>
                            </td>
                          </tr>
                        ))
                      ) : (
                        <tr>
                          <td
                            colSpan="5"
                            style={{ textAlign: "center", padding: "20px" }}
                          >
                            <Text
                              appearance="body-s-bold"
                              color="primary-grey-80"
                            >
                              No data present
                            </Text>
                          </td>
                        </tr>
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GrievanceLogs;
