import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useExport } from '../../context/ExportContext';

// Import Common Components
import Button from '../common/Button';
import Card from '../common/Card';
import Select from '../common/Select';
import Checkbox from '../common/Checkbox';
import Input from '../common/Input';
import Tabs from '../common/Tab';

// Import Export Components
import FrameworkSelector from './FrameworkSelector';
import ConfigSelector from './ConfigSelector';
import ExportPreview from './ExportPreview';

// Import Styles
import './Export.css';

/**
 * Main component for the Export Module
 */
const ExportPanel: React.FC = () => {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('framework');

  const {
    isLoading,
    error,
    exportOptions,
    selectedFramework,
    selectedLanguage,
    exportConfig,
    frameworks,
    setFramework,
    setLanguage,
    updateExportConfig,
    loadExportOptions,
    exportProjectCode
  } = useExport();

  // Load export options when the component mounts
  useEffect(() => {
    if (projectId) {
      loadExportOptions(projectId);
    }
  }, [projectId, loadExportOptions]);

  // Handle framework change
  const handleFrameworkChange = (frameworkId: string) => {
    setFramework(frameworkId);
    
    // Find the selected framework and update languages
    const framework = frameworks.find(f => f.id === frameworkId);
    if (framework && framework.languages.length > 0) {
      // Set the first language by default
      setLanguage(framework.languages[0]);
      
      // Update output structure based on language
      if (exportOptions?.availableStructures) {
        updateExportConfig({
          outputStructure: exportOptions.availableStructures[framework.languages[0]] || ''
        });
      }
    }
  };

  // Handle language change
  const handleLanguageChange = (language: string) => {
    setLanguage(language);
    
    // Update output structure based on language
    if (exportOptions?.availableStructures) {
      updateExportConfig({
        outputStructure: exportOptions.availableStructures[language] || ''
      });
    }
  };

  // Handle export button click
  const handleExport = async () => {
    await exportProjectCode();
  };

  // Handle cancel button click
  const handleCancel = () => {
    if (projectId) {
      navigate(`/projects/${projectId}`);
    } else {
      navigate('/projects');
    }
  };

  // Render loading state
  if (isLoading && !exportOptions) {
    return (
      <div className="export-container">
        <div className="export-loading">Loading export options...</div>
      </div>
    );
  }

  return (
    <div className="export-container">
      <div className="export-header">
        <h1 className="export-title">Export Test Code</h1>
      </div>
      
      <div className="export-description">
        Export your test automation code to one of the supported frameworks. 
        Choose a framework, language, and customize export options.
      </div>
      
      {error && <div className="export-error">{error}</div>}
      
      <Tabs 
        activeTab={activeTab} 
        onChange={setActiveTab}
        items={[
          {
            id: "framework",
            title: "Framework",
            content: (
              <>
                <div className="export-section">
                  <div className="export-section-title">Select Framework</div>
                  <FrameworkSelector
                    frameworks={frameworks}
                    selectedFramework={selectedFramework}
                    onSelectFramework={handleFrameworkChange}
                  />
                </div>
                
                <div className="export-section">
                  <div className="export-section-title">Select Language</div>
                  <div className="language-options">
                    {selectedFramework && frameworks.find(f => f.id === selectedFramework)?.languages.map(lang => (
                      <div 
                        key={lang}
                        className={`language-option ${lang === selectedLanguage ? 'selected' : ''}`}
                        onClick={() => handleLanguageChange(lang)}
                      >
                        {lang}
                      </div>
                    ))}
                  </div>
                </div>
                
                <div className="export-footer">
                  <div className="export-actions">
                    <Button
                      variant="secondary"
                      onClick={() => setActiveTab('options')}
                    >
                      Next: Configure Options
                    </Button>
                  </div>
                </div>
              </>
            )
          },
          {
            id: "options",
            title: "Options",
            content: (
              <>
                <ConfigSelector
                  config={exportConfig}
                  onConfigChange={updateExportConfig}
                />
                
                <div className="export-footer">
                  <div className="export-actions">
                    <Button
                      variant="secondary"
                      onClick={() => setActiveTab('framework')}
                    >
                      Back: Framework Selection
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={() => setActiveTab('preview')}
                    >
                      Next: Preview Export
                    </Button>
                  </div>
                </div>
              </>
            )
          },
          {
            id: "preview",
            title: "Preview",
            content: (
              <>
                <ExportPreview
                  projectId={projectId || ''}
                  config={exportConfig}
                />
                
                <div className="export-footer">
                  <div className="export-actions">
                    <Button
                      variant="secondary"
                      onClick={() => setActiveTab('options')}
                    >
                      Back: Configure Options
                    </Button>
                    <Button
                      variant="secondary"
                      onClick={handleCancel}
                    >
                      Cancel
                    </Button>
                    <Button
                      variant="primary"
                      onClick={handleExport}
                      disabled={isLoading}
                    >
                      {isLoading ? 'Exporting...' : 'Export Code'}
                    </Button>
                  </div>
                </div>
              </>
            )
          }
        ]}
      />
    </div>
  );
};

export default ExportPanel; 