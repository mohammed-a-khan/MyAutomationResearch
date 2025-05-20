import React, { useState, useEffect } from 'react';
import { 
  ParallelExecutionStatus,
  BrowserType
} from '../../types/execution';
import { useExecution } from '../../context/ExecutionContext';
import Input from '../common/Input';
import Button from '../common/Button';
import './Execution.css';

interface ParallelExecutionManagerProps {
  onRefresh?: () => void;
}

/**
 * ParallelExecutionManager component for managing parallel test executions
 */
const ParallelExecutionManager: React.FC<ParallelExecutionManagerProps> = ({
  onRefresh
}) => {
  const { 
    state: { parallelStatus },
    getParallelStatus, 
    updateParallelConfig
  } = useExecution();
  
  // Component state
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [maxParallel, setMaxParallel] = useState<number>(5); // Default value
  const [isSaving, setIsSaving] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState<NodeJS.Timeout | null>(null);

  // Load parallel status
  useEffect(() => {
    const loadParallelStatus = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        const status = await getParallelStatus();
        setMaxParallel(status.maxParallel);
      } catch (err) {
        setError('Failed to load parallel execution status. Please try again.');
        console.error('Error loading parallel status:', err);
      } finally {
        setIsLoading(false);
      }
    };
    
    loadParallelStatus();
  }, [getParallelStatus]);

  // Set up auto-refresh
  useEffect(() => {
    // Start auto-refresh interval
    const interval = setInterval(async () => {
      try {
        await getParallelStatus();
      } catch (err) {
        console.error('Error refreshing parallel status:', err);
      }
    }, 10000); // Refresh every 10 seconds
    
    setRefreshInterval(interval);
    
    // Clean up interval on unmount
    return () => {
      if (interval) {
        clearInterval(interval);
      }
    };
  }, [getParallelStatus]);

  // Handle max parallel change
  const handleMaxParallelChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value);
    if (!isNaN(value) && value > 0) {
      setMaxParallel(value);
    }
  };

  // Handle save configuration
  const handleSaveConfig = async () => {
    setIsSaving(true);
    setError(null);
    
    try {
      const success = await updateParallelConfig(maxParallel);
      if (success) {
        // Refresh data
        if (onRefresh) {
          onRefresh();
        }
      }
    } catch (err) {
      setError('Failed to update parallel configuration. Please try again.');
      console.error('Error updating parallel config:', err);
    } finally {
      setIsSaving(false);
    }
  };

  // Handle refresh button click
  const handleRefresh = async () => {
    setIsLoading(true);
    setError(null);
    
    try {
      await getParallelStatus();
      if (onRefresh) {
        onRefresh();
      }
    } catch (err) {
      setError('Failed to refresh parallel execution status. Please try again.');
      console.error('Error refreshing parallel status:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Get browser color
  const getBrowserColor = (browser: string): string => {
    switch (browser) {
      case BrowserType.CHROME:
        return '#4285F4';
      case BrowserType.FIREFOX:
        return '#FF7139';
      case BrowserType.EDGE:
        return '#0078D7';
      case BrowserType.SAFARI:
        return '#000000';
      default:
        return '#94196B';
    }
  };

  // Render loading state
  if (isLoading && !parallelStatus) {
    return (
      <div className="parallel-manager d-flex justify-content-center align-items-center">
        <div className="text-center">
          <div className="spinner-border text-primary mb-3" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p>Loading parallel execution status...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="parallel-manager">
      <div className="parallel-manager-header">
        <div className="d-flex justify-content-between align-items-center mb-2">
          <h4>Parallel Execution Manager</h4>
          <Button
            variant="outline"
            size="sm"
            onClick={handleRefresh}
            disabled={isLoading}
          >
            <i className={`bi bi-arrow-repeat ${isLoading ? 'spinner-border spinner-border-sm' : ''}`}></i>
            {isLoading ? ' Refreshing...' : ' Refresh'}
          </Button>
        </div>
        <p className="text-muted">
          Monitor and configure parallel test executions.
        </p>
      </div>
      
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      
      {parallelStatus && (
        <>
          <div className="parallel-status">
            <div className="parallel-status-item">
              <div className="parallel-status-value">{parallelStatus.maxParallel}</div>
              <div className="parallel-status-label">Maximum Parallel Executions</div>
            </div>
            <div className="parallel-status-item">
              <div className="parallel-status-value">{parallelStatus.currentActive}</div>
              <div className="parallel-status-label">Currently Active</div>
            </div>
            <div className="parallel-status-item">
              <div className="parallel-status-value">{parallelStatus.queued}</div>
              <div className="parallel-status-label">Tests in Queue</div>
            </div>
            <div className="parallel-status-item">
              <div className="parallel-status-value">
                {Math.round((parallelStatus.currentActive / parallelStatus.maxParallel) * 100)}%
              </div>
              <div className="parallel-status-label">Capacity Utilization</div>
            </div>
          </div>
          
          {/* Browser Distribution */}
          <div className="browser-distribution mb-4">
            <h5 className="mb-3">Browser Distribution</h5>
            {Object.entries(parallelStatus.browser).map(([browser, count]) => (
              <div key={browser} className="browser-distribution-item">
                <div className="browser-distribution-header">
                  <div className="browser-distribution-label">{browser}</div>
                  <div className="browser-distribution-value">{count}</div>
                </div>
                <div className="browser-distribution-bar">
                  <div 
                    className="browser-distribution-bar-fill"
                    style={{ 
                      width: `${(count / parallelStatus.currentActive) * 100}%`,
                      backgroundColor: getBrowserColor(browser) 
                    }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
          
          {/* Environment Distribution */}
          <div className="environment-distribution mb-4">
            <h5 className="mb-3">Environment Distribution</h5>
            {Object.entries(parallelStatus.environment).map(([env, count]) => (
              <div key={env} className="environment-distribution-item">
                <div className="environment-distribution-header">
                  <div className="environment-distribution-label">{env}</div>
                  <div className="environment-distribution-value">{count}</div>
                </div>
                <div className="environment-distribution-bar">
                  <div 
                    className="environment-distribution-bar-fill"
                    style={{ 
                      width: `${(count / parallelStatus.currentActive) * 100}%`
                    }}
                  ></div>
                </div>
              </div>
            ))}
          </div>
          
          {/* Configuration Section */}
          <div className="parallel-config card">
            <div className="card-header">
              <h5 className="mb-0">Parallel Execution Configuration</h5>
            </div>
            <div className="card-body">
              <div className="row mb-3">
                <div className="col-md-6">
                  <label htmlFor="maxParallel" className="form-label">
                    Maximum Parallel Executions
                  </label>
                  <Input
                    id="maxParallel"
                    type="number"
                    value={maxParallel.toString()}
                    onChange={handleMaxParallelChange}
                    min="1"
                    max="20"
                    disabled={isSaving}
                  />
                  <div className="form-text">
                    Sets the maximum number of test executions that can run in parallel.
                  </div>
                </div>
              </div>
              <div className="d-flex justify-content-end">
                <Button
                  onClick={handleSaveConfig}
                  disabled={isSaving || maxParallel === parallelStatus.maxParallel}
                >
                  {isSaving ? 'Saving...' : 'Save Configuration'}
                </Button>
              </div>
            </div>
          </div>
          
          <div className="alert alert-info mt-4" role="alert">
            <h5 className="alert-heading">Performance Impact</h5>
            <p>
              Running too many parallel tests may impact system performance. 
              Monitor system resources and adjust the maximum parallel executions accordingly.
            </p>
            <hr />
            <p className="mb-0">
              Recommended maximum: {navigator.hardwareConcurrency || 4} 
              (based on available CPU cores on the execution server)
            </p>
          </div>
        </>
      )}
    </div>
  );
};

export default ParallelExecutionManager; 