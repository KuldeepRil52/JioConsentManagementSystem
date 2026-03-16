import React from "react";
import "./Icon.css";

// Import React Icons - Bootstrap Icons (using available filled versions)
import {
  BsHouseFill,
  BsBellFill,
  BsPersonFill,
  BsEnvelopeFill,
  BsTelephoneFill,
  BsBuildingFill,
  BsGearFill,
  BsFileTextFill,
  BsSearchHeart, // Using heart search as filled search
  BsArrowLeftCircleFill,
  BsArrowRightCircleFill,
  BsArrowUpCircleFill,
  BsArrowDownCircleFill,
  BsChevronLeft,
  BsChevronRight,
  BsChevronUp,
  BsChevronDown,
  BsPlusCircleFill,
  BsXCircleFill,
  BsCheckCircleFill,
  BsExclamationCircleFill,
  BsInfoCircleFill,
  BsTrashFill,
  BsPencilFill,
  BsEyeFill,
  BsEyeSlashFill,
  BsCloudDownloadFill,
  BsCloudUploadFill,
  BsFunnelFill,
  BsThreeDotsVertical,
  BsCalendarFill,
  BsClockFill,
  BsFileEarmarkTextFill,
  BsArrowRepeat,
  BsArrowClockwise,
  BsPaperclip,
  BsCalendarWeekFill,
  BsShieldFillCheck,
  BsQrCodeScan, // Using scan variant for filled look
  BsHeartFill,
  BsExclamationTriangleFill,
  BsSortDown,
  BsClipboardFill,
  BsToggle2On, // Using Toggle2On as filled toggle
  BsWhatsapp,
  BsCircleFill,
  BsAlarmFill,
  BsAlignBottom,
  BsFileEarmarkFill,
  BsKeyboardFill,
  BsGlobeCentralSouthAsia, // Using globe variant
  BsStickiesFill,
  BsMicFill,
  BsCreditCard2FrontFill,
  BsListCheck,
  BsXOctagonFill,
  BsPencilSquare,
  BsPeopleFill,
  BsChatLeftTextFill,
  BsDiagram3Fill,
  BsWrench, // No fill version available
  BsSliders, // No fill version available
  BsGridFill,
  BsDashCircleFill,
  BsDatabaseFill,
  BsDiagram2Fill,
  BsJournalText, // No fill version available
  BsArrowsVertical,
  BsBellSlashFill,
  BsTranslate,
  BsCookie, // No fill version available - using regular
} from "react-icons/bs";

/**
 * Custom Icon Component - Replaces JDS icons with React Icons (Bootstrap Icons)
 * 
 * Usage examples:
 * <Icon ic={IcHome} size="medium" />
 * <Icon ic="IcHome" size="medium" />
 * <IcHome height={24} width={24} />
 */

