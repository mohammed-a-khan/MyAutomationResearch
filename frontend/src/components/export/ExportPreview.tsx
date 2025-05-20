import React, { useEffect, useState } from 'react';
import Card from '../common/Card';
import { useExport } from '../../context/ExportContext';
import { ExportConfig } from '../../services/exportService';
import './Export.css';

interface ExportPreviewProps {
  projectId: string;
  config: ExportConfig;
}

const ExportPreview: React.FC<ExportPreviewProps> = ({ projectId, config }) => {
  const [previewCode, setPreviewCode] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  
  const { generatePreview } = useExport();
  
  useEffect(() => {
    if (!projectId) return;
    
    const fetchPreview = async () => {
      setIsLoading(true);
      setError(null);
      
      try {
        const preview = await generatePreview(projectId, config);
        setPreviewCode(preview || '// No preview available');
      } catch (err: any) {
        setError(`Failed to generate preview: ${err.message || 'Unknown error'}`);
        setPreviewCode('// Error generating preview code');
      } finally {
        setIsLoading(false);
      }
    };
    
    fetchPreview();
  }, [projectId, config, generatePreview]);
  
  return (
    <div className="export-preview-container">
      <Card>
        <h3 className="preview-title">Code Preview</h3>
        
        {isLoading && (
          <div className="preview-loading">
            Generating code preview...
          </div>
        )}
        
        {error && (
          <div className="preview-error">
            {error}
          </div>
        )}
        
        {!isLoading && !error && (
          <div className="preview-content">
            <div className="preview-info">
              <p>This is a preview of your generated code using the selected framework and options.</p>
            </div>
            
            <div className="code-preview">
              <pre className="code-block">
                <code>{previewCode}</code>
              </pre>
            </div>
            
            <div className="preview-notice">
              <p>Note: The actual exported code may contain additional files and structure not shown in this preview.</p>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
};

export default ExportPreview; 