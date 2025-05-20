import React, { createContext, useContext, useReducer, useCallback, ReactNode, useState, useRef } from 'react';
import {
  Project,
  ProjectDetail,
  ProjectListResponse,
  CreateProjectPayload,
  UpdateProjectPayload,
  ProjectFilters,
  ProjectSettings,
  Environment,
  PaginationParams
} from '../types/project';
import * as projectService from '../services/projectService';
import { formatError } from '../utils/errorHandling';

/**
 * Project context state interface
 */
interface ProjectContextState {
  projects: Project[];
  selectedProject: ProjectDetail | null;
  isLoading: boolean;
  error: string | null;
  filters: ProjectFilters;
  pagination: PaginationParams;
  projectTags: string[];
}

/**
 * Action types for the project reducer
 */
enum ActionType {
  SET_PROJECTS = 'SET_PROJECTS',
  SET_SELECTED_PROJECT = 'SET_SELECTED_PROJECT',
  UPDATE_PROJECT = 'UPDATE_PROJECT',
  DELETE_PROJECT = 'DELETE_PROJECT',
  ADD_PROJECT = 'ADD_PROJECT',
  SET_LOADING = 'SET_LOADING',
  SET_ERROR = 'SET_ERROR',
  SET_FILTERS = 'SET_FILTERS',
  SET_PAGINATION = 'SET_PAGINATION',
  SET_PROJECT_TAGS = 'SET_PROJECT_TAGS',
  CLEAR_ERROR = 'CLEAR_ERROR',
}

/**
 * Union type for all project actions
 */
type Action =
    | { type: ActionType.SET_PROJECTS; payload: Project[] }
    | { type: ActionType.SET_SELECTED_PROJECT; payload: ProjectDetail | null }
    | { type: ActionType.UPDATE_PROJECT; payload: Project }
    | { type: ActionType.DELETE_PROJECT; payload: string }
    | { type: ActionType.ADD_PROJECT; payload: Project }
    | { type: ActionType.SET_LOADING; payload: boolean }
    | { type: ActionType.SET_ERROR; payload: string | null }
    | { type: ActionType.SET_FILTERS; payload: ProjectFilters }
    | { type: ActionType.SET_PAGINATION; payload: PaginationParams }
    | { type: ActionType.SET_PROJECT_TAGS; payload: string[] }
    | { type: ActionType.CLEAR_ERROR };

/**
 * Initial state for the project context
 */
const initialState: ProjectContextState = {
  projects: [],
  selectedProject: null,
  isLoading: false,
  error: null,
  filters: {},
  pagination: {
    page: 1,
    size: 10,
    totalItems: 0,
    totalPages: 0,
  },
  projectTags: [],
};

/**
 * Reducer function for the project context
 */
