import React, { createContext, useContext, useReducer, useCallback, useEffect, ReactNode } from 'react';
import { 
  RecordedEvent,
  RecordingOptions,
  RecordingSession,
  RecordingStatus,
  RecorderContextState,
  ElementInfo,
  CodeGenerationOptions,
  RecordedElement,
  Condition,
  Loop,
  DataSource,
  VariableBinding,
  AssertionConfig,
  StepGroup,
  RecordedEventType
} from '../types/recorder';
import { recorderService } from '../services/recorderService';
import wsService, { WsMessageType, ConnectionStatus } from '../services/wsService';
import { formatError } from '../utils/errorHandling';

// Define action types
type RecorderAction = 
  | { type: 'START_RECORDING'; payload: { options: RecordingOptions } }
  | { type: 'STOP_RECORDING' }
  | { type: 'PAUSE_RECORDING' }
  | { type: 'RESUME_RECORDING' }
  | { type: 'SET_SESSION'; payload: { session: RecordingSession } }
  | { type: 'UPDATE_STATUS'; payload: { status: RecordingStatus } }
  | { type: 'ADD_EVENT'; payload: { event: RecordedEvent } }
  | { type: 'UPDATE_EVENT'; payload: { eventId: string; updates: Partial<RecordedEvent> } }
  | { type: 'DELETE_EVENT'; payload: { eventId: string } }
  | { type: 'REORDER_EVENTS'; payload: { eventIds: string[] } }
  | { type: 'SET_EVENTS'; payload: { events: RecordedEvent[] } }
  | { type: 'SELECT_EVENT'; payload: { event: RecordedEvent | null } }
  | { type: 'SET_INSPECTED_ELEMENT'; payload: { element: ElementInfo | null } }
  | { type: 'SET_GENERATED_CODE'; payload: { code: string | null } }
  | { type: 'SET_ERROR'; payload: { error: string | null } }
  | { type: 'SET_LOADING'; payload: { isLoading: boolean } }
  | { type: 'RESET_STATE' }
  | { type: 'SET_CONNECTION_STATUS'; payload: { connectionStatus: ConnectionStatus } }
  // New actions for advanced features
  | { type: 'ADD_CONDITION'; payload: { parentEventId: string; condition: Condition } }
  | { type: 'UPDATE_CONDITION'; payload: { eventId: string; condition: Condition } }
  | { type: 'ADD_LOOP'; payload: { parentEventId: string; loop: Loop } }
  | { type: 'UPDATE_LOOP'; payload: { eventId: string; loop: Loop } }
  | { type: 'ADD_DATA_SOURCE'; payload: { eventId: string; dataSource: DataSource } }
  | { type: 'UPDATE_DATA_SOURCE'; payload: { eventId: string; dataSource: DataSource } }
  | { type: 'ADD_VARIABLE_BINDING'; payload: { eventId: string; binding: VariableBinding } }
  | { type: 'UPDATE_VARIABLE_BINDING'; payload: { eventId: string; binding: VariableBinding } }
  | { type: 'ADD_ASSERTION'; payload: { eventId: string; assertion: AssertionConfig } }
  | { type: 'UPDATE_ASSERTION'; payload: { eventId: string; assertion: AssertionConfig } }
  | { type: 'GROUP_EVENTS'; payload: { eventIds: string[]; groupConfig: StepGroup } }
  | { type: 'UNGROUP_EVENTS'; payload: { groupId: string } };

// Define initial state
const initialState: RecorderContextState = {
  session: null,
  status: RecordingStatus.IDLE,
  events: [],
  selectedEvent: null,
  inspectedElement: null,
  generatedCode: null,
  error: null,
  isLoading: false,
  connectionStatus: ConnectionStatus.DISCONNECTED
};

