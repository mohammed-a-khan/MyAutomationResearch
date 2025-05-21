import React, { useState, useEffect } from 'react';
import { RecordingStatus } from '../../types/recorder';
import Button from '../common/Button';
import './EnhancedRecorderToolbar.css';

interface EnhancedRecorderToolbarProps {
  status: RecordingStatus;
  onStop: () => void;
  onPause: () => void;
  onResume: () => void;
  onGenerateCode: () => void;
  onViewRecordings: () => void;
  disabled: boolean;
  sessionId?: string;
}

/**
 * Enhanced toolbar for recorder with improved visibility and usability
 */
const EnhancedRecorderToolbar: React.FC<EnhancedRecorderToolbarProps> = ({
                                                                           status,
                                                                           onStop,
                                                                           onPause,
                                                                           onResume,
                                                                           onGenerateCode,
                                                                           onViewRecordings,
                                                                           disabled,
                                                                           sessionId
                                                                         }) => {
  const isRecording = status === RecordingStatus.RECORDING;
  const isPaused = status === RecordingStatus.PAUSED;
  const isCompleted = status === RecordingStatus.COMPLETED;
  const [pulseStop, setPulseStop] = useState<boolean>(false);
  const [elapsedTime, setElapsedTime] = useState<number>(0);

  // Add pulsing animation to the stop button to draw attention
  useEffect(() => {
    if (isRecording) {
      // After 10 seconds of recording, start pulsing the stop button
      // This helps users notice it if they've been recording for a while
      const timer = setTimeout(() => {
        setPulseStop(true);
      }, 10000);

      return () => clearTimeout(timer);
    } else {
      setPulseStop(false);
    }
  }, [isRecording]);

  // Timer for recording duration
  useEffect(() => {
    let interval: NodeJS.Timeout | null = null;

    if (isRecording) {
      interval = setInterval(() => {
        setElapsedTime(prev => prev + 1);
      }, 1000);
    }

    return () => {
      if (interval) clearInterval(interval);
    };
  }, [isRecording]);

  // Format time as MM:SS
  const formatTime = (seconds: number): string => {
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  return (
      <div className="enhanced-recorder-toolbar">
        <div className="recorder-status-container">
          <div className="recorder-status">
          <span
              className={`status-indicator ${
                  isRecording ? 'status-recording' :
                      isPaused ? 'status-paused' :
                          status === RecordingStatus.ERROR ? 'status-error' :
                              isCompleted ? 'status-completed' :
                                  'status-idle'
              }`}
          ></span>
            <span className="status-text">
            {isRecording ? 'Recording in Progress' :
                isPaused ? 'Recording Paused' :
                    status === RecordingStatus.INITIALIZING ? 'Starting Recording...' :
                        status === RecordingStatus.STOPPING ? 'Stopping Recording...' :
                            isCompleted ? 'Recording Completed' :
                                status === RecordingStatus.ERROR ? 'Recording Error' :
                                    'Not Recording'}
          </span>
          </div>

          {/* Timer display when recording */}
          {(isRecording || isPaused) && (
              <div className={`recording-timer ${isPaused ? 'paused' : ''}`}>
                <i className="bi bi-clock me-1"></i>
                {formatTime(elapsedTime)}
              </div>
          )}

          {/* Session ID if available */}
          {sessionId && (
              <div className="session-id">
                <span className="session-id-label">Session:</span>
                <span className="session-id-value">{sessionId.substring(0, 8)}</span>
              </div>
          )}
        </div>

        <div className="recorder-actions">
          {/* View Recordings button - always visible */}
          <Button
              variant="outline"
              onClick={onViewRecordings}
              disabled={disabled}
              className="view-recordings-button"
              title="View all recordings"
          >
            <i className="bi bi-collection me-1"></i>
            View Recordings
          </Button>

          {/* Conditional controls based on recording state */}
          <div className="recording-controls">
            {isRecording && (
                <Button
                    variant="outline"
                    onClick={onPause}
                    disabled={disabled}
                    className="pause-button"
                    title="Pause current recording"
                >
                  <i className="bi bi-pause-fill me-1"></i>
                  Pause
                </Button>
            )}

            {isPaused && (
                <Button
                    variant="outline"
                    onClick={onResume}
                    disabled={disabled}
                    className="resume-button"
                    title="Resume current recording"
                >
                  <i className="bi bi-play-fill me-1"></i>
                  Resume
                </Button>
            )}

            {(isRecording || isPaused) && (
                <Button
                    variant="danger"
                    onClick={onStop}
                    disabled={disabled}
                    className={`stop-button ${pulseStop ? 'pulse-animation' : ''}`}
                    title="Stop the recording"
                >
                  <i className="bi bi-stop-fill me-1"></i>
                  <strong>Stop Recording</strong>
                </Button>
            )}

            <Button
                variant="primary"
                onClick={onGenerateCode}
                disabled={disabled || (!isCompleted && !isPaused)}
                className="generate-code-button"
                title={(!isCompleted && !isPaused) ?
                    "Stop or pause recording first to generate code" :
                    "Generate test code from recording"}
            >
              <i className="bi bi-code-slash me-1"></i>
              Generate Code
            </Button>
          </div>
        </div>
      </div>
  );
};

export default EnhancedRecorderToolbar;