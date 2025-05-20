import React from 'react';
import { TestResult, TestStatus } from '../../types/execution';
import Card from '../common/Card';
import './TestResults.css';

interface TestResultsListProps {
  results: TestResult[];
  selectedResultId?: string;
  onSelectResult?: (result: TestResult) => void;
}

/**
 * Component to display a list of test results
 */
const TestResultsList: React.FC<TestResultsListProps> = ({
  results,
  selectedResultId,
  onSelectResult
}) => {
  // Helper to get status class
  const getStatusClass = (status: TestStatus): string => {
    switch (status) {
      case TestStatus.PASSED:
        return 'status-passed';
      case TestStatus.FAILED:
        return 'status-failed';
      case TestStatus.RUNNING:
        return 'status-running';
      case TestStatus.QUEUED:
        return 'status-queued';
      case TestStatus.SKIPPED:
        return 'status-skipped';
      case TestStatus.ERROR:
        return 'status-error';
      default:
        return '';
    }
  };
  
  // Format duration from milliseconds
  const formatDuration = (duration?: number): string => {
    if (!duration) return '0s';
    
    const seconds = Math.floor(duration / 1000);
    if (seconds < 60) return `${seconds}s`;
    
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}m ${remainingSeconds}s`;
  };
  
  return (
    <div className="test-results-list">
      {results.length === 0 ? (
        <Card className="empty-results">
          <div className="empty-results-message">
            No test results available.
          </div>
        </Card>
      ) : (
        results.map(result => (
          <div 
            key={result.id}
            className={`test-result-wrapper ${selectedResultId === result.id ? 'selected' : ''}`}
            onClick={() => onSelectResult && onSelectResult(result)}
          >
            <Card 
              className={`test-result-item ${getStatusClass(result.status)}`}
            >
              <div className="test-result-header">
                <div className="test-result-status">
                  <span className={`status-indicator ${getStatusClass(result.status)}`}></span>
                  <span className="status-text">{result.status}</span>
                </div>
                <div className="test-result-name">{result.name}</div>
              </div>
              
              <div className="test-result-meta">
                {result.duration !== undefined && (
                  <div className="test-result-duration">
                    <span className="meta-label">Duration:</span>
                    <span className="meta-value">{formatDuration(result.duration)}</span>
                  </div>
                )}
                
                <div className="test-result-time">
                  <span className="meta-label">Started:</span>
                  <span className="meta-value">
                    {new Date(result.startTime).toLocaleString()}
                  </span>
                </div>
              </div>
              
              {result.status === TestStatus.FAILED && result.errorMessage && (
                <div className="test-result-error">
                  <div className="error-message">{result.errorMessage}</div>
                </div>
              )}
            </Card>
          </div>
        ))
      )}
    </div>
  );
};

export default TestResultsList; 