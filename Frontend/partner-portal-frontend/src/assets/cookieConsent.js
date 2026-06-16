// ; (function () {
//     // ── load guard ──
//     if (window.__cookieConsentInitialized) return;
//     window.__cookieConsentInitialized = true;



//     (function injectGoogleSansCodeFonts() {
//         const style = document.createElement('style');
//         style.textContent = `
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-Regular.ttf') format('truetype');
//       font-weight: 400;
//       font-style: normal;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-Italic.ttf') format('truetype');
//       font-weight: 400;
//       font-style: italic;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-Medium.ttf') format('truetype');
//       font-weight: 500;
//       font-style: normal;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-MediumItalic.ttf') format('truetype');
//       font-weight: 500;
//       font-style: italic;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-Bold.ttf') format('truetype');
//       font-weight: 700;
//       font-style: normal;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-BoldItalic.ttf') format('truetype');
//       font-weight: 700;
//       font-style: italic;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-ExtraBold.ttf') format('truetype');
//       font-weight: 800;
//       font-style: normal;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-ExtraBoldItalic.ttf') format('truetype');
//       font-weight: 800;
//       font-style: italic;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-Light.ttf') format('truetype');
//       font-weight: 300;
//       font-style: normal;
//     }
//     @font-face {
//       font-family: 'GoogleSansCode';
//       src: url('/fonts/GoogleSansCode/GoogleSansCode-LightItalic.ttf') format('truetype');
//       font-weight: 300;
//       font-style: italic;
//     }
//   `;
//         document.head.appendChild(style);
//     })();


//     function showToast(msg) {
//         // create
//         const toast = document.createElement('div');
//         Object.assign(toast.style, {
//             position: 'fixed',
//             top: '20px',
//             right: '20px',
//             background: 'rgba(0,0,0,0.8)',
//             color: '#fff',
//             padding: '10px 16px',
//             borderRadius: '4px',
//             fontSize: '14px',
//             zIndex: 100001,
//             opacity: '0',
//             transition: 'opacity 0.3s ease',
//             pointerEvents: 'none'
//         });
//         toast.textContent = msg;
//         document.body.appendChild(toast);

//         // fade in
//         requestAnimationFrame(() => {
//             toast.style.opacity = '1';
//         });

//         // fade out after 3s
//         setTimeout(() => {
//             toast.style.opacity = '0';
//             setTimeout(() => toast.remove(), 300);
//         }, 3000);
//     }


//     // 0) Inject gorgeous top‑right toast CSS
//     ; (function initToastStyles() {
//         const css = `
//     #toastContainer {
//       position: fixed;
//       top: 24px;
//       right: 24px;
//       display: flex;
//       flex-direction: column;
//       align-items: flex-end;
//       gap: 12px;
//       z-index: 100000;
//     }

//     .toast {
//       background: linear-gradient(135deg, #4b6cb7 0%, #182848 100%);
//       color: #fff;
//       padding: 12px 20px;
//       border-radius: 8px;
//       box-shadow: 0 8px 16px rgba(0, 0, 0, 0.2);
//       font-size: 15px;
//       min-width: 220px;
//       max-width: 320px;
//       line-height: 1.4;
//       opacity: 0;
//       transform: translateX(100%);
//       display: flex;
//       align-items: center;
//       animation:
//         slideIn 0.4s ease-out forwards,
//         fadeOut 0.4s ease-in forwards 3.0s;
//     }

//     @keyframes slideIn {
//       from { transform: translateX(100%); opacity: 0; }
//       to   { transform: translateX(0);     opacity: 1; }
//     }

//     @keyframes fadeOut {
//       to { transform: translateX(100%); opacity: 0; }
//     }
//       /* —— Switch toggle styles —— */
//     .switch {
//       position: relative;
//       display: inline-block;
//       width: 40px;
//       height: 20px;
//     }
//     .switch input {
//       opacity: 0;
//       width: 0;
//       height: 0;
//     }
//     .slider {
//       position: absolute;
//       cursor: pointer;
//       top: 0; left: 0; right: 0; bottom: 0;
//       background-color: #ccc;
//       border-radius: 20px;
//       transition: .4s;
//     }
//     .slider:before {
//       position: absolute;
//       content: "";
//       height: 16px;
//       width: 16px;
//       left: 2px;
//       bottom: 2px;
//       background-color: white;
//       transition: .4s;
//       border-radius: 50%;
//     }
//     input:checked + .slider {
//       background-color: #2196F3;
//     }
//     input:checked + .slider:before {
//       transform: translateX(20px);
//     }

// /* —— ADD YOUR ACCEPT-ALL OVERRIDE BELOW —— */
//     .accept-all-btn {
//   background-color: #007bff !important;
//   color: #fff !important;
//   border: 1px solid #007bff !important;
//   transition: background-color .3s !important, color .3s !important;
// }
// .accept-all-btn:hover {
//   background-color: #fff !important;
//   color: #007bff !important;
// }


//   `;
//         const style = document.createElement('style');
//         style.appendChild(document.createTextNode(css));
//         document.head.appendChild(style);
//     })();

//     // Toast container & helper
//     // function initToastContainer() {
//     //     if (!document.getElementById('toastContainer')) {
//     //         const c = document.createElement('div');
//     //         c.id = 'toastContainer';
//     //         document.body.appendChild(c);
//     //     }
//     // }
//     // function showToast(msg, duration = 3000000) {
//     //     initToastContainer();
//     //     const t = document.createElement('div');
//     //     t.className = 'toast';
//     //     t.textContent = msg;
//     //     document.getElementById('toastContainer').appendChild(t);
//     //     setTimeout(() => {
//     //         t.style.opacity = '0';
//     //         setTimeout(() => t.remove(), 500);
//     //     }, duration);
//     // }


//     // 1) Dynamically load Bootstrap CSS (only if not already on the page)
//     function loadBootstrapCSS() {
//         const link = document.createElement('link');
//         link.rel = 'stylesheet';
//         link.href = 'https://stackpath.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css';
//         document.head.appendChild(link);
//     }


//     /**Author: Raviraj Mishra*/
//     //let SCAN_ID = "2f5be5da-7831-48c8-a0e9-5e93a91f6160"
//     let SCAN_ID = "9e6e8def-2da5-486b-9b22-afaaa620d79b"
//     let CLIENT_Id = "RJ2943SANV"

//     var cookieCategories = [];
//     let c001 = `859fe258-3350-42b4-93cc-f3d73da902b0`
//     let c002 = `q6Y8Q~-gMmTcB4NxKgnRCW0QY-45c9RiBYzuXaw-`
//     let c003 = '85772c47-6dc6-4e6a-a950-95a0f91663c8/.default'
//     let a001 = 'Basic NzFiYWExOTQtMGQ2NS00NTllLWE1NDItY2NhMDZhNjgyODdiOlQxXzhRfklzTVIwaVguMFVHS0FiM2VhakNtdGNISXlucE9Fd2xjRHI='
//     let tu850 = "https://rpapi.consent.jiolabs.com/oauth2/v2.0/token"
//     let gBn001 = "http://10.144.34.38:9018/v1.4/consent/getCookieScanReport/" + SCAN_ID
//     let crCon001 = 'http://10.144.34.38:9018/v1.4/consent/createCookiesConsent'
//     let h001 = { "Authorization": `Basic ` + a001 }
//     let data = { "grant_type": "client_credentials", "scope": c003 + `/.default` }
//     let clId = CLIENT_Id
//     let accessToken = ""
//     var cookieForConsentCreation = [];
//     var bannerSelected = "";

//     window.onload = async function () {


//         let req = {
//             "grant_type": "client_credentials",
//             "scope": c003
//         };

//         const tk = await fetch(tu850, {
//             method: 'POST',
//             body: JSON.stringify(req),
//             headers: {
//                 'Content-Type': 'application/json',
//                 'Authorization': a001,
//             }
//         });

//         const tkJSON001 = await tk.json();
//         accessToken = tkJSON001.access_token;

//         callGCon(accessToken);

//     }

//     async function callGCon(aTkn) {

//         const response = await fetch(gBn001, {
//             method: 'GET',
//             headers: {
//                 'Content-Type': 'application/json',
//                 'Authorization': "Bearer " + accessToken,
//                 'SubscriptionKey': "2778ed2e60b94b88aa3563206d5f2b28"
//             }
//         });
//         const ckrpJsn = await response.json();
//         console.log("Inside callGCon-->" + JSON.stringify(ckrpJsn));

//         createBanner(ckrpJsn);

//     }

//     // Dynamically load Font Awesome CSS
//     (function loadFontAwesome() {
//         var link = document.createElement('link');
//         link.rel = 'stylesheet';
//         link.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0-beta3/css/all.min.css';
//         document.head.appendChild(link);
//     })();




//     // function createBanner(ckrpJsn) {

//     //     // let banner = JSON.parse(ckrpJsn.preferenceCenter.theme);

//     //     // console.log("theme parsing-->" + JSON.stringify(banner));

//     //     // 1) grab whatever came back
//     //     let themeVal = ckrpJsn.preferenceCenter.theme;
//     //     let banner;

//     //     // 2) if it really is already an object, use it
//     //     if (typeof themeVal === 'object') {
//     //         banner = themeVal;

//     //         // 3) if it's the literal "[object Object]" or any other non-JSON, fall back
//     //     } else if (themeVal === '[object Object]') {
//     //         console.warn('Theme came back as [object Object], using defaults');
//     //         banner = {};

//     //         // 4) otherwise try to JSON.parse it, stripping extra quotes if present
//     //     } else {
//     //         if (typeof themeVal === 'string' && themeVal.startsWith('"') && themeVal.endsWith('"')) {
//     //             themeVal = themeVal.slice(1, -1);
//     //         }
//     //         try {
//     //             banner = JSON.parse(themeVal);
//     //         } catch (err) {
//     //             console.warn('Failed to parse theme JSON:', themeVal, err);
//     //             banner = {};
//     //         }
//     //     }

//     //     // 5) enforce your defaults
//     //     banner.consentBannerEnable = banner.consentBannerEnable || 'yes';
//     //     banner.displayStyle = banner.displayStyle || 'modal';
//     //     banner.preferenceButton = banner.preferenceButton || 'yes';

//     //     bannerSelected = banner.displayStyle;

//     //     // 0) theme color for arrows/buttons
//     //     const themeColor = "#007bff";

//     //     // let functionCookies = {
//     //     //     "name": "Functional Cookies",
//     //     //     "status": "",
//     //     //     "desc": "",
//     //     //     "list": []
//     //     // }
//     //     // let strictlyNecessaryCookies = {
//     //     //     "name": "Strictly Necessary Cookies",
//     //     //     "status": "",
//     //     //     "desc": "",
//     //     //     "list": []
//     //     // }
//     //     // let marketingCookies = {
//     //     //     "name": "Marketing Cookies",
//     //     //     "status": "",
//     //     //     "desc": "",
//     //     //     "list": []
//     //     // }
//     //     // let analyticsCookies = {
//     //     //     "name": "Analytics Cookies",
//     //     //     "status": "",
//     //     //     "desc": "",
//     //     //     "list": []
//     //     // }

//     //     // for (var cookie of ckrpJsn.cookiesDetails) {
//     //     //     // showToast("cookie : "+JSON.stringify(cookie))

//     //     //     if (cookie.category == "unclassified") {
//     //     //         strictlyNecessaryCookies.list.push(cookie)
//     //     //     } else if (cookie.category == "Functional") {
//     //     //         functionCookies.desc = cookie.description;
//     //     //         functionCookies.list.push(cookie);

//     //     //     } else if (cookie.category == "Analytics") {
//     //     //         analyticsCookies.desc = cookie.description;
//     //     //         analyticsCookies.list.push(cookie)
//     //     //     } else if (cookie.category == "Marketing") {
//     //     //         marketingCookies.desc = cookie.description;
//     //     //         marketingCookies.list.push(cookie)
//     //     //     }
//     //     // }

//     //     // if (functionCookies.list.length > 0) {
//     //     //     cookieCategories.push(functionCookies);
//     //     // }
//     //     // if (analyticsCookies.list.length > 0) {
//     //     //     cookieCategories.push(analyticsCookies);
//     //     // }
//     //     // if (marketingCookies.list.length > 0) {
//     //     //     cookieCategories.push(marketingCookies);
//     //     // }
//     //     // if (strictlyNecessaryCookies.list.length > 0) {
//     //     //     cookieCategories.push(strictlyNecessaryCookies);
//     //     // }

//     //     cookieCategories = [];
//     //     const map = {};
//     //     ckrpJsn.cookiesDetails.forEach(cookie => {
//     //         // use the literal category string (fallback to "Unspecified")
//     //         const key = cookie.category || "Unspecified";
//     //         if (!map[key]) {
//     //             map[key] = {
//     //                 name: key,    // e.g. "unclassified", "Functional", etc.
//     //                 status: "",   // start all off (user can toggle)
//     //                 desc: "",     // optional: you could map in a description here
//     //                 list: []
//     //             };
//     //         }
//     //         map[key].list.push(cookie);
//     //     });
//     //     cookieCategories = Object.values(map);

//     //     console.log("------------cookie category is created here -------- " + JSON.stringify(cookieCategories));

//     //     banner.consentBannerEnable = "yes"
//     //     banner.displayStyle = "modal"
//     //     banner.preferenceButton = "yes"

//     //     if (banner.consentBannerEnable == "yes" || true) {

//     //         if (banner.displayStyle == "modal" || true) {
//     //             console.log("inside banner")

//     //             // Create modal elements
//     //             // const modal = document.createElement('div');
//     //             // modal.classList.add('modal');
//     //             // modal.id = "cookieModal";

//     //             // const modalContent = document.createElement('div');
//     //             // modalContent.classList.add('modal-content');

//     //             // // const closeBtn = document.createElement('span');
//     //             // // closeBtn.classList.add('close');
//     //             // // closeBtn.innerHTML = '&times;';

//     //             // // ***** NEW: Cookie icon (Font Awesome) *****
//     //             // const cookieIcon = document.createElement('i');
//     //             // cookieIcon.classList.add('fas', 'fa-cookie-bite');
//     //             // cookieIcon.style.fontSize = '80px';
//     //             // cookieIcon.style.marginBottom = '10px';


