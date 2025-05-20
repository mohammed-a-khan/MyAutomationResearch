import React, { useState } from 'react';
import { AssertionConfig, AssertionType, RecordedEvent } from '../../types/recorder';
import { useRecorder } from '../../context/RecorderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import './AssertionBuilder.css';

interface AssertionBuilderProps {
  event: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for building complex assertions
 */
const AssertionBuilder: React.FC<AssertionBuilderProps> = ({ event, onClose }) => {
  const { addAssertion, updateAssertion, deleteAssertion } = useRecorder();
  
  // Start with existing assertions or an empty one
  const [assertions, setAssertions] = useState<AssertionConfig[]>(
    event.assertions || [
      {
        id: `assertion-${Date.now()}`,
        type: AssertionType.ELEMENT_EXISTS,
        elementSelector: event.selectedLocator || { type: 'css', value: '', score: 1 },
        softAssertion: false
      }
    ]
  );

  // Currently editing assertion index
  const [editingIndex, setEditingIndex] = useState<number>(0);

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Save all assertions
    for (const assertion of assertions) {
      if (event.assertions?.find(a => a.id === assertion.id)) {
        // Update existing assertion
        await updateAssertion(event.id, assertion.id, assertion);
      } else {
        // Add new assertion
        await addAssertion(event.id, assertion);
      }
    }
    
    // Handle removed assertions
    if (event.assertions) {
      const removedIds = event.assertions
        .filter(a => !assertions.some(newA => newA.id === a.id))
        .map(a => a.id);
      
      for (const id of removedIds) {
        await deleteAssertion(event.id, id);
      }
    }
    
    onClose();
  };

  // Add a new assertion
  const addNewAssertion = () => {
    const newAssertion: AssertionConfig = {
      id: `assertion-${Date.now()}`,
      type: AssertionType.ELEMENT_EXISTS,
      elementSelector: { type: 'css', value: '', score: 1 },
      softAssertion: false
    };
    
    setAssertions([...assertions, newAssertion]);
    setEditingIndex(assertions.length);
  };

  // Delete an assertion
  const deleteAssertionAtIndex = (index: number) => {
    const newAssertions = [...assertions];
    newAssertions.splice(index, 1);
    
    setAssertions(newAssertions);
    setEditingIndex(Math.min(editingIndex, newAssertions.length - 1));
  };

  // Update assertion property
  const updateAssertionProperty = (
    index: number,
    property: keyof AssertionConfig,
    value: any
  ) => {
    const updatedAssertions = [...assertions];
    updatedAssertions[index] = {
      ...updatedAssertions[index],
      [property]: value
    };
    setAssertions(updatedAssertions);
  };

  // Options for assertion type dropdown
  const assertionTypeOptions = Object.values(AssertionType).map(type => ({
    value: type,
    label: type.replace(/_/g, ' ').toLowerCase()
  }));

  // Get current assertion being edited
  const currentAssertion = assertions[editingIndex] || assertions[0];

  // Check if the assertion type requires an element selector
  const needsElementSelector = (type: AssertionType) => {
    return [
      AssertionType.ELEMENT_EXISTS,
      AssertionType.ELEMENT_VISIBLE,
      AssertionType.ATTRIBUTE_EQUALS,
      AssertionType.PROPERTY_EQUALS
    ].includes(type);
  };

  // Check if the assertion type needs an expected value
  const needsExpectedValue = (type: AssertionType) => {
    return [
      AssertionType.EQUALS,
      AssertionType.NOT_EQUALS,
      AssertionType.CONTAINS,
      AssertionType.NOT_CONTAINS,
      AssertionType.MATCHES_REGEX,
      AssertionType.GREATER_THAN,
      AssertionType.LESS_THAN,
      AssertionType.ATTRIBUTE_EQUALS,
      AssertionType.PROPERTY_EQUALS
    ].includes(type);
  };

