import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useProjects } from '../../context/ProjectContext';
import { ProjectStatus, TestSuite, Environment } from '../../types/project';
import Button from '../common/Button';
import Modal from '../common/Modal';
import Tabs from '../common/Tabs';
import Card from '../common/Card';
import './Project.css';

/**
 * ProjectDetail component displays detailed information about a project
 */
const ProjectDetail: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const {
    selectedProject,
    loadProject,
    updateProject,
    deleteProject,
    isLoading,
    error,
  } = useProjects();

  const [activeTab, setActiveTab] = useState('overview');
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  // Load project on component mount or when project ID changes
  useEffect(() => {
    if (projectId) {
      loadProject(projectId);
    }
  }, [projectId, loadProject]);

  // Handle navigation back to projects list
  const handleBackToProjects = () => {
    navigate('/projects');
  };

  // Handle edit project navigation
  const handleEditProject = () => {
    navigate(`/projects/${projectId}/edit`);
  };

  // Handle navigating to recorder
  const handleRecordTests = () => {
    navigate(`/recorder/${projectId}`);
  };

  // Handle project deletion
  const handleDeleteProject = async () => {
    if (!projectId) return;

    try {
      setIsDeleting(true);
      await deleteProject(projectId);
      navigate('/projects');
    } catch (error) {
      // Error will be handled by context
    } finally {
      setIsDeleting(false);
      setShowDeleteModal(false);
    }
  };

  // Format date for display
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
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

  // Render empty state when no project is loaded
  if (!isLoading && !selectedProject) {
    return (
        <div className="project-empty-state">
          <div className="empty-state-icon">📋</div>
          <h3 className="empty-state-title">Project Not Found</h3>
          <p className="empty-state-message">
            The project you're looking for doesn't exist or you don't have permission to view it.
          </p>
          <Button onClick={handleBackToProjects}>Back to Projects</Button>
        </div>
    );
  }

  // Render loading state
  if (isLoading) {
    return (
        <div className="text-center p-5">
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading project details...</p>
        </div>
    );
  }

  return (
      <div className="project-details">
        {/* Project Header */}
        <div className="project-details-header">
          <div className="project-details-title">
            <Button
                variant="outline"
                size="sm"
                onClick={handleBackToProjects}
            >
              ← Back
            </Button>
            <h2>{selectedProject?.name}</h2>
            <span className={`project-card-badge ${getStatusBadgeClass(selectedProject?.status!)}`}>
            {selectedProject?.status}
          </span>
          </div>
          <div className="d-flex gap-2">
            <Button
                variant="outline"
                onClick={handleRecordTests}
            >
              <i className="bi bi-record-circle me-1"></i>
              Record Tests
            </Button>
            <Button
                variant="outline"
                onClick={handleEditProject}
            >
              Edit Project
            </Button>
            <Button
                variant="danger"
                onClick={() => setShowDeleteModal(true)}
            >
              Delete
            </Button>
          </div>
        </div>

        {/* Project Content */}
        <div className="project-details-content">
          {/* Error Display */}
          {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
          )}

          {/* Tabs Navigation */}
          <div className="project-details-tabs">
            <Tabs
                activeTab={activeTab}
                onChange={setActiveTab}
                tabs={[
                  { id: 'overview', label: 'Overview' },
                  { id: 'suites', label: 'Test Suites' },
                  { id: 'environments', label: 'Environments' },
                  { id: 'settings', label: 'Settings' },
                ]}
            />
          </div>

          {/* Tab Content */}
          <div className="tab-content">
            {/* Overview Tab */}
            {activeTab === 'overview' && selectedProject && (
                <>
                  <div className="project-details-section">
                    <h3 className="project-details-section-title">Project Information</h3>
                    <div className="project-info-grid">
                      <div className="project-info-item">
                        <span className="info-label">Project ID</span>
                        <span className="info-value">{selectedProject.id}</span>
                      </div>
                      <div className="project-info-item">
                        <span className="info-label">Type</span>
                        <span className="info-value">{selectedProject.type}</span>
                      </div>
                      <div className="project-info-item">
                        <span className="info-label">Created By</span>
                        <span className="info-value">{selectedProject.createdBy}</span>
                      </div>
                      <div className="project-info-item">
                        <span className="info-label">Created At</span>
                        <span className="info-value">{formatDate(selectedProject.createdAt)}</span>
                      </div>
                      <div className="project-info-item">
                        <span className="info-label">Last Updated</span>
                        <span className="info-value">{formatDate(selectedProject.updatedAt)}</span>
                      </div>
                      {selectedProject.lastRunDate && (
                          <div className="project-info-item">
                            <span className="info-label">Last Test Run</span>
                            <span className="info-value">{formatDate(selectedProject.lastRunDate)}</span>
                          </div>
                      )}
                    </div>
                  </div>

                  <div className="project-details-section">
                    <h3 className="project-details-section-title">Description</h3>
                    <p>{selectedProject.description || 'No description provided.'}</p>
                  </div>

                  {selectedProject.tags && selectedProject.tags.length > 0 && (
                      <div className="project-details-section">
                        <h3 className="project-details-section-title">Tags</h3>
                        <div className="project-tags">
                          {selectedProject.tags.map((tag, index) => (
                              <span key={index} className="project-tag">{tag}</span>
                          ))}
                        </div>
                      </div>
                  )}

                  <div className="project-details-section">
                    <h3 className="project-details-section-title">Test Actions</h3>
                    <div className="row g-3">
                      <div className="col-md-4">
                        <Card>
                          <Card.Body>
                            <h5><i className="bi bi-record-circle me-2"></i>Record Tests</h5>
                            <p>Record user interactions to automatically generate test scripts.</p>
                            <Button
                                variant="primary"
                                onClick={handleRecordTests}
                            >
                              Start Recording
                            </Button>
                          </Card.Body>
                        </Card>
                      </div>
                      <div className="col-md-4">
                        <Card>
                          <Card.Body>
                            <h5><i className="bi bi-lightning me-2"></i>Run Tests</h5>
                            <p>Execute tests for this project in various environments.</p>
                            <Button
                                variant="primary"
                                onClick={() => navigate(`/runner/${projectId}`)}
                            >
                              Run Tests
                            </Button>
                          </Card.Body>
                        </Card>
                      </div>
                      <div className="col-md-4">
                        <Card>
                          <Card.Body>
                            <h5><i className="bi bi-code-slash me-2"></i>Code Builder</h5>
                            <p>Generate or customize test code for specific scenarios.</p>
                            <Button
                                variant="primary"
                                onClick={() => navigate(`/builder/${projectId}`)}
                            >
                              Open Builder
                            </Button>
                          </Card.Body>
                        </Card>
                      </div>
                    </div>
                  </div>
                </>
            )}

            {/* Test Suites Tab */}
            {activeTab === 'suites' && selectedProject && (
                <div className="project-details-section">
                  <h3 className="project-details-section-title">
                    Test Suites
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/projects/${projectId}/test-suites/create`)}
                    >
                      Create Test Suite
                    </Button>
                  </h3>

                  {selectedProject.testSuites && selectedProject.testSuites.length > 0 ? (
                      <div className="row g-3">
                        {selectedProject.testSuites.map((suite: TestSuite) => (
                            <div key={suite.id} className="col-md-6 col-lg-4">
                              <Card>
                                <Card.Header>
                                  <h4 className="m-0">{suite.name}</h4>
                                </Card.Header>
                                <Card.Body>
                                  <p>{suite.description || 'No description provided.'}</p>
                                  <div className="d-flex justify-content-between">
                                    <span>Tests: <b>{suite.testCount}</b></span>
                                    {suite.lastRunDate && (
                                        <span>Success rate: <b>{suite.successRate}%</b></span>
                                    )}
                                  </div>
                                  {suite.lastRunDate && (
                                      <div className="text-muted mt-2">
                                        Last run: {new Date(suite.lastRunDate).toLocaleDateString()}
                                      </div>
                                  )}
                                </Card.Body>
                                <Card.Footer className="d-flex justify-content-end">
                                  <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={() => navigate(`/projects/${projectId}/test-suites/${suite.id}`)}
                                  >
                                    View Tests
                                  </Button>
                                </Card.Footer>
                              </Card>
                            </div>
                        ))}
                      </div>
                  ) : (
                      <div className="text-center p-4">
                        <p>No test suites found for this project.</p>
                        <Button
                            onClick={() => navigate(`/projects/${projectId}/test-suites/create`)}
                        >
                          Create First Test Suite
                        </Button>
                      </div>
                  )}
                </div>
            )}

            {/* Environments Tab */}
            {activeTab === 'environments' && selectedProject && (
                <div className="project-details-section">
                  <h3 className="project-details-section-title">
                    Environments
                    <Button
                        variant="outline"
                        size="sm"
                        onClick={() => navigate(`/projects/${projectId}/environments/create`)}
                    >
                      Add Environment
                    </Button>
                  </h3>

                  {selectedProject.environments && selectedProject.environments.length > 0 ? (
                      <div className="row g-3">
                        {selectedProject.environments.map((env: Environment) => (
                            <div key={env.id} className="col-md-6">
                              <Card>
                                <Card.Header className="d-flex justify-content-between align-items-center">
                                  <h4 className="m-0">{env.name}</h4>
                                  {env.isDefault && (
                                      <span className="badge badge-primary">Default</span>
                                  )}
                                </Card.Header>
                                <Card.Body>
                                  <div className="mb-2">
                                    <strong>URL:</strong> {env.url}
                                  </div>
                                  {env.description && (
                                      <p>{env.description}</p>
                                  )}
                                  <div>
                                    <strong>Variables:</strong> {env.variables.length}
                                  </div>
                                </Card.Body>
                                <Card.Footer className="d-flex justify-content-end">
                                  <Button
                                      variant="outline"
                                      size="sm"
                                      onClick={() => navigate(`/projects/${projectId}/environments/${env.id}`)}
                                  >
                                    Edit Environment
                                  </Button>
                                </Card.Footer>
                              </Card>
                            </div>
                        ))}
                      </div>
                  ) : (
                      <div className="text-center p-4">
                        <p>No environments configured for this project.</p>
                        <Button
                            onClick={() => navigate(`/projects/${projectId}/environments/create`)}
                        >
                          Add First Environment
                        </Button>
                      </div>
                  )}
                </div>
            )}

            {/* Settings Tab */}
            {activeTab === 'settings' && selectedProject && (
                <div className="project-details-section">
                  <h3 className="project-details-section-title">Project Settings</h3>
                  <Button
                      onClick={() => navigate(`/projects/${projectId}/settings`)}
                  >
                    Manage Project Settings
                  </Button>
                </div>
            )}
          </div>
        </div>

        {/* Delete Confirmation Modal */}
        <Modal
            isOpen={showDeleteModal}
            onClose={() => setShowDeleteModal(false)}
            title="Confirm Deletion"
        >
          <p>Are you sure you want to delete this project? This action cannot be undone.</p>
          <div className="d-flex justify-content-end gap-2 mt-4">
            <Button
                variant="secondary"
                onClick={() => setShowDeleteModal(false)}
                disabled={isDeleting}
            >
              Cancel
            </Button>
            <Button
                variant="danger"
                onClick={handleDeleteProject}
                disabled={isDeleting}
            >
              {isDeleting ? 'Deleting...' : 'Delete Project'}
            </Button>
          </div>
        </Modal>
      </div>
  );
};

export default ProjectDetail;