import React, { useState, useEffect } from 'react';
import { TestStep } from '../../types/codebuilder';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import Button from '../common/Button';
import StepItem from './StepItem';

interface StepListProps {
  steps: TestStep[];
  isLoading: boolean;
}

/**
 * StepList component displays a list of test steps with sorting functionality
 */
const StepList: React.FC<StepListProps> = ({ steps, isLoading }) => {
  const { selectStep, selectedStep, reorderSteps } = useCodeBuilder();
  
  // State for the sorted steps
  const [sortedSteps, setSortedSteps] = useState<TestStep[]>([]);

  // Update sorted steps when steps prop changes
  useEffect(() => {
    const sorted = [...steps].sort((a, b) => a.order - b.order);
    setSortedSteps(sorted);
  }, [steps]);

  // Handle click on a step
  const handleStepClick = (step: TestStep) => {
    selectStep(step);
  };

  // Handle creating a new step
  const handleCreateStep = () => {
    selectStep(null);
  };

  // Handle step reordering
  const moveStep = async (stepId: string, direction: 'up' | 'down') => {
    const currentIndex = sortedSteps.findIndex(step => step.id === stepId);
    if (currentIndex === -1) return;
    
    // Don't move if at the edges
    if (direction === 'up' && currentIndex === 0) return;
    if (direction === 'down' && currentIndex === sortedSteps.length - 1) return;
    
    const newIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
    
    // Create new array for the state update
    const newSortedSteps = [...sortedSteps];
    const [removed] = newSortedSteps.splice(currentIndex, 1);
    newSortedSteps.splice(newIndex, 0, removed);
    
    // Update local state
    setSortedSteps(newSortedSteps);
    
    // Update the order on the server
    try {
      const stepIds = newSortedSteps.map(step => step.id);
      await reorderSteps(stepIds);
    } catch (error) {
      // If there's an error, revert to the previous order
      setSortedSteps([...steps].sort((a, b) => a.order - b.order));
    }
  };

  return (
    <div className="step-list">
      <div className="step-list-header">
        <h5 className="mb-0">Test Steps</h5>
        <span className="badge bg-primary">{steps.length}</span>
      </div>
      
      <div className="step-list-content">
        {isLoading ? (
          <div className="loading-spinner">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        ) : sortedSteps.length === 0 ? (
          <div className="step-list-empty">
            <p>No steps created yet.</p>
            <p>Click the button below to create your first step.</p>
          </div>
        ) : (
          sortedSteps.map((step) => (
            <StepItem
              key={step.id}
              step={step}
              isSelected={selectedStep?.id === step.id}
              onClick={() => handleStepClick(step)}
              onMoveUp={() => moveStep(step.id, 'up')}
              onMoveDown={() => moveStep(step.id, 'down')}
              canMoveUp={sortedSteps.findIndex(s => s.id === step.id) !== 0}
              canMoveDown={sortedSteps.findIndex(s => s.id === step.id) !== sortedSteps.length - 1}
            />
          ))
        )}
      </div>
      
      <div className="step-list-footer">
        <Button 
          onClick={handleCreateStep}
          fullWidth
          disabled={isLoading}
        >
          <i className="bi bi-plus-circle me-1"></i> New Step
        </Button>
      </div>
    </div>
  );
};

export default StepList; 