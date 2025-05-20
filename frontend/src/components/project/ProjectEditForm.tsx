import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useProjects } from '../../context/ProjectContext';
import ProjectForm from './ProjectForm';
import Button from '../common/Button';
import './Project.css';

/**
 * ProjectEditForm component for editing existing projects
 */
const ProjectEditForm: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const {
    selectedProject,
    loadProject,
    isLoading,
    error,
  } = useProjects();

  // Load project on component mount
  useEffect(() => {
    if (projectId) {
      loadProject(projectId);
    }
  }, [projectId, loadProject]);

  // Handle navigation back to project
  const handleBackToProject = () => {
    if (projectId) {
      navigate(`/projects/${projectId}`);
    } else {
      navigate('/projects');
    }
  };

  // Handle form submission
  const handleFormSubmit = () => {
    navigate(`/projects/${projectId}`);
  };

  // Render loading state
  if (isLoading) {
    return (
      <div className="text-center p-5">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
        <p className="mt-3">Loading project...</p>
      </div>
    );
  }

  // Render error state if project not found
  if (!isLoading && !selectedProject) {
    return (
      <div className="project-empty-state">
        <div className="empty-state-icon">📋</div>
        <h3 className="empty-state-title">Project Not Found</h3>
        <p className="empty-state-message">
          The project you're trying to edit doesn't exist or you don't have permission to modify it.
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
            ← Back
          </Button>
          <h2>Edit Project: {selectedProject?.name}</h2>
        </div>
      </div>

      <div className="project-details-content">
        {/* Error Display */}
        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
        
        {/* Project Form */}
        {selectedProject && (
          <ProjectForm
            projectId={projectId}
            initialData={selectedProject}
            onSubmit={handleFormSubmit}
            onCancel={handleBackToProject}
          />
        )}
      </div>
    </div>
  );
};

export default ProjectEditForm; 