import React from 'react';
import { useDashboard } from '../../context/DashboardContext';
import { TestTimelineData } from '../../services/dashboardService';
import Button from '../common/Button';
import Chart, { ChartDataPoint } from '../common/Chart';
import './Dashboard.css';

/**
 * TestTimeline component displays historical test execution data
 */
const TestTimeline: React.FC = () => {
  const {
    timeline,
    loading,
    errors,
    refreshDashboardData,
    timeRange,
    updateTimeRange
  } = useDashboard();

  // Prepare chart data from timeline data
  const getChartData = (data: TestTimelineData | null): ChartDataPoint[] => {
    // Defensive check for empty or invalid data
    if (!data || !data.executionsByDay || !Array.isArray(data.executionsByDay)) {
      return [];
    }

    // Transform the data into the format expected by Chart component
    return data.executionsByDay.map(day => ({
      label: day.date,
      value: day.count,
      color: '#94196B'
    }));
  };

  // Handle time range change
  const handleTimeRangeChange = (days: number) => {
    updateTimeRange(days);
  };

  // Render error message
  const renderError = () => {
    if (!errors.timeline) return null;

    return (
        <div className="error-message">
          <p>{errors.timeline}</p>
          <Button
              variant="secondary"
              size="sm"
              onClick={() => refreshDashboardData('timeline')}
          >
            Retry
          </Button>
        </div>
    );
  };

  // Safely calculate total executions
  const getTotalExecutions = (): number => {
    if (!timeline || !timeline.executionsByDay || !Array.isArray(timeline.executionsByDay)) {
      return 0;
    }
    return timeline.executionsByDay.reduce((sum, day) => sum + day.count, 0);
  };

  // Check if we have valid data to display
  const hasData = timeline &&
      timeline.executionsByDay &&
      Array.isArray(timeline.executionsByDay) &&
      timeline.executionsByDay.length > 0;

  return (
      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <h2 className="dashboard-card-title">Test Execution Timeline</h2>
          <div className="dashboard-card-actions">
            <Button
                variant="text"
                size="sm"
                onClick={() => refreshDashboardData('timeline')}
                disabled={loading.timeline}
            >
              Refresh
            </Button>
          </div>
        </div>

        <div className="dashboard-card-content">
          <div className="timeline-controls">
            <div className="timeline-range-selector">
              <button
                  className={`timeline-button ${timeRange === 7 ? 'active' : ''}`}
                  onClick={() => handleTimeRangeChange(7)}
              >
                7 Days
              </button>
              <button
                  className={`timeline-button ${timeRange === 14 ? 'active' : ''}`}
                  onClick={() => handleTimeRangeChange(14)}
              >
                14 Days
              </button>
              <button
                  className={`timeline-button ${timeRange === 30 ? 'active' : ''}`}
                  onClick={() => handleTimeRangeChange(30)}
              >
                30 Days
              </button>
              <button
                  className={`timeline-button ${timeRange === 90 ? 'active' : ''}`}
                  onClick={() => handleTimeRangeChange(90)}
              >
                90 Days
              </button>
            </div>
          </div>

          {loading.timeline ? (
              <div className="loading-indicator">Loading timeline data...</div>
          ) : errors.timeline ? (
              renderError()
          ) : hasData ? (
              <div className="timeline-container">
                <div className="chart-container">
                  <Chart
                      type="line"
                      data={getChartData(timeline)}
                      title="Test Executions Over Time"
                      height={300}
                      showLegend={true}
                      gridLines={true}
                  />
                </div>
              </div>
          ) : (
              <div className="no-data-message">
                <p>No timeline data available.</p>
                <p>Run some tests to start seeing execution trends over time.</p>
              </div>
          )}
        </div>

        {hasData && (
            <div className="dashboard-card-footer">
              <span>Period: {timeline?.startDate || '-'} to {timeline?.endDate || '-'}</span>
              <span>Total Executions: {getTotalExecutions()}</span>
            </div>
        )}
      </div>
  );
};

export default TestTimeline;