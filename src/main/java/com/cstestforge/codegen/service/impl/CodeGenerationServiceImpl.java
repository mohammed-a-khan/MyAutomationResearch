package com.cstestforge.codegen.service.impl;

import com.cstestforge.codegen.model.*;
import com.cstestforge.codegen.model.event.*;
import com.cstestforge.codegen.service.CodeGenerationService;
import com.cstestforge.codegen.service.generator.CodeGenerator;
import com.cstestforge.codegen.service.generator.SeleniumJavaGenerator;
import com.cstestforge.codegen.service.generator.PlaywrightTypeScriptGenerator;
import com.cstestforge.codegen.service.template.TemplateEngine;
import com.cstestforge.project.service.ProjectService;
import com.cstestforge.project.storage.FileStorageService;
import com.cstestforge.storage.repository.TestRepository;
import com.cstestforge.project.repository.ProjectRepository;
import com.cstestforge.recorder.repository.RecordingRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;

/**
 * Implementation of the CodeGenerationService
 */
@Service
public class CodeGenerationServiceImpl implements CodeGenerationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeGenerationServiceImpl.class);
    private static final String GENERATIONS_PATH = "codegen/generations";
    
    private final Map<String, CodeGenerator> generators = new HashMap<>();
    private final ProjectService projectService;
    private final FileStorageService storageService;
    private final TemplateEngine templateEngine;
    private final ObjectMapper objectMapper;
    private final TestRepository testRepository;
    private final ProjectRepository projectRepository;
    private final RecordingRepository recordingRepository;
    
    // Constants for storage paths
    private static final String TEMPLATES_PATH = "code-builder/templates";
    private static final String CUSTOMIZATIONS_PATH = "code-builder/customizations";
    
    @Autowired
    public CodeGenerationServiceImpl(
            ProjectService projectService, 
            FileStorageService storageService,
            TemplateEngine templateEngine,
            TestRepository testRepository,
            ProjectRepository projectRepository,
            RecordingRepository recordingRepository) {
        this.projectService = projectService;
        this.storageService = storageService;
        this.templateEngine = templateEngine;
        this.objectMapper = new ObjectMapper();
        this.testRepository = testRepository;
        this.projectRepository = projectRepository;
        this.recordingRepository = recordingRepository;
        
        // Register code generators
        generators.put("selenium-java", new SeleniumJavaGenerator(templateEngine));
        generators.put("playwright-typescript", new PlaywrightTypeScriptGenerator(templateEngine));
        
        // Ensure storage directory exists
        storageService.createDirectory(GENERATIONS_PATH);
    }

    @Override
    public GeneratedCode generateFromTest(String testId, String framework, String language) {
        // Fetch test from project service
        List<Event> events = fetchEventsForTest(testId);
        
        String projectId = getProjectIdFromTestId(testId);
        
        // Get customization for the project
        CodeGenerationConfig config = getCustomization(projectId);
        
        // Create GeneratedCode instance
        GeneratedCode generatedCode = generateFromSteps(events, framework, language, config);
        generatedCode.setSourceId(testId);
        generatedCode.setProjectId(projectId);
        
        return generatedCode;
    }

    @Override
    public GeneratedCode generateFromRecording(String recordingId, String framework, String language) {
        // Fetch recording from project service
        List<Event> events = fetchEventsForRecording(recordingId);
        
        String projectId = getProjectIdFromRecordingId(recordingId);
        
        // Get customization for the project
        CodeGenerationConfig config = getCustomization(projectId);
        
        // Create GeneratedCode instance
        GeneratedCode generatedCode = generateFromSteps(events, framework, language, config);
        generatedCode.setSourceId(recordingId);
        generatedCode.setProjectId(projectId);
        
        return generatedCode;
    }

    /**
     * Generate code from a list of events using project configuration
     * 
     * @param events List of events to generate code from
     * @param framework Framework to use for code generation
     * @param language Language to use for code generation
     * @param config Configuration for code generation
     * @return Generated code
     */
    public GeneratedCode generateFromSteps(List<Event> events, String framework, String language, CodeGenerationConfig config) {
        // Get the appropriate generator
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        
        // Configure generators based on configuration
        configureGenerator(generator, config);
        
        // Generate code
        String code = generator.generateCode(events);
        
        // Also generate page object if needed
        String pageObjectCode = null;
        if (config.isGeneratePageObjects() && !events.isEmpty()) {
            Map<String, String> elements = extractElementsFromEvents(events);
            if (!elements.isEmpty()) {
                if (generator instanceof SeleniumJavaGenerator) {
                    pageObjectCode = ((SeleniumJavaGenerator) generator).generatePageObject(
                            config.getPageObjectName(), elements);
                } else if (generator instanceof PlaywrightTypeScriptGenerator) {
                    pageObjectCode = ((PlaywrightTypeScriptGenerator) generator).generatePageObject(
                            config.getPageObjectName(), elements);
                }
            }
        }
        
        // Create and store the generated code
        GeneratedCode generatedCode = new GeneratedCode();
        generatedCode.setCode(code);
        generatedCode.setPageObjectCode(pageObjectCode);
        generatedCode.setFramework(framework);
        generatedCode.setLanguage(language);
        generatedCode.setGeneratedAt(LocalDateTime.now());
        generatedCode.addMetadata("style", config.getTestStyle());
        
        // Save the generated code
        saveGeneratedCode(generatedCode);
        
        return generatedCode;
    }
    
    /**
     * Configure a code generator based on configuration
     * 
     * @param generator Generator to configure
     * @param config Configuration for code generation
     */
    private void configureGenerator(CodeGenerator generator, CodeGenerationConfig config) {
        if (generator instanceof SeleniumJavaGenerator) {
            SeleniumJavaGenerator seleniumGenerator = (SeleniumJavaGenerator) generator;
            
            // Set test style
            seleniumGenerator.setGenerationStyle(
                    "BDD".equalsIgnoreCase(config.getTestStyle()) ? 
                    SeleniumJavaGenerator.GenerationStyle.BDD : 
                    SeleniumJavaGenerator.GenerationStyle.TESTNG);
            
            // Set test name
            if (config.getTestName() != null && !config.getTestName().isEmpty()) {
                seleniumGenerator.setTestName(config.getTestName());
            }
            
            // Set package name
            if (config.getPackageName() != null && !config.getPackageName().isEmpty()) {
                seleniumGenerator.setPackageName(config.getPackageName());
            }
        } else if (generator instanceof PlaywrightTypeScriptGenerator) {
            PlaywrightTypeScriptGenerator playwrightGenerator = (PlaywrightTypeScriptGenerator) generator;
            
            // Set test style
            playwrightGenerator.setGenerationStyle(
                    "BDD".equalsIgnoreCase(config.getTestStyle()) ? 
                    PlaywrightTypeScriptGenerator.GenerationStyle.BDD : 
                    PlaywrightTypeScriptGenerator.GenerationStyle.STANDARD);
            
            // Set test name
            if (config.getTestName() != null && !config.getTestName().isEmpty()) {
                playwrightGenerator.setTestName(config.getTestName());
            }
        }
    }
    
    /**
     * Extract elements from events for page object generation
     * 
     * @param events List of events
     * @return Map of element names to selectors
     */
    private Map<String, String> extractElementsFromEvents(List<Event> events) {
        Map<String, String> elements = new HashMap<>();
        
        for (Event event : events) {
            if (event instanceof ActionEvent) {
                ActionEvent actionEvent = (ActionEvent) event;
                if (actionEvent.getTargetSelector() != null && !actionEvent.getTargetSelector().isEmpty()) {
                    String selector = actionEvent.getTargetSelector();
                    String name = generateElementName(actionEvent, selector);
                    elements.put(name, selector);
                }
            } else if (event instanceof AssertionEvent) {
                AssertionEvent assertionEvent = (AssertionEvent) event;
                if (assertionEvent.getAssertType() == AssertionEvent.AssertType.ELEMENT_PRESENT ||
                    assertionEvent.getAssertType() == AssertionEvent.AssertType.ELEMENT_NOT_PRESENT) {
                    String selector = assertionEvent.getActualExpression();
                    if (selector != null && !selector.isEmpty()) {
                        String name = generateElementNameFromAssertion(assertionEvent, selector);
                        elements.put(name, selector);
                    }
                }
            } else if (event instanceof GroupEvent) {
                // Recursively process group events
                elements.putAll(extractElementsFromEvents(((GroupEvent) event).getEvents()));
            } else if (event instanceof ConditionalEvent) {
                // Recursively process conditional events
                elements.putAll(extractElementsFromEvents(((ConditionalEvent) event).getThenEvents()));
                if (((ConditionalEvent) event).isHasElse()) {
                    elements.putAll(extractElementsFromEvents(((ConditionalEvent) event).getElseEvents()));
                }
            } else if (event instanceof LoopEvent) {
                // Recursively process loop events
                elements.putAll(extractElementsFromEvents(((LoopEvent) event).getLoopEvents()));
            } else if (event instanceof TryCatchEvent) {
                // Recursively process try-catch events
                elements.putAll(extractElementsFromEvents(((TryCatchEvent) event).getTryEvents()));
                for (List<Event> catchEvents : ((TryCatchEvent) event).getCatchBlocks().values()) {
                    elements.putAll(extractElementsFromEvents(catchEvents));
                }
                if (((TryCatchEvent) event).isHasFinally()) {
                    elements.putAll(extractElementsFromEvents(((TryCatchEvent) event).getFinallyEvents()));
                }
            }
        }
        
        return elements;
    }
    
    /**
     * Generate element name from action event
     * 
     * @param actionEvent Action event
     * @param selector CSS selector
     * @return Element name
     */
    private String generateElementName(ActionEvent actionEvent, String selector) {
        String name = null;
        
        // Try to determine a meaningful name based on action type and selector
        switch (actionEvent.getActionType()) {
            case CLICK:
                if (selector.toLowerCase().contains("button")) {
                    name = extractLastPart(selector) + "Button";
                } else if (selector.toLowerCase().contains("link")) {
                    name = extractLastPart(selector) + "Link";
                } else {
                    name = extractLastPart(selector) + "Element";
                }
                break;
                
            case TYPE:
                if (selector.toLowerCase().contains("input") || 
                    selector.toLowerCase().contains("field") ||
                    selector.toLowerCase().contains("text")) {
                    name = extractLastPart(selector) + "Input";
                } else {
                    name = extractLastPart(selector) + "Field";
                }
                break;
                
            case SELECT:
                name = extractLastPart(selector) + "Dropdown";
                break;
                
            default:
                name = extractLastPart(selector) + "Element";
        }
        
        return sanitizeElementName(name);
    }
    
    /**
     * Generate element name from assertion event
     * 
     * @param assertionEvent Assertion event
     * @param selector CSS selector
     * @return Element name
     */
    private String generateElementNameFromAssertion(AssertionEvent assertionEvent, String selector) {
        String name = extractLastPart(selector) + "Element";
        return sanitizeElementName(name);
    }
    
    /**
     * Extract the last part of a CSS selector
     * 
     * @param selector CSS selector
     * @return Last part of selector
     */
    private String extractLastPart(String selector) {
        String[] parts = selector.split(" ");
        String lastPart = parts[parts.length - 1];
        
        // Extract id or class if present
        if (lastPart.startsWith("#")) {
            return lastPart.substring(1);
        } else if (lastPart.startsWith(".")) {
            return lastPart.substring(1);
        } else if (lastPart.contains("#")) {
            return lastPart.substring(lastPart.indexOf('#') + 1);
        } else if (lastPart.contains(".")) {
            return lastPart.substring(lastPart.indexOf('.') + 1);
        }
        
        // Otherwise just return the element type
        return lastPart;
    }
    
    /**
     * Sanitize element name to be a valid Java/TypeScript identifier
     * 
     * @param name Element name
     * @return Sanitized element name
     */
    private String sanitizeElementName(String name) {
        // Remove non-alphanumeric characters
        name = name.replaceAll("[^a-zA-Z0-9]", "");
        
        // Ensure first character is lowercase
        if (!name.isEmpty()) {
            name = Character.toLowerCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
        }
        
        return name;
    }

    @Override
    public GeneratedCode generateFromSteps(List<Event> events, String framework, String language) {
        // Get default configuration
        CodeGenerationConfig config = new CodeGenerationConfig(null);
        return generateFromSteps(events, framework, language, config);
    }

    @Override
    public List<CodeTemplate> getTemplates(String framework, String language) {
        List<CodeTemplate> templates = new ArrayList<>();
        
        try {
            // Define the template directory path based on framework and language
            String templatesDir = TEMPLATES_PATH;
            if (framework != null && language != null) {
                templatesDir += "/" + framework + "/" + language;
            } else if (framework != null) {
                templatesDir += "/" + framework;
            }
            
            // Check if directory exists
            if (storageService.fileExists(templatesDir)) {
                // List all template files
                List<String> templateFiles = storageService.listFiles(templatesDir, path -> path.endsWith(".template"));
                
                // Load each template
                for (String templateFile : templateFiles) {
                    try {
                        CodeTemplate template = storageService.readFromJson(templateFile, CodeTemplate.class);
                        if (template != null) {
                            templates.add(template);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to load template from file {}: {}", templateFile, e.getMessage());
                    }
                }
            } else {
                logger.info("Template directory not found: {}", templatesDir);
            }
        } catch (Exception e) {
            logger.error("Error loading templates for framework={}, language={}: {}", 
                    framework, language, e.getMessage(), e);
        }
        
        return templates;
    }

    @Override
    public CodeTemplate getTemplateById(String id) {
        // Try to find the template in the index file first
        String indexPath = TEMPLATES_PATH + "/_index.json";
        try {
            if (storageService.fileExists(indexPath)) {
                Map<String, String> templateIndex = storageService.readFromJson(indexPath, Map.class);
                
                // If found in index, get the path and load it
                if (templateIndex.containsKey(id)) {
                    String templatePath = templateIndex.get(id);
                    return storageService.readFromJson(templatePath, CodeTemplate.class);
                }
            }
        } catch (Exception e) {
            // Fall back to searching all templates
        }
        
        // Search all templates if not found in index
        for (CodeTemplate template : getTemplates(null, null)) {
            if (id.equals(template.getId())) {
                return template;
            }
        }
        
        return null;
    }

    @Override
    public List<FrameworkDefinition> getSupportedFrameworks() {
        List<FrameworkDefinition> frameworks = new ArrayList<>();
        
        // Create Selenium Java framework definition
        FrameworkDefinition seleniumJava = new FrameworkDefinition(
                "selenium-java", 
                "Selenium Java", 
                "java", 
                "4.10.0");
        seleniumJava.setBasePackage("com.cstestforge.framework.selenium");
        seleniumJava.addSupportedTestType("UI");
        seleniumJava.addSupportedTestType("functional");
        frameworks.add(seleniumJava);
        
        // Create Playwright TypeScript framework definition
        FrameworkDefinition playwrightTs = new FrameworkDefinition(
                "playwright-typescript",
                "Playwright TypeScript",
                "typescript",
                "1.38.0");
        playwrightTs.setBasePackage("com.cstestforge.typescript.cstestforge-playwright");
        playwrightTs.addSupportedTestType("UI");
        playwrightTs.addSupportedTestType("functional");
        frameworks.add(playwrightTs);
        
        return frameworks;
    }

    @Override
    public boolean validateCode(String code, String framework, String language) {
        if (code == null || code.trim().isEmpty()) {
            logger.error("Cannot validate empty code");
            return false;
        }
        
        logger.debug("Validating {} code for {} framework", language, framework);
        boolean isValid = false;
        
        try {
            if ("selenium-java".equals(framework)) {
                // Use Java compiler API to validate the code syntax
                isValid = validateJavaCode(code);
            } else if ("playwright-typescript".equals(framework)) {
                // Use TypeScript parser to validate the code syntax
                isValid = validateTypeScriptCode(code);
            } else {
                // For unknown frameworks, just check for basic syntax
                isValid = validateBasicSyntax(code, language);
            }
        } catch (Exception e) {
            logger.error("Error validating code: {}", e.getMessage(), e);
            return false;
        }
        
        if (isValid) {
            logger.debug("Code validation successful");
        } else {
            logger.warn("Code validation failed");
        }
        
        return isValid;
    }
    
    /**
     * Validate Java code syntax
     */
    private boolean validateJavaCode(String code) {
        // Basic checks for Java syntax
        if (!code.contains("public class") && !code.contains("public interface")) {
            logger.warn("Java code is missing class or interface declaration");
            return false;
        }
        
        if (!code.contains("import")) {
            logger.warn("Java code is missing imports");
            return false;
        }
        
        // Check for basic Java syntax errors
        int openBraces = countOccurrences(code, '{');
        int closeBraces = countOccurrences(code, '}');
        if (openBraces != closeBraces) {
            logger.warn("Java code has unbalanced braces: {} open, {} closed", openBraces, closeBraces);
            return false;
        }
        
        int openParens = countOccurrences(code, '(');
        int closeParens = countOccurrences(code, ')');
        if (openParens != closeParens) {
            logger.warn("Java code has unbalanced parentheses: {} open, {} closed", openParens, closeParens);
            return false;
        }
        
        // Check for Selenium specific imports and patterns
        if (!code.contains("import org.openqa.selenium")) {
            logger.warn("Java code is missing Selenium imports");
            return false;
        }
        
        // Check for WebDriver usage
        if (!code.contains("WebDriver") && !code.contains("driver.")) {
            logger.warn("Java code doesn't appear to use WebDriver");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate TypeScript code syntax
     */
    private boolean validateTypeScriptCode(String code) {
        // Basic checks for TypeScript syntax
        if (!code.contains("import")) {
            logger.warn("TypeScript code is missing imports");
            return false;
        }
        
        // Check for Playwright imports
        if (!code.contains("import { test, expect }") && !code.contains("import {test,expect}")) {
            logger.warn("TypeScript code is missing Playwright test imports");
            return false;
        }
        
        // Check for basic syntax errors
        int openBraces = countOccurrences(code, '{');
        int closeBraces = countOccurrences(code, '}');
        if (openBraces != closeBraces) {
            logger.warn("TypeScript code has unbalanced braces: {} open, {} closed", openBraces, closeBraces);
            return false;
        }
        
        int openParens = countOccurrences(code, '(');
        int closeParens = countOccurrences(code, ')');
        if (openParens != closeParens) {
            logger.warn("TypeScript code has unbalanced parentheses: {} open, {} closed", openParens, closeParens);
            return false;
        }
        
        // Check for async/await pattern
        if (!code.contains("async") || !code.contains("await")) {
            logger.warn("TypeScript code may be missing async/await pattern for Playwright");
            return false;
        }
        
        return true;
    }
    
    /**
     * Validate basic syntax for any code
     */
    private boolean validateBasicSyntax(String code, String language) {
        // Check for balanced braces, brackets, and parentheses
        int openBraces = countOccurrences(code, '{');
        int closeBraces = countOccurrences(code, '}');
        int openBrackets = countOccurrences(code, '[');
        int closeBrackets = countOccurrences(code, ']');
        int openParens = countOccurrences(code, '(');
        int closeParens = countOccurrences(code, ')');
        
        boolean isValid = true;
        
        if (openBraces != closeBraces) {
            logger.warn("Code has unbalanced braces: {} open, {} closed", openBraces, closeBraces);
            isValid = false;
        }
        
        if (openBrackets != closeBrackets) {
            logger.warn("Code has unbalanced brackets: {} open, {} closed", openBrackets, closeBrackets);
            isValid = false;
        }
        
        if (openParens != closeParens) {
            logger.warn("Code has unbalanced parentheses: {} open, {} closed", openParens, closeParens);
            isValid = false;
        }
        
        // Check for string literals
        int quotes = countOccurrences(code, '"');
        int singleQuotes = countOccurrences(code, '\'');
        
        if (quotes % 2 != 0) {
            logger.warn("Code has unbalanced double quotes");
            isValid = false;
        }
        
        if (singleQuotes % 2 != 0) {
            logger.warn("Code has unbalanced single quotes");
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Count occurrences of a character in a string
     */
    private int countOccurrences(String str, char c) {
        return (int) str.chars().filter(ch -> ch == c).count();
    }

    @Override
    public String generateCodeForConditional(ConditionalEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateConditional(event, 0);
    }

    @Override
    public String generateCodeForLoop(LoopEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateLoop(event, 0);
    }

    @Override
    public String generateCodeForCapture(CaptureEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateCapture(event, 0);
    }

    @Override
    public String generateCodeForAssertion(AssertionEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateAssertion(event, 0);
    }

    @Override
    public String generateCodeForGroup(GroupEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateGroup(event, 0);
    }

    @Override
    public String generateCodeForTryCatch(TryCatchEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateTryCatch(event, 0);
    }
    
    @Override
    public String generateCodeForAction(ActionEvent event, String language, String framework) {
        CodeGenerator generator = getGenerator(framework, language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported framework or language: " + framework + ", " + language);
        }
        return generator.generateAction(event, 0);
    }

    @Override
    public CodeGenerationConfig saveCustomization(String projectId, CodeGenerationConfig config) {
        // Ensure the config has the project ID set
        config.setProjectId(projectId);
        
        // Save the config to storage
        String configPath = CUSTOMIZATIONS_PATH + "/" + projectId + ".json";
        try {
            storageService.saveToJson(configPath, config);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save customization: " + e.getMessage(), e);
        }
        
        return config;
    }

    @Override
    public CodeGenerationConfig getCustomization(String projectId) {
        String configPath = CUSTOMIZATIONS_PATH + "/" + projectId + ".json";
        
        try {
            if (storageService.fileExists(configPath)) {
                return storageService.readFromJson(configPath, CodeGenerationConfig.class);
            } else {
                // Return default config if none exists
                return new CodeGenerationConfig(projectId);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load customization: " + e.getMessage(), e);
        }
    }

    @Override
    public GeneratedCode regenerateWithCustomization(String codeId, CodeGenerationConfig config) {
        logger.info("Regenerating code {} with customization", codeId);
        
        try {
            // Load the original generated code
            GeneratedCode originalCode = loadGeneratedCode(codeId);
            if (originalCode == null) {
                logger.error("Generated code not found: {}", codeId);
                return null;
            }
            
            // Store the original code as a backup
            String backupId = originalCode.getId() + "_backup_" + System.currentTimeMillis();
            GeneratedCode backupCode = new GeneratedCode();
            backupCode.setCode(originalCode.getCode());
            backupCode.setPageObjectCode(originalCode.getPageObjectCode());
            backupCode.setFramework(originalCode.getFramework());
            backupCode.setLanguage(originalCode.getLanguage());
            backupCode.setGeneratedAt(originalCode.getGeneratedAt());
            backupCode.setMetadata(new HashMap<>(originalCode.getMetadata()));
            backupCode.setId(backupId);
            backupCode.setSourceId(originalCode.getSourceId());
            backupCode.setProjectId(originalCode.getProjectId());
            
            // Save the backup
            saveGeneratedCode(backupCode);
            
            // Get the events based on the source of the original code
            List<Event> events;
            String sourceId = originalCode.getSourceId();
            if (sourceId != null) {
                if (sourceId.startsWith("test_")) {
                    // Source is a test
                    events = fetchEventsForTest(sourceId);
                } else if (sourceId.startsWith("rec_")) {
                    // Source is a recording
                    events = fetchEventsForRecording(sourceId);
                } else {
                    // Try to parse the source code back to events
                    events = parseCodeToEvents(originalCode.getCode(), originalCode.getFramework(), originalCode.getLanguage());
                }
            } else {
                // Try to parse the source code back to events
                events = parseCodeToEvents(originalCode.getCode(), originalCode.getFramework(), originalCode.getLanguage());
            }
            
            if (events == null || events.isEmpty()) {
                logger.error("Failed to get events for regeneration");
                return originalCode;
            }
            
            // Apply customization and regenerate code
            String framework = originalCode.getFramework();
            String language = originalCode.getLanguage();
            
            GeneratedCode newCode = generateFromSteps(events, framework, language, config);
            
            // Copy key properties from original code
            newCode.setId(originalCode.getId());
            newCode.setSourceId(originalCode.getSourceId());
            newCode.setProjectId(originalCode.getProjectId());
            newCode.addMetadata("customized", true);
            newCode.addMetadata("customizedAt", LocalDateTime.now().toString());
            newCode.addMetadata("originalGeneratedAt", originalCode.getGeneratedAt().toString());
            
            // Save the regenerated code
            saveGeneratedCode(newCode);
            
            return newCode;
        } catch (Exception e) {
            logger.error("Error regenerating code: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Parse generated code back into a list of events
     * This is useful for customizing code that doesn't have a direct source
     */
    private List<Event> parseCodeToEvents(String code, String framework, String language) {
        // This is a best-effort implementation that attempts to parse code back into events
        List<Event> events = new ArrayList<>();
        
        try {
            if ("selenium-java".equals(framework)) {
                events = parseJavaCodeToEvents(code);
            } else if ("playwright-typescript".equals(framework)) {
                events = parseTypeScriptCodeToEvents(code);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse code to events: {}", e.getMessage(), e);
        }
        
        return events;
    }
    
    /**
     * Parse Java code into events
     */
    private List<Event> parseJavaCodeToEvents(String code) {
        List<Event> events = new ArrayList<>();
        
        // Split code into lines
        String[] lines = code.split("\\r?\\n");
        
        // Look for specific patterns in the code
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Navigation events
            if (line.contains("driver.get(") || line.contains("driver.navigate().to(")) {
                NavigationEvent event = new NavigationEvent();
                
                // Extract URL from the line
                int startIndex = line.indexOf("\"");
                int endIndex = line.indexOf("\"", startIndex + 1);
                if (startIndex >= 0 && endIndex > startIndex) {
                    String url = line.substring(startIndex + 1, endIndex);
                    event.setUrl(url);
                    events.add(event);
                }
            } 
            // Click events
            else if (line.contains(".click()")) {
                ClickEvent event = new ClickEvent();
                
                // Try to extract selector from previous lines
                for (int j = i - 1; j >= 0 && j >= i - 3; j--) {
                    String prevLine = lines[j].trim();
                    if (prevLine.contains("findElement(By.")) {
                        int startIndex = prevLine.indexOf("By.");
                        int endIndex = prevLine.indexOf(")", startIndex);
                        if (startIndex >= 0 && endIndex > startIndex) {
                            String selector = prevLine.substring(startIndex, endIndex + 1);
                            event.addMetadata("selector", selector);
                            break;
                        }
                    }
                }
                
                events.add(event);
            } 
            // Input events
            else if (line.contains(".sendKeys(")) {
                InputEvent event = new InputEvent();
                
                // Extract value from the line
                int startIndex = line.indexOf("\"");
                int endIndex = line.indexOf("\"", startIndex + 1);
                if (startIndex >= 0 && endIndex > startIndex) {
                    String value = line.substring(startIndex + 1, endIndex);
                    event.setValue(value);
                }
                
                // Try to extract selector from previous lines
                for (int j = i - 1; j >= 0 && j >= i - 3; j--) {
                    String prevLine = lines[j].trim();
                    if (prevLine.contains("findElement(By.")) {
                        int byStartIndex = prevLine.indexOf("By.");
                        int byEndIndex = prevLine.indexOf(")", byStartIndex);
                        if (byStartIndex >= 0 && byEndIndex > byStartIndex) {
                            String selector = prevLine.substring(byStartIndex, byEndIndex + 1);
                            event.addMetadata("selector", selector);
                            break;
                        }
                    }
                }
                
                events.add(event);
            } 
            // Wait events
            else if (line.contains("Thread.sleep(") || line.contains(".implicitlyWait(") || line.contains("WebDriverWait")) {
                WaitEvent event = new WaitEvent();
                
                // Try to extract timeout
                if (line.contains("Thread.sleep(")) {
                    int startIndex = line.indexOf("sleep(") + 6;
                    int endIndex = line.indexOf(")", startIndex);
                    if (startIndex >= 0 && endIndex > startIndex) {
                        try {
                            String timeoutStr = line.substring(startIndex, endIndex).trim();
                            // Convert ms to seconds if needed
                            if (timeoutStr.endsWith("000")) {
                                timeoutStr = timeoutStr.substring(0, timeoutStr.length() - 3);
                            }
                            int timeout = Integer.parseInt(timeoutStr);
                            event.setTimeout(timeout);
                        } catch (NumberFormatException e) {
                            event.setTimeout(1000); // Default timeout
                        }
                    }
                }
                
                events.add(event);
            }
            // Assertion events
            else if (line.contains("assertEquals") || line.contains("assertTrue") || line.contains("assertFalse") || 
                    line.contains("assertNull") || line.contains("assertNotNull") || line.contains("expect(")) {
                AssertionEvent event = new AssertionEvent();
                
                if (line.contains("assertEquals")) {
                    event.setAssertType(AssertionEvent.AssertType.EQUALS);
                } else if (line.contains("assertTrue")) {
                    event.setAssertType(AssertionEvent.AssertType.TRUE);
                } else if (line.contains("assertFalse")) {
                    event.setAssertType(AssertionEvent.AssertType.FALSE);
                } else if (line.contains("assertNull")) {
                    event.setAssertType(AssertionEvent.AssertType.NULL);
                } else if (line.contains("assertNotNull")) {
                    event.setAssertType(AssertionEvent.AssertType.NOT_NULL);
                } else {
                    event.setAssertType(AssertionEvent.AssertType.CUSTOM);
                }
                
                events.add(event);
            }
        }
        
        return events;
    }
    
    /**
     * Parse TypeScript code into events
     */
    private List<Event> parseTypeScriptCodeToEvents(String code) {
        List<Event> events = new ArrayList<>();
        
        // Split code into lines
        String[] lines = code.split("\\r?\\n");
        
        // Look for specific patterns in the code
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Navigation events
            if (line.contains(".goto(")) {
                NavigationEvent event = new NavigationEvent();
                
                // Extract URL from the line
                int startIndex = line.indexOf("'");
                if (startIndex < 0) startIndex = line.indexOf("\"");
                int endIndex = -1;
                if (startIndex >= 0) {
                    char quoteChar = line.charAt(startIndex);
                    endIndex = line.indexOf(quoteChar, startIndex + 1);
                }
                
                if (startIndex >= 0 && endIndex > startIndex) {
                    String url = line.substring(startIndex + 1, endIndex);
                    event.setUrl(url);
                    events.add(event);
                }
            } 
            // Click events
            else if (line.contains(".click(") || line.contains(".click ")) {
                ClickEvent event = new ClickEvent();
                
                // Extract selector if present on the same line
                if (line.contains("locator(")) {
                    int startIndex = line.indexOf("locator(") + 8;
                    int endIndex = line.indexOf(")", startIndex);
                    if (startIndex >= 0 && endIndex > startIndex) {
                        String selector = line.substring(startIndex, endIndex);
                        selector = selector.replace("'", "").replace("\"", "");
                        event.addMetadata("selector", selector);
                    }
                }
                
                events.add(event);
            } 
            // Input events
            else if (line.contains(".fill(") || line.contains(".type(")) {
                InputEvent event = new InputEvent();
                
                // Extract value from the line
                int commaIndex = line.indexOf(",");
                if (commaIndex > 0) {
                    int startIndex = line.indexOf("'", commaIndex);
                    if (startIndex < 0) startIndex = line.indexOf("\"", commaIndex);
                    int endIndex = -1;
                    if (startIndex >= 0) {
                        char quoteChar = line.charAt(startIndex);
                        endIndex = line.indexOf(quoteChar, startIndex + 1);
                    }
                    
                    if (startIndex >= 0 && endIndex > startIndex) {
                        String value = line.substring(startIndex + 1, endIndex);
                        event.setValue(value);
                    }
                }
                
                events.add(event);
            } 
            // Wait events
            else if (line.contains(".waitFor") || line.contains("await page.waitFor")) {
                WaitEvent event = new WaitEvent();
                events.add(event);
            }
            // Assertion events
            else if (line.contains("expect(") && (line.contains(".toBe") || line.contains(".toContain") || 
                    line.contains(".toEqual") || line.contains(".toHave"))) {
                AssertionEvent event = new AssertionEvent();
                
                if (line.contains(".toBe(")) {
                    event.setAssertType(AssertionEvent.AssertType.EQUALS);
                } else if (line.contains(".toContain(")) {
                    event.setAssertType(AssertionEvent.AssertType.CONTAINS);
                } else if (line.contains(".toEqual(")) {
                    event.setAssertType(AssertionEvent.AssertType.EQUALS);
                } else {
                    event.setAssertType(AssertionEvent.AssertType.CUSTOM);
                }
                
                events.add(event);
            }
        }
        
        return events;
    }

    @Override
    public String exportFramework(ExportRequest request, String destinationPath) {
        logger.info("Exporting framework to {}: {}", destinationPath, request);
        
        try {
            // Create the destination directory
            Path exportPath = Paths.get(destinationPath, request.getExportName());
            Files.createDirectories(exportPath);
            
            // Copy framework template files
            String frameworkTemplatePath = "templates/" + request.getFramework();
            if (!storageService.fileExists(frameworkTemplatePath)) {
                logger.error("Framework template not found: {}", frameworkTemplatePath);
                throw new IllegalArgumentException("Framework template not found: " + request.getFramework());
            }
            
            // Copy framework base files
            copyFrameworkFiles(frameworkTemplatePath, exportPath.toString());
            
            // Generate framework configuration 
            generateFrameworkConfig(request, exportPath.toString());
            
            // Export the generated tests
            exportTests(request, exportPath.toString());
            
            // Generate project build files (pom.xml, package.json, etc.)
            generateBuildFiles(request, exportPath.toString());
            
            logger.info("Framework exported successfully to {}", exportPath);
            return exportPath.toString();
        } catch (IOException e) {
            logger.error("Failed to export framework: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export framework: " + e.getMessage(), e);
        }
    }
    
    /**
     * Copy framework template files to export directory
     */
    private void copyFrameworkFiles(String sourcePath, String destinationPath) throws IOException {
        logger.debug("Copying framework files from {} to {}", sourcePath, destinationPath);
        
        // List all files in the framework template directory
        List<String> templateFiles = storageService.listFiles(sourcePath, name -> true);
        for (String fileName : templateFiles) {
            String sourceFilePath = sourcePath + "/" + fileName;
            String destFilePath = destinationPath + "/" + fileName;
            
            // Handle directories recursively
            if (fileName.contains(".")) {
                // It's a file, read and write it
                Path dest = Paths.get(destFilePath);
                Files.createDirectories(dest.getParent());
                
                // Read file content from storage service and write to destination
                byte[] content = Files.readAllBytes(Paths.get(storageService.getAbsolutePath(sourceFilePath)));
                Files.write(dest, content);
            } else {
                // Assume it's a directory and create it
                Files.createDirectories(Paths.get(destFilePath));
                
                // Recursively copy contents
                copyFrameworkFiles(sourceFilePath, destFilePath);
            }
        }
    }
    
    /**
     * Generate framework configuration files
     */
    private void generateFrameworkConfig(ExportRequest request, String destinationPath) throws IOException {
        logger.debug("Generating framework configuration for {}", request.getFramework());
        
        // Handle different frameworks
        if ("selenium-java".equals(request.getFramework())) {
            // Generate selenium configuration
            generateSeleniumConfig(request, destinationPath);
        } else if ("playwright-typescript".equals(request.getFramework())) {
            // Generate playwright configuration
            generatePlaywrightConfig(request, destinationPath);
        }
    }
    
    /**
     * Generate Selenium Java configuration
     */
    private void generateSeleniumConfig(ExportRequest request, String destinationPath) throws IOException {
        Path configPath = Paths.get(destinationPath, "src", "test", "resources", "selenium-config.properties");
        Files.createDirectories(configPath.getParent());
        
        List<String> configLines = new ArrayList<>();
        configLines.add("# Selenium Configuration");
        configLines.add("browser=" + request.getConfig().getOrDefault("browser", "chrome"));
        configLines.add("baseUrl=" + request.getConfig().getOrDefault("baseUrl", "http://localhost:3000"));
        configLines.add("implicitWait=" + request.getConfig().getOrDefault("implicitWait", "10"));
        configLines.add("screenshotPath=target/screenshots");
        configLines.add("reportPath=target/reports");
        
        Files.write(configPath, configLines, StandardCharsets.UTF_8);
    }
    
    /**
     * Generate Playwright TypeScript configuration
     */
    private void generatePlaywrightConfig(ExportRequest request, String destinationPath) throws IOException {
        Path configPath = Paths.get(destinationPath, "playwright.config.ts");
        
        String browserType = request.getConfig().getOrDefault("browser", "chromium").toString();
        String baseUrl = request.getConfig().getOrDefault("baseUrl", "http://localhost:3000").toString();
        
        List<String> configLines = new ArrayList<>();
        configLines.add("import { defineConfig, devices } from '@playwright/test';");
        configLines.add("export default defineConfig({");
        configLines.add("  testDir: './tests',");
        configLines.add("  fullyParallel: true,");
        configLines.add("  forbidOnly: !!process.env.CI,");
        configLines.add("  retries: process.env.CI ? 2 : 0,");
        configLines.add("  reporter: 'html',");
        configLines.add("  use: {");
        configLines.add("    baseURL: '" + baseUrl + "',");
        configLines.add("    trace: 'on-first-retry',");
        configLines.add("  },");
        configLines.add("  projects: [");
        configLines.add("    {");
        configLines.add("      name: '" + browserType + "',");
        configLines.add("      use: { ...devices['" + getBrowserConfig(browserType) + "'] },");
        configLines.add("    },");
        configLines.add("  ],");
        configLines.add("});");
        
        Files.write(configPath, configLines, StandardCharsets.UTF_8);
    }
    
    /**
     * Get Playwright browser configuration
     */
    private String getBrowserConfig(String browserType) {
        switch (browserType.toLowerCase()) {
            case "chrome":
            case "chromium":
                return "Desktop Chrome";
            case "firefox":
                return "Desktop Firefox";
            case "safari":
                return "Desktop Safari";
            case "edge":
                return "Desktop Edge";
            case "mobile-chrome":
                return "Pixel 5";
            case "mobile-safari":
                return "iPhone 13";
            default:
                return "Desktop Chrome";
        }
    }
    
    /**
     * Export generated tests to framework export directory
     */
    private void exportTests(ExportRequest request, String destinationPath) throws IOException {
        logger.debug("Exporting tests for framework: {}", request.getFramework());
        
        // Export each test
        for (String testId : request.getTestIds()) {
            try {
                GeneratedCode generatedCode = loadGeneratedCode(testId);
                if (generatedCode == null) {
                    logger.warn("Generated code not found for ID: {}", testId);
                    continue;
                }
                
                // Write the test file
                writeTestFile(generatedCode, request.getFramework(), destinationPath);
                
                // Write page objects if available
                if (generatedCode.getPageObjectCode() != null && !generatedCode.getPageObjectCode().isEmpty()) {
                    writePageObjectFile(generatedCode, request.getFramework(), destinationPath);
                }
            } catch (Exception e) {
                logger.error("Error exporting test {}: {}", testId, e.getMessage(), e);
            }
        }
    }
    
    /**
     * Write a test file to the export directory
     */
    private void writeTestFile(GeneratedCode generatedCode, String framework, String destinationPath) throws IOException {
        String testName = generatedCode.getMetadata().getOrDefault("testName", "GeneratedTest").toString();
        
        Path testFilePath;
        if ("selenium-java".equals(framework)) {
            testFilePath = Paths.get(destinationPath, "src/test/java/com/cstestforge/tests", testName + "Test.java");
        } else if ("playwright-typescript".equals(framework)) {
            testFilePath = Paths.get(destinationPath, "tests", testName + ".spec.ts");
        } else {
            testFilePath = Paths.get(destinationPath, "tests", testName + ".test");
        }
        
        // Create parent directory
        Files.createDirectories(testFilePath.getParent());
        
        // Write test file
        Files.write(testFilePath, generatedCode.getCode().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Write a page object file to the export directory
     */
    private void writePageObjectFile(GeneratedCode generatedCode, String framework, String destinationPath) throws IOException {
        String pageObjectName = generatedCode.getMetadata().getOrDefault("pageObjectName", "GeneratedPage").toString();
        
        Path pageObjectFilePath;
        if ("selenium-java".equals(framework)) {
            pageObjectFilePath = Paths.get(destinationPath, "src/test/java/com/cstestforge/pages", pageObjectName + ".java");
        } else if ("playwright-typescript".equals(framework)) {
            pageObjectFilePath = Paths.get(destinationPath, "pages", pageObjectName + ".ts");
        } else {
            pageObjectFilePath = Paths.get(destinationPath, "pages", pageObjectName + ".page");
        }
        
        // Create parent directory
        Files.createDirectories(pageObjectFilePath.getParent());
        
        // Write page object file
        Files.write(pageObjectFilePath, generatedCode.getPageObjectCode().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Generate build files for the exported project
     */
    private void generateBuildFiles(ExportRequest request, String destinationPath) throws IOException {
        logger.debug("Generating build files for framework: {}", request.getFramework());
        
        if ("selenium-java".equals(request.getFramework())) {
            generateMavenBuildFile(request, destinationPath);
        } else if ("playwright-typescript".equals(request.getFramework())) {
            generateNpmBuildFiles(request, destinationPath);
        }
    }
    
    /**
     * Generate Maven build file for Selenium Java projects
     */
    private void generateMavenBuildFile(ExportRequest request, String destinationPath) throws IOException {
        Path pomPath = Paths.get(destinationPath, "pom.xml");
        
        String groupId = request.getConfig().getOrDefault("groupId", "com.cstestforge").toString();
        String artifactId = request.getConfig().getOrDefault("artifactId", request.getExportName()).toString();
        String version = request.getConfig().getOrDefault("version", "1.0-SNAPSHOT").toString();
        
        List<String> pomLines = new ArrayList<>();
        pomLines.add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        pomLines.add("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"");
        pomLines.add("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">");
        pomLines.add("    <modelVersion>4.0.0</modelVersion>");
        pomLines.add("    <groupId>" + groupId + "</groupId>");
        pomLines.add("    <artifactId>" + artifactId + "</artifactId>");
        pomLines.add("    <version>" + version + "</version>");
        
        pomLines.add("    <properties>");
        pomLines.add("        <maven.compiler.source>11</maven.compiler.source>");
        pomLines.add("        <maven.compiler.target>11</maven.compiler.target>");
        pomLines.add("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>");
        pomLines.add("        <selenium.version>4.10.0</selenium.version>");
        pomLines.add("        <testng.version>7.7.1</testng.version>");
        pomLines.add("    </properties>");
        
        pomLines.add("    <dependencies>");
        pomLines.add("        <dependency>");
        pomLines.add("            <groupId>org.seleniumhq.selenium</groupId>");
        pomLines.add("            <artifactId>selenium-java</artifactId>");
        pomLines.add("            <version>${selenium.version}</version>");
        pomLines.add("        </dependency>");
        pomLines.add("        <dependency>");
        pomLines.add("            <groupId>org.testng</groupId>");
        pomLines.add("            <artifactId>testng</artifactId>");
        pomLines.add("            <version>${testng.version}</version>");
        pomLines.add("            <scope>test</scope>");
        pomLines.add("        </dependency>");
        pomLines.add("        <dependency>");
        pomLines.add("            <groupId>io.github.bonigarcia</groupId>");
        pomLines.add("            <artifactId>webdrivermanager</artifactId>");
        pomLines.add("            <version>5.4.1</version>");
        pomLines.add("        </dependency>");
        pomLines.add("    </dependencies>");
        
        pomLines.add("    <build>");
        pomLines.add("        <plugins>");
        pomLines.add("            <plugin>");
        pomLines.add("                <groupId>org.apache.maven.plugins</groupId>");
        pomLines.add("                <artifactId>maven-surefire-plugin</artifactId>");
        pomLines.add("                <version>3.1.2</version>");
        pomLines.add("            </plugin>");
        pomLines.add("        </plugins>");
        pomLines.add("    </build>");
        pomLines.add("</project>");
        
        Files.write(pomPath, pomLines, StandardCharsets.UTF_8);
    }
    
    /**
     * Generate NPM build files for Playwright TypeScript projects
     */
    private void generateNpmBuildFiles(ExportRequest request, String destinationPath) throws IOException {
        Path packagePath = Paths.get(destinationPath, "package.json");
        
        Map<String, Object> packageJson = new HashMap<>();
        packageJson.put("name", request.getExportName());
        packageJson.put("version", "1.0.0");
        packageJson.put("description", "Generated test project using Playwright");
        packageJson.put("main", "index.js");
        
        Map<String, String> scripts = new HashMap<>();
        scripts.put("test", "playwright test");
        scripts.put("test:headed", "playwright test --headed");
        scripts.put("test:ui", "playwright test --ui");
        packageJson.put("scripts", scripts);
        
        Map<String, String> devDependencies = new HashMap<>();
        devDependencies.put("@playwright/test", "^1.38.0");
        devDependencies.put("typescript", "^5.1.6");
        packageJson.put("devDependencies", devDependencies);
        
        // Write package.json
        Files.write(packagePath, objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(packageJson).getBytes(StandardCharsets.UTF_8));
        
        // Create tsconfig.json
        Path tsconfigPath = Paths.get(destinationPath, "tsconfig.json");
        Map<String, Object> tsConfig = new HashMap<>();
        
        Map<String, Object> compilerOptions = new HashMap<>();
        compilerOptions.put("target", "es2020");
        compilerOptions.put("module", "commonjs");
        compilerOptions.put("strict", true);
        compilerOptions.put("esModuleInterop", true);
        compilerOptions.put("forceConsistentCasingInFileNames", true);
        compilerOptions.put("outDir", "./dist");
        
        tsConfig.put("compilerOptions", compilerOptions);
        tsConfig.put("include", Arrays.asList("tests/**/*.ts", "pages/**/*.ts"));
        
        Files.write(tsconfigPath, objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(tsConfig).getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Get the appropriate code generator for the given framework and language
     */
    private CodeGenerator getGenerator(String framework, String language) {
        return generators.get(framework);
    }
    
    /**
     * Save generated code to storage
     */
    private void saveGeneratedCode(GeneratedCode generatedCode) {
        String codePath = GENERATIONS_PATH + "/" + generatedCode.getId() + ".json";
        try {
            storageService.saveToJson(codePath, generatedCode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save generated code: " + e.getMessage(), e);
        }
    }
    
    /**
     * Load generated code from storage
     */
    private GeneratedCode loadGeneratedCode(String codeId) {
        String codePath = GENERATIONS_PATH + "/" + codeId + ".json";
        try {
            if (storageService.fileExists(codePath)) {
                return storageService.readFromJson(codePath, GeneratedCode.class);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load generated code: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetch events for a test definition
     */
    private List<Event> fetchEventsForTest(String testId) {
        try {
            // Get the test from the repository
            Optional<com.cstestforge.project.model.test.Test> testOpt = testRepository.findById(getProjectIdFromTestId(testId), testId);
            if (!testOpt.isPresent()) {
                logger.error("Test not found: {}", testId);
                return Collections.emptyList();
            }
            
            com.cstestforge.project.model.test.Test test = testOpt.get();
            List<Event> events = new ArrayList<>();
            
            // Convert test steps to events
            if (test.getSteps() != null) {
                for (com.cstestforge.project.model.test.TestStep step : test.getSteps()) {
                    Event event = convertStepToEvent(step);
                    if (event != null) {
                        events.add(event);
                    }
                }
            }
            
            return events;
        } catch (Exception e) {
            logger.error("Error fetching events for test: {}", testId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Fetch events for a recording session
     */
    private List<Event> fetchEventsForRecording(String recordingId) {
        try {
            // Get the recording from the repository
            List<com.cstestforge.recorder.model.RecordedEvent> recordedEvents = 
                recordingRepository.findEventsBySessionId(recordingId);
            
            if (recordedEvents.isEmpty()) {
                logger.error("No recorded events found for session: {}", recordingId);
                return Collections.emptyList();
            }
            
            List<Event> events = new ArrayList<>();
            
            // Convert recorded events to code generation events
            for (com.cstestforge.recorder.model.RecordedEvent recordedEvent : recordedEvents) {
                Event event = convertRecordedEventToEvent(recordedEvent);
                if (event != null) {
                    events.add(event);
                }
            }
            
            return events;
        } catch (Exception e) {
            logger.error("Error fetching events for recording: {}", recordingId, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get project ID from test ID
     */
    private String getProjectIdFromTestId(String testId) {
        try {
            // Query test repository to find the test
            for (String projectId : projectRepository.getAllProjectIds()) {
                if (testRepository.findById(projectId, testId).isPresent()) {
                    return projectId;
                }
            }
            
            logger.error("Could not find project ID for test: {}", testId);
            throw new IllegalArgumentException("Test not found: " + testId);
        } catch (Exception e) {
            logger.error("Error getting project ID from test ID: {}", testId, e);
            throw new IllegalArgumentException("Error resolving project for test: " + testId, e);
        }
    }
    
    /**
     * Get project ID from recording ID
     */
    private String getProjectIdFromRecordingId(String recordingId) {
        try {
            // Get the recording metadata which includes the project ID
            com.cstestforge.recorder.model.RecordingMetadata metadata = 
                recordingRepository.getRecordingMetadata(recordingId);
            
            if (metadata != null && metadata.getProjectId() != null) {
                return metadata.getProjectId();
            }
            
            logger.error("Could not find project ID for recording: {}", recordingId);
            throw new IllegalArgumentException("Recording not found or has no project ID: " + recordingId);
        } catch (Exception e) {
            logger.error("Error getting project ID from recording ID: {}", recordingId, e);
            throw new IllegalArgumentException("Error resolving project for recording: " + recordingId, e);
        }
    }
    
    /**
     * Convert a test step to an event
     */
    private Event convertStepToEvent(com.cstestforge.project.model.test.TestStep step) {
        try {
            // Determine event type based on step type
            String eventType = mapStepTypeToEventType(step.getType());
            
            // Create appropriate event based on type
            Event event;
            switch (eventType) {
                case "click":
                    event = new ClickEvent();
                    break;
                case "input":
                    event = new InputEvent();
                    if (step.getParameters().containsKey("value")) {
                        ((InputEvent) event).setValue(step.getParameters().get("value").toString());
                    }
                    break;
                case "navigation":
                    event = new NavigationEvent();
                    if (step.getParameters().containsKey("url")) {
                        ((NavigationEvent) event).setUrl(step.getParameters().get("url").toString());
                    }
                    break;
                case "assertion":
                    event = new AssertionEvent();
                    if (step.getParameters().containsKey("expectedValue")) {
                        ((AssertionEvent) event).setExpectedExpression(step.getParameters().get("expectedValue").toString());
                    }
                    break;
                case "wait":
                    event = new WaitEvent();
                    if (step.getParameters().containsKey("timeout")) {
                        ((WaitEvent) event).setTimeout(Integer.parseInt(step.getParameters().get("timeout").toString()));
                    }
                    break;
                default:
                    event = new CustomEvent();
                    break;
            }
            
            // Set common properties
            event.setId(step.getId());
            event.setName(step.getName());
            event.setDescription(step.getDescription());
            
            // Add element selector if available
            if (step.getParameters().containsKey("selector")) {
                event.addMetadata("selector", step.getParameters().get("selector"));
            }
            
            return event;
        } catch (Exception e) {
            logger.error("Error converting step to event: {}", step.getId(), e);
            return null;
        }
    }
    
    /**
     * Convert a recorded event to a code generation event
     */
    private Event convertRecordedEventToEvent(com.cstestforge.recorder.model.RecordedEvent recordedEvent) {
        try {
            // Create appropriate event based on type
            Event event;
            switch (recordedEvent.getType()) {
                case CLICK:
                    event = new ClickEvent();
                    break;
                case INPUT:
                case TYPE:
                    event = new InputEvent();
                    ((InputEvent) event).setValue(recordedEvent.getValue());
                    break;
                case NAVIGATION:
                    event = new NavigationEvent();
                    ((NavigationEvent) event).setUrl(recordedEvent.getUrl());
                    break;
                case ASSERTION:
                    event = new AssertionEvent();
                    ((AssertionEvent) event).setExpectedExpression(recordedEvent.getValue());
                    break;
                case WAIT:
                    event = new WaitEvent();
                    if (recordedEvent.getValue() != null) {
                        try {
                            ((WaitEvent) event).setTimeout(Integer.parseInt(recordedEvent.getValue()));
                        } catch (NumberFormatException e) {
                            ((WaitEvent) event).setTimeout(1000); // Default timeout
                        }
                    }
                    break;
                default:
                    event = new CustomEvent();
                    break;
            }
            
            // Set common properties
            event.setId(recordedEvent.getId().toString());
            event.setName(recordedEvent.getType().toString());
            
            // Add element info if available
            if (recordedEvent.getElementInfo() != null) {
                event.addMetadata("selector", recordedEvent.getElementInfo().getSelector());
                event.addMetadata("tagName", recordedEvent.getElementInfo().getTagName());
                event.addMetadata("xpath", recordedEvent.getElementInfo().getXpath());
            }
            
            return event;
        } catch (Exception e) {
            logger.error("Error converting recorded event to event: {}", recordedEvent.getId(), e);
            return null;
        }
    }
    
    /**
     * Map test step type to event type
     */
    private String mapStepTypeToEventType(com.cstestforge.project.model.test.TestStepType stepType) {
        if (stepType == null) {
            return "custom";
        }
        
        switch (stepType) {
            case CLICK:
                return "click";
            case INPUT:
                return "input";
            case NAVIGATION:
                return "navigation";
            case ASSERTION:
                return "assertion";
            case WAIT:
                return "wait";
            default:
                return "custom";
        }
    }
} 