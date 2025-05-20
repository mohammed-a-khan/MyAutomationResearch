import React, { SelectHTMLAttributes, forwardRef } from 'react';
import './Select.css';

export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'size'> {
  label?: string;
  error?: string;
  helperText?: string;
  options: SelectOption[];
  fullWidth?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

const Select = forwardRef<HTMLSelectElement, SelectProps>(({
  label,
  error,
  helperText,
  options,
  fullWidth = false,
  leftIcon,
  rightIcon,
  className = '',
  ...rest
}, ref) => {
  const selectClasses = [
    'select',
    fullWidth ? 'select-full-width' : '',
    error ? 'select-error' : '',
    leftIcon ? 'select-with-left-icon' : '',
    rightIcon ? 'select-with-right-icon' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <div className="select-wrapper">
      {label && (
        <label className="select-label">
          {label}
        </label>
      )}
      <div className="select-container">
        {leftIcon && (
          <span className="select-icon select-icon-left">
            {leftIcon}
          </span>
        )}
        <select
          ref={ref}
          className={selectClasses}
          aria-invalid={!!error}
          aria-describedby={error ? `${rest.id}-error` : undefined}
          {...rest}
        >
          {options.map((option) => (
            <option
              key={option.value}
              value={option.value}
              disabled={option.disabled}
            >
              {option.label}
            </option>
          ))}
        </select>
        {rightIcon && (
          <span className="select-icon select-icon-right">
            {rightIcon}
          </span>
        )}
        <span className="select-arrow" aria-hidden="true">
          <svg
            width="12"
            height="12"
            viewBox="0 0 12 12"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <path
              d="M2.5 4.5L6 8L9.5 4.5"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>
      </div>
      {error && (
        <span id={`${rest.id}-error`} className="select-error-message">
          {error}
        </span>
      )}
      {helperText && !error && (
        <span className="select-helper-text">
          {helperText}
        </span>
      )}
    </div>
  );
});

Select.displayName = 'Select';

export default Select; 