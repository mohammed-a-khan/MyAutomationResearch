import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useParams } from 'react-router-dom';
import { 
  getExportOptions, 
  exportProject, 
  exportTestSuite, 
  exportTestCase,
  generatePreviewCode,
  ExportConfig,
  ExportResponse,
  FrameworkOption
} from '../services/exportService';

// Context state type
interface ExportContextState {
  projectId: string | null;
  isLoading: boolean;
  error: string | null;
  exportOptions: ExportResponse | null;
  selectedFramework: string;
  selectedLanguage: string;
  exportConfig: ExportConfig;
  frameworks: FrameworkOption[];
  setFramework: (framework: string) => void;
  setLanguage: (language: string) => void;
  updateExportConfig: (config: Partial<ExportConfig>) => void;
  loadExportOptions: (projectId: string) => Promise<void>;
  exportProjectCode: () => Promise<void>;
  exportTestSuiteCode: (testSuiteId: string) => Promise<void>;
  exportTestCaseCode: (testCaseId: string) => Promise<void>;
  generatePreview: (projectId: string, config: ExportConfig) => Promise<string>;
}

// Initial state
const initialConfig: ExportConfig = {
  framework: '',
  language: '',
  includeFramework: true,
  includeConfig: true,
  includeEnvironment: true,
  outputStructure: '',
  generatePageObjects: true
};

// Create the context
const ExportContext = createContext<ExportContextState | undefined>(undefined);

// Provider component
export const ExportProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const { projectId } = useParams<{ projectId: string }>();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [exportOptions, setExportOptions] = useState<ExportResponse | null>(null);
  const [selectedFramework, setSelectedFramework] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState('');
  const [exportConfig, setExportConfig] = useState<ExportConfig>(initialConfig);
  const [frameworks, setFrameworks] = useState<FrameworkOption[]>([]);
  
  // Load export options when project ID changes
  useEffect(() => {
    if (projectId) {
      loadExportOptions(projectId);
    }
  }, [projectId]);
  
  // Update selected framework
  const setFramework = (framework: string) => {
    setSelectedFramework(framework);
    setExportConfig(prev => ({ ...prev, framework }));
  };
  
  // Update selected language
  const setLanguage = (language: string) => {
    setSelectedLanguage(language);
    setExportConfig(prev => ({ ...prev, language }));
  };
  
  // Update export configuration
  const updateExportConfig = (config: Partial<ExportConfig>) => {
    setExportConfig(prev => ({ ...prev, ...config }));
  };
  
  // Load export options
  const loadExportOptions = async (projectId: string) => {
    setIsLoading(true);
    setError(null);
    
    try {
      const options = await getExportOptions(projectId);
      setExportOptions(options);
      setFrameworks(options.availableFrameworks);
      
      // Set defaults if available
      if (options.availableFrameworks.length > 0) {
        const defaultFramework = options.availableFrameworks[0].id;
        setSelectedFramework(defaultFramework);
        
        const defaultFrameworkLanguages = options.availableFrameworks[0].languages;
        if (defaultFrameworkLanguages.length > 0) {
          setSelectedLanguage(defaultFrameworkLanguages[0]);
        }
        
        setExportConfig(prev => ({
          ...prev,
          framework: defaultFramework,
          language: defaultFrameworkLanguages.length > 0 ? defaultFrameworkLanguages[0] : '',
          outputStructure: options.availableStructures[defaultFrameworkLanguages[0]] || ''
        }));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load export options');
      console.error('Error loading export options:', err);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Export project code
  const exportProjectCode = async () => {
    if (!projectId || !exportConfig.framework || !exportConfig.language) {
      setError('Invalid export configuration');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      await exportProject(projectId, exportConfig);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to export project');
      console.error('Error exporting project:', err);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Export test suite code
  const exportTestSuiteCode = async (testSuiteId: string) => {
    if (!projectId || !testSuiteId || !exportConfig.framework || !exportConfig.language) {
      setError('Invalid export configuration');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      await exportTestSuite(projectId, testSuiteId, exportConfig);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to export test suite');
      console.error('Error exporting test suite:', err);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Export test case code
  const exportTestCaseCode = async (testCaseId: string) => {
    if (!projectId || !testCaseId || !exportConfig.framework || !exportConfig.language) {
      setError('Invalid export configuration');
      return;
    }
    
    setIsLoading(true);
    setError(null);
    
    try {
      await exportTestCase(projectId, testCaseId, exportConfig);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to export test case');
      console.error('Error exporting test case:', err);
    } finally {
      setIsLoading(false);
    }
  };
  
  // Generate preview code
  const generatePreview = async (projectId: string, config: ExportConfig): Promise<string> => {
    if (!projectId || !config.framework || !config.language) {
      throw new Error('Invalid export configuration');
    }
    
    try {
      return await generatePreviewCode(projectId, config);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to generate preview';
      console.error('Error generating preview:', err);
      throw new Error(errorMessage);
    }
  };
  
  // Context value
  const value: ExportContextState = {
    projectId: projectId || null,
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
    exportProjectCode,
    exportTestSuiteCode,
    exportTestCaseCode,
    generatePreview
  };
  
  return (
    <ExportContext.Provider value={value}>
      {children}
    </ExportContext.Provider>
  );
};

// Hook for using the context
export const useExport = () => {
  const context = useContext(ExportContext);
  if (context === undefined) {
    throw new Error('useExport must be used within an ExportProvider');
  }
  return context;
}; 