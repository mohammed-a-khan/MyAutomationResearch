import React, { useState, useEffect } from 'react';
import { Variable, VariableType, VariableScope } from '../../types/codebuilder';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import VariableForm from './VariableForm';
import VariableItem from './VariableItem';

interface VariableManagerProps {
  projectId: string;
  variables: Variable[];
  isLoading: boolean;
}

/**
 * VariableManager component for managing test variables
 */
const VariableManager: React.FC<VariableManagerProps> = ({ 
  projectId, 
  variables, 
  isLoading 
}) => {
  const { deleteVariable } = useCodeBuilder();
  
  const [selectedVariable, setSelectedVariable] = useState<Variable | null>(null);
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [filteredVariables, setFilteredVariables] = useState<Variable[]>(variables);
  
  // Filter variables when the search term or variables change
  useEffect(() => {
    if (searchTerm.trim() === '') {
      setFilteredVariables(variables);
    } else {
      const term = searchTerm.toLowerCase();
      const filtered = variables.filter(variable => (
        variable.name.toLowerCase().includes(term) ||
        variable.value.toLowerCase().includes(term) ||
        (variable.description && variable.description.toLowerCase().includes(term))
      ));
      setFilteredVariables(filtered);
    }
  }, [searchTerm, variables]);
  
  // Handle selecting a variable
  const handleVariableClick = (variable: Variable) => {
    setSelectedVariable(variable);
  };
  
  // Handle deleting a variable
  const handleDeleteVariable = async (variableId: string) => {
    try {
      await deleteVariable(variableId);
      if (selectedVariable?.id === variableId) {
        setSelectedVariable(null);
      }
    } catch (error) {
      // Error handling is done in context
    }
  };
  
  // Handle creating a new variable
  const handleCreateVariable = () => {
    setSelectedVariable(null);
  };
  
  // Handle form save (either create or update)
  const handleSaveVariable = () => {
    // This will be handled by the form component
    // Just ensure we keep the variable list updated
  };
  
  // Sort variables by scope and then by name
  const sortedVariables = [...filteredVariables].sort((a, b) => {
    // First, sort by scope priority
    const scopePriority: Record<VariableScope, number> = {
      [VariableScope.GLOBAL]: 0,
      [VariableScope.PROJECT]: 1,
      [VariableScope.TEST]: 2,
      [VariableScope.STEP]: 3
    };
    
    const scopeDiff = scopePriority[a.scope] - scopePriority[b.scope];
    if (scopeDiff !== 0) return scopeDiff;
    
    // Then sort alphabetically by name
    return a.name.localeCompare(b.name);
  });
  
  return (
    <div className="variable-manager">
      {/* Variable List Panel */}
      <div className="variable-list">
        <div className="variable-list-header">
          <h5 className="mb-0">Variables</h5>
          <span className="badge bg-primary">{variables.length}</span>
        </div>
        
        <div className="p-3 border-bottom">
          <div className="input-group">
            <input
              type="text"
              className="form-control"
              placeholder="Search variables..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              disabled={isLoading}
            />
            {searchTerm && (
              <button
                className="btn btn-outline-secondary"
                type="button"
                onClick={() => setSearchTerm('')}
              >
                <i className="bi bi-x"></i>
              </button>
            )}
          </div>
        </div>
        
        <div className="variable-list-content">
          {isLoading ? (
            <div className="loading-spinner">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
            </div>
          ) : sortedVariables.length === 0 ? (
            <div className="variable-list-empty">
              <p>No variables found.</p>
              {searchTerm ? (
                <p>Try modifying your search criteria.</p>
              ) : (
                <p>Click the button below to create your first variable.</p>
              )}
            </div>
          ) : (
            sortedVariables.map((variable) => (
              <VariableItem
                key={variable.id}
                variable={variable}
                isSelected={selectedVariable?.id === variable.id}
                onClick={() => handleVariableClick(variable)}
                onDelete={() => handleDeleteVariable(variable.id)}
              />
            ))
          )}
        </div>
        
        <div className="p-3 border-top">
          <button
            className="btn btn-primary w-100"
            onClick={handleCreateVariable}
            disabled={isLoading}
          >
            <i className="bi bi-plus-circle me-1"></i> New Variable
          </button>
        </div>
      </div>
      
      {/* Variable Form Panel */}
      <div className="variable-form-container">
        <VariableForm 
          projectId={projectId}
          initialVariable={selectedVariable}
          onSave={handleSaveVariable}
          isLoading={isLoading}
        />
      </div>
    </div>
  );
};

export default VariableManager; 