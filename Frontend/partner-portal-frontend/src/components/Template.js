import { ActionButton, Text, Icon, BadgeV2 } from "../custom-components";
import { IcEditPen, IcSort, IcSwap } from "../custom-components/Icon";
import { useNavigate } from "react-router-dom";
import "../styles/EmailTemplate.css";
import { IcSmartSwitchPlug, IcWhatsapp } from "../custom-components/Icon";
const EmailTemplate = () => {
  const navigate = useNavigate();
  const createEmailTemp = () => {
    navigate("/createEmailTemplate");
  };
  return (
    <>
      <div className="configurePage">
        <div className="emailTemplateWidth">
          <div className="emailTemplate-Heading-Button">
            <div className="emailTemplate-Heading-badge">
              <Text appearance="heading-s" color="primary-grey-100">
                Email Template
              </Text>
              <div className="systemConfig-badge">
                <Text appearance="body-xs-bold" color="primary-grey-80">
                  Consent
                </Text>
              </div>
            </div>
            <div>
              <ActionButton
                label="Create email Template"
                onClick={createEmailTemp}
                kind="primary"
              ></ActionButton>
            </div>
          </div>
          <br></br>
          <div>
            <div className="emailTemplate-custom-table-outer-div">
              <table className="emailTempalte-custom-table">
                <thead>
                  <tr>
                    <th>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Event
                        </Text>
                        <Icon
                          ic={<IcSort height={16} width={16} />}
                          color="primary_60"
                          size="small"
                        />
                      </div>
                    </th>
                    <th>
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Subject
                        </Text>
                        <Icon
                          ic={<IcSort height={16} width={16} />}
                          color="primary_60"
                          size="small"
                        />
                      </div>
                    </th>
                    <th>
                      {" "}
                      <div className="emailTemplate-table-header-icon">
                        <Text appearance="body-xs-bold" color="primary-grey-80">
                          Message
                        </Text>
                        <Icon
                          ic={<IcSort height={16} width={16} />}
                          color="primary_60"
                          size="small"
                        />
                      </div>
                    </th>

                    <th>
                      {" "}
                      <Text appearance="body-xs-bold" color="primary-grey-80">
                        Action
                      </Text>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>
                      {" "}
                      <Text appearance="body-xs-bold" color="black">
                        Consent Created
                      </Text>
                    </td>
                    <td>
                      {" "}
                      <Text appearance="body-xs-bold" color="black">
                        Your consent for Purpose with Validity is created
                      </Text>
                    </td>

                    <td>
                      <Text appearance="body-xs-bold" color="black">
                        Your consent for Purpose with Validity is created
                      </Text>
                    </td>
                    <td>
                      {" "}
                      <Icon
                        ic={<IcEditPen height={24} width={24} />}
                        color="primary_grey_80"
                        kind="default"
                        size="medium"
                      ></Icon>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
export default EmailTemplate;