  return (
    <form onSubmit={handleSubmit} className="assertion-builder">
      {/* Assertion tabs */}
      <div className="assertion-tabs mb-3">
        <div className="assertion-tabs-list">
          {assertions.map((assertion, index) => (
            <div
              key={assertion.id}
              className={`assertion-tab ${index === editingIndex ? 'active' : ''}`}
              onClick={() => setEditingIndex(index)}
            >
              <span className="assertion-tab-name">
                {`Assertion ${index + 1}`}
              </span>
              {assertions.length > 1 && (
                <button 
                  type="button"
                  className="assertion-tab-delete"
                  onClick={(e) => {
                    e.stopPropagation();
                    deleteAssertionAtIndex(index);
                  }}
                >
                  <i className="bi bi-x"></i>
                </button>
              )}
            </div>
          ))}
          <button
            type="button"
            className="assertion-tab-add"
            onClick={addNewAssertion}
          >
            <i className="bi bi-plus"></i>
          </button>
        </div>
      </div>

      {/* Current assertion editor */}
      <div className="assertion-editor">
        <div className="form-group mb-3">
          <label htmlFor="assertion-type" className="form-label">Assertion Type</label>
          <Select
            id="assertion-type"
            value={currentAssertion.type}
            onChange={(e) => updateAssertionProperty(editingIndex, 'type', e.target.value)}
            options={assertionTypeOptions}
          />
        </div>

        {/* Element selector if needed */}
        {needsElementSelector(currentAssertion.type) && (
          <div className="form-group mb-3">
            <label htmlFor="element-selector" className="form-label">Element Selector</label>
            <Input
              id="element-selector"
              type="text"
              value={currentAssertion.elementSelector?.value || ''}
              onChange={(e) => updateAssertionProperty(editingIndex, 'elementSelector', {
                type: 'css',
                value: e.target.value,
                score: 1
              })}
              placeholder="CSS Selector (e.g., #email)"
            />
          </div>
        )}

        {/* Property name for property assertions */}
        {currentAssertion.type === AssertionType.PROPERTY_EQUALS && (
          <div className="form-group mb-3">
            <label htmlFor="property-name" className="form-label">Property Name</label>
            <Input
              id="property-name"
              type="text"
              value={currentAssertion.property || ''}
              onChange={(e) => updateAssertionProperty(editingIndex, 'property', e.target.value)}
              placeholder="e.g., textContent, value, disabled"
            />
          </div>
        )}

        {/* Attribute name for attribute assertions */}
        {currentAssertion.type === AssertionType.ATTRIBUTE_EQUALS && (
          <div className="form-group mb-3">
            <label htmlFor="attribute-name" className="form-label">Attribute Name</label>
            <Input
              id="attribute-name"
              type="text"
              value={currentAssertion.attributeName || ''}
              onChange={(e) => updateAssertionProperty(editingIndex, 'attributeName', e.target.value)}
              placeholder="e.g., data-id, aria-label"
            />
          </div>
        )}

        {/* Expected value for assertions that need it */}
        {needsExpectedValue(currentAssertion.type) && (
          <div className="form-group mb-3">
            <label htmlFor="expected-value" className="form-label">Expected Value</label>
            <Input
              id="expected-value"
              type="text"
              value={currentAssertion.expectedValue || ''}
              onChange={(e) => updateAssertionProperty(editingIndex, 'expectedValue', e.target.value)}
              placeholder="Expected value or ${variable} reference"
            />
            <small className="form-text text-muted">
              Use ${'{variable}'} syntax to reference test variables
            </small>
          </div>
        )}

        {/* Actual value expression for custom assertions */}
        {currentAssertion.type === AssertionType.CUSTOM && (
          <div className="form-group mb-3">
            <label htmlFor="actual-value" className="form-label">Actual Value Expression</label>
            <Input
              id="actual-value"
              type="text"
              value={currentAssertion.actualValueExpression || ''}
              onChange={(e) => updateAssertionProperty(editingIndex, 'actualValueExpression', e.target.value)}
              placeholder="e.g., document.title"
            />
            <small className="form-text text-muted">
              JavaScript expression that will be evaluated to get the actual value
            </small>
          </div>
        )}

        {/* Custom assertion code */}
        {currentAssertion.type === AssertionType.CUSTOM && (
          <div className="form-group mb-3">
            <label htmlFor="custom-assertion" className="form-label">Custom Assertion Code</label>
            <textarea
              id="custom-assertion"
              className="form-control"
              rows={3}
              value={currentAssertion.customAssertion || ''}
              onChange={(e) => updateAssertionProperty(editingIndex, 'customAssertion', e.target.value)}
              placeholder="e.g., expect(actualValue).to.be.true"
            />
            <small className="form-text text-muted">
              Custom assertion code that will be inserted into the test
            </small>
          </div>
        )}

        {/* Custom failure message */}
        <div className="form-group mb-3">
          <label htmlFor="failure-message" className="form-label">Failure Message (Optional)</label>
          <Input
            id="failure-message"
            type="text"
            value={currentAssertion.failureMessage || ''}
            onChange={(e) => updateAssertionProperty(editingIndex, 'failureMessage', e.target.value)}
            placeholder="Custom message shown when assertion fails"
          />
        </div>

        {/* Soft assertion toggle */}
        <div className="form-group mb-4">
          <div className="d-flex align-items-center">
            <input
              type="checkbox"
              id="soft-assertion"
              className="form-check-input me-2"
              checked={currentAssertion.softAssertion || false}
              onChange={(e) => updateAssertionProperty(editingIndex, 'softAssertion', e.target.checked)}
            />
            <label htmlFor="soft-assertion" className="form-check-label">
              Soft Assertion
            </label>
          </div>
          <small className="form-text text-muted">
            If checked, test execution will continue even if this assertion fails
          </small>
        </div>
      </div>

      {/* Assertion preview */}
      <div className="assertion-preview mb-4">
        <h6>Assertion Preview</h6>
        <div className="code-preview border rounded p-2 bg-light">
          <code>
            {getAssertionPreview(currentAssertion)}
          </code>
        </div>
      </div>

      <div className="d-flex justify-content-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" variant="primary">
          Save Assertions
        </Button>
      </div>
    </form>
  );
};

