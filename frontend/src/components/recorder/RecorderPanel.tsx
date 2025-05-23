import React, { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useRecorder } from '../../context/RecorderContext';
import { useProjects } from '../../context/ProjectContext';
import {
  RecordingOptions,
  RecordingStatus,
  RecordedEvent,
  RecordedEventType
} from '../../types/recorder';
import wsService, { ConnectionStatus } from '../../services/wsService';
import './Recorder.css';

// Subcomponents
import EnhancedRecorderToolbar from './EnhancedRecorderToolbar';
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
  const { projectId: urlProjectId, recordingId } = useParams<{ projectId: string, recordingId: string }>();
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
      isLoading,
      connectionStatus
    },
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    addEvent,
    selectEvent,
    generateCode,
    resetState,
    reconnectWebSocket,
    checkRecorderStatus
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

  // Check recorder status on mount and when needed
  useEffect(() => {
    if (session?.id || recordingId) {
      checkRecorderStatus().catch(console.error);
    }
  }, [session?.id, recordingId, checkRecorderStatus]);

  // Add useCallback for reconnection logic
  const handleReconnect = useCallback(async () => {
    try {
      await reconnectWebSocket();
      // Check recorder status after reconnection
      if (session?.id) {
        await checkRecorderStatus();
      }
    } catch (error) {
      console.error('Reconnection failed:', error);
    }
  }, [reconnectWebSocket, checkRecorderStatus, session?.id]);

  // Enhanced useEffect for connection monitoring
  useEffect(() => {
    if (status === RecordingStatus.RECORDING &&
        connectionStatus !== ConnectionStatus.CONNECTED &&
        connectionStatus !== ConnectionStatus.CONNECTING &&
        session?.id) {
      console.debug('Detected connection issues during recording, attempting reconnection');

      // Add delay before reconnection attempt to avoid rapid reconnection loops
      const reconnectTimer = setTimeout(() => {
        handleReconnect();
      }, 2000);

      return () => clearTimeout(reconnectTimer);
    }
  }, [status, connectionStatus, session?.id, handleReconnect]);

  // Clean up on unmount
  useEffect(() => {
    return () => {
      resetState();
    };
  }, [resetState]);

  // Function to clear error message
  const clearError = () => {
    // We can't directly access the dispatch function here,
    // but we can reset the recorder state partially
    if (error) {
      resetState();
      // If we had a session, we need to reload it
      if (session?.id) {
        checkRecorderStatus().catch(console.error);
      }
    }
  };

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

  // Navigate to recordings list
  const handleViewRecordings = () => {
    navigate('/recordings');
  };

  // Render toolbar based on recording status
  const renderToolbar = () => {
    // Always render the toolbar when a session exists, regardless of status
    if (session) {
      return (
          <EnhancedRecorderToolbar
              status={status}
              onStop={stopRecording}
              onPause={pauseRecording}
              onResume={resumeRecording}
              onGenerateCode={handleGenerateCode}
              onViewRecordings={handleViewRecordings}
              disabled={isLoading}
              sessionId={session.id}
          />
      );
    }

    // If no session, show start button
    return (
        <div className="recorder-start-prompt">
          <Button
              variant="primary"
              size="lg"
              onClick={() => setShowStartDialog(true)}
              disabled={isLoading}
          >
            <i className="bi bi-record-circle me-2"></i>
            Start Recording
          </Button>
        </div>
    );
  };

  // Enhanced connection status indicator with detailed information
  const renderConnectionStatus = () => {
    if (!session) return null;

    let statusLabel = '';
    let statusClass = '';
    let statusIcon = '';

    switch (connectionStatus) {
      case ConnectionStatus.CONNECTED:
        statusLabel = 'Connected';
        statusClass = 'status-connected';
        statusIcon = 'bi-wifi';
        break;
      case ConnectionStatus.CONNECTING:
        statusLabel = 'Connecting...';
        statusClass = 'status-connecting';
        statusIcon = 'bi-arrow-repeat spin';
        break;
      case ConnectionStatus.DISCONNECTED:
        statusLabel = 'Disconnected';
        statusClass = 'status-disconnected';
        statusIcon = 'bi-wifi-off';
        break;
      case ConnectionStatus.ERROR:
        statusLabel = 'Connection Error';
        statusClass = 'status-error';
        statusIcon = 'bi-exclamation-triangle';
        break;
    }

    return (
        <div className={`connection-status ${statusClass}`}>
          <i className={`bi ${statusIcon}`}></i>
          <span>{statusLabel}</span>
          {(connectionStatus === ConnectionStatus.DISCONNECTED ||
              connectionStatus === ConnectionStatus.ERROR) && (
              <Button
                  variant="text"
                  size="sm"
                  onClick={handleReconnect}
                  className="reconnect-button"
              >
                <i className="bi bi-arrow-repeat me-1"></i>
                Reconnect
              </Button>
          )}
        </div>
    );
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
              {renderToolbar()}
              <div className="toolbar-actions">
                {renderConnectionStatus()}
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
                  onSelectEvent={(event) => selectEvent(event)}
                  onAddEvent={(partialEvent) => {
                    const event: RecordedEvent = {
                      id: `custom-${Date.now()}`,
                      type: partialEvent.type || RecordedEventType.CUSTOM,
                      timestamp: partialEvent.timestamp || Date.now(),
                      url: partialEvent.url || window.location.href,
                      order: partialEvent.order || events.length + 1,
                      ...partialEvent
                    };
                    addEvent(event);
                    return Promise.resolve();
                  }}
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
                <div className="alert alert-danger">
                  {error}
                  <Button
                      variant="outline"
                      size="sm"
                      className="ms-2 text-danger"
                      onClick={clearError}
                  >
                    Dismiss
                  </Button>
                </div>
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