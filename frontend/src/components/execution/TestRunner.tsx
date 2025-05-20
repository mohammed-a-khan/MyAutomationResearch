import React, { useState, useEffect, useCallback } from 'react';
import { 
  TestExecutionInfo,
  TestStatus,
  ExecutionConfig,
  BrowserType,
  TestExecutionRequest
} from '../../types/execution';
import { useExecution } from '../../context/ExecutionContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import StatusBadge from './StatusBadge';
import './Execution.css';

interface TestRunnerProps {
  projectId: string;
  testIds: string[];
  onExecutionComplete?: (executionId: string) => void;
  onViewResults?: (executionId: string) => void;
}

/**
 * TestRunner component for managing test execution
 */
const TestRunner: React.FC<TestRunnerProps> = ({
  projectId,
  testIds,
  onExecutionComplete,
  onViewResults
}) => {
  const {
    state: { currentExecution },
    runTests,
    stopExecution,
    getExecutionStatus,
    getDefaultConfig,
    clearCurrentExecution
  } = useExecution();
  
  // State for execution configuration
  const [config, setConfig] = useState<ExecutionConfig>(getDefaultConfig(projectId));
  const [isStarting, setIsStarting] = useState(false);
  const [isStopping, setIsStopping] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState<NodeJS.Timeout | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Environments available for the project
  const [availableEnvironments] = useState<string[]>(['DEV', 'QA', 'STAGING', 'PROD']);

  // Handle config changes
  const handleConfigChange = (field: keyof ExecutionConfig, value: any) => {
    setConfig(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle checkbox changes
  const handleCheckboxChange = (field: keyof ExecutionConfig) => {
    setConfig(prev => ({
      ...prev,
      [field]: !prev[field]
    }));
  };

  // Start the test execution
  const startExecution = async () => {
    if (testIds.length === 0) return;
    
    setIsStarting(true);
    setError(null);
    
    try {
      // Prepare the execution request
      const request: TestExecutionRequest = {
        projectId,
        testIds,
        config
      };
      
      // Run the tests
      const execution = await runTests(request);
      
      // Start polling for updates
      startPolling(execution.id);
    } catch (err) {
      setError('Failed to start test execution. Please try again.');
      console.error('Error starting execution:', err);
    } finally {
      setIsStarting(false);
    }
  };

  // Stop the test execution
  const handleStopExecution = async () => {
    if (!currentExecution) return;
    
    setIsStopping(true);
    
    try {
      await stopExecution(currentExecution.id);
    } catch (err) {
      setError('Failed to stop execution. Please try again.');
      console.error('Error stopping execution:', err);
    } finally {
      setIsStopping(false);
    }
  };

  // Poll for execution updates
  const startPolling = useCallback((executionId: string) => {
    // Clear any existing interval
    if (refreshInterval) {
      clearInterval(refreshInterval);
    }
    
    // Create new polling interval
    const interval = setInterval(async () => {
      try {
        const updated = await getExecutionStatus(executionId);
        
        // If execution is complete, stop polling
        if (
          updated.status === TestStatus.PASSED ||
          updated.status === TestStatus.FAILED ||
          updated.status === TestStatus.ERROR
        ) {
          clearInterval(interval);
          setRefreshInterval(null);
          
          // Notify parent component
          if (onExecutionComplete) {
            onExecutionComplete(executionId);
          }
        }
      } catch (err) {
        console.error('Error polling execution status:', err);
      }
    }, 3000); // Poll every 3 seconds
    
    setRefreshInterval(interval);
  }, [getExecutionStatus, onExecutionComplete, refreshInterval]);

  // Clean up polling interval when component unmounts
  useEffect(() => {
    return () => {
      if (refreshInterval) {
        clearInterval(refreshInterval);
      }
    };
  }, [refreshInterval]);

  // Calculate progress percentage
  const calculateProgress = (execution: TestExecutionInfo): number => {
    const { totalTests, passedTests, failedTests, skippedTests, errorTests } = execution;
    const completed = passedTests + failedTests + skippedTests + errorTests;
    return (completed / totalTests) * 100;
  };

  // Format duration from milliseconds to readable format
  const formatDuration = (ms: number | undefined): string => {
    if (ms === undefined) return '00:00';
    
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

  // Check if execution is in progress
  const isExecutionInProgress = currentExecution && (
    currentExecution.status === TestStatus.RUNNING ||
    currentExecution.status === TestStatus.QUEUED
  );

  // Calculate execution duration so far
  const calculateDuration = (execution: TestExecutionInfo): number => {
    if (execution.duration) return execution.duration;
    
    const start = new Date(execution.startTime).getTime();
    const end = execution.endTime
      ? new Date(execution.endTime).getTime()
      : new Date().getTime();
    
    return end - start;
  };

  return (
    <div className="test-runner">
      {!currentExecution ? (
        <>
          <div className="test-runner-header">
            <h4>Configure Test Execution</h4>
            <p className="text-muted">
              Configure how your tests will be executed.
            </p>
          </div>
          
          <div className="execution-config">
            <div className="form-group">
              <label className="form-label">Environment</label>
              <Select
                value={config.environment}
                onChange={(e) => handleConfigChange('environment', e.target.value)}
                options={availableEnvironments.map(env => ({
                  value: env,
                  label: env
                }))}
              />
            </div>
            
            <div className="form-group">
              <label className="form-label">Browser</label>
              <Select
                value={config.browser}
                onChange={(e) => handleConfigChange('browser', e.target.value as BrowserType)}
                options={Object.values(BrowserType).map(browser => ({
                  value: browser,
                  label: browser.charAt(0) + browser.slice(1).toLowerCase()
                }))}
              />
            </div>
            
            <div className="form-group">
              <label className="form-label">Timeout (seconds)</label>
              <Input
                type="number"
                value={config.timeoutSeconds.toString()}
                onChange={(e) => handleConfigChange('timeoutSeconds', parseInt(e.target.value))}
                min="5"
                max="300"
              />
            </div>
            
            <div className="form-group">
              <label className="form-label">Retry Count</label>
              <Input
                type="number"
                value={config.retryCount.toString()}
                onChange={(e) => handleConfigChange('retryCount', parseInt(e.target.value))}
                min="0"
                max="3"
              />
            </div>
          </div>
          
          <div className="mb-3">
            <div className="form-check">
              <input
                className="form-check-input"
                type="checkbox"
                id="headlessMode"
                checked={config.headless}
                onChange={() => handleCheckboxChange('headless')}
              />
              <label className="form-check-label" htmlFor="headlessMode">
                Run in headless mode
              </label>
            </div>
            
            <div className="form-check">
              <input
                className="form-check-input"
                type="checkbox"
                id="parallelExecution"
                checked={config.parallel}
                onChange={() => handleCheckboxChange('parallel')}
              />
              <label className="form-check-label" htmlFor="parallelExecution">
                Enable parallel execution
              </label>
            </div>
          </div>
          
          {config.parallel && (
            <div className="form-group mb-3">
              <label className="form-label">Max Parallel Executions</label>
              <Input
                type="number"
                value={config.maxParallel.toString()}
                onChange={(e) => handleConfigChange('maxParallel', parseInt(e.target.value))}
                min="2"
                max="10"
              />
              <div className="form-text">
                Maximum number of tests to run in parallel.
              </div>
            </div>
          )}
          
          {error && (
            <div className="alert alert-danger" role="alert">
              {error}
            </div>
          )}
          
          <div className="test-runner-controls">
            <Button
              variant="primary"
              onClick={startExecution}
              disabled={isStarting || testIds.length === 0}
            >
              {isStarting ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Starting...
                </>
              ) : (
                <>
                  <i className="bi bi-play-fill me-1"></i> Start Execution
                </>
              )}
            </Button>
          </div>
        </>
      ) : (
        <>
          <div className="test-runner-header">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <h4>
                Execution: {currentExecution.id.substring(0, 8)}...
              </h4>
              <StatusBadge status={currentExecution.status} />
            </div>
            <div className="d-flex flex-wrap gap-3 text-muted">
              <div>
                <i className="bi bi-globe me-1"></i>
                Environment: {currentExecution.environment}
              </div>
              <div>
                <i className="bi bi-browser-chrome me-1"></i>
                Browser: {currentExecution.browser}
              </div>
              <div>
                <i className="bi bi-clock-history me-1"></i>
                Duration: {formatDuration(calculateDuration(currentExecution))}
              </div>
            </div>
          </div>
          
          <div className="test-runner-summary">
            <div className="test-runner-summary-item test-runner-summary-passed">
              <div className="test-runner-summary-value">{currentExecution.passedTests}</div>
              <div className="test-runner-summary-label">Passed</div>
            </div>
            <div className="test-runner-summary-item test-runner-summary-failed">
              <div className="test-runner-summary-value">{currentExecution.failedTests}</div>
              <div className="test-runner-summary-label">Failed</div>
            </div>
            <div className="test-runner-summary-item test-runner-summary-running">
              <div className="test-runner-summary-value">{currentExecution.runningTests}</div>
              <div className="test-runner-summary-label">Running</div>
            </div>
            <div className="test-runner-summary-item test-runner-summary-queued">
              <div className="test-runner-summary-value">{currentExecution.queuedTests}</div>
              <div className="test-runner-summary-label">Queued</div>
            </div>
            <div className="test-runner-summary-item test-runner-summary-total">
              <div className="test-runner-summary-value">{currentExecution.totalTests}</div>
              <div className="test-runner-summary-label">Total</div>
            </div>
          </div>
          
          <div className="test-runner-progress">
            <div className="progress" style={{ height: '8px' }}>
              <div
                className="progress-bar bg-success"
                role="progressbar"
                style={{ width: `${(currentExecution.passedTests / currentExecution.totalTests) * 100}%` }}
                aria-valuenow={(currentExecution.passedTests / currentExecution.totalTests) * 100}
                aria-valuemin={0}
                aria-valuemax={100}
              ></div>
              <div
                className="progress-bar bg-danger"
                role="progressbar"
                style={{ width: `${(currentExecution.failedTests / currentExecution.totalTests) * 100}%` }}
                aria-valuenow={(currentExecution.failedTests / currentExecution.totalTests) * 100}
                aria-valuemin={0}
                aria-valuemax={100}
              ></div>
              <div
                className="progress-bar bg-primary"
                role="progressbar"
                style={{ width: `${(currentExecution.runningTests / currentExecution.totalTests) * 100}%` }}
                aria-valuenow={(currentExecution.runningTests / currentExecution.totalTests) * 100}
                aria-valuemin={0}
                aria-valuemax={100}
              ></div>
            </div>
            <div className="d-flex justify-content-between mt-2">
              <div className="small text-muted">
                {calculateProgress(currentExecution).toFixed(0)}% Complete
              </div>
              <div className="small text-muted">
                {currentExecution.passedTests + currentExecution.failedTests} of {currentExecution.totalTests} tests completed
              </div>
            </div>
          </div>
          
          {error && (
            <div className="alert alert-danger" role="alert">
              {error}
            </div>
          )}
          
          <div className="test-runner-controls">
            {isExecutionInProgress ? (
              <Button
                variant="danger"
                onClick={handleStopExecution}
                disabled={isStopping}
              >
                {isStopping ? 'Stopping...' : 'Stop Execution'}
              </Button>
            ) : (
              <>
                <Button
                  variant="outline"
                  onClick={() => clearCurrentExecution()}
                >
                  Start New Execution
                </Button>
                
                <Button
                  variant="primary"
                  onClick={() => onViewResults && onViewResults(currentExecution.id)}
                >
                  View Detailed Results
                </Button>
              </>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default TestRunner; 