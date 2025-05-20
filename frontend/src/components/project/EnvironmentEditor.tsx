import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useProjects } from '../../context/ProjectContext';
import EnvironmentForm from './EnvironmentForm';
import Button from '../common/Button';
import './Project.css';

interface EnvironmentEditorProps {
  isNew?: boolean;
}

/**
 * EnvironmentEditor component for creating or editing project environments
 */
const EnvironmentEditor: React.FC<EnvironmentEditorProps> = ({ isNew = false }) => {
  const { projectId, environmentId } = useParams<{ projectId: string; environmentId: string }>();
  const navigate = useNavigate();
  const {
    selectedProject,
    loadProject,
    getEnvironment,
    createEnvironment,
    updateEnvironment,
    isLoading,
    error,
  } = useProjects();

  const [environment, setEnvironment] = useState<any>(null);
  const [isSaving, setIsSaving] = useState(false);

  // Load project and environment data
  useEffect(() => {
    const loadData = async () => {
      if (!projectId) return;

      await loadProject(projectId);

      if (!isNew && environmentId) {
        try {
          const envData = await getEnvironment(projectId, environmentId);
          setEnvironment(envData);
        } catch (error) {
          // Error handling is done in context
        }
      }
    };

    loadData();
  }, [projectId, environmentId, isNew, loadProject, getEnvironment]);

  // Handle form submission
  const handleSubmit = async (environmentData: any) => {
    if (!projectId) return;

    try {
      setIsSaving(true);

      if (isNew) {
        await createEnvironment(projectId, environmentData);
      } else if (environmentId) {
        await updateEnvironment(projectId, environmentId, environmentData);
      }

      navigate(`/projects/${projectId}`);
    } catch (error) {
      // Error handling is done in context
    } finally {
      setIsSaving(false);
    }
  };

  // Handle cancel
  const handleCancel = () => {
    navigate(`/projects/${projectId}`);
  };

  // Render loading state
  if (isLoading && !isNew && !environment) {
    return (
        <div className="text-center p-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading environment data...</p>
        </div>
    );
  }

  // Render not found state
  if (!isLoading && !selectedProject) {
    return (
        <div className="project-empty-state">
          <div className="empty-state-icon">🌐</div>
          <h3 className="empty-state-title">Project Not Found</h3>
          <p className="empty-state-message">
            The project you're looking for doesn't exist or you don't have permission to view it.
          </p>
          <Button onClick={() => navigate('/projects')}>Back to Projects</Button>
        </div>
    );
  }

  // Render not found state for environment
  if (!isLoading && !isNew && !environment && environmentId) {
    return (
        <div className="project-empty-state">
          <div className="empty-state-icon">🌐</div>
          <h3 className="empty-state-title">Environment Not Found</h3>
          <p className="empty-state-message">
            The environment you're looking for doesn't exist or you don't have permission to view it.
          </p>
          <Button onClick={() => navigate(`/projects/${projectId}`)}>Back to Project</Button>
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
                onClick={handleCancel}
            >
              ← Back to Project
            </Button>
            <h2>
              {isNew ? 'Create Environment' : 'Edit Environment'}
              {selectedProject && `: ${selectedProject.name}`}
            </h2>
          </div>
        </div>

        <div className="project-details-content">
          {/* Error Display */}
          {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
          )}

          {/* Environment Form */}
          <EnvironmentForm
              projectId={projectId || ''}
              initialData={environment}
              onSubmit={handleSubmit}
              onCancel={handleCancel}
              isSaving={isSaving}
          />
        </div>
      </div>
  );
};

export default EnvironmentEditor;