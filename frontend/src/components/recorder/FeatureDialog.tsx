import React from 'react';
import Modal from '../common/Modal';
import Button from '../common/Button';
import { useRecorder } from '../../context/RecorderContext';
import { RecordingStatus } from '../../types/recorder';
import './Recorder.css';

interface FeatureDialogProps {
  feature: string | null;
  onClose: () => void;
}

/**
 * Modal dialog for advanced features (conditions, loops, assertions, etc.)
 */
const FeatureDialog: React.FC<FeatureDialogProps> = ({ feature, onClose }) => {
  const { state } = useRecorder();
  
  if (!feature) {
    return null;
  }
  
  // Determine dialog title and content based on feature
  let title = '';
  let content = null;
  
  switch (feature) {
    case 'condition':
      title = 'Add Conditional Logic';
      content = <ConditionContent />;
      break;
    case 'loop':
      title = 'Add Loop';
      content = <LoopContent />;
      break;
    case 'dataSources':
      title = 'Manage Data Sources';
      content = <DataSourcesContent />;
      break;
    case 'variables':
      title = 'Manage Variables';
      content = <VariablesContent />;
      break;
    case 'assertions':
      title = 'Add Assertions';
      content = <AssertionsContent />;
      break;
    case 'groups':
      title = 'Manage Groups';
      content = <GroupsContent />;
      break;
    case 'help':
      title = 'Advanced Features Help';
      content = <HelpContent />;
      break;
    default:
      return null;
  }
  
  return (
    <Modal 
      isOpen={!!feature}
      title={title}
      onClose={onClose}
      size="lg"
    >
      <div className="feature-dialog-content">
        {content}
      </div>
      <div className="modal-footer">
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      </div>
    </Modal>
  );
};

// Condition dialog content
const ConditionContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <p className="text-muted">Create conditional logic to control test flow based on runtime conditions.</p>
      <div className="form-group">
        <label htmlFor="condition-type">Condition Type</label>
        <select className="form-control" id="condition-type">
          <option value="element">Element Exists</option>
          <option value="text">Text Contains</option>
          <option value="attribute">Element Attribute</option>
          <option value="url">URL Contains</option>
          <option value="cookie">Cookie Value</option>
          <option value="custom">Custom Expression</option>
        </select>
      </div>
      <div className="form-group">
        <label htmlFor="condition-value">Condition Value</label>
        <input type="text" className="form-control" id="condition-value" placeholder="Enter value to check" />
      </div>
      <div className="form-group">
        <label>If Condition is True:</label>
        <div className="form-check">
          <input className="form-check-input" type="radio" name="condition-action" id="action-continue" defaultChecked />
          <label className="form-check-label" htmlFor="action-continue">
            Continue with selected steps
          </label>
        </div>
        <div className="form-check">
          <input className="form-check-input" type="radio" name="condition-action" id="action-skip" />
          <label className="form-check-label" htmlFor="action-skip">
            Skip selected steps
          </label>
        </div>
      </div>
      <Button variant="primary">Save Condition</Button>
    </div>
  );
};

// Loop dialog content
const LoopContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <p className="text-muted">Create loops to repeat steps multiple times or iterate through data.</p>
      <div className="form-group">
        <label htmlFor="loop-type">Loop Type</label>
        <select className="form-control" id="loop-type">
          <option value="count">Fixed Count</option>
          <option value="while">While Condition True</option>
          <option value="for-each">For Each Item in Data Source</option>
        </select>
      </div>
      <div className="form-group">
        <label htmlFor="loop-count">Number of Iterations</label>
        <input type="number" className="form-control" id="loop-count" min="1" max="100" defaultValue="5" />
      </div>
      <div className="form-group">
        <label>Steps to Include in Loop:</label>
        <div className="selected-steps-list">
          <p className="text-muted">No steps selected. Please select steps from the event list first.</p>
        </div>
      </div>
      <Button variant="primary">Save Loop</Button>
    </div>
  );
};

// Data sources dialog content
const DataSourcesContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <p className="text-muted">Manage data sources to use in your tests (CSV, Excel, Database).</p>
      <div className="data-sources-list">
        <div className="data-source-item">
          <h5>No Data Sources</h5>
          <p>You haven't added any data sources yet.</p>
        </div>
      </div>
      <div className="form-group mt-4">
        <label htmlFor="data-source-type">Add Data Source</label>
        <select className="form-control" id="data-source-type">
          <option value="">Select Source Type</option>
          <option value="csv">CSV File</option>
          <option value="excel">Excel File</option>
          <option value="json">JSON File</option>
          <option value="database">Database Query</option>
        </select>
      </div>
      <Button variant="primary">Add Data Source</Button>
    </div>
  );
};

