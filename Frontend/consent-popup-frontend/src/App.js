import React from "react";
import "./JDS_Styles/main.scss";
import { Footer, TokenProvider } from "@jds/core";
import Nav from "./Routes/Nav";
import "./Styles/app.css";

function App() {
  return (
    <TokenProvider
      value={{
        theme: "JioBase",
        mode: "light",
      }}
    >
      <div className="app-layout">
        <Nav />
      </div>
    </TokenProvider>
  );
}

export default App;