//     //             // const heading = document.createElement('h1');
//     //             // heading.innerText = "Your Privacy";
//     //             // heading.style.margin = '12px';

//     //             // const modalText = document.createElement('p');
//     //             // modalText.style.textAlign = '-webkit-left';
//     //             // modalText.style.margin = '12px';
//     //             // modalText.innerText = "By clicking “Accept all cookies,” you agree that Jio can store cookies on your device and disclose information in accordance with our Cookie Policy.";

//     //             // modalContent.appendChild(cookieIcon);
//     //             // modalContent.appendChild(heading);


//     //             // const footerContent = document.createElement('div');
//     //             // footerContent.classList.add('row');

//     //             // const acceptBtn = document.createElement('button');
//     //             // acceptBtn.classList.add('btn', 'btn-success', 'm-2');  // bootstrap classes
//     //             // acceptBtn.innerHTML = 'Accept';

//     //             // const declineBtn = document.createElement('button');
//     //             // declineBtn.classList.add('btn');
//     //             // declineBtn.innerHTML = 'Decline';

//     //             // const preferenceBtn = document.createElement('button');
//     //             // preferenceBtn.classList.add('btn');
//     //             // preferenceBtn.innerHTML = 'Manage Cookies';

//     //             // document.body.appendChild(modal);
//     //             // modal.appendChild(modalContent);
//     //             // // modalContent.appendChild(closeBtn);
//     //             // modalContent.appendChild(modalText);

//     //             // footerContent.appendChild(acceptBtn);

//     //             // if (banner.declineButton == "yes") {
//     //             //     footerContent.appendChild(declineBtn);
//     //             // }
//     //             // if (banner.preferenceButton == "yes" || true) {
//     //             //     footerContent.appendChild(preferenceBtn);
//     //             // }

//     //             // modalContent.append(footerContent);

//     //             // modal.style.display = 'block';
//     //             // modal.style.position = 'fixed';
//     //             // modal.style.zIndex = '1';
//     //             // modal.style.left = '0';
//     //             // modal.style.top = '0';
//     //             // modal.style.width = '100%';
//     //             // modal.style.height = '100%';
//     //             // modal.style.overflow = 'auto';
//     //             // // modal.style.backgroundColor = '#000000b8';

//     //             // modalContent.style.backgroundColor = '#727070';
//     //             // modalContent.style.color = 'white';
//     //             // modalContent.style.margin = '15% auto';
//     //             // modalContent.style.padding = '20px';
//     //             // modalContent.style.width = '40%';
//     //             // modalContent.style.borderRadius = '20px';
//     //             // modalContent.style.boxShadow = '0 0 10px rgba(0, 0, 0, 0.3)';

//     //             // // closeBtn.style.color = '#aaa';
//     //             // // closeBtn.style.float = 'right';
//     //             // // closeBtn.style.fontSize = '28px';
//     //             // // closeBtn.style.fontWeight = 'bold';
//     //             // // closeBtn.style.cursor = 'pointer';
//     //             // // closeBtn.style.marginBottom = '2%';

//     //             // // closeBtn.addEventListener('click', closeModal);

//     //             // acceptBtn.style.backgroundColor = '#28a745';
//     //             // acceptBtn.style.width = '25%';
//     //             // acceptBtn.style.color = '#fff';
//     //             // acceptBtn.style.margin = '2%';
//     //             // acceptBtn.style.padding = '10px';
//     //             // acceptBtn.style.borderRadius = '8px';
//     //             // acceptBtn.style.border = 'none';
//     //             // acceptBtn.style.cursor = 'pointer';

//     //             // declineBtn.style.backgroundColor = '#dc3545';
//     //             // declineBtn.style.width = '25%';
//     //             // declineBtn.style.color = '#fff';
//     //             // declineBtn.style.margin = '2%';
//     //             // declineBtn.style.padding = '10px';
//     //             // declineBtn.style.borderRadius = '8px';
//     //             // declineBtn.style.border = 'none';
//     //             // declineBtn.style.cursor = 'pointer';


//     //             // preferenceBtn.style.backgroundColor = '#4fb6dd';
//     //             // preferenceBtn.style.width = '25%';
//     //             // preferenceBtn.style.border = '1px solid #fff';
//     //             // preferenceBtn.style.color = '#fff';
//     //             // preferenceBtn.style.margin = '2%';
//     //             // preferenceBtn.style.padding = '10px';
//     //             // preferenceBtn.style.borderRadius = '8px';
//     //             // preferenceBtn.style.border = 'none';
//     //             // preferenceBtn.style.cursor = 'pointer';


//     //             // acceptBtn.addEventListener('click', function () {
//     //             //     const parameter = cookieCategories;
//     //             //     acceptConsent(parameter);
//     //             // });
//     //             // declineBtn.addEventListener('click', function () {
//     //             //     const parameter = cookieCategories;
//     //             //     acceptNeccessaryConsent(parameter);
//     //             // });

//     //             // // preferenceBtn.addEventListener('click', function () {
//     //             // //     const parameter = cookieCategories;
//     //             // //     openPreferences(parameter);
//     //             // // });

//     //             // preferenceBtn.addEventListener('click', function () {
//     //             //     // Close any existing banner/modal/tooltip by hiding them
//     //             //     let existingModal = document.getElementById("cookieModal");
//     //             //     let existingBanner = document.getElementById("cookieBannerLow");
//     //             //     let existingTooltip = document.getElementById("cookieTooltip");

//     //             //     if (existingModal) {
//     //             //         existingModal.style.display = "none";
//     //             //     }
//     //             //     if (existingBanner) {
//     //             //         existingBanner.style.display = "none";
//     //             //     }
//     //             //     if (existingTooltip) {
//     //             //         existingTooltip.style.display = "none";
//     //             //     }

//     //             //     const parameter = cookieCategories;
//     //             //     openPreferences(parameter);
//     //             // });



//     //             // function openModal() {
//     //             //     modal.style.display = 'block';
//     //             // }
//     //             // function closeModal() {
//     //             //     modal.style.display = 'none';
//     //             // }

//     //             // … remove any existing banner …
//     //             const old = document.getElementById("cookieModal");
//     //             if (old) old.remove();

//     //             const bannerDiv = document.createElement("div");
//     //             bannerDiv.id = "cookieModal";
//     //             // Object.assign(bannerDiv.style.setProperty, {
//     //             //     // position: "fixed",
//     //             //     // bottom: "0",
//     //             //     // left: "0",
//     //             //     // width: "100%",
//     //             //     // background: "#fff",
//     //             //     // borderTop: "1px solid #ddd",
//     //             //     // boxShadow: "0 -2px 6px rgba(0,0,0,0.1)",
//     //             //     // padding: "12px 16px",
//     //             //     // display: "flex",
//     //             //     // alignItems: "center",
//     //             //     // justifyContent: "space-between",
//     //             //     // fontFamily: "Arial, sans-serif",
//     //             //     // zIndex: "10000"
//     //             //     position: "fixed",
//     //             //     bottom: "0",
//     //             //     left: "0",
//     //             //     width: "100%",
//     //             //     background: "#fff",
//     //             //     borderTop: "1px solid #ddd",
//     //             //     boxShadow: "0 -2px 6px rgba(0,0,0,0.1)",
//     //             //     padding: "16px",
//     //             //     fontFamily: 'GoogleSansCode', // ✅ Apply font
//     //             //     zIndex: "10000",

//     //             //     // ⚡ key change: stack vertically
//     //             //     display: "flex",
//     //             //     flexDirection: "column",
//     //             //     gap: "16px",
//     //             // });

//     //             // ✅ Set styles with setProperty (especially for !important font)
//     //             const styles = {
//     //                 position: "fixed",
//     //                 bottom: "0",
//     //                 left: "0",
//     //                 width: "100%",
//     //                 background: "#fff",
//     //                 borderTop: "1px solid #ddd",
//     //                 boxShadow: "0 -2px 6px rgba(0,0,0,0.1)",
//     //                 padding: "16px",
//     //                 zIndex: "10000",
//     //                 display: "flex",
//     //                 flexDirection: "column",
//     //                 gap: "16px",
//     //             };

//     //             for (let prop in styles) {
//     //                 bannerDiv.style.setProperty(prop.replace(/([A-Z])/g, "-$1").toLowerCase(), styles[prop]);
//     //             }

//     //             // ✅ Apply font-family with !important
//     //             bannerDiv.style.setProperty("font-family", "'GoogleSansCode', Arial, sans-serif", "important");

//     //             // — Add this close button —
//     //             const closeBtn = document.createElement("span");
//     //             closeBtn.innerHTML = "&times;";
//     //             Object.assign(closeBtn.style, {
//     //                 position: "absolute",
//     //                 top: "8px",
//     //                 right: "12px",
//     //                 fontSize: "24px",
//     //                 fontWeight: "600",
//     //                 cursor: "pointer",
//     //                 color: "#666",
//     //                 zIndex: "10001",
//     //             });
//     //             closeBtn.addEventListener("click", closeBanner);
//     //             bannerDiv.appendChild(closeBtn);



//     //             // --- NEW: text container with h4 + p ---
//     //             const textContainer = document.createElement("div");
//     //             textContainer.style.flex = "1";
//     //             textContainer.style.display = "flex";
//     //             textContainer.style.flexDirection = "column";

//     //             const heading = document.createElement("h4");
//     //             heading.innerText = "This site uses cookies to make your experience better.";
//     //             heading.style.margin = "0 0 16px 0";
//     //             heading.style.fontSize = "16px";
//     //             heading.style.fontWeight = "600";
//     //             heading.style.setProperty("font-family", "'GoogleSansCode', Arial, sans-serif", "important");

//     //             const paragraph = document.createElement("p");
//     //             paragraph.innerText =
//     //                 "We use essential cookies to make our site work. With your consent, we may also use non‑essential cookies to improve user experience and analyze website traffic. " +
//     //                 "By clicking “Accept all,” you agree to our cookie settings as described in our Cookie Policy. You can change your settings at any time by clicking “Manage preferences.”";
//     //             paragraph.style.margin = "0";
//     //             paragraph.style.fontSize = "14px";
//     //             paragraph.style.lineHeight = "1.4";
//     //             // paragraph.style.textAlign = "justify";
//     //             paragraph.style.setProperty("font-family", "'GoogleSansCode', Arial, sans-serif", "important");

//     //             textContainer.appendChild(heading);
//     //             textContainer.appendChild(paragraph);
//     //             bannerDiv.appendChild(textContainer);
//     //             // -----------------------------------------

//     //             // … then build your buttons exactly as before …
//     //             function mkBtn(label, handler, primary = false) {
//     //                 const btn = document.createElement("button");
//     //                 btn.innerText = label;

//     //                 // base styles
//     //                 Object.assign(btn.style, {
//     //                     padding: "8px 16px",
//     //                     borderRadius: "25px",
//     //                     cursor: "pointer",
//     //                     fontSize: "14px",
//     //                     fontWeight: "700",
//     //                     minWidth: "130px",
//     //                     fontFamily: '"Inter", Arial, sans-serif',
//     //                     textAlign: "center",
//     //                     transition: "background-color 0.3s, color 0.3s",
//     //                     border: primary
//     //                         ? "none"
//     //                         : `1px solid ${themeColor}`,               // white-bg buttons get a blue border
//     //                     background: primary ? themeColor : "#fff",  // primary=solid, else white
//     //                     color: primary ? "#fff" : themeColor,
//     //                 });

//     //                 // hover effects
//     //                 btn.addEventListener("mouseenter", () => {
//     //                     if (!primary) {
//     //                         btn.style.background = themeColor;
//     //                         btn.style.color = "#fff";
//     //                     }
//     //                 });
//     //                 btn.addEventListener("mouseleave", () => {
//     //                     if (!primary) {
//     //                         btn.style.background = "#fff";
//     //                         btn.style.color = themeColor;
//     //                     }
//     //                 });

//     //                 btn.addEventListener("click", handler);
//     //                 return btn;
//     //             }

//     //             // const btns = document.createElement("div");
//     //             // btns.style.display = "flex";
//     //             // btns.style.gap = "8px";

//     //             // btns.appendChild(
//     //             //     mkBtn("Manage preferences", false, () => {
//     //             //         closeBanner();
//     //             //         openPreferences(cookieCategories);
//     //             //     })
//     //             // );

//     //             // // helper to simply close whatever banner is shown
//     //             // function closeBanner() {
//     //             //     document.getElementById("cookieModal")?.remove();
//     //             //     document.getElementById("cookieBannerLow")?.remove();
//     //             //     document.getElementById("cookieTooltip")?.remove();
//     //             // }

//     //             // btns.appendChild(
//     //             //     mkBtn("Reject all", false, closeBanner)
//     //             // );
//     //             // btns.appendChild(
//     //             //     mkBtn("Accept necessary cookies", false, () => acceptNeccessaryConsent(cookieCategories))
//     //             // );
//     //             // btns.appendChild(
//     //             //     mkBtn("Accept all", true, () => acceptConsent(cookieCategories))
//     //             // );

//     //             // bannerDiv.appendChild(btns);
//     //             const btns = document.createElement("div");
//     //             btns.style.display = "flex";
//     //             btns.style.gap = "12px";
//     //             btns.style.justifyContent = "flex-end";

//     //             function closeBanner() {
//     //                 document.getElementById("cookieModal")?.remove();
//     //                 document.getElementById("cookieBannerLow")?.remove();
//     //                 document.getElementById("cookieTooltip")?.remove();
//     //             }

//     //             // btns.appendChild(
//     //             //     mkBtn("Manage preferences", () => {
//     //             //         closeBanner();
//     //             //         openPreferences(cookieCategories);
//     //             //     })
//     //             // );
//     //             // btns.appendChild(
//     //             //     mkBtn("Reject all", closeBanner)
//     //             // );
//     //             // btns.appendChild(
//     //             //     mkBtn("Accept necessary cookies", () => acceptNeccessaryConsent(cookieCategories))
//     //             // );
//     //             // btns.appendChild(
//     //             //     mkBtn("Accept all", () => acceptConsent(cookieCategories))
//     //             // );
//     //             btns.append(
//     //                 mkBtn("Manage preferences", () => { closeBanner(); openPreferences(cookieCategories); }, false),
//     //                 mkBtn("Reject all", closeBanner, false),
//     //                 mkBtn("Accept necessary cookies", () => acceptNeccessaryConsent(cookieCategories), false),
//     //                 // mkBtn("Accept all", () => acceptConsent(cookieCategories), true)   // primary
//     //             );

