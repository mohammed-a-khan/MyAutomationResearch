import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import StepList from './StepList';
import StepBuilder from './StepBuilder';
import VariableManager from './VariableManager';
import NLPCodeGenerator from './NLPCodeGenerator';
import CodePreview from './CodePreview';
import Tabs from '../common/Tab';
import { CodeGenerationOptions } from '../../types/codebuilder';
import './CodeBuilder.css';

/**
 * CodeBuilderPanel is the main container component for the Code Builder module
 */
const CodeBuilderPanel: React.FC = () => {
  const { projectId = '' } = useParams<{ projectId: string }>();
  const {
    steps,
    variables,
    selectedStep,
    isLoading,
    error,
    loadProjectSteps,
    loadProjectVariables,
    setProjectId,
    clearError
  } = useCodeBuilder();

  // Component state
  const [activeTab, setActiveTab] = useState<string>('steps');
  const [codeOptions, setCodeOptions] = useState<CodeGenerationOptions>({
    language: 'java',
    framework: 'selenium',
    includeComments: true,
    includeImports: true,
    prettify: true
  });

  // Load project data on component mount
  useEffect(() => {
    if (projectId) {
      setProjectId(projectId);
      loadProjectSteps(projectId);
      loadProjectVariables(projectId);
    }
  }, [projectId, setProjectId, loadProjectSteps, loadProjectVariables]);

  // Handle tab change
  const handleTabChange = (tabId: string) => {
    setActiveTab(tabId);
  };

  // Update code generation options
  const handleOptionsChange = (options: Partial<CodeGenerationOptions>) => {
    setCodeOptions(prev => ({
      ...prev,
      ...options
    }));
  };

  // Create tab items for the Tabs component
  const tabItems = [
    {
      id: 'steps',
      title: 'Steps',
      content: (
        <div className="row">
          <div className="col-md-4">
            <StepList steps={steps} isLoading={isLoading} />
          </div>
          <div className="col-md-8">
            <StepBuilder projectId={projectId} selectedStep={selectedStep} />
          </div>
        </div>
      )
    },
    {
      id: 'variables',
      title: 'Variables',
      content: (
        <VariableManager projectId={projectId} variables={variables} isLoading={isLoading} />
      )
    },
    {
      id: 'nlp',
      title: 'NLP Code Generator',
      content: (
        <NLPCodeGenerator projectId={projectId} />
      )
    },
    {
      id: 'preview',
      title: 'Code Preview',
      content: (
        <CodePreview 
          steps={steps} 
          variables={variables} 
          options={codeOptions}
          onOptionsChange={handleOptionsChange}
        />
      )
    }
  ];

  return (
    <div className="code-builder-panel">
      {error && (
        <div className="alert alert-danger alert-dismissible fade show" role="alert">
          {error}
          <button
            type="button"
            className="btn-close"
            onClick={clearError}
            aria-label="Close"
          ></button>
        </div>
      )}

      <div className="code-builder-header">
        <h2>Code Builder</h2>
        <p className="text-muted">
          Create and manage test steps and variables for your project.
        </p>
      </div>

      {/* Use the Tabs component with items */}
      <div className="code-builder-content">
        <Tabs 
          items={tabItems}
          activeId={activeTab}
          onChange={handleTabChange}
          variant="horizontal"
          size="medium"
        />
      </div>
    </div>
  );
};

export default CodeBuilderPanel; 