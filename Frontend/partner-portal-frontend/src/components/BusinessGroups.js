import React, { useMemo, useState, useEffect } from "react";
import "../styles/businessGroups.css";
import "../styles/pageConfiguration.css";
import "../styles/toast.css";
import { Text, InputCheckbox, InputFieldV2, InputDropdown, Button, Modal, Icon, ActionButton } from "../custom-components";
import { IcClose, IcTrash, IcSort } from "../custom-components/Icon";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useDispatch, useSelector } from "react-redux";
import { createBusiness, fetchBusinessApplications } from "../store/actions/CommonAction";
import { formatToIST } from "../utils/dateUtils";

const BusinessGroups = () => {

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState("");
  const [nameError, setNameError] = useState("");
  const nameCharLimit = 250;
  const dispatch = useDispatch();
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');


  const handleOpenModal = () => {
    setIsModalOpen(true);
  };
  const handleCloseModal = () => {
    setIsModalOpen(false);
  };

  const [text, setText] = useState("");
  const [charError, setCharError] = useState("");
  const charLimit = 300;

  const handleTextAreaChange = (e) => {
    const inputValue = e.target.value;

    if (inputValue.length <= charLimit) {
      setText(inputValue);
      // Show error when limit is reached
      if (inputValue.length === charLimit) {
        setCharError("Maximum 300 characters allowed.");
      } else {
        setCharError("");
      }
    } else {
      // Prevent typing beyond limit
      setText(inputValue.slice(0, charLimit));
      setCharError("Maximum 300 characters allowed.");
    }
  };

  const handleNameChange = (e) => {
    const inputValue = e.target.value;

    if (inputValue.length <= nameCharLimit) {
      setName(inputValue);
      // Show error when limit is reached
      if (inputValue.length === nameCharLimit) {
        setNameError("Maximum 250 characters allowed.");
      } else {
        setNameError("");
      }
    } else {
      // Prevent typing beyond limit
      setName(inputValue.slice(0, nameCharLimit));
      setNameError("Maximum 250 characters allowed.");
    }
  };

  const [createBusinessGroup, setCreateBusinessGroup] = useState(false);

  const tenantId = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);

  const resetForm = () => {
    setName("");
    setText("");
    setNameError("");
    setCharError("");
  };

  const closeBusinessGroupModal = () => {
    resetForm();
    setCreateBusinessGroup(false);
  }

  const handleSubmit = async () => {
    // alert("Creating business group...");


    if (!name.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please fill name."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (name.length > nameCharLimit) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Maximum 250 characters allowed for name."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (text.length > charLimit) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Maximum 300 characters allowed."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!text.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please fill description."}
          />
        ),
        { icon: false }
      );
      return;
    }

    try {
      const payload = {
        name: name.trim(),
        description: text.trim(),
      };

      const response = await createBusiness(payload, token, tenantId);

      if (response?.businessId) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Business group created successfully!"}
            />
          ),
          { icon: false }
        );

        resetForm();
        setCreateBusinessGroup(false);
        dispatch(fetchBusinessApplications(token, tenantId));
        return;
      }

      if (Array.isArray(response) && response[0]?.errorMessage) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={response[0].errorMessage}
            />
          ),
          { icon: false }
        );
        return;
      }

      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Failed to create business group. Please try again."}
          />
        ),
        { icon: false }
      );

    } catch (err) {
      console.error("Error creating business group:", err);
      const errorMessage = err?.[0]?.errorMessage || err?.errorMessage || "Failed to create business group. Please try again.";

      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={errorMessage}
          />
        ),
        { icon: false }
      );
    }

  };

  const formatDate = (dateStr) => {
    if (!dateStr) return "-";
    const formatted = formatToIST(dateStr);
    if (formatted === 'N/A') return "-";
    // Extract just the date part (DD-MM-YYYY)
    return formatted.split(' ')[0]; // Get "DD-MM-YYYY" from "DD-MM-YYYY HH:mm:ss"
  };

  useEffect(() => {
    if (!tenantId || !token) return;
    dispatch(fetchBusinessApplications(token, tenantId));
  }, [dispatch, tenantId, token]);

  const businesses = useSelector((state) => state.common.businesses);

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

  const sortedBusinesses = useMemo(() => {
    if (!sortColumn) return businesses;

    return [...businesses].sort((a, b) => {
      const aValue = a[sortColumn] || '';
      const bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [businesses, sortColumn, sortDirection]);

  return (
    <>
      <div className="configurePage">
        <div className="bs-page">
          <div className="main-heading-ct">
            <div className="bs-heading">
              <Text appearance="heading-s" color="primary-grey-100">
                Business groups
              </Text>
              <div className="tag">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  User Management
                </Text>
              </div>
            </div>
            <div className="bs-button" style={{ marginRight: "5%" }}>
              <div>
                <ActionButton
                  label="Add business group"
                  onClick={() => setCreateBusinessGroup(true)}
                  kind="primary"
                ></ActionButton>
              </div>
            </div>
          </div>



          <div className="bs-content">
            <div className="business-table-container">
              <table className="business-table">
                <thead>
                  <tr>
                    <th onClick={() => handleSort('name')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Business group name</span>
                        {renderSortIcon('name')}
                      </div>
                    </th>
                    <th onClick={() => handleSort('description')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Description</span>
                        {renderSortIcon('description')}
                      </div>
                    </th>
                    <th onClick={() => handleSort('createdAt')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Created on</span>
                        {renderSortIcon('createdAt')}
                      </div>
                    </th>
                    {/* <th>Total users</th> */}
                  </tr>
                </thead>
                <tbody>
                  {sortedBusinesses && sortedBusinesses.length > 0 ? (
                    sortedBusinesses.map((biz) => (
                      <tr key={biz.businessId}>
                        <td>{biz.name || "-"}</td>
                        <td>{biz.description || "-"}</td>
                        <td>{formatDate(biz.createdAt)}</td>
                        {/* <td>0</td> */}
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="3" style={{ textAlign: "center", padding: "10px" }}>
                        No business applications found
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>

          {createBusinessGroup && (
            <div className="modal-outer-container">
              <div className="modal-container-old" style={{ height: '410px' }}>
                <div className="modal-close-btn-container">
                  <ActionButton
                    onClick={closeBusinessGroupModal}
                    icon={<IcClose />}
                    kind="tertiary"
                    size="small"
                  ></ActionButton>
                </div>
                <Text appearance="heading-xs" color="primary-grey-100">

                  Add business group
                </Text>
                <br></br>
                <div style={{ marginTop: "10px" }}>
                  <InputFieldV2
                    label="Name (Required)"
                    size="small"
                    value={name}
                    onChange={(e) => handleNameChange(e)}
                    maxLength={nameCharLimit}
                    state={nameError ? "error" : "none"}
                    stateText={nameError || ""}
                  />
                  <div style={{ display: "flex", justifyContent: "flex-end", marginTop: "4px" }}>
                    <Text appearance="body-xs" color={name.length === nameCharLimit ? "error" : "primary-grey-80"} style={{ fontSize: "12px" }}>
                      {name.length}/{nameCharLimit}
                    </Text>
                  </div>
                </div>
                <br></br>
                <Text appearance="body-xs" color="primary-grey-80">
                  Description (Required)
                </Text>

                <textarea
                  placeholder="Enter description of the group's function or scope."
                  value={text}
                  onChange={handleTextAreaChange}
                  maxLength={charLimit}
                  rows="4"
                  className="custom-text-area"
                />
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: "4px" }}>
                  <div>
                    {charError && (
                      <Text appearance="body-xs" color="error" style={{ fontSize: "12px" }}>
                        {charError}
                      </Text>
                    )}
                  </div>
                  <div className="char-counter" style={{ color: text.length === charLimit ? "#f44336" : "#666" }}>
                    {text.length}/{charLimit}
                  </div>
                </div>
                <div className="modal-add-btn-container">

                  <ActionButton label="Create" onClick={handleSubmit}></ActionButton>
                </div>
              </div>
            </div>
          )}

        </div>
      </div>
      <ToastContainer
        position="bottom-left"
        autoClose={3000}
        hideProgressBar
        closeOnClick
        pauseOnHover
        draggable
        closeButton={false}
        toastClassName={() => "toast-wrapper"}
        transition={Slide}
      />
    </>
  );

}


export default BusinessGroups;