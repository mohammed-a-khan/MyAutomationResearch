import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAdoIntegration } from '../../context/AdoIntegrationContext';
import Card from '../common/Card';
import Button from '../common/Button';
import Select from '../common/Select';
import Input from '../common/Input';
import Tabs from '../common/Tabs';
import Spinner from '../common/Spinner';
import Alert from '../common/Alert';
import ConnectionForm from './ConnectionForm';
import TestSyncConfig from './TestSyncConfig';
import PipelineConfig from './PipelineConfig';
import './AdoIntegration.css';

/**
 * AdoIntegrationPanel component that provides interface for Azure DevOps integration
 */
const AdoIntegrationPanel: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<string>('connections');
  const [selectedConnectionId, setSelectedConnectionId] = useState<string>('');
  
  const {
    connections,
    isLoading,
    error,
    getConnections,
    clearError
  } = useAdoIntegration();

  // Load connections when component mounts
  useEffect(() => {
    getConnections().catch(err => {
      console.error('Failed to load ADO connections:', err);
    });
  }, [getConnections]);

  // Handle connection selection
  const handleConnectionChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setSelectedConnectionId(e.target.value);
  };

  // Handle tab change
  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
  };

  // Handle project selection
  const handleProjectSelect = (id: string) => {
    navigate(`/ado-integration/${id}`);
  };

  // Render loading state
  if (isLoading && !connections.length) {
    return <Spinner text="Loading ADO Integration..." />;
  }

  return (
    <div className="ado-integration-container">
      <h1 className="page-title">Azure DevOps Integration</h1>
      
      {error && (
        <Alert 
          type="error" 
          message={error} 
          onClose={clearError} 
        />
      )}
      
      <Tabs
        activeTab={activeTab}
        onChange={handleTabChange}
        tabs={[
          { id: 'connections', label: 'Connections' },
          { id: 'test-sync', label: 'Test Case Sync', disabled: !selectedConnectionId },
          { id: 'pipelines', label: 'Pipeline Integration', disabled: !selectedConnectionId }
        ]}
      />
      
      <div className="ado-integration-content">
        {activeTab === 'connections' && (
          <Card className="connections-card">
            <div className="connections-header">
              <h2>Azure DevOps Connections</h2>
              <div className="connection-controls">
                <Select
                  label="Select Connection"
                  value={selectedConnectionId}
                  onChange={handleConnectionChange}
                  options={[
                    { value: '', label: 'Select Connection' },
                    ...connections.map(conn => ({
                      value: conn.id,
                      label: `${conn.name} (${conn.organizationName}/${conn.projectName})`
                    }))
                  ]}
                />
              </div>
            </div>
            
            <ConnectionForm 
              existingConnectionId={selectedConnectionId}
              onConnectionSaved={() => {
                getConnections();
              }}
            />
          </Card>
        )}
        
        {activeTab === 'test-sync' && selectedConnectionId && (
          <Card className="test-sync-card">
            <h2>Test Case Synchronization</h2>
            <TestSyncConfig 
              connectionId={selectedConnectionId} 
              projectId={projectId || ''}
              onProjectSelect={handleProjectSelect}
            />
          </Card>
        )}
        
        {activeTab === 'pipelines' && selectedConnectionId && (
          <Card className="pipelines-card">
            <h2>Pipeline Integration</h2>
            <PipelineConfig 
              connectionId={selectedConnectionId} 
              projectId={projectId || ''}
              onProjectSelect={handleProjectSelect}
            />
          </Card>
        )}
      </div>
    </div>
  );
};

export default AdoIntegrationPanel; 