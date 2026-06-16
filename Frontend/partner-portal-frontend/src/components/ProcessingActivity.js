import "../styles/masterSetup.css";
import { useState, useRef, useMemo, useCallback, useEffect } from "react";
import Select from "react-select";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import { Text, InputFieldV2, ActionButton, Icon, Spinner } from "../custom-components";
import { IcClose, IcEditPen, IcChevronLeft, IcChevronRight } from "../custom-components/Icon";
import {
  getDataTypes,
  getProcessingActivity,
  getProcessor,
  updateProcessingActivity,
  getTranslations,
} from "../store/actions/CommonAction";
import { createProcessingActivity } from "../store/actions/CommonAction";

import { useDispatch, useSelector } from "react-redux";
import WordLimitedTextarea from "./wordLimitedTextForActivity";
import ProcessingActivityUpdateModal from "./ProcessingActivityUpdateModal";
import { useNavigate } from "react-router-dom";
import { languages } from "../utils/languages";

const ProcessingActivity = ({ selectedLanguage }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const multilingualConfigSaved = useSelector((state) => state.common.multilingualConfigSaved);
  const businessId = useSelector((state) => state.common.business_id);
  const wordLimit = 300;
  const [activityName, setActivityName] = useState("");
  const [activityDetails, setActivityDetails] = useState("");
  const [dataActivityModal, setDataActivityModal] = useState(false);
  const [updatedataActivityModal, setUpdateDataActivityModal] = useState(false);
  const [showMultilingualWarningModal, setShowMultilingualWarningModal] = useState(false);
  
  // State for original, untranslated data from the API
  const [originalProcessingActivityList, setOriginalProcessingActivityList] = useState([]);
  // State for the data displayed in the UI (potentially translated)
  const [processingActivityList, setProcessingActivityList] = useState([]);
  const [processorList, setProcessorList] = useState([]);
  const [dataTypes, setDataTypes] = useState([]);
  const [selectedTypes, setSelectedTypes] = useState([]); // Changed to array for multi-select
  const [selectedItemsByType, setSelectedItemsByType] = useState({}); // Object: { dataTypeName: [items] }
  const [selectedProcessingActivity, setSelecetdProcessingActivity] =
    useState("");
  const [loadActivity, setLoadActivity] = useState(true);
  console.log("Selected processing Activity", selectedProcessingActivity);

  const [showSessionModal, setShowSessionModal] = useState(false);

  // Language selection for add activity modal
  const [modalSelectedLanguage, setModalSelectedLanguage] = useState('en');
  // Store original English values for translation
  const [originalActivityName, setOriginalActivityName] = useState("");
  const [originalActivityDetails, setOriginalActivityDetails] = useState("");
  // Flag to prevent translation while user is typing
  const isTranslatingRef = useRef(false);
  // Loading state for translation
  const [isTranslating, setIsTranslating] = useState(false);


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

  const [selectedProcessor, setSelectedProcessor] = useState("");
  const [selectedProcessorId, setSelectedProcessorId] = useState("");

  const handleEditProcessingActivity = (item) => {
    setUpdateDataActivityModal(true);
    setSelecetdProcessingActivity(item);

    // 👇 Pre-fill Data Types + Items for each type
    if (item.dataTypesList?.length > 0) {
      const types = item.dataTypesList.map(dt => dt.dataTypeName);
      setSelectedTypes(types);
      
      // Create object mapping data type name to its selected items
      const itemsByType = {};
      item.dataTypesList.forEach(dt => {
        itemsByType[dt.dataTypeName] = dt.dataItems || [];
      });
      setSelectedItemsByType(itemsByType);
    }
  };

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
          // Filter to keep only the latest version of each processorActivityId
          const activitiesMap = new Map();
          
          res.data.searchList.forEach((activity) => {
            const existingActivity = activitiesMap.get(activity.processorActivityId);
            
            if (!existingActivity) {
              // First occurrence of this processorActivityId
              activitiesMap.set(activity.processorActivityId, activity);
            } else {
              // Compare versions - keep the one with higher version
              const existingVersion = existingActivity.version || 0;
              const currentVersion = activity.version || 0;
              
              if (currentVersion > existingVersion) {
                // Current activity has higher version, replace
                activitiesMap.set(activity.processorActivityId, activity);
              } else if (currentVersion === existingVersion) {
                // Same version, use updatedAt as tiebreaker (most recent)
                const existingUpdatedAt = new Date(existingActivity.updatedAt || existingActivity.createdAt);
                const currentUpdatedAt = new Date(activity.updatedAt || activity.createdAt);
                
                if (currentUpdatedAt > existingUpdatedAt) {
                  activitiesMap.set(activity.processorActivityId, activity);
                }
              }
            }
          });
          
          // Convert map values to array
          const filteredActivities = Array.from(activitiesMap.values());
          
          setOriginalProcessingActivityList(filteredActivities);
          setProcessingActivityList(filteredActivities);
        } else {
          setOriginalProcessingActivityList([]);
          setProcessingActivityList([]);
        }
        setLoadActivity(false);
      }
      if (res?.status === 403) {
        setLoadActivity(false);
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
      // Better error handling to check if err is an array before accessing
      if (Array.isArray(err) && err[0] && (err[0]?.errorCode == "JCMP4003" || err[0]?.errorCode == "JCMP4001")) {
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
      } else if (Array.isArray(err) && err[0] && err[0]?.errorCode == "JCMP3001") {
        // No data found - this is normal when there are no processing activities yet
        // Let the empty state in the table handle this, don't show error toast
        setOriginalProcessingActivityList([]);
        setProcessingActivityList([]);
        console.log("No processing activities found - showing empty state");
      } else {
        // Show error only for actual errors, not for "no data found"
        // Set empty arrays to prevent undefined errors in UI - don't show error toast
        setOriginalProcessingActivityList([]);
        setProcessingActivityList([]);
        console.log("Error fetching processing activity:", err);
      }
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
      if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
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
      if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
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
  }, [dispatch, businessId]);

  // Translation effect for processing activity list
  useEffect(() => {
    const translateActivities = async () => {
      const lang = selectedLanguage || 'en';
      if (lang === 'en') {
        setProcessingActivityList(originalProcessingActivityList);
        return;
      }
      if (originalProcessingActivityList.length === 0) return;

      const textsToTranslate = originalProcessingActivityList.flatMap(activity => [
        { id: `activity-${activity.processorActivityId}-name`, source: activity.activityName },
        { id: `activity-${activity.processorActivityId}-details`, source: activity.details || "" }
      ]);

      try {
        const translationResult = await dispatch(getTranslations(textsToTranslate, lang));

        if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
          const translatedActivities = originalProcessingActivityList.map(activity => {
            const translatedName = translationResult.output.find(t => t.id === `activity-${activity.processorActivityId}-name`)?.target || activity.activityName;
            const translatedDetails = translationResult.output.find(t => t.id === `activity-${activity.processorActivityId}-details`)?.target || activity.details || "";
            return {
              ...activity,
              activityName: translatedName,
              details: translatedDetails
            };
          });
          setProcessingActivityList(translatedActivities);
        }
      } catch (error) {
        console.error('Error translating activities:', error);
        setProcessingActivityList(originalProcessingActivityList);
      }
    };

    translateActivities();
  }, [selectedLanguage, originalProcessingActivityList, dispatch]);
  const closeDataActivityModal = () => {
    setDataActivityModal(false);
    setSelectedProcessor("");
    setSelectedProcessorId("");
    setSelectedTypes([]);
    setSelectedItemsByType({});
    setActivityName("");
    setActivityDetails("");
    setOriginalActivityName("");
    setOriginalActivityDetails("");
    setModalSelectedLanguage('en');
    setSelecetdProcessingActivity("");
  };

  const openDataActivityModal = () => {
    setDataActivityModal(true);
    // Reset to English when opening modal
    setModalSelectedLanguage('en');
    setOriginalActivityName("");
    setOriginalActivityDetails("");
  };

  const closeUpdateDataActivityModal = () => {
    setUpdateDataActivityModal(false);
    setSelectedProcessor("");
    setSelectedTypes([]);
    setSelectedItemsByType({});
    setActivityName("");
    setActivityDetails("");
    setSelecetdProcessingActivity("");
    fetchProcessingActivity();
  };
  const handleAcivityNameValBlur = (e) => {
    let value = e.target.value;
    setActivityName(value);
    // If typing in English, store as original
    if (modalSelectedLanguage === 'en') {
      setOriginalActivityName(value);
    }
  };

  const handleActivityDetailsChange = (value) => {
    // Don't update if we're currently translating (to prevent flickering)
    if (isTranslatingRef.current) {
      return;
    }
    
    // Always update the display value immediately for responsive typing
    setActivityDetails(value || "");
    
    // If typing in English, store as original English value
    if (modalSelectedLanguage === 'en') {
      setOriginalActivityDetails(value || "");
    }
  };

  // Translation function for input fields
  const translateInputFields = async (name, details, targetLanguage) => {
    // Set flag to prevent user input from interfering
    isTranslatingRef.current = true;
    setIsTranslating(true);
    
    try {
      const targetLang = targetLanguage || modalSelectedLanguage;
      
      if (targetLang === 'en') {
        // If English, use original values
        setActivityName(originalActivityName || name || "");
        setActivityDetails(originalActivityDetails || details || "");
        return;
      }

      // Use original English values for translation
      const nameToTranslate = originalActivityName || name || "";
      const detailsToTranslate = originalActivityDetails || details || "";

      const textsToTranslate = [];
      if (nameToTranslate && nameToTranslate.trim()) {
        textsToTranslate.push({ id: 'activity-name', source: nameToTranslate });
      }
      if (detailsToTranslate && detailsToTranslate.trim()) {
        textsToTranslate.push({ id: 'activity-details', source: detailsToTranslate });
      }

      if (textsToTranslate.length > 0) {
        // Always translate from English to target language
        const translationResult = await dispatch(getTranslations(textsToTranslate, targetLang, 'en'));
        
        if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
          const translatedName = translationResult.output.find(t => t.id === 'activity-name')?.target || nameToTranslate;
          const translatedDetails = translationResult.output.find(t => t.id === 'activity-details')?.target || detailsToTranslate;
          
          // Update all fields at once to prevent flickering
        setActivityName(translatedName);
        setActivityDetails(translatedDetails);
      }
    }
    } catch (error) {
      console.error('Error translating input fields:', error);
      // On error, keep original values
      const nameToTranslate = originalActivityName || name || "";
      const detailsToTranslate = originalActivityDetails || details || "";
      setActivityName(nameToTranslate);
      setActivityDetails(detailsToTranslate);
    } finally {
      // Reset flag after translation completes with a small delay
      // to ensure WordLimitedTextarea has processed the value change
      setTimeout(() => {
        isTranslatingRef.current = false;
        setIsTranslating(false);
      }, 100);
    }
  };

  // Handle language change in modal
  const handleModalLanguageChange = async (e) => {
    const selectedLang = e.target.value;
    const previousLang = modalSelectedLanguage;
    
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
    
    // Update language state first to prevent re-triggering
    setModalSelectedLanguage(selectedLang);
    
    // If switching to English
    if (selectedLang === 'en') {
      // If we have original English values, use them
      if (originalActivityName || originalActivityDetails) {
        setActivityName(originalActivityName || "");
        setActivityDetails(originalActivityDetails || "");
      } else if (previousLang !== 'en' && (activityName || activityDetails)) {
        // No originals stored but we have values, translate current values back to English
        setIsTranslating(true);
        try {
          const textsToTranslate = [];
          if (activityName && activityName.trim()) {
            textsToTranslate.push({ id: 'activity-name', source: activityName });
          }
          if (activityDetails && activityDetails.trim()) {
            textsToTranslate.push({ id: 'activity-details', source: activityDetails });
          }
          
          if (textsToTranslate.length > 0) {
            const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
            
            if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
              const englishName = translationResult.output.find(t => t.id === 'activity-name')?.target || activityName;
              const englishDetails = translationResult.output.find(t => t.id === 'activity-details')?.target || activityDetails;
              
              setOriginalActivityName(englishName);
              setOriginalActivityDetails(englishDetails);
              
              setActivityName(englishName);
              setActivityDetails(englishDetails);
            }
          }
        } catch (error) {
          console.error('Error translating back to English:', error);
        } finally {
          setIsTranslating(false);
        }
      } else {
        // No values at all, clear fields
        setActivityName("");
        setActivityDetails("");
      }
    } 
    // If switching to non-English
    else {
      // First, ensure we have English originals
      // If we don't have originals, we need to get them first
      if (!originalActivityName && !originalActivityDetails) {
        // If previous language was English and we have values, store them as originals
        if (previousLang === 'en' && (activityName || activityDetails)) {
          setOriginalActivityName(activityName || "");
          setOriginalActivityDetails(activityDetails || "");
          // Now translate to the new language
          await translateInputFields(activityName, activityDetails, selectedLang);
        } 
        // If previous language was non-English, translate current values back to English first
        else if (previousLang !== 'en' && (activityName || activityDetails)) {
          setIsTranslating(true);
          try {
            const textsToTranslate = [];
            if (activityName && activityName.trim()) {
              textsToTranslate.push({ id: 'activity-name', source: activityName });
            }
            if (activityDetails && activityDetails.trim()) {
              textsToTranslate.push({ id: 'activity-details', source: activityDetails });
            }
            
            if (textsToTranslate.length > 0) {
              // Translate from previous language to English
              const translationResult = await dispatch(getTranslations(textsToTranslate, 'en', previousLang));
              
              if (translationResult && translationResult.output && Array.isArray(translationResult.output)) {
                const englishName = translationResult.output.find(t => t.id === 'activity-name')?.target || activityName;
                const englishDetails = translationResult.output.find(t => t.id === 'activity-details')?.target || activityDetails;
                
                setOriginalActivityName(englishName);
                setOriginalActivityDetails(englishDetails);
                
                // Now translate from English to the new selected language
                await translateInputFields(englishName, englishDetails, selectedLang);
              }
            }
          } catch (error) {
            console.error('Error translating to English first:', error);
            setIsTranslating(false);
          }
        } else {
          // No values at all, clear fields
          setActivityName("");
          setActivityDetails("");
        }
      } else {
        // We have English originals, translate to new language
        await translateInputFields(originalActivityName, originalActivityDetails, selectedLang);
      }
    }
  };

  const submitActivity = async () => {
    let errors = [];

    // Use original English values for validation and submission
    const nameToValidate = modalSelectedLanguage === 'en' ? activityName : originalActivityName;
    const detailsToValidate = modalSelectedLanguage === 'en' ? activityDetails : originalActivityDetails;

    // Processor Name
    let value1 = nameToValidate.trimStart();
    value1 = value1.replace(/\s+/g, " ");
    const isValid1 = /^(?=.*[A-Za-z])[A-Za-z0-9\s]+$/.test(value1);
    if (!isValid1 || value1.trim() === "") {
      errors.push("Please enter valid activity name.");
    }
    //check for processing activity name duplicacy
    const duplicate = processingActivityList.some(
      (item) =>
        item.activityName.toLowerCase() === nameToValidate.toLowerCase()
    );
    if (duplicate) {
      errors.push("Processing Activity name already exists.");
    }

    // Text area description
    let value2 = detailsToValidate.trimStart();
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

    // Show all errors if any
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

        // Submit with original English values (or current if English is selected)
        const nameToSubmit = modalSelectedLanguage === 'en' ? activityName : originalActivityName;
        const detailsToSubmit = modalSelectedLanguage === 'en' ? activityDetails : originalActivityDetails;
        
        let res = await dispatch(
          createProcessingActivity(
            nameToSubmit,
            selectedProcessor,
            selectedProcessorId,
            dataTypeList,
            detailsToSubmit
          )
        );

        if (res.status == 200 || res.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Processing activity added successfully."}
              />
            ),
            { icon: false }
          );
          setDataActivityModal(false);
          setSelectedProcessor("");
          setSelectedProcessorId("");
          setSelectedTypes([]);
          setSelectedItemsByType({});
          setActivityName("");
          setActivityDetails("");
          setOriginalActivityName("");
          setOriginalActivityDetails("");
          setModalSelectedLanguage('en');
          fetchProcessingActivity();
        }
      } catch (err) {
        if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
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
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Error occured while creating procesing activity."}
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
      <div>
        {/* <div className="custom-divider"></div> */}
        <div className="outer-modal-add-btn">
          {/* <ActionButton
                    kind="tertiary"
                    label="Add new"
                    onClick={openDataActivityModal}
                  /> */}
          <div
            className="outer-modal-add-btn-inner-container"
            onClick={openDataActivityModal}
          >
            <Text appearance="body-s-bold" color="primary-60">
              Add processing activity
            </Text>
          </div>
        </div>
        <div className="custom-table-outer-div">
          <table className="custom-table">
            <thead>
              <tr>
                <th>
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Activity name
                  </Text>
                </th>
                <th>
                  {" "}
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Processor name
                  </Text>
                </th>
                <th>
                  {" "}
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Details
                  </Text>
                </th>
                <th>
                  {" "}
                  <Text appearance="body-xs-bold" color="primary-grey-80">
                    Action
                  </Text>
                </th>
              </tr>
            </thead>
            <tbody>
              {loadActivity ? (
                <tr>
                  <td colSpan="4">
                    <div className="customerActivityLoader">
                      <Spinner
                        kind="normal"
                        label=""
                        labelPosition="right"
                        size="small"
                      />
                    </div>
                  </td>
                </tr>
              ) : processingActivityList.length > 0 ? (
                processingActivityList.map((item, index) => (
                  <tr key={item.dataProcessorId}>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {item.activityName}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {item.processorName}
                      </Text>
                    </td>
                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        {item.details}
                      </Text>
                    </td>
                    <td>
                      <Icon
                        ic={<IcEditPen height={24} width={24} />}
                        color="primary_grey_80"
                        kind="default"
                        size="medium"
                        onClick={() => handleEditProcessingActivity(item)}
                      />
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="4" style={{ textAlign: "center", padding: "40px 20px" }}>
                    <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: "12px" }}>
                      <Text appearance="body-m-bold" color="primary-grey-80">
                        No Processing Activities Found
                      </Text>
                      <Text appearance="body-s" color="primary-grey-60">
                        Click on "Add processing activity" above to create your first entry
                      </Text>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {/* <div className="common-add-btn">
          <ActionButton label="Save" kind="primary"></ActionButton>
        </div> */}
      </div>

      {dataActivityModal && (
        <div className="modal-outer-container">
          <div className="master-set-up-modal-container" style={{ position: 'relative' }}>
            {isTranslating && (
              <div
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                  right: 0,
                  bottom: 0,
                  backgroundColor: 'rgba(255, 255, 255, 0.8)',
                  display: 'flex',
                  justifyContent: 'center',
                  alignItems: 'center',
                  zIndex: 1000,
                  borderRadius: '8px',
                }}
              >
                <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '12px' }}>
                  <Spinner
                    kind="normal"
                    label=""
                    labelPosition="right"
                    size="medium"
                  />
                  <Text appearance="body-s" color="primary-grey-80">
                    Translating...
                  </Text>
                </div>
              </div>
            )}
            <div className="modal-close-btn-container">
              <ActionButton
                onClick={closeDataActivityModal}
                icon={<IcClose />}
                kind="tertiary"
              ></ActionButton>
            </div>
            <Text appearance="heading-xs" color="primary-grey-100">
              {" "}
              Add new activity
            </Text>
            <div className="language-tabs-container">
                <select
                  value={modalSelectedLanguage}
                  onChange={handleModalLanguageChange}
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
              onChange={(e) => {
                const value = e.target.value;
                setActivityName(value);
              }}
              onBlur={handleAcivityNameValBlur}
            ></InputFieldV2>
            <br></br>
            <Text appearance="body-xs" color="primary-grey-80">
              Select processor (Required)
            </Text>
            <div className="dropdown-group-pa" style={{marginBottom:'10px'}}>
              <select
                value={selectedProcessor}
                onChange={handleProcessorChange}
                id="grievance-type"
              >
                <option value="" disabled>
                  Select
                </option>
                {processorList?.map((processor, index) => (
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
              Select data type (Required)
            </Text>
            <div className="dropdown-group-pa" style={{marginBottom:'10px'}}>
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
                    Select data items for {typeName} (Required)
                  </Text>
                  <div className="dropdown-group-pa" style={{marginBottom:'10px'}}>
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

            <WordLimitedTextarea value={activityDetails || ""} onTextChange={handleActivityDetailsChange} />
            <div className="modal-add-btn-container">
              <ActionButton label="Add" onClick={submitActivity}></ActionButton>
            </div>
          </div>
        </div>
      )}

      {updatedataActivityModal && (
        <ProcessingActivityUpdateModal
          processoringActivity={selectedProcessingActivity}
          onClose={closeUpdateDataActivityModal}
        />
      )}

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
export default ProcessingActivity;
