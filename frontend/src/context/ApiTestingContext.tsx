import React, { createContext, useState, useContext, useEffect } from 'react';
import { ApiRequest, ApiAssertion } from '../types/api';
import { apiTestingService } from '../services/apiTestingService';

interface ApiTestingContextType {
  // API Requests
  apiRequests: ApiRequest[];
  currentRequest: ApiRequest | null;
  currentResponse: any | null;
  isLoading: boolean;
  error: string | null;
  
  // API Request Management
  fetchApiRequests: (projectId?: string) => Promise<void>;
  getRequestById: (id: string) => Promise<void>;
  createRequest: (request: ApiRequest) => Promise<ApiRequest>;
  updateRequest: (request: ApiRequest) => Promise<ApiRequest>;
  deleteRequest: (id: string) => Promise<boolean>;
  
  // API Request Execution
  executeRequest: (request: ApiRequest) => Promise<any>;
  validateResponse: (response: any, assertions: ApiAssertion[]) => boolean;
  
  // API Request Form State
  setCurrentRequest: (request: ApiRequest | null) => void;
  resetCurrentRequest: () => void;
}

const defaultApiRequest: ApiRequest = {
  id: '',
  projectId: '',
  name: '',
  description: '',
  method: 'GET',
  url: '',
  headers: {},
  queryParams: {},
  bodyType: 'none',
  assertions: [],
  variables: [],
  createdAt: Date.now(),
  updatedAt: Date.now()
};

const ApiTestingContext = createContext<ApiTestingContextType | undefined>(undefined);

export const ApiTestingProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [apiRequests, setApiRequests] = useState<ApiRequest[]>([]);
  const [currentRequest, setCurrentRequest] = useState<ApiRequest | null>(null);
  const [currentResponse, setCurrentResponse] = useState<any | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchApiRequests = async (projectId?: string) => {
    try {
      setIsLoading(true);
      setError(null);
      const requests = await apiTestingService.getApiRequests(projectId);
      setApiRequests(requests);
    } catch (err) {
      setError('Failed to fetch API requests');
      console.error('Error fetching API requests:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const getRequestById = async (id: string) => {
    try {
      setIsLoading(true);
      setError(null);
      const request = await apiTestingService.getApiRequestById(id);
      setCurrentRequest(request);
    } catch (err) {
      setError('Failed to fetch API request');
      console.error('Error fetching API request:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const createRequest = async (request: ApiRequest): Promise<ApiRequest> => {
    try {
      setIsLoading(true);
      setError(null);
      const createdRequest = await apiTestingService.createApiRequest(request);
      setApiRequests(prev => [...prev, createdRequest]);
      return createdRequest;
    } catch (err) {
      setError('Failed to create API request');
      console.error('Error creating API request:', err);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const updateRequest = async (request: ApiRequest): Promise<ApiRequest> => {
    try {
      setIsLoading(true);
      setError(null);
      const updatedRequest = await apiTestingService.updateApiRequest(request);
      setApiRequests(prev => prev.map(r => r.id === updatedRequest.id ? updatedRequest : r));
      return updatedRequest;
    } catch (err) {
      setError('Failed to update API request');
      console.error('Error updating API request:', err);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const deleteRequest = async (id: string): Promise<boolean> => {
    try {
      setIsLoading(true);
      setError(null);
      const success = await apiTestingService.deleteApiRequest(id);
      if (success) {
        setApiRequests(prev => prev.filter(r => r.id !== id));
      }
      return success;
    } catch (err) {
      setError('Failed to delete API request');
      console.error('Error deleting API request:', err);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const executeRequest = async (request: ApiRequest) => {
    try {
      setIsLoading(true);
      setError(null);
      const response = await apiTestingService.executeApiRequest(request);
      setCurrentResponse(response);
      return response;
    } catch (err) {
      setError('Failed to execute API request');
      console.error('Error executing API request:', err);
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const validateResponse = (response: any, assertions: ApiAssertion[]): boolean => {
    // Simple implementation - should be expanded for more complex validation
    return assertions.every(assertion => {
      try {
        switch (assertion.type) {
          case 'status':
            return String(response.status) === assertion.expected;
          case 'responseTime':
            const responseTime = response.responseTime || 0;
            const expected = Number(assertion.expected);
            const operator = assertion.operator;
            switch (operator) {
              case '<': return responseTime < expected;
              case '<=': return responseTime <= expected;
              case '>': return responseTime > expected;
              case '>=': return responseTime >= expected;
              case '=': return responseTime === expected;
              default: return false;
            }
          // Additional validation types would be implemented here
          default:
            return false;
        }
      } catch (err) {
        console.error('Error validating assertion:', err);
        return false;
      }
    });
  };

  const resetCurrentRequest = () => {
    setCurrentRequest({ ...defaultApiRequest });
    setCurrentResponse(null);
  };

  const value = {
    apiRequests,
    currentRequest,
    currentResponse,
    isLoading,
    error,
    fetchApiRequests,
    getRequestById,
    createRequest,
    updateRequest,
    deleteRequest,
    executeRequest,
    validateResponse,
    setCurrentRequest,
    resetCurrentRequest
  };

  return (
    <ApiTestingContext.Provider value={value}>
      {children}
    </ApiTestingContext.Provider>
  );
};

export const useApiTesting = (): ApiTestingContextType => {
  const context = useContext(ApiTestingContext);
  if (context === undefined) {
    throw new Error('useApiTesting must be used within an ApiTestingProvider');
  }
  return context;
}; 