import React, { useEffect, useState } from 'react';
import { apiTestingService } from '../../services/apiTestingService';
import { useApiTesting } from '../../context/ApiTestingContext';
import Button from '../common/Button';

interface ApiHistoryViewerProps {
  requestId: string | null;
  projectId?: string;
}

interface HistoryItem {
  id: string;
  requestId: string;
  timestamp: number;
  duration: number;
  status: number;
  statusText: string;
  success: boolean;
  requestSnapshot: any;
  responseSnapshot: any;
}

const ApiHistoryViewer: React.FC<ApiHistoryViewerProps> = ({ requestId, projectId }) => {
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [selectedItem, setSelectedItem] = useState<HistoryItem | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { currentRequest } = useApiTesting();
  
  useEffect(() => {
    const fetchHistory = async () => {
      if (!requestId && !projectId) {
        setHistory([]);
        return;
      }
      
      try {
        setLoading(true);
        setError(null);
        
        // If we have a request ID, fetch history for that request
        // Otherwise, fetch history for the project or latest executions
        let historyData: HistoryItem[] = [];
        
        if (requestId) {
          historyData = await apiTestingService.getRequestExecutionHistory(requestId);
        } else if (projectId) {
          // This endpoint would need to be implemented on the backend
          // historyData = await apiTestingService.getProjectExecutionHistory(projectId);
          historyData = [];
        }
        
        setHistory(historyData);
        
        // Select the most recent item by default
        if (historyData.length > 0) {
          setSelectedItem(historyData[0]);
        } else {
          setSelectedItem(null);
        }
      } catch (err) {
        setError('Failed to fetch execution history');
        console.error('Error fetching execution history:', err);
      } finally {
        setLoading(false);
      }
    };
    
    fetchHistory();
  }, [requestId, projectId]);
  
  const handleSelectItem = (item: HistoryItem) => {
    setSelectedItem(item);
  };
  
  const getStatusBadgeVariant = (status: number) => {
    if (status >= 200 && status < 300) return 'bg-success';
    if (status >= 300 && status < 400) return 'bg-info';
    if (status >= 400 && status < 500) return 'bg-warning';
    return 'bg-danger';
  };
  
  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleString();
  };
  
  const renderHistoryList = () => {
    if (loading) {
      return <div className="text-center p-5">Loading history...</div>;
    }
    
    if (error) {
      return (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      );
    }
    
    if (history.length === 0) {
      return (
        <div className="text-center p-5">
          <p className="text-muted">No execution history available</p>
          {currentRequest && (
            <p>Execute this request to create history entries</p>
          )}
        </div>
      );
    }
    
    return (
      <table className="table table-hover">
        <thead>
          <tr>
            <th>Date/Time</th>
            <th>Status</th>
            <th>Duration</th>
            <th>Request Name</th>
          </tr>
        </thead>
        <tbody>
          {history.map(item => (
            <tr
              key={item.id}
              onClick={() => handleSelectItem(item)}
              className={`cursor-pointer ${selectedItem === item ? 'table-active' : ''}`}
            >
              <td>{formatDate(item.timestamp)}</td>
              <td>
                <span className={`badge ${getStatusBadgeVariant(item.status)}`}>
                  {item.status} {item.statusText}
                </span>
              </td>
              <td>{item.duration.toFixed(2)} ms</td>
              <td>{item.requestSnapshot?.name || 'Unknown'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    );
  };
  
  const renderHistoryDetail = () => {
    if (!selectedItem) {
      return <div className="text-center p-5">Select a history item to view details</div>;
    }
    
    return (
      <div className="history-detail">
        <h4>Execution Details</h4>
        
        <div className="mb-4">
          <strong>Executed at:</strong> {formatDate(selectedItem.timestamp)}<br />
          <strong>Duration:</strong> {selectedItem.duration.toFixed(2)} ms<br />
          <strong>Status:</strong>{' '}
          <span className={`badge ${getStatusBadgeVariant(selectedItem.status)}`}>
            {selectedItem.status} {selectedItem.statusText}
          </span>
        </div>
        
        <h5>Request</h5>
        <div className="card mb-3">
          <div className="card-body">
            <div className="mb-3">
              <strong>{selectedItem.requestSnapshot.method}</strong> {selectedItem.requestSnapshot.url}
            </div>
            
            <div className="mb-3">
              <h6>Headers</h6>
              <pre className="request-headers border p-2 rounded bg-light">
                {JSON.stringify(selectedItem.requestSnapshot.headers, null, 2) || '{}'}
              </pre>
            </div>
            
            {selectedItem.requestSnapshot.body && (
              <div className="mb-3">
                <h6>Body</h6>
                <pre className="request-body border p-2 rounded bg-light">
                  {typeof selectedItem.requestSnapshot.body === 'object'
                    ? JSON.stringify(selectedItem.requestSnapshot.body, null, 2)
                    : selectedItem.requestSnapshot.body}
                </pre>
              </div>
            )}
          </div>
        </div>
        
        <h5>Response</h5>
        <div className="card">
          <div className="card-body">
            <div className="mb-3">
              <h6>Headers</h6>
              <pre className="response-headers border p-2 rounded bg-light">
                {JSON.stringify(selectedItem.responseSnapshot.headers, null, 2) || '{}'}
              </pre>
            </div>
            
            <div>
              <h6>Body</h6>
              <pre className="response-body border p-2 rounded bg-light">
                {typeof selectedItem.responseSnapshot.data === 'object'
                  ? JSON.stringify(selectedItem.responseSnapshot.data, null, 2)
                  : selectedItem.responseSnapshot.data || '(No response body)'}
              </pre>
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="api-history-viewer">
      <h3>API Request History</h3>
      
      <div className="row mt-4">
        <div className="col-md-5">
          {renderHistoryList()}
        </div>
        <div className="col-md-7">
          {renderHistoryDetail()}
        </div>
      </div>
    </div>
  );
};

export default ApiHistoryViewer; 