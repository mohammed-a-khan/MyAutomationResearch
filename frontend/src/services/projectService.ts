/**
 * Service for Project Management API interactions
 */
import api from './api';
import { ENDPOINTS } from '../config/apiConfig';
import {
  Project,
  ProjectDetail,
  ProjectListResponse,
  CreateProjectPayload,
  UpdateProjectPayload,
  ProjectFilters,
  ProjectSettings,
  Environment
} from '../types/project';
import { logError } from '../utils/errorHandling';
import { buildApiUrlWithParams } from '../utils/apiClient';

/**
 * Get a list of projects with optional filtering
 * @param filters Optional filtering parameters
 * @param page Page number (1-based)
 * @param size Page size
 * @returns Promise with the project list response
 */
export const getProjects = async (
    filters: ProjectFilters = {},
    page: number = 1,
    size: number = 10
): Promise<ProjectListResponse> => {
  try {
    // Build query parameters
    const queryParams = new URLSearchParams();
    queryParams.append('page', page.toString());
    queryParams.append('size', size.toString());

    // Add filters to query parameters
    if (filters.search) {
      queryParams.append('search', filters.search);
    }

    if (filters.status && filters.status.length > 0) {
      filters.status.forEach(status => {
        queryParams.append('status', status);
      });
    }

    if (filters.types && filters.types.length > 0) {
      filters.types.forEach(type => {
        queryParams.append('type', type);
      });
    }

    if (filters.tags && filters.tags.length > 0) {
      filters.tags.forEach(tag => {
        queryParams.append('tag', tag);
      });
    }

    if (filters.sortBy) {
      queryParams.append('sortBy', filters.sortBy);
      queryParams.append('sortDirection', filters.sortDirection || 'asc');
    }

    console.log('Calling getProjects with:', queryParams.toString());
    const response = await api.get(`${ENDPOINTS.PROJECTS}?${queryParams.toString()}`);

    console.log('API Raw Response:', response);

    // Handle the ApiResponse wrapper
    if (response.data) {
      // Backend returns: { success: true, data: [...], message: "..." }
      if (response.data.success === true && response.data.data) {
        // Handle array of projects
        if (Array.isArray(response.data.data)) {
          return {
            projects: response.data.data,
            pagination: {
              page,
              size,
              totalItems: response.data.data.length,
              totalPages: Math.ceil(response.data.data.length / size)
            }
          };
        }

        // Handle ProjectListResponse object with projects and pagination
        if (typeof response.data.data === 'object' && 'projects' in response.data.data) {
          return response.data.data;
        }
      }

      // Fallback if direct data (not wrapped in success/data/message)
      if (Array.isArray(response.data)) {
        return {
          projects: response.data,
          pagination: {
            page,
            size,
            totalItems: response.data.length,
            totalPages: Math.ceil(response.data.length / size)
          }
        };
      }

      if (response.data.projects && Array.isArray(response.data.projects)) {
        return response.data;
      }
    }

    // If we can't determine the format, return empty to prevent errors
    console.error('Unexpected API response format:', response);
    return {
      projects: [],
      pagination: {
        page,
        size,
        totalItems: 0,
        totalPages: 0
      }
    };
  } catch (error) {
    console.error('Error in getProjects:', error);
    logError(error, 'Get Projects');
    // Return empty result to prevent UI errors
    return {
      projects: [],
      pagination: {
        page,
        size,
        totalItems: 0,
        totalPages: 0
      }
    };
  }
};

/**
 * Helper function to extract data from ApiResponse
 * @param response The API response
 * @returns The extracted data
 */
const extractDataFromApiResponse = <T>(response: any): T => {
  if (!response || !response.data) {
    throw new Error('Invalid response format: no data property');
  }

  // Backend returns: { success: true, data: {...}, message: "..." }
  if (response.data.success === true && response.data.data !== undefined) {
    return response.data.data;
  }

  // If not wrapped in ApiResponse, return directly
  return response.data;
};

/**
 * Get a single project by ID
 * @param projectId Project ID
 * @returns Promise with the project details
 */
export const getProjectById = async (projectId: string): Promise<ProjectDetail> => {
  try {
    const response = await api.get(ENDPOINTS.PROJECT_DETAILS(projectId));
    return extractDataFromApiResponse<ProjectDetail>(response);
  } catch (error) {
    logError(error, 'Get Project');
    throw error;
  }
};

/**
 * Create a new project
 * @param project Project data
 * @returns Promise with the created project
 */
