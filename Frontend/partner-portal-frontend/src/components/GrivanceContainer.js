import React from "react";
import { useState, useEffect } from "react";
import {
  ActionButton,
  Icon,
  Text,
  InputFieldV2,
  InputRadio,
  InputCheckbox,
  SearchBox,
  Tabs,
  TabItem,
} from "../custom-components";
import Select from "react-select";
import "../styles/purposeContainer.css";
import "../styles/grievanceContainer.css";
import {
  getGrievanceType,
  getProcessingActivity,
  getPurposeList,
  getUserDetails,
  getUserTypes,
  postGrievanceType,
  postUserDetail,
  postUserType,
  putGrievanceType,
} from "../store/actions/CommonAction";
import { useDispatch } from "react-redux";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { IcClose, IcTrash, ic_search } from "../custom-components/Icon";

const GrievanceContainer = ({
  userData,
  onChange,
  purposeNumber,
  onClear,
  isFirst,
  processingActivityList,
  setProcessingActivityList,
  purposeOptions,
  setPurposeOptions,
  processingActivityOption,
  setProcessingActivityOptions,
  // new
  detailsMap,
  setDetailsMap,
  activeInfoTab,
  setActiveInfoTab,
  grievanceTypes,
  setGrievanceTypes,
  selectedType,
  setSelectedType,
  userDetailOptions,
  setUserDetailOptions,
  tenant_id,
  businessId,
}) => {
  const dispatch = useDispatch();

  const [searchType, setSearchType] = useState("");
  const [searchDetail, setSearchDetail] = useState("");

  const [newTypeInput, setNewTypeInput] = useState("");
  const [newDetailInput, setNewDetailInput] = useState("");
  const [showNewTypeInput, setShowNewTypeInput] = useState(false);
  const [showNewDetailInput, setShowNewDetailInput] = useState(false);
  const [userTypeDropdownOpen, setUserTypeDropdownOpen] = useState(false);
  const [grievanceTypeDropdownOpen, setGrievanceTypeDropdownOpen] =
    useState(false);
  const [userDetailDropdownOpen, setUserDetailDropdownOpen] = useState(false);

  const [userTypeSearch, setUserTypeSearch] = useState("");
  const [userDetailSearch, setUserDetailSearch] = useState("");
  const [showAddModal, setShowAddModal] = useState(false);
  const [newEntryName, setNewEntryName] = useState("");
  const [newEntryDescription, setNewEntryDescription] = useState("");
  const [addType, setAddType] = useState(""); // "userType" or "userDetail"
  // variable to store grievance type with id
  const [grievanceTypeWithId, setGrievanceTypeWithId] = useState([]);
  useEffect(() => {
    setActiveInfoTab("grievance");
  }, [ setActiveInfoTab]);

  // Example POST API handler
  const handleAddNew = async () => {
    if (!newEntryName.trim()) return;

    try {
      const name = newEntryName.trim();
      const description = newEntryDescription?.trim() || ""; // optional field
      let res;

      if (addType === "userType") {
        // Send both name & description to API
        res = await dispatch(postUserType({ name, description }));

        // Update local state options
        setPurposeOptions((prev) => [
          ...prev,
          { value: res.data.name, label: res.data.name },
        ]);
      } else {
        res = await dispatch(postUserDetail({ name, description }));

        setUserDetailOptions((prev) => [
          ...prev,
          { value: res.data.name, label: res.data.name },
        ]);
      }

      // Reset fields & close modal
      setNewEntryName("");
      setNewEntryDescription("");
      setShowAddModal(false);

      toast.success("Added successfully!");
    } catch (err) {
      toast.error("Failed to add new entry");
    }
  };

  const filteredTypes = grievanceTypes.filter((type) =>
    type.toLowerCase().includes(searchType.toLowerCase())
  );

  const filteredDetails =
    selectedType && detailsMap[selectedType] && detailsMap[selectedType]
      ? detailsMap[selectedType].filter((d) =>
          d?.toLowerCase().includes(searchDetail.toLowerCase())
        )
      : [];

  // Add new type inline
  // const addNewTypeInline = (newValue) => {

  //   if (newTypeInput && !grievanceTypes.includes(newTypeInput)) {
  //     onChange({
  //       grievances: {
  //         ...(userData.grievances || {}),
  //         [newValue]: userData.grievances?.[newValue] || [],
  //       },
  //     });
  //     setGrievanceTypes([...grievanceTypes, newTypeInput]);
  //     setDetailsMap({ ...detailsMap, [newTypeInput]: [] });
  //     setNewTypeInput("");
  //     setShowNewTypeInput(false);
  //   }
  // };

  const addNewTypeInline = async (newValue) => {
    if (!newValue.trim() || grievanceTypes.includes(newValue.trim())) return;

    try {
      const grievanceType = newValue.trim();
      const grievanceItem = [];

      const description = ""; // optional, or you can add an input if needed

      // Call your API via Redux dispatch (similar to postUserType)
      const res = await dispatch(
        postGrievanceType({
          grievanceType,
          grievanceItem,
          description,
          tenant_id,
          businessId,
        })
      );

      // Update local grievance type options
      const updatedType = res?.data?.grievanceType;

      // Store both grievanceType and grievanceTypeId
      const grievanceTypeInfo = {
        grievanceType: res?.data.grievanceType,
        grievanceTypeId: res?.data.grievanceTypeId,
      };
      //  Merge with existing grievanceTypeWithId, avoiding duplicates
      setGrievanceTypeWithId((prev) => {
        const merged = [...prev, grievanceTypeInfo];
        // remove duplicates by grievanceTypeId
        const unique = merged.filter(
          (v, i, self) =>
            i === self.findIndex((t) => t.grievanceTypeId === v.grievanceTypeId)
        );
        return unique;
      });

      // Update user data with new type
      onChange({
        grievances: {
          ...(userData.grievances || {}),
          [updatedType]: userData.grievances?.[updatedType] || [],
        },
      });

      // Update local lists
      setGrievanceTypes((prev) => [...prev, updatedType]);
      setDetailsMap((prev) => ({ ...prev, [updatedType]: [] }));

      // Reset input and close field
      setNewTypeInput("");
      setShowNewTypeInput(false);

      toast.success("Grievance category added successfully!");
    } catch (err) {
      console.error("Failed to add new grievance category:", err);
      toast.error("Failed to add new grievance subcategory");
    }
  };

  // Add new detail inline
  // const addNewDetailInline = () => {
  //   if (!selectedType) return;
  //   if (newDetailInput && !detailsMap[selectedType].includes(newDetailInput)) {
  //     setDetailsMap({
  //       ...detailsMap,
  //       [selectedType]: [...detailsMap[selectedType], newDetailInput],
  //     });
  //     setNewDetailInput("");
  //     setShowNewDetailInput(false);
  //   }
  // };
  const addNewDetailInline = async () => {
    if (!selectedType || !newDetailInput.trim()) return;

    try {
      const grievanceType = selectedType;
      const newItem = newDetailInput.trim();

      // Existing details in list (for display)
      const existingItems = detailsMap[selectedType] || [];

      // Already selected (checked) details in userData
      const selectedDetails = userData.grievances?.[selectedType] || [];

      // Merge for API call — includes all items
      const grievanceItem = [...new Set([...existingItems, newItem])];
      const description = "";

      // Find grievanceTypeId
      const selectedTypeObj = grievanceTypeWithId.find(
        (t) => t.grievanceType === grievanceType
      );
      const grievanceTypeId = selectedTypeObj?.grievanceTypeId;

      // API call
      const res = await dispatch(
        putGrievanceType({
          grievanceType,
          grievanceItem,
          description,
          grievanceTypeId,
        })
      );

      // Ensure clean list from API
      const updatedItems = Array.isArray(res?.data?.grievanceItem)
        ? res.data.grievanceItem
        : [res?.data?.grievanceItem].filter(Boolean);

      // Flatten + deduplicate
      const mergedDetails = [...new Set([...existingItems, ...updatedItems])];

      // ✅ Update only the list for display, not selection
      setDetailsMap((prev) => ({
        ...prev,
        [selectedType]: mergedDetails,
      }));

      // ✅ Preserve previously selected details (don’t auto-check new one)
      onChange({
        grievances: {
          ...(userData.grievances || {}),
          [selectedType]: selectedDetails,
        },
      });

      // Reset input and hide field
      setNewDetailInput("");
      setShowNewDetailInput(false);

      toast.success("Grievance subcategory added successfully!");
    } catch (err) {
      console.error("Failed to add grievance subcategory:", err);
      toast.error("Failed to add grievance subcategory");
    }
  };

  const toggleDetail = (detail, checked) => {
    const grievances = { ...(userData.grievances || {}) };
    let details = grievances[selectedType] ? [...grievances[selectedType]] : [];

    if (checked) {
      if (!details.includes(detail)) {
        details.push(detail);
      }
    } else {
      details = details.filter((d) => d !== detail);
    }

    grievances[selectedType] = details;
    onChange({ grievances });
  };

  // Helper to sync userInformation structure
  const syncUserInformation = (updatedData = {}) => {
    const types = updatedData.purposeNames || userData.purposeNames || [];
    const details = updatedData.detailNames || userData.detailNames || [];

    const userInformation = [
      {
        userType: types,
        userItems: details,
      },
    ];

    onChange({
      ...userData,
      ...updatedData,
      userInformation,
    });
  };

  const customStyles = {
    control: (base, state) => ({
      ...base,
      color: "#333",
      backgroundColor: "#fff",
      border: "1px solid #ccc",
      borderRadius: "8px",
      padding: "2px 4px",
      fontSize: "14px",
      minHeight: "40px",
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc",
      "&:hover": {
        borderColor: "#999",
      },
    }),

    valueContainer: (base) => ({
      ...base,
      padding: "0 8px",
      gap: "4px",
    }),

    placeholder: (base) => ({
      ...base,
      color: "#666",
      fontSize: "14px",
    }),

    multiValue: (base) => ({
      ...base,
      borderRadius: "6px",
      backgroundColor: "#f0f0f0",
      padding: "2px 6px",
    }),

    multiValueLabel: (base) => ({
      ...base,
      color: "#333",
      fontSize: "13px",
    }),

    multiValueRemove: (base) => ({
      ...base,
      color: "#666",
      cursor: "pointer",
      ":hover": {
        backgroundColor: "#e0e0e0",
        color: "#000",
      },
    }),

    menu: (base) => ({
      ...base,
      borderRadius: "8px",
      border: "1px solid #ccc",
      marginTop: "4px",
      zIndex: 9999,
    }),

    menuList: (base) => ({
      ...base,
      padding: "4px 0",
    }),

    option: (base, state) => ({
      ...base,
      fontSize: "14px",
      padding: "8px 12px",
      cursor: "pointer",
      backgroundColor: state.isSelected
        ? "#e6f0ff"
        : state.isFocused
        ? "#f5f5f5"
        : "#fff",
      color: "#333",
      ":active": {
        backgroundColor: "#e6f0ff",
      },
    }),

    dropdownIndicator: (base) => ({
      ...base,
      color: "#666",
      padding: "0 8px",
      "&:hover": {
        color: "#333",
      },
    }),

    clearIndicator: (base) => ({
      ...base,
      padding: "0 8px",
    }),
  };

  // here fetchPurpose is userTypes
  const fetchUserType = async () => {
    try {
      let res = await dispatch(getUserTypes());
      if (res?.data?.length) {
        const opts = res.data.map((item) => ({
          value: item.name,
          label: item.name,
        }));
        setPurposeOptions(opts);
      } else if (res?.status == 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      // Better error handling to check if err is an array before accessing
      if (Array.isArray(err) && err[0] && (err[0]?.errorCode == "JCMP4003" || err[0]?.errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching purpose."}
            />
          ),
          { icon: false }
        );
      }
      console.log("Error fetching purposes:", err);
    }
  };

  const fetchGrievanceType = async () => {
    try {
      const res = await dispatch(getGrievanceType());

      if (res?.data?.length) {
        // Convert to custom format
        const grievanceTypeList = res.data.map((item) => item.grievanceType);
        // Store both grievanceType and grievanceTypeId
        const grievanceTypeInfo = res.data.map((item) => ({
          grievanceType: item.grievanceType,
          grievanceTypeId: item.grievanceTypeId,
        }));

        setGrievanceTypeWithId(grievanceTypeInfo);
        const grievanceMap = res.data.reduce((acc, item) => {
          acc[item.grievanceType] = item.grievanceItem || [];
          return acc;
        }, {});

        setDetailsMap(grievanceMap);
        setGrievanceTypes(grievanceTypeList);
        const grievancesObject = grievanceTypeList.reduce((acc, type) => {
          acc[type] = [];
          return acc;
        }, {});

        // Update state
        setDetailsMap(grievanceMap);
        setGrievanceTypes(grievanceTypeList);
        const allGrievances = grievanceTypeList.reduce((acc, type) => {
          acc[type] = userData.grievances?.[type] || [];
          return acc;
        }, {});

        // Update userData using onChange
        onChange({
          grievances: {
            ...(userData.grievances || {}),
            ...allGrievances,
          },
        });
      } else if (res?.status === 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      console.error("Error fetching grievance categories:", err);

      if (
        err?.[0]?.errorCode === "JCMP4003" ||
        err?.[0]?.errorCode === "JCMP4001"
      ) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching grievance categories."}
            />
          ),
          { icon: false }
        );
      }
    }
  };

  const fetchUserDetailOptions = async () => {
    try {
      let res = await dispatch(getUserDetails());
      if (res?.data?.length) {
        // setPurposeList(res.userData.searchList);

        const opts = res?.data?.map((item) => ({
          value: item.name,
          label: item.name,
        }));
        setUserDetailOptions(opts);
      } else if (res?.status == 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      // Better error handling to check if err is an array before accessing
      if (Array.isArray(err) && err[0] && (err[0]?.errorCode == "JCMP4003" || err[0]?.errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching purpose."}
            />
          ),
          { icon: false }
        );
      }
      console.log("Error fetching purposes:", err);
    }
  };

  useEffect(() => {
    fetchUserType();
    fetchGrievanceType();
    fetchUserDetailOptions();
    // fetchProcessingActivity();
  }, [dispatch, businessId]);
  useEffect(() => {
    // Get all grievance categories (keys)
    const grievanceCategories = Object.keys(userData.grievances || {});

    // If no type is selected, select the first one automatically
    if (!selectedType && grievanceCategories.length > 0) {
      const firstType = grievanceCategories[0];
      setSelectedType(firstType);

      // Initialize the selected category in state
      onChange({
        grievances: {
          ...(userData.grievances || {}),
          [firstType]: userData.grievances?.[firstType] || [],
        },
      });
    }
  }, [userData.grievances]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (!event.target.closest(".checkbox-dropdown")) {
        setUserTypeDropdownOpen(false);
        setUserDetailDropdownOpen(false);
        setGrievanceTypeDropdownOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div className="user-grievance-container">
      <Tabs
        appearance="normal"
        overflow="fit"
        display="flex"
        alignItems="flex-start"
        onTabChange={(index) => {
          // Map tab index to your activeInfoTab state
          if (index === 0) setActiveInfoTab("grievance");
          else if (index === 1) setActiveInfoTab("user");
        }}
        style={{ marginBottom: "20px" }}
      >
        <TabItem
          label={
            <Text
              appearance="body-xs"
              color={
                activeInfoTab === "grievance" ? "primary" : "primary-grey-80"
              }
            >
              Step 1: Grievance Categories
            </Text>
          }
        />
        <TabItem
          label={
            <Text
              appearance="body-xs"
              color={activeInfoTab === "user" ? "primary" : "primary-grey-80"}
            >
              Step 2: User Information
            </Text>
          }
        />
      </Tabs>
      {activeInfoTab === "grievance" && (
        <div className="grievance-table">
          <div className="table-header">
            <div className="header-cell">
              <Text appearance="body-xs-bold">Grievance category</Text>
              <button
                className="add-btn"
                style={{ marginRight: "30px", fontSize: "16px" }}
                onClick={() => setShowNewTypeInput(true)}
              >
                Add new
              </button>
            </div>
            <div className="header-cell">
              <Text appearance="body-xs-bold">Grievance subcategory</Text>
              <button
                className="add-btn"
                style={{ marginRight: "15px", fontSize: "16px" }}
                onClick={() => setShowNewDetailInput(true)}
              >
                Add new
              </button>
            </div>
          </div>

          <div className="table-search">
            <input
              type="text"
              placeholder="Search grievance category"
              value={searchType}
              onChange={(e) => setSearchType(e.target.value)}
            />
            <input
              type="text"
              placeholder="Search grievance subcategory"
              value={searchDetail}
              onChange={(e) => setSearchDetail(e.target.value)}
            />
          </div>

          <div className="table-body">
            <div className="column">
              {filteredTypes.map((type) => (
                <div style={{ marginBottom: "10px" }} key={type}>
                  <label key={type} className="radio-item">
                    <input
                      type="radio"
                      name="grievanceType"
                      checked={selectedType === type}
                      onChange={() => {
                        setSelectedType(type);
                        onChange({
                          grievances: {
                            ...(userData.grievances || {}),
                            [type]: userData.grievances?.[type] || [],
                          },
                        });
                      }}
                    />
                    {type}
                  </label>
                </div>
              ))}
              {showNewTypeInput && (
                <div className="modal-outer-container">
                  <div
                    className="modal-container-old"
                    style={{ height: "280px" }}
                  >
                    {/* Close button */}
                    <div className="modal-close-btn-container">
                      <ActionButton
                        onClick={() => {
                          setNewTypeInput("");
                          setShowNewTypeInput(false);
                        }}
                        icon={<IcClose />}
                        kind="tertiary"
                        size="small"
                      />
                    </div>

                    {/* Title */}
                    <Text appearance="heading-xs" color="primary-grey-100">
                      Add New Grievance category
                    </Text>

                    {/* Input field */}
                    <div
                      style={{
                        marginTop: "16px",
                        marginBottom: "50px",
                        display: "flex",
                        flexDirection: "column",
                        gap: "12px",
                      }}
                    >
                      <InputFieldV2
                        label="Add Grievance category (Required)"
                        value={newTypeInput}
                        onChange={(e) => {
                          setNewTypeInput(e.target.value);
                          setSelectedType(e.target.value);
                        }}
                        placeholder="Enter new Grievance category..."
                        size="medium"
                      />
                    </div>

                    {/* Action buttons */}
                    <div
                      className="modal-add-btn-container"
                      style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        marginTop: "18px",
                        gap: "10px",
                      }}
                    >
                      <ActionButton
                        label="Cancel"
                        kind="tertiary"
                        onClick={() => {
                          setNewTypeInput("");
                          setShowNewTypeInput(false);
                        }}
                      />
                      <ActionButton
                        label="Add"
                        onClick={() => {
                          addNewTypeInline(newTypeInput);
                          setShowNewTypeInput(false);
                        }}
                        disabled={!newTypeInput.trim()}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="column">
              {filteredDetails.map((detail) => (
                <div
                  key={detail}
                  className="checkbox-item"
                  style={{ marginBottom: "10px" }}
                >
                  <input
                    type="checkbox"
                    checked={
                      userData.grievances?.[selectedType]?.includes(detail) ||
                      false
                    }
                    onChange={(e) => toggleDetail(detail, e.target.checked)}
                  />
                  <span>{detail}</span>
                </div>
              ))}
              {selectedType && showNewDetailInput && (
                <div className="modal-outer-container">
                  <div className="modal-container-old">
                    {/* Close button */}
                    <div className="modal-close-btn-container">
                      <ActionButton
                        onClick={() => {
                          setNewDetailInput("");
                          setShowNewDetailInput(false);
                        }}
                        icon={<IcClose />}
                        kind="tertiary"
                        size="small"
                      />
                    </div>

                    {/* Title */}
                    <Text appearance="heading-xs" color="primary-grey-100">
                      Add New Grievance subcategory
                    </Text>

                    {/* Input fields */}
                    <div
                      style={{
                        marginTop: "16px",
                        marginBottom: "50px",
                        display: "flex",
                        flexDirection: "column",
                        gap: "12px",
                      }}
                    >
                      {/* Auto-filled grievance category */}
                      <InputFieldV2
                        label="Grievance category"
                        value={selectedType}
                        readOnly
                        size="medium"
                      />

                      {/* New  Grievance subcategory name */}
                      <InputFieldV2
                        label="Add Grievance subcategory"
                        value={newDetailInput}
                        onChange={(e) => setNewDetailInput(e.target.value)}
                        placeholder="Enter new Grievance subcategory..."
                        size="medium"
                      />
                    </div>

                    {/* Action buttons */}
                    <div
                      className="modal-add-btn-container"
                      style={{
                        display: "flex",
                        justifyContent: "flex-end",
                        marginTop: "18px",
                        gap: "10px",
                      }}
                    >
                      <ActionButton
                        label="Cancel"
                        kind="tertiary"
                        onClick={() => {
                          setNewDetailInput("");
                          setShowNewDetailInput(false);
                        }}
                      />
                      <ActionButton
                        label="Add"
                        onClick={() => {
                          setNewDetailInput("");
                          addNewDetailInline();
                          setShowNewDetailInput(false);
                        }}
                        disabled={!newDetailInput.trim()}
                      />
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>

          <div className="grievance-bottom">
            <label className="checkbox-item">
              <input
                type="checkbox"
                checked={userData.addDescription || false}
                onChange={(e) => onChange({ addDescription: e.target.checked })}
              />
              Add grievance description
            </label>
            <label className="checkbox-item">
              <input
                type="checkbox"
                checked={userData.allowFiles || false}
                onChange={(e) => onChange({ allowFiles: e.target.checked })}
              />
              Allow uploading files
            </label>
          </div>
        </div>
      )}
      {console.log('userData',userData)}
      {/* User Information */}{" "}
      {activeInfoTab === "user" && (
        <div className="user-info-section">
          <div style={{ 
            background: '#fff4e6', 
            padding: '12px 16px', 
            borderRadius: '8px', 
            marginBottom: '20px',
            border: '1px solid #ffd699'
          }}>
            <Text appearance="body-xs-bold" color="primary-grey-100">
              ℹ️ Configure Data Collection Fields
            </Text>
            <br />
            <Text appearance="body-xs" color="primary-grey-80">
              Select what user information fields you want to collect when someone submits a grievance using this form. These fields will appear in the form for users to fill out.
            </Text>
          </div>
          <div className="user-type-container">
            <label className="form-label">User Type </label>
            <div className="checkbox-dropdown">
              <div
                className="checkbox-dropdown-button"
                onClick={() => setUserTypeDropdownOpen(!userTypeDropdownOpen)}
              >
                <div className="selected-chips">
                  {userData.purposeNames?.length > 0 ? (
                    userData.purposeNames?.map((name, index) => (
                      <span key={index} className="chip">
                        {name}
                        <button
                          type="button"
                          className="chip-remove"
                          onClick={(e) => {
                            e.stopPropagation();
                            const valueToRemove = userData.purposeIds[index];
                            const updated = userData.purposeIds.filter(
                              (id) => id !== valueToRemove
                            );
                            const updatedNames = userData.purposeNames.filter(
                              (_, i) => i !== index
                            );
                            syncUserInformation({
                              purposeIds: updated,
                              purposeNames: updatedNames,
                            });
                          }}
                        >
                          x
                        </button>
                      </span>
                    ))
                  ) : (
                    <span className="placeholder">Select...</span>
                  )}
                </div>
                <div className="dropdown-actions">
                  {userData.purposeNames?.length > 0 && (
                    <button
                      type="button"
                      className="clear-all-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        syncUserInformation({
                          purposeIds: [],
                          purposeNames: [],
                        });
                      }}
                    >
                      x
                    </button>
                  )}
                  <span className="dropdown-arrow">▼</span>
                </div>
              </div>

              {userTypeDropdownOpen && (
                <div className="checkbox-dropdown-content">
                  {/* 🔍 Search Input */}
                  <div
                    className="search-wrapper"
                    style={{ padding: "8px 6px" }}
                  >
                    <SearchBox
                      kind="normal"
                      label="Search user type"
                      onChange={(e) => setUserTypeSearch(e.target.value)}
                      prefix="ic_search"
                    />
                  </div>

                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      cursor: "pointer",
                      gap: "6px",
                      padding: "8px 12px",
                    }}
                    onClick={() => {
                      setAddType("userType");
                      setShowAddModal(true);
                    }}
                  >
                    <Icon
                      color="primary"
                      ic="ic_add"
                      appearance="link"
                      className="mr-2"
                      size="m"
                    />
                    <Text appearance="body-xs-bold" color="primary-60">
                      Add User Type
                    </Text>
                  </div>

                  {/* Filtered Options */}
                  {purposeOptions
                    .filter((opt) =>
                      opt.label
                        .toLowerCase()
                        .includes(userTypeSearch.toLowerCase())
                    )
                    .map((option) => (
                      <div key={option.value} className="checkbox-item-wrapper">
                        <InputCheckbox
                          helperText=""
                          label={option.label}
                          name={option.value}
                          checked={
                            userData.purposeNames?.includes(option.value) || false
                          }
                          onClick={() => {
                            const isChecked = userData.purposeIds?.includes(
                              option.value
                            );
                            let updated = [...(userData.purposeIds || [])];
                            let updatedNames = [
                              ...(userData.purposeNames || []),
                            ];
                            if (!isChecked) {
                              updated.push(option.value);
                              updatedNames.push(option.label);
                            } else {
                              updated = updated.filter(
                                (id) => id !== option.value
                              );
                              updatedNames = updatedNames.filter(
                                (name) => name !== option.label
                              );
                            }
                            syncUserInformation({
                              purposeIds: updated,
                              purposeNames: updatedNames,
                            });
                          }}
                          size="medium"
                          state="none"
                        />
                      </div>
                    ))}
                </div>
              )}
            </div>


          </div>
          <div className="user-detail-container">
            <label className="form-label">User Details</label>
            <div className="checkbox-dropdown">
              <div
                className="checkbox-dropdown-button"
                onClick={() =>
                  setUserDetailDropdownOpen(!userDetailDropdownOpen)
                }
              >
                <div className="selected-chips">
                  {userData.detailNames?.length > 0 ? (
                    userData.detailNames?.map((name, index) => (
                      <span key={index} className="chip">
                        {name}
                        <button
                          type="button"
                          className="chip-remove"
                          onClick={(e) => {
                            e.stopPropagation();
                            const valueToRemove = userData.detailIds[index];
                            const updated = userData.detailIds.filter(
                              (id) => id !== valueToRemove
                            );
                            const updatedNames = userData.detailNames.filter(
                              (_, i) => i !== index
                            );
                            syncUserInformation({
                              detailIds: updated,
                              detailNames: updatedNames,
                            });
                          }}
                        >
                          x
                        </button>
                      </span>
                    ))
                  ) : (
                    <span className="placeholder">Select...</span>
                  )}
                </div>
                <div className="dropdown-actions">
                  {userData.detailNames?.length > 0 && (
                    <button
                      type="button"
                      className="clear-all-button"
                      onClick={(e) => {
                        e.stopPropagation();
                        syncUserInformation({ detailIds: [], detailNames: [] });
                      }}
                    >
                      x
                    </button>
                  )}
                  <span className="dropdown-arrow">▼</span>
                </div>
              </div>

              {userDetailDropdownOpen && (
                <div className="checkbox-dropdown-content">
                  {/* 🔍 Search Input */}
                  <div
                    className="search-wrapper"
                    style={{ padding: "8px 6px" }}
                  >
                    <SearchBox
                      kind="normal"
                      label="Search user detail"
                      onChange={(e) => setUserDetailSearch(e.target.value)}
                      prefix="ic_search"
                    />
                  </div>

                  {/* ➕ Add New User Detail */}
                  <div
                    style={{
                      display: "flex",
                      alignItems: "center",
                      cursor: "pointer",
                      gap: "6px",
                      padding: "8px 12px",
                    }}
                    onClick={() => {
                      setAddType("userDetail");
                      setShowAddModal(true);
                    }}
                  >
                    <Icon
                      color="primary"
                      ic="ic_add"
                      appearance="link"
                      className="mr-2"
                      size="m"
                    />
                    <Text appearance="body-xs-bold" color="primary-60">
                      Add User detail
                    </Text>
                  </div>

                  {/* Filtered Options */}
                  {userDetailOptions
                    .filter((opt) =>
                      opt.label
                        .toLowerCase()
                        .includes(userDetailSearch.toLowerCase())
                    )
                    .map((option) => (
                      <div key={option.value} className="checkbox-item-wrapper">
                        <InputCheckbox
                          helperText=""
                          label={option.label}
                          name={option.value}
                          checked={
                            userData.detailNames?.includes(option.value) || false
                          }
                          onClick={() => {
                            const isChecked = userData.detailIds?.includes(
                              option.value
                            );
                            let updated = [...(userData.detailIds || [])];
                            let updatedNames = [
                              ...(userData.detailNames || []),
                            ];
                            if (!isChecked) {
                              updated.push(option.value);
                              updatedNames.push(option.label);
                            } else {
                              updated = updated.filter(
                                (id) => id !== option.value
                              );
                              updatedNames = updatedNames.filter(
                                (name) => name !== option.label
                              );
                            }
                            syncUserInformation({
                              detailIds: updated,
                              detailNames: updatedNames,
                            });
                          }}
                          size="medium"
                          state="none"
                        />
                      </div>
                    ))}
                </div>
              )}
            </div>
          </div>
                      {showAddModal && (
              <div className="modal-outer-container">
                <div className="modal-container-old">
                  {/* Close button */}
                  <div className="modal-close-btn-container">
                    <ActionButton
                      onClick={() => {
                        setNewEntryName("");
                        setNewEntryDescription("");
                        setShowAddModal(false);
                      }}
                      icon={<IcClose />}
                      kind="tertiary"
                      size="small"
                    />
                  </div>

                  {/* Title */}
                  <Text appearance="heading-xs" color="primary-grey-100">
                    {addType === "userType"
                      ? "Add New User Type"
                      : "Add New User Detail"}
                  </Text>

                  {/* Input fields */}
                  <div
                    style={{
                      marginTop: "16px",
                      marginBottom: "50px",
                      display: "flex",
                      flexDirection: "column",
                      gap: "12px",
                    }}
                  >
                    <InputFieldV2
                      label="Name (Required)"
                      value={newEntryName}
                      onChange={(e) => setNewEntryName(e.target.value)}
                      placeholder="Enter name..."
                      size="medium"
                    />

                    <InputFieldV2
                      label="Description"
                      value={newEntryDescription}
                      onChange={(e) => setNewEntryDescription(e.target.value)}
                      placeholder="Enter short description..."
                      size="medium"
                      // style={{ marginBottom: "15px" }}
                    />
                  </div>

                  {/* Action buttons */}
                  <div
                    className="modal-add-btn-container"
                    style={{
                      display: "flex",
                      justifyContent: "flex-end",
                      marginTop: "18px",
                      gap: "10px",
                    }}
                  >
                    <ActionButton
                      label="Cancel"
                      kind="tertiary"
                      onClick={() => {
                        setNewEntryName("");
                        setNewEntryDescription("");
                        setShowAddModal(false);
                      }}
                    />
                    <ActionButton
                      label="Add"
                      onClick={handleAddNew}
                      disabled={!newEntryName.trim()}
                    />
                  </div>
                </div>
              </div>
            )}
        </div>
      )}
    </div>
  );
};

export default GrievanceContainer;
