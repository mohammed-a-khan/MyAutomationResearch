/**
 * Service for Code Builder API interactions
 */
import api from './api';
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
import { logError } from '../utils/errorHandling';

/**
 * Creates a new test step
 * @param step Step data
 * @returns Promise with the created step
 */
export const createStep = async (step: CreateStepPayload): Promise<TestStep> => {
  try {
    return await api.post<TestStep>('/api/codebuilder/step', step);
  } catch (error) {
    logError(error, 'Create Step');
    throw error;
  }
};

/**
 * Updates an existing test step
 * @param stepId Step ID
 * @param updates Step updates
 * @returns Promise with the updated step
 */
export const updateStep = async (stepId: string, updates: UpdateStepPayload): Promise<TestStep> => {
  try {
    return await api.put<TestStep>(`/api/codebuilder/step/${stepId}`, updates);
  } catch (error) {
    logError(error, 'Update Step');
    throw error;
  }
};

/**
 * Deletes a test step
 * @param stepId Step ID
 * @returns Promise with void
 */
export const deleteStep = async (stepId: string): Promise<void> => {
  try {
    return await api.delete<void>(`/api/codebuilder/step/${stepId}`);
  } catch (error) {
    logError(error, 'Delete Step');
    throw error;
  }
};

/**
 * Gets a test step by ID
 * @param stepId Step ID
 * @returns Promise with the step
 */
export const getStepById = async (stepId: string): Promise<TestStep> => {
  try {
    return await api.get<TestStep>(`/api/codebuilder/step/${stepId}`);
  } catch (error) {
    logError(error, 'Get Step');
    throw error;
  }
};

/**
 * Gets all steps for a project
 * @param projectId Project ID
 * @returns Promise with array of steps
 */
export const getStepsByProject = async (projectId: string): Promise<TestStep[]> => {
  try {
    return await api.get<TestStep[]>(`/api/codebuilder/project/${projectId}/steps`);
  } catch (error) {
    logError(error, 'Get Steps');
    throw error;
  }
};

/**
 * Creates a new variable
 * @param variable Variable data
 * @returns Promise with the created variable
 */
export const createVariable = async (variable: CreateVariablePayload): Promise<Variable> => {
  try {
    return await api.post<Variable>('/api/codebuilder/variable', variable);
  } catch (error) {
    logError(error, 'Create Variable');
    throw error;
  }
};

/**
 * Updates an existing variable
 * @param variableId Variable ID
 * @param updates Variable updates
 * @returns Promise with the updated variable
 */
export const updateVariable = async (variableId: string, updates: UpdateVariablePayload): Promise<Variable> => {
  try {
    return await api.put<Variable>(`/api/codebuilder/variable/${variableId}`, updates);
  } catch (error) {
    logError(error, 'Update Variable');
    throw error;
  }
};

/**
 * Deletes a variable
 * @param variableId Variable ID
 * @returns Promise with void
 */
export const deleteVariable = async (variableId: string): Promise<void> => {
  try {
    return await api.delete<void>(`/api/codebuilder/variable/${variableId}`);
  } catch (error) {
    logError(error, 'Delete Variable');
    throw error;
  }
};

/**
 * Gets a variable by ID
 * @param variableId Variable ID
 * @returns Promise with the variable
 */
export const getVariableById = async (variableId: string): Promise<Variable> => {
  try {
    return await api.get<Variable>(`/api/codebuilder/variable/${variableId}`);
  } catch (error) {
    logError(error, 'Get Variable');
    throw error;
  }
};

/**
 * Gets all variables for a project
 * @param projectId Project ID
 * @returns Promise with array of variables
 */
export const getVariablesByProject = async (projectId: string): Promise<Variable[]> => {
  try {
    return await api.get<Variable[]>(`/api/codebuilder/project/${projectId}/variables`);
  } catch (error) {
    logError(error, 'Get Variables');
    throw error;
  }
};

/**
 * Gets smart locator suggestions for an element
 * @param projectId Project ID
 * @param elementRef Reference to element (e.g. screenshot, DOM path, or description)
 * @returns Promise with locator suggestions
 */
export const getLocatorSuggestions = async (
  projectId: string,
  elementRef: string | File
): Promise<LocatorSuggestion[]> => {
  try {
    if (typeof elementRef === 'string') {
      return await api.post<LocatorSuggestion[]>(`/api/codebuilder/locators`, {
        projectId,
        elementRef
      });
    } else {
      // If it's a file (screenshot), use the uploadFile method
      const formData = new FormData();
      formData.append('file', elementRef);
      formData.append('projectId', projectId);
      
      return await api.post<LocatorSuggestion[]>(`/api/codebuilder/locators/screenshot`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
    }
  } catch (error) {
    logError(error, 'Get Locator Suggestions');
    throw error;
  }
};

/**
 * Generate code from natural language
 * @param request NLP code generation request
 * @returns Promise with generated code steps
 */
export const generateCodeFromNLP = async (request: NlpCodeRequest): Promise<NlpCodeResponse> => {
  try {
    return await api.post<NlpCodeResponse>('/api/codebuilder/nlp', request);
  } catch (error) {
    logError(error, 'Generate Code from NLP');
    throw error;
  }
};

/**
 * Reorder steps in the test
 * @param projectId Project ID
 * @param stepIds Ordered array of step IDs
 * @returns Promise with array of updated steps
 */
export const reorderSteps = async (projectId: string, stepIds: string[]): Promise<TestStep[]> => {
  try {
    return await api.put<TestStep[]>(`/api/codebuilder/project/${projectId}/steps/reorder`, { stepIds });
  } catch (error) {
    logError(error, 'Reorder Steps');
    throw error;
  }
};

/**
 * Get available commands for a step type
 * @param stepType Step type
 * @returns Promise with array of available commands
 */
export const getAvailableCommands = async (stepType: string): Promise<string[]> => {
  try {
    return await api.get<string[]>(`/api/codebuilder/commands/${stepType}`);
  } catch (error) {
    logError(error, 'Get Available Commands');
    throw error;
  }
}; 