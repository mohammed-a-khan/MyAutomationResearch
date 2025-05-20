import React from 'react';
import Card from '../common/Card';
import Select from '../common/Select';
import Checkbox from '../common/Checkbox';
import Input from '../common/Input';
import { ExportConfig } from '../../services/exportService';

interface ConfigSelectorProps {
  config: ExportConfig;
  onConfigChange: (config: Partial<ExportConfig>) => void;
}

/**
 * Component for configuring export options
 */
const ConfigSelector: React.FC<ConfigSelectorProps> = ({ config, onConfigChange }) => {
  const handleChange = (field: string, value: any) => {
    onConfigChange({ [field]: value });
  };

  return (
    <div className="config-selector">
      <Card>
        <h3 className="config-title">Export Configuration</h3>
        
        <div className="config-option">
          <label>Output Structure</label>
          <Select
            options={[
              { value: 'pageObject', label: 'Page Object Model' },
              { value: 'flat', label: 'Flat Structure' },
              { value: 'hierarchical', label: 'Hierarchical' }
            ]}
            value={config.outputStructure}
            onChange={(value) => handleChange('outputStructure', value)}
          />
        </div>
        
        <div className="config-option">
          <Checkbox
            id="include-comments"
            label="Include Comments"
            checked={config.includeComments || false}
            onChange={(checked) => handleChange('includeComments', checked)}
          />
          <div className="config-description">
            Include descriptive comments in generated code
          </div>
        </div>
        
        <div className="config-option">
          <Checkbox
            id="generate-documentation"
            label="Generate Documentation"
            checked={config.generateDocumentation || false}
            onChange={(checked) => handleChange('generateDocumentation', checked)}
          />
          <div className="config-description">
            Generate documentation alongside code
          </div>
        </div>
        
        <div className="config-option">
          <Checkbox
            id="include-assertions"
            label="Include Assertions"
            checked={config.includeAssertions || false}
            onChange={(checked) => handleChange('includeAssertions', checked)}
          />
          <div className="config-description">
            Include default assertions in generated tests
          </div>
        </div>
        
        <div className="config-option">
          <label>Framework Version</label>
          <Input
            type="text"
            value={config.targetFrameworkVersion || ''}
            onChange={(e) => handleChange('targetFrameworkVersion', e.target.value)}
            placeholder="Latest"
          />
        </div>
        
        <div className="config-option">
          <label>Custom Output Path (Optional)</label>
          <Input
            type="text"
            value={config.customOutputPath || ''}
            onChange={(e) => handleChange('customOutputPath', e.target.value)}
            placeholder="/path/to/output"
          />
        </div>
      </Card>
    </div>
  );
};

export default ConfigSelector; 