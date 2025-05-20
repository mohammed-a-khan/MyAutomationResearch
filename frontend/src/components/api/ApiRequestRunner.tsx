import React, { useState } from 'react';
import { useApiTesting } from '../../context/ApiTestingContext';
import Button from '../common/Button';
import Tabs from '../common/Tab';

const ApiRequestRunner: React.FC = () => {
  const { 
    currentRequest, 
    executeRequest, 
    currentResponse,
    isLoading 
  } = useApiTesting();
  
  const [activeTab, setActiveTab] = useState('response');
  const [executionTime, setExecutionTime] = useState<number | null>(null);
  const [assertionResults, setAssertionResults] = useState<boolean[]>([]);
  
  if (!currentRequest) {
    return <div>No request selected</div>;
  }
  
  const handleExecute = async () => {
    if (!currentRequest) return;
    
    const startTime = performance.now();
    
    try {
      const response = await executeRequest(currentRequest);
      const endTime = performance.now();
      setExecutionTime(endTime - startTime);
      
      // Validate assertions
      if (currentRequest.assertions && currentRequest.assertions.length > 0) {
        const responseWithTime = {
          ...response,
          responseTime: endTime - startTime
        };
        
        const results = currentRequest.assertions.map(assertion => {
          try {
            switch (assertion.type) {
              case 'status':
                return String(response.status) === assertion.expected;
              case 'responseTime':
                const responseTime = endTime - startTime;
                const expected = Number(assertion.expected);
                switch (assertion.operator) {
                  case '<': return responseTime < expected;
                  case '<=': return responseTime <= expected;
                  case '>': return responseTime > expected;
                  case '>=': return responseTime >= expected;
                  case '=': return responseTime === expected;
                  default: return false;
                }
              case 'header':
                if (!assertion.property) return false;
                const headerValue = response.headers && response.headers[assertion.property.toLowerCase()];
                switch (assertion.operator) {
                  case '=': return headerValue === assertion.expected;
                  case '!=': return headerValue !== assertion.expected;
                  case 'contains': return headerValue && headerValue.includes(assertion.expected);
                  case 'not_contains': return headerValue && !headerValue.includes(assertion.expected);
                  case 'exists': return headerValue !== undefined;
                  case 'not_exists': return headerValue === undefined;
                  case 'matches': return headerValue && new RegExp(assertion.expected).test(headerValue);
                  default: return false;
                }
              // Additional assertion types can be added here
              default:
                return false;
            }
          } catch (error) {
            console.error('Error validating assertion:', error);
            return false;
          }
        });
        
        setAssertionResults(results);
      }
    } catch (error) {
      console.error('Error executing request:', error);
      setExecutionTime(null);
    }
  };
  
  const renderResponseContent = () => {
    if (!currentResponse) {
      return <div className="text-center p-5">No response data available</div>;
    }

    return (
      <div>
        <div className="d-flex justify-content-between mb-3">
          <div>
            <strong>Status:</strong>{' '}
            <span className={`badge ${currentResponse.status < 400 ? 'bg-success' : 'bg-danger'}`}>
              {currentResponse.status} {currentResponse.statusText}
            </span>
          </div>
          {executionTime !== null && (
            <div>
              <strong>Time:</strong> {executionTime.toFixed(2)} ms
            </div>
          )}
        </div>

        <pre className="response-body p-3 border rounded">
          {currentResponse.data ? 
            JSON.stringify(currentResponse.data, null, 2) : 
            '(No response body)'
          }
        </pre>
      </div>
    );
  };

  const renderHeadersContent = () => {
    if (!currentResponse) {
      return <div className="text-center p-5">No response data available</div>;
    }

    return (
      <div>
        <pre className="response-headers p-3 border rounded">
          {currentResponse.headers ? 
            JSON.stringify(currentResponse.headers, null, 2) : 
            '(No headers)'
          }
        </pre>
      </div>
    );
  };

  const renderAssertionsContent = () => {
    if (!currentResponse) {
      return <div className="text-center p-5">No response data available</div>;
    }

    if (currentRequest.assertions.length === 0) {
      return <p>No assertions defined</p>;
    }

    return (
      <div className="assertions-list">
        {currentRequest.assertions.map((assertion, index) => (
          <div 
            key={assertion.id} 
            className={`assertion-item ${
              assertionResults[index] === undefined ? '' : 
              assertionResults[index] ? 'assertion-passed' : 'assertion-failed'
            }`}
          >
            <div className="row mb-2 p-2 border-bottom">
              <div className="col-md-1">
                {assertionResults[index] !== undefined && (
                  <span className={`assertion-status ${assertionResults[index] ? 'text-success' : 'text-danger'}`}>
                    <i className={`bi ${assertionResults[index] ? 'bi-check-circle-fill' : 'bi-x-circle-fill'}`}></i>
                  </span>
                )}
              </div>
              <div className="col-md-11">
                <div>
                  <strong>{assertion.type}</strong>
                  {assertion.property && <span> - {assertion.property}</span>}
                  <span> {assertion.operator} </span>
                  <code>{assertion.expected}</code>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    );
  };
  
  // Define tab items
  const tabItems = [
    {
      id: 'response',
      title: 'Response',
      content: renderResponseContent()
    },
    {
      id: 'headers',
      title: 'Headers',
      content: renderHeadersContent()
    },
    {
      id: 'assertions',
      title: 'Assertions',
      content: renderAssertionsContent()
    }
  ];
  
  return (
    <div className="api-request-runner">
      <h3>Request Runner</h3>
      
      <div className="card mb-4">
        <div className="card-header">
          <div className="row">
            <div className="col-md-9">
              <span className="badge bg-secondary me-2">
                {currentRequest.method}
              </span>
              {currentRequest.url}
            </div>
            <div className="col-md-3 text-end">
              <Button 
                variant="primary" 
                onClick={handleExecute}
                disabled={isLoading}
              >
                {isLoading ? 'Executing...' : 'Execute'}
              </Button>
            </div>
          </div>
        </div>
        <div className="card-body">
          <div className="request-summary mb-4">
            <div className="row">
              <div className="col-md-6">
                <strong>Headers:</strong>
                <ul className="list-unstyled">
                  {Object.keys(currentRequest.headers).length === 0 ? (
                    <li><em>No headers</em></li>
                  ) : (
                    Object.entries(currentRequest.headers).map(([key, value]) => (
                      <li key={key}>
                        <strong>{key}:</strong> {value}
                      </li>
                    ))
                  )}
                </ul>
              </div>
              <div className="col-md-6">
                <strong>Query Parameters:</strong>
                <ul className="list-unstyled">
                  {Object.keys(currentRequest.queryParams).length === 0 ? (
                    <li><em>No query parameters</em></li>
                  ) : (
                    Object.entries(currentRequest.queryParams).map(([key, value]) => (
                      <li key={key}>
                        <strong>{key}:</strong> {value}
                      </li>
                    ))
                  )}
                </ul>
              </div>
            </div>
            
            {currentRequest.body && currentRequest.bodyType !== 'none' && (
              <div>
                <strong>Body ({currentRequest.bodyType}):</strong>
                <pre className="request-body mt-2 p-2 border rounded">
                  {currentRequest.body}
                </pre>
              </div>
            )}
          </div>
          
          <div className="response-section">
            <h4 className="mb-3">Response</h4>
            <Tabs
              items={tabItems}
              activeId={activeTab}
              onChange={setActiveTab}
              variant="horizontal"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default ApiRequestRunner; 