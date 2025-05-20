import React, { createContext, useContext, useReducer, useCallback, ReactNode } from 'react';
import {
  TestStep,
  CreateStepPayload,
  UpdateStepPayload,
  Variable,
  CreateVariablePayload,
  UpdateVariablePayload,
  LocatorSuggestion,
  NlpCodeRequest,
  NlpCodeResponse
} from '../types/codebuilder';
import * as codebuilderService from '../services/codebuilderService';
import { formatError } from '../utils/errorHandling';

/**
 * CodeBuilder context state interface
 */
interface CodeBuilderState {
  steps: TestStep[];
  variables: Variable[];
  selectedStep: TestStep | null;
  locatorSuggestions: LocatorSuggestion[];
  isLoading: boolean;
  error: string | null;
  projectId: string | null;
}

/**
 * Action types for the CodeBuilder reducer
 */
enum ActionType {
  SET_STEPS = 'SET_STEPS',
  SET_VARIABLES = 'SET_VARIABLES',
  SELECT_STEP = 'SELECT_STEP',
  ADD_STEP = 'ADD_STEP',
  UPDATE_STEP = 'UPDATE_STEP',
  REMOVE_STEP = 'REMOVE_STEP',
  SET_LOCATOR_SUGGESTIONS = 'SET_LOCATOR_SUGGESTIONS',
  ADD_VARIABLE = 'ADD_VARIABLE',
  UPDATE_VARIABLE = 'UPDATE_VARIABLE',
  REMOVE_VARIABLE = 'REMOVE_VARIABLE',
  SET_PROJECT_ID = 'SET_PROJECT_ID',
  SET_LOADING = 'SET_LOADING',
  SET_ERROR = 'SET_ERROR',
  CLEAR_ERROR = 'CLEAR_ERROR',
}

/**
 * Union type for all CodeBuilder actions
 */
type Action =
  | { type: ActionType.SET_STEPS; payload: TestStep[] }
  | { type: ActionType.SET_VARIABLES; payload: Variable[] }
  | { type: ActionType.SELECT_STEP; payload: TestStep | null }
  | { type: ActionType.ADD_STEP; payload: TestStep }
  | { type: ActionType.UPDATE_STEP; payload: TestStep }
  | { type: ActionType.REMOVE_STEP; payload: string }
  | { type: ActionType.SET_LOCATOR_SUGGESTIONS; payload: LocatorSuggestion[] }
  | { type: ActionType.ADD_VARIABLE; payload: Variable }
  | { type: ActionType.UPDATE_VARIABLE; payload: Variable }
  | { type: ActionType.REMOVE_VARIABLE; payload: string }
  | { type: ActionType.SET_PROJECT_ID; payload: string }
  | { type: ActionType.SET_LOADING; payload: boolean }
  | { type: ActionType.SET_ERROR; payload: string | null }
  | { type: ActionType.CLEAR_ERROR };

/**
 * Initial state for the CodeBuilder context
 */
const initialState: CodeBuilderState = {
  steps: [],
  variables: [],
  selectedStep: null,
  locatorSuggestions: [],
  isLoading: false,
  error: null,
  projectId: null,
};

/**
 * Reducer function for the CodeBuilder context
 */
const codeBuilderReducer = (state: CodeBuilderState, action: Action): CodeBuilderState => {
  switch (action.type) {
    case ActionType.SET_STEPS:
      return {
        ...state,
        steps: action.payload,
      };

    case ActionType.SET_VARIABLES:
      return {
        ...state,
        variables: action.payload,
      };

    case ActionType.SELECT_STEP:
      return {
        ...state,
        selectedStep: action.payload,
      };

    case ActionType.ADD_STEP:
      return {
        ...state,
        steps: [...state.steps, action.payload],
      };

    case ActionType.UPDATE_STEP:
      return {
        ...state,
        steps: state.steps.map(step =>
          step.id === action.payload.id ? action.payload : step
        ),
        selectedStep: state.selectedStep?.id === action.payload.id
          ? action.payload
          : state.selectedStep,
      };

    case ActionType.REMOVE_STEP:
      return {
        ...state,
        steps: state.steps.filter(step => step.id !== action.payload),
        selectedStep: state.selectedStep?.id === action.payload
          ? null
          : state.selectedStep,
      };

    case ActionType.SET_LOCATOR_SUGGESTIONS:
      return {
        ...state,
        locatorSuggestions: action.payload,
      };

    case ActionType.ADD_VARIABLE:
      return {
        ...state,
        variables: [...state.variables, action.payload],
      };

    case ActionType.UPDATE_VARIABLE:
      return {
        ...state,
        variables: state.variables.map(variable =>
          variable.id === action.payload.id ? action.payload : variable
        ),
      };

    case ActionType.REMOVE_VARIABLE:
      return {
        ...state,
        variables: state.variables.filter(variable => variable.id !== action.payload),
      };

    case ActionType.SET_PROJECT_ID:
      return {
        ...state,
        projectId: action.payload,
      };

    case ActionType.SET_LOADING:
      return {
        ...state,
        isLoading: action.payload,
      };

    case ActionType.SET_ERROR:
      return {
        ...state,
        error: action.payload,
      };

    case ActionType.CLEAR_ERROR:
      return {
        ...state,
        error: null,
      };

    default:
      return state;
  }
};

