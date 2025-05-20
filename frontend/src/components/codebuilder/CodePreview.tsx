import React, { useState, useEffect } from 'react';
import { TestStep, Variable, CodeGenerationOptions } from '../../types/codebuilder';
import Select from '../common/Select';
import Button from '../common/Button';

interface CodePreviewProps {
  steps: TestStep[];
  variables: Variable[];
  options: CodeGenerationOptions;
  onOptionsChange: (options: Partial<CodeGenerationOptions>) => void;
}

/**
 * CodePreview component for previewing generated code from test steps
 */
const CodePreview: React.FC<CodePreviewProps> = ({
  steps,
  variables,
  options,
  onOptionsChange
}) => {
  // State for the generated code
  const [generatedCode, setGeneratedCode] = useState<string>('');
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  
  // Available language options
  const languageOptions = [
    { value: 'java', label: 'Java' },
    { value: 'javascript', label: 'JavaScript' },
    { value: 'python', label: 'Python' },
    { value: 'csharp', label: 'C#' }
  ];
  
  // Available framework options by language
  const frameworkOptions = {
    java: [
      { value: 'selenium', label: 'Selenium' },
      { value: 'rest-assured', label: 'REST Assured' },
      { value: 'appium', label: 'Appium' }
    ],
    javascript: [
      { value: 'playwright', label: 'Playwright' },
      { value: 'cypress', label: 'Cypress' },
      { value: 'webdriverio', label: 'WebdriverIO' }
    ],
    python: [
      { value: 'selenium', label: 'Selenium' },
      { value: 'pytest', label: 'Pytest' },
      { value: 'robot', label: 'Robot Framework' }
    ],
    csharp: [
      { value: 'selenium', label: 'Selenium' },
      { value: 'playwright', label: 'Playwright' },
      { value: 'specflow', label: 'SpecFlow' }
    ]
  };
  
  // Handle option changes
  const handleOptionChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type } = e.target;
    const isCheckbox = type === 'checkbox';
    
    onOptionsChange({
      [name]: isCheckbox ? (e.target as HTMLInputElement).checked : value
    });
  };
  
  // Generate code whenever options or steps change
  useEffect(() => {
    // Only generate code if there are steps
    if (steps.length === 0) {
      setGeneratedCode('// No steps to generate code from.\n// Add steps in the Steps tab.');
      return;
    }
    
    setIsGenerating(true);
    setError(null);
    
    // In a real implementation, this would make an API call to a code generation service
    // For demonstration purposes, we'll generate simple code based on the steps
    setTimeout(() => {
      try {
        const code = generateCodeFromSteps(steps, variables, options);
        setGeneratedCode(code);
      } catch (err) {
        setError('Failed to generate code. Please try again later.');
        setGeneratedCode('// Error generating code');
      } finally {
        setIsGenerating(false);
      }
    }, 500);
  }, [steps, variables, options]);
  
  // Function to generate code from steps
  const generateCodeFromSteps = (
    steps: TestStep[],
    variables: Variable[],
    options: CodeGenerationOptions
  ): string => {
    const { language, framework, includeComments, includeImports, prettify } = options;
    
    // Sort steps by order
    const sortedSteps = [...steps].sort((a, b) => a.order - b.order);
    
    let code = '';
    
    // Add imports based on language and framework
    if (includeImports) {
      switch (language) {
        case 'java':
          code += '// Java imports\n';
          code += 'import org.openqa.selenium.By;\n';
          code += 'import org.openqa.selenium.WebDriver;\n';
          code += 'import org.openqa.selenium.WebElement;\n';
          code += 'import org.openqa.selenium.chrome.ChromeDriver;\n';
          if (framework === 'rest-assured') {
            code += 'import io.restassured.RestAssured;\n';
            code += 'import static io.restassured.RestAssured.*;\n';
          }
          code += '\n';
          break;
          
        case 'javascript':
          code += '// JavaScript imports\n';
          if (framework === 'playwright') {
            code += 'const { chromium } = require("playwright");\n';
          } else if (framework === 'cypress') {
            code += '// Cypress does not require traditional imports\n';
          } else if (framework === 'webdriverio') {
            code += 'const { remote } = require("webdriverio");\n';
          }
          code += '\n';
          break;
          
        case 'python':
          code += '# Python imports\n';
          if (framework === 'selenium') {
            code += 'from selenium import webdriver\n';
            code += 'from selenium.webdriver.common.by import By\n';
            code += 'from selenium.webdriver.common.keys import Keys\n';
          } else if (framework === 'pytest') {
            code += 'import pytest\n';
            code += 'from selenium import webdriver\n';
          }
          code += '\n';
          break;
          
        case 'csharp':
          code += '// C# imports\n';
          code += 'using System;\n';
          if (framework === 'selenium') {
            code += 'using OpenQA.Selenium;\n';
            code += 'using OpenQA.Selenium.Chrome;\n';
          } else if (framework === 'specflow') {
            code += 'using TechTalk.SpecFlow;\n';
            code += 'using OpenQA.Selenium;\n';
          }
          code += '\n';
          break;
      }
    }
    
    // Add class and method declarations
    switch (language) {
      case 'java':
        code += 'public class AutomatedTest {\n';
        code += '    public static void main(String[] args) {\n';
        if (framework === 'selenium') {
          code += '        WebDriver driver = new ChromeDriver();\n';
          code += '        \n';
        }
        break;
        
      case 'javascript':
        code += 'async function runTest() {\n';
        if (framework === 'playwright') {
          code += '    const browser = await chromium.launch();\n';
          code += '    const context = await browser.newContext();\n';
          code += '    const page = await context.newPage();\n';
          code += '    \n';
        }
        break;
        
      case 'python':
        code += 'def run_test():\n';
        if (framework === 'selenium') {
          code += '    driver = webdriver.Chrome()\n';
          code += '    \n';
        }
        break;
        
      case 'csharp':
        code += 'public class AutomatedTest\n{\n';
        code += '    public void RunTest()\n    {\n';
        if (framework === 'selenium') {
          code += '        IWebDriver driver = new ChromeDriver();\n';
          code += '        \n';
        }
        break;
    }
    
    // Add variable declarations
    if (variables.length > 0) {
      if (includeComments) {
        switch (language) {
          case 'java':
          case 'csharp':
            code += '        // Declare variables\n';
            break;
          case 'javascript':
            code += '    // Declare variables\n';
            break;
          case 'python':
            code += '    # Declare variables\n';
            break;
        }
      }
      
      variables.forEach(variable => {
        let variableDeclaration = '';
        
        switch (language) {
          case 'java':
            switch (variable.type) {
              case 'STRING':
                variableDeclaration = `        String ${variable.name} = "${variable.value}";`;
                break;
              case 'NUMBER':
                variableDeclaration = `        double ${variable.name} = ${variable.value};`;
                break;
              case 'BOOLEAN':
                variableDeclaration = `        boolean ${variable.name} = ${variable.value};`;
                break;
              case 'OBJECT':
                variableDeclaration = `        Map<String, Object> ${variable.name} = new HashMap<>(); // Initialized with ${variable.value}`;
                break;
              case 'ARRAY':
                variableDeclaration = `        List<Object> ${variable.name} = new ArrayList<>(); // Initialized with ${variable.value}`;
                break;
            }
            break;
            
          case 'javascript':
            variableDeclaration = `    const ${variable.name} = ${
              variable.type === 'STRING' ? `"${variable.value}"` : variable.value
            };`;
            break;
            
          case 'python':
            variableDeclaration = `    ${variable.name} = ${
              variable.type === 'STRING' ? `"${variable.value}"` : variable.value
            }`;
            break;
            
          case 'csharp':
            switch (variable.type) {
              case 'STRING':
                variableDeclaration = `        string ${variable.name} = "${variable.value}";`;
                break;
              case 'NUMBER':
                variableDeclaration = `        double ${variable.name} = ${variable.value};`;
                break;
              case 'BOOLEAN':
                variableDeclaration = `        bool ${variable.name} = ${variable.value};`;
                break;
              case 'OBJECT':
                variableDeclaration = `        var ${variable.name} = new Dictionary<string, object>(); // Initialized with ${variable.value}`;
                break;
              case 'ARRAY':
                variableDeclaration = `        var ${variable.name} = new List<object>(); // Initialized with ${variable.value}`;
                break;
            }
            break;
        }
        
        code += variableDeclaration + '\n';
      });
      
      code += '\n';
    }
    
    // Add steps
    if (includeComments) {
      switch (language) {
        case 'java':
        case 'csharp':
          code += '        // Test steps\n';
          break;
        case 'javascript':
          code += '    // Test steps\n';
          break;
        case 'python':
          code += '    # Test steps\n';
          break;
      }
    }
    
    sortedSteps.forEach((step, index) => {
      // Add step description if include comments is enabled
      if (includeComments && step.description) {
        switch (language) {
          case 'java':
          case 'csharp':
            code += `        // Step ${index + 1}: ${step.description}\n`;
            break;
          case 'javascript':
            code += `    // Step ${index + 1}: ${step.description}\n`;
            break;
          case 'python':
            code += `    # Step ${index + 1}: ${step.description}\n`;
            break;
        }
      }
      
      // Skip disabled steps
      if (step.disabled) {
        switch (language) {
          case 'java':
          case 'csharp':
            code += `        // DISABLED: ${step.name}\n`;
            break;
          case 'javascript':
            code += `    // DISABLED: ${step.name}\n`;
            break;
          case 'python':
            code += `    # DISABLED: ${step.name}\n`;
            break;
        }
        return;
      }
      
      // Generate code for the step
      let stepCode = '';
      
      switch (language) {
        case 'java':
          stepCode = generateJavaStepCode(step, framework);
          break;
        case 'javascript':
          stepCode = generateJavaScriptStepCode(step, framework);
          break;
        case 'python':
          stepCode = generatePythonStepCode(step, framework);
          break;
        case 'csharp':
          stepCode = generateCSharpStepCode(step, framework);
          break;
      }
      
      code += stepCode;
    });
    
    // Add closing brackets
    switch (language) {
      case 'java':
        code += '    }\n';
        code += '}\n';
        break;
        
      case 'javascript':
        if (framework === 'playwright') {
          code += '    \n';
          code += '    await browser.close();\n';
        }
        code += '}\n\n';
        code += 'runTest().catch(console.error);\n';
        break;
        
      case 'python':
        code += '\n';
        code += 'if __name__ == "__main__":\n';
        code += '    run_test()\n';
        break;
        
      case 'csharp':
        code += '    }\n';
        code += '}\n';
        break;
    }
    
    return code;
  };
  
  // Generate Java step code
  const generateJavaStepCode = (step: TestStep, framework: string): string => {
    let code = '';
    
    switch (step.type) {
      case 'ACTION':
        if (step.command === 'click' && step.target) {
          code += `        driver.findElement(By.${getJavaLocatorStrategy(step.target.strategy)}("${step.target.value}")).click();\n`;
        } else if (step.command === 'type' && step.target && step.parameters.length > 0) {
          const value = step.parameters.find(p => p.name === 'text')?.value || '';
          code += `        driver.findElement(By.${getJavaLocatorStrategy(step.target.strategy)}("${step.target.value}")).sendKeys("${value}");\n`;
        }
        // Add more action commands as needed
        break;
        
      case 'ASSERTION':
        if (step.command === 'exists' && step.target) {
          code += `        boolean elementExists = driver.findElements(By.${getJavaLocatorStrategy(step.target.strategy)}("${step.target.value}")).size() > 0;\n`;
          code += `        assert elementExists : "Element should exist";\n`;
        }
        // Add more assertion commands as needed
        break;
        
      case 'WAIT':
        if (step.command === 'wait') {
          const time = step.parameters.find(p => p.name === 'time')?.value || '1000';
          code += `        Thread.sleep(${time});\n`;
        }
        // Add more wait commands as needed
        break;
        
      case 'NAVIGATION':
        if (step.command === 'navigate') {
          const url = step.parameters.find(p => p.name === 'url')?.value || '';
          code += `        driver.get("${url}");\n`;
        }
        // Add more navigation commands as needed
        break;
        
      default:
        code += `        // Custom step: ${step.name}\n`;
        break;
    }
    
    return code;
  };
  
  // Generate JavaScript step code
  const generateJavaScriptStepCode = (step: TestStep, framework: string): string => {
    let code = '';
    const indent = '    ';
    
    switch (step.type) {
      case 'ACTION':
        if (framework === 'playwright') {
          if (step.command === 'click' && step.target) {
            code += `${indent}await page.click('${getPlaywrightLocator(step.target)}');\n`;
          } else if (step.command === 'type' && step.target && step.parameters.length > 0) {
            const value = step.parameters.find(p => p.name === 'text')?.value || '';
            code += `${indent}await page.fill('${getPlaywrightLocator(step.target)}', '${value}');\n`;
          }
        }
        // Add more frameworks and action commands as needed
        break;
        
      case 'NAVIGATION':
        if (framework === 'playwright') {
          if (step.command === 'navigate') {
            const url = step.parameters.find(p => p.name === 'url')?.value || '';
            code += `${indent}await page.goto('${url}');\n`;
          }
        }
        // Add more frameworks and navigation commands as needed
        break;
        
      default:
        code += `${indent}// Custom step: ${step.name}\n`;
        break;
    }
    
    return code;
  };
  
  // Generate Python step code
  const generatePythonStepCode = (step: TestStep, framework: string): string => {
    let code = '';
    const indent = '    ';
    
    switch (step.type) {
      case 'ACTION':
        if (framework === 'selenium') {
          if (step.command === 'click' && step.target) {
            code += `${indent}driver.find_element(${getPythonLocatorStrategy(step.target.strategy)}, "${step.target.value}").click()\n`;
          } else if (step.command === 'type' && step.target && step.parameters.length > 0) {
            const value = step.parameters.find(p => p.name === 'text')?.value || '';
            code += `${indent}driver.find_element(${getPythonLocatorStrategy(step.target.strategy)}, "${step.target.value}").send_keys("${value}")\n`;
          }
        }
        // Add more frameworks and action commands as needed
        break;
        
      case 'NAVIGATION':
        if (framework === 'selenium') {
          if (step.command === 'navigate') {
            const url = step.parameters.find(p => p.name === 'url')?.value || '';
            code += `${indent}driver.get("${url}")\n`;
          }
        }
        // Add more frameworks and navigation commands as needed
        break;
        
      default:
        code += `${indent}# Custom step: ${step.name}\n`;
        break;
    }
    
    return code;
  };
  
  // Generate C# step code
  const generateCSharpStepCode = (step: TestStep, framework: string): string => {
    let code = '';
    const indent = '        ';
    
    switch (step.type) {
      case 'ACTION':
        if (framework === 'selenium') {
          if (step.command === 'click' && step.target) {
            code += `${indent}driver.FindElement(By.${getCSharpLocatorStrategy(step.target.strategy)}("${step.target.value}")).Click();\n`;
          } else if (step.command === 'type' && step.target && step.parameters.length > 0) {
            const value = step.parameters.find(p => p.name === 'text')?.value || '';
            code += `${indent}driver.FindElement(By.${getCSharpLocatorStrategy(step.target.strategy)}("${step.target.value}")).SendKeys("${value}");\n`;
          }
        }
        // Add more frameworks and action commands as needed
        break;
        
      case 'NAVIGATION':
        if (framework === 'selenium') {
          if (step.command === 'navigate') {
            const url = step.parameters.find(p => p.name === 'url')?.value || '';
            code += `${indent}driver.Navigate().GoToUrl("${url}");\n`;
          }
        }
        // Add more frameworks and navigation commands as needed
        break;
        
      default:
        code += `${indent}// Custom step: ${step.name}\n`;
        break;
    }
    
    return code;
  };
  
  // Helper functions for locator strategies
  const getJavaLocatorStrategy = (strategy: string): string => {
    switch (strategy) {
      case 'CSS': return 'cssSelector';
      case 'XPATH': return 'xpath';
      case 'ID': return 'id';
      case 'NAME': return 'name';
      case 'TAG': return 'tagName';
      case 'CLASS': return 'className';
      case 'LINK_TEXT': return 'linkText';
      case 'PARTIAL_LINK_TEXT': return 'partialLinkText';
      default: return 'xpath';
    }
  };
  
  const getPlaywrightLocator = (target: { strategy: string, value: string }): string => {
    switch (target.strategy) {
      case 'CSS': return target.value;
      case 'XPATH': return `xpath=${target.value}`;
      case 'ID': return `#${target.value}`;
      case 'NAME': return `[name="${target.value}"]`;
      case 'TAG': return target.value;
      case 'CLASS': return `.${target.value}`;
      case 'LINK_TEXT': return `text="${target.value}"`;
      case 'PARTIAL_LINK_TEXT': return `text="${target.value}"`;
      default: return target.value;
    }
  };
  
  const getPythonLocatorStrategy = (strategy: string): string => {
    switch (strategy) {
      case 'CSS': return 'By.CSS_SELECTOR';
      case 'XPATH': return 'By.XPATH';
      case 'ID': return 'By.ID';
      case 'NAME': return 'By.NAME';
      case 'TAG': return 'By.TAG_NAME';
      case 'CLASS': return 'By.CLASS_NAME';
      case 'LINK_TEXT': return 'By.LINK_TEXT';
      case 'PARTIAL_LINK_TEXT': return 'By.PARTIAL_LINK_TEXT';
      default: return 'By.XPATH';
    }
  };
  
  const getCSharpLocatorStrategy = (strategy: string): string => {
    switch (strategy) {
      case 'CSS': return 'CssSelector';
      case 'XPATH': return 'XPath';
      case 'ID': return 'Id';
      case 'NAME': return 'Name';
      case 'TAG': return 'TagName';
      case 'CLASS': return 'ClassName';
      case 'LINK_TEXT': return 'LinkText';
      case 'PARTIAL_LINK_TEXT': return 'PartialLinkText';
      default: return 'XPath';
    }
  };
  
  // Handle copy code to clipboard
  const handleCopyCode = () => {
    navigator.clipboard.writeText(generatedCode).then(() => {
      // Show a temporary success message
      const button = document.getElementById('copy-button');
      if (button) {
        const originalText = button.textContent;
        button.textContent = 'Copied!';
        setTimeout(() => {
          button.textContent = originalText;
        }, 2000);
      }
    });
  };
  
  // Handle download code
  const handleDownloadCode = () => {
    const extension = getFileExtension(options.language);
    const blob = new Blob([generatedCode], { type: 'text/plain' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `generated_test.${extension}`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
  };
  
  const getFileExtension = (language: string): string => {
    switch (language) {
      case 'java': return 'java';
      case 'javascript': return 'js';
      case 'python': return 'py';
      case 'csharp': return 'cs';
      default: return 'txt';
    }
  };
  
  return (
    <div className="code-preview">
      <div className="code-preview-options">
        <div className="row">
          <div className="col-md-4">
            <div className="mb-3">
              <label htmlFor="language" className="form-label">Language</label>
              <Select
                id="language"
                name="language"
                value={options.language}
                onChange={handleOptionChange}
                options={languageOptions}
              />
            </div>
          </div>
          
          <div className="col-md-4">
            <div className="mb-3">
              <label htmlFor="framework" className="form-label">Framework</label>
              <Select
                id="framework"
                name="framework"
                value={options.framework}
                onChange={handleOptionChange}
                options={frameworkOptions[options.language as keyof typeof frameworkOptions] || []}
              />
            </div>
          </div>
          
          <div className="col-md-4">
            <div className="mb-3">
              <label className="form-label d-block">Options</label>
              <div className="form-check form-check-inline">
                <input
                  className="form-check-input"
                  type="checkbox"
                  id="includeComments"
                  name="includeComments"
                  checked={options.includeComments}
                  onChange={handleOptionChange}
                />
                <label className="form-check-label" htmlFor="includeComments">
                  Comments
                </label>
              </div>
              
              <div className="form-check form-check-inline">
                <input
                  className="form-check-input"
                  type="checkbox"
                  id="includeImports"
                  name="includeImports"
                  checked={options.includeImports}
                  onChange={handleOptionChange}
                />
                <label className="form-check-label" htmlFor="includeImports">
                  Imports
                </label>
              </div>
              
              <div className="form-check form-check-inline">
                <input
                  className="form-check-input"
                  type="checkbox"
                  id="prettify"
                  name="prettify"
                  checked={options.prettify}
                  onChange={handleOptionChange}
                />
                <label className="form-check-label" htmlFor="prettify">
                  Prettify
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div className="code-preview-content">
        {isGenerating && (
          <div className="loading-spinner-overlay">
            <div className="spinner-border text-primary" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
          </div>
        )}
        
        {error ? (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        ) : (
          <pre className="language-javascript">
            <code>{generatedCode}</code>
          </pre>
        )}
      </div>
      
      <div className="code-preview-actions">
        <Button
          id="copy-button"
          variant="outline"
          onClick={handleCopyCode}
          disabled={isGenerating || steps.length === 0}
        >
          <i className="bi bi-clipboard me-1"></i> Copy Code
        </Button>
        
        <Button
          variant="primary"
          onClick={handleDownloadCode}
          disabled={isGenerating || steps.length === 0}
        >
          <i className="bi bi-download me-1"></i> Download Code
        </Button>
      </div>
    </div>
  );
};

export default CodePreview; 