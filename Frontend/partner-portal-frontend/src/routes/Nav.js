import React from "react";
import { Routes, Route } from "react-router-dom";

import Cookie from "../components/Cookie";
import About from "../components/About";
import AdminLogin from "../components/AdminLogin";
import SignUp from "../components/SignUp";
import LandingPage from "../components/LandingPage";
import ContactUs from "../components/ContactUs";
import MainDashboard from "../components/MainDashboard";
import DPO from "../components/DPO";
import SystemConfigure from "../components/SystemConfigure";
import Consent from "../components/Consent";
import Grievance from "../components/Grievance";
import Notification from "../components/Notification";
import MasterSetup from "../components/MasterSetup";
import BusinessGroups from "../components/BusinessGroups";
import Roles from "../components/Roles";
import Users from "../components/Users";
import Dashboard from "../components/Dashboard";
import Templates from "../components/Templates";
import CreateConsent from "../components/CreateConsent";
import EmailTemplate from "../components/CreateEmailTemplate";
import ProtectedRoutes from "../utils/ProtectedRoutes";
import CreateEmailTemplate from "../components/CreateEmailTemplate";
import EmailTemplate from "../components/EmailTemplate";
import SmsTemplate from "../components/SmsTemplate";
import CreateSmsTemplate from "../components/CreateSmsTemplate";
import ConsentLogs from "../components/ConsentLogs";
import GrievanceLogs from "../components/GrievanceLogs";
import RegisteredCookies from "../components/RegisteredCookies";
import Cookie from "../components/Cookie";
import CookieLogs from "../components/CookieLogs";
import AddRole from "../components/AddRole";
import AddUser from "../components/AddUser";
import UpdateRole from "../components/UpdateRole";
import UpdateUser from "../components/UpdateUser";
import CookieCategory from "../components/CookieCategory";
import ROPA from "../components/ROPA";
import AddROPAEntry from "../components/AddROPAEntry";
import DPODashboard from "../components/DPODashboard";
import AuditCompliance from "../components/AuditCompliance";
import CreateAudit from "../components/CreateAudit";
import BreachNotifications from "../components/BreachNotifications";
import ReportBreach from "../components/ReportBreach";
import ReportBreachEntry from "../components/ReportBreachEntry";
import Request from "../components/Request";
import EditRequest from "../components/EditRequest";
import GrievanceEmailTemplate from "../components/GrievanceEmailTemplate";
import CreateGrievanceEmailTemplate from "../components/CreateGrievanceEmailTemplate";
import GrievanceSmsTemplate from "../components/GrievanceSmsTemplate";
import CreateGrievanceSmsTemplate from "../components/CreateGrievanceSmsTemplate";
import GrievanceFormTemplates from "../components/GrievanceFormTemplates";
import CreateGrievanceForm from "../components/CreateGrievanceForm";
import PendingRequests from "../components/PendingRequests";
import Profile from "../components/Profile";
import Unauthorized from "../components/Unauthorized";
import OrganizationMap from "../components/OrganizationMap";
import UserDashboard from "../components/UserDashboard";
import ConsentReport from "../components/ConsentReport";
import SchedulerStats from "../components/SchedulerStats";
import RetentionPolicies from "../components/RetentionPolicies";
import DataComplianceReport from "../components/DataComplianceReport";
import DataDeletionPurgeDashboard from "../components/DataDeletionPurgeDashboard";
import ConsentDeletionDetail from "../components/ConsentDeletionDetail";
import NotificationTemplate from "../components/NotificationTemplate";
import TestPDFDownload from "../components/TestPDFDownload";
import PDFViewer from "../components/PDFViewer";
import Systems from "../components/Systems";
import Datasets from "../components/Datasets";

