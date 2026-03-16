import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useDispatch } from "react-redux";
import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { updateRole, searchRoles, listComponents } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const UpdateRole = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const { roleId } = useParams();

  const [roleName, setRoleName] = useState("");
  const [text, setText] = useState("");
  const [accessToItems, setAccessToItems] = useState({});

  const [permissions, setPermissions] = useState(() => {
    const initial = {};
    Object.keys(accessToItems).forEach((item) => {
      initial[item] = { read: false, write: false };
    });
    return initial;
  });

  useEffect(() => {
    const fetchData = async () => {
      try {
        // Fetch components
        const componentsResponse = await dispatch(listComponents());
        const components = componentsResponse || [];
        const newAccessToItems = {};
        const newPermissions = {};
        components.forEach(component => {
          newAccessToItems[component.componentName] = component.componentId;
          newPermissions[component.componentName] = { read: false, write: false };
        });
        setAccessToItems(newAccessToItems);
        setPermissions(newPermissions);

        // Fetch role details
        if (roleId) {
          const roleResponse = await dispatch(searchRoles(roleId));
          const roleData = roleResponse || [];
          if (roleData) {
            setRoleName(roleData.role || "");
            setText(roleData.description || "");
            const fetchedPermissions = roleData.permissions || [];
            const updatedPermissions = { ...newPermissions }; // Use the newly created permissions object
            fetchedPermissions.forEach(p => {
              const componentName = Object.keys(newAccessToItems).find(key => newAccessToItems[key] === p.componentId);
              if (componentName) {
                if (p.action.includes('READ')) {
                  updatedPermissions[componentName].read = true;
                }
                if (p.action.includes('WRITE')) {
                  updatedPermissions[componentName].write = true;
                }
              }
            });
            setPermissions(updatedPermissions);
          }
        }
      } catch (error) {
        console.error("Failed to fetch data in UpdateRole:", error);
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to load initial data."}
            />
          ),
          { icon: false }
        );
      }
    };
    fetchData();
  }, [dispatch, roleId]);

  const handleBack = () => {
    navigate("/roles");
  };

  const handlePermissionChange = (item, type) => {
    setPermissions((prev) => ({
      ...prev,
      [item]: {
        ...prev[item],
        [type]: !prev[item][type],
      },
    }));
  };

  const handleSubmit = async () => {
    if (!roleName.trim() || !text.trim()) {
      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={"Role name and description are required."}
          />
        ),
        { icon: false }
      );
      return;
    }

    const permissionsPayload = [];
    for (const componentName in permissions) {
      const action = [];
      if (permissions[componentName].read) {
        action.push("READ");
      }
      if (permissions[componentName].write) {
        action.push("WRITE");
      }
      if (action.length > 0) {
        permissionsPayload.push({ componentId: accessToItems[componentName], action });
      }
    }

    const payload = { role: roleName, description: text, permissions: permissionsPayload };

    try {
      const response = await dispatch(updateRole(roleId, payload));
      if (response && (response.status === 200 || response.status === 201)) {
        toast.success(
          (props) => (
            <CustomToast
              {...props}
              type="success"
              message={"Role updated successfully!"}
            />
          ),
          { icon: false }
        );
        // Add delay before navigation to ensure toast is visible
        setTimeout(() => {
          navigate("/roles");
        }, 1000);
      } else {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"Failed to update role. Please try again."}
            />
          ),
          { icon: false }
        );
      }
    } catch (error) {
      console.error("Update role error:", error);
      let errorMessage = "An error occurred while updating the role. Please try again.";

      // Extract error message from different error formats
      if (Array.isArray(error) && error[0]?.errorMessage) {
        errorMessage = error[0].errorMessage;
      } else if (error?.errorList && Array.isArray(error.errorList) && error.errorList.length > 0) {
        errorMessage = error.errorList[0]?.errorMessage || errorMessage;
      } else if (error?.errorMessage) {
        errorMessage = error.errorMessage;
      } else if (error?.message) {
        errorMessage = error.message;
      }

      toast.error(
        (props) => (
          <CustomToast
            {...props}
            type="error"
            message={errorMessage}
          />
        ),
        { icon: false }
      );
    }
  };
  return (
    <>
      <div className="configurePage">
        <div style={{ display: "flex", gap: 70, width: "50%" }}>
          <div style={{ flex: "0 0 auto" }}>
            <Button icon="ic_back" kind="secondary" onClick={handleBack} />
          </div>
          <div style={{ flex: 1 }}>
            <Text appearance="heading-xs" color="primary-grey-100">Update Role</Text>
            <br />
            <InputFieldV2
              label="Role name (Required)"
              value={roleName}
              onChange={(e) => setRoleName(e.target.value)}
            />
            <br />
            <Text appearance="body-xs" color="primary-grey-80">Description (Required)</Text>
            <textarea
              value={text}
              onChange={(e) => setText(e.target.value)}
              rows="4"
              className="custom-text-area"
            />
            <br />
            <div style={{ padding: "5px", display: 'inline-block', width: '110%' }}>
              <table className="business-table">
                <thead>
                  <tr>
                    <th>Access to</th>
                    <th>Can edit</th>
                    <th>Can view</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.keys(accessToItems).map((item) => (
                    <tr key={item}>
                      <td>{item}</td>
                      <td>
                        <label className="custom-checkbox-container">
                          <input
                            type="checkbox"
                            className="custom-checkbox"
                            checked={permissions[item]?.write || false}
                            onChange={() => handlePermissionChange(item, "write")}
                          />
                        </label>
                      </td>
                      <td>
                        <label className="custom-checkbox-container">
                          <input
                            type="checkbox"
                            className="custom-checkbox"
                            checked={permissions[item]?.read || false}
                            onChange={() => handlePermissionChange(item, "read")}
                          />
                        </label>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <br />
            <div className="flex justify-end">
              <ActionButton label="Update" kind="primary" onClick={handleSubmit} />
            </div>
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

export default UpdateRole;
