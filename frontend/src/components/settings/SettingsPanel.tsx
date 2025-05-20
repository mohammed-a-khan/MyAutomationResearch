import React, { useState, useEffect } from 'react';
import Card from '../common/Card';
import Button from '../common/Button';
import Select from '../common/Select';
import Input from '../common/Input';
import Spinner from '../common/Spinner';
import Alert from '../common/Alert';
import Tabs from '../common/Tabs';
import settingsService, { 
  GeneralSettings, 
  ParallelExecutionSettings, 
  NotificationSettings 
} from '../../services/settingsService';
import './Settings.css';

/**
 * SettingsPanel component for application settings
 */
const SettingsPanel: React.FC = () => {
  const [activeTab, setActiveTab] = useState<string>('general');
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [message, setMessage] = useState<{text: string, type: 'success' | 'error' | 'info'} | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  // General settings
  const [generalSettings, setGeneralSettings] = useState<GeneralSettings>({
    defaultBrowser: 'chrome',
    defaultTimeout: 30,
    maxRetries: 1,
    screenshotOnError: true,
    logsEnabled: true,
    defaultEnvironment: 'DEV'
  });
  
  // Parallel execution settings
  const [parallelSettings, setParallelSettings] = useState<ParallelExecutionSettings>({
    enabled: false,
    maxParallelExecutions: 4
  });
  
  // Notification settings
  const [notificationSettings, setNotificationSettings] = useState<NotificationSettings>({
    emailNotifications: false,
    emailRecipients: '',
    slackIntegration: false,
    slackWebhook: '',
    notifyOnSuccess: false,
    notifyOnFailure: true
  });
  
  // Load settings when component mounts
  useEffect(() => {
    const loadSettings = async () => {
      setLoading(true);
      setError(null);
      
      try {
        // Load all settings from API using service
        const [generalData, parallelData, notificationData] = await Promise.all([
          settingsService.getGeneralSettings(),
          settingsService.getParallelSettings(),
          settingsService.getNotificationSettings()
        ]);
        
        setGeneralSettings(generalData);
        setParallelSettings(parallelData);
        setNotificationSettings(notificationData);
      } catch (err) {
        console.error('Failed to load settings:', err);
        setError(err instanceof Error ? err.message : 'Failed to load settings. Please try again.');
      } finally {
        setLoading(false);
      }
    };
    
    loadSettings();
  }, []);
  
  // Handle tab change
  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
  };
  
  // Handle general settings change
  const handleGeneralChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    const isCheckbox = type === 'checkbox';
    const isNumber = type === 'number';
    
    setGeneralSettings(prev => ({
      ...prev,
      [name]: isCheckbox 
        ? (e.target as HTMLInputElement).checked 
        : (isNumber ? parseInt(value, 10) : value)
    }));
  };
  
  // Handle parallel settings change
  const handleParallelChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    const isCheckbox = type === 'checkbox';
    const isNumber = type === 'number';
    
    setParallelSettings(prev => ({
      ...prev,
      [name]: isCheckbox 
        ? (e.target as HTMLInputElement).checked 
        : (isNumber ? parseInt(value, 10) : value)
    }));
  };
  
  // Handle notification settings change
  const handleNotificationChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>
  ) => {
    const { name, value, type } = e.target;
    const isCheckbox = type === 'checkbox';
    
    setNotificationSettings(prev => ({
      ...prev,
      [name]: isCheckbox ? (e.target as HTMLInputElement).checked : value
    }));
  };
  
  // Save settings
  const handleSaveSettings = async () => {
    setIsSaving(true);
    setMessage(null);
    
    try {
      // Save settings using the appropriate service method based on active tab
      if (activeTab === 'general') {
        await settingsService.updateGeneralSettings(generalSettings);
      } else if (activeTab === 'parallel') {
        await settingsService.updateParallelSettings(parallelSettings);
      } else if (activeTab === 'notifications') {
        await settingsService.updateNotificationSettings(notificationSettings);
      }
      
      setMessage({
        text: `${activeTab.charAt(0).toUpperCase() + activeTab.slice(1)} settings saved successfully!`,
        type: 'success'
      });
    } catch (error) {
      setMessage({
        text: error instanceof Error ? error.message : 'Failed to save settings. Please try again.',
        type: 'error'
      });
    } finally {
      setIsSaving(false);
    }
  };
  
  if (loading) {
    return <Spinner text="Loading settings..." />;
  }
  
  return (
    <div className="settings-container">
      <h1 className="page-title">Settings</h1>
      
      {error && (
        <Alert 
          type="error" 
          message={error} 
          onClose={() => setError(null)} 
        />
      )}
      
      {message && (
        <Alert 
          type={message.type} 
          message={message.text} 
          onClose={() => setMessage(null)} 
        />
      )}
      
      <Tabs
        activeTab={activeTab}
        onChange={handleTabChange}
        tabs={[
          { id: 'general', label: 'General Settings' },
          { id: 'parallel', label: 'Parallel Execution' },
          { id: 'notifications', label: 'Notifications' }
        ]}
      />
      
      <Card className="settings-card">
        {activeTab === 'general' && (
          <div className="settings-form">
            <h2>General Settings</h2>
            
            <div className="form-grid">
              <Select
                label="Default Browser"
                name="defaultBrowser"
                value={generalSettings.defaultBrowser}
                onChange={handleGeneralChange}
                options={[
                  { value: 'chrome', label: 'Google Chrome' },
                  { value: 'firefox', label: 'Mozilla Firefox' },
                  { value: 'edge', label: 'Microsoft Edge' },
                  { value: 'safari', label: 'Safari' }
                ]}
              />
              
              <Input
                label="Default Timeout (seconds)"
                type="number"
                name="defaultTimeout"
                value={generalSettings.defaultTimeout.toString()}
                onChange={handleGeneralChange}
                min="5"
                max="300"
              />
              
              <Input
                label="Maximum Retries"
                type="number"
                name="maxRetries"
                value={generalSettings.maxRetries.toString()}
                onChange={handleGeneralChange}
                min="0"
                max="5"
              />
              
              <div className="form-group checkbox-group">
                <Input
                  label="Take Screenshot on Error"
                  type="checkbox"
                  name="screenshotOnError"
                  checked={generalSettings.screenshotOnError}
                  onChange={handleGeneralChange}
                />
              </div>
              
              <div className="form-group checkbox-group">
                <Input
                  label="Enable Verbose Logging"
                  type="checkbox"
                  name="logsEnabled"
                  checked={generalSettings.logsEnabled}
                  onChange={handleGeneralChange}
                />
              </div>
              
              <Select
                label="Default Environment"
                name="defaultEnvironment"
                value={generalSettings.defaultEnvironment}
                onChange={handleGeneralChange}
                options={[
                  { value: 'DEV', label: 'Development' },
                  { value: 'QA', label: 'QA' },
                  { value: 'STAGING', label: 'Staging' },
                  { value: 'PROD', label: 'Production' }
                ]}
              />
            </div>
          </div>
        )}
        
        {activeTab === 'parallel' && (
          <div className="settings-form">
            <h2>Parallel Execution Settings</h2>
            
            <div className="form-grid">
              <div className="form-group checkbox-group">
                <Input
                  label="Enable Parallel Execution"
                  type="checkbox"
                  name="enabled"
                  checked={parallelSettings.enabled}
                  onChange={handleParallelChange}
                />
              </div>
              
              <Input
                label="Maximum Parallel Executions"
                type="number"
                name="maxParallelExecutions"
                value={parallelSettings.maxParallelExecutions.toString()}
                onChange={handleParallelChange}
                min="1"
                max="10"
                disabled={!parallelSettings.enabled}
              />
            </div>
            
            <div className="parallel-info">
              <p>
                <strong>Note:</strong> Parallel execution allows multiple test cases to run simultaneously, 
                which can significantly reduce test execution time. However, it may require more system resources.
              </p>
            </div>
          </div>
        )}
        
        {activeTab === 'notifications' && (
          <div className="settings-form">
            <h2>Notification Settings</h2>
            
            <div className="form-grid">
              <div className="form-group checkbox-group">
                <Input
                  label="Enable Email Notifications"
                  type="checkbox"
                  name="emailNotifications"
                  checked={notificationSettings.emailNotifications}
                  onChange={handleNotificationChange}
                />
              </div>
              
              <Input
                label="Email Recipients (comma separated)"
                type="text"
                name="emailRecipients"
                value={notificationSettings.emailRecipients}
                onChange={handleNotificationChange}
                placeholder="user@example.com, admin@example.com"
                disabled={!notificationSettings.emailNotifications}
              />
              
              <div className="form-group checkbox-group">
                <Input
                  label="Enable Slack Integration"
                  type="checkbox"
                  name="slackIntegration"
                  checked={notificationSettings.slackIntegration}
                  onChange={handleNotificationChange}
                />
              </div>
              
              <Input
                label="Slack Webhook URL"
                type="text"
                name="slackWebhook"
                value={notificationSettings.slackWebhook}
                onChange={handleNotificationChange}
                placeholder="https://hooks.slack.com/services/..."
                disabled={!notificationSettings.slackIntegration}
              />
              
              <div className="notification-triggers">
                <h3>Notification Triggers</h3>
                
                <div className="form-group checkbox-group">
                  <Input
                    label="Notify on Test Success"
                    type="checkbox"
                    name="notifyOnSuccess"
                    checked={notificationSettings.notifyOnSuccess}
                    onChange={handleNotificationChange}
                  />
                </div>
                
                <div className="form-group checkbox-group">
                  <Input
                    label="Notify on Test Failure"
                    type="checkbox"
                    name="notifyOnFailure"
                    checked={notificationSettings.notifyOnFailure}
                    onChange={handleNotificationChange}
                  />
                </div>
              </div>
            </div>
          </div>
        )}
        
        <div className="settings-actions">
          <Button
            variant="primary"
            onClick={handleSaveSettings}
            disabled={isSaving}
          >
            {isSaving ? 'Saving...' : 'Save Settings'}
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default SettingsPanel; 