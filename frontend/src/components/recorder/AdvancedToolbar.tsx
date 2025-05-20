import React from 'react';
import Button from '../common/Button';
import { useRecorder } from '../../context/RecorderContext';
import { RecordingStatus } from '../../types/recorder';
import './Recorder.css';

interface AdvancedToolbarProps {
  status: RecordingStatus;
  onFeatureSelect: (feature: string) => void;
}

/**
 * Toolbar for advanced recorder features (conditionals, loops, assertions, etc.)
 */
const AdvancedToolbar: React.FC<AdvancedToolbarProps> = ({
  status,
  onFeatureSelect
}) => {
  const { state } = useRecorder();
  const isActive = status === RecordingStatus.RECORDING || status === RecordingStatus.PAUSED;
  const hasSelectedEvent = !!state.selectedEvent;

  return (
    <div className="advanced-toolbar">
      <div className="advanced-toolbar-section">
        <span className="advanced-toolbar-label">Logic:</span>
        <Button 
          variant="outline"
          size="sm"
          onClick={() => onFeatureSelect('condition')}
          disabled={!isActive || !hasSelectedEvent}
          title={!hasSelectedEvent ? "Select an event first" : "Add conditional logic"}
        >
          <i className="bi bi-code-slash me-1"></i>
          If Condition
        </Button>
        <Button 
          variant="outline"
          size="sm"
          onClick={() => onFeatureSelect('loop')}
          disabled={!isActive || !hasSelectedEvent}
          title={!hasSelectedEvent ? "Select an event first" : "Add loop"}
        >
          <i className="bi bi-arrow-repeat me-1"></i>
          Loop
        </Button>
      </div>

      <div className="advanced-toolbar-section">
        <span className="advanced-toolbar-label">Data:</span>
        <Button 
          variant="outline"
          size="sm"
          onClick={() => onFeatureSelect('dataSources')}
          disabled={!isActive}
          title="Manage data sources"
        >
          <i className="bi bi-database me-1"></i>
          Data Sources
        </Button>
        <Button 
          variant="outline"
          size="sm"
          onClick={() => onFeatureSelect('variables')}
          disabled={!isActive || !hasSelectedEvent}
          title={!hasSelectedEvent ? "Select an event first" : "Bind variables"}
        >
          <i className="bi bi-braces me-1"></i>
          Variables
        </Button>
      </div>

      <div className="advanced-toolbar-section">
        <span className="advanced-toolbar-label">Testing:</span>
        <Button 
          variant="outline"
          size="sm"
          onClick={() => onFeatureSelect('assertions')}
          disabled={!isActive || !hasSelectedEvent}
          title={!hasSelectedEvent ? "Select an event first" : "Add assertions"}
        >
          <i className="bi bi-check-circle me-1"></i>
          Assertions
        </Button>
        <Button 
          variant="outline"
          size="sm"
          onClick={() => onFeatureSelect('groups')}
          disabled={!isActive}
          title="Group steps together"
        >
          <i className="bi bi-collection me-1"></i>
          Groups
        </Button>
      </div>

      <div className="advanced-toolbar-section">
        <Button 
          variant="primary"
          size="sm"
          onClick={() => onFeatureSelect('help')}
        >
          <i className="bi bi-question-circle me-1"></i>
          Help
        </Button>
      </div>
    </div>
  );
};

export default AdvancedToolbar; 