const Nav = () => {
  // debug logs — remove after fixing
  // console.log({ RegisteredCookies, CookieReportContainer });

  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/about" element={<About />} />
      <Route path="/contact" element={<ContactUs />} />
      <Route path="/maindashboard" element={<MainDashboard />} />

      <Route path="/adminLogin" element={<AdminLogin />} />
      <Route path="/signup" element={<SignUp />} />
      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="/pdf/:documentType" element={<PDFViewer />} />

      <Route element={<ProtectedRoutes />}>
        <Route path="/dataProtectionOfficer" element={<DPO />} />
        <Route path="/systemConfiguration" element={<SystemConfigure />} />
        <Route path="/consent" element={<Consent />} />
        <Route path="/consent-system" element={<Systems />} />
        <Route path="/consent-system/add" element={<Systems />} />
        <Route path="/consent-system/edit/:id" element={<Systems />} />
        <Route path="/consent-datasets" element={<Datasets />} />
        <Route path="/consent-datasets/create" element={<Datasets />} />
        <Route path="/consent-datasets/edit/:id" element={<Datasets />} />
        <Route path="/consent-datasets/:key" element={<Datasets />} />
        <Route path="/grievance" element={<Grievance />} />
        <Route path="/notification" element={<Notification />} />
        <Route path="/master" element={<MasterSetup />} />
        <Route path="/user-dashboard" element={<UserDashboard />} />
        <Route path="/business" element={<BusinessGroups />} />
        <Route path="/roles" element={<Roles />} />
        <Route path="/users" element={<Users />} />
        <Route path="/organization-map" element={<OrganizationMap />} />
        <Route path="/scheduler-stats" element={<SchedulerStats />} />
        <Route path="/retention-policies" element={<RetentionPolicies />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/profile" element={<Profile />} />
        <Route path="/templates" element={<Templates />} />
        <Route path="/request" element={<Request />} />
        <Route path="/editRequest/:reqId" element={<EditRequest />} />
        <Route path="/grievanceEmailTemplate" element={<GrievanceEmailTemplate />} />
        <Route path="/createGrievanceEmailTemplate" element={<CreateGrievanceEmailTemplate />} />
        <Route path="/grievanceSmsTemplate" element={<GrievanceSmsTemplate />} />
        <Route path="/createGrievanceSmsTemplate" element={<CreateGrievanceSmsTemplate />} />
        <Route path="/grievanceFormTemplates" element={<GrievanceFormTemplates />} />
        <Route path="/createGrievanceForm" element={<CreateGrievanceForm />} />
        <Route path="/createConsent" element={<CreateConsent />} />
        <Route path="/createEmailTemplate" element={<CreateEmailTemplate />} />
        <Route path="/emailTemplate" element={<EmailTemplate />} />
        <Route path="/smsTemplate" element={<SmsTemplate />} />
        <Route path="/createSmsTemplate" element={<CreateSmsTemplate />} />
        <Route path="/addRole" element={<AddRole />} />
        <Route path="/addUser" element={<AddUser />} />
        <Route path="/updateRole/:roleId" element={<UpdateRole />} />
        <Route path="/updateUser/:userId" element={<UpdateUser />} />

        <Route path="/createConsent" element={<CreateConsent />} />
        <Route path="/consentLogs" element={<ConsentLogs />} />
        <Route path="/grievacneLogs" element={<GrievanceLogs />} />
        <Route path="/pendingRequests" element={<PendingRequests />} />
        <Route path="/cookie-category" element={<CookieCategory />} />
        <Route path="/ropa" element={<ROPA />} />
        <Route path="/addROPAEntry" element={<AddROPAEntry />} />
        <Route path="/dpo-dashboard" element={<DPODashboard />} />
        <Route path="/audit-compliance" element={<AuditCompliance />} />
        <Route path="/createAudit" element={<CreateAudit />} />
        <Route path="/breach-notifications" element={<BreachNotifications />} />
        <Route path="/reportBreach" element={<ReportBreach />} />
        <Route path="/reportBreachEntry" element={<ReportBreachEntry />} />
        <Route path="/consent-report" element={<ConsentReport />} />
        <Route path="/data-compliance-report" element={<DataComplianceReport />} />
        <Route path="/data-deletion-purge-dashboard" element={<DataDeletionPurgeDashboard />} />
        <Route path="/consent-deletion-detail/:eventId" element={<ConsentDeletionDetail />} />
        <Route path="/notification-templates" element={<NotificationTemplate />} />
        <Route path="/test-pdf" element={<TestPDFDownload />} />
      </Route>
      <Route path="/registercookies" element={<RegisteredCookies />} end />

      <Route path="/registercookies/report/:txId" element={<Cookie />} />
      <Route path="/cookieslogs" element={<CookieLogs />} />
    </Routes>
  );
};

export default Nav;
