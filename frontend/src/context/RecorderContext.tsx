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
  // New actions for advanced features
  | { type: 'ADD_CONDITION'; payload: { parentEventId: string; condition: Condition } }
  | { type: 'UPDATE_CONDITION'; payload: { eventId: string; condition: Condition } }
  | { type: 'ADD_LOOP'; payload: { parentEventId: string; loop: Loop } }
  | { type: 'UPDATE_LOOP'; payload: { eventId: string; loop: Loop } }
  | { type: 'ADD_DATA_SOURCE'; payload: { dataSource: DataSource } }
  | { type: 'UPDATE_DATA_SOURCE'; payload: { dataSourceId: string; updates: Partial<DataSource> } }
  | { type: 'DELETE_DATA_SOURCE'; payload: { dataSourceId: string } }
  | { type: 'ADD_VARIABLE_BINDING'; payload: { parentEventId: string; variableBinding: VariableBinding } }
  | { type: 'UPDATE_VARIABLE_BINDING'; payload: { eventId: string; variableBinding: VariableBinding } }
  | { type: 'ADD_ASSERTION'; payload: { parentEventId: string; assertion: AssertionConfig } }
  | { type: 'UPDATE_ASSERTION'; payload: { eventId: string; assertionId: string; updates: Partial<AssertionConfig> } }
  | { type: 'DELETE_ASSERTION'; payload: { eventId: string; assertionId: string } }
  | { type: 'CREATE_STEP_GROUP'; payload: { name: string; eventIds: string[] } }
  | { type: 'UPDATE_STEP_GROUP'; payload: { groupId: string; updates: Partial<StepGroup> } }
  | { type: 'DELETE_STEP_GROUP'; payload: { groupId: string } };

