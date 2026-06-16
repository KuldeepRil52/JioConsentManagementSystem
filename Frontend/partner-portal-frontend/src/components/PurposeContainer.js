import React from "react";
import { useState, useEffect } from "react";
import { ActionButton, Icon, Text, InputFieldV2, InputRadio } from "../custom-components";
import { IcClose, IcWarningColored } from "../custom-components/Icon";
import Select from "react-select";
import "../styles/purposeContainer.css";
import "../styles/masterSetup.css";
import {
  getProcessingActivity,
  getPurposeList,
} from "../store/actions/CommonAction";
import CustomToast from "./CustomToastContainer";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";

const PurposeContainer = ({
  data,
  onChange,
  purposeNumber,
  onClear,
  isFirst,
  processingActivityList,
  setProcessingActivityList,
  purposeOptions,
  setPurposeOptions,
  processingActivityOption,
  setProcessingActivityOptions,
}) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [showNoActivityModal, setShowNoActivityModal] = useState(false);
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
      boxShadow: "none",
      borderColor: state.isFocused ? "#ccc" : "#ccc",
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
        ? "#e6f0ff"
        : state.isFocused
        ? "#f5f5f5"
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

  const fetchPurposes = async () => {
    try {
      let res = await dispatch(getPurposeList());
      if (res?.data?.searchList) {
        // setPurposeList(res.data.searchList);

        const opts = res.data.searchList.map((item) => ({
          value: item.purposeId,
          label: item.purposeName,
        }));
        setPurposeOptions(opts);
      }
      if (res?.status == 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      if (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001") {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Error in fetching purpose."}
            />
          ),
          { icon: false }
        );
      }
      console.log("Error fetching purposes:", err);
    }
  };

  // Helper function to filter processing activities to keep only latest version per processorActivityId
  const filterLatestVersions = (activityList) => {
    if (!activityList || activityList.length === 0) return [];
    
    // Group by processorActivityId
    const grouped = activityList.reduce((acc, item) => {
      const id = item.processorActivityId;
      if (!acc[id]) {
        acc[id] = [];
      }
      acc[id].push(item);
      return acc;
    }, {});
    
    // For each group, keep the one with highest version (and latest updatedAt if versions are equal)
    return Object.values(grouped).map((group) => {
      return group.reduce((latest, current) => {
        if (current.version > latest.version) {
          return current;
        } else if (current.version === latest.version) {
          // If versions are equal, prefer the one with latest updatedAt
          const currentDate = new Date(current.updatedAt);
          const latestDate = new Date(latest.updatedAt);
          return currentDate > latestDate ? current : latest;
        }
        return latest;
      });
    });
  };

  const fetchProcessingActivity = async () => {
    try {
      let res = await dispatch(getProcessingActivity());
      if (res?.status === 200 || res?.status === 201) {
        if (res?.data?.searchList) {
          // Filter to keep only latest versions
          const filteredList = filterLatestVersions(res.data.searchList);
          setProcessingActivityList(filteredList);
          const opts = filteredList.map((item) => ({
            value: item.processorActivityId,
            label: item.activityName,
            processorName: item.processorName,
            dataItems: item.dataTypesList?.flatMap((d) => d.dataItems) || [],
            dataTypesList: item.dataTypesList || [], // Store full dataTypesList structure
          }));
          setProcessingActivityOptions(opts);
          console.log("Processing Activity List:", filteredList);
        } else {
          // Handle case when no processing activity exists - show modal
          setProcessingActivityList([]);
          setProcessingActivityOptions([]);
          setShowNoActivityModal(true);
        }
      }
      if (res?.status === 403) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Facing Network Error..Please try again later."}
            />
          ),
          { icon: false }
        );
      }
    } catch (err) {
      // Better error handling to check if err is an array before accessing
      if (Array.isArray(err) && err[0] && (err[0]?.errorCode == "JCMP4003" || err[0]?.errorCode == "JCMP4001")) {
        toast.error(
          (props) => (
            <CustomToast {...props} type="error" message={"Session expired"} />
          ),
          { icon: false }
        );
      } else {
        // Set empty arrays to prevent undefined errors in UI - show modal
        setProcessingActivityList([]);
        setProcessingActivityOptions([]);
        setShowNoActivityModal(true);
      }
      console.log("Error fetching processing activity:", err);
    }
  };
  useEffect(() => {
    fetchPurposes();
    fetchProcessingActivity();
  }, [dispatch]);

  return (
    <>
      {/* No Processing Activity Modal */}
      {showNoActivityModal && (
        <div className="modal-outer-container">
          <div
            className="master-set-up-modal-container"
            style={{ 
              maxWidth: "450px",
              width: "90%",
              padding: "24px"
            }}
          >
            <div style={{ 
              display: "flex", 
              alignItems: "center", 
              justifyContent: "center",
              gap: "12px",
              marginBottom: "16px"
            }}>
              <Icon
                ic={<IcWarningColored />}
                size="l"
                color="feedback_warning_50"
              />
              <Text appearance="heading-xs" color="primary-grey-100">
                Processing Activity Required
              </Text>
            </div>

            <Text appearance="body-s" color="primary-grey-80" style={{ textAlign: "center", display: "block", marginBottom: "12px" }}>
              No processing activities are available. Processing activities are essential for creating consent templates.
            </Text>

            <Text appearance="body-s" color="primary-grey-80" style={{ textAlign: "center", display: "block", marginBottom: "20px" }}>
              Please navigate to <br></br><span style={{ 
                backgroundColor: "#FFF4E5", 
                padding: "2px 8px", 
                borderRadius: "4px",
                fontWeight: "600",
                color: "#D97706"
              }}>Master Data → Processing Activity</span> <br></br>and create at least one processing activity before proceeding.
            </Text>

            <div style={{ display: "flex", justifyContent: "center" }}>
              <ActionButton
                kind="primary"
                size="medium"
                state="normal"
                label="Go to Master Data"
                onClick={() => navigate('/master')}
              />
            </div>
          </div>
        </div>
      )}

      <div className="purpose-con">
      <div className="purpose-heading">
        <div className="left">
          <Text appearance="heading-xxs" color="primary-grey-80">
            Purpose {purposeNumber}
          </Text>
          <Text appearance="body-xxs" color="primary-grey-80">
            All fields are required
          </Text>
        </div>
        <div className="right">
          {!isFirst && (
            <ActionButton
              kind="secondary"
              size="small"
              state="normal"
              label="Delete"
              onClick={onClear}
            />
          )}
        </div>
      </div>

      <div className="purpose-select" style={{ padding: "0px 15px" }}>
        <Text appearance="body-xs" color="primary-grey-80">
          Select Purpose
        </Text>
        <Select
          isMulti
          options={purposeOptions}
          value={purposeOptions.filter((opt) =>
            data.purposeIds.includes(opt.value)
          )}
          onChange={(selected) => {
            const updated = selected ? selected.map((s) => s.value) : [];
            const updatedPNames = selected ? selected.map((s) => s.label) : [];
            onChange({ purposeIds: updated, purposeNames: updatedPNames });
            console.log("Purposes after update:", updated);
          }}
          styles={customStyles}
        />
      </div>

      <div
        className="puspose-opt"
        style={{ marginTop: "15px", padding: "0px 15px" }}
      >
        <Text appearance="body-xs" color="primary-grey-80">
          Is consent for this purpose required or optional ?
        </Text>
        <div style={{ display: "flex", flexDirection: "row", gap: "15px" }}>
          <InputRadio
            label="Required"
            name={`purpose-mandatory-${purposeNumber}`}
            checked={data.isMandatory === "Required"}
            onClick={() => onChange({ isMandatory: "Required" })}
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
          />

          <InputRadio
            label="Optional"
            name={`purpose-mandatory-${purposeNumber}`}
            checked={data.isMandatory === "Optional"}
            onClick={() => onChange({ isMandatory: "Optional" })}
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
          />
        </div>
      </div>

      <div
        className="purpose-validity"
        style={{ marginTop: "15px", padding: "0px 15px" }}
      >
        <Text appearance="body-xs" color="primary-grey-80">
          Enter the validity period in days/months/years
        </Text>
        <div style={{ display: "flex", flexDirection: "row", gap: "30px" }}>
          <InputRadio
            checked={data.preferenceValidity.unit === "DAYS"}
            label="Day"
            name={`validity-${purposeNumber}`}
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
            onClick={() =>
              onChange({
                preferenceValidity: {
                  ...data.preferenceValidity,
                  unit: "DAYS",
                },
              })
            }
          />

          <InputRadio
            checked={data.preferenceValidity.unit === "MONTHS"}
            label="Month"
            name={`validity-${purposeNumber}`}
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
            onClick={() =>
              onChange({
                preferenceValidity: {
                  ...data.preferenceValidity,
                  unit: "MONTHS",
                },
              })
            }
          />

          <InputRadio
            checked={data.preferenceValidity.unit === "YEARS"}
            label="Year"
            name={`validity-${purposeNumber}`} // unique per purpose
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
            onClick={() =>
              onChange({
                preferenceValidity: {
                  ...data.preferenceValidity,
                  unit: "YEARS",
                },
              })
            }
          />
        </div>

        <div style={{ marginTop: "5px" }}>
          <InputFieldV2
            value={data.preferenceValidity.value}
            type="number"
            size="small"
            placeholder=""
            onChange={(e) =>
              onChange({
                preferenceValidity: {
                  ...data.preferenceValidity,
                  value: e.target.value,
                },
              })
            }
          />
        </div>
      </div>

      <div
        className="purpose-auto"
        style={{ marginTop: "15px", padding: "0px 15px" }}
      >
        <Text appearance="body-xs" color="primary-grey-80">
          Auto-renew validity after expiry period?
        </Text>
        <div style={{ display: "flex", flexDirection: "row", gap: "15px" }}>
          <InputRadio
            label="Yes"
            checked={data.autoRenew === "Yes"}
            onClick={() => onChange({ autoRenew: "Yes" })}
            name={`purpose-autorenew-${purposeNumber}`}
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
          />
          <InputRadio
            label="No"
            name={`purpose-autorenew-${purposeNumber}`}
            checked={data.autoRenew === "No"}
            onClick={() => onChange({ autoRenew: "No" })}
            prefix="ic_profile"
            size="small"
            suffix="ic_profile"
          />
        </div>
      </div>

      <div className="pro-act-con">
        <div className="pro-act-heading">
          <div className="left">
            <Text appearance="heading-xxs" color="primary-grey-100">
              Processing activity
            </Text>
          </div>
          <div className="right">
            
          </div>
        </div>
        <div className="purpose-select">
          <Text appearance="body-xs" color="primary-grey-80">
            Select Processing activity (Required)
          </Text>
          <Select
              isMulti
              options={processingActivityOption}
              value={processingActivityOption.filter((opt) =>
                data.purposeActivityIds.includes(opt.value)
              )}
              onChange={(selected) => {
                const updated = selected ? selected.map((s) => s.value) : [];
                const updatedPA = selected ? selected.map((s) => s.label) : [];
                const processorName1 = selected
                  ? selected.map((s) => s.processorName)
                  : [];
                const processorName = [...new Set(processorName1)];
                const items1 = selected ? selected.map((s) => s.dataItems) : [];
                const items = [...new Set(items1)];
                const dataTypes1 = selected ? selected.map((s) => s.dataTypesList || []) : [];
                const dataTypes = dataTypes1.filter(dt => dt && dt.length > 0); // Filter out empty arrays
                onChange({
                  purposeActivityIds: updated,
                  usedBy: processorName,
                  dataItems: items,
                  dataTypes: dataTypes, // Include dataTypes with dataTypeName and dataItems
                  processingAct: updatedPA,
                });
                console.log(
                  "purposeActivityIdsafter update:",
                  processingActivityOption
                );
              }}
              styles={customStyles}
            />

          <div style={{ marginTop: "10px" }}>
            <InputFieldV2
              label="Used by"
              value={data.usedBy.join(", ")}
              readOnly
              size="small"
            />
          </div>

          <div style={{ marginTop: "10px" }}>
            <InputFieldV2
              label="Data item"
              value={data.dataItems.join(", ")}
              readOnly
              size="small"
            />
          </div>
        </div>
      </div>

      <div
        style={{
          marginTop: "15px",
          display: "flex",
          flexDirection: "row",
          gap: "10px",
          padding: "10 15px",
          marginBottom: "20px",
        }}
      >
        <br></br>
        {/* <Icon
          color="primary"
          ic="ic_add"
          kind="background-bold"
          onClick={function noRefCheck() {}}
          size="s"
        /> */}
        {/* <Text appearance="body-xs-bold" color="primary-60">
          Add another activity
        </Text> */}
      </div>
    </div>
    </>
  );
};

export default PurposeContainer;
