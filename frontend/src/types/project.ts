/**
 * Types for the Project Management module
 */

/**
 * Project status enum
 */
export enum ProjectStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ARCHIVED = 'ARCHIVED',
  DRAFT = 'DRAFT'
}

/**
 * Project type enum
 */
export enum ProjectType {
  WEB = 'WEB',
  MOBILE = 'MOBILE',
  API = 'API',
  DESKTOP = 'DESKTOP',
  HYBRID = 'HYBRID'
}

/**
 * Environment variable
 */
export interface EnvironmentVariable {
  id: string;
  name: string;
  value: string;
  isSecret: boolean;
}

/**
 * Environment configuration
 */
export interface Environment {
  id: string;
  name: string;
  url: string;
  description?: string;
  isDefault: boolean;
  variables: EnvironmentVariable[];
}

/**
 * Test suite
 */
export interface TestSuite {
  id: string;
  name: string;
  description?: string;
  testCount: number;
  lastRunDate?: string;
  successRate?: number;
}

/**
 * Integration configuration
 */
export interface Integration {
  id: string;
  type: 'ADO' | 'GITHUB' | 'GITLAB' | 'JIRA' | 'SLACK';
  name: string;
  status: 'ACTIVE' | 'INACTIVE' | 'ERROR';
  config: Record<string, any>;
}

/**
 * Project settings
 */
export interface ProjectSettings {
  defaultEnvironment?: string;
  defaultBrowser?: string;
  defaultTimeout: number;
  screenshotsEnabled: boolean;
  videoRecordingEnabled: boolean;
  parallelExecutionEnabled: boolean;
  maxParallelInstances: number;
  retryFailedTests: boolean;
  maxRetries: number;
  customSettings: Record<string, any>;
}

/**
 * Base project interface
 */
export interface Project {
  id: string;
  name: string;
  description?: string;
  status: ProjectStatus;
  type: ProjectType;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
  testCount?: number;
  lastRunDate?: string;
  successRate?: number;
  tags?: string[];
  environmentNames?: string[];
}

/**
 * Project with detailed information
 */
export interface ProjectDetail extends Project {
  repositoryUrl?: string;
  baseUrl?: string;
  branches?: string[];
  environments: Environment[];
  testSuites: TestSuite[];
  integrations: Integration[];
  settings: ProjectSettings;
}

/**
 * Project creation payload
 */
export interface CreateProjectPayload {
  name: string;
  description?: string;
  type: ProjectType;
  baseUrl?: string;
  repositoryUrl?: string;
  tags?: string[];
}

/**
 * Project update payload
 */
export interface UpdateProjectPayload {
  name?: string;
  description?: string;
  status?: ProjectStatus;
  type?: ProjectType;
  baseUrl?: string;
  repositoryUrl?: string;
  tags?: string[];
}

/**
 * Project list filter options
 */
export interface ProjectFilters {
  search?: string;
  status?: ProjectStatus[];
  types?: ProjectType[];
  tags?: string[];
  sortBy?: 'name' | 'createdAt' | 'updatedAt' | 'lastRunDate' | 'successRate';
  sortDirection?: 'asc' | 'desc';
}

/**
 * Pagination parameters
 */
export interface PaginationParams {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

/**
 * Project list response 
 */
export interface ProjectListResponse {
  projects: Project[];
  pagination: PaginationParams;
} 