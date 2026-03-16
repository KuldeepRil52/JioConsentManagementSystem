import {
  ActionButton,
  BadgeV2,
  Icon,
  Modal,
  SearchBox,
  Spinner,
  Text,
} from "@jds/core";
import "../Styles/toast.css";
import SessionExpiredModal from "./SessionExpiredModal";
import { saveAs } from "file-saver";
import { Slide, ToastContainer, toast } from "react-toastify";
import * as XLSX from "xlsx-js-style";
import CustomToast from "./CustomToastContainer";
import "../Styles/fiduciaryRequest.css";
import {
  IcBack,
  IcChevronDown,
  IcChevronUp,
  IcClose,
  IcDownload,
  IcEditPen,
  IcFilter,
  IcNext,
  IcSearch,
  IcVisible,
} from "@jds/core-icons";
import { useEffect, useRef, useState, useMemo } from "react";
import { IcArrowDown, IcLanguage, IcNetwork } from "@jds/extended-icons";
import "../Styles/configurePage.css";
import {
  getConsentHandleByHandleID,
  getConsentsByTemplateId,
  searchConsentsByConsentId,
  searchConsentHandleByCustomerId,
  fetchDocumentById,
  withdrawConsentByConsentId,
  updatePendingConsentByConsentId,
  translateData,
  captureAllConsent2,
  getDigilockerDetails,
  generateDigilockerTransactionId,
  sendParentalConsentRequest,
} from "../store/actions/CommonAction";
import { useDispatch, useSelector } from "react-redux";
import React from "react";
import FilterPanel from "./FilterPanel";
import useTranslation, {
  getApiValueFromLanguage,
} from "../hooks/useTranslation";

