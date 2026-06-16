import { textStyle, FONT_FAMILY_STACK } from "../utils/textStyles";
import { ICON_SIZE } from "../utils/iconSizes";
import "../Styles/loader.css";
import "../Styles/FilterPanel.css";
import * as XLSX from "xlsx-js-style";
import React, { act, useEffect, useRef, useMemo } from "react";
import "../Styles/fiduciaryRequest.css";
import "../Styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { saveAs } from "file-saver";

import { useState } from "react";

import "../Styles/configurePage.css";
import { useDispatch, useSelector } from "react-redux";
import {
  fetchDocumentById,
  generateDigilockerTransactionId,
  getBusinessDetails,
  getConsentHandleByHandleID,
  getConsentsByTemplateId,
  getDigilockerDetails,
  getLogoByBusinessId,
  saveCustomerIdAndenantId,
  searchConsentsByConsentId,
  translateConfiguration,
  translateData,
  withdrawConsentByConsentId,
} from "../store/actions/CommonAction";
import FilterPanel from "./FilterPanel";
import SessionExpiredModal from "./SessionExpiredModal";
import useTranslation, {
  getApiValueFromLanguage,
} from "../hooks/useTranslation";
import {
  FaCheckCircle,
  FaChevronDown,
  FaChevronUp,
  FaEdit,
  FaExclamation,
  FaExclamationCircle,
  FaEye,
  FaInfoCircle,
  FaLanguage,
  FaPencilAlt,
  FaSearch,
  FaTimes,
  FaTimesCircle,
} from "react-icons/fa";
import {
  FiChevronLeft,
  FiChevronRight,
  FiDownload,
  FiFilter,
  FiSearch,
} from "react-icons/fi";

