package com.cstestforge.codegen.service.generator;

import com.cstestforge.codegen.model.event.*;
import com.cstestforge.codegen.service.template.TemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Code generator for Selenium Java framework using the CSTestForge framework structure
 */
public class SeleniumJavaGenerator extends AbstractCodeGenerator {
    
    private static final String FRAMEWORK_ID = "selenium-java";
    private static final String LANGUAGE = "java";
    
    // Generation style: TestNG or BDD
    public enum GenerationStyle {
        TESTNG,
        BDD
    }
    
    private GenerationStyle style = GenerationStyle.TESTNG; // Default to TestNG
    private String testName = "GeneratedTest";
    private String packageName = "com.cstestforge.generated";
    
    public SeleniumJavaGenerator(TemplateEngine templateEngine) {
        super(templateEngine, FRAMEWORK_ID, LANGUAGE);
    }
    
    /**
     * Set the generation style (TestNG or BDD)
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
    
    /**
     * Set the package name
     * 
     * @param packageName The package name
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
    
    @Override
    public String generateCode(List<Event> events) {
        if (style == GenerationStyle.TESTNG) {
            return generateTestNGStyle(events);
        } else {
            return generateBDDStyle(events);
        }
    }
    
    /**
     * Generate code using TestNG style
     */
    private String generateTestNGStyle(List<Event> events) {
        StringBuilder codeBuilder = new StringBuilder();
        
        codeBuilder.append(generatePackageAndImports());
        codeBuilder.append(generateTestNGClassHeader());
        
        // Generate test method content
        String codeContent = generateEventsCode(events, 2);
        codeBuilder.append(codeContent);
        
        codeBuilder.append(generateTestNGClassFooter());
        
        return codeBuilder.toString();
    }
    
    /**
     * Generate code using BDD style
     */
    private String generateBDDStyle(List<Event> events) {
        StringBuilder codeBuilder = new StringBuilder();
        
        codeBuilder.append(generateBDDPackageAndImports());
        codeBuilder.append(generateBDDClassHeader());
        
        // Group events into steps
        for (Event event : events) {
            String stepCode = generateEventCode(event, 1);
            codeBuilder.append(stepCode).append("\n\n");
        }
        
        codeBuilder.append(generateBDDClassFooter());
        
        return codeBuilder.toString();
    }
    
    /**
     * Generate package declaration and imports for TestNG style
     */
    private String generatePackageAndImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("package ").append(packageName).append(";\n\n");
        
        imports.append("import org.slf4j.Logger;\n");
        imports.append("import org.slf4j.LoggerFactory;\n");
        imports.append("import org.testng.Assert;\n");
        imports.append("import org.testng.annotations.Test;\n");
        imports.append("import org.testng.annotations.BeforeMethod;\n");
        imports.append("import org.testng.annotations.BeforeClass;\n");
        imports.append("import org.testng.annotations.AfterMethod;\n");
        imports.append("import org.testng.annotations.AfterClass;\n");
        imports.append("import org.openqa.selenium.WebDriver;\n");
        imports.append("import org.openqa.selenium.By;\n");
        imports.append("import org.openqa.selenium.WebElement;\n");
        imports.append("import java.util.Map;\n\n");
        
        // Include CSTestForge framework imports
        imports.append("// CSTestForge framework imports\n");
        imports.append("import com.cstestforge.framework.selenium.annotation.CSDataProvider;\n");
        imports.append("import com.cstestforge.framework.selenium.annotation.CSMetaData;\n");
        imports.append("import com.cstestforge.framework.selenium.annotation.CSTestStep;\n");
        imports.append("import com.cstestforge.framework.selenium.core.CSBaseTest;\n");
        imports.append("import com.cstestforge.framework.selenium.core.CSDriverManager;\n");
        imports.append("import com.cstestforge.framework.selenium.reporting.CSReporting;\n");
        imports.append("import com.cstestforge.framework.selenium.utils.CSJsonReader;\n");
        imports.append("import com.cstestforge.framework.selenium.element.CSElementInteractionHandler;\n");
        imports.append("\n");
        
        // Add page object import
        imports.append("// Page object import\n");
        imports.append("import ").append(packageName).append(".page.").append(testName.replace("Test", "Page")).append(";\n\n");
        
