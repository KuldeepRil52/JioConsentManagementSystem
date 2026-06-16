import React from "react";
import { useNavigate } from "react-router-dom";
import { Text, ActionButton } from "../custom-components";
import { IcJioDot } from "../custom-components/Icon";
import "../styles/contactUs.css";

const ContactUs = () => {
  const navigate = useNavigate();

  return (
    <div className="contact-page">
      <div className="contact-card">
        <div className="logoDiv">
          <div className="logoStyle">
            <IcJioDot></IcJioDot>
          </div>
          <Text appearance="heading-xxs">Consent Management</Text>
        </div>

        <div className="contact-content">
          <div className="container-header">
            <Text appearance="heading-xs">Contact Us</Text>
          </div>

          <div className="contact-box">
            <div className="contact-item">
              <Text appearance="body-s-bold" color="primary-80">
                Email
              </Text>
              <Text appearance="body-s" color="primary-grey-80">
              support-consent@ril.com
              </Text>
            </div>

            <div className="contact-item">
              <Text appearance="body-s-bold" color="primary-80">
                Team Name
              </Text>
              <Text appearance="body-s" color="primary-grey-80">
                Consent Management Team
              </Text>
            </div>
          </div>

          <br></br>

          <ActionButton
            label="Back to Home"
            onClick={() => navigate("/")}
            stretch={true}
          />
        </div>
      </div>
    </div>
  );
};

export default ContactUs;

