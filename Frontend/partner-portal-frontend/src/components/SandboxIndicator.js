import React from 'react';
import { Text, ActionButton, BadgeV2 } from '../custom-components';
import { IcClose } from '../custom-components/Icon';
import { isSandboxMode, getSandboxUserData, disableSandboxMode } from '../utils/sandboxMode';
import { useNavigate } from 'react-router-dom';

const SandboxIndicator = () => {
  const navigate = useNavigate();
  
  if (!isSandboxMode()) return null;

  const userData = getSandboxUserData();

  const handleExitSandbox = () => {
    if (window.confirm('Exit sandbox mode? This will close the tab.')) {
      disableSandboxMode();
      window.close();
      // If window.close() doesn't work (some browsers block it), navigate to landing page
      setTimeout(() => {
        navigate('/');
      }, 100);
    }
  };

  return (
    <div style={{
      display: 'inline-flex',
      alignItems: 'center',
      gap: '10px',
      background: '#3535f3',
      color: '#FFFFFF',
      padding: '6px 12px',
      borderRadius: '8px',
      marginLeft: '12px',
      fontSize: '13px',
      fontWeight: '600',
      position: 'relative',
    }}>
      <style>
        {`
          @keyframes sandboxPulse {
            0%, 100% {
              transform: scale(1);
              box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
            }
            50% {
              transform: scale(1.05);
              box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
            }
          }
          
          .sandbox-exit-btn {
            background: rgba(255, 255, 255, 0.95) !important;
            border: none !important;
            border-radius: 6px !important;
            padding: 4px 10px !important;
            color: #0066FF !important;
            font-weight: 700 !important;
            font-size: 12px !important;
            cursor: pointer !important;
            transition: all 0.25s ease !important;
            display: flex !important;
            align-items: center !important;
            gap: 4px !important;
            white-space: nowrap !important;
            box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1) !important;
          }
          
          .sandbox-exit-btn:hover {
            background: #FFFFFF !important;
            color: #0052CC !important;
            transform: translateY(-1px) !important;
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15) !important;
          }
          
          .sandbox-exit-btn:active {
            transform: translateY(0) !important;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1) !important;
          }
          
          .sandbox-exit-btn svg {
            transition: transform 0.2s ease !important;
          }
          
          .sandbox-exit-btn:hover svg {
            transform: rotate(90deg) !important;
          }
          
          .sandbox-status-dot {
            width: 6px;
            height: 6px;
            background: #10B981;
            border-radius: 50%;
            animation: sandboxPulse 2s ease-in-out infinite;
            box-shadow: 0 0 6px rgba(16, 185, 129, 0.7);
          }
          
          .sandbox-divider {
            width: 1px;
            height: 18px;
            background: rgba(255, 255, 255, 0.3);
          }
          
          .sandbox-badge {
            background: rgba(255, 255, 255, 0.15);
            padding: 2px 8px;
            border-radius: 4px;
            backdrop-filter: blur(10px);
            font-size: 10px;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.3px;
          }
        `}
      </style>
      
      <div style={{ display: 'flex', alignItems: 'center', gap: '7px' }}>
        <div className="sandbox-status-dot"></div>
        <span style={{ fontWeight: '700', fontSize: '13px', color: '#FFFFFF', letterSpacing: '0.2px' }}>
          Sandbox Mode
        </span>
      </div>
      
      <div className="sandbox-divider"></div>
      
      <span className="sandbox-badge">
        {userData.name}
      </span>
      
      <button
        type="button"
        onClick={handleExitSandbox}
        className="sandbox-exit-btn"
        title="Exit Sandbox Mode"
        aria-label="Exit Sandbox Mode"
      >
        <span>Exit</span>
        <IcClose height={12} width={12} />
      </button>
    </div>
  );
};

export default SandboxIndicator;

