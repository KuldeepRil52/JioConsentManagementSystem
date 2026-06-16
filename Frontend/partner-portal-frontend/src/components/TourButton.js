import React from 'react';
import { ActionButton, Button } from '../custom-components';
import { IcHelp } from '../custom-components/Icon';
import { startTour } from './GuidedTour';
import '../styles/tourButton.css';

/**
 * TourButton Component
 * 
 * A floating button that allows users to restart the guided tour at any time
 */
const TourButton = ({ tourId, label = "Help", position = "bottom-right" }) => {
  const handleClick = () => {
    startTour(tourId);
  };

  const positionStyles = {
    'bottom-right': { bottom: '100px', right: '24px' },
    'bottom-left': { bottom: '100px', left: '24px' },
    'top-right': { top: '80px', right: '24px' },
    'top-left': { top: '80px', left: '24px' },
  };

  return (
    <button
    type='button'
      className="tour-button"
      onClick={handleClick}
      style={positionStyles[position]}
      title="Start guided tour"
      aria-label="Start guided tour"
      data-tour-button={tourId}
    >
      <span className="tour-button-label">{label}</span>
    </button>
  );
};

export default TourButton;