// Define initial state
const initialState: RecorderContextState = {
  session: null,
  status: RecordingStatus.IDLE,
  events: [],
  selectedEvent: null,
  inspectedElement: null,
  generatedCode: null,
  error: null,
  isLoading: false
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
        events: action.payload.session.events,
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
        ),
        selectedEvent: state.selectedEvent?.id === action.payload.eventId
          ? { ...state.selectedEvent, ...action.payload.updates }
          : state.selectedEvent
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
        ).filter(Boolean)
      };
    
    case 'SET_EVENTS':
      return {
        ...state,
        events: action.payload.events,
        isLoading: false
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
    
    case 'RESET_STATE':
      return initialState;
    
    case 'ADD_CONDITION': {
      const { parentEventId, condition } = action.payload;
      const newEvent: RecordedEvent = {
        id: `condition-${Date.now()}`,
        type: RecordedEventType.CONDITIONAL,
        timestamp: Date.now(),
        url: state.session?.currentUrl || '',
        condition,
        order: state.events.length + 1,
        parentId: parentEventId
      };
      
      return {
        ...state,
        events: [...state.events, newEvent]
      };
    }
    
    case 'UPDATE_CONDITION': {
      const { eventId, condition } = action.payload;
      return {
        ...state,
        events: state.events.map(event => 
          event.id === eventId 
            ? { ...event, condition } 
            : event
        )
      };
    }
    
    case 'ADD_LOOP': {
      const { parentEventId, loop } = action.payload;
      const newEvent: RecordedEvent = {
        id: `loop-${Date.now()}`,
        type: RecordedEventType.LOOP,
        timestamp: Date.now(),
        url: state.session?.currentUrl || '',
        loop,
        order: state.events.length + 1,
        parentId: parentEventId
      };
      
      return {
        ...state,
        events: [...state.events, newEvent]
      };
    }
    
    case 'UPDATE_LOOP': {
      const { eventId, loop } = action.payload;
      return {
        ...state,
        events: state.events.map(event => 
          event.id === eventId 
            ? { ...event, loop } 
            : event
        )
      };
    }
    
    case 'ADD_DATA_SOURCE': {
      const { dataSource } = action.payload;
      const newEvent: RecordedEvent = {
        id: `data-source-${Date.now()}`,
        type: RecordedEventType.DATA_SOURCE,
        timestamp: Date.now(),
        url: state.session?.currentUrl || '',
        dataSource,
        order: state.events.length + 1
      };
      
      return {
        ...state,
        events: [...state.events, newEvent]
      };
    }
    
    case 'UPDATE_DATA_SOURCE': {
      const { dataSourceId, updates } = action.payload;
      return {
        ...state,
        events: state.events.map(event => 
          event.id === dataSourceId && event.dataSource
            ? { 
                ...event, 
                dataSource: { ...event.dataSource, ...updates } 
              } 
            : event
        )
      };
    }
    
    case 'DELETE_DATA_SOURCE': {
      const { dataSourceId } = action.payload;
      return {
        ...state,
        events: state.events.filter(event => event.id !== dataSourceId)
      };
    }
    
    case 'ADD_VARIABLE_BINDING': {
      const { parentEventId, variableBinding } = action.payload;
      const newEvent: RecordedEvent = {
        id: `capture-${Date.now()}`,
        type: RecordedEventType.CAPTURE,
        timestamp: Date.now(),
        url: state.session?.currentUrl || '',
        variableBinding,
        order: state.events.length + 1,
        parentId: parentEventId
      };
      
      return {
        ...state,
        events: [...state.events, newEvent]
      };
    }
    
    case 'UPDATE_VARIABLE_BINDING': {
      const { eventId, variableBinding } = action.payload;
      return {
        ...state,
        events: state.events.map(event => 
          event.id === eventId 
            ? { ...event, variableBinding } 
            : event
        )
      };
    }
    
    case 'ADD_ASSERTION': {
      const { parentEventId, assertion } = action.payload;
      // Find the parent event
      const parentEvent = state.events.find(e => e.id === parentEventId);
      
      if (!parentEvent) {
        return state;
      }
      
      // Add the assertion to the parent event
      const updatedEvent = {
        ...parentEvent,
        assertions: [
          ...(parentEvent.assertions || []),
          assertion
        ]
      };
      
      return {
        ...state,
        events: state.events.map(event => 
          event.id === parentEventId 
            ? updatedEvent
            : event
        ),
        selectedEvent: state.selectedEvent?.id === parentEventId
          ? updatedEvent
          : state.selectedEvent
      };
    }
    
    case 'UPDATE_ASSERTION': {
      const { eventId, assertionId, updates } = action.payload;
      // Find the event containing the assertion
      const event = state.events.find(e => e.id === eventId);
      
      if (!event || !event.assertions) {
        return state;
      }
      
      // Update the specific assertion
      const updatedAssertions = event.assertions.map(assertion => 
        assertion.id === assertionId 
          ? { ...assertion, ...updates }
          : assertion
      );
      
      // Update the event with the new assertions
      const updatedEvent = {
        ...event,
        assertions: updatedAssertions
      };
      
      return {
        ...state,
        events: state.events.map(e => 
          e.id === eventId 
            ? updatedEvent
            : e
        ),
        selectedEvent: state.selectedEvent?.id === eventId
          ? updatedEvent
          : state.selectedEvent
      };
    }
    
    case 'DELETE_ASSERTION': {
      const { eventId, assertionId } = action.payload;
      // Find the event containing the assertion
      const event = state.events.find(e => e.id === eventId);
      
      if (!event || !event.assertions) {
        return state;
      }
      
      // Filter out the specific assertion
      const updatedAssertions = event.assertions.filter(assertion => 
        assertion.id !== assertionId
      );
      
      // Update the event with the filtered assertions
      const updatedEvent = {
        ...event,
        assertions: updatedAssertions
      };
      
      return {
        ...state,
        events: state.events.map(e => 
          e.id === eventId 
            ? updatedEvent
            : e
        ),
        selectedEvent: state.selectedEvent?.id === eventId
          ? updatedEvent
          : state.selectedEvent
      };
    }
    
    case 'CREATE_STEP_GROUP': {
      const { name, eventIds } = action.payload;
      const groupId = `group-${Date.now()}`;
      
      const newGroup: StepGroup = {
        id: groupId,
        name,
        eventIds,
        type: 'group',
        collapsed: false
      };
      
      const newEvent: RecordedEvent = {
        id: groupId,
        type: RecordedEventType.GROUP,
        timestamp: Date.now(),
        url: state.session?.currentUrl || '',
        stepGroup: newGroup,
        order: state.events.length + 1
      };
      
      // Update child events to reference their parent
      const updatedEvents = state.events.map(event => 
        eventIds.includes(event.id)
          ? { ...event, parentId: groupId }
          : event
      );
      
      return {
        ...state,
        events: [...updatedEvents, newEvent]
      };
    }
    
    case 'UPDATE_STEP_GROUP': {
      const { groupId, updates } = action.payload;
      return {
        ...state,
        events: state.events.map(event => 
          event.id === groupId && event.stepGroup
            ? { 
                ...event, 
                stepGroup: { ...event.stepGroup, ...updates } 
              } 
            : event
        )
      };
    }
    
    case 'DELETE_STEP_GROUP': {
      const { groupId } = action.payload;
      
      // Find the group event
      const groupEvent = state.events.find(e => e.id === groupId);
      
      if (!groupEvent || !groupEvent.stepGroup) {
        return state;
      }
      
      // Remove parent references from child events
      const updatedEvents = state.events.map(event => 
        event.parentId === groupId
          ? { ...event, parentId: undefined }
          : event
      );
      
      // Remove the group event itself
      return {
        ...state,
        events: updatedEvents.filter(event => event.id !== groupId)
      };
    }
    
    default:
      return state;
  }
};