//     //             // … now build Accept all …
//     //             const acceptAllBtn = mkBtn("Accept all", () => acceptConsent(cookieCategories));
//     //             acceptAllBtn.classList.add("accept-all-btn");
//     //             btns.appendChild(acceptAllBtn);

//     //             bannerDiv.appendChild(btns);
//     //             document.body.appendChild(bannerDiv);

//     //         }

//     //         else if (banner.displayStyle == "banner") {

//     //             const bannerDiv = document.createElement('div');
//     //             bannerDiv.classList.add('banner');
//     //             bannerDiv.innerText = "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking “Accept,“ you agree to our website's cookie use as described in our Cookie Policy";
//     //             bannerDiv.id = "cookieBannerLow"

//     //             const footerContent = document.createElement('div');
//     //             footerContent.classList.add('row');

//     //             const acceptBtn = document.createElement('button');
//     //             acceptBtn.classList.add('btn');
//     //             acceptBtn.innerHTML = 'Accept';

//     //             const declineBtn = document.createElement('button');
//     //             declineBtn.classList.add('btn');
//     //             declineBtn.innerHTML = 'Decline';

//     //             const preferenceBtn = document.createElement('button');
//     //             preferenceBtn.classList.add('btn');
//     //             preferenceBtn.innerHTML = 'Preferences';

//     //             footerContent.appendChild(acceptBtn);

//     //             if (banner.declineButton == "yes") {
//     //                 footerContent.appendChild(declineBtn);
//     //             }
//     //             if (banner.preferenceButton == "yes") {
//     //                 footerContent.appendChild(preferenceBtn);
//     //             }

//     //             acceptBtn.addEventListener('click', function () {
//     //                 const parameter = cookieCategories;
//     //                 acceptConsent(parameter);
//     //             });
//     //             declineBtn.addEventListener('click', function () {
//     //                 const parameter = cookieCategories;
//     //                 acceptNeccessaryConsent(parameter);
//     //             });
//     //             preferenceBtn.addEventListener('click', function () {
//     //                 const parameter = cookieCategories;
//     //                 openPreferences(parameter);
//     //             });
//     //             bannerDiv.append(footerContent);



//     //             // Apply CSS styles
//     //             bannerDiv.style.position = 'fixed';
//     //             bannerDiv.style.bottom = '0';
//     //             bannerDiv.style.left = '0';
//     //             bannerDiv.style.width = '95%';
//     //             bannerDiv.style.backgroundColor = banner.banner.backgroundColor;
//     //             bannerDiv.style.color = banner.banner.color;;
//     //             bannerDiv.style.padding = '25px';
//     //             bannerDiv.style.margin = '2%';
//     //             bannerDiv.style.textAlign = 'left';

//     //             acceptBtn.style.backgroundColor = '#28a745';
//     //             acceptBtn.style.width = '20%';
//     //             acceptBtn.style.color = '#fff';
//     //             acceptBtn.style.margin = '1%';

//     //             declineBtn.style.backgroundColor = '#dc3545';
//     //             declineBtn.style.width = '20%';
//     //             declineBtn.style.color = '#fff';
//     //             declineBtn.style.margin = '1%';


//     //             preferenceBtn.style.backgroundColor = 'transparent';
//     //             preferenceBtn.style.width = '20%';
//     //             preferenceBtn.style.border = '1px solid #fff';
//     //             preferenceBtn.style.color = '#fff';
//     //             preferenceBtn.style.margin = '1%';

//     //             // Append the banner to the document body
//     //             document.body.appendChild(bannerDiv);

//     //         }

//     //         else if (banner.displayStyle == "tooltip") {

//     //             const bannerDiv = document.createElement('div');
//     //             bannerDiv.classList.add('banner');
//     //             bannerDiv.innerText = "We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. By clicking “Accept,“ you agree to our website's cookie use as described in our Cookie Policy";
//     //             bannerDiv.id = "cookieTooltip"
//     //             const footerContent = document.createElement('div');
//     //             footerContent.classList.add('row');

//     //             const acceptBtn = document.createElement('button');
//     //             acceptBtn.classList.add('btn');
//     //             acceptBtn.innerHTML = 'Accept';

//     //             const declineBtn = document.createElement('button');
//     //             declineBtn.classList.add('btn');
//     //             declineBtn.innerHTML = 'Decline';

//     //             const preferenceBtn = document.createElement('button');
//     //             preferenceBtn.classList.add('btn');
//     //             preferenceBtn.innerHTML = 'Preferences';

//     //             footerContent.appendChild(acceptBtn);

//     //             if (banner.declineButton == "yes") {
//     //                 footerContent.appendChild(declineBtn);
//     //             }
//     //             if (banner.preferenceButton == "yes") {
//     //                 footerContent.appendChild(preferenceBtn);
//     //             }

//     //             acceptBtn.addEventListener('click', function () {
//     //                 const parameter = cookieCategories;
//     //                 acceptConsent(parameter);
//     //             });
//     //             declineBtn.addEventListener('click', function () {
//     //                 const parameter = cookieCategories;
//     //                 acceptNeccessaryConsent(parameter);
//     //             });
//     //             preferenceBtn.addEventListener('click', function () {
//     //                 const parameter = cookieCategories;
//     //                 openPreferences(parameter);
//     //             });

//     //             bannerDiv.append(footerContent);

//     //             // Apply CSS styles
//     //             bannerDiv.style.position = 'fixed';
//     //             bannerDiv.style.bottom = '0';
//     //             bannerDiv.style.right = '15px';
//     //             bannerDiv.style.width = '45%';
//     //             bannerDiv.style.backgroundColor = banner.banner.backgroundColor;
//     //             bannerDiv.style.color = banner.banner.color;;
//     //             bannerDiv.style.padding = '25px';
//     //             bannerDiv.style.margin = '2%';
//     //             bannerDiv.style.textAlign = 'left';

//     //             acceptBtn.style.backgroundColor = '#28a745';
//     //             acceptBtn.style.width = '25%';
//     //             acceptBtn.style.color = '#fff';
//     //             acceptBtn.style.margin = '1%';

//     //             declineBtn.style.backgroundColor = '#dc3545';
//     //             declineBtn.style.width = '25%';
//     //             declineBtn.style.color = '#fff';
//     //             declineBtn.style.margin = '1%';


//     //             preferenceBtn.style.backgroundColor = 'transparent';
//     //             preferenceBtn.style.width = '25%';
//     //             preferenceBtn.style.border = '1px solid #fff';
//     //             preferenceBtn.style.color = '#fff';
//     //             preferenceBtn.style.margin = '1%';

//     //             // Append the banner to the document body
//     //             document.body.appendChild(bannerDiv);

//     //         }

//     //     }

//     // }













//     function createBanner(ckrpJsn) {
//         // ---------- helpers ----------
//         function safeParseTheme(themeVal) {
//             if (!themeVal) return {};
//             if (typeof themeVal === 'object') return themeVal;
//             let str = String(themeVal);
//             if (str.startsWith('"') && str.endsWith('"')) str = str.slice(1, -1);
//             try { return JSON.parse(str); } catch { return {}; }
//         }

//         function getResolvedTheme(json) {
//             // read from either root.theme or preferenceCenter.theme
//             const rawStr = json?.theme ?? json?.preferenceCenter?.theme ?? {};
//             const raw = safeParseTheme(rawStr);

//             const resolved = {
//                 // if backend says "no", we’ll still render so you can test.
//                 consentBannerEnable: (raw.consentBannerEnable ?? 'yes'),
//                 declineButton: raw.declineButton ?? 'yes',
//                 preferenceButton: raw.preferenceButton ?? 'yes',
//                 optInOptOut: raw.optInOptOut ?? 'optOut',
//                 scrollToConsent: raw.scrollToConsent ?? 'no',
//                 displayStyle: raw.displayStyle ?? 'modal',
//                 banner: {
//                     backgroundColor: '#ffffff',
//                     color: '#000000',
//                     ...(raw.banner || {})
//                 },
//                 button: {
//                     backgroundColor: '#007bff',
//                     color: '#ffffff',
//                     ...(raw.button || {})
//                 },
//                 font: raw.font || null
//             };

//             // normalize hex casing
//             const norm = v => (typeof v === 'string' ? v : '').trim();
//             resolved.banner.backgroundColor = norm(resolved.banner.backgroundColor) || '#ffffff';
//             resolved.banner.color = norm(resolved.banner.color) || '#000000';
//             resolved.button.backgroundColor = norm(resolved.button.backgroundColor) || '#007bff';
//             resolved.button.color = norm(resolved.button.color) || '#ffffff';

//             return resolved;
//         }

//         function applyCustomFont(font) {
//             try {
//                 if (!font || font.mode !== 'custom' || !font.family || !font.dataUri) return;
//                 const id = 'cc-custom-font-' + font.family;
//                 if (document.getElementById(id)) return;
//                 const style = document.createElement('style');
//                 style.id = id;
//                 style.textContent = `
// @font-face {
//   font-family: '${font.family}';
//   src: url('${font.dataUri}') format('truetype');
//   font-weight: 400;
//   font-style: normal;
// }`;
//                 document.head.appendChild(style);
//                 document.documentElement.style.setProperty('--cc-font', `'${font.family}', Arial, sans-serif`);
//             } catch (e) {
//                 console.warn('Font injection failed:', e);
//             }
//         }

//         function mkBtn(theme, label, handler, primary = false) {
//             const btn = document.createElement('button');
//             btn.innerText = label;

//             const btnBg = theme.button.backgroundColor;
//             const btnFg = theme.button.color;

//             Object.assign(btn.style, {
//                 padding: '8px 16px',
//                 borderRadius: '25px',
//                 cursor: 'pointer',
//                 fontSize: '14px',
//                 fontWeight: '700',
//                 minWidth: '130px',
//                 textAlign: 'center',
//                 transition: 'background-color .2s, color .2s, border-color .2s, filter .2s',
//                 border: primary ? 'none' : `1px solid ${btnBg}`,
//                 background: primary ? btnBg : 'transparent',
//                 color: primary ? btnFg : btnBg,
//                 fontFamily: "var(--cc-font, 'GoogleSansCode', Arial, sans-serif)"
//             });

//             btn.addEventListener('mouseenter', () => {
//                 if (!primary) { btn.style.background = btnBg; btn.style.color = btnFg; }
//                 else { btn.style.filter = 'brightness(1.05)'; }
//             });
//             btn.addEventListener('mouseleave', () => {
//                 if (!primary) { btn.style.background = 'transparent'; btn.style.color = btnBg; }
//                 else { btn.style.filter = 'none'; }
//             });

//             btn.addEventListener('click', handler);
//             return btn;
//         }

//         function closeBanner() {
//             document.getElementById('cookieModal')?.remove();
//             document.getElementById('cookieBannerLow')?.remove();
//             document.getElementById('cookieTooltip')?.remove();
//         }

//         // ---------- theme + font ----------
//         const theme = getResolvedTheme(ckrpJsn);
//         window.__cookieTheme = theme;
//         theme.declineButton = 'yes';
//         theme.preferenceButton = 'yes';


//         bannerSelected = theme.displayStyle;
//         applyCustomFont(theme.font);

//         // ---------- categories ----------
//         cookieCategories = [];
//         const map = {};
//         (ckrpJsn?.cookiesDetails || []).forEach(cookie => {
//             const key = cookie.category || 'Unspecified';
//             if (!map[key]) map[key] = { name: key, status: '', desc: '', list: [] };
//             map[key].list.push(cookie);
//         });
//         cookieCategories = Object.values(map);

//         // ALWAYS render (ignore "no" to let you test UI)
//         // if (theme.consentBannerEnable !== 'yes') return;

//         // ---------- inner content ----------
//         function buildContent(container, layout = 'modal') {
//             container.style.setProperty('font-family', "var(--cc-font, 'GoogleSansCode', Arial, sans-serif)", 'important');
//             container.style.backgroundColor = theme.banner.backgroundColor;
//             container.style.color = theme.banner.color;

//             const textContainer = document.createElement('div');
//             textContainer.style.flex = '1';
//             textContainer.style.display = 'flex';
//             textContainer.style.flexDirection = 'column';

//             const heading = document.createElement('h4');
//             heading.innerText = 'This site uses cookies to make your experience better.';
//             heading.style.margin = '0 0 16px 0';
//             heading.style.fontSize = '16px';
//             heading.style.fontWeight = '600';
//             heading.style.setProperty('font-family', "var(--cc-font, 'GoogleSansCode', Arial, sans-serif)", 'important');
//             heading.style.color = theme.banner.color; // dynamic

//             const paragraph = document.createElement('p');
//             paragraph.innerText =
//                 'We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience and analyze website traffic. ' +
//                 'By clicking “Accept all,” you agree to our cookie settings as described in our Cookie Policy. You can change your settings anytime via “Manage preferences.”';
//             paragraph.style.margin = '0';
//             paragraph.style.fontSize = '13px';
//             paragraph.style.lineHeight = '1.4';
//             paragraph.style.setProperty('font-family', "var(--cc-font, 'GoogleSansCode', Arial, sans-serif)", 'important');
//             paragraph.style.color = theme.banner.color; // dynamic

//             textContainer.append(heading, paragraph);
//             container.appendChild(textContainer);

//             const btns = document.createElement('div');
//             btns.style.display = 'flex';
//             btns.style.gap = '12px';
//             btns.style.justifyContent = 'flex-end';

