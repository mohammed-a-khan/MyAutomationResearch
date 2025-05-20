import React, { InputHTMLAttributes, forwardRef } from 'react';
import './Checkbox.css';

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
}

const Checkbox = forwardRef<HTMLInputElement, CheckboxProps>(({
  label,
  error,
  helperText,
  fullWidth = false,
  className = '',
  ...rest
}, ref) => {
  const checkboxClasses = [
    'checkbox',
    fullWidth ? 'checkbox-full-width' : '',
    error ? 'checkbox-error' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <div className="checkbox-wrapper">
      <label className="checkbox-label">
        <input
          ref={ref}
          type="checkbox"
          className={checkboxClasses}
          aria-invalid={!!error}
          aria-describedby={error ? `${rest.id}-error` : undefined}
          {...rest}
        />
        <span className="checkbox-custom"></span>
        {label && <span className="checkbox-text">{label}</span>}
      </label>
      {error && (
        <span id={`${rest.id}-error`} className="checkbox-error-message">
          {error}
        </span>
      )}
      {helperText && !error && (
        <span className="checkbox-helper-text">
          {helperText}
        </span>
      )}
    </div>
  );
});

Checkbox.displayName = 'Checkbox';

export default Checkbox; 