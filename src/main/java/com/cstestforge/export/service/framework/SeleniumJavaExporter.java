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
 * Exporter for Selenium Java framework
 */
@Component
public class SeleniumJavaExporter implements FrameworkExporter {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumJavaExporter.class);

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
                "selenium-java-export",
                "selenium",
                "java",
                request.getBuildTool()
        );
        
        // Set up directory structure based on package name
        String basePackagePath = request.getPackageName().replace('.', '/');
        
        // Main structure (core framework)
        template.addDirectory("src/main/java/" + basePackagePath + "/pages");
        template.addDirectory("src/main/java/" + basePackagePath + "/core");
        template.addDirectory("src/main/java/" + basePackagePath + "/utils");
        template.addDirectory("src/main/java/" + basePackagePath + "/element");
        template.addDirectory("src/main/java/" + basePackagePath + "/reporting");
        template.addDirectory("src/main/resources");
        
        // Test structure
        template.addDirectory("src/test/java/" + basePackagePath + "/tests");
        template.addDirectory("src/test/resources");
        
        // BDD structure if needed
        if (request.isGenerateBDD()) {
            template.addDirectory("src/test/java/" + basePackagePath + "/steps");
            template.addDirectory("src/test/resources/features");
        }
        
        // Config directories
        template.addDirectory("config");
        template.addDirectory("drivers");
        template.addDirectory("test-output");
        
        // Add build configuration based on build tool
        if ("maven".equalsIgnoreCase(request.getBuildTool())) {
            template.addConfigFile("pom.xml", generateMavenPomXml(request));
        } else if ("gradle".equalsIgnoreCase(request.getBuildTool())) {
            template.addConfigFile("build.gradle", generateGradleBuildScript(request));
        }
        
        return template;
    }

    @Override
    public Map<String, String> generateFiles(ExportRequest request) throws IOException {
        Map<String, String> files = new HashMap<>();
        String basePackagePath = request.getPackageName().replace('.', '/');
        
        // Copy core framework files from cstestforge/framework/selenium
        logger.info("Copying framework files for Selenium Java export");
        copyFrameworkFiles(files, basePackagePath, request.getPackageName());
        
        // Generate test files based on recorded tests using the CodeGenerationService
        logger.info("Generating test files for {} tests", request.getTestIds().size());
        generateTestFiles(files, request, basePackagePath);
        
        // Add additional config files and documentation
        files.put("config/selenium.properties", generateSeleniumProperties());
        files.put("README.md", generateReadme(request));
        files.put("testng.xml", generateTestNGXml(request));
        
        return files;
    }

    @Override
    public byte[] createExportPackage(ExportRequest request) throws IOException {
        // Initialize project template
        logger.info("Creating Selenium Java export package for project {}", request.getProjectId());
        ProjectTemplate template = initializeTemplate(request);
        
        // Generate files
        Map<String, String> generatedFiles = generateFiles(request);
        
        // Create ZIP file
        return createZipPackage(template, generatedFiles);
    }
    
    /**
     * Generate Maven POM file
     * 
     * @param request Export request
     * @return POM XML content
     */
    private String generateMavenPomXml(ExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n")
          .append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
          .append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n")
          .append("    <modelVersion>4.0.0</modelVersion>\n\n")
          .append("    <groupId>").append(request.getPackageName()).append("</groupId>\n")
          .append("    <artifactId>selenium-tests</artifactId>\n")
          .append("    <version>1.0.0</version>\n")
          .append("    <name>CSTestForge Selenium Tests</name>\n")
          .append("    <description>Generated Selenium tests using CSTestForge</description>\n\n")
          .append("    <properties>\n")
          .append("        <maven.compiler.source>17</maven.compiler.source>\n")
          .append("        <maven.compiler.target>17</maven.compiler.target>\n")
          .append("        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>\n")
          .append("        <selenium.version>4.12.1</selenium.version>\n")
          .append("        <webdrivermanager.version>5.5.3</webdrivermanager.version>\n")
          .append("        <testng.version>7.8.0</testng.version>\n")
          .append("        <slf4j.version>2.0.9</slf4j.version>\n")
          .append("        <logback.version>1.4.11</logback.version>\n")
          .append("        <jackson.version>2.15.2</jackson.version>\n")
          .append("        <commons.io.version>2.13.0</commons.io.version>\n");
        
        // Add BDD properties if needed
        if (request.isGenerateBDD()) {
            sb.append("        <cucumber.version>7.14.0</cucumber.version>\n");
        }
        
        sb.append("    </properties>\n\n")
          .append("    <dependencies>\n")
          .append("        <!-- Selenium -->\n")
          .append("        <dependency>\n")
          .append("            <groupId>org.seleniumhq.selenium</groupId>\n")
          .append("            <artifactId>selenium-java</artifactId>\n")
          .append("            <version>${selenium.version}</version>\n")
          .append("        </dependency>\n\n")
          .append("        <!-- WebDriverManager -->\n")
          .append("        <dependency>\n")
          .append("            <groupId>io.github.bonigarcia</groupId>\n")
          .append("            <artifactId>webdrivermanager</artifactId>\n")
          .append("            <version>${webdrivermanager.version}</version>\n")
          .append("        </dependency>\n\n")
          .append("        <!-- TestNG -->\n")
          .append("        <dependency>\n")
          .append("            <groupId>org.testng</groupId>\n")
          .append("            <artifactId>testng</artifactId>\n")
          .append("            <version>${testng.version}</version>\n")
          .append("            <scope>test</scope>\n")
          .append("        </dependency>\n\n")
          .append("        <!-- Logging -->\n")
          .append("        <dependency>\n")
          .append("            <groupId>org.slf4j</groupId>\n")
          .append("            <artifactId>slf4j-api</artifactId>\n")
          .append("            <version>${slf4j.version}</version>\n")
          .append("        </dependency>\n")
          .append("        <dependency>\n")
          .append("            <groupId>ch.qos.logback</groupId>\n")
          .append("            <artifactId>logback-classic</artifactId>\n")
          .append("            <version>${logback.version}</version>\n")
          .append("        </dependency>\n\n")
          .append("        <!-- Jackson for JSON -->\n")
          .append("        <dependency>\n")
          .append("            <groupId>com.fasterxml.jackson.core</groupId>\n")
          .append("            <artifactId>jackson-databind</artifactId>\n")
          .append("            <version>${jackson.version}</version>\n")
          .append("        </dependency>\n\n")
          .append("        <!-- Commons IO -->\n")
          .append("        <dependency>\n")
          .append("            <groupId>commons-io</groupId>\n")
          .append("            <artifactId>commons-io</artifactId>\n")
          .append("            <version>${commons.io.version}</version>\n")
          .append("        </dependency>\n");
        
        // Add BDD dependencies if needed
        if (request.isGenerateBDD()) {
            sb.append("\n")
              .append("        <!-- Cucumber BDD -->\n")
              .append("        <dependency>\n")
              .append("            <groupId>io.cucumber</groupId>\n")
              .append("            <artifactId>cucumber-java</artifactId>\n")
              .append("            <version>${cucumber.version}</version>\n")
              .append("        </dependency>\n")
              .append("        <dependency>\n")
              .append("            <groupId>io.cucumber</groupId>\n")
              .append("            <artifactId>cucumber-testng</artifactId>\n")
              .append("            <version>${cucumber.version}</version>\n")
              .append("        </dependency>\n")
              .append("        <dependency>\n")
              .append("            <groupId>io.cucumber</groupId>\n")
              .append("            <artifactId>cucumber-core</artifactId>\n")
              .append("            <version>${cucumber.version}</version>\n")
              .append("        </dependency>\n");
        }
        
        sb.append("    </dependencies>\n\n")
          .append("    <build>\n")
          .append("        <plugins>\n")
          .append("            <plugin>\n")
          .append("                <groupId>org.apache.maven.plugins</groupId>\n")
          .append("                <artifactId>maven-compiler-plugin</artifactId>\n")
          .append("                <version>3.11.0</version>\n")
          .append("                <configuration>\n")
          .append("                    <source>${maven.compiler.source}</source>\n")
          .append("                    <target>${maven.compiler.target}</target>\n")
          .append("                </configuration>\n")
          .append("            </plugin>\n")
          .append("            <plugin>\n")
          .append("                <groupId>org.apache.maven.plugins</groupId>\n")
          .append("                <artifactId>maven-surefire-plugin</artifactId>\n")
          .append("                <version>3.1.2</version>\n")
          .append("                <configuration>\n")
          .append("                    <suiteXmlFiles>\n")
          .append("                        <suiteXmlFile>testng.xml</suiteXmlFile>\n")
          .append("                    </suiteXmlFiles>\n")
          .append("                </configuration>\n")
          .append("            </plugin>\n")
          .append("        </plugins>\n")
          .append("    </build>\n")
          .append("</project>");
        
        return sb.toString();
    }
    
    /**
     * Generate Gradle build script
     * 
     * @param request Export request
     * @return Gradle script content
     */
    private String generateGradleBuildScript(ExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("plugins {\n")
          .append("    id 'java'\n");
        
        if (request.isGenerateBDD()) {
            sb.append("    id 'io.cucumber.java' version '7.0.0'\n");
        }
        
        sb.append("}\n\n")
          .append("group = '").append(request.getPackageName()).append("'\n")
          .append("version = '1.0.0'\n\n")
          .append("java {\n")
          .append("    toolchain {\n")
          .append("        languageVersion = JavaLanguageVersion.of(17)\n")
          .append("    }\n")
          .append("}\n\n")
          .append("repositories {\n")
          .append("    mavenCentral()\n")
          .append("}\n\n")
          .append("dependencies {\n")
          .append("    implementation 'org.seleniumhq.selenium:selenium-java:4.12.1'\n")
          .append("    implementation 'io.github.bonigarcia:webdrivermanager:5.5.3'\n")
          .append("    implementation 'org.slf4j:slf4j-api:2.0.9'\n")
          .append("    implementation 'ch.qos.logback:logback-classic:1.4.11'\n")
          .append("    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.2'\n")
          .append("    implementation 'commons-io:commons-io:2.13.0'\n\n")
          .append("    testImplementation 'org.testng:testng:7.8.0'\n");
        
        if (request.isGenerateBDD()) {
            sb.append("    testImplementation 'io.cucumber:cucumber-java:7.14.0'\n")
              .append("    testImplementation 'io.cucumber:cucumber-testng:7.14.0'\n")
              .append("    testImplementation 'io.cucumber:cucumber-core:7.14.0'\n");
        }
        
        sb.append("}\n\n")
          .append("test {\n")
          .append("    useTestNG() {\n")
          .append("        suites 'testng.xml'\n")
          .append("    }\n")
          .append("    testLogging {\n")
          .append("        events \"PASSED\", \"FAILED\", \"SKIPPED\"\n")
          .append("    }\n")
          .append("}");
        
        return sb.toString();
    }
    
    /**
     * Generate Selenium properties file
     * 
     * @return Properties file content
     */
    private String generateSeleniumProperties() {
        return "# WebDriver Configuration\n" +
               "webdriver.browser=chrome\n" +
               "webdriver.headless=false\n" +
               "webdriver.implicit.wait.seconds=10\n" +
               "webdriver.page.load.timeout.seconds=30\n" +
               "webdriver.script.timeout.seconds=30\n\n" +
               "# Screenshot Configuration\n" +
               "screenshot.on.failure=true\n" +
               "screenshot.directory=test-output/screenshots\n\n" +
               "# Reporting Configuration\n" +
               "report.directory=test-output/reports\n" +
               "report.title=CSTestForge Test Report\n";
    }
    
    /**
     * Generate TestNG XML configuration
     * 
     * @param request Export request
     * @return TestNG XML content
     */
    private String generateTestNGXml(ExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
          .append("<!DOCTYPE suite SYSTEM \"https://testng.org/testng-1.0.dtd\">\n")
          .append("<suite name=\"CSTestForge Test Suite\">\n")
          .append("    <test name=\"Regression Tests\">\n")
          .append("        <classes>\n");
          
        // Add test classes based on the test IDs
        for (String testId : request.getTestIds()) {
            try {
                Optional<TestCase> testCase = testCaseService.getTestById(request.getProjectId(), testId);
                if (testCase.isPresent()) {
                    String className = getClassName(testCase.get().getName()) + "Test";
                    sb.append("            <class name=\"").append(request.getPackageName()).append(".tests.").append(className).append("\"/>\n");
                }
            } catch (Exception e) {
                logger.warn("Failed to add test class to TestNG XML for test ID: {}", testId, e);
            }
        }
        
        sb.append("        </classes>\n")
          .append("    </test>\n")
          .append("</suite>");
        
        return sb.toString();
    }
    
    /**
     * Generate README file
     * 
     * @param request Export request
     * @return Markdown content
     */
    private String generateReadme(ExportRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("# CSTestForge Selenium Test Suite\n\n")
          .append("This project contains automatically generated Selenium tests using CSTestForge.\n\n")
          .append("## Framework Details\n\n")
          .append("- **Framework**: Selenium\n")
          .append("- **Language**: Java\n")
          .append("- **Build Tool**: ").append(request.getBuildTool()).append("\n");
        
        if (request.isGenerateBDD()) {
            sb.append("- **BDD Framework**: Cucumber\n");
        }
        
        sb.append("\n## Project Structure\n\n")
          .append("- `src/main/java/` - Framework code and utilities\n")
          .append("- `src/test/java/` - Test cases");
        
        if (request.isGenerateBDD()) {
            sb.append(" and step definitions\n");
            sb.append("- `src/test/resources/features/` - Cucumber feature files\n");
        } else {
            sb.append("\n");
        }
        
        sb.append("- `config/` - Configuration files\n")
          .append("- `drivers/` - WebDriver executables\n")
          .append("- `test-output/` - Test reports and logs\n\n")
          .append("## Running Tests\n\n");
        
        if ("maven".equalsIgnoreCase(request.getBuildTool())) {
            sb.append("```bash\n")
              .append("# Run all tests\n")
              .append("mvn clean test\n\n")
              .append("# Run specific test class\n")
              .append("mvn test -Dtest=TestClassName\n")
              .append("```\n");
        } else if ("gradle".equalsIgnoreCase(request.getBuildTool())) {
            sb.append("```bash\n")
              .append("# Run all tests\n")
              .append("./gradlew clean test\n\n")
              .append("# Run specific test class\n")
              .append("./gradlew test --tests \"TestClassName\"\n")
              .append("```\n");
        }
        
        sb.append("\n## Configuration\n\n")
          .append("Edit `config/selenium.properties` to adjust browser and test behavior settings.\n\n")
          .append("## Reports\n\n")
          .append("Test reports can be found in the `test-output` directory after test execution.\n");
        
        return sb.toString();
    }
    
    /**
     * Copy framework files from source to export package
     * 
     * @param files Map to store generated files
     * @param basePackagePath Base package path
     * @param packageName Target package name for import statements
     * @throws IOException if file operations fail
     */
    private void copyFrameworkFiles(Map<String, String> files, String basePackagePath, String packageName) throws IOException {
        Path frameworkPath = Paths.get("src/main/java/com/cstestforge/framework/selenium");
        if (!Files.exists(frameworkPath)) {
            logger.warn("Framework directory not found: {}", frameworkPath);
            return;
        }
        
        try (Stream<Path> paths = Files.walk(frameworkPath)) {
            paths.filter(Files::isRegularFile)
                 .forEach(path -> {
                     try {
                         String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                         
                         // Update package declarations
                         content = content.replace("package com.cstestforge.framework.selenium", 
                                                  "package " + packageName);
                         
                         // Update import statements
                         content = content.replaceAll("import com\\.cstestforge\\.framework\\.selenium", 
                                                    "import " + packageName);
                         
                         // Get relative path
                         Path relativePath = frameworkPath.relativize(path);
                         String targetPath = "src/main/java/" + basePackagePath + "/" + relativePath.toString();
                         
                         // Store the file
                         files.put(targetPath, content);
                         logger.debug("Copied framework file: {} -> {}", path, targetPath);
                     } catch (IOException e) {
                         logger.error("Failed to process framework file: {}", path, e);
                     }
                 });
        }
    }
    
    /**
     * Generate test files from recorded tests using CodeGenerationService
     * 
     * @param files Map to store generated files
     * @param request Export request
     * @param basePackagePath Base package path
     * @throws IOException if file operations fail
     */
    private void generateTestFiles(Map<String, String> files, ExportRequest request, String basePackagePath) throws IOException {
        // Create export request for code generation service
        com.cstestforge.codegen.model.ExportRequest codegenRequest = new com.cstestforge.codegen.model.ExportRequest(
                request.getProjectId(), "selenium-java", "CSTestForge Export");
        codegenRequest.setTestIds(request.getTestIds());
        codegenRequest.setPackageName(request.getPackageName());
        codegenRequest.setIncludeFrameworkFiles(true);
        
        // Add export options based on the ExportRequest
        codegenRequest.addExportOption("generateBDD", request.isGenerateBDD());
        codegenRequest.addExportOption("buildTool", request.getBuildTool());
        
        logger.info("Fetching test cases for generation");
        for (String testId : request.getTestIds()) {
            try {
                Optional<TestCase> testCase = testCaseService.getTestById(request.getProjectId(), testId);
                
                if (testCase.isPresent()) {
                    TestCase test = testCase.get();
                    logger.debug("Generating code for test: {}", test.getName());
                    
                    // Generate code using CodeGenerationService
                    GeneratedCode generatedCode = codeGenerationService.generateFromTest(testId, "selenium-java", "java");
                    
                    // Add the generated code to files
                    if (request.isGenerateBDD()) {
                        // Add BDD feature file and step definition
                        addBddFiles(files, test, generatedCode, basePackagePath, request.getPackageName());
                    } else {
                        // Add TestNG test class
                        addTestNgFiles(files, test, generatedCode, basePackagePath, request.getPackageName());
                    }
                    
                    // Add page object if available
                    if (generatedCode.getPageObjectCode() != null && !generatedCode.getPageObjectCode().isEmpty()) {
                        String className = getClassName(test.getName()) + "Page";
                        String filePath = "src/main/java/" + basePackagePath + "/pages/" + className + ".java";
                        files.put(filePath, generatedCode.getPageObjectCode());
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
     * Add TestNG test files to the export
     * 
     * @param files Map to store generated files
     * @param test Test case data
     * @param generatedCode Generated code from CodeGenerationService 
     * @param basePackagePath Base package path
     * @param packageName Package name for import statements
     */
    private void addTestNgFiles(Map<String, String> files, TestCase test, GeneratedCode generatedCode, 
                                String basePackagePath, String packageName) {
        String className = getClassName(test.getName()) + "Test";
        String filePath = "src/test/java/" + basePackagePath + "/tests/" + className + ".java";
        
        // Add the test class
        if (generatedCode.getCode() != null && !generatedCode.getCode().isEmpty()) {
            files.put(filePath, generatedCode.getCode());
            logger.debug("Added TestNG test class to: {}", filePath);
        } else {
            logger.warn("TestNG test class content missing for test: {}", test.getName());
            // Generate minimal test class as fallback
            files.put(filePath, generateDefaultTestNGClass(test, basePackagePath, packageName));
        }
    }
    
    /**
     * Generate a default TestNG test class if one is not available
     * 
     * @param test Test case data
     * @param basePackagePath Base package path
     * @param packageName Package name for import statements
     * @return Generated TestNG test class content
     */
    private String generateDefaultTestNGClass(TestCase test, String basePackagePath, String packageName) {
        String className = getClassName(test.getName());
        
        StringBuilder code = new StringBuilder();
        code.append("package ").append(packageName).append(".tests;\n\n")
            .append("import org.testng.annotations.Test;\n")
            .append("import org.slf4j.Logger;\n")
            .append("import org.slf4j.LoggerFactory;\n")
            .append("import org.testng.Assert;\n")
            
            // Use CSTestForge framework imports
            .append("import ").append(packageName).append(".core.CSDriverManager;\n")
            .append("import ").append(packageName).append(".core.CSBaseTest;\n")
            .append("import ").append(packageName).append(".annotation.CSMetaData;\n")
            .append("import ").append(packageName).append(".annotation.CSTestStep;\n")
            .append("import ").append(packageName).append(".reporting.CSReporting;\n")
            .append("import ").append(packageName).append(".element.CSElementInteractionHandler;\n")
            .append("import ").append(packageName).append(".pages.").append(className).append("Page;\n\n")
            
            // Add class annotations and header
            .append("/**\n")
            .append(" * Test class for ").append(test.getName()).append("\n")
            .append(" */\n")
            .append("@CSMetaData(\n")
            .append("    feature = \"").append(test.getName()).append("\",\n")
            .append("    description = \"Auto-generated test for ").append(test.getName()).append("\",\n")
            .append("    authors = {\"CSTestForge Generator\"},\n")
            .append("    tags = {\"generated\", \"automated\"}\n")
            .append(")\n")
            .append("public class ").append(className).append("Test extends CSBaseTest {\n\n")
            
            // Add logger and fields
            .append("    private static final Logger logger = LoggerFactory.getLogger(").append(className).append("Test.class);\n")
            .append("    private ").append(className).append("Page page;\n")
            .append("    private CSElementInteractionHandler elementHandler;\n\n")
            
            // Add BeforeClass/BeforeMethod setup
            .append("    @Override\n")
            .append("    protected void beforeMethod() {\n")
            .append("        super.beforeMethod();\n")
            .append("        logger.info(\"Initializing test for ").append(test.getName()).append("\");\n")
            .append("        page = new ").append(className).append("Page(driver);\n")
            .append("        elementHandler = getElementHandler();\n")
            .append("        reporting.startTest(\"").append(test.getName()).append("\");\n")
            .append("    }\n\n")
            
            // Add test method with steps
            .append("    @Test\n")
            .append("    public void ").append(getCamelCase(test.getName())).append("() {\n")
            .append("        logger.info(\"Executing test: ").append(test.getName()).append("\");\n")
            .append("        try {\n")
            .append("            // Navigate to the application\n")
            .append("            navigateToApplication();\n\n")
            .append("            // Execute test steps\n")
            .append("            executeTestSteps();\n\n")
            .append("            // Verify expected results\n")
            .append("            verifyResults();\n\n")
            .append("            reporting.logSuccess(\"Test executed successfully\");\n")
            .append("        } catch (Exception e) {\n")
            .append("            reporting.logFailure(\"Test failed: \" + e.getMessage());\n")
            .append("            throw e;\n")
            .append("        }\n")
            .append("    }\n\n")
            
            // Add helper methods with proper annotations
            .append("    @CSTestStep(description = \"Navigate to the application\")\n")
            .append("    private void navigateToApplication() {\n")
            .append("        reporting.logInfo(\"Navigating to application\");\n")
            .append("        driver.get(\"").append(test.getBaseUrl() != null ? test.getBaseUrl() : "https://example.com").append("\");\n")
            .append("    }\n\n")
            
            .append("    @CSTestStep(description = \"Execute test steps\")\n")
            .append("    private void executeTestSteps() {\n")
            .append("        reporting.logInfo(\"Executing test steps\");\n")
            .append("        // Implementation would call page object methods for recorded actions\n")
            .append("        // page.performAction();\n")
            .append("    }\n\n")
            
            .append("    @CSTestStep(description = \"Verify test results\")\n")
            .append("    private void verifyResults() {\n")
            .append("        reporting.logInfo(\"Verifying test results\");\n")
            .append("        // Implementation would contain assertions based on expected outcomes\n")
            .append("        // Assert.assertTrue(page.isElementPresent(\"result\"), \"Result element should be present\");\n")
            .append("    }\n")
            .append("}\n");
            
        return code.toString();
    }
    
    /**
     * Add BDD test files to the export
     * 
     * @param files Map to store generated files
     * @param test Test case data
     * @param generatedCode Generated code from CodeGenerationService
     * @param basePackagePath Base package path
     * @param packageName Package name for import statements
     */
    private void addBddFiles(Map<String, String> files, TestCase test, GeneratedCode generatedCode, 
                            String basePackagePath, String packageName) {
        // Extract feature file and step definition from generated code
        String featureName = getSnakeCase(test.getName());
        String featurePath = "src/test/resources/features/" + featureName + ".feature";
        
        String stepClassName = getClassName(test.getName()) + "Steps";
        String stepPath = "src/test/java/" + basePackagePath + "/steps/" + stepClassName + ".java";
        
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
        // Try to get from pageObjectCode if not in metadata
        else if (generatedCode.getPageObjectCode() != null && generatedCode.getPageObjectCode().contains("@Given")) {
            stepDefinition = generatedCode.getPageObjectCode();
            logger.debug("Using step definition from pageObjectCode for: {}", test.getName());
        }
        // Generate default step definition as fallback
        else {
            logger.info("Generating default step definition for: {}", test.getName());
            stepDefinition = generateDefaultStepDefinition(test, basePackagePath, packageName);
        }
        
        files.put(stepPath, stepDefinition);
        logger.debug("Added step definition to: {}", stepPath);
    }
    
    /**
     * Generate a default feature file if one is not available
     * 
     * @param test Test case data
     * @return Generated feature file content
     */
    private String generateDefaultFeature(TestCase test) {
        StringBuilder feature = new StringBuilder();
        feature.append("Feature: ").append(test.getName()).append("\n\n");
        feature.append("  Scenario: ").append(test.getName()).append("\n");
        feature.append("    Given the user is on the application page\n");
        feature.append("    When user performs test actions\n");
        feature.append("    Then the expected outcome should be achieved\n");
        
        return feature.toString();
    }
    
    /**
     * Generate a default step definition for BDD
     * 
     * @param test Test case data
     * @param basePackagePath Base package path
     * @param packageName Package name for import statements
     * @return Generated step definition code
     */
    private String generateDefaultStepDefinition(TestCase test, String basePackagePath, String packageName) {
        StringBuilder steps = new StringBuilder();
        steps.append("package ").append(packageName).append(".steps;\n\n")
             .append("import io.cucumber.java.en.Given;\n")
             .append("import io.cucumber.java.en.When;\n")
             .append("import io.cucumber.java.en.Then;\n")
             .append("import org.openqa.selenium.WebDriver;\n")
             .append("import ").append(packageName).append(".core.WebDriverFactory;\n")
             .append("import ").append(packageName).append(".pages.").append(getClassName(test.getName())).append("Page;\n\n")
             .append("public class ").append(getClassName(test.getName())).append("Steps {\n\n")
             .append("    private WebDriver driver = WebDriverFactory.getDriver();\n")
             .append("    private ").append(getClassName(test.getName())).append("Page page = new ")
             .append(getClassName(test.getName())).append("Page(driver);\n\n")
             .append("    @Given(\"the user is on the application page\")\n")
             .append("    public void userIsOnApplicationPage() {\n")
             .append("        driver.get(\"").append(test.getBaseUrl() != null ? test.getBaseUrl() : "https://example.com").append("\");\n")
             .append("    }\n\n")
             .append("    @When(\"user performs test actions\")\n")
             .append("    public void userPerformsTestActions() {\n")
             .append("        // Generated steps from recorded actions\n")
             .append("        page.performTestActions();\n")
             .append("    }\n\n")
             .append("    @Then(\"the expected outcome should be achieved\")\n")
             .append("    public void expectedOutcomeAchieved() {\n")
             .append("        // Assertions based on expected outcomes\n")
             .append("    }\n")
             .append("}\n");
        
        return steps.toString();
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
        
        logger.info("Export ZIP package created successfully");
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
                sb.append(Character.toLowerCase(c));
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Convert string to camel case (for method names)
     * 
     * @param input Input string
     * @return Camel case string
     */
    private String getCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return "test";
        }
        
        String className = getClassName(input);
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }
    
    /**
     * Convert string to snake case (for file names)
     * 
     * @param input Input string
     * @return Snake case string
     */
    private String getSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return "test";
        }
        
        return input.toLowerCase()
                     .replaceAll("[^a-zA-Z0-9]", "_")
                     .replaceAll("_+", "_");
    }
} 