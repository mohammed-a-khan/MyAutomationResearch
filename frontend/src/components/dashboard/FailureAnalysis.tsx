import React from 'react';
import { Link } from 'react-router-dom';
import { useDashboard } from '../../context/DashboardContext';
import { FailureAnalysis as FailureAnalysisData } from '../../services/dashboardService';
import Button from '../common/Button';
import Chart, { ChartDataPoint } from '../common/Chart';
import './Dashboard.css';

/**
 * FailureAnalysis component displays failure trends and analysis
 */
const FailureAnalysis: React.FC = () => {
  const {
    failureAnalysis,
    loading,
    errors,
    refreshDashboardData,
    timeRange
  } = useDashboard();

  // Format date for display
  const formatDate = (timestamp: number): string => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleDateString();
  };

  // Transform failure by type data for pie chart
  const getFailureByTypeData = (data: FailureAnalysisData | null): ChartDataPoint[] => {
    if (!data || !data.failuresByType || !Array.isArray(data.failuresByType)) return [];

    return data.failuresByType.map(item => ({
      label: item.type,
      value: item.count,
      color: getColorForFailureType(item.type)
    }));
  };

  // Transform failure by browser data for pie chart
  const getFailureByBrowserData = (data: FailureAnalysisData | null): ChartDataPoint[] => {
    if (!data || !data.failuresByBrowser || !Array.isArray(data.failuresByBrowser)) return [];

    return data.failuresByBrowser.map(item => ({
      label: item.browser,
      value: item.count,
      color: getColorForBrowser(item.browser)
    }));
  };

  // Get color for failure type
  const getColorForFailureType = (type: string): string => {
    if (!type) return '#94196B';

    const colors: Record<string, string> = {
      'ElementNotFound': '#f44336',
      'AssertionFailed': '#ff9800',
      'Timeout': '#2196f3',
      'ScriptError': '#9c27b0',
      'NetworkError': '#e91e63',
      'EnvironmentError': '#795548'
    };

    return colors[type] || '#94196B';
  };

  // Get color for browser
  const getColorForBrowser = (browser: string): string => {
    if (!browser) return '#94196B';

    const colors: Record<string, string> = {
      'Chrome': '#0F9D58',
      'Firefox': '#FF9800',
      'Edge': '#0078D7',
      'Safari': '#147EFB',
      'IE': '#0076D7'
    };

    return colors[browser] || '#94196B';
  };

  // Render error message
  const renderError = () => {
    if (!errors.failureAnalysis) return null;

    return (
        <div className="error-message">
          <p>{errors.failureAnalysis}</p>
          <Button
              variant="secondary"
              size="sm"
              onClick={() => refreshDashboardData('failureAnalysis')}
          >
            Retry
          </Button>
        </div>
    );
  };

  // Check if we have valid data
  const hasValidData = failureAnalysis !== null && failureAnalysis !== undefined;

  // Check if we have most common failures
  const hasCommonFailures = failureAnalysis &&
      failureAnalysis.mostCommonFailures &&
      Array.isArray(failureAnalysis.mostCommonFailures) &&
      failureAnalysis.mostCommonFailures.length > 0;

  // Check if we have failure types data
  const hasFailureTypes = failureAnalysis &&
      failureAnalysis.failuresByType &&
      Array.isArray(failureAnalysis.failuresByType) &&
      failureAnalysis.failuresByType.length > 0;

  // Check if we have browser data
  const hasFailuresByBrowser = failureAnalysis &&
      failureAnalysis.failuresByBrowser &&
      Array.isArray(failureAnalysis.failuresByBrowser) &&
      failureAnalysis.failuresByBrowser.length > 0;

  // Check if we have unstable tests
  const hasUnstableTests = failureAnalysis &&
      failureAnalysis.unstableTests &&
      Array.isArray(failureAnalysis.unstableTests) &&
      failureAnalysis.unstableTests.length > 0;

  return (
      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <h2 className="dashboard-card-title">Failure Analysis</h2>
          <div className="dashboard-card-actions">
            <Button
                variant="text"
                size="sm"
                onClick={() => refreshDashboardData('failureAnalysis')}
                disabled={loading.failureAnalysis}
            >
              Refresh
            </Button>
          </div>
        </div>

        <div className="dashboard-card-content">
          {loading.failureAnalysis && !failureAnalysis ? (
              <div className="loading-indicator">Loading failure analysis...</div>
          ) : errors.failureAnalysis ? (
              renderError()
          ) : hasValidData ? (
              <>
                {/* Most Common Failures */}
                {hasCommonFailures && (
                    <div className="failure-section">
                      <h3 className="section-title">Most Common Failures</h3>
                      <div className="failure-list">
                        {failureAnalysis.mostCommonFailures.map((failure, index) => (
                            <div key={index} className="failure-item">
                              <div>
                                <span className="failure-count">{failure.count || 0}x</span>
                                <span className="failure-message">{failure.message || 'Unknown error'}</span>
                              </div>
                              <div className="failure-tests">
                                Affecting {failure.testIds && Array.isArray(failure.testIds) ? failure.testIds.length : 0} tests
                              </div>
                            </div>
                        ))}
                      </div>
                    </div>
                )}

                {/* Distribution Charts */}
                {(hasFailureTypes || hasFailuresByBrowser) && (
                    <div className="distribution-charts">
                      {hasFailureTypes && (
                          <div className="distribution-chart">
                            <h3 className="distribution-chart-title">Failures by Type</h3>
                            <Chart
                                type="pie"
                                data={getFailureByTypeData(failureAnalysis)}
                                height={200}
                                showLegend={true}
                                showValues={false}
                            />
                          </div>
                      )}

                      {hasFailuresByBrowser && (
                          <div className="distribution-chart">
                            <h3 className="distribution-chart-title">Failures by Browser</h3>
                            <Chart
                                type="pie"
                                data={getFailureByBrowserData(failureAnalysis)}
                                height={200}
                                showLegend={true}
                                showValues={false}
                            />
                          </div>
                      )}
                    </div>
                )}

                {/* Unstable Tests */}
                {hasUnstableTests && (
                    <div className="failure-section">
                      <h3 className="section-title">Unstable Tests</h3>
                      <div className="unstable-tests">
                        {failureAnalysis.unstableTests.map((test, index) => (
                            <div key={index} className="unstable-test-item">
                              <div>
                                <Link to={`/tests/${test.testId || 'unknown'}`} className="unstable-test-name">
                                  {test.testName || 'Unnamed Test'}
                                </Link>
                                <div className="unstable-test-date">
                                  Last run: {formatDate(test.lastExecuted)}
                                </div>
                              </div>
                              <div className="unstable-test-rate">
                                {((test.failureRate || 0) * 100).toFixed(1)}% failures
                              </div>
                            </div>
                        ))}
                      </div>
                    </div>
                )}

                {!hasCommonFailures && !hasFailureTypes && !hasFailuresByBrowser && !hasUnstableTests && (
                    <div className="no-data-message">
                      <p>No failure analysis data available.</p>
                      <p>This is a good thing! It means there may not be any test failures to analyze.</p>
                    </div>
                )}
              </>
          ) : (
              <div className="no-data-message">
                <p>No failure analysis data available.</p>
              </div>
          )}
        </div>

        {hasValidData && (
            <div className="dashboard-card-footer">
              <span>Analysis based on data from the last {timeRange} days</span>
            </div>
        )}
      </div>
  );
};

export default FailureAnalysis;