//             if (theme.preferenceButton === 'yes') {
//                 btns.append(
//                     mkBtn(theme, 'Manage preferences', () => { closeBanner(); openPreferences(cookieCategories); }, false)
//                 );
//             }
//             if (theme.declineButton === 'yes') {
//                 btns.append(
//                     mkBtn(theme, 'Reject all', async () => {
//                         const cloned = JSON.parse(JSON.stringify(cookieCategories));
//                         for (const cat of cloned) cat.status = (cat.name === 'Strictly Necessary Cookies') ? 'yes' : 'no';
//                         // await acceptSelectedConsent(cloned);
//                         closeBanner();
//                     }, false)
//                 );
//             }
//             btns.append(
//                 mkBtn(theme, 'Accept necessary cookies', () => acceptNeccessaryConsent(cookieCategories), false)
//             );
//             btns.append(
//                 mkBtn(theme, 'Accept all', () => acceptConsent(cookieCategories), true)
//             );

//             container.appendChild(btns);
//         }

//         // ---------- clear + render ----------
//         closeBanner();

//         if (theme.displayStyle === 'modal') {
//             const overlay = document.createElement('div');
//             overlay.id = 'cookieModal';
//             Object.assign(overlay.style, {
//                 position: 'fixed', left: '0', top: '0', width: '100%', height: '100%',
//                 backgroundColor: 'rgba(0,0,0,0.6)', display: 'flex',
//                 alignItems: 'center', justifyContent: 'center', zIndex: '10000'
//             });

//             const card = document.createElement('div');
//             Object.assign(card.style, {
//                 width: '90%', maxWidth: '640px', padding: '24px',
//                 borderRadius: '24px', boxShadow: '0 4px 16px rgba(0,0,0,0.25)',
//                 display: 'flex', flexDirection: 'column', gap: '16px'
//             });

//             const closeBtn = document.createElement('span');
//             closeBtn.innerHTML = '&times;';
//             Object.assign(closeBtn.style, {
//                 position: 'absolute', top: '16px', right: '24px',
//                 color: '#fff', fontSize: '28px', fontWeight: '700', cursor: 'pointer'
//             });
//             closeBtn.addEventListener('click', closeBanner);

//             overlay.append(closeBtn, card);
//             document.body.appendChild(overlay);
//             buildContent(card, 'modal');

//         } else if (theme.displayStyle === 'banner') {
//             const bar = document.createElement('div');
//             bar.id = 'cookieBannerLow';
//             Object.assign(bar.style, {
//                 position: 'fixed', bottom: '0', left: '0', width: '100%',
//                 borderTop: '1px solid rgba(0,0,0,0.1)', boxShadow: '0 -2px 6px rgba(0,0,0,0.1)',
//                 padding: '16px', zIndex: '10000', display: 'flex', flexDirection: 'column', gap: '16px'
//             });

//             const closeBtn = document.createElement('span');
//             closeBtn.innerHTML = '&times;';
//             Object.assign(closeBtn.style, {
//                 position: 'absolute', right: '16px', top: '8px',
//                 fontSize: '22px', fontWeight: '700', color: theme.banner.color, cursor: 'pointer'
//             });
//             closeBtn.addEventListener('click', closeBanner);
//             bar.appendChild(closeBtn);

//             document.body.appendChild(bar);
//             buildContent(bar, 'banner');

//         } else if (theme.displayStyle === 'tooltip') {
//             const tip = document.createElement('div');
//             tip.id = 'cookieTooltip';
//             Object.assign(tip.style, {
//                 position: 'fixed', bottom: '16px', right: '16px',
//                 width: 'min(480px, 92vw)', borderRadius: '16px',
//                 boxShadow: '0 8px 24px rgba(0,0,0,0.25)', padding: '16px',
//                 zIndex: '10000', display: 'flex', flexDirection: 'column', gap: '16px'
//             });

//             const closeBtn = document.createElement('span');
//             closeBtn.innerHTML = '&times;';
//             Object.assign(closeBtn.style, {
//                 position: 'absolute', right: '12px', top: '8px',
//                 fontSize: '22px', fontWeight: '700', color: theme.banner.color, cursor: 'pointer'
//             });
//             closeBtn.addEventListener('click', closeBanner);
//             tip.appendChild(closeBtn);

//             document.body.appendChild(tip);
//             buildContent(tip, 'tooltip');

//         } else {
//             // fallback to banner
//             const bar = document.createElement('div');
//             bar.id = 'cookieBannerLow';
//             Object.assign(bar.style, {
//                 position: 'fixed', bottom: '0', left: '0', width: '100%',
//                 borderTop: '1px solid rgba(0,0,0,0.1)', padding: '16px',
//                 zIndex: '10000', display: 'flex', flexDirection: 'column', gap: '16px'
//             });
//             document.body.appendChild(bar);
//             buildContent(bar, 'banner');
//         }

//         // helpful: log what we parsed
//         console.log('[cookie-banner] theme used:', theme);
//     }












//     function hideAllConsentUI() {
//         ["preferenceModal", "cookieModal", "cookieBannerLow", "cookieTooltip"].forEach(id => {
//             const el = document.getElementById(id);
//             if (el) el.remove();            // or: el.style.display = "none";
//         });
//     }


















//     // async function acceptConsent(cookies) {
//     //     // showToast("accepting"+JSON.stringify(cookies));
//     //     console.log("accepting" + JSON.stringify(cookies));

//     //     for (var singleCookie of cookies) {
//     //         singleCookie.status = "yes"
//     //     }

//     //     //showToast(JSON.stringify(cookies));
//     //     console.log("cookiesCategories send-->" + JSON.stringify(cookies));

//     //     var requestConsent = {
//     //         "status": "ACTIVE",
//     //         "userDetails": {
//     //             "ip": "127.0.0.1",
//     //             "country": "india"
//     //         },
//     //         "cookiesCategories": cookies
//     //     }

//     //     const response = await fetch(crCon001, {
//     //         method: 'POST',
//     //         body: JSON.stringify(requestConsent),
//     //         headers: {
//     //             'Content-Type': 'application/json',
//     //             'Authorization': "Bearer " + accessToken,
//     //             'SubscriptionKey': "2778ed2e60b94b88aa3563206d5f2b28",
//     //             'clientId': clId
//     //         }
//     //     });
//     //     const ckrpJsn = await response.json();
//     //     //showToast(JSON.stringify(ckrpJsn));
//     //     if (ckrpJsn.cookieConsentId != "" || ckrpJsn.cookieConsentId != null) {
//     //         document.getElementById("cookieModal").style.display = "none";
//     //         if (bannerSelected == "banner") {
//     //             document.getElementById("cookieBannerLow").style.display = "none";
//     //         } else if (bannerSelected == "tooltip") {
//     //             document.getElementById("cookieTooltip").style.display = "none";
//     //         } else if (bannerSelected == "modal") {
//     //             document.getElementById("cookieModal").style.display = "none";
//     //         }

//     //         showToast("Your Consent is captured!");

//     //     } else {
//     //         showToast("Your Consent could not be captured!");
//     //     }

//     // }

//     // async function acceptNeccessaryConsent(cookies) {
//     //     //showToast("accepting"+JSON.stringify(cookies));

//     //     for (var singleCookie of cookies) {
//     //         if (singleCookie.name == "Strictly Necessary Cookies") {
//     //             singleCookie.status = "yes"
//     //         } else {
//     //             singleCookie.status = "no"
//     //         }

//     //     }

//     //     //showToast(JSON.stringify(cookies));

//     //     var requestConsent = {
//     //         "status": "ACTIVE",
//     //         "userDetails": {
//     //             "ip": "127.0.0.1",
//     //             "country": "india"
//     //         },
//     //         "cookiesCategories": cookies
//     //     }

//     //     const response = await fetch(crCon001, {
//     //         method: 'POST',
//     //         body: JSON.stringify(requestConsent),
//     //         headers: {
//     //             'Content-Type': 'application/json',
//     //             'Authorization': "Bearer " + accessToken,
//     //             'SubscriptionKey': "2778ed2e60b94b88aa3563206d5f2b28",
//     //             'clientId': clId
//     //         }
//     //     });
//     //     const ckrpJsn = await response.json();
//     //     // showToast(JSON.stringify(ckrpJsn));
//     //     if (ckrpJsn.cookieConsentId != "" || ckrpJsn.cookieConsentId != null) {
//     //         document.getElementById("cookieModal").style.display = "none";
//     //         showToast("Your Consent is captured!");
//     //     } else {
//     //         showToast("Your Consent could not be captured!");
//     //     }
//     // }































//     // function openPreferences(cookies) {
//     //     //showToast("opening preferences");
//     //     var countCheck = 0;


//     //     // Create modal elements
//     //     const modal = document.createElement('div');
//     //     modal.classList.add('modal');
//     //     modal.id = "preferenceModal"

//     //     const modalContent = document.createElement('div');
//     //     modalContent.classList.add('modal-content');

//     //     const closeBtn = document.createElement('span');
//     //     closeBtn.classList.add('close');
//     //     closeBtn.innerHTML = '&times;';

//     //     const modalTitle = document.createElement('h2');
//     //     modalTitle.style.marginBottom = '2%';
//     //     modalTitle.innerText = 'Manage Cookies';

//     //     const modalText = document.createElement('p');
//     //     modalText.style.textAlign = 'justify';
//     //     modalText.style.marginBottom = '3%';
//     //     modalText.innerText = "When you visit any website, it may store or retrieve information on your browser, mostly in the form of cookies. This information might be about you, your preferences or your device and is mostly used to make the site work as you expect it to. The information does not usually directly identify you, but it can give you a more personalized web experience. Because we respect your right to privacy, you can choose not to allow some types of cookies. Click on the different category headings to find out more and change your default settings.";

//     //     document.body.appendChild(modal);
//     //     modal.appendChild(modalContent);
//     //     modalContent.appendChild(closeBtn);
//     //     modalContent.appendChild(modalTitle);
//     //     modalContent.appendChild(modalText);

//     //     for (let singleCookie of cookies) {

//     //         // showToast("singleCookie in loop : "+singleCookie.name);

//     //         const panel = document.createElement('div');
//     //         panel.innerText = singleCookie.name;
//     //         panel.style.border = '1px solid #ddd';
//     //         panel.style.padding = '10px';
//     //         panel.style.background = '#154670';
//     //         panel.style.color = '#fff';
//     //         panel.style.cursor = 'pointer'

//     //         var label = document.createElement("label");
//     //         label.className = "switch";
//     //         label.style.margin = "70%";
//     //         label.style.verticalAlign = "-webkit-baseline-middle";


//     //         var input = document.createElement("input");
//     //         input.type = "checkbox";
//     //         input.name = singleCookie.name;
//     //         input.value = singleCookie.name;
//     //         input.id = "check_" + countCheck++;

//     //         label.appendChild(input);
//     //         var span = document.createElement("span");
//     //         span.className = "slider round";


//     //         // span.setAttribute("onclick","checkTheBox('"+input.value+","+cookies+"')");

//     //         span.addEventListener('click', function () {
//     //             //showToast("sending the value : "+singleCookie.name);
//     //             const parameter = singleCookie.name;
//     //             checkTheBox(parameter, cookies);
//     //         });



//     //         label.appendChild(span);

//     //         // label.addEventListener('click', function() {
//     //         //     showToast("sending the value : "+input.value);
//     //         //     checkTheBox(input.value, cookies);
//     //         // });

//     //         panel.appendChild(label);

//     //         panel.addEventListener('click', () => {
//     //             const content = panel.nextElementSibling;
//     //             if (content.style.display === 'block') {
//     //                 content.style.display = 'none';
//     //             } else {
//     //                 content.style.display = 'block';
//     //             }
//     //         });
//     //         const content = document.createElement('div');

//     //         if (panel.innerText == "Functional Cookies") {
//     //             const descVal = document.createElement('p');
//     //             descVal.textContent = "These cookies enable the website to provide enhanced functionality and personalisation based on your interaction with the website. They may be set by us or by third party providers whose services we have added to our pages."
//     //             content.appendChild(descVal);

//     //         } else if (panel.innerText == "Marketing Cookies") {
//     //             const descVal = document.createElement('p');
//     //             descVal.textContent = "These cookies may be set through our site by our advertising partners. They may be used by those partners to build a profile of your interests and show you relevant advertisements on other websites."
//     //             content.appendChild(descVal);

//     //         } else if (panel.innerText == "Strictly Necessary Cookies") {
//     //             const descVal = document.createElement('p');
//     //             descVal.textContent = "These cookies are necessary for the website to function and cannot be switched off in our systems. They are usually only set in response to actions made by you which amount to a request for services, such as setting your privacy preferences or filling in forms."
//     //             content.appendChild(descVal);

//     //         }



//     //         for (const cookieText of singleCookie.list) {
//     //             const mainContent = document.createElement('div');

//     //             const contentInner = document.createElement('div');
//     //             const h6 = document.createElement('label');
//     //             h6.textContent = 'Cookie Name';
//     //             const p = document.createElement('p');
//     //             p.textContent = cookieText.cookieName;
//     //             contentInner.appendChild(h6);
//     //             contentInner.appendChild(p);
//     //             mainContent.appendChild(contentInner);

//     //             const contentInner2 = document.createElement('div');
//     //             const h62 = document.createElement('label');
//     //             h62.textContent = 'Domain';
//     //             const p2 = document.createElement('p');
//     //             p2.textContent = cookieText.domain;
//     //             contentInner2.appendChild(h62);
//     //             contentInner2.appendChild(p2);
//     //             mainContent.appendChild(contentInner2);

//     //             const contentInner3 = document.createElement('div');
//     //             const h63 = document.createElement('label');
//     //             h63.textContent = 'Description';
//     //             const p3 = document.createElement('p');
//     //             p3.textContent = cookieText.description;
//     //             contentInner3.appendChild(h63);
//     //             contentInner3.appendChild(p3);
//     //             mainContent.appendChild(contentInner3);

//     //             const contentInner4 = document.createElement('div');
//     //             const h64 = document.createElement('label');
//     //             h64.textContent = 'Retention Period';
//     //             const p4 = document.createElement('p');
//     //             p4.textContent = cookieText.retentionPeriod;
//     //             contentInner4.appendChild(h64);
//     //             contentInner4.appendChild(p4);
//     //             mainContent.appendChild(contentInner4);

//     //             content.appendChild(mainContent);

