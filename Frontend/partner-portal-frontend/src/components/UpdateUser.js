import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Select from "react-select";
import { useDispatch } from "react-redux";
import { ActionButton, Button, InputFieldV2, Text } from "../custom-components";
import { useDispatch, useSelector } from "react-redux";
import { updateUser, listUsers, listRoles, searchBusiness } from "../store/actions/CommonAction";
import "../styles/toast.css";
import { Slide, ToastContainer, toast } from "react-toastify";
import CustomToast from "./CustomToastContainer";

const UpdateUser = () => {
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const { userId } = useParams();

    // Get logged-in user's original businessId and tenantId from Redux
    const userBusinessId = useSelector((state) => state.common.user_original_business_id);
    const tenantId = useSelector((state) => state.common.tenant_id);

    const [name, setName] = useState("");
    const [designation, setDesignation] = useState("");
    const [email, setEmail] = useState("");
    const [selectedRole, setSelectedRole] = useState(null);
    const [selectedBusinessGroups, setSelectedBusinessGroups] = useState([]);

    const [roleOptions, setRoleOptions] = useState([]);
    const [businessGroupOptions, setBusinessGroupOptions] = useState([]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                // Fetch Roles and Business Groups for dropdowns
                const rolesResponse = await dispatch(listRoles());
                const roles = rolesResponse || [];
                const roleOpts = roles.map(role => ({ value: role.roleId, label: role.role }));
                setRoleOptions(roleOpts);

                const businessResponse = await dispatch(searchBusiness());
                const businessGroups = businessResponse.searchList || [];


                // Filter business groups based on user role
                // If businessId === tenantId: Super Admin - show all business groups
                // If businessId !== tenantId: Regular User - show only their business group
                let filteredBusinessGroups = businessGroups;
                if (userBusinessId && tenantId && userBusinessId !== tenantId) {
                    // Regular user - filter to show only their business group
                    filteredBusinessGroups = businessGroups.filter(bg => bg.businessId === userBusinessId);
                } else {
                    console.log("Super Admin - showing all business groups");
                }

                const businessOpts = filteredBusinessGroups.map(bg => ({ value: bg.businessId, label: bg.name }));
                setBusinessGroupOptions(businessOpts);

                // Fetch the specific user's details to pre-fill the form
                if (userId) {
                    const userResponse = await dispatch(listUsers());
                    const allUsers = userResponse || [];
                    const userData = allUsers.find(user => user.userId === userId);
                    if (userData) {
                        setName(userData.username || "");
                        setDesignation(userData.designation || "");
                        // Set email or mobile - check both fields
                        setEmail(userData.email || userData.mobile || "");

                        // Pre-select role and business groups
                        const userRoles = userData.roles || [];
                        if (userRoles.length > 0) {
                            const roleId = userRoles[0].roleId;
                            const currentRole = roleOpts.find(r => r.value === roleId);
                            setSelectedRole(currentRole);

                            const businessGroupIds = userRoles.map(r => r.businessId);
                            const currentBusinessGroups = businessOpts.filter(b => businessGroupIds.includes(b.value));
                            setSelectedBusinessGroups(currentBusinessGroups);
                        }
                    }
                }
            } catch (error) {
                console.error("Failed to fetch data:", error);
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
    }, [dispatch, userId]);

    const handleBack = () => {
        navigate("/users");
    };

    const handleSubmit = async () => {
        if (!name || !email || !designation || !selectedRole || selectedBusinessGroups.length === 0) {
            toast.error(
                (props) => (
                    <CustomToast
                        {...props}
                        type="error"
                        message={"Please fill out all required fields."}
                    />
                ),
                { icon: false }
            );
            return;
        }

        const emailOrPhoneRegex = /(^\d{10}$)|(^\S+@\S+\.\S+$)/;
        if (!emailOrPhoneRegex.test(email)) {
            toast.error("Please enter a valid 10-digit mobile number or an email address.");
            toast.error(
                (props) => (
                    <CustomToast
                        {...props}
                        type="error"
                        message={"Please enter a valid 10-digit mobile number or an email address."}
                    />
                ),
                { icon: false }
            );
            return;
        }

        try {
            const allUsersResponse = await dispatch(listUsers());
            const allUsers = allUsersResponse || [];
            if (allUsers.some(user => user.username.toLowerCase() === name.toLowerCase() && user.userId !== userId)) {
                toast.error("A user with this name already exists.");
                return;
            }

            const payload = {
                username: name,
                email: email,
                designation: designation,
                roles: selectedBusinessGroups.map(bg => ({
                    roleId: selectedRole.value,
                    businessId: bg.value
                }))
            };

            const response = await dispatch(updateUser(userId, payload));
            if (response && (response.status === 200 || response.status === 201)) {
                toast.success(
                    (props) => (
                        <CustomToast
                            {...props}
                            type="success"
                            message={"User updated successfully!"}
                        />
                    ),
                    { icon: false }
                );
                // Add delay before navigation to ensure toast is visible
                setTimeout(() => {
                    navigate("/users");
                }, 1000);
            } else {
                toast.error(
                    (props) => (
                        <CustomToast
                            {...props}
                            type="error"
                            message={"Failed to update user. Please try again."}
                        />
                    ),
                    { icon: false }
                );
            }
        } catch (error) {
            console.error("Error updating user:", error);
            let errorMessage = "An error occurred while updating the user. Please try again.";

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
            <div className="configurePage" style={{ padding: '2rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', marginBottom: '2rem' }}>
                    <Button icon="ic_back" kind="secondary" onClick={handleBack} />
                    <Text appearance="heading-xs" color="primary-grey-100" style={{ marginLeft: '1rem' }}>Update user</Text>
                </div>

                <div style={{ width: '50%' }}>
                    <InputFieldV2
                        label="Name (Required)"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                    />
                    <br />
                    <InputFieldV2
                        label="Designation (Required)"
                        value={designation}
                        onChange={(e) => setDesignation(e.target.value)}
                    />
                    <br />
                    <InputFieldV2
                        label="Mobile number or Email (Required)"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                    />
                    <br />
                    <Text as="p" appearance="body-xs" color="primary-grey-80" style={{ marginBottom: '0.5rem' }}>Assign role</Text>
                    <Select
                        options={roleOptions}
                        value={selectedRole}
                        onChange={setSelectedRole}
                        isLoading={roleOptions.length === 0}
                    />
                    <br />
                    <Text as="p" appearance="body-xs" color="primary-grey-80" style={{ marginBottom: '0.5rem' }}>Select business group (Required)</Text>
                    <Select
                        isMulti
                        options={businessGroupOptions}
                        value={selectedBusinessGroups}
                        onChange={setSelectedBusinessGroups}
                        isLoading={businessGroupOptions.length === 0}
                    />
                    <Text as="p" appearance="body-xs" color="primary-grey-80" style={{ marginTop: '0.5rem' }}>Choose business groups where this user should have this role</Text>

                    <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '2rem' }}>
                        <ActionButton label="Update" kind="primary" onClick={handleSubmit} />
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

export default UpdateUser;