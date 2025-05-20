import React from 'react';
import { TestStep, StepType } from '../../types/codebuilder';
import Button from '../common/Button';

interface StepItemProps {
  step: TestStep;
  isSelected: boolean;
  onClick: () => void;
  onMoveUp?: () => void;
  onMoveDown?: () => void;
  canMoveUp?: boolean;
  canMoveDown?: boolean;
}

/**
 * StepItem component represents a single test step in the StepList
 */
const StepItem: React.FC<StepItemProps> = ({
  step,
  isSelected,
  onClick,
  onMoveUp,
  onMoveDown,
  canMoveUp = true,
  canMoveDown = true
}) => {
  // Get CSS class for step type
  const getTypeClass = (type: StepType): string => {
    switch (type) {
      case StepType.ACTION:
        return 'action';
      case StepType.ASSERTION:
        return 'assertion';
      case StepType.WAIT:
        return 'wait';
      case StepType.NAVIGATION:
        return 'navigation';
      case StepType.CUSTOM:
        return 'custom';
      default:
        return '';
    }
  };

  return (
    <div 
      className={`step-item ${isSelected ? 'selected' : ''}`} 
      onClick={onClick}
      data-testid={`step-item-${step.id}`}
    >
      <div className="step-item-content">
        <div className="d-flex align-items-center">
          <span className="step-item-name">{step.name}</span>
          <span className={`step-item-type ${getTypeClass(step.type)}`}>
            {step.type.charAt(0) + step.type.slice(1).toLowerCase()}
          </span>
          {step.disabled && (
            <span className="badge bg-secondary ms-2">Disabled</span>
          )}
        </div>
        {step.description && (
          <div className="step-item-description">{step.description}</div>
        )}
      </div>
      
      <div className="step-item-actions">
        <div className="btn-group btn-group-sm">
          {onMoveUp && (
            <Button 
              variant="outline"
              size="sm"
              disabled={!canMoveUp}
              onClick={(e) => {
                e.stopPropagation();
                onMoveUp();
              }}
              title="Move Up"
            >
              <i className="bi bi-arrow-up"></i>
            </Button>
          )}
          
          {onMoveDown && (
            <Button 
              variant="outline"
              size="sm"
              disabled={!canMoveDown}
              onClick={(e) => {
                e.stopPropagation();
                onMoveDown();
              }}
              title="Move Down"
            >
              <i className="bi bi-arrow-down"></i>
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default StepItem; 