//     //             mainContent.style.backgroundColor = 'rgb(232 232 232)';
//     //             mainContent.style.padding = '2%';
//     //             mainContent.style.margin = '2%';
//     //             mainContent.style.fontSize = '14px';
//     //             mainContent.style.borderRadius = '8px';
//     //             h6.style.fontWeight = 'bold';
//     //             h62.style.fontWeight = 'bold';
//     //             h63.style.fontWeight = 'bold';
//     //             h64.style.fontWeight = 'bold';


//     //         }




//     //         content.style.display = 'none'; // Initially hide the content
//     //         content.style.border = '1px solid #ddd';
//     //         content.style.padding = '10px';
//     //         content.style.height = '250px';
//     //         content.style.overflow = 'auto';

//     //         modalContent.appendChild(panel);
//     //         modalContent.appendChild(content);


//     //     }







//     //     const footerContent = document.createElement('div');
//     //     footerContent.classList.add('row');

//     //     const acceptBtn = document.createElement('button');
//     //     acceptBtn.classList.add('btn');
//     //     acceptBtn.innerHTML = 'Accept';

//     //     const declineBtn = document.createElement('button');
//     //     declineBtn.classList.add('btn');
//     //     declineBtn.innerHTML = 'Decline';

//     //     const acceptSelected = document.createElement('button');
//     //     acceptSelected.classList.add('btn');
//     //     acceptSelected.innerHTML = 'Accept Selected';





//     //     footerContent.appendChild(acceptBtn);
//     //     footerContent.appendChild(declineBtn);
//     //     footerContent.appendChild(acceptSelected);

//     //     // if(banner.declineButton == "yes"){
//     //     //     footerContent.appendChild(declineBtn);
//     //     // }
//     //     // if(banner.preferenceButton == "yes"){
//     //     //     footerContent.appendChild(preferenceBtn);
//     //     // }

//     //     modalContent.append(footerContent);

//     //     modal.style.display = 'block';
//     //     modal.style.position = 'fixed';
//     //     modal.style.zIndex = '1';
//     //     modal.style.left = '0';
//     //     modal.style.top = '1';
//     //     modal.style.width = '100%';
//     //     modal.style.height = '100%';
//     //     modal.style.overflow = 'auto';
//     //     // modal.style.backgroundColor = '#000000b8';

//     //     modalContent.style.backgroundColor = '#fff';
//     //     modalContent.style.color = '#000';
//     //     modalContent.style.margin = '15% auto';
//     //     modalContent.style.marginTop = '3%';
//     //     modalContent.style.padding = '20px';
//     //     modalContent.style.width = '60%';
//     //     modalContent.style.boxShadow = '0 0 10px rgba(0, 0, 0, 0.3)';
//     //     modalContent.style.borderRadius = '20px';

//     //     closeBtn.style.color = '#aaa';
//     //     closeBtn.style.float = 'right';
//     //     closeBtn.style.fontSize = '28px';
//     //     closeBtn.style.fontWeight = 'bold';
//     //     closeBtn.style.cursor = 'pointer';
//     //     closeBtn.style.marginBottom = '2%';

//     //     closeBtn.addEventListener('click', closeModal);

//     //     acceptBtn.style.backgroundColor = '#28a745';
//     //     acceptBtn.style.width = '20%';
//     //     acceptBtn.style.color = '#fff';
//     //     // acceptBtn.style.margin = '2%';
//     //     acceptBtn.style.padding = '10px';
//     //     acceptBtn.style.borderRadius = '8px';
//     //     acceptBtn.style.border = 'none';
//     //     acceptBtn.style.cursor = 'pointer';

//     //     declineBtn.style.backgroundColor = '#dc3545';
//     //     declineBtn.style.width = '20%';
//     //     declineBtn.style.color = '#fff';
//     //     declineBtn.style.margin = '2%';
//     //     declineBtn.style.padding = '10px';
//     //     declineBtn.style.borderRadius = '8px';
//     //     declineBtn.style.border = 'none';
//     //     declineBtn.style.cursor = 'pointer';


//     //     acceptSelected.style.backgroundColor = '#4fb6dd';
//     //     acceptSelected.style.width = '20%';
//     //     acceptSelected.style.border = '1px solid #000';
//     //     acceptSelected.style.color = '#fff';
//     //     // acceptSelected.style.margin = '2%';
//     //     acceptSelected.style.padding = '10px';
//     //     acceptSelected.style.borderRadius = '8px';
//     //     acceptSelected.style.border = 'none';
//     //     acceptSelected.style.cursor = 'pointer';


//     //     acceptBtn.addEventListener('click', function () {
//     //         const parameter = cookieCategories;
//     //         acceptConsent(parameter);
//     //     });

//     //     acceptSelected.addEventListener('click', function () {
//     //         const parameter = cookieForConsentCreation;
//     //         acceptSelectedConsent(parameter);
//     //     });
//     //     // declineBtn.addEventListener('click', function() {
//     //     //     const parameter = cookieCategories;
//     //     //     acceptNeccessaryConsent(parameter);
//     //     //     });
//     //     // preferenceBtn.addEventListener('click', function() {
//     //     //     const parameter = cookieCategories;
//     //     //     openPreferences(parameter);
//     //     //     });


//     //     function openModal() {
//     //         modal.style.display = 'block';
//     //     }
//     //     function closeModal() {
//     //         modal.style.display = 'none';
//     //     }

//     // }













//     // function openPreferences(cookies) {
//     //     // 1) Create backdrop + centered modal
//     //     const modal = document.createElement('div');
//     //     modal.id = "preferenceModal";
//     //     Object.assign(modal.style, {
//     //         position: 'fixed',
//     //         top: 0, left: 0, width: '100%', height: '100%',
//     //         background: 'rgba(0,0,0,0.5)',
//     //         display: 'flex', alignItems: 'center', justifyContent: 'center',
//     //         zIndex: 10001,
//     //         overflowY: 'auto'
//     //     });

//     //     // 2) Modal content container
//     //     const modalContent = document.createElement('div');
//     //     Object.assign(modalContent.style, {
//     //         background: '#fff',
//     //         borderRadius: '20px',
//     //         width: '450px',
//     //         maxHeight: '100%',
//     //         padding: '24px',
//     //         boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
//     //         position: 'relative',
//     //         display: 'flex',
//     //         flexDirection: 'column'
//     //     });

//     //     // 3) Close button (×) top‑right
//     //     const closeBtn = document.createElement('span');
//     //     closeBtn.innerHTML = '&times;';
//     //     Object.assign(closeBtn.style, {
//     //         position: 'absolute',
//     //         top: '16px',
//     //         right: '16px',
//     //         fontSize: '20px',
//     //         cursor: 'pointer',
//     //         color: '#666'
//     //     });
//     //     closeBtn.addEventListener('click', () => modal.remove());

//     //     // 4) Header: title
//     //     const header = document.createElement('h2');
//     //     header.innerText = 'Manage preferences';
//     //     Object.assign(header.style, {
//     //         margin: 0,
//     //         marginBottom: '0px',
//     //         fontSize: '20px',
//     //         fontWeight: '600'
//     //     });

//     //     modalContent.appendChild(closeBtn);
//     //     modalContent.appendChild(header);

//     //     // <<< Add this intro paragraph >>>
//     //     const intro = document.createElement('p');
//     //     intro.innerText = "Choose the cookies you allow. You can update your preferences anytime.";
//     //     Object.assign(intro.style, {
//     //         margin: '8px 0 16px',
//     //         fontSize: '14px',
//     //         color: '#555',
//     //         lineHeight: '1.5'
//     //     });
//     //     modalContent.appendChild(intro);

//     //     // 5) Iterate categories
//     //     cookies.forEach((cat, idx) => {
//     //         // a) Section wrapper
//     //         const section = document.createElement('div');
//     //         Object.assign(section.style, {
//     //             marginBottom: '24px'
//     //         });

//     //         //     // b) Section header with toggle
//     //         //     const secHeader = document.createElement('div');
//     //         //     Object.assign(secHeader.style, {
//     //         //         display: 'flex',
//     //         //         justifyContent: 'space-between',
//     //         //         alignItems: 'center',
//     //         //         padding: '8px 12px',
//     //         //         background: '#f1f1f1',
//     //         //         borderRadius: '4px'
//     //         //     });
//     //         //     const title = document.createElement('span');
//     //         //     title.innerText = cat.name;
//     //         //     title.style.fontWeight = '500';

//     //         //     // build the slider toggle
//     //         //     const labelSwitch = document.createElement('label');
//     //         //     labelSwitch.className = 'switch';  // assumes your CSS has .switch/.slider.round
//     //         //     const toggle = document.createElement('input');
//     //         //     toggle.type = 'checkbox';
//     //         //     toggle.checked = (cat.status === 'yes');
//     //         //     toggle.addEventListener('change', () => {
//     //         //         checkTheBox(cat.name, cookies);
//     //         //     });
//     //         //     const slider = document.createElement('span');
//     //         //     slider.className = 'slider round';

//     //         //     labelSwitch.appendChild(toggle);
//     //         //     labelSwitch.appendChild(slider);

//     //         //     // append title + slider
//     //         //     secHeader.appendChild(title);
//     //         //     secHeader.appendChild(labelSwitch);
//     //         //     section.appendChild(secHeader);

//     //         //     // c) Description
//     //         //     if (cat.desc) {
//     //         //         const desc = document.createElement('p');
//     //         //         desc.innerText = cat.desc;
//     //         //         Object.assign(desc.style, {
//     //         //             margin: '8px 0',
//     //         //             fontSize: '14px',
//     //         //             color: '#333'
//     //         //         });
//     //         //         section.appendChild(desc);
//     //         //     }

//     //         //     // d) Cookie table
//     //         //     if (cat.list.length) {
//     //         //         const table = document.createElement('table');
//     //         //         Object.assign(table.style, {
//     //         //             width: '100%',
//     //         //             borderCollapse: 'collapse',
//     //         //             marginTop: '8px'
//     //         //         });
//     //         //         // header row
//     //         //         table.innerHTML = `
//     //         //     <thead>
//     //         //       <tr>
//     //         //         <th style="text-align:left;padding:8px;border-bottom:1px solid #ddd;">Cookie name</th>
//     //         //         <th style="text-align:left;padding:8px;border-bottom:1px solid #ddd;">Description</th>
//     //         //       </tr>
//     //         //     </thead>
//     //         //     <tbody>
//     //         //       ${cat.list.map(c => `
//     //         //         <tr>
//     //         //           <td style="padding:8px;border-bottom:1px solid #eee;">${c.cookieName}</td>
//     //         //           <td style="padding:8px;border-bottom:1px solid #eee;">${c.description}</td>
//     //         //         </tr>`).join('')}
//     //         //     </tbody>
//     //         //   `;
//     //         //         section.appendChild(table);
//     //         //     }

//     //         //     modalContent.appendChild(section);
//     //         // b) Section header with slider toggle
//     //         // ▶ 1) Arrow indicator
//     //         const arrow = document.createElement('span');
//     //         arrow.innerText = '►';                    // right‑pointing triangle
//     //         Object.assign(arrow.style, {
//     //             display: 'inline-block',
//     //             marginRight: '8px',
//     //             transition: 'transform 0.3s',
//     //             fontSize: '12px',
//     //             color: '#555',
//     //             transform: 'rotate(0deg)'
//     //         });
//     //         const secHeader = document.createElement('div');
//     //         Object.assign(secHeader.style, {
//     //             display: 'flex',
//     //             justifyContent: 'space-between',
//     //             alignItems: 'center',
//     //             padding: '8px 12px',
//     //             background: '#f1f1f1',
//     //             borderRadius: '4px',
//     //             cursor: 'pointer'              // make header clickable
//     //         });
//     //         const title = document.createElement('span');
//     //         title.innerText = cat.name;
//     //         title.style.fontWeight = '500';

//     //         // build your .switch/.slider here…
//     //         const labelSwitch = document.createElement('label');
//     //         labelSwitch.className = 'switch';
//     //         const toggle = document.createElement('input');
//     //         toggle.type = 'checkbox';
//     //         toggle.checked = (cat.status === 'yes');
//     //         toggle.addEventListener('change', () => checkTheBox(cat.name, cookies));
//     //         const slider = document.createElement('span');
//     //         slider.className = 'slider round';
//     //         labelSwitch.append(toggle, slider);

//     //         secHeader.append(arrow, title, labelSwitch);
//     //         section.appendChild(secHeader);

//     //         // ▶ collapsible content container (hidden by default)
//     //         const contentDiv = document.createElement('div');
//     //         Object.assign(contentDiv.style, {
//     //             display: 'none',
//     //             padding: '8px 12px',
//     //             border: '1px solid #eee',
//     //             borderTop: 'none',
//     //             borderRadius: '0 0 4px 4px',
//     //             background: '#fafafa'
//     //         });

//     //         // c) Description (inside contentDiv)
//     //         if (cat.desc) {
//     //             const desc = document.createElement('p');
//     //             desc.innerText = cat.desc;
//     //             desc.style.margin = '8px 0';
//     //             contentDiv.appendChild(desc);
//     //         }

//     //         // d) Cookie table (also inside contentDiv)
//     //         if (cat.list.length) {
//     //             const table = document.createElement('table');
//     //             table.style.width = '100%';
//     //             table.style.borderCollapse = 'collapse';
//     //             // header row
//     //             const thead = document.createElement('thead');
//     //             thead.innerHTML = `
//     //     <tr>
//     //       <th style="text-align:left;padding:6px;border-bottom:1px solid #ddd;">Name</th>
//     //       <th style="text-align:left;padding:6px;border-bottom:1px solid #ddd;">Description</th>
//     //     </tr>`;
//     //             table.appendChild(thead);
//     //             // body rows
//     //             const tbody = document.createElement('tbody');
//     //             cat.list.forEach(c => {
//     //                 const tr = document.createElement('tr');
//     //                 tr.innerHTML = `
//     //       <td style="padding:6px;border-bottom:1px solid #eee;">${c.cookieName}</td>
//     //       <td style="padding:6px;border-bottom:1px solid #eee;">${c.description || ''}</td>
//     //     `;
//     //                 tbody.appendChild(tr);
//     //             });
//     //             table.appendChild(tbody);
//     //             contentDiv.appendChild(table);
//     //         }

