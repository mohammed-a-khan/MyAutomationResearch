import React, { useState, useEffect } from 'react';
import { StepGroup, RecordedEvent, RecordedEventType } from '../../types/recorder';
import { useRecorder } from '../../context/RecorderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import './StepGroupManager.css';

interface StepGroupManagerProps {
  event?: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for managing step groups and error handling
 */
const StepGroupManager: React.FC<StepGroupManagerProps> = ({ event, onClose }) => {
  const { createStepGroup, updateStepGroup, state } = useRecorder();
  const { events } = state;
  const isEditing = !!event?.stepGroup;
  
  // Initialize step group from existing or create new
  const [stepGroup, setStepGroup] = useState<StepGroup>(
    event?.stepGroup || {
      id: `group-${Date.now()}`,
      name: '',
      type: 'group',
      eventIds: [],
      collapsed: false
    }
  );

  // Available events for selection (exclude current group if editing)
  const [availableEvents, setAvailableEvents] = useState<RecordedEvent[]>([]);
  
  // Selected events for the group
  const [selectedEventIds, setSelectedEventIds] = useState<string[]>(
    event?.stepGroup?.eventIds || []
  );
  
  // Selected events for catch block (for try-catch groups)
  const [selectedCatchEventIds, setSelectedCatchEventIds] = useState<string[]>(
    event?.stepGroup?.catchEventIds || []
  );
  
  // Selected events for finally block (for try-catch groups)
  const [selectedFinallyEventIds, setSelectedFinallyEventIds] = useState<string[]>(
    event?.stepGroup?.finallyEventIds || []
  );

  // Filter available events on component mount and when events change
  useEffect(() => {
    // Filter out events that are already in groups or are groups themselves
    const filterEvents = events.filter(e => 
      // Don't include the current group if editing
      (isEditing ? e.id !== event.id : true) &&
      // Don't include events that are already in groups
      !e.parentId &&
      // Don't include group events
      e.type !== RecordedEventType.GROUP &&
      e.type !== RecordedEventType.TRY_CATCH
    );
    
    setAvailableEvents(filterEvents);
  }, [events, isEditing, event?.id]);

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Update step group with selected events
    const updatedStepGroup: StepGroup = {
      ...stepGroup,
      eventIds: selectedEventIds,
      catchEventIds: stepGroup.type === 'try_catch' ? selectedCatchEventIds : undefined,
      finallyEventIds: stepGroup.type === 'try_catch' ? selectedFinallyEventIds : undefined
    };
    
    if (isEditing) {
      await updateStepGroup(stepGroup.id, updatedStepGroup);
    } else {
      await createStepGroup(updatedStepGroup.name, selectedEventIds);
    }
    
    onClose();
  };

