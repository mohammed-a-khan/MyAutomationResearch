import React, { useState, useRef, useEffect, useCallback } from 'react';
import './CodeEditor.css';

export interface Position {
  line: number;
  column: number;
}

export interface Selection {
  start: Position;
  end: Position;
}

export interface Theme {
  background: string;
  text: string;
  lineNumbers: string;
  lineHighlight: string;
  selection: string;
  comment: string;
  keyword: string;
  string: string;
  number: string;
  function: string;
  operator: string;
  variable: string;
}

export interface CodeEditorProps {
  code: string;
  language?: string;
  readOnly?: boolean;
  lineNumbers?: boolean;
  highlightedLines?: number[];
  showInvisibles?: boolean;
  tabSize?: number;
  theme?: 'light' | 'dark' | 'custom';
  customTheme?: Theme;
  minHeight?: string;
  maxHeight?: string;
  className?: string;
  onChange?: (value: string) => void;
  onSelectionChange?: (selection: Selection | null) => void;
  onCursorPositionChange?: (position: Position) => void;
  onMount?: (editor: any) => void;
}

// Default themes
const lightTheme: Theme = {
  background: '#FFFFFF',
  text: '#374151',
  lineNumbers: '#9CA3AF',
  lineHighlight: '#F3F4F6',
  selection: 'rgba(148, 25, 107, 0.1)',
  comment: '#6B7280',
  keyword: '#8B5CF6',
  string: '#10B981',
  number: '#F59E0B',
  function: '#3B82F6',
  operator: '#EF4444',
  variable: '#EC4899'
};

const darkTheme: Theme = {
  background: '#1F2937',
  text: '#F9FAFB',
  lineNumbers: '#9CA3AF',
  lineHighlight: '#374151',
  selection: 'rgba(236, 72, 153, 0.2)',
  comment: '#9CA3AF',
  keyword: '#A78BFA',
  string: '#34D399',
  number: '#FBBF24',
  function: '#60A5FA',
  operator: '#F87171',
  variable: '#F472B6'
};

// Define a type for the pattern handlers
type PatternHandler = string | ((match: string, ...groups: string[]) => string);

