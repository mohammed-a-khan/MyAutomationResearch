import React, { createContext, useContext, useReducer, useMemo, ReactNode, useCallback } from 'react';
import { 
  TestExecutionRequest, 
  TestExecutionInfo, 
  TestExecutionDetail,
  TestResult,
  Screenshot,
  ParallelExecutionStatus,
  TestFilter,
  TestStatus,
  BrowserType,
  ExecutionConfig
} from '../types/execution';
import executionService from '../services/executionService';

// Define the state shape
interface ExecutionState {
  currentExecution: TestExecutionInfo | null;
  executionDetail: TestExecutionDetail | null;
  selectedTestResult: TestResult | null;
  executionHistory: TestExecutionInfo[];
  totalHistoryItems: number;
  totalHistoryPages: number;
  historyPage: number;
  historyFilter: TestFilter;
  parallelStatus: ParallelExecutionStatus | null;
  isLoading: boolean;
  error: string | null;
}

// Initial state
const initialState: ExecutionState = {
  currentExecution: null,
  executionDetail: null,
  selectedTestResult: null,
  executionHistory: [],
  totalHistoryItems: 0,
  totalHistoryPages: 0,
  historyPage: 1,
  historyFilter: {},
  parallelStatus: null,
  isLoading: false,
  error: null,
};

// Action types
enum ActionType {
  START_LOADING = 'START_LOADING',
  SET_ERROR = 'SET_ERROR',
  CLEAR_ERROR = 'CLEAR_ERROR',
  SET_CURRENT_EXECUTION = 'SET_CURRENT_EXECUTION',
  CLEAR_CURRENT_EXECUTION = 'CLEAR_CURRENT_EXECUTION',
  SET_EXECUTION_DETAIL = 'SET_EXECUTION_DETAIL',
  SET_SELECTED_TEST_RESULT = 'SET_SELECTED_TEST_RESULT',
  SET_EXECUTION_HISTORY = 'SET_EXECUTION_HISTORY',
  SET_HISTORY_FILTER = 'SET_HISTORY_FILTER',
  SET_PARALLEL_STATUS = 'SET_PARALLEL_STATUS',
}

// Action interfaces
interface StartLoadingAction {
  type: ActionType.START_LOADING;
}

interface SetErrorAction {
  type: ActionType.SET_ERROR;
  payload: string;
}

interface ClearErrorAction {
  type: ActionType.CLEAR_ERROR;
}

interface SetCurrentExecutionAction {
  type: ActionType.SET_CURRENT_EXECUTION;
  payload: TestExecutionInfo;
}

interface ClearCurrentExecutionAction {
  type: ActionType.CLEAR_CURRENT_EXECUTION;
}

interface SetExecutionDetailAction {
  type: ActionType.SET_EXECUTION_DETAIL;
  payload: TestExecutionDetail;
}

interface SetSelectedTestResultAction {
  type: ActionType.SET_SELECTED_TEST_RESULT;
  payload: TestResult | null;
}

interface SetExecutionHistoryAction {
  type: ActionType.SET_EXECUTION_HISTORY;
  payload: {
    items: TestExecutionInfo[];
    totalItems: number;
    totalPages: number;
    page: number;
  };
}

interface SetHistoryFilterAction {
  type: ActionType.SET_HISTORY_FILTER;
  payload: TestFilter;
}

interface SetParallelStatusAction {
  type: ActionType.SET_PARALLEL_STATUS;
  payload: ParallelExecutionStatus;
}

type ExecutionAction =
  | StartLoadingAction
  | SetErrorAction
  | ClearErrorAction
  | SetCurrentExecutionAction
  | ClearCurrentExecutionAction
  | SetExecutionDetailAction
  | SetSelectedTestResultAction
  | SetExecutionHistoryAction
  | SetHistoryFilterAction
  | SetParallelStatusAction;

