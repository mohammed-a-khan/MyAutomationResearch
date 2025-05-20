import React from 'react';
import Button from '../common/Button';
import { TestExecutionInfo, TestStatus } from '../../types/execution';
import './TestExecutionControls.css';

interface TestExecutionControlsProps {
  execution: TestExecutionInfo | null;
  onStop?: () => void;
  onRerun?: () => void;
  onViewResults?: () => void;
}

/**
 * Component for controlling test execution
 */
const TestExecutionControls: React.FC<TestExecutionControlsProps> = ({
  execution,
  onStop,
  onRerun,
  onViewResults
}) => {
  // Get execution status text
  const getStatusText = (): string => {
    if (!execution) return 'Not Started';
    
    switch (execution.status) {
      case TestStatus.QUEUED:
        return 'Queued for Execution';
      case TestStatus.RUNNING:
        return 'Running Tests';
      case TestStatus.PASSED:
        return 'All Tests Passed';
      case TestStatus.FAILED:
        return 'Tests Failed';
      case TestStatus.ERROR:
        return 'Execution Error';
      default:
        return 'Unknown Status';
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
  
  // Get progress percentage
  const getProgressPercentage = (): number => {
    if (!execution) return 0;
    
    const total = execution.totalTests;
    if (total === 0) return 0;
    
    const completed = 
      execution.passedTests + 
      execution.failedTests + 
      execution.errorTests + 
      execution.skippedTests;
    
    return Math.floor((completed / total) * 100);
  };
  
  // Status colors
  const getStatusColor = (): string => {
    if (!execution) return '#d9d9d9';
    
    switch (execution.status) {
      case TestStatus.QUEUED:
        return '#d9d9d9';
      case TestStatus.RUNNING:
        return '#1890ff';
      case TestStatus.PASSED:
        return '#52c41a';
      case TestStatus.FAILED:
      case TestStatus.ERROR:
        return '#ff4d4f';
      default:
        return '#d9d9d9';
    }
  };
  
  const isInProgress = execution?.status === TestStatus.QUEUED || execution?.status === TestStatus.RUNNING;
  const isComplete = execution?.status === TestStatus.PASSED || execution?.status === TestStatus.FAILED || execution?.status === TestStatus.ERROR;
  
  return (
    <div className="test-execution-controls">
      <div className="execution-status">
        <div className="status-header">
          <h3 className="status-title">Execution Status</h3>
          <span 
            className={`status-badge status-${execution?.status?.toLowerCase() || 'default'}`}
            style={{ backgroundColor: getStatusColor() }}
          >
            {getStatusText()}
          </span>
        </div>
        
        {execution && (
          <>
            <div className="progress-bar">
              <div 
                className="progress-fill" 
                style={{ 
                  width: `${getProgressPercentage()}%`,
                  backgroundColor: getStatusColor()
                }}
              ></div>
            </div>
            
            <div className="execution-stats">
              <div className="stat-item">
                <span className="stat-label">Total Tests:</span>
                <span className="stat-value">{execution.totalTests}</span>
              </div>
              
              <div className="stat-item">
                <span className="stat-label">Running:</span>
                <span className="stat-value">{execution.runningTests}</span>
              </div>
              
              <div className="stat-item">
                <span className="stat-label">Queued:</span>
                <span className="stat-value">{execution.queuedTests}</span>
              </div>
              
              <div className="stat-item">
                <span className="stat-label">Passed:</span>
                <span className="stat-value stat-passed">{execution.passedTests}</span>
              </div>
              
              <div className="stat-item">
                <span className="stat-label">Failed:</span>
                <span className="stat-value stat-failed">{execution.failedTests}</span>
              </div>
              
              <div className="stat-item">
                <span className="stat-label">Errors:</span>
                <span className="stat-value stat-error">{execution.errorTests}</span>
              </div>
              
              <div className="stat-item">
                <span className="stat-label">Skipped:</span>
                <span className="stat-value stat-skipped">{execution.skippedTests}</span>
              </div>
              
              {execution.duration !== undefined && (
                <div className="stat-item">
                  <span className="stat-label">Duration:</span>
                  <span className="stat-value">{formatDuration(execution.duration)}</span>
                </div>
              )}
            </div>
          </>
        )}
      </div>
      
      <div className="control-actions">
        {isInProgress && onStop && (
          <Button 
            variant="danger" 
            onClick={onStop}
          >
            Stop Execution
          </Button>
        )}
        
        {isComplete && onRerun && (
          <Button 
            variant="primary" 
            onClick={onRerun}
          >
            Run Tests Again
          </Button>
        )}
        
        {isComplete && onViewResults && (
          <Button 
            variant="secondary" 
            onClick={onViewResults}
          >
            View Detailed Results
          </Button>
        )}
      </div>
    </div>
  );
};

export default TestExecutionControls;

// Add an empty export to mark file as a module
export {}; 