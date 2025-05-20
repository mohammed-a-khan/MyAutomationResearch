import React, { InputHTMLAttributes, TextareaHTMLAttributes, forwardRef } from 'react';
import './Input.css';

type InputType = 'text' | 'password' | 'email' | 'number' | 'tel' | 'url' | 'textarea' | string;

export interface InputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
  type?: InputType;
  rows?: number;
}

const Input = forwardRef<HTMLInputElement | HTMLTextAreaElement, InputProps>(({
  label,
  error,
  helperText,
  fullWidth = false,
  leftIcon,
  rightIcon,
  className = '',
  type = 'text',
  rows = 3,
  ...rest
}, ref) => {
  const inputClasses = [
    'input',
    fullWidth ? 'input-full-width' : '',
    error ? 'input-error' : '',
    leftIcon ? 'input-with-left-icon' : '',
    rightIcon ? 'input-with-right-icon' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <div className="input-wrapper">
      {label && (
        <label className="input-label">
          {label}
        </label>
      )}
      <div className="input-container">
        {leftIcon && (
          <span className="input-icon input-icon-left">
            {leftIcon}
          </span>
        )}
        {type === 'textarea' ? (
          <textarea
            ref={ref as React.Ref<HTMLTextAreaElement>}
            className={inputClasses}
            aria-invalid={!!error}
            aria-describedby={error ? `${rest.id}-error` : undefined}
            rows={rows}
            {...(rest as unknown as TextareaHTMLAttributes<HTMLTextAreaElement>)}
          />
        ) : (
          <input
            ref={ref as React.Ref<HTMLInputElement>}
            className={inputClasses}
            type={type}
            aria-invalid={!!error}
            aria-describedby={error ? `${rest.id}-error` : undefined}
            {...rest}
          />
        )}
        {rightIcon && (
          <span className="input-icon input-icon-right">
            {rightIcon}
          </span>
        )}
      </div>
      {error && (
        <span id={`${rest.id}-error`} className="input-error-message">
          {error}
        </span>
      )}
      {helperText && !error && (
        <span className="input-helper-text">
          {helperText}
        </span>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input; 