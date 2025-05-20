import React, { useState, FormEvent } from 'react';
import { RecordingOptions } from '../../types/recorder';
import Input from '../common/Input';
import Checkbox from '../common/Checkbox';
import Button from '../common/Button';
import Select from '../common/Select';

interface RecorderFormProps {
  initialValues: RecordingOptions;
  onSubmit: (options: RecordingOptions) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

/**
 * Form for configuring recording options
 */
const RecorderForm: React.FC<RecorderFormProps> = ({ 
  initialValues, 
  onSubmit, 
  onCancel,
  isLoading = false
}) => {
  const [options, setOptions] = useState<RecordingOptions>(initialValues);
  const [urlError, setUrlError] = useState<string>('');

  // Browser options
  const browserOptions = [
    { value: 'chrome', label: 'Chrome (Selenium)' },
    { value: 'firefox', label: 'Firefox (Selenium)' },
    { value: 'edge', label: 'Edge (Selenium)' },
    { value: 'safari', label: 'Safari (Selenium)' },
    { value: 'chrome_playwright', label: 'Chrome (Playwright)' },
    { value: 'firefox_playwright', label: 'Firefox (Playwright)' },
    { value: 'webkit_playwright', label: 'WebKit (Playwright)' },
    { value: 'msedge_playwright', label: 'Edge (Playwright)' }
  ];

  // Framework options
  const frameworkOptions = [
    { value: 'selenium_java_testng', label: 'Selenium + Java (TestNG)' },
    { value: 'selenium_java_bdd', label: 'Selenium + Java (BDD/Cucumber)' },
    { value: 'playwright_typescript_testng', label: 'Playwright + TypeScript (TestNG)' },
    { value: 'playwright_typescript_bdd', label: 'Playwright + TypeScript (BDD)' }
  ];

  // Handle form submission
  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    
    if (isLoading) {
      return;
    }
    
    // Validate URL
    if (!options.targetUrl) {
      setUrlError('Target URL is required');
      return;
    }

    try {
      // Check if it's a valid URL format
      new URL(options.targetUrl);
      setUrlError('');
      onSubmit(options);
    } catch (error) {
      setUrlError('Please enter a valid URL including protocol (e.g., https://example.com)');
    }
  };

  // Handle field changes
  const handleChange = (name: keyof RecordingOptions, value: boolean | string) => {
    setOptions(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear URL error when user types in the URL field
    if (name === 'targetUrl') {
      setUrlError('');
    }
  };

  return (
    <form onSubmit={handleSubmit} className="recorder-options-form">
      <div className="form-group">
        <label htmlFor="targetUrl" className="form-label">Target URL</label>
        <Input
          id="targetUrl"
          name="targetUrl"
          value={options.targetUrl}
          onChange={(e) => handleChange('targetUrl', e.target.value)}
          placeholder="https://example.com"
          error={urlError}
          disabled={isLoading}
          required
        />
        <div className="form-help-text">
          Enter the URL of the website where you want to record actions
        </div>
      </div>

      <div className="form-group mt-3">
        <label htmlFor="browser" className="form-label">Browser</label>
        <Select
          id="browser"
          name="browser"
          value={options.browser}
          onChange={(e) => handleChange('browser', e.target.value)}
          options={browserOptions}
          disabled={isLoading}
        />
        <div className="form-help-text">
          Select which browser to use for recording
        </div>
      </div>

      <div className="form-group mt-3">
        <label htmlFor="framework" className="form-label">Framework</label>
        <Select
          id="framework"
          name="framework"
          value={options.framework || 'selenium_java_testng'}
          onChange={(e) => handleChange('framework', e.target.value)}
          options={frameworkOptions}
          disabled={isLoading}
        />
        <div className="form-help-text">
          Select which test framework to generate code for
        </div>
      </div>
      
      <div className="form-group mt-4">
        <h5>Recording Options</h5>
        
        <Checkbox
          id="captureScreenshots"
          name="captureScreenshots"
          label="Capture Screenshots"
          checked={options.captureScreenshots}
          onChange={(e) => handleChange('captureScreenshots', e.target.checked)}
          disabled={isLoading}
        />
        <div className="form-help-text mb-3">
          Take screenshots for each action (useful for debugging)
        </div>
        
        <Checkbox
          id="recordCssSelectors"
          name="recordCssSelectors"
          label="Record CSS Selectors"
          checked={options.recordCssSelectors}
          onChange={(e) => handleChange('recordCssSelectors', e.target.checked)}
          disabled={isLoading}
        />
        <div className="form-help-text mb-3">
          Generate CSS selectors for all elements
        </div>
        
        <Checkbox
          id="recordXpaths"
          name="recordXpaths"
          label="Record XPath Expressions"
          checked={options.recordXpaths}
          onChange={(e) => handleChange('recordXpaths', e.target.checked)}
          disabled={isLoading}
        />
        <div className="form-help-text mb-3">
          Generate XPath expressions for all elements
        </div>
        
        <Checkbox
          id="ignoreNonInteractive"
          name="ignoreNonInteractive"
          label="Ignore Non-interactive Elements"
          checked={options.ignoreNonInteractive}
          onChange={(e) => handleChange('ignoreNonInteractive', e.target.checked)}
          disabled={isLoading}
        />
        <div className="form-help-text mb-3">
          Don't record clicks on non-interactive elements
        </div>
        
        <Checkbox
          id="includeTimeouts"
          name="includeTimeouts"
          label="Include Wait Timeouts"
          checked={options.includeTimeouts}
          onChange={(e) => handleChange('includeTimeouts', e.target.checked)}
          disabled={isLoading}
        />
        <div className="form-help-text mb-3">
          Add wait steps between actions for more stable tests
        </div>
        
        <Checkbox
          id="smartAssertions"
          name="smartAssertions"
          label="Smart Assertions"
          checked={options.smartAssertions}
          onChange={(e) => handleChange('smartAssertions', e.target.checked)}
          disabled={isLoading}
        />
        <div className="form-help-text mb-3">
          Automatically add assertions for key elements
        </div>
      </div>
      
      <div className="d-flex justify-content-end gap-2 mt-4">
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
          isLoading={isLoading}
          disabled={isLoading}
        >
          Start Recording
        </Button>
      </div>
    </form>
  );
};

export default RecorderForm; 