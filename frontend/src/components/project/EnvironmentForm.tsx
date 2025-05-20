import React, { useState, FormEvent, useEffect } from 'react';
import { Environment, EnvironmentVariable } from '../../types/project';
import Input from '../common/Input';
import Button from '../common/Button';
import Checkbox from '../common/Checkbox';
import Card from '../common/Card';
import './Project.css';

interface EnvironmentFormProps {
  projectId: string;
  initialData?: Environment;
  onSubmit: (environment: Environment) => void;
  onCancel: () => void;
  isSaving?: boolean;
}

/**
 * EnvironmentForm component for creating or editing project environments
 */
const EnvironmentForm: React.FC<EnvironmentFormProps> = ({
                                                           projectId,
                                                           initialData,
                                                           onSubmit,
                                                           onCancel,
                                                           isSaving = false
                                                         }) => {
  // Default form values
  const defaultEnvironment: Omit<Environment, 'id'> = {
    name: '',
    url: '',
    description: '',
    isDefault: false,
    variables: []
  };

  // Form state
  const [formData, setFormData] = useState<Omit<Environment, 'id'>>(
      initialData || defaultEnvironment
  );

  const [variableName, setVariableName] = useState('');
  const [variableValue, setVariableValue] = useState('');
  const [variableIsSecret, setVariableIsSecret] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isEditing, setIsEditing] = useState<string | null>(null);

  // Update form data when initialData changes
  useEffect(() => {
    if (initialData) {
      setFormData(initialData);
    }
  }, [initialData]);

  // Handle input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value, type, checked } = e.target as HTMLInputElement;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));

    // Clear error for the field when user changes it
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: ''
      }));
    }
  };

  // Add variable to environment
  const addVariable = () => {
    if (variableName.trim()) {
      // Validate variable name
      if (formData.variables.some(v => v.name === variableName.trim() && v.id !== isEditing)) {
        setErrors(prev => ({
          ...prev,
          variable: 'Variable with this name already exists'
        }));
        return;
      }

      if (isEditing) {
        // Update existing variable
        setFormData(prev => ({
          ...prev,
          variables: prev.variables.map(v =>
              v.id === isEditing
                  ? { ...v, name: variableName, value: variableValue, isSecret: variableIsSecret }
                  : v
          )
        }));
      } else {
        // Add new variable
        setFormData(prev => ({
          ...prev,
          variables: [
            ...prev.variables,
            {
              id: `temp-${Date.now()}`, // Temporary ID, will be replaced by backend
              name: variableName,
              value: variableValue,
              isSecret: variableIsSecret
            }
          ]
        }));
      }

      // Reset variable form
      setVariableName('');
      setVariableValue('');
      setVariableIsSecret(false);
      setIsEditing(null);
      setErrors(prev => ({ ...prev, variable: '' }));
    }
  };

  // Remove variable from environment
  const removeVariable = (id: string) => {
    setFormData(prev => ({
      ...prev,
      variables: prev.variables.filter(v => v.id !== id)
    }));

    // If editing this variable, cancel edit
    if (isEditing === id) {
      setVariableName('');
      setVariableValue('');
      setVariableIsSecret(false);
      setIsEditing(null);
    }
  };

  // Edit existing variable
  const editVariable = (variable: EnvironmentVariable) => {
    setVariableName(variable.name);
    setVariableValue(variable.value);
    setVariableIsSecret(variable.isSecret);
    setIsEditing(variable.id);
  };

  // Cancel variable editing
  const cancelEditVariable = () => {
    setVariableName('');
    setVariableValue('');
    setVariableIsSecret(false);
    setIsEditing(null);
    setErrors(prev => ({ ...prev, variable: '' }));
  };

  // Handle form submission
  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // Validate form
    const validationErrors: Record<string, string> = {};

    if (!formData.name.trim()) {
      validationErrors.name = 'Environment name is required';
    }

    if (!formData.url.trim()) {
      validationErrors.url = 'URL is required';
    } else if (!isValidUrl(formData.url)) {
      validationErrors.url = 'Please enter a valid URL';
    }

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    // Submit data with ID if editing
    const submissionData: Environment = {
      ...formData,
      id: initialData?.id || `temp-${Date.now()}` // Backend will assign real ID for new environments
    };

    onSubmit(submissionData);
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
        {/* Basic Information */}
        <div className="form-section">
          <h3 className="form-section-title">Environment Details</h3>

          <div className="form-group mb-3">
            <label htmlFor="name" className="form-label">Environment Name *</label>
            <Input
                id="name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                placeholder="e.g. Development, Staging, Production"
                error={errors.name}
                required
            />
          </div>

          <div className="form-group mb-3">
            <label htmlFor="url" className="form-label">Base URL *</label>
            <Input
                id="url"
                name="url"
                value={formData.url}
                onChange={handleInputChange}
                placeholder="https://example.com"
                error={errors.url}
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
                placeholder="Environment description"
                type="textarea"
                rows={2}
            />
          </div>

          <div className="form-group mb-3">
            <Checkbox
                id="isDefault"
                name="isDefault"
                label="Set as Default Environment"
                checked={formData.isDefault}
                onChange={handleInputChange}
            />
            <div className="form-help-text">
              This environment will be used as the default for test execution
            </div>
          </div>
        </div>

        {/* Environment Variables */}
        <div className="form-section">
          <h3 className="form-section-title">Environment Variables</h3>

          <div className="mb-4">
            <Card>
              <Card.Body>
                <div className="form-row mb-2">
                  <div className="form-group flex-grow-1 mb-2">
                    <label htmlFor="variableName" className="form-label">Name</label>
                    <Input
                        id="variableName"
                        value={variableName}
                        onChange={(e) => setVariableName(e.target.value)}
                        placeholder="Variable name"
                        error={errors.variable}
                    />
                  </div>
                  <div className="form-group flex-grow-1 mb-2">
                    <label htmlFor="variableValue" className="form-label">Value</label>
                    <Input
                        id="variableValue"
                        value={variableValue}
                        onChange={(e) => setVariableValue(e.target.value)}
                        placeholder="Variable value"
                        type={variableIsSecret ? 'password' : 'text'}
                    />
                  </div>
                </div>

                <div className="mb-3">
                  <Checkbox
                      id="variableIsSecret"
                      checked={variableIsSecret}
                      onChange={(e) => setVariableIsSecret(e.target.checked)}
                      label="Secret variable (will be masked in logs)"
                  />
                </div>

                <div className="d-flex gap-2">
                  <Button
                      type="button"
                      onClick={addVariable}
                  >
                    {isEditing ? 'Update Variable' : 'Add Variable'}
                  </Button>
                  {isEditing && (
                      <Button
                          type="button"
                          variant="secondary"
                          onClick={cancelEditVariable}
                      >
                        Cancel
                      </Button>
                  )}
                </div>
              </Card.Body>
            </Card>
          </div>

          {/* Variable List */}
          {formData.variables.length > 0 ? (
              <div className="table-responsive">
                <table className="table">
                  <thead>
                  <tr>
                    <th>Name</th>
                    <th>Value</th>
                    <th>Secret</th>
                    <th>Actions</th>
                  </tr>
                  </thead>
                  <tbody>
                  {formData.variables.map((variable) => (
                      <tr key={variable.id}>
                        <td>{variable.name}</td>
                        <td>{variable.isSecret ? '••••••••' : variable.value}</td>
                        <td>{variable.isSecret ? 'Yes' : 'No'}</td>
                        <td>
                          <div className="d-flex gap-2">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => editVariable(variable)}
                            >
                              Edit
                            </Button>
                            <Button
                                variant="danger"
                                size="sm"
                                onClick={() => removeVariable(variable.id)}
                            >
                              Remove
                            </Button>
                          </div>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </div>
          ) : (
              <div className="text-center p-4">
                <p>No variables defined for this environment.</p>
                <p className="text-muted">Add variables to store configuration values like API keys or endpoints.</p>
              </div>
          )}
        </div>

        {/* Form Actions */}
        <div className="form-actions">
          <Button
              type="button"
              variant="secondary"
              onClick={onCancel}
              disabled={isSaving}
          >
            Cancel
          </Button>
          <Button
              type="submit"
              disabled={isSaving}
          >
            {isSaving ? 'Saving...' : initialData ? 'Update Environment' : 'Create Environment'}
          </Button>
        </div>
      </form>
  );
};

export default EnvironmentForm;