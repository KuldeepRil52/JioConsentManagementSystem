import "../styles/masterSetup.css";
import { useState } from "react";
import Select from "react-select";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useCallback } from "react";
import { Text, InputFieldV2, ActionButton } from "../custom-components";
import { IcClose } from "../custom-components/Icon";
import { Text, InputFieldV2, ActionButton, Icon } from "../custom-components";
import { IcClose } from "../custom-components/Icon";
import { useEffect } from "react";
import {
  getDataTypes,
  getProcessingActivity,
  getProcessor,
  updateProcessingActivity,
  getTranslations,
} from "../store/actions/CommonAction";
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import WordLimitedTextarea from "./wordLimitedtext";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";

import { getTranslations } from "../utils/translationApi";
import { IcChevronRight, IcChevronLeft } from '../custom-components/Icon';
import { IcChevronRight, IcChevronLeft } from '../custom-components/Icon';

import { languages } from "../utils/languages";

const ProcessingActivityUpdateModal = ({ processoringActivity, onClose }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);

  const [activeLanguage, setActiveLanguage] = useState(languages[0].value);
  const [startIndex, setStartIndex] = useState(0);
  const visibleLanguages = 6;
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);

  // State declarations
  const [activityName, setActivityName] = useState(processoringActivity?.activityName || "");
  const [selectedTypes, setSelectedTypes] = useState([]); // Changed to array for multi-select
  const [selectedItemsByType, setSelectedItemsByType] = useState({}); // Object: { dataTypeName: [items] }
  const [selectedProcessingActivityId, setSelectedProcessingActivityId] = useState("");
  const [selectedProcessingActivity, setSelecetdProcessingActivity] = useState(
    processoringActivity?.details || ""
  );
  const [dataTypes, setDataTypes] = useState([]);
  const [processorList, setProcessorList] = useState([]);
  const [processingActivityList, setProcessingActivityList] = useState([]);
  const [loadActivity, setLoadActivity] = useState(true);
  const [showSessionModal, setShowSessionModal] = useState(false);

  useEffect(() => {
    const translateInputs = async () => {
      const textsToTranslate = [
        { id: 'activityName', source: activityName },
        { id: 'selectedProcessingActivity', source: selectedProcessingActivity || "" },
      ].filter(item => item.source);

      if (textsToTranslate.length === 0) return;

      try {
        const translationResult = await dispatch(getTranslations(textsToTranslate, activeLanguage));

        if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
          const translatedName = translationResult.output.find(t => t.id === 'activityName')?.target || activityName;
          const translatedDetails = translationResult.output.find(t => t.id === 'selectedProcessingActivity')?.target || selectedProcessingActivity || "";

          setActivityName(translatedName);
          setSelecetdProcessingActivity(translatedDetails);
        }
      } catch (error) {
        console.error('Error translating inputs:', error);
      }
    };

    if (activeLanguage !== 'en') {
      translateInputs();
    }
  }, [activeLanguage, activityName, selectedProcessingActivity, dispatch]);

  const handleNext = () => {
    if (startIndex + visibleLanguages < languages.length) {
      setStartIndex(startIndex + 1);
    }
  };

  const handlePrev = () => {
    if (startIndex > 0) {
      setStartIndex(startIndex - 1);
    }
  };

  const handleLanguageChange = (e) => {
    const selectedLang = e.target.value;
    const previousLang = activeLanguage;
    
    // Don't do anything if same language is selected
    if (selectedLang === previousLang) {
      return;
    }
    
    // Check if multilingual config is saved (only for non-English languages)
    if (selectedLang !== 'en' && !multilingualConfigSaved) {
      setShowMultilingualWarningModal(true);
      // Reset dropdown to previous value
      e.target.value = previousLang || "";
      return;
    }
    
    // Update language state
    setActiveLanguage(selectedLang);
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

      // remove blue glow
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc", // keep same on focus
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
      maxHeight: "200px",
      overflowY: "auto",
    }),

    option: (base, state) => ({
      ...base,
      fontSize: "14px",
      padding: "8px 12px",
      cursor: "pointer",
      backgroundColor: state.isSelected
        ? "#e6f0ff" // selected background
        : state.isFocused
          ? "#f5f5f5" // hover background
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

  const handleDetailsChange = useCallback((newValue) => {
    setSelecetdProcessingActivity(newValue);
  }, []);

  const [selectedProcessor, setSelectedProcessor] = useState(
    processoringActivity?.processorName || ""
  );
  const [selectedProcessorId, setSelectedProcessorId] = useState(
    processoringActivity?.processorId || ""
  );

  const setDropDownContent = () => {
    if (processoringActivity) {
      // Set processor ID if available
      if (processoringActivity.processorId) {
        setSelectedProcessorId(processoringActivity.processorId);
      } else if (processoringActivity.processorName && processorList.length > 0) {
        // If processorId is not available, find it from processorName
        const processorObj = processorList.find(
          (processor) => processor.dataProcessorName === processoringActivity.processorName
        );
        if (processorObj) {
          setSelectedProcessorId(processorObj.dataProcessorId);
        }
      }
      
      if (processoringActivity.dataTypesList?.length > 0) {
        const types = processoringActivity.dataTypesList.map(dt => dt.dataTypeName);
        setSelectedTypes(types);
        setSelectedProcessingActivityId(processoringActivity.processorActivityId);
        
        // Create object mapping data type name to its selected items
        const itemsByType = {};
        processoringActivity.dataTypesList.forEach(dt => {
          itemsByType[dt.dataTypeName] = dt.dataItems || [];
        });
        setSelectedItemsByType(itemsByType);
      }
    }
  };

  useEffect(() => {
    setDropDownContent();
  }, [processoringActivity, processorList]);

  const handleProcessorChange = (e) => {
    const processorName = e.target.value;
    setSelectedProcessor(processorName);
    
    // Find and store the processor ID
    const selectedProcessorObj = processorList.find(
      (processor) => processor.dataProcessorName === processorName
    );
    if (selectedProcessorObj) {
      setSelectedProcessorId(selectedProcessorObj.dataProcessorId);
      console.log("Selected Processor:", processorName, "ID:", selectedProcessorObj.dataProcessorId);
    } else {
      setSelectedProcessorId("");
    }
  };
  // Get options for all data types
  const dataTypeOptions = dataTypes.map((dt) => ({
    value: dt.name,
    label: dt.name,
  }));

  // Get items for a specific data type
  const getItemsForDataType = (dataTypeName) => {
    const dataType = dataTypes.find((dt) => dt.name === dataTypeName);
    return dataType?.items || [];
  };

  // Get options for a specific data type
  const getOptionsForDataType = (dataTypeName) => {
    const items = getItemsForDataType(dataTypeName);
    return items.map((item) => ({
      value: item,
      label: item,
    }));
  };

  const fetchProcessingActivity = async () => {
    try {
      let res = await dispatch(getProcessingActivity()); // wait for thunk to finish
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          setProcessingActivityList(res.data.searchList);
          setLoadActivity(false);
        } else {
          // Handle case when no processing activity exists
          setProcessingActivityList([]);
          setLoadActivity(false);
        }
      }
      if (res?.status === 403) {
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
      setLoadActivity(false);
      if (Array.isArray(err) && err[0] && (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
        dispatch({ type: CLEAR_SESSION });
        setShowSessionModal(true);
        setTimeout(() => {
          setShowSessionModal(false);
          navigate("/adminLogin");
        }, 7000);
      } else {
        // Set empty array to prevent undefined errors in UI - don't show error toast
        setProcessingActivityList([]);
      }

      console.log("Error fetching processing activity:", err);
    }
  };

  const fetchProcessor = async () => {
    try {
      let res = await dispatch(getProcessor()); // wait for thunk to finish
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          setProcessorList(res.data.searchList);
        }
      }
      if (res?.status === 403) {
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
      if (Array.isArray(err) && err[0] && (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
        dispatch({ type: CLEAR_SESSION });
        setShowSessionModal(true);
        setTimeout(() => {
          setShowSessionModal(false);
          navigate("/adminLogin");
        }, 7000);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching processor."}
            />
          ),
          { icon: false }
        );
      }
      console.log("Error fetching purposes:", err);
    }
  };

  const fetchDataTypes = async () => {
    try {
      let res = await dispatch(getDataTypes()); // wait for thunk to finish
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          // Transform API response into your UI structure

          const formattedData = res.data.searchList.map((item) => ({
            id: item.dataTypeId,
            name: item.dataTypeName, // use API field
            items: item.dataItems, // use API field
          }));

          setDataTypes(formattedData);
        }
      }
      if (res?.status === 403) {
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
      if (Array.isArray(err) && err[0] && (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
        dispatch({ type: CLEAR_SESSION });
        setShowSessionModal(true);
        setTimeout(() => {
          setShowSessionModal(false);
          navigate("/adminLogin");
        }, 7000);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching data types"}
            />
          ),
          { icon: false }
        );
      }
      console.log("Error fetching data types:", err);
    }
  };
  useEffect(() => {
    fetchProcessingActivity();
    fetchProcessor();
    fetchDataTypes();
  }, [dispatch]);
  const closeDataActivityModal = () => {
    setDataActivityModal(false);
    setSelectedProcessor("");
    setSelectedItem([]);
    setActivityName("");
    setActivityDetails("");
    setSelecetdProcessingActivity("");
    setSelectedType("");
  };

  const closeUpdateDataActivityModal = () => {
    setUpdateDataActivityModal(false);
    setSelectedProcessor("");
    setSelectedItem([]);
    setActivityName("");
    setActivityDetails("");
    setSelecetdProcessingActivity("");
    setSelectedType("");
  };
  const openDataActivityModal = () => {
    setDataActivityModal(true);
  };
  const handleAcivityNameChange = (e) => {
    let value = e.target.value;
    setActivityName(value);
  };

  const submitUpdatetdActivity = async () => {
    let errors = [];

    // Activity name is read-only, so no validation needed
    let value1 = (activityName || "").trimStart();

    // Text area description
    let value2 = (selectedProcessingActivity || "").trimStart();
    value2 = value2.replace(/\s+/g, " ");
    const isValid2 = /^(?=.*[A-Za-z]).+$/.test(value2);
    if (!isValid2 || value2.trim() === "") {
      errors.push("Please enter valid description.");
    }

    // Validate data types
    if (selectedTypes.length === 0) {
      errors.push("Please select at least one data type");
    }

    // Validate data items for each selected type
    selectedTypes.forEach((typeName) => {
      const items = selectedItemsByType[typeName] || [];
      if (items.length === 0) {
        errors.push(`Please select at least one data item for ${typeName}`);
      }
    });

    let value4 = selectedProcessor;
    if (value4 == "" || value4 == null || value4 == undefined) {
      errors.push("Please select Processor");
    }

    if (errors.length > 1) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={
              <ul style={{ margin: 0 }}>
                {errors.map((err, idx) => (
                  <li key={idx}>{err}</li>
                ))}
              </ul>
            }
          />
        ),
        { icon: false }
      );
      return;
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
      return;
    } else {
      try {
        // Create dataTypeList array with one object per selected data type
        const dataTypeList = selectedTypes.map((typeName) => {
          const dataType = dataTypes.find((dt) => dt.name === typeName);
          return {
            dataTypeId: dataType?.id || "",
            dataTypeName: typeName,
            dataItems: selectedItemsByType[typeName] || [],
          };
        });

        let res = await dispatch(
          updateProcessingActivity(
            value1, // safe value for activityName
            selectedProcessor,
            selectedProcessorId,
            dataTypeList,
            value2, // safe value for activityDetails
            selectedProcessingActivityId
          )
        );

        console.log("Update processing activity response:", res);

        if (res && (res.status == 200 || res.status == 201)) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Processing activity updated successfully."}
              />
            ),
            { icon: false }
          );
          onClose();
          closeUpdateDataActivityModal();
          setSelectedProcessor("");
          setSelectedItem("");
          setActivityName("");
          setActivityDetails("");
          setSelecetdProcessingActivity("");
          setSelectedType("");
          // Don't call fetchProcessingActivity here as it has its own error handling
          // that shows toasts, which can cause both success and error toasts to appear
          // The parent component should handle refreshing the list
          onClose();
        } else {
          // Handle non-success responses
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Error occurred while updating processing activity."}
              />
            ),
            { icon: false }
          );
        }
      } catch (err) {
        console.error("Error updating processing activity:", err);
        
        // Check for error in different formats
        let errorCode = null;
        let errorMessage = null;
        
        if (Array.isArray(err)) {
          errorCode = err[0]?.errorCode;
          errorMessage = err[0]?.errorMessage || err[0]?.message;
        } else if (err?.errorList && Array.isArray(err.errorList) && err.errorList.length > 0) {
          errorCode = err.errorList[0]?.errorCode;
          errorMessage = err.errorList[0]?.errorMessage || err.errorList[0]?.message;
        } else if (err?.errorCode) {
          errorCode = err.errorCode;
          errorMessage = err.errorMessage || err.message;
        }
        
        if (errorCode == "JCMP4003" || errorCode == "JCMP4001") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expired"}
              />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        } else {
          // Show specific error message if available, otherwise generic message
          const displayMessage = errorMessage || "Error occurred while updating processing activity.";
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={displayMessage}
              />
            ),
            { icon: false }
          );
        }
      }
    }
  };

  return (
    <>
      {showSessionModal && (
        <div className="session-timeout-overlay">
          <div className="session-timeout-modal">
            <Text appearance="heading-s" color="feedback_error_50">
              Session Time Out
            </Text>
            <br></br>
            <Text appearance="body-s" color="primary-80">
              Your session has expired. Please log in again.
            </Text>
          </div>
        </div>
      )}
      <div className="modal-outer-container">
        <div className="master-set-up-modal-container">
          <div className="modal-close-btn-container">
            <ActionButton
              onClick={onClose}
              icon={<IcClose />}
              kind="tertiary"
            ></ActionButton>
          </div>
          <Text appearance="heading-xs" color="primary-grey-100">
            Update activity
          </Text>
          <div className="language-tabs-container">
                <select
                  value={activeLanguage}
                  onChange={handleLanguageChange}
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
            </div>
          <div className="title-margin "></div>
          <InputFieldV2
            label="Activity name (Required)"
            value={activityName}
            onChange={handleAcivityNameChange}
            name="activityName"
            readOnly={true}
          ></InputFieldV2>
          <br></br>
          <Text appearance="body-xs" color="primary-grey-80">
            Select processor (required)
          </Text>
          <div className="dropdown-group"style={{ padding: "0px",marginTop: "0px" }}>
            <select
              value={selectedProcessor}
              onChange={handleProcessorChange}
              id="grievance-type"
              name="processorName"
              style={{width: "100%", marginTop: "0px"}}
            >
              <option value="" disabled>
                Select
              </option>
              {processorList?.map((processor) => (
                <option
                  key={processor.dataProcessorId}
                  value={processor.dataProcessorName}
                >
                  {processor.dataProcessorName}
                </option>
              ))}
            </select>
          </div>

          <Text appearance="body-xs" color="primary-grey-80">
            Select data type (required)
          </Text>
          <div className="dropdown-group"style={{ padding: "0px", marginTop: "0px" }}>
            <Select
              isMulti
              options={dataTypeOptions}
              value={dataTypeOptions.filter((opt) =>
                selectedTypes.includes(opt.value)
              )}
              onChange={(selected) => {
                const newTypes = selected.map((s) => s.value);
                setSelectedTypes(newTypes);
                
                // Remove items for unselected types
                const newItemsByType = { ...selectedItemsByType };
                Object.keys(newItemsByType).forEach((typeName) => {
                  if (!newTypes.includes(typeName)) {
                    delete newItemsByType[typeName];
                  }
                });
                setSelectedItemsByType(newItemsByType);
              }}
              styles={customStyles}
              placeholder="Select data types"
            />
          </div>

          {/* Render data item selectors for each selected data type */}
          {selectedTypes.map((typeName) => {
            const options = getOptionsForDataType(typeName);
            const selectedItems = selectedItemsByType[typeName] || [];
            
            return (
              <div key={typeName} style={{marginBottom:'10px'}}>
                <Text appearance="body-xs" color="primary-grey-80">
                  Select data items for {typeName} (required)
                </Text>
                <div className="dropdown-group"style={{ padding: "0px", marginTop: "0px" }}>
                  <Select
                    isMulti
                    options={options}
                    value={options.filter((opt) =>
                      selectedItems.includes(opt.value)
                    )}
                    onChange={(selected) => {
                      const newItems = selected.map((s) => s.value);
                      setSelectedItemsByType((prev) => ({
                        ...prev,
                        [typeName]: newItems,
                      }));
                    }}
                    styles={customStyles}
                    placeholder={`Select data items for ${typeName}`}
                  />
                </div>
              </div>
            );
          })}

          <WordLimitedTextarea
            value={selectedProcessingActivity}
            onTextChange={setSelecetdProcessingActivity}
          />

          <div className="modal-add-btn-container">
            <ActionButton
              label="Update"
              onClick={submitUpdatetdActivity}
            ></ActionButton>
          </div>
        </div>
      </div>

      {/* Multilingual Warning Modal */}
      {showMultilingualWarningModal && (
        <div className="modal-overlay" onClick={() => setShowMultilingualWarningModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '500px' }}>
            <div className="modal-header">
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: '600' }}>
                Multilingual Support Required
              </h2>
              <button
                className="modal-close-btn"
                onClick={() => setShowMultilingualWarningModal(false)}
                style={{
                  background: 'none',
                  border: 'none',
                  fontSize: '24px',
                  cursor: 'pointer',
                  color: '#666',
                }}
              >
                ×
              </button>
            </div>
            
            <div className="modal-body">
              <Text appearance="body-s" color="primary-grey-80" style={{ marginBottom: '24px' }}>
                You need to configure multilingual support in the consent configuration to enable the translate functionality.
              </Text>

              <div style={{
                display: 'flex',
                gap: '12px',
                justifyContent: 'flex-end',
              }}>
                <Button
                  kind="secondary"
                  size="medium"
                  label="Cancel"
                  onClick={() => setShowMultilingualWarningModal(false)}
                />
                <ActionButton
                  kind="primary"
                  size="medium"
                  label="Go to Configuration"
                  onClick={() => {
                    setShowMultilingualWarningModal(false);
                    navigate("/consent");
                  }}
                />
              </div>
            </div>
          </div>
        </div>
      )}

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
};
export default ProcessingActivityUpdateModal;
