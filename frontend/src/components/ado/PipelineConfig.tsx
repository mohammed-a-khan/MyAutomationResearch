import React, { useState, useEffect } from 'react';
import { useAdoIntegration, PipelineConfig as PipelineConfigType } from '../../context/AdoIntegrationContext';
import Select from '../common/Select';
import Button from '../common/Button';
import Spinner from '../common/Spinner';

interface PipelineConfigProps {
  connectionId: string;
  projectId: string;
  onProjectSelect: (projectId: string) => void;
}

/**
 * Component for configuring pipeline integration with Azure DevOps
 */
const PipelineConfig: React.FC<PipelineConfigProps> = ({
  connectionId,
  projectId,
  onProjectSelect
}) => {
  const [config, setConfig] = useState<PipelineConfigType>({
    connectionId: connectionId,
    projectId: projectId,
    pipelineId: '',
    triggerMode: 'manual',
    includeTest: true
  });
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isTriggering, setIsTriggering] = useState<boolean>(false);
  const [message, setMessage] = useState<{text: string, type: 'success' | 'error' | 'info'} | null>(null);

  const {
    projects,
    pipelines,
    isLoading,
    getProjects,
    getPipelines,
    getPipelineConfig,
    savePipelineConfig,
    triggerPipeline
  } = useAdoIntegration();

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

  // Load pipelines and configuration when project is selected
  useEffect(() => {
    if (connectionId && projectId) {
      const loadConfig = async () => {
        try {
          // Load pipelines
          await getPipelines(connectionId, projectId);
          
          // Load pipeline configuration
          const config = await getPipelineConfig(projectId);
          if (config) {
            setConfig(config);
          }
        } catch (err) {
          console.error('Failed to load pipeline data:', err);
          setMessage({
            text: 'Failed to load pipeline configuration. Please try again.',
            type: 'error'
          });
        }
      };
      
      loadConfig();
    }
  }, [connectionId, projectId, getPipelines, getPipelineConfig]);

  // Handle project selection
  const handleProjectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    onProjectSelect(e.target.value);
  };

  // Handle pipeline selection
  const handlePipelineChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setConfig(prev => ({ ...prev, pipelineId: e.target.value }));
  };

  // Handle trigger mode change
  const handleTriggerModeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    setConfig(prev => ({ 
      ...prev, 
      triggerMode: e.target.value as 'manual' | 'auto' 
    }));
  };

  // Handle include test toggle
  const handleIncludeTestChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setConfig(prev => ({ ...prev, includeTest: e.target.checked }));
  };

  // Save pipeline configuration
  const handleSaveConfig = async () => {
    setIsSaving(true);
    setMessage(null);
    
    try {
      await savePipelineConfig(projectId, config);
      setMessage({
        text: 'Pipeline configuration saved successfully!',
        type: 'success'
      });
    } catch (error) {
      setMessage({
        text: error instanceof Error 
          ? error.message 
          : 'Failed to save pipeline configuration. Please try again.',
        type: 'error'
      });
    } finally {
      setIsSaving(false);
    }
  };

  // Trigger pipeline execution
  const handleTriggerPipeline = async () => {
    setIsTriggering(true);
    setMessage(null);
    
    try {
      const success = await triggerPipeline(projectId);
      if (success) {
        setMessage({
          text: 'Pipeline triggered successfully! Please check Azure DevOps for execution status.',
          type: 'success'
        });
      } else {
        setMessage({
          text: 'Failed to trigger pipeline. Please try again.',
          type: 'error'
        });
      }
    } catch (error) {
      setMessage({
        text: error instanceof Error 
          ? error.message 
          : 'Failed to trigger pipeline. Please try again.',
        type: 'error'
      });
    } finally {
      setIsTriggering(false);
    }
  };

  if (isLoading && !projects.length) {
    return <Spinner text="Loading pipeline configuration..." />;
  }

  return (
    <div className="pipeline-config">
      <div className="pipeline-form">
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
                label="Pipeline"
                value={config.pipelineId}
                onChange={handlePipelineChange}
                options={[
                  { value: '', label: 'Select Pipeline' },
                  ...pipelines.map(pipeline => ({
                    value: pipeline.id,
                    label: pipeline.name
                  }))
                ]}
              />
              
              <Select
                label="Trigger Mode"
                value={config.triggerMode}
                onChange={handleTriggerModeChange}
                options={[
                  { value: 'manual', label: 'Manual Only' },
                  { value: 'auto', label: 'Automatic (After Test Execution)' }
                ]}
              />
              
              <div className="checkbox-field">
                <label>
                  <input
                    type="checkbox"
                    checked={config.includeTest}
                    onChange={handleIncludeTestChange}
                  />
                  Include Test Results in Pipeline Run
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
              disabled={isSaving || isLoading || !projectId || !config.pipelineId}
            >
              {isSaving ? 'Saving...' : 'Save Configuration'}
            </Button>
            
            <Button
              variant="secondary"
              onClick={handleTriggerPipeline}
              disabled={isTriggering || isLoading || !projectId || !config.pipelineId}
            >
              {isTriggering ? 'Triggering...' : 'Trigger Pipeline Now'}
            </Button>
          </div>
        )}
      </div>
      
      {projectId && config.pipelineId && (
        <div className="pipeline-info">
          <h3>Pipeline Integration Information</h3>
          
          <div className="info-grid">
            <div className="info-item">
              <span className="info-label">Pipeline:</span>
              <span className="info-value">
                {pipelines.find(p => p.id === config.pipelineId)?.name || 'Unknown'}
              </span>
            </div>
            
            <div className="info-item">
              <span className="info-label">Trigger Mode:</span>
              <span className="info-value">
                {config.triggerMode === 'manual' ? 'Manual Only' : 'Automatic (After Test Execution)'}
              </span>
            </div>
            
            <div className="info-item">
              <span className="info-label">Include Test Results:</span>
              <span className="info-value">{config.includeTest ? 'Yes' : 'No'}</span>
            </div>
            
            <div className="info-item">
              <span className="info-label">Integration Status:</span>
              <span className="info-value status-success">Configured</span>
            </div>
          </div>
          
          <div className="pipeline-notes">
            <h4>Integration Notes:</h4>
            <ul>
              <li>Pipeline executions will use the latest test results from CSTestForge when the "Include Test Results" option is enabled.</li>
              <li>Automatic triggers will fire after any test execution is completed in CSTestForge.</li>
              <li>Pipeline executions can be monitored directly in Azure DevOps.</li>
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default PipelineConfig; 