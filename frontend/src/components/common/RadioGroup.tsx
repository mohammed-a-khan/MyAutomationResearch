import React from 'react';
import Radio from './Radio';
import './RadioGroup.css';

export interface RadioOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface RadioGroupProps {
  name: string;
  value?: string;
  onChange?: (value: string) => void;
  label?: string;
  error?: string;
  helperText?: string;
  options: RadioOption[];
  orientation?: 'horizontal' | 'vertical';
  fullWidth?: boolean;
  className?: string;
  inline?: boolean;
}

const RadioGroup: React.FC<RadioGroupProps> = ({
  name,
  value,
  onChange,
  label,
  error,
  helperText,
  options,
  orientation = 'vertical',
  fullWidth = false,
  className = '',
  inline = false
}) => {
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (onChange) {
      onChange(e.target.value);
    }
  };

  const groupClasses = [
    'radio-group',
    `radio-group-${orientation}`,
    fullWidth ? 'radio-group-full-width' : '',
    inline ? 'radio-group-inline' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <div className={groupClasses}>
      {label && (
        <div className="radio-group-label">
          {label}
        </div>
      )}
      <div className="radio-group-options">
        {options.map((option) => (
          <Radio
            key={option.value}
            name={name}
            value={option.value}
            label={option.label}
            checked={value === option.value}
            onChange={handleChange}
            disabled={option.disabled}
            fullWidth={fullWidth}
          />
        ))}
      </div>
      {error && (
        <div className="radio-group-error">
          {error}
        </div>
      )}
      {helperText && !error && (
        <div className="radio-group-helper-text">
          {helperText}
        </div>
      )}
    </div>
  );
};

export default RadioGroup; 