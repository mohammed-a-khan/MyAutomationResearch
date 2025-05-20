/**
 * Types for the Test Recorder module
 */

/**
 * Type of event recorded
 */
export enum RecordedEventType {
  CLICK = 'CLICK',
  INPUT = 'INPUT',
  SELECT = 'SELECT',
  KEYPRESS = 'KEYPRESS',
  NAVIGATION = 'NAVIGATION',
  SCREENSHOT = 'SCREENSHOT',
  ASSERTION = 'ASSERTION',
  WAIT = 'WAIT',
  CUSTOM = 'CUSTOM',
  MOUSE_HOVER = 'MOUSE_HOVER',
  RIGHT_CLICK = 'RIGHT_CLICK',
  DOUBLE_CLICK = 'DOUBLE_CLICK',
  DRAG_DROP = 'DRAG_DROP',
  TYPE = 'TYPE',
  HOVER = 'HOVER',
  KEY_PRESS = 'KEY_PRESS',
  SCROLL = 'SCROLL',
  MOUSE_DOWN = 'MOUSE_DOWN',
  MOUSE_UP = 'MOUSE_UP',
  CLEAR = 'CLEAR',
  CONDITIONAL = 'CONDITIONAL',
  LOOP = 'LOOP',
  DATA_SOURCE = 'DATA_SOURCE',
  CAPTURE = 'CAPTURE',
  GROUP = 'GROUP',
  TRY_CATCH = 'TRY_CATCH'
}

/**
 * Status of the recording session
 */
export enum RecordingStatus {
  IDLE = 'IDLE',
  ACTIVE = 'ACTIVE',
  PAUSED = 'PAUSED',
  ERROR = 'ERROR',
  INITIALIZING = 'INITIALIZING',
  RECORDING = 'RECORDING',
  STOPPING = 'STOPPING',
  COMPLETED = 'COMPLETED'
}

/**
 * Information about an element in the DOM
 */
export interface ElementInfo {
  tagName: string;
  id: string;
  className: string;
  textContent: string;
  value?: string;
  attributes: Record<string, string>;
  xpath: string;
  cssSelector: string;
  rect: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
}

/**
 * Represents a DOM element involved in a recording event
 * @deprecated Use ElementInfo instead
 */
export interface RecordedElement {
  tagName: string;
  id?: string;
  name?: string;
  className?: string;
  innerText?: string;
  attributes: Record<string, string>;
  xpath: string;
  cssSelector: string;
  coordinates?: {
    x: number;
    y: number;
    width: number;
    height: number;
  };
}

/**
 * Locator strategy for identifying elements
 */
export interface Locator {
  type: 'id' | 'css' | 'xpath' | 'text' | 'linkText' | 'testId' | 'name' | 'tagName' | 'className';
  value: string;
  score?: number; // Score for smart locator suggestions (higher is better)
}

/**
 * Represents a recorded event
 */
export interface RecordedEvent {
  id: string;
  type: RecordedEventType;
  timestamp: number;
  url: string;
  element?: ElementInfo;
  value?: string;
  data?: any; // Additional data specific to the event type
  locators?: Locator[]; // Available locator strategies for this element
  selectedLocator?: Locator; // The currently selected locator strategy
  screenshot?: string; // Base64 encoded screenshot at the time of the event
  code?: string; // Generated code for this event
  order: number; // Order in the sequence of events
  disabled?: boolean; // Whether this event is disabled/skipped
  notes?: string; // User notes about this event
  custom?: boolean; // Whether this is a manually added event
  metaData?: Record<string, any>;
  parentId?: string; // ID of the parent event (for nested events)
  childIds?: string[]; // IDs of child events
  condition?: Condition; // Condition configuration
  loop?: Loop; // Loop configuration
  dataSource?: DataSource; // Data source configuration
  variableBinding?: VariableBinding; // Variable binding configuration
  assertions?: AssertionConfig[]; // Assertion configurations
  stepGroup?: StepGroup; // Step grouping configuration
}

/**
 * Options for a recording session
 */
export interface RecordingOptions {
  projectId: string;
  browser: string;
  viewport: {
    width: number;
    height: number;
  };
  baseUrl: string;
  targetUrl: string;
  recordCss: boolean;
  recordCssSelectors?: boolean;
  recordXpaths?: boolean;
  recordNetwork: boolean;
  generateSmartLocators: boolean;
  includeAssertions: boolean;
  smartAssertions?: boolean;
  headless: boolean;
  captureScreenshots?: boolean;
  ignoreNonInteractive?: boolean;
  includeTimeouts?: boolean;
  framework?: string;
}

/**
 * Recording session information
 */
export interface RecordingSession {
  id: string;
  projectId: string;
  status: RecordingStatus;
  startTime: string;
  endTime?: string;
  browser: string;
  browserVersion?: string;
  viewport: {
    width: number;
    height: number;
  };
  events: RecordedEvent[];
  baseUrl: string;
  targetUrl?: string;
  currentUrl?: string;
  options: RecordingOptions;
  lastError?: string;
}

/**
 * State type for the recorder context
 */
export interface RecorderContextState {
  session: RecordingSession | null;
  status: RecordingStatus;
  events: RecordedEvent[];
  selectedEvent: RecordedEvent | null;
  inspectedElement: ElementInfo | null;
  generatedCode: string | null;
  error: string | null;
  isLoading: boolean;
}

/**
 * Code generation options
 */
export interface CodeGenerationOptions {
  language: 'typescript' | 'javascript' | 'java' | 'csharp' | 'python';
  framework: 'selenium' | 'playwright' | 'cypress' | 'testcafe' | 'webdriver';
  includeComments: boolean;
  includeImports: boolean;
  includePageObjects: boolean;
  useTypeScript: boolean;
  assertion: 'auto' | 'soft' | 'hard' | 'none';
}