//     //         section.appendChild(contentDiv);

//     //         // ▶ clicking header toggles its content
//     //         secHeader.addEventListener('click', () => {
//     //             contentDiv.style.display = contentDiv.style.display === 'none' ? 'block' : 'none';
//     //         });

//     //         modalContent.appendChild(section);
//     //     });

//     //     // 6) Footer: Save button
//     //     const footer = document.createElement('div');
//     //     Object.assign(footer.style, {
//     //         display: 'flex',
//     //         justifyContent: 'flex-end',
//     //         gap: '12px'
//     //     });
//     //     const saveBtn = document.createElement('button');
//     //     saveBtn.innerText = 'Save preferences';
//     //     Object.assign(saveBtn.style, {
//     //         padding: '8px 16px',
//     //         background: '#007bff',
//     //         color: '#fff',
//     //         border: 'none',
//     //         borderRadius: '4px',
//     //         cursor: 'pointer',
//     //         fontSize: '14px'
//     //     });
//     //     saveBtn.addEventListener('click', () => {
//     //         // your existing acceptSelectedConsent or composite logic
//     //         const selected = cookieForConsentCreation;
//     //         acceptSelectedConsent(selected);
//     //     });
//     //     footer.appendChild(saveBtn);

//     //     modalContent.appendChild(footer);
//     //     modal.appendChild(modalContent);
//     //     document.body.appendChild(modal);
//     // }


















//     // function openPreferences(cookies) {
//     //     const themeColor = "#007bff";

//     //     // — 0) Inject CSS once to hide the modal’s scrollbar —
//     //     if (!document.getElementById("preferenceModalScrollbarStyle")) {
//     //         const styleTag = document.createElement("style");
//     //         styleTag.id = "preferenceModalScrollbarStyle";
//     //         styleTag.textContent = `
//     //   /* hide scrollbar in our modal-content */
//     //   #preferenceModal > div {
//     //     scrollbar-width: none;           /* Firefox */
//     //     -ms-overflow-style: none;        /* IE10+ */
//     //   }
//     //   #preferenceModal > div::-webkit-scrollbar {
//     //     width: 0;
//     //     height: 0;
//     //   }
//     // `;
//     //         document.head.appendChild(styleTag);
//     //     }

//     //     // — 1) Your category descriptions —
//     //     const CATEGORY_DESCRIPTIONS = {
//     //         Functional: 'These Cookies allow us to analyze your use of the site to evaluate and improve its performance. They may also be used to provide a better customer experience on this site.',
//     //         Analytics: 'These Cookies help us understand how visitors interact with our site by collecting and reporting information anonymously.',
//     //         Marketing: 'These Cookies are used to deliver personalized advertisements based on your interests.',
//     //         Others: 'Other Cookies that don’t fit into the above categories.'
//     //     };

//     //     // — 2) Clean up any existing modal —
//     //     document.getElementById("preferenceModal")?.remove();

//     //     // — 3) Create backdrop & container —
//     //     const modal = document.createElement("div");
//     //     modal.id = "preferenceModal";
//     //     Object.assign(modal.style, {
//     //         position: "fixed", top: 0, left: 0,
//     //         width: "100%", height: "100%",
//     //         backgroundColor: "rgba(0,0,0,0.7)",
//     //         display: "flex", alignItems: "center", justifyContent: "center",
//     //         zIndex: 10000,
//     //     });

//     //     const modalContent = document.createElement("div");
//     //     Object.assign(modalContent.style, {
//     //         backgroundColor: "#fff",
//     //         borderRadius: "32px",
//     //         width: "90%", maxWidth: "550px", maxHeight: "90vh",
//     //         padding: "24px",
//     //         boxShadow: "0 4px 8px rgba(0,0,0,0.2)",
//     //         fontFamily: '"Inter", Arial, sans-serif',
//     //         display: "flex", flexDirection: "column", gap: "16px",
//     //         overflowY: "auto"    // allows internal scrolling
//     //     });
//     //     modal.append(modalContent);

//     //     // — 4) Header & intro —
//     //     const header = document.createElement("div");
//     //     Object.assign(header.style, { display: "flex", justifyContent: "space-between", alignItems: "center" });
//     //     const title = document.createElement("h2");
//     //     title.innerText = "Manage Preferences";
//     //     Object.assign(title.style, { margin: 0, fontSize: "24px", fontWeight: 900 });
//     //     const closeBtn = document.createElement("span");
//     //     closeBtn.innerHTML = "&times;";
//     //     Object.assign(closeBtn.style, { fontSize: "24px", cursor: "pointer" });
//     //     closeBtn.addEventListener("click", () => modal.remove());
//     //     header.append(title, closeBtn);
//     //     modalContent.append(header);

//     //     const intro = document.createElement("p");
//     //     intro.innerText = "Choose the cookies you allow. You can update your preferences anytime.";
//     //     Object.assign(intro.style, { margin: "0", fontSize: "14px", color: "#555", lineHeight: "1.5" });
//     //     modalContent.append(intro);

//     //     // — 5) Build each accordion section —
//     //     function createAccordionSection(cat) {
//     //         const nameKey = cat.name.toLowerCase() === "unclassified" ? "Others" : cat.name;
//     //         const wrapper = document.createElement("div");

//     //         // header row
//     //         const row = document.createElement("div");
//     //         Object.assign(row.style, {
//     //             display: "flex", alignItems: "center",
//     //             padding: "12px", cursor: "pointer",
//     //         });
//     //         const arrow = document.createElement("span");
//     //         Object.assign(arrow.style, {
//     //             display: "inline-block", width: "8px", height: "8px",
//     //             borderBottom: `3px solid ${themeColor}`, borderRight: `3px solid ${themeColor}`,
//     //             transform: "rotate(45deg)", transition: "transform 0.3s",
//     //             marginRight: "12px"
//     //         });
//     //         const lbl = document.createElement("span");
//     //         lbl.innerText = nameKey;
//     //         Object.assign(lbl.style, { fontSize: "16px", fontWeight: "500", flex: "1" });
//     //         const switchLabel = document.createElement("label");
//     //         switchLabel.className = "switch";
//     //         const toggle = document.createElement("input");
//     //         toggle.type = "checkbox";
//     //         toggle.checked = (cat.status === "yes");
//     //         toggle.addEventListener("click", e => e.stopPropagation());
//     //         const slider = document.createElement("span");
//     //         slider.className = "slider round";
//     //         switchLabel.append(toggle, slider);

//     //         row.append(arrow, lbl, switchLabel);
//     //         wrapper.append(row);

//     //         // description (always visible, indented 32px)
//     //         const desc = CATEGORY_DESCRIPTIONS[nameKey];
//     //         if (desc) {
//     //             const p = document.createElement("p");
//     //             p.innerText = desc;
//     //             Object.assign(p.style, {
//     //                 margin: "4px 0 0 32px",
//     //                 fontSize: "12px",
//     //                 color: "#666",
//     //                 lineHeight: "1.4",
//     //                 paddingBottom: "12px",
//     //             });
//     //             wrapper.append(p);

//     //             const hr = document.createElement("hr");
//     //             Object.assign(hr.style, {
//     //                 margin: "0 0 12px 32px",
//     //                 border: "none",
//     //                 borderBottom: "1px solid #a7a2a2",
//     //                 width: "91%"
//     //             });
//     //             wrapper.append(hr);
//     //         }

//     //         // collapsible table (also indented 32px)
//     //         const content = document.createElement("div");
//     //         Object.assign(content.style, {
//     //             display: "none",
//     //             padding: "0 12px 12px 32px",
//     //             fontSize: "14px", color: "#333"
//     //         });

//     //         if (cat.list.length) {
//     //             const tableWrapper = document.createElement("div");
//     //             tableWrapper.style.overflowX = "auto";
//     //             tableWrapper.innerHTML = `
//     //     <table style="width:100%; table-layout: fixed; border-collapse:collapse; border:1px solid #ddd;">
//     //       <colgroup>
//     //         <col style="width:30%">
//     //         <col style="width:70%">
//     //       </colgroup>
//     //       <thead style="background-color: #000000; color: #ffffff;">
//     //         <tr>
//     //           <th style="text-align:left; padding:8px; border-bottom:1px solid #ddd;">Cookie</th>
//     //           <th style="text-align:left; padding:8px; border-bottom:1px solid #ddd;">Description</th>
//     //         </tr>
//     //       </thead>
//     //       <tbody>
//     //         ${cat.list.map(c => `
//     //           <tr>
//     //             <td style="padding:8px; border-bottom:1px solid #eee; overflow-wrap:anywhere;">${c.cookieName}</td>
//     //             <td style="padding:8px; border-bottom:1px solid #eee; overflow-wrap:anywhere;">${c.description || "-"}</td>
//     //           </tr>
//     //         `).join("")}
//     //       </tbody>
//     //     </table>
//     //   `;
//     //             content.append(tableWrapper);
//     //         } else {
//     //             content.innerHTML = `<p style="margin:0;font-size:14px;color:#666">No cookies in this category.</p>`;
//     //         }

//     //         row.addEventListener("click", () => {
//     //             const open = content.style.display === "block";
//     //             content.style.display = open ? "none" : "block";
//     //             arrow.style.transform = open ? "rotate(45deg)" : "rotate(-135deg)";
//     //         });

//     //         wrapper.append(content);
//     //         return wrapper;
//     //     }

//     //     // append in order (Others last)
//     //     [
//     //         ...cookies.filter(c => c.name.toLowerCase() !== 'unclassified'),
//     //         ...cookies.filter(c => c.name.toLowerCase() === 'unclassified')
//     //     ].forEach(cat => modalContent.append(createAccordionSection(cat)));

//     //     // — 6) Footer & Save button —
//     //     const footer = document.createElement("div");
//     //     Object.assign(footer.style, {
//     //         display: "flex",
//     //         justifyContent: "flex-end",
//     //         marginTop: "24px"
//     //     });

//     //     const saveBtn = document.createElement("button");
//     //     saveBtn.innerText = "Save Preferences";
//     //     saveBtn.classList.add("accept-all-btn");

//     //     // apply your custom style:
//     //     Object.assign(saveBtn.style, {
//     //         width: "40%",          // keeps your desired width
//     //         padding: "10px 0",
//     //         fontSize: "1rem",
//     //         fontWeight: "bold",
//     //         border: "none",
//     //         borderRadius: ".1875rem",
//     //         transition: "background-color 0.2s"
//     //     });

//     //     // force the two you care about:
//     //     saveBtn.style.setProperty("color", "#fff", "important");
//     //     saveBtn.style.setProperty("background-color", "#000", "important");
//     //     saveBtn.style.setProperty("border", "none", "important");

//     //     saveBtn.addEventListener("click", async () => {
//     //         await acceptSelectedConsent(cookies);
//     //         modal.remove();
//     //     });

//     //     footer.append(saveBtn);
//     //     modalContent.append(footer);


//     //     document.body.append(modal);
//     // }


//     async function acceptConsent(cookies) {
//         console.log("accepting", JSON.stringify(cookies));
//         // mark all as yes
//         cookies.forEach(c => c.status = "yes");

//         const requestConsent = {
//             status: "ACTIVE",
//             userDetails: { ip: "127.0.0.1", country: "india" },
//             cookiesCategories: cookies
//         };

//         try {
//             const response = await fetch(crCon001, {
//                 method: "POST",
//                 body: JSON.stringify(requestConsent),
//                 headers: {
//                     "Content-Type": "application/json",
//                     "Authorization": "Bearer " + accessToken,
//                     "SubscriptionKey": "2778ed2e60b94b88aa3563206d5f2b28",
//                     "clientId": clId
//                 }
//             });
//             const ckrpJsn = await response.json();

//             if (ckrpJsn && ckrpJsn.cookieConsentId) {
//                 hideAllConsentUI();
//                 showToast("Your Consent is captured!");
//             } else {
//                 showToast("Your Consent could not be captured!");
//             }
//         } catch (e) {
//             console.error(e);
//             showToast("An error occurred. Please try again.");
//         }
//     }

//     async function acceptNeccessaryConsent(cookies) {
//         // mark only Strictly Necessary as yes
//         cookies.forEach(c => {
//             c.status = (c.name === "Strictly Necessary Cookies") ? "yes" : "no";
//         });

//         const requestConsent = {
//             status: "ACTIVE",
//             userDetails: { ip: "127.0.0.1", country: "india" },
//             cookiesCategories: cookies
//         };

//         try {
//             const response = await fetch(crCon001, {
//                 method: "POST",
//                 body: JSON.stringify(requestConsent),
//                 headers: {
//                     "Content-Type": "application/json",
//                     "Authorization": "Bearer " + accessToken,
//                     "SubscriptionKey": "2778ed2e60b94b88aa3563206d5f2b28",
//                     "clientId": clId
//                 }
//             });
//             const ckrpJsn = await response.json();

//             if (ckrpJsn && ckrpJsn.cookieConsentId) {
//                 hideAllConsentUI();
//                 showToast("Your Consent is captured!");
//             } else {
//                 showToast("Your Consent could not be captured!");
//             }
//         } catch (e) {
//             console.error(e);
//             showToast("An error occurred. Please try again.");
//         }
//     }





//     function openPreferences(cookies) {
//         const themeColor = "#007bff";
//         const theme = window.__cookieTheme || { button: { backgroundColor: "#000" } };

//         // — 0) Hide the modal’s scrollbar —
//         if (!document.getElementById("preferenceModalScrollbarStyle")) {
//             const styleTag = document.createElement("style");
//             styleTag.id = "preferenceModalScrollbarStyle";
//             styleTag.textContent = `
//       #preferenceModal > div { scrollbar-width:none; -ms-overflow-style:none; }
//       #preferenceModal > div::-webkit-scrollbar { width:0; height:0; }
//     `;
//             document.head.appendChild(styleTag);
//         }

//         // — 0b) Force the modal to use the themed font everywhere (title, intro, descriptions, etc.) —
//         if (!document.getElementById("preferenceModalFontStyle")) {
//             const fontTag = document.createElement("style");
//             fontTag.id = "preferenceModalFontStyle";
//             fontTag.textContent = `
//       #preferenceModal, #preferenceModal * {
//         font-family: var(--cc-font, 'GoogleSansCode', Arial, sans-serif) !important;
//       }
//     `;
//             document.head.appendChild(fontTag);
//         }

