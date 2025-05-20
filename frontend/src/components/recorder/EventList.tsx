import React, { useCallback, useState, useMemo } from 'react';
import { RecordedEvent, RecordedEventType, StepGroup } from '../../types/recorder';
import Button from '../common/Button';
import Modal from '../common/Modal';
import './Recorder.css';

interface EventListProps {
  events: RecordedEvent[];
  selectedEvent: RecordedEvent | null;
  onSelectEvent: (event: RecordedEvent | null) => void;
  onAddEvent: (event: Partial<RecordedEvent>) => Promise<void>;
  isActive: boolean;
  disabled: boolean;
}

/**
 * Displays a list of recorded events with options to select, edit, and delete
 */
const EventList: React.FC<EventListProps> = ({
  events,
  selectedEvent,
  onSelectEvent,
  onAddEvent,
  isActive,
  disabled
}) => {
  const [eventToDelete, setEventToDelete] = useState<string | null>(null);
  const [showAddModal, setShowAddModal] = useState<boolean>(false);
  const [addEventType, setAddEventType] = useState<RecordedEventType>(RecordedEventType.CUSTOM);
  const [collapsedGroups, setCollapsedGroups] = useState<Record<string, boolean>>({});

  // Process events to create a hierarchical structure
  const processedEvents = useMemo(() => {
    // Create a map of parent IDs to child events
    const parentMap: Record<string, RecordedEvent[]> = {};
    
    // Filter out child events and group them by parent
    const rootEvents = (events || []).filter(event => {
      if (event.parentId) {
        if (!parentMap[event.parentId]) {
          parentMap[event.parentId] = [];
        }
        parentMap[event.parentId].push(event);
        return false;
      }
      return true;
    });

    // Sort each group of children
    Object.keys(parentMap).forEach(parentId => {
      parentMap[parentId].sort((a, b) => a.order - b.order);
    });
    
    return { rootEvents, parentMap };
  }, [events]);

  // Toggle collapsed state for a group
  const toggleGroupCollapse = (groupId: string) => {
    setCollapsedGroups(prev => ({
      ...prev,
      [groupId]: !prev[groupId]
    }));
  };

  // Handle adding a custom event
  const handleAddEvent = async () => {
    const timestamp = Date.now();
    
    const newEvent: Partial<RecordedEvent> = {
      type: addEventType,
      timestamp,
      url: window.location.href,
      order: (events || []).length + 1,
      custom: true
    };

    await onAddEvent(newEvent);
    setShowAddModal(false);
  };

  // Get CSS class for an event type
  const getEventTypeClass = (eventType: RecordedEventType) => {
    switch (eventType) {
      case RecordedEventType.CLICK:
      case RecordedEventType.DOUBLE_CLICK:
      case RecordedEventType.RIGHT_CLICK:
        return 'event-type-click';
      case RecordedEventType.INPUT:
      case RecordedEventType.TYPE:
      case RecordedEventType.KEYPRESS:
      case RecordedEventType.KEY_PRESS:
        return 'event-type-input';
      case RecordedEventType.NAVIGATION:
        return 'event-type-navigation';
      case RecordedEventType.ASSERTION:
        return 'event-type-assertion';
      case RecordedEventType.WAIT:
        return 'event-type-wait';
      case RecordedEventType.SCREENSHOT:
        return 'event-type-screenshot';
      case RecordedEventType.MOUSE_HOVER:
      case RecordedEventType.HOVER:
        return 'event-type-hover';
      case RecordedEventType.CONDITIONAL:
        return 'event-type-conditional';
      case RecordedEventType.LOOP:
        return 'event-type-loop';
      case RecordedEventType.DATA_SOURCE:
        return 'event-type-data-source';
      case RecordedEventType.CAPTURE:
        return 'event-type-capture';
      case RecordedEventType.GROUP:
      case RecordedEventType.TRY_CATCH:
        return 'event-type-group';
      default:
        return 'event-type-other';
    }
  };

  // Get icon for an event type
  const getEventTypeIcon = (eventType: RecordedEventType) => {
    switch (eventType) {
      case RecordedEventType.CLICK:
      case RecordedEventType.MOUSE_DOWN:
      case RecordedEventType.MOUSE_UP:
        return 'bi-mouse';
      case RecordedEventType.DOUBLE_CLICK:
        return 'bi-mouse2';
      case RecordedEventType.RIGHT_CLICK:
        return 'bi-mouse3';
      case RecordedEventType.INPUT:
      case RecordedEventType.TYPE:
        return 'bi-keyboard';
      case RecordedEventType.KEYPRESS:
      case RecordedEventType.KEY_PRESS:
        return 'bi-key';
      case RecordedEventType.NAVIGATION:
        return 'bi-signpost';
      case RecordedEventType.ASSERTION:
        return 'bi-check-circle';
      case RecordedEventType.WAIT:
        return 'bi-hourglass-split';
      case RecordedEventType.SCREENSHOT:
        return 'bi-camera';
      case RecordedEventType.MOUSE_HOVER:
      case RecordedEventType.HOVER:
        return 'bi-hand-index';
      case RecordedEventType.SELECT:
        return 'bi-list';
      case RecordedEventType.CUSTOM:
        return 'bi-code-slash';
      case RecordedEventType.CONDITIONAL:
        return 'bi-diagram-2';
      case RecordedEventType.LOOP:
        return 'bi-arrow-repeat';
      case RecordedEventType.DATA_SOURCE:
        return 'bi-database';
      case RecordedEventType.CAPTURE:
        return 'bi-clipboard-data';
      case RecordedEventType.GROUP:
        return 'bi-folder';
      case RecordedEventType.TRY_CATCH:
        return 'bi-shield-check';
      default:
        return 'bi-lightning';
    }
  };

  // Format event data for display
  const getEventDetails = (event: RecordedEvent) => {
    let details = '';

    switch (event.type) {
      case RecordedEventType.CLICK:
      case RecordedEventType.DOUBLE_CLICK:
      case RecordedEventType.RIGHT_CLICK:
        details = event.element?.tagName?.toLowerCase() || 'element';
        if (event.element?.id) {
          details += `#${event.element.id}`;
        } else if (event.element?.textContent) {
          const text = event.element.textContent.substring(0, 20);
          details += ` with text "${text}${text.length > 20 ? '...' : ''}"`;
        }
        break;
      case RecordedEventType.INPUT:
      case RecordedEventType.TYPE:
        details = `"${event.value?.substring(0, 25) || ''}"`;
        if (event.value && event.value.length > 25) {
          details += '...';
        }
        details += ` into ${event.element?.tagName?.toLowerCase() || 'input'}`;
        break;
      case RecordedEventType.NAVIGATION:
        details = new URL(event.url).pathname;
        break;
      case RecordedEventType.ASSERTION:
        details = `Assert ${event.data?.property || 'condition'} ${event.data?.value || ''}`;
        break;
      case RecordedEventType.WAIT:
        details = `Wait ${event.data?.timeout || 1000}ms`;
        break;
      case RecordedEventType.SELECT:
        details = `Selected "${event.value}" from ${event.element?.tagName.toLowerCase()}`;
        break;
      case RecordedEventType.CONDITIONAL:
        details = event.condition ? 
          `If ${event.condition.leftOperand} ${event.condition.type.toLowerCase()} ${event.condition.rightOperand || ''}` : 
          'Conditional';
        break;
      case RecordedEventType.LOOP:
        details = event.loop ? 
          `Loop ${event.loop.type.toLowerCase()}${event.loop.count ? ` ${event.loop.count} times` : ''}` : 
          'Loop';
        break;
      case RecordedEventType.DATA_SOURCE:
        details = event.dataSource ? 
          `Data source: ${event.dataSource.name} (${event.dataSource.type})` : 
          'Data source';
        break;
      case RecordedEventType.CAPTURE:
        details = event.variableBinding ? 
          `Capture ${event.variableBinding.name} from ${event.variableBinding.source}` : 
          'Capture variable';
        break;
      case RecordedEventType.GROUP:
        details = event.stepGroup ? 
          `Group: ${event.stepGroup.name} (${event.stepGroup.eventIds.length} steps)` : 
          'Group';
        break;
      case RecordedEventType.TRY_CATCH:
        details = event.stepGroup ? 
          `Try-Catch: ${event.stepGroup.name}` : 
          'Try-Catch block';
        break;
      default:
        details = event.notes || event.type;
    }

    return details;
  };

  // Format timestamp
  const formatTimestamp = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };

  // Recursive function to render events
  const renderEvents = (eventList: RecordedEvent[], depth: number = 0) => {
    return (eventList || []).map(event => {
      const hasChildren = processedEvents.parentMap && 
                         processedEvents.parentMap[event.id] && 
                         processedEvents.parentMap[event.id].length > 0;
      const isCollapsed = collapsedGroups[event.id] || false;
      
      return (
        <React.Fragment key={event.id}>
          <div
            className={`event-item ${
              selectedEvent?.id === event.id ? 'event-item-selected' : ''
            } ${event.disabled ? 'event-item-disabled' : ''}`}
            onClick={() => onSelectEvent(event)}
            style={{ paddingLeft: `${depth * 16 + 8}px` }}
          >
            <div className="event-item-connector">
              {depth > 0 && (
                <>
                  <div className="connector-vertical"></div>
                  <div className="connector-horizontal"></div>
                </>
              )}
            </div>
            
            {hasChildren && (
              <div 
                className="event-collapse-toggle" 
                onClick={(e) => {
                  e.stopPropagation();
                  toggleGroupCollapse(event.id);
                }}
              >
                <i className={`bi ${isCollapsed ? 'bi-chevron-right' : 'bi-chevron-down'}`}></i>
              </div>
            )}
            
            <div className={`event-item-icon ${getEventTypeClass(event.type)}`}>
              <i className={`bi ${getEventTypeIcon(event.type)}`}></i>
            </div>
            
            <div className="event-item-details">
              <div className="event-item-type">{event.type}</div>
              <div className="event-item-description">{getEventDetails(event)}</div>
            </div>
            
            <div className="event-item-meta">
              <div className="event-item-time">{formatTimestamp(event.timestamp)}</div>
              <div className="event-item-actions">
                {/* Event actions */}
              </div>
            </div>
          </div>
          
          {/* Render child events if not collapsed */}
          {hasChildren && !isCollapsed && renderEvents(processedEvents.parentMap?.[event.id] || [], depth + 1)}
        </React.Fragment>
      );
    });
  };

  if ((events || []).length === 0) {
    return (
      <div className="event-list">
        <div className="event-list-header">
          <h5 className="event-list-title">Recorded Events</h5>
          <div className="event-list-actions">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setShowAddModal(true)}
              disabled={disabled || !isActive}
            >
              <i className="bi bi-plus"></i> Add Event
            </Button>
          </div>
        </div>
        
        <div className="event-list-empty">
          <div className="event-list-empty-icon">
            <i className="bi bi-record-circle"></i>
          </div>
          <p>No events have been recorded yet.</p>
          {isActive ? (
            <p>Interact with the page to record events.</p>
          ) : (
            <p>Start recording to capture events.</p>
          )}
        </div>

        <Modal
          isOpen={showAddModal}
          title="Add Event"
          onClose={() => setShowAddModal(false)}
        >
          <div className="p-3">
            <div className="form-group mb-3">
              <label htmlFor="event-type" className="form-label">Event Type</label>
              <select
                id="event-type"
                className="form-select"
                value={addEventType}
                onChange={(e) => setAddEventType(e.target.value as RecordedEventType)}
              >
                <option value={RecordedEventType.CUSTOM}>Custom</option>
                <option value={RecordedEventType.CONDITIONAL}>Conditional</option>
                <option value={RecordedEventType.LOOP}>Loop</option>
                <option value={RecordedEventType.CAPTURE}>Variable Capture</option>
                <option value={RecordedEventType.ASSERTION}>Assertion</option>
                <option value={RecordedEventType.GROUP}>Group</option>
                <option value={RecordedEventType.WAIT}>Wait</option>
              </select>
            </div>
            <p>Add a new {addEventType.toLowerCase()} event to the recording.</p>
            <div className="d-flex justify-content-end">
              <Button
                variant="secondary"
                onClick={() => setShowAddModal(false)}
                className="me-2"
              >
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleAddEvent}
              >
                Add Event
              </Button>
            </div>
          </div>
        </Modal>
      </div>
    );
  }

  return (
    <div className="event-list">
      <div className="event-list-header">
        <h5 className="event-list-title">Recorded Events ({(events || []).length})</h5>
        <div className="event-list-actions">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setShowAddModal(true)}
            disabled={disabled || !isActive}
          >
            <i className="bi bi-plus"></i> Add Event
          </Button>
        </div>
      </div>

      <div className="event-list-container">
        {(processedEvents.rootEvents || []).length === 0 ? (
          <div className="no-events-message">
            {isActive ? (
              <p>Interact with the page to record events.</p>
            ) : (
              <p>Start recording to capture events.</p>
            )}
          </div>
        ) : (
          renderEvents(processedEvents.rootEvents || [])
        )}
      </div>

      <Modal
        isOpen={showAddModal}
        title="Add Event"
        onClose={() => setShowAddModal(false)}
      >
        <div className="p-3">
          <div className="form-group mb-3">
            <label htmlFor="event-type" className="form-label">Event Type</label>
            <select
              id="event-type"
              className="form-select"
              value={addEventType}
              onChange={(e) => setAddEventType(e.target.value as RecordedEventType)}
            >
              <option value={RecordedEventType.CUSTOM}>Custom</option>
              <option value={RecordedEventType.CONDITIONAL}>Conditional</option>
              <option value={RecordedEventType.LOOP}>Loop</option>
              <option value={RecordedEventType.CAPTURE}>Variable Capture</option>
              <option value={RecordedEventType.ASSERTION}>Assertion</option>
              <option value={RecordedEventType.GROUP}>Group</option>
              <option value={RecordedEventType.WAIT}>Wait</option>
            </select>
          </div>
          <p>Add a new {addEventType.toLowerCase()} event to the recording.</p>
          <div className="d-flex justify-content-end">
            <Button
              variant="secondary"
              onClick={() => setShowAddModal(false)}
              className="me-2"
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleAddEvent}
            >
              Add Event
            </Button>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={!!eventToDelete}
        title="Confirm Delete"
        onClose={() => setEventToDelete(null)}
      >
        <div className="p-3">
          <p>Are you sure you want to delete this event?</p>
          <div className="d-flex justify-content-end">
            <Button
              variant="secondary"
              onClick={() => setEventToDelete(null)}
              className="me-2"
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={() => {
                // Delete logic would be implemented here
                setEventToDelete(null);
              }}
            >
              Delete
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default EventList; 