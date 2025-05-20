import React, { createContext, useContext, useReducer, useMemo, ReactNode, useCallback } from 'react';
import adoService from '../services/adoService';

// Define the state types
export interface AdoConnection {
  id: string;
  name: string;
  url: string;
  pat: string;
  organizationName: string;
  projectName: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AdoProject {
  id: string;
  name: string;
  description: string;
}

export interface AdoTestPlan {
  id: string;
  name: string;
  projectId: string;
}

export interface AdoTestSuite {
  id: string;
  name: string;
  testPlanId: string;
  parentId?: string;
}

export interface AdoPipeline {
  id: string;
  name: string;
  projectId: string;
  url: string;
}

export interface SyncConfig {
  connectionId: string;
  projectId: string;
  testPlanId: string;
  testSuiteId: string;
  syncFrequency: 'manual' | 'daily' | 'weekly';
  bidirectional: boolean;
}

export interface SyncStatus {
  lastSyncTime: string | null;
  lastSyncStatus: 'success' | 'failed' | 'in-progress' | 'not-started';
  lastSyncMessage: string | null;
  nextScheduledSync: string | null;
}

export interface PipelineConfig {
  connectionId: string;
  projectId: string;
  pipelineId: string;
  triggerMode: 'manual' | 'auto';
  includeTest: boolean;
}

interface AdoState {
  connections: AdoConnection[];
  projects: AdoProject[];
  testPlans: AdoTestPlan[];
  testSuites: AdoTestSuite[];
  pipelines: AdoPipeline[];
  syncConfig: SyncConfig | null;
  syncStatus: SyncStatus | null;
  pipelineConfig: PipelineConfig | null;
  isLoading: boolean;
  error: string | null;
}

// Initial state
const initialState: AdoState = {
  connections: [],
  projects: [],
  testPlans: [],
  testSuites: [],
  pipelines: [],
  syncConfig: null,
  syncStatus: null,
  pipelineConfig: null,
  isLoading: false,
  error: null,
};

// Action types
enum ActionType {
  START_LOADING = 'START_LOADING',
  SET_ERROR = 'SET_ERROR',
  CLEAR_ERROR = 'CLEAR_ERROR',
  SET_CONNECTIONS = 'SET_CONNECTIONS',
  SET_PROJECTS = 'SET_PROJECTS',
  SET_TEST_PLANS = 'SET_TEST_PLANS',
  SET_TEST_SUITES = 'SET_TEST_SUITES',
  SET_PIPELINES = 'SET_PIPELINES',
  SET_SYNC_CONFIG = 'SET_SYNC_CONFIG',
  SET_SYNC_STATUS = 'SET_SYNC_STATUS',
  SET_PIPELINE_CONFIG = 'SET_PIPELINE_CONFIG',
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

interface SetConnectionsAction {
  type: ActionType.SET_CONNECTIONS;
  payload: AdoConnection[];
}

interface SetProjectsAction {
  type: ActionType.SET_PROJECTS;
  payload: AdoProject[];
}

interface SetTestPlansAction {
  type: ActionType.SET_TEST_PLANS;
  payload: AdoTestPlan[];
}

interface SetTestSuitesAction {
  type: ActionType.SET_TEST_SUITES;
  payload: AdoTestSuite[];
}

interface SetPipelinesAction {
  type: ActionType.SET_PIPELINES;
  payload: AdoPipeline[];
}

interface SetSyncConfigAction {
  type: ActionType.SET_SYNC_CONFIG;
  payload: SyncConfig;
}

interface SetSyncStatusAction {
  type: ActionType.SET_SYNC_STATUS;
  payload: SyncStatus;
}

interface SetPipelineConfigAction {
  type: ActionType.SET_PIPELINE_CONFIG;
  payload: PipelineConfig;
}

type AdoAction =
  | StartLoadingAction
  | SetErrorAction
  | ClearErrorAction
  | SetConnectionsAction
  | SetProjectsAction
  | SetTestPlansAction
  | SetTestSuitesAction
  | SetPipelinesAction
  | SetSyncConfigAction
  | SetSyncStatusAction
  | SetPipelineConfigAction;

// Reducer
const adoReducer = (state: AdoState, action: AdoAction): AdoState => {
  switch (action.type) {
    case ActionType.START_LOADING:
      return { ...state, isLoading: true, error: null };

    case ActionType.SET_ERROR:
      return { ...state, isLoading: false, error: action.payload };

    case ActionType.CLEAR_ERROR:
      return { ...state, error: null };

    case ActionType.SET_CONNECTIONS:
      return { ...state, connections: action.payload, isLoading: false };

    case ActionType.SET_PROJECTS:
      return { ...state, projects: action.payload, isLoading: false };

    case ActionType.SET_TEST_PLANS:
      return { ...state, testPlans: action.payload, isLoading: false };

    case ActionType.SET_TEST_SUITES:
      return { ...state, testSuites: action.payload, isLoading: false };

    case ActionType.SET_PIPELINES:
      return { ...state, pipelines: action.payload, isLoading: false };

    case ActionType.SET_SYNC_CONFIG:
      return { ...state, syncConfig: action.payload, isLoading: false };

    case ActionType.SET_SYNC_STATUS:
      return { ...state, syncStatus: action.payload, isLoading: false };

    case ActionType.SET_PIPELINE_CONFIG:
      return { ...state, pipelineConfig: action.payload, isLoading: false };

    default:
      return state;
  }
};

// Context interface
interface AdoIntegrationContextProps {
  connections: AdoConnection[];
  projects: AdoProject[];
  testPlans: AdoTestPlan[];
  testSuites: AdoTestSuite[];
  pipelines: AdoPipeline[];
  syncConfig: SyncConfig | null;
  syncStatus: SyncStatus | null;
  pipelineConfig: PipelineConfig | null;
  isLoading: boolean;
  error: string | null;
  getConnections: () => Promise<AdoConnection[]>;
  createConnection: (connection: Omit<AdoConnection, 'id' | 'createdAt' | 'updatedAt'>) => Promise<AdoConnection>;
  updateConnection: (id: string, connection: Partial<AdoConnection>) => Promise<AdoConnection>;
  deleteConnection: (id: string) => Promise<boolean>;
  validateConnection: (url: string, pat: string, organizationName: string, projectName: string) => Promise<boolean>;
  getProjects: (connectionId: string) => Promise<AdoProject[]>;
  getTestPlans: (connectionId: string, projectId: string) => Promise<AdoTestPlan[]>;
  getTestSuites: (connectionId: string, projectId: string, testPlanId: string) => Promise<AdoTestSuite[]>;
  getPipelines: (connectionId: string, projectId: string) => Promise<AdoPipeline[]>;
  getSyncConfig: (projectId: string) => Promise<SyncConfig | null>;
  saveSyncConfig: (projectId: string, config: SyncConfig) => Promise<SyncConfig>;
  getSyncStatus: (projectId: string) => Promise<SyncStatus | null>;
  startSync: (projectId: string) => Promise<boolean>;
  getPipelineConfig: (projectId: string) => Promise<PipelineConfig | null>;
  savePipelineConfig: (projectId: string, config: PipelineConfig) => Promise<PipelineConfig>;
  triggerPipeline: (projectId: string) => Promise<boolean>;
  clearError: () => void;
}

// Create context
const AdoIntegrationContext = createContext<AdoIntegrationContextProps | undefined>(undefined);

// Provider props
interface AdoIntegrationProviderProps {
  children: ReactNode;
}

// Create provider
export const AdoIntegrationProvider: React.FC<AdoIntegrationProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(adoReducer, initialState);

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

