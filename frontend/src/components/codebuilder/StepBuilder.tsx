import React, { useState, useEffect } from 'react';
import { 
  TestStep, 
  StepType, 
  CreateStepPayload, 
  UpdateStepPayload,
  StepParameter,
  Locator
} from '../../types/codebuilder';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import ParameterForm from './ParameterForm';
import LocatorSelector from './LocatorSelector';

interface StepBuilderProps {
  projectId: string;
  selectedStep: TestStep | null;
}

// Type that combines both payloads for form state while ensuring 
// type is always present for the form
interface StepFormData {
  projectId?: string;
  type: StepType;
  name: string;
  description?: string;
  command: string;
  disabled?: boolean;
  // Not storing parameters and target in form data directly
  // as they have their own state management
}

/**
 * StepBuilder component for creating and editing test steps
 */
const StepBuilder: React.FC<StepBuilderProps> = ({ projectId, selectedStep }) => {
  const { 
    createStep, 
    updateStep, 
    deleteStep,
    getLocatorSuggestions,
    isLoading
  } = useCodeBuilder();
  
  const isNewStep = !selectedStep;
  
  // Initial form data
  const defaultFormData: StepFormData = {
    projectId,
    type: StepType.ACTION,
    name: '',
    command: '',
    disabled: false
  };
  
  // Form states
  const [formData, setFormData] = useState<StepFormData>(defaultFormData);
  const [availableCommands, setAvailableCommands] = useState<string[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [isDeleting, setIsDeleting] = useState(false);
  const [parameters, setParameters] = useState<StepParameter[]>([]);
  const [target, setTarget] = useState<Locator | undefined>(undefined);
  
  // Set up form when selected step changes
  useEffect(() => {
    if (selectedStep) {
      setFormData({
        name: selectedStep.name,
        description: selectedStep.description || '',
        type: selectedStep.type,
        command: selectedStep.command,
        disabled: selectedStep.disabled
      });
      setParameters(selectedStep.parameters);
      setTarget(selectedStep.target);
    } else {
      // Reset form for new step
      setFormData(defaultFormData);
      setParameters([]);
      setTarget(undefined);
    }
    
    setErrors({});
  }, [selectedStep, projectId]);
  
  // Load available commands when step type changes
  useEffect(() => {
    const loadCommands = async () => {
      try {
        // This function would be implemented in the codebuilderService to fetch commands
        // const commands = await getAvailableCommands(formData.type);
        // setAvailableCommands(commands);
        
        // For now, we'll use hardcoded commands based on step type
        switch(formData.type) {
          case StepType.ACTION:
            setAvailableCommands(['click', 'type', 'clear', 'select', 'hover', 'drag', 'drop', 'submit']);
            break;
          case StepType.ASSERTION:
            setAvailableCommands(['equals', 'contains', 'matches', 'exists', 'notExists', 'isVisible', 'isEnabled']);
            break;
          case StepType.WAIT:
            setAvailableCommands(['waitForElement', 'waitForElementToBeVisible', 'waitForElementToBeEnabled', 'wait', 'waitForNavigation']);
            break;
          case StepType.NAVIGATION:
            setAvailableCommands(['navigate', 'back', 'forward', 'refresh', 'switchTab', 'closeTab']);
            break;
          case StepType.CUSTOM:
            setAvailableCommands(['executeJs', 'executeCommand', 'custom']);
            break;
          default:
            setAvailableCommands([]);
        }
      } catch (error) {
        console.error('Failed to load commands:', error);
        setAvailableCommands([]);
      }
    };
    
    loadCommands();
  }, [formData.type]);
  
  // Handle form input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear error for field
    if (errors[name]) {
      setErrors(prev => ({ ...prev, [name]: '' }));
    }
  };
  
  // Handle command change - may need to reset parameters
  const handleCommandChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const command = e.target.value;
    
    setFormData(prev => ({
      ...prev,
      command
    }));
    
    // Reset parameters if changing command
    if (command !== formData.command) {
      setParameters([]);
    }
  };
  
  // Handle parameters update
  const handleParametersChange = (newParameters: StepParameter[]) => {
    setParameters(newParameters);
  };
  
  // Handle locator update
  const handleLocatorChange = (newLocator?: Locator) => {
    setTarget(newLocator);
  };
  
  // Validate form
  const validateForm = (): boolean => {
    const validationErrors: Record<string, string> = {};
    
    if (!formData.name.trim()) {
      validationErrors.name = 'Step name is required';
    }
    
    if (!formData.command) {
      validationErrors.command = 'Command is required';
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
    
    try {
      if (isNewStep) {
        // Create new step
        await createStep({
          projectId,
          name: formData.name,
          type: formData.type,
          command: formData.command,
          description: formData.description,
          parameters,
          target
        });
      } else if (selectedStep) {
        // Update existing step
        await updateStep(selectedStep.id, {
          name: formData.name,
          command: formData.command,
          description: formData.description,
          parameters,
          target,
          disabled: formData.disabled
        });
      }
    } catch (error) {
      // Error handling is done in context
    }
  };
  
  // Handle step deletion
  const handleDelete = async () => {
    if (!selectedStep) return;
    
    setIsDeleting(true);
    try {
      await deleteStep(selectedStep.id);
    } catch (error) {
      // Error handling is done in context
    } finally {
      setIsDeleting(false);
    }
  };
  
  // Determine if form is in a loading state
  const isFormLoading = isLoading || isDeleting;
  
  return (
    <div className="step-builder">
      <div className="step-builder-header">
        <h4>{isNewStep ? 'Create New Step' : `Edit Step: ${selectedStep?.name}`}</h4>
      </div>
      
      <form onSubmit={handleSubmit}>
        <div className="step-builder-content">
          {/* Basic Information Section */}
          <div className="step-builder-section">
            <h5 className="step-builder-section-title">Basic Information</h5>
            
            <div className="mb-3">
              <label htmlFor="name" className="form-label">Step Name *</label>
              <Input
                id="name"
                name="name"
                value={formData.name}
                onChange={handleInputChange}
                placeholder="Enter step name"
                error={errors.name}
                disabled={isFormLoading}
                required
              />
            </div>
            
            <div className="mb-3">
              <label htmlFor="type" className="form-label">Step Type *</label>
              <Select
                id="type"
                name="type"
                value={formData.type}
                onChange={handleInputChange}
                options={Object.values(StepType).map(type => ({
                  value: type,
                  label: type.charAt(0) + type.slice(1).toLowerCase()
                }))}
                error={errors.type}
                disabled={isFormLoading || !isNewStep} // Cannot change type after creation
                required
              />
            </div>
            
            <div className="mb-3">
              <label htmlFor="command" className="form-label">Command *</label>
              <Select
                id="command"
                name="command"
                value={formData.command}
                onChange={handleCommandChange}
                options={availableCommands.map(cmd => ({
                  value: cmd,
                  label: cmd
                }))}
                error={errors.command}
                disabled={isFormLoading}
                required
              />
            </div>
            
            <div className="mb-3">
              <label htmlFor="description" className="form-label">Description</label>
              <Input
                id="description"
                name="description"
                value={formData.description || ''}
                onChange={handleInputChange}
                placeholder="Enter step description"
                type="textarea"
                rows={2}
                disabled={isFormLoading}
              />
            </div>
            
            {!isNewStep && (
              <div className="mb-3 form-check">
                <input
                  type="checkbox"
                  className="form-check-input"
                  id="disabled"
                  name="disabled"
                  checked={!!formData.disabled}
                  onChange={(e) => {
                    setFormData(prev => ({
                      ...prev,
                      disabled: e.target.checked
                    }));
                  }}
                  disabled={isFormLoading}
                />
                <label className="form-check-label" htmlFor="disabled">
                  Disable this step
                </label>
              </div>
            )}
          </div>
          
          {/* Target Element Section */}
          {['ACTION', 'ASSERTION', 'WAIT'].includes(formData.type) && formData.command && (
            <div className="step-builder-section">
              <h5 className="step-builder-section-title">Target Element</h5>
              <LocatorSelector
                projectId={projectId}
                initialLocator={target}
                onChange={handleLocatorChange}
                onGetSuggestions={getLocatorSuggestions}
                disabled={isFormLoading}
              />
            </div>
          )}
          
          {/* Parameters Section */}
          {formData.command && (
            <div className="step-builder-section">
              <h5 className="step-builder-section-title">Parameters</h5>
              <ParameterForm
                parameters={parameters}
                onChange={handleParametersChange}
                disabled={isFormLoading}
              />
            </div>
          )}
        </div>
        
        {/* Form Actions */}
        <div className="step-builder-footer">
          {!isNewStep && (
            <Button
              type="button"
              variant="danger"
              onClick={handleDelete}
              disabled={isFormLoading}
            >
              {isDeleting ? 'Deleting...' : 'Delete Step'}
            </Button>
          )}
          
          <Button
            type="submit"
            disabled={isFormLoading}
          >
            {isLoading 
              ? (isNewStep ? 'Creating...' : 'Saving...') 
              : (isNewStep ? 'Create Step' : 'Save Changes')}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default StepBuilder; 