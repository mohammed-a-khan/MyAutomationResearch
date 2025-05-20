/**
 * Types for the Code Builder module
 */

/**
 * Test step type enum
 */
export enum StepType {
  ACTION = 'ACTION',
  ASSERTION = 'ASSERTION',
  WAIT = 'WAIT',
  NAVIGATION = 'NAVIGATION',
  CUSTOM = 'CUSTOM'
}

/**
 * Assertion type enum
 */
export enum AssertionType {
  EQUALS = 'EQUALS',
  CONTAINS = 'CONTAINS',
  MATCHES = 'MATCHES',
  EXISTS = 'EXISTS',
  NOT_EXISTS = 'NOT_EXISTS',
  VISIBLE = 'VISIBLE',
  NOT_VISIBLE = 'NOT_VISIBLE',
  ENABLED = 'ENABLED',
  DISABLED = 'DISABLED',
  CHECKED = 'CHECKED',
  UNCHECKED = 'UNCHECKED'
}

/**
 * Locator strategy enum
 */
export enum LocatorStrategy {
  CSS = 'CSS',
  XPATH = 'XPATH',
  ID = 'ID',
  NAME = 'NAME',
  TAG = 'TAG',
  CLASS = 'CLASS',
  LINK_TEXT = 'LINK_TEXT',
  PARTIAL_LINK_TEXT = 'PARTIAL_LINK_TEXT',
  ACCESSIBILITY_ID = 'ACCESSIBILITY_ID'
}

/**
 * Element locator
 */
export interface Locator {
  id: string;
  strategy: LocatorStrategy;
  value: string;
  description?: string;
  confidence?: number;
}

/**
 * Suggested locator with confidence score
 */
export interface LocatorSuggestion {
  locator: Locator;
  alternatives: Locator[];
}

/**
 * Test step parameter
 */
export interface StepParameter {
  name: string;
  value: string;
  type: 'string' | 'number' | 'boolean' | 'variable' | 'locator';
  description?: string;
}

/**
 * Test step definition
 */
export interface TestStep {
  id: string;
  projectId: string;
  type: StepType;
  name: string;
  description?: string;
  command: string;
  parameters: StepParameter[];
  target?: Locator;
  order: number;
  parentId?: string;
  disabled?: boolean;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

/**
 * Test step creation payload
 */
export interface CreateStepPayload {
  projectId: string;
  type: StepType;
  name: string;
  description?: string;
  command: string;
  parameters: Omit<StepParameter, 'id'>[];
  target?: Omit<Locator, 'id'>;
  order?: number;
  parentId?: string;
}

/**
 * Test step update payload
 */
export interface UpdateStepPayload {
  name?: string;
  description?: string;
  command?: string;
  parameters?: Omit<StepParameter, 'id'>[];
  target?: Omit<Locator, 'id'>;
  order?: number;
  parentId?: string;
  disabled?: boolean;
}

/**
 * Variable type enum
 */
export enum VariableType {
  STRING = 'STRING',
  NUMBER = 'NUMBER',
  BOOLEAN = 'BOOLEAN',
  OBJECT = 'OBJECT',
  ARRAY = 'ARRAY'
}

/**
 * Variable scope enum
 */
export enum VariableScope {
  GLOBAL = 'GLOBAL',
  PROJECT = 'PROJECT',
  TEST = 'TEST',
  STEP = 'STEP'
}

/**
 * Variable definition
 */
export interface Variable {
  id: string;
  name: string;
  value: string;
  type: VariableType;
  scope: VariableScope;
  description?: string;
  projectId?: string;
  testId?: string;
  stepId?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Variable creation payload
 */
export interface CreateVariablePayload {
  name: string;
  value: string;
  type: VariableType;
  scope: VariableScope;
  description?: string;
  projectId?: string;
  testId?: string;
  stepId?: string;
}

/**
 * Variable update payload
 */
export interface UpdateVariablePayload {
  name?: string;
  value?: string;
  type?: VariableType;
  description?: string;
}

/**
 * NLP code generation request
 */
export interface NlpCodeRequest {
  projectId: string;
  naturalLanguage: string;
  contextSteps?: string[];
}

/**
 * NLP code generation response
 */
export interface NlpCodeResponse {
  steps: TestStep[];
  confidence: number;
  alternatives?: TestStep[][];
}

/**
 * Code generation options
 */
export interface CodeGenerationOptions {
  language: 'java' | 'javascript' | 'python' | 'csharp';
  framework: string;
  includeComments: boolean;
  includeImports: boolean;
  prettify: boolean;
}

/**
 * Generated code response
 */
export interface GeneratedCode {
  code: string;
  language: string;
  framework: string;
} 