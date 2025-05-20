package com.cstestforge.codegen.service.generator;

import com.cstestforge.codegen.model.event.*;
import com.cstestforge.codegen.service.template.TemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code generator for Playwright TypeScript framework using the CSTestForge framework structure
 */
public class PlaywrightTypeScriptGenerator extends AbstractCodeGenerator {
    
    private static final String FRAMEWORK_ID = "playwright-typescript";
    private static final String LANGUAGE = "typescript";
    
    // Generation style: Standard or BDD
    public enum GenerationStyle {
        STANDARD,
        BDD
    }
    
    private GenerationStyle style = GenerationStyle.STANDARD; // Default to Standard
    private String testName = "GeneratedTest";
    
    public PlaywrightTypeScriptGenerator(TemplateEngine templateEngine) {
        super(templateEngine, FRAMEWORK_ID, LANGUAGE);
    }
    
    /**
     * Set the generation style (Standard or BDD)
     * 
     * @param style The style to use
     */
    public void setGenerationStyle(GenerationStyle style) {
        this.style = style;
    }
    
    /**
     * Set the test class name
     * 
     * @param testName The name of the test class
     */
    public void setTestName(String testName) {
        this.testName = testName;
    }
    
    @Override
    public String generateCode(List<Event> events) {
        if (style == GenerationStyle.STANDARD) {
            return generateStandardStyle(events);
        } else {
            return generateBDDStyle(events);
        }
    }
    
    /**
     * Generate code using Standard style
     */
    private String generateStandardStyle(List<Event> events) {
        StringBuilder codeBuilder = new StringBuilder();
        
        codeBuilder.append(generateImports());
        codeBuilder.append(generateClassHeader());
        
        // Generate test method content
        String codeContent = generateEventsCode(events, 3);
        codeBuilder.append(codeContent);
        
        codeBuilder.append(generateClassFooter());
        
        return codeBuilder.toString();
    }
    
    /**
     * Generate code using BDD style
     */
    private String generateBDDStyle(List<Event> events) {
        StringBuilder codeBuilder = new StringBuilder();
        
        codeBuilder.append(generateBDDImports());
        codeBuilder.append(generateBDDClassHeader());
        
        // Group events into steps
        for (Event event : events) {
            String stepCode = generateEventCode(event, 1);
            codeBuilder.append(stepCode).append("\n\n");
        }
        
        codeBuilder.append(generateBDDClassFooter());
        
        return codeBuilder.toString();
    }
    
    @Override
    protected String generateImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("import { test, expect } from '@playwright/test';\n");
        imports.append("import { CSPlaywrightDriver } from '../../core/CSPlaywrightDriver';\n");
        imports.append("import { CSBasePage } from '../../core/CSBasePage';\n");
        imports.append("import { CSPlaywrightReporting, Status } from '../../reporting/CSPlaywrightReporting';\n");
        imports.append("import { CSElementInteractionHandler } from '../../element/CSElementInteractionHandler';\n");
        imports.append("import { CSBrowserManager } from '../../core/CSBrowserManager';\n\n");
        
