import React, { useState } from 'react';
import { Loop, LoopType, DataSource, RecordedEvent, Condition, ConditionType } from '../../types/recorder';
import { useRecorder } from '../../context/RecorderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';

interface LoopEditorProps {
  event: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for editing loop configuration
 */
const LoopEditor: React.FC<LoopEditorProps> = ({ event, onClose }) => {
  const { updateLoop, state } = useRecorder();
  const { events } = state;

  // Get data sources from recorded events
  const dataSources = events
    .filter(e => e.dataSource)
    .map(e => e.dataSource as DataSource);
  
  const [loop, setLoop] = useState<Loop>(event.loop || {
    type: LoopType.COUNT,
    count: 5,
    iterationVariable: 'i',
    eventIds: [],
    maxIterations: 100
  });

  // Generate loop condition if not present
  const [loopCondition, setLoopCondition] = useState<Condition>(
    (loop.type === LoopType.WHILE || loop.type === LoopType.UNTIL) && loop.condition
      ? loop.condition
      : {
          type: ConditionType.LESS_THAN,
          leftOperand: '${i}',
          rightOperand: '10',
          thenEventIds: []
        }
  );

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    // Update loop condition if relevant
    let updatedLoop = { ...loop };
    if (loop.type === LoopType.WHILE || loop.type === LoopType.UNTIL) {
      updatedLoop.condition = loopCondition;
    }
    
    await updateLoop(event.id, updatedLoop);
    onClose();
  };

  // Update loop property
  const handleLoopChange = (field: keyof Loop, value: any) => {
    setLoop(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Update condition property
  const handleConditionChange = (field: keyof Condition, value: any) => {
    setLoopCondition(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Options for loop type dropdown
  const loopTypeOptions = Object.values(LoopType).map(type => ({
    value: type,
    label: type.replace(/_/g, ' ').toLowerCase()
  }));

  // Options for condition type dropdown
  const conditionTypeOptions = Object.values(ConditionType)
    .filter(type => ![
      ConditionType.ELEMENT_EXISTS,
      ConditionType.ELEMENT_NOT_EXISTS,
      ConditionType.ELEMENT_VISIBLE,
      ConditionType.ELEMENT_NOT_VISIBLE
    ].includes(type))
    .map(type => ({
      value: type,
      label: type.replace(/_/g, ' ').toLowerCase()
    }));

  // Options for data source dropdown
  const dataSourceOptions = dataSources.map(source => ({
    value: source.id,
    label: `${source.name} (${source.type})`
  }));

  return (
    <form onSubmit={handleSubmit} className="loop-editor">
      <div className="form-group mb-3">
        <label htmlFor="loop-type" className="form-label">Loop Type</label>
        <Select
          id="loop-type"
          value={loop.type}
          onChange={(e) => handleLoopChange('type', e.target.value)}
          options={loopTypeOptions}
        />
      </div>

      {/* Fields specific to loop type */}
      {loop.type === LoopType.COUNT && (
        <div className="form-group mb-3">
          <label htmlFor="loop-count" className="form-label">Number of Iterations</label>
          <Input
            id="loop-count"
            type="number"
            min={1}
            max={1000}
            value={loop.count || 5}
            onChange={(e) => handleLoopChange('count', parseInt(e.target.value, 10))}
          />
        </div>
      )}

      {(loop.type === LoopType.WHILE || loop.type === LoopType.UNTIL) && (
        <div className="condition-section border rounded p-3 mb-3">
          <h6 className="mb-3">Loop Condition</h6>
          <div className="form-group mb-3">
            <label htmlFor="condition-type" className="form-label">Condition Type</label>
            <Select
              id="condition-type"
              value={loopCondition.type}
              onChange={(e) => handleConditionChange('type', e.target.value)}
              options={conditionTypeOptions}
            />
          </div>

          <div className="form-group mb-3">
            <label htmlFor="left-operand" className="form-label">Left Operand</label>
            <Input
              id="left-operand"
              type="text"
              value={loopCondition.leftOperand || ''}
              onChange={(e) => handleConditionChange('leftOperand', e.target.value)}
              placeholder="Variable or value"
            />
            <small className="form-text text-muted">
              Use ${'{variable}'} syntax to reference test variables
            </small>
          </div>

          <div className="form-group mb-3">
            <label htmlFor="right-operand" className="form-label">Right Operand</label>
            <Input
              id="right-operand"
              type="text"
              value={loopCondition.rightOperand || ''}
              onChange={(e) => handleConditionChange('rightOperand', e.target.value)}
              placeholder="Variable or value"
            />
            <small className="form-text text-muted">
              Use ${'{variable}'} syntax to reference test variables
            </small>
          </div>
        </div>
      )}

      {loop.type === LoopType.FOR_EACH && (
        <>
          <div className="form-group mb-3">
            <label htmlFor="data-source" className="form-label">Data Source</label>
            {dataSourceOptions.length > 0 ? (
              <Select
                id="data-source"
                value={loop.dataSourceId || ''}
                onChange={(e) => handleLoopChange('dataSourceId', e.target.value)}
                options={dataSourceOptions}
              />
            ) : (
              <div className="alert alert-warning" role="alert">
                No data sources available. Please add a data source first.
              </div>
            )}
          </div>
          
          {loop.dataSourceId && (
            <div className="form-group mb-3">
              <label htmlFor="data-path" className="form-label">Data Path</label>
              <Input
                id="data-path"
                type="text"
                value={loop.dataSourcePath || ''}
                onChange={(e) => handleLoopChange('dataSourcePath', e.target.value)}
                placeholder="e.g., $.data[*] or leave empty to iterate entire source"
              />
              <small className="form-text text-muted">
                Optional JSONPath expression to select array to iterate
              </small>
            </div>
          )}
        </>
      )}

      {/* Common fields for all loop types */}
      <div className="form-group mb-3">
        <label htmlFor="iteration-variable" className="form-label">Iteration Variable Name</label>
        <Input
          id="iteration-variable"
          type="text"
          value={loop.iterationVariable}
          onChange={(e) => handleLoopChange('iterationVariable', e.target.value)}
          placeholder="Variable name for current iteration value"
        />
        <small className="form-text text-muted">
          This variable will be available within the loop steps
        </small>
      </div>

      <div className="form-group mb-3">
        <label htmlFor="max-iterations" className="form-label">Maximum Iterations</label>
        <Input
          id="max-iterations"
          type="number"
          min={1}
          max={10000}
          value={loop.maxIterations || 100}
          onChange={(e) => handleLoopChange('maxIterations', parseInt(e.target.value, 10))}
        />
        <small className="form-text text-muted">
          Safety limit to prevent infinite loops
        </small>
      </div>

      <div className="loop-steps-section mb-4">
        <h6>Loop Steps</h6>
        <p className="text-muted">
          {loop.eventIds?.length ? 
            `${loop.eventIds.length} step(s) will be executed in each iteration.` : 
            'No steps have been added to this loop yet.'}
        </p>
        {/* Here we would show a list of selected events to execute in the loop */}
      </div>

      <div className="d-flex justify-content-end gap-2">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit" variant="primary">
          Save Loop
        </Button>
      </div>
    </form>
  );
};

export default LoopEditor; 