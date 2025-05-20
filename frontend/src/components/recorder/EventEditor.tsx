import React, { useState, FormEvent } from 'react';
import { useRecorder } from '../../context/RecorderContext';
import { RecordedEvent, RecordedEventType } from '../../types/recorder';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';

interface EventEditorProps {
  event: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for editing event details
 */
const EventEditor: React.FC<EventEditorProps> = ({ event, onClose }) => {
  const { state, updateEvent } = useRecorder();
  const { isLoading } = state;
  
  const [formData, setFormData] = useState<Partial<RecordedEvent>>({
    type: event.type,
    value: event.value || '',
    metaData: event.metaData || {}
  });

  // Handle form submission
  const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    
    try {
      await updateEvent(event.id, formData);
      onClose();
    } catch (error) {
      // Error handling is done in the context
    }
  };

  // Handle input change
  const handleChange = (name: string, value: string | any) => {
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  // Handle metadata change
  const handleMetaDataChange = (key: string, value: string) => {
    setFormData(prev => ({
      ...prev,
      metaData: {
        ...(prev.metaData || {}),
        [key]: value
      }
    }));
  };

  // Event type options
  const eventTypeOptions = Object.values(RecordedEventType).map(type => ({
    value: type,
    label: type
  }));

  return (
    <form onSubmit={handleSubmit} className="event-edit-form">
      <div className="form-group">
        <label htmlFor="type" className="form-label">Event Type</label>
        <Select
          id="type"
          name="type"
          value={formData.type}
          onChange={(e) => handleChange('type', e.target.value)}
          options={eventTypeOptions}
        />
      </div>

      {/* Show different fields based on event type */}
      {(formData.type === RecordedEventType.INPUT ||
        formData.type === RecordedEventType.KEYPRESS ||
        formData.type === RecordedEventType.NAVIGATION ||
        formData.type === RecordedEventType.ASSERTION ||
        formData.type === RecordedEventType.WAIT ||
        formData.type === RecordedEventType.CUSTOM) && (
        <div className="form-group">
          <label htmlFor="value" className="form-label">Value</label>
          <Input
            id="value"
            name="value"
            value={formData.value || ''}
            onChange={(e) => handleChange('value', e.target.value)}
            placeholder={getValuePlaceholder(formData.type as RecordedEventType)}
          />
        </div>
      )}

      {/* Add additional metadata fields that are relevant to the event type */}
      {formData.type === RecordedEventType.ASSERTION && (
        <div className="form-group">
          <label htmlFor="assertion-type" className="form-label">Assertion Type</label>
          <Select
            id="assertion-type"
            name="assertion-type"
            value={(formData.metaData?.assertionType as string) || 'exists'}
            onChange={(e) => handleMetaDataChange('assertionType', e.target.value)}
            options={[
              { value: 'exists', label: 'Element exists' },
              { value: 'visible', label: 'Element is visible' },
              { value: 'contains', label: 'Element contains text' },
              { value: 'equals', label: 'Element equals text' },
              { value: 'attribute', label: 'Element attribute check' }
            ]}
          />
        </div>
      )}

      {formData.type === RecordedEventType.WAIT && (
        <div className="form-group">
          <label htmlFor="wait-type" className="form-label">Wait Type</label>
          <Select
            id="wait-type"
            name="wait-type"
            value={(formData.metaData?.waitType as string) || 'timeout'}
            onChange={(e) => handleMetaDataChange('waitType', e.target.value)}
            options={[
              { value: 'timeout', label: 'Fixed timeout' },
              { value: 'element', label: 'Wait for element' },
              { value: 'network', label: 'Wait for network' },
              { value: 'animation', label: 'Wait for animation' }
            ]}
          />
        </div>
      )}

      {/* Element selector preview if element exists */}
      {event.element && (
        <div className="form-group">
          <h5>Element Information</h5>
          <div className="locator-section">
            <div className="locator-type">CSS Selector</div>
            <div className="locator-value">{event.element.cssSelector}</div>
            
            <div className="locator-type">XPath</div>
            <div className="locator-value">{event.element.xpath}</div>
          </div>
        </div>
      )}

      <div className="d-flex justify-content-end gap-2 mt-4">
        <Button 
          type="button" 
          variant="secondary" 
          onClick={onClose} 
          disabled={isLoading}
        >
          Cancel
        </Button>
        <Button 
          type="submit" 
          disabled={isLoading}
        >
          Save Changes
        </Button>
      </div>
    </form>
  );
};

/**
 * Get placeholder text based on event type
 */
const getValuePlaceholder = (eventType: RecordedEventType): string => {
  switch (eventType) {
    case RecordedEventType.INPUT:
      return 'Text entered';
    case RecordedEventType.KEYPRESS:
      return 'Key pressed (e.g., Enter)';
    case RecordedEventType.NAVIGATION:
      return 'URL navigated to';
    case RecordedEventType.ASSERTION:
      return 'Expected value';
    case RecordedEventType.WAIT:
      return 'Timeout in milliseconds';
    case RecordedEventType.CUSTOM:
      return 'Custom action description';
    default:
      return '';
  }
};

export default EventEditor; 