const SystemRequests = () => {
  const [currentModalStep, setCurrentModalStep] = useState(1);
  const [showParentModal, setShowParentModal] = useState(false);
  const [openRelationPopUp, setOpenRelationPopUp] = useState(false);
  const [guardianType, setGuardianType] = useState("PARENT");
  const [otherText, setOtherText] = useState("");
  const [selectedKYCMethod, setSelectedKYCMethod] = useState("");
  const [isOpenParent, setIsOpenParent] = useState(false);
  const [isParentChecked, setIsParentChecked] = useState(false);
  const [parentIdentity, setParentIdentity] = useState("");
  const [isDigiLockerLoading, setIsDigiLockerLoading] = useState(false);
  const [payloadForCaptureAll, setPayloadForCaptureAll] = useState({});
  const [sessionExpired, setSessionExpired] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);

  const rowsPerPage = 3;
  var totalPages;
  var startIdx;
  var endIdx;
  var paginatedConsents;

  const parentNameRef = useRef();
  const parentIdentityRef = useRef();
  const otherTextRef = useRef();
  const otpRef = useRef(null);
  const digiLockerTabRef = useRef(null);
  const [showAcceptWllModal, setShowAcceptAllModal] = useState(false);
  const [showUpdateModal, setShowUpdateModal] = useState(false);
  const [showConfirmationModal, setConfirmationModal] = useState(false);
  const [modalHeading, setModalHeading] = useState("Manage Consent");
  const [parentsName, setParentsName] = useState("Parent's Name");
  const [parentsIdentity, setParentsIdentity] = useState(
    "Identity (Email or Mobile",
  );

  const [relationshipModalHeader, setRelationshipModalHeader] = useState(
    "Select your relationship with the child",
  );
  const [parentModalHeader, setParentModalHeader] = useState(
    "Select verification method (Required)",
  );
  const [parentModalCancelBtnText, setParentModalCancelBtnText] =
    useState("Cancel");
  const [parentModalNextBtnText, setParentModalNextBtnText] = useState("Next");
  const [relationModalClearBtnText, setRelationModalClearBtnText] =
    useState("Clear");
  const [relationModalSaveBtnText, setRelationModalSaveBtnText] =
    useState("Save");
  const [relationParent, setRelationParent] = useState("Parent");
  const [relationLegalGuardian, setRelationGuardian] =
    useState("Legal Gurdian");
  const [relationOther, setRelationOther] = useState("Other");

  const [activeBadge, setActiveBadge] = useState("Always Active");
  const [ageSentence, setAgeSentence] = useState("I am below 18 years of age");
  const [searchText, setSearchText] = useState("");
  const [otherAccordionHeader, setOtherAccordionHeader] = useState("Others");
  const [withdrawConsentBtnText, setWithdrawConsentBtnText] =
    useState("Save my choices");
  const [updateConsentBtnText, setUpdateConsentBtnText] = useState("Allow all");
  const [customerIdLabel, setCustomerIdLabel] = useState("Customer ID");
  const [topDescription, setTopDescription] = useState("");
  const [rightsDescription, setRightsDescription] = useState("");
  const [permissionDescription, setPermissionDescription] = useState("");
  const [requiredText, setRequiredText] = useState("");
  const [prefereneceArray, setPreferenceArray] = useState([]);
  const [selectedConsentId, setSelectedConsentId] = useState("");
  const [selectedLanguagePreferences, setSelectedLanguagePreferences] =
    useState("ENGLISH");
  const [pendingTranslationLang, setPendingTranslationLang] = useState(null);

  const [pageLoader, setPageLoader] = useState(false);
  const currentPageLoadRef = useRef(0);
  const currentLangRef = useRef("en");
  const [selectedPrefereneces, setSelectedPreferences] = useState({});
  const [selectedConsentStatus, setSelectedConsentStatus] = useState("");
  const [showREquiredDetails, setShowRequiredDetails] = useState(false);
  const [showOhersDetails, setShowOthersDetails] = useState(false);
  const [showLoader, setShowLoader] = useState(false);
  const [lightTheme, setLightTheme] = useState({});
  const [parentalControl, setParentalControl] = useState(false);
  const [dataItemToBeShown, setDataItemToBeShown] = useState(false);
  const [validitytoBeShown, setValiditytoBeShown] = useState(false);
  const [translationLoader, setTranslationLoader] = useState(false);
  const [withdrawLoader, setWithdrawLoader] = useState(false);
  const [digiLoader, setDigiLoader] = useState(false);
  const [updateLoader, setUpdateLoader] = useState(false);
  const modalRef = useRef(null);
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
  const [tableData, setTableData] = useState([]);
  const [currentLangCode, setCurrentLangCode] = useState("en");
  const customerId = useSelector((state) => state.common.customer_id);
  const tntId = useSelector((state) => state.common.tenant_id);
  var cusId;
  var tenantId;
  cusId = customerId;
  tenantId = tntId;
  const [allSelected, setAllSelected] = useState(false);

  // Translation hook for header language dropdown integration
  const translationInputs = useMemo(
    () => [
      { id: "sys_pending_requests", source: "Pending requests" },
      { id: "sys_consent_badge", source: "Consent" },
      {
        id: "sys_description",
        source:
          "View and respond to new or updated consent requests awaiting your action.",
      },
      { id: "sys_request_id", source: "Request ID" },
      { id: "sys_request_date", source: "Request date" },
      { id: "sys_request_type", source: "Request type" },
      { id: "sys_status", source: "Status" },
      { id: "sys_actions", source: "Actions" },
      { id: "sys_renewal_request", source: "Renewal request" },
      { id: "sys_status_pending", source: "PENDING" },
      { id: "sys_status_active", source: "ACTIVE" },
      { id: "sys_status_withdrawn", source: "WITHDRAWN" },
      { id: "sys_status_expired", source: "EXPIRED" },
      { id: "sys_no_data", source: "No Data to Display" },
      {
        id: "sys_search_placeholder",
        source: "Search data item, type and purpose",
      },
      { id: "sys_page", source: "Page" },
      { id: "sys_of", source: "of" },
      // Expanded row headers
      { id: "sys_purpose_name", source: "Purpose Name" },
      { id: "sys_data_type", source: "Data type" },
      { id: "sys_data_item", source: "Data Item" },
      { id: "sys_data_used_by", source: "Data used by" },
    ],
    [],
  );

  const sendConsentRequest = async () => {
    try {
      const identityValue = parentIdentityRef.current.value.trim();

      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      const emailMobileVal = emailRegex.test(identityValue)
        ? "EMAIL"
        : "MOBILE";
      let successMessage = emailRegex.test(identityValue)
        ? "A consent request has been sent to your parent's email. Please ask them to approve it."
        : "A consent request has been sent to your parent's mobile. Please ask them to approve it.";

      const res = await dispatch(
        sendParentalConsentRequest(
          parentNameRef.current.value,
          identityValue,
          emailMobileVal,
          selectedConsentId,
        ),
      );

      if (res?.status === 200) {
        toast.success(
          (props) => (
            <CustomToast {...props} type="success" message={successMessage} />
          ),
          { icon: false },
        );

        setShowUpdateModal(false);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message="Failed to send parental consent request."
            />
          ),
          { icon: false },
        );
      }
    } catch (error) {
      console.log("Error while sending consent:", error);

      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message="Something went wrong. Please try again."
          />
        ),
        { icon: false },
      );
    }
  };

  const handleSave = () => {
    if (!guardianType) {
      alert("Please select a relationship");
      return;
    }

    if (guardianType === "OTHER" && !otherTextRef.current.value.trim()) {
      alert("Please specify the relationship");
      return;
    }

    // ✅ Build relationship string
    let relationship = "";

    switch (guardianType) {
      case "PARENT":
        relationship = "Parent";
        break;
      case "LEGAL_GUARDIAN":
        relationship = "Legal Guardian";
        break;
      case "OTHER":
        relationship = otherTextRef.current.value.trim();
        break;
      default:
        break;
    }

    const responseType = "code";
    const codeChallengeMethod = "S256";

    //  OPEN BLANK TAB IMMEDIATELY (user gesture)
    const newTab = window.open("", "_blank");

    if (!newTab) {
      alert("Popup blocked. Please allow popups for this site.");
      return;
    }

    digiLockerTabRef.current = newTab;

    // Close popup / show loader if you have one
    setOpenRelationPopUp(false);
    // setIsDigiLockerLoading(true); // optional

    //  Async flow AFTER tab is opened
    (async () => {
      try {
        const consentMetaId = await generateConsentMetaId(relationship);
        if (!consentMetaId) throw new Error("ConsentMetaId missing");

        const state = consentMetaId;
        const codeChallenge = await generateCodeChallenge(
          digilockerCodeVerifier,
        );

        const digilockerAuthUrl =
          `https://api.digitallocker.gov.in/public/oauth2/1/authorize` +
          `?response_type=${responseType}` +
          `&client_id=${digilockerClientId}` +
          `&redirect_uri=${digilockerRedirectUri}` +
          `&code_challenge=${codeChallenge}` +
          `&code_challenge_method=${codeChallengeMethod}` +
          `&state=${state}`;

        //  Redirect the already-opened tab
        digiLockerTabRef.current.location.href = digilockerAuthUrl;
      } catch (err) {
        console.error(err);
        digiLockerTabRef.current?.close();
        // setIsDigiLockerLoading(false);
      }
    })();
  };

  const handleClear = () => {
    setGuardianType("PARENT");
    setOtherText("");
  };
  // Helper to translate dynamic status values
  const getTranslatedStatus = (status) => {
    const statusMap = {
      PENDING: "sys_status_pending",
      ACTIVE: "sys_status_active",
      WITHDRAWN: "sys_status_withdrawn",
      EXPIRED: "sys_status_expired",
    };
    const translationId = statusMap[status?.toUpperCase()];
    return translationId ? getTranslation(translationId, status) : status;
  };

  // State for dynamic translations
  const [dynamicTranslations, setDynamicTranslations] = useState({});

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
          if (purpose.purposeInfo?.purposeName)
            uniqueTexts.add(purpose.purposeInfo.purposeName);
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

  // Sync with header language dropdown
  useEffect(() => {
    if (currentLanguage && currentLanguage !== "ENGLISH") {
      const targetLang = getApiValueFromLanguage(currentLanguage);
      if (targetLang !== currentLangCode) {
        setCurrentLangCode(targetLang);
        translateContent(translationInputs, currentLanguage);
      }
    } else if (currentLanguage === "ENGLISH" && currentLangCode !== "en") {
      setCurrentLangCode("en");
    }
  }, [currentLanguage, currentLangCode, translationInputs, translateContent]);

  const [consentsByTempId, setConsentsByTempId] = useState([]);
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
  const [formData, setFormData] = useState({});
  const [formOtherData, setformOtherData] = useState({});
  const [digilockerClientId, setDigilockerClientId] = useState("");
  const [digilockerClientSecret, setDigilockerClientSecret] = useState("");
  const [digilockerCodeVerifier, setDigilockerCodeVerifier] = useState("");
  const [digilockerRedirectUri, setDigilockerRedirectUri] = useState("");
  const [darkModeToBeShown, setDarkModeToBeShown] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);
  const activeTheme = isDarkMode ? darkTheme : lightTheme;

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

  const generateConsentMetaId = async (relationship) => {
    let finalPreferences;
    try {
      if (allSelected) {
        const filteredPreferences = Object.fromEntries(
          Object.entries(selectedPrefereneces).map(([key, _]) => [
            key,
            "ACCEPTED",
          ]),
        );
        finalPreferences = filteredPreferences;
      } else {
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
                message={"Please select at least one Preference."}
              />
            ),
            { icon: false },
          );
        } else {
          finalPreferences = filteredPreferences;
        }
      }
      let response = await dispatch(
        generateDigilockerTransactionId(
          selectedConsentId,
          selectedLanguagePreferences,
          finalPreferences,
          relationship,
        ),
      );
      return response?.data?.consentMetaId;
    } catch (error) {
      console.log("Erroro in generating consentMetaId", error);
      throw error;
    }
  };
  async function generateCodeChallenge(codeVerifier) {
    if (window.crypto && window.crypto.subtle) {
      const encoder = new TextEncoder();
      const data = encoder.encode(codeVerifier);
      const hashBuffer = await window.crypto.subtle.digest("SHA-256", data);
      const base64Hash = btoa(
        String.fromCharCode(...new Uint8Array(hashBuffer)),
      )
        .replace(/\+/g, "-")
        .replace(/\//g, "_")
        .replace(/=+$/, "");
      return base64Hash;
    } else {
      console.warn("crypto.subtle not available. Using fallback SHA-256.");

      function sha256(ascii) {
        const rightRotate = (value, amount) =>
          (value >>> amount) | (value << (32 - amount));

        let mathPow = Math.pow;
        let maxWord = mathPow(2, 32);
        let lengthProperty = "length";
        let i, j;
        let result = "";

        let words = [];
        let asciiBitLength = ascii[lengthProperty] * 8;

        let hash = (sha256.h = sha256.h || []);
        let k = (sha256.k = sha256.k || []);
        let primeCounter = k[lengthProperty];
        let isComposite = {};

        const fractionalPart = (n) => n - Math.floor(n);

        function getPrime(n) {
          let num = 2;
          while (n > 0) {
            if (!isComposite[num]) {
              for (let i = num * num; i < 312; i += num) {
                isComposite[i] = true;
              }
              if (--n === 0) return num;
            }
            num++;
          }
        }

        if (!primeCounter) {
          let n = 0;
          let prime = 2;
          while (n < 64) {
            while (isComposite[prime]) prime++;
            hash[n] = (fractionalPart(Math.pow(prime, 1 / 2)) * maxWord) | 0;
            k[n++] = (fractionalPart(Math.pow(prime, 1 / 3)) * maxWord) | 0;
            for (i = prime * prime; i < 312; i += prime) {
              isComposite[i] = true;
            }
            prime++;
          }
        }

        ascii += "\x80"; // append '1' bit (plus zero padding)
        while ((ascii[lengthProperty] % 64) - 56) ascii += "\x00"; // pad until length ≡ 56 mod 64
        for (i = 0; i < ascii[lengthProperty]; i++) {
          j = ascii.charCodeAt(i);
          if (j >> 8) return; // ASCII check
          words[i >> 2] |= j << (((3 - i) % 4) * 8);
        }
        words[words[lengthProperty]] = (asciiBitLength / maxWord) | 0;
        words[words[lengthProperty]] = asciiBitLength;

        for (j = 0; j < words[lengthProperty]; ) {
          const w = words.slice(j, (j += 16));
          const oldHash = hash.slice(0);

          for (i = 16; i < 64; i++) {
            const s0 =
              rightRotate(w[i - 15], 7) ^
              rightRotate(w[i - 15], 18) ^
              (w[i - 15] >>> 3);
            const s1 =
              rightRotate(w[i - 2], 17) ^
              rightRotate(w[i - 2], 19) ^
              (w[i - 2] >>> 10);
            w[i] = (w[i - 16] + s0 + w[i - 7] + s1) | 0;
          }

          let a = hash[0],
            b = hash[1],
            c = hash[2],
            d = hash[3],
            e = hash[4],
            f = hash[5],
            g = hash[6],
            h = hash[7];

          for (i = 0; i < 64; i++) {
            const S1 =
              rightRotate(e, 6) ^ rightRotate(e, 11) ^ rightRotate(e, 25);
            const ch = (e & f) ^ (~e & g);
            const temp1 = (h + S1 + ch + k[i] + w[i]) | 0;
            const S0 =
              rightRotate(a, 2) ^ rightRotate(a, 13) ^ rightRotate(a, 22);
            const maj = (a & b) ^ (a & c) ^ (b & c);
            const temp2 = (S0 + maj) | 0;

            h = g;
            g = f;
            f = e;
            e = (d + temp1) | 0;
            d = c;
            c = b;
            b = a;
            a = (temp1 + temp2) | 0;
          }

          for (i = 0; i < 8; i++)
            hash[i] = (hash[i] + [a, b, c, d, e, f, g, h][i]) | 0;
        }

        for (i = 0; i < 8; i++) {
          for (j = 3; j + 1; j--) {
            const b = (hash[i] >> (j * 8)) & 255;
            result += (b < 16 ? "0" : "") + b.toString(16);
          }
        }
        return result;
      }

      function hexToBase64Url(hex) {
        let bytes = [];
        for (let c = 0; c < hex.length; c += 2) {
          bytes.push(parseInt(hex.substr(c, 2), 16));
        }
        let base64 = btoa(String.fromCharCode(...bytes));
        return base64
          .replace(/\+/g, "-")
          .replace(/\//g, "_")
          .replace(/=+$/, "");
      }

      const hashHex = sha256(codeVerifier);
      return hexToBase64Url(hashHex);
    }
  }

  const downloadPendingRequestExcel = (consentsByTempId, listOfObjects) => {
    try {
      const wb = XLSX.utils.book_new();
      const ws_data = [];

      consentsByTempId.forEach((consent) => {
        // --- Parent Header ---
        ws_data.push([
          "Request ID",
          "Request Date",
          // "Request Expiry",
          "Request Type",
          "Status",
          "Action",
        ]);

        // --- Parent Row ---
        ws_data.push([
          consent.consentHandleId || "",
          formatToIST(consent.createdAt)
            ? new Date(formatToIST(consent.createdAt)).toLocaleDateString(
                "en-GB",
              )
            : "",
          // "-",
          "Renewal Request",
          consent.status || "",
          "",
        ]);

        ws_data.push([]); // spacing

        // --- Child Header ---
        ws_data.push([
          "Purpose Name",
          "Data Type",
          "Data Item",
          "Data Used By",
          // "Preference Status",
        ]);

        // --- Child Rows ---
        const matchedConsent = listOfObjects.find(
          (obj) => obj.consentHandleId === consent.consentHandleId,
        );

        if (matchedConsent && Array.isArray(matchedConsent.preferences)) {
          matchedConsent.preferences.forEach((pref) => {
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
                // pref.preferenceStatus || "",
              ]);
            });
          });
        } else {
          ws_data.push(["No detailed preferences available", "", "", "", ""]);
        }

        ws_data.push([]);
      });

      const ws = XLSX.utils.aoa_to_sheet(ws_data);

      ws["!cols"] = [
        { wch: 30 },
        { wch: 20 },
        { wch: 20 },
        { wch: 25 },
        { wch: 15 },
        { wch: 15 },
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

        for (let C = 0; C < (row?.length || 0); ++C) {
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

        if (row[0] === "Request ID") {
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

      XLSX.utils.book_append_sheet(wb, ws, "Request Logs");

      const wbout = XLSX.write(wb, { bookType: "xlsx", type: "array" });
      const blob = new Blob([wbout], { type: "application/octet-stream" });

      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "Request_Report.xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    } catch (err) {
      console.log("Excel download failed:", err);
    }
  };

  function closeParentModal() {
    setShowParentModal(false);
    setCurrentModalStep(1);
    setIsParentChecked(false);
    setSelectedKYCMethod(null);
    if (parentNameRef.current) parentNameRef.current.value = "";
    if (parentIdentityRef.current) parentIdentityRef.current.value = "";
    if (otpRef.current) otpRef.current.value = "";
  }
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

    if (currentLangRef.current === targetLang) {
      setPageLoader(false);
      setTranslationLoader(false);
      if (modalRef.current === "edit") {
        setShowUpdateModal(true);
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
      { id: "badge", source: activeBadge },
      { id: "requiredHeader", source: requiredText },
      { id: "othersHeader", source: otherAccordionHeader },
      { id: "customerId", source: customerIdLabel },
      { id: "modalHeading", source: modalHeading },
      { id: "documentTitle", source: documentName },
      { id: "parentName", source: parentsName },
      { id: "parentIdentity", source: parentsIdentity },
      { id: "relationshipModalHeader", source: relationshipModalHeader },
      { id: "relationParent", source: relationParent },
      { id: "relationLegalGuardian", source: relationLegalGuardian },
      { id: "relationOther", source: relationOther },
      { id: "parentModalHeader", source: parentModalHeader },

      { id: "parentalModalCancelBtn", source: parentModalCancelBtnText },
      { id: "parentalModalNextBtn", source: parentModalNextBtnText },
      { id: "relationModalClearBtn", source: relationModalClearBtnText },
      { id: "relationModalSaveBtn", source: relationModalSaveBtnText },

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
    // setCurrentLangCode(targetLang);
    currentLangRef.current = targetLang;

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

    const pName =
      data2.output.find((o) => o.id === "parentName")?.target ||
      "Parent's Name";
    setParentsName(pName);
    const pIdentity =
      data2.output.find((o) => o.id === "parentIdentity")?.target || "Identity";
    setParentsIdentity(pIdentity);
    const radioParent =
      data2.output.find((o) => o.id === "relationParent")?.target || "Parent";
    setRelationParent(radioParent);
    const radioLegalGurdian =
      data2.output.find((o) => o.id === "relationLegalGuardian")?.target ||
      "Legal Gurdian";
    setRelationGuardian(radioLegalGurdian);

    const relationModalHeader =
      data2.output.find((o) => o.id === "relationshipModalHeader")?.target ||
      "Select your relationship with the child";
    setRelationshipModalHeader(relationModalHeader);

    const parentModalHeader2 =
      data2.output.find((o) => o.id === "parentModalHeader")?.target ||
      "Select Verification Method";
    setParentModalHeader(parentModalHeader2);

    const pModalClBtn =
      data2.output.find((o) => o.id === "parentalModalCancelBtn")?.target ||
      "Cancel";
    setParentModalCancelBtnText(pModalClBtn);
    const pModalNxtBtn =
      data2.output.find((o) => o.id === "parentalModalNextBtn")?.target ||
      "Next";
    setParentModalNextBtnText(pModalNxtBtn);
    const relModalClrBtn =
      data2.output.find((o) => o.id === "relationModalClearBtn")?.target ||
      "Clear";
    setRelationModalClearBtnText(relModalClrBtn);

    const relModalSaveBtn =
      data2.output.find((o) => o.id === "relationModalSaveBtn")?.target ||
      "Save";
    setRelationModalSaveBtnText(relModalSaveBtn);
    const relOtherTxt =
      data2.output.find((o) => o.id === "relationOther")?.target || "Others";
    setRelationOther(relOtherTxt);

    const docName =
      data2.output.find((o) => o.id === "documentTitle")?.target ||
      "topDescription";
    setDocumentName(docName);

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
    if (modalRef.current === "edit") {
      setShowUpdateModal(true);
    }
  };

  const handleSelectAll = (isChecked) => {
    setAllSelected(isChecked);

    setSelectedPreferences((prev) => {
      const mandatoryEntries = Object.fromEntries(
        Object.entries(prev).filter(([_, value]) => value === "ACCEPTED"),
      );

      const updatedOthers = {};
      formOtherData.forEach((item) => {
        updatedOthers[item.id] = isChecked ? "ACCEPTED" : "NOT ACCEPTED";
      });

      return { ...mandatoryEntries, ...updatedOthers };
    });
  };
  const captureConsent = async () => {
    try {
      if (isParentChecked === true) {
        setShowParentModal(true);
        // setOpenRelationPopUp(true);
      } else {
        setUpdateLoader(true);
        let status = "ACTIVE";

        // Filter only ACCEPTED preferences
        const filteredPreferences = Object.fromEntries(
          Object.entries(selectedPrefereneces).filter(
            ([_, value]) => value === "ACCEPTED",
          ),
        );

        // Check if no preferences are selected
        if (Object.keys(filteredPreferences).length === 0) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Please select at least one Preference."}
              />
            ),
            { icon: false },
          );
          setUpdateLoader(false);
          return;
        }

        let res = await dispatch(
          captureAllConsent2(
            selectedConsentId,
            selectedLang,
            filteredPreferences,
            status,
          ),
        );

        if (res?.status === 201) {
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
      setWithdrawLoader(false);
      setUpdateLoader(false);
      return;
    }
  };

  const captureAllConsents = async () => {
    try {
      setAllSelected(true);
      if (isParentChecked === true) {
        setShowParentModal(true);
      } else {
        setUpdateLoader(true);

        let status = "ACTIVE";

        // Map all selected preferences to ACCEPTED
        const filteredPreferences = Object.fromEntries(
          Object.entries(selectedPrefereneces).map(([key, _]) => [
            key,
            "ACCEPTED",
          ]),
        );

        //  Check if filteredPreferences is empty
        if (Object.keys(filteredPreferences).length === 0) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Please select at least one Preference."}
              />
            ),
            { icon: false },
          );
          setUpdateLoader(false);
          return;
        }

        let res = await dispatch(
          captureAllConsent2(
            selectedConsentId,
            selectedLang,
            filteredPreferences,
            status,
          ),
        );

        if (res?.status === 201) {
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

          setShowUpdateModal(false);
          setConfirmationModal(false);
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
        }

        setUpdateLoader(false);
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
    }
  };

  const handleToggle = (rowId) => {
    setExpandedRow(expandedRow === rowId ? null : rowId);
  };
  const handleUpdateConsent = () => {
    updateConsent();
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

  const handleLanguageChange = (e) => {
    const lang = e.target.value;
    setSelectedLang(lang);
    callBhashiniAPI(lang);
  };
  const closeEditConsentModal = () => {
    setShowUpdateModal(false);
    setAllSelected(false);
    setDarkModeToBeShown(false);
    setIsDarkMode(false);
    setLightTheme({});
    setDataTypeLabel("Data type");
    setSelectedLang("ENGLISH");
    setCustomerIdLabel("Customer ID");
    setActiveBadge("Always Active");
    setOtherAccordionHeader("Others");
    setPurposeLabel("");
    setPurposeLabel("Purpose");
    setDurationLabel("Duration");
    setUsedByLabel("Used by");
    setDataItemLabel("Data item");
    setProcessingActivityLabel("Processing activity");
    setWithdrawConsentBtnText("Save my choices");
    setUpdateConsentBtnText("Allow all");
    setModalHeading("Manage Consent");
    setDocumentName("");
    setAgeSentence("I am below 18 years of age");
  };

  const transformConsentData = (apiResponse) => {
    const data = Array.isArray(apiResponse) ? apiResponse[0] : apiResponse;
    if (!data || !data.preferences) return { mandatory: [], others: [] };

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

      // const processingActivity = pref.processorActivityList.map(
      //   (p) => p.processActivityInfo.activityName
      // );
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
      if (processorActivities.length === 0) {
        throw new Error("Invalid processor activities");
      }

      const id = pref.preferenceId;
      const status = pref.preferenceStatus;

      const formattedObj = {
        purpose,
        duration,
        usedBy,
        dataItems,
        processorActivities,
        id,
        status,
      };

      if (pref.mandatory) {
        preferencesStatus[id] = "ACCEPTED";
      } else {
        preferencesStatus[id] = "NOTACCEPTED";
      }

      if (pref.mandatory === true) {
        mandatory.push(formattedObj);
      } else {
        others.push(formattedObj);
      }
    });

    return { mandatory, others, preferencesStatus };
  };

  const transformConsentData123 = (apiResponse) => {
    const data = Array.isArray(apiResponse) ? apiResponse[0] : apiResponse;
    if (!data || !data.preferences) return { mandatory: [], others: [] };

    const mandatory123 = [];
    const others123 = [];
    const preferencesStatus123 = {};

    data.preferences.forEach((pref) => {
      // Extract purpose info
      const purpose = pref.purposeList.map(
        (p) => `${p.purposeInfo.purposeName}`,
      );

      // Extract duration
      const duration = {
        value: pref.preferenceValidity?.value || "",
        unit: pref.preferenceValidity?.unit?.toLowerCase() || "",
      };

      // Extract processor names (usedBy)
      const usedBy = pref.processorActivityList.map(
        (p) => p.processActivityInfo.processorName,
      );

      // Extract all dataItems under all dataTypesList
      const dataItems = pref.processorActivityList.flatMap((p) =>
        p.processActivityInfo.dataTypesList.flatMap((d) => d.dataItems),
      );

      // const processingActivity = pref.processorActivityList.map(
      //   (p) => p.processActivityInfo.activityName
      // );
      const processorActivities = pref.processorActivityList.map((p) => ({
        activityName: p.processActivityInfo.activityName,
        processorName: p.processActivityInfo.processorName,
        dataItems: p.processActivityInfo.dataTypesList.flatMap(
          (d) => d.dataItems,
        ),
      }));

      const id = pref.preferenceId;
      const status = pref.preferenceStatus;

      const formattedObj = {
        purpose,
        duration,
        usedBy,
        dataItems,
        processorActivities,
        id,
        status,
      };
      preferencesStatus123[id] = status;

      // Separate based on preference type
      if (pref.mandatory === true) {
        mandatory123.push(formattedObj);
      } else {
        others123.push(formattedObj);
      }
    });

    return { mandatory123, others123, preferencesStatus123 };
  };
  const handleCallAPI2 = async (id, handleId, languagePreferences, status) => {
    setSelectedConsentId(handleId);
    setSelectedLanguagePreferences(languagePreferences);
    setSelectedConsentStatus(status);
    const response = await dispatch(getConsentHandleByHandleID(handleId));

    if (response?.multilingual?.supportedLanguages.length === 1) {
      setPendingTranslationLang(response?.multilingual?.supportedLanguages[0]);
      setSelectedLang(response?.multilingual?.supportedLanguages[0]);
    } else {
      setPendingTranslationLang("ENGLISH");
      setSelectedLang("ENGLISH");
    }
    currentPageLoadRef.current = 1;
    try {
      const { mandatory, others, preferencesStatus } =
        transformConsentData(response);

      var theme = response?.uiConfig?.theme;

      setFormData(mandatory);
      setformOtherData(others);
      setSelectedPreferences(preferencesStatus);
      setDocumentName(response?.documentMeta?.name);
      setDocumentId(response?.documentMeta?.documentId);
      setDarkModeToBeShown(response?.uiConfig?.darkMode);
      setLogoUrl(response?.uiConfig?.logo);
      setParentalControl(response?.uiConfig?.parentalControl);
      setDataItemToBeShown(response?.uiConfig?.dataItemToBeShown);
      setProcessActivityNameToBeShown(
        response?.uiConfig?.processActivityNameToBeShown,
      );
      setValiditytoBeShown(response?.uiConfig?.validitytoBeShown);
      if (theme) {
        var decodedThemeStr = atob(theme);

        var decodedTheme = JSON.parse(decodedThemeStr);

        setLightTheme(JSON.parse(decodedTheme.light));
        setDarkTheme(JSON.parse(decodedTheme.dark));
      }

      setTopDescription(
        response?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.description,
      );
      setRightsDescription(
        response?.multilingual?.languageSpecificContentMap?.ENGLISH?.rightsText,
      );
      setPermissionDescription(
        response?.multilingual?.languageSpecificContentMap?.ENGLISH
          ?.permissionText,
      );
      setRequiredText(
        response?.multilingual?.languageSpecificContentMap?.ENGLISH?.label,
      );
      modalRef.current = "edit";
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
      //setShowUpdateModal(false);
      modalRef.current = null;
      return;
    }
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
  var consentHandleIds = [];
  const [listOfObjects, setListOfObjects] = useState([]);

  // Translate dynamic table data when language or data changes
  useEffect(() => {
    if (
      currentLanguage &&
      currentLanguage !== "ENGLISH" &&
      listOfObjects &&
      listOfObjects.length > 0
    ) {
      const targetLang = getApiValueFromLanguage(currentLanguage);
      translateDynamicData(listOfObjects, targetLang);
    } else if (currentLanguage === "ENGLISH") {
      setDynamicTranslations({});
    }
  }, [currentLanguage, listOfObjects]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const searchList = await dispatch(searchConsentHandleByCustomerId());

        const pendingList = Array.isArray(searchList)
          ? searchList.filter((item) => item.status === "PENDING")
          : [];

        setConsentsByTempId(pendingList);

        const consentHandleIds = pendingList.map(
          (item) => item.consentHandleId,
        );

        const fetchedObjects = await Promise.all(
          consentHandleIds.map(async (id) => {
            const response = await dispatch(getConsentHandleByHandleID(id));
            return response;
          }),
        );

        setListOfObjects(fetchedObjects); // store in state
      } catch (err) {
        console.log("Error occurred in getConsentsByTemplateId", err);
        if (err?.[0]?.errorCode === "JCMP3001") {
          setConsentsByTempId([]);
        }
        if (err.code === "ERR_NETWORK") {
          setSessionExpired(true);
        }
      } finally {
        setLoadLogs(false);
      }
    };

    fetchData();
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

  const handlePreferenceChange = (id, isChecked) => {
    setSelectedPreferences((prev) => {
      const updated = {
        ...prev,
        [id]: isChecked ? "ACCEPTED" : "NOT ACCEPTED",
      };

      // Check if all others are now accepted
      const allAccepted = formOtherData.every(
        (item) => updated[item.id] === "ACCEPTED",
      );
      setAllSelected(allAccepted); // keep the Select All checkbox synced

      return updated;
    });
  };

  const handleConfirm = () => {
    setShowModal(false);
    setConfirmationModal(true);
  };
  const handleConfirmationModalClose = () => {
    setShowModal(true);
    setConfirmationModal(false);
  };

  const closeWithdrawConsentModal = () => {
    setShowModal(false);
  };

  const data = [
    {
      purpose: [
        "Account creation (collect data to create user account)",
        "Marketing",
      ],
      duration: { value: 1, unit: "year" },
      usedBy: ["Jio Platforms Limited", "Subsidiary ABC"],
      dataItems: ["Jio Platforms Limited", "Subsidiary ABC"],
    },
    {
      purpose: ["Profile Management"],
      duration: { value: 2, unit: "years" },
      usedBy: ["XYZ Ltd."],
      dataItems: ["Jio Platforms Limited", "Subsidiary ABC"],
    },
    {
      purpose: ["Profile Management"],
      duration: { value: 2, unit: "years" },
      usedBy: ["XYZ Ltd."],
      dataItems: ["Jio Platforms Limited", "Subsidiary ABC"],
    },
    {
      purpose: ["Profile Management"],
      duration: { value: 2, unit: "years" },
      usedBy: ["XYZ Ltd."],
      dataItems: ["Jio Platforms Limited", "Subsidiary ABC"],
    },
  ];
  const [showFilter, setShowFilter] = useState(false);

  const [filters, setFilters] = useState({
    search: "",
    status: [],
    startDate: "",
    endDate: "",
  });
  const applyFilters = (consents) => {
    return consents.filter((consent) => {
      // ----- 1. SEARCH FILTER -----
      if (filters.search?.trim()) {
        const text = filters.search.toLowerCase().trim().replace(/\s+/g, " ");

        const consentValues = [
          consent.consentId,
          consent.consentHandleId,
          consent.status,
          formatToIST(consent.createdAt),
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

      // ----- 2. CREATED DATE FILTER -----

      // Full day boundaries
      const start = filters.startDate
        ? new Date(filters.startDate + "T00:00:00")
        : null;

      const end = filters.endDate
        ? new Date(filters.endDate + "T23:59:59")
        : null;

      const created = new Date(consent.createdAt);

      if (start && created < start) return false;
      if (end && created > end) return false;

      return true;
    });
  };

  return (
    <main role="main" aria-label="Pending Requests Page">
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
      <div className="pageConfig">
        <header className="fiduciaryHeaderContainer" role="banner">
          <div className="fiduciaryHeader">
            <h1>
              <Text appearance="heading-s" color="primary-grey-100">
                {getTranslation("sys_pending_requests", "Pending requests")}
              </Text>
            </h1>
            <div
              className="badge"
              role="status"
              aria-label="Page type: Consent"
            >
              <Text appearance="body-xs-bold" color="primary-grey-80">
                {getTranslation("sys_consent_badge", "Consent")}
              </Text>
            </div>
          </div>
        </header>
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginTop: "1rem",
          }}
        >
          <div>
            <Text appearance="body-s" color="primary-grey-80">
              {getTranslation(
                "sys_description",
                "View and respond to new or updated consent requests awaiting your action.",
              )}
            </Text>
          </div>
          <div
            className="fiduciaryHeaderButtonContainer"
            style={{
              justifyContent: "flex-end",
              opacity: consentsByTempId.length === 0 ? 0.5 : 1,
              pointerEvents: consentsByTempId.length === 0 ? "none" : "auto",
            }}
          >
            <div className="search-input-wrapper" style={{ width: "100%" }}>
              <Icon ic={<IcSearch />} size="m" color="primary_grey_100" />
              <input
                type="text"
                className="search-input"
                placeholder={getTranslation(
                  "sys_search_placeholder",
                  "Search data item, type and purpose",
                )}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                aria-label={getTranslation(
                  "sys_search_placeholder",
                  "Search data item, type and purpose",
                )}
              />
            </div>
            <ActionButton
              kind="tertiary"
              ariaLabel="Open filter panel"
              onClick={() => setShowFilter(true)}
              icon={
                <Icon ic={<IcFilter />} size="m" color="primary_grey_80"></Icon>
              }
              size="medium"
            />
            <FilterPanel
              open={showFilter}
              showStatus={false}
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
            <ActionButton
              kind="tertiary"
              ariaLabel="downlod Report"
              icon={
                <Icon ic={<IcDownload />} size="m" color="primary_grey_80" />
              }
              onClick={() =>
                downloadPendingRequestExcel(consentsByTempId, listOfObjects)
              }
              size="medium"
            />
          </div>
        </div>
        <br></br>
        <div
          style={{ overflowX: "auto", paddingBottom: "1rem" }}
          aria-label="Consent Information Table."
        >
          <table
            className="consentLog-table"
            aria-label="details such as Request ID, Request Date, Status and Actions"
          >
            <colgroup>
              <col style={{ width: "3rem" }} />
              <col style={{ width: "25%" }} />
              <col style={{ width: "20%" }} />
              <col style={{ width: "20%" }} />
              <col style={{ width: "15%" }} />
              <col style={{ width: "19%" }} />
            </colgroup>
            <thead>
              <tr>
                <th></th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      {getTranslation("sys_request_id", "Request ID")}
                    </Text>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      {getTranslation("sys_request_date", "Request date")}
                    </Text>
                  </div>
                </th>
                {/* <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      Request expiry
                    </Text>
                  </div>
                </th> */}

                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      {getTranslation("sys_request_type", "Request type")}
                    </Text>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      {getTranslation("sys_status", "Status")}
                    </Text>
                  </div>
                </th>
                <th className="parentheader">
                  <div className="header-with-icon">
                    <Text appearance="body-xs-bold" color="primary-grey-80">
                      {getTranslation("sys_actions", "Actions")}
                    </Text>
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
                      {getTranslation("sys_no_data", "No Data to Display")}
                    </Text>
                  </td>
                </tr>
              ) : (
                (() => {
                  const dataAfterFilters = applyFilters(consentsByTempId || []);

                  const filteredConsents = dataAfterFilters.filter(
                    (consent) => {
                      // Normalize search text
                      const text = searchText
                        .toLowerCase()
                        .trim()
                        .replace(/\s+/g, " ");
                      if (!text) return true; // Show all if search is empty

                      const consentValues = [
                        consent.consentId,
                        consent.consentHandleId,
                        consent.status,
                        formatToIST(consent.createdAt),
                      ];

                      // Try to include preference info if available in listOfObjects
                      const matchedConsent = listOfObjects.find(
                        (obj) =>
                          obj.consentHandleId === consent.consentHandleId,
                      );
                      if (matchedConsent) {
                        matchedConsent.preferences?.forEach((pref) => {
                          pref.purposeList?.forEach((p) => {
                            consentValues.push(p.purposeInfo?.purposeName);
                          });

                          pref.processorActivityList?.forEach((pa) => {
                            consentValues.push(
                              pa.processActivityInfo?.processorName,
                            );
                            pa.processActivityInfo?.dataTypesList?.forEach(
                              (dt) => {
                                consentValues.push(dt.dataTypeName);
                                consentValues.push(...(dt.dataItems || []));
                              },
                            );
                          });
                        });
                      }

                      return consentValues.some((val) => {
                        if (typeof val !== "string") return false;
                        const normalizedVal = val
                          .toLowerCase()
                          .trim()
                          .replace(/\s+/g, " ");
                        return normalizedVal.includes(text);
                      });
                    },
                  );
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
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          No matching records found
                        </Text>
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
                        >
                          <td className="chevron-cell">
                            <div className="chevron-wrapper">
                              <Text
                                appearance="body-xs"
                                color="primary-grey-100"
                              >
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
                              {formatToIST(consent.createdAt)}
                            </Text>
                          </td>
                          {/* <td>
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              -
                            </Text>
                          </td> */}
                          <td>
                            <Text
                              appearance="body-xs-bold"
                              color="primary-grey-80"
                            >
                              {getTranslation(
                                "sys_renewal_request",
                                "Renewal request",
                              )}
                            </Text>
                          </td>
                          <td>
                            <div className="warningbadge">
                              <p className="warningbadge-text">
                                {getTranslatedStatus(consent.status)}
                              </p>
                            </div>
                          </td>
                          <td>
                            <div
                              style={{
                                display: "flex",
                                gap: "2rem",
                                alignItems: "center",
                              }}
                            >
                              <IcEditPen
                                height={20}
                                width={20}
                                style={{ cursor: "pointer" }}
                                onClick={() =>
                                  handleCallAPI2(
                                    consent.consentId,
                                    consent.consentHandleId,
                                    consent.languagePreferences,
                                    consent.status,
                                  )
                                }
                              />
                            </div>
                          </td>
                        </tr>

                        {/* Expanded row */}
                        {expandedRow === `row${index}` && (
                          <tr className="expanded-row">
                            <td></td>
                            <td colSpan="6">
                              <table className="inner-table">
                                <colgroup>
                                  <col style={{ width: "25%" }} />
                                  <col style={{ width: "25%" }} />
                                  <col style={{ width: "24%" }} />
                                  <col style={{ width: "20%" }} />
                                  {/* <col style={{ width: "15%" }} /> */}
                                </colgroup>

                                <thead>
                                  <tr>
                                    <th>
                                      <Text
                                        appearance="body-xs-bold"
                                        color="primary-grey-80"
                                      >
                                        {getTranslation(
                                          "sys_purpose_name",
                                          "Purpose Name",
                                        )}
                                      </Text>
                                    </th>
                                    <th>
                                      <Text
                                        appearance="body-xs-bold"
                                        color="primary-grey-80"
                                      >
                                        {getTranslation(
                                          "sys_data_type",
                                          "Data type",
                                        )}
                                      </Text>
                                    </th>
                                    <th>
                                      <Text
                                        appearance="body-xs-bold"
                                        color="primary-grey-80"
                                      >
                                        {getTranslation(
                                          "sys_data_item",
                                          "Data Item",
                                        )}
                                      </Text>
                                    </th>
                                    <th>
                                      <Text
                                        appearance="body-xs-bold"
                                        color="primary-grey-80"
                                      >
                                        {getTranslation(
                                          "sys_data_used_by",
                                          "Data used by",
                                        )}
                                      </Text>
                                    </th>
                                    {/* <th>
                                      <Text
                                        appearance="body-xs-bold"
                                        color="primary-grey-80"
                                      >
                                        Status
                                      </Text>
                                    </th> */}
                                  </tr>
                                </thead>

                                <tbody>
                                  {(() => {
                                    const matchedConsent = listOfObjects.find(
                                      (obj) =>
                                        obj.consentHandleId ===
                                        consent.consentHandleId,
                                    );

                                    if (!matchedConsent) return null;

                                    return matchedConsent.preferences.map(
                                      (pref) =>
                                        pref.purposeList.map((purpose) => (
                                          <tr key={purpose.purposeId}>
                                            <td>
                                              <Text
                                                appearance="body-xs-bold"
                                                color="primary-grey-80"
                                              >
                                                {getDynamicTranslation(
                                                  purpose?.purposeInfo
                                                    ?.purposeName,
                                                )}
                                              </Text>
                                            </td>
                                            <td>
                                              <Text
                                                appearance="body-xs-bold"
                                                color="primary-grey-80"
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
                                              </Text>
                                            </td>
                                            <td>
                                              <Text
                                                appearance="body-xs-bold"
                                                color="primary-grey-80"
                                              >
                                                {pref.processorActivityList
                                                  .flatMap((pa) =>
                                                    pa?.processActivityInfo?.dataTypesList.flatMap(
                                                      (dt) =>
                                                        dt.dataItems.map(
                                                          (item) =>
                                                            getDynamicTranslation(
                                                              item,
                                                            ),
                                                        ),
                                                    ),
                                                  )
                                                  .join(", ")}
                                              </Text>
                                            </td>
                                            <td>
                                              <Text
                                                appearance="body-xs-bold"
                                                color="primary-grey-80"
                                              >
                                                {pref.processorActivityList
                                                  .map((pa) =>
                                                    getDynamicTranslation(
                                                      pa?.processActivityInfo
                                                        ?.processorName,
                                                    ),
                                                  )
                                                  .join(", ")}
                                              </Text>
                                            </td>
                                            {/* <td>
                                              <span
                                                className={
                                                  pref.preferenceStatus ===
                                                  "ACCEPTED"
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
                                            </td> */}
                                          </tr>
                                        )),
                                    );
                                  })()}
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
              <ActionButton
                icon={<IcBack />}
                kind="secondary"
                onClick={() => setCurrentPage((p) => Math.max(p - 1, 1))}
                disabled={currentPage === 1}
              ></ActionButton>

              <span className="pagination-text">
                {getTranslation("sys_page", "Page")} {currentPage}{" "}
                {getTranslation("sys_of", "of")} {totalPages}
              </span>

              <ActionButton
                kind="secondary"
                icon={<IcNext />}
                onClick={() =>
                  setCurrentPage((p) => Math.min(p + 1, totalPages))
                }
                disabled={currentPage === totalPages}
              ></ActionButton>
            </div>
          )}
        </div>
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
                    {/* Icon positioned inside the select box */}
                    <div className="container-2-left-2-dropdown">
                      {/* Icon */}
                      <IcLanguage
                        height={20}
                        width={20}
                        className="container-2-left-2-dropdown-Icon"
                        style={{ color: `${activeTheme.cardFont}` }}
                      />

                      <select
                        className="container-2-left-2-dropdown-select"
                        value={selectedLang}
                        onChange={handleLanguageChange}
                        // disabled={selectedConsentStatus !== "ACTIVE"}
                        onClick={() => setIsOpen((prev) => !prev)} // toggle open/close
                        onBlur={() => setIsOpen(false)} // reset when focus lost
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
                        <IcChevronUp
                          height={20}
                          width={20}
                          className="container-2-left-2-dropdown-Icon-right"
                          style={{ color: `${activeTheme.cardFont}` }}
                        />
                      ) : (
                        <IcChevronDown
                          height={20}
                          width={20}
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
                      {/* Left: icon */}
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

                      {/* Middle: text */}
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
                      <div key={index} className="data-row-container">
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

                        {/*Checkbox Section */}
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
                        className="checkbox-new"
                        checked={isParentChecked}
                        onChange={(e) => {
                          const checked = e.target.checked;
                          setIsParentChecked(checked);
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
                  onClick={captureConsent}
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
                  onClick={captureAllConsents}
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
        {sessionExpired && (
          <SessionExpiredModal
            isOpen={sessionExpired}
            onClose={() => setSessionExpired(false)}
          />
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
        {openRelationPopUp && (
          <div className="modal-outer-container">
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
                  onClick={() => {
                    setOpenRelationPopUp(false);
                    setShowParentModal(true);
                  }}
                ></button>
              </div>
              <h1
                style={{
                  fontSize: "18px",
                  fontWeight: "700",
                  marginBottom: "20px",
                }}
              >
                {relationshipModalHeader}
              </h1>
              <div>
                <label style={{ display: "block", marginBottom: "12px" }}>
                  <input
                    type="radio"
                    name="guardianType"
                    value="PARENT"
                    checked={guardianType === "PARENT"}
                    onChange={() => setGuardianType("PARENT")}
                    style={{ marginRight: "8px" }}
                  />
                  {relationParent}
                </label>

                <label style={{ display: "block", marginBottom: "12px" }}>
                  <input
                    type="radio"
                    name="guardianType"
                    value="LEGAL_GUARDIAN"
                    checked={guardianType === "LEGAL_GUARDIAN"}
                    onChange={() => setGuardianType("LEGAL_GUARDIAN")}
                    style={{ marginRight: "8px" }}
                  />
                  {relationLegalGuardian}
                </label>

                <label style={{ display: "block", marginBottom: "8px" }}>
                  <input
                    type="radio"
                    name="guardianType"
                    value="OTHER"
                    checked={guardianType === "OTHER"}
                    onChange={() => setGuardianType("OTHER")}
                    style={{ marginRight: "8px" }}
                  />
                  {relationOther}
                </label>

                {/* Other Textbox */}
                {guardianType === "OTHER" && (
                  <div style={{ marginLeft: "24px", marginTop: "8px" }}>
                    <input
                      type="text"
                      placeholder="Please specify relationship"
                      ref={otherTextRef}
                      style={{
                        width: "90%",
                        padding: "6px 10px",
                        borderRadius: "6px",
                        border: "1px solid #ccc",
                        fontSize: "14px",
                      }}
                    />
                  </div>
                )}
              </div>
              <div
                style={{
                  display: "flex",
                  justifyContent: "flex-end",
                  gap: "10px",
                  marginTop: "24px",
                }}
              >
                <button
                  onClick={handleClear}
                  style={{
                    padding: "8px 18px",
                    borderRadius: "24px",
                    fontFamily: "system-ui",
                    fontWeight: 700,
                    color: activeTheme.buttonBackground,
                    fontSize: "18px",
                    backgroundColor: activeTheme.buttonFont,
                    border: "1px solid rgba(181, 181, 181, 1)",
                    cursor: "pointer",
                  }}
                >
                  {relationModalClearBtnText}
                </button>

                <button
                  onClick={handleSave}
                  style={{
                    padding: "8px 20px",
                    borderRadius: "24px",
                    backgroundColor: activeTheme.buttonBackground,
                    border: "1px solid rgba(181, 181, 181, 1)",
                    fontSize: "18px",
                    fontFamily: "system-ui",
                    fontWeight: 700,
                    color: activeTheme.buttonFont,

                    cursor: "pointer",
                  }}
                >
                  {relationModalSaveBtnText}
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
              }}
            >
              {selectedKYCMethod === "declaration" && (
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

                  {/* <p
                    className="step-text-parent"
                    style={{ fontWeight: 700, fontSize: "16px" }}
                  >
                    Step {currentModalStep}/2
                  </p> */}
                </div>
              )}

              {currentModalStep === 1 && (
                <>
                  <p className="parentTextStyle1">{parentModalHeader}</p>
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
                          fontSize: "14px",
                        }}
                      >
                        <option value="">Select Method</option>
                        {digilockerClientId && (
                          <option value="digilocker">
                            Proceed with KYC of the parent/legal guardian using
                            DigiLocker
                          </option>
                        )}

                        <option value="declaration">
                          Send the consent link to the parent/legal guardian
                          over SMS/email and obtain approval
                        </option>
                      </select>
                      {isOpenParent ? (
                        <IcChevronUp
                          height={20}
                          width={20}
                          style={{
                            position: "absolute",
                            right: "8px",
                            top: "50%",
                            transform: "translateY(-50%)",
                          }}
                        />
                      ) : (
                        <IcChevronDown
                          height={20}
                          width={20}
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
                      <p className="parentTextStyle1">{parentsName}</p>
                      <input className="input-parent" ref={parentNameRef} />

                      <p className="parentTextStyle1">{parentsIdentity}</p>
                      <input className="input-parent" ref={parentIdentityRef} />
                    </>
                  )}
                </>
              )}

              {/* {currentModalStep === 2 && (
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
              )} */}

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

                      setShowUpdateModal(true);
                    }}
                  >
                    <p
                      style={{
                        fontFamily: "system-ui",
                        fontWeight: 700,
                        color: activeTheme.buttonBackground,
                        fontSize: "18px",
                      }}
                    >
                      {parentModalCancelBtnText}
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
                        } else {
                          setShowParentModal(false);
                          sendConsentRequest();
                        }
                      }
                    }
                    if (selectedKYCMethod === "digilocker") {
                      const responseType = "code";
                      const codeChallengeMethod = "S256";

                      const newTab = window.open("", "_blank");

                      if (!newTab) {
                        alert(
                          "Popup blocked. Please allow popups for this site.",
                        );
                        return;
                      }

                      digiLockerTabRef.current = newTab;

                      // Close modals & show loader
                      setShowUpdateModal(false);
                      setShowParentModal(false);
                      setIsParentChecked(false);
                      setIsDigiLockerLoading(true);

                      // Now async work is SAFE
                      (async () => {
                        try {
                          const consentMetaId =
                            await generateConsentMetaId("parent");
                          if (!consentMetaId)
                            throw new Error("ConsentMetaId missing");

                          const state = consentMetaId;
                          const codeChallenge = await generateCodeChallenge(
                            digilockerCodeVerifier,
                          );

                          const digilockerAuthUrl =
                            `https://api.digitallocker.gov.in/public/oauth2/1/authorize` +
                            `?response_type=${responseType}` +
                            `&client_id=${digilockerClientId}` +
                            `&redirect_uri=${digilockerRedirectUri}` +
                            `&code_challenge=${codeChallenge}` +
                            `&code_challenge_method=${codeChallengeMethod}` +
                            `&state=${state}`;

                          //  Redirect already-opened tab
                          digiLockerTabRef.current.location.href =
                            digilockerAuthUrl;

                          //  Watch tab close
                          const checkInterval = setInterval(() => {
                            if (
                              !digiLockerTabRef.current ||
                              digiLockerTabRef.current.closed
                            ) {
                              clearInterval(checkInterval);
                              setIsDigiLockerLoading(false);
                            }
                          }, 1000);
                        } catch (err) {
                          console.error(err);
                          setIsDigiLockerLoading(false);
                          digiLockerTabRef.current?.close();
                        }
                      })();

                      return;
                    }
                  }}
                >
                  <p
                    style={{
                      fontFamily: "system-ui",
                      fontWeight: 700,
                      color: activeTheme.buttonFont,
                      fontSize: "18px",
                    }}
                  >
                    {parentModalNextBtnText}
                  </p>
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
        {digiLoader && (
          <div className="withdraw-overlay">
            <p className="blinking-text">
              Processing your consent<span className="dots"></span>
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
              Processing your consent<span className="dots"></span>
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
export default SystemRequests;
