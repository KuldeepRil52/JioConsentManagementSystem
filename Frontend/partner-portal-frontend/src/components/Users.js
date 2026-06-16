import React, { useState, useEffect } from "react";
import { useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Text, ActionButton, Icon } from "../custom-components";
import { IcEditPen, IcTrash, IcChevronDown, IcChevronUp } from "../custom-components/Icon";
import { IcEditPen, IcTrash, IcChevronDown, IcChevronUp, IcSort } from "../custom-components/Icon";
import { IcDocumentViewer } from "../custom-components/Icon";
import { listUsers, deleteUser, searchBusiness, searchRoles } from "../store/actions/CommonAction";
import { toast } from 'react-toastify';
import "../styles/businessGroups.css";
import "../styles/pageConfiguration.css";

const Users = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [users, setUsers] = useState([]);
  const [expandedRowId, setExpandedRowId] = useState(null);
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState('asc');

  const sortedUsers = React.useMemo(() => {
    if (!sortColumn) return users;

    return [...users].sort((a, b) => {
      const aValue = a[sortColumn] || '';
      const bValue = b[sortColumn] || '';

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [users, sortColumn, sortDirection]);


  useEffect(() => {
    const fetchUsersAndDetails = async () => {
      try {
        const usersResponse = await dispatch(listUsers());
        const fetchedUsers = usersResponse || [];

        const usersWithDetails = await Promise.all(
          fetchedUsers.map(async (user) => {
            const rolesWithDetails = await Promise.all(
              (user.roles || []).map(async (role) => {
                let businessName = 'N/A';
                let roleName = 'N/A';

                // Fetch Business Group Name
                try {
                  const businessResponse = await dispatch(searchBusiness(role.businessId));
                  // Handle both array response (searchList) and single object response
                  businessName = businessResponse?.searchList?.[0]?.name ||
                    businessResponse?.name ||
                    'N/A';
                } catch (error) {
                  console.error("Failed to fetch business details for", role.businessId, ":", error);
                }

                // Fetch Role Name
                try {
                  const roleResponse = await dispatch(searchRoles(role.roleId));
                  roleName = roleResponse?.role || 'N/A';
                } catch (error) {
                  console.error("Failed to fetch role details for", role.roleId, ":", error);
                }

                return { ...role, businessName, roleName };
              })
            );
            return { ...user, roles: rolesWithDetails };
          })
        );
        setUsers(usersWithDetails);
      } catch (error) {
        console.error("Failed to fetch users or their details:", error);
        setUsers([]);
      }
    };
    fetchUsersAndDetails();
  }, [dispatch]);

  const handleRowExpand = (userId) => {
    setExpandedRowId(expandedRowId === userId ? null : userId);
  };

  const handleAddUserClick = () => {
    navigate("/addUser");
  };

  const handleUpdate = (e, userId) => {
    e.stopPropagation(); // Prevent row from expanding when clicking icon
    navigate(`/updateUser/${userId}`);
  };

  const handleDelete = async (e, userId) => {
    e.stopPropagation(); // Prevent row from expanding when clicking icon
    if (window.confirm("Are you sure you want to delete this user?")) {
      try {
        await dispatch(deleteUser(userId));
        setUsers(users.filter(user => user.userId !== userId));
        toast.success("User deleted successfully!");
      } catch (error) {
        toast.error("An error occurred while deleting the user.");
      }
    }
  };

  const handleView = (e, userId) => {
    e.stopPropagation(); // Prevent row from expanding when clicking icon
    navigate(`/addUser?mode=view&id=${userId}`);
  };

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortColumn(column);
      setSortDirection('asc');
    }
  };

  const renderSortIcon = (column) => {
    return <Icon ic={<IcSort />} size="small" color="black" />;
  };

  return (
    <div className="configurePage">
      <div className="bs-page">
        <div className="main-heading-ct">
          <div className="bs-heading">
            <Text appearance="heading-s" color="primary-grey-100">Users</Text>
            <div className="tag">
              <Text appearance="body-xs-bold" color="primary-grey-80">User Management</Text>
            </div>
          </div>
          <div className="bs-button" style={{ marginRight: "5%" }}>
            <ActionButton label="Add user" onClick={handleAddUserClick} kind="primary" />
          </div>
        </div>

        <div className="bs-content">
          <div className="business-table-container">
            <table className="business-table">
              <thead>
                <tr>
                  <th style={{ width: '5%' }}></th>
                  <th onClick={() => handleSort('username')} style={{ cursor: 'pointer' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>Name</span>
                      {renderSortIcon('username')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('designation')} style={{ cursor: 'pointer' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>Designation</span>
                      {renderSortIcon('designation')}
                    </div>
                  </th>
                  <th onClick={() => handleSort('email')} style={{ cursor: 'pointer' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span>Email or mobile</span>
                      {renderSortIcon('email')}
                    </div>
                  </th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {sortedUsers && sortedUsers.length > 0 ? (
                  sortedUsers.map((user) => (
                    <React.Fragment key={user.userId}>
                      <tr onClick={() => handleRowExpand(user.userId)} style={{ cursor: 'pointer' }}>
                        <td>
                          <Icon ic={expandedRowId === user.userId ? <IcChevronUp /> : <IcChevronDown />} />
                        </td>
                        <td>{user.username || "N/A"}</td>
                        <td>{user.designation || "N/A"}</td>
                        <td>{user.email || user.mobile || "N/A"}</td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                            <span onClick={(e) => handleView(e, user.userId)} style={{ cursor: 'pointer' }}><Icon ic={<IcDocumentViewer />} size="medium" color="primary_grey_80" /></span>
                            <span onClick={(e) => handleUpdate(e, user.userId)} style={{ cursor: 'pointer' }}><Icon ic={<IcEditPen />} size="medium" color="primary_grey_80" /></span>
                            {!user.roles.some(role => role.roleName && role.roleName.toLowerCase() === 'admin') && (
                              <span onClick={(e) => handleDelete(e, user.userId)} style={{ cursor: 'pointer' }}><Icon ic={<IcTrash />} size="medium" color="primary_grey_80" /></span>
                            )}
                          </div>
                        </td>
                      </tr>
                      {expandedRowId === user.userId && (
                        <tr className="expanded-row">
                          <td colSpan="5">
                            <div style={{ padding: '1rem', backgroundColor: '#f9f9f9' }}>
                              <Text as="p" appearance="body-s-bold">Business Groups & Roles</Text>
                              <table style={{ width: '100%', marginTop: '0.5rem' }}>
                                <thead>
                                  <tr>
                                    <th style={{ textAlign: 'left' }}>Business group</th>
                                    <th style={{ textAlign: 'left' }}>Role</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {(user.roles || []).map((role, index) => (
                                    <tr key={index}>
                                      <td>{role.businessName || 'N/A'}</td>
                                      <td>{role.roleName || 'N/A'}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  ))
                ) : (
                  <tr>
                    <td colSpan="5" style={{ textAlign: "center" }}>No users found.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Users;