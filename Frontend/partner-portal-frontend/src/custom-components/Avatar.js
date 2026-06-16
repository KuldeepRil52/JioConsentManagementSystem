import React from "react";
import "./Avatar.css";

/**
 * Custom Avatar Component - Mimics JDS Avatar
 * 
 * Props:
 * - kind: image, initials, icon
 * - initials: string for initials (e.g., "AB")
 * - src: image URL
 * - size: small, medium, large, xtra-large
 * - onClick: click handler
 * - className: additional CSS classes
 * - style: inline styles
 */

const Avatar = ({ 
  kind = "initials", 
  initials = "U",
  src = null,
  size = "medium",
  onClick = null,
  className = "",
  style = {},
  ...props 
}) => {
  const renderContent = () => {
    if (kind === "image" && src) {
      return <img src={src} alt="Avatar" className="avatar-image" />;
    }
    
    if (kind === "initials" || !src) {
      return <span className="avatar-initials">{initials}</span>;
    }
    
    return <span className="avatar-icon">👤</span>;
  };

  return (
    <div
      className={`custom-avatar custom-avatar-${size} ${onClick ? 'custom-avatar-clickable' : ''} ${className}`}
      onClick={onClick}
      style={style}
      {...props}
    >
      {renderContent()}
    </div>
  );
};

export default Avatar;

// AvatarV2 is an alias for Avatar (no functional difference in custom components)
export const AvatarV2 = Avatar;

