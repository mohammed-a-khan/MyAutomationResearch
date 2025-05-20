package com.cstestforge.export.service.framework;

import com.cstestforge.export.model.ExportRequest;
import com.cstestforge.export.model.ProjectTemplate;
import com.cstestforge.project.service.ProjectService;
import com.cstestforge.testing.model.TestCase;
import com.cstestforge.testing.service.TestCaseService;
import com.cstestforge.codegen.model.GeneratedCode;
import com.cstestforge.codegen.service.CodeGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exporter for Playwright TypeScript framework
 */
@Component
public class PlaywrightTypeScriptExporter implements FrameworkExporter {
    private static final Logger logger = LoggerFactory.getLogger(PlaywrightTypeScriptExporter.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TestCaseService testCaseService;
    
    @Autowired
    private CodeGenerationService codeGenerationService;

    @Override
    public ProjectTemplate initializeTemplate(ExportRequest request) {
        // Create project template
        ProjectTemplate template = new ProjectTemplate(
                "playwright-typescript-export",
                "playwright",
                "typescript",
                request.getBuildTool()
        );
        
        // Set up directory structure
        template.addDirectory("src/pages");
        template.addDirectory("src/utils");
        template.addDirectory("tests");
        template.addDirectory("tests/fixtures");
        
        // BDD structure if needed
        if (request.isGenerateBDD()) {
            template.addDirectory("tests/steps");
            template.addDirectory("features");
        }
        
        // Config and other directories
        template.addDirectory("config");
        template.addDirectory("test-output");
        template.addDirectory("node_modules");
        
        // Add npm configuration
        template.addConfigFile("package.json", generatePackageJson(request));
        template.addConfigFile("tsconfig.json", generateTsConfig());
        template.addConfigFile("playwright.config.ts", generatePlaywrightConfig(request));
        
        return template;
    }

    @Override
    public Map<String, String> generateFiles(ExportRequest request) throws IOException {
        Map<String, String> files = new HashMap<>();
        
        // Copy framework files from cstestforge-playwright
        copyFrameworkFiles(files);
        
        // Generate test files based on recorded tests
        generateTestFiles(files, request);
        
        // Add additional config files and documentation
        files.put("README.md", generateReadme(request));
        files.put(".gitignore", generateGitIgnore());
        
        return files;
    }

    @Override
    public byte[] createExportPackage(ExportRequest request) throws IOException {
        // Initialize project template
        ProjectTemplate template = initializeTemplate(request);
        
        // Generate files
        Map<String, String> generatedFiles = generateFiles(request);
        
        // Create ZIP file
        return createZipPackage(template, generatedFiles);
    }
    
    /**
     * Generate package.json file
     * 
     * @param request Export request
     * @return JSON content as string
     */
    private String generatePackageJson(ExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
          .append("  \"name\": \"playwright-tests\",\n")
          .append("  \"version\": \"1.0.0\",\n")
          .append("  \"description\": \"Generated Playwright Tests using CSTestForge\",\n")
          .append("  \"scripts\": {\n")
          .append("    \"test\": \"npx playwright test\",\n")
          .append("    \"test:headed\": \"npx playwright test --headed\",\n")
          .append("    \"report\": \"npx playwright show-report\",\n");
        
        if (request.isGenerateBDD()) {
            sb.append("    \"test:bdd\": \"cucumber-js\",\n");
        }
        
        sb.append("    \"build\": \"tsc -p tsconfig.json\",\n")
          .append("    \"lint\": \"eslint . --ext .ts\"\n")
          .append("  },\n")
          .append("  \"keywords\": [\n")
          .append("    \"playwright\",\n")
          .append("    \"testing\",\n")
          .append("    \"automation\",\n")
          .append("    \"cstestforge\"\n")
          .append("  ],\n")
          .append("  \"dependencies\": {\n")
          .append("    \"@playwright/test\": \"^1.38.1\",\n")
          .append("    \"dotenv\": \"^16.3.1\"\n")
          .append("  },\n")
          .append("  \"devDependencies\": {\n")
          .append("    \"@types/node\": \"^20.6.0\",\n")
          .append("    \"typescript\": \"^5.2.2\",\n")
          .append("    \"ts-node\": \"^10.9.1\",\n");
        
        if (request.isGenerateBDD()) {
            sb.append("    \"@cucumber/cucumber\": \"^9.5.1\",\n")
              .append("    \"@types/cucumber\": \"^7.0.0\",\n");
        }
        
        sb.append("    \"eslint\": \"^8.49.0\",\n")
          .append("    \"@typescript-eslint/eslint-plugin\": \"^6.6.0\",\n")
          .append("    \"@typescript-eslint/parser\": \"^6.6.0\"\n")
          .append("  },\n")
          .append("  \"engines\": {\n")
          .append("    \"node\": \">=16.0.0\"\n")
          .append("  }\n")
          .append("}\n");
        
        return sb.toString();
    }
    
    /**
     * Generate TypeScript configuration
     * 
     * @return tsconfig.json content
     */
    private String generateTsConfig() {
        return "{\n" +
                "  \"compilerOptions\": {\n" +
                "    \"target\": \"ES2022\",\n" +
                "    \"module\": \"commonjs\",\n" +
                "    \"moduleResolution\": \"node\",\n" +
                "    \"sourceMap\": true,\n" +
                "    \"outDir\": \"./dist\",\n" +
                "    \"esModuleInterop\": true,\n" +
                "    \"strict\": true,\n" +
                "    \"resolveJsonModule\": true\n" +
                "  },\n" +
                "  \"include\": [\"src/**/*.ts\", \"tests/**/*.ts\"],\n" +
                "  \"exclude\": [\"node_modules\"]\n" +
                "}\n";
    }
    
    /**
     * Generate Playwright configuration
     * 
     * @param request Export request
     * @return playwright.config.ts content
     */
    private String generatePlaywrightConfig(ExportRequest request) {
        return "import { PlaywrightTestConfig, devices } from '@playwright/test';\n" +
                "import dotenv from 'dotenv';\n\n" +
                "// Read environment variables from .env file\n" +
                "dotenv.config();\n\n" +
                "const config: PlaywrightTestConfig = {\n" +
                "  testDir: './tests',\n" +
                "  timeout: 60000,\n" +
                "  forbidOnly: !!process.env.CI,\n" +
                "  retries: process.env.CI ? 2 : 0,\n" +
                "  workers: process.env.CI ? 1 : undefined,\n" +
                "  reporter: [\n" +
                "    ['html'],\n" +
                "    ['json', { outputFile: 'test-results/test-results.json' }],\n" +
                "    ['junit', { outputFile: 'test-results/junit.xml' }]\n" +
                "  ],\n" +
                "  use: {\n" +
                "    headless: true,\n" +
                "    viewport: { width: 1280, height: 720 },\n" +
                "    ignoreHTTPSErrors: true,\n" +
                "    screenshot: 'only-on-failure',\n" +
                "    video: 'retain-on-failure',\n" +
                "    trace: 'retain-on-failure'\n" +
                "  },\n" +
                "  projects: [\n" +
                "    {\n" +
                "      name: 'chromium',\n" +
                "      use: { ...devices['Desktop Chrome'] },\n" +
                "    },\n" +
                "    {\n" +
                "      name: 'firefox',\n" +
                "      use: { ...devices['Desktop Firefox'] },\n" +
                "    },\n" +
                "    {\n" +
                "      name: 'webkit',\n" +
                "      use: { ...devices['Desktop Safari'] },\n" +
                "    }\n" +
                "  ],\n" +
                "  outputDir: 'test-output/'\n" +
                "};\n\n" +
                "export default config;\n";
    }
    
    /**
     * Generate .gitignore file
     * 
     * @return .gitignore content
     */
    private String generateGitIgnore() {
        return "node_modules/\n" +
                "dist/\n" +
                "test-output/\n" +
                "test-results/\n" +
                "playwright-report/\n" +
                ".env\n" +
                "*.log\n" +
                "npm-debug.log*\n" +
                "yarn-debug.log*\n" +
                "yarn-error.log*\n" +
                ".DS_Store\n";
    }
    
    /**
     * Generate README file
     * 
     * @param request Export request
     * @return Markdown content
     */
    private String generateReadme(ExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CSTestForge Playwright Test Suite\n\n")
          .append("This project contains automatically generated Playwright tests using CSTestForge.\n\n")
          .append("## Framework Details\n\n")
          .append("- **Framework**: Playwright\n")
          .append("- **Language**: TypeScript\n");
        
        if (request.isGenerateBDD()) {
            sb.append("- **BDD Framework**: Cucumber\n");
        }
        
        sb.append("\n## Project Structure\n\n")
          .append("- `src/` - Page objects and utilities\n")
          .append("- `tests/` - Test cases and fixtures\n");
        
        if (request.isGenerateBDD()) {
            sb.append("- `tests/steps/` - Step definitions for BDD tests\n")
              .append("- `features/` - Cucumber feature files\n");
        }
        
        sb.append("- `config/` - Configuration files\n")
          .append("- `playwright.config.ts` - Playwright configuration\n")
          .append("- `test-output/` - Test reports and screenshots\n\n")
          .append("## Setup\n\n")
          .append("```bash\n")
          .append("# Install dependencies\n")
          .append("npm install\n\n")
          .append("# Install browsers\n")
          .append("npx playwright install\n")
          .append("```\n\n")
          .append("## Running Tests\n\n")
          .append("```bash\n")
          .append("# Run all tests\n")
          .append("npm test\n\n")
          .append("# Run tests in headed mode\n")
          .append("npm run test:headed\n\n");
        
        if (request.isGenerateBDD()) {
            sb.append("# Run BDD tests\n")
              .append("npm run test:bdd\n\n");
        }
        
        sb.append("# View test report\n")
          .append("npm run report\n")
          .append("```\n\n")
          .append("## Configuration\n\n")
          .append("You can configure test behavior by editing `playwright.config.ts` or by setting environment variables in a `.env` file.\n\n");
        
        return sb.toString();
    }
    
    /**
     * Copy framework files from source to export package
     * 
     * @param files Map to store generated files
     * @throws IOException if file operations fail
     */
    private void copyFrameworkFiles(Map<String, String> files) throws IOException {
        Path frameworkPath = Paths.get("src/main/typescript/cstestforge-playwright");
        try (Stream<Path> paths = Files.walk(frameworkPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(path -> {
                     String fileName = path.getFileName().toString();
                     // Skip node_modules, package files, and other unnecessary files
                     return !path.toString().contains("node_modules") && 
                            !fileName.equals("package.json") && 
                            !fileName.equals("package-lock.json") && 
                            !fileName.equals("playwright.config.ts") &&
                            !fileName.equals("tsconfig.json") &&
                            !fileName.endsWith(".log") &&
                            !path.toString().contains("test-output");
                 })
                 .forEach(path -> {
                     try {
                         // Get relative path from framework directory
                         Path relativePath = frameworkPath.relativize(path);
                         String targetPath = relativePath.toString().replace('\\', '/');
                         
                         // Read file content
                         String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                         
                         // Store in files map
                         files.put(targetPath, content);
                     } catch (IOException e) {
                         logger.error("Failed to process framework file: " + path, e);
                     }
                 });
        }
    }
    
    /**
     * Generate test files based on recorded tests
     * 
     * @param files Map to store generated files
     * @param request Export request
     * @throws IOException if file operations fail
     */
    private void generateTestFiles(Map<String, String> files, ExportRequest request) throws IOException {
        // Create export request for code generation service
        com.cstestforge.codegen.model.ExportRequest codegenRequest = new com.cstestforge.codegen.model.ExportRequest(
                request.getProjectId(), "playwright-typescript", "CSTestForge Export");
        codegenRequest.setTestIds(request.getTestIds());
        codegenRequest.setIncludeFrameworkFiles(true);
        
        // Add export options based on the ExportRequest
        codegenRequest.addExportOption("generateBDD", request.isGenerateBDD());
        
        logger.info("Fetching test cases for generation");
        // Fetch test cases for the project
        List<String> testIds = request.getTestIds();
        
        for (String testId : testIds) {
            try {
                Optional<TestCase> testCase = testCaseService.getTestById(request.getProjectId(), testId);
                
                if (testCase.isPresent()) {
                    TestCase test = testCase.get();
                    logger.debug("Generating code for test: {}", test.getName());
                    
                    // Generate code using CodeGenerationService
                    GeneratedCode generatedCode = codeGenerationService.generateFromTest(testId, "playwright-typescript", "typescript");
                    
                    // Add the generated code to files
                    if (request.isGenerateBDD()) {
                        // Add BDD feature file and step definition
                        addBDDFiles(files, test, generatedCode);
                    } else {
                        // Add Playwright test spec
                        addPlaywrightFiles(files, test, generatedCode);
                    }
                    
                    // Add page object if available
                    if (generatedCode.getPageObjectCode() != null && !generatedCode.getPageObjectCode().isEmpty()) {
                        String className = getClassName(test.getName());
                        String fileName = getKebabCase(test.getName());
                        String filePath = "src/pages/" + fileName + ".page.ts";
                        files.put(filePath, generatedCode.getPageObjectCode());
                    } else {
                        // Generate default page object
                        generatePageObjects(files, test);
                    }
                } else {
                    logger.warn("Test not found: {}", testId);
                }
            } catch (Exception e) {
                logger.error("Failed to generate test for ID: {}", testId, e);
                throw new IOException("Failed to generate test: " + e.getMessage());
            }
        }
    }
    
    /**
     * Add Playwright test files to the export
     * 
     * @param files Map to store generated files
     * @param test Test case data
     * @param generatedCode Generated code from CodeGenerationService 
     */
    private void addPlaywrightFiles(Map<String, String> files, TestCase test, GeneratedCode generatedCode) {
        String fileName = getKebabCase(test.getName());
        String filePath = "tests/" + fileName + ".spec.ts";
        
        // Add the test spec file
        if (generatedCode.getCode() != null && !generatedCode.getCode().isEmpty()) {
            files.put(filePath, generatedCode.getCode());
            logger.debug("Added test spec to: {}", filePath);
        } else {
            // Fallback to generated test spec
            files.put(filePath, generateDefaultPlaywrightTest(test));
            logger.warn("Using fallback test spec for: {}", test.getName());
        }
    }
    
    /**
     * Add BDD test files to the export
     * 
     * @param files Map to store generated files
     * @param test Test case data
     * @param generatedCode Generated code from CodeGenerationService
     */
    private void addBDDFiles(Map<String, String> files, TestCase test, GeneratedCode generatedCode) {
        // Extract feature file and step definition from generated code
        String fileName = getKebabCase(test.getName());
        String featurePath = "features/" + fileName + ".feature";
        String stepPath = "tests/steps/" + fileName + ".steps.ts";
        
        // The GeneratedCode object contains:
        // - Feature file in the 'code' field when style is BDD
        // - Step definition in the metadata with key 'stepDefinitions'
        // - If not available, try to extract from pageObjectCode or generate a default
        
        // Add the feature file
        if (generatedCode.getCode() != null && !generatedCode.getCode().isEmpty()) {
            files.put(featurePath, generatedCode.getCode());
            logger.debug("Added feature file to: {}", featurePath);
        } else {
            logger.warn("Feature file content missing for test: {}", test.getName());
            // Generate minimal feature file as fallback
            files.put(featurePath, generateDefaultFeature(test));
        }
        
        // Add the step definition
        String stepDefinition = null;
        
        // Try to get step definition from metadata
        if (generatedCode.getMetadata().containsKey("stepDefinitions")) {
            stepDefinition = (String) generatedCode.getMetadata().get("stepDefinitions");
            logger.debug("Using step definition from metadata for: {}", test.getName());
        } 
        // Try to get from pageObjectCode if not in metadata (some generators put step definitions in pageObjectCode)
        else if (generatedCode.getPageObjectCode() != null && generatedCode.getPageObjectCode().contains("Given(")) {
            stepDefinition = generatedCode.getPageObjectCode();
            logger.debug("Using step definition from pageObjectCode for: {}", test.getName());
        }
        // Generate default step definition as fallback
        else {
            logger.info("Generating default step definition for: {}", test.getName());
            stepDefinition = generateDefaultStepDefinition(test);
        }
        
        files.put(stepPath, stepDefinition);
        logger.debug("Added step definition to: {}", stepPath);
    }
    
    /**
     * Generate a default Playwright test spec
     * 
     * @param test Test case data
     * @return Generated test spec
     */
    private String generateDefaultPlaywrightTest(TestCase test) {
        String className = getClassName(test.getName());
        String fileName = getKebabCase(test.getName());
        
        StringBuilder code = new StringBuilder();
        code.append("import { test, expect } from '@playwright/test';\n")
            .append("import { CSPlaywrightDriver } from '../src/utils/CSPlaywrightDriver';\n")
            .append("import { CSReporter } from '../src/utils/CSReporter';\n")
            .append("import { ").append(className).append("Page } from '../src/pages/").append(fileName).append(".page';\n\n")
            
            // Add proper test metadata annotations
            .append("/**\n")
            .append(" * @cstest\n")
            .append(" * @description Auto-generated test for ").append(test.getName()).append("\n")
            .append(" * @feature ").append(test.getName()).append("\n")
            .append(" * @author CSTestForge Generator\n")
            .append(" * @tags generated,automated\n")
            .append(" */\n")
            
            // Test structure following the CSTestForge framework pattern
            .append("test.describe('").append(test.getName()).append("', () => {\n")
            .append("  let driver: CSPlaywrightDriver;\n")
            .append("  let reporter: CSReporter;\n")
            .append("  let testPage: ").append(className).append("Page;\n\n")
            
            // Setup/teardown hooks
            .append("  test.beforeEach(async ({ page }) => {\n")
            .append("    driver = new CSPlaywrightDriver(page);\n")
            .append("    reporter = new CSReporter(page);\n")
            .append("    testPage = new ").append(className).append("Page(page);\n")
            .append("    \n")
            .append("    // Start test reporting\n")
            .append("    await reporter.startTest('").append(test.getName()).append("');\n")
            .append("  });\n\n")
            
            .append("  test.afterEach(async () => {\n")
            .append("    await reporter.endTest();\n")
            .append("  });\n\n")
            
            // Main test case following the step pattern
            .append("  test('should execute test flow', async ({ page }) => {\n")
            .append("    try {\n")
            .append("      // Navigate to application\n")
            .append("      await reporter.logStep('Navigate to application');\n")
            .append("      await page.goto('").append(test.getBaseUrl() != null ? test.getBaseUrl() : "https://example.com").append("');\n")
            .append("      await testPage.waitForPageLoad();\n\n")
            
            .append("      // Execute test steps\n")
            .append("      await reporter.logStep('Execute test actions');\n")
            .append("      await testPage.executeTestSteps();\n\n")
            
            .append("      // Verify results\n")
            .append("      await reporter.logStep('Verify test results');\n")
            .append("      // Example: await expect(page.locator('.result')).toContainText('Success');\n\n")
            
            .append("      // Mark test as successful\n")
            .append("      await reporter.logSuccess('Test completed successfully');\n")
            .append("    } catch (error) {\n")
            .append("      // Log failure and capture screenshot\n")
            .append("      await reporter.logFailure(`Test failed: ${error.message}`);\n")
            .append("      await testPage.takeScreenshot('test-failure');\n")
            .append("      throw error;\n")
            .append("    }\n")
            .append("  });\n")
            .append("});\n");
        
        return code.toString();
    }
    
    /**
     * Generate a default feature file if one is not available
     * 
     * @param test Test case data
     * @return Generated feature file content
     */
    private String generateDefaultFeature(TestCase test) {
        StringBuilder feature = new StringBuilder();
        feature.append("Feature: ").append(test.getName()).append("\n\n")
               .append("  Scenario: ").append(test.getName()).append("\n")
               .append("    Given the user is on the application page\n")
               .append("    When user performs test actions\n")
               .append("    Then the expected outcome should be achieved\n");
        
        return feature.toString();
    }
    
    /**
     * Generate a default step definition for BDD
     * 
     * @param test Test case data
     * @return Generated step definition code
     */
    private String generateDefaultStepDefinition(TestCase test) {
        String className = getClassName(test.getName());
        String fileName = getKebabCase(test.getName());
        
        StringBuilder steps = new StringBuilder();
        steps.append("import { Given, When, Then } from '@cucumber/cucumber';\n")
             .append("import { chromium, Browser, Page } from 'playwright';\n")
             .append("import { ").append(className).append("Page } from '../../src/pages/").append(fileName).append(".page';\n\n")
             .append("let browser: Browser;\n")
             .append("let page: Page;\n")
             .append("let testPage: ").append(className).append("Page;\n\n")
             .append("Given('the user is on the application page', async function() {\n")
             .append("  browser = await chromium.launch({ headless: true });\n")
             .append("  const context = await browser.newContext();\n")
             .append("  page = await context.newPage();\n")
             .append("  testPage = new ").append(className).append("Page(page);\n")
             .append("  await page.goto('").append(test.getBaseUrl() != null ? test.getBaseUrl() : "https://example.com").append("');\n")
             .append("});\n\n")
             .append("When('user performs test actions', async function() {\n")
             .append("  // Generated steps from recorded actions\n")
             .append("  await testPage.executeTestSteps();\n")
             .append("});\n\n")
             .append("Then('the expected outcome should be achieved', async function() {\n")
             .append("  // Assertions based on expected outcomes\n")
             .append("  // Example: await expect(page.locator('.result')).toContainText('Success');\n")
             .append("  await browser.close();\n")
             .append("});\n");
        
        return steps.toString();
    }
    
    /**
     * Generate page object class
     * 
     * @param files Map to store generated files
     * @param test Test case data
     */
    private void generatePageObjects(Map<String, String> files, TestCase test) {
        String className = getClassName(test.getName());
        String fileName = getKebabCase(test.getName());
        String filePath = "src/pages/" + fileName + ".page.ts";
        
        StringBuilder code = new StringBuilder();
        code.append("import { Page, Locator } from '@playwright/test';\n")
            .append("import { BasePage } from './base.page';\n\n")
            .append("export class ").append(className).append("Page extends BasePage {\n")
            .append("  // Page elements would be defined here based on recorded selectors\n")
            .append("  // Example: readonly usernameInput: Locator;\n\n")
            .append("  constructor(page: Page) {\n")
            .append("    super(page);\n")
            .append("    // Initialize locators\n")
            .append("    // Example: this.usernameInput = page.locator('#username');\n")
            .append("  }\n\n")
            .append("  /**\n")
            .append("   * Execute the test steps for ").append(test.getName()).append("\n")
            .append("   */\n")
            .append("  async executeTestSteps(): Promise<void> {\n")
            .append("    // Implementation of test steps based on recorded actions\n")
            .append("    // Example: await this.usernameInput.fill('testuser');\n")
            .append("  }\n")
            .append("}\n");
        
        files.put(filePath, code.toString());
        
        // Add base page if not already added
        if (!files.containsKey("src/pages/base.page.ts")) {
            String basePageContent = "import { Page, Locator } from '@playwright/test';\n\n" +
                    "export abstract class BasePage {\n" +
                    "  protected readonly page: Page;\n\n" +
                    "  constructor(page: Page) {\n" +
                    "    this.page = page;\n" +
                    "  }\n\n" +
                    "  /**\n" +
                    "   * Wait for page to load completely\n" +
                    "   */\n" +
                    "  async waitForPageLoad(): Promise<void> {\n" +
                    "    await this.page.waitForLoadState('networkidle');\n" +
                    "  }\n\n" +
                    "  /**\n" +
                    "   * Take screenshot\n" +
                    "   * @param name Screenshot name\n" +
                    "   */\n" +
                    "  async takeScreenshot(name: string): Promise<void> {\n" +
                    "    await this.page.screenshot({ path: `./test-output/screenshots/${name}-${Date.now()}.png` });\n" +
                    "  }\n" +
                    "}\n";
            
            files.put("src/pages/base.page.ts", basePageContent);
        }
    }
    
    /**
     * Create a ZIP package containing all project files
     * 
     * @param template Project template
     * @param generatedFiles Map of file paths to file contents
     * @return Byte array of ZIP data
     * @throws IOException if file operations fail
     */
    private byte[] createZipPackage(ProjectTemplate template, Map<String, String> generatedFiles) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            // Add directories
            for (String dir : template.getDirectories()) {
                ZipEntry entry = new ZipEntry(dir + "/");
                zos.putNextEntry(entry);
                zos.closeEntry();
            }
            
            // Add config files
            for (Map.Entry<String, String> entry : template.getConfigFiles().entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue().getBytes());
                zos.closeEntry();
            }
            
            // Add generated files
            for (Map.Entry<String, String> entry : generatedFiles.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue().getBytes());
                zos.closeEntry();
            }
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Convert string to class name (PascalCase)
     * 
     * @param input Input string
     * @return Class name
     */
    private String getClassName(String input) {
        if (input == null || input.isEmpty()) {
            return "Test";
        }
        
        StringBuilder sb = new StringBuilder();
        boolean capitalizeNext = true;
        
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '_' || c == '-') {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                sb.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                sb.append(c);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Convert string to kebab case (for file names)
     * 
     * @param input Input string
     * @return Kebab case string
     */
    private String getKebabCase(String input) {
        if (input == null || input.isEmpty()) {
            return "test";
        }
        
        return input.toLowerCase()
                     .replaceAll("[^a-zA-Z0-9]", "-")
                     .replaceAll("-+", "-");
    }
} 