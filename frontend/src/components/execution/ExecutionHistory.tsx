import React, { useState, useEffect } from 'react';
import { 
  TestExecutionInfo,
  TestStatus,
  TestFilter
} from '../../types/execution';
import { useExecution } from '../../context/ExecutionContext';
import Select from '../common/Select';
import Input from '../common/Input';
import Button from '../common/Button';
import StatusBadge from './StatusBadge';
import './Execution.css';

interface ExecutionHistoryProps {
  projectId?: string;
  limit?: number;
  onViewExecution?: (executionId: string) => void;
}

/**
 * ExecutionHistory component for displaying test execution history
 */
const ExecutionHistory: React.FC<ExecutionHistoryProps> = ({
  projectId,
  limit = 10,
  onViewExecution
}) => {
  const { 
    state: { 
      executionHistory, 
      historyPage, 
      totalHistoryPages,
      totalHistoryItems,
      historyFilter,
      isLoading 
    },
    getExecutionHistory,
    updateHistoryFilter
  } = useExecution();
  
  // Local component state
  const [error, setError] = useState<string | null>(null);
  
  // Fetch execution history
  useEffect(() => {
    const fetchHistory = async () => {
      setError(null);
      
      try {
        await getExecutionHistory(historyPage, limit, projectId);
      } catch (err) {
        setError('Failed to load execution history. Please try again.');
        console.error('Error loading execution history:', err);
      }
    };
    
    fetchHistory();
  }, [getExecutionHistory, historyPage, limit, projectId, historyFilter]);

  // Extended filter interface to handle UI fields not in the API filter
  interface ExtendedFilter extends Omit<TestFilter, 'status'> {
    searchText?: string;
    dateFrom?: string;
    dateTo?: string;
    status?: string | TestStatus[];
    environment?: string;
    browser?: string;
  }

  // Handle filter change
  const handleFilterChange = (field: keyof ExtendedFilter, value: any) => {
    updateHistoryFilter({
      ...historyFilter,
      [field]: value
    } as TestFilter);
  };
  
  // Reset filters
  const handleResetFilters = () => {
    updateHistoryFilter({
      projectId: projectId,
      searchText: '',
      dateFrom: '',
      dateTo: ''
    });
  };

  // Handle page change
  const handlePageChange = (page: number) => {
    if (page >= 1 && page <= totalHistoryPages) {
      getExecutionHistory(page, limit, projectId);
    }
  };

  // Format date
  const formatDate = (dateString: string): string => {
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  // Format duration
  const formatDuration = (ms: number): string => {
    if (ms < 1000) {
      return `${ms}ms`;
    }
    
    const seconds = Math.floor((ms / 1000) % 60);
    const minutes = Math.floor((ms / (1000 * 60)) % 60);
    const hours = Math.floor(ms / (1000 * 60 * 60));
    
    if (hours > 0) {
      return `${hours}h ${minutes}m ${seconds}s`;
    } else if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    } else {
      return `${seconds}s`;
    }
  };

  // Handle view execution click
  const handleViewExecution = (executionId: string) => {
    if (onViewExecution) {
      onViewExecution(executionId);
    }
  };

  return (
    <div className="execution-history">
      <div className="execution-history-header">
        <h4>Execution History</h4>
        <p className="text-muted">
          View and search past test executions.
        </p>
      </div>
      
      {/* Filters */}
      <div className="execution-history-filters">
        <div className="row mb-3">
          <div className="col-md-4 mb-2 mb-md-0">
            <Input
              placeholder="Search executions..."
              value={(historyFilter as ExtendedFilter).searchText || ''}
              onChange={(e) => handleFilterChange('searchText', e.target.value)}
              leftIcon={<i className="bi bi-search"></i>}
              disabled={isLoading}
            />
          </div>
          
          <div className="col-md-3 mb-2 mb-md-0">
            <Select
              value={(historyFilter as ExtendedFilter).status as string || ''}
              onChange={(e) => {
                const value = e.target.value;
                handleFilterChange('status', value ? [value as TestStatus] : undefined);
              }}
              options={[
                { value: '', label: 'All Statuses' },
                { value: TestStatus.PASSED, label: 'Passed' },
                { value: TestStatus.FAILED, label: 'Failed' },
                { value: TestStatus.RUNNING, label: 'Running' },
                { value: TestStatus.QUEUED, label: 'Queued' },
                { value: TestStatus.ERROR, label: 'Error' }
              ]}
              disabled={isLoading}
            />
          </div>
          
          <div className="col-md-3 mb-2 mb-md-0">
            <Select
              value={(historyFilter as ExtendedFilter).environment || ''}
              onChange={(e) => {
                // Store environment in customTags or similar field if needed
                handleFilterChange('tags', e.target.value ? [e.target.value] : []);
              }}
              options={[
                { value: '', label: 'All Environments' },
                { value: 'DEV', label: 'DEV' },
                { value: 'QA', label: 'QA' },
                { value: 'STAGING', label: 'STAGING' },
                { value: 'PROD', label: 'PROD' }
              ]}
              disabled={isLoading}
            />
          </div>
          
          <div className="col-md-2">
            <Button
              variant="outline"
              size="sm"
              onClick={handleResetFilters}
              disabled={isLoading || (
                !(historyFilter as ExtendedFilter).searchText &&
                !(historyFilter as ExtendedFilter).status &&
                !(historyFilter as ExtendedFilter).dateFrom &&
                !(historyFilter as ExtendedFilter).dateTo &&
                !historyFilter.tags?.length
              )}
              fullWidth
            >
              Clear Filters
            </Button>
          </div>
        </div>
        
        <div className="row">
          <div className="col-md-3 mb-2 mb-md-0">
            <label className="form-label small">Start Date</label>
            <Input
              type="date"
              value={(historyFilter as ExtendedFilter).dateFrom || ''}
              onChange={(e) => handleFilterChange('dateFrom', e.target.value)}
              disabled={isLoading}
            />
          </div>
          
          <div className="col-md-3 mb-2 mb-md-0">
            <label className="form-label small">End Date</label>
            <Input
              type="date"
              value={(historyFilter as ExtendedFilter).dateTo || ''}
              onChange={(e) => handleFilterChange('dateTo', e.target.value)}
              disabled={isLoading}
            />
          </div>
        </div>
      </div>
      
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      
      {/* Execution History Table */}
      <div className="execution-history-table-container">
        <table className="execution-history-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Status</th>
              <th>Tests</th>
              <th>Environment</th>
              <th>Start Time</th>
              <th>Duration</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={7} className="text-center py-4">
                  <div className="spinner-border spinner-border-sm text-primary me-2" role="status">
                    <span className="visually-hidden">Loading...</span>
                  </div>
                  Loading execution history...
                </td>
              </tr>
            ) : executionHistory.length === 0 ? (
              <tr>
                <td colSpan={7} className="text-center py-4">
                  <div className="text-muted">
                    <i className="bi bi-clock-history mb-2" style={{ fontSize: '24px' }}></i>
                    <p>No execution history found.</p>
                    <p>Run some tests to see them here.</p>
                  </div>
                </td>
              </tr>
            ) : (
              executionHistory.map((execution) => (
                <tr key={execution.id}>
                  <td>
                    <span className="execution-id">{execution.id.substring(0, 8)}...</span>
                  </td>
                  <td>
                    <StatusBadge status={execution.status} />
                  </td>
                  <td>
                    <div className="test-stats">
                      <span className="test-stats-item test-stats-total">{execution.totalTests}</span>
                      <span className="test-stats-item test-stats-passed">{execution.passedTests}</span>
                      <span className="test-stats-item test-stats-failed">{execution.failedTests}</span>
                    </div>
                  </td>
                  <td>{execution.environment}</td>
                  <td>{formatDate(execution.startTime)}</td>
                  <td>
                    {execution.duration ? formatDuration(execution.duration) : 'In Progress'}
                  </td>
                  <td>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleViewExecution(execution.id)}
                    >
                      <i className="bi bi-eye me-1"></i>
                      View
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
      
      {/* Pagination */}
      {totalHistoryPages > 1 && (
        <div className="execution-history-pagination">
          <div className="execution-history-pagination-info">
            Showing {((historyPage - 1) * limit) + 1} to {Math.min(historyPage * limit, totalHistoryItems)} of {totalHistoryItems} executions
          </div>
          <div className="execution-history-pagination-controls">
            <Button
              variant="outline"
              size="sm"
              disabled={historyPage === 1 || isLoading}
              onClick={() => handlePageChange(1)}
            >
              <i className="bi bi-chevron-double-left"></i>
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={historyPage === 1 || isLoading}
              onClick={() => handlePageChange(historyPage - 1)}
            >
              <i className="bi bi-chevron-left"></i>
            </Button>
            
            <span className="execution-history-pagination-page">
              Page {historyPage} of {totalHistoryPages}
            </span>
            
            <Button
              variant="outline"
              size="sm"
              disabled={historyPage === totalHistoryPages || isLoading}
              onClick={() => handlePageChange(historyPage + 1)}
            >
              <i className="bi bi-chevron-right"></i>
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={historyPage === totalHistoryPages || isLoading}
              onClick={() => handlePageChange(totalHistoryPages)}
            >
              <i className="bi bi-chevron-double-right"></i>
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};

export default ExecutionHistory; 