        return imports.toString();
    }
    
    /**
     * Generate package declaration and imports for BDD style
     */
    private String generateBDDPackageAndImports() {
        StringBuilder imports = new StringBuilder();
        imports.append("package ").append(packageName).append(";\n\n");
        
        imports.append("import org.slf4j.Logger;\n");
        imports.append("import org.slf4j.LoggerFactory;\n");
        imports.append("import org.testng.Assert;\n");
        imports.append("import org.openqa.selenium.WebElement;\n\n");
        
        // Cucumber imports
        imports.append("// Cucumber imports\n");
        imports.append("import io.cucumber.java.en.Given;\n");
        imports.append("import io.cucumber.java.en.When;\n");
        imports.append("import io.cucumber.java.en.Then;\n");
        imports.append("import io.cucumber.java.Scenario;\n");
        imports.append("import io.cucumber.java.Before;\n");
        imports.append("import io.cucumber.java.After;\n");
        imports.append("import io.cucumber.java.AfterStep;\n\n");
        
        // Include CSTestForge framework imports
        imports.append("// CSTestForge framework imports\n");
        imports.append("import com.cstestforge.framework.selenium.annotation.CSMetaData;\n");
        imports.append("import com.cstestforge.framework.selenium.annotation.CSTestStep;\n");
        imports.append("import com.cstestforge.framework.selenium.bdd.CSBaseStepDefinition;\n");
        imports.append("import com.cstestforge.framework.selenium.bdd.CSScenarioContext;\n");
        imports.append("import com.cstestforge.framework.selenium.core.CSDriverManager;\n");
        imports.append("import com.cstestforge.framework.selenium.element.CSElementInteractionHandler;\n");
        imports.append("import com.cstestforge.framework.selenium.reporting.CSReporting;\n");
        imports.append("\n");
        
        // Add page object import
        imports.append("// Page object import\n");
        imports.append("import ").append(packageName).append(".page.").append(testName.replace("Steps", "Page")).append(";\n\n");
        
        return imports.toString();
    }
    
    /**
     * Generate test class header for TestNG style
     */
    private String generateTestNGClassHeader() {
        StringBuilder header = new StringBuilder();
        header.append("/**\n");
        header.append(" * Auto-generated test class using CSTestForge\n");
        header.append(" */\n");
        header.append("@CSMetaData(\n");
        header.append("    feature = \"Generated Feature\",\n");
        header.append("    description = \"Auto-generated test\",\n");
        header.append("    authors = {\"CSTestForge Generator\"},\n");
        header.append("    tags = {\"generated\", \"automated\"}\n");
        header.append(")\n");
        header.append("public class ").append(testName).append(" extends CSBaseTest {\n\n");
        
        header.append("    private static final Logger logger = LoggerFactory.getLogger(").append(testName).append(".class);\n");
        
        // Page object reference with proper page package name
        header.append("    private ").append(testName.replace("Test", "Page")).append(" page;\n\n");
        
        // Setup method - not needed as it's in CSBaseTest
        header.append("    /**\n");
        header.append("     * Set up before test method\n");
        header.append("     */\n");
        header.append("    @BeforeMethod\n");
        header.append("    @Override\n");
        header.append("    public void setupTest(String browser) {\n");
        header.append("        super.setupTest(browser);\n");
        header.append("        // Initialize page object\n");
        header.append("        page = new ").append(testName.replace("Test", "Page")).append("();\n");
        header.append("        logger.info(\"Test setup complete with page initialization\");\n");
        header.append("    }\n\n");
        
        // DataProvider method
        header.append("    /**\n");
        header.append("     * Data provider for the test\n");
        header.append("     * \n");
        header.append("     * @return Test data\n");
        header.append("     */\n");
        header.append("    @CSDataProvider(source = \"testdata/").append(testName.toLowerCase()).append("-data.json\",\n");
        header.append("                  description = \"Test data for ").append(testName).append("\")\n");
        header.append("    public Object[][] getTestData() {\n");
        header.append("        try {\n");
        header.append("            CSJsonReader jsonReader = new CSJsonReader(\"testdata/").append(testName.toLowerCase()).append("-data.json\");\n");
        header.append("            return jsonReader.createDataProvider();\n");
        header.append("        } catch (Exception e) {\n");
        header.append("            logger.warn(\"Could not load test data, using default values\", e);\n");
        header.append("            // Default test data if file not found\n");
        header.append("            return new Object[][] {\n");
        header.append("                { Map.of(\"param1\", \"value1\", \"param2\", \"value2\") },\n");
        header.append("                { Map.of(\"param1\", \"value3\", \"param2\", \"value4\") }\n");
        header.append("            };\n");
        header.append("        }\n");
        header.append("    }\n\n");
        
        // Test method
        header.append("    @Test\n");
        header.append("    @CSMetaData(\n");
        header.append("        testId = \"").append(testName.toUpperCase().replace("TEST", "")).append("-001\",\n");
        header.append("        description = \"Generated test case\",\n");
        header.append("        severity = CSMetaData.Severity.NORMAL\n");
        header.append("    )\n");
        header.append("    public void executeTest() {\n");
        header.append("        // Start reporting\n");
        header.append("        reporting.startTest(\"Generated test\", \"Auto-generated test using CSTestForge\");\n");
        header.append("        reporting.startStep(\"Test execution\", \"Main test flow\");\n\n");
        header.append("        try {\n");
        
        return header.toString();
    }
    
    /**
     * Generate test class footer for TestNG style
     */
    private String generateTestNGClassFooter() {
        StringBuilder footer = new StringBuilder();
        footer.append("\n            // End test reporting\n");
        footer.append("            reporting.endStep(\"Test execution\", true, 0);\n");
        footer.append("            reporting.endTest(\"Generated test\", true);\n");
        footer.append("        } catch (Exception e) {\n");
        footer.append("            // Log and report failure\n");
        footer.append("            logger.error(\"Test failed: {}\", e.getMessage(), e);\n");
        footer.append("            reporting.endStep(\"Test execution\", false, 0, e);\n");
        footer.append("            reporting.endTest(\"Generated test\", false);\n");
        footer.append("            // Re-throw exception to mark test as failed\n");
        footer.append("            throw e;\n");
        footer.append("        }\n");
        footer.append("    }\n\n");
        
        // Data-driven test method
        footer.append("    /**\n");
        footer.append("     * Data-driven test method\n");
        footer.append("     * \n");
        footer.append("     * @param testData Test data map\n");
        footer.append("     */\n");
        footer.append("    @Test(dataProvider = \"getTestData\")\n");
        footer.append("    @CSMetaData(\n");
        footer.append("        testId = \"").append(testName.toUpperCase().replace("TEST", "")).append("-002\",\n");
        footer.append("        description = \"Data-driven test case\"\n");
        footer.append("    )\n");
        footer.append("    public void executeDataDrivenTest(Map<String, Object> testData) {\n");
        footer.append("        // Extract test data\n");
        footer.append("        String param1 = (String) testData.get(\"param1\");\n");
        footer.append("        String param2 = (String) testData.get(\"param2\");\n\n");
        footer.append("        // Start reporting\n");
        footer.append("        String testName = \"Data-driven test with \" + param1;\n");
        footer.append("        reporting.startTest(testName, \"Data-driven test execution\");\n\n");
        footer.append("        try {\n");
        footer.append("            // Test logic here using page object and parameters\n");
        footer.append("            logger.info(\"Executing data-driven test with parameters: {} and {}\", param1, param2);\n");
        footer.append("            \n");
        footer.append("            // Navigate to page\n");
        footer.append("            page.navigateToPage();\n");
        footer.append("            \n");
        footer.append("            // Example test flow\n");
        footer.append("            // TODO: Update with actual test steps\n");
        footer.append("            \n");
        footer.append("            // End reporting\n");
        footer.append("            reporting.endTest(testName, true);\n");
        footer.append("        } catch (Exception e) {\n");
        footer.append("            logger.error(\"Data-driven test failed: {}\", e.getMessage(), e);\n");
        footer.append("            reporting.endTest(testName, false);\n");
        footer.append("            throw e;\n");
        footer.append("        }\n");
        footer.append("    }\n");
        
        footer.append("}\n");
        
        return footer.toString();
    }
    
    /**
     * Generate class header for BDD style (step definitions)
     */
    private String generateBDDClassHeader() {
        StringBuilder header = new StringBuilder();
        header.append("/**\n");
        header.append(" * Auto-generated step definitions using CSTestForge\n");
        header.append(" */\n");
        header.append("@CSMetaData(\n");
        header.append("    feature = \"Generated Feature\",\n");
        header.append("    description = \"Auto-generated BDD steps\",\n");
        header.append("    authors = {\"CSTestForge Generator\"},\n");
        header.append("    tags = {\"generated\", \"bdd\"}\n");
        header.append(")\n");
        header.append("public class ").append(testName).append("Steps extends CSBaseStepDefinition {\n\n");
        
        header.append("    private static final Logger logger = LoggerFactory.getLogger(").append(testName).append("Steps.class);\n");
        header.append("    private ").append(testName.replace("Steps", "Page")).append(" page;\n\n");
        
        // Constructor
        header.append("    /**\n");
        header.append("     * Constructor initializes the base step definition\n");
        header.append("     */\n");
        header.append("    public ").append(testName).append("Steps() {\n");
        header.append("        super();\n");
        header.append("    }\n\n");
        
        // Custom setup
        header.append("    /**\n");
        header.append("     * Custom setup for these step definitions\n");
        header.append("     */\n");
        header.append("    @Override\n");
        header.append("    protected void customSetUp() {\n");
        header.append("        // Initialize page object using framework method\n");
        header.append("        this.page = initializePage(").append(testName.replace("Steps", "Page")).append(".class);\n");
        header.append("        logger.info(\"Step definition setup complete with page initialization\");\n");
        header.append("    }\n\n");
        
        return header.toString();
    }
    
    /**
     * Generate class footer for BDD style
     */
    private String generateBDDClassFooter() {
        StringBuilder footer = new StringBuilder();
        
        // Custom teardown
        footer.append("    /**\n");
        footer.append("     * Custom teardown for these step definitions\n");
        footer.append("     */\n");
        footer.append("    @Override\n");
        footer.append("    protected void customTearDown() {\n");
        footer.append("        logger.info(\"Step definition teardown complete\");\n");
        footer.append("    }\n\n");
        
        // Helper methods
        footer.append("    /**\n");
        footer.append("     * Store value in scenario context\n");
        footer.append("     * \n");
        footer.append("     * @param key Context key\n");
        footer.append("     * @param value Value to store\n");
        footer.append("     */\n");
        footer.append("    protected void storeInContext(String key, Object value) {\n");
        footer.append("        scenarioContext.set(key, value);\n");
        footer.append("        logger.debug(\"Stored in context: {} = {}\", key, value);\n");
        footer.append("    }\n\n");
        
        footer.append("    /**\n");
        footer.append("     * Get value from scenario context\n");
        footer.append("     * \n");
        footer.append("     * @param key Context key\n");
        footer.append("     * @param clazz Expected value type\n");
        footer.append("     * @return Value from context\n");
        footer.append("     */\n");
        footer.append("    protected <T> T getFromContext(String key, Class<T> clazz) {\n");
        footer.append("        return scenarioContext.get(key, clazz);\n");
        footer.append("    }\n");
        
        footer.append("}\n");
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
        
        code.append(indent).append("// Action: ").append(stepName).append("\n");
        code.append(indent).append("reporting.startStep(\"").append(stepName).append("\", \"").append(event.getActionType()).append(" action\");\n");
        code.append(indent).append("try {\n");
        
        switch (event.getActionType()) {
            case CLICK:
                String clickElementName = formatElementName(event.getTargetSelector());
                code.append(indent).append("    logger.info(\"Clicking element: ").append(clickElementName).append("\");\n");
                code.append(indent).append("    page.").append(formatFieldToMethod(clickElementName)).append("();\n");
                break;
                
            case DOUBLE_CLICK:
                String doubleClickElementName = formatElementName(event.getTargetSelector());
                code.append(indent).append("    logger.info(\"Double-clicking element: ").append(doubleClickElementName).append("\");\n");
                code.append(indent).append("    interactionHandler.doubleClick(page.").append(formatElementReference(doubleClickElementName)).append(");\n");
                break;
                
            case TYPE:
                String typeElementName = formatElementName(event.getTargetSelector());
                code.append(indent).append("    logger.info(\"Typing into element: ").append(typeElementName)
                        .append(" value: ").append(event.getValue()).append("\");\n");
                code.append(indent).append("    page.enter").append(formatMethodName(typeElementName))
                        .append("(\"").append(event.getValue()).append("\");\n");
                break;
                
            case NAVIGATE:
                code.append(indent).append("    logger.info(\"Navigating to: ").append(event.getValue()).append("\");\n");
                code.append(indent).append("    page.navigateToPage();\n");
                break;
                
            case WAIT:
                String waitElementName = formatElementName(event.getTargetSelector());
                code.append(indent).append("    logger.info(\"Waiting for element: ").append(waitElementName).append("\");\n");
                code.append(indent).append("    interactionHandler.waitForElementVisible(page.").append(formatElementReference(waitElementName)).append(");\n");
                break;
                
            case SELECT:
                String selectElementName = formatElementName(event.getTargetSelector());
                code.append(indent).append("    logger.info(\"Selecting option: ").append(event.getValue()).append("\");\n");
                code.append(indent).append("    interactionHandler.selectByVisibleText(page.").append(formatElementReference(selectElementName))
                        .append(", \"").append(event.getValue()).append("\");\n");
                break;
                
            default:
                code.append(indent).append("    // Unsupported action type: ").append(event.getActionType()).append("\n");
                code.append(indent).append("    logger.warning(\"Unsupported action type: ").append(event.getActionType()).append("\");\n");
        }
        
        code.append(indent).append("    reporting.endStep(\"").append(stepName).append("\", true, 0);\n");
        code.append(indent).append("} catch (Exception e) {\n");
        code.append(indent).append("    logger.error(\"Step failed: ").append(stepName).append("\", e);\n");
        code.append(indent).append("    reporting.endStep(\"").append(stepName).append("\", false, 0, e);\n");
        code.append(indent).append("    throw e;\n");
        code.append(indent).append("}");
        
        return code.toString();
    }
    
    /**
     * Generate action code for BDD style
     */
    private String generateActionForBDD(ActionEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = " ".repeat(indentLevel * 4);
        
        String stepType = "Given";
        // Always use "When" for non-navigation actions
        if (event.getActionType() != ActionEvent.ActionType.NAVIGATE) {
            stepType = "When";
        }
        
        // Generate context string based on action
        String stepDescription = "";
        String stepName = "";
        String methodName = "";
        String targetElement = formatElementName(event.getTargetSelector());
        
        switch (event.getActionType()) {
            case CLICK:
                stepDescription = "the user clicks on the " + targetElement;
                stepName = "theUserClicksOn" + formatMethodName(targetElement);
                methodName = "click" + formatMethodName(targetElement);
                break;
            case TYPE:
                stepDescription = "the user enters {string} in the " + targetElement;
                stepName = "theUserEntersTextIn" + formatMethodName(targetElement);
                methodName = "enter" + formatMethodName(targetElement);
                break;
            case NAVIGATE:
                stepDescription = "the user navigates to the " + testName.replace("Steps", "") + " page";
                stepName = "theUserNavigatesToThe" + testName.replace("Steps", "") + "Page";
                methodName = "navigateToPage";
                break;
            default:
                stepDescription = "the user performs an action on the " + targetElement;
                stepName = "theUserPerformsActionOn" + formatMethodName(targetElement);
                methodName = "performActionOn" + formatMethodName(targetElement);
        }
        
        // Generate step method with CSTestStep annotation
        code.append(indent).append("@").append(stepType).append("(\"").append(stepDescription).append("\")\n");
        code.append(indent).append("@CSTestStep(description = \"").append(stepDescription).append("\", screenshot = true)\n");
        
        if (event.getActionType() == ActionEvent.ActionType.TYPE) {
            // For type actions, add a parameter
            code.append(indent).append("public void ").append(stepName).append("(String text) {\n");
            code.append(indent).append("    logger.info(\"").append(stepDescription.replace("{string}", "{}")).append("\", text);\n");
            code.append(indent).append("    page.").append(methodName).append("(text);\n");
        } else {
            code.append(indent).append("public void ").append(stepName).append("() {\n");
            code.append(indent).append("    logger.info(\"").append(stepDescription).append("\");\n");
            code.append(indent).append("    page.").append(methodName).append("();\n");
        }
        
        // Add screenshot option
        code.append(indent).append("    // Take screenshot if needed\n");
        code.append(indent).append("    scenarioContext.set(\"SCREENSHOT_AFTER_STEP\", true);\n");
        
        code.append(indent).append("}\n");
        
        return code.toString();
    }
    
    /**
     * Format element name from target selector
     * @param target Target selector or element name
     * @return Formatted element name
     */
    private String formatElementName(String target) {
        if (target == null || target.isEmpty()) {
            return "element";
        }
        
        String elementName = target;
        // Remove CSS selector syntax if present
        if (elementName.startsWith("#") || elementName.startsWith(".") || elementName.startsWith("[")) {
            elementName = elementName.replaceAll("[#.\\[\\]=\"']", "");
        }
        
        // Convert to camel case
        String[] parts = elementName.split("[-_\\s]");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].length() > 0) {
                result.append(parts[i].substring(0, 1).toUpperCase())
                      .append(parts[i].substring(1).toLowerCase());
            }
        }
        
        // Add suffix based on usage if not present
        if (!result.toString().contains("button") && !result.toString().contains("field") &&
            !result.toString().contains("input") && !result.toString().contains("link")) {
            result.append("Element");
        }
        
        return result.toString();
    }
    
    /**
     * Format element reference for use in code
     * @param elementName Element name
     * @return Element reference name
     */
    private String formatElementReference(String elementName) {
        return elementName;
    }
    
    /**
     * Format method name from element name
     * @param elementName Element name
     * @return Method name
     */
    private String formatMethodName(String elementName) {
        if (elementName == null || elementName.isEmpty()) {
            return "Element";
        }
        
        return elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
    }
    
    @Override
    public String generateAssertion(AssertionEvent event, int indentLevel) {
        if (style == GenerationStyle.BDD) {
            return generateAssertionForBDD(event, indentLevel);
        }
        
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Assertion: ").append(event.getName()).append("\n");
        code.append(indent).append("@CSTestStep(description = \"Assertion: ").append(event.getName()).append("\")\n");
        code.append(indent).append("{\n");
        code.append(indent).append("    reporting.startStep(\"Assertion: ").append(event.getName()).append("\");\n");
        code.append(generateActualAssertion(event, indentLevel + 1)).append("\n");
        code.append(indent).append("    reporting.endStep(\"Assertion: ").append(event.getName()).append("\", true);\n");
        code.append(indent).append("}");
        
        return code.toString();
    }
    
    /**
     * Generate assertion code for BDD style
     */
    private String generateAssertionForBDD(AssertionEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = " ".repeat(indentLevel * 4);
        
        String stepDescription = "";
        String stepName = "";
        // Extract target from actual expression or default to "element"
        String targetElement;
        try {
            targetElement = formatElementName(event.getActualExpression());
        } catch (Exception e) {
            targetElement = "element";
        }
        
        String assertionType = event.getAssertType().toString();
        
        // Handle different assertion types
        if ("EQUALS".equalsIgnoreCase(assertionType)) {
            stepDescription = "the " + targetElement + " should be {string}";
            stepName = "the" + formatMethodName(targetElement) + "ShouldBe";
        } else if ("CONTAINS".equalsIgnoreCase(assertionType)) {
            stepDescription = "the " + targetElement + " should contain {string}";
            stepName = "the" + formatMethodName(targetElement) + "ShouldContain";
        } else if ("ELEMENT_PRESENT".equalsIgnoreCase(assertionType)) {
            stepDescription = "the " + targetElement + " should be displayed";
            stepName = "the" + formatMethodName(targetElement) + "ShouldBeDisplayed";
        } else if ("ELEMENT_NOT_PRESENT".equalsIgnoreCase(assertionType)) {
            stepDescription = "the " + targetElement + " should not be displayed";
            stepName = "the" + formatMethodName(targetElement) + "ShouldNotBeDisplayed";
        } else {
            stepDescription = "the " + targetElement + " should match the expectation";
            stepName = "the" + formatMethodName(targetElement) + "ShouldMatchExpectation";
        }
        
        // Generate step method with CSTestStep annotation
        code.append(indent).append("@Then(\"").append(stepDescription).append("\")\n");
        code.append(indent).append("@CSTestStep(description = \"").append(stepDescription).append("\", screenshot = true)\n");
        
        if ("EQUALS".equalsIgnoreCase(assertionType) || "CONTAINS".equalsIgnoreCase(assertionType)) {
            // For assertions with expected values
            code.append(indent).append("public void ").append(stepName).append("(String expectedValue) {\n");
            code.append(indent).append("    logger.info(\"").append(stepDescription.replace("{string}", "{}")).append("\", expectedValue);\n");
            
            if ("EQUALS".equalsIgnoreCase(assertionType)) {
                code.append(indent).append("    String actualValue = page.get").append(formatMethodName(targetElement)).append("Text();\n");
                code.append(indent).append("    Assert.assertEquals(actualValue, expectedValue, \"").append(targetElement).append(" text does not match expected value\");\n");
            } else {
                code.append(indent).append("    String actualValue = page.get").append(formatMethodName(targetElement)).append("Text();\n");
                code.append(indent).append("    Assert.assertTrue(actualValue.contains(expectedValue), \"").append(targetElement).append(" text does not contain expected value\");\n");
            }
        } else {
            // For display assertions
            code.append(indent).append("public void ").append(stepName).append("() {\n");
            code.append(indent).append("    logger.info(\"").append(stepDescription).append("\");\n");
            
            if ("ELEMENT_PRESENT".equalsIgnoreCase(assertionType)) {
                code.append(indent).append("    Assert.assertTrue(page.is").append(formatMethodName(targetElement)).append("Displayed(), \"").append(targetElement).append(" is not displayed\");\n");
            } else if ("ELEMENT_NOT_PRESENT".equalsIgnoreCase(assertionType)) {
                code.append(indent).append("    Assert.assertFalse(page.is").append(formatMethodName(targetElement)).append("Displayed(), \"").append(targetElement).append(" is displayed when it should not be\");\n");
            }
        }
        
        // Store result in scenario context
        code.append(indent).append("    // Store result in context\n");
        code.append(indent).append("    storeInContext(\"").append(targetElement.toUpperCase()).append("_ASSERTION_RESULT\", true);\n");
        
        code.append(indent).append("}\n");
        
        return code.toString();
    }
    
    /**
     * Generate the actual assertion code
     */
    private String generateActualAssertion(AssertionEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        switch (event.getAssertType()) {
            case EQUALS:
                code.append(indent).append("Assert.assertEquals(").append(event.getActualExpression())
                    .append(", ").append(event.getExpectedExpression());
                if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                    code.append(", \"").append(event.getMessage()).append("\"");
                }
                code.append(");");
                break;
            case NOT_EQUALS:
                code.append(indent).append("Assert.assertNotEquals(").append(event.getActualExpression())
                    .append(", ").append(event.getExpectedExpression());
                if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                    code.append(", \"").append(event.getMessage()).append("\"");
                }
                code.append(");");
                break;
            case TRUE:
                code.append(indent).append("Assert.assertTrue(").append(event.getActualExpression());
                if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                    code.append(", \"").append(event.getMessage()).append("\"");
                }
                code.append(");");
                break;
            case FALSE:
                code.append(indent).append("Assert.assertFalse(").append(event.getActualExpression());
                if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                    code.append(", \"").append(event.getMessage()).append("\"");
                }
                code.append(");");
                break;
            case ELEMENT_PRESENT:
                code.append(indent).append("Assert.assertTrue(page.isElementPresent(By.cssSelector(\"")
                    .append(event.getActualExpression()).append("\"))");
                if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                    code.append(", \"").append(event.getMessage()).append("\"");
                }
                code.append(");");
                break;
            case ELEMENT_NOT_PRESENT:
                code.append(indent).append("Assert.assertFalse(page.isElementPresent(By.cssSelector(\"")
                    .append(event.getActualExpression()).append("\"))");
                if (event.getMessage() != null && !event.getMessage().isEmpty()) {
                    code.append(", \"").append(event.getMessage()).append("\"");
                }
                code.append(");");
                break;
            default:
                code.append(indent).append("// Unsupported assertion type: ").append(event.getAssertType());
        }
        
        return code.toString();
    }
    
    /**
     * Generate page object class code following CSTestForge framework conventions
     * 
     * @param pageName Name of the page
     * @param elements Map of element names to locators
     * @return Generated page object class code
     */
    public String generatePageObject(String pageName, Map<String, String> elements) {
        StringBuilder pageObjectBuilder = new StringBuilder();
        
        // Package and imports
        pageObjectBuilder.append("package ").append(packageName).append(".page;\n\n");
        
        pageObjectBuilder.append("import org.openqa.selenium.WebElement;\n");
        pageObjectBuilder.append("import org.openqa.selenium.support.FindBy;\n");
        pageObjectBuilder.append("import org.openqa.selenium.support.PageFactory;\n");
        pageObjectBuilder.append("import org.slf4j.Logger;\n");
        pageObjectBuilder.append("import org.slf4j.LoggerFactory;\n\n");
        
        // Import CSTestForge framework classes
        pageObjectBuilder.append("import com.cstestforge.framework.selenium.annotation.CSFindBy;\n");
        pageObjectBuilder.append("import com.cstestforge.framework.selenium.annotation.CSMetaData;\n");
        pageObjectBuilder.append("import com.cstestforge.framework.selenium.annotation.CSTestStep;\n");
        pageObjectBuilder.append("import com.cstestforge.framework.selenium.core.CSDriverManager;\n");
        pageObjectBuilder.append("import com.cstestforge.framework.selenium.element.CSElementInteractionHandler;\n");
        pageObjectBuilder.append("import com.cstestforge.framework.selenium.page.CSBasePage;\n\n");
        
        // Class comment
        pageObjectBuilder.append("/**\n");
        pageObjectBuilder.append(" * Page object for ").append(pageName).append(" using CSTestForge framework.\n");
        pageObjectBuilder.append(" * Generated by CSTestForge Generator\n");
        pageObjectBuilder.append(" */\n");
        
        // CSMetaData annotation
        pageObjectBuilder.append("@CSMetaData(\n");
        pageObjectBuilder.append("    description = \"Page object for ").append(pageName).append("\",\n");
        pageObjectBuilder.append("    authors = {\"CSTestForge Generator\"}\n");
        pageObjectBuilder.append(")\n");
        
        // Class declaration extending CSBasePage
        pageObjectBuilder.append("public class ").append(pageName).append(" extends CSBasePage {\n");
        pageObjectBuilder.append("    private static final Logger logger = LoggerFactory.getLogger(").append(pageName).append(".class);\n\n");
        
        // Add element interaction handler
        pageObjectBuilder.append("    // Element interaction handler for robust interactions\n");
        pageObjectBuilder.append("    private final CSElementInteractionHandler interactionHandler;\n\n");
        
        // Page URL
        pageObjectBuilder.append("    // Page URL\n");
        pageObjectBuilder.append("    private static final String PAGE_URL = \"https://example.com/")
                .append(pageName.toLowerCase().replace("page", ""))
                .append("\";\n\n");
        
        // Elements
        pageObjectBuilder.append("    // Page elements with CSFindBy annotation for self-healing\n");
        for (Map.Entry<String, String> element : elements.entrySet()) {
            String fieldName = element.getKey();
            String locator = element.getValue();
            
            // Parse locator to determine type and value
            String locatorType;
            String locatorValue;
            
            if (locator.startsWith("id=")) {
                locatorType = "id";
                locatorValue = locator.substring(3);
            } else if (locator.startsWith("css=")) {
                locatorType = "css";
                locatorValue = locator.substring(4);
            } else if (locator.startsWith("xpath=")) {
                locatorType = "xpath";
                locatorValue = locator.substring(6);
            } else if (locator.startsWith("name=")) {
                locatorType = "name";
                locatorValue = locator.substring(5);
            } else {
                // Default to id
                locatorType = "id";
                locatorValue = locator;
            }
            
            // Generate CSFindBy with multiple locators for self-healing
            pageObjectBuilder.append("    @CSFindBy(").append(locatorType).append(" = \"").append(locatorValue).append("\", orLocators = {\n");
            
            // Add alternative locators based on the primary locator type
            if ("id".equals(locatorType)) {
                pageObjectBuilder.append("            @FindBy(css = \"#").append(locatorValue).append("\"),\n");
                pageObjectBuilder.append("            @FindBy(xpath = \"//*[@id='").append(locatorValue).append("']\")\n");
            } else if ("css".equals(locatorType)) {
                pageObjectBuilder.append("            @FindBy(xpath = \"//").append(fieldName.replace("Field", "")).append("\"),\n");
                pageObjectBuilder.append("            @FindBy(name = \"").append(fieldName.replace("Field", "")).append("\")\n");
            } else if ("xpath".equals(locatorType)) {
                pageObjectBuilder.append("            @FindBy(css = \"[name='").append(fieldName.replace("Field", "")).append("']\"),\n");
                pageObjectBuilder.append("            @FindBy(id = \"").append(fieldName.replace("Field", "")).append("\")\n");
            } else {
                pageObjectBuilder.append("            @FindBy(css = \"[name='").append(locatorValue).append("']\"),\n");
                pageObjectBuilder.append("            @FindBy(xpath = \"//*[@name='").append(locatorValue).append("']\")\n");
            }
            
            pageObjectBuilder.append("    })\n");
            pageObjectBuilder.append("    private WebElement ").append(fieldName).append(";\n\n");
        }
        
        // Constructor
        pageObjectBuilder.append("    /**\n");
        pageObjectBuilder.append("     * Constructor\n");
        pageObjectBuilder.append("     */\n");
        pageObjectBuilder.append("    public ").append(pageName).append("() {\n");
        pageObjectBuilder.append("        super();\n");
        pageObjectBuilder.append("        // Initialize page elements\n");
        pageObjectBuilder.append("        PageFactory.initElements(CSDriverManager.getDriver(), this);\n");
        pageObjectBuilder.append("        this.interactionHandler = new CSElementInteractionHandler();\n");
        pageObjectBuilder.append("        \n");
        pageObjectBuilder.append("        // Set page URL in base class\n");
        pageObjectBuilder.append("        setPageUrl(PAGE_URL);\n");
        pageObjectBuilder.append("    }\n\n");
        
        // Navigate method
        pageObjectBuilder.append("    /**\n");
        pageObjectBuilder.append("     * Navigate to the page\n");
        pageObjectBuilder.append("     * \n");
        pageObjectBuilder.append("     * @return This page object\n");
        pageObjectBuilder.append("     */\n");
        pageObjectBuilder.append("    @CSTestStep(description = \"Navigate to ").append(pageName).append("\", screenshot = true)\n");
        pageObjectBuilder.append("    public ").append(pageName).append(" navigateToPage() {\n");
        pageObjectBuilder.append("        logger.info(\"Navigating to page: {}\", PAGE_URL);\n");
        pageObjectBuilder.append("        super.navigateToPage();\n");
        pageObjectBuilder.append("        return this;\n");
        pageObjectBuilder.append("    }\n\n");
        
        // Generate methods for each element
        for (Map.Entry<String, String> element : elements.entrySet()) {
            String fieldName = element.getKey();
            String methodName = formatFieldToMethod(fieldName);
            String description = formatFieldToDescription(fieldName);
            
            // Is this a button or a field?
            boolean isButton = fieldName.toLowerCase().contains("button") || fieldName.toLowerCase().contains("btn");
            boolean isInput = fieldName.toLowerCase().contains("field") || fieldName.toLowerCase().contains("input");
            
            if (isButton) {
                // Click method
                pageObjectBuilder.append("    /**\n");
                pageObjectBuilder.append("     * Click the ").append(description).append("\n");
                pageObjectBuilder.append("     * \n");
                pageObjectBuilder.append("     * @return This page object\n");
                pageObjectBuilder.append("     */\n");
                pageObjectBuilder.append("    @CSTestStep(description = \"Click ").append(description).append("\", screenshot = true)\n");
                pageObjectBuilder.append("    @CSMetaData(description = \"Clicks the ").append(description).append("\")\n");
                pageObjectBuilder.append("    public ").append(pageName).append(" ").append(methodName).append("() {\n");
                pageObjectBuilder.append("        logger.info(\"Clicking ").append(description).append("\");\n");
                pageObjectBuilder.append("        interactionHandler.click(").append(fieldName).append(");\n");
                pageObjectBuilder.append("        return this;\n");
                pageObjectBuilder.append("    }\n\n");
            }
            
            if (isInput) {
                // Enter text method
                pageObjectBuilder.append("    /**\n");
                pageObjectBuilder.append("     * Enter text in the ").append(description).append("\n");
                pageObjectBuilder.append("     * \n");
                pageObjectBuilder.append("     * @param text Text to enter\n");
                pageObjectBuilder.append("     * @return This page object\n");
                pageObjectBuilder.append("     */\n");
                pageObjectBuilder.append("    @CSTestStep(description = \"Enter text in ").append(description).append("\")\n");
                pageObjectBuilder.append("    @CSMetaData(description = \"Enters text in the ").append(description).append("\")\n");
                pageObjectBuilder.append("    public ").append(pageName).append(" enter").append(methodName.substring(0, 1).toUpperCase()).append(methodName.substring(1)).append("(String text) {\n");
                pageObjectBuilder.append("        logger.info(\"Entering text in ").append(description).append("\");\n");
                pageObjectBuilder.append("        interactionHandler.sendKeys(").append(fieldName).append(", new CharSequence[]{text}, true);\n");
                pageObjectBuilder.append("        return this;\n");
                pageObjectBuilder.append("    }\n\n");
            }
            
            // Get text method for input fields and other elements
            pageObjectBuilder.append("    /**\n");
            pageObjectBuilder.append("     * Get text from the ").append(description).append("\n");
            pageObjectBuilder.append("     * \n");
            pageObjectBuilder.append("     * @return Text from the element\n");
            pageObjectBuilder.append("     */\n");
            pageObjectBuilder.append("    public String get").append(methodName.substring(0, 1).toUpperCase()).append(methodName.substring(1)).append("Text() {\n");
            pageObjectBuilder.append("        return interactionHandler.getText(").append(fieldName).append(");\n");
            pageObjectBuilder.append("    }\n\n");
            
            // Is displayed method
            pageObjectBuilder.append("    /**\n");
            pageObjectBuilder.append("     * Check if the ").append(description).append(" is displayed\n");
            pageObjectBuilder.append("     * \n");
            pageObjectBuilder.append("     * @return true if the element is displayed\n");
            pageObjectBuilder.append("     */\n");
            pageObjectBuilder.append("    public boolean is").append(methodName.substring(0, 1).toUpperCase()).append(methodName.substring(1)).append("Displayed() {\n");
            pageObjectBuilder.append("        return isElementDisplayed(").append(fieldName).append(");\n");
            pageObjectBuilder.append("    }\n\n");
        }
        
        // Check if page is displayed
        pageObjectBuilder.append("    /**\n");
        pageObjectBuilder.append("     * Check if the page is displayed\n");
        pageObjectBuilder.append("     * \n");
        pageObjectBuilder.append("     * @return true if the page is displayed\n");
        pageObjectBuilder.append("     */\n");
        pageObjectBuilder.append("    public boolean isPageDisplayed() {\n");
        pageObjectBuilder.append("        return isPageLoaded();\n");
        pageObjectBuilder.append("    }\n");
        
        // Close class
        pageObjectBuilder.append("}\n");
        
        return pageObjectBuilder.toString();
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
                code.append(indent).append("for (").append(event.getIteratorVariable())
                    .append(" : ").append(event.getIterableExpression()).append(") {\n");
                break;
        }
        
        code.append(generateEventsCode(event.getLoopEvents(), indentLevel + 1));
        code.append("\n").append(indent).append("}");
        
        return code.toString();
    }
    
    @Override
    public String generateCapture(CaptureEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        String resultVar = event.getResultVariable();
        
        code.append(indent).append("// Capture: ").append(event.getName()).append("\n");
        code.append(indent).append("@CSTestStep(description = \"Capture: ").append(event.getName()).append("\")\n");
        code.append(indent).append("{\n");
        
        switch (event.getCaptureType()) {
            case TEXT:
                code.append(indent).append("    String ").append(resultVar).append(" = page.findElementByCssSelector(\"")
                    .append(event.getTargetSelector()).append("\").getText();\n");
                code.append(indent).append("    reporting.info(\"Captured text: \" + ").append(resultVar).append(");");
                break;
            case ATTRIBUTE:
                code.append(indent).append("    String ").append(resultVar).append(" = page.findElementByCssSelector(\"")
                    .append(event.getTargetSelector()).append("\").getAttribute(\"")
                    .append(event.getTargetAttribute()).append("\");\n");
                code.append(indent).append("    reporting.info(\"Captured attribute: \" + ").append(resultVar).append(");");
                break;
            case VALUE:
                code.append(indent).append("    String ").append(resultVar).append(" = page.findElementByCssSelector(\"")
                    .append(event.getTargetSelector()).append("\").getAttribute(\"value\");\n");
                code.append(indent).append("    reporting.info(\"Captured value: \" + ").append(resultVar).append(");");
                break;
            case SCREENSHOT:
                code.append(indent).append("    String screenshotPath = takeScreenshot(\"")
                    .append(event.getName().replaceAll("\\s+", "_")).append("\");\n");
                code.append(indent).append("    reporting.addScreenshot(\"Captured screenshot\", screenshotPath);");
                break;
            default:
                code.append(indent).append("    // Unsupported capture type: ").append(event.getCaptureType());
        }
        
        code.append("\n").append(indent).append("}");
        
        return code.toString();
    }
    
    @Override
    public String generateGroup(GroupEvent event, int indentLevel) {
        StringBuilder code = new StringBuilder();
        String indent = indent(indentLevel);
        
        code.append(indent).append("// Group: ").append(event.getName()).append("\n");
        code.append(indent).append("@CSTestStep(description = \"").append(event.getName()).append("\")\n");
        code.append(indent).append("{\n");
        code.append(indent).append("    reporting.startStep(\"").append(event.getName()).append("\");\n");
        
        // Generate code for each event in the group
        code.append(generateEventsCode(event.getEvents(), indentLevel + 1));
        
        code.append("\n").append(indent).append("    reporting.endStep(\"").append(event.getName()).append("\", true);\n");
        code.append(indent).append("}");
        
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
            code.append(" catch (").append(catchBlock.getKey()).append(" e) {\n");
            code.append(indent).append("    reporting.error(\"Exception caught: \" + e.getMessage(), e);\n");
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
    
    @Override
    protected String generateImports() {
        return generatePackageAndImports();
    }
    
    @Override
    protected String generateClassHeader() {
        return generateTestNGClassHeader();
    }
    
    @Override
    protected String generateClassFooter() {
        return generateTestNGClassFooter();
    }
    
    // Helper method to format field name
    private String formatFieldName(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return "";
        }
        
        // Replace underscores with spaces, then capitalize each word and remove spaces
        String[] words = fieldName.split("_");
        StringBuilder result = new StringBuilder();
        
        for (String word : words) {
            if (word != null && !word.isEmpty()) {
                result.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
            }
        }
        
        return result.toString();
    }
} 