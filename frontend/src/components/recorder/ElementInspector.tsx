import React from 'react';
import { ElementInfo } from '../../types/recorder';
import './Recorder.css';

interface ElementInspectorProps {
  element: ElementInfo | null;
}

/**
 * Displays detailed information about an inspected element
 */
const ElementInspector: React.FC<ElementInspectorProps> = ({ element }) => {
  if (!element) {
    return (
      <div className="element-inspector">
        <div className="element-inspector-empty">
          <div className="element-inspector-icon">
            <i className="bi bi-search"></i>
          </div>
          <h4>Element Inspector</h4>
          <p>
            Hover over an element or select an event to inspect element details
          </p>
        </div>
      </div>
    );
  }

  // Format an attribute value for display
  const formatAttributeValue = (value: string) => {
    if (value.length > 100) {
      return value.substring(0, 100) + '...';
    }
    return value;
  };

  // Create a syntax highlighted version of the element's HTML
  const renderElementPreview = () => {
    let preview = `<${element.tagName.toLowerCase()}`;
    
    // Add ID if present
    if (element.id) {
      preview += ` id="${element.id}"`;
    }
    
    // Add class if present
    if (element.className) {
      preview += ` class="${element.className}"`;
    }

    // Add selected other attributes
    const importantAttributes = ['name', 'type', 'value', 'href', 'src', 'alt', 'title', 'data-testid'];
    importantAttributes.forEach(attr => {
      if (element.attributes[attr]) {
        preview += ` ${attr}="${element.attributes[attr]}"`;
      }
    });

    // Close or add content depending on element type
    const selfClosingTags = ['img', 'input', 'br', 'hr', 'meta', 'link'];
    if (selfClosingTags.includes(element.tagName.toLowerCase())) {
      preview += ' />';
    } else {
      let content = element.textContent || '';
      if (content.length > 50) {
        content = content.substring(0, 50) + '...';
      }
      preview += `>${content}</${element.tagName.toLowerCase()}>`;
    }

    return (
      <pre className="element-preview-code">
        <code>{preview}</code>
      </pre>
    );
  };

  return (
    <div className="element-inspector">
      <div className="element-inspector-header">
        <h4>Element Inspector</h4>
      </div>
      
      <div className="element-inspector-content">
        <div className="element-preview">
          <h5>Element Preview</h5>
          {renderElementPreview()}
        </div>

        <div className="element-properties">
          <h5>Properties</h5>
          <table className="element-properties-table">
            <tbody>
              <tr>
                <td>Tag</td>
                <td><code>{element.tagName.toLowerCase()}</code></td>
              </tr>
              {element.id && (
                <tr>
                  <td>ID</td>
                  <td><code>{element.id}</code></td>
                </tr>
              )}
              {element.className && (
                <tr>
                  <td>Class</td>
                  <td><code>{element.className}</code></td>
                </tr>
              )}
              {element.textContent && (
                <tr>
                  <td>Text</td>
                  <td><code>{element.textContent.length > 50 
                    ? element.textContent.substring(0, 50) + '...' 
                    : element.textContent}</code></td>
                </tr>
              )}
              <tr>
                <td>Position</td>
                <td>
                  x: {Math.round(element.rect.x)}, 
                  y: {Math.round(element.rect.y)}, 
                  w: {Math.round(element.rect.width)}, 
                  h: {Math.round(element.rect.height)}
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div className="element-locators">
          <h5>Locators</h5>
          <div className="locators-list">
            <div className="locator-item">
              <div className="locator-label">XPath</div>
              <code className="locator-value">{element.xpath}</code>
            </div>
            <div className="locator-item">
              <div className="locator-label">CSS</div>
              <code className="locator-value">{element.cssSelector}</code>
            </div>
          </div>
        </div>

        {Object.keys(element.attributes).length > 0 && (
          <div className="element-attributes">
            <h5>Attributes</h5>
            <table className="element-attributes-table">
              <tbody>
                {Object.entries(element.attributes).map(([name, value]) => (
                  <tr key={name}>
                    <td>{name}</td>
                    <td><code>{formatAttributeValue(value)}</code></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default ElementInspector; 