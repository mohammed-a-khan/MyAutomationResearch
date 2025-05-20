import React, { useState, useEffect, ChangeEvent } from 'react';
import { useApiTesting } from '../../context/ApiTestingContext';
import { ApiRequest, ApiAssertion, Variable } from '../../types/api';
import { v4 as uuidv4 } from 'uuid';
import Button from '../common/Button';
import Input from '../common/Input';
import Select from '../common/Select';
import Tabs from '../common/Tab';

interface ApiRequestEditorProps {
  requestId: string | null;
  projectId?: string;
  onSaved?: () => void;
}

const ApiRequestEditor: React.FC<ApiRequestEditorProps> = ({ 
  requestId, 
  projectId,
  onSaved
}) => {
  const { 
    currentRequest, 
    setCurrentRequest, 
    createRequest, 
    updateRequest,
    resetCurrentRequest,
    isLoading,
    error
  } = useApiTesting();

  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [activeTab, setActiveTab] = useState('basic');
  const [bodyFormat, setBodyFormat] = useState('json');
  
  useEffect(() => {
    // If no request is selected, reset to create a new one
    if (!requestId) {
      resetCurrentRequest();
      
      // If we have a project ID, we should set it
      if (projectId && currentRequest) {
        setCurrentRequest({
          ...currentRequest,
          projectId
        });
      }
    }
  }, [requestId, projectId, resetCurrentRequest, currentRequest, setCurrentRequest]);

  if (!currentRequest) {
    return <div>Loading request...</div>;
  }

  const validate = (): boolean => {
    const errors: Record<string, string> = {};
    
    if (!currentRequest.name.trim()) {
      errors.name = 'Name is required';
    }
    
    if (!currentRequest.url.trim()) {
      errors.url = 'URL is required';
    } else if (!/^https?:\/\//.test(currentRequest.url)) {
      errors.url = 'URL must start with http:// or https://';
    }

    setValidationErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validate()) {
      return;
    }
    
    try {
      // Generate an ID if this is a new request
      const requestToSave: ApiRequest = {
        ...currentRequest,
        id: currentRequest.id || uuidv4(),
        updatedAt: Date.now()
      };
      
      // If this is a new request, also set the creation time
      if (!currentRequest.id) {
        requestToSave.createdAt = Date.now();
      }
      
      // Create or update based on whether we have an ID
      if (currentRequest.id) {
        await updateRequest(requestToSave);
      } else {
        await createRequest(requestToSave);
      }
      
      if (onSaved) {
        onSaved();
      }
    } catch (err) {
      console.error('Failed to save request:', err);
    }
  };

  const handleChange = (field: keyof ApiRequest, value: any) => {
    setCurrentRequest({
      ...currentRequest,
      [field]: value
    });
  };

  const handleHeaderChange = (index: number, field: 'key' | 'value', value: string) => {
    const headers = { ...currentRequest.headers };
    
    if (field === 'key') {
      const oldKey = Object.keys(headers)[index];
      const oldValue = headers[oldKey];
      delete headers[oldKey];
      headers[value] = oldValue;
    } else {
      const key = Object.keys(headers)[index];
      headers[key] = value;
    }
    
    setCurrentRequest({
      ...currentRequest,
      headers
    });
  };

  const addHeader = () => {
    setCurrentRequest({
      ...currentRequest,
      headers: {
        ...currentRequest.headers,
        '': ''
      }
    });
  };

  const removeHeader = (key: string) => {
    const headers = { ...currentRequest.headers };
    delete headers[key];
    
    setCurrentRequest({
      ...currentRequest,
      headers
    });
  };

  const handleQueryParamChange = (index: number, field: 'key' | 'value', value: string) => {
    const params = { ...currentRequest.queryParams };
    
    if (field === 'key') {
      const oldKey = Object.keys(params)[index];
      const oldValue = params[oldKey];
      delete params[oldKey];
      params[value] = oldValue;
    } else {
      const key = Object.keys(params)[index];
      params[key] = value;
    }
    
    setCurrentRequest({
      ...currentRequest,
      queryParams: params
    });
  };

  const addQueryParam = () => {
    setCurrentRequest({
      ...currentRequest,
      queryParams: {
        ...currentRequest.queryParams,
        '': ''
      }
    });
  };

  const removeQueryParam = (key: string) => {
    const params = { ...currentRequest.queryParams };
    delete params[key];
    
    setCurrentRequest({
      ...currentRequest,
      queryParams: params
    });
  };

  const handleAssertionChange = (index: number, field: keyof ApiAssertion, value: any) => {
    const assertions = [...currentRequest.assertions];
    assertions[index] = {
      ...assertions[index],
      [field]: value
    };
    
    setCurrentRequest({
      ...currentRequest,
      assertions
    });
  };

  const addAssertion = () => {
    const newAssertion: ApiAssertion = {
      id: uuidv4(),
      type: 'status',
      operator: '=',
      expected: '200'
    };
    
    setCurrentRequest({
      ...currentRequest,
      assertions: [...currentRequest.assertions, newAssertion]
    });
  };

  const removeAssertion = (index: number) => {
    const assertions = [...currentRequest.assertions];
    assertions.splice(index, 1);
    
    setCurrentRequest({
      ...currentRequest,
      assertions
    });
  };
  
  // Define form tabs
  const basicTabContent = (
    <div className="mt-3">
      <div className="row mb-3">
        <div className="col-md-6">
          <div className="form-group">
            <label className="form-label">Request Name</label>
            <Input
              type="text"
              value={currentRequest.name}
              onChange={(e: ChangeEvent<HTMLInputElement>) => handleChange('name', e.target.value)}
              className={validationErrors.name ? 'is-invalid' : ''}
              error={validationErrors.name}
            />
          </div>
        </div>
        <div className="col-md-6">
          <div className="form-group">
            <label className="form-label">Description</label>
            <Input
              type="textarea"
              rows={1}
              value={currentRequest.description}
              onChange={(e: any) => {
                handleChange('description', e.target.value);
              }}
            />
          </div>
        </div>
      </div>
      
      <div className="row mb-3">
        <div className="col-md-3">
          <div className="form-group">
            <label className="form-label">HTTP Method</label>
            <Select
              value={currentRequest.method}
              onChange={(e: React.ChangeEvent<HTMLSelectElement>) => handleChange('method', e.target.value)}
              options={[
                { value: 'GET', label: 'GET' },
                { value: 'POST', label: 'POST' },
                { value: 'PUT', label: 'PUT' },
                { value: 'DELETE', label: 'DELETE' },
                { value: 'PATCH', label: 'PATCH' },
                { value: 'HEAD', label: 'HEAD' },
                { value: 'OPTIONS', label: 'OPTIONS' }
              ]}
            />
          </div>
        </div>
        <div className="col-md-9">
          <div className="form-group">
            <label className="form-label">URL</label>
            <Input
              type="text"
              value={currentRequest.url}
              onChange={(e: ChangeEvent<HTMLInputElement>) => handleChange('url', e.target.value)}
              placeholder="https://api.example.com/endpoint"
              className={validationErrors.url ? 'is-invalid' : ''}
              error={validationErrors.url}
            />
          </div>
        </div>
      </div>
    </div>
  );
  
  const headersTabContent = (
    <div className="mt-3">
      {Object.keys(currentRequest.headers).map((key, index) => (
        <div key={index} className="row mb-2">
          <div className="col-md-5">
            <Input
              type="text"
              placeholder="Header name"
              value={key}
              onChange={(e: ChangeEvent<HTMLInputElement>) => handleHeaderChange(index, 'key', e.target.value)}
            />
          </div>
          <div className="col-md-5">
            <Input
              type="text"
              placeholder="Header value"
              value={currentRequest.headers[key]}
              onChange={(e: ChangeEvent<HTMLInputElement>) => handleHeaderChange(index, 'value', e.target.value)}
            />
          </div>
          <div className="col-md-2">
            <Button 
              variant="outline" 
              onClick={() => removeHeader(key)}
            >
              Remove
            </Button>
          </div>
        </div>
      ))}
      <Button 
        variant="outline" 
        onClick={addHeader} 
        className="mt-2"
      >
        Add Header
      </Button>
    </div>
  );
  
  const paramsTabContent = (
    <div className="mt-3">
      {Object.keys(currentRequest.queryParams).map((key, index) => (
        <div key={index} className="row mb-2">
          <div className="col-md-5">
            <Input
              type="text"
              placeholder="Parameter name"
              value={key}
              onChange={(e: ChangeEvent<HTMLInputElement>) => handleQueryParamChange(index, 'key', e.target.value)}
            />
          </div>
          <div className="col-md-5">
            <Input
              type="text"
              placeholder="Parameter value"
              value={currentRequest.queryParams[key]}
              onChange={(e: ChangeEvent<HTMLInputElement>) => handleQueryParamChange(index, 'value', e.target.value)}
            />
          </div>
          <div className="col-md-2">
            <Button 
              variant="outline" 
              onClick={() => removeQueryParam(key)}
            >
              Remove
            </Button>
          </div>
        </div>
      ))}
      <Button 
        variant="outline" 
        onClick={addQueryParam} 
        className="mt-2"
      >
        Add Query Parameter
      </Button>
    </div>
  );
  
  const bodyTabContent = (
    <div className="mt-3">
      <div className="form-group mb-3">
        <label className="form-label">Body Format</label>
        <Select
          value={currentRequest.bodyType}
          onChange={(e: React.ChangeEvent<HTMLSelectElement>) => handleChange('bodyType', e.target.value)}
          options={[
            { value: 'none', label: 'None' },
            { value: 'json', label: 'JSON' },
            { value: 'form', label: 'Form' },
            { value: 'text', label: 'Text' },
            { value: 'xml', label: 'XML' }
          ]}
        />
      </div>
      
      {currentRequest.bodyType !== 'none' && (
        <div className="form-group">
          <Input
            type="textarea"
            rows={10}
            value={currentRequest.body || ''}
            onChange={(e: any) => {
              handleChange('body', e.target.value);
            }}
            placeholder={currentRequest.bodyType === 'json' ? '{\n  "key": "value"\n}' : ''}
          />
        </div>
      )}
    </div>
  );
  
  const assertionsTabContent = (
    <div className="mt-3">
      {currentRequest.assertions.map((assertion, index) => (
        <div key={assertion.id} className="row mb-3 border-bottom pb-3">
          <div className="col-md-3">
            <div className="form-group">
              <label className="form-label">Assertion Type</label>
              <Select
                value={assertion.type}
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => handleAssertionChange(index, 'type', e.target.value)}
                options={[
                  { value: 'status', label: 'Status' },
                  { value: 'header', label: 'Header' },
                  { value: 'body', label: 'Body' },
                  { value: 'jsonPath', label: 'JSON Path' },
                  { value: 'responseTime', label: 'Response Time' }
                ]}
              />
            </div>
          </div>
          
          {(assertion.type === 'header' || assertion.type === 'jsonPath') && (
            <div className="col-md-2">
              <div className="form-group">
                <label className="form-label">Property</label>
                <Input
                  type="text"
                  value={assertion.property || ''}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => handleAssertionChange(index, 'property', e.target.value)}
                  placeholder={assertion.type === 'header' ? 'Content-Type' : '$.data.id'}
                />
              </div>
            </div>
          )}
          
          <div className={`col-md-${assertion.type === 'header' || assertion.type === 'jsonPath' ? '2' : '3'}`}>
            <div className="form-group">
              <label className="form-label">Operator</label>
              <Select
                value={assertion.operator}
                onChange={(e: React.ChangeEvent<HTMLSelectElement>) => handleAssertionChange(index, 'operator', e.target.value)}
                options={[
                  { value: '=', label: 'equals (=)' },
                  { value: '!=', label: 'not equals (!=)' },
                  { value: '>', label: 'greater than (>)' },
                  { value: '<', label: 'less than (<)' },
                  { value: '>=', label: 'greater or equal (>=)' },
                  { value: '<=', label: 'less or equal (<=)' },
                  { value: 'contains', label: 'contains' },
                  { value: 'not_contains', label: 'not contains' },
                  { value: 'exists', label: 'exists' },
                  { value: 'not_exists', label: 'not exists' },
                  { value: 'matches', label: 'matches regex' }
                ]}
              />
            </div>
          </div>
          
          <div className={`col-md-${assertion.type === 'header' || assertion.type === 'jsonPath' ? '3' : '4'}`}>
            <div className="form-group">
              <label className="form-label">Expected Value</label>
              <Input
                type="text"
                value={assertion.expected}
                onChange={(e: ChangeEvent<HTMLInputElement>) => handleAssertionChange(index, 'expected', e.target.value)}
                placeholder={assertion.type === 'status' ? '200' : ''}
              />
            </div>
          </div>
          
          <div className="col-md-2 d-flex align-items-end mb-2">
            <Button 
              variant="outline" 
              onClick={() => removeAssertion(index)}
            >
              Remove
            </Button>
          </div>
        </div>
      ))}
      
      <Button 
        variant="outline" 
        onClick={addAssertion}
      >
        Add Assertion
      </Button>
    </div>
  );
  
  // Define tab items
  const tabItems = [
    {
      id: 'basic',
      title: 'Basic',
      content: basicTabContent
    },
    {
      id: 'headers',
      title: 'Headers',
      content: headersTabContent
    },
    {
      id: 'params',
      title: 'Query Params',
      content: paramsTabContent
    },
    {
      id: 'body',
      title: 'Body',
      content: bodyTabContent
    },
    {
      id: 'assertions',
      title: 'Assertions',
      content: assertionsTabContent
    }
  ];

  return (
    <div className="api-request-editor">
      <h3>{currentRequest.id ? 'Edit Request' : 'Create New Request'}</h3>
      
      {error && (
        <div className="alert alert-danger" role="alert">
          Error: {error}
        </div>
      )}
      
      <form onSubmit={handleSave}>
        <Tabs
          items={tabItems}
          activeId={activeTab}
          onChange={setActiveTab}
          variant="horizontal"
        />
        
        <div className="d-flex justify-content-end mt-4">
          <Button 
            type="submit" 
            variant="primary" 
            disabled={isLoading}
          >
            {isLoading ? 'Saving...' : currentRequest.id ? 'Update Request' : 'Create Request'}
          </Button>
        </div>
      </form>
    </div>
  );
};

export default ApiRequestEditor; 