export const createProject = async (project: CreateProjectPayload): Promise<Project> => {
  try {
    const response = await api.post(ENDPOINTS.PROJECTS, project);
    return extractDataFromApiResponse<Project>(response);
  } catch (error) {
    logError(error, 'Create Project');
    throw error;
  }
};

/**
 * Update an existing project
 * @param projectId Project ID
 * @param updates Project updates
 * @returns Promise with the updated project
 */
export const updateProject = async (projectId: string, updates: UpdateProjectPayload): Promise<Project> => {
  try {
    const response = await api.put(ENDPOINTS.PROJECT_DETAILS(projectId), updates);
    return extractDataFromApiResponse<Project>(response);
  } catch (error) {
    logError(error, 'Update Project');
    throw error;
  }
};

/**
 * Delete a project
 * @param projectId Project ID
 * @returns Promise with success status
 */
export const deleteProject = async (projectId: string): Promise<void> => {
  try {
    await api.delete(ENDPOINTS.PROJECT_DETAILS(projectId));
    return;
  } catch (error) {
    logError(error, 'Delete Project');
    throw error;
  }
};

/**
 * Get project configuration
 * @param projectId Project ID
 * @returns Promise with project settings
 */
export const getProjectConfig = async (projectId: string): Promise<ProjectSettings> => {
  try {
    const response = await api.get(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/config`);
    return extractDataFromApiResponse<ProjectSettings>(response);
  } catch (error) {
    logError(error, 'Get Project Config');
    throw error;
  }
};

/**
 * Update project configuration
 * @param projectId Project ID
 * @param settings Project settings
 * @returns Promise with updated settings
 */
export const updateProjectConfig = async (projectId: string, settings: ProjectSettings): Promise<ProjectSettings> => {
  try {
    const response = await api.put(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/config`, settings);
    return extractDataFromApiResponse<ProjectSettings>(response);
  } catch (error) {
    logError(error, 'Update Project Config');
    throw error;
  }
};

/**
 * Get all available tags across projects
 * @returns Promise with array of tags
 */
export const getProjectTags = async (): Promise<string[]> => {
  try {
    const response = await api.get(ENDPOINTS.PROJECT_TAGS);
    return extractDataFromApiResponse<string[]>(response);
  } catch (error) {
    logError(error, 'Get Project Tags');
    throw error;
  }
};

/**
 * Clone an existing project
 * @param projectId Source project ID
 * @param newName Name for the cloned project
 * @returns Promise with the cloned project
 */
export const cloneProject = async (projectId: string, newName: string): Promise<Project> => {
  try {
    const response = await api.post(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/clone`, { name: newName });
    return extractDataFromApiResponse<Project>(response);
  } catch (error) {
    logError(error, 'Clone Project');
    throw error;
  }
};

/**
 * Get environment by ID
 * @param projectId Project ID
 * @param environmentId Environment ID
 * @returns Promise with environment details
 */
export const getEnvironment = async (projectId: string, environmentId: string): Promise<Environment> => {
  try {
    const response = await api.get(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/environments/${environmentId}`);
    return extractDataFromApiResponse<Environment>(response);
  } catch (error) {
    logError(error, 'Get Environment');
    throw error;
  }
};

/**
 * Create a new environment
 * @param projectId Project ID
 * @param environment Environment data
 * @returns Promise with the created environment
 */
export const createEnvironment = async (projectId: string, environment: Omit<Environment, 'id'>): Promise<Environment> => {
  try {
    const response = await api.post(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/environments`, environment);
    return extractDataFromApiResponse<Environment>(response);
  } catch (error) {
    logError(error, 'Create Environment');
    throw error;
  }
};

/**
 * Update an existing environment
 * @param projectId Project ID
 * @param environmentId Environment ID
 * @param environment Environment data
 * @returns Promise with the updated environment
 */
export const updateEnvironment = async (
    projectId: string,
    environmentId: string,
    environment: Omit<Environment, 'id'>
): Promise<Environment> => {
  try {
    const response = await api.put(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/environments/${environmentId}`, environment);
    return extractDataFromApiResponse<Environment>(response);
  } catch (error) {
    logError(error, 'Update Environment');
    throw error;
  }
};

/**
 * Delete an environment
 * @param projectId Project ID
 * @param environmentId Environment ID
 * @returns Promise with success status
 */
export const deleteEnvironment = async (projectId: string, environmentId: string): Promise<void> => {
  try {
    await api.delete(`${ENDPOINTS.PROJECT_DETAILS(projectId)}/environments/${environmentId}`);
    return;
  } catch (error) {
    logError(error, 'Delete Environment');
    throw error;
  }
};