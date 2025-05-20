import api from './api';
import { ENDPOINTS } from '../config/apiConfig';

// Types
export interface ExportConfig {
  framework: string;
  language: string;
  includeFramework: boolean;
  includeConfig: boolean;
  includeEnvironment: boolean;
  outputStructure: string;
  packageName?: string;
  generatePageObjects: boolean;
  includeComments?: boolean;
  generateDocumentation?: boolean;
  includeAssertions?: boolean;
  targetFrameworkVersion?: string;
  customOutputPath?: string;
}

export interface FrameworkOption {
  id: string;
  name: string;
  description: string;
  languages: string[];
  features: string[];
}

export interface ExportResponse {
  projectId: string;
  projectName: string;
  availableFrameworks: FrameworkOption[];
  availableLanguages: string[];
  availableStructures: { [key: string]: string };
}

/**
 * Get export options for a project
 * @param projectId Project ID
 * @returns Promise resolving to available export options
 */
export const getExportOptions = async (projectId: string): Promise<ExportResponse> => {
  return await api.get(`${ENDPOINTS.EXPORT_OPTIONS}?projectId=${projectId}`);
};

/**
 * Export a complete project with all test cases
 * @param projectId Project ID
 * @param config Export configuration
 * @returns Promise resolving when the export is downloaded
 */
export const exportProject = async (projectId: string, config: ExportConfig): Promise<void> => {
  const filename = `${projectId}-export.zip`;
  return await api.downloadFile(`${ENDPOINTS.EXPORT_EXECUTE}?projectId=${projectId}`, filename, {
    method: 'POST',
    data: config,
    headers: {
      'Content-Type': 'application/json'
    }
  });
};

/**
 * Export a specific test suite
 * @param projectId Project ID
 * @param testSuiteId Test suite ID
 * @param config Export configuration
 * @returns Promise resolving when the export is downloaded
 */
export const exportTestSuite = async (
  projectId: string,
  testSuiteId: string,
  config: ExportConfig
): Promise<void> => {
  const filename = `testsuite-${testSuiteId}-export.zip`;
  return await api.downloadFile(
    `${ENDPOINTS.EXPORT_EXECUTE}?projectId=${projectId}&testSuiteId=${testSuiteId}`,
    filename,
    {
      method: 'POST',
      data: config,
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );
};

/**
 * Export a specific test case
 * @param projectId Project ID
 * @param testCaseId Test case ID
 * @param config Export configuration
 * @returns Promise resolving when the export is downloaded
 */
export const exportTestCase = async (
  projectId: string,
  testCaseId: string,
  config: ExportConfig
): Promise<void> => {
  const filename = `testcase-${testCaseId}-export.zip`;
  return await api.downloadFile(
    `${ENDPOINTS.EXPORT_EXECUTE}?projectId=${projectId}&testCaseId=${testCaseId}`,
    filename,
    {
      method: 'POST',
      data: config,
      headers: {
        'Content-Type': 'application/json'
      }
    }
  );
};

/**
 * Generate a preview of exported code
 * @param projectId Project ID
 * @param config Export configuration
 * @returns Promise resolving to a code preview string
 */
export const generatePreviewCode = async (
  projectId: string,
  config: ExportConfig
): Promise<string> => {
  try {
    const response = await api.post(`${ENDPOINTS.EXPORT_PREVIEW}?projectId=${projectId}`, config);
    return response.preview || '// No preview available';
  } catch (error) {
    console.error('Error generating code preview:', error);
    throw error;
  }
}; 