/**
 * CodeBuilder context interface
 */
interface CodeBuilderContextValue extends CodeBuilderState {
  loadProjectSteps: (projectId: string) => Promise<void>;
  loadProjectVariables: (projectId: string) => Promise<void>;
  createStep: (step: CreateStepPayload) => Promise<TestStep>;
  updateStep: (stepId: string, updates: UpdateStepPayload) => Promise<TestStep>;
  deleteStep: (stepId: string) => Promise<void>;
  selectStep: (step: TestStep | null) => void;
  reorderSteps: (stepIds: string[]) => Promise<void>;
  createVariable: (variable: CreateVariablePayload) => Promise<Variable>;
  updateVariable: (variableId: string, updates: UpdateVariablePayload) => Promise<Variable>;
  deleteVariable: (variableId: string) => Promise<void>;
  getLocatorSuggestions: (elementRef: string | File) => Promise<LocatorSuggestion[]>;
  generateCodeFromNLP: (request: NlpCodeRequest) => Promise<NlpCodeResponse>;
  setProjectId: (projectId: string) => void;
  clearError: () => void;
}

/**
 * Create the CodeBuilder context with default values
 */
const CodeBuilderContext = createContext<CodeBuilderContextValue>({
  ...initialState,
  loadProjectSteps: async () => {},
  loadProjectVariables: async () => {},
  createStep: async () => ({} as TestStep),
  updateStep: async () => ({} as TestStep),
  deleteStep: async () => {},
  selectStep: () => {},
  reorderSteps: async () => {},
  createVariable: async () => ({} as Variable),
  updateVariable: async () => ({} as Variable),
  deleteVariable: async () => {},
  getLocatorSuggestions: async () => [],
  generateCodeFromNLP: async () => ({ steps: [], confidence: 0 }),
  setProjectId: () => {},
  clearError: () => {},
});

/**
 * CodeBuilder provider props
 */
interface CodeBuilderProviderProps {
  children: ReactNode;
}

/**
 * CodeBuilder context provider component
 */
