/**
 * API Service for making HTTP requests
 */
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { API_BASE_URL, API_TIMEOUT, DEFAULT_HEADERS } from '../config/apiConfig';
import { 
  logError, 
  checkCircuitBreaker, 
  recordSuccess, 
  recordFailure, 
  getBaseEndpoint 
} from '../utils/errorHandling';

// Configure defaults
const MAX_RETRIES = 3;
const RETRY_DELAY = 1000; // 1 second

// Retry conditions - only retry on network errors, 5xx responses, or specific 4xx responses
const shouldRetry = (error: AxiosError): boolean => {
  return (
    !error.response || // Network error
    (error.response.status >= 500 && error.response.status < 600) || // Server error
    error.response.status === 429 || // Too many requests
    error.response.status === 408 // Request timeout
  );
};

/**
 * Custom API service for making HTTP requests
 */
class ApiService {
  private instance: AxiosInstance;
  private authToken: string | null = null;

  constructor() {
    // Create an Axios instance with default configuration
    this.instance = axios.create({
      baseURL: API_BASE_URL,
      timeout: API_TIMEOUT,
      headers: DEFAULT_HEADERS,
    });

    // Set up request interceptor for auth and logging
    this.instance.interceptors.request.use(
      (config) => {
        // Add authentication token if available
        if (this.authToken) {
          config.headers = config.headers || {};
          config.headers.Authorization = `Bearer ${this.authToken}`;
        }
        return config;
      },
      (error) => {
        logError(error, 'API Request Interceptor');
        return Promise.reject(error);
      }
    );

    // Set up response interceptor for error handling
    this.instance.interceptors.response.use(
      (response) => {
        return response;
      },
      async (error) => {
        // Extract request config and current retry count
        const config = error.config || {};
        const retryCount = config.retryCount || 0;
        
        // Check if we should retry the request
        if (retryCount < MAX_RETRIES && shouldRetry(error)) {
          // Increase retry count
          config.retryCount = retryCount + 1;
          
          // Wait before retrying
          await new Promise(resolve => setTimeout(resolve, RETRY_DELAY * retryCount));
          
          // Retry the request
          return this.instance(config);
        }
        
        // Handle specific error cases
        if (error.response) {
          // The server responded with an error status
          if (error.response.status === 401) {
            // Clear token and redirect to login if unauthorized
            this.clearAuthToken();
            // In a real app, we would redirect to login page or trigger an auth event
          } else if (error.response.status === 404) {
            // Log 404 errors with more context for debugging
            const url = config.url || '';
            const fullUrl = config.baseURL ? `${config.baseURL}${url}` : url;
            console.error(`[API 404 Error: ${url}]`, `Error: The requested resource was not found. Full URL: ${fullUrl}`);
            logError(error, `API 404 Error: ${url}`);
          } else {
            // Log other server errors with detailed information
            const url = config.url || '';
            const fullUrl = config.baseURL ? `${config.baseURL}${url}` : url;
            console.error(`[API Error ${error.response.status}: ${url}]`, 
              `Message: ${error.response.data?.message || error.message}. Full URL: ${fullUrl}`);
            logError(error, `API Server Error (${error.response.status}): ${url}`);
          }
        } else if (error.request) {
          // The request was made but no response was received - network error
          const url = config.url || '';
          console.error(`[API Network Error: ${url}]`, 'No response received from server. Check network connection.');
          logError(error, `Network Error: ${url}`);
        } else {
          // Something happened in setting up the request
          console.error(`[API Configuration Error]`, error.message);
          logError(error, `Request Configuration Error: ${error.message}`);
        }
        
        return Promise.reject(error);
      }
    );
  }

  /**
   * Sets the authentication token for API requests
   * @param token - The authentication token
   */
  public setAuthToken(token: string): void {
    this.authToken = token;
    localStorage.setItem('auth_token', token);
  }

  /**
   * Clears the authentication token
   */
  public clearAuthToken(): void {
    this.authToken = null;
    localStorage.removeItem('auth_token');
  }

  /**
   * Loads the authentication token from storage
   */
  public loadAuthToken(): void {
    const token = localStorage.getItem('auth_token');
    if (token) {
      this.authToken = token;
    }
  }

