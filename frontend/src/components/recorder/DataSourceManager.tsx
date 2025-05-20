import React, { useState, useCallback, useRef, useEffect } from 'react';
import { DataSource, DataSourceType, RecordedEvent } from '../../types/recorder';
import { useRecorder } from '../../context/RecorderContext';
import Input from '../common/Input';
import Select from '../common/Select';
import Button from '../common/Button';
import TextArea from '../common/TextArea';
import './DataSourceManager.css';

interface DataSourceManagerProps {
  event?: RecordedEvent;
  onClose: () => void;
}

/**
 * Component for managing data sources from various formats
 */
const DataSourceManager: React.FC<DataSourceManagerProps> = ({ event, onClose }) => {
  const { addDataSource, updateDataSource, state } = useRecorder();
  const isEditing = !!event?.dataSource;
  const fileInputRef = useRef<HTMLInputElement>(null);
  const { events } = state;
  
  const [dataSource, setDataSource] = useState<DataSource>(
    event?.dataSource || {
      id: `ds-${Date.now()}`,
      name: '',
      type: DataSourceType.JSON,
      content: '',
      mappings: {}
    }
  );
  
  const [previewData, setPreviewData] = useState<Array<any>>([]);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const [fileContent, setFileContent] = useState<string | null>(null);

  // Generate preview data
  useEffect(() => {
    if (dataSource.content) {
      try {
        let parsed;
        switch (dataSource.type) {
          case DataSourceType.JSON:
            parsed = JSON.parse(dataSource.content);
            // Ensure it's an array, or wrap single object
            if (!Array.isArray(parsed)) {
              if (typeof parsed === 'object' && parsed !== null) {
                parsed = [parsed];
              } else {
                throw new Error('Expected JSON array or object');
              }
            }
            setPreviewData(parsed.slice(0, 5)); // Show first 5 items
            setPreviewError(null);
            break;
            
          case DataSourceType.CSV:
            // Simple CSV parsing (very basic)
            const lines = dataSource.content.split('\n');
            const headers = lines[0].split(',');
            parsed = lines.slice(1).map(line => {
              const values = line.split(',');
              return headers.reduce((obj, header, i) => {
                obj[header.trim()] = values[i]?.trim() || '';
                return obj;
              }, {} as Record<string, string>);
            });
            setPreviewData(parsed.slice(0, 5));
            setPreviewError(null);
            break;
            
          case DataSourceType.VARIABLE:
            // Variable data sources don't need preview
            setPreviewData([]);
            setPreviewError(null);
            break;
            
          default:
            setPreviewData([]);
            setPreviewError('Preview not available for this data source type');
        }
      } catch (error) {
        setPreviewError(`Error parsing data: ${(error as Error).message}`);
        setPreviewData([]);
      }
    } else {
      setPreviewData([]);
      setPreviewError(null);
    }
  }, [dataSource.content, dataSource.type]);

  // Handle file upload
  const handleFileUpload = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (evt: ProgressEvent<FileReader>) => {
      const content = evt.target?.result as string;
      setFileContent(content);
      setDataSource(prev => ({
        ...prev,
        content,
        filePath: file.name
      }));
    };
    reader.readAsText(file);
  }, []);

  // Update data source property
  const handleChange = (field: keyof DataSource, value: any) => {
    setDataSource(prev => ({
      ...prev,
      [field]: value
    }));
  };

  // Add or update mapping
  const handleMappingChange = (key: string, value: string) => {
    setDataSource(prev => ({
      ...prev,
      mappings: {
        ...(prev.mappings || {}),
        [key]: value
      }
    }));
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isEditing) {
      await updateDataSource(dataSource.id, dataSource);
    } else {
      await addDataSource(dataSource);
    }
    
    onClose();
  };

  // Options for data source type dropdown
  const dataSourceTypeOptions = Object.values(DataSourceType).map(type => ({
    value: type,
    label: type
  }));

  return (
    <form onSubmit={handleSubmit} className="data-source-manager">
      <div className="form-group mb-3">
        <label htmlFor="ds-name" className="form-label">Data Source Name</label>
        <Input
          id="ds-name"
          type="text"
          value={dataSource.name}
          onChange={(e) => handleChange('name', e.target.value)}
          placeholder="Enter a descriptive name"
          required
        />
      </div>

      <div className="form-group mb-3">
        <label htmlFor="ds-type" className="form-label">Data Source Type</label>
        <Select
          id="ds-type"
          value={dataSource.type}
          onChange={(e) => handleChange('type', e.target.value)}
          options={dataSourceTypeOptions}
        />
      </div>

      {/* Different inputs based on data source type */}
      {dataSource.type === DataSourceType.JSON || dataSource.type === DataSourceType.CSV ? (
        <>
          <div className="form-group mb-3">
            <label htmlFor="ds-content" className="form-label">Data Content</label>
            <div className="d-flex mb-2">
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={() => fileInputRef.current?.click()}
              >
                <i className="bi bi-upload"></i> Upload File
              </Button>
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleFileUpload}
                style={{ display: 'none' }}
                accept={dataSource.type === DataSourceType.JSON ? '.json' : '.csv'}
              />
            </div>
            <TextArea
              id="ds-content"
              rows={8}
              value={dataSource.content || ''}
              onChange={(e) => handleChange('content', e.target.value)}
              placeholder={dataSource.type === DataSourceType.JSON ? 
                '[\n  { "id": 1, "name": "Example" }\n]' : 
                'id,name\n1,Example'}
            />
          </div>

          {/* Data Preview */}
          <div className="data-preview mb-3">
            <h6>Data Preview</h6>
            {previewError ? (
              <div className="alert alert-danger">{previewError}</div>
            ) : previewData.length > 0 ? (
              <div className="table-responsive">
                <table className="table table-sm table-bordered">
                  <thead>
                    <tr>
                      {Object.keys(previewData[0]).map(key => (
                        <th key={key}>{key}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {previewData.map((item, index) => (
                      <tr key={index}>
                        {Object.values(item).map((value, i) => (
                          <td key={i}>{typeof value === 'object' ? JSON.stringify(value) : String(value)}</td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="text-muted">No preview available. Enter valid data to see preview.</p>
            )}
          </div>
        </>
      ) : dataSource.type === DataSourceType.DATABASE ? (
        <>
          <div className="form-group mb-3">
            <label htmlFor="ds-connection" className="form-label">Connection String</label>
            <Input
              id="ds-connection"
              type="text"
              value={dataSource.connectionString || ''}
              onChange={(e) => handleChange('connectionString', e.target.value)}
              placeholder="e.g., jdbc:mysql://localhost:3306/testdb"
            />
          </div>

          <div className="form-group mb-3">
            <label htmlFor="ds-query" className="form-label">SQL Query</label>
            <TextArea
              id="ds-query"
              rows={3}
              value={dataSource.query || ''}
              onChange={(e) => handleChange('query', e.target.value)}
              placeholder="SELECT * FROM users LIMIT 10"
            />
          </div>
        </>
      ) : dataSource.type === DataSourceType.VARIABLE ? (
        <div className="variable-mappings mb-3">
          <h6 className="mb-2">Variable Mappings</h6>
          <p className="text-muted mb-3">
            Define variable mappings to use in your tests
          </p>
          
          {/* Variable mappings UI */}
          <div className="variable-mapping-list">
            {Object.entries(dataSource.variables || {}).map(([key, value], index) => (
              <div key={index} className="variable-mapping-item d-flex mb-2">
                <Input
                  value={key}
                  onChange={(e) => {
                    const newMappings = { ...dataSource.variables };
                    delete newMappings[key];
                    newMappings[e.target.value] = value;
                    handleChange('variables', newMappings);
                  }}
                  placeholder="Variable name"
                  className="me-2"
                />
                <Input
                  value={value}
                  onChange={(e) => {
                    handleChange('variables', {
                      ...(dataSource.variables || {}),
                      [key]: e.target.value
                    });
                  }}
                  placeholder="Value"
                />
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="ms-2"
                  onClick={() => {
                    const newMappings = { ...dataSource.variables };
                    delete newMappings[key];
                    handleChange('variables', newMappings);
                  }}
                >
                  <i className="bi bi-trash"></i>
                </Button>
              </div>
            ))}
            
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => {
                handleChange('variables', {
                  ...(dataSource.variables || {}),
                  [`variable${Object.keys(dataSource.variables || {}).length + 1}`]: ''
                });
              }}
            >
              <i className="bi bi-plus"></i> Add Variable
            </Button>
          </div>
        </div>
      ) : null}

      <div className="d-flex justify-content-end gap-2 mt-4">
        <Button type="button" variant="secondary" onClick={onClose}>
          Cancel
        </Button>
        <Button type="submit">
          {isEditing ? 'Update Data Source' : 'Create Data Source'}
        </Button>
      </div>
    </form>
  );
};

export default DataSourceManager; 