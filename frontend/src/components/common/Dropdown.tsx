import React, { useState, useRef, useEffect } from 'react';
import './Dropdown.css';

export interface DropdownOption {
  value: string;
  label: React.ReactNode;
  disabled?: boolean;
}

export interface DropdownProps {
  options: DropdownOption[];
  value?: string | string[];
  placeholder?: string;
  onChange?: (value: string | string[]) => void;
  multiple?: boolean;
  disabled?: boolean;
  error?: string;
  label?: string;
  className?: string;
  fullWidth?: boolean;
  searchable?: boolean;
  maxHeight?: string;
  clearable?: boolean;
}

const Dropdown: React.FC<DropdownProps> = ({
  options,
  value,
  placeholder = 'Select option',
  onChange,
  multiple = false,
  disabled = false,
  error,
  label,
  className = '',
  fullWidth = false,
  searchable = false,
  maxHeight = '250px',
  clearable = false
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchValue, setSearchValue] = useState('');
  const [selectedValues, setSelectedValues] = useState<string[]>(
    multiple && Array.isArray(value) ? value :
    !multiple && typeof value === 'string' ? [value] :
    []
  );
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  
  // Update internal state when external value changes
  useEffect(() => {
    if (multiple && Array.isArray(value)) {
      setSelectedValues(value);
    } else if (!multiple && typeof value === 'string') {
      setSelectedValues([value]);
    }
  }, [value, multiple]);
  
  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
        setSearchValue('');
      }
    };
    
    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);
  
  // Focus search input when dropdown opens
  useEffect(() => {
    if (isOpen && searchable && searchInputRef.current) {
      searchInputRef.current.focus();
    }
  }, [isOpen, searchable]);
  
  // Toggle dropdown
  const toggleDropdown = () => {
    if (!disabled) {
      setIsOpen(prev => !prev);
      if (isOpen) {
        setSearchValue('');
      }
    }
  };
  
  // Handle option select
  const handleSelect = (option: DropdownOption) => {
    if (disabled || option.disabled) return;
    
    if (multiple) {
      const newSelectedValues = selectedValues.includes(option.value)
        ? selectedValues.filter(val => val !== option.value)
        : [...selectedValues, option.value];
      
      setSelectedValues(newSelectedValues);
      onChange?.(newSelectedValues);
    } else {
      setSelectedValues([option.value]);
      onChange?.(option.value);
      setIsOpen(false);
      setSearchValue('');
    }
  };
  
  // Handle search input
  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchValue(e.target.value);
  };
  
  // Filter options by search value
  const filteredOptions = searchValue
    ? options.filter(option => 
        typeof option.label === 'string' 
          ? option.label.toLowerCase().includes(searchValue.toLowerCase())
          : String(option.label).toLowerCase().includes(searchValue.toLowerCase())
      )
    : options;
  
  // Selected option labels for display
  const selectedLabels = options
    .filter(option => selectedValues.includes(option.value))
    .map(option => option.label);
  
  // Get display value
  const displayValue = selectedLabels.length > 0
    ? multiple
      ? `${selectedLabels.length} selected`
      : selectedLabels[0]
    : placeholder;
  
  // Clear selection
  const handleClear = (e: React.MouseEvent) => {
    e.stopPropagation();
    setSelectedValues([]);
    onChange?.(multiple ? [] : '');
  };
  
  // Classes
  const dropdownClasses = [
    'dropdown',
    isOpen ? 'dropdown-open' : '',
    disabled ? 'dropdown-disabled' : '',
    error ? 'dropdown-error' : '',
    fullWidth ? 'dropdown-full-width' : '',
    className
  ].filter(Boolean).join(' ');
  
  return (
    <div className={dropdownClasses} ref={dropdownRef}>
      {label && <label className="dropdown-label">{label}</label>}
      
      <div className="dropdown-control" onClick={toggleDropdown}>
        <div className="dropdown-value">
          {displayValue}
        </div>
        
        <div className="dropdown-indicators">
          {clearable && selectedValues.length > 0 && (
            <button
              type="button"
              className="dropdown-clear-button"
              onClick={handleClear}
              aria-label="Clear selection"
            >
              ×
            </button>
          )}
          <div className="dropdown-arrow">▼</div>
        </div>
      </div>
      
      {isOpen && (
        <div className="dropdown-menu" style={{ maxHeight }}>
          {searchable && (
            <div className="dropdown-search">
              <input
                ref={searchInputRef}
                type="text"
                value={searchValue}
                onChange={handleSearchChange}
                placeholder="Search..."
                className="dropdown-search-input"
                onClick={e => e.stopPropagation()}
              />
            </div>
          )}
          
          {filteredOptions.length > 0 ? (
            <div className="dropdown-options">
              {filteredOptions.map(option => {
                const isSelected = selectedValues.includes(option.value);
                const optionClasses = [
                  'dropdown-option',
                  isSelected ? 'dropdown-option-selected' : '',
                  option.disabled ? 'dropdown-option-disabled' : ''
                ].filter(Boolean).join(' ');
                
                return (
                  <div
                    key={option.value}
                    className={optionClasses}
                    onClick={() => handleSelect(option)}
                  >
                    {multiple && (
                      <div className="dropdown-checkbox">
                        {isSelected && <span className="dropdown-checkbox-icon">✓</span>}
                      </div>
                    )}
                    <div className="dropdown-option-label">{option.label}</div>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="dropdown-no-options">No options found</div>
          )}
        </div>
      )}
      
      {error && <div className="dropdown-error-message">{error}</div>}
    </div>
  );
};

export default Dropdown; 