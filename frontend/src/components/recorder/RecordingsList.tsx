import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import Button from '../common/Button';
import Card from '../common/Card';
import Modal from '../common/Modal';
import Select from '../common/Select';
import './RecordingsList.css';
import { RecordingStatus } from '../../types/recorder';
import { apiClient } from '../../utils/apiClient';
import axios from 'axios';

// Interface for recording session data
interface RecordingSession {
  id: string;
  name: string;
  status: RecordingStatus;
  startTime: string;
  endTime?: string;
  baseUrl: string;
  browser: string;
  eventCount: number;
}

// Types for code generation
interface CodeGenerationRequest {
  framework: string;
  language: string;
  includeComments?: boolean;
  includeAssertions?: boolean;
}

interface GeneratedCode {
  code: string;
  language: string;
  framework: string;
}

/**
 * Recordings List Page - displays all recording sessions with options to view, generate code, and manage
 */
const RecordingsList: React.FC = () => {
  const navigate = useNavigate();
  const [recordings, setRecordings] = useState<RecordingSession[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Modal states
  const [codeModal, setCodeModal] = useState<boolean>(false);
  const [selectedRecording, setSelectedRecording] = useState<RecordingSession | null>(null);
  const [generatedCode, setGeneratedCode] = useState<string>('');
  const [codeLoading, setCodeLoading] = useState<boolean>(false);
  const [deleteModal, setDeleteModal] = useState<boolean>(false);
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);

  // Code generation options
  const [codeOptions, setCodeOptions] = useState<CodeGenerationRequest>({
    framework: 'selenium-java',
    language: 'java',
    includeComments: true,
    includeAssertions: true
  });

  // Fetch recordings on component mount
  useEffect(() => {
    fetchRecordings();
  }, []);

  // Fetch all recordings
  const fetchRecordings = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.get('/api/recorder/recordings');
      setRecordings(response.data);
    } catch (err) {
      setError(`Error loading recordings: ${err instanceof Error ? err.message : String(err)}`);
      console.error('Error fetching recordings:', err);
    } finally {
      setLoading(false);
    }
  };

  // Generate code for a recording
  const generateCode = useCallback(async (recordingId: string, options: CodeGenerationRequest) => {
    setCodeLoading(true);

    try {
      const response = await axios.post(`/api/recorder/generate-code/${recordingId}`, options);
      setGeneratedCode(response.data.code || '');
    } catch (err) {
      console.error('Error generating code:', err);
      setGeneratedCode(`// Error generating code: ${err instanceof Error ? err.message : String(err)}`);
    } finally {
      setCodeLoading(false);
    }
  }, []);

  // Delete a recording
  const deleteRecording = async (recordingId: string) => {
    setDeleteLoading(true);

    try {
      await axios.delete(`/api/recorder/recordings/${recordingId}`);

      // Refresh recordings list
      await fetchRecordings();
      setDeleteModal(false);
    } catch (err) {
      setError(`Error deleting recording: ${err instanceof Error ? err.message : String(err)}`);
      console.error('Error deleting recording:', err);
    } finally {
      setDeleteLoading(false);
    }
  };

  // Handle copying code to clipboard
  const handleCopyCode = () => {
    navigator.clipboard.writeText(generatedCode)
        .then(() => {
          // Create a temporary element to show success message
          const successMsg = document.createElement('div');
          successMsg.className = 'copy-success-message';
          successMsg.textContent = 'Code copied to clipboard!';
          document.body.appendChild(successMsg);

          // Remove after 2 seconds
          setTimeout(() => {
            document.body.removeChild(successMsg);
          }, 2000);
        })
        .catch(err => {
          console.error('Failed to copy code:', err);
        });
  };

  // Handle downloading code
  const handleDownloadCode = () => {
    // Create language extension based on selected language
    const extension = codeOptions.language === 'java' ? '.java'
        : codeOptions.language === 'python' ? '.py'
            : codeOptions.language === 'typescript' ? '.ts'
                : '.txt';

    const filename = `test_${selectedRecording?.id.substring(0, 8)}${extension}`;
    const blob = new Blob([generatedCode], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);

    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();

    // Clean up
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };

  // Handle changing code generation options
  const handleCodeOptionChange = (field: keyof CodeGenerationRequest, value: any) => {
    setCodeOptions(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Format timestamp for display
  const formatTimestamp = (timestamp: string): string => {
    return new Date(timestamp).toLocaleString();
  };

  // Get badge class based on recording status
  const getStatusBadgeClass = (status: RecordingStatus): string => {
    switch (status) {
      case RecordingStatus.RECORDING:
        return 'badge-recording';
      case RecordingStatus.PAUSED:
        return 'badge-paused';
      case RecordingStatus.COMPLETED:
        return 'badge-completed';
      case RecordingStatus.ERROR:
        return 'badge-error';
      default:
        return 'badge-default';
    }
  };

  // Show code generation modal
  const showGenerateCodeModal = (recording: RecordingSession) => {
    setSelectedRecording(recording);
    setGeneratedCode('');
    setCodeModal(true);

    // Generate code automatically with default options
    generateCode(recording.id, codeOptions);
  };

  // Show delete confirmation modal
  const showDeleteModal = (recording: RecordingSession) => {
    setSelectedRecording(recording);
    setDeleteModal(true);
  };

  // Handle regenerate button
  const handleRegenerateCode = () => {
    if (selectedRecording) {
      generateCode(selectedRecording.id, codeOptions);
    }
  };

  // Framework options
  const frameworkOptions = [
    { value: 'selenium-java', label: 'Selenium Java' },
    { value: 'selenium-python', label: 'Selenium Python' },
    { value: 'playwright-typescript', label: 'Playwright TypeScript' },
    { value: 'playwright-javascript', label: 'Playwright JavaScript' },
    { value: 'cypress', label: 'Cypress' }
  ];

  // Language options
  const languageOptions = [
    { value: 'java', label: 'Java' },
    { value: 'python', label: 'Python' },
    { value: 'typescript', label: 'TypeScript' },
    { value: 'javascript', label: 'JavaScript' }
  ];

  // Filter language options based on selected framework
  const filteredLanguageOptions = (() => {
    const framework = codeOptions.framework;

    if (framework === 'selenium-java') {
      return languageOptions.filter(opt => opt.value === 'java');
    } else if (framework === 'selenium-python') {
      return languageOptions.filter(opt => opt.value === 'python');
    } else if (framework === 'playwright-typescript') {
      return languageOptions.filter(opt => opt.value === 'typescript');
    } else if (framework === 'playwright-javascript') {
      return languageOptions.filter(opt => opt.value === 'javascript');
    } else if (framework === 'cypress') {
      return languageOptions.filter(opt => opt.value === 'javascript' || opt.value === 'typescript');
    }

    return languageOptions;
  })();

  return (
      <div className="recordings-list-container">
        <div className="recordings-list-header">
          <h1>Recorded Test Sessions</h1>
          <Button
              variant="primary"
              onClick={() => navigate('/recorder/start')}
          >
            <i className="bi bi-record-circle me-2"></i>
            Start New Recording
          </Button>
        </div>

        {/* Error message if any */}
        {error && (
            <div className="alert alert-danger">
              {error}
              <Button
                  variant="outline"
                  size="sm"
                  className="ms-2 float-end"
                  onClick={() => setError(null)}
              >
                Dismiss
              </Button>
            </div>
        )}

        {/* Loading state */}
        {loading && (
            <div className="recordings-loading">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
              <p>Loading recordings...</p>
            </div>
        )}

        {/* Empty state */}
        {!loading && recordings.length === 0 && (
            <div className="recordings-empty-state">
              <div className="recordings-empty-icon">
                <i className="bi bi-collection"></i>
              </div>
              <h3>No Recordings Found</h3>
              <p>Start a new recording session to create your first test</p>
              <Button
                  variant="primary"
                  onClick={() => navigate('/recorder/start')}
              >
                Start Recording
              </Button>
            </div>
        )}

        {/* Recordings list */}
        {!loading && recordings.length > 0 && (
            <div className="recordings-grid">
              {recordings.map(recording => (
                  <Card key={recording.id} className="recording-card">
                    <div className="recording-card-header">
                      <h3 className="recording-card-title">{recording.name}</h3>
                      <span className={`recording-status-badge ${getStatusBadgeClass(recording.status)}`}>
                  {recording.status}
                </span>
                    </div>

                    <div className="recording-card-info">
                      <div className="info-item">
                        <span className="info-label">Started:</span>
                        <span className="info-value">{formatTimestamp(recording.startTime)}</span>
                      </div>

                      {recording.endTime && (
                          <div className="info-item">
                            <span className="info-label">Ended:</span>
                            <span className="info-value">{formatTimestamp(recording.endTime)}</span>
                          </div>
                      )}

                      <div className="info-item">
                        <span className="info-label">Browser:</span>
                        <span className="info-value">{recording.browser}</span>
                      </div>

                      <div className="info-item">
                        <span className="info-label">URL:</span>
                        <span className="info-value url-value">{recording.baseUrl}</span>
                      </div>

                      <div className="info-item">
                        <span className="info-label">Events:</span>
                        <span className="info-value">{recording.eventCount}</span>
                      </div>
                    </div>

                    <div className="recording-card-actions">
                      <Button
                          variant="outline"
                          onClick={() => navigate(`/recorder/view/${recording.id}`)}
                      >
                        <i className="bi bi-eye me-1"></i>
                        View Details
                      </Button>

                      <Button
                          variant="primary"
                          onClick={() => showGenerateCodeModal(recording)}
                      >
                        <i className="bi bi-code-slash me-1"></i>
                        Generate Code
                      </Button>

                      <Button
                          variant="danger"
                          onClick={() => showDeleteModal(recording)}
                      >
                        <i className="bi bi-trash me-1"></i>
                        Delete
                      </Button>
                    </div>
                  </Card>
              ))}
            </div>
        )}

        {/* Generate Code Modal */}
        <Modal
            isOpen={codeModal}
            title="Generate Test Code"
            onClose={() => setCodeModal(false)}
            size="lg"
        >
          <div className="code-generation-modal">
            <div className="code-options">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="framework">Framework</label>
                  <Select
                      id="framework"
                      value={codeOptions.framework}
                      onChange={(e) => handleCodeOptionChange('framework', e.target.value)}
                      options={frameworkOptions}
                  />
                </div>

                <div className="form-group">
                  <label htmlFor="language">Language</label>
                  <Select
                      id="language"
                      value={codeOptions.language}
                      onChange={(e) => handleCodeOptionChange('language', e.target.value)}
                      options={filteredLanguageOptions}
                      disabled={filteredLanguageOptions.length <= 1}
                  />
                </div>
              </div>

              <div className="form-row checkbox-row">
                <div className="form-check">
                  <input
                      type="checkbox"
                      className="form-check-input"
                      id="includeComments"
                      checked={codeOptions.includeComments}
                      onChange={(e) => handleCodeOptionChange('includeComments', e.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="includeComments">
                    Include Comments
                  </label>
                </div>

                <div className="form-check">
                  <input
                      type="checkbox"
                      className="form-check-input"
                      id="includeAssertions"
                      checked={codeOptions.includeAssertions}
                      onChange={(e) => handleCodeOptionChange('includeAssertions', e.target.checked)}
                  />
                  <label className="form-check-label" htmlFor="includeAssertions">
                    Include Assertions
                  </label>
                </div>
              </div>

              <Button
                  onClick={handleRegenerateCode}
                  disabled={codeLoading}
                  variant="primary"
                  className="regenerate-button"
              >
                {codeLoading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Generating...
                    </>
                ) : (
                    <>
                      <i className="bi bi-arrow-repeat me-1"></i>
                      Regenerate Code
                    </>
                )}
              </Button>
            </div>

            <div className="code-preview-container">
              {codeLoading ? (
                  <div className="code-loading">
                    <div className="spinner-border text-primary" role="status">
                      <span className="visually-hidden">Generating code...</span>
                    </div>
                    <p>Generating code...</p>
                  </div>
              ) : (
                  <pre className="code-preview">
                <code>{generatedCode}</code>
              </pre>
              )}
            </div>

            <div className="code-actions">
              <Button
                  variant="outline"
                  onClick={handleCopyCode}
                  disabled={!generatedCode || codeLoading}
              >
                <i className="bi bi-clipboard me-1"></i>
                Copy to Clipboard
              </Button>

              <Button
                  variant="outline"
                  onClick={handleDownloadCode}
                  disabled={!generatedCode || codeLoading}
              >
                <i className="bi bi-download me-1"></i>
                Download
              </Button>
            </div>
          </div>
        </Modal>

        {/* Delete Confirmation Modal */}
        <Modal
            isOpen={deleteModal}
            title="Confirm Delete"
            onClose={() => setDeleteModal(false)}
            size="sm"
        >
          <div className="delete-confirmation">
            <p>Are you sure you want to delete this recording?</p>
            <p><strong>{selectedRecording?.name}</strong></p>
            <p className="text-danger">This action cannot be undone.</p>

            <div className="delete-actions">
              <Button
                  variant="secondary"
                  onClick={() => setDeleteModal(false)}
                  disabled={deleteLoading}
              >
                Cancel
              </Button>

              <Button
                  variant="danger"
                  onClick={() => selectedRecording && deleteRecording(selectedRecording.id)}
                  disabled={deleteLoading}
              >
                {deleteLoading ? (
                    <>
                      <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                      Deleting...
                    </>
                ) : (
                    <>
                      <i className="bi bi-trash me-1"></i>
                      Delete Recording
                    </>
                )}
              </Button>
            </div>
          </div>
        </Modal>
      </div>
  );
};

export default RecordingsList;