import React from 'react';
import { TestStatus } from '../../types/execution';
import './Execution.css';

interface StatusBadgeProps {
  status: TestStatus | string;
  showIcon?: boolean;
  className?: string;
}

/**
 * StatusBadge component for displaying test execution status
 */
const StatusBadge: React.FC<StatusBadgeProps> = ({ status, showIcon = true, className = '' }) => {
  // Get the appropriate CSS class based on status
  const getBadgeClass = (status: TestStatus | string): string => {
    let statusClass = '';
    
    switch (status) {
      case TestStatus.QUEUED:
        statusClass = 'test-status-badge-queued';
        break;
      case TestStatus.RUNNING:
        statusClass = 'test-status-badge-running';
        break;
      case TestStatus.PASSED:
        statusClass = 'test-status-badge-passed';
        break;
      case TestStatus.FAILED:
        statusClass = 'test-status-badge-failed';
        break;
      case TestStatus.SKIPPED:
        statusClass = 'test-status-badge-skipped';
        break;
      case TestStatus.ERROR:
        statusClass = 'test-status-badge-error';
        break;
      default:
        statusClass = 'test-status-badge-queued';
    }
    
    return `test-status-badge ${statusClass} ${className}`;
  };
  
  // Get the appropriate icon based on status
  const getStatusIcon = (status: TestStatus | string): string => {
    switch (status) {
      case TestStatus.QUEUED:
        return 'bi bi-hourglass-split';
      case TestStatus.RUNNING:
        return 'bi bi-arrow-repeat';
      case TestStatus.PASSED:
        return 'bi bi-check-circle-fill';
      case TestStatus.FAILED:
        return 'bi bi-x-circle-fill';
      case TestStatus.SKIPPED:
        return 'bi bi-skip-forward-fill';
      case TestStatus.ERROR:
        return 'bi bi-exclamation-triangle-fill';
      default:
        return 'bi bi-question-circle';
    }
  };
  
  return (
    <span className={getBadgeClass(status)}>
      {showIcon && (
        <i className={getStatusIcon(status)}></i>
      )}
      <span>{status}</span>
    </span>
  );
};

export default StatusBadge; 