        return imports.toString();
    }
    
    /**
     * Generate imports for BDD style
     */
    private String generateBDDImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("import { test, expect } from '@playwright/test';\n");
        imports.append("import { CSPlaywrightDriver } from '../../core/CSPlaywrightDriver';\n");
        imports.append("import { CSBasePage } from '../../core/CSBasePage';\n");
        imports.append("import { CSPlaywrightReporting, Status } from '../../reporting/CSPlaywrightReporting';\n");
        imports.append("import { CSElementInteractionHandler } from '../../element/CSElementInteractionHandler';\n");
        imports.append("import { CSBrowserManager } from '../../core/CSBrowserManager';\n\n");
        
        return imports.toString();
    }
    
    @Override
    protected String generateClassHeader() {
        StringBuilder header = new StringBuilder();
        header.append("/**\n");
        header.append(" * Auto-generated test using CSTestForge\n");
        header.append(" */\n\n");
        
        // Create Page Object class
        header.append("/**\n");
        header.append(" * Page Object for the test\n");
        header.append(" */\n");
        header.append("class GeneratedPage extends CSBasePage {\n");
        header.append("    /**\n");
        header.append("     * Initialize the page\n");
        header.append("     */\n");
        header.append("    protected async initializeElements(): Promise<void> {\n");
        header.append("        // Element initialization is handled by decorators\n");
        header.append("    }\n");
        header.append("}\n\n");
        
        // Create Test class
        header.append("/**\n");
        header.append(" * Generated Test Implementation\n");
        header.append(" */\n");
        header.append("class ").append(testName).append(" {\n");
        header.append("    private driver: CSPlaywrightDriver;\n");
        header.append("    private page: GeneratedPage;\n");
        header.append("    private reporting: CSPlaywrightReporting;\n");
        header.append("    private elementHandler: CSElementInteractionHandler;\n\n");
        
        header.append("    /**\n");
        header.append("     * Constructor initializes framework components\n");
        header.append("     */\n");
        header.append("    constructor() {\n");
        header.append("        this.driver = new CSPlaywrightDriver();\n");
        header.append("        this.page = new GeneratedPage();\n");
        header.append("        this.reporting = new CSPlaywrightReporting();\n");
        header.append("        this.elementHandler = new CSElementInteractionHandler();\n");
        header.append("    }\n\n");
        
        header.append("    /**\n");
        header.append("     * Run the generated test\n");
        header.append("     */\n");
        header.append("    async runTest() {\n");
        header.append("        const browserPage = await this.driver.getPage();\n");
        header.append("        this.reporting.startTest('Generated Test', 'Auto-generated test using CSTestForge');\n\n");
        header.append("        try {\n");
        
        return header.toString();
    }
    
    /**
     * Generate class header for BDD style
     */
    private String generateBDDClassHeader() {
        StringBuilder header = new StringBuilder();
        header.append("/**\n");
        header.append(" * Auto-generated step definitions using CSTestForge\n");
        header.append(" */\n\n");
        
        // Create Page Object class
        header.append("/**\n");
        header.append(" * Page Object for the steps\n");
        header.append(" */\n");
        header.append("class GeneratedPage extends CSBasePage {\n");
        header.append("    /**\n");
        header.append("     * Initialize the page\n");
        header.append("     */\n");
        header.append("    protected async initializeElements(): Promise<void> {\n");
        header.append("        // Element initialization is handled by decorators\n");
        header.append("    }\n");
        header.append("}\n\n");
        
        // Create Steps class
        header.append("/**\n");
        header.append(" * Generated Step Definitions\n");
        header.append(" */\n");
        header.append("export class ").append(testName).append("Steps {\n");
        header.append("    private page: GeneratedPage;\n");
        header.append("    private reporting: CSPlaywrightReporting;\n");
        header.append("    private elementHandler: CSElementInteractionHandler;\n\n");
        
        header.append("    /**\n");
        header.append("     * Constructor initializes framework components\n");
        header.append("     */\n");
        header.append("    constructor() {\n");
        header.append("        this.page = new GeneratedPage();\n");
        header.append("        this.reporting = new CSPlaywrightReporting();\n");
        header.append("        this.elementHandler = new CSElementInteractionHandler();\n");
        header.append("    }\n\n");
        
        return header.toString();
    }
    
    @Override
    protected String generateClassFooter() {
        StringBuilder footer = new StringBuilder();
        footer.append("\n            this.reporting.endTest(Status.PASS);\n");
        footer.append("        } catch (error) {\n");
        footer.append("            this.reporting.log(`Test failed: ${error}`, Status.FAIL);\n");
        footer.append("            await this.driver.takeScreenshot('test_failure');\n");
        footer.append("            this.reporting.endTest(Status.FAIL);\n");
        footer.append("            throw error;\n");
        footer.append("        } finally {\n");
        footer.append("            await CSBrowserManager.getInstance().closeContext();\n");
        footer.append("        }\n");
        footer.append("    }\n");
        footer.append("}\n\n");
        
        footer.append("// Playwright test definition\n");
        footer.append("test('Generated Test', async () => {\n");
        footer.append("    const testInstance = new ").append(testName).append("();\n");
        footer.append("    await testInstance.runTest();\n");
        footer.append("});\n");
        
        return footer.toString();
    }
    
    /**
     * Generate class footer for BDD style
     */
    private String generateBDDClassFooter() {
        StringBuilder footer = new StringBuilder();
        footer.append("    /**\n");
        footer.append("     * Clean up after scenario\n");
        footer.append("     */\n");
        footer.append("    async cleanUp(): Promise<void> {\n");
        footer.append("        // Close browser context to clean up resources\n");
        footer.append("        await CSBrowserManager.getInstance().closeContext();\n");
        footer.append("    }\n");
        footer.append("}\n\n");
        
        return footer.toString();
    }
    
    @Override
    public String generateAction(ActionEvent event, int indentLevel) {
        if (style == GenerationStyle.BDD) {
            return generateActionForBDD(event, indentLevel);
        }
        
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        String stepName = event.getName() != null ? event.getName() : "Step " + event.getActionType();
        String elementName = formatElementName(event.getTargetSelector());
        String methodName = formatFieldToMethod(elementName);
        
        code.append(indent).append("// Action: ").append(stepName).append("\n");
        code.append(indent).append("this.reporting.startStep('").append(stepName).append("');\n");
        
        switch (event.getActionType()) {
            case CLICK:
                code.append(indent).append("this.reporting.log('Clicking element: ")
                    .append(elementName).append("');\n");
                code.append(indent).append("await this.page.click").append(methodName).append("();");
                break;
                
            case DOUBLE_CLICK:
                code.append(indent).append("this.reporting.log('Double-clicking element: ")
                    .append(elementName).append("');\n");
                code.append(indent).append("const ").append(elementName).append("Element = await this.page.")
                    .append(elementName).append(";\n");
                code.append(indent).append("await this.elementHandler.doubleClick(").append(elementName).append("Element);");
                break;
                
            case TYPE:
                code.append(indent).append("this.reporting.log('Typing into element: ")
                    .append(elementName).append(" value: ").append(event.getValue()).append("');\n");
                code.append(indent).append("await this.page.enter").append(methodName)
                    .append("('").append(event.getValue()).append("');");
                break;
                
            case NAVIGATE:
                code.append(indent).append("this.reporting.log('Navigating to: ").append(event.getValue()).append("');\n");
                code.append(indent).append("await this.page.navigateToPage();");
                break;
                
            case WAIT:
                code.append(indent).append("this.reporting.log('Waiting for element: ")
                    .append(elementName).append("');\n");
                code.append(indent).append("await this.page.waitFor").append(methodName).append("Visible();");
                break;
                
            case SELECT:
                code.append(indent).append("this.reporting.log('Selecting option: ").append(event.getValue()).append("');\n");
                code.append(indent).append("await this.page.select").append(methodName)
                    .append("ByText('").append(event.getValue()).append("');");
                break;
                
            case HOVER:
                code.append(indent).append("this.reporting.log('Hovering over element: ")
                    .append(elementName).append("');\n");
                code.append(indent).append("const ").append(elementName).append("ForHover = await this.page.")
                    .append(elementName).append(";\n");
                code.append(indent).append("await this.elementHandler.hover(").append(elementName).append("ForHover);");
                break;
                
            case CHECK:
                code.append(indent).append("this.reporting.log('Checking element: ")
                    .append(elementName).append("');\n");
                code.append(indent).append("await this.page.set").append(methodName).append("State(true);");
                break;
                
            case UNCHECK:
                code.append(indent).append("this.reporting.log('Unchecking element: ")
                    .append(elementName).append("');\n");
                code.append(indent).append("await this.page.set").append(methodName).append("State(false);");
                break;
                
            default:
                code.append(indent).append("// Unsupported action type: ").append(event.getActionType()).append("\n");
                code.append(indent).append("this.reporting.log('Unsupported action type: ")
                    .append(event.getActionType()).append("', Status.WARNING);");
        }
        
        code.append("\n").append(indent).append("this.reporting.endStep(Status.PASS);");
        
        return code.toString();
    }
    
    /**
     * Format element name from selector for use in methods
     * 
     * @param target Selector string
     * @return Cleaned element name
     */
    private String formatElementName(String target) {
        if (target == null) {
            return "element";
        }
        
        // Extract last part of selector for readability
        String elementName = target;
        
        // Handle different selector types
        if (target.startsWith("id=")) {
            elementName = target.substring(3) + "Element"; 
        } else if (target.startsWith("css=")) {
            elementName = target.substring(4);
        } else if (target.startsWith("xpath=")) {
            elementName = target.substring(6);
        } else if (target.startsWith("text=")) {
            elementName = target.substring(5) + "Text";
        }
        
        // Clean up the element name
        elementName = elementName
            .replaceAll("[\\[\\]\"'()=]", "")
            .replaceAll("[\\s\\W]+", "_")
            .replace("#", "")
            .replace(".", "")
            .toLowerCase();
        
        // Ensure it starts with a lowercase letter
        if (!elementName.isEmpty()) {
            elementName = elementName.substring(0, 1).toLowerCase() + elementName.substring(1);
        }
        
        return elementName;
    }
    
    /**
     * Generate action for BDD style
     */
    private String generateActionForBDD(ActionEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        String stepName = event.getName() != null ? event.getName() : "Step " + event.getActionType();
        String methodName = stepNameToMethodName(stepName);
        String elementName = formatElementName(event.getTargetSelector());
        String elementMethodName = formatFieldToMethod(elementName);
        
        code.append(indent).append("/**\n");
        code.append(indent).append(" * ").append(stepName).append("\n");
        code.append(indent).append(" */\n");
        code.append(indent).append("public async ").append(methodName).append("(): Promise<void> {\n");
        
        String innerIndent = indent(indentLevel + 1);
        code.append(innerIndent).append("this.reporting.startStep('").append(stepName).append("');\n");
        
        switch (event.getActionType()) {
            case CLICK:
                code.append(innerIndent).append("this.reporting.log('Clicking element: ")
                    .append(elementName).append("');\n");
                code.append(innerIndent).append("await this.page.click").append(elementMethodName).append("();");
                break;
                
            case DOUBLE_CLICK:
                code.append(innerIndent).append("this.reporting.log('Double-clicking element: ")
                    .append(elementName).append("');\n");
                code.append(innerIndent).append("const ").append(elementName).append("Element = await this.page.")
                    .append(elementName).append(";\n");
                code.append(innerIndent).append("await this.elementHandler.doubleClick(").append(elementName).append("Element);");
                break;
                
            case TYPE:
                code.append(innerIndent).append("this.reporting.log('Typing into element: ")
                    .append(elementName).append(" value: ").append(event.getValue()).append("');\n");
                code.append(innerIndent).append("await this.page.enter").append(elementMethodName)
                    .append("('").append(event.getValue()).append("');");
                break;
                
            case NAVIGATE:
                code.append(innerIndent).append("this.reporting.log('Navigating to: ").append(event.getValue()).append("');\n");
                code.append(innerIndent).append("await this.page.navigateToPage();");
                break;
                
            case WAIT:
                code.append(innerIndent).append("this.reporting.log('Waiting for element: ")
                    .append(elementName).append("');\n");
                code.append(innerIndent).append("await this.page.waitFor").append(elementMethodName).append("Visible();");
                break;
                
            case SELECT:
                code.append(innerIndent).append("this.reporting.log('Selecting option: ").append(event.getValue()).append("');\n");
                code.append(innerIndent).append("await this.page.select").append(elementMethodName)
                    .append("ByText('").append(event.getValue()).append("');");
                break;
                
            case HOVER:
                code.append(innerIndent).append("this.reporting.log('Hovering over element: ")
                    .append(elementName).append("');\n");
                code.append(innerIndent).append("const ").append(elementName).append("ForHover = await this.page.")
                    .append(elementName).append(";\n");
                code.append(innerIndent).append("await this.elementHandler.hover(").append(elementName).append("ForHover);");
                break;
                
            case CHECK:
                code.append(innerIndent).append("this.reporting.log('Checking element: ")
                    .append(elementName).append("');\n");
                code.append(innerIndent).append("await this.page.set").append(elementMethodName).append("State(true);");
                break;
                
            case UNCHECK:
                code.append(innerIndent).append("this.reporting.log('Unchecking element: ")
                    .append(elementName).append("');\n");
                code.append(innerIndent).append("await this.page.set").append(elementMethodName).append("State(false);");
                break;
                
            default:
                code.append(innerIndent).append("// Unsupported action type: ").append(event.getActionType()).append("\n");
                code.append(innerIndent).append("this.reporting.log('Unsupported action type: ")
                    .append(event.getActionType()).append("', Status.WARNING);");
        }
        
        code.append("\n").append(innerIndent).append("this.reporting.endStep(Status.PASS);");
        code.append("\n").append(indent).append("}");
        
        return code.toString();
    }
    
    @Override
    public String generateAssertion(AssertionEvent event, int indentLevel) {
        if (style == GenerationStyle.BDD) {
            return generateAssertionForBDD(event, indentLevel);
        }
        
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Assertion: ").append(event.getName()).append("\n");
        code.append(indent).append("this.reporting.startStep('Assertion: ").append(event.getName()).append("');\n");
        code.append(generateActualAssertion(event, indentLevel, "browserPage")).append("\n");
        code.append(indent).append("this.reporting.endStep(Status.PASS);");
        
        return code.toString();
    }
    
    /**
     * Generate assertion for BDD style
     */
    private String generateAssertionForBDD(AssertionEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        String stepName = event.getName() != null ? event.getName() : "Assert " + event.getAssertType();
        String methodName = stepNameToMethodName(stepName);
        
        code.append(indent).append("/**\n");
        code.append(indent).append(" * ").append(stepName).append("\n");
        code.append(indent).append(" */\n");
        code.append(indent).append("public async ").append(methodName).append("(): Promise<void> {\n");
        
        String innerIndent = indent(indentLevel + 1);
        code.append(innerIndent).append("this.reporting.startStep('").append(stepName).append("');\n");
        code.append(innerIndent).append("const page = await this.page.getPage();\n");
        code.append(generateActualAssertion(event, indentLevel + 1, "page")).append("\n");
        code.append(innerIndent).append("this.reporting.endStep(Status.PASS);");
        code.append("\n").append(indent).append("}");
        
        return code.toString();
    }
    
    private String generateActualAssertion(AssertionEvent event, int indentLevel, String pageName) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        String message = event.getMessage() != null && !event.getMessage().isEmpty() 
            ? ", {message: '" + event.getMessage() + "'}" 
            : "";
        
        // If this is an element assertion, format the element name
        String elementName = null;
        String elementMethodName = null;
        if (event.getAssertType() == AssertionEvent.AssertType.ELEMENT_PRESENT || 
            event.getAssertType() == AssertionEvent.AssertType.ELEMENT_NOT_PRESENT ||
            event.getAssertType() == AssertionEvent.AssertType.ELEMENT_VISIBLE ||
            event.getAssertType() == AssertionEvent.AssertType.ELEMENT_NOT_VISIBLE ||
            event.getAssertType() == AssertionEvent.AssertType.ELEMENT_ENABLED ||
            event.getAssertType() == AssertionEvent.AssertType.ELEMENT_DISABLED) {
            elementName = formatElementName(event.getActualExpression());
            elementMethodName = formatFieldToMethod(elementName);
        }
        
        switch (event.getAssertType()) {
            case EQUALS:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toEqual(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case NOT_EQUALS:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").not.toEqual(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case TRUE:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeTruthy(").append(message).append(");");
                break;
            case FALSE:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeFalsy(").append(message).append(");");
                break;
            case ELEMENT_PRESENT:
                code.append(indent).append("await expect(this.page.is").append(elementMethodName)
                    .append("Displayed()).resolves.toBeTruthy(").append(message).append(");");
                break;
            case ELEMENT_NOT_PRESENT:
                code.append(indent).append("await expect(this.page.is").append(elementMethodName)
                    .append("Displayed()).resolves.toBeFalsy(").append(message).append(");");
                break;
            case ELEMENT_VISIBLE:
                code.append(indent).append("await expect(this.page.is").append(elementMethodName)
                    .append("Displayed()).resolves.toBeTruthy(").append(message).append(");");
                break;
            case ELEMENT_NOT_VISIBLE:
                code.append(indent).append("await expect(this.page.is").append(elementMethodName)
                    .append("Displayed()).resolves.toBeFalsy(").append(message).append(");");
                break;
            case CONTAINS:
                // For elements, use getText() method if the actual expression is a selector
                if (event.getActualExpression().contains("=")) {
                    // This is likely a selector, so treat it as an element text assertion
                    elementName = formatElementName(event.getActualExpression());
                    elementMethodName = formatFieldToMethod(elementName);
                    code.append(indent).append("const actualText = await this.page.get").append(elementMethodName).append("Text();\n");
                    code.append(indent).append("expect(actualText).toContain(").append(event.getExpectedExpression()).append(message).append(");");
                } else {
                    // This is a regular string contains assertion
                    code.append(indent).append("expect(").append(event.getActualExpression()).append(")")
                        .append(".toContain(").append(event.getExpectedExpression()).append(message).append(");");
                }
                break;
            case NOT_CONTAINS:
                // For elements, use getText() method if the actual expression is a selector
                if (event.getActualExpression().contains("=")) {
                    // This is likely a selector, so treat it as an element text assertion
                    elementName = formatElementName(event.getActualExpression());
                    elementMethodName = formatFieldToMethod(elementName);
                    code.append(indent).append("const text = await this.page.get").append(elementMethodName).append("Text();\n");
                    code.append(indent).append("expect(text).not.toContain(").append(event.getExpectedExpression()).append(message).append(");");
                } else {
                    // This is a regular string not contains assertion
                    code.append(indent).append("expect(").append(event.getActualExpression()).append(")")
                        .append(".not.toContain(").append(event.getExpectedExpression()).append(message).append(");");
                }
                break;
            case ELEMENT_ENABLED:
                code.append(indent).append("const ").append(elementName).append("Element = await this.page.")
                    .append(elementName).append(";\n");
                code.append(indent).append("await expect(").append(elementName).append("Element.isEnabled())").append(".resolves.toBeTruthy(")
                    .append(message).append(");");
                break;
            case ELEMENT_DISABLED:
                code.append(indent).append("const ").append(elementName).append("DisabledElement = await this.page.")
                    .append(elementName).append(";\n");
                code.append(indent).append("await expect(").append(elementName).append("DisabledElement.isEnabled())").append(".resolves.toBeFalsy(")
                    .append(message).append(");");
                break;
            case NULL:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeNull(").append(message).append(");");
                break;
            case NOT_NULL:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").not.toBeNull(").append(message).append(");");
                break;
            case GREATER_THAN:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeGreaterThan(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case LESS_THAN:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeLessThan(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case GREATER_THAN_OR_EQUAL:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeGreaterThanOrEqual(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case LESS_THAN_OR_EQUAL:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toBeLessThanOrEqual(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case EMPTY:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toHaveLength(0").append(message).append(");");
                break;
            case NOT_EMPTY:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").not.toHaveLength(0").append(message).append(");");
                break;
            case MATCHES_REGEX:
                code.append(indent).append("expect(").append(event.getActualExpression())
                    .append(").toMatch(").append(event.getExpectedExpression()).append(message).append(");");
                break;
            case CUSTOM:
                if (event.getCustomAssertion() != null && !event.getCustomAssertion().isEmpty()) {
                    code.append(indent).append(event.getCustomAssertion());
                } else {
                    code.append(indent).append("// Custom assertion not specified");
                }
                break;
            default:
                code.append(indent).append("// Unsupported assertion type: ").append(event.getAssertType());
        }
        
        return code.toString();
    }
    
    @Override
    public String generateLoop(LoopEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Loop: ").append(event.getName()).append("\n");
        
        switch (event.getLoopType()) {
            case FOR:
                code.append(indent).append("for (").append(event.getInitialization()).append("; ")
                    .append(event.getCondition()).append("; ")
                    .append(event.getIncrement()).append(") {\n");
                break;
            case WHILE:
                code.append(indent).append("while (").append(event.getCondition()).append(") {\n");
                break;
            case FOR_EACH:
                code.append(indent).append("for (const ").append(event.getIteratorVariable())
                    .append(" of ").append(event.getIterableExpression()).append(") {\n");
                break;
        }
        
        code.append(generateEventsCode(event.getLoopEvents(), indentLevel + 1));
        code.append("\n").append(indent).append("}");
        
        return code.toString();
    }
    
    @Override
    public String generateConditional(ConditionalEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Conditional: ").append(event.getName()).append("\n");
        code.append(indent).append("if (").append(event.getCondition()).append(") {\n");
        code.append(generateEventsCode(event.getThenEvents(), indentLevel + 1));
        code.append("\n").append(indent).append("}");
        
        if (event.isHasElse() && !event.getElseEvents().isEmpty()) {
            code.append(" else {\n");
            code.append(generateEventsCode(event.getElseEvents(), indentLevel + 1));
            code.append("\n").append(indent).append("}");
        }
        
        return code.toString();
    }
    
    @Override
    public String generateCapture(CaptureEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        String resultVar = event.getResultVariable();
        
        code.append(indent).append("// Capture: ").append(event.getName()).append("\n");
        code.append(indent).append("this.reporting.startStep('").append(event.getName()).append("');\n");
        
        switch (event.getCaptureType()) {
            case TEXT:
                code.append(indent).append("const ").append(resultVar).append(" = await browserPage.locator('")
                    .append(event.getTargetSelector()).append("').textContent();\n");
                code.append(indent).append("this.reporting.log(`Captured text: ${").append(resultVar).append("}`);");
                break;
            case ATTRIBUTE:
                code.append(indent).append("const ").append(resultVar).append(" = await browserPage.locator('")
                    .append(event.getTargetSelector()).append("').getAttribute('")
                    .append(event.getTargetAttribute()).append("');\n");
                code.append(indent).append("this.reporting.log(`Captured attribute: ${").append(resultVar).append("}`);");
                break;
            case VALUE:
                code.append(indent).append("const ").append(resultVar).append(" = await browserPage.locator('")
                    .append(event.getTargetSelector()).append("').inputValue();\n");
                code.append(indent).append("this.reporting.log(`Captured value: ${").append(resultVar).append("}`);");
                break;
            case SCREENSHOT:
                code.append(indent).append("await this.driver.takeScreenshot('")
                    .append(event.getName().replaceAll("\\s+", "_")).append("');\n");
                code.append(indent).append("this.reporting.log('Captured screenshot');");
                break;
            default:
                code.append(indent).append("// Unsupported capture type: ").append(event.getCaptureType());
        }
        
        code.append("\n").append(indent).append("this.reporting.endStep(Status.PASS);");
        
        return code.toString();
    }
    
    @Override
    public String generateGroup(GroupEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Group: ").append(event.getName()).append("\n");
        code.append(indent).append("this.reporting.startStep('Group: ").append(event.getName()).append("');\n");
        
        // Generate code for each event in the group
        code.append(generateEventsCode(event.getEvents(), indentLevel));
        
        code.append("\n").append(indent).append("this.reporting.endStep(Status.PASS);");
        
        return code.toString();
    }
    
    @Override
    public String generateTryCatch(TryCatchEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Try-Catch block: ").append(event.getName()).append("\n");
        code.append(indent).append("try {\n");
        code.append(generateEventsCode(event.getTryEvents(), indentLevel + 1));
        code.append("\n").append(indent).append("}");
        
        for (Map.Entry<String, List<Event>> catchBlock : event.getCatchBlocks().entrySet()) {
            code.append(" catch (error) {\n");
            code.append(indent).append("    this.reporting.log(`Exception caught: ${error}`, Status.WARNING);\n");
            code.append(generateEventsCode(catchBlock.getValue(), indentLevel + 1));
            code.append("\n").append(indent).append("}");
        }
        
        if (event.isHasFinally()) {
            code.append(" finally {\n");
            code.append(generateEventsCode(event.getFinallyEvents(), indentLevel + 1));
            code.append("\n").append(indent).append("}");
        }
        
        return code.toString();
    }
    
    // Generate Page Object for test
    public String generatePageObject(String pageName, Map<String, String> elements) {
        StringBuilder pageCode = new StringBuilder();
        
        // Imports
        pageCode.append("import { Locator } from 'playwright';\n");
        pageCode.append("import { CSBasePage } from '../../core/CSBasePage';\n");
        pageCode.append("import { CSFindByCss, CSFindById, CSFindByText, CSFindByAI } from '../../annotation/CSFindBy';\n");
        pageCode.append("import { CSElementInteractionHandler } from '../../element/CSElementInteractionHandler';\n");
        pageCode.append("import { CSPlaywrightReporting, Status } from '../../reporting/CSPlaywrightReporting';\n\n");
        
        // Class declaration
        pageCode.append("/**\n");
        pageCode.append(" * Page object for ").append(pageName).append("\n");
        pageCode.append(" * Implements Page Object Model pattern with CSTestForge framework\n");
        pageCode.append(" */\n");
        pageCode.append("export class ").append(pageName).append(" extends CSBasePage {\n");
        
        // Define page URL and title
        pageCode.append("    // Define the page URL and title\n");
        pageCode.append("    protected readonly pageUrl: string = '/").append(pageName.toLowerCase().replace("page", "")).append("';\n");
        pageCode.append("    protected readonly pageTitle: string = '").append(formatFieldToDescription(pageName)).append("';\n\n");
        
        // Define elements
        pageCode.append("    // Page elements with CSFindBy decorators for self-healing\n");
        for (Map.Entry<String, String> element : elements.entrySet()) {
            String fieldName = element.getKey();
            String selector = element.getValue();
            
            // Determine the appropriate decorator based on selector type
            String decoratorType = "CSFindByCss";
            String selectorValue = selector;
            
            if (selector.startsWith("id=")) {
                decoratorType = "CSFindById";
                selectorValue = selector.substring(3);
            } else if (selector.startsWith("text=")) {
                decoratorType = "CSFindByText"; 
                selectorValue = selector.substring(5);
            } else if (selector.startsWith("xpath=")) {
                decoratorType = "CSFindByXPath";
                selectorValue = selector.substring(6);
            } else if (selector.startsWith("data-testid=")) {
                decoratorType = "CSFindByTestId";
                selectorValue = selector.substring(12);
            } else if (selector.startsWith("name=")) {
                decoratorType = "CSFindByName";
                selectorValue = selector.substring(5);
            }
            
            // Add additional options for critical elements
            String options = "";
            if (fieldName.toLowerCase().contains("submit") || 
                fieldName.toLowerCase().contains("button") || 
                fieldName.toLowerCase().contains("login") ||
                fieldName.toLowerCase().contains("form")) {
                options = ", { waitForVisible: true }";
            }
            
            pageCode.append("    @").append(decoratorType).append("('").append(selectorValue).append("'").append(options).append(")\n");
            pageCode.append("    private ").append(fieldName).append("!: Promise<Locator>;\n\n");
        }
        
        // Initialize elements method
        pageCode.append("    /**\n");
        pageCode.append("     * Initialize elements that need special handling\n");
        pageCode.append("     */\n");
        pageCode.append("    protected async initializeElements(): Promise<void> {\n");
        pageCode.append("        // Additional initialization can be implemented here if needed\n");
        pageCode.append("    }\n\n");
        
        // Protected validation method
        pageCode.append("    /**\n");
        pageCode.append("     * Additional validation for ").append(formatFieldToDescription(pageName)).append("\n");
        pageCode.append("     */\n");
        pageCode.append("    protected async performAdditionalValidation(): Promise<void> {\n");
        pageCode.append("        // Add custom validation logic here\n");
        pageCode.append("    }\n\n");
        
        // Methods for each element
        for (Map.Entry<String, String> element : elements.entrySet()) {
            String fieldName = element.getKey();
            String methodName = formatFieldToMethod(fieldName);
            String description = formatFieldToDescription(fieldName);
            
            // Click method
            pageCode.append("    /**\n");
            pageCode.append("     * Click the ").append(description).append("\n");
            pageCode.append("     * \n");
            pageCode.append("     * @returns This page object for method chaining\n");
            pageCode.append("     */\n");
            pageCode.append("    public async click").append(methodName).append("(): Promise<this> {\n");
            pageCode.append("        const element = await this.").append(fieldName).append(";\n");
            pageCode.append("        await this.elementHandler.click(element);\n");
            pageCode.append("        return this;\n");
            pageCode.append("    }\n\n");
            
            // Type method (if fieldName contains "input", "field", "text")
            if (fieldName.toLowerCase().contains("input") || 
                fieldName.toLowerCase().contains("field") || 
                fieldName.toLowerCase().contains("text")) {
                
                pageCode.append("    /**\n");
                pageCode.append("     * Enter text in the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @param text The text to enter\n");
                pageCode.append("     * @returns This page object for method chaining\n");
                pageCode.append("     */\n");
                pageCode.append("    public async enter").append(methodName).append("(text: string): Promise<this> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        await this.elementHandler.fill(element, text);\n");
                pageCode.append("        return this;\n");
                pageCode.append("    }\n\n");
                
                // Clear method for input fields
                pageCode.append("    /**\n");
                pageCode.append("     * Clear the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @returns This page object for method chaining\n");
                pageCode.append("     */\n");
                pageCode.append("    public async clear").append(methodName).append("(): Promise<this> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        await this.elementHandler.clearElement(element);\n");
                pageCode.append("        return this;\n");
                pageCode.append("    }\n\n");
            }
            
            // Select method (if fieldName contains "select", "dropdown", "combo")
            if (fieldName.toLowerCase().contains("select") || 
                fieldName.toLowerCase().contains("dropdown") || 
                fieldName.toLowerCase().contains("combo")) {
                
                pageCode.append("    /**\n");
                pageCode.append("     * Select an option by visible text from the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @param optionText The visible text of the option to select\n");
                pageCode.append("     * @returns This page object for method chaining\n");
                pageCode.append("     */\n");
                pageCode.append("    public async select").append(methodName).append("ByText(optionText: string): Promise<this> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        await this.elementHandler.selectByVisibleText(element, optionText);\n");
                pageCode.append("        return this;\n");
                pageCode.append("    }\n\n");
                
                pageCode.append("    /**\n");
                pageCode.append("     * Select an option by value from the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @param value The value of the option to select\n");
                pageCode.append("     * @returns This page object for method chaining\n");
                pageCode.append("     */\n");
                pageCode.append("    public async select").append(methodName).append("ByValue(value: string): Promise<this> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        await this.elementHandler.selectByValue(element, value);\n");
                pageCode.append("        return this;\n");
                pageCode.append("    }\n\n");
            }
            
            // Checkbox/radio method (if fieldName contains "checkbox", "radio", "toggle")
            if (fieldName.toLowerCase().contains("checkbox") || 
                fieldName.toLowerCase().contains("radio") || 
                fieldName.toLowerCase().contains("toggle")) {
                
                pageCode.append("    /**\n");
                pageCode.append("     * Check/uncheck the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @param check True to check, false to uncheck\n");
                pageCode.append("     * @returns This page object for method chaining\n");
                pageCode.append("     */\n");
                pageCode.append("    public async set").append(methodName).append("State(check: boolean): Promise<this> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        await this.elementHandler.setCheckboxState(element, check);\n");
                pageCode.append("        return this;\n");
                pageCode.append("    }\n\n");
                
                pageCode.append("    /**\n");
                pageCode.append("     * Get the checked state of the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @returns True if checked, false otherwise\n");
                pageCode.append("     */\n");
                pageCode.append("    public async is").append(methodName).append("Checked(): Promise<boolean> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        return await element.isChecked();\n");
                pageCode.append("    }\n\n");
            }
            
            // Get text method for text-containing elements
            if (!fieldName.toLowerCase().contains("button") &&
                !fieldName.toLowerCase().contains("checkbox") &&
                !fieldName.toLowerCase().contains("radio")) {
                    
                pageCode.append("    /**\n");
                pageCode.append("     * Get text from the ").append(description).append("\n");
                pageCode.append("     * \n");
                pageCode.append("     * @returns The element text\n");
                pageCode.append("     */\n");
                pageCode.append("    public async get").append(methodName).append("Text(): Promise<string> {\n");
                pageCode.append("        const element = await this.").append(fieldName).append(";\n");
                pageCode.append("        return await this.elementHandler.getText(element);\n");
                pageCode.append("    }\n\n");
            }
            
            // Is visible/displayed method for all elements
            pageCode.append("    /**\n");
            pageCode.append("     * Check if the ").append(description).append(" is displayed\n");
            pageCode.append("     * \n");
            pageCode.append("     * @returns True if displayed, false otherwise\n");
            pageCode.append("     */\n");
            pageCode.append("    public async is").append(methodName).append("Displayed(): Promise<boolean> {\n");
            pageCode.append("        try {\n");
            pageCode.append("            const element = await this.").append(fieldName).append(";\n");
            pageCode.append("            return await element.isVisible();\n");
            pageCode.append("        } catch (error) {\n");
            pageCode.append("            return false;\n");
            pageCode.append("        }\n");
            pageCode.append("    }\n\n");
        }
        
        // Wait for page to load method
        pageCode.append("    /**\n");
        pageCode.append("     * Wait for the page to be fully loaded\n");
        pageCode.append("     * \n");
        pageCode.append("     * @returns This page object for method chaining\n");
        pageCode.append("     */\n");
        pageCode.append("    public async waitForPageLoad(): Promise<this> {\n");
        pageCode.append("        const page = await this.getPage();\n");
        pageCode.append("        await page.waitForLoadState('networkidle');\n");
        pageCode.append("        await page.waitForLoadState('domcontentloaded');\n");
        pageCode.append("        return this;\n");
        pageCode.append("    }\n");
        
        pageCode.append("}\n");
        
        return pageCode.toString();
    }
    
    /**
     * Format field name to method name (e.g., loginButton -> LoginButton)
     */
    private String formatFieldToMethod(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "";
        }
        return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }
    
    /**
     * Format field name to description (e.g., loginButton -> login button)
     */
    private String formatFieldToDescription(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "";
        }
        
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                desc.append(' ');
            }
            desc.append(i == 0 ? Character.toLowerCase(c) : c);
        }
        return desc.toString();
    }
    
    /**
     * Convert a step name to a camel case method name
     * e.g. "User clicks on login button" -> "userClicksOnLoginButton"
     */
    private String stepNameToMethodName(String stepName) {
        if (stepName == null || stepName.isEmpty()) {
            return "step";
        }
        
        // Replace special characters and split by spaces
        String[] parts = stepName.replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+");
        StringBuilder method = new StringBuilder();
        
        // Start with lowercase
        method.append(parts[0].toLowerCase());
        
        // Continue with uppercase first letter
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                method.append(parts[i].substring(0, 1).toUpperCase());
                if (parts[i].length() > 1) {
                    method.append(parts[i].substring(1));
                }
            }
        }
        
        return method.toString();
    }
} 