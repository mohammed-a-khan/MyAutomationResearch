import React, { useState, useEffect } from 'react';
import './Recorder.css';
import Button from '../common/Button';

interface CodePreviewProps {
  code: string;
}

/**
 * Component for displaying generated code with syntax highlighting
 */
const CodePreview: React.FC<CodePreviewProps> = ({ code }) => {
  const [theme, setTheme] = useState<'light' | 'dark' | 'custom'>('light');
  const [isCopied, setIsCopied] = useState<boolean>(false);

  useEffect(() => {
    // Reset copy state after 2 seconds
    if (isCopied) {
      const timer = setTimeout(() => {
        setIsCopied(false);
      }, 2000);
      
      return () => clearTimeout(timer);
    }
  }, [isCopied]);

  // Copy code to clipboard
  const handleCopyClick = () => {
    if (navigator.clipboard && code) {
      navigator.clipboard.writeText(code)
        .then(() => {
          setIsCopied(true);
        })
        .catch(error => {
          console.error('Failed to copy code:', error);
        });
    }
  };

  // Create highlighted HTML from code
  const formatCode = () => {
    if (!code) return <div className="code-preview-empty">No code generated yet</div>;

    // Basic syntax highlighting in JSX
    // In a real implementation, you would use a library like highlight.js or prism
    return (
      <pre className="code-preview-content">
        <code>
          {code.split('\n').map((line, i) => {
            // Highlight comments
            if (line.trimStart().startsWith('//')) {
              return (
                <div key={i} className="code-line">
                  <span className="code-comment">{line}</span>
                </div>
              );
            }

            // Highlight keywords
            const keywordHighlighted = line
              .replace(
                /\b(import|from|export|const|let|var|function|async|await|if|else|return|class|new|this)\b/g, 
                '<span class="code-keyword">$1</span>'
              )
              .replace(
                /\b(true|false|null|undefined)\b/g,
                '<span class="code-literal">$1</span>'
              )
              .replace(
                /(["'])(?:(?=(\\?))\2.)*?\1/g,
                '<span class="code-string">$&</span>'
              )
              .replace(
                /\b(\d+)\b/g,
                '<span class="code-number">$1</span>'
              );

            return (
              <div key={i} className="code-line">
                <span dangerouslySetInnerHTML={{ __html: keywordHighlighted }} />
              </div>
            );
          })}
        </code>
      </pre>
    );
  };

  return (
    <div className={`code-preview code-preview-${theme}`}>
      <div className="code-preview-header">
        <div className="code-preview-themes">
          <button
            className={`theme-selector ${theme === 'light' ? 'active' : ''}`}
            onClick={() => setTheme('light')}
            aria-label="Light theme"
            title="Light theme"
          >
            <i className="bi bi-sun"></i>
          </button>
          <button
            className={`theme-selector ${theme === 'dark' ? 'active' : ''}`}
            onClick={() => setTheme('dark')}
            aria-label="Dark theme"
            title="Dark theme"
          >
            <i className="bi bi-moon"></i>
          </button>
        </div>
        
        <div className="code-preview-actions">
          <Button
            variant="outline"
            size="sm"
            onClick={handleCopyClick}
            disabled={!code || isCopied}
          >
            {isCopied ? (
              <>
                <i className="bi bi-check me-1"></i>
                Copied!
              </>
            ) : (
              <>
                <i className="bi bi-clipboard me-1"></i>
                Copy Code
              </>
            )}
          </Button>
        </div>
      </div>
      
      <div className="code-preview-container">
        {formatCode()}
      </div>
    </div>
  );
};

export default CodePreview; 