import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import errorHandling from './errorHandling';
import { API_BASE_URL } from '../config/apiConfig';

// Create base axios instance
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  },
  timeout: 30000 // 30 seconds
});

// Request interceptor - add auth token if available
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('auth_token');
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - handle common errors
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error: AxiosError) => {
    // Log the error and perform any common error handling
    errorHandling.logError(error, 'API Request');
    
    // Handle unauthorized errors (could trigger logout or token refresh)
    if (error.response?.status === 401) {
      // In a real app, handle auth errors (refresh token or logout)
      console.log('Authentication error');
    }
    
    return Promise.reject(error);
  }
);

/**
 * Wrapper for GET requests with proper error handling
 * @param url Endpoint URL
 * @param config Optional axios config
 * @returns Promise with response data
 */
const get = async <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  try {
    const response: AxiosResponse<T> = await apiClient.get(url, config);
    return response.data;
  } catch (error) {
    throw error;
  }
};

/**
 * Wrapper for POST requests with proper error handling
 * @param url Endpoint URL
 * @param data Request body data
 * @param config Optional axios config
 * @returns Promise with response data
 */
const post = async <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  try {
    const response: AxiosResponse<T> = await apiClient.post(url, data, config);
    return response.data;
  } catch (error) {
    throw error;
  }
};

/**
 * Wrapper for PUT requests with proper error handling
 * @param url Endpoint URL
 * @param data Request body data
 * @param config Optional axios config
 * @returns Promise with response data
 */
const put = async <T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> => {
  try {
    const response: AxiosResponse<T> = await apiClient.put(url, data, config);
    return response.data;
  } catch (error) {
    throw error;
  }
};

/**
 * Wrapper for DELETE requests with proper error handling
 * @param url Endpoint URL
 * @param config Optional axios config
 * @returns Promise with response data
 */
const del = async <T = any>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  try {
    const response: AxiosResponse<T> = await apiClient.delete(url, config);
    return response.data;
  } catch (error) {
    throw error;
  }
};

/**
 * API Client Utilities
 * Provides helper functions for consistent API path construction
 */

/**
 * Constructs a complete API URL from a path
 * @param path API endpoint path
 * @returns Properly formatted API URL
 */
export const buildApiUrl = (path: string): string => {
  // Remove leading slash from path if present to avoid double slashes
  const normalizedPath = path.startsWith('/') ? path.substring(1) : path;
  
  // Combine base URL and path
  return `${API_BASE_URL}/${normalizedPath}`;
};

/**
 * Builds query parameters string from an object
 * @param params Object containing query parameters
 * @returns Properly formatted query string (including the ? prefix)
 */
export const buildQueryParams = (params: Record<string, any>): string => {
  if (!params || Object.keys(params).length === 0) {
    return '';
  }
  
  const queryParams = Object.entries(params)
    .filter(([_, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
    .join('&');
  
  return queryParams ? `?${queryParams}` : '';
};

/**
 * Constructs a complete API URL with query parameters
 * @param path API endpoint path
 * @param params Query parameters object
 * @returns Fully constructed API URL with query parameters
 */
export const buildApiUrlWithParams = (path: string, params?: Record<string, any>): string => {
  const baseUrl = buildApiUrl(path);
  const queryString = params ? buildQueryParams(params) : '';
  return `${baseUrl}${queryString}`;
};

/**
 * Validates an API response and throws an error if it's not valid
 * @param response The API response to validate
 * @returns The validated response
 * @throws Error if the response is not valid
 */
export const validateApiResponse = <T>(response: T): T => {
  if (!response) {
    throw new Error('API returned an empty response');
  }
  return response;
};

// Export both the axios instance and the wrapper methods
export {
  apiClient,
  get,
  post,
  put,
  del as delete
}; 