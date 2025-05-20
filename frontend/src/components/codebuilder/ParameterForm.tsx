import React, { useState } from 'react';
import { StepParameter } from '../../types/codebuilder';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

interface ParameterFormProps {
  parameters: StepParameter[];
  onChange: (parameters: StepParameter[]) => void;
  disabled?: boolean;
}

/**
 * ParameterForm component for managing step parameters
 */
const ParameterForm: React.FC<ParameterFormProps> = ({ 
  parameters, 
  onChange,
  disabled = false
}) => {
  const [editingParameter, setEditingParameter] = useState<Partial<StepParameter>>({
    name: '',
    value: '',
    type: 'string'
  });
  
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  // Parameter types options
  const parameterTypes = [
    { value: 'string', label: 'String' },
    { value: 'number', label: 'Number' },
    { value: 'boolean', label: 'Boolean' },
    { value: 'variable', label: 'Variable' },
    { value: 'locator', label: 'Locator' }
  ];
  
  // Handle input changes for new parameter
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    
    setEditingParameter(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear errors when user types
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };
  
  // Add new parameter
  const addParameter = () => {
    // Validate inputs
    const validationErrors: Record<string, string> = {};
    
    if (!editingParameter.name?.trim()) {
      validationErrors.name = 'Parameter name is required';
    }
    
    if (parameters.some(p => p.name === editingParameter.name)) {
      validationErrors.name = 'Parameter name must be unique';
    }
    
    if (!editingParameter.value?.trim() && editingParameter.type !== 'boolean') {
      validationErrors.value = 'Parameter value is required';
    }
    
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    
    // Add new parameter
    const newParameter: StepParameter = {
      name: editingParameter.name as string,
      value: editingParameter.value as string,
      type: editingParameter.type as 'string' | 'number' | 'boolean' | 'variable' | 'locator',
      description: editingParameter.description
    };
    
    onChange([...parameters, newParameter]);
    
    // Reset form
    setEditingParameter({
      name: '',
      value: '',
      type: 'string'
    });
    setErrors({});
  };
  
  // Remove parameter
  const removeParameter = (index: number) => {
    const updatedParameters = [...parameters];
    updatedParameters.splice(index, 1);
    onChange(updatedParameters);
  };
  
  // Edit parameter
  const editParameter = (index: number) => {
    // Load parameter into editing form
    setEditingParameter(parameters[index]);
    
    // Remove from list
    removeParameter(index);
  };
  
  return (
    <div className="parameter-form">
      {/* List of existing parameters */}
      {parameters.length > 0 ? (
        <div className="parameter-list">
          {parameters.map((param, index) => (
            <div className="parameter-item" key={index}>
              <div>
                <div className="parameter-item-name">{param.name}</div>
                <div className="parameter-item-value">
                  <span className="badge bg-secondary me-2">{param.type}</span>
                  {param.value}
                </div>
              </div>
              <div className="parameter-item-actions">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => editParameter(index)}
                  disabled={disabled}
                >
                  <i className="bi bi-pencil"></i>
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => removeParameter(index)}
                  disabled={disabled}
                  className="ms-2"
                >
                  <i className="bi bi-trash"></i>
                </Button>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <div className="alert alert-info" role="alert">
          No parameters added yet. Add parameters below.
        </div>
      )}
      
      {/* Form to add new parameters */}
      <div className="card">
        <div className="card-header">
          <h5 className="mb-0">Add Parameter</h5>
        </div>
        <div className="card-body">
          <div className="row">
            <div className="col-md-6 mb-3">
              <label htmlFor="paramName" className="form-label">Name</label>
              <Input
                id="paramName"
                name="name"
                value={editingParameter.name || ''}
                onChange={handleInputChange}
                error={errors.name}
                disabled={disabled}
                placeholder="Parameter name"
              />
            </div>
            
            <div className="col-md-6 mb-3">
              <label htmlFor="paramType" className="form-label">Type</label>
              <Select
                id="paramType"
                name="type"
                value={editingParameter.type || 'string'}
                onChange={handleInputChange}
                options={parameterTypes}
                disabled={disabled}
              />
            </div>
          </div>
          
          <div className="mb-3">
            <label htmlFor="paramValue" className="form-label">Value</label>
            <Input
              id="paramValue"
              name="value"
              value={editingParameter.value || ''}
              onChange={handleInputChange}
              error={errors.value}
              disabled={disabled}
              placeholder="Parameter value"
            />
          </div>
          
          <div className="mb-3">
            <label htmlFor="paramDescription" className="form-label">Description (optional)</label>
            <Input
              id="paramDescription"
              name="description"
              value={editingParameter.description || ''}
              onChange={handleInputChange}
              disabled={disabled}
              placeholder="Optional description"
            />
          </div>
          
          <Button
            onClick={addParameter}
            disabled={disabled}
          >
            <i className="bi bi-plus-circle me-1"></i> Add Parameter
          </Button>
        </div>
      </div>
    </div>
  );
};

export default ParameterForm; 