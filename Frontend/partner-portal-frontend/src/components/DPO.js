import { ActionButton, InputFieldV2, Text, Notifications } from "../custom-components";
import "../styles/dataProtectionOfficer.css";
import "../styles/pageConfiguration.css";
import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import {
  clearSession,
  getDPODetails,
  submitDPODetails,
  updateDPODetails,
} from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { useNavigate } from "react-router-dom";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";
import ConfigurationShimmer from "./ConfigurationShimmer";

const DPO = () => {
  const [dpoName, setDpoName] = useState("");
  const [dpoemail, setDpoEmail] = useState("");
  const [dpophone, setDpiPhone] = useState("");
  const [dpoaddress, setDpoAddress] = useState("");
  const [readyToSubmit, setReadyToSubmit] = useState(false);
  const [disableSaveBtn, setDiableSaveBtn] = useState(false);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [updateDPO, setUpdateDPO] = useState(false);
  const [dpoConfigId, setDpoConfigId] = useState("");
  const [loading, setLoading] = useState(true);
  var displayDPODetails = {};
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const phoneRegex = /^[6-9]\d{9}$/;
  const addressRegex = /^[A-Za-z0-9].*$/;
  const handleDpoNameChange = (e) => {
    let value = e.target.value;

    // collapse multiple spaces to single
    value = value.replace(/\s+/g, " ");

    // remove special characters (keep alphabets, digits, spaces)
    value = value.replace(/[^a-zA-Z0-9 ]/g, "");

    // trim leading space
    value = value.trimStart();

    setDpoName(value);
  };

  const handleDpoEmailChange = (e) => {
    let value = e.target.value;

    // remove all spaces
    value = value.replace(/\s+/g, "");

    setDpoEmail(value);
  };

  const handleDpoPhoneChange = (e) => {
    const value = e.target.value.replace(/\D/g, ""); // keep digits only
    setDpiPhone(value.slice(0, 10));
  };

  const handleDpoAddressChange = (e) => {
    let value = e.target.value;

    // collapse multiple spaces
    value = value.replace(/\s+/g, " ");

    // trim leading space
    value = value.trimStart();

    //  Require at least one alphabet
    const isValid = /[a-zA-Z]/.test(value);

    console.log("address val: ", value);

    if (isValid) {
      setDpoAddress(value);
    } else {
      setDpoAddress("");
    }
  };

  const handleDpoSubmit = async () => {
    const errors = [];

    // Name validation
    if (!/^(?=.*[A-Za-z])[A-Za-z0-9 ]+$/.test(dpoName.trim())) {
      errors.push("Please Enter Valid Data Protection Officer Name.");
    }

    // Email validation
    const emailRegex =
      /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:\.[a-zA-Z]{2,})*$/;
    if (!emailRegex.test(dpoemail)) {
      errors.push("Please Enter Valid Email.");
    }

    //  Phone validation
    if (!/^\d{10}$/.test(dpophone) || !phoneRegex.test(dpophone)) {
      errors.push("Please Enter Valid Phone Number.");
    }

    // Address validation
    if (!addressRegex.test(dpoaddress.trim()) || dpoaddress.trim() === "") {
      errors.push("Please Enter Valid Address.");
    }

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
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
    } else {
      setReadyToSubmit(true);
      // var res = dispatch(submitDPODetails(dpoName, dpoemail, dpophone));

      try {
        const res = await dispatch(
          submitDPODetails(
            dpoName,
            dpoemail.toUpperCase(),
            dpophone,
            dpoaddress
          )
        );

        if (res.status === 500) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Something went wrong..Please try again later"}
              />
            ),
            { icon: false }
          );
        } else if (res.status === 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings saved successfully."}
              />
            ),
            { icon: false }
          );
        } else if (res.status === 401) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Enter Valid DPO Details"}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        console.log("Error in DPO", error[0]);
        if (error[0]?.errorCode == "JCMP3003") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Data Protection Officer is already exists."}
              />
            ),
            { icon: false }
          );
          setDiableSaveBtn(true);

          return;
        } else if (
          error[0].errorCode == "JCMP4003" ||
          error[0].errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expored"}
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
                message={"something went wrong please try again later"}
              />
            ),
            { icon: false }
          );
          return;
        }
      }
    }
  };

  const handleDpoUpdate = async () => {
    const errors = [];

    // Name validation
    if (!/^(?=.*[A-Za-z])[A-Za-z0-9 ]+$/.test(dpoName.trim())) {
      errors.push("Please Enter Valid Data Protection Officer Name.");
    }

    // Email validation
    const emailRegex =
      /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:\.[a-zA-Z]{2,})*$/;
    if (!emailRegex.test(dpoemail)) {
      errors.push("Please Enter Valid Email.");
    }

    //  Phone validation
    if (!/^\d{10}$/.test(dpophone) || !phoneRegex.test(dpophone)) {
      errors.push("Please Enter Valid Phone Number.");
    }

    // Address validation
    if (!addressRegex.test(dpoaddress.trim()) || dpoaddress.trim() === "") {
      errors.push("Please Enter Valid Address.");
    }

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
    } else if (errors.length == 1) {
      toast.error(
        (props) => <CustomToast {...props} type="error" message={errors[0]} />,
        { icon: false }
      );
    } else {
      setReadyToSubmit(true);
      // var res = dispatch(submitDPODetails(dpoName, dpoemail, dpophone));

      try {
        const res = await dispatch(
          updateDPODetails(dpoName, dpoemail, dpophone, dpoaddress, dpoConfigId)
        );

        if (res.status === 500) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Something went wrong..Please try again later"}
              />
            ),
            { icon: false }
          );
        } else if (res.status === 200) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings updated successfully."}
              />
            ),
            { icon: false }
          );
          setTimeout(() => {
            navigate("/dpo-dashboard");
          }, 1500);
        } else if (res.status === 401) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Enter Valid DPO Details"}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        console.log("Error in DPO", error[0]);
        if (error[0]?.errorCode == "JCMP3003") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Data Protection Officer is already exists."}
              />
            ),
            { icon: false }
          );
          setDiableSaveBtn(true);

          return;
        } else if (
          error[0].errorCode == "JCMP4003" ||
          error[0].errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Session expored"}
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
                message={"something went wrong please try again later"}
              />
            ),
            { icon: false }
          );
          return;
        }
      }
    }
  };

  useEffect(() => {
    const fetchDPO = async () => {
      try {
        setLoading(true);
        const response = await dispatch(getDPODetails());
        console.log("Searched DPO details:", response?.data?.searchList?.[0]);

        const dpoInfo = response?.data?.searchList?.[0]?.configurationJson;
        if (dpoInfo) {
          setDpoName(dpoInfo.name || "");
          setDpoEmail(dpoInfo.email || "");
          setDpiPhone(dpoInfo.mobile || "");
          setDpoAddress(dpoInfo.address || "");
          setUpdateDPO(true);
          setDpoConfigId(response?.data?.searchList?.[0]?.configId);
        } else {
          console.warn("No DPO details found in API response");
        }
      } catch (error) {
        console.error("Failed to fetch DPO details:", error);
        // ✅ Optional: show toast or fallback UI
        // toast.error("Failed to fetch DPO details. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchDPO();
  }, [dispatch]);

  if (loading) {
    return <ConfigurationShimmer />;
  }

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
      <div className="configurePage">
        <div className="dataProtectionOfficer-outer-division">
          <div className="dataProtectionOfficer-header-and-badge">
            <Text appearance="heading-s" color="primary-grey-100">
              Data protection officer (DPO)
            </Text>
            <div className="dataProtectionOfficer-badge">
              <Text appearance="body-xs-bold" color="primary-grey-80">
                Governance
              </Text>
            </div>
          </div>

          <div className="dataProtectionOfficer-inputs-column-div">
            <InputFieldV2
              label="DPO Name (Required)"
              helperText="Full name of DPO"
              className="dataProtectionOfficer-input"
              size="medium"
              onChange={handleDpoNameChange}
              value={dpoName}
              placeholder="Enter"
            />
            <InputFieldV2
              label="Email address (Required)"
              helperText="Official email address of DPO"
              className="dataProtectionOfficer-input"
              size="medium"
              onChange={handleDpoEmailChange}
              value={dpoemail}
              placeholder="Enter"
            />
            <InputFieldV2
              label="Phone number (Required)"
              helperText="Contact number of DPO"
              className="dataProtectionOfficer-input"
              size="medium"
              onChange={handleDpoPhoneChange}
              value={dpophone}
              type="phone"
              maxLength={10}
              placeholder="Enter"
            />
            <InputFieldV2
              label="Address (Required)"
              helperText="Address of DPO"
              className="dataProtectionOfficer-input"
              size="medium"
              onChange={handleDpoAddressChange}
              value={dpoaddress}
              autoComplete
              placeholder="Enter"
            />
          </div>
        </div>

        <div className="content-5">
          <ActionButton
            label="Save"
            onClick={updateDPO ? handleDpoUpdate : handleDpoSubmit}
            state={disableSaveBtn ? "disabled" : "normal"}
          />
        </div>
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
    </>
  );
};

export default DPO;