// Context value interface
interface RecorderContextValue {
  state: RecorderContextState;
  startRecording: (options: RecordingOptions) => Promise<void>;
  stopRecording: () => Promise<void>;
  pauseRecording: () => Promise<void>;
  resumeRecording: () => Promise<void>;
  addEvent: (event: Partial<RecordedEvent>) => Promise<void>;
  updateEvent: (eventId: string, updates: Partial<RecordedEvent>) => Promise<void>;
  deleteEvent: (eventId: string) => Promise<void>;
  reorderEvents: (eventIds: string[]) => Promise<void>;
  selectEvent: (event: RecordedEvent | null) => void;
  setInspectedElement: (element: ElementInfo | null) => void;
  generateCode: (options: CodeGenerationOptions) => Promise<void>;
  getEvents: () => Promise<void>;
  resetState: () => void;
  // New methods for advanced features
  addCondition: (parentEventId: string, condition: Condition) => Promise<void>;
  updateCondition: (eventId: string, condition: Condition) => Promise<void>;
  addLoop: (parentEventId: string, loop: Loop) => Promise<void>;
  updateLoop: (eventId: string, loop: Loop) => Promise<void>;
  addDataSource: (dataSource: DataSource) => Promise<void>;
  updateDataSource: (dataSourceId: string, updates: Partial<DataSource>) => Promise<void>;
  deleteDataSource: (dataSourceId: string) => Promise<void>;
  addVariableBinding: (parentEventId: string, variableBinding: VariableBinding) => Promise<void>;
  updateVariableBinding: (eventId: string, variableBinding: VariableBinding) => Promise<void>;
  addAssertion: (parentEventId: string, assertion: AssertionConfig) => Promise<void>;
  updateAssertion: (eventId: string, assertionId: string, updates: Partial<AssertionConfig>) => Promise<void>;
  deleteAssertion: (eventId: string, assertionId: string) => Promise<void>;
  createStepGroup: (name: string, eventIds: string[]) => Promise<void>;
  updateStepGroup: (groupId: string, updates: Partial<StepGroup>) => Promise<void>;
  deleteStepGroup: (groupId: string) => Promise<void>;
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
  const [websocketStatus, setWebsocketStatus] = React.useState<ConnectionStatus>(
    wsService.getStatus()
  );