const projectReducer = (state: ProjectContextState, action: Action): ProjectContextState => {
  switch (action.type) {
    case ActionType.SET_PROJECTS:
      return {
        ...state,
        projects: action.payload,
      };

    case ActionType.SET_SELECTED_PROJECT:
      return {
        ...state,
        selectedProject: action.payload,
      };

    case ActionType.UPDATE_PROJECT:
      return {
        ...state,
        projects: state.projects.map(project =>
            project.id === action.payload.id ? action.payload : project
        ),
        selectedProject: state.selectedProject?.id === action.payload.id
            ? { ...state.selectedProject, ...action.payload }
            : state.selectedProject,
      };

    case ActionType.DELETE_PROJECT:
      return {
        ...state,
        projects: state.projects.filter(project => project.id !== action.payload),
        selectedProject: state.selectedProject?.id === action.payload
            ? null
            : state.selectedProject,
      };

    case ActionType.ADD_PROJECT:
      return {
        ...state,
        projects: [...state.projects, action.payload],
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

    case ActionType.SET_FILTERS:
      return {
        ...state,
        filters: action.payload,
      };

    case ActionType.SET_PAGINATION:
      return {
        ...state,
        pagination: action.payload,
      };

    case ActionType.SET_PROJECT_TAGS:
      return {
        ...state,
        projectTags: action.payload,
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
 * Project context interface
 */
interface ProjectContextValue extends ProjectContextState {
  loadProjects: (filters?: ProjectFilters, page?: number, size?: number) => Promise<void>;
  loadProject: (projectId: string) => Promise<void>;
  createProject: (data: CreateProjectPayload) => Promise<Project>;
  updateProject: (projectId: string, data: UpdateProjectPayload) => Promise<Project>;
  deleteProject: (projectId: string) => Promise<void>;
  getProjectConfig: (projectId: string) => Promise<ProjectSettings>;
  updateProjectConfig: (projectId: string, settings: ProjectSettings) => Promise<ProjectSettings>;
  loadProjectTags: () => Promise<void>;
  setFilters: (filters: ProjectFilters) => void;
  clearError: () => void;
  setPage: (page: number) => void;
  getEnvironment: (projectId: string, environmentId: string) => Promise<Environment>;
  createEnvironment: (projectId: string, environment: Omit<Environment, 'id'>) => Promise<Environment>;
  updateEnvironment: (projectId: string, environmentId: string, environment: Omit<Environment, 'id'>) => Promise<Environment>;
  deleteEnvironment: (projectId: string, environmentId: string) => Promise<void>;
}

/**
 * Create the project context with default values
 */
const ProjectContext = createContext<ProjectContextValue>({
  ...initialState,
  loadProjects: async () => {},
  loadProject: async () => {},
  createProject: async () => ({ id: '', name: '', status: '' as any, type: '' as any, createdAt: '', updatedAt: '', createdBy: '' }),
  updateProject: async () => ({ id: '', name: '', status: '' as any, type: '' as any, createdAt: '', updatedAt: '', createdBy: '' }),
  deleteProject: async () => {},
  getProjectConfig: async () => ({
    defaultTimeout: 30000,
    screenshotsEnabled: true,
    videoRecordingEnabled: false,
    parallelExecutionEnabled: false,
    maxParallelInstances: 1,
    retryFailedTests: false,
    maxRetries: 0,
    customSettings: {}
  }),
  updateProjectConfig: async () => ({
    defaultTimeout: 30000,
    screenshotsEnabled: true,
    videoRecordingEnabled: false,
    parallelExecutionEnabled: false,
    maxParallelInstances: 1,
    retryFailedTests: false,
    maxRetries: 0,
    customSettings: {}
  }),
  loadProjectTags: async () => {},
  setFilters: () => {},
  clearError: () => {},
  setPage: () => {},
  getEnvironment: async () => ({ id: '', name: '', url: '', isDefault: false, variables: [] }),
  createEnvironment: async () => ({ id: '', name: '', url: '', isDefault: false, variables: [] }),
  updateEnvironment: async () => ({ id: '', name: '', url: '', isDefault: false, variables: [] }),
  deleteEnvironment: async () => {},
});

/**
 * Project provider props
 */
interface ProjectProviderProps {
  children: ReactNode;
}

/**
 * Project context provider component
 */
export const ProjectProvider: React.FC<ProjectProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(projectReducer, initialState);

  // Add a loading ref to prevent multiple simultaneous API calls
  const isLoadingRef = useRef(false);
  // Add a timer ref to handle filter debounce
  const filterTimerRef = useRef<NodeJS.Timeout | null>(null);

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
   * Load projects with filters and pagination
   */
  const loadProjects = useCallback(async (
      filters?: ProjectFilters,
      page?: number,
      size?: number
  ): Promise<void> => {
    // Prevent multiple simultaneous calls
    if (isLoadingRef.current) {
      console.log('Already loading projects, skipping duplicate call');
      return;
    }

    isLoadingRef.current = true;
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      const updatedFilters = { ...state.filters, ...filters };
      const currentPage = page || (state.pagination?.page || 1);
      const pageSize = size || (state.pagination?.size || 10);

      console.log('Loading projects with filters:', updatedFilters, 'page:', currentPage, 'size:', pageSize);
      const response = await projectService.getProjects(updatedFilters, currentPage, pageSize);

      if (response.projects) {
        dispatch({ type: ActionType.SET_PROJECTS, payload: response.projects });
      }

      if (response.pagination) {
        dispatch({ type: ActionType.SET_PAGINATION, payload: response.pagination });
      }

      // Only update filters if explicitly provided
      if (filters) {
        dispatch({ type: ActionType.SET_FILTERS, payload: updatedFilters });
      }
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to load projects')
      });
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
      // Set a small delay before allowing new API calls to prevent rapid successive calls
      setTimeout(() => {
        isLoadingRef.current = false;
      }, 300);
    }
  }, [state.filters, state.pagination]);

  /**
   * Load a single project by ID
   */
  const loadProject = useCallback(async (projectId: string): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      const project = await projectService.getProjectById(projectId);
      dispatch({ type: ActionType.SET_SELECTED_PROJECT, payload: project });
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to load project')
      });
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Create a new project
   */
  const createNewProject = useCallback(async (data: CreateProjectPayload): Promise<Project> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      const newProject = await projectService.createProject(data);
      dispatch({ type: ActionType.ADD_PROJECT, payload: newProject });
      return newProject;
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to create project')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Update an existing project
   */
  const updateExistingProject = useCallback(async (
      projectId: string,
      data: UpdateProjectPayload
  ): Promise<Project> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      const updatedProject = await projectService.updateProject(projectId, data);
      dispatch({ type: ActionType.UPDATE_PROJECT, payload: updatedProject });
      return updatedProject;
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to update project')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Delete a project
   */
  const deleteExistingProject = useCallback(async (projectId: string): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      await projectService.deleteProject(projectId);
      dispatch({ type: ActionType.DELETE_PROJECT, payload: projectId });
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to delete project')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Get project configuration
   */
  const getProjectConfig = useCallback(async (projectId: string): Promise<ProjectSettings> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      return await projectService.getProjectConfig(projectId);
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to load project configuration')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Update project configuration
   */
  const updateProjectConfig = useCallback(async (
      projectId: string,
      settings: ProjectSettings
  ): Promise<ProjectSettings> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      return await projectService.updateProjectConfig(projectId, settings);
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to update project configuration')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Get environment by ID
   */
  const getEnvironment = useCallback(async (
      projectId: string,
      environmentId: string
  ): Promise<Environment> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      return await projectService.getEnvironment(projectId, environmentId);
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to load environment')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, []);

  /**
   * Create a new environment
   */
  const createEnvironment = useCallback(async (
      projectId: string,
      environment: Omit<Environment, 'id'>
  ): Promise<Environment> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      const newEnvironment = await projectService.createEnvironment(projectId, environment);

      // Update the selected project with the new environment if it's loaded
      if (state.selectedProject && state.selectedProject.id === projectId) {
        const updatedProject = {
          ...state.selectedProject,
          environments: [
            ...(state.selectedProject.environments || []),
            newEnvironment
          ]
        };
        dispatch({ type: ActionType.SET_SELECTED_PROJECT, payload: updatedProject });
      }

      return newEnvironment;
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to create environment')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.selectedProject]);

  /**
   * Update an existing environment
   */
  const updateEnvironment = useCallback(async (
      projectId: string,
      environmentId: string,
      environment: Omit<Environment, 'id'>
  ): Promise<Environment> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      const updatedEnvironment = await projectService.updateEnvironment(
          projectId,
          environmentId,
          environment
      );

      // Update the selected project with the updated environment if it's loaded
      if (state.selectedProject && state.selectedProject.id === projectId) {
        const updatedEnvironments = state.selectedProject.environments?.map(env =>
            env.id === environmentId ? updatedEnvironment : env
        ) || [];

        const updatedProject = {
          ...state.selectedProject,
          environments: updatedEnvironments
        };

        dispatch({ type: ActionType.SET_SELECTED_PROJECT, payload: updatedProject });
      }

      return updatedEnvironment;
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to update environment')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.selectedProject]);

  /**
   * Delete an environment
   */
  const deleteEnvironment = useCallback(async (
      projectId: string,
      environmentId: string
  ): Promise<void> => {
    dispatch({ type: ActionType.SET_LOADING, payload: true });

    try {
      await projectService.deleteEnvironment(projectId, environmentId);

      // Update the selected project by removing the deleted environment if it's loaded
      if (state.selectedProject && state.selectedProject.id === projectId) {
        const updatedEnvironments = state.selectedProject.environments?.filter(
            env => env.id !== environmentId
        ) || [];

        const updatedProject = {
          ...state.selectedProject,
          environments: updatedEnvironments
        };

        dispatch({ type: ActionType.SET_SELECTED_PROJECT, payload: updatedProject });
      }
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to delete environment')
      });
      throw error;
    } finally {
      dispatch({ type: ActionType.SET_LOADING, payload: false });
    }
  }, [state.selectedProject]);

  /**
   * Load all project tags
   */
  const loadProjectTags = useCallback(async (): Promise<void> => {
    try {
      const tags = await projectService.getProjectTags();
      dispatch({ type: ActionType.SET_PROJECT_TAGS, payload: tags });
    } catch (error) {
      dispatch({
        type: ActionType.SET_ERROR,
        payload: formatError(error, 'Failed to load project tags')
      });
    }
  }, []);

  /**
   * Set filters for project list with debounce
   */
  const setFilters = useCallback((filters: ProjectFilters): void => {
    // Update the filters in state immediately
    dispatch({ type: ActionType.SET_FILTERS, payload: filters });

    // Clear any existing timer
    if (filterTimerRef.current) {
      clearTimeout(filterTimerRef.current);
    }

    // Set a new timer to load projects after a delay
    filterTimerRef.current = setTimeout(() => {
      loadProjects(filters);
    }, 300); // 300ms debounce
  }, [loadProjects]);

  /**
   * Clear error message
   */
  const clearError = useCallback((): void => {
    dispatch({ type: ActionType.CLEAR_ERROR });
  }, []);

  /**
   * Set current page with debounce
   */
  const setPage = useCallback((page: number): void => {
    // Add defensive check for pagination
    const currentPagination = state.pagination || initialState.pagination;

    // Update pagination in state immediately
    dispatch({
      type: ActionType.SET_PAGINATION,
      payload: { ...currentPagination, page }
    });

    // Clear any existing timer
    if (filterTimerRef.current) {
      clearTimeout(filterTimerRef.current);
    }

    // Set a new timer to load projects after a delay
    filterTimerRef.current = setTimeout(() => {
      loadProjects(undefined, page);
    }, 300); // 300ms debounce
  }, [state.pagination, loadProjects]);

  // Effect to load projects when filters or pagination changes
  // This is now safely handled by the setFilters and setPage functions with debounce

  // Combine all values and functions for the context
  const contextValue: ProjectContextValue = {
    ...state,
    loadProjects,
    loadProject,
    createProject: createNewProject,
    updateProject: updateExistingProject,
    deleteProject: deleteExistingProject,
    getProjectConfig,
    updateProjectConfig,
    loadProjectTags,
    setFilters,
    clearError,
    setPage,
    getEnvironment,
    createEnvironment,
    updateEnvironment,
    deleteEnvironment,
  };

  return (
      <ProjectContext.Provider value={contextValue}>
        {children}
      </ProjectContext.Provider>
  );
};

/**
 * Custom hook for accessing the project context
 */
export const useProjects = (): ProjectContextValue => {
  const context = useContext(ProjectContext);

  if (!context) {
    throw new Error('useProjects must be used within a ProjectProvider');
  }

  return context;
};

export default ProjectContext;