  /**
   * Makes a GET request with retry capability
   * @param url - The URL to request
   * @param config - Optional request configuration
   * @returns Promise resolving to the response data
   */
  public async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'GET', url };
      throw circuitError;
    }
    
    try {
      // Log the API request in development mode
      if (process.env.NODE_ENV === 'development') {
        console.debug(`[API Request] GET ${url}`);
      }
      
      const response: AxiosResponse<T> = await this.instance.get(url, config);
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      return response.data;
    } catch (error) {
      // Transform the error to include API info for better debugging
      const apiError = error as any;
      apiError.apiInfo = { method: 'GET', url };
      throw apiError;
    }
  }

  /**
   * Makes a POST request with retry capability
   * @param url - The URL to request
   * @param data - The data to send
   * @param config - Optional request configuration
   * @returns Promise resolving to the response data
   */
  public async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'POST', url };
      throw circuitError;
    }
    
    try {
      // Log the API request in development mode
      if (process.env.NODE_ENV === 'development') {
        console.debug(`[API Request] POST ${url}`);
      }
      
      const response: AxiosResponse<T> = await this.instance.post(url, data, config);
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      return response.data;
    } catch (error) {
      const apiError = error as any;
      apiError.apiInfo = { method: 'POST', url };
      throw apiError;
    }
  }

  /**
   * Makes a PUT request with retry capability
   * @param url - The URL to request
   * @param data - The data to send
   * @param config - Optional request configuration
   * @returns Promise resolving to the response data
   */
  public async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'PUT', url };
      throw circuitError;
    }
    
    try {
      // Log the API request in development mode
      if (process.env.NODE_ENV === 'development') {
        console.debug(`[API Request] PUT ${url}`);
      }
      
      const response: AxiosResponse<T> = await this.instance.put(url, data, config);
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      return response.data;
    } catch (error) {
      const apiError = error as any;
      apiError.apiInfo = { method: 'PUT', url };
      throw apiError;
    }
  }

  /**
   * Makes a DELETE request with retry capability
   * @param url - The URL to request
   * @param config - Optional request configuration
   * @returns Promise resolving to the response data
   */
  public async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'DELETE', url };
      throw circuitError;
    }
    
    try {
      // Log the API request in development mode
      if (process.env.NODE_ENV === 'development') {
        console.debug(`[API Request] DELETE ${url}`);
      }
      
      const response: AxiosResponse<T> = await this.instance.delete(url, config);
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      return response.data;
    } catch (error) {
      const apiError = error as any;
      apiError.apiInfo = { method: 'DELETE', url };
      throw apiError;
    }
  }

  /**
   * Makes a PATCH request with retry capability
   * @param url - The URL to request
   * @param data - The data to send
   * @param config - Optional request configuration
   * @returns Promise resolving to the response data
   */
  public async patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'PATCH', url };
      throw circuitError;
    }
    
    try {
      const response: AxiosResponse<T> = await this.instance.patch(url, data, config);
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      return response.data;
    } catch (error) {
      const apiError = error as any;
      apiError.apiInfo = { method: 'PATCH', url };
      throw apiError;
    }
  }

  /**
   * Downloads a file
   * @param url - The URL to request
   * @param filename - The name to save the file as
   * @param config - Optional request configuration
   * @returns Promise that resolves when the download is complete
   */
  public async downloadFile(url: string, filename: string, config?: AxiosRequestConfig): Promise<void> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'GET', url };
      throw circuitError;
    }
    
    try {
      const response = await this.instance.get(url, {
        ...config,
        responseType: 'blob'
      });
      
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      
      // Create a download link and trigger the download
      const downloadUrl = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = downloadUrl;
      link.setAttribute('download', filename);
      document.body.appendChild(link);
      link.click();
      
      // Clean up
      link.remove();
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      const apiError = error as any;
      apiError.apiInfo = { method: 'GET', url };
      throw apiError;
    }
  }

  /**
   * Uploads a file
   * @param url - The URL to upload to
   * @param file - The file to upload
   * @param progressCallback - Optional callback for upload progress
   * @returns Promise resolving to the response data
   */
  public async uploadFile<T = any>(
    url: string, 
    file: File, 
    progressCallback?: (progress: number) => void
  ): Promise<T> {
    // Check circuit breaker before making the request
    const baseEndpoint = getBaseEndpoint(url);
    if (!checkCircuitBreaker(baseEndpoint)) {
      const circuitError = new Error(`Circuit breaker open for ${baseEndpoint}`);
      (circuitError as any).apiInfo = { method: 'POST', url };
      throw circuitError;
    }
    
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await this.instance.post<T>(url, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
        onUploadProgress: (progressEvent) => {
          if (progressCallback && progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            progressCallback(progress);
          }
        }
      });
      
      // Record success for circuit breaker
      recordSuccess(baseEndpoint);
      return response.data;
    } catch (error) {
      const apiError = error as any;
      apiError.apiInfo = { method: 'POST', url };
      throw apiError;
    }
  }
}

// Create and export a singleton instance
const api = new ApiService();

// Load token from storage on startup
api.loadAuthToken();

export default api; 