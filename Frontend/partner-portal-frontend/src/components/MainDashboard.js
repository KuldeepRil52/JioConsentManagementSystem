import React from 'react';
import { Text, ActionButton } from '../custom-components';
import '../styles/mainDashboard.css';

const MainDashboard = () => {
  return (
    <div className="main-dashboard-content">
          <Text appearance="heading-s" className="dashboard-title">Dashboard</Text>

          {/* Data processing register */}
          <div className="section">
            <div className="section-header">
              <Text appearance="heading-xs">Data processing register</Text>
              <div className="header-actions">
                <select className="custom-dropdown">
                  <option value="30days">Last 30 days</option>
                </select>
                <button className="action-button">Audit & reporting</button>
              </div>
            </div>
            <div className="cards-grid">
              <div className="card data-card">
                <div className="card-icon blue">⭐</div>
                <Text appearance="heading-m">156</Text>
                <Text appearance="body-s" color="primary-grey-80">Total purpose created</Text>
              </div>
              <div className="card data-card">
                <div className="card-icon purple">📦</div>
                <Text appearance="heading-m">342</Text>
                <Text appearance="body-s" color="primary-grey-80">Data Types & Items</Text>
              </div>
              <div className="card data-card">
                <div className="card-icon pink">TL</div>
                <Text appearance="heading-m">89</Text>
                <Text appearance="body-s" color="primary-grey-80">Processing Activities</Text>
              </div>
              <div className="card data-card">
                <div className="card-icon orange">🕐</div>
                <Text appearance="heading-m">6 months</Text>
                <Text appearance="body-s" color="primary-grey-80">Retention Period</Text>
              </div>
              <div className="card data-card">
                <div className="card-icon blue">📊</div>
                <Text appearance="heading-m">2847</Text>
                <Text appearance="body-s" color="primary-grey-80">Logs</Text>
              </div>
              <div className="card data-card">
                <div className="card-icon green">✓</div>
                <Text appearance="heading-m">1247</Text>
                <Text appearance="body-s" color="primary-grey-80">Consent Artefacts</Text>
              </div>
            </div>
          </div>

          {/* Consents */}
          <div className="section">
            <Text appearance="heading-s">Consents</Text>
            <div className="consents-grid">
              <div className="consents-left">
                <div className="card status-card green">
                  <div className="card-icon-green">✓</div>
                  <Text appearance="heading-m">24</Text>
                  <Text appearance="body-s" color="primary-grey-80">Published templates</Text>
                </div>
                <div className="card status-card orange">
                  <div className="card-icon-orange">!</div>
                  <Text appearance="heading-m">12</Text>
                  <Text appearance="body-s" color="primary-grey-80">Pending renewal</Text>
                </div>
              </div>
              <div className="consents-right">
                <div className="card summary-card">
                  <Text appearance="heading-m">1245</Text>
                  <Text appearance="body-s" color="primary-grey-80">Total consents</Text>
                  <div className="summary-list">
                    <div className="summary-row">
                      <Text appearance="body-s">Active consents</Text>
                      <Text appearance="body-s">967</Text>
                    </div>
                    <div className="summary-row">
                      <Text appearance="body-s">Revoked consents</Text>
                      <Text appearance="body-s">189</Text>
                    </div>
                    <div className="summary-row">
                      <Text appearance="body-s">Expired consents</Text>
                      <Text appearance="body-s">87</Text>
                    </div>
                  </div>
                </div>
                <div className="card summary-card">
                  <Text appearance="heading-m">4690</Text>
                  <Text appearance="body-s" color="primary-grey-80">Notifications sent</Text>
                  <div className="summary-list">
                    <div className="summary-row">
                      <Text appearance="body-s">Email</Text>
                      <Text appearance="body-s">3456</Text>
                    </div>
                    <div className="summary-row">
                      <Text appearance="body-s">SMS</Text>
                      <Text appearance="body-s">1234</Text>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Cookies */}
          <div className="section">
            <Text appearance="heading-s">Cookies</Text>
            <div className="cookies-grid">
              <div className="cookies-left">
                <div className="card status-card green">
                  <div className="card-icon-green">✓</div>
                  <Text appearance="heading-m">24</Text>
                  <Text appearance="body-s" color="primary-grey-80">Published</Text>
                </div>
                <div className="card status-card orange">
                  <div className="card-icon-orange">📄</div>
                  <Text appearance="heading-m">12</Text>
                  <Text appearance="body-s" color="primary-grey-80">Draft</Text>
                </div>
                <div className="card status-card grey">
                  <div className="card-icon-grey">!</div>
                  <Text appearance="heading-m">2</Text>
                  <Text appearance="body-s" color="primary-grey-80">Inactive</Text>
                </div>
              </div>
              <div className="cookies-right">
                <div className="card chart-card">
                  <Text appearance="heading-m">Total cookies</Text>
                  <div className="chart-content">
                    <div className="donut-chart-wrapper">
                      <svg width="150" height="150" viewBox="0 0 36 36" className="donut">
                        <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#52C41A" strokeWidth="3" strokeDasharray="45 55" strokeDashoffset="0"></circle>
                        <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#1890FF" strokeWidth="3" strokeDasharray="23 77" strokeDashoffset="-45"></circle>
                        <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#F5222D" strokeWidth="3" strokeDasharray="17 83" strokeDashoffset="-68"></circle>
                        <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#FAAD14" strokeWidth="3" strokeDasharray="15 85" strokeDashoffset="-85"></circle>
                      </svg>
                      <div className="chart-center">
                        <Text appearance="heading-m">1245</Text>
                      </div>
                    </div>
                    <div className="chart-legend">
                      <div className="legend-item">
                        <span className="legend-color green"></span>
                        <Text appearance="body-s">All accepted</Text>
                        <Text appearance="body-s">450</Text>
                      </div>
                      <div className="legend-item">
                        <span className="legend-color dark-blue"></span>
                        <Text appearance="body-s">Partially accepted</Text>
                        <Text appearance="body-s">230</Text>
                      </div>
                      <div className="legend-item">
                        <span className="legend-color red"></span>
                        <Text appearance="body-s">All rejected</Text>
                        <Text appearance="body-s">120</Text>
                      </div>
                      <div className="legend-item">
                        <span className="legend-color orange"></span>
                        <Text appearance="body-s">No action</Text>
                        <Text appearance="body-s">200</Text>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Grievance redressal */}
          <div className="section">
            <Text appearance="heading-s">Grievance redressal</Text>
            <div className="grievance-grid">
              <div className="card chart-card grievance-chart">
                <Text appearance="heading-m">Total requests</Text>
                <div className="chart-content">
                  <div className="donut-chart-wrapper">
                    <svg width="150" height="150" viewBox="0 0 36 36" className="donut">
                      <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#52C41A" strokeWidth="3" strokeDasharray="15 85" strokeDashoffset="0"></circle>
                      <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#722ED1" strokeWidth="3" strokeDasharray="23 77" strokeDashoffset="-15"></circle>
                      <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#1890FF" strokeWidth="3" strokeDasharray="45 55" strokeDashoffset="-38"></circle>
                      <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#FAAD14" strokeWidth="3" strokeDasharray="17 83" strokeDashoffset="-83"></circle>
                    </svg>
                    <div className="chart-center">
                      <Text appearance="heading-m">2300</Text>
                    </div>
                  </div>
                  <div className="chart-legend">
                    <div className="legend-item">
                      <span className="legend-color green"></span>
                      <Text appearance="body-s">Resolved</Text>
                      <Text appearance="body-s">1250</Text>
                    </div>
                    <div className="legend-item">
                      <span className="legend-color purple"></span>
                      <Text appearance="body-s">In progress</Text>
                      <Text appearance="body-s">230</Text>
                    </div>
                    <div className="legend-item">
                      <span className="legend-color light-blue"></span>
                      <Text appearance="body-s">Escalated</Text>
                      <Text appearance="body-s">120</Text>
                    </div>
                    <div className="legend-item">
                      <span className="legend-color orange"></span>
                      <Text appearance="body-s">Rejected</Text>
                      <Text appearance="body-s">200</Text>
                    </div>
                  </div>
                </div>
              </div>

              <div className="card grievance-sla">
                <Text appearance="heading-m">Grievance SLA</Text>
                <div className="sla-content">
                  <div className="donut-chart-wrapper small">
                    <svg width="100" height="100" viewBox="0 0 36 36" className="donut">
                      <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#52C41A" strokeWidth="3" strokeDasharray="75 25" strokeDashoffset="0"></circle>
                      <circle className="donut-segment" cx="18" cy="18" r="15.915" fill="none" stroke="#FAAD14" strokeWidth="3" strokeDasharray="5 95" strokeDashoffset="-75"></circle>
                    </svg>
                    <div className="chart-center-small">
                      <Text appearance="body-s">75%</Text>
                    </div>
                  </div>
                  <div className="sla-details">
                    <div className="sla-item">
                      <span className="icon-green">✓</span>
                      <Text appearance="body-s">75% Resolved in SLA</Text>
                    </div>
                    <div className="sla-item">
                      <span className="icon-orange">!</span>
                      <Text appearance="body-s">5% Exceeded SLA</Text>
                    </div>
                  </div>
                </div>
              </div>

              <div className="card grievance-notifications">
                <Text appearance="heading-m">Grievances Notifications</Text>
                <div className="notification-item">
                  <div className="icon-grey">📱</div>
                  <Text appearance="heading-m">100</Text>
                  <Text appearance="body-s" color="primary-grey-80">SMS Alerts</Text>
                </div>
                <div className="notification-item">
                  <div className="icon-grey">📧</div>
                  <Text appearance="heading-m">100</Text>
                  <Text appearance="body-s" color="primary-grey-80">Email Alerts</Text>
                </div>
              </div>
            </div>
          </div>
    </div>
  );
};

export default MainDashboard;

