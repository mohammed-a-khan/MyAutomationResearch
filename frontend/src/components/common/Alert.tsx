import React from 'react';
import './Alert.css';

interface AlertProps {
  message: string;
  type: 'success' | 'error' | 'warning' | 'info';
  onClose?: () => void;
}

/**
 * Alert component for notifications and messages
 */
const Alert: React.FC<AlertProps> = ({ 
  message, 
  type, 
  onClose 
}) => {
  return (
    <div className={`alert alert-${type}`}>
      <div className="alert-content">{message}</div>
      {onClose && (
        <button className="alert-close" onClick={onClose} aria-label="Close">
          ×
        </button>
      )}
    </div>
  );
};

export default Alert; 