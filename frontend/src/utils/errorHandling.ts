/**
 * Error handling utilities for the application
 */

/**
 * Type definition for API errors
 */
export interface ApiError {
  status?: number;
  message: string;
  code?: string;
  errors?: Record<string, string[]>;
  timestamp?: string;
  apiInfo?: {
    method: string;
    url: string;
  };
}

// Circuit breaker implementation
interface CircuitBreakerState {
  failures: number;
  lastFailure: number;
  status: 'CLOSED' | 'OPEN' | 'HALF_OPEN';
}

const circuitBreakers: Record<string, CircuitBreakerState> = {};

const CIRCUIT_THRESHOLD = 5; // Number of failures before opening circuit
const CIRCUIT_TIMEOUT = 30000; // 30 seconds timeout before trying half-open
const DEFAULT_LOG_LEVEL = 'error'; // Default log level

/**
 * Check circuit breaker status for an endpoint
 * @param endpoint - The API endpoint to check
 * @returns True if the circuit is closed (requests can proceed)
 */
export const checkCircuitBreaker = (endpoint: string): boolean => {
  if (!circuitBreakers[endpoint]) {
    // Initialize circuit breaker if it doesn't exist
    circuitBreakers[endpoint] = {
      failures: 0,
      lastFailure: 0,
      status: 'CLOSED'
    };
    return true;
  }

  const breaker = circuitBreakers[endpoint];
  const now = Date.now();

  if (breaker.status === 'OPEN') {
    // Check if timeout has elapsed to move to half-open
    if (now - breaker.lastFailure > CIRCUIT_TIMEOUT) {
      breaker.status = 'HALF_OPEN';
      return true;
    }
    return false;
  }

  return true;
};

/**
 * Record success for circuit breaker
 * @param endpoint - The API endpoint
 */
export const recordSuccess = (endpoint: string): void => {
  if (circuitBreakers[endpoint]) {
    const breaker = circuitBreakers[endpoint];
    if (breaker.status === 'HALF_OPEN') {
      // Reset on successful half-open request
      breaker.failures = 0;
      breaker.status = 'CLOSED';
    }
  }
};

/**
 * Record failure for circuit breaker
 * @param endpoint - The API endpoint
 */
export const recordFailure = (endpoint: string): void => {
  if (!circuitBreakers[endpoint]) {
    circuitBreakers[endpoint] = {
      failures: 1,
      lastFailure: Date.now(),
      status: 'CLOSED'
    };
    return;
  }

  const breaker = circuitBreakers[endpoint];
  breaker.failures += 1;
  breaker.lastFailure = Date.now();

  if (breaker.status === 'HALF_OPEN' || breaker.failures >= CIRCUIT_THRESHOLD) {
    breaker.status = 'OPEN';
  }
};

/**
 * Get the base endpoint from a URL
 * This is used for circuit breaker grouping
 * @param url - The full URL
 * @returns The base endpoint
 */
export const getBaseEndpoint = (url: string): string => {
  // Extract the base path without query params and specific IDs
  const path = url.split('?')[0];
  // Replace specific IDs with placeholders to group similar endpoints
  return path.replace(/\/[a-f0-9-]{36}\b/g, '/:id');
};

/**
 * Formats various types of errors into a user-friendly message
 * @param error - The error object to format
 * @param defaultMessage - Default message to use if error cannot be parsed
 * @returns Formatted error message string
 */
