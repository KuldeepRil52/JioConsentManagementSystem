import React, { useState, useEffect } from "react";
import "../styles/grievance.css";
import "../styles/pageConfiguration.css";
import "../styles/sessionModal.css";
import "../styles/toast.css";
import { CLEAR_SESSION } from "../store/constants/Constants";
import { useDispatch, useSelector } from "react-redux";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import { IcCalendarWeek } from "../custom-components/Icon";
import {
  Text,
  InputFieldV2,
  InputCheckbox,
  Tabs,
  TabItem,
  TokenProvider,
  Button,
} from "../custom-components";
import { saveGrievanceConfig, getGrievanceDetails, updateGrievanceConfig } from "../store/actions/CommonAction";
import Select from "react-select";
import ConfigurationShimmer from "./ConfigurationShimmer";

const Grievance = () => {
  const dispatch = useDispatch();
  const [endpointUrl, setEndpointUrl] = useState("");
  const [isEndpointUrlValid, setIsEndpointUrlValid] = useState(true);
  const [slaTimeline, setSlaTimeline] = useState({ value: "", unit: "DAYS" });
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [escalationPolicy, setEscalationPolicy] = useState({
    value: "",
    unit: "DAYS",
  });
  const [retentionPolicy, setRetentionPolicy] = useState({
    value: "",
    unit: "",
  });
  const [isEmailChecked, setIsEmailChecked] = useState(true);
  const [isSmsChecked, setIsSmsChecked] = useState(true);
  const [grievanceTypes, setGrievanceTypes] = useState(["ACCESS", "CORRECTION", "DELETION"]);
  const [intakeMethods, setIntakeMethods] = useState(["WEB_FORM"]);
  const [retentionDuration, setRetentionDuration] = useState("YEARS");
  const workflowOptions = [
    { value: "New", label: "New" },
    { value: "In Progress", label: "In Progress" },
    { value: "Resolved", label: "Resolved" },
  ];
  const [workflow, setWorkflow] = useState(workflowOptions.map(opt => opt.value));

  const [slaInput, setSlaInput] = useState("7");
  const [escalationInput, setEscalationInput] = useState("2");
  const [retentionInput, setRetentionInput] = useState("99");

  const tenantId = useSelector((state) => state.common.tenant_id);
  const token = useSelector((state) => state.common.session_token);
  const businessId = useSelector((state) => state.common.business_id);
  const [configId, setConfigId] = useState("");
  const [loading, setLoading] = useState(true);

  const urlRegex = /^(https?:\/\/)?([\w-]+\.)+[\w-]{2,}(\/\S*)?$/;

  const options = [
    { value: "ACCESS", label: "Access" },
    { value: "CORRECTION", label: "Correction" },
    { value: "DELETION", label: "Deletion" },
    { value: "OBJECTION", label: "Objection" },
  ];

  const optionsIntake = [
    { value: "WEB_FORM", label: "Web Form" },
  ];


  const customStyles = {
    control: (base, state) => ({
      ...base,
      color: "#333",
      backgroundColor: "#fff",
      border: "1px solid #ccc",
      borderRadius: "8px",
      padding: "2px 4px",
      fontSize: "14px",
      minHeight: "40px",

      // remove blue glow
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc", // keep same on focus
      "&:hover": {
        borderColor: "#999",
      },
    }),

    valueContainer: (base) => ({
      ...base,
      padding: "0 8px",
      gap: "4px",
    }),

    placeholder: (base) => ({
      ...base,
      color: "#666",
      fontSize: "14px",
    }),

    multiValue: (base) => ({
      ...base,
      borderRadius: "6px",
      backgroundColor: "#f0f0f0",
      padding: "2px 6px",
    }),

    multiValueLabel: (base) => ({
      ...base,
      color: "#333",
      fontSize: "13px",
    }),

    multiValueRemove: (base) => ({
      ...base,
      color: "#666",
      cursor: "pointer",
      ":hover": {
        backgroundColor: "#e0e0e0",
        color: "#000",
      },
    }),

    menu: (base) => ({
      ...base,
      borderRadius: "8px",
      border: "1px solid #ccc",
      marginTop: "4px",
      zIndex: 9999,
    }),

    menuList: (base) => ({
      ...base,
      padding: "4px 0",
    }),

    option: (base, state) => ({
      ...base,
      fontSize: "14px",
      padding: "8px 12px",
      cursor: "pointer",
      backgroundColor: state.isSelected
        ? "#e6f0ff" // selected background
        : state.isFocused
          ? "#f5f5f5" // hover background
          : "#fff",
      color: "#333",
      ":active": {
        backgroundColor: "#e6f0ff",
      },
    }),

    dropdownIndicator: (base) => ({
      ...base,
      color: "#666",
      padding: "0 8px",
      "&:hover": {
        color: "#333",
      },
    }),

    clearIndicator: (base) => ({
      ...base,
      padding: "0 8px",
    }),
  };

  const handleEndpointUrlChange = (e) => {
    const value = e.target.value.trim();
    setEndpointUrl(value);

    if (value === "") {
      setIsEndpointUrlValid(true); // don’t show error when empty
      return;
    }

    if (urlRegex.test(value)) {
      setIsEndpointUrlValid(true);
    } else {
      setIsEndpointUrlValid(false);
    }
  };

  const handleRententionDurationChange = (e) => {
    setRetentionDuration(e.target.value);
  };

  const handleSlaChange = (e) => setSlaInput(e.target.value);
  const handleEscalationChange = (e) => setEscalationInput(e.target.value);

  const handleRetentionChange = (e) => setRetentionInput(e.target.value);


  const handleSubmit = async () => {

    // Get input values
    const endpointUrl = document
      .querySelector("input[name='endpointUrl']")
      .value.trim();

    if (!grievanceTypes || grievanceTypes.length === 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please select a Grievance Type."}
          />
        ),
        { icon: false }
      );

      return;
    }

    if (!intakeMethods || intakeMethods.length === 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please select an Intake Method."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!endpointUrl) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter an Endpoint URL."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!workflow || workflow.length === 0) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please select Workflow Steps."}
          />
        ),
        { icon: false }
      );
      return;
    }

    if (!slaTimeline.value || !slaTimeline.unit)
      return toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter SLA Timelines."}
          />
        ),
        { icon: false }
      );
    if (!escalationPolicy.value || !escalationPolicy.unit)
      return toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter Escalation Policy."}
          />
        ),
        { icon: false }
      );
    if (!retentionPolicy.value || !retentionPolicy.unit)
      return toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please enter Retention Policy."}
          />
        ),
        { icon: false }
      );

    const requestBody = {
      configurationJson: {
        grievanceTypes,
        endpointUrl,
        intakeMethods, // wrap in array
        workflow,
        slaTimeline,
        escalationPolicy,
        retentionPolicy,
        communicationConfig: {
          email: isEmailChecked,
          sms: isSmsChecked,
        },
      },
    };

    const headers = {
      "business-id": businessId,
      "scope-level": "tenant",
      "tenant-id": tenantId,
      "x-session-token": token,
    };

    const putHeaders = {
      "tenant-id": tenantId,
      "business-id": businessId,
      "x-session-token": token,
    };

    if (!configId) {

      try {
        const resp = await dispatch(saveGrievanceConfig(requestBody, headers));
        console.log("Response from saveGrievanceConfig:", resp);
        if (resp.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings saved successfully!"}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        console.error("Error saving configuration:", error);
        if (
          error[0]?.errorCode == "JCMP4003" ||
          error[0]?.errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast {...props} type="error" message={"Session expired"} />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        } else if (error[0]?.errorCode == "JCMP3003") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Greviance Set up is already exists"}
              />
            ),
            { icon: false }
          );
        }
      }

    }
    else {
      try {
        const resp = await dispatch(updateGrievanceConfig(requestBody, putHeaders, configId));
        console.log("Response from updateGrievanceConfig:", resp);
        if (resp.status == 200) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Settings updated successfully!"}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        console.error("Error saving configuration:", error);
        if (
          error[0]?.errorCode == "JCMP4003" ||
          error[0]?.errorCode == "JCMP4001"
        ) {
          toast.error(
            (props) => (
              <CustomToast {...props} type="error" message={"Session expired"} />
            ),
            { icon: false }
          );
          dispatch({ type: CLEAR_SESSION });
          setShowSessionModal(true);
          setTimeout(() => {
            setShowSessionModal(false);
            navigate("/adminLogin");
          }, 7000);
        } else if (error[0]?.errorCode == "JCMP3003") {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Greviance Set up is already exists"}
              />
            ),
            { icon: false }
          );
        }
      }
    }


  };

  const format = ({ value, unit }) => {
    if (!value || !unit) return "";
    // Convert to lowercase for consistency
    return `${value} ${unit.toLowerCase()}`;
  };

  useEffect(() => {
    setSlaTimeline({
      value: slaInput ? String(slaInput) : "",
      unit: "DAYS",
    });
  }, [slaInput]);

  useEffect(() => {
    setEscalationPolicy({
      value: escalationInput ? String(escalationInput) : "",
      unit: "DAYS",
    });
  }, [escalationInput]);

  useEffect(() => {
    setRetentionPolicy({
      value: retentionInput ? String(retentionInput) : "",
      unit: retentionDuration || "YEARS",
    });
  }, [retentionInput, retentionDuration]);

  useEffect(() => {
    const fetchConsentDetails = async () => {
      try {
        setLoading(true);
        const response = await getGrievanceDetails(token, tenantId, businessId);
        console.log("gri response: ", response);

        const config = response?.searchList?.[0]?.configurationJson;
        console.log("config: ", config);
        console.log("config url: ", config?.endpointUrl);
        setConfigId(response?.searchList?.[0]?.configId);
        console.log("configId: ", response?.searchList?.[0]?.configId);
        if (!config) {
          setLoading(false);
          return;
        }

        setEndpointUrl(config.endpointUrl || "");
        setSlaTimeline({
          value: config.slaTimeline?.value || "",
          unit: config.slaTimeline?.unit || "",
        });
        //  setSlaInput(format({
        //    value: config.slaTimeline?.value || "",
        //    unit: config.slaTimeline?.unit || "",
        //  }));

        setEscalationPolicy({
          value: config.escalationPolicy?.value || "",
          unit: config.escalationPolicy?.unit || "",
        });
        // setEscalationInput(format({ 
        //   value: config.escalationPolicy?.value || "",
        //   unit: config.escalationPolicy?.unit || "",
        // }));

        //  setRetentionPolicy({
        //    value: config.retentionPolicy?.value || "",
        //    unit: config.retentionPolicy?.unit || "",
        //  });
        //  setRetentionInput(format({
        //     value: config.retentionPolicy?.value || "",
        //     unit: config.retentionPolicy?.unit || "",
        //  }));

        setSlaInput(String(config.slaTimeline?.value ?? ""));
        setEscalationInput(String(config.escalationPolicy?.value ?? ""));
        setRetentionInput(String(config.retentionPolicy?.value ?? ""));
        setRetentionDuration(config.retentionPolicy?.unit ?? "");


        setRetentionPolicy({
          value: String(config.retentionPolicy?.value ?? ""),
          unit: config.retentionPolicy?.unit ?? "",
        });



        setIsEmailChecked(config.communicationConfig?.email ?? true);
        setIsSmsChecked(config.communicationConfig?.sms ?? true);
        setGrievanceTypes(config.grievanceTypes || ["ACCESS", "CORRECTION", "DELETION"]);
        setIntakeMethods(config.intakeMethods || []);
        setWorkflow(config.workflow || []);


      } catch (error) {
        console.error("Error fetching consent details:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchConsentDetails();
  }, [token, tenantId, businessId]
  );

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
        <div className="gr-page">
          <div className="gr-heading">
            <Text appearance="heading-s" color="primary-grey-100">
              Configuration
            </Text>

            <div className="tag">
              <Text appearance="body-xs-bold" color="primary-grey-80">
              Grievance Redressal
              </Text>
            </div>
          </div>

          <div className="grievance-inputs-column-div">
            <div>
              {/* <div className="gr-dropdown-group">
                <label>Grievance Type (Required)</label>

               

                <Select
                  isMulti
                  options={options}
                  value={options.filter((opt) =>
                    grievanceTypes.includes(opt.value)
                  )}
                  onChange={(selected) =>
                    setGrievanceTypes(
                      selected ? selected.map((s) => s.value) : []
                    )
                  }
                  styles={customStyles}
                />

                <Text appearance="body-xs" color="primary-grey-80">
                  Types of grievance which can be raised by the user
                </Text>
              </div> */}

              <div className="gr-dropdown-group">
                <label>Intake method (Required)</label>

                <Select
                  isMulti
                  options={optionsIntake}
                  value={optionsIntake.filter((opt) =>
                    intakeMethods.includes(opt.value)
                  )}
                  onChange={(selected) =>
                    setIntakeMethods(selected ? selected.map((s) => s.value) : [])
                  }
                  styles={customStyles}
                  isDisabled={
                    optionsIntake.length === 1 &&
                    intakeMethods.includes(optionsIntake[0].value)
                  }
                />


                <Text appearance="body-xs" color="primary-grey-80">
                  This will be user’s default language for consent communication
                </Text>
              </div>
              <div className="gr-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Grievance Callback Endpoint (Required)"
                  helperText="Callback URL to notify the Data Fiduciary"
                  placeholder="https://example.com/callback"
                  name="endpointUrl"
                  value={endpointUrl}
                  onChange={handleEndpointUrlChange}
                  // state={
                  //   isEndpointUrlValid
                  //     ? "success"
                  //     : isEndpointUrlValid === false
                  //     ? "error"
                  //     : "none"
                  // }
                  stateText={
                    isEndpointUrlValid
                      ? ""
                      : "Please enter a valid URL (e.g. https://api.example.com)"
                  }
                ></InputFieldV2>
              </div>

              <div className="gr-dropdown-group">
                <label>Workflow steps (Required)</label>

                <Select
                  isMulti
                  options={workflowOptions}
                  value={workflowOptions.filter((opt) =>
                    workflow.includes(opt.value)
                  )}
                  onChange={(selected) =>
                    setWorkflow(selected ? selected.map((s) => s.value) : [])
                  }
                  styles={customStyles}
                  isDisabled={true}
                />

                <Text appearance="body-xs" color="primary-grey-80">
                  The steps grievance redressal will follow
                </Text>
              </div>
              <div className="gr-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="SLA timelines in days (Required)"
                  helperText="Estimated time to handle grievances"
                  value={slaInput || "7 days"}
                  onChange={(e) => handleSlaChange(e)}
                  placeholder="Enter a number"
                  type='number'
                ></InputFieldV2>
              </div>
              <div className="gr-input">
                <InputFieldV2
                  className="systemConfig-input"
                  label="Escalation policy in days (Required)"
                  placeholder="Enter a number"
                  name="escalationPolicy"
                  value={escalationInput || "2 days"}
                  onChange={(e) => handleEscalationChange(e)}
                  helperText="Time frame to escalate the grievance request"
                  type="number"
                ></InputFieldV2>
              </div>
            </div>

            <div>
              <div className="gr-input">
                {/* <div className="content-2" style={{ alignItems: 'center' }}>
                  <div style={{ width: '246%' }}>
                    <InputFieldV2
                      className="systemConfig-input"
                      label="Retention policy (Required)"
                      helperText="Enter for how long grievance logs are retained"
                      name="retentionPolicy"
                      value={retentionInput}
                      onChange={(e) => handleRetentionChange(e)}
                      placeholder="Enter a number"
                      size="medium"
                      type="number"
                    />

                  </div>
                  <div className="duration-dropdown-group">
                    <select value={retentionDuration} onChange={handleRententionDurationChange} id="language-select" >
                      <option value="" disabled> Duration </option>
                      <option value="DAYS">DAYS</option>
                      <option value="MONTHS">MONTHS</option>
                      <option value="YEARS">YEARS</option>
                    </select>
                  </div>

                </div> */}

              </div>

              {/* <InputCheckbox
                helperText=""
                label="Communication configuration via Email"
                name="Name"
                checked={isEmailChecked}
                onClick={() => setIsEmailChecked((prev) => !prev)}
                size="medium"
                state="none"
              /> */}
              <br></br>

              {/* <InputCheckbox
                helperText=""
                label="Communication configuration via SMS"
                name="Name"
                checked={isSmsChecked}
                onClick={() => setIsSmsChecked((prev) => !prev)}
                size="medium"
                state="none"
              /> */}
            </div>
          </div>
          <div className="content-5">
            <Button
              ariaControls="Button Clickable"
              ariaDescribedby="Button"
              ariaExpanded="Expanded"
              ariaLabel="Button"
              className="Button"
              icon=""
              iconAriaLabel="Icon Favorite"
              iconLeft=""
              kind="primary"
              label="Save"
              onClick={handleSubmit}
              state="normal"
            />
          </div>
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

export default Grievance;
