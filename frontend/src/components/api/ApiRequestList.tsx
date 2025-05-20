import React, { useEffect, useState } from 'react';
import { useApiTesting } from '../../context/ApiTestingContext';
import Button from '../common/Button';
import Input from '../common/Input';

interface ApiRequestListProps {
  onSelectRequest: (requestId: string) => void;
  onCreateNew: () => void;
  projectId?: string;
}

const ApiRequestList: React.FC<ApiRequestListProps> = ({ 
  onSelectRequest, 
  onCreateNew,
  projectId 
}) => {
  const { apiRequests, fetchApiRequests, isLoading, error, deleteRequest } = useApiTesting();
  const [filter, setFilter] = useState('');

  useEffect(() => {
    fetchApiRequests(projectId);
  }, [fetchApiRequests, projectId]);

  const handleDelete = async (e: React.MouseEvent<HTMLButtonElement>, id: string) => {
    e.stopPropagation();
    if (window.confirm('Are you sure you want to delete this API request?')) {
      await deleteRequest(id);
    }
  };

  const filteredRequests = apiRequests.filter(
    request => request.name.toLowerCase().includes(filter.toLowerCase())
  );

  const getMethodBadgeClass = (method: string) => {
    switch (method) {
      case 'GET': return 'badge bg-success';
      case 'POST': return 'badge bg-primary';
      case 'PUT': return 'badge bg-warning';
      case 'DELETE': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  };

  return (
    <div className="api-request-list">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h3>API Requests</h3>
        <Button onClick={onCreateNew} variant="primary">
          Create New Request
        </Button>
      </div>

      <div className="mb-3">
        <Input
          placeholder="Filter requests..."
          value={filter}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setFilter(e.target.value)}
          leftIcon={<i className="bi bi-search"></i>}
        />
      </div>

      {isLoading && <p>Loading API requests...</p>}
      
      {error && (
        <div className="alert alert-danger" role="alert">
          Error: {error}
        </div>
      )}

      {!isLoading && !error && apiRequests.length === 0 && (
        <div className="text-center p-5">
          <p className="text-muted">No API requests found.</p>
          <Button onClick={onCreateNew} variant="outline">
            Create your first API request
          </Button>
        </div>
      )}

      {!isLoading && filteredRequests.length > 0 && (
        <table className="table table-hover">
          <thead>
            <tr>
              <th>Name</th>
              <th>Method</th>
              <th>URL</th>
              <th>Last Updated</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredRequests.map((request) => (
              <tr 
                key={request.id} 
                onClick={() => onSelectRequest(request.id)}
                className="cursor-pointer"
              >
                <td>{request.name}</td>
                <td>
                  <span className={getMethodBadgeClass(request.method)}>
                    {request.method}
                  </span>
                </td>
                <td className="text-truncate" style={{ maxWidth: '250px' }}>
                  {request.url}
                </td>
                <td>{new Date(request.updatedAt).toLocaleString()}</td>
                <td>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={(e) => handleDelete(e, request.id)}
                  >
                    Delete
                  </Button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default ApiRequestList; 