  // Update step group property
  const handleChange = (field: keyof StepGroup, value: any) => {
    setStepGroup(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Toggle event selection for main group
  const toggleEventSelection = (eventId: string) => {
    if (selectedEventIds.includes(eventId)) {
      setSelectedEventIds(selectedEventIds.filter(id => id !== eventId));
    } else {
      setSelectedEventIds([...selectedEventIds, eventId]);
    }
  };

  // Toggle event selection for catch block
  const toggleCatchEventSelection = (eventId: string) => {
    if (selectedCatchEventIds.includes(eventId)) {
      setSelectedCatchEventIds(selectedCatchEventIds.filter(id => id !== eventId));
    } else {
      setSelectedCatchEventIds([...selectedCatchEventIds, eventId]);
    }
  };

  // Toggle event selection for finally block
  const toggleFinallyEventSelection = (eventId: string) => {
    if (selectedFinallyEventIds.includes(eventId)) {
      setSelectedFinallyEventIds(selectedFinallyEventIds.filter(id => id !== eventId));
    } else {
      setSelectedFinallyEventIds([...selectedFinallyEventIds, eventId]);
    }
  };

  // Options for group type dropdown
  const groupTypeOptions = [
    { value: 'group', label: 'Regular Group' },
    { value: 'try_catch', label: 'Try-Catch Block' }
  ];

  // Get event display name
  const getEventDisplayName = (event: RecordedEvent): string => {
    switch (event.type) {
      case RecordedEventType.CLICK:
        return `Click: ${event.element?.tagName || 'element'}`;
      case RecordedEventType.INPUT:
        return `Input: "${event.value?.substring(0, 15)}${event.value && event.value.length > 15 ? '...' : ''}"`;
      case RecordedEventType.NAVIGATION:
        return `Navigate: ${new URL(event.url).pathname}`;
      case RecordedEventType.ASSERTION:
        return `Assert: ${event.data?.property || 'condition'}`;
      case RecordedEventType.CONDITIONAL:
        return `If: ${event.condition?.leftOperand || 'condition'}`;
      case RecordedEventType.LOOP:
        return `Loop: ${event.loop?.type || 'iteration'}`;
      case RecordedEventType.CAPTURE:
        return `Capture: ${event.variableBinding?.name || 'variable'}`;
      default:
        return `${event.type}: ${event.notes || event.id}`;
    }
  };

  return (
    <form onSubmit={handleSubmit} className="step-group-manager">
      <div className="form-group mb-3">
        <label htmlFor="group-name" className="form-label">Group Name</label>
        <Input
          id="group-name"
          type="text"
          value={stepGroup.name}
          onChange={(e) => handleChange('name', e.target.value)}
          placeholder="Enter a descriptive name for this group"
          required
        />
      </div>

      <div className="form-group mb-3">
        <label htmlFor="group-type" className="form-label">Group Type</label>
        <Select
          id="group-type"
          value={stepGroup.type}
          onChange={(e) => handleChange('type', e.target.value)}
          options={groupTypeOptions}
        />
        <small className="form-text text-muted">
          {stepGroup.type === 'try_catch' 
            ? 'Try-Catch blocks handle errors that occur during test execution' 
            : 'Regular groups organize related test steps together'}
        </small>
      </div>

      {/* Main group steps selection */}
      <div className="step-selection mb-4">
        <h6>
          {stepGroup.type === 'try_catch' ? 'Try Block Steps' : 'Group Steps'}
        </h6>
        <p className="text-muted mb-2">
          Select the steps to include in {stepGroup.type === 'try_catch' ? 'the try block' : 'this group'}
        </p>
        
        {availableEvents.length > 0 ? (
          <div className="step-selection-list">
            {availableEvents.map(event => (
              <div 
                key={event.id} 
                className={`step-selection-item ${selectedEventIds.includes(event.id) ? 'selected' : ''}`}
                onClick={() => toggleEventSelection(event.id)}
              >
                <div className="step-selection-checkbox">
                  <input 
                    type="checkbox" 
                    checked={selectedEventIds.includes(event.id)}
                    onChange={() => {}} // Handled by the div click
                    onClick={(e) => e.stopPropagation()}
                  />
                </div>
                <div className="step-selection-details">
                  <div className="step-selection-type">{event.type}</div>
                  <div className="step-selection-description">{getEventDisplayName(event)}</div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="alert alert-warning">
            No available steps to select. Create some steps first.
          </div>
        )}
      </div>

      {/* Catch block steps selection (for try-catch only) */}
      {stepGroup.type === 'try_catch' && (
        <div className="step-selection mb-4">
          <h6>Catch Block Steps</h6>
          <p className="text-muted mb-2">
            Select steps to execute when an error occurs in the try block
          </p>
          
          {availableEvents.length > 0 ? (
            <div className="step-selection-list">
              {availableEvents.map(event => (
                <div 
                  key={event.id} 
                  className={`step-selection-item ${selectedCatchEventIds.includes(event.id) ? 'selected' : ''}`}
                  onClick={() => toggleCatchEventSelection(event.id)}
                >
                  <div className="step-selection-checkbox">
                    <input 
                      type="checkbox" 
                      checked={selectedCatchEventIds.includes(event.id)}
                      onChange={() => {}} // Handled by the div click
                      onClick={(e) => e.stopPropagation()}
                    />
                  </div>
                  <div className="step-selection-details">
                    <div className="step-selection-type">{event.type}</div>
                    <div className="step-selection-description">{getEventDisplayName(event)}</div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="alert alert-warning">
              No available steps to select. Create some steps first.
            </div>
          )}
        </div>
      )}

      {/* Finally block steps selection (for try-catch only) */}
      {stepGroup.type === 'try_catch' && (
        <div className="step-selection mb-4">
          <h6>Finally Block Steps</h6>
          <p className="text-muted mb-2">
            Select steps to always execute after the try-catch blocks
          </p>
          
          {availableEvents.length > 0 ? (
            <div className="step-selection-list">
              {availableEvents.map(event => (
                <div 
                  key={event.id} 
                  className={`step-selection-item ${selectedFinallyEventIds.includes(event.id) ? 'selected' : ''}`}
                  onClick={() => toggleFinallyEventSelection(event.id)}
                >
                  <div className="step-selection-checkbox">
                    <input 
                      type="checkbox" 
                      checked={selectedFinallyEventIds.includes(event.id)}
                      onChange={() => {}} // Handled by the div click
                      onClick={(e) => e.stopPropagation()}
                    />
                  </div>
                  <div className="step-selection-details">
                    <div className="step-selection-type">{event.type}</div>
                    <div className="step-selection-description">{getEventDisplayName(event)}</div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="alert alert-warning">
              No available steps to select. Create some steps first.
            </div>
          )}
        </div>
      )}

      <div className="d-flex justify-content-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button 
          type="submit" 
          variant="primary"
          disabled={selectedEventIds.length === 0}
        >
          {isEditing ? 'Update Group' : 'Create Group'}
        </Button>
      </div>
    </form>
  );
};

export default StepGroupManager; 