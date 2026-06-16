import { BrowserRouter, Routes, Route } from "react-router-dom";
import FiduciaryRequests from "../Components/FiduciaryRequests";
import Requests from "../Components/Requests";
import SystemRequests from "../Components/SystemRequests";
import CreateRequest from "../Components/CreateRequest";
import RequestDetails from "../Components/RequestDetails";
import ParentConsent from "../Components/ParentConsent";
import IntegrationGrievanceRequestsDetails from "../Components/IntegrationGrievanceRequestsDetails";
import IntegrationCreateRequest from "../Components/IntegrationCreateRequest";
import IntegrationGrievanceRequests from "../Components/IntegrationGrievanceRequests";

const Nav = () => {
  return (
    <Routes>
      <Route path="/" element={<FiduciaryRequests />} />
      <Route path="/requests" element={<Requests />} />
      <Route path="/createRequest" element={<CreateRequest />} />
      <Route path="/requests/:grievanceId" element={<RequestDetails />} />
      <Route path="/systemRequests" element={<SystemRequests />} />
      <Route path="/app-documents" element={<ParentConsent />} />

      <Route
        path="/integrationGrievanceRequests"
        element={<IntegrationGrievanceRequests />}
      />
      <Route
        path="/integrationCreateRequest"
        element={<IntegrationCreateRequest />}
      />
      <Route
        path="/integrationGrievanceRequests/:grievanceId"
        element={<IntegrationGrievanceRequestsDetails />}
      />
    </Routes>
  );
};
export default Nav;