// Icon mapping for JDS icon components to Bootstrap icons (Filled versions where available)
const iconMap = {
  // Navigation & Home
  IcHome: BsHouseFill,
  IcNotification: BsBellFill,
  IcSearch: BsSearchHeart,
  IcQrCode: BsQrCodeScan,
  
  // User & Person
  IcPerson: BsPersonFill,
  IcBusinessman: BsPersonFill,
  
  // Communication
  IcMail: BsEnvelopeFill,
  IcPhone: BsTelephoneFill,
  IcWhatsapp: BsWhatsapp,
  
  // Building & Organization
  IcBuilding: BsBuildingFill,
  IcGroup: BsBuildingFill,
  
  // Settings & Configuration
  IcSettings: BsGearFill,
  IcSmartSwitchPlug: BsToggle2On,
  
  // Documents & Files
  IcDocument: BsFileTextFill,
  IcDocumentViewer: BsFileEarmarkTextFill,
  IcAttachment: BsPaperclip,
  IcCode: BsFileEarmarkTextFill,
  IcTicketDetails: BsFileEarmarkFill,
  IcNotes: BsChatLeftTextFill,
  
  // Actions
  IcEditPen: BsPencilFill,
  IcTrash: BsTrashFill,
  IcClose: BsXCircleFill,
  IcAdd: BsPlusCircleFill,
  IcUpload: BsCloudUploadFill,
  IcSwap: BsArrowRepeat,
  
  // Arrows & Chevrons
  IcArrowLeft: BsArrowLeftCircleFill,
  IcArrowRight: BsArrowRightCircleFill,
  IcArrowUp: BsArrowUpCircleFill,
  IcArrowDown: BsArrowDownCircleFill,
  IcChevronLeft: BsChevronLeft,
  IcChevronRight: BsChevronRight,
  IcChevronUp: BsChevronUp,
  IcChevronDown: BsChevronDown,
  
  // Status & Feedback
  IcSuccess: BsCheckCircleFill,
  IcError: BsXCircleFill,
  IcWarning: BsExclamationTriangleFill,
  IcInfo: BsInfoCircleFill,
  IcCheck: BsCheckCircleFill,
  IcStatusSuccessful: BsCheckCircleFill,
  IcSuccessColored: BsCheckCircleFill,
  IcErrorColored: BsXCircleFill,
  IcWarningColored: BsExclamationTriangleFill,
  IcStatusFail: BsXOctagonFill,
  
  // Visibility
  IcVisible: BsEyeFill,
  IcInvisible: BsEyeSlashFill,
  
  // Time & Calendar
  IcCalendar: BsCalendarFill,
  IcCalendarWeek: BsCalendarWeekFill,
  IcClock: BsClockFill,
  IcTime: BsClockFill,
  IcTimelapse: BsClockFill,
  IcAlarmOff: BsAlarmFill,
  IcTimezone: BsGlobeCentralSouthAsia,
  
  // Sorting & Filtering
  IcSort: BsSortDown,
  IcFilter: BsFunnelFill,
  IcDownload: BsCloudDownloadFill,
  IcRefresh: BsArrowClockwise,
  IcUpdate: BsPencilSquare,
  IcDataLoan: BsGearFill,
  
  // Input & Media
  IcKeyboard: BsKeyboardFill,
  IcLanguage: BsTranslate,
  IcStamp: BsStickiesFill,
  IcVoice: BsMicFill,
  IcVoiceRecording: BsMicFill,
  
  // Cookies & Data
  IcCookies: BsCookie,
  IcDatabase: BsDatabaseFill,
  
  // Special
  IcJioDot: BsCircleFill,
  IcFavorite: BsHeartFill,
  Ic404Error: BsExclamationTriangleFill,
  IcAlignBottom: BsAlignBottom,
  IcRequest: BsFileEarmarkFill,
  IcRechargeNow: BsCreditCard2FrontFill,
  IcSubscriptions: BsListCheck,
  IcSnake: BsDiagram2Fill,
  IcPlan: BsJournalText,
  IcFlipVertical: BsArrowsVertical,
  IcDownloads: BsCloudDownloadFill,
  IcAlarmSensor: BsBellSlashFill,
  
  // Navigation & Layout
  IcTeam: BsPeopleFill,
  IcForms: BsFileEarmarkTextFill,
  IcSms: BsChatLeftTextFill,
  IcNetwork: BsDiagram3Fill,
  IcEngineeringRequest: BsWrench,
  IcPageSettings: BsSliders,
  IcLayout: BsGridFill,
  IcEdit: BsPencilFill,
  
  // Additional missing icons
  IcBack: BsArrowLeftCircleFill,
  IcMinus: BsDashCircleFill,
  IcProfile: BsPersonFill,
  
  // Lowercase aliases for backwards compatibility
  ic_search: BsSearchHeart,
  ic_back: BsArrowLeftCircleFill,
  ic_close: BsXCircleFill,
  ic_add: BsPlusCircleFill,
  ic_minus: BsDashCircleFill,
  ic_chevron_down: BsChevronDown,
  ic_profile: BsPersonFill,
  ic_jio_dot: BsCircleFill,
};

/**
 * Icon wrapper component that mimics JDS icon components
 */
const Icon = ({ 
  ic = null,
  icon = null,
  size = "medium",
  width = null,
  height = null,
  color = null,
  kind = "default",
  className = "",
  style = {},
  onClick = null,
  ...props 
}) => {
  const iconProp = ic || icon;
  
  let IconComponent = null;
  
  // If iconProp is a string (icon name), map it
  if (typeof iconProp === 'string') {
    IconComponent = iconMap[iconProp] || BsInfoCircleFill;
  } else if (React.isValidElement(iconProp)) {
    // If it's already a React element, return it
    return (
      <span 
        className={`custom-icon custom-icon-${kind} ${className}`}
        style={{ color, ...style }}
        onClick={onClick}
        {...props}
      >
        {iconProp}
      </span>
    );
  } else {
    // Default icon
    IconComponent = BsInfoCircleFill;
  }
  
  // Size calculation: use width/height if provided, otherwise use size prop
  const sizeMap = {
    small: 16,
    medium: 20,
    large: 24,
    'xtra-large': 32,
  };
  
  const iconSize = width || height || (typeof size === 'number' ? size : sizeMap[size] || 20);
  
  // Color mapping
  const colorMap = {
    "primary_60": "#0f3cc9",
    "primary_grey_80": "#6b7280",
    "feedback_error_50": "#dc2626",
    "feedback_warning_50": "#f59e0b",
    "feedback_success_50": "#10b981",
  };
  
  const iconColor = colorMap[color] || color;
  
  return (
    <span 
      className={`custom-icon custom-icon-${kind} ${onClick ? 'custom-icon-clickable' : ''} ${className}`}
      style={{ color: iconColor, display: 'inline-flex', alignItems: 'center', ...style }}
      onClick={onClick}
      {...props}
    >
      <IconComponent size={iconSize} />
    </span>
  );
};

// Create individual icon components that mimic JDS exports
// These can be used directly like <IcHome /> in existing code
Object.keys(iconMap).forEach(iconName => {
  const IconFunc = ({ width = 20, height = 20, color, className, style, onClick, ...props }) => (
    <Icon 
      ic={iconName} 
      width={width} 
      height={height} 
      color={color}
      className={className}
      style={style}
      onClick={onClick}
      {...props}
    />
  );
  IconFunc.displayName = iconName;
  Icon[iconName] = IconFunc;
});

