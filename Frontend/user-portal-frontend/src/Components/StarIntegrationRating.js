import React, { useEffect, useState } from "react";
import {
  updateFeedback,
  updateIntegrationFeedback,
} from "../store/actions/CommonAction";
import { useDispatch } from "react-redux";
import { FaStar } from "react-icons/fa";
import { ICON_SIZE } from "../utils/iconSizes";

const StarIntegrationRating = ({
  grievanceId,
  initialRating,
  readOnly = false,
}) => {
  const [rating, setRating] = useState(initialRating || 0);
  const [hover, setHover] = useState(0);
  const dispatch = useDispatch();
  useEffect(() => {
    setRating(initialRating || 0);
  }, [initialRating]);

  const handleRatingClick = async (newRating) => {
    if (readOnly) return;

    setRating(newRating);

    try {
      await dispatch(updateIntegrationFeedback({ grievanceId, newRating }));
    } catch (error) {
      console.error("Error updating rating:", error);
    }
  };

  return (
    <div
      style={{
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
      }}
    >
      {Array.from({ length: 5 }, (_, i) => {
        const starNumber = i + 1;
        const isFilled = starNumber <= (hover || rating);

        return (
          <span
            key={starNumber}
            onMouseEnter={() => !readOnly && setHover(starNumber)}
            onMouseLeave={() => !readOnly && setHover(0)}
            onClick={() => handleRatingClick(starNumber)}
            style={{
              display: "inline-block",
              marginRight: 2,
              cursor: readOnly ? "default" : "pointer",
            }}
          >
            <FaStar
              size={ICON_SIZE}
              color={isFilled ? "#2563EB" : "#9CA3AF"}
              style={{
                opacity: isFilled ? 1 : 0.4,
                transition: "opacity 0.2s ease",
              }}
            />
          </span>
        );
      })}
    </div>
  );
};

export default StarIntegrationRating;
