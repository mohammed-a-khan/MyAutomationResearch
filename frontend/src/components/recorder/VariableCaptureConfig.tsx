import React, { useState } from 'react';
import { VariableBinding, RecordedEvent } from '../../types/recorder';
import { useRecorder } from '../../context/RecorderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';

interface VariableCaptureConfigProps {
  event: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for configuring variable capture from different sources
 */
const VariableCaptureConfig: React.FC<VariableCaptureConfigProps> = ({ event, onClose }) => {
  const { updateVariableBinding } = useRecorder();
  const [variableBinding, setVariableBinding] = useState<VariableBinding>(
    event.variableBinding || {
      name: '',
      source: 'element',
      elementProperty: 'textContent',
      scope: 'test'
    }
  );

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await updateVariableBinding(event.id, variableBinding);
    onClose();
  };

  // Update variable binding property
  const handleChange = (field: keyof VariableBinding, value: any) => {
    setVariableBinding(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Options for source type dropdown
  const sourceOptions = [
    { value: 'element', label: 'Element' },
    { value: 'response', label: 'API Response' },
    { value: 'expression', label: 'JavaScript Expression' },
    { value: 'constant', label: 'Constant Value' }
  ];

  // Options for element property dropdown
  const elementPropertyOptions = [
    { value: 'textContent', label: 'Text Content' },
    { value: 'value', label: 'Input Value' },
    { value: 'attribute', label: 'Attribute Value' },
    { value: 'innerText', label: 'Inner Text' },
    { value: 'innerHTML', label: 'Inner HTML' },
    { value: 'outerHTML', label: 'Outer HTML' },
    { value: 'className', label: 'CSS Class' },
    { value: 'id', label: 'ID' },
    { value: 'tagName', label: 'Tag Name' }
  ];

  // Options for variable scope
  const scopeOptions = [
    { value: 'test', label: 'Test Scope (current test only)' },
    { value: 'session', label: 'Session Scope (persists between tests)' },
    { value: 'global', label: 'Global Scope (available everywhere)' }
  ];

  return (
    <form onSubmit={handleSubmit} className="variable-capture-config">
      <div className="form-group mb-3">
        <label htmlFor="variable-name" className="form-label">Variable Name</label>
        <Input
          id="variable-name"
          type="text"
          value={variableBinding.name}
          onChange={(e) => handleChange('name', e.target.value)}
          placeholder="Enter variable name (e.g., customerName)"
          required
        />
        <small className="form-text text-muted">
          This name will be used to reference the variable in your test
        </small>
      </div>

      <div className="form-group mb-3">
        <label htmlFor="variable-source" className="form-label">Source Type</label>
        <Select
          id="variable-source"
          value={variableBinding.source}
          onChange={(e) => handleChange('source', e.target.value)}
          options={sourceOptions}
        />
      </div>

      {/* Show different inputs based on source type */}
      {variableBinding.source === 'element' && (
        <>
          <div className="form-group mb-3">
            <label htmlFor="element-selector" className="form-label">Element Selector</label>
            <Input
              id="element-selector"
              type="text"
              value={variableBinding.elementSelector?.value || ''}
              onChange={(e) => handleChange('elementSelector', { 
                type: 'css',
                value: e.target.value,
                score: 1
              })}
              placeholder="CSS Selector (e.g., #email)"
            />
          </div>

          <div className="form-group mb-3">
            <label htmlFor="element-property" className="form-label">Element Property</label>
            <Select
              id="element-property"
              value={variableBinding.elementProperty || 'textContent'}
              onChange={(e) => handleChange('elementProperty', e.target.value)}
              options={elementPropertyOptions}
            />
          </div>

          {variableBinding.elementProperty === 'attribute' && (
            <div className="form-group mb-3">
              <label htmlFor="attribute-name" className="form-label">Attribute Name</label>
              <Input
                id="attribute-name"
                type="text"
                value={variableBinding.attributeName || ''}
                onChange={(e) => handleChange('attributeName', e.target.value)}
                placeholder="e.g., data-id"
              />
            </div>
          )}
        </>
      )}

      {variableBinding.source === 'response' && (
        <div className="form-group mb-3">
          <label htmlFor="json-path" className="form-label">JSON Path</label>
          <Input
            id="json-path"
            type="text"
            value={variableBinding.jsonPath || ''}
            onChange={(e) => handleChange('jsonPath', e.target.value)}
            placeholder="e.g., $.data.users[0].name"
          />
          <small className="form-text text-muted">
            Path to extract data from JSON response
          </small>
        </div>
      )}

      {variableBinding.source === 'expression' && (
        <div className="form-group mb-3">
          <label htmlFor="js-expression" className="form-label">JavaScript Expression</label>
          <Input
            id="js-expression"
            type="text"
            value={variableBinding.expression || ''}
            onChange={(e) => handleChange('expression', e.target.value)}
            placeholder="e.g., new Date().toISOString()"
          />
          <small className="form-text text-muted">
            JavaScript expression that will be evaluated to get the value
          </small>
        </div>
      )}

      {variableBinding.source === 'constant' && (
        <div className="form-group mb-3">
          <label htmlFor="constant-value" className="form-label">Constant Value</label>
          <Input
            id="constant-value"
            type="text"
            value={variableBinding.value || ''}
            onChange={(e) => handleChange('value', e.target.value)}
            placeholder="e.g., test@example.com"
          />
        </div>
      )}

      <div className="form-group mb-3">
        <label htmlFor="variable-scope" className="form-label">Variable Scope</label>
        <Select
          id="variable-scope"
          value={variableBinding.scope}
          onChange={(e) => handleChange('scope', e.target.value)}
          options={scopeOptions}
        />
        <small className="form-text text-muted">
          Determines where and how long the variable will be available
        </small>
      </div>

      <div className="variable-preview mb-4">
        <h6>Variable Usage Preview</h6>
        <div className="code-preview border rounded p-2 bg-light">
          <code>${`{${variableBinding.name || 'variableName'}}`}</code>
        </div>
        <p className="text-muted mt-2">
          Use this syntax in your test steps to reference the captured value
        </p>
      </div>

      <div className="d-flex justify-content-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" variant="primary">
          Save Variable Capture
        </Button>
      </div>
    </form>
  );
};

export default VariableCaptureConfig; 