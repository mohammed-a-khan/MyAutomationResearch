import api from './api';
import { ApiRequest } from '../types/api';
import { ENDPOINTS } from '../config/apiConfig';

/**
 * Service for API Testing functionality
 */
export const apiTestingService = {
  /**
   * Get all API requests, optionally filtered by project
   * @param projectId Optional project ID to filter by
   * @returns List of API requests
   */
  async getApiRequests(projectId?: string): Promise<ApiRequest[]> {
    try {
      const url = projectId 
        ? `${ENDPOINTS.API_REQUESTS}?projectId=${projectId}`
        : ENDPOINTS.API_REQUESTS;
      return await api.get<ApiRequest[]>(url);
    } catch (error) {
      console.error('Error fetching API requests:', error);
      throw error;
    }
  },

  /**
   * Get an API request by ID
   * @param id API request ID
   * @returns API request
   */
  async getApiRequestById(id: string): Promise<ApiRequest> {
    try {
      return await api.get<ApiRequest>(`${ENDPOINTS.API_REQUESTS}/${id}`);
    } catch (error) {
      console.error(`Error fetching API request with ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Create a new API request
   * @param request API request to create
   * @returns Created API request
   */
  async createApiRequest(request: ApiRequest): Promise<ApiRequest> {
    try {
      return await api.post<ApiRequest>(ENDPOINTS.API_REQUESTS, request);
    } catch (error) {
      console.error('Error creating API request:', error);
      throw error;
    }
  },

  /**
   * Update an existing API request
   * @param request API request to update
   * @returns Updated API request
   */
  async updateApiRequest(request: ApiRequest): Promise<ApiRequest> {
    try {
      return await api.put<ApiRequest>(`${ENDPOINTS.API_REQUESTS}/${request.id}`, request);
    } catch (error) {
      console.error(`Error updating API request with ID ${request.id}:`, error);
      throw error;
    }
  },

  /**
   * Delete an API request
   * @param id ID of the API request to delete
   * @returns True if successfully deleted
   */
  async deleteApiRequest(id: string): Promise<boolean> {
    try {
      await api.delete(`${ENDPOINTS.API_REQUESTS}/${id}`);
      return true;
    } catch (error) {
      console.error(`Error deleting API request with ID ${id}:`, error);
      throw error;
    }
  },

  /**
   * Execute an API request
   * @param request API request to execute
   * @returns API response
   */
  async executeApiRequest(request: ApiRequest): Promise<any> {
    try {
      return await api.post(`${ENDPOINTS.API_REQUEST_EXECUTE(request.id || '')}`, request);
    } catch (error) {
      console.error('Error executing API request:', error);
      throw error;
    }
  },

  /**
   * Get history of API request executions
   * @param requestId API request ID
   * @returns List of execution history items
   */
  async getRequestExecutionHistory(requestId: string): Promise<any[]> {
    try {
      return await api.get(`${ENDPOINTS.API_REQUESTS}/${requestId}/history`);
    } catch (error) {
      console.error(`Error fetching execution history for request ${requestId}:`, error);
      throw error;
    }
  }
}; 