// Reducer function
const recorderReducer = (state: RecorderContextState, action: RecorderAction): RecorderContextState => {
  switch (action.type) {
    case 'START_RECORDING':
      return {
        ...state,
        status: RecordingStatus.INITIALIZING,
        isLoading: true,
        error: null
      };
    
    case 'STOP_RECORDING':
      return {
        ...state,
        status: RecordingStatus.STOPPING,
        isLoading: true
      };
    
    case 'PAUSE_RECORDING':
      return {
        ...state,
        status: RecordingStatus.PAUSED,
        isLoading: true
      };
    
    case 'RESUME_RECORDING':
      return {
        ...state,
        status: RecordingStatus.RECORDING,
        isLoading: true
      };
    
    case 'SET_SESSION':
      return {
        ...state,
        session: action.payload.session,
        status: action.payload.session.status,
        events: action.payload.session.events || state.events,
        isLoading: false
      };
    
    case 'UPDATE_STATUS':
      return {
        ...state,
        status: action.payload.status,
        isLoading: false
      };
    
    case 'ADD_EVENT':
      return {
        ...state,
        events: [...state.events, action.payload.event],
        selectedEvent: action.payload.event
      };
    
    case 'UPDATE_EVENT':
      return {
        ...state,
        events: state.events.map(event => 
          event.id === action.payload.eventId 
            ? { ...event, ...action.payload.updates } 
            : event
        )
      };
    
    case 'DELETE_EVENT':
      return {
        ...state,
        events: state.events.filter(event => event.id !== action.payload.eventId),
        selectedEvent: state.selectedEvent?.id === action.payload.eventId 
          ? null 
          : state.selectedEvent
      };
    
    case 'REORDER_EVENTS':
      return {
        ...state,
        events: action.payload.eventIds.map(id => 
          state.events.find(event => event.id === id)!
        )
      };
    
    case 'SET_EVENTS':
      return {
        ...state,
        events: action.payload.events,
        selectedEvent: action.payload.events.length > 0 
          ? action.payload.events[action.payload.events.length - 1] 
          : null
      };
    
    case 'SELECT_EVENT':
      return {
        ...state,
        selectedEvent: action.payload.event
      };
    
    case 'SET_INSPECTED_ELEMENT':
      return {
        ...state,
        inspectedElement: action.payload.element
      };
    
    case 'SET_GENERATED_CODE':
      return {
        ...state,
        generatedCode: action.payload.code,
        isLoading: false
      };
    
    case 'SET_ERROR':
      return {
        ...state,
        error: action.payload.error,
        isLoading: false
      };
    
    case 'SET_LOADING':
      return {
        ...state,
        isLoading: action.payload.isLoading
      };
    
    case 'SET_CONNECTION_STATUS':
      return {
        ...state,
        connectionStatus: action.payload.connectionStatus
      };
    
    case 'RESET_STATE':
      return {
        ...initialState
      };
    
    // Advanced feature handlers
    case 'ADD_CONDITION':
    case 'UPDATE_CONDITION':
    case 'ADD_LOOP':
    case 'UPDATE_LOOP':
    case 'ADD_DATA_SOURCE':
    case 'UPDATE_DATA_SOURCE':
    case 'ADD_VARIABLE_BINDING':
    case 'UPDATE_VARIABLE_BINDING':
    case 'ADD_ASSERTION':
    case 'UPDATE_ASSERTION':
    case 'GROUP_EVENTS':
    case 'UNGROUP_EVENTS':
      // These would be implemented with specific logic for each advanced feature
      console.log(`Action ${action.type} not fully implemented yet`);
      return state;
    
    default:
      console.warn('Unknown action type:', (action as any).type);
      return state;
  }
};

// Define the context value type
interface RecorderContextValue {
  state: RecorderContextState;
  startRecording: (options: RecordingOptions) => Promise<void>;
  stopRecording: () => Promise<void>;
  pauseRecording: () => Promise<void>;
  resumeRecording: () => Promise<void>;
  addEvent: (event: RecordedEvent) => void;
  updateEvent: (eventId: string, updates: Partial<RecordedEvent>) => void;
  deleteEvent: (eventId: string) => void;
  reorderEvents: (eventIds: string[]) => void;
  selectEvent: (event: RecordedEvent | null) => void;
  setInspectedElement: (element: ElementInfo | null) => void;
  generateCode: (options: CodeGenerationOptions) => Promise<void>;
  resetState: () => void;
  reconnectWebSocket: () => Promise<void>;
  checkRecorderStatus: () => Promise<void>;
  // Add assertion methods
  addAssertion: (eventId: string, assertion: AssertionConfig) => Promise<void>;
  updateAssertion: (eventId: string, assertionId: string, updates: Partial<AssertionConfig>) => Promise<void>;
  deleteAssertion: (eventId: string, assertionId: string) => Promise<void>;
  // Add condition methods
  addCondition: (parentEventId: string, condition: Condition) => Promise<void>;
  updateCondition: (eventId: string, condition: Condition) => Promise<void>;
  // Add data source methods
  addDataSource: (dataSource: DataSource) => Promise<void>;
  updateDataSource: (dataSourceId: string, updates: Partial<DataSource>) => Promise<void>;
  deleteDataSource: (dataSourceId: string) => Promise<void>;
  // Add loop methods
  addLoop: (parentEventId: string, loop: Loop) => Promise<void>;
  updateLoop: (eventId: string, loop: Loop) => Promise<void>;
  // Add step group methods
  createStepGroup: (name: string, eventIds: string[]) => Promise<void>;
  updateStepGroup: (groupId: string, updates: Partial<StepGroup>) => Promise<void>;
  deleteStepGroup: (groupId: string) => Promise<void>;
  // Add variable binding methods
  addVariableBinding: (parentEventId: string, binding: VariableBinding) => Promise<void>;
  updateVariableBinding: (eventId: string, binding: VariableBinding) => Promise<void>;
}

