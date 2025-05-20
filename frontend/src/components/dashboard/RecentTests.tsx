import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDashboard } from '../../context/DashboardContext';
import Button from '../common/Button';
import './Dashboard.css';

interface RecentTestsProps {
  limit?: number;
  showPagination?: boolean;
}

/**
 * RecentTests component displays recent test executions
 */
const RecentTests: React.FC<RecentTestsProps> = ({
                                                   limit = 5,
                                                   showPagination = false
                                                 }) => {
  const {
    recentTests,
    loading,
    errors,
    refreshDashboardData,
    handleRerunTest
  } = useDashboard();

  // State for pagination
  const [currentPage, setCurrentPage] = useState(1);
  const testsPerPage = limit;
  const maxPage = Math.ceil((recentTests?.length || 0) / testsPerPage);

  // Get current tests for pagination
  const getCurrentTests = () => {
    if (!Array.isArray(recentTests)) {
      return [];
    }

    if (!showPagination) {
      return recentTests.slice(0, limit);
    }

    const startIndex = (currentPage - 1) * testsPerPage;
    return recentTests.slice(startIndex, startIndex + testsPerPage);
  };

  // Format timestamp to readable string
  const formatTimestamp = (timestamp: number): string => {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  // Format duration to readable string
  const formatDuration = (duration: number): string => {
    if (duration === undefined || duration === null) return 'N/A';
    if (duration < 1000) return `${duration}ms`;
    return `${(duration / 1000).toFixed(2)}s`;
  };

  // Get status badge class
  const getStatusBadgeClass = (status: string): string => {
    if (!status) return 'status-badge status-warning';

    switch(status.toLowerCase()) {
      case 'passed': return 'status-badge status-success';
      case 'failed': return 'status-badge status-danger';
      case 'running': return 'status-badge status-info';
      default: return 'status-badge status-warning';
    }
  };

  // Handle pagination
  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  // Render pagination controls
  const renderPagination = () => {
    if (!showPagination || maxPage <= 1) return null;

    return (
        <div className="pagination">
          <Button
              variant="text"
              size="sm"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
          >
            Previous
          </Button>

          <span className="pagination-info">
          Page {currentPage} of {maxPage}
        </span>

          <Button
              variant="text"
              size="sm"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === maxPage}
          >
            Next
          </Button>
        </div>
    );
  };

  // Render error message
  const renderError = () => {
    if (!errors.recentTests) return null;

    return (
        <div className="error-message">
          <p>{errors.recentTests}</p>
          <Button
              variant="secondary"
              size="sm"
              onClick={() => refreshDashboardData('recentTests')}
          >
            Retry
          </Button>
        </div>
    );
  };

  // Check if we have valid data
  const hasValidData = Array.isArray(recentTests) && recentTests.length > 0;
  const currentTests = getCurrentTests();

  return (
      <div className="dashboard-card recent-tests">
        <div className="dashboard-card-header">
          <h2 className="dashboard-card-title">Recent Test Runs</h2>
          <div className="dashboard-card-actions">
            <Button
                variant="text"
                size="sm"
                onClick={() => refreshDashboardData('recentTests')}
                disabled={loading.recentTests}
            >
              Refresh
            </Button>
          </div>
        </div>

        <div className="dashboard-card-content">
          {loading.recentTests && !hasValidData ? (
              <div className="loading-indicator">Loading recent tests...</div>
          ) : errors.recentTests ? (
              renderError()
          ) : hasValidData && currentTests.length > 0 ? (
              <>
                <table className="table">
                  <thead>
                  <tr>
                    <th>Test Name</th>
                    <th>Status</th>
                    <th>Duration</th>
                    <th>Time</th>
                    <th>Actions</th>
                  </tr>
                  </thead>
                  <tbody>
                  {currentTests.map(test => (
                      <tr key={test.id || Math.random().toString()}>
                        <td>{test.name || 'Unnamed Test'}</td>
                        <td>
                      <span className={getStatusBadgeClass(test.status)}>
                        {test.status || 'Unknown'}
                      </span>
                        </td>
                        <td>{formatDuration(test.duration)}</td>
                        <td>{formatTimestamp(test.timestamp)}</td>
                        <td>
                          <Link to={`/reports/details/${test.id || 'unknown'}`}>
                            <Button variant="text" size="sm">View</Button>
                          </Link>
                          <Button
                              variant="text"
                              size="sm"
                              onClick={() => handleRerunTest(test.id)}
                              disabled={!test.id || test.status === 'running'}
                          >
                            Rerun
                          </Button>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>

                {renderPagination()}
              </>
          ) : (
              <div className="no-data-message">
                <p>No recent test runs found.</p>
                <Link to="/execution">
                  <Button variant="primary">Run Tests</Button>
                </Link>
              </div>
          )}
        </div>

        {!showPagination && hasValidData && (
            <div className="dashboard-card-footer">
              <Link to="/reports">View All Test Runs →</Link>
            </div>
        )}
      </div>
  );
};

export default RecentTests;