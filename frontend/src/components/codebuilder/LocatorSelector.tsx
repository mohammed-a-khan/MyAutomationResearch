import React, { useState, useEffect } from 'react';
import { Locator, LocatorStrategy, LocatorSuggestion } from '../../types/codebuilder';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import { v4 as uuidv4 } from 'uuid';

interface LocatorSelectorProps {
  projectId: string;
  initialLocator?: Locator;
  onChange: (locator?: Locator) => void;
  onGetSuggestions: (elementRef: string | File) => Promise<LocatorSuggestion[]>;
  disabled?: boolean;
}

/**
 * LocatorSelector component for selecting and configuring element locators
 */
const LocatorSelector: React.FC<LocatorSelectorProps> = ({
  projectId,
  initialLocator,
  onChange,
  onGetSuggestions,
  disabled = false
}) => {
  // State
  const [locator, setLocator] = useState<Locator | undefined>(initialLocator);
  const [suggestions, setSuggestions] = useState<LocatorSuggestion[]>([]);
  const [elementRef, setElementRef] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [showSuggestions, setShowSuggestions] = useState<boolean>(false);
  
  // Initialize locator when initialLocator changes
  useEffect(() => {
    if (initialLocator) {
      setLocator(initialLocator);
    } else {
      // Default empty locator
      setLocator({
        id: uuidv4(),
        strategy: LocatorStrategy.CSS,
        value: ''
      });
    }
  }, [initialLocator]);
  
  // Update parent when locator changes
  useEffect(() => {
    onChange(locator);
  }, [locator, onChange]);
  
  // Handle locator strategy change
  const handleStrategyChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newStrategy = e.target.value as LocatorStrategy;
    
    if (locator) {
      setLocator({
        ...locator,
        strategy: newStrategy
      });
    }
  };
  
  // Handle locator value change
  const handleValueChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value;
    
    if (locator) {
      setLocator({
        ...locator,
        value: newValue
      });
    }
  };
  
  // Handle description change
  const handleDescriptionChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newDescription = e.target.value;
    
    if (locator) {
      setLocator({
        ...locator,
        description: newDescription || undefined
      });
    }
  };
  
  // Get locator suggestions
  const getSuggestions = async () => {
    if (!elementRef.trim()) {
      setError('Please provide an element reference');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      const newSuggestions = await onGetSuggestions(elementRef);
      setSuggestions(newSuggestions);
      setShowSuggestions(true);
    } catch (err) {
      setError('Failed to get locator suggestions');
      setSuggestions([]);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Select a suggestion
  const selectSuggestion = (suggestion: LocatorSuggestion) => {
    setLocator(suggestion.locator);
    setShowSuggestions(false);
  };
  
  // Show locator strategy display name
  const getStrategyDisplayName = (strategy: LocatorStrategy) => {
    switch (strategy) {
      case LocatorStrategy.CSS:
        return 'CSS Selector';
      case LocatorStrategy.XPATH:
        return 'XPath';
      case LocatorStrategy.ID:
        return 'ID';
      case LocatorStrategy.NAME:
        return 'Name';
      case LocatorStrategy.TAG:
        return 'Tag';
      case LocatorStrategy.CLASS:
        return 'Class';
      case LocatorStrategy.LINK_TEXT:
        return 'Link Text';
      case LocatorStrategy.PARTIAL_LINK_TEXT:
        return 'Partial Link Text';
      case LocatorStrategy.ACCESSIBILITY_ID:
        return 'Accessibility ID';
      default:
        return strategy;
    }
  };
  
  return (
    <div className="locator-selector">
      <div className="locator-selector-header">
        <h6>Element Locator</h6>
        {locator?.confidence && (
          <span className="badge bg-success">
            {Math.round(locator.confidence * 100)}% Confidence
          </span>
        )}
      </div>
      
      <div className="row mb-3">
        <div className="col-md-4">
          <label htmlFor="locatorStrategy" className="form-label">Strategy</label>
          <Select
            id="locatorStrategy"
            value={locator?.strategy || LocatorStrategy.CSS}
            onChange={handleStrategyChange}
            options={Object.values(LocatorStrategy).map(strategy => ({
              value: strategy,
              label: getStrategyDisplayName(strategy)
            }))}
            disabled={disabled}
          />
        </div>
        
        <div className="col-md-8">
          <label htmlFor="locatorValue" className="form-label">Value</label>
          <Input
            id="locatorValue"
            value={locator?.value || ''}
            onChange={handleValueChange}
            disabled={disabled}
            placeholder={`Enter ${getStrategyDisplayName(locator?.strategy || LocatorStrategy.CSS)} value`}
          />
        </div>
      </div>
      
      <div className="mb-3">
        <label htmlFor="locatorDescription" className="form-label">Description (optional)</label>
        <Input
          id="locatorDescription"
          value={locator?.description || ''}
          onChange={handleDescriptionChange}
          disabled={disabled}
          placeholder="Describe this element (e.g. 'Login button')"
        />
      </div>
      
      {locator?.value && (
        <div className="locator-preview mb-3">
          <strong>Preview: </strong>
          {locator.strategy === LocatorStrategy.CSS && <span>document.querySelector('{locator.value}')</span>}
          {locator.strategy === LocatorStrategy.XPATH && <span>//XPath: {locator.value}</span>}
          {locator.strategy === LocatorStrategy.ID && <span>document.getElementById('{locator.value}')</span>}
          {locator.strategy === LocatorStrategy.NAME && <span>document.getElementsByName('{locator.value}')[0]</span>}
          {locator.strategy === LocatorStrategy.TAG && <span>document.getElementsByTagName('{locator.value}')[0]</span>}
          {locator.strategy === LocatorStrategy.CLASS && <span>document.getElementsByClassName('{locator.value}')[0]</span>}
          {locator.strategy === LocatorStrategy.LINK_TEXT && <span>Link: '{locator.value}'</span>}
          {locator.strategy === LocatorStrategy.PARTIAL_LINK_TEXT && <span>Link containing: '{locator.value}'</span>}
          {locator.strategy === LocatorStrategy.ACCESSIBILITY_ID && <span>Accessibility ID: '{locator.value}'</span>}
        </div>
      )}
      
      <hr className="my-4" />
      
      <h6>Smart Locator Suggestions</h6>
      <p className="text-muted small">
        Get AI-powered locator suggestions by providing a description or other reference to the element.
      </p>
      
      <div className="input-group mb-3">
        <Input
          value={elementRef}
          onChange={(e) => setElementRef(e.target.value)}
          disabled={disabled || isLoading}
          placeholder="Describe the element (e.g. 'Login button', 'Username input')"
        />
        <Button
          onClick={getSuggestions}
          disabled={disabled || isLoading || !elementRef.trim()}
        >
          {isLoading ? 'Loading...' : 'Get Suggestions'}
        </Button>
      </div>
      
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      
      {showSuggestions && suggestions.length > 0 && (
        <div className="locator-suggestions">
          <h6>Select a suggested locator:</h6>
          {suggestions.map((suggestion, index) => (
            <div
              key={index}
              className={`locator-suggestion-item ${locator?.id === suggestion.locator.id ? 'selected' : ''}`}
              onClick={() => selectSuggestion(suggestion)}
            >
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <strong>{getStrategyDisplayName(suggestion.locator.strategy)}</strong>
                  <div className="text-muted">{suggestion.locator.value}</div>
                </div>
                <div>
                  <span className="badge bg-primary">
                    {Math.round((suggestion.locator.confidence || 0) * 100)}%
                  </span>
                </div>
              </div>
              <div className="locator-confidence mt-2">
                <div 
                  className="locator-confidence-bar"
                  style={{ width: `${(suggestion.locator.confidence || 0) * 100}%` }}
                ></div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default LocatorSelector; 