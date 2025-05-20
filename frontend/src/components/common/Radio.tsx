import React, { InputHTMLAttributes, forwardRef } from 'react';
import './Radio.css';

export interface RadioProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
}

const Radio = forwardRef<HTMLInputElement, RadioProps>(({
  label,
  error,
  helperText,
  fullWidth = false,
  className = '',
  ...rest
}, ref) => {
  const radioClasses = [
    'radio',
    fullWidth ? 'radio-full-width' : '',
    error ? 'radio-error' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <div className="radio-wrapper">
      <label className="radio-label">
        <input
          ref={ref}
          type="radio"
          className={radioClasses}
          aria-invalid={!!error}
          aria-describedby={error ? `${rest.id}-error` : undefined}
          {...rest}
        />
        <span className="radio-custom"></span>
        {label && <span className="radio-text">{label}</span>}
      </label>
      {error && (
        <span id={`${rest.id}-error`} className="radio-error-message">
          {error}
        </span>
      )}
      {helperText && !error && (
        <span className="radio-helper-text">
          {helperText}
        </span>
      )}
    </div>
  );
});

Radio.displayName = 'Radio';

export default Radio; 