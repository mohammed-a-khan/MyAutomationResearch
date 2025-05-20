import React from 'react';
import { Variable, VariableScope } from '../../types/codebuilder';
import Button from '../common/Button';

interface VariableItemProps {
  variable: Variable;
  isSelected: boolean;
  onClick: () => void;
  onDelete: () => void;
}

/**
 * VariableItem component for displaying a single variable in the variable list
 */
const VariableItem: React.FC<VariableItemProps> = ({ 
  variable, 
  isSelected, 
  onClick, 
  onDelete 
}) => {
  // Get scope display name
  const getScopeLabel = (scope: VariableScope) => {
    switch (scope) {
      case VariableScope.GLOBAL:
        return 'Global';
      case VariableScope.PROJECT:
        return 'Project';
      case VariableScope.TEST:
        return 'Test';
      case VariableScope.STEP:
        return 'Step';
      default:
        return scope;
    }
  };
  
  // Format variable value for display (truncate if too long)
  const formatValue = (value: string) => {
    if (value.length > 30) {
      return `${value.substring(0, 30)}...`;
    }
    return value;
  };
  
  return (
    <div 
      className={`variable-item ${isSelected ? 'selected' : ''}`}
      onClick={onClick}
      data-testid={`variable-item-${variable.id}`}
    >
      <div className="variable-item-name">
        {variable.name}
        <span className="variable-item-scope">
          {getScopeLabel(variable.scope)}
        </span>
      </div>
      
      <div className="variable-item-value">
        {formatValue(variable.value)}
      </div>
      
      <div className="d-flex align-items-center justify-content-between">
        <div className="variable-item-type">
          {variable.type}
        </div>
        
        {isSelected && (
          <Button 
            variant="outline"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              onDelete();
            }}
          >
            <i className="bi bi-trash"></i>
          </Button>
        )}
      </div>
    </div>
  );
};

export default VariableItem; 