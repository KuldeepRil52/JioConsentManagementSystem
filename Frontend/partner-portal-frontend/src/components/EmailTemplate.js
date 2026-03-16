import { ActionButton, Text, Icon, BadgeV2 } from "../custom-components";
import { IcEditPen, IcSort, IcSwap } from "../custom-components/Icon";
import { useNavigate } from "react-router-dom";
import { useSelector } from "react-redux";
import { useState, useEffect, useMemo } from "react";
import "../styles/EmailTemplate.css";
import { IcSmartSwitchPlug, IcWhatsapp } from "../custom-components/Icon";
import config from "../utils/config";
const EmailTemplate = () => {
  const navigate = useNavigate();
  const tenantId = useSelector((state) => state.common.tenant_id);
  const businessId = useSelector((state) => state.common.business_id);
  const sessionToken = useSelector((state) => state.common.session_token);
  
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sortColumn, setSortColumn] = useState(null);
  const [sortDirection, setSortDirection] = useState("asc");

  const createEmailTemp = () => {
    navigate("/createEmailTemplate");
  };

  // Fetch email templates from API
  const fetchEmailTemplates = async () => {
    try {
      setLoading(true);
      setError(null);

      // Calculate scope level based on tenantId and businessId
      const scopeLevel = tenantId === businessId ? 'TENANT' : 'BUSINESS';

      const response = await fetch(
        `${config.notification_templates}?channel=EMAIL&type=NOTIFICATION`,
        {
          method: 'GET',
          headers: {
            'X-Scope-Level': scopeLevel,
            'X-Tenant-Id': tenantId,
            'X-Business-Id': businessId,
            'x-session-token': sessionToken,
            'Accept': 'application/json'
          }
        }
      );

      if (!response.ok) {
        throw new Error(`API error: ${response.status} ${response.statusText}`);
      }

      const data = await response.json();
      console.log("Email templates received:", data);
      
      // Extract templates from nested data structure
      const templatesArray = data?.data?.data || [];
      
      // Filter templates to show only those with eventType starting with "CONSENT"
      const filteredTemplates = templatesArray.filter(template => 
        template.eventType && template.eventType.startsWith('CONSENT')
      );
      
      setTemplates(Array.isArray(filteredTemplates) ? filteredTemplates : []);
    } catch (err) {
      console.error("Error fetching email templates:", err);
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleSort = (column) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === "asc" ? "desc" : "asc");
    } else {
      setSortColumn(column);
      setSortDirection("asc");
    }
  };

  const sortedTemplates = useMemo(() => {
    if (!sortColumn) return templates;

    return [...templates].sort((a, b) => {
      let aValue, bValue;

      switch (sortColumn) {
        case 'subject':
          aValue = a.emailConfig?.subject || '';
          bValue = b.emailConfig?.subject || '';
          break;
        case 'body':
          aValue = a.emailConfig?.body || '';
          bValue = b.emailConfig?.body || '';
          break;
        default:
          aValue = a[sortColumn] || '';
          bValue = b[sortColumn] || '';
      }

      if (aValue < bValue) return sortDirection === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }, [templates, sortColumn, sortDirection]);

  useEffect(() => {
    if (tenantId && businessId) {
      fetchEmailTemplates();
    }
  }, [tenantId, businessId]);

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
                label="Add email template"
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
                          onClick={() => handleSort('eventType')}
                          style={{ cursor: 'pointer' }}
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
                          onClick={() => handleSort('subject')}
                          style={{ cursor: 'pointer' }}
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
                          onClick={() => handleSort('body')}
                          style={{ cursor: 'pointer' }}
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
                  {loading ? (
                    <tr>
                      <td colSpan="4" style={{ textAlign: 'center', padding: '20px' }}>
                        <Text appearance="body-xs-bold" color="primary-grey-60">
                          Loading templates...
                        </Text>
                      </td>
                    </tr>
                  ) : error ? (
                    <tr>
                      <td colSpan="4" style={{ textAlign: 'center', padding: '20px' }}>
                        <Text appearance="body-xs-bold" color="error">
                          Error: {error}
                        </Text>
                        <br />
                        <ActionButton
                          label="Retry"
                          onClick={fetchEmailTemplates}
                          kind="secondary"
                          size="small"
                        />
                      </td>
                    </tr>
                  ) : sortedTemplates.length === 0 ? (
                    <tr>
                      <td colSpan="4" style={{ textAlign: 'center', padding: '20px' }}>
                        <Text appearance="body-xs-bold" color="primary-grey-60">
                          No email templates found
                        </Text>
                      </td>
                    </tr>
                  ) : (
                    sortedTemplates.map((template, index) => (
                      <tr key={template.id || index}>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {template.eventType || 'N/A'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {template.emailConfig?.subject || 'N/A'}
                          </Text>
                        </td>
                        <td>
                          <Text appearance="body-xs-bold" color="black">
                            {template.emailConfig?.body?.substring(0, 50) || 'N/A'}{template.emailConfig?.body?.length > 50 ? '...' : ''}
                          </Text>
                        </td>
                        <td>
                          <Icon
                            ic={<IcEditPen height={24} width={24} />}
                            color="primary_grey_80"
                            kind="default"
                            size="medium"
                            style={{ cursor: 'pointer' }}
                            onClick={() => {
                              // Navigate to create page with template data for editing
                              navigate("/createEmailTemplate", { 
                                state: { 
                                  editMode: true,
                                  templateData: template 
                                } 
                              });
                            }}
                          />
                        </td>
                      </tr>
                    ))
                  )}
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