//         // — 1) Descriptions —
//         const CATEGORY_DESCRIPTIONS = {
//             Functional: 'These Cookies allow us to analyze your use of the site to evaluate and improve its performance. They may also be used to provide a better customer experience on this site.',
//             Analytics: 'These Cookies help us understand how visitors interact with our site by collecting and reporting information anonymously.',
//             Marketing: 'These Cookies are used to deliver personalized advertisements based on your interests.',
//             Others: 'Other Cookies that don’t fit into the above categories.'
//         };

//         // — 2) Clean up any existing modal —
//         document.getElementById("preferenceModal")?.remove();

//         // — 3) Backdrop & container —
//         const modal = document.createElement("div");
//         modal.id = "preferenceModal";
//         Object.assign(modal.style, {
//             position: "fixed", top: 0, left: 0, width: "100%", height: "100%",
//             backgroundColor: "rgba(0,0,0,0.7)",
//             display: "flex", alignItems: "center", justifyContent: "center",
//             zIndex: 10000
//         });

//         const modalContent = document.createElement("div");
//         Object.assign(modalContent.style, {
//             backgroundColor: "#fff",
//             borderRadius: "32px",
//             width: "90%", maxWidth: "550px", maxHeight: "90vh",
//             padding: "24px",
//             boxShadow: "0 4px 8px rgba(0,0,0,0.2)",
//             display: "flex", flexDirection: "column", gap: "16px",
//             overflowY: "auto"
//         });
//         modal.append(modalContent);

//         // — 4) Header & intro —
//         const header = document.createElement("div");
//         Object.assign(header.style, { display: "flex", justifyContent: "space-between", alignItems: "center" });

//         const title = document.createElement("h2");
//         title.innerText = "Manage Preferences";
//         Object.assign(title.style, { margin: 0, fontSize: "24px", fontWeight: 900 });

//         const closeBtn = document.createElement("span");
//         closeBtn.innerHTML = "&times;";
//         Object.assign(closeBtn.style, { fontSize: "24px", cursor: "pointer" });
//         closeBtn.addEventListener("click", () => modal.remove());
//         header.append(title, closeBtn);
//         modalContent.append(header);

//         const intro = document.createElement("p");
//         intro.innerText = "Choose the cookies you allow. You can update your preferences anytime.";
//         Object.assign(intro.style, { margin: "0", fontSize: "14px", color: "#555", lineHeight: "1.5" });
//         modalContent.append(intro);

//         // — 5) Accordion sections —
//         function createAccordionSection(cat) {
//             const nameKey = cat.name.toLowerCase() === "unclassified" ? "Others" : cat.name;
//             const wrapper = document.createElement("div");

//             const row = document.createElement("div");
//             Object.assign(row.style, { display: "flex", alignItems: "center", padding: "12px", cursor: "pointer" });

//             const arrow = document.createElement("span");
//             Object.assign(arrow.style, {
//                 display: "inline-block", width: "8px", height: "8px",
//                 borderBottom: `3px solid ${themeColor}`, borderRight: `3px solid ${themeColor}`,
//                 transform: "rotate(45deg)", transition: "transform 0.3s", marginRight: "12px"
//             });

//             const lbl = document.createElement("span");
//             lbl.innerText = nameKey;
//             Object.assign(lbl.style, { fontSize: "16px", fontWeight: "500", flex: "1" });

//             const switchLabel = document.createElement("label");
//             switchLabel.className = "switch";
//             const toggle = document.createElement("input");
//             toggle.type = "checkbox";
//             toggle.checked = (cat.status === "yes");
//             toggle.addEventListener("click", e => e.stopPropagation());
//             const slider = document.createElement("span");
//             slider.className = "slider round";
//             switchLabel.append(toggle, slider);

//             row.append(arrow, lbl, switchLabel);
//             wrapper.append(row);

//             const desc = CATEGORY_DESCRIPTIONS[nameKey];
//             if (desc) {
//                 const p = document.createElement("p");
//                 p.innerText = desc;
//                 Object.assign(p.style, {
//                     margin: "4px 0 0 32px",
//                     fontSize: "12px",
//                     color: "#666",
//                     lineHeight: "1.4",
//                     paddingBottom: "12px"
//                 });
//                 wrapper.append(p);

//                 const hr = document.createElement("hr");
//                 Object.assign(hr.style, {
//                     margin: "0 0 12px 32px",
//                     border: "none",
//                     borderBottom: "1px solid #a7a2a2",
//                     width: "91%"
//                 });
//                 wrapper.append(hr);
//             }

//             const content = document.createElement("div");
//             Object.assign(content.style, { display: "none", padding: "0 12px 12px 32px", fontSize: "14px", color: "#333" });

//             if (cat.list.length) {
//                 const tableWrapper = document.createElement("div");
//                 tableWrapper.style.overflowX = "auto";
//                 tableWrapper.innerHTML = `
//         <table style="width:100%; table-layout:fixed; border-collapse:collapse; border:1px solid #ddd;">
//           <colgroup><col style="width:30%"><col style="width:70%"></colgroup>
//           <thead style="background-color:#000; color:#fff;">
//             <tr><th style="text-align:left; padding:8px; border-bottom:1px solid #ddd;">Cookie</th>
//                 <th style="text-align:left; padding:8px; border-bottom:1px solid #ddd;">Description</th></tr>
//           </thead>
//           <tbody>
//             ${cat.list.map(c => `
//               <tr>
//                 <td style="padding:8px; border-bottom:1px solid #eee; overflow-wrap:anywhere;">${c.cookieName}</td>
//                 <td style="padding:8px; border-bottom:1px solid #eee; overflow-wrap:anywhere;">${c.description || "-"}</td>
//               </tr>`).join("")}
//           </tbody>
//         </table>`;
//                 content.append(tableWrapper);
//             } else {
//                 content.innerHTML = `<p style="margin:0;font-size:14px;color:#666">No cookies in this category.</p>`;
//             }

//             row.addEventListener("click", () => {
//                 const open = content.style.display === "block";
//                 content.style.display = open ? "none" : "block";
//                 arrow.style.transform = open ? "rotate(45deg)" : "rotate(-135deg)";
//             });

//             wrapper.append(content);
//             return wrapper;
//         }

//         [
//             ...cookies.filter(c => c.name.toLowerCase() !== "unclassified"),
//             ...cookies.filter(c => c.name.toLowerCase() === "unclassified")
//         ].forEach(cat => modalContent.append(createAccordionSection(cat)));

//         // — 6) Footer & Save button —
//         const footer = document.createElement("div");
//         Object.assign(footer.style, { display: "flex", justifyContent: "flex-end", marginTop: "24px" });

//         const saveBtn = document.createElement("button");
//         saveBtn.innerText = "Save Preferences";
//         saveBtn.classList.add("accept-all-btn");
//         Object.assign(saveBtn.style, {
//             width: "40%", padding: "10px 0", fontSize: "1rem", fontWeight: "bold",
//             border: "none", borderRadius: "25px", transition: "background-color 0.2s"
//         });
//         // keep text color; set BG from theme
//         saveBtn.style.setProperty("color", "#fff", "important");
//         saveBtn.style.setProperty("border", "none", "important");
//         saveBtn.style.setProperty("background-color", theme.button?.backgroundColor || "#000", "important");

//         saveBtn.addEventListener("click", async () => {
//             await acceptSelectedConsent(cookies);
//             modal.remove();
//         });

//         footer.append(saveBtn);
//         modalContent.append(footer);
//         document.body.append(modal);
//     }













//     function checkTheBox(value, cookies) {
//         // showToast("the value that came in checkTheBox : "+value)
//         for (let singleCookie of cookies) {

//             if (singleCookie.name == value) {
//                 // showToast("in here for"+singleCookie.name)
//                 singleCookie.status = 'YES';
//                 cookieForConsentCreation.push(singleCookie);
//             }

//         }
//     }

//     async function acceptSelectedConsent(cookiesDetail) {
//         console.log(
//             "cookiesDetail selected ===============> : " +
//             JSON.stringify(cookiesDetail)
//         );

//         // 1) Build payload
//         const requestConsent = {
//             status: "ACTIVE",
//             userDetails: {
//                 ip: "127.0.0.1",
//                 country: "india",
//             },
//             cookiesCategories: cookiesDetail,
//         };

//         try {
//             // 2) Send the consent to your API
//             const response = await fetch(crCon001, {
//                 method: "POST",
//                 body: JSON.stringify(requestConsent),
//                 headers: {
//                     "Content-Type": "application/json",
//                     Authorization: "Bearer " + accessToken,
//                     SubscriptionKey: "2778ed2e60b94b88aa3563206d5f2b28",
//                     clientId: clId,
//                 },
//             });

//             // 3) Parse the JSON
//             const ckrpJsn = await response.json();

//             // 4) If we got an ID back, treat as success
//             if (ckrpJsn.cookieConsentId) {
//                 // remove the preferences modal
//                 const pref = document.getElementById("preferenceModal");
//                 if (pref) pref.remove();

//                 // hide any banners/tooltips
//                 const low = document.getElementById("cookieBannerLow");
//                 if (low) low.style.display = "none";
//                 const tip = document.getElementById("cookieTooltip");
//                 if (tip) tip.style.display = "none";
//                 const modal = document.getElementById("cookieModal");
//                 if (modal) modal.style.display = "none";

//                 showToast("Your Consent is captured!");
//             } else {
//                 // API returned no ID
//                 showToast("Your Consent could not be captured!");
//             }
//         } catch (err) {
//             // network error, JSON error, etc.
//             console.error("Error submitting consent:", err);
//             showToast("An error occurred. Please try again.");
//         }
//     }


// })();





// cookieConsent.js
// Self-contained cookie consent UI loader.
// Requires window.__CookieConsentConfig = { baseUrl, tenantId, token, scanId }
// baseUrl example: "https://api.dscoe.jiolabs.com:8443/cookie"