export default Icon;

// Export individual icon components
export const IcHome = Icon.IcHome;
export const IcNotification = Icon.IcNotification;
export const IcSearch = Icon.IcSearch;
export const IcPerson = Icon.IcPerson;
export const IcBusinessman = Icon.IcBusinessman;
export const IcMail = Icon.IcMail;
export const IcPhone = Icon.IcPhone;
export const IcWhatsapp = Icon.IcWhatsapp;
export const IcBuilding = Icon.IcBuilding;
export const IcGroup = Icon.IcGroup;
export const IcSettings = Icon.IcSettings;
export const IcDocument = Icon.IcDocument;
export const IcCode = Icon.IcCode;
export const IcTicketDetails = Icon.IcTicketDetails;
export const IcEditPen = Icon.IcEditPen;
export const IcTrash = Icon.IcTrash;
export const IcClose = Icon.IcClose;
export const IcAdd = Icon.IcAdd;
export const IcUpload = Icon.IcUpload;
export const IcChevronLeft = Icon.IcChevronLeft;
export const IcChevronRight = Icon.IcChevronRight;
export const IcChevronUp = Icon.IcChevronUp;
export const IcChevronDown = Icon.IcChevronDown;
export const IcSuccess = Icon.IcSuccess;
export const IcError = Icon.IcError;
export const IcWarning = Icon.IcWarning;
export const IcSort = Icon.IcSort;
export const IcSwap = Icon.IcSwap;
export const IcCookies = Icon.IcCookies;
export const IcJioDot = Icon.IcJioDot;
export const IcCalendar = Icon.IcCalendar;
export const IcCalendarWeek = Icon.IcCalendarWeek;
export const IcTime = Icon.IcTime;
export const IcTimelapse = Icon.IcTimelapse;
export const IcSmartSwitchPlug = Icon.IcSmartSwitchPlug;
export const IcAttachment = Icon.IcAttachment;
export const IcDocumentViewer = Icon.IcDocumentViewer;
export const IcNotes = Icon.IcNotes;
export const IcAlarmOff = Icon.IcAlarmOff;
export const IcAlignBottom = Icon.IcAlignBottom;
export const IcStatusSuccessful = Icon.IcStatusSuccessful;
export const IcQrCode = Icon.IcQrCode;
export const IcFavorite = Icon.IcFavorite;
export const Ic404Error = Icon.Ic404Error;
export const ic_search = Icon.ic_search;
export const IcRequest = Icon.IcRequest;
export const IcVisible = Icon.IcVisible;
export const IcInvisible = Icon.IcInvisible;
export const IcSuccessColored = Icon.IcSuccessColored;
export const IcErrorColored = Icon.IcErrorColored;
export const IcWarningColored = Icon.IcWarningColored;
export const IcFilter = Icon.IcFilter;
export const IcDownload = Icon.IcDownload;
export const IcRefresh = Icon.IcRefresh;
export const IcUpdate = Icon.IcUpdate;
export const IcKeyboard = Icon.IcKeyboard;
export const IcLanguage = Icon.IcLanguage;
export const IcStamp = Icon.IcStamp;
export const IcVoice = Icon.IcVoice;
export const IcVoiceRecording = Icon.IcVoiceRecording;
export const IcRechargeNow = Icon.IcRechargeNow;
export const IcSubscriptions = Icon.IcSubscriptions;
export const IcStatusFail = Icon.IcStatusFail;
export const IcInfo = Icon.IcInfo;
export const IcTimezone = Icon.IcTimezone;
export const IcTeam = Icon.IcTeam;
export const IcForms = Icon.IcForms;
export const IcSms = Icon.IcSms;
export const IcNetwork = Icon.IcNetwork;
export const IcEngineeringRequest = Icon.IcEngineeringRequest;
export const IcPageSettings = Icon.IcPageSettings;
export const IcLayout = Icon.IcLayout;
export const IcEdit = Icon.IcEdit;
export const IcBack = Icon.IcBack;
export const IcArrowDown = Icon.IcArrowDown;
export const IcArrowUp = Icon.IcArrowUp;
export const IcMinus = Icon.IcMinus;
export const IcProfile = Icon.IcProfile;
export const ic_back = Icon.ic_back;
export const ic_close = Icon.ic_close;
export const ic_add = Icon.ic_add;
export const ic_minus = Icon.ic_minus;
export const ic_chevron_down = Icon.ic_chevron_down;
export const ic_profile = Icon.ic_profile;
export const ic_jio_dot = Icon.ic_jio_dot;
export const IcDatabase = Icon.IcDatabase;
export const IcSnake = Icon.IcSnake;
export const IcPlan = Icon.IcPlan;
export const IcFlipVertical = Icon.IcFlipVertical;
export const IcDownloads = Icon.IcDownloads;
export const IcAlarmSensor = Icon.IcAlarmSensor;
export const IcDataLoan = Icon.IcDataLoan;
