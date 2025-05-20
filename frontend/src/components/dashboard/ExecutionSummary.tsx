import React from 'react';
import { Link } from 'react-router-dom';
import { useDashboard } from '../../context/DashboardContext';
import Button from '../common/Button';
import './Dashboard.css';

/**
 * ExecutionSummary component displays test execution statistics
 */
const ExecutionSummary: React.FC = () => {
    const {
        stats,
        loading,
        errors,
        refreshDashboardData,
        exportReport
    } = useDashboard();

    // Handle exportReport
    const handleExport = (format: 'pdf' | 'csv' | 'excel') => {
        exportReport(format);
    };

    // Render error message
    const renderError = () => {
        if (!errors.stats) return null;

        return (
            <div className="error-message">
                <p>{errors.stats}</p>
                <Button
                    variant="secondary"
                    size="sm"
                    onClick={() => refreshDashboardData('stats')}
                >
                    Retry
                </Button>
            </div>
        );
    };

    // Check if we have valid data
    const hasValidData = stats !== null && stats !== undefined;

    // Check if we have test data by day
    const hasTestsByDay = stats && stats.testsByDay && Array.isArray(stats.testsByDay) && stats.testsByDay.length > 0;

    return (
        <div className="dashboard-card">
            <div className="dashboard-card-header">
                <h2 className="dashboard-card-title">Test Execution Summary</h2>
                <div className="dashboard-card-actions">
                    <Button
                        variant="text"
                        size="sm"
                        onClick={() => refreshDashboardData('stats')}
                        disabled={loading.stats}
                    >
                        Refresh
                    </Button>
                    <Button
                        variant="text"
                        size="sm"
                        onClick={() => handleExport('pdf')}
                        disabled={!hasValidData}
                    >
                        Export PDF
                    </Button>
                </div>
            </div>

            <div className="dashboard-card-content">
                {loading.stats && !stats ? (
                    <div className="loading-indicator">Loading statistics...</div>
                ) : errors.stats ? (
                    renderError()
                ) : hasValidData ? (
                    <div className="dashboard-stats">
                        <div className="stat-card">
                            <div className="stat-value">{stats.totalTests || 0}</div>
                            <div className="stat-label">Total Tests</div>
                        </div>
                        <div className="stat-card success">
                            <div className="stat-value">{stats.passedTests || 0}</div>
                            <div className="stat-label">Passed</div>
                        </div>
                        <div className="stat-card danger">
                            <div className="stat-value">{stats.failedTests || 0}</div>
                            <div className="stat-label">Failed</div>
                        </div>
                        <div className="stat-card warning">
                            <div className="stat-value">{stats.skippedTests || 0}</div>
                            <div className="stat-label">Skipped</div>
                        </div>
                        <div className="stat-card info">
                            <div className="stat-value">{(stats.successRate || 0).toFixed(1)}%</div>
                            <div className="stat-label">Success Rate</div>
                        </div>
                        <div className="stat-card">
                            <div className="stat-value">{((stats.avgDuration || 0) / 1000).toFixed(1)}s</div>
                            <div className="stat-label">Avg. Duration</div>
                        </div>
                    </div>
                ) : (
                    <div className="no-data-message">
                        <p>No test execution data available.</p>
                        <Link to="/execution">
                            <Button variant="primary">Run Tests</Button>
                        </Link>
                    </div>
                )}
            </div>

            {hasTestsByDay && (
                <div className="dashboard-card-footer">
                    <span>Last 7 days: {stats.testsByDay.reduce((sum, day) => sum + (day.total || 0), 0)} tests run</span>
                    <Link to="/reports">View All Reports →</Link>
                </div>
            )}
        </div>
    );
};

export default ExecutionSummary;