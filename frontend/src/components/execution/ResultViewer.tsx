import React, { useState, useEffect } from 'react';
import { 
  TestResult, 
  TestStepResult, 
  Screenshot,
  LogEntry,
  TestStatus
} from '../../types/execution';
import { useExecution } from '../../context/ExecutionContext';
import Tabs from '../common/Tab';
import Button from '../common/Button';
import StatusBadge from './StatusBadge';
import './Execution.css';

interface ResultViewerProps {
  executionId: string;
  testId?: string;
  onBack?: () => void;
}

/**
 * ResultViewer component for displaying test execution results
 */
const ResultViewer: React.FC<ResultViewerProps> = ({
  executionId,
  testId,
  onBack
}) => {
  const { getExecutionDetails, getTestResult, getScreenshot } = useExecution();
  
  // Component state
  const [result, setResult] = useState<TestResult | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTabId, setActiveTabId] = useState<string>('steps');
  const [expandedSteps, setExpandedSteps] = useState<Set<string>>(new Set());
  const [screenshotModal, setScreenshotModal] = useState<{
    isOpen: boolean;
    screenshot: Screenshot | null;
  }>({ isOpen: false, screenshot: null });

  // Load test result data
  useEffect(() => {
    const fetchResult = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        if (testId) {
          // Fetch specific test result
          const result = await getTestResult(executionId, testId);
          setResult(result);
        } else {
          // Fetch execution details and use first test if no specific test ID
          const execution = await getExecutionDetails(executionId);
          if (execution.results && execution.results.length > 0) {
            setResult(execution.results[0]);
          }
        }
      } catch (err) {
        setError('Failed to load test results. Please try again.');
        console.error('Error loading test results:', err);
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchResult();
  }, [executionId, testId, getExecutionDetails, getTestResult]);
  
  // Toggle step expansion
  const toggleStepExpansion = (stepId: string) => {
    setExpandedSteps(prevExpanded => {
      const newExpanded = new Set(prevExpanded);
      if (newExpanded.has(stepId)) {
        newExpanded.delete(stepId);
      } else {
        newExpanded.add(stepId);
      }
      return newExpanded;
    });
  };

  // Format timestamp
  const formatTimestamp = (timestamp: string): string => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString();
  };

  // Format duration
  const formatDuration = (ms: number | undefined): string => {
    if (ms === undefined) return 'N/A';
    
    if (ms < 1000) {
      return `${ms}ms`;
    }
    
    return `${(ms / 1000).toFixed(2)}s`;
  };

  // Open screenshot modal
  const openScreenshot = (screenshot: Screenshot) => {
    setScreenshotModal({
      isOpen: true,
      screenshot
    });
  };

  // Close screenshot modal
  const closeScreenshot = () => {
    setScreenshotModal({
      isOpen: false,
      screenshot: null
    });
  };

  // Render loading state
  if (isLoading) {
    return (
      <div className="result-viewer d-flex justify-content-center align-items-center">
        <div className="text-center">
          <div className="spinner-border text-primary mb-3" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p>Loading test results...</p>
        </div>
      </div>
    );
  }

  // Render error state
  if (error) {
    return (
      <div className="result-viewer">
        <div className="alert alert-danger" role="alert">
          <h4 className="alert-heading">Error Loading Results</h4>
          <p>{error}</p>
          <hr />
          <div className="d-flex justify-content-end">
            <Button onClick={onBack}>
              Back to Test Runner
            </Button>
          </div>
        </div>
      </div>
    );
  }

  // Render empty state
  if (!result) {
    return (
      <div className="result-viewer d-flex justify-content-center align-items-center">
        <div className="text-center">
          <div className="alert alert-info">
            <h4>No Test Results Available</h4>
            <p>No test results were found for this execution.</p>
            <div className="mt-3">
              <Button onClick={onBack}>
                Back to Test Runner
              </Button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Define tab configuration
  const tabItems = [
    {
      id: 'steps',
      title: 'Steps',
      content: (
        <div className="result-step-list">
          {result.stepResults.length === 0 ? (
            <div className="text-center p-4 text-muted">
              No step results available for this test.
            </div>
          ) : (
            result.stepResults
              .sort((a, b) => a.order - b.order)
              .map((step) => (
                <div 
                  key={step.id} 
                  className="result-step-item"
                  onClick={() => toggleStepExpansion(step.id)}
                >
                  <div className="result-step-header">
                    <div className="d-flex align-items-center gap-2">
                      <StatusBadge status={step.status} />
                      <span className="result-step-name">{step.name}</span>
                      <i 
                        className={`bi ${expandedSteps.has(step.id) 
                          ? 'bi-chevron-up' 
                          : 'bi-chevron-down'}`}
                      ></i>
                    </div>
                    <div className="result-step-duration">
                      {formatDuration(step.duration)}
                    </div>
                  </div>
                  
                  {expandedSteps.has(step.id) && (
                    <div className="mt-2">
                      {step.status === TestStatus.FAILED && step.errorMessage && (
                        <div className="result-step-error">
                          {step.errorMessage}
                        </div>
                      )}
                      {step.screenshotId && (
                        <div className="mt-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              const screenshot = result.screenshots.find(s => s.id === step.screenshotId);
                              if (screenshot) {
                                openScreenshot(screenshot);
                              }
                            }}
                          >
                            <i className="bi bi-image me-1"></i>
                            View Screenshot
                          </Button>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))
          )}
        </div>
      )
    },
    {
      id: 'logs',
      title: 'Logs',
      content: (
        <div className="result-log-list">
          {result.logs.length === 0 ? (
            <div className="text-center p-4 text-muted">
              No logs available for this test.
            </div>
          ) : (
            result.logs.map((log, index) => (
              <div 
                key={index} 
                className={`result-log-entry result-log-entry-${log.level.toLowerCase()}`}
              >
                <span className="result-log-timestamp">
                  [{formatTimestamp(log.timestamp)}]
                </span>
                <span className="result-log-level">
                  [{log.level}]
                </span>
                <span className="result-log-message ms-2">
                  {log.message}
                </span>
              </div>
            ))
          )}
        </div>
      )
    },
    {
      id: 'screenshots',
      title: 'Screenshots',
      content: (
        <div className="result-screenshots">
          {result.screenshots.length === 0 ? (
            <div className="text-center p-4 text-muted">
              No screenshots available for this test.
            </div>
          ) : (
            result.screenshots.map((screenshot) => (
              <div key={screenshot.id} className="result-screenshot-item">
                <img
                  src={screenshot.base64Data 
                    ? `data:image/png;base64,${screenshot.base64Data}`
                    : screenshot.path
                  }
                  alt={screenshot.title}
                  className="result-screenshot-image"
                  onClick={() => openScreenshot(screenshot)}
                />
                <div className="result-screenshot-info">
                  <div className="result-screenshot-title">{screenshot.title}</div>
                  <div className="result-screenshot-time">
                    {formatTimestamp(screenshot.timestamp)}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )
    }
  ];

  return (
    <div className="result-viewer">
      <div className="result-viewer-header">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h4>Test Result: {result.name}</h4>
          <StatusBadge status={result.status} />
        </div>
        
        {onBack && (
          <div className="mb-3">
            <Button 
              variant="outline"
              size="sm"
              onClick={onBack}
            >
              <i className="bi bi-arrow-left me-1"></i>
              Back to Test Runner
            </Button>
          </div>
        )}
      </div>
      
      <div className="result-summary">
        <div className="row">
          <div className="col-md-6">
            <div className="result-summary-row">
              <div className="result-summary-label">Status:</div>
              <div><StatusBadge status={result.status} /></div>
            </div>
            <div className="result-summary-row">
              <div className="result-summary-label">Duration:</div>
              <div>{formatDuration(result.duration)}</div>
            </div>
            <div className="result-summary-row">
              <div className="result-summary-label">Start Time:</div>
              <div>{new Date(result.startTime).toLocaleString()}</div>
            </div>
            {result.endTime && (
              <div className="result-summary-row">
                <div className="result-summary-label">End Time:</div>
                <div>{new Date(result.endTime).toLocaleString()}</div>
              </div>
            )}
          </div>
          <div className="col-md-6">
            <div className="result-summary-row">
              <div className="result-summary-label">Execution ID:</div>
              <div>{result.executionId}</div>
            </div>
            <div className="result-summary-row">
              <div className="result-summary-label">Test ID:</div>
              <div>{result.testId}</div>
            </div>
            <div className="result-summary-row">
              <div className="result-summary-label">Steps:</div>
              <div>{result.stepResults.length}</div>
            </div>
            <div className="result-summary-row">
              <div className="result-summary-label">Retries:</div>
              <div>
                {result.retryIndex > 0 
                  ? `${result.retryIndex} of ${result.retryCount}`
                  : 'None'
                }
              </div>
            </div>
          </div>
        </div>
        
        {result.errorMessage && (
          <div className="mt-3">
            <div className="result-summary-label mb-1">Error Message:</div>
            <div className="result-step-error">{result.errorMessage}</div>
          </div>
        )}
      </div>
      
      <div className="result-tabs-container">
        <Tabs 
          items={tabItems}
          activeId={activeTabId}
          onChange={setActiveTabId}
        />
      </div>
      
      {/* Screenshot Modal */}
      {screenshotModal.isOpen && screenshotModal.screenshot && (
        <div 
          className="result-screenshot-modal"
          onClick={closeScreenshot}
        >
          <button 
            className="result-screenshot-modal-close"
            onClick={closeScreenshot}
          >
            &times;
          </button>
          <div className="result-screenshot-modal-content">
            <img
              src={screenshotModal.screenshot.base64Data 
                ? `data:image/png;base64,${screenshotModal.screenshot.base64Data}`
                : screenshotModal.screenshot.path
              }
              alt={screenshotModal.screenshot.title}
              className="result-screenshot-modal-image"
              onClick={(e) => e.stopPropagation()}
            />
          </div>
        </div>
      )}
    </div>
  );
};

export default ResultViewer; 