/**
 * Result of code generation
 */
export interface GeneratedCode {
  testCode: string;
  language: string;
  framework: string;
  pageObjects?: Record<string, string>;
  testName?: string;
}

/**
 * Condition type for conditional logic
 */
export enum ConditionType {
  EQUALS = 'EQUALS',
  NOT_EQUALS = 'NOT_EQUALS',
  CONTAINS = 'CONTAINS',
  NOT_CONTAINS = 'NOT_CONTAINS',
  STARTS_WITH = 'STARTS_WITH',
  ENDS_WITH = 'ENDS_WITH',
  MATCHES_REGEX = 'MATCHES_REGEX',
  GREATER_THAN = 'GREATER_THAN',
  LESS_THAN = 'LESS_THAN',
  ELEMENT_EXISTS = 'ELEMENT_EXISTS',
  ELEMENT_NOT_EXISTS = 'ELEMENT_NOT_EXISTS',
  ELEMENT_VISIBLE = 'ELEMENT_VISIBLE',
  ELEMENT_NOT_VISIBLE = 'ELEMENT_NOT_VISIBLE',
  CUSTOM_EXPRESSION = 'CUSTOM_EXPRESSION'
}

/**
 * Condition for conditional logic
 */
export interface Condition {
  type: ConditionType;
  leftOperand: string; // Can be a variable or a value
  rightOperand?: string; // Optional for some condition types
  elementSelector?: Locator; // For element-based conditions
  customExpression?: string; // For custom expressions
  thenEventIds: string[]; // Events to execute if condition is true
  elseEventIds?: string[]; // Optional events to execute if condition is false
}

/**
 * Loop type for iterations
 */
export enum LoopType {
  COUNT = 'COUNT', // Fixed number of iterations
  WHILE = 'WHILE', // Loop while condition is true
  FOR_EACH = 'FOR_EACH', // Iterate over a data source
  UNTIL = 'UNTIL' // Loop until condition is true
}

/**
 * Loop configuration for iterations
 */
export interface Loop {
  type: LoopType;
  count?: number; // For COUNT loop type
  condition?: Condition; // For WHILE/UNTIL loop types
  dataSourceId?: string; // For FOR_EACH loop type
  dataSourcePath?: string; // Path within data source (e.g., JSON path)
  iterationVariable: string; // Variable to store current iteration value
  maxIterations?: number; // Safety limit for maximum iterations
  eventIds: string[]; // Events to execute in each iteration
}

/**
 * Data source type
 */
export enum DataSourceType {
  CSV = 'CSV',
  JSON = 'JSON',
  EXCEL = 'EXCEL',
  DATABASE = 'DATABASE',
  VARIABLE = 'VARIABLE'
}

/**
 * Data source configuration
 */
export interface DataSource {
  id: string;
  name: string;
  type: DataSourceType;
  content?: string; // For inline data (JSON, CSV)
  filePath?: string; // For file-based sources
  connectionString?: string; // For database sources
  query?: string; // For database sources
  variables?: Record<string, string>; // For variable-based sources
  mappings?: Record<string, string>; // Field mappings
}

/**
 * Variable binding for capturing and storing values
 */
export interface VariableBinding {
  name: string; // Variable name
  source: 'element' | 'response' | 'expression' | 'constant'; // Source of the value
  elementSelector?: Locator; // For element source
  elementProperty?: string; // For element source (e.g., 'textContent', 'value', 'attribute')
  attributeName?: string; // For element attribute
  jsonPath?: string; // For response source (JSON path or XPath)
  expression?: string; // For expression source
  value?: string; // For constant source
  scope: 'test' | 'session' | 'global'; // Scope of the variable
}

/**
 * Assertion type
 */
export enum AssertionType {
  EQUALS = 'EQUALS',
  NOT_EQUALS = 'NOT_EQUALS',
  CONTAINS = 'CONTAINS',
  NOT_CONTAINS = 'NOT_CONTAINS',
  MATCHES_REGEX = 'MATCHES_REGEX',
  GREATER_THAN = 'GREATER_THAN',
  LESS_THAN = 'LESS_THAN',
  ELEMENT_EXISTS = 'ELEMENT_EXISTS',
  ELEMENT_VISIBLE = 'ELEMENT_VISIBLE',
  ATTRIBUTE_EQUALS = 'ATTRIBUTE_EQUALS',
  PROPERTY_EQUALS = 'PROPERTY_EQUALS',
  CUSTOM = 'CUSTOM'
}

/**
 * Assertion configuration
 */
export interface AssertionConfig {
  id: string;
  type: AssertionType;
  elementSelector?: Locator; // Element to assert on
  property?: string; // Element property (e.g., 'textContent', 'value')
  attributeName?: string; // Element attribute name
  expectedValue?: string; // Expected value
  actualValueExpression?: string; // Expression to get actual value
  customAssertion?: string; // Custom assertion code
  failureMessage?: string; // Custom failure message
  softAssertion?: boolean; // Whether this is a soft assertion (test continues on failure)
}

/**
 * Step group configuration for organizing steps
 */
export interface StepGroup {
  id: string;
  name: string;
  description?: string;
  type: 'group' | 'try_catch'; // Regular group or try-catch block
  eventIds: string[]; // Events in the group
  catchEventIds?: string[]; // Events to execute if error occurs (for try-catch)
  finallyEventIds?: string[]; // Events to always execute (for try-catch)
  collapsed?: boolean; // UI state for collapsed/expanded view
} 