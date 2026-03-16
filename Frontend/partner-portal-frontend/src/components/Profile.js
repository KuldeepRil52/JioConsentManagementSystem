import React, { useState, useEffect } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useNavigate } from "react-router-dom";
import { Text, ActionButton, Avatar, Divider } from "../custom-components";
import { toast, ToastContainer } from "react-toastify";
import "react-toastify/dist/ReactToastify.css";
import "../styles/profile.css";

const Profile = () => {
  // Get user data from Redux store
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const mobile = useSelector((state) => state.common.mobile);
  const pan = useSelector((state) => state.common.pan);
  const clientId = useSelector((state) => state.common.clientId);
  const email = useSelector((state) => state.common.email);
  const spocName = useSelector((state) => state.common.spocName || "");
  const userRole = useSelector((state) => state.common.userRole);
  const totpSecret = useSelector((state) => state.common.totpSecret);

  // Format role: Remove underscores and capitalize words
  const formatRole = (role) => {
    if (!role) return "";
    return role.replace(/_/g, " ");
  };

  return (
    <div className="profile-page">
      <div className="profile-container">
        <h2 className="profile-title">Account Information</h2>

        <div className="profile-card">
          <div className="profile-field">
            <label className="profile-label">Full Name</label>
            <p className="profile-value">{spocName || "N/A"}</p>
          </div>

          <div className="profile-field">
            <label className="profile-label">Role</label>
            <p className="profile-value">{formatRole(userRole) || "N/A"}</p>
          </div>

          <div className="profile-field">
            <label className="profile-label">Email ID</label>
            <p className="profile-value">{email || "N/A"}</p>
          </div>

          <div className="profile-field">
            <label className="profile-label">Mobile No</label>
            <p className="profile-value">{mobile ? `+91 ${mobile}` : "N/A"}</p>
          </div>
          <div className="profile-field">
            <label className="profile-label">Pan Card No</label>
            <p className="profile-value">{pan ? `${pan}` : "N/A"}</p>
          </div>
          <div className="profile-field">
            <label className="profile-label">Client Id</label>
            <p className="profile-value">{clientId ? `${clientId}` : "N/A"}</p>
          </div>

          <div className="profile-field">
            <label className="profile-label">Tenant ID</label>
            <p className="profile-value">{tenantId || "N/A"}</p>
          </div>

          <div className="profile-field">
            <label className="profile-label">Business ID</label>
            <p className="profile-value">{businessId || "N/A"}</p>
          </div>

          <div className="profile-field">
            <label className="profile-label">TOTP Secret</label>
            <p className="profile-value">{totpSecret || "N/A"}</p>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Profile;

