import React from 'react';
import { RecordingStatus } from '../../types/recorder';
import Button from '../common/Button';

interface RecorderToolbarProps {
  status: RecordingStatus;
  onStop: () => void;
  onPause: () => void;
  onResume: () => void;
  onGenerateCode: () => void;
  disabled: boolean;
}

/**
 * Toolbar for recorder actions (stop, pause, resume, generate code)
 */
const RecorderToolbar: React.FC<RecorderToolbarProps> = ({
  status,
  onStop,
  onPause,
  onResume,
  onGenerateCode,
  disabled
}) => {
  const isRecording = status === RecordingStatus.RECORDING;
  const isPaused = status === RecordingStatus.PAUSED;

  return (
    <div className="recorder-toolbar">
      <div className="recorder-status">
        <span 
          className={`status-indicator ${
            isRecording ? 'status-recording' :
            isPaused ? 'status-paused' :
            status === RecordingStatus.ERROR ? 'status-error' :
            'status-idle'
          }`}
        ></span>
        <span className="status-text">
          {isRecording ? 'Recording' :
           isPaused ? 'Paused' :
           status === RecordingStatus.INITIALIZING ? 'Starting...' :
           status === RecordingStatus.STOPPING ? 'Stopping...' :
           status === RecordingStatus.COMPLETED ? 'Completed' :
           status === RecordingStatus.ERROR ? 'Error' :
           'Not Recording'}
        </span>
      </div>

      <div className="recorder-actions">
        {isRecording && (
          <Button
            variant="outline"
            onClick={onPause}
            disabled={disabled}
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
          >
            <i className="bi bi-stop-fill me-1"></i>
            Stop
          </Button>
        )}
        
        <Button
          variant="primary"
          onClick={onGenerateCode}
          disabled={disabled || (status !== RecordingStatus.COMPLETED && status !== RecordingStatus.PAUSED)}
        >
          <i className="bi bi-code-slash me-1"></i>
          Generate Code
        </Button>
      </div>
    </div>
  );
};

export default RecorderToolbar; 