import React, { useState, FormEvent, useEffect } from 'react';
import { useProjects } from '../../context/ProjectContext';
import { 
  CreateProjectPayload, 
  ProjectType, 
  Project,
  UpdateProjectPayload
} from '../../types/project';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import './Project.css';

interface ProjectFormProps {
  projectId?: string;
  initialData?: Project;
  onSubmit: (project: Project) => void;
  onCancel: () => void;
}

/**
 * ProjectForm component for creating or editing projects
 */
const ProjectForm: React.FC<ProjectFormProps> = ({ 
  projectId, 
  initialData, 
  onSubmit, 
  onCancel 
}) => {
  // Default form values
  const defaultFormData: CreateProjectPayload = {
    name: '',
    description: '',
    type: ProjectType.WEB,
    baseUrl: '',
    repositoryUrl: '',
    tags: []
  };

  // Form state
  const [formData, setFormData] = useState<CreateProjectPayload | UpdateProjectPayload>(
    initialData 
      ? {
          name: initialData.name,
          description: initialData.description || '',
          type: initialData.type,
          baseUrl: (initialData as any).baseUrl || '',
          repositoryUrl: (initialData as any).repositoryUrl || '',
          tags: initialData.tags || []
        }
      : defaultFormData
  );
  
  const [tagInput, setTagInput] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  const { createProject, updateProject, projectTags, loadProjectTags, isLoading, error } = useProjects();

  // Load project tags on component mount
  useEffect(() => {
    loadProjectTags();
  }, [loadProjectTags]);

  // Handle input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error for the field when user changes it
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  // Add tag to form data
  const addTag = () => {
    if (tagInput.trim()) {
      const tag = tagInput.trim();
      if (!formData.tags?.includes(tag)) {
        setFormData(prev => ({
          ...prev,
          tags: [...(prev.tags || []), tag]
        }));
      }
      setTagInput('');
    }
  };

  // Remove tag from form data
  const removeTag = (tagToRemove: string) => {
    setFormData(prev => ({
      ...prev,
      tags: prev.tags?.filter(tag => tag !== tagToRemove) || []
    }));
  };

  // Handle tag input keydown (add on Enter)
  const handleTagKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      addTag();
    }
  };

  // Handle form submission
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    
    // Validate form
    const validationErrors: Record<string, string> = {};
    
    if (!formData.name || !formData.name.trim()) {
      validationErrors.name = 'Project name is required';
    }
    
    if (!formData.type) {
      validationErrors.type = 'Project type is required';
    }
    
    if (formData.baseUrl && !isValidUrl(formData.baseUrl)) {
      validationErrors.baseUrl = 'Please enter a valid URL';
    }
    
    if (formData.repositoryUrl && !isValidUrl(formData.repositoryUrl)) {
      validationErrors.repositoryUrl = 'Please enter a valid URL';
    }
    
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    
    try {
      if (projectId && initialData) {
        // Update existing project
        const updatedProject = await updateProject(projectId, formData as UpdateProjectPayload);
        onSubmit(updatedProject);
      } else {
        // Create new project
        const newProject = await createProject(formData as CreateProjectPayload);
        onSubmit(newProject);
      }
    } catch (err) {
      // Error handling is done in context, no need to handle here
    }
  };

  // Check if URL is valid
  const isValidUrl = (urlString: string): boolean => {
    try {
      new URL(urlString);
      return true;
    } catch {
      return false;
    }
  };

  return (
    <form onSubmit={handleSubmit} className="project-form">
      {/* Basic Information Section */}
      <div className="form-section">
        <h3 className="form-section-title">Basic Information</h3>
        
        <div className="form-group mb-3">
          <label htmlFor="name" className="form-label">Project Name *</label>
          <Input
            id="name"
            name="name"
            value={formData.name}
            onChange={handleInputChange}
            placeholder="Enter project name"
            error={errors.name}
            required
          />
        </div>
        
        <div className="form-group mb-3">
          <label htmlFor="type" className="form-label">Project Type *</label>
          <Select
            id="type"
            name="type"
            value={formData.type}
            onChange={handleInputChange}
            options={Object.values(ProjectType).map(type => ({
              value: type,
              label: type.charAt(0) + type.slice(1).toLowerCase()
            }))}
            error={errors.type}
            required
          />
        </div>
        
        <div className="form-group mb-3">
          <label htmlFor="description" className="form-label">Description</label>
          <Input
            id="description"
            name="description"
            value={formData.description || ''}
            onChange={handleInputChange}
            placeholder="Enter project description"
            type="textarea"
            rows={3}
          />
        </div>
      </div>
      
      {/* Configuration Section */}
      <div className="form-section">
        <h3 className="form-section-title">Configuration</h3>
        
        <div className="form-group mb-3">
          <label htmlFor="baseUrl" className="form-label">Base URL</label>
          <Input
            id="baseUrl"
            name="baseUrl"
            value={formData.baseUrl || ''}
            onChange={handleInputChange}
            placeholder="https://example.com"
            error={errors.baseUrl}
          />
          <div className="form-help-text">
            The base URL for your application under test
          </div>
        </div>
        
        <div className="form-group mb-3">
          <label htmlFor="repositoryUrl" className="form-label">Repository URL</label>
          <Input
            id="repositoryUrl"
            name="repositoryUrl"
            value={formData.repositoryUrl || ''}
            onChange={handleInputChange}
            placeholder="https://github.com/username/repo"
            error={errors.repositoryUrl}
          />
          <div className="form-help-text">
            Link to your source code repository
          </div>
        </div>
      </div>
      
      {/* Tags Section */}
      <div className="form-section">
        <h3 className="form-section-title">Tags</h3>
        
        <div className="form-group mb-3">
          <label htmlFor="tags" className="form-label">Project Tags</label>
          <div className="d-flex mb-2">
            <Input
              id="tagInput"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={handleTagKeyDown}
              placeholder="Add tag and press Enter"
            />
            <Button 
              type="button" 
              onClick={addTag}
              className="ms-2"
            >
              Add
            </Button>
          </div>
          
          {/* Tag suggestions */}
          {projectTags.length > 0 && tagInput && (
            <div className="mb-2">
              <small>Suggestions: </small>
              <div className="d-flex flex-wrap gap-1">
                {projectTags
                  .filter(tag => 
                    tag.toLowerCase().includes(tagInput.toLowerCase()) && 
                    !formData.tags?.includes(tag)
                  )
                  .slice(0, 5)
                  .map(tag => (
                    <Button
                      key={tag}
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setFormData(prev => ({
                          ...prev,
                          tags: [...(prev.tags || []), tag]
                        }));
                        setTagInput('');
                      }}
                    >
                      {tag}
                    </Button>
                  ))}
              </div>
            </div>
          )}
          
          {/* Display selected tags */}
          <div className="project-tags mt-2">
            {formData.tags?.map((tag, index) => (
              <div key={index} className="project-tag d-inline-flex align-items-center">
                {tag}
                <button
                  type="button"
                  className="btn-close ms-1"
                  style={{ fontSize: '0.5rem' }}
                  onClick={() => removeTag(tag)}
                  aria-label="Remove tag"
                ></button>
              </div>
            ))}
          </div>
        </div>
      </div>
      
      {/* Server-side error display */}
      {error && (
        <div className="alert alert-danger mt-3" role="alert">
          {error}
        </div>
      )}
      
      {/* Form Actions */}
      <div className="form-actions">
        <Button 
          type="button" 
          variant="secondary" 
          onClick={onCancel} 
          disabled={isLoading}
        >
          Cancel
        </Button>
        <Button 
          type="submit" 
          disabled={isLoading}
        >
          {isLoading ? 'Saving...' : projectId ? 'Update Project' : 'Create Project'}
        </Button>
      </div>
    </form>
  );
};

export default ProjectForm;