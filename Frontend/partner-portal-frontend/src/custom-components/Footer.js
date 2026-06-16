import React from "react";
import "./Footer.css";

const Footer = ({ copyright, logo, bottomLinks, children, ...rest }) => {
  return (
    <footer className="custom-footer" {...rest}>
      <div className="footer-content">
        {/* Left side - Logo and Copyright */}
        <div className="footer-left">
          {/* Logo section */}
          {logo && (
            <div className="footer-logo">
              <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="20" cy="20" r="18" fill="#0066FF"/>
                <text x="20" y="26" fontSize="16" fontWeight="bold" fill="white" textAnchor="middle" fontFamily="Arial, sans-serif">Jio</text>
              </svg>
            </div>
          )}
          
          {/* Copyright */}
          {copyright && (
            <div className="footer-copyright">
              {copyright.href ? (
                <a
                  href={copyright.href}
                  target={copyright.newTab ? "_blank" : "_self"}
                  rel={copyright.newTab ? "noopener noreferrer" : undefined}
                  className="footer-copyright-link"
                >
                  {copyright.title}
                </a>
              ) : (
                <span>{copyright.title}</span>
              )}
            </div>
          )}
        </div>
        
        {/* Right side - Links */}
        {bottomLinks && bottomLinks.length > 0 && (
          <div className="footer-links">
            {bottomLinks.map((link, index) => (
              <a
                key={index}
                href={link.href}
                target={link.newTab ? "_blank" : "_self"}
                rel={link.newTab ? "noopener noreferrer" : undefined}
                className="footer-link"
              >
                {link.title}
              </a>
            ))}
          </div>
        )}
        
        {/* Custom children content */}
        {children}
      </div>
    </footer>
  );
};

export default Footer;

