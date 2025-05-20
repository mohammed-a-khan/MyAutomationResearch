import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useRecorder } from '../../context/RecorderContext';
import { useProjects } from '../../context/ProjectContext';
import { RecordingOptions, RecordingStatus } from '../../types/recorder';
import wsService, { ConnectionStatus } from '../../services/wsService';
import './Recorder.css';

// Subcomponents
import RecorderToolbar from './RecorderToolbar';
import RecorderForm from './RecorderForm';
import EventList from './EventList';
import ElementInspector from './ElementInspector';
import CodePreview from './CodePreview';
import AdvancedToolbar from './AdvancedToolbar';
import FeatureDialog from './FeatureDialog';

// UI components
import Modal from '../common/Modal';
import Button from '../common/Button';
import Select from '../common/Select';
import Card from '../common/Card';
import Tabs from '../common/Tabs';
import Dropdown from '../common/Dropdown';

interface RecorderPanelProps {
  projectId?: string;
}

/**
 * Main panel for the recorder module.
 * Integrates all recorder subcomponents and handles state coordination.
 */
const RecorderPanel: React.FC<RecorderPanelProps> = ({ projectId }) => {
  // URL params
  const { projectId: urlProjectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const activeProjectId = projectId || urlProjectId;

  // Get recorder context
  const {
    state: {
      session,
      status,
      events,
      selectedEvent,
      inspectedElement,
      generatedCode,
      error,
      isLoading
    },
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    addEvent,
    selectEvent,
    generateCode,
    resetState
  } = useRecorder();

  // Get projects context
  const {
    projects,
    loadProjects,
    isLoading: projectsLoading
  } = useProjects();

  // Local state
  const [showStartDialog, setShowStartDialog] = useState<boolean>(false);
  const [showCodePreview, setShowCodePreview] = useState<boolean>(false);
  const [selectedProjectId, setSelectedProjectId] = useState<string>(activeProjectId || '');
  const [showAdvancedToolbar, setShowAdvancedToolbar] = useState<boolean>(false);
  const [advancedFeature, setAdvancedFeature] = useState<string | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<'connected' | 'connecting' | 'disconnected'>('disconnected');

  // Load projects when component mounts
  useEffect(() => {
    loadProjects();
  }, [loadProjects]);

  // Set selected project ID from props or params
  useEffect(() => {
    if (activeProjectId) {
      setSelectedProjectId(activeProjectId);
    }
  }, [activeProjectId]);

  // Update connection status when WebSocket status changes
  useEffect(() => {
    const handleConnectionStatus = (status: ConnectionStatus) => {
      if (status === ConnectionStatus.CONNECTED) {
        setConnectionStatus('connected');
      } else if (status === ConnectionStatus.CONNECTING) {
        setConnectionStatus('connecting');
      } else {
        setConnectionStatus('disconnected');
      }
    };

    // Subscribe to WebSocket status
    const unsubscribe = wsService.subscribeToStatus(handleConnectionStatus);
    
    // Initial connection attempt if needed
    if (session) {
      wsService.connect(session.id)
        .catch(error => {
          console.error('Failed to connect to WebSocket:', error);
        });
    }
    
    return () => {
      unsubscribe();
    };
  }, [session]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      resetState();
    };
  }, [resetState]);

  // Default recording options
  const defaultOptions: RecordingOptions = {
    projectId: selectedProjectId || '',
    browser: 'chrome',
    viewport: {
      width: 1280,
      height: 800
    },
    baseUrl: '',
    targetUrl: '',
    recordCss: true,
    recordNetwork: false,
    generateSmartLocators: true,
    includeAssertions: true,
    headless: false,
    captureScreenshots: true,
    framework: 'selenium_java_testng' // Default framework
  };

  // Handle recording start
  const handleStartRecording = async (options: RecordingOptions) => {
    await startRecording(options);
    setShowStartDialog(false);
  };

  // Handle project selection
  const handleProjectChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const newProjectId = e.target.value;
    setSelectedProjectId(newProjectId);

    // Navigate to the project's recorder page if a project is selected
    if (newProjectId) {
      navigate(`/recorder/${newProjectId}`);
    } else {
      navigate('/recorder'); // Navigate to base recorder route if no project selected
    }
  };

  // Handle code generation
  const handleGenerateCode = async () => {
    await generateCode({
      language: 'typescript',
      framework: 'playwright',
      includeComments: true,
      includeImports: true,
      includePageObjects: false,
      useTypeScript: true,
      assertion: 'auto'
    });
    setShowCodePreview(true);
  };

  // Handle advanced feature selection
  const handleAdvancedFeatureSelect = (feature: string) => {
    setAdvancedFeature(feature);
  };

  // Render based on recording state
  const renderContent = () => {
    if (!session && status === RecordingStatus.IDLE) {
      return (
        <div className="recorder-empty-state">
          <div className="recorder-empty-state-content">
            <h2>Test Recorder</h2>
            <p>Record user interactions and generate automated tests</p>

            {/* Project Selection Dropdown */}
            <div className="project-selector mb-4">
              <label htmlFor="project-select" className="form-label">Select a Project</label>
              {projectsLoading ? (
                <div className="text-center">
                  <div className="spinner-border spinner-border-sm text-primary" role="status">
                    <span className="visually-hidden">Loading projects...</span>
                  </div>
                  <p className="mt-2">Loading projects...</p>
                </div>
              ) : (
                <Select
                  id="project-select"
                  value={selectedProjectId || ''}
                  onChange={handleProjectChange}
                  options={[
                    { value: '', label: '-- Select a Project --' },
                    ...projects.map(project => ({
                      value: project.id,
                      label: project.name
                    }))
                  ]}
                />
              )}

              {/* Link to create project if none exist */}
              {!projectsLoading && projects.length === 0 && (
                <div className="mt-2">
                  <p>No projects found. <a href="/projects">Create a new project</a> first.</p>
                </div>
              )}
            </div>

            <Button
              variant="primary"
              onClick={() => setShowStartDialog(true)}
              disabled={!selectedProjectId}
            >
              Start Recording
            </Button>
            {!selectedProjectId && (
              <p className="text-danger mt-3">
                Please select a project to enable recording
              </p>
            )}
          </div>
        </div>
      );
    }

    return (
      <div className="recorder-content">
        <div className="recorder-toolbar-container">
          <div className="recorder-toolbar-main">
            <RecorderToolbar
              status={status}
              onStop={stopRecording}
              onPause={pauseRecording}
              onResume={resumeRecording}
              onGenerateCode={handleGenerateCode}
              disabled={isLoading}
            />
            <div className="toolbar-actions">
              <div className="connection-status">
                <span 
                  className={`connection-indicator ${connectionStatus}`} 
                  title={`WebSocket: ${connectionStatus}`}
                ></span>
                {connectionStatus === 'disconnected' && (
                  <button 
                    className="reconnect-button" 
                    onClick={() => session && wsService.connect(session.id)}
                    title="Reconnect WebSocket"
                  >
                    <i className="bi bi-arrow-repeat"></i>
                  </button>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => setShowAdvancedToolbar(!showAdvancedToolbar)}
                className="advanced-toolbar-toggle"
              >
                <i className={`bi ${showAdvancedToolbar ? 'bi-chevron-up' : 'bi-chevron-down'} me-1`}></i>
                {showAdvancedToolbar ? 'Hide Advanced' : 'Show Advanced'}
              </Button>
            </div>
          </div>

          {showAdvancedToolbar && (
            <AdvancedToolbar 
              status={status}
              onFeatureSelect={handleAdvancedFeatureSelect}
            />
          )}
        </div>

        <div className="recorder-main">
          <div className="recorder-left-panel">
            <EventList
              events={events}
              selectedEvent={selectedEvent}
              onSelectEvent={selectEvent}
              onAddEvent={addEvent}
              disabled={status !== RecordingStatus.RECORDING && status !== RecordingStatus.PAUSED}
              isActive={status === RecordingStatus.RECORDING}
            />
          </div>
          <div className="recorder-right-panel">
            <ElementInspector element={inspectedElement} />
          </div>
        </div>

        {error && (
          <div className="recorder-error-message">
            <div className="alert alert-danger">{error}</div>
          </div>
        )}
      </div>
    );
  };

  return (
      <div className="recorder-panel">
        {renderContent()}

        {/* Start Recording Modal */}
        <Modal
            isOpen={showStartDialog}
            title="Start Recording"
            onClose={() => setShowStartDialog(false)}
            size="lg"
        >
          <RecorderForm
              initialValues={{...defaultOptions, projectId: selectedProjectId || ''}}
              onSubmit={handleStartRecording}
              onCancel={() => setShowStartDialog(false)}
              isLoading={isLoading}
          />
        </Modal>

        {/* Code Preview Modal */}
        <Modal
            isOpen={showCodePreview}
            title="Generated Test Code"
            onClose={() => setShowCodePreview(false)}
            size="xl"
        >
          <CodePreview code={generatedCode || ''} />
          <div className="modal-footer">
            <Button
                variant="secondary"
                onClick={() => setShowCodePreview(false)}
            >
              Close
            </Button>
          </div>
        </Modal>

        {/* Feature Dialog for Advanced Features */}
        <FeatureDialog 
          feature={advancedFeature}
          onClose={() => setAdvancedFeature(null)}
        />
      </div>
  );
};

export default RecorderPanel;