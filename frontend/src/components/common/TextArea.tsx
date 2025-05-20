import React from 'react';

interface TextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
}

/**
 * Reusable textarea component for multiline text input
 */
const TextArea: React.FC<TextAreaProps> = ({
  id,
  label,
  className = '',
  ...props
}) => {
  return (
    <>
      {label && <label htmlFor={id} className="form-label">{label}</label>}
      <textarea
        id={id}
        className={`form-control ${className}`}
        {...props}
      />
    </>
  );
};

export default TextArea; 