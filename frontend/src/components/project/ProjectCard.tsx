import React, { useState } from 'react';
import { Project, ProjectStatus } from '../../types/project';
import Button from '../common/Button';
import { useProjects } from '../../context/ProjectContext';
import './Project.css';

interface ProjectCardProps {
  project: Project;
  onView: () => void;
}

/**
 * ProjectCard displays a single project in a card format
 */
const ProjectCard: React.FC<ProjectCardProps> = ({ project, onView }) => {
  const [showActions, setShowActions] = useState(false);
  const { deleteProject, isLoading } = useProjects();
  const [confirmDelete, setConfirmDelete] = useState(false);

  // Format date for display
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    }).format(date);
  };

  // Get status badge class based on project status
  const getStatusBadgeClass = (status: ProjectStatus) => {
    switch (status) {
      case ProjectStatus.ACTIVE:
        return 'badge-active';
      case ProjectStatus.INACTIVE:
        return 'badge-inactive';
      case ProjectStatus.ARCHIVED:
        return 'badge-archived';
      case ProjectStatus.DRAFT:
        return 'badge-draft';
      default:
        return '';
    }
  };

  // Format percentage for display
  const formatPercentage = (value?: number) => {
    if (value === undefined || value === null) {
      return 'N/A';
    }
    return `${Math.round(value)}%`;
  };

  // Handle project deletion
  const handleDelete = async () => {
    if (confirmDelete) {
      try {
        await deleteProject(project.id);
        // No need to navigate, parent component will refresh
      } catch (error) {
        // Error is handled in context
        setConfirmDelete(false);
      }
    } else {
      setConfirmDelete(true);
    }
  };

  // Cancel delete confirmation
  const cancelDelete = () => {
    setConfirmDelete(false);
  };

  // Toggle the actions menu
  const toggleActions = () => {
    setShowActions(!showActions);
  };

  // Go to edit project page
  const goToEditProject = () => {
    window.location.href = `/projects/${project.id}/edit`;
  };

  // Go to settings page
  const goToSettings = () => {
    window.location.href = `/projects/${project.id}/config`;
  };

  // Go to recorder page
  const goToRecorder = () => {
    window.location.href = `/recorder/${project.id}`;
  };

  return (
      <div className="project-card">
        {/* Card Header */}
        <div className="project-card-header">
          <h3 className="project-card-title">{project.name}</h3>
          <span className={`project-card-badge ${getStatusBadgeClass(project.status)}`}>
          {project.status}
        </span>
        </div>

        {/* Card Content */}
        <div className="project-card-content">
          <div className="project-description">
            {project.description || 'No description provided'}
          </div>

          <div className="project-meta">
            <div>Type: {project.type}</div>
            <div>Created: {formatDate(project.createdAt)}</div>
          </div>

          {project.tags && project.tags.length > 0 && (
              <div className="project-tags">
                {project.tags.map((tag, index) => (
                    <span key={index} className="project-tag">{tag}</span>
                ))}
              </div>
          )}
        </div>

        {/* Card Footer */}
        <div className="project-card-footer">
          <div className="project-metrics">
            <div className="project-metric">
              <span className="metric-value">{project.testCount || 0}</span>
              <span className="metric-label">Tests</span>
            </div>
            <div className="project-metric">
              <span className="metric-value">{formatPercentage(project.successRate)}</span>
              <span className="metric-label">Success</span>
            </div>
          </div>

          <div className="project-actions">
            {confirmDelete ? (
                <div className="d-flex gap-2">
                  <Button
                      size="sm"
                      variant="danger"
                      onClick={handleDelete}
                      disabled={isLoading}
                  >
                    Confirm
                  </Button>
                  <Button
                      size="sm"
                      variant="secondary"
                      onClick={cancelDelete}
                  >
                    Cancel
                  </Button>
                </div>
            ) : showActions ? (
                <div className="d-flex gap-2 flex-wrap">
                  <Button
                      size="sm"
                      onClick={onView}
                  >
                    View
                  </Button>
                  <Button
                      size="sm"
                      onClick={goToRecorder}
                  >
                    <i className="bi bi-record-circle me-1"></i>
                    Record
                  </Button>
                  <Button
                      size="sm"
                      onClick={goToEditProject}
                  >
                    Edit
                  </Button>
                  <Button
                      size="sm"
                      onClick={goToSettings}
                  >
                    Settings
                  </Button>
                  <Button
                      size="sm"
                      variant="danger"
                      onClick={handleDelete}
                  >
                    Delete
                  </Button>
                  <Button
                      size="sm"
                      variant="secondary"
                      onClick={toggleActions}
                  >
                    Close
                  </Button>
                </div>
            ) : (
                <div className="d-flex gap-2">
                  <Button
                      size="sm"
                      onClick={onView}
                  >
                    View
                  </Button>
                  <Button
                      size="sm"
                      variant="outline"
                      onClick={toggleActions}
                  >
                    •••
                  </Button>
                </div>
            )}
          </div>
        </div>
      </div>
  );
};

export default ProjectCard;