import React from 'react';
import './Spinner.css';

interface SpinnerProps {
  text?: string;
  size?: 'small' | 'medium' | 'large';
}

/**
 * Spinner component for loading states
 */
const Spinner: React.FC<SpinnerProps> = ({ 
  text = 'Loading...',
  size = 'medium'
}) => {
  return (
    <div className={`spinner-container size-${size}`}>
      <div className={`spinner size-${size}`}></div>
      {text && <div className="spinner-text">{text}</div>}
    </div>
  );
};

export default Spinner;
