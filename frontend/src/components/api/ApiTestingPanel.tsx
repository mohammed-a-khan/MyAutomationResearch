import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { ApiTestingProvider, useApiTesting } from '../../context/ApiTestingContext';
import ApiRequestList from './ApiRequestList';
import ApiRequestEditor from './ApiRequestEditor';
import ApiRequestRunner from './ApiRequestRunner';
import ApiHistoryViewer from './ApiHistoryViewer';
import Tabs from '../common/Tab';

// Import styles
import './ApiTesting.css';

interface ApiTestingPanelProps {
  projectId?: string;
}

const ApiTestingContainer: React.FC<ApiTestingPanelProps> = ({ projectId }) => {
  const [activeTab, setActiveTab] = useState('requests');
  const [selectedRequestId, setSelectedRequestId] = useState<string | null>(null);
  const { 
    fetchApiRequests, 
    getRequestById, 
    currentRequest
  } = useApiTesting();

  useEffect(() => {
    const loadRequests = async () => {
      await fetchApiRequests(projectId);
    };
    
    loadRequests();
  }, [fetchApiRequests, projectId]);

  const handleRequestSelect = async (requestId: string) => {
    setSelectedRequestId(requestId);
    await getRequestById(requestId);
    setActiveTab('editor');
  };

  const handleCreateNew = () => {
    setSelectedRequestId(null);
    setActiveTab('editor');
  };

  // Define tab items
  const tabItems = [
    {
      id: 'requests',
      title: 'API Requests',
      content: (
        <ApiRequestList 
          onSelectRequest={handleRequestSelect}
          onCreateNew={handleCreateNew}
          projectId={projectId}
        />
      )
    },
    {
      id: 'editor',
      title: 'Request Editor',
      content: (
        <ApiRequestEditor 
          requestId={selectedRequestId}
          projectId={projectId}
          onSaved={() => setActiveTab('runner')}
        />
      )
    },
    {
      id: 'runner',
      title: 'Request Runner',
      content: <ApiRequestRunner />,
      disabled: !currentRequest
    },
    {
      id: 'history',
      title: 'History',
      content: (
        <ApiHistoryViewer 
          requestId={selectedRequestId}
          projectId={projectId}
        />
      )
    }
  ];

  return (
    <div className="api-testing-panel">
      <h2>API Testing</h2>
      
      <Tabs
        items={tabItems}
        activeId={activeTab}
        onChange={setActiveTab}
        variant="horizontal"
      />
    </div>
  );
};

const ApiTestingPanel: React.FC = () => {
  const { projectId } = useParams<{ projectId?: string }>();
  
  return (
    <ApiTestingProvider>
      <ApiTestingContainer projectId={projectId} />
    </ApiTestingProvider>
  );
};

export default ApiTestingPanel; 