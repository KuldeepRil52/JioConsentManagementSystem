import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { Provider } from "react-redux";
import { PersistGate } from "redux-persist/integration/react";
import {store, persistor} from "./store/store";
import { BrowserRouter } from "react-router-dom";
// Import sandbox mode early to set up fetch interceptor
import './utils/sandboxMode';

if (process.env.NODE_ENV === "development") {
    const originalWarn = console.warn;
    const originalError = console.error;
  
    console.warn = (...args) => {
      // show warnings in console only
      originalWarn(...args);
    };
  
    console.error = (...args) => {
      // ✅ prevent overlay: log as a warning instead of error
      originalWarn("Suppressed error overlay:", ...args);
    };
  }
  

const root = ReactDOM.createRoot(document.getElementById("root"));

root.render(

    <Provider store={store}>
        <PersistGate loading={null} persistor={persistor}>
            <BrowserRouter>
                <App />
            </BrowserRouter>
        </PersistGate>
    </Provider>

);


