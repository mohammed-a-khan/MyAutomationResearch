import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useProjects } from '../../context/ProjectContext';
import { ProjectStatus, ProjectType, ProjectFilters } from '../../types/project';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';
import Modal from '../common/Modal';
import ProjectCard from './ProjectCard';
import ProjectForm from './ProjectForm';
import './Project.css';

/**
 * ProjectList component displays a grid of projects with filtering options
 */
const ProjectList: React.FC = () => {
  const navigate = useNavigate();
  const {
    projects,
    loadProjects,
    loadProjectTags,
    projectTags,
    pagination,
    filters,
    setFilters,
    isLoading,
    error,
    setPage,
  } = useProjects();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [searchInput, setSearchInput] = useState('');

  // Load projects and tags on component mount
  useEffect(() => {
    const initializeData = async () => {
      try {
        await loadProjects();
        await loadProjectTags();
      } catch (error) {
        console.error("Error loading initial data:", error);
      }
    };

    initializeData();
  }, [loadProjects, loadProjectTags]);

  // Update projects when filters change
  useEffect(() => {
    if (filters) { // Only run if filters is defined
      loadProjects();
    }
  }, [filters, pagination?.page, loadProjects]);

  // Handle search input changes
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchInput(e.target.value);
  };

  // Submit search filter
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFilters({ ...filters, search: searchInput });
    setPage(1); // Reset to first page when searching
  };

  // Handle filter changes
  const handleFilterChange = (name: keyof ProjectFilters, value: any) => {
    setFilters({ ...filters, [name]: value });
    setPage(1); // Reset to first page when filters change
  };

  // Create status filter options
  const statusOptions = [
    { value: '', label: 'All Statuses' },
    ...Object.values(ProjectStatus).map(status => ({
      value: status,
      label: status.charAt(0) + status.slice(1).toLowerCase(),
    })),
  ];

  // Create type filter options
  const typeOptions = [
    { value: '', label: 'All Types' },
    ...Object.values(ProjectType).map(type => ({
      value: type,
      label: type.charAt(0) + type.slice(1).toLowerCase(),
    })),
  ];

  // Create sort options
  const sortOptions = [
    { value: 'name', label: 'Name' },
    { value: 'createdAt', label: 'Creation Date' },
    { value: 'updatedAt', label: 'Last Updated' },
    { value: 'lastRunDate', label: 'Last Run' },
    { value: 'successRate', label: 'Success Rate' },
  ];

  // Create sort direction options
  const sortDirectionOptions = [
    { value: 'asc', label: 'Ascending' },
    { value: 'desc', label: 'Descending' },
  ];

  // Handle page change
  const handlePageChange = (newPage: number) => {
    setPage(newPage);
  };

  // View project details
  const handleViewProject = (projectId: string) => {
    navigate(`/projects/${projectId}`);
  };

  // Calculate pagination
  const totalPages = pagination?.totalPages || 1;
  const currentPage = pagination?.page || 1;
  const paginationItems = [];

  // Create pagination items
  for (let i = 1; i <= totalPages; i++) {
    if (
        i === 1 || // Always show first page
        i === totalPages || // Always show last page
        (i >= currentPage - 1 && i <= currentPage + 1) // Show current page and neighbors
    ) {
      paginationItems.push(
          <Button
              key={i}
              variant={i === currentPage ? 'primary' : 'outline'}
              size="sm"
              onClick={() => handlePageChange(i)}
          >
            {i}
          </Button>
      );
    } else if (
        (i === 2 && currentPage > 3) ||
        (i === totalPages - 1 && currentPage < totalPages - 2)
    ) {
      // Add ellipsis placeholder
      paginationItems.push(
          <span key={i} className="pagination-ellipsis">...</span>
      );
    }
  }

  return (
      <div className="project-container">
        <div className="project-header">
          <h2 className="project-title">Projects</h2>
          <div className="project-controls">
            <Button
                onClick={() => setShowCreateModal(true)}
            >
              Create Project
            </Button>
          </div>
        </div>

        <div className="project-content">
          {/* Filters */}
          <div className="project-filters">
            <form onSubmit={handleSearchSubmit} className="filter-row mb-3">
              <div className="filter-item flex-grow-1">
                <Input
                    placeholder="Search projects..."
                    value={searchInput}
                    onChange={handleSearchChange}
                />
              </div>
              <Button type="submit">Search</Button>
            </form>

            <div className="filter-row">
              <div className="filter-item">
                <Select
                    name="status"
                    label="Status"
                    value={filters?.status?.[0] || ''}
                    onChange={(e) => handleFilterChange('status', e.target.value ? [e.target.value as ProjectStatus] : undefined)}
                    options={statusOptions}
                />
              </div>
              <div className="filter-item">
                <Select
                    name="type"
                    label="Type"
                    value={filters?.types?.[0] || ''}
                    onChange={(e) => handleFilterChange('types', e.target.value ? [e.target.value as ProjectType] : undefined)}
                    options={typeOptions}
                />
              </div>
              <div className="filter-item">
                <Select
                    name="sortBy"
                    label="Sort By"
                    value={filters?.sortBy || 'name'}
                    onChange={(e) => handleFilterChange('sortBy', e.target.value)}
                    options={sortOptions}
                />
              </div>
              <div className="filter-item">
                <Select
                    name="sortDirection"
                    label="Order"
                    value={filters?.sortDirection || 'asc'}
                    onChange={(e) => handleFilterChange('sortDirection', e.target.value)}
                    options={sortDirectionOptions}
                />
              </div>
            </div>
          </div>

          {/* Error Display */}
          {error && (
              <div className="alert alert-danger" role="alert">
                {error}
              </div>
          )}

          {/* Loading State */}
          {isLoading ? (
              <div className="text-center p-5">
                <div className="spinner-border text-primary" role="status">
                  <span className="visually-hidden">Loading...</span>
                </div>
                <p className="mt-3">Loading projects...</p>
              </div>
          ) : !projects || projects.length === 0 ? (
              // Empty State
              <div className="project-empty-state">
                <div className="empty-state-icon">📁</div>
                <h3 className="empty-state-title">No Projects Found</h3>
                <p className="empty-state-message">
                  {(filters?.search || filters?.status || filters?.types)
                      ? "No projects match your current filters. Try adjusting your search criteria."
                      : "Start by creating your first project using the 'Create Project' button."}
                </p>
                <Button onClick={() => setShowCreateModal(true)}>Create Project</Button>
              </div>
          ) : (
              // Project Grid
              <div className="project-grid">
                {projects.map(project => (
                    <ProjectCard
                        key={project.id}
                        project={project}
                        onView={() => handleViewProject(project.id)}
                    />
                ))}
              </div>
          )}

          {/* Pagination */}
          {!isLoading && projects && projects.length > 0 && totalPages > 1 && (
              <div className="d-flex justify-content-center gap-2 mt-3">
                <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                >
                  Previous
                </Button>

                {paginationItems}

                <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage === totalPages}
                >
                  Next
                </Button>
              </div>
          )}
        </div>

        {/* Create Project Modal */}
        <Modal
            isOpen={showCreateModal}
            onClose={() => setShowCreateModal(false)}
            title="Create New Project"
            size="lg"
        >
          <ProjectForm
              onSubmit={() => {
                setShowCreateModal(false);
                loadProjects(); // Refresh the project list
              }}
              onCancel={() => setShowCreateModal(false)}
          />
        </Modal>
      </div>
  );
};

export default ProjectList;