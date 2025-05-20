import React, { useState, useEffect } from 'react';
import { useAdoIntegration, SyncConfig } from '../../context/AdoIntegrationContext';
import Select from '../common/Select';
import Button from '../common/Button';
import Spinner from '../common/Spinner';
import { useExecution } from '../../context/ExecutionContext';

interface TestSyncConfigProps {
  connectionId: string;
  projectId: string;
  onProjectSelect: (projectId: string) => void;
}

/**
 * Component for configuring test case synchronization with Azure DevOps
 */
const TestSyncConfig: React.FC<TestSyncConfigProps> = ({
  connectionId,
  projectId,
  onProjectSelect
}) => {
  const [config, setConfig] = useState<SyncConfig>({
    connectionId: connectionId,
    projectId: projectId,
    testPlanId: '',
    testSuiteId: '',
    syncFrequency: 'manual',
    bidirectional: false
  });
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isSyncing, setIsSyncing] = useState<boolean>(false);
  const [message, setMessage] = useState<{text: string, type: 'success' | 'error' | 'info'} | null>(null);
  const [testCasesCount, setTestCasesCount] = useState<number>(0);

  const {
    projects,
    testPlans,
    testSuites,
    syncStatus,
    isLoading,
    getProjects,
    getTestPlans,
    getTestSuites,
    getSyncConfig,
    saveSyncConfig,
    getSyncStatus,
    startSync
  } = useAdoIntegration();

  const { state } = useExecution();

  // Load projects when component mounts or connection changes
  useEffect(() => {
    if (connectionId) {
      getProjects(connectionId).catch(err => {
        console.error('Failed to load ADO projects:', err);
        setMessage({
          text: 'Failed to load Azure DevOps projects. Please check your connection and try again.',
          type: 'error'
        });
      });
    }
  }, [connectionId, getProjects]);

  // Load test case count when needed
  useEffect(() => {
    // Here we would normally fetch the test case count from an API
    // For now, just set a placeholder value
    setTestCasesCount(state.executionHistory.length || 0);
  }, [state.executionHistory]);

  // Load sync configuration and status when project is selected
  useEffect(() => {
    if (projectId) {
      const loadConfig = async () => {
        try {
          const config = await getSyncConfig(projectId);
          if (config) {
            setConfig(config);
            
            // If we have a connection and test plan, load the test suites
            if (config.connectionId && config.testPlanId) {
              await getTestPlans(config.connectionId, projectId);
              await getTestSuites(config.connectionId, projectId, config.testPlanId);
            }
          }
          
          // Load sync status
          await getSyncStatus(projectId);
        } catch (err) {
          console.error('Failed to load sync configuration:', err);
          setMessage({
            text: 'Failed to load sync configuration. Please try again.',
            type: 'error'
          });
        }
      };
      
      loadConfig();
    }
  }, [projectId, getSyncConfig, getSyncStatus, getTestPlans, getTestSuites]);

  // Load test plans when project or connection ID changes
  useEffect(() => {
    if (connectionId && projectId) {
      getTestPlans(connectionId, projectId).catch(err => {
        console.error('Failed to load ADO test plans:', err);
        setMessage({
          text: 'Failed to load Azure DevOps test plans. Please try again.',
          type: 'error'
        });
      });
    }
  }, [connectionId, projectId, getTestPlans]);

  // Load test suites when test plan changes
  useEffect(() => {
    if (connectionId && projectId && config.testPlanId) {
      getTestSuites(connectionId, projectId, config.testPlanId).catch(err => {
        console.error('Failed to load ADO test suites:', err);
        setMessage({
          text: 'Failed to load Azure DevOps test suites. Please try again.',
          type: 'error'
        });
      });
    }
  }, [connectionId, projectId, config.testPlanId, getTestSuites]);

  // Handle project selection
  const handleProjectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onProjectSelect(e.target.value);
  };

  // Handle test plan selection
  const handleTestPlanChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const testPlanId = e.target.value;
    setConfig(prev => ({ ...prev, testPlanId, testSuiteId: '' }));
  };

  // Handle test suite selection
  const handleTestSuiteChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setConfig(prev => ({ ...prev, testSuiteId: e.target.value }));
  };

  // Handle sync frequency change
  const handleFrequencyChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setConfig(prev => ({ 
      ...prev, 
      syncFrequency: e.target.value as 'manual' | 'daily' | 'weekly' 
    }));
  };

  // Handle bidirectional sync toggle
  const handleBidirectionalChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setConfig(prev => ({ ...prev, bidirectional: e.target.checked }));
  };

  // Save sync configuration
  const handleSaveConfig = async () => {
    setIsSaving(true);
    setMessage(null);
    
    try {
      await saveSyncConfig(projectId, config);
      setMessage({
        text: 'Sync configuration saved successfully!',
        type: 'success'
      });
    } catch (error) {
      setMessage({
        text: error instanceof Error 
          ? error.message 
          : 'Failed to save sync configuration. Please try again.',
        type: 'error'
      });
    } finally {
      setIsSaving(false);
    }
  };

  // Start manual synchronization
  const handleStartSync = async () => {
    setIsSyncing(true);
    setMessage(null);
    
    try {
      const success = await startSync(projectId);
      if (success) {
        setMessage({
          text: 'Synchronization started successfully! This process may take a few minutes to complete.',
          type: 'success'
        });
        
        // Refresh the sync status after a slight delay
        setTimeout(() => {
          getSyncStatus(projectId);
        }, 2000);
      } else {
        setMessage({
          text: 'Failed to start synchronization. Please try again.',
          type: 'error'
        });
      }
    } catch (error) {
      setMessage({
        text: error instanceof Error 
          ? error.message 
          : 'Failed to start synchronization. Please try again.',
        type: 'error'
      });
    } finally {
      setIsSyncing(false);
    }
  };

  // Format timestamp for display
  const formatTimestamp = (timestamp: string | null) => {
    if (!timestamp) return 'Never';
    return new Date(timestamp).toLocaleString();
  };

  if (isLoading && !projects.length) {
    return <Spinner text="Loading test sync configuration..." />;
  }

  return (
    <div className="test-sync-config">
      <div className="sync-form">
        <div className="form-grid">
          <Select
            label="Project"
            value={projectId}
            onChange={handleProjectChange}
            options={[
              { value: '', label: 'Select Project' },
              ...projects.map(project => ({
                value: project.id,
                label: project.name
              }))
            ]}
          />
          
          {projectId && (
            <>
              <Select
                label="Test Plan"
                value={config.testPlanId}
                onChange={handleTestPlanChange}
                options={[
                  { value: '', label: 'Select Test Plan' },
                  ...testPlans.map(plan => ({
                    value: plan.id,
                    label: plan.name
                  }))
                ]}
              />
              
              {config.testPlanId && (
                <Select
                  label="Test Suite"
                  value={config.testSuiteId}
                  onChange={handleTestSuiteChange}
                  options={[
                    { value: '', label: 'Select Test Suite' },
                    ...testSuites.map(suite => ({
                      value: suite.id,
                      label: suite.name
                    }))
                  ]}
                />
              )}
              
              <Select
                label="Sync Frequency"
                value={config.syncFrequency}
                onChange={handleFrequencyChange}
                options={[
                  { value: 'manual', label: 'Manual Only' },
                  { value: 'daily', label: 'Daily' },
                  { value: 'weekly', label: 'Weekly' }
                ]}
              />
              
              <div className="checkbox-field">
                <label>
                  <input
                    type="checkbox"
                    checked={config.bidirectional}
                    onChange={handleBidirectionalChange}
                  />
                  Bidirectional Sync (sync changes from ADO to CSTestForge)
                </label>
              </div>
            </>
          )}
        </div>
        
        {message && (
          <div className={`message ${message.type}`}>
            {message.text}
          </div>
        )}
        
        {projectId && (
          <div className="form-actions">
            <Button
              variant="primary"
              onClick={handleSaveConfig}
              disabled={isSaving || isLoading || !projectId || !config.testPlanId || !config.testSuiteId}
            >
              {isSaving ? 'Saving...' : 'Save Configuration'}
            </Button>
            
            <Button
              variant="secondary"
              onClick={handleStartSync}
              disabled={isSyncing || isLoading || !projectId || !config.testPlanId || !config.testSuiteId}
            >
              {isSyncing ? 'Starting Sync...' : 'Start Sync Now'}
            </Button>
          </div>
        )}
      </div>
      
      {syncStatus && projectId && (
        <div className="sync-status">
          <h3>Synchronization Status</h3>
          
          <div className="status-grid">
            <div className="status-item">
              <span className="status-label">Last Sync:</span>
              <span className="status-value">{formatTimestamp(syncStatus.lastSyncTime)}</span>
            </div>
            
            <div className="status-item">
              <span className="status-label">Status:</span>
              <span className={`status-value status-${syncStatus.lastSyncStatus}`}>
                {syncStatus.lastSyncStatus === 'success' ? 'Successful' : 
                  syncStatus.lastSyncStatus === 'failed' ? 'Failed' : 
                  syncStatus.lastSyncStatus === 'in-progress' ? 'In Progress' : 
                  'Not Started'}
              </span>
            </div>
            
            {syncStatus.lastSyncMessage && (
              <div className="status-item full-width">
                <span className="status-label">Message:</span>
                <span className="status-value">{syncStatus.lastSyncMessage}</span>
              </div>
            )}
            
            {syncStatus.nextScheduledSync && (
              <div className="status-item">
                <span className="status-label">Next Scheduled Sync:</span>
                <span className="status-value">{formatTimestamp(syncStatus.nextScheduledSync)}</span>
              </div>
            )}
            
            <div className="status-item">
              <span className="status-label">Local Test Cases:</span>
              <span className="status-value">{testCasesCount}</span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TestSyncConfig; 