(function () {
  if (window.__cookieConsentInitialized) return;
  window.__cookieConsentInitialized = true;

  // ---------- Config & validation ----------
  const DEFAULT_CONFIG = { baseUrl: null, tenantId: null, token: null, scanId: null, subscriptionKey: null };
  const cfgFromWindow = (window.__CookieConsentConfig && typeof window.__CookieConsentConfig === 'object') ? window.__CookieConsentConfig : {};
  const currentScript = document.currentScript || (function () {
    const s = document.getElementsByTagName('script');
    return s[s.length - 1];
  })();
  const cfgFromAttrs = {};
  if (currentScript) {
    cfgFromAttrs.baseUrl = currentScript.getAttribute('data-base-url') || undefined;
    cfgFromAttrs.tenantId = currentScript.getAttribute('data-tenant-id') || undefined;
    cfgFromAttrs.token = currentScript.getAttribute('data-token') || undefined;
    cfgFromAttrs.scanId = currentScript.getAttribute('data-scan-id') || undefined;
    cfgFromAttrs.subscriptionKey = currentScript.getAttribute('data-subscription-key') || undefined;
  }
  const cfg = Object.assign({}, DEFAULT_CONFIG, cfgFromAttrs, cfgFromWindow);

  if (!cfg.baseUrl || !cfg.tenantId || !cfg.token || !cfg.scanId) {
    console.error('[cookieConsent] missing required configuration. Provide baseUrl, tenantId, token, scanId in window.__CookieConsentConfig or script data- attributes.');
    return;
  }

  const BASE = cfg.baseUrl.replace(/\/+$/, '');
  const TEMPLATE_GET    = `${BASE}/cookie-templates/tenant?scanId=${encodeURIComponent(cfg.scanId)}`; // GET template
  const CONSENT_POST    = `${BASE}/v1.4/consent/createCookiesConsent`;                                 // POST consent
  const HEADERS = {
    'Content-Type': 'application/json',
    'x-tenant-id': cfg.tenantId,
    'Authorization': `Bearer ${cfg.token}`
  };
  if (cfg.subscriptionKey) HEADERS['SubscriptionKey'] = cfg.subscriptionKey;

  // ---------- Helpers ----------
  function el(tag, attrs = {}, ...children) {
    const d = document.createElement(tag);
    for (const k in attrs) {
      if (k === 'style' && typeof attrs[k] === 'object') Object.assign(d.style, attrs[k]);
      else if (k.startsWith('on') && typeof attrs[k] === 'function') d.addEventListener(k.slice(2), attrs[k]);
      else if (k === 'class') d.className = attrs[k];
      else d.setAttribute(k, attrs[k]);
    }
    children.forEach(ch => {
      if (ch == null) return;
      if (typeof ch === 'string' || typeof ch === 'number') d.appendChild(document.createTextNode(String(ch)));
      else d.appendChild(ch);
    });
    return d;
  }

  function showToast(msg, timeout = 3000) {
    const id = 'cc-toast-container';
    let container = document.getElementById(id);
    if (!container) {
      container = el('div', { id, style: { position: 'fixed', top: '16px', right: '16px', zIndex: 200000 }});
      document.body.appendChild(container);
    }
    const t = el('div', { style: { background: 'rgba(0,0,0,0.85)', color: '#fff', padding: '10px 14px', borderRadius: '8px', marginTop: '8px', fontSize: '14px', boxShadow: '0 6px 18px rgba(0,0,0,0.2)'}}, msg);
    container.appendChild(t);
    setTimeout(() => {
      t.style.transition = 'opacity 0.25s ease';
      t.style.opacity = '0';
      setTimeout(() => t.remove(), 250);
    }, timeout);
  }

  async function safeFetch(url, opts = {}) {
    const merged = Object.assign({ headers: HEADERS }, opts);
    const res = await fetch(url, merged);
    if (!res.ok) {
      const txt = await res.text().catch(() => '');
      const err = new Error(`Request failed ${res.status} ${txt}`);
      err.status = res.status;
      throw err;
    }
    return res.json().catch(() => ({}));
  }

  // ---------- Inject CSS ----------
  (function insertCSS() {
    const css = `
/* Cookie consent styles */
.cc-banner, #cookieModalOverlay, #cc-preferences-modal { font-family: Inter, system-ui, -apple-system, "Segoe UI", Roboto, Arial; }
.cc-banner {
  position: fixed; bottom: 0; left: 0; right: 0; background: #fff; border-top: 1px solid rgba(16,24,40,0.06);
  box-shadow: 0 -8px 24px rgba(2,6,23,0.06); padding: 18px; z-index: 100000; display:flex; gap:18px; align-items:center; justify-content:space-between;
}
.cc-banner .cc-text { flex:1; min-width:0; }
.cc-banner h4 { margin:0 0 6px 0; font-size:15px; font-weight:700; color:#0f172a; }
.cc-banner p { margin:0; color:#6b7280; font-size:13px; line-height:1.4; }
.cc-banner .cc-actions { display:flex; gap:10px; align-items:center; }
.cc-btn { padding:10px 14px; border-radius:22px; font-weight:700; cursor:pointer; border:1px solid transparent; min-width:120px; }
.cc-btn.ghost { background:transparent; color:#0f172a; border:1px solid rgba(15,23,42,0.06); }
.cc-btn.primary { background:#0f3cc9; color:#fff; border:1px solid #0f3cc9; }
.cc-btn.outline { background:#fff; color:#0f3cc9; border:1px solid #0f3cc9; }

#cookieModalOverlay { position:fixed; inset:0; background:rgba(0,0,0,0.6); display:flex; align-items:center; justify-content:center; z-index:100001; }
#cookieModal { width:92%; max-width:720px; background:#fff; border-radius:18px; box-shadow:0 18px 60px rgba(2,6,23,0.4); max-height:86vh; overflow:auto; padding:20px; position:relative;}
#cookieModal .close-x { position:absolute; right:12px; top:12px; font-size:22px; cursor:pointer; color:#374151; }

.cc-modal-header { margin-bottom: 12px; }
.cc-modal-header h2 { margin:0; font-size:20px; font-weight:800; color:#0f172a; }
.cc-modal-intro { color:#6b7280; margin-top:8px; font-size:13px; }

.cc-accordion { margin-top:14px; display:flex; flex-direction:column; gap:12px; }
.cc-section { border:1px solid #eef2ff; border-radius:12px; overflow:hidden; background:#fff; }
.cc-section .cc-section-head { display:flex; align-items:center; justify-content:space-between; padding:12px 14px; cursor:pointer; gap:12px; }
.cc-section .cc-section-head .title { font-weight:700; color:#0f172a; }
.cc-section .cc-section-body { display:none; padding:12px 14px; border-top:1px solid #f3f4f6; background:#fff; }
.cc-section.open .cc-section-body { display:block; }
.cc-cookie-table { width:100%; border-collapse:collapse; margin-top:8px; }
.cc-cookie-table th, .cc-cookie-table td { text-align:left; padding:8px; border-bottom:1px solid #f3f4f6; font-size:13px; color:#374151; }
.cc-toggle { display:flex; align-items:center; gap:8px; }

.cc-footer { display:flex; justify-content:flex-end; gap:12px; margin-top:16px; padding-top:8px; border-top:1px solid #f3f4f6; }
@media (max-width:520px) {
  .cc-banner { flex-direction: column; align-items: stretch; gap: 12px; }
  .cc-banner .cc-actions { justify-content: space-between; }
  #cookieModal { width:95%; padding:16px; border-radius:12px; }
}
`;
    const s = document.createElement('style');
    s.textContent = css;
    document.head.appendChild(s);
  })();

  // ---------- State ----------
  let templateData = null;
  let cookiesByCategory = [];
  let selectedCookiesForConsent = []; // array of cookie items picked (status=yes)
  let bannerEl = null;
  let modalOverlayEl = null;

  // ---------- Normalizer ----------
  function normalizeTemplate(resp) {
    if (!resp) return { cookiesDetails: [] };
    // try several fields where cookies might live
    const cookiesArr = resp.cookiesDetails || resp.cookies || (resp.preferenceCenter && resp.preferenceCenter.cookies) || [];
    const map = {};
    cookiesArr.forEach(c => {
      const cat = (c.category || c.group || 'Unclassified').trim();
      if (!map[cat]) map[cat] = { name: cat, status: c.status || 'no', desc: c.description || c.desc || '', list: [] };
      map[cat].list.push({
        cookieName: c.cookieName || c.name || '',
        description: c.description || c.desc || '',
        domain: c.domain || c.host || '',
        retentionPeriod: c.retentionPeriod || c.retention || c.expires || '',
        category: cat
      });
    });
    return Object.assign({}, resp, { cookiesDetails: Object.values(map) });
  }

  // ---------- Rendering ----------
  function renderBanner() {
    if (bannerEl) bannerEl.remove();
    bannerEl = el('div', { class: 'cc-banner', id: 'cc-banner' });

    const title = (templateData && templateData.multilingual && templateData.multilingual.languageSpecificContentMap && templateData.multilingual.languageSpecificContentMap.ENGLISH && (templateData.multilingual.languageSpecificContentMap.ENGLISH.label || templateData.multilingual.languageSpecificContentMap.ENGLISH.title)) || 'This site uses cookies to make your experience better.';
    const desc = (templateData && templateData.multilingual && templateData.multilingual.languageSpecificContentMap && templateData.multilingual.languageSpecificContentMap.ENGLISH && (templateData.multilingual.languageSpecificContentMap.ENGLISH.description || templateData.multilingual.languageSpecificContentMap.ENGLISH.permissionText)) || 'We use essential cookies to make our site work. With your consent, we may also use non-essential cookies to improve user experience.';

    const text = el('div', { class: 'cc-text' },
      el('h4', {}, title),
      el('p', {}, desc)
    );

    const actions = el('div', { class: 'cc-actions' });
    const manageBtn = el('button', { class: 'cc-btn ghost', onclick: openPreferencesModal }, 'Manage preferences');
    const rejectBtn = el('button', { class: 'cc-btn outline', onclick: acceptNecessary }, 'Reject all');
    const acceptAllBtn = el('button', { class: 'cc-btn primary', onclick: acceptAll }, 'Accept all');

    actions.appendChild(manageBtn);
    actions.appendChild(rejectBtn);
    actions.appendChild(acceptAllBtn);

    bannerEl.appendChild(text);
    bannerEl.appendChild(actions);

    document.body.appendChild(bannerEl);
  }

  function buildPreferencesModal() {
    if (modalOverlayEl) modalOverlayEl.remove();
    modalOverlayEl = el('div', { id: 'cookieModalOverlay' });
    const modal = el('div', { id: 'cookieModal', role: 'dialog', 'aria-modal': 'true' });

    const closeX = el('div', { class: 'close-x', onclick: () => modalOverlayEl.remove() }, '✕');
    modal.appendChild(closeX);

    modal.appendChild(el('div', { class: 'cc-modal-header' }, el('h2', {}, 'Manage preferences')));
    modal.appendChild(el('div', { class: 'cc-modal-intro' }, 'Choose the cookies you allow. You can update your preferences anytime.'));

    const accordion = el('div', { class: 'cc-accordion', id: 'cc-accordion' });

    cookiesByCategory.forEach((cat, idx) => {
      const section = el('div', { class: 'cc-section', 'data-idx': idx });
      const head = el('div', { class: 'cc-section-head' });
      const title = el('div', { class: 'title' }, cat.name);
      const toggleWrap = el('div', { class: 'cc-toggle' });
      const toggle = el('input', { type: 'checkbox' });
      toggle.checked = (cat.status && cat.status.toString().toLowerCase() === 'yes');
      toggle.addEventListener('change', (e) => {
        const checked = !!e.target.checked;
        cat.status = checked ? 'yes' : 'no';
        if (checked) {
          cat.list.forEach(c => {
            if (!selectedCookiesForConsent.find(x => x.cookieName === c.cookieName)) selectedCookiesForConsent.push(Object.assign({ status: 'yes' }, c));
          });
        } else {
          selectedCookiesForConsent = selectedCookiesForConsent.filter(sc => !cat.list.some(cc => cc.cookieName === sc.cookieName));
        }
      });
      toggleWrap.appendChild(toggle);
      head.appendChild(title);
      head.appendChild(toggleWrap);

      const body = el('div', { class: 'cc-section-body' });
      if (cat.desc) body.appendChild(el('div', { style: { color: '#6b7280', marginBottom: '8px' } }, cat.desc));

      const table = el('table', { class: 'cc-cookie-table' });
      const thead = el('thead', {}, el('tr', {}, el('th', {}, 'Cookie name'), el('th', {}, 'Description')));
      const tbody = el('tbody');
      cat.list.forEach(c => {
        const tr = el('tr', {});
        const td1 = el('td', {}, c.cookieName || '-');
        const td2 = el('td', {}, c.description || '-');
        tr.appendChild(td1);
        tr.appendChild(td2);

        tr.addEventListener('click', () => {
          const idx = selectedCookiesForConsent.findIndex(x => x.cookieName === c.cookieName);
          if (idx >= 0) {
            selectedCookiesForConsent.splice(idx, 1);
            tr.style.opacity = '0.6';
          } else {
            selectedCookiesForConsent.push(Object.assign({ status: 'yes' }, c));
            tr.style.opacity = '1';
          }
        });

        tbody.appendChild(tr);
      });
      table.appendChild(thead);
      table.appendChild(tbody);
      body.appendChild(table);

      section.appendChild(head);
      section.appendChild(body);
      accordion.appendChild(section);

      head.addEventListener('click', () => section.classList.toggle('open'));
    });

    modal.appendChild(accordion);

    const footer = el('div', { class: 'cc-footer' });
    const cancelBtn = el('button', { class: 'cc-btn ghost', onclick: () => modalOverlayEl.remove() }, 'Cancel');
    const saveBtn = el('button', { class: 'cc-btn primary', onclick: async () => { await submitSelectedPreferences(); modalOverlayEl.remove(); } }, 'Save preferences');
    footer.appendChild(cancelBtn);
    footer.appendChild(saveBtn);
    modal.appendChild(footer);

    modalOverlayEl.appendChild(modal);
    document.body.appendChild(modalOverlayEl);
  }

  // ---------- Actions & POST ----------
  function hideAllConsentUI() {
    document.getElementById('cc-banner')?.remove();
    document.getElementById('cookieModalOverlay')?.remove();
  }

  async function postConsent(cookiesArray) {
    // transform cookies array into categories if backend expects categories
    const byCat = {};
    (cookiesArray || []).forEach(c => {
      const cat = c.category || 'Unclassified';
      if (!byCat[cat]) byCat[cat] = { name: cat, status: 'no', list: [] };
      byCat[cat].list.push(Object.assign({}, c));
      if (c.status && (String(c.status).toLowerCase() === 'yes')) byCat[cat].status = 'yes';
    });
    // include any categories that exist but were not in cookiesArray (treat as no)
    cookiesByCategory.forEach(cat => {
      if (!byCat[cat.name]) byCat[cat.name] = { name: cat.name, status: cat.status || 'no', list: cat.list.map(l => Object.assign({}, l)) };
    });

    const payload = {
      status: 'ACTIVE',
      userDetails: { ip: '0.0.0.0', country: '' },
      cookiesCategories: Object.values(byCat)
    };

    return safeFetch(CONSENT_POST, { method: 'POST', body: JSON.stringify(payload) });
  }

  async function acceptAll() {
    const cloned = JSON.parse(JSON.stringify(cookiesByCategory));
    selectedCookiesForConsent = [];
    cloned.forEach(cat => {
      cat.status = 'yes';
      cat.list.forEach(c => {
        c.status = 'yes';
        selectedCookiesForConsent.push(c);
      });
    });
    try {
      await postConsent(selectedCookiesForConsent);
      showToast('Your Consent is captured!');
      hideAllConsentUI();
    } catch (e) {
      console.error('acceptAll failed', e);
      showToast('Failed to capture consent.');
    }
  }

  async function acceptNecessary() {
    selectedCookiesForConsent = [];
    cookiesByCategory.forEach(cat => {
      const necessary = /required|strictly necessary|necessary/i.test(cat.name);
      cat.status = necessary ? 'yes' : 'no';
      cat.list.forEach(c => {
        c.status = necessary ? 'yes' : 'no';
        if (necessary) selectedCookiesForConsent.push(c);
      });
    });
    try {
      await postConsent(selectedCookiesForConsent);
      showToast('Necessary consent saved.');
      hideAllConsentUI();
    } catch (e) {
      console.error('acceptNecessary failed', e);
      showToast('Failed to capture necessary consent.');
    }
  }

  async function submitSelectedPreferences() {
    try {
      await postConsent(selectedCookiesForConsent);
      showToast('Preferences saved.');
      hideAllConsentUI();
    } catch (e) {
      console.error('submitSelectedPreferences failed', e);
      showToast('Failed to save preferences.');
      throw e;
    }
  }

  function openPreferencesModal() {
    buildPreferencesModal();
  }

  // ---------- Initialization: fetch template ----------
  async function init() {
    try {
      const resp = await safeFetch(TEMPLATE_GET, { method: 'GET' });
      templateData = normalizeTemplate(resp);
      // map into cookiesByCategory
      cookiesByCategory = (templateData.cookiesDetails || []).map(c => ({
        name: c.name || c.category || 'Unclassified',
        status: c.status || 'no',
        desc: c.desc || c.description || '',
        list: (c.list || []).map(it => Object.assign({}, it))
      }));
      // preselect those with status yes
      selectedCookiesForConsent = [];
      cookiesByCategory.forEach(cat => {
        if (cat.status && String(cat.status).toLowerCase() === 'yes') {
          cat.list.forEach(c => selectedCookiesForConsent.push(Object.assign({ status: 'yes' }, c)));
        }
      });
      renderBanner();
    } catch (err) {
      console.error('[cookieConsent] failed to load template', err);
      showToast('Failed to load cookie template.');
      // fallback: render empty banner so user can interact
      templateData = { multilingual: { languageSpecificContentMap: { ENGLISH: { label: 'This site uses cookies', description: 'We use cookies to make our site work.' } } }, cookiesDetails: [] };
      cookiesByCategory = [];
      renderBanner();
    }
  }

  // Start
  init();

})();