export const formatError = (error: any, defaultMessage: string = 'An unexpected error occurred'): string => {
  // If error is already a string, return it
  if (typeof error === 'string') {
    return error;
  }

  // Handle API error responses
  if (error?.response?.data) {
    const data = error.response.data;
    
    // Check if the server returned a specific error message
    if (typeof data.message === 'string') {
      return data.message;
    }
    
    // For validation errors, return a combined message
    if (data.errors && typeof data.errors === 'object') {
      const errorMessages = Object.values(data.errors)
        .flat()
        .filter(Boolean);
      
      if (errorMessages.length > 0) {
        return errorMessages.join('. ');
      }
    }
    
    // Handle common HTTP status codes
    const status = error.response.status;
    if (status === 401) {
      return 'Authentication required. Please sign in.';
    } else if (status === 403) {
      return 'You do not have permission to perform this action.';
    } else if (status === 404) {
      return 'The requested resource was not found.';
    } else if (status === 408) {
      return 'The request timed out. Please try again.';
    } else if (status === 429) {
      return 'Too many requests. Please try again later.';
    } else if (status >= 500) {
      return 'A server error occurred. Please try again later.';
    }
    
    return `Server error (${status}): Please try again later.`;
  }
  
  // For network errors
  if (error?.message === 'Network Error') {
    return 'Network error: Please check your internet connection.';
  }
  
  // For timeout errors
  if (error?.code === 'ECONNABORTED') {
    return 'The request timed out. Please try again.';
  }
  
  // For circuit breaker errors
  if (error?.message?.includes('Circuit breaker open')) {
    return 'Service is currently unavailable. Please try again later.';
  }
  
  // For standard Error objects
  if (error instanceof Error) {
    return error.message;
  }
  
  // If we can't determine the error type, return the default message
  return defaultMessage;
};

/**
 * Logs errors to the console in a consistent format
 * In a production app, this would send errors to a monitoring service
 * @param error - The error to log
 * @param context - Additional context about where the error occurred
 * @param level - Log level (debug, info, warn, error)
 */
export const logError = (
  error: any, 
  context: string = '',
  level: 'debug' | 'info' | 'warn' | 'error' = DEFAULT_LOG_LEVEL
): void => {
  const timestamp = new Date().toISOString();
  const errorMessage = formatError(error);
  
  // Extract API info if available
  const apiInfo = error?.apiInfo;
  const apiContext = apiInfo ? `${apiInfo.method} ${apiInfo.url}` : '';
  const fullContext = [context, apiContext].filter(Boolean).join(' - ');
  
  // Apply circuit breaker pattern if we have API info
  if (apiInfo?.url) {
    const baseEndpoint = getBaseEndpoint(apiInfo.url);
    recordFailure(baseEndpoint);
  }
  
  const logPrefix = `[${timestamp}]${fullContext ? ` [${fullContext}]` : ''}`;
  
  // Log based on level
  switch (level) {
    case 'debug':
      console.debug(`${logPrefix} Debug: ${errorMessage}`);
      break;
    case 'info':
      console.info(`${logPrefix} Info: ${errorMessage}`);
      break;
    case 'warn':
      console.warn(`${logPrefix} Warning: ${errorMessage}`);
      break;
    case 'error':
    default:
      console.error(`${logPrefix} Error: ${errorMessage}`);
  }
  
  // Always log detailed error information in development
  // In a real app with proper environment variables, you would check NODE_ENV
  // if (process.env.NODE_ENV !== 'production') {
  console.error('Error details:', error);
  // }
  
  // In a real app, you would also send this to an error monitoring service
  // Such as Sentry, LogRocket, etc.
};

/**
 * Extracts a field-specific validation error message from a server response
 * @param error - The API error response
 * @param fieldName - The name of the field to extract errors for
 * @returns The error message for the field, or null if not found
 */
export const getFieldError = (error: any, fieldName: string): string | null => {
  if (error?.response?.data?.errors && error.response.data.errors[fieldName]) {
    const fieldErrors = error.response.data.errors[fieldName];
    if (fieldErrors && fieldErrors.length > 0) {
      return fieldErrors[0];
    }
  }
  return null;
};

/**
 * Checks if the error is a network connectivity issue
 * @param error - The error to check
 * @returns True if it's a network connectivity issue
 */
export const isNetworkError = (error: any): boolean => {
  return error?.message === 'Network Error' || !error?.response;
};

/**
 * Checks if the error is an authentication issue (401 Unauthorized)
 * @param error - The error to check
 * @returns True if it's an authentication error
 */
export const isAuthError = (error: any): boolean => {
  return error?.response?.status === 401;
};

export default {
  formatError,
  logError,
  getFieldError,
  isNetworkError,
  isAuthError
}; 