// Variables dialog content
const VariablesContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <p className="text-muted">Capture values from the UI into variables for later use.</p>
      <div className="form-group">
        <label htmlFor="variable-name">Variable Name</label>
        <input type="text" className="form-control" id="variable-name" placeholder="e.g., productPrice" />
      </div>
      <div className="form-group">
        <label htmlFor="variable-source">Capture From</label>
        <select className="form-control" id="variable-source">
          <option value="text">Element Text</option>
          <option value="attribute">Element Attribute</option>
          <option value="url">Current URL</option>
          <option value="title">Page Title</option>
          <option value="count">Element Count</option>
        </select>
      </div>
      <div className="form-group">
        <label htmlFor="variable-selector">CSS Selector (if applicable)</label>
        <input type="text" className="form-control" id="variable-selector" placeholder="e.g., .product-price" />
      </div>
      <Button variant="primary">Save Variable</Button>
    </div>
  );
};

// Assertions dialog content
const AssertionsContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <p className="text-muted">Add assertions to verify expected behavior in your test.</p>
      <div className="form-group">
        <label htmlFor="assertion-type">Assertion Type</label>
        <select className="form-control" id="assertion-type">
          <option value="equals">Equals</option>
          <option value="contains">Contains</option>
          <option value="matches">Matches Regex</option>
          <option value="greater">Greater Than</option>
          <option value="less">Less Than</option>
          <option value="exists">Element Exists</option>
          <option value="visible">Element Is Visible</option>
        </select>
      </div>
      <div className="form-group">
        <label htmlFor="assertion-target">Assert Against</label>
        <select className="form-control" id="assertion-target">
          <option value="element">Element Text</option>
          <option value="attribute">Element Attribute</option>
          <option value="url">Current URL</option>
          <option value="title">Page Title</option>
          <option value="count">Element Count</option>
          <option value="variable">Variable Value</option>
        </select>
      </div>
      <div className="form-group">
        <label htmlFor="assertion-selector">CSS Selector (if applicable)</label>
        <input type="text" className="form-control" id="assertion-selector" placeholder="e.g., .product-price" />
      </div>
      <div className="form-group">
        <label htmlFor="assertion-expected">Expected Value</label>
        <input type="text" className="form-control" id="assertion-expected" placeholder="Expected value" />
      </div>
      <Button variant="primary">Save Assertion</Button>
    </div>
  );
};

// Groups dialog content
const GroupsContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <p className="text-muted">Group related steps together for better organization.</p>
      <div className="form-group">
        <label htmlFor="group-name">Group Name</label>
        <input type="text" className="form-control" id="group-name" placeholder="e.g., Login Sequence" />
      </div>
      <div className="form-group">
        <label>Steps to Include in Group:</label>
        <div className="selected-steps-list">
          <p className="text-muted">No steps selected. Please select steps from the event list first.</p>
        </div>
      </div>
      <Button variant="primary">Create Group</Button>
    </div>
  );
};

// Help dialog content
const HelpContent: React.FC = () => {
  return (
    <div className="feature-panel">
      <h5>Advanced Features Guide</h5>
      
      <div className="help-section">
        <h6>Conditional Logic</h6>
        <p>Use conditions to make tests respond to the state of the page. For example:</p>
        <ul>
          <li>Skip login steps if already logged in</li>
          <li>Handle different flows based on error messages</li>
          <li>Adapt to different UI states</li>
        </ul>
      </div>
      
      <div className="help-section">
        <h6>Loops</h6>
        <p>Repeat steps multiple times or iterate through data:</p>
        <ul>
          <li>Fixed count loops (repeat N times)</li>
          <li>While condition loops (repeat while condition is true)</li>
          <li>For-each loops (repeat for each item in a data source)</li>
        </ul>
      </div>
      
      <div className="help-section">
        <h6>Data Sources</h6>
        <p>Use external data to drive your tests:</p>
        <ul>
          <li>CSV files for tabular data</li>
          <li>Excel files for complex structured data</li>
          <li>JSON files for hierarchical data</li>
          <li>Database queries for dynamic data</li>
        </ul>
      </div>
      
      <div className="help-section">
        <h6>Variables</h6>
        <p>Capture values during test execution for later use:</p>
        <ul>
          <li>Extract text from elements</li>
          <li>Store element attributes</li>
          <li>Remember counts, URLs, or titles</li>
          <li>Use in assertions or as input for other steps</li>
        </ul>
      </div>
      
      <div className="help-section">
        <h6>Assertions</h6>
        <p>Verify expected behavior in your tests:</p>
        <ul>
          <li>Verify text content matches expected values</li>
          <li>Check element attributes</li>
          <li>Validate navigation worked correctly</li>
          <li>Ensure elements exist or are visible</li>
        </ul>
      </div>
      
      <div className="help-section">
        <h6>Groups</h6>
        <p>Organize related steps together:</p>
        <ul>
          <li>Create logical sections in your test</li>
          <li>Collapse/expand groups for better readability</li>
          <li>Apply conditions or loops to entire groups</li>
        </ul>
      </div>
    </div>
  );
};

export default FeatureDialog; 