// Reducer
const executionReducer = (state: ExecutionState, action: ExecutionAction): ExecutionState => {
  switch (action.type) {
    case ActionType.START_LOADING:
      return { ...state, isLoading: true, error: null };

    case ActionType.SET_ERROR:
      return { ...state, isLoading: false, error: action.payload };

    case ActionType.CLEAR_ERROR:
      return { ...state, error: null };

    case ActionType.SET_CURRENT_EXECUTION:
      return { ...state, currentExecution: action.payload, isLoading: false };

    case ActionType.CLEAR_CURRENT_EXECUTION:
      return { ...state, currentExecution: null, executionDetail: null, selectedTestResult: null };

    case ActionType.SET_EXECUTION_DETAIL:
      return { ...state, executionDetail: action.payload, isLoading: false };

    case ActionType.SET_SELECTED_TEST_RESULT:
      return { ...state, selectedTestResult: action.payload, isLoading: false };

    case ActionType.SET_EXECUTION_HISTORY:
      return { 
        ...state, 
        executionHistory: action.payload.items, 
        totalHistoryItems: action.payload.totalItems,
        totalHistoryPages: action.payload.totalPages,
        historyPage: action.payload.page,
        isLoading: false 
      };

    case ActionType.SET_HISTORY_FILTER:
      return { ...state, historyFilter: action.payload };

    case ActionType.SET_PARALLEL_STATUS:
      return { ...state, parallelStatus: action.payload, isLoading: false };

    default:
      return state;
  }
};

// Context interface
interface ExecutionContextProps {
  state: ExecutionState;
  runTests: (request: TestExecutionRequest) => Promise<TestExecutionInfo>;
  stopExecution: (executionId: string) => Promise<boolean>;
  getExecutionStatus: (executionId: string) => Promise<TestExecutionInfo>;
  getExecutionDetails: (executionId: string) => Promise<TestExecutionDetail>;
  getTestResult: (executionId: string, testId: string) => Promise<TestResult>;
  getScreenshot: (screenshotId: string) => Promise<Screenshot>;
  loadExecutionHistory: (
    page?: number,
    pageSize?: number,
    projectId?: string
  ) => Promise<void>;
  getExecutionHistory: (
    page?: number,
    pageSize?: number,
    projectId?: string
  ) => Promise<void>;
  setHistoryFilter: (filter: TestFilter) => void;
  updateHistoryFilter: (filter: TestFilter) => void;
  getParallelStatus: () => Promise<ParallelExecutionStatus>;
  updateParallelConfig: (maxParallel: number) => Promise<boolean>;
  clearCurrentExecution: () => void;
  selectTestResult: (testResult: TestResult | null) => void;
  clearError: () => void;
  getDefaultConfig: (projectId: string) => ExecutionConfig;
}

// Create context
const ExecutionContext = createContext<ExecutionContextProps | undefined>(undefined);

// Provider props
interface ExecutionProviderProps {
  children: ReactNode;
}