/**
 * Generate a preview of the assertion code
 */
const getAssertionPreview = (assertion: AssertionConfig): string => {
  let preview = '';
  
  // Start with soft assertion wrapper if needed
  const softPrefix = assertion.softAssertion ? 'softAssert.' : '';
  
  switch (assertion.type) {
    case AssertionType.ELEMENT_EXISTS:
      preview = `${softPrefix}expect(page.locator('${assertion.elementSelector?.value}')).toBeVisible()`;
      break;
    case AssertionType.ELEMENT_VISIBLE:
      preview = `${softPrefix}expect(page.locator('${assertion.elementSelector?.value}')).toBeVisible()`;
      break;
    case AssertionType.EQUALS:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).toEqual(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.NOT_EQUALS:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).not.toEqual(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.CONTAINS:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).toContain(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.NOT_CONTAINS:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).not.toContain(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.MATCHES_REGEX:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).toMatch(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.GREATER_THAN:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).toBeGreaterThan(${assertion.expectedValue})`;
      break;
    case AssertionType.LESS_THAN:
      preview = `${softPrefix}expect(${assertion.actualValueExpression || 'value'}).toBeLessThan(${assertion.expectedValue})`;
      break;
    case AssertionType.ATTRIBUTE_EQUALS:
      preview = `${softPrefix}expect(page.locator('${assertion.elementSelector?.value}').getAttribute('${assertion.attributeName}')).toEqual(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.PROPERTY_EQUALS:
      preview = `${softPrefix}expect(page.locator('${assertion.elementSelector?.value}').${assertion.property || 'textContent'}).toEqual(${formatValue(assertion.expectedValue)})`;
      break;
    case AssertionType.CUSTOM:
      preview = assertion.customAssertion || 'custom assertion';
      break;
  }
  
  return preview;
};

/**
 * Format a value for display in code preview
 */
const formatValue = (value: string | undefined): string => {
  if (!value) return '""';
  if (value.startsWith('${') && value.endsWith('}')) {
    return value.substring(2, value.length - 1);
  }
  return `"${value}"`;
};

export default AssertionBuilder;