  // Service integration
  const getConnections = useCallback(async (): Promise<AdoConnection[]> => {
    setLoading();
    try {
      const connections = await adoService.getConnections();
      dispatch({ type: ActionType.SET_CONNECTIONS, payload: connections });
      return connections;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get ADO connections';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const createConnection = useCallback(async (connection: Omit<AdoConnection, 'id' | 'createdAt' | 'updatedAt'>): Promise<AdoConnection> => {
    setLoading();
    try {
      const newConnection = await adoService.createConnection(connection);
      // Refresh the connections list
      await getConnections();
      return newConnection;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to create ADO connection';
      setError(message);
      throw error;
    }
  }, [setLoading, setError, getConnections]);

  const updateConnection = useCallback(async (id: string, connection: Partial<AdoConnection>): Promise<AdoConnection> => {
    setLoading();
    try {
      const updatedConnection = await adoService.updateConnection(id, connection);
      // Refresh the connections list
      await getConnections();
      return updatedConnection;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to update ADO connection';
      setError(message);
      throw error;
    }
  }, [setLoading, setError, getConnections]);

  const deleteConnection = useCallback(async (id: string): Promise<boolean> => {
    setLoading();
    try {
      const success = await adoService.deleteConnection(id);
      if (success) {
        // Refresh the connections list
        await getConnections();
      }
      return success;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to delete ADO connection';
      setError(message);
      throw error;
    }
  }, [setLoading, setError, getConnections]);

  const validateConnection = useCallback(async (url: string, pat: string, organizationName: string, projectName: string): Promise<boolean> => {
    setLoading();
    try {
      return await adoService.validateConnection(url, pat, organizationName, projectName);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to validate ADO connection';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getProjects = useCallback(async (connectionId: string): Promise<AdoProject[]> => {
    setLoading();
    try {
      const projects = await adoService.getProjects(connectionId);
      dispatch({ type: ActionType.SET_PROJECTS, payload: projects });
      return projects;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get ADO projects';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getTestPlans = useCallback(async (connectionId: string, projectId: string): Promise<AdoTestPlan[]> => {
    setLoading();
    try {
      const testPlans = await adoService.getTestPlans(connectionId, projectId);
      dispatch({ type: ActionType.SET_TEST_PLANS, payload: testPlans });
      return testPlans;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get ADO test plans';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getTestSuites = useCallback(async (connectionId: string, projectId: string, testPlanId: string): Promise<AdoTestSuite[]> => {
    setLoading();
    try {
      const testSuites = await adoService.getTestSuites(connectionId, projectId, testPlanId);
      dispatch({ type: ActionType.SET_TEST_SUITES, payload: testSuites });
      return testSuites;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get ADO test suites';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getPipelines = useCallback(async (connectionId: string, projectId: string): Promise<AdoPipeline[]> => {
    setLoading();
    try {
      const pipelines = await adoService.getPipelines(connectionId, projectId);
      dispatch({ type: ActionType.SET_PIPELINES, payload: pipelines });
      return pipelines;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get ADO pipelines';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getSyncConfig = useCallback(async (projectId: string): Promise<SyncConfig | null> => {
    setLoading();
    try {
      const config = await adoService.getSyncConfig(projectId);
      dispatch({ type: ActionType.SET_SYNC_CONFIG, payload: config });
      return config;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get sync configuration';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const saveSyncConfig = useCallback(async (projectId: string, config: SyncConfig): Promise<SyncConfig> => {
    setLoading();
    try {
      const savedConfig = await adoService.saveSyncConfig(projectId, config);
      dispatch({ type: ActionType.SET_SYNC_CONFIG, payload: savedConfig });
      return savedConfig;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to save sync configuration';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const getSyncStatus = useCallback(async (projectId: string): Promise<SyncStatus | null> => {
    setLoading();
    try {
      const status = await adoService.getSyncStatus(projectId);
      dispatch({ type: ActionType.SET_SYNC_STATUS, payload: status });
      return status;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get sync status';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const startSync = useCallback(async (projectId: string): Promise<boolean> => {
    setLoading();
    try {
      const success = await adoService.startSync(projectId);
      if (success) {
        // Refresh the sync status
        await getSyncStatus(projectId);
      }
      return success;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to start synchronization';
      setError(message);
      throw error;
    }
  }, [setLoading, setError, getSyncStatus]);

  const getPipelineConfig = useCallback(async (projectId: string): Promise<PipelineConfig | null> => {
    setLoading();
    try {
      const config = await adoService.getPipelineConfig(projectId);
      dispatch({ type: ActionType.SET_PIPELINE_CONFIG, payload: config });
      return config;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to get pipeline configuration';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const savePipelineConfig = useCallback(async (projectId: string, config: PipelineConfig): Promise<PipelineConfig> => {
    setLoading();
    try {
      const savedConfig = await adoService.savePipelineConfig(projectId, config);
      dispatch({ type: ActionType.SET_PIPELINE_CONFIG, payload: savedConfig });
      return savedConfig;
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to save pipeline configuration';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  const triggerPipeline = useCallback(async (projectId: string): Promise<boolean> => {
    setLoading();
    try {
      return await adoService.triggerPipeline(projectId);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Failed to trigger pipeline';
      setError(message);
      throw error;
    }
  }, [setLoading, setError]);

  // Context value
  const contextValue = useMemo(() => ({
    connections: state.connections,
    projects: state.projects,
    testPlans: state.testPlans,
    testSuites: state.testSuites,
    pipelines: state.pipelines,
    syncConfig: state.syncConfig,
    syncStatus: state.syncStatus,
    pipelineConfig: state.pipelineConfig,
    isLoading: state.isLoading,
    error: state.error,
    getConnections,
    createConnection,
    updateConnection,
    deleteConnection,
    validateConnection,
    getProjects,
    getTestPlans,
    getTestSuites,
    getPipelines,
    getSyncConfig,
    saveSyncConfig,
    getSyncStatus,
    startSync,
    getPipelineConfig,
    savePipelineConfig,
    triggerPipeline,
    clearError
  }), [
    state,
    getConnections,
    createConnection,
    updateConnection,
    deleteConnection,
    validateConnection,
    getProjects,
    getTestPlans,
    getTestSuites,
    getPipelines,
    getSyncConfig,
    saveSyncConfig,
    getSyncStatus,
    startSync,
    getPipelineConfig,
    savePipelineConfig,
    triggerPipeline,
    clearError
  ]);

  return (
    <AdoIntegrationContext.Provider value={contextValue}>
      {children}
    </AdoIntegrationContext.Provider>
  );
};

// Custom hook for using the ADO integration context
export const useAdoIntegration = (): AdoIntegrationContextProps => {
  const context = useContext(AdoIntegrationContext);
  if (!context) {
    throw new Error('useAdoIntegration must be used within an AdoIntegrationProvider');
  }
  return context;
}; 