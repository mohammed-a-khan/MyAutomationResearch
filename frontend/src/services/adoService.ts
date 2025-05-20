import api from './api';
import {
  AdoConnection,
  AdoProject,
  AdoTestPlan,
  AdoTestSuite,
  AdoPipeline,
  SyncConfig,
  SyncStatus,
  PipelineConfig
} from '../context/AdoIntegrationContext';

const ADO_API_BASE = '/api/ado';

/**
 * Service for Azure DevOps integration
 */
const adoService = {
  /**
   * Get all ADO connections
   */
  async getConnections(): Promise<AdoConnection[]> {
    const response = await api.get(`${ADO_API_BASE}/connections`);
    return response.data;
  },

  /**
   * Create a new ADO connection
   */
  async createConnection(connection: Omit<AdoConnection, 'id' | 'createdAt' | 'updatedAt'>): Promise<AdoConnection> {
    const response = await api.post(`${ADO_API_BASE}/connections`, connection);
    return response.data;
  },

  /**
   * Update an existing ADO connection
   */
  async updateConnection(id: string, connection: Partial<AdoConnection>): Promise<AdoConnection> {
    const response = await api.put(`${ADO_API_BASE}/connections/${id}`, connection);
    return response.data;
  },

  /**
   * Delete an ADO connection
   */
  async deleteConnection(id: string): Promise<boolean> {
    const response = await api.delete(`${ADO_API_BASE}/connections/${id}`);
    return response.status === 200;
  },

  /**
   * Validate an ADO connection
   */
  async validateConnection(url: string, pat: string, organizationName: string, projectName: string): Promise<boolean> {
    const response = await api.post(`${ADO_API_BASE}/connections/validate`, {
      url,
      pat,
      organizationName,
      projectName
    });
    return response.data.valid;
  },

  /**
   * Get projects for a connection
   */
  async getProjects(connectionId: string): Promise<AdoProject[]> {
    const response = await api.get(`${ADO_API_BASE}/connections/${connectionId}/projects`);
    return response.data;
  },

  /**
   * Get test plans for a project
   */
  async getTestPlans(connectionId: string, projectId: string): Promise<AdoTestPlan[]> {
    const response = await api.get(`${ADO_API_BASE}/connections/${connectionId}/projects/${projectId}/test-plans`);
    return response.data;
  },

  /**
   * Get test suites for a test plan
   */
  async getTestSuites(connectionId: string, projectId: string, testPlanId: string): Promise<AdoTestSuite[]> {
    const response = await api.get(
      `${ADO_API_BASE}/connections/${connectionId}/projects/${projectId}/test-plans/${testPlanId}/test-suites`
    );
    return response.data;
  },

  /**
   * Get pipelines for a project
   */
  async getPipelines(connectionId: string, projectId: string): Promise<AdoPipeline[]> {
    const response = await api.get(`${ADO_API_BASE}/connections/${connectionId}/projects/${projectId}/pipelines`);
    return response.data;
  },

  /**
   * Get synchronization configuration for a project
   */
  async getSyncConfig(projectId: string): Promise<SyncConfig> {
    const response = await api.get(`${ADO_API_BASE}/sync/config/${projectId}`);
    return response.data;
  },

  /**
   * Save synchronization configuration for a project
   */
  async saveSyncConfig(projectId: string, config: SyncConfig): Promise<SyncConfig> {
    const response = await api.post(`${ADO_API_BASE}/sync/config/${projectId}`, config);
    return response.data;
  },

  /**
   * Get synchronization status for a project
   */
  async getSyncStatus(projectId: string): Promise<SyncStatus> {
    const response = await api.get(`${ADO_API_BASE}/sync/status/${projectId}`);
    return response.data;
  },

  /**
   * Start synchronization for a project
   */
  async startSync(projectId: string): Promise<boolean> {
    const response = await api.post(`${ADO_API_BASE}/sync/start/${projectId}`);
    return response.status === 200;
  },

  /**
   * Get pipeline configuration for a project
   */
  async getPipelineConfig(projectId: string): Promise<PipelineConfig> {
    const response = await api.get(`${ADO_API_BASE}/pipeline/config/${projectId}`);
    return response.data;
  },

  /**
   * Save pipeline configuration for a project
   */
  async savePipelineConfig(projectId: string, config: PipelineConfig): Promise<PipelineConfig> {
    const response = await api.post(`${ADO_API_BASE}/pipeline/config/${projectId}`, config);
    return response.data;
  },

  /**
   * Trigger a pipeline run for a project
   */
  async triggerPipeline(projectId: string): Promise<boolean> {
    const response = await api.post(`${ADO_API_BASE}/pipeline/trigger/${projectId}`);
    return response.status === 200;
  }
};

export default adoService; 