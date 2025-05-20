import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardProvider } from '../../context/DashboardContext';
import Button from '../common/Button';
import ExecutionSummary from './ExecutionSummary';
import TestTimeline from './TestTimeline';
import MetricsChart from './MetricsChart';
import FailureAnalysis from './FailureAnalysis';
import RecentTests from './RecentTests';
import './Dashboard.css';

/**
 * DashboardPanel component integrates all dashboard views
 */
const DashboardPanel: React.FC = () => {
  const [activeTab, setActiveTab] = useState('overview');

  // Define tab items
  const tabs = [
    {
      id: 'overview',
      label: 'Overview'
    },
    {
      id: 'analytics',
      label: 'Analytics'
    },
    {
      id: 'reports',
      label: 'Reports'
    },
    {
      id: 'analysis',
      label: 'Failure Analysis'
    }
  ];

  // Handle tab change
  const handleTabChange = (tabId: string) => {
    if (tabId) {
      setActiveTab(tabId);
    }
  };

  // Render the active tab content
  const renderTabContent = () => {
    switch (activeTab) {
      case 'overview':
        return (
            <div className="dashboard-overview">
              <ExecutionSummary />

              <div className="dashboard-main-content">
                <div className="dashboard-main-column">
                  <TestTimeline />
                  <MetricsChart />
                </div>

                <div className="dashboard-side-column">
                  <RecentTests />
                  <div className="dashboard-card quick-actions">
                    <div className="dashboard-card-header">
                      <h2 className="dashboard-card-title">Quick Actions</h2>
                    </div>
                    <div className="dashboard-card-content">
                      <div className="action-buttons">
                        <Link to="/execution?runAll=true">
                          <Button variant="primary" fullWidth>Run All Tests</Button>
                        </Link>
                        <Link to="/execution?failedOnly=true">
                          <Button variant="outline" fullWidth>Run Failed Tests</Button>
                        </Link>
                        <Link to="/recorder">
                          <Button variant="outline" fullWidth>Record New Test</Button>
                        </Link>
                        <Link to="/export">
                          <Button variant="outline" fullWidth>Export Tests</Button>
                        </Link>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
        );

      case 'analytics':
        return (
            <div className="dashboard-analytics">
              <ExecutionSummary />
              <div className="analytics-content">
                <MetricsChart />
                <FailureAnalysis />
              </div>
            </div>
        );

      case 'reports':
        return (
            <div className="dashboard-reports">
              <div className="dashboard-main-content">
                <div className="dashboard-main-column">
                  <RecentTests limit={20} showPagination={true} />
                </div>

                <div className="dashboard-side-column">
                  <div className="dashboard-card report-actions">
                    <div className="dashboard-card-header">
                      <h2 className="dashboard-card-title">Report Actions</h2>
                    </div>
                    <div className="dashboard-card-content">
                      <div className="action-buttons">
                        <Button
                            variant="primary"
                            fullWidth
                            onClick={() => window.open('/cstestforge/api/v1/dashboard/stats/export?format=pdf')}
                        >
                          Export as PDF
                        </Button>
                        <Button
                            variant="outline"
                            fullWidth
                            onClick={() => window.open('/cstestforge/api/v1/dashboard/stats/export?format=excel')}
                        >
                          Export as Excel
                        </Button>
                        <Button
                            variant="outline"
                            fullWidth
                            onClick={() => window.open('/cstestforge/api/v1/dashboard/stats/export?format=csv')}
                        >
                          Export as CSV
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
        );

      case 'analysis':
        return (
            <div className="dashboard-failure-analysis">
              <FailureAnalysis />
            </div>
        );

      default:
        return <div>Tab content not found</div>;
    }
  };

  return (
      <DashboardProvider>
        <div className="dashboard">
          <div className="dashboard-header">
            <h1>CSTestForge Dashboard</h1>
            <div className="dashboard-actions">
              <Link to="/execution">
                <Button variant="primary">Run Tests</Button>
              </Link>
              <Link to="/builder">
                <Button variant="secondary">Create Test</Button>
              </Link>
            </div>
          </div>

          <div className="dashboard-tabs">
            <div className="tabs-header">
              {tabs.map(tab => (
                  <div
                      key={tab.id}
                      className={`tab-item ${activeTab === tab.id ? 'tab-active' : ''}`}
                      onClick={() => handleTabChange(tab.id)}
                  >
                    <span className="tab-label">{tab.label}</span>
                  </div>
              ))}
            </div>

            <div className="tabs-content">
              {renderTabContent()}
            </div>
          </div>
        </div>
      </DashboardProvider>
  );
};

export default DashboardPanel;