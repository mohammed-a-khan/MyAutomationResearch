import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  getTestStats, 
  getRecentTests, 
  getEnvironmentStatus,
  rerunTest
} from '../services/dashboardService';
import { formatError } from '../utils/errorHandling';
import { TestStats, RecentTest, EnvironmentStatus } from '../types/api';
import './Dashboard.css';

const Dashboard: React.FC = () => {
  // State for data
  const [stats, setStats] = useState<TestStats | null>(null);
  const [recentTests, setRecentTests] = useState<RecentTest[]>([]);
  const [environments, setEnvironments] = useState<EnvironmentStatus[]>([]);
  
  // Loading states
  const [isLoading, setIsLoading] = useState<{[key: string]: boolean}>({
    stats: true,
    recentTests: true,
    environments: true
  });
  
  // Error states
  const [errors, setErrors] = useState<{[key: string]: string | null}>({
    stats: null,
    recentTests: null,
    environments: null
  });

  // Fetch data on component mount
  useEffect(() => {
    const fetchDashboardData = async () => {
      await Promise.all([
        fetchTestStats(),
        fetchRecentTests(),
        fetchEnvironmentStatus()
      ]);
    };
    
    fetchDashboardData();
    
    // Set up polling interval for real-time updates (every 30 seconds)
    const pollingInterval = setInterval(fetchDashboardData, 30000);
    
    // Clean up interval on component unmount
    return () => clearInterval(pollingInterval);
  }, []);

  // Function to fetch test stats
  const fetchTestStats = async () => {
    setIsLoading(prev => ({ ...prev, stats: true }));
    try {
      const data = await getTestStats();
      setStats(data);
      setErrors(prev => ({ ...prev, stats: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        stats: formatError(error, 'Failed to load test statistics') 
      }));
    } finally {
      setIsLoading(prev => ({ ...prev, stats: false }));
    }
  };

  // Function to fetch recent tests
  const fetchRecentTests = async () => {
    setIsLoading(prev => ({ ...prev, recentTests: true }));
    try {
      const data = await getRecentTests(5); // Fetch 5 most recent tests
      setRecentTests(data);
      setErrors(prev => ({ ...prev, recentTests: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        recentTests: formatError(error, 'Failed to load recent tests') 
      }));
    } finally {
      setIsLoading(prev => ({ ...prev, recentTests: false }));
    }
  };

  // Function to fetch environment status
  const fetchEnvironmentStatus = async () => {
    setIsLoading(prev => ({ ...prev, environments: true }));
    try {
      const data = await getEnvironmentStatus();
      setEnvironments(data);
      setErrors(prev => ({ ...prev, environments: null }));
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        environments: formatError(error, 'Failed to load environment status') 
      }));
    } finally {
      setIsLoading(prev => ({ ...prev, environments: false }));
    }
  };

  // Function to handle test rerun
  const handleRerunTest = async (testId: string) => {
    try {
      // Optimistically update UI
      setRecentTests(prev => 
        prev.map(test => 
          test.id === testId 
            ? { ...test, status: 'running' } 
            : test
        )
      );
      
      // Make API call to rerun the test
      await rerunTest(testId);
      
      // Refresh data after a short delay to get updated status
      setTimeout(fetchRecentTests, 1000);
    } catch (error) {
      // Revert optimistic update
      await fetchRecentTests();
      console.error('Error rerunning test:', error);
    }
  };

  // Format timestamp to readable string
  const formatTimestamp = (timestamp: number): string => {
    const date = new Date(timestamp);
    return date.toLocaleString();
  };

  // Format duration to readable string
  const formatDuration = (duration: number): string => {
    if (duration < 1000) return `${duration}ms`;
    return `${(duration / 1000).toFixed(2)}s`;
  };

  // Get status badge class
  const getStatusBadgeClass = (status: string): string => {
    switch(status) {
      case 'passed': return 'status-badge status-success';
      case 'failed': return 'status-badge status-danger';
      case 'running': return 'status-badge status-info';
      default: return 'status-badge status-warning';
    }
  };

  // Map environment status to badge class
  const getEnvironmentBadgeClass = (status: string): string => {
    switch(status) {
      case 'available': return 'status-badge status-success';
      case 'degraded': return 'status-badge status-warning';
      case 'unavailable': return 'status-badge status-danger';
      default: return 'status-badge status-info';
    }
  };

  // Handle errors and loading states
  const renderErrorMessage = (errorKey: string) => {
    if (!errors[errorKey]) return null;
    
    return (
      <div className="error-message">
        <p>{errors[errorKey]}</p>
        <button 
          className="btn btn-sm btn-outline"
          onClick={() => {
            if (errorKey === 'stats') fetchTestStats();
            if (errorKey === 'recentTests') fetchRecentTests();
            if (errorKey === 'environments') fetchEnvironmentStatus();
          }}
        >
          Retry
        </button>
      </div>
    );
  };

  return (
    <div className="dashboard">
      <div className="dashboard-header">
        <h1>Test Dashboard</h1>
        <div className="dashboard-actions">
          <Link to="/builder" className="btn btn-primary">Create New Test</Link>
          <Link to="/runner" className="btn btn-secondary">Run Tests</Link>
        </div>
      </div>

      {/* Test Statistics */}
      <div className="dashboard-stats">
        {isLoading.stats && !stats ? (
          <div className="loading-indicator">Loading statistics...</div>
        ) : errors.stats ? (
          renderErrorMessage('stats')
        ) : stats ? (
          <>
            <div className="stat-card">
              <div className="stat-value">{stats.totalTests}</div>
              <div className="stat-label">Total Tests</div>
            </div>
            <div className="stat-card success">
              <div className="stat-value">{stats.passedTests}</div>
              <div className="stat-label">Passed</div>
            </div>
            <div className="stat-card danger">
              <div className="stat-value">{stats.failedTests}</div>
              <div className="stat-label">Failed</div>
            </div>
            <div className="stat-card warning">
              <div className="stat-value">{stats.skippedTests}</div>
              <div className="stat-label">Skipped</div>
            </div>
            <div className="stat-card">
              <div className="stat-value">{stats.successRate.toFixed(1)}%</div>
              <div className="stat-label">Success Rate</div>
            </div>
          </>
        ) : null}
      </div>

      <div className="dashboard-content">
        {/* Recent Tests */}
        <div className="recent-tests card">
          <div className="card-header">Recent Test Runs</div>
          {isLoading.recentTests && recentTests.length === 0 ? (
            <div className="loading-indicator">Loading recent tests...</div>
          ) : errors.recentTests ? (
            renderErrorMessage('recentTests')
          ) : recentTests.length > 0 ? (
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
                  {recentTests.map(test => (
                    <tr key={test.id}>
                      <td>{test.name}</td>
                      <td>
                        <span className={getStatusBadgeClass(test.status)}>
                          {test.status}
                        </span>
                      </td>
                      <td>{formatDuration(test.duration)}</td>
                      <td>{formatTimestamp(test.timestamp)}</td>
                      <td>
                        <Link to={`/reports/details/${test.id}`} className="btn btn-sm btn-outline">View</Link>
                        <button 
                          className="btn btn-sm btn-outline ml-2"
                          onClick={() => handleRerunTest(test.id)}
                          disabled={test.status === 'running'}
                        >
                          Rerun
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <div className="view-all">
                <Link to="/reports">View All Test Runs →</Link>
              </div>
            </>
          ) : (
            <div className="no-data-message">
              <p>No recent test runs found.</p>
              <Link to="/runner" className="btn btn-primary">Run Your First Test</Link>
            </div>
          )}
        </div>

        <div className="dashboard-side">
          {/* Quick Actions Card */}
          <div className="quick-actions card">
            <div className="card-header">Quick Actions</div>
            <div className="action-buttons">
              <Link to="/runner?runAll=true" className="btn btn-primary">Run All Tests</Link>
              <Link to="/runner?failedOnly=true" className="btn btn-outline">Run Failed Tests</Link>
              <Link to="/recorder" className="btn btn-outline">Record New Test</Link>
              <Link to="/builder?import=true" className="btn btn-outline">Import Test Script</Link>
            </div>
          </div>

          {/* Environment Status Card */}
          <div className="environment-status card">
            <div className="card-header">Environment Status</div>
            {isLoading.environments && environments.length === 0 ? (
              <div className="loading-indicator">Loading environment status...</div>
            ) : errors.environments ? (
              renderErrorMessage('environments')
            ) : environments.length > 0 ? (
              <div className="env-status-list">
                {environments.map(env => (
                  <div className="env-status-item" key={env.name}>
                    <span className="env-name">{env.name}</span>
                    <span className={getEnvironmentBadgeClass(env.status)}>
                      {env.status}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <div className="no-data-message">No environment data available.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard; 