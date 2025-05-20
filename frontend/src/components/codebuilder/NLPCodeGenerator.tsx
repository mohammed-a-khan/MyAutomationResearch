import React, { useState } from 'react';
import { useCodeBuilder } from '../../context/CodeBuilderContext';
import { TestStep, NlpCodeRequest, NlpCodeResponse } from '../../types/codebuilder';
import Button from '../common/Button';
import Input from '../common/Input';
import StepItem from './StepItem';

interface NLPCodeGeneratorProps {
  projectId: string;
}

/**
 * NLPCodeGenerator component for generating test steps from natural language
 */
const NLPCodeGenerator: React.FC<NLPCodeGeneratorProps> = ({ projectId }) => {
  const { generateCodeFromNLP, selectStep, isLoading } = useCodeBuilder();
  
  // Component state
  const [naturalLanguage, setNaturalLanguage] = useState<string>('');
  const [contextSteps, setContextSteps] = useState<string[]>([]);
  const [generatedResponse, setGeneratedResponse] = useState<NlpCodeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [isGenerating, setIsGenerating] = useState<boolean>(false);
  const [selectedStepIndex, setSelectedStepIndex] = useState<number | null>(null);
  
  // Handle input change
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    setNaturalLanguage(e.target.value);
    // Clear error when user starts typing
    if (error) setError(null);
  };
  
  // Handle generate code
  const handleGenerateCode = async () => {
    if (!naturalLanguage.trim()) {
      setError('Please enter a description of the steps you want to generate.');
      return;
    }
    
    setIsGenerating(true);
    setError(null);
    setGeneratedResponse(null);
    
    try {
      const request: NlpCodeRequest = {
        projectId,
        naturalLanguage: naturalLanguage.trim(),
        contextSteps
      };
      
      const response = await generateCodeFromNLP(request);
      setGeneratedResponse(response);
      setSelectedStepIndex(null);
    } catch (err) {
      setError('Failed to generate code. Please try again with a different description.');
    } finally {
      setIsGenerating(false);
    }
  };
  
  // Handle step click
  const handleStepClick = (step: TestStep, index: number) => {
    setSelectedStepIndex(index);
    selectStep(step);
  };
  
  // Compute confidence level class
  const getConfidenceClass = (confidence: number): string => {
    if (confidence >= 0.75) return 'text-success';
    if (confidence >= 0.5) return 'text-warning';
    return 'text-danger';
  };
  
  return (
    <div className="nlp-code-generator">
      <div className="nlp-code-generator-header">
        <h4>Natural Language Code Generator</h4>
        <p className="text-muted">
          Describe the test steps you want to create in plain English, and our AI will generate them for you.
        </p>
      </div>
      
      <div className="nlp-code-generator-input">
        <div className="mb-3">
          <label htmlFor="naturalLanguage" className="form-label">
            Step Description
          </label>
          <Input
            id="naturalLanguage"
            value={naturalLanguage}
            onChange={handleInputChange}
            type="textarea"
            rows={5}
            placeholder="Describe the test steps in natural language (e.g. 'Navigate to the login page, enter username and password, and click the login button')"
            disabled={isLoading || isGenerating}
            error={error || undefined}
          />
        </div>
        
        <div className="d-flex justify-content-end mb-4">
          <Button
            onClick={handleGenerateCode}
            disabled={isLoading || isGenerating || !naturalLanguage.trim()}
          >
            {isGenerating ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                Generating...
              </>
            ) : (
              <>
                <i className="bi bi-magic me-2"></i>
                Generate Steps
              </>
            )}
          </Button>
        </div>
        
        {generatedResponse && (
          <div className="card mb-4">
            <div className="card-header d-flex justify-content-between align-items-center">
              <h5 className="mb-0">Generated Steps</h5>
              <span className={`badge ${getConfidenceClass(generatedResponse.confidence)}`}>
                {Math.round(generatedResponse.confidence * 100)}% Confidence
              </span>
            </div>
            <div className="card-body">
              <p>The following steps were generated based on your description:</p>
            </div>
          </div>
        )}
      </div>
      
      {generatedResponse && (
        <div className="nlp-code-generator-results">
          {generatedResponse.steps.length === 0 ? (
            <div className="alert alert-warning" role="alert">
              No steps could be generated from the provided description. Please try again with a more specific description.
            </div>
          ) : (
            <>
              <div className="mb-3">
                <p className="small text-muted">
                  Click on a step to edit it in the Step Builder tab.
                </p>
              </div>
              
              <div className="step-list border rounded">
                {generatedResponse.steps.map((step, index) => (
                  <StepItem
                    key={step.id || index}
                    step={step}
                    isSelected={selectedStepIndex === index}
                    onClick={() => handleStepClick(step, index)}
                  />
                ))}
              </div>
              
              <div className="mt-4">
                <Button
                  onClick={() => {
                    setNaturalLanguage('');
                    setGeneratedResponse(null);
                    setSelectedStepIndex(null);
                  }}
                  variant="outline"
                >
                  Clear & Start Again
                </Button>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
};

export default NLPCodeGenerator; 