import React from 'react';
import { useDashboard } from '../../context/DashboardContext';
import Button from '../common/Button';
import Chart, { ChartDataPoint } from '../common/Chart';
import './Dashboard.css';

/**
 * MetricsChart component displays various performance and test metrics
 */
const MetricsChart: React.FC = () => {
  const {
    metrics,
    loading,
    errors,
    refreshDashboardData,
    metricType,
    updateMetricType,
    period,
    updatePeriod
  } = useDashboard();

  // Check if we have valid data for the current metric type
  const hasValidData = metrics &&
      metrics[metricType] &&
      metrics[metricType].datasets &&
      Array.isArray(metrics[metricType].datasets) &&
      metrics[metricType].datasets.length > 0 &&
      metrics[metricType].labels &&
      Array.isArray(metrics[metricType].labels);

  // Convert metrics data to Chart component format
  const transformMetricsData = (): ChartDataPoint[] => {
    if (!hasValidData) return [];

    const currentMetrics = metrics[metricType];

    // If it's a pie chart (for errors distribution)
    if (metricType === 'errors') {
      const dataset = currentMetrics.datasets[0];
      return currentMetrics.labels.map((label, index) => ({
        label: label || 'Unknown',
        value: dataset.data[index] || 0,
        color: dataset.backgroundColor && Array.isArray(dataset.backgroundColor) ?
            dataset.backgroundColor[index] as string :
            '#94196B'
      }));
    }

    // For line/bar charts
    const dataset = currentMetrics.datasets[0];
    return currentMetrics.labels.map((label, index) => ({
      label: label || 'Unknown',
      value: dataset.data[index] || 0,
      color: dataset.borderColor as string || '#94196B'
    }));
  };

  // Get the title based on the metric type
  const getChartTitle = (): string => {
    switch (metricType) {
      case 'execution':
        return 'Test Execution Performance';
      case 'performance':
        return 'Performance Metrics';
      case 'errors':
        return 'Error Distribution';
      default:
        return 'Metrics';
    }
  };

  // Get the chart type based on the metric type
  const getChartType = (): 'line' | 'bar' | 'pie' => {
    switch (metricType) {
      case 'errors':
        return 'pie';
      case 'performance':
        return 'bar';
      default:
        return 'line';
    }
  };

  // Handle metric type change
  const handleMetricTypeChange = (type: 'execution' | 'performance' | 'errors') => {
    updateMetricType(type);
  };

  // Handle period change
  const handlePeriodChange = (newPeriod: 'day' | 'week' | 'month') => {
    updatePeriod(newPeriod);
  };

  // Render error message
  const renderError = () => {
    if (!errors.metrics) return null;

    return (
        <div className="error-message">
          <p>{errors.metrics}</p>
          <Button
              variant="secondary"
              size="sm"
              onClick={() => refreshDashboardData('metrics')}
          >
            Retry
          </Button>
        </div>
    );
  };

  return (
      <div className="dashboard-card">
        <div className="dashboard-card-header">
          <h2 className="dashboard-card-title">{getChartTitle()}</h2>
          <div className="dashboard-card-actions">
            <Button
                variant="text"
                size="sm"
                onClick={() => refreshDashboardData('metrics')}
                disabled={loading.metrics}
            >
              Refresh
            </Button>
          </div>
        </div>

        <div className="dashboard-card-content">
          <div className="metrics-controls">
            <div className="metrics-type-selector">
              <button
                  className={`metrics-button ${metricType === 'execution' ? 'active' : ''}`}
                  onClick={() => handleMetricTypeChange('execution')}
              >
                Execution
              </button>
              <button
                  className={`metrics-button ${metricType === 'performance' ? 'active' : ''}`}
                  onClick={() => handleMetricTypeChange('performance')}
              >
                Performance
              </button>
              <button
                  className={`metrics-button ${metricType === 'errors' ? 'active' : ''}`}
                  onClick={() => handleMetricTypeChange('errors')}
              >
                Errors
              </button>
            </div>

            {metricType !== 'errors' && (
                <div className="metrics-period-selector">
                  <button
                      className={`metrics-button ${period === 'day' ? 'active' : ''}`}
                      onClick={() => handlePeriodChange('day')}
                  >
                    Day
                  </button>
                  <button
                      className={`metrics-button ${period === 'week' ? 'active' : ''}`}
                      onClick={() => handlePeriodChange('week')}
                  >
                    Week
                  </button>
                  <button
                      className={`metrics-button ${period === 'month' ? 'active' : ''}`}
                      onClick={() => handlePeriodChange('month')}
                  >
                    Month
                  </button>
                </div>
            )}
          </div>

          {loading.metrics ? (
              <div className="loading-indicator">Loading metrics data...</div>
          ) : errors.metrics ? (
              renderError()
          ) : hasValidData ? (
              <div className="chart-container">
                <Chart
                    type={getChartType()}
                    data={transformMetricsData()}
                    title={getChartTitle()}
                    height={300}
                    showLegend={true}
                    showValues={true}
                    animate={true}
                    gridLines={metricType !== 'errors'}
                />
              </div>
          ) : (
              <div className="no-data-message">
                <p>No metrics data available for {metricType} metrics.</p>
                <p>Run tests to start collecting metrics data.</p>
              </div>
          )}
        </div>

        <div className="dashboard-card-footer">
          <span>{metricType === 'errors' ? 'Error analysis based on recent tests' : `Data shown for ${period}`}</span>
        </div>
      </div>
  );
};

export default MetricsChart;