const FiduciaryRequests = () => {
  const parentNameRef = useRef();
  var PageLoaderFlag = 0;
  const [consentLanguageList, setConsentLanguageList] = useState([]);
  const parentIdentityRef = useRef();
  const otpRef = useRef(null);
  const modalRef = useRef(null);
  const [isDigiLockerLoading, setIsDigiLockerLoading] = useState(false);
  const businessName = useSelector((state) => state.common.business_name);
  const [showModal, setShowModal] = useState(false);
  const [isParentChecked, setIsParentChecked] = useState(false);
  const [showParentModal, setShowParentModal] = useState(false);
  const [showAcceptWllModal, setShowAcceptAllModal] = useState(false);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [showRenewModal, setShowRenewModal] = useState(false);
  const [consentIdListForAutoRenew, setConsentidListForAutoRenew] = useState(
    {},
  );
  const [showConfirmationModal, setConfirmationModal] = useState(false);
  const [activeModal, setActiveModal] = useState(null);
  const [targetLanguage, setTargetLanguage] = useState("");
  const [isDarkMode, setIsDarkMode] = useState(false);
  const [parentCheckBox, setParentCheckBox] = useState(false);
  const [showFilter, setShowFilter] = useState(false);

  const [modalHeading, setModalHeading] = useState("Manage Consent");
  const [activeBadge, setActiveBadge] = useState("Always Active");
  const [ageSentence, setAgeSentence] = useState("I am below 18 years of age");
  const [otherAccordionHeader, setOtherAccordionHeader] = useState("Others");
  const [isOpenParent, setIsOpenParent] = useState(false);
  const [selectedKYCMethod, setSelectedKYCMethod] = useState("");
  const [searchText, setSearchText] = useState("");
  const [pendingTranslationLang, setPendingTranslationLang] = useState(null);

  const [filters, setFilters] = useState({
    search: "",
    status: [],
    startDate: "",
    endDate: "",
  });
  const [withdrawConsentBtnText, setWithdrawConsentBtnText] =
    useState("Withdraw Consent");
  const [updateConsentBtnText, setUpdateConsentBtnText] =
    useState("Update Consent");
  const [renewConsentBtnText, setRenewConsentBtnText] = useState("Renew");
  const [customerIdLabel, setCustomerIdLabel] = useState("Customer ID");
  const [topDescription, setTopDescription] = useState("");
  const [rightsDescription, setRightsDescription] = useState("");
  const [permissionDescription, setPermissionDescription] = useState("");
  const [requiredText, setRequiredText] = useState("");
  const [prefereneceArray, setPreferenceArray] = useState([]);
  const [selectedConsentId, setSelectedConsentId] = useState("");
  const [selectedConsentStartDate, setSelectedConsentStartDate] = useState("");
  const [selectedConsentHandleId, setSelectedConsentHandleId] = useState("");
  const [selectedLanguagePreferences, setSelectedLanguagePreferences] =
    useState("");
  const [selectedPrefereneces, setSelectedPreferences] = useState({});
  const [listOfRenewPref, setListOfRenewPref] = useState({});
  const [selectedPReferencesForRenewal, setSelectedPrefrenecesForRenewal] =
    useState({});
  const [selectedConsentStatus, setSelectedConsentStatus] = useState("");
  const [showREquiredDetails, setShowRequiredDetails] = useState(false);
  const [showOhersDetails, setShowOthersDetails] = useState(false);
  const [showLoader, setShowLoader] = useState(false);
  const [lightTheme, setLightTheme] = useState({});
  const [parentalControl, setParentalControl] = useState(false);
  const [dataItemToBeShown, setDataItemToBeShown] = useState(false);
  const [darkModeToBeShown, setDarkModeToBeShown] = useState(false);
  const [validitytoBeShown, setValiditytoBeShown] = useState(false);
  const [withdrawLoader, setWithdrawLoader] = useState(false);
  const [updateLoader, setUpdateLoader] = useState(false);
  const [renewLoader, setRenewLoader] = useState(false);
  const [translationLoader, setTranslationLoader] = useState(false);
  const [pageLoader, setPageLoader] = useState(false);
  const [processActivityNameToBeShown, setProcessActivityNameToBeShown] =
    useState(false);
  const [processorNameToBeShown, setProcessorNameToBeShown] = useState(false);
  const [darkTheme, setDarkTheme] = useState({});
  const [purposeLabel, setPurposeLabel] = useState("Purpose");
  const [durationLabel, setDurationLabel] = useState("Duration");
  const [usedbyLabel, setUsedByLabel] = useState("Used by");
  const [dataItemLabel, setDataItemLabel] = useState("Data item");
  const [dataTypeLabel, setDataTypeLabel] = useState("Data type");
  const [processingActivityLabel, setProcessingActivityLabel] = useState(
    "Processing activity",
  );
  const [withdrawCount, setWithdrawCount] = useState(0);
  const [activeCount, setActiveCount] = useState(0);
  const [expiredCount, setExpiredCount] = useState(0);
  const [tableData, setTableData] = useState([]);
  const [currentLangCode, setCurrentLangCode] = useState("en"); // default English
  const currentLangRef = useRef("en");
  const currentPageLoadRef = useRef(0);
  const [dynamicTranslations, setDynamicTranslations] = useState({}); // Store translated dynamic content
  const customerId = useSelector((state) => state.common.customer_id);
  const tntId = useSelector((state) => state.common.tenant_id);
  const businssId = useSelector((state) => state.common.business_id);
  const secureCode = useSelector((state) => state.common.secure_id);

  const grievanceId = useSelector(
    (state) => state.common.grievance_template_id,
  );
  const parentalRedirectUrl = useSelector(
    (state) => state.common.parental_redirect_url,
  );

  // Translation hook for header language dropdown integration
  const translationInputs = useMemo(
    () => [
      { id: "fid_granted_consents", source: "Granted consents" },
      { id: "fid_consent_badge", source: "Consent" },
      {
        id: "fid_description",
        source:
          "View and respond to your consent requests awaiting your action.",
      },
      { id: "fid_active", source: "Active" },
      { id: "fid_withdrawn", source: "Withdrawn" },
      { id: "fid_expired", source: "Expired" },
      { id: "fid_consent_id", source: "Consent ID" },
      { id: "fid_created_date", source: "Created date" },
      { id: "fid_expiry_date", source: "Expiry date" },
      { id: "fid_status", source: "Status" },
      { id: "fid_actions", source: "Actions" },
      { id: "fid_no_data", source: "No Data to Display" },
      {
        id: "fid_search_placeholder",
        source: "Search data item, type and purpose",
      },
      { id: "fid_page", source: "Page" },
      { id: "fid_of", source: "of" },
      { id: "fid_status_active", source: "ACTIVE" },
      { id: "fid_status_withdrawn", source: "WITHDRAWN" },
      { id: "fid_status_expired", source: "EXPIRED" },
      { id: "fid_status_pending", source: "PENDING" },
      // Expanded row labels
      { id: "fid_purpose_name", source: "Purpose Name" },
      { id: "fid_data_type", source: "Data type" },
      { id: "fid_data_item", source: "Data Item" },
      { id: "fid_data_used_by", source: "Data used by" },
      { id: "fid_pref_accepted", source: "ACCEPTED" },
      { id: "fid_pref_not_accepted", source: "NOTACCEPTED" },
      { id: "fid_data_status", source: "Status" },
    ],
    [],
  );

  // Helper to translate dynamic status values
  const getTranslatedStatus = (status) => {
    const statusMap = {
      ACTIVE: "fid_status_active",
      WITHDRAWN: "fid_status_withdrawn",
      EXPIRED: "fid_status_expired",
      PENDING: "fid_status_pending",
    };
    const translationId = statusMap[status?.toUpperCase()];
    return translationId ? getTranslation(translationId, status) : status;
  };

  // Helper to translate preference status values
  const getTranslatedPrefStatus = (status) => {
    const statusMap = {
      ACCEPTED: "fid_pref_accepted",
      NOTACCEPTED: "fid_pref_not_accepted",
    };
    const translationId = statusMap[status?.toUpperCase()];
    return translationId ? getTranslation(translationId, status) : status;
  };

  // Helper to get dynamic translation with fallback
  const getDynamicTranslation = (text) => {
    if (!text || currentLanguage === "ENGLISH") return text;
    return dynamicTranslations[text] || text;
  };

  // Function to translate dynamic table data
  const translateDynamicData = async (consents, targetLang) => {
    if (!consents || consents.length === 0 || targetLang === "en") {
      setDynamicTranslations({});
      return;
    }

    // Collect all unique translatable text from consents
    const uniqueTexts = new Set();
    consents.forEach((consent) => {
      consent.preferences?.forEach((pref) => {
        pref.purposeList?.forEach((purpose) => {
          if (purpose.purposeInfo?.purposeName) {
            uniqueTexts.add(purpose.purposeInfo.purposeName);
          }
        });
        pref.processorActivityList?.forEach((pa) => {
          if (pa.processActivityInfo?.processorName)
            uniqueTexts.add(pa.processActivityInfo.processorName);
          pa.processActivityInfo?.dataTypesList?.forEach((dt) => {
            if (dt.dataTypeName) uniqueTexts.add(dt.dataTypeName);
            dt.dataItems?.forEach((item) => uniqueTexts.add(item));
          });
        });
      });
    });

    if (uniqueTexts.size === 0) return;

    // Create translation inputs
    const inputs = Array.from(uniqueTexts).map((text, idx) => ({
      id: `dynamic_${idx}`,
      source: text,
    }));

    try {
      const response = await dispatch(translateData("en", targetLang, inputs));
      if (response?.status === 200 && response?.data?.output) {
        const translatedMap = {};
        response.data.output.forEach((item, idx) => {
          const originalText = inputs[idx].source;
          translatedMap[originalText] = item.target;
        });
        setDynamicTranslations(translatedMap);
      }
    } catch (error) {
      console.log("Error translating dynamic data:", error);
    }
  };

  const { getTranslation, translateContent, currentLanguage } =
    useTranslation(translationInputs);

  // Sync with header language dropdown (static translations only)
  useEffect(() => {
    if (currentLanguage && currentLanguage !== "ENGLISH") {
      const targetLang = getApiValueFromLanguage(currentLanguage);
      if (targetLang !== currentLangCode) {
        setCurrentLangCode(targetLang);
        translateContent(translationInputs, currentLanguage);
      }
    } else if (currentLanguage === "ENGLISH" && currentLangCode !== "en") {
      setCurrentLangCode("en");
      setDynamicTranslations({}); // Reset dynamic translations for English
    }
  }, [currentLanguage, currentLangCode, translationInputs, translateContent]);

  var cusId;
  var tenantId;
  var businessId;
  var grievanceTemplateId;
  var scCode;
  var recievedCallBackUrl;

  const [allSelected, setAllSelected] = useState(false);

  function isExpiringSoon(endDate) {
    const end = new Date(endDate);
    const today = new Date();

    // Difference in milliseconds
    const diffMs = end - today;

    // Convert to days
    const diffDays = diffMs / (1000 * 60 * 60 * 24);

    return diffDays <= 30 && diffDays >= 0;
  }

  const formatToIST = (utcDateString) => {
    const normalized = utcDateString.endsWith("Z")
      ? utcDateString
      : `${utcDateString}Z`;

    const date = new Date(normalized);

    return date.toLocaleString("en-IN", {
      timeZone: "Asia/Kolkata",
      hour12: false,
      year: "numeric",
      month: "short",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
    });
  };
  const [sessionExpired, setSessionExpired] = useState(false);
  const [consentsByTempId, setConsentsByTempId] = useState([]);

  // Translate dynamic table data when language or data changes
  useEffect(() => {
    if (
      currentLanguage &&
      currentLanguage !== "ENGLISH" &&
      consentsByTempId &&
      consentsByTempId.length > 0
    ) {
      const targetLang = getApiValueFromLanguage(currentLanguage);
      translateDynamicData(consentsByTempId, targetLang);
    }
  }, [currentLanguage, consentsByTempId]);

  const [currentPage, setCurrentPage] = useState(1);
  const rowsPerPage = 10;
  var totalPages;
  var startIdx;
  var endIdx;
  var paginatedConsents;
  const [expandedRow, setExpandedRow] = useState(null);
  const [loadLogs, setLoadLogs] = useState(true);
  const [withdrawConsentLoader, setWithdrawConsentLoader] = useState(false);
  const [documentName, setDocumentName] = useState("");
  const [documentId, setDocumentId] = useState("");
  const [logoUrl, setLogoUrl] = useState("");
  const [linkFontColor, setLinkFont] = useState("");

  const [selectedLang, setSelectedLang] = useState("ENGLISH");
  const dispatch = useDispatch();
  const [isOpen, setIsOpen] = useState(false);

  const [currentModalStep, setCurrentModalStep] = useState(1);
  const activeTheme = isDarkMode ? darkTheme : lightTheme;

  const [parentName, setParentName] = useState("");
  const [parentIdentity, setParentIdentity] = useState("");
  const [otp, setOtp] = useState("");

  const [digilockerClientId, setDigilockerClientId] = useState("");
  const [digilockerClientSecret, setDigilockerClientSecret] = useState("");
  const [digilockerCodeVerifier, setDigilockerCodeVerifier] = useState("");
  const [digilockerRedirectUri, setDigilockerRedirectUri] = useState("");

  const languages = [
    { value: "ENGLISH", label: "English", apiValue: "en" },
    { value: "HINDI", label: "Hindi", apiValue: "hi" },
    { value: "BENGALI", label: "Bengali", apiValue: "bn" },
    { value: "TAMIL", label: "Tamil", apiValue: "ta" },
    { value: "TELUGU", label: "Telugu", apiValue: "te" },
    { value: "KANNADA", label: "Kannada", apiValue: "kn" },
    { value: "MALAYALAM", label: "Malayalam", apiValue: "ml" },
    { value: "GUJARATI", label: "Gujarati", apiValue: "gu" },
    { value: "MARATHI", label: "Marathi", apiValue: "mr" },
    { value: "ODIA", label: "Odia", apiValue: "or" },
    { value: "PUNJABI", label: "Punjabi", apiValue: "pa" },
    { value: "ASSAMESE", label: "Assamese", apiValue: "as" },
    { value: "NEPALI", label: "Nepali", apiValue: "ne" },
    { value: "SINDHI", label: "Sindhi", apiValue: "sd" },
    { value: "KASHMIRI", label: "Kashmiri", apiValue: "ks" },
    { value: "DOGRI", label: "Dogri", apiValue: "doi" },
    { value: "KONKANI", label: "Konkani", apiValue: "gom" },
    { value: "MAITHILI", label: "Maithili", apiValue: "mai" },
    { value: "SANSKRIT", label: "Sanskrit", apiValue: "sa" },
    { value: "SANTALI", label: "Santali", apiValue: "sat" },
    { value: "BODO", label: "Bodo", apiValue: "brx" },
    { value: "MANIPURI", label: "Manipuri", apiValue: "mni" },
  ];

  const generateAutoRenewStatus = (consentsByTemplateId) => {
    const result = {};

    consentsByTemplateId.forEach((consent) => {
      let isExpiring = false;

      consent.preferences.forEach((pref) => {
        if (pref.autoRenew) {
          const remainingDays = calculateValidityInDays(pref.endDate);

          if (!isNaN(remainingDays) && remainingDays < 30) {
            isExpiring = true;
          }
        }
      });

      result[consent.consentId] = isExpiring;
    });

    setConsentidListForAutoRenew(result);
  };

  function closeParentModal() {
    setShowParentModal(false);

    function closeParentModal() {
      setShowParentModal(false);
      setIsParentChecked(false);

      setCurrentModalStep(1);

      setSelectedKYCMethod(null);
      if (parentNameRef.current) parentNameRef.current.value = "";
      if (parentIdentityRef.current) parentIdentityRef.current.value = "";
      if (otpRef.current) otpRef.current.value = "";
    }

    setCurrentModalStep(1);

    setSelectedKYCMethod(null);
    if (parentNameRef.current) parentNameRef.current.value = "";
    if (parentIdentityRef.current) parentIdentityRef.current.value = "";
    if (otpRef.current) otpRef.current.value = "";
  }

  async function generateCodeChallenge(codeVerifier) {
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    const hashBuffer = await crypto.subtle.digest("SHA-256", data);
    const base64Hash = btoa(String.fromCharCode(...new Uint8Array(hashBuffer)))
      .replace(/\+/g, "-") // replace '+' with '-'
      .replace(/\//g, "_") // replace '/' with '_'
      .replace(/=+$/, ""); // remove '=' padding

    return base64Hash;
  }

  const generateConsentMetaId = async () => {
    try {
      let response = await dispatch(
        generateDigilockerTransactionId(
          selectedConsentHandleId,
          selectedLanguagePreferences,
          selectedPrefereneces,
        ),
      );
      return response?.data?.consentMetaId;
    } catch (error) {
      console.log("Error in generating consentMetaId", error);
      throw error;
    }
  };

  const checkParentKYC = (checked) => {
    if (parentCheckBox) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"KYC for this customer id is already done."}
          />
        ),
        { icon: false },
      );
      return;
    } else {
      setIsParentChecked(checked);

      if (checked) {
        setShowParentModal(true);
        setShowUpdateModal(false);
      } else {
        closeParentModal();
        setShowUpdateModal(true);
      }
    }
  };
  const openDocument = async () => {
    try {
      const blob = await dispatch(fetchDocumentById(documentId));
      const pdfUrl = URL.createObjectURL(blob);

      if (pdfUrl) {
        window.open(pdfUrl, "_blank");
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error Occurred while fetching document"}
            />
          ),
          { icon: false },
        );
      }
    } catch (err) {
      console.log("Error opening document:", err);
    }
  };

  const withdrawConsent = async () => {
    setWithdrawLoader(true);

    // const filteredPreferences = Object.fromEntries(
    //   Object.entries(selectedPrefereneces).filter(
    //     ([_, value]) => value === "ACCEPTED"
    //   )
    // );

    try {
      let status = "WITHDRAWN";
      let res = await dispatch(
        withdrawConsentByConsentId(
          selectedConsentId,
          selectedLang,
          selectedPrefereneces,
          status,
        ),
      );

      if (res?.status === 202) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Consent withdrawn successfully!"}
            />
          ),
          { icon: false },
        );
        currentLangRef.current = "en";
        setShowModal(false);
        setShowUpdateModal(false);
        setConfirmationModal(false);
        setWithdrawLoader(false);
        setTimeout(() => {
          window.location.reload();
        }, 3000);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to withdraw consent. Please try again."}
            />
          ),
          { icon: false },
        );
        setWithdrawLoader(false);
        return;
      }
    } catch (err) {
      console.log("Withdraw API failed:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"An error occurred while withdrawing consent."}
          />
        ),
        { icon: false },
      );
      setWithdrawLoader(false);
      return;
    }
  };

  const handleConfirm = () => {
    setShowModal(false);
    setConfirmationModal(true);
    setActiveModal("withdraw");
  };

  const handleUpdateConfirm = () => {
    setShowUpdateModal(false);
    setConfirmationModal(true);
    setActiveModal("update");
  };

  const updateConsent = async () => {
    try {
      setUpdateLoader(true);
      let status = "ACTIVE";
      const filteredPreferences = Object.fromEntries(
        Object.entries(selectedPrefereneces).filter(
          ([_, value]) => value === "ACCEPTED",
        ),
      );

      if (Object.keys(filteredPreferences).length === 0) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Please select at least one purpose."}
            />
          ),
          { icon: false },
        );
        setUpdateLoader(false);
        return;
      } else {
        let res = await dispatch(
          withdrawConsentByConsentId(
            selectedConsentId,
            selectedLang,
            filteredPreferences,
            status,
          ),
        );

        if (res?.status === 202) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Consent updated successfully!"}
              />
            ),
            { icon: false },
          );
          currentLangRef.current = "en";

          setShowModal(false);
          setShowUpdateModal(false);
          setConfirmationModal(false);
          setUpdateLoader(false);
          setTimeout(() => {
            window.location.reload();
          }, 3000);
        } else {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Failed to update consent. Please try again."}
              />
            ),
            { icon: false },
          );
          setUpdateLoader(false);
          return;
        }
      }
    } catch (err) {
      console.log("Withdraw API failed:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"An error occurred while updating consent."}
          />
        ),
        { icon: false },
      );
      setUpdateLoader(false);
      return;
    }
  };

  const renewConsent = async () => {
    try {
      setRenewLoader(true);

      const selectedRenewIds = Object.keys(listOfRenewPref).filter(
        (id) => listOfRenewPref[id] === "RENEW",
      );

      if (selectedRenewIds.length === 0) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Please select at least one purpose for renewal."}
            />
          ),
          { icon: false },
        );
        setRenewLoader(false);
        return;
      }

      const updatedPreferences = { ...selectedPReferencesForRenewal };

      selectedRenewIds.forEach((id) => {
        updatedPreferences[id] = "RENEW";
      });

      const res = await dispatch(
        withdrawConsentByConsentId(
          selectedConsentId,
          selectedLang,
          updatedPreferences,
          "ACTIVE",
        ),
      );

      if (res?.status === 202) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Consent renewed successfully!"}
            />
          ),
          { icon: false },
        );

        setShowRenewModal(false);
        setRenewLoader(false);

        setTimeout(() => {
          window.location.reload();
        }, 3000);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to renew consent. Please try again."}
            />
          ),
          { icon: false },
        );
        setRenewLoader(false);
      }
    } catch (err) {
      console.log("Renew API failed:", err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"An error occurred while updating consent."}
          />
        ),
        { icon: false },
      );
      setRenewLoader(false);
    }
  };

  const handleUpdateConsent = () => {
    updateConsent();
  };
  const handleRenewConsent = () => {
    renewConsent();
  };
  const handleConfirmationModalClose = () => {
    if (activeModal === "withdraw") {
      setShowModal(true);
    }
    if (activeModal === "update") {
      setShowUpdateModal(true);
    }
    setConfirmationModal(false);
  };

  const closeWithdrawConsentModal = () => {
    currentLangRef.current = "en";
    setShowModal(false);
    setLightTheme({});
    setIsDarkMode(false);
    setSelectedLang("ENGLISH");
    setCustomerIdLabel("Customer ID");
    setActiveBadge("Always Active");
    setOtherAccordionHeader("Others");
    setPurposeLabel("");
    setPurposeLabel("Purpose");
    setDurationLabel("Duration");
    setUsedByLabel("Used by");
    setDataItemLabel("Data item");
    setDataTypeLabel("Data type");
    setModalHeading("Manage Consent");
    setProcessingActivityLabel("Processing activity");
    setWithdrawConsentBtnText("Withdraw Consent");
    setUpdateConsentBtnText("Update Consent");
    setDocumentName("");
    setAgeSentence("I am below 18 years of age");
  };

  const callBhashiniAPI = async (lang) => {
    if (currentPageLoadRef.current == 1) {
      setPageLoader(true);
      currentPageLoadRef.current = 0;
    } else {
      setTranslationLoader(true);
    }

    const options = [
      { value: "ENGLISH", label: "English", apiValue: "en" },
      { value: "HINDI", label: "Hindi", apiValue: "hi" },
      { value: "BENGALI", label: "Bengali", apiValue: "bn" },
      { value: "TAMIL", label: "Tamil", apiValue: "ta" },
      { value: "TELUGU", label: "Telugu", apiValue: "te" },
      { value: "KANNADA", label: "Kannada", apiValue: "kn" },
      { value: "MALAYALAM", label: "Malayalam", apiValue: "ml" },
      { value: "GUJARATI", label: "Gujarati", apiValue: "gu" },
      { value: "MARATHI", label: "Marathi", apiValue: "mr" },
      { value: "ODIA", label: "Odia", apiValue: "or" },
      { value: "PUNJABI", label: "Punjabi", apiValue: "pa" },
      { value: "ASSAMESE", label: "Assamese", apiValue: "as" },
      { value: "NEPALI", label: "Nepali", apiValue: "ne" },
      { value: "SINDHI", label: "Sindhi", apiValue: "sd" },
      { value: "KASHMIRI", label: "Kashmiri", apiValue: "ks" },
      { value: "DOGRI", label: "Dogri", apiValue: "doi" },
      { value: "KONKANI", label: "Konkani", apiValue: "gom" },
      { value: "MAITHILI", label: "Maithili", apiValue: "mai" },
      { value: "SANSKRIT", label: "Sanskrit", apiValue: "sa" },
      { value: "SANTALI", label: "Santali", apiValue: "sat" },
      { value: "BODO", label: "Bodo", apiValue: "brx" },
      { value: "MANIPURI", label: "Manipuri", apiValue: "mni" },
    ];
    function toText(value) {
      if (Array.isArray(value)) {
        return value.join(", ");
      } else if (typeof value === "object" && value !== null) {
        if ("value" in value && "unit" in value) {
          return `${value.value} ${value.unit}`;
        }

        return JSON.stringify(value);
      } else {
        return value;
      }
    }

    const selectedOption = options.find((opt) => opt.value === lang);
    const targetLang = selectedOption ? selectedOption.apiValue : "";
    setTargetLanguage(targetLang);
    console.log("Target language for translation:", targetLang);

    if (currentLangRef.current === targetLang) {
      setPageLoader(false);
      setTranslationLoader(false);
      if (modalRef.current === "view") {
        setShowModal(true);
      }
      if (modalRef.current === "edit") {
        setShowUpdateModal(true);
      }
      if (modalRef.current === "renew") {
        setShowRenewModal(true);
      }
      return;
    }

    const staticInputs = [
      { id: "topDesc", source: topDescription },
      { id: "permissionDesc", source: permissionDescription },
      { id: "rightsDesc", source: rightsDescription },
      { id: "parentCheck", source: ageSentence },
      { id: "updateConsent", source: updateConsentBtnText },
      { id: "withdrawConsent", source: withdrawConsentBtnText },
      { id: "renewConsent", source: renewConsentBtnText },
      { id: "badge", source: activeBadge },
      { id: "requiredHeader", source: requiredText },
      { id: "othersHeader", source: otherAccordionHeader },
      { id: "customerId", source: customerIdLabel },
      { id: "modalHeading", source: modalHeading },
      { id: "documentTitle", source: documentName },
      // { id: "click", source: "click here" },
      // add label translations
      { id: "Purpose", source: purposeLabel },
      { id: "Data item", source: dataItemLabel },
      { id: "Processing activity", source: processingActivityLabel },
      { id: "Used by", source: usedbyLabel },
      { id: "Duration", source: durationLabel },
      { id: "Data type", source: dataTypeLabel },
      { id: "Id", source: "Id" },
    ];

    const dynamicInputs = formData
      .map((item, index) => {
        const reqNo = index + 1;

        // Replace flatMap → map + reduce
        const processorActivityInputs = item.processorActivities
          .map((activity, actIdx) => {
            const dataTypeInputs = activity.dataTypes.map((dt, dtIdx) => ({
              id: `dataTypeName_req_${reqNo}_${actIdx}_${dtIdx}`,
              source: dt.dataTypeName,
            }));

            return [
              {
                id: `activityName_req_${reqNo}_${actIdx}`,
                source: toText(activity.activityName),
              },
              {
                id: `processorName_req_${reqNo}_${actIdx}`,
                source: toText(activity.processorName),
              },
              ...activity.dataTypes.map((dt, dtIdx) => ({
                id: `dataItems_req_${reqNo}_${actIdx}_${dtIdx}`,
                source: toText(dt.dataItems),
              })),

              // <-- add datatype names here
              ...dataTypeInputs,
            ];
          })
          .reduce((acc, curr) => acc.concat(curr), []);

        return [
          { id: `purpose_req_${reqNo}`, source: toText(item.purpose) },
          { id: `usedBy_req_${reqNo}`, source: toText(item.usedBy) },
          { id: `duration_req_${reqNo}`, source: toText(item.duration) },
          { id: `id_req_${reqNo}`, source: item.id },
          ...processorActivityInputs,
        ];
      })
      .reduce((acc, curr) => acc.concat(curr), []);

    const dynamicInputs2 = formOtherData
      .map((item, index) => {
        const reqNo = index + 1;

        const processorActivityInputs = item.processorActivities
          .map((activity, actIdx) => {
            // Add datatype names (same as main list)
            const dataTypeInputs = activity.dataTypes.map((dt, dtIdx) => ({
              id: `dataTypeName_otr_${reqNo}_${actIdx}_${dtIdx}`,
              source: dt.dataTypeName,
            }));

            return [
              {
                id: `activityName_otr_${reqNo}_${actIdx}`,
                source: toText(activity.activityName),
              },
              {
                id: `processorName_otr_${reqNo}_${actIdx}`,
                source: toText(activity.processorName),
              },
              ...activity.dataTypes.map((dt, dtIdx) => ({
                id: `dataItems_otr_${reqNo}_${actIdx}_${dtIdx}`,
                source: toText(dt.dataItems),
              })),

              // Add datatype inputs here (same as req version)
              ...dataTypeInputs,
            ];
          })
          .reduce((acc, curr) => acc.concat(curr), []); // flatten

        return [
          { id: `purpose_otr_${reqNo}`, source: toText(item.purpose) },
          { id: `usedBy_otr_${reqNo}`, source: toText(item.usedBy) },
          { id: `duration_otr_${reqNo}`, source: toText(item.duration) },
          { id: `id_otr_${reqNo}`, source: item.id },
          ...processorActivityInputs,
        ];
      })
      .reduce((acc, curr) => acc.concat(curr), []);

    const finalInputs = [...staticInputs, ...dynamicInputs, ...dynamicInputs2];

    const response = await dispatch(
      translateData(currentLangRef.current, targetLang, finalInputs),
    );
    setTranslationLoader(false);
    setPageLoader(false);

    const data2 = await response?.data;
    currentLangRef.current = targetLang;
    console.log("current Lang code after translation", currentLangCode);

    const purposeText =
      data2.output.find((o) => o.id === "Purpose")?.target || "topDescription";
    setPurposeLabel(purposeText);

    const dataItemText =
      data2.output.find((o) => o.id === "Data item")?.target ||
      "topDescription";
    setDataItemLabel(dataItemText);

    const dataTypeText =
      data2.output.find((o) => o.id === "Data type")?.target ||
      "topDescription";
    setDataTypeLabel(dataTypeText);

    const processingActivityText =
      data2.output.find((o) => o.id === "Processing activity")?.target ||
      "topDescription";
    setProcessingActivityLabel(processingActivityText);

    const usedByText =
      data2.output.find((o) => o.id === "Used by")?.target || "topDescription";
    setUsedByLabel(usedByText);

    const durationText =
      data2.output.find((o) => o.id === "Duration")?.target || "topDescription";
    setDurationLabel(durationText);

    const topDesc =
      data2.output.find((o) => o.id === null)?.target ||
      data2.output.find((o) => o.id === "topDesc")?.target ||
      "topDescription";

    setTopDescription(topDesc);

    const pDesc =
      data2.output.find((o) => o.id === "permissionDesc")?.target ||
      "Description";
    setPermissionDescription(pDesc);

    const rDesc =
      data2.output.find((o) => o.id === "rightsDesc")?.target || "Description";
    setRightsDescription(rDesc);

    const docName =
      data2.output.find((o) => o.id === "documentTitle")?.target ||
      "topDescription";
    setDocumentName(docName);
    const cus =
      data2.output.find((o) => o.id === "customerId")?.target || "Description";
    setCustomerIdLabel(cus);

    const heading =
      data2.output.find((o) => o.id === "modalHeading")?.target ||
      "Description";
    setModalHeading(heading);

    const badgeLabel =
      data2.output.find((o) => o.id === "badge")?.target || "Description";
    setActiveBadge(badgeLabel);

    const reqLabel =
      data2.output.find((o) => o.id === "requiredHeader")?.target ||
      "Description";
    setRequiredText(reqLabel);

    const otrLabel =
      data2.output.find((o) => o.id === "othersHeader")?.target ||
      "Description";
    setOtherAccordionHeader(otrLabel);

    const ageDesc =
      data2.output.find((o) => o.id === "parentCheck")?.target || "Description";
    setAgeSentence(ageDesc);

    const wconsent =
      data2.output.find((o) => o.id === "withdrawConsent")?.target ||
      "Description";
    setWithdrawConsentBtnText(wconsent);

    const renewText =
      data2.output.find((o) => o.id === "renewConsent")?.target ||
      "Description";
    setRenewConsentBtnText(renewText);
    const upconsent =
      data2.output.find((o) => o.id === "updateConsent")?.target ||
      "Description";
    setUpdateConsentBtnText(upconsent);

    const translatedTemplateList = formData.map((item, index) => {
      const reqNo = index + 1;

      const getTranslated = (id) =>
        data2.output.find((o) => o.id === id)?.target?.trim() || null; // return null instead of ""

      const translatedProcessorActivities = item.processorActivities.map(
        (activity, actIdx) => {
          const translatedActivityName =
            getTranslated(`activityName_req_${reqNo}_${actIdx}`) ||
            activity.activityName;

          const translatedProcessorName =
            getTranslated(`processorName_req_${reqNo}_${actIdx}`) ||
            activity.processorName;

          // Translate dataTypes
          const translatedDataTypes = activity.dataTypes.map((dt, dtIdx) => {
            const dtNameId = `dataTypeName_req_${reqNo}_${actIdx}_${dtIdx}`;
            const translatedDataTypeName =
              getTranslated(dtNameId) || dt.dataTypeName;

            // FIX: translate whole list at once
            const dataItemsId = `dataItems_req_${reqNo}_${actIdx}_${dtIdx}`;
            const translatedItemsString = getTranslated(dataItemsId);

            const translatedDataItems = translatedItemsString
              ? translatedItemsString.split(",").map((i) => i.trim())
              : dt.dataItems;

            return {
              dataTypeName: translatedDataTypeName,
              dataItems: translatedDataItems,
            };
          });

          return {
            activityName: translatedActivityName,
            processorName: translatedProcessorName,
            dataTypes: translatedDataTypes,
          };
        },
      );

      return {
        purpose: [
          getTranslated(`purpose_req_${reqNo}`) || item.purpose.join(", "),
        ],
        usedBy: [
          getTranslated(`usedBy_req_${reqNo}`) || item.usedBy.join(", "),
        ],
        duration: {
          value:
            getTranslated(`duration_req_${reqNo}`)?.split(" ")[0] ||
            item.duration.value,
          unit:
            getTranslated(`duration_req_${reqNo}`)?.split(" ")[1] ||
            item.duration.unit,
        },
        processorActivities: translatedProcessorActivities,
        id: item.id,
        status: item.status,
      };
    });

    const translatedTemplateList2 = formOtherData.map((item, index) => {
      const reqNo = index + 1;

      const getTranslated = (id) =>
        data2.output.find((o) => o.id === id)?.target?.trim() || null;

      const translatedProcessorActivities = item.processorActivities.map(
        (activity, actIdx) => {
          const translatedActivityName =
            getTranslated(`activityName_otr_${reqNo}_${actIdx}`) ||
            activity.activityName;

          const translatedProcessorName =
            getTranslated(`processorName_otr_${reqNo}_${actIdx}`) ||
            activity.processorName;

          // Translate data types
          const translatedDataTypes = activity.dataTypes.map((dt, dtIdx) => {
            const dtNameId = `dataTypeName_otr_${reqNo}_${actIdx}_${dtIdx}`;
            const translatedDataTypeName =
              getTranslated(dtNameId) || dt.dataTypeName;

            // FIX: translate entire dataItems list at once
            const dataItemsId = `dataItems_otr_${reqNo}_${actIdx}_${dtIdx}`;
            const translatedItemsString = getTranslated(dataItemsId);

            const translatedDataItems = translatedItemsString
              ? translatedItemsString.split(",").map((i) => i.trim())
              : dt.dataItems;

            return {
              dataTypeName: translatedDataTypeName,
              dataItems: translatedDataItems,
            };
          });

          return {
            activityName: translatedActivityName,
            processorName: translatedProcessorName,
            dataTypes: translatedDataTypes,
          };
        },
      );

      return {
        purpose: [
          getTranslated(`purpose_otr_${reqNo}`) || item.purpose.join(", "),
        ],
        usedBy: [
          getTranslated(`usedBy_otr_${reqNo}`) || item.usedBy.join(", "),
        ],
        duration: {
          value:
            getTranslated(`duration_otr_${reqNo}`)?.split(" ")[0] ||
            item.duration.value,
          unit:
            getTranslated(`duration_otr_${reqNo}`)?.split(" ")[1] ||
            item.duration.unit,
        },
        processorActivities: translatedProcessorActivities,
        id: item.id,
        status: item.status,
      };
    });

    setFormData(translatedTemplateList);
    setformOtherData(translatedTemplateList2);
    if (modalRef.current === "view") {
      setShowModal(true);
    }
    if (modalRef.current === "edit") {
      setShowUpdateModal(true);
    }
    if (modalRef.current === "renew") {
      setShowRenewModal(true);
    }
  };

  const handleLanguageChange = (e) => {
    const lang = e.target.value;
    setSelectedLang(lang);
    callBhashiniAPI(lang);
  };

  const closeEditConsentModal = () => {
    currentLangRef.current = "en";
    setShowUpdateModal(false);
    setLightTheme({});
    setIsDarkMode(false);
    setSelectedLang("ENGLISH");
    setCustomerIdLabel("Customer ID");
    setActiveBadge("Always Active");
    setOtherAccordionHeader("Others");
    setPurposeLabel("");
    setPurposeLabel("Purpose");
    setDurationLabel("Duration");
    setUsedByLabel("Used by");
    setDataItemLabel("Data item");
    setDataTypeLabel("Data type");
    setProcessingActivityLabel("Processing activity");
    setWithdrawConsentBtnText("Withdraw Consent");
    setUpdateConsentBtnText("Update Consent");
    setModalHeading("Manage Consent");

    setAgeSentence("I am below 18 years of age");
    setDocumentName("");
  };
  const closeRenewConsentModal = () => {
    currentLangRef.current = "en";
    setShowRenewModal(false);
    setLightTheme({});
    setIsDarkMode(false);
    setSelectedLang("ENGLISH");
    setCustomerIdLabel("Customer ID");
    setActiveBadge("Always Active");
    setOtherAccordionHeader("Others");
    setPurposeLabel("");
    setPurposeLabel("Purpose");
    setDurationLabel("Duration");
    setUsedByLabel("Used by");
    setDataItemLabel("Data item");
    setDataTypeLabel("Data type");
    setProcessingActivityLabel("Processing activity");
    setWithdrawConsentBtnText("Withdraw Consent");
    setUpdateConsentBtnText("Update Consent");
    setModalHeading("Manage Consent");
    setRenewConsentBtnText("Renew");
    setAgeSentence("I am below 18 years of age");
    setDocumentName("");
  };

  const downloadConsentExcel = (consentsByTempId) => {
    try {
      const wb = XLSX.utils.book_new();
      const ws_data = [];
      const rows = Array.isArray(consentsByTempId) ? consentsByTempId : [];

      rows.forEach((consent) => {
        ws_data.push([
          "Consent ID",
          "Created Date",
          "Expiry Date",
          "Status",
          "Action",
        ]);

        ws_data.push([
          consent.consentId || "",
          formatToIST(consent.startDate),

          formatToIST(consent.endDate),

          consent.status || "",
          "",
        ]);

        ws_data.push([]);

        ws_data.push([
          "Purpose Name",
          "Data Type",
          "Data Item",
          "Data Used By",
          "Preference Status",
        ]);

        consent.preferences?.forEach((pref) => {
          pref.purposeList?.forEach((purpose) => {
            const dataTypes =
              pref.processorActivityList
                ?.flatMap((pa) =>
                  pa.processActivityInfo.dataTypesList.map(
                    (dt) => dt.dataTypeName,
                  ),
                )
                ?.join(", ") || "";

            const dataItems =
              pref.processorActivityList
                ?.flatMap((pa) =>
                  pa.processActivityInfo.dataTypesList.flatMap(
                    (dt) => dt.dataItems,
                  ),
                )
                ?.join(", ") || "";

            const processors =
              pref.processorActivityList
                ?.map((pa) => pa.processActivityInfo.processorName)
                ?.join(", ") || "";

            ws_data.push([
              purpose.purposeInfo?.purposeName || "",
              dataTypes,
              dataItems,
              processors,
              pref.preferenceStatus || "",
            ]);
          });
        });

        ws_data.push([]);
        ws_data.push([]);
      });

      const ws = XLSX.utils.aoa_to_sheet(ws_data);

      ws["!cols"] = [
        { wch: 30 },
        { wch: 20 },
        { wch: 20 },
        { wch: 25 },
        { wch: 20 },
      ];

      const borderStyle = {
        top: { style: "thin", color: { rgb: "CCCCCC" } },
        bottom: { style: "thin", color: { rgb: "CCCCCC" } },
        left: { style: "thin", color: { rgb: "CCCCCC" } },
        right: { style: "thin", color: { rgb: "CCCCCC" } },
      };

      const range = XLSX.utils.decode_range(ws["!ref"]);

      for (let R = range.s.r; R <= range.e.r; ++R) {
        const row = ws_data[R];
        if (!row) continue;

        for (let C = 0; C < row.length; ++C) {
          const cellRef = XLSX.utils.encode_cell({ r: R, c: C });
          const cell = ws[cellRef];
          if (!cell) continue;

          cell.s = cell.s || {};
          cell.s.border = borderStyle;
          cell.s.alignment = {
            vertical: "center",
            wrapText: true,
          };
        }

        if (row[0] === "Consent ID") {
          for (let C = 0; C < row.length; ++C) {
            const cellRef = XLSX.utils.encode_cell({ r: R, c: C });
            const cell = ws[cellRef];
            if (!cell) continue;

            cell.s = {
              font: { bold: true, color: { rgb: "FFFFFF" } },
              fill: { fgColor: { rgb: "4472C4" } },
              alignment: { horizontal: "center", vertical: "center" },
              border: borderStyle,
            };
          }
        }

        if (row[0] === "Purpose Name") {
          for (let C = 0; C < row.length; ++C) {
            const cellRef = XLSX.utils.encode_cell({ r: R, c: C });
            const cell = ws[cellRef];
            if (!cell) continue;

            cell.s = {
              font: { bold: true, color: { rgb: "FFFFFF" } },
              fill: { fgColor: { rgb: "808080" } },
              alignment: { horizontal: "center", vertical: "center" },
              border: borderStyle,
            };
          }
        }
      }

      XLSX.utils.book_append_sheet(wb, ws, "Consent Logs");

      const wbout = XLSX.write(wb, { bookType: "xlsx", type: "array" });
      const blob = new Blob([wbout], { type: "application/octet-stream" });

      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "Consent_Report.xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.log("Excel download failed:", err);
    }
  };

  // const templateId = "7387531101";
  const params = new URLSearchParams(window.location.search);
  const encodedData = params.get("customerDetails");

  if (encodedData) {
    try {
      const decoded = atob(encodedData);
      const parsed = JSON.parse(decoded);

      cusId = parsed.cusId || customerId;
      tenantId = parsed.tenantId || tntId;
      businessId = parsed.businessId || businssId;
      scCode = parsed.secureCode || secureCode;
      grievanceTemplateId = parsed.grievanceTemplateId || grievanceId;
      recievedCallBackUrl = parsed.callBackUrl || parentalRedirectUrl;

      dispatch(
        saveCustomerIdAndenantId(
          cusId,
          tenantId,
          businessId,
          grievanceTemplateId,
          scCode,
          recievedCallBackUrl,
        ),
      );
    } catch (err) {
      console.log("Error decoding Base64 data param:", err);
    }
  } else {
    cusId = customerId;
    tenantId = tntId;
    businessId = businessId;
    grievanceTemplateId = grievanceId;
    scCode = secureCode;
  }

  const calculateValidityInDays = (endDateString) => {
    // Convert "23 Nov 2025, 18:25:39" into valid Date
    endDateString = formatToIST(endDateString);
    const endDate = new Date(endDateString);
    const now = new Date();
    const diffMs = endDate - now;
    const remainingDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

    return remainingDays;
  };

  const transformConsentData = (apiResponse) => {
    const data = Array.isArray(apiResponse) ? apiResponse[0] : apiResponse;
    if (!data || !data.preferences)
      return {
        mandatory: [],
        others: [],
      };

    const mandatory = [];
    const others = [];
    const preferencesStatus = {};

    data.preferences.forEach((pref) => {
      const purpose = pref.purposeList
        .map((p) => p?.purposeInfo?.purposeName)
        .filter(Boolean);

      if (purpose.length === 0) {
        throw new Error("Invalid purpose data");
      }
      const rawUnit = pref?.preferenceValidity?.unit || "";
      console.log("Raw duration unit:", String(rawUnit));
      if (rawUnit === "") {
        throw new Error("Missing duration unit");
      }

      const processedUnit =
        pref?.preferenceValidity?.value === 1
          ? rawUnit.toLowerCase().replace(/s$/, "")
          : rawUnit.toLowerCase();

      // Capitalize first letter
      const capitalizedUnit =
        processedUnit.charAt(0).toUpperCase() + processedUnit.slice(1);

      const duration = {
        value: pref?.preferenceValidity?.value ?? "",
        unit: capitalizedUnit,
      };

      const usedBy = pref.processorActivityList
        .map((p) => p?.processActivityInfo?.processorName)
        .filter(Boolean);

      if (usedBy.length === 0) {
        throw new Error("Invalid processor data");
      }

      const dataItems = pref.processorActivityList
        .flatMap((p) =>
          p?.processActivityInfo?.dataTypesList.flatMap((d) => d.dataItems),
        )
        .filter(Boolean);

      if (dataItems.length === 0) {
        throw new Error("Invalid data items");
      }

      const processorActivities = (pref.processorActivityList || [])
        .map((p) => {
          const activityName = p?.processActivityInfo?.activityName;
          const processorName = p?.processActivityInfo?.processorName;

          const dataTypes = (p?.processActivityInfo?.dataTypesList || [])
            .map((d) => ({
              dataTypeName: d?.dataTypeName,
              dataItems: d?.dataItems || [],
            }))
            .filter((d) => d.dataTypeName);

          if (!activityName || !processorName || dataTypes.length === 0) {
            return null;
          }

          return {
            activityName,
            processorName,
            dataTypes,
          };
        })
        .filter(Boolean);

      console.log("Processor Acti", processorActivities);
      if (processorActivities.length === 0) {
        throw new Error("Invalid processor activities");
      }
      const autoRenewStatus = pref.autoRenew;

      const id = pref.preferenceId;
      const status = pref.preferenceStatus;
      const preferenceExpiry = calculateValidityInDays(pref.endDate);

      const formattedObj = {
        purpose,
        duration,
        usedBy,
        dataItems,
        processorActivities,
        id,
        status,
        preferenceExpiry,
        autoRenewStatus,
      };

      preferencesStatus[id] = status;

      console.log("Formatted preference object:", formattedObj);
      if (pref.mandatory === true) {
        mandatory.push(formattedObj);
      } else {
        others.push(formattedObj);
      }
    });

    return { mandatory, others, preferencesStatus };
  };

  const [formData, setFormData] = useState({});
  const [formOtherData, setformOtherData] = useState({});
  const isAllChecked =
    formOtherData.length > 0 &&
    formOtherData.every((item) => item.status == "ACCEPTED");

  // const othersWithChecked = formOtherData.map((item) => ({
  //   ...item,
  //   checked: item.status === "ACCEPTED",
  // }));

  // const handleMainCheckboxChange = (e) => {
  //   const isChecked = e.target.checked;
  //   const updated = formOtherData.map((item) => ({
  //     ...item,
  //     checked: isChecked,
  //   }));
  //   setformOtherData(updated);
  // };

  const handleCallAPI = async (
    id,
    handleId,
    languagePreferences,
    status,
    startDate,
  ) => {
    console.log("Inside View");
    modalRef.current = "view";
    setSelectedConsentId(id);
    currentPageLoadRef.current = 1;
    const consentObj = consentLanguageList.find(
      (item) => item.consentHandleId === handleId,
    );

    setSelectedLanguagePreferences(languagePreferences);
    setSelectedConsentStatus(status);
    const response = await dispatch(searchConsentsByConsentId(id));
    //alert("response?.searchList?.parentalKyc" + response[0]?.parentalKyc);
    if (response[0]?.parentalKyc) {
      //alert("response[0]?.parentalKyc " + response[0]?.parentalKyc);
      setIsParentChecked(true);
      setParentCheckBox(true);
    } else {
      setIsParentChecked(false);
      setParentCheckBox(false);
    }

    console.log("API response for consent details:", response);
    try {
      const { mandatory, others, preferencesStatus } =
        transformConsentData(response);
      console.log("Mandatory:", mandatory);

      const response1 = await dispatch(getConsentHandleByHandleID(handleId));

      setFormData(mandatory);
      setformOtherData(others);
      setSelectedPreferences(preferencesStatus);
      var theme = response1?.uiConfig?.theme;
      setParentalControl(response1?.uiConfig?.parentalControl);
      setDarkModeToBeShown(response1?.uiConfig?.darkMode);
      setDataItemToBeShown(response1?.uiConfig?.dataItemToBeShown);
      setDocumentName(response1?.documentMeta?.name);
      setDocumentId(response1?.documentMeta?.documentId);
      setLogoUrl(response1?.uiConfig?.logo);
      setProcessActivityNameToBeShown(
        response1?.uiConfig?.processActivityNameToBeShown,
      );
      setProcessorNameToBeShown(response1?.uiConfig?.processorNameToBeShown);

      setValiditytoBeShown(response1?.uiConfig?.validitytoBeShown);

      if (theme) {
        var decodedThemeStr = atob(theme);

        var decodedTheme = JSON.parse(decodedThemeStr);

        setLightTheme(JSON.parse(decodedTheme.light));
        setDarkTheme(JSON.parse(decodedTheme.dark));
      }

      setTopDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.description,
      );
      setRightsDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.rightsText,
      );
      setPermissionDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.permissionText,
      );
      setRequiredText(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH?.label,
      );

      //setShowModal(true);
      setPendingTranslationLang(consentObj?.languagePreferences || "ENGLISH");
      console.log(
        "Current selected langiage is",
        consentObj?.languagePreferences || "ENGLISH",
      );
      setSelectedLang(consentObj?.languagePreferences || "ENGLISH");
    } catch (err) {
      console.log(err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Something went wrong while fetching consent details."}
          />
        ),
        { icon: false },
      );

      return;
    }

    const handleRowCheckboxChange = (index, e) => {
      const updated = [...formOtherData];
      updated[index].checked = e.target.checked;
      setformOtherData(updated);
    };
  };

  useEffect(() => {
    if (
      pendingTranslationLang &&
      Array.isArray(formData) &&
      Array.isArray(formOtherData) &&
      formData.length >= 0
    ) {
      callBhashiniAPI(pendingTranslationLang);
      setPendingTranslationLang(null); // reset
    }
  }, [pendingTranslationLang, formData, formOtherData]);

  const handleCallAPI2 = async (
    id,
    handleId,
    languagePreferences,
    status,
    startDate,
  ) => {
    modalRef.current = "edit";
    setSelectedConsentStartDate(formatToIST(startDate));
    setSelectedConsentId(id);
    currentPageLoadRef.current = 1;
    const consentObj = consentLanguageList.find(
      (item) => item.consentHandleId === handleId,
    );
    setSelectedConsentHandleId(handleId);
    setSelectedLanguagePreferences(languagePreferences);

    setSelectedConsentStatus(status);
    const response = await dispatch(searchConsentsByConsentId(id));
    try {
      const { mandatory, others, preferencesStatus } =
        transformConsentData(response);

      if (response[0]?.parentalKyc) {
        //alert("response[0]?.parentalKyc " + response[0]?.parentalKyc);
        setIsParentChecked(true);
        setParentCheckBox(true);
      } else {
        setIsParentChecked(false);
        setParentCheckBox(false);
      }

      const response1 = await dispatch(getConsentHandleByHandleID(handleId));
      setDarkModeToBeShown(response1?.uiConfig?.darkMode);
      var theme = response1?.uiConfig?.theme;

      setFormData(mandatory);
      setformOtherData(others);
      setSelectedPreferences(preferencesStatus);
      setSelectedPrefrenecesForRenewal(preferencesStatus);

      setDocumentName(response1?.documentMeta?.name);
      setDocumentId(response1?.documentMeta?.documentId);
      setLogoUrl(response1?.uiConfig?.logo);
      setParentalControl(response1?.uiConfig?.parentalControl);
      setDataItemToBeShown(response1?.uiConfig?.dataItemToBeShown);
      setProcessActivityNameToBeShown(
        response1?.uiConfig?.processActivityNameToBeShown,
      );
      setValiditytoBeShown(response1?.uiConfig?.validitytoBeShown);
      if (theme) {
        var decodedThemeStr = atob(theme);

        var decodedTheme = JSON.parse(decodedThemeStr);

        setLightTheme(JSON.parse(decodedTheme.light));
        setDarkTheme(JSON.parse(decodedTheme.dark));
      }

      setTopDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.description,
      );
      setRightsDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.rightsText,
      );
      setPermissionDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.permissionText,
      );
      setRequiredText(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH?.label,
      );
      //setShowUpdateModal(true);
      setPendingTranslationLang(consentObj?.languagePreferences || "ENGLISH");
      setSelectedLang(consentObj?.languagePreferences || "ENGLISH");
    } catch (err) {
      console.log(err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Something went wrong while fetching consent details."}
          />
        ),
        { icon: false },
      );

      return;
    }
  };

  const handleCallAPI3 = async (id, handleId, languagePreferences, status) => {
    setSelectedConsentId(id);
    modalRef.current = "renew";
    currentPageLoadRef.current = 1;
    const consentObj = consentLanguageList.find(
      (item) => item.consentHandleId === handleId,
    );
    setSelectedConsentHandleId(handleId);
    setSelectedLanguagePreferences(languagePreferences);

    setSelectedConsentStatus(status);
    const response = await dispatch(searchConsentsByConsentId(id));
    try {
      const { mandatory, others, preferencesStatus } =
        transformConsentData(response);

      if (response[0]?.parentalKyc) {
        //alert("response[0]?.parentalKyc " + response[0]?.parentalKyc);
        setIsParentChecked(true);
        setParentCheckBox(true);
      } else {
        setIsParentChecked(false);
        setParentCheckBox(false);
      }

      const response1 = await dispatch(getConsentHandleByHandleID(handleId));
      setDarkModeToBeShown(response1?.uiConfig?.darkMode);
      var theme = response1?.uiConfig?.theme;

      setFormData(mandatory);
      setformOtherData(others);
      setSelectedPreferences(preferencesStatus);
      setSelectedPrefrenecesForRenewal(preferencesStatus);

      setDocumentName(response1?.documentMeta?.name);
      setDocumentId(response1?.documentMeta?.documentId);
      setLogoUrl(response1?.uiConfig?.logo);
      setParentalControl(response1?.uiConfig?.parentalControl);
      setDataItemToBeShown(response1?.uiConfig?.dataItemToBeShown);
      setProcessActivityNameToBeShown(
        response1?.uiConfig?.processActivityNameToBeShown,
      );
      setValiditytoBeShown(response1?.uiConfig?.validitytoBeShown);
      if (theme) {
        var decodedThemeStr = atob(theme);

        var decodedTheme = JSON.parse(decodedThemeStr);

        setLightTheme(JSON.parse(decodedTheme.light));
        setDarkTheme(JSON.parse(decodedTheme.dark));
      }

      setTopDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.description,
      );
      setRightsDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.rightsText,
      );
      setPermissionDescription(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.permissionText,
      );
      setRequiredText(
        response1?.multilingual?.languageSpecificContentMap?.ENGLISH?.label,
      );
      //setShowRenewModal(true);
      setPendingTranslationLang(consentObj?.languagePreferences || "ENGLISH");
      setSelectedLang(consentObj?.languagePreferences || "ENGLISH");
    } catch (err) {
      console.log(err);
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Something went wrong while fetching consent details."}
          />
        ),
        { icon: false },
      );

      return;
    }
  };

  const [renewConsentList, setRenewConsentList] = useState([]);
  const handleRenewToggle = (id, isChecked) => {
    setRenewConsentList((prev) => {
      let updated;

      if (isChecked) {
        // ADD
        updated = [...prev, { [id]: "RENEW" }];
      } else {
        // REMOVE
        updated = prev.filter((item) => !item[id]);
      }

      return updated;
    });
  };

  const handlePreferenceChange = (id, isChecked) => {
    setSelectedPreferences((prev) => ({
      ...prev,
      [id]: isChecked ? "ACCEPTED" : "NOTACCEPTED",
    }));
  };

  const handleRenewPrefereneceChange = (id, isChecked) => {
    setListOfRenewPref((prev) => ({
      ...prev,
      [id]: isChecked ? "RENEW" : "",
    }));
  };

  const handleSelectAll = (isChecked) => {
    setAllSelected(isChecked);

    setSelectedPreferences((prev) => {
      const mandatoryEntries = Object.fromEntries(
        Object.entries(prev).filter(([_, value]) => value === "ACCEPTED"),
      );

      const updatedOthers = {};
      formOtherData.forEach((item) => {
        updatedOthers[item.id] = isChecked ? "ACCEPTED" : "NOTACCEPTED";
      });

      return { ...mandatoryEntries, ...updatedOthers };
    });
  };

  useEffect(() => {
    const allChecked =
      formOtherData.length > 0 &&
      formOtherData.every(
        (item) => selectedPrefereneces[item.id] === "ACCEPTED",
      );
    setAllSelected(allChecked);
  }, [formOtherData, selectedPrefereneces]);

  useEffect(() => {}, [selectedPrefereneces]);

  const getConsentData = async () => {
    try {
      setShowLoader(true);

      const jsonResponse = await getConsentsByTemplateId(
        cusId,
        tenantId,
        scCode,
      );

      if (!jsonResponse) return;

      setShowLoader(false);
      const grouped = {};
      jsonResponse.forEach((consent) => {
        const consentId = consent.consentId;
        const created = new Date(consent.createdAt).toLocaleDateString("en-GB");

        consent.preferences?.forEach((pref) => {
          const consentStatus = pref.preferenceStatus;
          pref.processorActivityList?.forEach((activity) => {
            const usedBy = `${activity.processActivityInfo.processorName} (Created: ${created})`;

            pref.purposeList?.forEach((purpose) => {
              const purposeName = purpose.purposeInfo.purposeName;
              const purposeStatus = purpose.purposeInfo.status;
              const key = `${consentId}||${usedBy}||${purposeName}`;

              if (!grouped[key]) {
                grouped[key] = {
                  consentId,
                  usedBy,
                  purpose: purposeName,
                  dataItems: [],
                  consentStatus,
                  purposeStatus,
                };
              }

              activity.processActivityInfo.dataTypesList?.forEach((dt) => {
                grouped[key].dataItems.push(...(dt.dataItems || []));
              });
            });
          });
        });
      });

      const formatted = Object.values(grouped).map((r) => ({
        ...r,
        dataItems: [...new Set(r.dataItems)].join(", "),
      }));

      formatted.sort((a, b) => {
        if (a.consentId < b.consentId) return -1;
        if (a.consentId > b.consentId) return 1;
        if (a.usedBy < b.usedBy) return -1;
        if (a.usedBy > b.usedBy) return 1;
        if (a.purpose < b.purpose) return -1;
        if (a.purpose > b.purpose) return 1;
        return 0;
      });

      setTableData(formatted);
    } catch (err) {
      console.log("Error fetching consent data:", err);

      const firstError = Array.isArray(err) ? err[0] : err?.errors?.[0];

      if (!firstError) return;

      console.log("Error code:", firstError.errorCode);

      if (firstError.errorCode === "JCMP2001") {
        setShowLoader(false);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Please enter valid tenant ID."}
            />
          ),
          { icon: false },
        );
      }

      if (firstError.errorCode === "JCMP3001") {
        setShowLoader(false);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"No data available for provided Email Id/Phone Number."}
            />
          ),
          { icon: false },
        );
      }
    }
  };

  const getRowSpanInfo = (data, accessor) => {
    const info = [];
    let prev = null;
    let prevIndex = -1;
    let count = 0;

    data.forEach((row, idx) => {
      const val = accessor(row);
      if (val === prev) {
        count++;
        info[prevIndex].rowSpan = count + 1;

        info.push({ show: false });
      } else {
        count = 0;
        info.push({ show: true, rowSpan: 1 });
        prev = val;
        prevIndex = idx;
      }
    });

    return info;
  };
  const consentRowSpanInfo = getRowSpanInfo(tableData, (r) => r.consentId);
  const usedByRowSpanInfo = getRowSpanInfo(tableData, (r) => r.usedBy);

  // useEffect(() => {
  //   if (templateId) {
  //     getConsentData();
  //   }
  // }, [templateId]); // run whenever templateId changes

  const handleToggle = (rowId) => {
    setExpandedRow(expandedRow === rowId ? null : rowId);
  };

  useEffect(() => {
    const fetchTranslationConfig = async () => {
      try {
        const res = await dispatch(translateConfiguration());
      } catch (error) {
        console.log(
          "Error in translation configuration:",
          error.message || error,
        );
      }
    };

    fetchTranslationConfig();
  }, [dispatch]);

  useEffect(() => {
    const fetchBusiness = async () => {
      try {
        if (businessId) {
          const res = await dispatch(getBusinessDetails(businessId));
          const res2 = await dispatch(getLogoByBusinessId(businessId));
        }
      } catch (error) {
        console.log(
          "Error in translation configuration:",
          error.message || error,
        );
      }
    };

    fetchBusiness();
  }, [dispatch]);

  useEffect(() => {
    const fetchDigilockerDetails = async () => {
      try {
        if (tenantId) {
          const res = await dispatch(getDigilockerDetails(tenantId));

          const response = res?.data;
          if (response?.searchList) {
            setDigilockerClientId(response.searchList[0]?.clientId || "");
            setDigilockerClientSecret(
              response.searchList[0]?.clientSecret || "",
            );
            setDigilockerCodeVerifier(
              response.searchList[0]?.codeVerifier || "",
            );
            setDigilockerRedirectUri(response.searchList[0]?.redirectUri || "");
          }
        }
      } catch (error) {
        console.log("Error in Digilocker api:", error.message || error);
      }
    };

    fetchDigilockerDetails();
  }, [dispatch, tenantId]);

  useEffect(() => {
    setLoadLogs(true);
    getConsentsByTemplateId(cusId, tenantId, scCode)
      .then((searchList) => {
        const list = Array.isArray(searchList) ? searchList : [];
        setConsentsByTempId(list);
        console.log("SearchList recieved", JSON.stringify(list));
        const consentLanguageList2 = list.map((item) => ({
          consentHandleId: item.consentHandleId,
          languagePreferences: item.languagePreferences,
        }));
        setConsentLanguageList(consentLanguageList2);
        console.log(
          "Consent Language List",
          JSON.stringify(consentLanguageList2),
        );
        generateAutoRenewStatus(list);

        const withdraw = list.filter(
          (item) => item.status === "WITHDRAWN",
        ).length;
        const active = list.filter((item) => item.status === "ACTIVE").length;
        const expire = list.filter((item) => item.status === "EXPIRED").length;

        setWithdrawCount(withdraw);
        setActiveCount(active);
        setExpiredCount(expire);

        setLoadLogs(false);
      })
      .catch((err) => {
        console.log("Error occured in getConsentsByTemplateId", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setConsentsByTempId([]);
        }
        if (err.code === "ERR_NETWORK") {
          setSessionExpired(true);
        }
        setLoadLogs(false);
      });
  }, [cusId, tenantId]);
  const applyFilters = (consents) => {
    return consents.filter((consent) => {
      if (filters.search?.trim()) {
        const text = filters.search.toLowerCase().trim().replace(/\s+/g, " ");

        const consentValues = [
          consent.consentId,
          formatToIST(consent.startDate),
          formatToIST(consent.endDate),
          consent.status,
        ];

        consent.preferences?.forEach((pref) => {
          pref.purposeList?.forEach((p) =>
            consentValues.push(p.purposeInfo?.purposeName),
          );

          pref.processorActivityList?.forEach((pa) => {
            consentValues.push(pa.processActivityInfo?.processorName);

            pa.processActivityInfo?.dataTypesList?.forEach((dt) => {
              consentValues.push(dt.dataTypeName);
              consentValues.push(...(dt.dataItems || []));
            });
          });
        });

        const match = consentValues.some((value) => {
          if (typeof value !== "string") return false;

          const clean = value.toLowerCase().trim().replace(/\s+/g, " ");
          return clean.includes(text);
        });

        if (!match) return false;
      }

      // ----- 2. STATUS FILTER -----
      if (filters.status?.length > 0) {
        if (!filters.status.includes(consent.status)) return false;
      }

      // ----- 3. CREATED DATE FILTER (startDate → start of day, endDate → end of day) -----

      const start = filters.startDate
        ? new Date(filters.startDate + "T00:00:00")
        : null;

      const end = filters.endDate
        ? new Date(filters.endDate + "T23:59:59")
        : null;

      const created = new Date(consent.startDate);

      if (start && created < start) return false;
      if (end && created > end) return false;

      return true;
    });
  };

  return (
    <main role="main" aria-label="Granted Consents Page">
      <div className="pageConfig">
        <header className="fiduciaryHeaderContainer" role="banner">
          <div className="fiduciaryHeader">
            <h1>
              <span style={textStyle("heading-s", "primary-grey-100")}>
                {getTranslation("fid_granted_consents", "Granted consents")}
              </span>
            </h1>
            <div
              className="badge"
              role="status"
              aria-label="Page type: Consent"
            >
              <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                {getTranslation("fid_consent_badge", "Consent")}
              </span>
            </div>
          </div>
        </header>
        <span style={textStyle("body-s", "primary-grey-80")}>
          {getTranslation(
            "fid_description",
            "View and respond to your consent requests awaiting your action.",
          )}
        </span>
        <br></br>
        <div className="status-container">
          <div
            className="status-card active-card"
            aria-label={`${getTranslation(
              "fid_active",
              "Active",
            )}: ${activeCount} consents`}
          >
            <div className="status-content">
              <FaCheckCircle style={{ color: "green" }} size={ICON_SIZE} />
              <div className="status-text">
                <span style={textStyle("body-xs-bold", "primary-grey-100")}>
                  {String(activeCount)}
                </span>
                <span style={textStyle("body-xs", "primary-grey-80")}>
                  {getTranslation("fid_active", "Active")}
                </span>
              </div>
            </div>
          </div>

          <div
            className="status-card withdrawn-card"
            aria-label={`${getTranslation(
              "fid_withdrawn",
              "Withdrawn",
            )}: ${withdrawCount} consents`}
          >
            <div className="status-content">
              <FaTimesCircle style={{ color: "red" }} size={ICON_SIZE} />
              <div className="status-text">
                <span style={textStyle("body-xs-bold", "primary-grey-100")}>
                  {String(withdrawCount)}
                </span>
                <span style={textStyle("body-xs", "primary-grey-80")}>
                  {getTranslation("fid_withdrawn", "Withdrawn")}
                </span>
              </div>
            </div>
          </div>

          <div
            className="status-card withdrawn-card"
            style={{ backgroundColor: "#fef0e7" }}
            aria-label={`${getTranslation(
              "fid_expired",
              "Expired",
            )}: ${expiredCount} consents`}
          >
            <div className="status-content">
              <FaExclamationCircle
                style={{ color: "rgb(240, 109, 15)" }}
                size={ICON_SIZE}
              />
              <div className="status-text">
                <span style={textStyle("body-xs-bold", "primary-grey-100")}>
                  {String(expiredCount)}
                </span>
                <span style={textStyle("body-xs", "primary-grey-80")}>
                  {getTranslation("fid_expired", "Expired")}
                </span>
              </div>
            </div>
          </div>
        </div>
        <div
          className="fiduciaryHeaderButtonContainer"
          style={{
            justifyContent: "flex-end",
            opacity: (consentsByTempId?.length ?? 0) === 0 ? 0.5 : 1,
            pointerEvents: (consentsByTempId?.length ?? 0) === 0 ? "none" : "auto",
          }}
        >
          <div className="search-input-wrapper" style={{ width: "100%" }}>
            <FiSearch size={ICON_SIZE} />
            <input
              type="text"
              className="search-input"
              placeholder={getTranslation(
                "fid_search_placeholder",
                "Search data item, type and purpose",
              )}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              aria-label={getTranslation(
                "fid_search_placeholder",
                "Search data item, type and purpose",
              )}
            />
          </div>
          <div
            role="button"
            aria-label="Open filter panel"
            tabIndex={0}
            onClick={() => setShowFilter(true)}
            style={{
              cursor: "pointer",
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              padding: "6px",
              borderRadius: "6px",
            }}
          >
            <FiFilter size={ICON_SIZE} color="#555" />
          </div>
          <FilterPanel
            open={showFilter}
            showStatus={true}
            onClose={() => setShowFilter(false)}
            onApply={(data) => {
              setFilters(data); // <-- IMPORTANT
            }}
            onClear={() =>
              setFilters({
                search: "",
                status: [],
                startDate: "",
                endDate: "",
              })
            }
          />

          <div
            role="button"
            tabIndex={0}
            aria-label="Download Report"
            onClick={() => downloadConsentExcel(consentsByTempId)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                downloadConsentExcel(consentsByTempId);
              }
            }}
            onMouseEnter={(e) =>
              (e.currentTarget.style.backgroundColor = "#f5f5f5")
            }
            onMouseLeave={(e) =>
              (e.currentTarget.style.backgroundColor = "transparent")
            }
            style={{
              cursor: "pointer",
              display: "inline-flex",
              alignItems: "center",
              justifyContent: "center",
              padding: "6px",
              borderRadius: "6px",
              transition: "0.2s",
            }}
          >
            <FiDownload size={ICON_SIZE} color="#555" />
          </div>
        </div>
        <br></br>

        <div
          style={{ overflowX: "auto", paddingBottom: "1rem" }}
          aria-label="Consent Information Table."
        >
          <table
            className="consentLog-table"
            aria-label="details such as Consent ID, Created Date, Expiry Date, Status and Actions"
          >
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
                <th scope="col"></th>
                <th className="parentheader" scope="col">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("fid_consent_id", "Consent ID")}
                    </span>
                  </div>
                </th>
                <th className="parentheader" scope="col">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("fid_created_date", "Created date")}
                    </span>
                  </div>
                </th>
                <th className="parentheader" scope="col">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("fid_expiry_date", "Expiry date")}
                    </span>
                  </div>
                </th>
                <th className="parentheader" scope="col">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("fid_status", "Status")}
                    </span>
                  </div>
                </th>
                <th className="parentheader" scope="col">
                  <div className="header-with-icon">
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("fid_actions", "Actions")}
                    </span>
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
                      <>
                        <style>
                          {`
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
    `}
                        </style>

                        <div
                          style={{
                            width: "20px",
                            height: "20px",
                            border: "3px solid #f3f3f3",
                            borderTop: "3px solid #0a2885",
                            borderRadius: "50%",
                            animation: "spin 1s linear infinite",
                          }}
                        />
                      </>
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
                    <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                      {getTranslation("fid_no_data", "No Data to Display")}
                    </span>
                  </td>
                </tr>
              ) : (
                (() => {
                  const dataAfterFilters = applyFilters(consentsByTempId || []);
                  const sortedConsents = [...dataAfterFilters].sort((a, b) => {
                    return new Date(b.startDate) - new Date(a.startDate); // latest first
                  });

                  const filteredConsents = sortedConsents.filter((consent) => {
                    const text = searchText
                      .toLowerCase()
                      .trim()
                      .replace(/\s+/g, " ");

                    if (!text) return true;

                    const consentValues = [
                      consent.consentId,
                      formatToIST(consent.startDate),
                      formatToIST(consent.endDate),
                      consent.status,
                    ];

                    consent.preferences?.forEach((pref) => {
                      pref.purposeList?.forEach((p) => {
                        consentValues.push(p.purposeInfo?.purposeName);
                      });

                      pref.processorActivityList?.forEach((pa) => {
                        consentValues.push(
                          pa.processActivityInfo?.processorName,
                        );
                        pa.processActivityInfo?.dataTypesList?.forEach((dt) => {
                          consentValues.push(dt.dataTypeName);
                          consentValues.push(...(dt.dataItems || []));
                        });
                      });
                    });

                    return consentValues.some((val) => {
                      if (typeof val !== "string") return false;
                      const normalizedVal = val
                        .toLowerCase()
                        .trim()
                        .replace(/\s+/g, " ");
                      return normalizedVal.includes(text);
                    });
                  });
                  totalPages = Math.ceil(filteredConsents.length / rowsPerPage);

                  startIdx = (currentPage - 1) * rowsPerPage;
                  endIdx = startIdx + rowsPerPage;

                  paginatedConsents = filteredConsents.slice(startIdx, endIdx);

                  return filteredConsents.length === 0 ? (
                    <tr>
                      <td
                        colSpan="6"
                        style={{ textAlign: "center", padding: "1rem" }}
                      >
                        <span style={textStyle("body-xs-bold", "primary-grey-80")}>
                          No matching records found
                        </span>
                      </td>
                    </tr>
                  ) : (
                    paginatedConsents.map((consent, index) => (
                      <React.Fragment key={consent.consentId}>
                        {/* Parent row */}
                        <tr
                          onClick={() => handleToggle(`row${index}`)}
                          className={
                            expandedRow === `row${index}`
                              ? "expanded-row-parent"
                              : ""
                          }
                          role="button"
                          aria-expanded={
                            expandedRow === `row${index}` ? "true" : "false"
                          }
                          aria-label={`Consent ${consent.consentId} details`}
                          tabIndex="0"
                        >
                          <td className="chevron-cell">
                            <div className="chevron-wrapper">
                              <span
                                style={textStyle("body-xs", "primary-grey-100")}
                              >
                                {expandedRow === `row${index}` ? (
                                  <FaChevronUp size={ICON_SIZE} />
                                ) : (
                                  <FaChevronDown size={ICON_SIZE} />
                                )}
                              </span>
                            </div>
                          </td>
                          <td>
                            <span
                              style={textStyle("body-xs-bold", "primary-grey-80")}
                            >
                              {consent.consentId}
                            </span>
                          </td>
                          <td>
                            <span
                              style={textStyle("body-xs-bold", "primary-grey-80")}
                            >
                              {formatToIST(consent.startDate)}
                            </span>
                          </td>
                          <td>
                            <span
                              style={textStyle("body-xs-bold", "primary-grey-80")}
                            >
                              {formatToIST(consent.endDate)}
                            </span>
                          </td>
                          <td>
                            <div
                              className={
                                consent.status === "ACTIVE"
                                  ? "badge-active"
                                  : consent.status === "WITHDRAWN"
                                    ? "badge-withdraw"
                                    : consent.status === "EXPIRED"
                                      ? "badge-expired"
                                      : "badge-text"
                              }
                            >
                              <p
                                className={
                                  consent.status === "ACTIVE"
                                    ? "badge-text-active"
                                    : consent.status === "WITHDRAWN"
                                      ? "badge-text-withdraw"
                                      : consent.status === "EXPIRED"
                                        ? "badge-text-expired"
                                        : "badge-text"
                                }
                              >
                                {getTranslatedStatus(consent.status)}
                              </p>
                            </div>
                          </td>
                          <td className="consent-actions-cell">
                            <div
                              style={{
                                display: "flex",
                                gap: "2rem",
                                alignItems: "center",
                              }}
                            >
                              <FaEye
                                aria-label={`View details for consent ${consent.consentId}`}
                                role="button"
                                tabIndex={0}
                                size={ICON_SIZE}
                                style={{ cursor: "pointer" }}
                                onClick={() =>
                                  handleCallAPI(
                                    consent.consentId,
                                    consent.consentHandleId,
                                    consent.languagePreferences,
                                    consent.status,
                                    consent.startDate,
                                  )
                                }
                              />
                              {consent.status === "ACTIVE" && (
                                <FaPencilAlt
                                  aria-label={`Edit consent ${consent.consentId}`}
                                  role="button"
                                  tabIndex={0}
                                  size={ICON_SIZE} // instead of height & width
                                  style={{ cursor: "pointer" }}
                                  onClick={() =>
                                    handleCallAPI2(
                                      consent.consentId,
                                      consent.consentHandleId,
                                      consent.languagePreferences,
                                      consent.status,
                                      consent.startDate,
                                    )
                                  }
                                />
                              )}

                              {consent.status != "WITHDRAWN" &&
                                consent.status != "EXPIRED" &&
                                consentIdListForAutoRenew[consent.consentId] ===
                                  true && (
                                  <FaExclamationCircle
                                    size={ICON_SIZE}
                                    onClick={() =>
                                      handleCallAPI3(
                                        consent.consentId,
                                        consent.consentHandleId,
                                        consent.languagePreferences,
                                        consent.status,
                                        consent.startDate,
                                      )
                                    }
                                  />
                                )}
                            </div>
                          </td>
                        </tr>

                        {/* Expanded Row */}
                        {expandedRow === `row${index}` && (
                          <tr className="expanded-row">
                            <td></td>
                            <td colSpan="5">
                              <table
                                className="inner-table"
                                role="table"
                                aria-label="Detailed purpose, data type, data item, processor, and status information"
                              >
                                <colgroup>
                                  <col style={{ width: "25%" }} />
                                  <col style={{ width: "20%" }} />
                                  <col style={{ width: "20%" }} />
                                  <col style={{ width: "15%" }} />
                                  <col style={{ width: "15%" }} />
                                </colgroup>

                                <thead>
                                  <tr>
                                    <th>
                                      <span
                                        style={textStyle("body-xs-bold", "primary-grey-80")}
                                      >
                                        {getTranslation(
                                          "fid_purpose_name",
                                          "Purpose Name",
                                        )}
                                      </span>
                                    </th>

                                    <th>
                                      <span
                                        style={textStyle("body-xs-bold", "primary-grey-80")}
                                      >
                                        {getTranslation(
                                          "fid_data_type",
                                          "Data type",
                                        )}
                                      </span>
                                    </th>
                                    <th>
                                      <span
                                        style={textStyle("body-xs-bold", "primary-grey-80")}
                                      >
                                        {getTranslation(
                                          "fid_data_item",
                                          "Data Item",
                                        )}
                                      </span>
                                    </th>
                                    <th>
                                      <span
                                        style={textStyle("body-xs-bold", "primary-grey-80")}
                                      >
                                        {getTranslation(
                                          "fid_data_used_by",
                                          "Data used by",
                                        )}
                                      </span>
                                    </th>
                                    <th>
                                      <span
                                        style={textStyle("body-xs-bold", "primary-grey-80")}
                                      >
                                        {getTranslation(
                                          "fid_data_status",
                                          "Status",
                                        )}
                                      </span>
                                    </th>
                                  </tr>
                                </thead>

                                <tbody>
                                  {consent.preferences.map((pref) =>
                                    pref.purposeList.map((purpose) => (
                                      <tr key={purpose.purposeId}>
                                        <td>
                                              <span
                                                style={textStyle("body-xs-bold", "primary-grey-80")}
                                              >
                                            {getDynamicTranslation(
                                              purpose?.purposeInfo?.purposeName,
                                            )}
                                          </span>
                                        </td>

                                        <td>
                                              <span
                                                style={textStyle("body-xs-bold", "primary-grey-80")}
                                              >
                                            {pref.processorActivityList
                                              .flatMap((pa) =>
                                                pa?.processActivityInfo?.dataTypesList.map(
                                                  (dt) =>
                                                    getDynamicTranslation(
                                                      dt.dataTypeName,
                                                    ),
                                                ),
                                              )
                                              .join(", ")}
                                          </span>
                                        </td>
                                        <td>
                                              <span
                                                style={textStyle("body-xs-bold", "primary-grey-80")}
                                              >
                                            {pref.processorActivityList
                                              .flatMap((pa) =>
                                                pa?.processActivityInfo?.dataTypesList.flatMap(
                                                  (dt) =>
                                                    dt.dataItems.map((item) =>
                                                      getDynamicTranslation(
                                                        item,
                                                      ),
                                                    ),
                                                ),
                                              )
                                              .join(", ")}
                                          </span>
                                        </td>
                                        <td>
                                              <span
                                                style={textStyle("body-xs-bold", "primary-grey-80")}
                                              >
                                            {pref.processorActivityList
                                              .flatMap((pa) =>
                                                getDynamicTranslation(
                                                  pa?.processActivityInfo
                                                    ?.processorName,
                                                ),
                                              )
                                              .join(", ")}
                                          </span>
                                        </td>
                                        <td>
                                          <span
                                            className={
                                              pref.preferenceStatus ===
                                              "ACCEPTED"
                                                ? "accepted"
                                                : "not-accepted"
                                            }
                                          >
                                              <span
                                                style={textStyle("body-xs-bold", "primary-grey-80")}
                                              >
                                              {getTranslatedPrefStatus(
                                                pref?.preferenceStatus,
                                              )}
                                            </span>
                                          </span>
                                        </td>
                                      </tr>
                                    )),
                                  )}
                                </tbody>
                              </table>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    ))
                  );
                })()
              )}
            </tbody>
          </table>
          {Array.isArray(paginatedConsents) && paginatedConsents.length > 0 && (
            <div className="pagination-container">
              <div
                role="button"
                tabIndex={0}
                aria-label="Previous page"
                onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    setCurrentPage((p) => Math.max(p - 1, 1));
                  }
                }}
                style={{
                  cursor: currentPage === 1 ? "not-allowed" : "pointer",
                  width: "36px",
                  height: "36px",
                  borderRadius: "50%",
                  backgroundColor: "#fff", // white background
                  border: "1px solid #d1d5db", // light grey border
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  opacity: currentPage === 1 ? 0.5 : 1,
                }}
              >
                <FiChevronLeft size={ICON_SIZE} color="#555" />
              </div>

              <span className="pagination-text">
                {getTranslation("fid_page", "Page")} {currentPage}{" "}
                {getTranslation("fid_of", "of")} {totalPages}
              </span>

              <div
                role="button"
                tabIndex={0}
                aria-label="Next page"
                onClick={() =>
                  setCurrentPage((p) => Math.min(p + 1, totalPages))
                }
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    setCurrentPage((p) => Math.min(p + 1, totalPages));
                  }
                }}
                style={{
                  cursor:
                    currentPage === totalPages ? "not-allowed" : "pointer",
                  width: "36px",
                  height: "36px",
                  borderRadius: "50%",
                  backgroundColor: "#fff", // white background
                  border: "1px solid #d1d5db", // light grey border
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  opacity: currentPage === totalPages ? 0.5 : 1,
                }}
                onMouseEnter={(e) => {
                  if (currentPage !== totalPages) {
                    e.currentTarget.style.backgroundColor = "#e0e0e0";
                  }
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.backgroundColor = "#f0f0f0";
                }}
              >
                <FiChevronRight size={ICON_SIZE} color="#555" />
              </div>
            </div>
          )}
        </div>
        {showModal && (
          <div className="modal-outer-container">
            {darkModeToBeShown && (
              <div className="theme-toggle-container">
                <label className="theme-switch">
                  <input
                    type="checkbox"
                    checked={isDarkMode}
                    onChange={() => setIsDarkMode(!isDarkMode)}
                  />
                  <span className="slider round"></span>
                </label>
              </div>
            )}

            <div
              className="modal-container"
              style={{
                backgroundColor: activeTheme.cardBackground,
                color: activeTheme.cardFont,
              }}
            >
              <div className="container-1">
                <button
                  class="close-btn"
                  style={{ "--close-color": activeTheme.buttonBackground }}
                  onClick={closeWithdrawConsentModal}
                ></button>
              </div>
              <div className="fidu-heading-container">
                <div className="container-2-left">
                  <p className="container-2-left-1">{modalHeading}</p>
                  <div className="container-2-left-2">
                    <div className="container-2-left-2-dropdown">
                      <FaLanguage
                        size={ICON_SIZE}
                        className="container-2-left-2-dropdown-Icon"
                        style={{ color: `${activeTheme.cardFont}` }}
                      />

                      <select
                        className="container-2-left-2-dropdown-select"
                        value={selectedLang}
                        onChange={handleLanguageChange}
                        // disabled={selectedConsentStatus !== "ACTIVE"}
                        disabled
                        onClick={() => setIsOpen((prev) => !prev)}
                        onBlur={() => setIsOpen(false)}
                      >
                        {languages.map((lang) => (
                          <option key={lang.value} value={lang.value}>
                            {lang.label}
                          </option>
                        ))}
                      </select>
                      {isOpen ? (
                        <FaChevronUp
                          size={ICON_SIZE}
                          className="container-2-left-2-dropdown-Icon-right"
                        />
                      ) : (
                        <FaChevronDown
                          size={ICON_SIZE}
                          className="container-2-left-2-dropdown-Icon-right"
                        />
                      )}
                    </div>
                  </div>
                </div>
                <img src={logoUrl} alt="Logo" className="logo-image" />
              </div>
              <div className="mainScrollableContainer">
                <div className="container-3">
                  <p>{topDescription}</p>
                </div>
                <div className="container-4">
                  <p>
                    {customerIdLabel}: {cusId}
                  </p>
                </div>
                {formData && Object.keys(formData).length > 0 && (
                  <div
                    onClick={() => setShowRequiredDetails(!showREquiredDetails)}
                  >
                    <div
                      className="container-5-required"
                      // onClick={() => setShowRequiredDetails(!showREquiredDetails)}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-start",
                        }}
                      >
                        <svg
                          width="30"
                          height="30"
                          viewBox="5 0 12 23"
                          style={{
                            // position: "absolute",
                            top: "50%",
                            right: "10px",
                            transform: showREquiredDetails
                              ? "translateY(0%) rotate(180deg)"
                              : "translateY(0%) rotate(0deg)",
                            pointerEvents: "none",
                            transition: "transform 0.3s ease",
                            color: activeTheme.buttonBackground,
                          }}
                        >
                          <path
                            d="M12 15a1.002 1.002 0 01-.71-.29l-4-4a1.004 1.004 0 111.42-1.42l3.29 3.3 3.29-3.3a1.004 1.004 0 111.42 1.42l-4 4A1.001 1.001 0 0112 15z"
                            fill="currentColor"
                          />
                        </svg>
                      </div>

                      <div className="container-Heading-fs">
                        <p>{requiredText}</p>
                      </div>

                      <div className="badge-container-outer">
                        <div
                          className="badge-container"
                          style={{
                            border: `1px solid ${activeTheme.cardFont}`,
                            backgroundColor: activeTheme.cardBackground,
                          }}
                        >
                          <p
                            className="modal-badge"
                            style={{ color: activeTheme.cardFont }}
                          >
                            {activeBadge}
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="required-details-container"></div>
                  </div>
                )}
                {showREquiredDetails && (
                  <div>
                    {formData.map((item, index) => (
                      <div key={index} className="data-row-container-fidu">
                        <div></div>
                        <div style={{ marginTop: "0.5rem" }}>
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {purposeLabel}:{" "}
                            </span>{" "}
                            {item.purpose.join(", ")}
                          </p>
                          {validitytoBeShown && (
                            <p className="data-row">
                              <span style={{ fontWeight: 700 }}>
                                {durationLabel}:
                              </span>{" "}
                              {item.duration.value} {item.duration.unit}
                            </p>
                          )}
                          {!(
                            dataItemToBeShown === false &&
                            processActivityNameToBeShown === false &&
                            processorNameToBeShown === false
                          ) && (
                            <div className="required-details-container"></div>
                          )}
                          {/* {processorNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {usedbyLabel}:
                            </span>{" "}
                            {item.usedBy.join(", ")}
                          </p>
                        )}

                        {dataItemToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {dataItemLabel} :
                            </span>
                            {item.dataItems.join(", ")}
                          </p>
                        )}

                        {processActivityNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {processingActivityLabel}:
                            </span>
                            {item.processingActivity.join(", ")}
                          </p>
                        )} */}
                          {item.processorActivities.map((activity, idx) => (
                            <React.Fragment key={idx}>
                              <div style={{ marginTop: "0.5rem" }}>
                                {processActivityNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {processingActivityLabel}:
                                    </span>{" "}
                                    {activity.activityName}
                                  </p>
                                )}
                                {processorNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {usedbyLabel}:
                                    </span>{" "}
                                    {activity.processorName}
                                  </p>
                                )}
                                {activity.dataTypes.map((dt, dtIdx) => (
                                  <div key={dtIdx}>
                                    {dataItemToBeShown && (
                                      <>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataTypeLabel}:
                                          </span>{" "}
                                          {dt.dataTypeName}
                                        </p>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataItemLabel}:
                                          </span>{" "}
                                          {dt.dataItems.join(", ")}
                                        </p>
                                      </>
                                    )}

                                    {/* Divider between multiple data types */}
                                    {/* {dtIdx !== activity.dataTypes.length - 1 && (
                                    <div className="required-details-container"></div>
                                  )} */}
                                  </div>
                                ))}
                              </div>

                              {!(
                                dataItemToBeShown === false &&
                                processActivityNameToBeShown === false &&
                                processorNameToBeShown === false
                              ) &&
                                idx !== item.processorActivities.length - 1 && (
                                  <div className="required-details-container"></div>
                                )}
                            </React.Fragment>
                          ))}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                {formOtherData && Object.keys(formOtherData).length > 0 && (
                  <div>
                    <div
                      className="container-6"
                      onClick={() => setShowOthersDetails(!showOhersDetails)}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-start",
                          alignItems: "center",
                        }}
                      >
                        <svg
                          width="30"
                          height="30"
                          viewBox="5 0 12 23"
                          style={{
                            // position: "absolute",
                            top: "50%",
                            right: "10px",
                            transform: showOhersDetails
                              ? "translateY(0%) rotate(180deg)"
                              : "translateY(0%) rotate(0deg)",
                            pointerEvents: "none",
                            transition: "transform 0.3s ease",
                            color: activeTheme.buttonBackground,
                          }}
                        >
                          <path
                            d="M12 15a1.002 1.002 0 01-.71-.29l-4-4a1.004 1.004 0 111.42-1.42l3.29 3.3 3.29-3.3a1.004 1.004 0 111.42 1.42l-4 4A1.001 1.001 0 0112 15z"
                            fill="currentColor"
                          />
                        </svg>
                        <div className="container-Heading-fs">
                          <p>{otherAccordionHeader}</p>
                        </div>
                      </div>

                      <input
                        type="checkbox"
                        className="container-6-checkbox"
                        checked={allSelected}
                        disabled
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        style={{
                          appearance: "none",
                          WebkitAppearance: "none",
                          MozAppearance: "none",
                          height: "25px",
                          width: "25px",
                          borderRadius: "4px",
                          border: `1px solid ${activeTheme.cardFont}`,
                          cursor: "pointer",
                          backgroundColor: allSelected ? "blue" : "white",
                          backgroundImage: allSelected
                            ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                            : "none",
                          backgroundRepeat: "no-repeat",
                          backgroundPosition: "center",
                        }}
                      />
                    </div>
                    <div className="required-details-container"></div>
                  </div>
                )}
                {showOhersDetails && (
                  <div>
                    {formOtherData.map((item, index) => (
                      <div key={index} className="others-data-row-container">
                        <div></div>
                        <div style={{ marginTop: "0.5rem" }}>
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {" "}
                              {purposeLabel}:{" "}
                            </span>{" "}
                            {item.purpose.join(", ")}
                          </p>
                          {validitytoBeShown && (
                            <p className="data-row">
                              <span style={{ fontWeight: 700 }}>
                                {durationLabel}:
                              </span>{" "}
                              {item.duration.value} {item.duration.unit}
                            </p>
                          )}
                          {!(
                            dataItemToBeShown === false &&
                            processActivityNameToBeShown === false &&
                            processorNameToBeShown === false
                          ) && (
                            <div className="required-details-container"></div>
                          )}
                          {/* {processorNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {usedbyLabel}:
                            </span>{" "}
                            {item.usedBy.join(", ")}
                          </p>
                        )}

                        {dataItemToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {" "}
                              {dataItemLabel}:
                            </span>

                            {item.dataItems.join(", ")}
                          </p>
                        )}

                        {processActivityNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {processingActivityLabel}:
                            </span>
                            {item.processingActivity.join(", ")}
                          </p>
                        )} */}
                          {item.processorActivities.map((activity, idx) => (
                            <React.Fragment key={idx}>
                              <div style={{ marginTop: "0.5rem" }}>
                                {processActivityNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {processingActivityLabel}:
                                    </span>{" "}
                                    {activity.activityName}
                                  </p>
                                )}

                                {processorNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {usedbyLabel}:
                                    </span>{" "}
                                    {activity.processorName}
                                  </p>
                                )}
                                {activity.dataTypes.map((dt, dtIdx) => (
                                  <div key={dtIdx}>
                                    {dataItemToBeShown && (
                                      <>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            Data type:
                                          </span>{" "}
                                          {dt.dataTypeName}
                                        </p>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            Data item:
                                          </span>{" "}
                                          {dt.dataItems.join(", ")}
                                        </p>
                                      </>
                                    )}

                                    {/* Divider between multiple data types
                                  {dtIdx !== activity.dataTypes.length - 1 && (
                                    <div className="required-details-container"></div>
                                  )} */}
                                  </div>
                                ))}
                              </div>

                              {!(
                                dataItemToBeShown === false &&
                                processActivityNameToBeShown === false &&
                                processorNameToBeShown === false
                              ) &&
                                idx !== item.processorActivities.length - 1 && (
                                  <div className="required-details-container"></div>
                                )}
                            </React.Fragment>
                          ))}
                        </div>

                        <div
                          style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            marginTop: "0.5rem",
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={
                              selectedPrefereneces[item.id] === "ACCEPTED"
                            }
                            disabled
                            onChange={(e) =>
                              handlePreferenceChange(item.id, e.target.checked)
                            }
                            style={{
                              appearance: "none",
                              WebkitAppearance: "none",
                              MozAppearance: "none",
                              height: "25px",
                              width: "25px",
                              borderRadius: "4px",
                              border: `1px solid ${activeTheme.cardFont}`,
                              cursor: "pointer",
                              display: "inline-block",
                              position: "relative",
                              backgroundColor:
                                selectedPrefereneces[item.id] === "ACCEPTED"
                                  ? "blue"
                                  : "white",
                              backgroundImage:
                                selectedPrefereneces[item.id] === "ACCEPTED"
                                  ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                                  : "none",
                              backgroundRepeat: "no-repeat",
                              backgroundPosition: "center",
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                <div className="documentText">
                  <p
                    style={{
                      cursor: "not-allowed",
                      color: "#aaa",
                      pointerEvents: "none",
                    }}
                    onClick={openDocument}
                  >
                    {documentName}
                  </p>
                </div>
                <div className="container-7">
                  {parentalControl && (
                    <label className="container-7-outer">
                      <input
                        type="checkbox"
                        disabled
                        className="container-7-checkbox"
                        checked={isParentChecked}
                        onChange={(e) => checkParentKYC(e.target.checked)}
                        style={{
                          border: `1px solid ${activeTheme.cardFont}`,
                          backgroundColor: isParentChecked ? "blue" : "white",
                          backgroundImage: isParentChecked
                            ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                            : "none",
                          backgroundRepeat: "no-repeat",
                          backgroundPosition: "center",
                        }}
                      />

                      <p className="container-7-text">{ageSentence}</p>
                    </label>
                  )}
                </div>
                <div className="container-8">
                  <p>
                    {rightsDescription}{" "}
                    <span
                      style={{
                        cursor: "pointer",
                        color: activeTheme.linkFont,
                      }}
                    >
                      {/* Click here */}
                    </span>
                  </p>
                </div>
                <div className="container-9">
                  <p>{permissionDescription}</p>
                </div>
              </div>
              {selectedConsentStatus === "ACTIVE" && (
                <div className="container-10">
                  <button
                    className="container-10-button"
                    style={{
                      backgroundColor: activeTheme.buttonFont,
                      border: "1px solid rgb(181, 181, 181)",
                    }}
                    onClick={handleConfirm}
                  >
                    <p
                      style={{
                        fontFamily: FONT_FAMILY_STACK,
                        fontWeight: 700,
                        // color: "rgba(102, 0, 20, 1)",
                        color: activeTheme.buttonBackground,
                        fontSize: "11px",
                      }}
                    >
                      {withdrawConsentBtnText}
                    </p>
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
        {sessionExpired && (
          <SessionExpiredModal
            isOpen={sessionExpired}
            onClose={() => setSessionExpired(false)}
          />
        )}
        {showUpdateModal && (
          <div className="modal-outer-container">
            {darkModeToBeShown && (
              <div className="theme-toggle-container">
                <label className="theme-switch">
                  <input
                    type="checkbox"
                    checked={isDarkMode}
                    onChange={() => setIsDarkMode(!isDarkMode)}
                  />
                  <span className="slider round"></span>
                </label>
              </div>
            )}

            <div
              className="modal-container"
              style={{
                backgroundColor: activeTheme.cardBackground,
                color: activeTheme.cardFont,
              }}
            >
              <div className="container-1">
                <button
                  class="close-btn"
                  style={{ "--close-color": activeTheme.buttonBackground }}
                  onClick={closeEditConsentModal}
                ></button>
              </div>
              <div className="fidu-heading-container">
                <div className="container-2-left">
                  <p className="container-2-left-1">{modalHeading}</p>
                  <div className="container-2-left-2">
                    <div className="container-2-left-2-dropdown">
                      <FaLanguage
                        size={ICON_SIZE}
                        className="container-2-left-2-dropdown-Icon"
                        style={{ color: `${activeTheme.cardFont}` }}
                      />

                      <select
                        className="container-2-left-2-dropdown-select"
                        value={selectedLang}
                        onChange={handleLanguageChange}
                        disabled={selectedConsentStatus !== "ACTIVE"}
                        onClick={() => setIsOpen((prev) => !prev)}
                        onBlur={() => setIsOpen(false)}
                        style={{
                          backgroundColor: activeTheme.cardBackground,
                          color: activeTheme.cardFont,
                        }}
                      >
                        {languages.map((lang) => (
                          <option key={lang.value} value={lang.value}>
                            {lang.label}
                          </option>
                        ))}
                      </select>
                      {isOpen ? (
                        <FaChevronUp
                          size={ICON_SIZE}
                          className="container-2-left-2-dropdown-Icon-right"
                          style={{ color: `${activeTheme.cardFont}` }}
                        />
                      ) : (
                        <FaChevronDown
                          size={ICON_SIZE}
                          className="container-2-left-2-dropdown-Icon-right"
                          style={{ color: `${activeTheme.cardFont}` }}
                        />
                      )}
                    </div>
                  </div>
                </div>
                <img src={logoUrl} alt="Logo" className="logo-image" />
              </div>
              <div className="mainScrollableContainer">
                <div className="container-3">
                  <p>{topDescription}</p>
                </div>
                <div className="container-4">
                  <p>
                    {customerIdLabel}: {cusId}
                  </p>
                </div>
                {formData && Object.keys(formData).length > 0 && (
                  <div
                    onClick={() => setShowRequiredDetails(!showREquiredDetails)}
                  >
                    <div
                      className="container-5-required"
                      // onClick={() => setShowRequiredDetails(!showREquiredDetails)}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-start",
                        }}
                      >
                        <svg
                          width="30"
                          height="30"
                          viewBox="5 0 12 23"
                          style={{
                            // position: "absolute",
                            top: "50%",
                            right: "10px",
                            transform: showREquiredDetails
                              ? "translateY(0%) rotate(180deg)"
                              : "translateY(0%) rotate(0deg)",
                            pointerEvents: "none",
                            transition: "transform 0.3s ease",
                            color: activeTheme.buttonBackground,
                          }}
                        >
                          <path
                            d="M12 15a1.002 1.002 0 01-.71-.29l-4-4a1.004 1.004 0 111.42-1.42l3.29 3.3 3.29-3.3a1.004 1.004 0 111.42 1.42l-4 4A1.001 1.001 0 0112 15z"
                            fill="currentColor"
                          />
                        </svg>
                      </div>

                      <div className="container-Heading-fs">
                        <p>{requiredText}</p>
                      </div>

                      <div className="badge-container-outer">
                        <div
                          className="badge-container"
                          style={{
                            border: `1px solid ${activeTheme.cardFont}`,
                            backgroundColor: activeTheme.cardBackground,
                          }}
                        >
                          <p
                            className="modal-badge"
                            style={{ color: activeTheme.cardFont }}
                          >
                            {activeBadge}
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="required-details-container"></div>
                  </div>
                )}
                {showREquiredDetails && (
                  <div>
                    {formData.map((item, index) => (
                      <div key={index} className="data-row-container-fidu">
                        <div></div>
                        <div style={{ marginTop: "0.5rem" }}>
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {purposeLabel}:{" "}
                            </span>
                            {item.purpose.join(", ")}
                          </p>
                          {validitytoBeShown && (
                            <p className="data-row">
                              <span style={{ fontWeight: 700 }}>
                                {durationLabel}:
                              </span>{" "}
                              {item.duration.value} {item.duration.unit}
                            </p>
                          )}
                          {!(
                            dataItemToBeShown === false &&
                            processActivityNameToBeShown === false &&
                            processorNameToBeShown === false
                          ) && (
                            <div className="required-details-container"></div>
                          )}

                          {/* {processorNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {usedByLabel}:
                            </span>{" "}
                            {item.usedBy.join(", ")}
                          </p>
                        )}

                        {dataItemToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {dataItemLabel}:
                            </span>
                            {item.dataItems.join(", ")}
                          </p>
                        )}

                        {processActivityNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {processingActivityLabel}:
                            </span>
                            {item.processingActivity.join(", ")}
                          </p>
                        )} */}
                          {item.processorActivities.map((activity, idx) => (
                            <React.Fragment key={idx}>
                              <div style={{ marginTop: "0.5rem" }}>
                                {processActivityNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {processingActivityLabel}:
                                    </span>{" "}
                                    {activity.activityName}
                                  </p>
                                )}

                                {processorNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {usedbyLabel}:
                                    </span>{" "}
                                    {activity.processorName}
                                  </p>
                                )}
                                {activity.dataTypes.map((dt, dtIdx) => (
                                  <div key={dtIdx}>
                                    {dataItemToBeShown && (
                                      <>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataTypeLabel}:
                                          </span>{" "}
                                          {dt.dataTypeName}
                                        </p>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataItemLabel}:
                                          </span>{" "}
                                          {dt.dataItems.join(", ")}
                                        </p>
                                      </>
                                    )}
                                  </div>
                                ))}
                              </div>

                              {!(
                                dataItemToBeShown === false &&
                                processActivityNameToBeShown === false &&
                                processorNameToBeShown === false
                              ) &&
                                idx !== item.processorActivities.length - 1 && (
                                  <div className="required-details-container"></div>
                                )}
                            </React.Fragment>
                          ))}
                        </div>
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            marginTop: "0.5rem",
                          }}
                        ></div>
                      </div>
                    ))}
                  </div>
                )}
                {/* 
              <div className="container-divider"></div> */}
                {formOtherData && Object.keys(formOtherData).length > 0 && (
                  <div>
                    <div
                      className="container-6"
                      onClick={() => setShowOthersDetails(!showOhersDetails)}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-start",
                          alignItems: "center",
                        }}
                      >
                        <svg
                          width="30"
                          height="30"
                          viewBox="5 0 12 23"
                          style={{
                            // position: "absolute",
                            top: "50%",
                            right: "10px",
                            transform: showOhersDetails
                              ? "translateY(0%) rotate(180deg)"
                              : "translateY(0%) rotate(0deg)",
                            pointerEvents: "none",
                            transition: "transform 0.3s ease",
                            color: activeTheme.buttonBackground,
                          }}
                        >
                          <path
                            d="M12 15a1.002 1.002 0 01-.71-.29l-4-4a1.004 1.004 0 111.42-1.42l3.29 3.3 3.29-3.3a1.004 1.004 0 111.42 1.42l-4 4A1.001 1.001 0 0112 15z"
                            fill="currentColor"
                          />
                        </svg>
                        <div className="container-Heading-fs">
                          <p>{otherAccordionHeader}</p>
                        </div>
                      </div>

                      <input
                        type="checkbox"
                        className="container-6-checkbox"
                        checked={allSelected}
                        onChange={(e) => handleSelectAll(e.target.checked)}
                        style={{
                          appearance: "none",
                          WebkitAppearance: "none",
                          MozAppearance: "none",
                          height: "25px",
                          width: "25px",
                          borderRadius: "4px",
                          border: `1px solid ${activeTheme.cardFont}`,
                          cursor: "pointer",
                          backgroundColor: allSelected ? "blue" : "white",
                          backgroundImage: allSelected
                            ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                            : "none",
                          backgroundRepeat: "no-repeat",
                          backgroundPosition: "center",
                        }}
                      />
                    </div>
                    <div className="required-details-container"></div>
                  </div>
                )}
                {showOhersDetails && (
                  <div>
                    {formOtherData.map((item, index) => (
                      <div key={index} className="others-data-row-container">
                        <div></div>
                        <div style={{ marginTop: "0.5rem" }}>
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {purposeLabel}:{" "}
                            </span>{" "}
                            {item.purpose.join(", ")}
                          </p>
                          {validitytoBeShown && (
                            <p className="data-row">
                              <span style={{ fontWeight: 700 }}>
                                {durationLabel}:
                              </span>{" "}
                              {item.duration.value} {item.duration.unit}
                            </p>
                          )}
                          {!(
                            dataItemToBeShown === false &&
                            processActivityNameToBeShown === false &&
                            processorNameToBeShown === false
                          ) && (
                            <div className="required-details-container"></div>
                          )}

                          {/* {processorNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {usedByLabel}:
                            </span>{" "}
                            {item.usedBy.join(", ")}
                          </p>
                        )}

                        {dataItemToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {dataItemLabel}:
                            </span>
                            {item.dataItems.join(", ")}
                          </p>
                        )}

                        {processActivityNameToBeShown && (
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {processingActivityLabel}:
                            </span>
                            {item.processingActivity.join(", ")}
                          </p>
                        )} */}

                          {item.processorActivities.map((activity, idx) => (
                            <React.Fragment key={idx}>
                              <div style={{ marginTop: "0.5rem" }}>
                                {processActivityNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {processingActivityLabel}:
                                    </span>{" "}
                                    {activity.activityName}
                                  </p>
                                )}

                                {processorNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {usedbyLabel}:
                                    </span>{" "}
                                    {activity.processorName}
                                  </p>
                                )}
                                {activity.dataTypes.map((dt, dtIdx) => (
                                  <div key={dtIdx}>
                                    {dataItemToBeShown && (
                                      <>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataTypeLabel}:
                                          </span>{" "}
                                          {dt.dataTypeName}
                                        </p>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataItemLabel}:
                                          </span>{" "}
                                          {dt.dataItems.join(", ")}
                                        </p>
                                      </>
                                    )}
                                  </div>
                                ))}
                              </div>

                              {!(
                                dataItemToBeShown === false &&
                                processActivityNameToBeShown === false &&
                                processorNameToBeShown === false
                              ) &&
                                idx !== item.processorActivities.length - 1 && (
                                  <div className="required-details-container"></div>
                                )}
                            </React.Fragment>
                          ))}
                        </div>

                        <div
                          style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            marginTop: "0.5rem",
                          }}
                        >
                          <input
                            type="checkbox"
                            checked={
                              selectedPrefereneces[item.id] === "ACCEPTED"
                            }
                            onChange={(e) =>
                              handlePreferenceChange(item.id, e.target.checked)
                            }
                            style={{
                              appearance: "none",
                              WebkitAppearance: "none",
                              MozAppearance: "none",
                              height: "25px",
                              width: "25px",
                              borderRadius: "4px",
                              border: `1px solid ${activeTheme.cardFont}`,
                              cursor: "pointer",
                              display: "inline-block",
                              position: "relative",
                              backgroundColor:
                                selectedPrefereneces[item.id] === "ACCEPTED"
                                  ? "blue"
                                  : "white",
                              backgroundImage:
                                selectedPrefereneces[item.id] === "ACCEPTED"
                                  ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                                  : "none",
                              backgroundRepeat: "no-repeat",
                              backgroundPosition: "center",
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}
                <div className="documentText">
                  <p
                    style={{ cursor: "pointer", color: activeTheme.linkFont }}
                    onClick={openDocument}
                  >
                    {documentName}
                  </p>
                </div>
                <div className="container-7">
                  {parentalControl && (
                    <label className="container-7-outer">
                      <input
                        type="checkbox"
                        disabled
                        className="container-7-checkbox"
                        checked={isParentChecked}
                        onChange={(e) => checkParentKYC(e.target.checked)}
                        style={{
                          border: `1px solid ${activeTheme.cardFont}`,
                          backgroundColor: isParentChecked ? "blue" : "white",
                          backgroundImage: isParentChecked
                            ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                            : "none",
                          backgroundRepeat: "no-repeat",
                          backgroundPosition: "center",
                        }}
                      />

                      <p className="container-7-text">{ageSentence}</p>
                    </label>
                  )}
                </div>
                <div className="container-8">
                  <p>
                    {rightsDescription}{" "}
                    <span
                      style={{
                        cursor: "pointer",
                        color: activeTheme.linkFont,
                      }}
                    >
                      {/* Click here */}
                    </span>
                  </p>
                </div>
                <div className="container-9">
                  <p>{permissionDescription}</p>
                </div>
              </div>
              <div className="container-10-update">
                <button
                  className="container-10-update-withdraw"
                  onClick={handleUpdateConfirm}
                  style={{
                    backgroundColor: activeTheme.buttonFont,
                    border: "1px solid rgb(181, 181, 181)",
                  }}
                >
                  <p
                    className="container-10-update-withdraw-text"
                    style={{ color: activeTheme.buttonBackground }}
                  >
                    {withdrawConsentBtnText}
                  </p>
                </button>
                <button
                  className="container-10-update-consent"
                  onClick={handleUpdateConsent}
                  style={{
                    backgroundColor: activeTheme.buttonBackground,
                    border: "1px solid rgb(181, 181, 181)",
                  }}
                >
                  <p
                    className="container-10-update-consent-text "
                    style={{ color: activeTheme.buttonFont }}
                  >
                    {updateConsentBtnText}
                  </p>
                </button>
              </div>
            </div>
          </div>
        )}
        {showRenewModal && (
          <div className="modal-outer-container">
            {darkModeToBeShown && (
              <div className="theme-toggle-container">
                <label className="theme-switch">
                  <input
                    type="checkbox"
                    checked={isDarkMode}
                    onChange={() => setIsDarkMode(!isDarkMode)}
                  />
                  <span className="slider round"></span>
                </label>
              </div>
            )}

            <div
              className="modal-container"
              style={{
                backgroundColor: activeTheme.cardBackground,
                color: activeTheme.cardFont,
              }}
            >
              <div className="container-1">
                <button
                  class="close-btn"
                  style={{ "--close-color": activeTheme.buttonBackground }}
                  onClick={closeRenewConsentModal}
                ></button>
              </div>
              <div className="fidu-heading-container">
                <div className="container-2-left">
                  <p className="container-2-left-1">{modalHeading}</p>
                  <div className="container-2-left-2">
                    <div className="container-2-left-2-dropdown">
                      <FaLanguage
                        size={ICON_SIZE}
                        className="container-2-left-2-dropdown-Icon"
                        style={{ color: `${activeTheme.cardFont}` }}
                      />

                      <select
                        className="container-2-left-2-dropdown-select"
                        value={selectedLang}
                        onChange={handleLanguageChange}
                        disabled={selectedConsentStatus !== "ACTIVE"}
                        onClick={() => setIsOpen((prev) => !prev)}
                        onBlur={() => setIsOpen(false)}
                        style={{
                          backgroundColor: activeTheme.cardBackground,
                          color: activeTheme.cardFont,
                        }}
                      >
                        {languages.map((lang) => (
                          <option key={lang.value} value={lang.value}>
                            {lang.label}
                          </option>
                        ))}
                      </select>
                      {isOpen ? (
                        <FaChevronUp
                          size={ICON_SIZE}
                          className="container-2-left-2-dropdown-Icon-right"
                          style={{ color: `${activeTheme.cardFont}` }}
                        />
                      ) : (
                        <FaChevronDown
                          size={ICON_SIZE}
                          className="container-2-left-2-dropdown-Icon-right"
                          style={{ color: `${activeTheme.cardFont}` }}
                        />
                      )}
                    </div>
                  </div>
                </div>
                <img src={logoUrl} alt="Logo" className="logo-image" />
              </div>
              <div className="mainScrollableContainer">
                <div className="container-3">
                  <p>{topDescription}</p>
                </div>
                <div className="container-4">
                  <p>
                    {customerIdLabel}: {cusId}
                  </p>
                </div>
                {formData && Object.keys(formData).length > 0 && (
                  <div
                    onClick={() => setShowRequiredDetails(!showREquiredDetails)}
                  >
                    <div
                      className="container-5-required"
                      // onClick={() => setShowRequiredDetails(!showREquiredDetails)}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-start",
                        }}
                      >
                        <svg
                          width="30"
                          height="30"
                          viewBox="5 0 12 23"
                          style={{
                            // position: "absolute",
                            top: "50%",
                            right: "10px",
                            transform: showREquiredDetails
                              ? "translateY(0%) rotate(180deg)"
                              : "translateY(0%) rotate(0deg)",
                            pointerEvents: "none",
                            transition: "transform 0.3s ease",
                            color: activeTheme.buttonBackground,
                          }}
                        >
                          <path
                            d="M12 15a1.002 1.002 0 01-.71-.29l-4-4a1.004 1.004 0 111.42-1.42l3.29 3.3 3.29-3.3a1.004 1.004 0 111.42 1.42l-4 4A1.001 1.001 0 0112 15z"
                            fill="currentColor"
                          />
                        </svg>
                      </div>

                      <div className="container-Heading-fs">
                        <p>{requiredText}</p>
                      </div>

                      <div className="badge-container-outer">
                        <div
                          className="badge-container"
                          style={{
                            border: `1px solid ${activeTheme.cardFont}`,
                            backgroundColor: activeTheme.cardBackground,
                          }}
                        >
                          <p
                            className="modal-badge"
                            style={{ color: activeTheme.cardFont }}
                          >
                            {activeBadge}
                          </p>
                        </div>
                      </div>
                    </div>

                    <div className="required-details-container"></div>
                  </div>
                )}
                {showREquiredDetails && (
                  <div>
                    {formData.map((item, index) => (
                      <div
                        key={index}
                        className="data-row-container-fidu"
                        style={{
                          opacity:
                            Number(item.preferenceExpiry) < 30 &&
                            item.autoRenewStatus === true
                              ? 1
                              : 0.5,
                          pointerEvents:
                            Number(item.preferenceExpiry) < 30 &&
                            item.autoRenewStatus === true
                              ? "curosor"
                              : "none",
                        }}
                      >
                        <div></div>
                        <div style={{ marginTop: "0.5rem" }}>
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {purposeLabel}:{" "}
                            </span>
                            {item.purpose.join(", ")}
                          </p>
                          {validitytoBeShown && (
                            <p className="data-row">
                              <span style={{ fontWeight: 700 }}>
                                {durationLabel}:
                              </span>{" "}
                              {item.duration.value} {item.duration.unit}
                            </p>
                          )}
                          {!(
                            dataItemToBeShown === false &&
                            processActivityNameToBeShown === false &&
                            processorNameToBeShown === false
                          ) && (
                            <div className="required-details-container"></div>
                          )}

                          {item.processorActivities.map((activity, idx) => (
                            <React.Fragment key={idx}>
                              <div style={{ marginTop: "0.5rem" }}>
                                {processActivityNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {processingActivityLabel}:
                                    </span>{" "}
                                    {activity.activityName}
                                  </p>
                                )}

                                {processorNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {usedbyLabel}:
                                    </span>{" "}
                                    {activity.processorName}
                                  </p>
                                )}
                                {activity.dataTypes.map((dt, dtIdx) => (
                                  <div key={dtIdx}>
                                    {dataItemToBeShown && (
                                      <>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataTypeLabel}:
                                          </span>{" "}
                                          {dt.dataTypeName}
                                        </p>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataItemLabel}:
                                          </span>{" "}
                                          {dt.dataItems.join(", ")}
                                        </p>
                                      </>
                                    )}
                                  </div>
                                ))}
                              </div>

                              {!(
                                dataItemToBeShown === false &&
                                processActivityNameToBeShown === false &&
                                processorNameToBeShown === false
                              ) &&
                                idx !== item.processorActivities.length - 1 && (
                                  <div className="required-details-container"></div>
                                )}
                            </React.Fragment>
                          ))}
                        </div>
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            marginTop: "0.5rem",
                          }}
                        >
                          <input
                            type="checkbox"
                            onChange={(e) =>
                              handleRenewPrefereneceChange(
                                item.id,
                                e.target.checked,
                              )
                            }
                            checked={listOfRenewPref[item.id] === "RENEW"}
                            style={{
                              appearance: "none",
                              WebkitAppearance: "none",
                              MozAppearance: "none",
                              height: "25px",
                              width: "25px",
                              borderRadius: "4px",
                              border: `1px solid ${activeTheme.cardFont}`,
                              cursor: "pointer",
                              display: "inline-block",
                              position: "relative",
                              backgroundColor:
                                listOfRenewPref[item.id] === "RENEW"
                                  ? "blue"
                                  : "white",
                              backgroundImage:
                                listOfRenewPref[item.id] === "RENEW"
                                  ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                                  : "none",
                              backgroundRepeat: "no-repeat",
                              backgroundPosition: "center",
                            }}
                          />
                        </div>
                        <div
                          style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            marginTop: "0.5rem",
                          }}
                        ></div>
                      </div>
                    ))}
                  </div>
                )}
                {/* 
              <div className="container-divider"></div> */}
                {formOtherData && Object.keys(formOtherData).length > 0 && (
                  <div>
                    <div
                      className="container-6"
                      onClick={() => setShowOthersDetails(!showOhersDetails)}
                    >
                      <div
                        style={{
                          display: "flex",
                          justifyContent: "flex-start",
                          alignItems: "center",
                        }}
                      >
                        <svg
                          width="30"
                          height="30"
                          viewBox="5 0 12 23"
                          style={{
                            // position: "absolute",
                            top: "50%",
                            right: "10px",
                            transform: showOhersDetails
                              ? "translateY(0%) rotate(180deg)"
                              : "translateY(0%) rotate(0deg)",
                            pointerEvents: "none",
                            transition: "transform 0.3s ease",
                            color: activeTheme.buttonBackground,
                          }}
                        >
                          <path
                            d="M12 15a1.002 1.002 0 01-.71-.29l-4-4a1.004 1.004 0 111.42-1.42l3.29 3.3 3.29-3.3a1.004 1.004 0 111.42 1.42l-4 4A1.001 1.001 0 0112 15z"
                            fill="currentColor"
                          />
                        </svg>
                        <div className="container-Heading-fs">
                          <p>{otherAccordionHeader}</p>
                        </div>
                      </div>
                    </div>
                    <div className="required-details-container"></div>
                  </div>
                )}

                {showOhersDetails && (
                  <div>
                    {formOtherData.map((item, index) => (
                      <div
                        key={index}
                        className="others-data-row-container"
                        style={{
                          opacity:
                            Number(item.preferenceExpiry) < 30 &&
                            item.autoRenewStatus === true
                              ? 1
                              : 0.5,
                          pointerEvents:
                            Number(item.preferenceExpiry) < 30 &&
                            item.autoRenewStatus === true
                              ? "curosor"
                              : "none",
                        }}
                      >
                        <div></div>
                        <div style={{ marginTop: "0.5rem" }}>
                          <p className="data-row">
                            <span style={{ fontWeight: 700 }}>
                              {purposeLabel}:{" "}
                            </span>{" "}
                            {item.purpose.join(", ")}
                          </p>
                          {validitytoBeShown && (
                            <p className="data-row">
                              <span style={{ fontWeight: 700 }}>
                                {durationLabel}:
                              </span>{" "}
                              {item.duration.value} {item.duration.unit}
                            </p>
                          )}
                          {!(
                            dataItemToBeShown === false &&
                            processActivityNameToBeShown === false &&
                            processorNameToBeShown === false
                          ) && (
                            <div className="required-details-container"></div>
                          )}

                          {item.processorActivities.map((activity, idx) => (
                            <React.Fragment key={idx}>
                              <div style={{ marginTop: "0.5rem" }}>
                                {processActivityNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {processingActivityLabel}:
                                    </span>{" "}
                                    {activity.activityName}
                                  </p>
                                )}

                                {processorNameToBeShown && (
                                  <p className="data-row">
                                    <span style={{ fontWeight: 700 }}>
                                      {usedbyLabel}:
                                    </span>{" "}
                                    {activity.processorName}
                                  </p>
                                )}
                                {activity.dataTypes.map((dt, dtIdx) => (
                                  <div key={dtIdx}>
                                    {dataItemToBeShown && (
                                      <>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataTypeLabel}:
                                          </span>{" "}
                                          {dt.dataTypeName}
                                        </p>
                                        <p className="data-row">
                                          <span style={{ fontWeight: 700 }}>
                                            {dataItemLabel}:
                                          </span>{" "}
                                          {dt.dataItems.join(", ")}
                                        </p>
                                      </>
                                    )}
                                  </div>
                                ))}
                              </div>

                              {!(
                                dataItemToBeShown === false &&
                                processActivityNameToBeShown === false &&
                                processorNameToBeShown === false
                              ) &&
                                idx !== item.processorActivities.length - 1 && (
                                  <div className="required-details-container"></div>
                                )}
                            </React.Fragment>
                          ))}
                        </div>

                        <div
                          style={{
                            display: "flex",
                            justifyContent: "flex-end",
                            marginTop: "0.5rem",
                          }}
                        >
                          <input
                            type="checkbox"
                            onChange={(e) =>
                              handleRenewPrefereneceChange(
                                item.id,
                                e.target.checked,
                              )
                            }
                            checked={listOfRenewPref[item.id] === "RENEW"}
                            style={{
                              appearance: "none",
                              WebkitAppearance: "none",
                              MozAppearance: "none",
                              height: "25px",
                              width: "25px",
                              borderRadius: "4px",
                              border: `1px solid ${activeTheme.cardFont}`,
                              cursor: "pointer",
                              display: "inline-block",
                              position: "relative",
                              backgroundColor:
                                listOfRenewPref[item.id] === "RENEW"
                                  ? "blue"
                                  : "white",
                              backgroundImage:
                                listOfRenewPref[item.id] === "RENEW"
                                  ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                                  : "none",
                              backgroundRepeat: "no-repeat",
                              backgroundPosition: "center",
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                <div className="documentText">
                  <p
                    style={{ cursor: "pointer", color: activeTheme.linkFont }}
                    onClick={openDocument}
                  >
                    {documentName}
                  </p>
                </div>

                <div className="container-7">
                  {parentalControl && (
                    <label className="container-7-outer">
                      <input
                        type="checkbox"
                        disabled
                        className="container-7-checkbox"
                        checked={isParentChecked}
                        onChange={(e) => checkParentKYC(e.target.checked)}
                        style={{
                          border: `1px solid ${activeTheme.cardFont}`,
                          backgroundColor: isParentChecked ? "blue" : "white",
                          backgroundImage: isParentChecked
                            ? "url(\"data:image/svg+xml;utf8,<svg viewBox='0 0 16 16' xmlns='http://www.w3.org/2000/svg'><path d='M3 8l3 3 7-7' stroke='white' stroke-width='2' fill='none' stroke-linecap='round' stroke-linejoin='round'/></svg>\")"
                            : "none",
                          backgroundRepeat: "no-repeat",
                          backgroundPosition: "center",
                        }}
                      />

                      <p className="container-7-text">{ageSentence}</p>
                    </label>
                  )}
                </div>

                <div className="container-8">
                  <p>
                    {rightsDescription}{" "}
                    <span
                      style={{
                        cursor: "pointer",
                        color: activeTheme.linkFont,
                      }}
                    >
                      {/* Click here */}
                    </span>
                  </p>
                </div>
                <div className="container-9">
                  <p>{permissionDescription}</p>
                </div>
              </div>
              <div className="container-10-update">
                <button
                  className="container-10-update-consent"
                  onClick={handleRenewConsent}
                  style={{
                    backgroundColor: activeTheme.buttonBackground,
                    border: "1px solid rgb(181, 181, 181)",
                  }}
                >
                  <p
                    className="container-10-update-consent-text "
                    style={{ color: activeTheme.buttonFont }}
                  >
                    {renewConsentBtnText}
                  </p>
                </button>
              </div>
            </div>
          </div>
        )}
        {showParentModal && (
          <div className="modal-outer-container">
            <div
              className="modal-container"
              style={{
                backgroundColor: activeTheme.cardBackground,
                color: activeTheme.cardFont,
                // position: "relative", // for Step 1/2 absolute positioning
                // width: "400px",
                // padding: "20px",
                // borderRadius: "16px",
              }}
            >
              {selectedKYCMethod === "I am away from my parent" && (
                <div
                  className="step-header-parent"
                  style={{
                    display: "flex",
                    justifyContent:
                      currentModalStep === 2 ? "space-between" : "flex-end",
                    alignItems: "center",
                    marginBottom: "16px",
                  }}
                >
                  {currentModalStep === 2 && (
                    <button
                      onClick={() => setCurrentModalStep(1)}
                      style={{
                        background: "none",
                        border: "none",
                        cursor: "pointer",
                        padding: 0,
                        display: "flex",
                        alignItems: "center",
                        color: activeTheme.buttonBackground,
                      }}
                    >
                      <svg
                        width="24"
                        height="24"
                        viewBox="0 0 24 24"
                        fill="none"
                        xmlns="http://www.w3.org/2000/svg"
                      >
                        <path
                          d="M15 20a1.003 1.003 0 01-.71-.29l-7-7a1 1 0 010-1.42l7-7a1.005 1.005 0 011.42 1.42L9.41 12l6.3 6.29a.997.997 0 01.219 1.095.999.999 0 01-.93.615z"
                          fill="currentColor"
                        />
                      </svg>
                    </button>
                  )}

                  <p
                    className="step-text-parent"
                    style={{ fontWeight: 700, fontSize: "11px" }}
                  >
                    Step {currentModalStep}/2
                  </p>
                </div>
              )}

              {currentModalStep === 1 && (
                <>
                  <p className="parentTextStyle1">
                    Select verification method (Required)
                  </p>
                  <div className="dropdown-container-parent">
                    <div
                      className="dropdown-wrapper-parent"
                      style={{ position: "relative" }}
                    >
                      <select
                        className="custom-select-parent"
                        value={selectedKYCMethod}
                        onChange={(e) => setSelectedKYCMethod(e.target.value)}
                        onClick={() => setIsOpenParent((prev) => !prev)}
                        onBlur={() => setIsOpenParent(false)}
                        style={{
                          width: "100%",
                          height: "40px",
                          borderRadius: "8px",
                          border: "1px solid rgba(181, 181, 181, 1)",
                          paddingLeft: "8px",
                          paddingRight: "8px",
                          appearance: "none",
                          fontSize: "11px",
                        }}
                      >
                        <option value="">Select Method</option>
                        {digilockerClientId && (
                          <option value="digilocker">Digilocker</option>
                        )}

                        <option value="declaration">Declaration based</option>
                      </select>
                      {isOpenParent ? (
                        <FaChevronUp
                          size={ICON_SIZE}
                          style={{
                            position: "absolute",
                            right: "8px",
                            top: "50%",
                            transform: "translateY(-50%)",
                          }}
                        />
                      ) : (
                        <FaChevronDown
                          size={ICON_SIZE}
                          style={{
                            position: "absolute",
                            right: "8px",
                            top: "50%",
                            transform: "translateY(-50%)",
                          }}
                        />
                      )}
                    </div>
                  </div>

                  {selectedKYCMethod === "declaration" && (
                    <>
                      <p className="parentTextStyle1">
                        Parent's name (Required)
                      </p>
                      <input className="input-parent" ref={parentNameRef} />

                      <p className="parentTextStyle1">
                        Identity (Email or Mobile)
                      </p>
                      <input className="input-parent" ref={parentIdentityRef} />
                    </>
                  )}
                </>
              )}

              {currentModalStep === 2 && (
                <>
                  <p className="parentTextStyle3">OTP Verification</p>
                  <p
                    className="parentTextStyle1"
                    style={{ marginBottom: "16px" }}
                  >
                    Please enter the 6-digit OTP sent to {parentIdentity}.
                  </p>

                  <input
                    type="text"
                    ref={otpRef}
                    maxLength={6}
                    placeholder="000000"
                    className="otp-input-parent"
                    onInput={(e) => {
                      e.target.value = e.target.value.replace(/\D/g, "");
                    }}
                  />
                </>
              )}

              <div
                className="container-10"
                style={{
                  display: "flex",
                  justifyContent: "flex-end",
                  gap: "10px",
                }}
              >
                {currentModalStep < 2 && (
                  <button
                    className="container-10-button"
                    style={{
                      backgroundColor: activeTheme.buttonFont,
                      border: "1px solid rgba(181, 181, 181, 1)",
                    }}
                    onClick={() => {
                      closeParentModal();
                      setIsParentChecked(false);
                      setShowUpdateModal(true);
                    }}
                  >
                    <p
                      style={{
                        fontFamily: FONT_FAMILY_STACK,
                        fontWeight: 700,
                        color: activeTheme.buttonBackground,
                        fontSize: "11px",
                      }}
                    >
                      Cancel
                    </p>
                  </button>
                )}

                <button
                  className="container-10-button"
                  style={{
                    backgroundColor: activeTheme.buttonBackground,
                    border: "1px solid rgba(181, 181, 181, 1)",
                  }}
                  onClick={() => {
                    if (!selectedKYCMethod) {
                      toast.error(
                        (props) => (
                          <CustomToast
                            {...props}
                            type="error"
                            message={"Please select a method"}
                          />
                        ),
                        { icon: false },
                      );
                      return;
                    }
                    if (selectedKYCMethod === "digilocker") {
                      var responseType = "code";
                      var codeChallengeMethod = "S256";

                      generateConsentMetaId().then((consentMetaId) => {
                        if (!consentMetaId) return;

                        const state = consentMetaId;

                        generateCodeChallenge(digilockerCodeVerifier).then(
                          (codeChallenge) => {
                            const digilockerAuthUrl = `https://api.digitallocker.gov.in/public/oauth2/1/authorize?response_type=${responseType}&client_id=${digilockerClientId}&redirect_uri=${digilockerRedirectUri}&code_challenge=${codeChallenge}&code_challenge_method=${codeChallengeMethod}&state=${state}`;

                            setIsDigiLockerLoading(true);

                            // Try opening new tab
                            const newTab = window.open(
                              digilockerAuthUrl,
                              "_blank",
                              "noopener,noreferrer",
                            );

                            if (!newTab) {
                              setIsDigiLockerLoading(false);
                              return;
                            }

                            // Wait for the new tab to initialize before checking
                            const checkInterval = setInterval(() => {
                              if (newTab?.closed) {
                                clearInterval(checkInterval);

                                //setIsDigiLockerLoading(false);
                              }
                            }, 1000);
                          },
                        );
                      });

                      return;
                    }

                    if (selectedKYCMethod === "declaration") {
                      if (currentModalStep === 1) {
                        let name = parentNameRef.current.value.trim();
                        let identity = parentIdentityRef.current.value.trim();

                        name = name.replace(/\s+/g, " ");
                        parentNameRef.current.value = name;

                        const errors = [];

                        const nameRegex = /^[A-Za-z]+(?:\s[A-Za-z]+)*$/;

                        if (!name) {
                          errors.push("Please enter parent's name");
                        } else if (!nameRegex.test(name)) {
                          if (/[0-9]/.test(name)) {
                            errors.push(
                              "Please enter valid name (no numbers allowed)",
                            );
                          } else if (/[^A-Za-z\s]/.test(name)) {
                            errors.push(
                              "Please enter valid name (no special characters allowed)",
                            );
                          }
                        }

                        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
                        const mobileRegex = /^\d{10}$/;

                        identity = identity.replace(/\s+/g, "");
                        parentIdentityRef.current.value = identity;

                        if (!identity) {
                          errors.push(
                            "Please enter parent's identity (email or mobile)",
                          );
                        } else if (
                          !emailRegex.test(identity) &&
                          !mobileRegex.test(identity)
                        ) {
                          errors.push(
                            "Please enter valid identity (email or 10-digit mobile number)",
                          );
                        }

                        if (errors.length > 0) {
                          toast.error(
                            (props) => (
                              <CustomToast
                                {...props}
                                type="error"
                                message={errors.map((err, index) => (
                                  <div key={index}>{`${
                                    index + 1
                                  }. ${err}`}</div>
                                ))}
                              />
                            ),
                            { icon: false },
                          );
                          return;
                        }

                        setCurrentModalStep(2);
                      } else if (currentModalStep === 2) {
                        const otp = otpRef.current.value.trim();

                        if (!otp || otp.length !== 6) {
                          toast.error(
                            (props) => (
                              <CustomToast
                                {...props}
                                type="error"
                                message={"Enter a valid 6-digit OTP"}
                              />
                            ),
                            { icon: false },
                          );
                          return;
                        }

                        toast.success(
                          (props) => (
                            <CustomToast
                              {...props}
                              type="success"
                              message={"OTP verified!"}
                            />
                          ),
                          { icon: false },
                        );

                        closeParentModal();
                      }
                    }
                  }}
                >
                  <p
                    style={{
                      fontFamily: FONT_FAMILY_STACK,
                      fontWeight: 700,
                      color: activeTheme.buttonFont,
                      fontSize: "11px",
                    }}
                  >
                    {currentModalStep === 2 ? "Verify" : "Next"}
                  </p>
                </button>
              </div>
            </div>
          </div>
        )}
        {isDigiLockerLoading && (
          <div
            style={{
              position: "absolute",
              top: 0,
              left: 0,
              width: "100%",
              height: "100%",
              backgroundColor: "rgba(255, 255, 255, 0.8)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 9999,
            }}
          >
            <div className="loader"></div>
          </div>
        )}
        {showConfirmationModal && (
          <div
            style={{
              position: "fixed",
              top: 0,
              left: 0,
              width: "100%",
              height: "100%",
              backgroundColor: "rgba(0,0,0,0.4)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              zIndex: 1000,
            }}
            onClick={handleConfirmationModalClose}
          >
            <div
              onClick={(e) => e.stopPropagation()}
              style={{
                position: "relative", // IMPORTANT for positioning close icon
                backgroundColor: "#fff",
                padding: "20px",
                borderRadius: "10px",
                width: "400px",
                maxWidth: "90%",
                boxShadow: "0 4px 12px rgba(0,0,0,0.2)",
              }}
            >
              <FaTimes
                size={ICON_SIZE}
                onClick={handleConfirmationModalClose}
                style={{
                  position: "absolute",
                  top: "12px",
                  right: "12px",
                  cursor: "pointer",
                  color: "#666",
                }}
              />

              {/* Heading */}
              <p
                style={{
                  fontSize: "11px",
                  fontWeight: "600",
                  color: "#1a1a1a",
                  margin: 0,
                }}
              >
                Are you sure you would like to revoke consent?
              </p>

              <br />

              {/* Description */}
              <p
                style={{
                  fontSize: "11px",
                  color: "#555",
                  margin: 0,
                }}
              >
                You might not be able to continue using some/all of{" "}
                {businessName} features.
              </p>

              {/* Button */}
              <div
                style={{
                  marginTop: "20px",
                  display: "flex",
                  justifyContent: "flex-end",
                }}
              >
                <button
                  onClick={withdrawConsent}
                  style={{
                    backgroundColor: "#fee6ea",
                    color: "black",
                    border: "none",
                    padding: "7px 16px",
                    borderRadius: "250px",
                    cursor: "pointer",
                  }}
                >
                  Withdraw consent
                </button>
              </div>
            </div>
          </div>
        )}
        {withdrawLoader && (
          <div className="withdraw-overlay">
            <p className="blinking-text">
              Withdrawing your consent<span className="dots"></span>
            </p>
          </div>
        )}
        {translationLoader && (
          <div className="withdraw-overlay">
            <p className="blinking-text">
              Translating<span className="dots"></span>
            </p>
          </div>
        )}
        {pageLoader && (
          <div className="withdraw-overlay">
            <p className="blinking-text">
              Loading Pop Up<span className="dots"></span>
            </p>
          </div>
        )}
        {updateLoader && (
          <div className="withdraw-overlay">
            <p className="blinking-text">
              Updating your consent<span className="dots"></span>
            </p>
          </div>
        )}
        {renewLoader && (
          <div className="withdraw-overlay">
            <p className="blinking-text">
              Renewing your consent<span className="dots"></span>
            </p>
          </div>
        )}
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
    </main>
  );
};

export default FiduciaryRequests;