// Create context
const RecorderContext = createContext<RecorderContextValue | undefined>(undefined);

// Provider component props
interface RecorderProviderProps {
  children: ReactNode;
}

// Provider component
export const RecorderProvider: React.FC<RecorderProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(recorderReducer, initialState);
  
  // Setup WebSocket connection status listener
  useEffect(() => {
    const unsubscribe = wsService.subscribeToStatus((status) => {
      dispatch({ type: 'SET_CONNECTION_STATUS', payload: { connectionStatus: status } });
      
      // If connection is lost during recording, attempt to reconnect
      if (status === ConnectionStatus.DISCONNECTED && 
          state.status === RecordingStatus.RECORDING && 
          state.session?.id) {
        console.debug('WebSocket disconnected during active recording, attempting to reconnect');
        wsService.connect(state.session.id)
          .catch(err => console.error('Failed to reconnect WebSocket:', err));
      }
    });
    
    return () => unsubscribe();
  }, [state.status, state.session?.id]);
  
  // Listen for recorded events from WebSocket
  useEffect(() => {
    const unsubscribe = wsService.subscribe(WsMessageType.EVENT_RECORDED, (eventData: RecordedEvent) => {
      console.debug('Received event from WebSocket:', eventData);
      if (eventData && eventData.id) {
        dispatch({ type: 'ADD_EVENT', payload: { event: eventData } });
      } else {
        console.warn('Received invalid event data from WebSocket:', eventData);
      }
    });
    
    return () => unsubscribe();
  }, []);
  
  // Listen for session status updates
  useEffect(() => {
    const unsubscribe = wsService.subscribe(WsMessageType.SESSION_STATUS, (statusData: { status: RecordingStatus }) => {
      console.debug('Received status update from WebSocket:', statusData);
      if (statusData && statusData.status) {
        dispatch({ type: 'UPDATE_STATUS', payload: { status: statusData.status } });
      }
    });
    
    return () => unsubscribe();
  }, []);
  
  // Listen for errors
  useEffect(() => {
    const unsubscribe = wsService.subscribe(WsMessageType.ERROR, (errorData: { message: string }) => {
      console.error('Received error from WebSocket:', errorData);
      if (errorData && errorData.message) {
        dispatch({ type: 'SET_ERROR', payload: { error: errorData.message } });
      }
    });
    
    return () => unsubscribe();
  }, []);

  // Start recording session
  const startRecording = useCallback(async (options: RecordingOptions): Promise<void> => {
    dispatch({ type: 'START_RECORDING', payload: { options } });
    
    try {
      const session = await recorderService.startRecording(options);
      dispatch({ type: 'SET_SESSION', payload: { session } });
      
      // Connect to WebSocket with the session ID
      if (session?.id) {
        try {
          await wsService.connect(session.id);
          console.debug('WebSocket connected for session', session.id);
        } catch (err) {
          console.error('Failed to connect to WebSocket:', err);
          // Continue without WebSocket for now, we'll reconnect when needed
        }
      }
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to start recording session' } });
      throw error; // Rethrow to handle in UI
    }
  }, []);

  // Stop recording session
  const stopRecording = useCallback(async (): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session to stop' } });
      return;
    }
    
    dispatch({ type: 'STOP_RECORDING' });
    
    try {
      const session = await recorderService.stopRecording(state.session.id);
      dispatch({ type: 'SET_SESSION', payload: { session } });
      
      // Disconnect WebSocket
      wsService.disconnect();
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to stop recording session' } });
      throw error; // Rethrow to handle in UI
    }
  }, [state.session?.id]);

  // Pause recording session
  const pauseRecording = useCallback(async (): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session to pause' } });
      return;
    }
    
    dispatch({ type: 'PAUSE_RECORDING' });
    
    try {
      const session = await recorderService.pauseRecording(state.session.id);
      dispatch({ type: 'SET_SESSION', payload: { session } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to pause recording session' } });
      throw error;
    }
  }, [state.session?.id]);

  // Resume recording session
  const resumeRecording = useCallback(async (): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session to resume' } });
      return;
    }
    
    dispatch({ type: 'RESUME_RECORDING' });
    
    try {
      const session = await recorderService.resumeRecording(state.session.id);
      dispatch({ type: 'SET_SESSION', payload: { session } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to resume recording session' } });
      throw error;
    }
  }, [state.session?.id]);
  
  // Add recording event
  const addEvent = useCallback((event: RecordedEvent): void => {
    dispatch({ type: 'ADD_EVENT', payload: { event } });
  }, []);
  
  // Update recording event
  const updateEvent = useCallback((eventId: string, updates: Partial<RecordedEvent>): void => {
    dispatch({ type: 'UPDATE_EVENT', payload: { eventId, updates } });
  }, []);
  
  // Delete recording event
  const deleteEvent = useCallback((eventId: string): void => {
    dispatch({ type: 'DELETE_EVENT', payload: { eventId } });
  }, []);
  
  // Reorder recording events
  const reorderEvents = useCallback((eventIds: string[]): void => {
    dispatch({ type: 'REORDER_EVENTS', payload: { eventIds } });
  }, []);
  
  // Select recording event
  const selectEvent = useCallback((event: RecordedEvent | null): void => {
    dispatch({ type: 'SELECT_EVENT', payload: { event } });
  }, []);
  
  // Set inspected element
  const setInspectedElement = useCallback((element: ElementInfo | null): void => {
    dispatch({ type: 'SET_INSPECTED_ELEMENT', payload: { element } });
  }, []);
  
  // Generate code from recorded events
  const generateCode = useCallback(async (options: CodeGenerationOptions): Promise<void> => {
    if (!state.session?.id || state.events.length === 0) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No events to generate code from' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      const codeResult = await recorderService.generateCode(state.session.id, options);
      // If codeResult is a GeneratedCode object, extract the testCode property
      const code = typeof codeResult === 'string' ? codeResult : codeResult.testCode;
      dispatch({ type: 'SET_GENERATED_CODE', payload: { code } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to generate code' } });
      throw error;
    }
  }, [state.session?.id, state.events]);
  
  // Reset state
  const resetState = useCallback((): void => {
    wsService.disconnect();
    dispatch({ type: 'RESET_STATE' });
  }, []);
  
  // Reconnect WebSocket
  const reconnectWebSocket = useCallback(async (): Promise<void> => {
    if (!state.session?.id) {
      console.warn('Cannot reconnect WebSocket: No active session');
      return;
    }
    
    try {
      await wsService.connect(state.session.id);
      console.debug('WebSocket reconnected for session', state.session.id);
    } catch (error) {
      console.error('Failed to reconnect WebSocket:', error);
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to reconnect to recording session' } });
      throw error;
    }
  }, [state.session?.id]);
  
  // Check recorder status
  const checkRecorderStatus = useCallback(async (): Promise<void> => {
    if (!state.session?.id) {
      return;
    }
    
    try {
      const session = await recorderService.getSession(state.session.id);
      dispatch({ type: 'SET_SESSION', payload: { session } });
      
      // If recording but WebSocket disconnected, reconnect
      if (session.status === RecordingStatus.RECORDING && 
          state.connectionStatus !== ConnectionStatus.CONNECTED) {
        await reconnectWebSocket();
      }
    } catch (error) {
      console.error('Failed to check recorder status:', error);
    }
  }, [state.session?.id, state.connectionStatus, reconnectWebSocket]);
  
  // Periodic health check for long recording sessions
  useEffect(() => {
    if (state.status !== RecordingStatus.RECORDING || !state.session?.id) {
      return;
    }
    
    const healthCheckInterval = setInterval(() => {
      checkRecorderStatus().catch(err => 
        console.error('Health check failed:', err)
      );
    }, 30000); // Check every 30 seconds
    
    return () => clearInterval(healthCheckInterval);
  }, [state.status, state.session?.id, checkRecorderStatus]);
  
  const contextValue: RecorderContextValue = {
    state,
    startRecording,
    stopRecording,
    pauseRecording,
    resumeRecording,
    addEvent,
    updateEvent,
    deleteEvent,
    reorderEvents,
    selectEvent,
    setInspectedElement,
    generateCode,
    resetState,
    reconnectWebSocket,
    checkRecorderStatus,
    // Add assertion methods
    addAssertion: async (eventId: string, assertion: AssertionConfig): Promise<void> => {
      try {
        await recorderService.addAssertion(eventId, assertion);
        // Reload events after adding assertion
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to add assertion' } });
        throw error;
      }
    },
    updateAssertion: async (eventId: string, assertionId: string, updates: Partial<AssertionConfig>): Promise<void> => {
      try {
        await recorderService.updateAssertion(eventId, assertionId, updates);
        // Reload events after updating assertion
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update assertion' } });
        throw error;
      }
    },
    deleteAssertion: async (eventId: string, assertionId: string): Promise<void> => {
      try {
        await recorderService.deleteAssertion(eventId, assertionId);
        // Reload events after deleting assertion
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to delete assertion' } });
        throw error;
      }
    },
    // Add condition methods
    addCondition: async (parentEventId: string, condition: Condition): Promise<void> => {
      try {
        await recorderService.addCondition(parentEventId, condition);
        // Reload events after adding condition
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to add condition' } });
        throw error;
      }
    },
    updateCondition: async (eventId: string, condition: Condition): Promise<void> => {
      try {
        await recorderService.updateCondition(eventId, condition);
        // Reload events after updating condition
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update condition' } });
        throw error;
      }
    },
    // Add data source methods
    addDataSource: async (dataSource: DataSource): Promise<void> => {
      try {
        await recorderService.addDataSource(dataSource);
        // Reload events after adding data source
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to add data source' } });
        throw error;
      }
    },
    updateDataSource: async (dataSourceId: string, updates: Partial<DataSource>): Promise<void> => {
      try {
        await recorderService.updateDataSource(dataSourceId, updates);
        // Reload events after updating data source
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update data source' } });
        throw error;
      }
    },
    deleteDataSource: async (dataSourceId: string): Promise<void> => {
      try {
        await recorderService.deleteDataSource(dataSourceId);
        // Reload events after deleting data source
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to delete data source' } });
        throw error;
      }
    },
    // Add loop methods
    addLoop: async (parentEventId: string, loop: Loop): Promise<void> => {
      try {
        await recorderService.addLoop(parentEventId, loop);
        // Reload events after adding loop
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to add loop' } });
        throw error;
      }
    },
    updateLoop: async (eventId: string, loop: Loop): Promise<void> => {
      try {
        await recorderService.updateLoop(eventId, loop);
        // Reload events after updating loop
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update loop' } });
        throw error;
      }
    },
    // Add step group methods
    createStepGroup: async (name: string, eventIds: string[]): Promise<void> => {
      try {
        await recorderService.createStepGroup(name, eventIds);
        // Reload events after creating step group
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to create step group' } });
        throw error;
      }
    },
    updateStepGroup: async (groupId: string, updates: Partial<StepGroup>): Promise<void> => {
      try {
        await recorderService.updateStepGroup(groupId, updates);
        // Reload events after updating step group
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update step group' } });
        throw error;
      }
    },
    deleteStepGroup: async (groupId: string): Promise<void> => {
      try {
        await recorderService.deleteStepGroup(groupId);
        // Reload events after deleting step group
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to delete step group' } });
        throw error;
      }
    },
    // Add variable binding methods
    addVariableBinding: async (parentEventId: string, binding: VariableBinding): Promise<void> => {
      try {
        await recorderService.addVariableBinding(parentEventId, binding);
        // Reload events after adding variable binding
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to add variable binding' } });
        throw error;
      }
    },
    updateVariableBinding: async (eventId: string, binding: VariableBinding): Promise<void> => {
      try {
        await recorderService.updateVariableBinding(eventId, binding);
        // Reload events after updating variable binding
        if (state.session?.id) {
          const session = await recorderService.getSession(state.session.id);
          dispatch({ type: 'SET_SESSION', payload: { session } });
        }
      } catch (error) {
        dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update variable binding' } });
        throw error;
      }
    }
  };
  
  return (
    <RecorderContext.Provider value={contextValue}>
      {children}
    </RecorderContext.Provider>
  );
};

// Custom hook for using the recorder context
export const useRecorder = (): RecorderContextValue => {
  const context = useContext(RecorderContext);
  
  if (!context) {
    throw new Error('useRecorder must be used within a RecorderProvider');
  }
  
  return context;
};

export default RecorderContext; 