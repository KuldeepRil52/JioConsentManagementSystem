import React, { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Button, Text } from '../custom-components';
import { IcClose } from '../custom-components/Icon';
import '../styles/guidedTour.css';

/**
 * GuidedTour Component
 * 
 * A reusable component that provides step-by-step guided tours for different pages.
 * 
 * @param {Array} steps - Array of tour step objects
 * @param {string} tourId - Unique identifier for this tour (used for localStorage)
 * @param {boolean} showOnFirstVisit - Whether to show tour automatically on first visit
 * @param {function} onComplete - Callback when tour is completed
 * @param {function} onSkip - Callback when tour is skipped
 */
const GuidedTour = ({ 
  steps = [], 
  tourId = 'default-tour',
  showOnFirstVisit = true,
  onComplete,
  onSkip 
}) => {
  const [isActive, setIsActive] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [targetPosition, setTargetPosition] = useState(null);
  const [isDragging, setIsDragging] = useState(false);
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
  const [tooltipPosition, setTooltipPosition] = useState(null);
  const overlayRef = useRef(null);
  const tooltipRef = useRef(null);
  const dragStartPos = useRef({ x: 0, y: 0 });
  const actionExecutedRef = useRef(false);

  // Check if user has seen this tour before
  useEffect(() => {
    const tourCompleted = localStorage.getItem(`tour_${tourId}_completed`);
    
    if (showOnFirstVisit && !tourCompleted && steps.length > 0) {
      // Small delay to ensure DOM is ready
      setTimeout(() => {
        setIsActive(true);
        updateTargetPosition();
      }, 500);
    }
  }, [tourId, showOnFirstVisit, steps.length]);

  // Listen for custom event to restart tour
  useEffect(() => {
    const handleRestartTour = (event) => {
      if (event.detail.tourId === tourId) {
        restartTour();
      }
    };

    window.addEventListener('restartTour', handleRestartTour);
    return () => {
      window.removeEventListener('restartTour', handleRestartTour);
    };
  }, [tourId]);

  // Update target position when step changes
  useEffect(() => {
    if (isActive) {
      actionExecutedRef.current = false; // Reset action flag for new step
      updateTargetPosition();
    }
  }, [currentStep, isActive]);

  // Update position on window resize only (removed scroll listeners to prevent flickering)
  useEffect(() => {
    if (isActive) {
      const handleResize = () => {
        updateTargetPosition();
      };
      
      window.addEventListener('resize', handleResize);
      
      return () => {
        window.removeEventListener('resize', handleResize);
      };
    }
  }, [isActive, currentStep]);

  const updateTargetPosition = () => {
    if (!steps[currentStep]?.target) {
      setTargetPosition(null);
      // Still execute action if present (for center steps) - but only once
      if (steps[currentStep]?.action && !actionExecutedRef.current) {
        actionExecutedRef.current = true;
        setTimeout(() => {
          steps[currentStep].action(null);
        }, 50);
      }
      return;
    }

    const element = document.querySelector(steps[currentStep].target);
    if (element) {
      // Execute action callback if present (e.g., to click accordion) - but only once
      if (steps[currentStep]?.action && !actionExecutedRef.current) {
        actionExecutedRef.current = true;
        steps[currentStep].action(element);
        // Wait for DOM to update after action
        setTimeout(() => {
          const updatedElement = document.querySelector(steps[currentStep].target);
          if (updatedElement) {
            updatedElement.scrollIntoView({
              behavior: 'auto',
              block: 'center',
              inline: 'nearest'
            });
            // Update position after scroll - reuse the same positioning logic
            setTimeout(() => {
              const rect = updatedElement.getBoundingClientRect();
              
              const isFixedOrSticky = (el) => {
                while (el && el !== document.body) {
                  const style = window.getComputedStyle(el);
                  if (style.position === 'fixed' || style.position === 'sticky') {
                    return true;
                  }
                  el = el.parentElement;
                }
                return false;
              };
              
              const isFixed = isFixedOrSticky(updatedElement);
              const highlightPadding = 8;
              const viewportWidth = window.innerWidth;
              const viewportHeight = window.innerHeight;
              
              let adjustedLeft = rect.left;
              let adjustedTop = rect.top;
              let adjustedWidth = rect.width;
              let adjustedHeight = rect.height;
              
              if (rect.right + highlightPadding > viewportWidth) {
                adjustedWidth = Math.max(0, viewportWidth - rect.left - highlightPadding);
              }
              if (rect.left - highlightPadding < 0) {
                adjustedLeft = highlightPadding;
                adjustedWidth = Math.min(rect.width, rect.right - highlightPadding);
              }
              if (rect.bottom + highlightPadding > viewportHeight) {
                adjustedHeight = Math.max(0, viewportHeight - rect.top - highlightPadding);
              }
              if (rect.top - highlightPadding < 0) {
                adjustedTop = highlightPadding;
                adjustedHeight = Math.min(rect.height, rect.bottom - highlightPadding);
              }
              
              setTargetPosition({
                top: adjustedTop,
                left: adjustedLeft,
                width: adjustedWidth,
                height: adjustedHeight,
                right: adjustedLeft + adjustedWidth,
                bottom: adjustedTop + adjustedHeight,
                viewportTop: adjustedTop,
                viewportLeft: adjustedLeft,
                viewportRight: adjustedLeft + adjustedWidth,
                viewportBottom: adjustedTop + adjustedHeight,
                isFixed: isFixed,
              });
            }, 100);
          }
        }, 100);
        return; // Exit early, position will be updated after action completes
      }

      // Auto-scroll the element into view (when no action) - only once per step
      if (!actionExecutedRef.current) {
        actionExecutedRef.current = true;
        element.scrollIntoView({
          behavior: 'auto',
          block: 'center',
          inline: 'nearest'
        });
      }

      // Wait for scroll to complete before updating position
      setTimeout(() => {
        const rect = element.getBoundingClientRect();
        
        // Check if element or any parent is fixed or sticky
        const isFixedOrSticky = (el) => {
          while (el && el !== document.body) {
            const style = window.getComputedStyle(el);
            if (style.position === 'fixed' || style.position === 'sticky') {
              return true;
            }
            el = el.parentElement;
          }
          return false;
        };
        
        const isFixed = isFixedOrSticky(element);
        
        // Ensure highlight stays within viewport bounds
        // The highlight adds 8px padding around the element, so we need to account for that
        const highlightPadding = 8;
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        
        let adjustedLeft = rect.left;
        let adjustedTop = rect.top;
        let adjustedWidth = rect.width;
        let adjustedHeight = rect.height;
        
        // Check if highlight would go off the right edge (element right + padding)
        if (rect.right + highlightPadding > viewportWidth) {
          const availableWidth = viewportWidth - rect.left - highlightPadding;
          adjustedWidth = Math.max(0, availableWidth);
        }
        
        // Check if highlight would go off the left edge (element left - padding)
        if (rect.left - highlightPadding < 0) {
          adjustedLeft = highlightPadding;
          adjustedWidth = Math.min(rect.width, rect.right - highlightPadding);
        }
        
        // Check if highlight would go off the bottom edge (element bottom + padding)
        if (rect.bottom + highlightPadding > viewportHeight) {
          const availableHeight = viewportHeight - rect.top - highlightPadding;
          adjustedHeight = Math.max(0, availableHeight);
        }
        
        // Check if highlight would go off the top edge (element top - padding)
        if (rect.top - highlightPadding < 0) {
          adjustedTop = highlightPadding;
          adjustedHeight = Math.min(rect.height, rect.bottom - highlightPadding);
        }
        
        // Always use viewport coordinates since we're using fixed positioning
        // The highlight and tooltip are both fixed, so they need viewport coordinates
        setTargetPosition({
          top: adjustedTop,
          left: adjustedLeft,
          width: adjustedWidth,
          height: adjustedHeight,
          right: adjustedLeft + adjustedWidth,
          bottom: adjustedTop + adjustedHeight,
          // Store viewport positions for fixed positioning
          viewportTop: adjustedTop,
          viewportLeft: adjustedLeft,
          viewportRight: adjustedLeft + adjustedWidth,
          viewportBottom: adjustedTop + adjustedHeight,
          isFixed: isFixed, // Track if element is fixed
        });
      }, 50); // Minimal delay for instant response
    } else {
      setTargetPosition(null);
    }
  };

  const handleNext = () => {
    if (currentStep < steps.length - 1) {
      setCurrentStep(currentStep + 1);
    } else {
      completeTour();
    }
  };

  const handlePrevious = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
    }
  };

  const handleSkip = () => {
    setIsActive(false);
    setCurrentStep(0);
    if (onSkip) onSkip();
  };

  const completeTour = () => {
    localStorage.setItem(`tour_${tourId}_completed`, 'true');
    setIsActive(false);
    setCurrentStep(0);
    if (onComplete) onComplete();
  };

  const restartTour = () => {
    setCurrentStep(0);
    setIsActive(true);
    updateTargetPosition();
  };

  // Drag handlers
  const handleDragStart = (e) => {
    if (e.target.closest('.guided-tour-drag-handle')) {
      // Check if tooltipRef is available before accessing it
      if (!tooltipRef.current) return;
      
      setIsDragging(true);
      const rect = tooltipRef.current.getBoundingClientRect();
      dragStartPos.current = {
        x: e.clientX - rect.left,
        y: e.clientY - rect.top
      };
      e.preventDefault();
    }
  };

  const handleDragMove = (e) => {
    if (isDragging) {
      const newX = e.clientX - dragStartPos.current.x;
      const newY = e.clientY - dragStartPos.current.y;
      
      setDragOffset({ x: newX, y: newY });
      e.preventDefault();
    }
  };

  const handleDragEnd = () => {
    setIsDragging(false);
  };

  // Add event listeners for dragging
  useEffect(() => {
    if (isDragging) {
      window.addEventListener('mousemove', handleDragMove);
      window.addEventListener('mouseup', handleDragEnd);
      return () => {
        window.removeEventListener('mousemove', handleDragMove);
        window.removeEventListener('mouseup', handleDragEnd);
      };
    }
  }, [isDragging]);

  // Reset drag offset when step changes
  useEffect(() => {
    setDragOffset({ x: 0, y: 0 });
  }, [currentStep]);

  // Calculate tooltip position using viewport coordinates
  const getTooltipPosition = () => {
    // If dragged, use absolute positioning
    if (dragOffset.x !== 0 || dragOffset.y !== 0) {
      return {
        left: `${dragOffset.x}px`,
        top: `${dragOffset.y}px`,
        transform: 'none',
        right: 'auto',
        bottom: 'auto',
      };
    }

    if (!targetPosition) return { top: '50%', left: '50%', transform: 'translate(-50%, -50%)', right: 'auto', bottom: 'auto' };

    const step = steps[currentStep];
    const position = step.position || 'bottom';
    const offset = 20;
    const tooltipWidth = 420; // Account for max-width
    const tooltipHeight = 300; // Account for dynamic content height
    const safetyMargin = 30; // Increased safety margin

    const viewportHeight = window.innerHeight;
    const viewportWidth = window.innerWidth;
    
    // Calculate actual tooltip width based on viewport
    const actualTooltipWidth = Math.min(tooltipWidth, viewportWidth - 2 * safetyMargin);

    // Use viewport positions (these stay fixed relative to viewport)
    let style = {
      right: 'auto',
      bottom: 'auto',
    };

    switch (position) {
      case 'top':
        style = {
          top: `${targetPosition.viewportTop - offset}px`,
          left: `${targetPosition.viewportLeft + targetPosition.width / 2}px`,
          transform: 'translate(-50%, -100%)',
        };
        // Fallback to bottom if not enough space
        if (targetPosition.viewportTop - tooltipHeight < 0) {
          style.top = `${targetPosition.viewportBottom + offset}px`;
          style.transform = 'translate(-50%, 0)';
        }
        break;
      case 'bottom':
        style = {
          top: `${targetPosition.viewportBottom + offset}px`,
          left: `${targetPosition.viewportLeft + targetPosition.width / 2}px`,
          transform: 'translate(-50%, 0)',
        };
        // Fallback to top if not enough space
        if (targetPosition.viewportBottom + tooltipHeight > viewportHeight) {
          style.top = `${targetPosition.viewportTop - offset}px`;
          style.transform = 'translate(-50%, -100%)';
        }
        break;
      case 'left':
        style = {
          top: `${targetPosition.viewportTop + targetPosition.height / 2}px`,
          left: `${targetPosition.viewportLeft - offset}px`,
          transform: 'translate(-100%, -50%)',
        };
        // Fallback to right if not enough space
        if (targetPosition.viewportLeft - tooltipWidth < 0) {
          style.left = `${targetPosition.viewportRight + offset}px`;
          style.transform = 'translate(0, -50%)';
        }
        break;
      case 'right':
        style = {
          top: `${targetPosition.viewportTop + targetPosition.height / 2}px`,
          left: `${targetPosition.viewportRight + offset}px`,
          transform: 'translate(0, -50%)',
        };
        // Fallback to left if not enough space
        if (targetPosition.viewportRight + tooltipWidth > viewportWidth) {
          style.left = `${targetPosition.viewportLeft - offset}px`;
          style.transform = 'translate(-100%, -50%)';
        }
        break;
      case 'center':
      default:
        style = {
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
        };
    }

    // Enhanced viewport boundary checking for tooltip
    if (style.left && !style.left.includes('%')) {
      const leftPos = parseFloat(style.left);
      let adjustedLeft = leftPos;
      let adjustedTransform = style.transform;
      
      // Calculate the actual left position based on transform
      if (adjustedTransform.includes('translate(-50%')) {
        // Centered: actual left = leftPos - tooltipWidth/2
        const actualLeft = leftPos - tooltipWidth / 2;
        if (actualLeft < safetyMargin) {
          adjustedLeft = safetyMargin + tooltipWidth / 2;
          style.left = `${adjustedLeft}px`;
        } else if (actualLeft + tooltipWidth > viewportWidth - safetyMargin) {
          adjustedLeft = viewportWidth - tooltipWidth - safetyMargin + tooltipWidth / 2;
          style.left = `${adjustedLeft}px`;
        }
      } else if (adjustedTransform.includes('translate(-100%')) {
        // Right-aligned: actual left = leftPos - tooltipWidth
        const actualLeft = leftPos - tooltipWidth;
        if (actualLeft < safetyMargin) {
          // Not enough space on left, force it to stay within bounds
          style.left = `${viewportWidth - safetyMargin}px`;
          style.transform = style.transform.replace('translate(-100%', 'translate(-100%');
        }
      } else if (adjustedTransform.includes('translate(0')) {
        // Left-aligned: actual left = leftPos
        if (leftPos < safetyMargin) {
          style.left = `${safetyMargin}px`;
        } else if (leftPos + tooltipWidth > viewportWidth - safetyMargin) {
          // Force it to stay within right edge
          style.left = `${viewportWidth - tooltipWidth - safetyMargin}px`;
        }
      }
    }
    
    // Ensure tooltip stays within viewport vertically
    if (style.top && !style.top.includes('%')) {
      const topPos = parseFloat(style.top);
      if (style.transform.includes('translate(-50%, -100%)') || style.transform.includes('translate(0, -100%)') || style.transform.includes('translate(-100%, -100%)')) {
        // Top-aligned (transform moves it up)
        const actualTop = topPos - tooltipHeight;
        if (actualTop < safetyMargin) {
          style.top = `${safetyMargin + tooltipHeight}px`;
        }
      } else if (style.transform.includes('translate(-50%, -50%)') || style.transform.includes('translate(0, -50%)') || style.transform.includes('translate(-100%, -50%)')) {
        // Center-aligned vertically
        const actualTop = topPos - tooltipHeight / 2;
        if (actualTop < safetyMargin) {
          style.top = `${safetyMargin + tooltipHeight / 2}px`;
        } else if (actualTop + tooltipHeight > viewportHeight - safetyMargin) {
          style.top = `${viewportHeight - tooltipHeight - safetyMargin + tooltipHeight / 2}px`;
        }
      } else {
        // Bottom-aligned
        if (topPos + tooltipHeight > viewportHeight - safetyMargin) {
          style.top = `${viewportHeight - tooltipHeight - safetyMargin}px`;
        } else if (topPos < safetyMargin) {
          style.top = `${safetyMargin}px`;
        }
      }
    }

    // FINAL AGGRESSIVE CLAMP - ensure tooltip absolutely stays within viewport
    // Use max-width constraint directly in the style to prevent overflow
    style.maxWidth = `${actualTooltipWidth}px`;
    style.width = 'auto';
    
    // Completely rewrite positioning logic to use simple non-transform approach
    // This eliminates all transform-based positioning issues
    if (style.left && !style.left.includes('%')) {
      let calculatedLeft = parseFloat(style.left);
      let calculatedTop = parseFloat(style.top) || 0;
      
      // Remove transform and calculate absolute position
      if (style.transform) {
        // Account for transform in the position
        if (style.transform.includes('translate(-100%')) {
          calculatedLeft = calculatedLeft - actualTooltipWidth;
        } else if (style.transform.includes('translate(-50%')) {
          calculatedLeft = calculatedLeft - actualTooltipWidth / 2;
        }
        
        if (style.transform.includes('-100%)')) {
          calculatedTop = calculatedTop - tooltipHeight;
        } else if (style.transform.includes('-50%)')) {
          calculatedTop = calculatedTop - tooltipHeight / 2;
        }
      }
      
      // Now clamp to viewport bounds
      calculatedLeft = Math.max(safetyMargin, Math.min(calculatedLeft, viewportWidth - actualTooltipWidth - safetyMargin));
      calculatedTop = Math.max(safetyMargin, Math.min(calculatedTop, viewportHeight - tooltipHeight - safetyMargin));
      
      // Apply the final constrained position without transform
      style.left = `${calculatedLeft}px`;
      style.top = `${calculatedTop}px`;
      style.transform = 'none';
      style.right = 'auto';
      style.bottom = 'auto';
    }

    return style;
  };

  if (!isActive || steps.length === 0) {
    return null;
  }

  const currentStepData = steps[currentStep];

  return (
    <AnimatePresence>
      {isActive && (
        <>
          {/* Overlay with spotlight */}
          <motion.div
            ref={overlayRef}
            className="guided-tour-overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            {/* SVG Mask for spotlight effect */}
            {targetPosition && (() => {
              const maskLeft = Math.max(0, targetPosition.viewportLeft - 8);
              const maskTop = Math.max(0, targetPosition.viewportTop - 8);
              const maxMaskWidth = window.innerWidth - maskLeft - 16; // 16px safety margin
              const maxMaskHeight = window.innerHeight - maskTop - 16; // 16px safety margin
              const maskWidth = Math.min(targetPosition.width + 16, maxMaskWidth);
              const maskHeight = Math.min(targetPosition.height + 16, maxMaskHeight);
              
              return (
                <svg className="guided-tour-mask">
                  <defs>
                    <mask id={`spotlight-${tourId}`}>
                      <rect width="100%" height="100%" fill="white" />
                      <rect
                        x={maskLeft}
                        y={maskTop}
                        width={Math.max(0, maskWidth)}
                        height={Math.max(0, maskHeight)}
                        rx="8"
                        fill="black"
                      />
                    </mask>
                  </defs>
                  <rect
                    width="100%"
                    height="100%"
                    fill="rgba(0, 0, 0, 0.7)"
                    mask={`url(#spotlight-${tourId})`}
                  />
                </svg>
              );
            })()}

            {/* Highlighted element border */}
            {targetPosition && (() => {
              const highlightLeft = Math.max(0, targetPosition.viewportLeft - 8);
              const highlightTop = Math.max(0, targetPosition.viewportTop - 8);
              const maxWidth = window.innerWidth - highlightLeft - 16; // 16px safety margin
              const maxHeight = window.innerHeight - highlightTop - 16; // 16px safety margin
              const highlightWidth = Math.min(targetPosition.width + 16, maxWidth);
              const highlightHeight = Math.min(targetPosition.height + 16, maxHeight);
              
              return (
                <motion.div
                  className="guided-tour-highlight"
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ duration: 0.3 }}
                  style={{
                    top: `${highlightTop}px`,
                    left: `${highlightLeft}px`,
                    width: `${Math.max(0, highlightWidth)}px`,
                    height: `${Math.max(0, highlightHeight)}px`,
                    right: 'auto',
                    bottom: 'auto',
                  }}
                />
              );
            })()}
          </motion.div>

          {/* Tooltip */}
          <motion.div
            ref={tooltipRef}
            className={`guided-tour-tooltip ${isDragging ? 'dragging' : ''}`}
            initial={{ opacity: 0, scale: 0.9, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: -10 }}
            transition={{ duration: 0.3, ease: "easeOut" }}
            style={getTooltipPosition()}
            onMouseDown={handleDragStart}
          >
            {/* Header with drag handle */}
            <div className="guided-tour-header">
              <div className="guided-tour-drag-handle">
                <div className="drag-handle-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                    <circle cx="8" cy="6" r="1.5" fill="currentColor" />
                    <circle cx="8" cy="12" r="1.5" fill="currentColor" />
                    <circle cx="8" cy="18" r="1.5" fill="currentColor" />
                    <circle cx="16" cy="6" r="1.5" fill="currentColor" />
                    <circle cx="16" cy="12" r="1.5" fill="currentColor" />
                    <circle cx="16" cy="18" r="1.5" fill="currentColor" />
                  </svg>
                </div>
                <div className="guided-tour-step-indicator">
                  <Text appearance="body-xxs" color="primary-white-80">
                    Step {currentStep + 1} of {steps.length}
                  </Text>
                </div>
              </div>
              
              {/* Close button */}
              <button type="button" className="guided-tour-close" onClick={handleSkip}>
                <IcClose size="small" />
              </button>
            </div>

            {/* Content */}
            <div className="guided-tour-content">
              {currentStepData.title && (
                <h3 className="guided-tour-title">
                  {currentStepData.title}
                </h3>
              )}
              {currentStepData.description && (
                <p className="guided-tour-description">
                  {currentStepData.description}
                </p>
              )}
              {currentStepData.image && (
                <img 
                  src={currentStepData.image} 
                  alt={currentStepData.title} 
                  className="guided-tour-image"
                />
              )}
            </div>

            {/* Progress dots */}
            <div className="guided-tour-progress">
              {steps.map((_, index) => (
                <div
                  key={index}
                  className={`guided-tour-dot ${index === currentStep ? 'active' : ''} ${index < currentStep ? 'completed' : ''}`}
                  onClick={() => setCurrentStep(index)}
                  title={`Step ${index + 1}`}
                />
              ))}
            </div>

            {/* Footer with navigation buttons */}
            <div className="guided-tour-footer">
              <div className="guided-tour-actions">
                <Button
                  kind="secondary"
                  size="small"
                  label="Skip"
                  onClick={handleSkip}
                />
                <div className="guided-tour-nav-buttons">
                  {currentStep > 0 && (
                    <Button
                      kind="secondary"
                      size="small"
                      label="Previous"
                      onClick={handlePrevious}
                    />
                  )}
                  <Button
                    kind="primary"
                    size="small"
                    label={currentStep === steps.length - 1 ? 'Finish' : 'Next'}
                    onClick={handleNext}
                  />
                </div>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};

// Helper function to manually trigger a tour
export const startTour = (tourId) => {
  localStorage.removeItem(`tour_${tourId}_completed`);
  // Dispatch custom event to restart tour without page reload
  const event = new CustomEvent('restartTour', { detail: { tourId } });
  window.dispatchEvent(event);
};

// Helper function to check if tour is completed
export const isTourCompleted = (tourId) => {
  return localStorage.getItem(`tour_${tourId}_completed`) === 'true';
};

// Helper function to reset all tours
export const resetAllTours = () => {
  const keys = Object.keys(localStorage);
  keys.forEach(key => {
    if (key.startsWith('tour_') && key.endsWith('_completed')) {
      localStorage.removeItem(key);
    }
  });
};

export default GuidedTour;

