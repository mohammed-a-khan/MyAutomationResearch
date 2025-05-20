import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useExecution } from '../../context/ExecutionContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Select from '../common/Select';
import Input from '../common/Input';
import Spinner from '../common/Spinner';
import TestResultsList from '../execution/TestResultsList';
import TestExecutionControls from '../execution/TestExecutionControls';
import { BrowserType, TestExecutionRequest } from '../../types/execution';
import './TestRunner.css';
import api from '../../services/api';

interface Environment {
  id: string;
  name: string;
}

interface TestCase {
  id: string;
  name: string;
  suiteId: string;
  suiteName: string;
  tags?: string[];
}

/**
 * TestRunnerPanel component that provides the interface for running tests
 */
const TestRunnerPanel: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const [selectedEnvironment, setSelectedEnvironment] = useState<string>('');
  const [selectedTests, setSelectedTests] = useState<string[]>([]);
  const [isQueued, setIsQueued] = useState<boolean>(false);
  const [filterOptions, setFilterOptions] = useState({
    suite: '',
    tag: '',
    search: '',
  });
  const [environments, setEnvironments] = useState<Environment[]>([]);
  const [testCases, setTestCases] = useState<TestCase[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const {
    state,
    getExecutionStatus,
    runTests,
    stopExecution,
    clearError
  } = useExecution();

  // Load environments and test cases
  useEffect(() => {
    if (projectId) {
      setLoading(true);
      
      // Fetch environments and test cases from API
      Promise.all([
        api.get(`/projects/${projectId}/environments`),
        api.get(`/projects/${projectId}/test-cases`)
      ])
      .then(([environments, testCases]) => {
        setEnvironments(environments);
        setTestCases(testCases);
        setLoading(false);
      })
      .catch((err) => {
        console.error('Error loading test runner data:', err);
        setError('Failed to load test runner data. Please try again.');
        setLoading(false);
      });
    }
  }, [projectId]);

  // Load test results if there's an ongoing execution
  useEffect(() => {
    if (projectId && state?.currentExecution?.id) {
      getExecutionStatus(state.currentExecution.id);
      setIsQueued(true);
    }
  }, [projectId, state?.currentExecution, getExecutionStatus]);

  // Filter test cases based on selected options
  const filteredTestCases = testCases.filter(test => {
    const matchesSuite = filterOptions.suite ? test.suiteId === filterOptions.suite : true;
    const matchesTag = filterOptions.tag 
      ? test.tags && test.tags.includes(filterOptions.tag) 
      : true;
    const matchesSearch = filterOptions.search 
      ? test.name.toLowerCase().includes(filterOptions.search.toLowerCase()) 
      : true;
    
    return matchesSuite && matchesTag && matchesSearch;
  });

  // Handle test selection
  const handleTestSelection = (testId: string) => {
    setSelectedTests(prev => {
      if (prev.includes(testId)) {
        return prev.filter(id => id !== testId);
      } else {
        return [...prev, testId];
      }
    });
  };

  // Handle environment selection
  const handleEnvironmentChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedEnvironment(e.target.value);
  };

  // Handle filter changes
  const handleFilterChange = (field: string, value: string) => {
    setFilterOptions(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Handle test execution
  const handleRunTests = async () => {
    if (!projectId || !selectedEnvironment || selectedTests.length === 0) {
      return;
    }

    try {
      setLoading(true);
      const testExecutionRequest: TestExecutionRequest = {
        projectId,
        testIds: selectedTests,
        config: {
          environment: selectedEnvironment,
          browser: BrowserType.CHROME,
          headless: false,
          retryCount: 1,
          timeoutSeconds: 30,
          parallel: true,
          maxParallel: 4
        }
      };
      
      await runTests(testExecutionRequest);
      setIsQueued(true);
    } catch (err) {
      console.error('Failed to execute tests:', err);
      setError(err instanceof Error ? err.message : 'Failed to execute tests');
    } finally {
      setLoading(false);
    }
  };

  // Handle stopping the test execution
  const handleStopTests = async () => {
    if (state?.currentExecution?.id) {
      try {
        setLoading(true);
        await stopExecution(state.currentExecution.id);
        setIsQueued(false);
      } catch (err) {
        console.error('Failed to stop test execution:', err);
        setError(err instanceof Error ? err.message : 'Failed to stop execution');
      } finally {
        setLoading(false);
      }
    }
  };

  // Clear errors
  const handleClearError = () => {
    setError(null);
    clearError();
  };

  // Get unique test suites and tags for filters
  const testSuitesArray = testCases.map(test => ({ 
    value: test.suiteId, 
    label: test.suiteName 
  }));
  
  const uniqueTestSuites = testSuitesArray.filter((suite, index, self) =>
    index === self.findIndex((s) => s.value === suite.value)
  );
  
  const allTags = testCases.flatMap(test => test.tags || []);
  const uniqueTags = Array.from(new Set(allTags)).map(tag => ({ 
    value: tag, 
    label: tag 
  }));

  if (loading && !testCases.length) {
    return <Spinner text="Loading test runner..." />;
  }

  return (
    <div className="test-runner-container">
      <h1 className="page-title">Test Runner</h1>
      
      {(error || state?.error) && (
        <div className="error-message" onClick={handleClearError}>
          {error || state?.error}
          <span className="close-error">&times;</span>
        </div>
      )}
      
      <div className="test-runner-grid">
        {/* Test Selection Panel */}
        <Card className="test-selection-panel">
          <h2>Test Selection</h2>
          
          <div className="test-filters">
            <Select
              label="Test Suite"
              value={filterOptions.suite}
              onChange={(e) => handleFilterChange('suite', e.target.value)}
              options={[{ value: '', label: 'All Suites' }, ...uniqueTestSuites]}
            />
            
            <Select
              label="Tag"
              value={filterOptions.tag}
              onChange={(e) => handleFilterChange('tag', e.target.value)}
              options={[{ value: '', label: 'All Tags' }, ...uniqueTags]}
            />
            
            <Input
              label="Search"
              type="text"
              value={filterOptions.search}
              onChange={(e) => handleFilterChange('search', e.target.value)}
              placeholder="Search test cases..."
            />
          </div>
          
          <div className="test-cases-list">
            {filteredTestCases.length === 0 ? (
              <div className="no-tests-message">
                No test cases found matching the criteria.
              </div>
            ) : (
              <div className="test-case-items">
                {filteredTestCases.map(test => (
                  <div 
                    key={test.id}
                    className={`test-case-item ${selectedTests.includes(test.id) ? 'selected' : ''}`}
                    onClick={() => handleTestSelection(test.id)}
                  >
                    <div className="test-case-checkbox">
                      <input 
                        type="checkbox" 
                        checked={selectedTests.includes(test.id)}
                        onChange={() => {}}
                      />
                    </div>
                    <div className="test-case-details">
                      <div className="test-case-name">{test.name}</div>
                      <div className="test-case-suite">{test.suiteName}</div>
                      {test.tags && test.tags.length > 0 && (
                        <div className="test-case-tags">
                          {test.tags.map(tag => (
                            <span key={tag} className="test-tag">{tag}</span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
          
          <div className="test-selection-controls">
            <div className="environment-selector">
              <Select
                label="Environment"
                value={selectedEnvironment}
                onChange={handleEnvironmentChange}
                options={[
                  { value: '', label: 'Select Environment' },
                  ...environments.map(env => ({
                    value: env.id,
                    label: env.name
                  }))
                ]}
              />
            </div>
            
            <div className="selection-stats">
              Selected: {selectedTests.length} of {testCases.length} tests
            </div>
            
            <div className="run-controls">
              <Button
                variant="primary"
                onClick={handleRunTests}
                disabled={loading || isQueued || selectedTests.length === 0 || !selectedEnvironment}
              >
                {loading ? 'Starting...' : 'Run Selected Tests'}
              </Button>
            </div>
          </div>
        </Card>
        
        {/* Test Execution Panel */}
        <Card className="test-execution-panel">
          <h2>Test Execution</h2>
          
          {isQueued ? (
            <>
              <TestExecutionControls 
                execution={state?.currentExecution}
                onStop={handleStopTests}
                onRerun={handleRunTests}
              />
              
              <div className="test-results-section">
                <h3>Results</h3>
                {state?.executionDetail?.results && (
                  <TestResultsList 
                    results={state.executionDetail.results}
                    selectedResultId={state?.selectedTestResult?.id}
                    onSelectResult={(result) => {
                      // Handle result selection
                    }}
                  />
                )}
              </div>
            </>
          ) : (
            <div className="no-execution-message">
              No tests are currently running. Select tests and click "Run Selected Tests" to start execution.
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

export default TestRunnerPanel;