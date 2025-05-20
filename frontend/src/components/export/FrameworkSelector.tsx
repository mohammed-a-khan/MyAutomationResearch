import React from 'react';

interface Framework {
  id: string;
  name: string;
  description: string;
  languages: string[];
  features?: string[];
}

interface FrameworkSelectorProps {
  frameworks: Framework[];
  selectedFramework: string;
  onSelectFramework: (framework: string) => void;
}

/**
 * Component for selecting a framework in the Export module
 */
const FrameworkSelector: React.FC<FrameworkSelectorProps> = ({
  frameworks,
  selectedFramework,
  onSelectFramework
}) => {
  return (
    <div className="framework-selector">
      {frameworks.map((framework) => (
        <div
          key={framework.id}
          className={`framework-card ${selectedFramework === framework.id ? 'selected' : ''}`}
          onClick={() => onSelectFramework(framework.id)}
        >
          <div className="framework-name">{framework.name}</div>
          <div className="framework-description">{framework.description}</div>
          
          {framework.features && framework.features.length > 0 && (
            <div className="framework-features">
              {framework.features.slice(0, 3).map((feature, index) => (
                <span key={index} className="framework-feature">{feature}</span>
              ))}
              {framework.features.length > 3 && (
                <span className="framework-feature">+{framework.features.length - 3} more</span>
              )}
            </div>
          )}
          
          <div className="framework-languages">
            <small>Supports: {framework.languages.join(', ')}</small>
          </div>
        </div>
      ))}
      
      {frameworks.length === 0 && (
        <div className="no-frameworks">
          No frameworks available. Please check your connection or try again later.
        </div>
      )}
    </div>
  );
};

export default FrameworkSelector; 