export const CodeBuilderProvider: React.FC<CodeBuilderProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(codeBuilderReducer, initialState);

  // Reset error after 5 seconds
  React.useEffect(() => {
    if (state.error) {
      const timer = setTimeout(() => {
        dispatch({ type: ActionType.CLEAR_ERROR });
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [state.error]);

  /**
   * Load all steps for a project
   */
  const loadProjectSteps = useCallback(async (projectId: string): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      // Set project ID if not already set
      if (state.projectId !== projectId) {
        dispatch({ type: ActionType.SET_PROJECT_ID, payload: projectId });
      }
      
      const steps = await codebuilderService.getStepsByProject(projectId);
      dispatch({ type: ActionType.SET_STEPS, payload: steps });
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to load test steps') 
      });
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.projectId]);

  /**
   * Load all variables for a project
   */
  const loadProjectVariables = useCallback(async (projectId: string): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      // Set project ID if not already set
      if (state.projectId !== projectId) {
        dispatch({ type: ActionType.SET_PROJECT_ID, payload: projectId });
      }
      
      const variables = await codebuilderService.getVariablesByProject(projectId);
      dispatch({ type: ActionType.SET_VARIABLES, payload: variables });
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to load variables') 
      });
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.projectId]);

  /**
   * Create a new step
   */
  const createNewStep = useCallback(async (step: CreateStepPayload): Promise<TestStep> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const newStep = await codebuilderService.createStep(step);
      dispatch({ type: ActionType.ADD_STEP, payload: newStep });
      return newStep;
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to create step') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Update an existing step
   */
  const updateExistingStep = useCallback(async (
    stepId: string, 
    updates: UpdateStepPayload
  ): Promise<TestStep> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const updatedStep = await codebuilderService.updateStep(stepId, updates);
      dispatch({ type: ActionType.UPDATE_STEP, payload: updatedStep });
      return updatedStep;
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to update step') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Delete a step
   */
  const deleteExistingStep = useCallback(async (stepId: string): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      await codebuilderService.deleteStep(stepId);
      dispatch({ type: ActionType.REMOVE_STEP, payload: stepId });
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to delete step') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Select a step
   */
  const selectStep = useCallback((step: TestStep | null): void => {
    dispatch({ type: ActionType.SELECT_STEP, payload: step });
  }, []);

  /**
   * Reorder steps
   */
  const reorderProjectSteps = useCallback(async (stepIds: string[]): Promise<void> => {
    if (!state.projectId) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: 'No project selected for reordering steps'
      });
      return;
    }
    
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const updatedSteps = await codebuilderService.reorderSteps(state.projectId, stepIds);
      dispatch({ type: ActionType.SET_STEPS, payload: updatedSteps });
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to reorder steps') 
      });
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.projectId]);

  /**
   * Create a new variable
   */
  const createNewVariable = useCallback(async (variable: CreateVariablePayload): Promise<Variable> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const newVariable = await codebuilderService.createVariable(variable);
      dispatch({ type: ActionType.ADD_VARIABLE, payload: newVariable });
      return newVariable;
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to create variable') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Update an existing variable
   */
  const updateExistingVariable = useCallback(async (
    variableId: string, 
    updates: UpdateVariablePayload
  ): Promise<Variable> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const updatedVariable = await codebuilderService.updateVariable(variableId, updates);
      dispatch({ type: ActionType.UPDATE_VARIABLE, payload: updatedVariable });
      return updatedVariable;
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to update variable') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Delete a variable
   */
  const deleteExistingVariable = useCallback(async (variableId: string): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      await codebuilderService.deleteVariable(variableId);
      dispatch({ type: ActionType.REMOVE_VARIABLE, payload: variableId });
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to delete variable') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Get locator suggestions
   */
  const getElementLocatorSuggestions = useCallback(async (
    elementRef: string | File
  ): Promise<LocatorSuggestion[]> => {
    if (!state.projectId) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: 'No project selected for locator suggestions'
      });
      return [];
    }
    
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const suggestions = await codebuilderService.getLocatorSuggestions(
        state.projectId,
        elementRef
      );
      dispatch({ type: ActionType.SET_LOCATOR_SUGGESTIONS, payload: suggestions });
      return suggestions;
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to get locator suggestions') 
      });
      return [];
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.projectId]);

  /**
   * Generate code from NLP
   */
  const generateCodeFromNaturalLanguage = useCallback(async (
    request: NlpCodeRequest
  ): Promise<NlpCodeResponse> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });
    
    try {
      const response = await codebuilderService.generateCodeFromNLP(request);
      
      // Add the generated steps to the current steps list
      if (response.steps.length > 0) {
        dispatch({ 
          type: ActionType.SET_STEPS, 
          payload: [...state.steps, ...response.steps]
        });
      }
      
      return response;
    } catch (error) {
      dispatch({ 
        type: ActionType.SET_ERROR, 
        payload: formatError(error, 'Failed to generate code from text') 
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.steps]);

  /**
   * Set the current project ID
   */
  const setProjectId = useCallback((projectId: string): void => {
    dispatch({ type: ActionType.SET_PROJECT_ID, payload: projectId });
  }, []);

  /**
   * Clear error message
   */
  const clearError = useCallback((): void => {
    dispatch({ type: ActionType.CLEAR_ERROR });
  }, []);

  // Combine all values and functions for the context
  const contextValue: CodeBuilderContextValue = {
    ...state,
    loadProjectSteps,
    loadProjectVariables,
    createStep: createNewStep,
    updateStep: updateExistingStep,
    deleteStep: deleteExistingStep,
    selectStep,
    reorderSteps: reorderProjectSteps,
    createVariable: createNewVariable,
    updateVariable: updateExistingVariable,
    deleteVariable: deleteExistingVariable,
    getLocatorSuggestions: getElementLocatorSuggestions,
    generateCodeFromNLP: generateCodeFromNaturalLanguage,
    setProjectId,
    clearError,
  };

  return (
    <CodeBuilderContext.Provider value={contextValue}>
      {children}
    </CodeBuilderContext.Provider>
  );
};

/**
 * Custom hook for accessing the CodeBuilder context
 */
export const useCodeBuilder = (): CodeBuilderContextValue => {
  const context = useContext(CodeBuilderContext);
  
  if (!context) {
    throw new Error('useCodeBuilder must be used within a CodeBuilderProvider');
  }
  
  return context;
};

export default CodeBuilderContext; 