// Simple syntax highlighting based on regular expressions
const highlightSyntax = (code: string, language: string): string => {
  let highlighted = code
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
  
  // Basic patterns for common languages
  const patterns: Record<string, Array<[RegExp, PatternHandler]>> = {
    javascript: [
      [/\/\/.*$/gm, 'comment'],
      [/\/\*[\s\S]*?\*\//gm, 'comment'],
      [/"(?:\\"|[^"])*"/g, 'string'],
      [/'(?:\\'|[^'])*'/g, 'string'],
      [/`(?:\\`|[^`])*`/g, 'string'],
      [/\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|class|import|export|from|as|async|await|new|this|typeof|instanceof)\b/g, 'keyword'],
      [/\b(true|false|null|undefined|NaN)\b/g, 'keyword'],
      [/\b\d+\.?\d*\b/g, 'number'],
      [/\b[A-Za-z_$][\w$]*(?=\s*\()/g, 'function'],
      [/[+\-*/%=!<>?:&|^~]+/g, 'operator'],
    ],
    typescript: [
      [/\/\/.*$/gm, 'comment'],
      [/\/\*[\s\S]*?\*\//gm, 'comment'],
      [/"(?:\\"|[^"])*"/g, 'string'],
      [/'(?:\\'|[^'])*'/g, 'string'],
      [/`(?:\\`|[^`])*`/g, 'string'],
      [/\b(const|let|var|function|return|if|else|for|while|do|switch|case|break|continue|try|catch|finally|class|interface|type|enum|implements|extends|import|export|from|as|async|await|new|this|typeof|instanceof|keyof)\b/g, 'keyword'],
      [/\b(true|false|null|undefined|NaN|any|string|number|boolean|void)\b/g, 'keyword'],
      [/\b\d+\.?\d*\b/g, 'number'],
      [/\b[A-Za-z_$][\w$]*(?=\s*[\(<])/g, 'function'],
      [/[+\-*/%=!<>?:&|^~]+/g, 'operator'],
    ],
    html: [
      [/<!--[\s\S]*?-->/g, 'comment'],
      [/(<style[\s\S]*?>)([\s\S]*?)(<\/style>)/gi, (match: string, p1: string, p2: string, p3: string) => 
        `${p1.replace(/</g, '&lt;').replace(/>/g, '&gt;')}<span class="code-css">${highlightSyntax(p2, 'css')}</span>${p3.replace(/</g, '&lt;').replace(/>/g, '&gt;')}`
      ],
      [/(<script[\s\S]*?>)([\s\S]*?)(<\/script>)/gi, (match: string, p1: string, p2: string, p3: string) => 
        `${p1.replace(/</g, '&lt;').replace(/>/g, '&gt;')}<span class="code-javascript">${highlightSyntax(p2, 'javascript')}</span>${p3.replace(/</g, '&lt;').replace(/>/g, '&gt;')}`
      ],
      [/(<)([^>]+)(>)/g, (match: string, p1: string, p2: string, p3: string) => 
        `${p1}<span class="code-keyword">${p2}</span>${p3}`
      ],
      [/(<\/)([^>]+)(>)/g, (match: string, p1: string, p2: string, p3: string) => 
        `${p1}<span class="code-keyword">${p2}</span>${p3}`
      ],
      [/"[^"]*"/g, 'string'],
      [/'[^']*'/g, 'string'],
    ],
    css: [
      [/\/\*[\s\S]*?\*\//g, 'comment'],
      [/@[a-z-]+/g, 'keyword'],
      [/[.#][a-zA-Z0-9_-]+/g, 'variable'],
      [/[a-z-]+(?=:)/g, 'function'],
      [/"[^"]*"/g, 'string'],
      [/'[^']*'/g, 'string'],
      [/#[0-9a-fA-F]{3,6}/g, 'number'],
      [/\b\d+\.?\d*(px|em|rem|%|vh|vw|s|ms)?\b/g, 'number'],
      [/[:{};,]/g, 'operator'],
    ],
    json: [
      [/"(?:\\"|[^"])*"(?=\s*:)/g, 'function'], // Property names
      [/: ?"(?:\\"|[^"])*"/g, (match: string) => `: <span class="code-string">${match.substring(2)}</span>`], // String values
      [/: ?true|false|null/g, (match: string) => `: <span class="code-keyword">${match.substring(2)}</span>`], // Keywords
      [/: ?-?\d+\.?\d*([eE][+-]?\d+)?/g, (match: string) => `: <span class="code-number">${match.substring(2)}</span>`], // Numbers
    ],
    // Add more languages as needed
  };

  // Default to javascript if language isn't supported
  const languagePatterns = patterns[language.toLowerCase()] || patterns.javascript;
  
  // Apply highlighting patterns
  for (const [pattern, className] of languagePatterns) {
    if (typeof className === 'string') {
      highlighted = highlighted.replace(pattern, (match) => 
        `<span class="code-${className}">${match}</span>`
      );
    } else if (typeof className === 'function') {
      highlighted = highlighted.replace(pattern, className);
    }
  }

  return highlighted;
};

const CodeEditor: React.FC<CodeEditorProps> = ({
  code,
  language = 'javascript',
  readOnly = false,
  lineNumbers = true,
  highlightedLines = [],
  showInvisibles = false,
  tabSize = 2,
  theme = 'light',
  customTheme,
  minHeight = '200px',
  maxHeight = '600px',
  className = '',
  onChange,
  onSelectionChange,
  onCursorPositionChange,
  onMount
}) => {
  const [value, setValue] = useState(code);
  const [currentTheme, setCurrentTheme] = useState<Theme>(theme === 'light' ? lightTheme : darkTheme);
  const editorRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const highlightedContentRef = useRef<HTMLDivElement>(null);

  // Update theme when props change
  useEffect(() => {
    if (theme === 'custom' && customTheme) {
      setCurrentTheme(customTheme);
    } else if (theme === 'dark') {
      setCurrentTheme(darkTheme);
    } else {
      setCurrentTheme(lightTheme);
    }
  }, [theme, customTheme]);

  // Update code when props change
  useEffect(() => {
    setValue(code);
    updateHighlightedCode(code);
  }, [code]);

  // Initial setup on mount
  useEffect(() => {
    if (editorRef.current && textareaRef.current) {
      // Set tab size
      textareaRef.current.style.tabSize = String(tabSize);
      
      // Initialize
      updateHighlightedCode(value);
      
      // Call onMount callback if provided
      if (onMount && textareaRef.current) {
        onMount(textareaRef.current);
      }
    }
  }, []);

  // Handle tab key
  const handleKeyDown = useCallback((e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (readOnly) return;
    
    const textarea = e.currentTarget;
    
    // Handle tab key
    if (e.key === 'Tab') {
      e.preventDefault();
      
      const start = textarea.selectionStart;
      const end = textarea.selectionEnd;
      
      // Insert tab at cursor
      const newValue = value.substring(0, start) + ' '.repeat(tabSize) + value.substring(end);
      setValue(newValue);
      
      // Move cursor after the inserted tab
      setTimeout(() => {
        textarea.selectionStart = textarea.selectionEnd = start + tabSize;
      }, 0);
      
      updateHighlightedCode(newValue);
      onChange?.(newValue);
    }
  }, [value, tabSize, readOnly, onChange]);

  // Handle input changes
  const handleChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
    if (readOnly) return;
    
    const newValue = e.target.value;
    setValue(newValue);
    updateHighlightedCode(newValue);
    onChange?.(newValue);
  }, [readOnly, onChange]);

  // Handle selection changes
  const handleSelect = useCallback(() => {
    if (!textareaRef.current) return;
    
    const start = textareaRef.current.selectionStart;
    const end = textareaRef.current.selectionEnd;
    
    if (start === end) {
      // Just cursor position, no selection
      const position = getPositionFromIndex(value, start);
      onCursorPositionChange?.(position);
      onSelectionChange?.(null);
    } else {
      // Selection
      const selection = {
        start: getPositionFromIndex(value, start),
        end: getPositionFromIndex(value, end)
      };
      onSelectionChange?.(selection);
    }
  }, [value, onCursorPositionChange, onSelectionChange]);

  // Handle scroll sync
  const handleScroll = useCallback(() => {
    if (!textareaRef.current || !highlightedContentRef.current) return;
    
    highlightedContentRef.current.scrollTop = textareaRef.current.scrollTop;
    highlightedContentRef.current.scrollLeft = textareaRef.current.scrollLeft;
  }, []);

  // Convert string index to line/column position
  const getPositionFromIndex = (text: string, index: number): Position => {
    const lines = text.slice(0, index).split('\n');
    return {
      line: lines.length,
      column: lines[lines.length - 1].length + 1
    };
  };

  // Update highlighted code with syntax highlighting
  const updateHighlightedCode = (text: string) => {
    if (!highlightedContentRef.current) return;
    
    const lines = text.split('\n');
    const highlightedContent = lines.map((line, i) => {
      // Add non-breaking spaces for empty lines and preserve indentation
      const processedLine = line.replace(/^ {2}/g, '  ') || ' ';
      return `<div class="code-line${highlightedLines.includes(i + 1) ? ' code-line-highlighted' : ''}" data-line-number="${i + 1}">${highlightSyntax(processedLine, language)}</div>`;
    }).join('');
    
    highlightedContentRef.current.innerHTML = highlightedContent;
  };

  // Build CSS variables for theme
  const themeVars = {
    '--code-bg': currentTheme.background,
    '--code-text': currentTheme.text,
    '--code-line-numbers': currentTheme.lineNumbers,
    '--code-line-highlight': currentTheme.lineHighlight,
    '--code-selection': currentTheme.selection,
    '--code-comment': currentTheme.comment,
    '--code-keyword': currentTheme.keyword,
    '--code-string': currentTheme.string,
    '--code-number': currentTheme.number,
    '--code-function': currentTheme.function,
    '--code-operator': currentTheme.operator,
    '--code-variable': currentTheme.variable,
    '--code-min-height': minHeight,
    '--code-max-height': maxHeight,
  } as React.CSSProperties;

  const editorClasses = [
    'code-editor',
    readOnly ? 'code-editor-readonly' : '',
    lineNumbers ? 'code-editor-line-numbers' : '',
    showInvisibles ? 'code-editor-show-invisibles' : '',
    className
  ].filter(Boolean).join(' ');

  return (
    <div 
      className={editorClasses}
      style={themeVars}
      ref={editorRef}
    >
      <div className="code-editor-wrapper">
        {lineNumbers && (
          <div className="code-line-numbers">
            {value.split('\n').map((_, i) => (
              <div 
                key={i}
                className={`code-line-number ${highlightedLines.includes(i + 1) ? 'code-line-number-highlighted' : ''}`}
              >
                {i + 1}
              </div>
            ))}
          </div>
        )}
        <div className="code-editor-content">
          <div 
            className="code-highlighted-content" 
            ref={highlightedContentRef}
          />
          <textarea
            ref={textareaRef}
            className="code-textarea"
            value={value}
            onChange={handleChange}
            onKeyDown={handleKeyDown}
            onSelect={handleSelect}
            onScroll={handleScroll}
            spellCheck="false"
            autoCapitalize="off"
            autoComplete="off"
            autoCorrect="off"
            readOnly={readOnly}
            aria-label="Code editor"
          />
        </div>
      </div>
    </div>
  );
};

export default CodeEditor; 