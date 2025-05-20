/**
 * Service for Test Recorder API interactions
 */
import { 
  RecordingOptions, 
  RecordingSession, 
  RecordedEvent,
  CodeGenerationOptions, 
  GeneratedCode,
  Condition,
  Loop,
  DataSource,
  VariableBinding,
  AssertionConfig,
  StepGroup
} from '../types/recorder';
import { apiClient } from '../utils/apiClient';
import { logError } from '../utils/errorHandling';

/**
 * Service for interacting with the recorder API
 */
class RecorderService {
  /**
   * Start a new recording session
   * @param options Recording configuration options
   * @returns The created recording session
   */
  async startRecording(options: RecordingOptions): Promise<RecordingSession> {
    try {
      // Map frontend RecordingOptions to backend RecordingRequest structure
      const requestData = {
        projectId: options.projectId,
        browserType: options.browser,
        framework: options.framework,
        url: options.targetUrl
      };
      
      // Debug logging
      console.log('Sending recorder start request:', requestData);
      
      const response = await apiClient.post('/api/recorder/start', requestData);
      console.log('Recorder start response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Recording start error:', error);
      throw new Error('Failed to start recording session');
    }
  }

  /**
   * Stop the current recording session
   * @param sessionId ID of the recording session to stop
   * @returns The completed recording session
   */
  async stopRecording(sessionId: string): Promise<RecordingSession> {
    try {
      const response = await apiClient.post(`/api/recorder/stop`, { sessionId });
      return response.data;
    } catch (error) {
      throw new Error('Failed to stop recording session');
    }
  }

