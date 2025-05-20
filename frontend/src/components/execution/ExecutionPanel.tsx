import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useExecution } from '../../context/ExecutionContext';
import Tabs from '../common/Tab';
import TestSelector from './TestSelector';
import TestRunner from './TestRunner';
import ResultViewer from './ResultViewer';
import ExecutionHistory from './ExecutionHistory';
import ParallelExecutionManager from './ParallelExecutionManager';
import './Execution.css';

interface ExecutionPanelProps {
  projectId?: string;
}

/**
 * ExecutionPanel component that coordinates the test execution workflow
 */
const ExecutionPanel: React.FC<ExecutionPanelProps> = ({ projectId: propProjectId }) => {
  const params = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  
  // Get project ID from props or URL params
  const projectId = propProjectId || params.projectId || '';
  
  // Component state
  const [selectedTab, setSelectedTab] = useState<string>('selector');
  const [selectedTestIds, setSelectedTestIds] = useState<string[]>([]);
  const [viewingExecutionId, setViewingExecutionId] = useState<string | null>(null);
  const [viewingTestId, setViewingTestId] = useState<string | null>(null);
  
  const { state: { currentExecution } } = useExecution();
  
  // If there's an active execution, show the runner tab
  useEffect(() => {
    if (currentExecution && selectedTab === 'selector') {
      setSelectedTab('runner');
    }
  }, [currentExecution, selectedTab]);
  
  // Handle test selection
  const handleTestsSelected = (testIds: string[]) => {
    setSelectedTestIds(testIds);
    setSelectedTab('runner');
  };
  
  // Handle execution view
  const handleViewExecution = (executionId: string) => {
    setViewingExecutionId(executionId);
    setViewingTestId(null);
    setSelectedTab('results');
  };
  
  // Handle back to test runner
  const handleBackToRunner = () => {
    setViewingExecutionId(null);
    setViewingTestId(null);
    setSelectedTab('runner');
  };
  
  // Define tab items
  const tabItems = [
    {
      id: 'selector',
      title: 'Select Tests',
      content: (
        <TestSelector
          projectId={projectId}
          onTestsSelected={handleTestsSelected}
        />
      )
    },
    {
      id: 'runner',
      title: 'Test Runner',
      content: (
        <TestRunner
          projectId={projectId}
          testIds={selectedTestIds}
          onViewResults={(executionId) => handleViewExecution(executionId)}
        />
      )
    },
    {
      id: 'results',
      title: 'Results',
      content: (
        <ResultViewer
          executionId={viewingExecutionId || (currentExecution ? currentExecution.id : '')}
          testId={viewingTestId || undefined}
          onBack={handleBackToRunner}
        />
      )
    },
    {
      id: 'history',
      title: 'History',
      content: (
        <ExecutionHistory
          projectId={projectId}
          onViewExecution={handleViewExecution}
        />
      )
    },
    {
      id: 'parallel',
      title: 'Parallel Execution',
      content: (
        <ParallelExecutionManager />
      )
    }
  ];
  
  // Validate project ID
  useEffect(() => {
    if (!projectId) {
      navigate('/projects');
    }
  }, [projectId, navigate]);
  
  return (
    <div className="execution-module">
      <div className="execution-header">
        <h2>Test Execution</h2>
        <p className="text-muted">
          Configure and run automated tests for your application.
        </p>
      </div>
      
      <div className="execution-container">
        <Tabs
          items={tabItems}
          activeId={selectedTab}
          onChange={setSelectedTab}
          variant="horizontal"
        />
      </div>
    </div>
  );
};

export default ExecutionPanel; 