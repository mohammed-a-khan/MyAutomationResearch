import React, { useState } from 'react';
import { LocatorSuggestion, LocatorStrategy } from '../../types/codebuilder';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import Button from '../common/Button';
import Input from '../common/Input';

interface SmartLocatorPanelProps {
  projectId: string;
}

/**
 * SmartLocatorPanel component for finding optimal element locators
 */
const SmartLocatorPanel: React.FC<SmartLocatorPanelProps> = ({ projectId }) => {
  const { getLocatorSuggestions } = useCodeBuilder();
  
  // State
  const [elementDescription, setElementDescription] = useState<string>('');
  const [screenshotFile, setScreenshotFile] = useState<File | null>(null);
  const [suggestions, setSuggestions] = useState<LocatorSuggestion[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'text' | 'screenshot'>('text');
  
  // Handle description input change
  const handleDescriptionChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setElementDescription(e.target.value);
    if (error) setError(null);
  };
  
  // Handle screenshot file upload
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      setScreenshotFile(e.target.files[0]);
      if (error) setError(null);
    }
  };
  
  // Get locator suggestions from text description
  const getTextSuggestions = async () => {
    if (!elementDescription.trim()) {
      setError('Please provide a description of the element');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      const suggestions = await getLocatorSuggestions(elementDescription);
      setSuggestions(suggestions);
    } catch (err) {
      setError('Failed to get locator suggestions');
      setSuggestions([]);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Get locator suggestions from screenshot
  const getScreenshotSuggestions = async () => {
    if (!screenshotFile) {
      setError('Please upload a screenshot');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      const suggestions = await getLocatorSuggestions(screenshotFile);
      setSuggestions(suggestions);
    } catch (err) {
      setError('Failed to get locator suggestions from screenshot');
      setSuggestions([]);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Handle get suggestions button click
  const handleGetSuggestions = () => {
    if (activeTab === 'text') {
      getTextSuggestions();
    } else {
      getScreenshotSuggestions();
    }
  };
  
  // Copy locator to clipboard
  const copyLocator = (value: string) => {
    navigator.clipboard.writeText(value).then(() => {
      // Show a toast notification or some other feedback
      alert('Copied to clipboard!');
    });
  };
  
  // Get display name for locator strategy
  const getStrategyDisplayName = (strategy: LocatorStrategy): string => {
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
        return 'Class Name';
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
    <div className="smart-locator-panel">
      <div className="mb-4">
        <h3>Smart Locator Generator</h3>
        <p className="text-muted">
          Generate optimal, stable locators for elements using AI technology.
        </p>
      </div>
      
      <div className="card mb-4">
        <div className="card-header">
          <ul className="nav nav-tabs card-header-tabs">
            <li className="nav-item">
              <a 
                className={`nav-link ${activeTab === 'text' ? 'active' : ''}`} 
                href="#" 
                onClick={(e) => {
                  e.preventDefault();
                  setActiveTab('text');
                }}
              >
                Text Description
              </a>
            </li>
            <li className="nav-item">
              <a 
                className={`nav-link ${activeTab === 'screenshot' ? 'active' : ''}`} 
                href="#" 
                onClick={(e) => {
                  e.preventDefault();
                  setActiveTab('screenshot');
                }}
              >
                Screenshot
              </a>
            </li>
          </ul>
        </div>
        
        <div className="card-body">
          {activeTab === 'text' ? (
            <div className="text-search">
              <div className="mb-3">
                <label htmlFor="elementDescription" className="form-label">
                  Element Description
                </label>
                <Input
                  id="elementDescription"
                  value={elementDescription}
                  onChange={handleDescriptionChange}
                  type="textarea"
                  rows={3}
                  placeholder="Describe the element (e.g. 'Login button on top right', 'Username field with label email')"
                  disabled={isLoading}
                />
              </div>
            </div>
          ) : (
            <div className="screenshot-search">
              <div className="mb-3">
                <label htmlFor="screenshot" className="form-label">
                  Upload Screenshot
                </label>
                <div className="input-group">
                  <input
                    type="file"
                    className="form-control"
                    id="screenshot"
                    accept="image/*"
                    onChange={handleFileChange}
                    disabled={isLoading}
                  />
                </div>
                <div className="form-text">
                  Upload a screenshot of the page with the element you want to locate.
                </div>
              </div>
              
              {screenshotFile && (
                <div className="mb-3">
                  <div className="card">
                    <div className="card-body">
                      <strong>Selected file:</strong> {screenshotFile.name} ({Math.round(screenshotFile.size / 1024)} KB)
                    </div>
                  </div>
                </div>
              )}
            </div>
          )}
          
          <div className="d-flex justify-content-end">
            <Button 
              onClick={handleGetSuggestions}
              disabled={isLoading || 
                (activeTab === 'text' && !elementDescription.trim()) || 
                (activeTab === 'screenshot' && !screenshotFile)
              }
            >
              {isLoading ? (
                <>
                  <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                  Generating...
                </>
              ) : (
                'Generate Locators'
              )}
            </Button>
          </div>
        </div>
      </div>
      
      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}
      
      {suggestions.length > 0 && (
        <div className="card">
          <div className="card-header">
            <h5 className="mb-0">Suggested Locators</h5>
          </div>
          <div className="card-body">
            <div className="locator-suggestions">
              {suggestions.map((suggestion, index) => (
                <div key={index} className="locator-suggestion-item">
                  <div className="d-flex justify-content-between align-items-center">
                    <div>
                      <strong>{getStrategyDisplayName(suggestion.locator.strategy)}</strong>
                      <div className="code mt-1">
                        <code>{suggestion.locator.value}</code>
                      </div>
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
                  
                  <div className="d-flex justify-content-between mt-3">
                    {suggestion.locator.description && (
                      <small className="text-muted">{suggestion.locator.description}</small>
                    )}
                    <Button 
                      variant="outline" 
                      size="sm"
                      onClick={() => copyLocator(suggestion.locator.value)}
                    >
                      <i className="bi bi-clipboard"></i> Copy
                    </Button>
                  </div>
                </div>
              ))}
            </div>
            
            <div className="mt-3">
              <p>
                <small className="text-muted">
                  <i className="bi bi-info-circle me-1"></i>
                  These locators are sorted by stability and reliability. Higher percentages indicate more robust locators.
                </small>
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SmartLocatorPanel; 