  /**
   * Get all recorded events for a session
   * @param sessionId ID of the recording session
   * @returns List of recorded events
   */
  async getEvents(sessionId: string): Promise<RecordedEvent[]> {
    try {
      const response = await apiClient.get(`/api/recorder/events`, {
        params: { sessionId }
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to fetch recorded events');
    }
  }

  /**
   * Add a manually created event
   * @param event Event to add to the recording
   * @returns The created event with ID
   */
  async addEvent(event: Partial<RecordedEvent>): Promise<RecordedEvent> {
    try {
      const response = await apiClient.post('/api/recorder/event', event);
      return response.data;
    } catch (error) {
      throw new Error('Failed to add event');
    }
  }

  /**
   * Delete a recorded event
   * @param eventId ID of the event to delete
   * @returns Success status
   */
  async deleteEvent(eventId: string): Promise<boolean> {
    try {
      await apiClient.delete(`/api/recorder/event/${eventId}`);
      return true;
    } catch (error) {
      throw new Error('Failed to delete event');
    }
  }

  /**
   * Reorder recorded events
   * @param sessionId ID of the recording session
   * @param eventIds Array of event IDs in the new order
   * @returns Updated array of events
   */
  async reorderEvents(sessionId: string, eventIds: string[]): Promise<RecordedEvent[]> {
    try {
      const response = await apiClient.put('/api/recorder/events/reorder', {
        sessionId,
        eventIds
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to reorder events');
    }
  }

  /**
   * Generate test code from recorded events
   * @param sessionId ID of the recording session
   * @param options Code generation options
   * @returns Generated test code
   */
  async generateCode(
    sessionId: string, 
    options: CodeGenerationOptions
  ): Promise<GeneratedCode> {
    try {
      const response = await apiClient.get('/api/recorder/code', {
        params: { 
          sessionId,
          ...options
        }
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to generate code');
    }
  }

  /**
   * Update an event's properties
   * @param eventId ID of the event to update
   * @param updates Properties to update
   * @returns Updated event
   */
  async updateEvent(
    eventId: string, 
    updates: Partial<RecordedEvent>
  ): Promise<RecordedEvent> {
    try {
      const response = await apiClient.put(`/api/recorder/event/${eventId}`, updates);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update event');
    }
  }

  /**
   * Get the status of an active recording session
   * @param sessionId ID of the recording session
   * @returns Current session status
   */
  async getSessionStatus(sessionId: string): Promise<RecordingSession> {
    try {
      const response = await apiClient.get(`/api/recorder/session/${sessionId}`);
      return response.data;
    } catch (error) {
      throw new Error('Failed to get session status');
    }
  }

  /**
   * Pause an active recording session
   * @param sessionId ID of the recording session
   * @returns Updated session
   */
  async pauseRecording(sessionId: string): Promise<RecordingSession> {
    try {
      const response = await apiClient.post(`/api/recorder/pause`, { sessionId });
      return response.data;
    } catch (error) {
      throw new Error('Failed to pause recording');
    }
  }

  /**
   * Resume a paused recording session
   * @param sessionId ID of the recording session
   * @returns Updated session
   */
  async resumeRecording(sessionId: string): Promise<RecordingSession> {
    try {
      const response = await apiClient.post(`/api/recorder/resume`, { sessionId });
      return response.data;
    } catch (error) {
      throw new Error('Failed to resume recording');
    }
  }

  /**
   * Add a condition to an event
   * @param parentEventId ID of the parent event
   * @param condition Condition to add
   * @returns The created condition
   */
  async addCondition(parentEventId: string, condition: Condition): Promise<Condition> {
    try {
      const response = await apiClient.post('/api/recorder/condition', {
        parentEventId,
        condition
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to add condition');
    }
  }

  /**
   * Update a condition
   * @param eventId ID of the event containing the condition
   * @param condition Updated condition data
   * @returns The updated condition
   */
  async updateCondition(eventId: string, condition: Condition): Promise<Condition> {
    try {
      const response = await apiClient.put(`/api/recorder/event/${eventId}/condition`, condition);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update condition');
    }
  }

  /**
   * Add a loop to an event
   * @param parentEventId ID of the parent event
   * @param loop Loop configuration to add
   * @returns The created loop
   */
  async addLoop(parentEventId: string, loop: Loop): Promise<Loop> {
    try {
      const response = await apiClient.post('/api/recorder/loop', {
        parentEventId,
        loop
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to add loop');
    }
  }

  /**
   * Update a loop
   * @param eventId ID of the event containing the loop
   * @param loop Updated loop data
   * @returns The updated loop
   */
  async updateLoop(eventId: string, loop: Loop): Promise<Loop> {
    try {
      const response = await apiClient.put(`/api/recorder/event/${eventId}/loop`, loop);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update loop');
    }
  }

  /**
   * Add a data source
   * @param dataSource Data source configuration
   * @returns The created data source
   */
  async addDataSource(dataSource: DataSource): Promise<DataSource> {
    try {
      const response = await apiClient.post('/api/recorder/datasource', dataSource);
      return response.data;
    } catch (error) {
      throw new Error('Failed to add data source');
    }
  }

  /**
   * Update a data source
   * @param dataSourceId ID of the data source
   * @param updates Data source updates
   * @returns The updated data source
   */
  async updateDataSource(dataSourceId: string, updates: Partial<DataSource>): Promise<DataSource> {
    try {
      const response = await apiClient.put(`/api/recorder/datasource/${dataSourceId}`, updates);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update data source');
    }
  }

  /**
   * Delete a data source
   * @param dataSourceId ID of the data source to delete
   * @returns Success status
   */
  async deleteDataSource(dataSourceId: string): Promise<boolean> {
    try {
      await apiClient.delete(`/api/recorder/datasource/${dataSourceId}`);
      return true;
    } catch (error) {
      throw new Error('Failed to delete data source');
    }
  }

  /**
   * Add a variable binding (value capture)
   * @param parentEventId ID of the parent event
   * @param variableBinding Variable binding configuration
   * @returns The created variable binding
   */
  async addVariableBinding(parentEventId: string, variableBinding: VariableBinding): Promise<VariableBinding> {
    try {
      const response = await apiClient.post('/api/recorder/variable', {
        parentEventId,
        variableBinding
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to add variable binding');
    }
  }

  /**
   * Update a variable binding
   * @param eventId ID of the event containing the variable binding
   * @param variableBinding Updated variable binding
   * @returns The updated variable binding
   */
  async updateVariableBinding(eventId: string, variableBinding: VariableBinding): Promise<VariableBinding> {
    try {
      const response = await apiClient.put(`/api/recorder/event/${eventId}/variable`, variableBinding);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update variable binding');
    }
  }

  /**
   * Add an assertion to an event
   * @param parentEventId ID of the parent event
   * @param assertion Assertion configuration
   * @returns The created assertion
   */
  async addAssertion(parentEventId: string, assertion: AssertionConfig): Promise<AssertionConfig> {
    try {
      const response = await apiClient.post('/api/recorder/assertion', {
        parentEventId,
        assertion
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to add assertion');
    }
  }

  /**
   * Update an assertion
   * @param eventId ID of the event containing the assertion
   * @param assertionId ID of the assertion to update
   * @param updates Assertion updates
   * @returns The updated assertion
   */
  async updateAssertion(
    eventId: string,
    assertionId: string,
    updates: Partial<AssertionConfig>
  ): Promise<AssertionConfig> {
    try {
      const response = await apiClient.put(`/api/recorder/event/${eventId}/assertion/${assertionId}`, updates);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update assertion');
    }
  }

  /**
   * Delete an assertion
   * @param eventId ID of the event containing the assertion
   * @param assertionId ID of the assertion to delete
   * @returns Success status
   */
  async deleteAssertion(eventId: string, assertionId: string): Promise<boolean> {
    try {
      await apiClient.delete(`/api/recorder/event/${eventId}/assertion/${assertionId}`);
      return true;
    } catch (error) {
      throw new Error('Failed to delete assertion');
    }
  }

  /**
   * Create a step group
   * @param name Name of the group
   * @param eventIds IDs of events to include in the group
   * @returns The created step group
   */
  async createStepGroup(name: string, eventIds: string[]): Promise<StepGroup> {
    try {
      const response = await apiClient.post('/api/recorder/group', {
        name,
        eventIds
      });
      return response.data;
    } catch (error) {
      throw new Error('Failed to create step group');
    }
  }

  /**
   * Update a step group
   * @param groupId ID of the step group
   * @param updates Group updates
   * @returns The updated step group
   */
  async updateStepGroup(groupId: string, updates: Partial<StepGroup>): Promise<StepGroup> {
    try {
      const response = await apiClient.put(`/api/recorder/group/${groupId}`, updates);
      return response.data;
    } catch (error) {
      throw new Error('Failed to update step group');
    }
  }

  /**
   * Delete a step group
   * @param groupId ID of the step group to delete
   * @returns Success status
   */
  async deleteStepGroup(groupId: string): Promise<boolean> {
    try {
      await apiClient.delete(`/api/recorder/group/${groupId}`);
      return true;
    } catch (error) {
      throw new Error('Failed to delete step group');
    }
  }
}

export const recorderService = new RecorderService(); 