// Create provider
export const ExecutionProvider: React.FC<ExecutionProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(executionReducer, initialState);

  // Action creators
  const setLoading = useCallback(() => {
    dispatch({ type: ActionType.START_LOADING });
  }, []);

  const setError = useCallback((error: string) => {
    dispatch({ type: ActionType.SET_ERROR, payload: error });
  }, []);

  const clearError = useCallback(() => {
    dispatch({ type: ActionType.CLEAR_ERROR });
  }, []);

  const clearCurrentExecution = useCallback(() => {
    dispatch({ type: ActionType.CLEAR_CURRENT_EXECUTION });
  }, []);

  const selectTestResult = useCallback((testResult: TestResult | null) => {
    dispatch({ type: ActionType.SET_SELECTED_TEST_RESULT, payload: testResult });
  }, []);

  const setHistoryFilter = useCallback((filter: TestFilter) => {
    dispatch({ type: ActionType.SET_HISTORY_FILTER, payload: filter });
  }, []);
  
  const updateHistoryFilter = useCallback((filter: TestFilter) => {
    dispatch({ type: ActionType.SET_HISTORY_FILTER, payload: filter });
  }, []);

  // Service integration
  const runTests = useCallback(async (request: TestExecutionRequest): Promise<TestExecutionInfo> => {
    setLoading();
    try {
      const executionInfo = await executionService.runTests(request);
      dispatch({ type: ActionType.SET_CURRENT_EXECUTION, payload: executionInfo });
      return executionInfo;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to run tests';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const stopExecution = useCallback(async (executionId: string): Promise<boolean> => {
    setLoading();
    try {
      const success = await executionService.stopExecution(executionId);
      // Refresh the status after stopping
      if (success && state.currentExecution?.id === executionId) {
        const updatedExecution = await executionService.getExecutionStatus(executionId);
        dispatch({ type: ActionType.SET_CURRENT_EXECUTION, payload: updatedExecution });
      }
      return success;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to stop execution';
      setError(message);
      throw error;
    }
  }, [state.currentExecution, setLoading, setError]);

  const getExecutionStatus = useCallback(async (executionId: string): Promise<TestExecutionInfo> => {
    setLoading();
    try {
      const executionInfo = await executionService.getExecutionStatus(executionId);
      dispatch({ type: ActionType.SET_CURRENT_EXECUTION, payload: executionInfo });
      return executionInfo;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get execution status';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getExecutionDetails = useCallback(async (executionId: string): Promise<TestExecutionDetail> => {
    setLoading();
    try {
      const executionDetail = await executionService.getExecutionDetails(executionId);
      dispatch({ type: ActionType.SET_EXECUTION_DETAIL, payload: executionDetail });
      // Also update the current execution status
      dispatch({ type: ActionType.SET_CURRENT_EXECUTION, payload: executionDetail });
      return executionDetail;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get execution details';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getTestResult = useCallback(async (executionId: string, testId: string): Promise<TestResult> => {
    setLoading();
    try {
      const testResult = await executionService.getTestResult(executionId, testId);
      dispatch({ type: ActionType.SET_SELECTED_TEST_RESULT, payload: testResult });
      return testResult;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get test result';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getScreenshot = useCallback(async (screenshotId: string): Promise<Screenshot> => {
    try {
      return await executionService.getScreenshot(screenshotId);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get screenshot';
      setError(message);
      throw error;
    }
  }, [setError]);

  const loadExecutionHistory = useCallback(async (
    page: number = 1,
    pageSize: number = 10,
    projectId?: string
  ): Promise<void> => {
    setLoading();
    try {
      const extendedFilter = {
        ...state.historyFilter,
        projectId
      };
      const { items, totalItems, totalPages } = await executionService.getExecutionHistory(
        extendedFilter, page, pageSize
      );
      dispatch({
        type: ActionType.SET_EXECUTION_HISTORY,
        payload: {
          items,
          totalItems,
          totalPages,
          page
        }
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to load execution history';
      setError(message);
    }
  }, [state.historyFilter, setLoading, setError]);
  
  const getExecutionHistory = useCallback(async (
    page: number = 1,
    pageSize: number = 10,
    projectId?: string
  ): Promise<void> => {
    return loadExecutionHistory(page, pageSize, projectId);
  }, [loadExecutionHistory]);

  const getParallelStatus = useCallback(async (): Promise<ParallelExecutionStatus> => {
    setLoading();
    try {
      const status = await executionService.getParallelStatus();
      dispatch({ type: ActionType.SET_PARALLEL_STATUS, payload: status });
      return status;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get parallel status';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const updateParallelConfig = useCallback(async (maxParallel: number): Promise<boolean> => {
    setLoading();
    try {
      const success = await executionService.updateParallelConfig(maxParallel);
      if (success) {
        // Refresh parallel status
        const status = await executionService.getParallelStatus();
        dispatch({ type: ActionType.SET_PARALLEL_STATUS, payload: status });
      }
      return success;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to update parallel configuration';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  // Utility functions
  const getDefaultConfig = useCallback((projectId: string): ExecutionConfig => {
    return {
      environment: 'DEV',
      browser: BrowserType.CHROME,
      headless: false,
      retryCount: 1,
      timeoutSeconds: 30,
      parallel: false,
      maxParallel: 1,
      customParams: {}
    };
  }, []);

  // Context value
  const contextValue = useMemo(() => ({
    state,
    runTests,
    stopExecution,
    getExecutionStatus,
    getExecutionDetails,
    getTestResult,
    getScreenshot,
    loadExecutionHistory,
    getExecutionHistory,
    setHistoryFilter,
    updateHistoryFilter,
    getParallelStatus,
    updateParallelConfig,
    clearCurrentExecution,
    selectTestResult,
    clearError,
    getDefaultConfig
  }), [
    state,
    runTests,
    stopExecution,
    getExecutionStatus,
    getExecutionDetails,
    getTestResult,
    getScreenshot,
    loadExecutionHistory,
    getExecutionHistory,
    setHistoryFilter,
    updateHistoryFilter,
    getParallelStatus,
    updateParallelConfig,
    clearCurrentExecution,
    selectTestResult,
    clearError,
    getDefaultConfig
  ]);

  return (
    <ExecutionContext.Provider value={contextValue}>
      {children}
    </ExecutionContext.Provider>
  );
};

// Custom hook for using the execution context
export const useExecution = (): ExecutionContextProps => {
  const context = useContext(ExecutionContext);
  if (!context) {
    throw new Error('useExecution must be used within an ExecutionProvider');
  }
  return context;
};

export default ExecutionContext; 