import React, { useState, useEffect } from 'react';
import { useAdoIntegration, AdoConnection } from '../../context/AdoIntegrationContext';
import Input from '../common/Input';
import Button from '../common/Button';
import Spinner from '../common/Spinner';

interface ConnectionFormProps {
  existingConnectionId?: string;
  onConnectionSaved: () => void;
}

/**
 * Form for creating or editing ADO connections
 */
const ConnectionForm: React.FC<ConnectionFormProps> = ({ 
  existingConnectionId,
  onConnectionSaved
}) => {
  const [formData, setFormData] = useState<Omit<AdoConnection, 'id' | 'createdAt' | 'updatedAt'>>({
    name: '',
    url: '',
    pat: '',
    organizationName: '',
    projectName: '',
    isActive: true
  });
  const [isValidating, setIsValidating] = useState<boolean>(false);
  const [validationMessage, setValidationMessage] = useState<string>('');
  const [validationStatus, setValidationStatus] = useState<'success' | 'error' | null>(null);

  const {
    connections,
    isLoading,
    createConnection,
    updateConnection,
    validateConnection
  } = useAdoIntegration();

  // Load existing connection data if editing
  useEffect(() => {
    if (existingConnectionId) {
      const connection = connections.find(c => c.id === existingConnectionId);
      if (connection) {
        setFormData({
          name: connection.name,
          url: connection.url,
          pat: connection.pat,
          organizationName: connection.organizationName,
          projectName: connection.projectName,
          isActive: connection.isActive
        });
      }
    } else {
      // Reset form when switching to new connection
      setFormData({
        name: '',
        url: '',
        pat: '',
        organizationName: '',
        projectName: '',
        isActive: true
      });
    }
    // Reset validation state
    setValidationMessage('');
    setValidationStatus(null);
  }, [existingConnectionId, connections]);

  // Handle form field changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    // Reset validation on input change
    setValidationMessage('');
    setValidationStatus(null);
  };

  // Validate connection credentials
  const handleValidate = async () => {
    setIsValidating(true);
    setValidationMessage('');
    setValidationStatus(null);
    
    try {
      const isValid = await validateConnection(
        formData.url,
        formData.pat,
        formData.organizationName,
        formData.projectName
      );
      
      if (isValid) {
        setValidationStatus('success');
        setValidationMessage('Connection verified successfully!');
      } else {
        setValidationStatus('error');
        setValidationMessage('Unable to connect with the provided credentials. Please check and try again.');
      }
    } catch (error) {
      setValidationStatus('error');
      setValidationMessage(error instanceof Error 
        ? error.message 
        : 'An error occurred during validation. Please try again.');
    } finally {
      setIsValidating(false);
    }
  };

  // Save connection
  const handleSave = async () => {
    try {
      if (existingConnectionId) {
        await updateConnection(existingConnectionId, formData);
      } else {
        await createConnection(formData);
      }
      onConnectionSaved();
      
      if (!existingConnectionId) {
        // Reset form after creating new connection
        setFormData({
          name: '',
          url: '',
          pat: '',
          organizationName: '',
          projectName: '',
          isActive: true
        });
      }
      
      setValidationMessage('Connection saved successfully!');
      setValidationStatus('success');
    } catch (error) {
      setValidationStatus('error');
      setValidationMessage(error instanceof Error 
        ? error.message 
        : 'Failed to save connection. Please try again.');
    }
  };

  return (
    <div className="connection-form">
      <h3>{existingConnectionId ? 'Edit Connection' : 'Create New Connection'}</h3>
      
      <div className="form-grid">
        <Input
          label="Connection Name"
          name="name"
          value={formData.name}
          onChange={handleInputChange}
          required
          placeholder="Enter a name for this connection"
        />
        
        <Input
          label="ADO URL"
          name="url"
          value={formData.url}
          onChange={handleInputChange}
          required
          placeholder="https://dev.azure.com"
        />
        
        <Input
          label="Personal Access Token (PAT)"
          name="pat"
          type="password"
          value={formData.pat}
          onChange={handleInputChange}
          required
          placeholder="Enter your ADO Personal Access Token"
        />
        
        <Input
          label="Organization Name"
          name="organizationName"
          value={formData.organizationName}
          onChange={handleInputChange}
          required
          placeholder="Your ADO organization name"
        />
        
        <Input
          label="Project Name"
          name="projectName"
          value={formData.projectName}
          onChange={handleInputChange}
          required
          placeholder="Your ADO project name"
        />
        
        <div className="checkbox-field">
          <Input
            type="checkbox"
            label="Active"
            name="isActive"
            checked={formData.isActive}
            onChange={handleInputChange}
          />
        </div>
      </div>
      
      {validationMessage && (
        <div className={`validation-message ${validationStatus}`}>
          {validationMessage}
        </div>
      )}
      
      <div className="form-actions">
        <Button
          variant="secondary"
          onClick={handleValidate}
          disabled={isLoading || isValidating || !formData.url || !formData.pat || !formData.organizationName || !formData.projectName}
        >
          {isValidating ? 'Validating...' : 'Validate Connection'}
        </Button>
        
        <Button
          variant="primary"
          onClick={handleSave}
          disabled={isLoading || isValidating || !formData.name || !formData.url || !formData.pat || !formData.organizationName || !formData.projectName}
        >
          {isLoading ? 'Saving...' : (existingConnectionId ? 'Update Connection' : 'Create Connection')}
        </Button>
      </div>
    </div>
  );
};

export default ConnectionForm; 