  // Reset error after 5 seconds
  useEffect(() => {
    if (state.error) {
      const timer = setTimeout(() => {
        dispatch({ type: 'SET_ERROR', payload: { error: null } });
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [state.error]);

  // WebSocket subscription for real-time events
  useEffect(() => {
    const statusUnsubscribe = wsService.subscribeToStatus(setWebsocketStatus);
    
    const eventUnsubscribe = wsService.subscribe<RecordedEvent>(
      WsMessageType.EVENT_RECORDED, 
      (payload) => {
        dispatch({ type: 'ADD_EVENT', payload: { event: payload } });
      }
    );
    
    const sessionStatusUnsubscribe = wsService.subscribe<RecordingSession>(
      WsMessageType.SESSION_STATUS, 
      (payload) => {
        dispatch({ type: 'SET_SESSION', payload: { session: payload } });
      }
    );
    
    const elementHighlightUnsubscribe = wsService.subscribe<ElementInfo>(
      WsMessageType.ELEMENT_HIGHLIGHTED, 
      (payload) => {
        dispatch({ type: 'SET_INSPECTED_ELEMENT', payload: { element: payload } });
      }
    );
    
    const errorUnsubscribe = wsService.subscribe<string>(
      WsMessageType.ERROR, 
      (payload) => {
        dispatch({ type: 'SET_ERROR', payload: { error: payload } });
      }
    );
    
    return () => {
      statusUnsubscribe();
      eventUnsubscribe();
      sessionStatusUnsubscribe();
      elementHighlightUnsubscribe();
      errorUnsubscribe();
    };
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
        }
      }
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to start recording session' } });
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
    }
  }, [state.session?.id]);

  // Add manual event
  const addEvent = useCallback(async (event: Partial<RecordedEvent>): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      const newEvent = await recorderService.addEvent(event);
      dispatch({ type: 'ADD_EVENT', payload: { event: newEvent } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to add event' } });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    }
  }, [state.session?.id]);

  // Update event
  const updateEvent = useCallback(async (eventId: string, updates: Partial<RecordedEvent>): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      const updatedEvent = await recorderService.updateEvent(eventId, updates);
      
      dispatch({ 
        type: 'UPDATE_EVENT', 
        payload: { eventId, updates: updatedEvent }
      });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to update event' } });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    }
  }, [state.session?.id]);

  // Delete event
  const deleteEvent = useCallback(async (eventId: string): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      await recorderService.deleteEvent(eventId);
      dispatch({ type: 'DELETE_EVENT', payload: { eventId } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to delete event' } });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    }
  }, [state.session?.id]);

  // Reorder events
  const reorderEvents = useCallback(async (eventIds: string[]): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      await recorderService.reorderEvents(state.session.id, eventIds);
      dispatch({ type: 'REORDER_EVENTS', payload: { eventIds } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to reorder events' } });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    }
  }, [state.session?.id]);

  // Select event
  const selectEvent = useCallback((event: RecordedEvent | null): void => {
    dispatch({ type: 'SELECT_EVENT', payload: { event } });
  }, []);

  // Set inspected element
  const setInspectedElement = useCallback((element: ElementInfo | null): void => {
    dispatch({ type: 'SET_INSPECTED_ELEMENT', payload: { element } });
  }, []);

  // Generate code
  const generateCode = useCallback(async (options: CodeGenerationOptions): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No recording session data available for code generation' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      const generatedCode = await recorderService.generateCode(state.session.id, options);
      dispatch({ 
        type: 'SET_GENERATED_CODE', 
        payload: { code: generatedCode.testCode } 
      });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to generate code' } });
    } finally {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    }
  }, [state.session?.id]);

  // Fetch events for the current session
  const getEvents = useCallback(async (): Promise<void> => {
    if (!state.session?.id) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'No active recording session' } });
      return;
    }
    
    dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
    
    try {
      const events = await recorderService.getEvents(state.session.id);
      dispatch({ type: 'SET_EVENTS', payload: { events } });
    } catch (error) {
      dispatch({ type: 'SET_ERROR', payload: { error: 'Failed to fetch events' } });
    }
  }, [state.session?.id]);

  // Reset state to initial values
  const resetState = useCallback((): void => {
    dispatch({ type: 'RESET_STATE' });
  }, []);

  // New methods for advanced features
  const addCondition = useCallback(async (parentEventId: string, condition: Condition) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Create condition in backend
      const response = await recorderService.addCondition(parentEventId, condition);
      
      dispatch({ 
        type: 'ADD_CONDITION', 
        payload: { parentEventId, condition: response || condition } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to add condition') } 
      });
    }
  }, []);
  
  const updateCondition = useCallback(async (eventId: string, condition: Condition) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Update condition in backend
      const response = await recorderService.updateCondition(eventId, condition);
      
      dispatch({ 
        type: 'UPDATE_CONDITION', 
        payload: { eventId, condition: response || condition } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to update condition') } 
      });
    }
  }, []);
  
  const addLoop = useCallback(async (parentEventId: string, loop: Loop) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Create loop in backend
      const response = await recorderService.addLoop(parentEventId, loop);
      
      dispatch({ 
        type: 'ADD_LOOP', 
        payload: { parentEventId, loop: response || loop } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to add loop') } 
      });
    }
  }, []);
  
  const updateLoop = useCallback(async (eventId: string, loop: Loop) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Update loop in backend
      const response = await recorderService.updateLoop(eventId, loop);
      
      dispatch({ 
        type: 'UPDATE_LOOP', 
        payload: { eventId, loop: response || loop } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to update loop') } 
      });
    }
  }, []);
  
  const addDataSource = useCallback(async (dataSource: DataSource) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Create data source in backend
      const response = await recorderService.addDataSource(dataSource);
      
      dispatch({ 
        type: 'ADD_DATA_SOURCE', 
        payload: { dataSource: response || dataSource } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to add data source') } 
      });
    }
  }, []);
  
  const updateDataSource = useCallback(async (dataSourceId: string, updates: Partial<DataSource>) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Update data source in backend
      const response = await recorderService.updateDataSource(dataSourceId, updates);
      
      dispatch({ 
        type: 'UPDATE_DATA_SOURCE', 
        payload: { dataSourceId, updates: response || updates } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to update data source') } 
      });
    }
  }, []);
  
  const deleteDataSource = useCallback(async (dataSourceId: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Delete data source in backend
      await recorderService.deleteDataSource(dataSourceId);
      
      dispatch({ type: 'DELETE_DATA_SOURCE', payload: { dataSourceId } });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to delete data source') } 
      });
    }
  }, []);
  
  const addVariableBinding = useCallback(async (parentEventId: string, variableBinding: VariableBinding) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Create variable binding in backend
      const response = await recorderService.addVariableBinding(parentEventId, variableBinding);
      
      dispatch({ 
        type: 'ADD_VARIABLE_BINDING', 
        payload: { parentEventId, variableBinding: response || variableBinding } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to add variable binding') } 
      });
    }
  }, []);
  
  const updateVariableBinding = useCallback(async (eventId: string, variableBinding: VariableBinding) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Update variable binding in backend
      const response = await recorderService.updateVariableBinding(eventId, variableBinding);
      
      dispatch({ 
        type: 'UPDATE_VARIABLE_BINDING', 
        payload: { eventId, variableBinding: response || variableBinding } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to update variable binding') } 
      });
    }
  }, []);
  
  const addAssertion = useCallback(async (parentEventId: string, assertion: AssertionConfig) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Create assertion in backend
      const response = await recorderService.addAssertion(parentEventId, assertion);
      
      dispatch({ 
        type: 'ADD_ASSERTION', 
        payload: { parentEventId, assertion: response || assertion } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to add assertion') } 
      });
    }
  }, []);
  
  const updateAssertion = useCallback(async (
    eventId: string, 
    assertionId: string, 
    updates: Partial<AssertionConfig>
  ) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Update assertion in backend
      const response = await recorderService.updateAssertion(eventId, assertionId, updates);
      
      dispatch({ 
        type: 'UPDATE_ASSERTION', 
        payload: { 
          eventId, 
          assertionId, 
          updates: response || updates 
        } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to update assertion') } 
      });
    }
  }, []);
  
  const deleteAssertion = useCallback(async (eventId: string, assertionId: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Delete assertion in backend
      await recorderService.deleteAssertion(eventId, assertionId);
      
      dispatch({ 
        type: 'DELETE_ASSERTION', 
        payload: { eventId, assertionId } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to delete assertion') } 
      });
    }
  }, []);
  
  const createStepGroup = useCallback(async (name: string, eventIds: string[]) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Create step group in backend
      const response = await recorderService.createStepGroup(name, eventIds);
      
      dispatch({ 
        type: 'CREATE_STEP_GROUP', 
        payload: { 
          name, 
          eventIds,
          ...(response ? { response } : {})
        } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to create step group') } 
      });
    }
  }, []);
  
  const updateStepGroup = useCallback(async (groupId: string, updates: Partial<StepGroup>) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Update step group in backend
      const response = await recorderService.updateStepGroup(groupId, updates);
      
      dispatch({ 
        type: 'UPDATE_STEP_GROUP', 
        payload: { groupId, updates: response || updates } 
      });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to update step group') } 
      });
    }
  }, []);
  
  const deleteStepGroup = useCallback(async (groupId: string) => {
    try {
      dispatch({ type: 'SET_LOADING', payload: { isLoading: true } });
      
      // Delete step group in backend
      await recorderService.deleteStepGroup(groupId);
      
      dispatch({ type: 'DELETE_STEP_GROUP', payload: { groupId } });
      
      dispatch({ type: 'SET_LOADING', payload: { isLoading: false } });
    } catch (error) {
      dispatch({ 
        type: 'SET_ERROR', 
        payload: { error: formatError(error, 'Failed to delete step group') } 
      });
    }
  }, []);
  
  // Value object to be provided by context
  const value: RecorderContextValue = {
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
    getEvents,
    resetState,
    // New methods
    addCondition,
    updateCondition,
    addLoop,
    updateLoop,
    addDataSource,
    updateDataSource,
    deleteDataSource,
    addVariableBinding,
    updateVariableBinding,
    addAssertion,
    updateAssertion,
    deleteAssertion,
    createStepGroup,
    updateStepGroup,
    deleteStepGroup
  };

  return (
    <RecorderContext.Provider value={value}>
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