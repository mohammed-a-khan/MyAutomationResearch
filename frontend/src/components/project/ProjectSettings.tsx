import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useProjects } from '../../context/ProjectContext';
import { ProjectSettings as ProjectSettingsType } from '../../types/project';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';
import Checkbox from '../common/Checkbox';
import './Project.css';

/**
 * ProjectSettings component to manage project configuration settings
 */
const ProjectSettings: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const {
    selectedProject,
    loadProject,
    getProjectConfig,
    updateProjectConfig,
    isLoading,
    error,
  } = useProjects();

  const [settings, setSettings] = useState<ProjectSettingsType>({
    defaultTimeout: 30000,
    screenshotsEnabled: true,
    videoRecordingEnabled: false,
    parallelExecutionEnabled: false,
    maxParallelInstances: 1,
    retryFailedTests: false,
    maxRetries: 0,
    customSettings: {},
  });

  const [isSaving, setIsSaving] = useState(false);
  const [settingsLoaded, setSettingsLoaded] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [formSuccess, setFormSuccess] = useState<string | null>(null);

  // Load project and settings on component mount
  useEffect(() => {
    const loadData = async () => {
      if (projectId) {
        await loadProject(projectId);
        try {
          const projectSettings = await getProjectConfig(projectId);
          setSettings(projectSettings);
          setSettingsLoaded(true);
        } catch (error) {
          // Error handling is done in context
        }
      }
    };
    
    loadData();
  }, [projectId, loadProject, getProjectConfig]);

  // Handle form input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    
    setSettings(prev => ({
      ...prev,
      [name]: type === 'number' ? Number(value) : value,
    }));
  };

  // Handle checkbox changes
  const handleCheckboxChange = (name: string, checked: boolean) => {
    setSettings(prev => ({
      ...prev,
      [name]: checked,
    }));
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    
    if (!projectId) return;
    
    // Validate settings
    if (settings.maxParallelInstances < 1 || settings.maxParallelInstances > 10) {
      setFormError('Max parallel instances must be between 1 and 10');
      return;
    }
    
    if (settings.defaultTimeout < 1000 || settings.defaultTimeout > 300000) {
      setFormError('Default timeout must be between 1,000ms and 300,000ms');
      return;
    }
    
    if (settings.maxRetries < 0 || settings.maxRetries > 5) {
      setFormError('Max retries must be between 0 and 5');
      return;
    }
    
    try {
      setIsSaving(true);
      setFormError(null);
      
      await updateProjectConfig(projectId, settings);
      
      setFormSuccess('Settings saved successfully');
      
      // Clear success message after 3 seconds
      setTimeout(() => {
        setFormSuccess(null);
      }, 3000);
    } catch (error) {
      // Error is handled in context
    } finally {
      setIsSaving(false);
    }
  };

  // Handle navigation back to project
  const handleBackToProject = () => {
    navigate(`/projects/${projectId}`);
  };

  // Render loading state
  if (isLoading && !settingsLoaded) {
    return (
      <div className="text-center p-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
        <p className="mt-3">Loading project settings...</p>
      </div>
    );
  }

  // Render not found state
  if (!isLoading && !selectedProject) {
    return (
      <div className="project-empty-state">
        <div className="empty-state-icon">⚙️</div>
        <h3 className="empty-state-title">Project Not Found</h3>
        <p className="empty-state-message">
          The project you're looking for doesn't exist or you don't have permission to view it.
        </p>
        <Button onClick={() => navigate('/projects')}>Back to Projects</Button>
      </div>
    );
  }

  return (
    <div className="project-details">
      <div className="project-details-header">
        <div className="project-details-title">
          <Button
            variant="outline"
            size="sm"
            onClick={handleBackToProject}
          >
            ← Back to Project
          </Button>
          <h2>Project Settings: {selectedProject?.name}</h2>
        </div>
      </div>

      <div className="project-details-content">
        {/* Error Display */}
        {(error || formError) && (
          <div className="alert alert-danger" role="alert">
            {error || formError}
          </div>
        )}
        
        {/* Success Message */}
        {formSuccess && (
          <div className="alert alert-success" role="alert">
            {formSuccess}
          </div>
        )}

        <form onSubmit={handleSubmit} className="settings-form">
          {/* Execution Settings */}
          <div className="settings-group">
            <h3 className="settings-group-title">Execution Settings</h3>
            
            <div className="mb-3">
              <label htmlFor="defaultTimeout" className="form-label">Default Timeout (ms)</label>
              <Input
                id="defaultTimeout"
                name="defaultTimeout"
                type="number"
                value={settings.defaultTimeout}
                onChange={handleInputChange}
                min={1000}
                max={300000}
                helperText="Timeout for test actions in milliseconds (1,000 - 300,000)"
              />
            </div>

            <div className="mb-3">
              <label htmlFor="defaultBrowser" className="form-label">Default Browser</label>
              <Select
                id="defaultBrowser"
                name="defaultBrowser"
                value={settings.defaultBrowser || ''}
                onChange={handleInputChange}
                options={[
                  { value: 'chrome', label: 'Chrome' },
                  { value: 'firefox', label: 'Firefox' },
                  { value: 'edge', label: 'Microsoft Edge' },
                  { value: 'safari', label: 'Safari' },
                ]}
              />
            </div>
            
            <div className="mb-3">
              <label htmlFor="defaultEnvironment" className="form-label">Default Environment</label>
              <Select
                id="defaultEnvironment"
                name="defaultEnvironment"
                value={settings.defaultEnvironment || ''}
                onChange={handleInputChange}
                options={[
                  { value: '', label: 'None' },
                  ...(selectedProject?.environments?.map(env => ({
                    value: env.id,
                    label: env.name,
                  })) || []),
                ]}
              />
            </div>
          </div>
          
          {/* Capture Settings */}
          <div className="settings-group mt-4">
            <h3 className="settings-group-title">Capture Settings</h3>
            
            <div className="mb-3">
              <Checkbox
                id="screenshotsEnabled"
                name="screenshotsEnabled"
                label="Enable Screenshots"
                checked={settings.screenshotsEnabled}
                onChange={(e) => handleCheckboxChange('screenshotsEnabled', e.target.checked)}
              />
              <div className="form-help-text">
                Take screenshots automatically on test failures
              </div>
            </div>
            
            <div className="mb-3">
              <Checkbox
                id="videoRecordingEnabled"
                name="videoRecordingEnabled"
                label="Enable Video Recording"
                checked={settings.videoRecordingEnabled}
                onChange={(e) => handleCheckboxChange('videoRecordingEnabled', e.target.checked)}
              />
              <div className="form-help-text">
                Record video of test execution (may impact performance)
              </div>
            </div>
          </div>
          
          {/* Parallel Execution Settings */}
          <div className="settings-group mt-4">
            <h3 className="settings-group-title">Parallel Execution</h3>
            
            <div className="mb-3">
              <Checkbox
                id="parallelExecutionEnabled"
                name="parallelExecutionEnabled"
                label="Enable Parallel Execution"
                checked={settings.parallelExecutionEnabled}
                onChange={(e) => handleCheckboxChange('parallelExecutionEnabled', e.target.checked)}
              />
              <div className="form-help-text">
                Run tests in parallel to improve execution speed
              </div>
            </div>
            
            {settings.parallelExecutionEnabled && (
              <div className="mb-3">
                <label htmlFor="maxParallelInstances" className="form-label">Max Parallel Instances</label>
                <Input
                  id="maxParallelInstances"
                  name="maxParallelInstances"
                  type="number"
                  value={settings.maxParallelInstances}
                  onChange={handleInputChange}
                  min={1}
                  max={10}
                  helperText="Maximum number of concurrent test executions (1-10)"
                />
              </div>
            )}
          </div>
          
          {/* Retry Settings */}
          <div className="settings-group mt-4">
            <h3 className="settings-group-title">Retry Settings</h3>
            
            <div className="mb-3">
              <Checkbox
                id="retryFailedTests"
                name="retryFailedTests"
                label="Retry Failed Tests"
                checked={settings.retryFailedTests}
                onChange={(e) => handleCheckboxChange('retryFailedTests', e.target.checked)}
              />
              <div className="form-help-text">
                Automatically retry failed tests
              </div>
            </div>
            
            {settings.retryFailedTests && (
              <div className="mb-3">
                <label htmlFor="maxRetries" className="form-label">Max Retries</label>
                <Input
                  id="maxRetries"
                  name="maxRetries"
                  type="number"
                  value={settings.maxRetries}
                  onChange={handleInputChange}
                  min={0}
                  max={5}
                  helperText="Maximum number of retry attempts (0-5)"
                />
              </div>
            )}
          </div>
          
          {/* Form Actions */}
          <div className="form-actions mt-4">
            <Button
              type="button"
              variant="secondary"
              onClick={handleBackToProject}
              disabled={isSaving}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              disabled={isSaving}
            >
              {isSaving ? 'Saving...' : 'Save Settings'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProjectSettings; 