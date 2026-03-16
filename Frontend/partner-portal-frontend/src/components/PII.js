import "../styles/masterSetup.css";
import { useState, useEffect } from "react";
import { useMemo } from "react";
import "../styles/toast.css";
import CustomToast from "./CustomToastContainer";
import {
  Text,
  InputFieldV2,
  SearchBox,
  InputRadio,
  ActionButton,
  InputCheckbox,
  Icon,
} from "../custom-components";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import { IcAdd, IcClose, IcChevronLeft, IcChevronRight } from "../custom-components/Icon";
import { IcAdd, IcClose, IcChevronLeft, IcChevronRight, ic_search } from "../custom-components/Icon";
import { CLEAR_SESSION } from "../store/constants/Constants";
import "../styles/sessionModal.css";

import { useDispatch } from "react-redux";
import {
  getDataTypes,
  createDataType,
  updateDataType,
} from "../store/actions/CommonAction";
import { Navigate, useNavigate } from "react-router-dom";
import { getTranslations } from "../store/actions/CommonAction";

import { languages } from "../utils/languages";

const PII = ({ selectedLanguage }) => {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [modalLanguage, setModalLanguage] = useState('en');
  const [dataTypeLabel, setDataTypeLabel] = useState("Enter data type");
  const [dataItemLabel, setDataItemLabel] = useState("Enter data item");

  // State for original, untranslated data from the API
  const [originalDataTypes, setOriginalDataTypes] = useState([]);
  // State for the data displayed in the UI (potentially translated)
  const [translatedDataTypes, setTranslatedDataTypes] = useState([]);
  const [fetchedDataTypes, setFetchedDataTypes] = useState([]);
  const [disableSave, setDisableSave] = useState(false);
  const [itemsAdded, setItemsAdded] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const translateData = async () => {
      if (selectedLanguage === 'en') {
        setTranslatedDataTypes(originalDataTypes);
        return;
      }
      if (originalDataTypes.length === 0) return;

      setLoading(true);

      const textsToTranslate = originalDataTypes.flatMap(type => [
        { id: `type-${type.name}`, source: type.name },
        ...(type.items || []).map(item => ({ id: `item-${type.name}-${item}`, source: item }))
      ]);

      const translationResult = await dispatch(getTranslations(textsToTranslate, selectedLanguage));

      const translatedData = originalDataTypes.map(type => {
        const translatedTypeName = translationResult.output.find(t => t.id === `type-${type.name}`)?.target || type.name;
        const translatedItems = (type.items || []).map(item => {
          return translationResult.output.find(t => t.id === `item-${type.name}-${item}`)?.target || item;
        });
        return { ...type, name: translatedTypeName, items: translatedItems };
      });

      setTranslatedDataTypes(translatedData);
      setLoading(false);
    };

    translateData();
  }, [selectedLanguage, originalDataTypes]);
  const [startIndex, setStartIndex] = useState(0);
  const visibleLanguages = 6;

  const handleNext = () => {
    if (startIndex + visibleLanguages < languages.length) {
      setStartIndex(startIndex + 1);
    }
  };

  const handlePrev = () => {
    if (startIndex > 0) {
      setStartIndex(startIndex - 1);
    }
  };
  useEffect(() => {
    const fetchPurposes = async () => {
      try {
        let res = await dispatch(getDataTypes()); // wait for thunk to finish
        if (res?.status === 200 || res?.status === 201) {
          if (res?.data?.searchList) {
            // Transform API response into your UI structure
            let l = res?.data?.searchList;
            if (l.length == 0) {
              setDisableSave(true);
            } else {
              const formattedData = res.data.searchList.map((item) => ({
                name: item.dataTypeName, // use API field
                items: item.dataItems, // use API field
              }));

              setOriginalDataTypes(formattedData);
              setTranslatedDataTypes(formattedData); // Initially set to original data

              const rawData = res.data.searchList.map((item) => ({
                dataTypeId: item.dataTypeId, // include dataTypeId here
                name: item.dataTypeName,
                items: item.dataItems,
              }));
              setFetchedDataTypes(rawData);
            }
          }
        }
        setLoading(false); // Set loading to false after fetch
        if (res?.status === 403) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Facing Network Error..Please try again later.."}
              />
            ),
            { icon: false }
          );
        }
        if (res?.status === 401) {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Your sesion is expired..Please Logged in again.."}
              />
            ),
            { icon: false }
          );
          navigate("/adminLogin");
        }
      } catch (err) {
        if (Array.isArray(err) && err[0] && (err[0].errorCode == "JCMP4003" || err[0].errorCode == "JCMP4001")) {
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
        } else {
          toast.error(
            (props) => (
              <CustomToast
                {...props}
                type="error"
                message={"Error occured while fetching purpose."}
              />
            ),
            { icon: false }
          );
        }
      }
    };

    fetchPurposes();
  }, [dispatch]);
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [searchText, setSearchText] = useState("");

  const removeInputSpaces = (str) => {
    if (!str || typeof str !== 'string') return '';
    return str.trim().replace(/\s+/g, " ").toLowerCase();
  };

  const filteredDataTypes = translatedDataTypes.filter((type) =>
    type && type.name && type.items && removeInputSpaces(type.name).includes(removeInputSpaces(searchText))
  );

  const [selectedType, setSelectedType] = useState("");
  const [dataTypeVal, setDataTypeVal] = useState("");
  const [dataItemVal, setDataItemVal] = useState("");
  const [dataItemSearchText, setDataItemSearchText] = useState("");
  const [dataItems, setDataItems] = useState([
    "Browsing activity",
    "Search history",
    "Likes and shares",
    "Loyalty ID",
    "Education",
    "Health info",
    "Purchase history",
  ]);
  const [finalSelectedData, setFinalSelectedData] = useState([]);
  const [dataTypeModal, setDataTypeModal] = useState(false);
  const [dataItemModal, setDataItemModal] = useState(false);

  const submitDataItem = async () => {
    let value = dataItemVal.trim().replace(/\s+/g, " ");
    const isValid = /^(?=.*[A-Za-z])[A-Za-z0-9\s._&-]+$/.test(value);

    if (!isValid || value === "") {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Enter Valid Data Item"}
          />
        ),
        { icon: false }
      );
      return;
    }

    let translatedValue = value;
    if (modalLanguage !== 'en') {
      try {
        const textsToTranslate = [{ id: 'newItem', source: value }];
        const translationResult = await dispatch(getTranslations(textsToTranslate, modalLanguage));
        if (translationResult && translationResult.output && translationResult.output[0]) {
          translatedValue = translationResult.output[0].target;
        }
      } catch (error) {
        console.error("Failed to translate new data item:", error);
        // Proceed with the untranslated value if translation fails
      }
    }

    // Check if data item already exists in the selected data type (case-insensitive comparison)
    const selectedTypeObj = translatedDataTypes.find(
      (type) => type.name === selectedType
    );

    if (selectedTypeObj && selectedTypeObj.items) {
      const normalizedValue = translatedValue.toLowerCase().trim();
      const existingItem = selectedTypeObj.items.find(
        (item) => item.toLowerCase().trim() === normalizedValue
      );

      if (existingItem) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Data item already exists"}
            />
          ),
          { icon: false }
        );
        return;
      }
    }

    setTranslatedDataTypes((prev) =>
      prev.map((type) =>
        type.name === selectedType
          ? { ...type, items: [...(type.items || []), translatedValue] }
          : type
      )
    );

    setDataItemVal("");
    setDataItemModal(false);
    setItemsAdded(true);
  };

  const handleItemCheckboxChange = (typeName, item, isChecked) => {
    setFinalSelectedData(prevSelectedData => {
      let updatedSelectedData = [...prevSelectedData];

      if (isChecked) {
        // Add item
        const typeIndex = updatedSelectedData.findIndex(data => data.name === typeName);
        if (typeIndex > -1) {
          // Type exists, add item if not already there
          if (!updatedSelectedData[typeIndex].items.includes(item)) {
            updatedSelectedData[typeIndex].items.push(item);
          }
        } else {
          // Type doesn't exist, add type and item
          updatedSelectedData.push({ name: typeName, items: [item] });
        }
      } else {
        // Remove item
        const typeIndex = updatedSelectedData.findIndex(data => data.name === typeName);
        if (typeIndex > -1) {
          updatedSelectedData[typeIndex].items = updatedSelectedData[typeIndex].items.filter(i => i !== item);
          // If no items left for this type, remove the type
          if (updatedSelectedData[typeIndex].items.length === 0) {
            updatedSelectedData = updatedSelectedData.filter(data => data.name !== typeName);
          }
        }
      }
      return updatedSelectedData;
    });
  };

  // const saveDataType = async () => {
  //   try {
  //     const selectedTypeObj = translatedDataTypes.find(
  //       (type) => type.name === selectedType
  //     );

  //     // Get only items for that selected type
  //     const selectedItems = selectedTypeObj ? selectedTypeObj.items : [];
  //     const fetchedTypeObj = fetchedDataTypes.find(
  //       (type) => type.name === selectedType
  //     );

  //     if (fetchedTypeObj) {
  //       let res = await dispatch(
  //         updateDataType(selectedType, selectedItems, fetchedTypeObj.dataTypeId)
  //       );
  //       if (res.status == 200 || res.status == 201) {
  //         toast.success(
  //           (props) => (
  //             <CustomToast
  //               {...props}
  //               type="success"
  //               message={"Data item updated successfully."}
  //             />
  //           ),
  //           { icon: false }
  //         );
  //         setDisableSave(false);
  //       }
  //     } else {
  //       let res = await dispatch(createDataType(selectedType, selectedItems));
  //       if (res.status == 200 || res.status == 201) {
  //         toast.success(
  //           (props) => (
  //             <CustomToast
  //               {...props}
  //               type="success"
  //               message={"Data item added successfully."}
  //             />
  //           ),
  //           { icon: false }
  //         );
  //         setDisableSave(false);
  //       }
  //     }
  //   } catch (error) {
  //     if (
  //       error[0].errorCode == "JCMP4003" ||
  //       error[0].errorCode == "JCMP4001"
  //     ) {
  //       toast.error(
  //         (props) => (
  //           <CustomToast {...props} type="error" message={"Session expired"} />
  //         ),
  //         { icon: false }
  //       );
  //       dispatch({ type: CLEAR_SESSION });
  //       setShowSessionModal(true);
  //       setTimeout(() => {
  //         setShowSessionModal(false);
  //         navigate("/adminLogin");
  //       }, 7000);
  //     } else {
  //       toast.error(
  //         (props) => (
  //           <CustomToast
  //             {...props}
  //             type="error"
  //             message={"Something went wrong.."}
  //           />
  //         ),
  //         { icon: false }
  //       );
  //     }
  //   }
  // };

  const saveDataType = async () => {
    try {
      const selectedTypeObj = translatedDataTypes.find(
        (type) => type.name === selectedType
      );

      // Get only items for that selected type
      const selectedItems = selectedTypeObj ? selectedTypeObj.items : [];
      const fetchedTypeObj = fetchedDataTypes.find(
        (type) => type.name === selectedType
      );

      if (fetchedTypeObj) {
        let res = await dispatch(
          updateDataType(selectedType, selectedItems, fetchedTypeObj.dataTypeId)
        );
        if (res.status == 200 || res.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Data item updated successfully."}
              />
            ),
            { icon: false }
          );
          setDisableSave(false);
          setItemsAdded(false);
        }
      } else {
        let res = await dispatch(createDataType(selectedType, selectedItems));
        if (res.status == 200 || res.status == 201) {
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Data item added successfully."}
              />
            ),
            { icon: false }
          );
          setDisableSave(false);
          setItemsAdded(false);
        }
      }
    } catch (error) {
      if (
        Array.isArray(error) &&
        error[0] &&
        (error[0].errorCode == "JCMP4003" || error[0].errorCode == "JCMP4001")
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
      } else if (Array.isArray(error) && error[0]?.errorMessage) {
        let backendMessage = error[0].errorMessage;

        if (backendMessage.startsWith("Conflict: ")) {
          backendMessage = backendMessage.replace("Conflict: ", "");
        }

        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={backendMessage}
            />
          ),
          { icon: false }
        );
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Something went wrong.."}
            />
          ),
          { icon: false }
        );
      }
    }
  };

  const handleDataItemValBlur = (e) => {
    let value = e.target.value;
    setDataItemVal(value);
  };
  const openDataItemModal = () => {
    setDataItemModal(true);
  };

  const checkDataTypeSelection = () => {
    if (
      selectedType == null ||
      selectedType == "" ||
      selectedType == undefined
    ) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please Select or Add Data Type."}
          />
        ),
        { icon: false }
      );
    } else {
      openDataItemModal();
    }
  };
  const closeDataItemModal = () => {
    setDataItemModal(false);
  };
  const handleDataTypeValBlur = (e) => {
    let value = e.target.value;
    setDataTypeVal(value);
  };


  const openDataTypeModel = () => {
    setDataTypeModal(true);
  };
  const closeDataTypeModal = () => {
    setDataTypeModal(false);
  };


  const submitDataType = async () => {
    let value = dataTypeVal.trim().replace(/\s+/g, " ");
    const isValid = /^(?=.*[A-Za-z])[A-Za-z0-9\s._&-]+$/.test(value);

    if (!isValid || value === "") {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Please Enter Valid Data Type."}
          />
        ),
        { icon: false }
      );
      return;
    }

    let translatedValue = value;
    if (modalLanguage !== 'en') {
      try {
        const textsToTranslate = [{ id: 'newType', source: value }];
        const translationResult = await dispatch(getTranslations(textsToTranslate, modalLanguage));
        if (translationResult && translationResult.output && translationResult.output[0]) {
          translatedValue = translationResult.output[0].target;
        }
      } catch (error) {
        console.error("Failed to translate new data type:", error);
        // Proceed with the untranslated value if translation fails
      }
    }

    // Check if data type already exists (case-insensitive comparison)
    const normalizedValue = translatedValue.toLowerCase().trim();
    const existingDataType = translatedDataTypes.find(
      (type) => type.name.toLowerCase().trim() === normalizedValue
    );

    if (existingDataType) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Data type already exists"}
          />
        ),
        { icon: false }
      );
      return;
    }

    setTranslatedDataTypes([...translatedDataTypes, { name: translatedValue, items: [] }]);
    setSelectedType(translatedValue); // auto-select the new type
    setDataTypeVal("");
    setDataTypeModal(false);
  };

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
      <div className="pii-outer-div">
        <br></br>
        <div style={{ display: "flex", gap: "32px", width: "100%" }}>
          <div className="pii-table-container" style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            minWidth: "700px", /* Keeping original table size intact, adjust as needed */
            border: "1px solid #e0e0e0",
            borderRadius: "8px",
            overflow: "hidden"
          }}>
            {/* LEFT COLUMN - Data Types */}
            <div style={{ borderRight: "1px solid #e0e0e0", display: "flex", flexDirection: "column" }}>
              <div style={{ backgroundColor: "#f5f5f5", padding: "12px 16px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Text appearance="body-m-bold" color="primary-grey-100">Data Types</Text>
                <div onClick={openDataTypeModel} style={{ cursor: "pointer" }}><Text appearance="body-s-bold" color="primary-60">Add data type</Text></div>
              </div>
              <div style={{ padding: "8px", borderBottom: "1px solid #e0e0e0" }}>
                <div className="pii-search-container">
                  <Icon ic={<ic_search />} size="sm" />
                  <input
                    className="pii-search-input"
                    placeholder="Search Data Types"
                    value={searchText}
                    onChange={(e) => setSearchText(e.target.value)}
                  />
                  {searchText && (
                    <button
                      className="pii-search-clear-btn"
                      onClick={() => setSearchText("")}
                    >
                      <Icon ic={<IcClose />} size="sm" />
                    </button>
                  )}
                </div>
              </div>
              <div style={{ flex: 1, overflowY: "auto" }}>
                {filteredDataTypes.map((type, index) => (
                  <div key={index} style={{ padding: "8px 16px", borderBottom: "1px solid #e0e0e0" }}>
                    <InputRadio
                      checked={selectedType === type.name}
                      label={type.name}
                      name="dataType"
                      value={type.name}
                      onClick={() => setSelectedType(type.name)}
                      prefix="ic_profile"
                      size="medium"
                      state="none"
                    />
                  </div>
                ))}
              </div>
            </div>

            {/* RIGHT COLUMN - Data Items */}
            <div style={{ display: "flex", flexDirection: "column" }}>
              <div style={{ backgroundColor: "#f5f5f5", padding: "12px 16px", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                <Text appearance="body-m-bold" color="primary-grey-100">Data Items</Text>
                <div onClick={checkDataTypeSelection} style={{ cursor: "pointer" }}><Text appearance="body-s-bold" color="primary-60">Add data item</Text></div>
              </div>
              <div style={{ padding: "8px", borderBottom: "1px solid #e0e0e0" }}>
                <div className="pii-search-container">
                  <Icon ic={<ic_search />} size="sm" />
                  <input
                    className="pii-search-input"
                    placeholder="Search Data Items"
                    value={dataItemSearchText}
                    onChange={(e) => setDataItemSearchText(e.target.value)}
                  />
                  {dataItemSearchText && (
                    <button
                      className="pii-search-clear-btn"
                      onClick={() => setDataItemSearchText("")}
                    >
                      <Icon ic={<IcClose />} size="sm" />
                    </button>
                  )}
                </div>
              </div>
              <div style={{ flex: 1, overflowY: "auto" }}>
                {selectedType ? (
                  filteredDataTypes
                    .find(type => type.name === selectedType)?.items
                    .filter(item => removeInputSpaces(item).includes(removeInputSpaces(dataItemSearchText)))
                    .map((item, itemIndex) => (
                      <div key={itemIndex} style={{ padding: "8px 16px", borderBottom: "1px solid #e0e0e0", height: "41px" }}>
                        <InputCheckbox
                          checked={true}
                          label={item}
                          name={item}
                          size="small"
                          onChange={(e) => handleItemCheckboxChange(selectedType, item, e.target.checked)}
                        />
                      </div>
                    ))
                ) : (
                  <div style={{ padding: "16px", color: "#888" }}>Select a data type to view its items</div>
                )}
              </div>
            </div>
          </div>

          {/* SELECTED DATA TYPES AND DATA ITEMS SECTION */}
          <div style={{
            flexGrow: 1, /* Takes remaining space */
            border: "1px solid #e0e0e0",
            borderRadius: "8px",
            overflow: "hidden",
            display: "flex",
            flexDirection: "column"
          }}>
            <div style={{ backgroundColor: "#f5f5f5", padding: "12px 16px" }}>
              <Text appearance="body-m-bold" color="primary-grey-100">Selected data types and data items</Text>
            </div>
            <div style={{ flex: 1, overflowY: "auto", padding: "16px" }}>
              {translatedDataTypes.length > 0 ? (
                translatedDataTypes.map((type, typeIndex) => (
                  <div key={typeIndex} style={{ marginBottom: "16px" }}>
                    <Text appearance="body-m-bold">{type.name}</Text>
                    <div style={{ display: "flex", flexWrap: "wrap", gap: "8px", marginTop: "8px" }}>
                      {(type.items || []).map((item, itemIndex) => (
                        <div key={itemIndex} className="data-item-pill">
                          <Text appearance="body-s">{item}</Text>
                        </div>
                      ))}
                    </div>
                  </div>
                ))
              ) : (
                <div style={{ color: "#888" }}>No data types and items available</div>
              )}
            </div>
          </div>
        </div>



        {dataTypeModal && (
          <div className="modal-outer-container">
            <div className="master-set-up-modal-container">
              <div className="modal-close-btn-container">
                <ActionButton
                  onClick={() => setDataTypeModal(false)}
                  icon={<IcClose />}
                  kind="tertiary"
                ></ActionButton>
              </div>{" "}
              <Text appearance="heading-xs" color="primary-grey-100">
                {" "}
                Add data type
              </Text>
              <div className="language-tabs-container">
                <select
                  value={modalLanguage}
                  onChange={(e) => setModalLanguage(e.target.value)}
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="title-margin "></div>
              <InputFieldV2
                label={dataTypeLabel}
                value={dataTypeVal}
                onChange={(e) => setDataTypeVal(e.target.value)}
              />
              <div className="modal-add-btn-container">
                <ActionButton
                  label="Add"
                  onClick={submitDataType}
                ></ActionButton>{" "}
              </div>{" "}
            </div>{" "}
          </div>
        )}
        {dataItemModal && (
          <div className="modal-outer-container">
            <div className="master-set-up-modal-container">
              <div className="modal-close-btn-container">
                <ActionButton
                  onClick={() => setDataItemModal(false)}
                  icon={<IcClose />}
                  kind="tertiary"
                ></ActionButton>
              </div>{" "}
              <Text appearance="heading-xs" color="primary-grey-100">
                {" "}
                Add preference data item
              </Text>
              <div className="language-tabs-container">
                <select
                  value={modalLanguage}
                  onChange={(e) => setModalLanguage(e.target.value)}
                >
                  {languages.map((lang) => (
                    <option key={lang.value} value={lang.value}>
                      {lang.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="title-margin "></div>
              <InputFieldV2
                label={dataItemLabel}
                value={dataItemVal}
                onChange={(e) => setDataItemVal(e.target.value)}
              />
              <div className="modal-add-btn-container">
                <ActionButton
                  label="Add"
                  onClick={submitDataItem}
                ></ActionButton>{" "}
              </div>{" "}
            </div>{" "}
          </div>
        )}
        <div className="common-add-btn">
          <ActionButton
            label="Save"
            kind="primary"
            state={!disableSave && !itemsAdded ? "disabled" : "normal"}
            onClick={saveDataType}
          ></ActionButton>
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

export default PII;
