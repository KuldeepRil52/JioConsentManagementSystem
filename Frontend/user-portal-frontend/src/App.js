import React from "react";

import { useLocation } from "react-router-dom";
import "./JDS_Styles/main.scss";
import { Footer, TokenProvider } from "@jds/core";
import SideNav from "./Components/SideNav";
import Nav from "./Routes/Nav";
import "./Styles/app.css";
import FooterComponent from "./Components/FooterComponent";
import HeaderComponent from "./Components/HeaderComponent";
function App() {
  const location = useLocation();
  const hideSideNavRoutes = [
    "/app-documents",
    "/requests/",
    "/createRequest",
    "/integrationGrievanceRequests",
    "/integrationCreateRequest",
    "/integrationGrievanceRequests/:grievanceId",
  ];
  const hideSideNav = hideSideNavRoutes.some((p) =>
    location.pathname.startsWith(p)
  );

  return (
    <TokenProvider theme="JioBase" mode="light">
      <HeaderComponent />
      <div className={!hideSideNav ? "app-layout" : ""}>
        {!hideSideNav && <SideNav />}
        <Nav />
      </div>
      {!hideSideNav && <FooterComponent />}
    </TokenProvider>
  );
}

export default App;
