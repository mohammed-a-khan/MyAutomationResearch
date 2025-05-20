import React, { useState, useEffect } from 'react';
import { 
  Variable, 
  VariableType, 
  VariableScope,
  CreateVariablePayload,
  UpdateVariablePayload
} from '../../types/codebuilder';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';

interface VariableFormProps {
  projectId: string;
  initialVariable: Variable | null;
  onSave: () => void;
  isLoading: boolean;
}

/**
 * VariableForm component for creating and editing variables
 */
const VariableForm: React.FC<VariableFormProps> = ({
  projectId,
  initialVariable,
  onSave,
  isLoading
}) => {
  const { createVariable, updateVariable } = useCodeBuilder();
  
  const isNewVariable = !initialVariable;
  
  // Form state
  const [formData, setFormData] = useState<CreateVariablePayload>({
    name: '',
    value: '',
    type: VariableType.STRING,
    scope: VariableScope.PROJECT,
    projectId
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isSaving, setIsSaving] = useState(false);
  
  // Initialize form when initialVariable changes
  useEffect(() => {
    if (initialVariable) {
      setFormData({
        name: initialVariable.name,
        value: initialVariable.value,
        type: initialVariable.type,
        scope: initialVariable.scope,
        description: initialVariable.description,
        projectId: initialVariable.projectId,
        testId: initialVariable.testId,
        stepId: initialVariable.stepId
      });
    } else {
      // Reset form for new variable
      setFormData({
        name: '',
        value: '',
        type: VariableType.STRING,
        scope: VariableScope.PROJECT,
        projectId
      });
    }
    
    setErrors({});
  }, [initialVariable, projectId]);
  
  // Handle input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear errors when user types
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };
  
  // Handle type change
  const handleTypeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const type = e.target.value as VariableType;
    
    // Ensure value is correct format for the type
    let value = formData.value;
    if (type === VariableType.BOOLEAN) {
      value = value === 'true' ? 'true' : 'false';
    } else if (type === VariableType.NUMBER) {
      const num = parseFloat(value);
      if (isNaN(num)) {
        value = '0';
      }
    }
    
    setFormData(prev => ({
      ...prev,
      type,
      value
    }));
  };
  
  // Handle scope change
  const handleScopeChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const scope = e.target.value as VariableScope;
    
    // Reset related IDs when scope changes
    const updates: Partial<CreateVariablePayload> = { scope };
    
    // Set appropriate IDs based on scope
    if (scope === VariableScope.PROJECT) {
      updates.projectId = projectId;
      updates.testId = undefined;
      updates.stepId = undefined;
    } else if (scope === VariableScope.GLOBAL) {
      updates.projectId = undefined;
      updates.testId = undefined;
      updates.stepId = undefined;
    }
    
    // For TEST and STEP scopes, we'd need to have a test/step selector here
    // For now, keep the existing IDs if present
    
    setFormData(prev => ({
      ...prev,
      ...updates
    }));
  };
  
  // Validate form
  const validateForm = (): boolean => {
    const validationErrors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      validationErrors.name = 'Variable name is required';
    } else if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(formData.name)) {
      validationErrors.name = 'Variable name must start with a letter or underscore and contain only letters, numbers, and underscores';
    }
    
    if (formData.type === VariableType.NUMBER) {
      if (isNaN(parseFloat(formData.value))) {
        validationErrors.value = 'Value must be a valid number';
      }
    }
    
    // Required IDs for specific scopes
    if (formData.scope === VariableScope.PROJECT && !formData.projectId) {
      validationErrors.scope = 'Project ID is required for project-scoped variables';
    } else if (formData.scope === VariableScope.TEST && !formData.testId) {
      validationErrors.scope = 'Test ID is required for test-scoped variables';
    } else if (formData.scope === VariableScope.STEP && !formData.stepId) {
      validationErrors.scope = 'Step ID is required for step-scoped variables';
    }
    
    setErrors(validationErrors);
    return Object.keys(validationErrors).length === 0;
  };
  
  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }
    
    setIsSaving(true);
    
    try {
      if (isNewVariable) {
        // Create new variable
        await createVariable(formData);
      } else if (initialVariable) {
        // Update existing variable
        const updateData: UpdateVariablePayload = {
          name: formData.name,
          value: formData.value,
          type: formData.type,
          description: formData.description
        };
        
        await updateVariable(initialVariable.id, updateData);
      }
      
      onSave();
      
      // Reset form if creating new variable
      if (isNewVariable) {
        setFormData({
          name: '',
          value: '',
          type: VariableType.STRING,
          scope: VariableScope.PROJECT,
          projectId
        });
      }
    } catch (error) {
      // Error handling is done in context
    } finally {
      setIsSaving(false);
    }
  };
  
  // Returns placeholder example based on variable type
  const getValuePlaceholder = (type: VariableType): string => {
    switch (type) {
      case VariableType.STRING:
        return 'e.g. "Hello World"';
      case VariableType.NUMBER:
        return 'e.g. 42 or 3.14';
      case VariableType.BOOLEAN:
        return 'true or false';
      case VariableType.OBJECT:
        return 'e.g. {"key": "value"}';
      case VariableType.ARRAY:
        return 'e.g. [1, 2, 3]';
      default:
        return '';
    }
  };
  
  // Component is loading if parent is loading or component is saving
  const isFormLoading = isLoading || isSaving;
  
  return (
    <div className="variable-form">
      <div className="mb-4">
        <h4>{isNewVariable ? 'Create New Variable' : `Edit Variable: ${initialVariable?.name}`}</h4>
      </div>
      
      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label htmlFor="name" className="form-label">Variable Name *</label>
          <Input
            id="name"
            name="name"
            value={formData.name}
            onChange={handleInputChange}
            placeholder="Enter variable name (e.g. userName, itemCount)"
            error={errors.name}
            disabled={isFormLoading}
            required
          />
        </div>
        
        <div className="row mb-3">
          <div className="col-md-6">
            <label htmlFor="type" className="form-label">Type *</label>
            <Select
              id="type"
              name="type"
              value={formData.type}
              onChange={handleTypeChange}
              options={Object.values(VariableType).map(type => ({
                value: type,
                label: type.charAt(0) + type.slice(1).toLowerCase()
              }))}
              error={errors.type}
              disabled={isFormLoading}
              required
            />
          </div>
          
          <div className="col-md-6">
            <label htmlFor="scope" className="form-label">Scope *</label>
            <Select
              id="scope"
              name="scope"
              value={formData.scope}
              onChange={handleScopeChange}
              options={Object.values(VariableScope).map(scope => ({
                value: scope,
                label: scope.charAt(0) + scope.slice(1).toLowerCase()
              }))}
              error={errors.scope}
              disabled={isFormLoading || !isNewVariable} // Cannot change scope after creation
              required
            />
          </div>
        </div>
        
        <div className="mb-3">
          <label htmlFor="value" className="form-label">Value *</label>
          {formData.type === VariableType.BOOLEAN ? (
            <Select
              id="value"
              name="value"
              value={formData.value}
              onChange={handleInputChange}
              options={[
                { value: 'true', label: 'true' },
                { value: 'false', label: 'false' }
              ]}
              error={errors.value}
              disabled={isFormLoading}
              required
            />
          ) : (
            <Input
              id="value"
              name="value"
              type={formData.type === VariableType.NUMBER ? 'number' : 'text'}
              value={formData.value}
              onChange={handleInputChange}
              placeholder={getValuePlaceholder(formData.type)}
              error={errors.value}
              disabled={isFormLoading}
              required
            />
          )}
        </div>
        
        <div className="mb-4">
          <label htmlFor="description" className="form-label">Description (optional)</label>
          <Input
            id="description"
            name="description"
            value={formData.description || ''}
            onChange={handleInputChange}
            type="textarea"
            rows={2}
            placeholder="Enter a description for this variable"
            disabled={isFormLoading}
          />
        </div>
        
        <div className="d-flex justify-content-end">
          <Button
            type="submit"
            disabled={isFormLoading}
          >
            {isSaving 
              ? (isNewVariable ? 'Creating...' : 'Saving...') 
              : (isNewVariable ? 'Create Variable' : 'Save Changes')}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default VariableForm; 