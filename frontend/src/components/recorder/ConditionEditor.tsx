import React, { useState, useEffect } from 'react';
import { Condition, ConditionType, RecordedEvent } from '../../types/recorder';
import { useRecorder } from '../../context/RecorderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';

interface ConditionEditorProps {
  event: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for editing condition configuration
 */
const ConditionEditor: React.FC<ConditionEditorProps> = ({ event, onClose }) => {
  const { updateCondition } = useRecorder();
  const [condition, setCondition] = useState<Condition>(event.condition || {
    type: ConditionType.EQUALS,
    leftOperand: '',
    rightOperand: '',
    thenEventIds: [],
  });

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await updateCondition(event.id, condition);
    onClose();
  };

  // Update condition property
  const handleChange = (field: keyof Condition, value: any) => {
    setCondition(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Check if condition type requires right operand
  const needsRightOperand = () => {
    return ![
      ConditionType.ELEMENT_EXISTS,
      ConditionType.ELEMENT_NOT_EXISTS,
      ConditionType.ELEMENT_VISIBLE,
      ConditionType.ELEMENT_NOT_VISIBLE,
      ConditionType.CUSTOM_EXPRESSION
    ].includes(condition.type);
  };

  // Check if condition type requires element selector
  const needsElementSelector = () => {
    return [
      ConditionType.ELEMENT_EXISTS,
      ConditionType.ELEMENT_NOT_EXISTS,
      ConditionType.ELEMENT_VISIBLE,
      ConditionType.ELEMENT_NOT_VISIBLE
    ].includes(condition.type);
  };

  // Options for condition type dropdown
  const conditionTypeOptions = Object.values(ConditionType).map(type => ({
    value: type,
    label: type.replace(/_/g, ' ').toLowerCase()
  }));

  return (
    <form onSubmit={handleSubmit} className="condition-editor">
      <div className="form-group mb-3">
        <label htmlFor="condition-type" className="form-label">Condition Type</label>
        <Select
          id="condition-type"
          value={condition.type}
          onChange={(e) => handleChange('type', e.target.value)}
          options={conditionTypeOptions}
        />
      </div>

      {condition.type === ConditionType.CUSTOM_EXPRESSION ? (
        <div className="form-group mb-3">
          <label htmlFor="custom-expression" className="form-label">Custom Expression</label>
          <Input
            id="custom-expression"
            type="text"
            value={condition.customExpression || ''}
            onChange={(e) => handleChange('customExpression', e.target.value)}
            placeholder="e.g., ${variable} > 10 && ${anotherVar} === 'test'"
          />
          <small className="form-text text-muted">
            Use ${'{variable}'} syntax to reference test variables
          </small>
        </div>
      ) : (
        <>
          {/* Left operand (always required) */}
          <div className="form-group mb-3">
            <label htmlFor="left-operand" className="form-label">
              {needsElementSelector() ? 'Element Selector' : 'Left Operand'}
            </label>
            <Input
              id="left-operand"
              type="text"
              value={condition.leftOperand || ''}
              onChange={(e) => handleChange('leftOperand', e.target.value)}
              placeholder={needsElementSelector() 
                ? "CSS Selector or XPath" 
                : "Variable or value"}
            />
            <small className="form-text text-muted">
              {needsElementSelector() 
                ? "Enter a valid CSS selector or XPath expression" 
                : "Use ${'{variable}'} syntax to reference test variables"}
            </small>
          </div>

          {/* Right operand (only for certain condition types) */}
          {needsRightOperand() && (
            <div className="form-group mb-3">
              <label htmlFor="right-operand" className="form-label">Right Operand</label>
              <Input
                id="right-operand"
                type="text"
                value={condition.rightOperand || ''}
                onChange={(e) => handleChange('rightOperand', e.target.value)}
                placeholder="Variable or value"
              />
              <small className="form-text text-muted">
                Use ${'{variable}'} syntax to reference test variables
              </small>
            </div>
          )}
        </>
      )}

      {/* Description of what happens when condition is true/false */}
      <div className="condition-flow mb-4">
        <div className="condition-flow-item">
          <div className="condition-flow-header">
            <div className="condition-flow-badge condition-true">IF TRUE</div>
          </div>
          <div className="condition-flow-content">
            <p>The test will execute steps in the "Then" branch.</p>
            {/* Here we would show a list of selected "then" events if we had UI for selecting them */}
          </div>
        </div>
        
        <div className="condition-flow-item">
          <div className="condition-flow-header">
            <div className="condition-flow-badge condition-false">IF FALSE</div>
          </div>
          <div className="condition-flow-content">
            <p>{condition.elseEventIds?.length ? 
              "The test will execute steps in the 'Else' branch." : 
              "The test will skip the condition and continue."}</p>
            {/* Here we would show a list of selected "else" events if we had UI for selecting them */}
          </div>
        </div>
      </div>

      <div className="d-flex justify-content-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" variant="primary">
          Save Condition
        </Button>
      </div>
    </form>
  );
};

export default ConditionEditor; 