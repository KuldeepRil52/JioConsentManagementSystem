import React, { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Text, ActionButton } from "../custom-components";
import { IcEditPen, IcTrash } from "../custom-components/Icon";
import { Icon } from "../custom-components";
import { IcEditPen, IcTrash, IcSort } from "../custom-components/Icon";
import { IcDocumentViewer } from "../custom-components/Icon";
import { listRoles, deleteRole } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";
import "../styles/businessGroups.css";
import "../styles/pageConfiguration.css";

const Roles = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [roles, setRoles] = useState([]);
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    return `${day}-${month}-${year}`;
  };

  useEffect(() => {
    const fetchRoles = async () => {
      try {
        const response = await dispatch(listRoles());
        setRoles(response || []);
      } catch (error) {
        console.error("Failed to fetch roles:", error);
        setRoles([]); // Ensure roles is an array on error
      }
    };
    fetchRoles();
  }, [dispatch]);

  const handleAddRoleClick = () => {
    navigate("/addRole");
  };

  const handleUpdate = (roleId) => {
    navigate(`/updateRole/${roleId}`);
  };

  const handleDelete = async (roleId) => {
    if (window.confirm("Are you sure you want to delete this role?")) {
      try {
        const response = await dispatch(deleteRole(roleId));
        if (response && response.status === 200) {
          setRoles(roles.filter(role => role.roleId !== roleId));
          toast.success(
            (props) => (
              <CustomToast
                {...props}
                type="success"
                message={"Role deleted successfully!"}
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
                message={"Failed to delete role. Please check the console."}
              />
            ),
            { icon: false }
          );
        }
      } catch (error) {
        toast.error(
          (props) => (
            <CustomToast
              {...props}
              type="error"
              message={"An error occurred while deleting the role."}
            />
          ),
          { icon: false }
        );
        console.error("Delete role error:", error);
      }
    }
  };

  const handleView = (roleId) => {
    navigate(`/addRole?mode=view&id=${roleId}`);
  };

  const sortedRoles = React.useMemo(() => {
    if (!sortColumn) return roles;

    return [...roles].sort((a, b) => {
      let aValue, bValue;

      if (sortColumn === 'permissions') {
        aValue = a.permissions ? a.permissions.map(p => p.componentName).join(", ") : "N/A";
        bValue = b.permissions ? b.permissions.map(p => p.componentName).join(", ") : "N/A";
      } else {
        aValue = a[sortColumn] || '';
        bValue = b[sortColumn] || '';
      }

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [roles, sortColumn, sortDirection]);

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  const renderSortIcon = () => {
    return <Icon ic={<IcSort />} size="small" color="black" />;
  };

  return (
    <>
      <div className="configurePage">
        <div className="bs-page">
          <div className="main-heading-ct">
            <div className="bs-heading">
              <Text appearance="heading-s" color="primary-grey-100">Roles</Text>
              <div className="tag">
                <Text appearance="body-xs-bold" color="primary-grey-80">User Management</Text>
              </div>
            </div>
            <div className="bs-button" style={{ marginRight: "5%" }}>
              <ActionButton
                label="Add role"
                onClick={handleAddRoleClick}
                kind="primary"
              />
            </div>
          </div>

          <div className="bs-content">
            <div className="business-table-container">
              <table className="business-table">
                <thead>
                  <tr>
                    <th onClick={() => handleSort('role')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Role</span>
                        {renderSortIcon()}
                      </div>
                    </th>
                    <th onClick={() => handleSort('description')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Description</span>
                        {renderSortIcon()}
                      </div>
                    </th>
                    <th onClick={() => handleSort('permissions')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Access To</span>
                        {renderSortIcon()}
                      </div>
                    </th>
                    <th onClick={() => handleSort('createdAt')} style={{ cursor: 'pointer' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span>Created on</span>
                        {renderSortIcon()}
                      </div>
                    </th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {sortedRoles && sortedRoles.length > 0 ? (
                    sortedRoles.map((role) => {
                      const componentNames = role.permissions
                        ? role.permissions.map(permission => permission.componentName).join(", ")
                        : "N/A";
                      return (
                        <tr key={role.roleId}>
                          <td>{role.role || "N/A"}</td>
                          <td>{role.description || "N/A"}</td>
                          <td>{componentNames}</td>
                          <td>{role.createdAt ? formatDate(role.createdAt) : "N/A"}</td>
                          <td>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                              <span onClick={() => handleView(role.roleId)} style={{ cursor: 'pointer' }}><Icon ic={<IcDocumentViewer />} size="medium" color="primary_grey_80" /></span>
                              {/* <span onClick={() => handleUpdate(role.roleId)} style={{ marginRight: '1rem'}}><Icon ic={<IcEditPen />} size="medium" color="primary_grey_80" /></span> */}
                              {role.role && role.role.toLowerCase() !== 'admin' && (
                                <span onClick={() => handleDelete(role.roleId)} style={{ cursor: 'pointer' }}><Icon ic={<IcTrash />} size="medium" color="primary_grey_80" /></span>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan="5" style={{ textAlign: "center" }}>No roles found.</td>
                    </tr>
                  )}
                </tbody>
              </table>
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

export default Roles;
