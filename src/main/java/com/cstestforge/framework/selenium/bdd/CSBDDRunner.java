package com.cstestforge.framework.selenium.bdd;

import io.cucumber.core.cli.Main;
import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Main runner for BDD tests using Cucumber and TestNG.
 * Extends AbstractTestNGCucumberTests for TestNG integration.
 */
@CucumberOptions(
    plugin = {
        "pretty",
        "html:target/cucumber-reports/cucumber-pretty.html",
        "json:target/cucumber-reports/CucumberTestReport.json",
        "timeline:target/cucumber-reports/timeline",
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
    },
    monochrome = true,
    dryRun = false
)
public class CSBDDRunner extends AbstractTestNGCucumberTests {
    private static final Logger logger = LoggerFactory.getLogger(CSBDDRunner.class);
    
    /**
     * Cucumber feature file directory
     */
    protected String featuresPath = "src/test/resources/features";
    
    /**
     * Cucumber step definitions directory
     */
    protected String gluePackage = "com.cstestforge.framework.selenium.bdd.steps";
    
    /**
     * Profile settings from system properties
     */
    protected Properties profile = System.getProperties();
    
    /**
     * Cucumber runner initialization.
     */
    @BeforeSuite
    @Parameters({"featuresPath", "gluePackage"})
    public void setUpBDDEnvironment(String featuresPath, String gluePackage) {
        if (featuresPath != null && !featuresPath.isEmpty()) {
            this.featuresPath = featuresPath;
        }
        
        if (gluePackage != null && !gluePackage.isEmpty()) {
            this.gluePackage = gluePackage;
        }
        
        // Verify feature directory exists
        File featureDir = new File(this.featuresPath);
        if (!featureDir.exists() || !featureDir.isDirectory()) {
            logger.warn("Features directory not found: {}", this.featuresPath);
            return;
        }
        
        // Configure Cucumber options
        System.setProperty("cucumber.features", this.featuresPath);
        System.setProperty("cucumber.glue", this.gluePackage);
        
        // Configure any additional properties
        String tagsExpression = profile.getProperty("cucumber.tags");
        if (tagsExpression != null && !tagsExpression.isEmpty()) {
            System.setProperty("cucumber.filter.tags", tagsExpression);
        }
        
        logger.info("BDD Environment setup complete. Features: {}, Glue: {}", 
                this.featuresPath, this.gluePackage);
    }
    
    /**
     * Clean up after test execution.
     */
    @AfterSuite
    public void tearDownBDDEnvironment() {
        logger.info("BDD Test execution complete");
    }
    
    /**
     * Override to provide scenarios parallel execution.
     * 
     * @return Cucumber scenarios
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
    
    /**
     * Execute BDD tests using Cucumber CLI.
     * This method allows for manual execution outside of TestNG.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        List<String> arguments = new ArrayList<>();
        
        // Check if arguments were provided
        if (args.length == 0) {
            arguments.add("--glue");
            arguments.add("com.cstestforge.framework.selenium.bdd.steps");
            arguments.add("--plugin");
            arguments.add("pretty");
            arguments.add("--plugin");
            arguments.add("html:target/cucumber-reports/cucumber-pretty.html");
            arguments.add("--plugin");
            arguments.add("json:target/cucumber-reports/CucumberTestReport.json");
            arguments.add("--monochrome");
            
            // Add feature files from default directory
            try {
                List<String> featureFiles = findFeatureFiles("src/test/resources/features");
                arguments.addAll(featureFiles);
            } catch (IOException e) {
                logger.error("Failed to find feature files", e);
                arguments.add("src/test/resources/features");
            }
        } else {
            // Use provided arguments
            for (String arg : args) {
                arguments.add(arg);
            }
        }
        
        // Run Cucumber with the arguments
        logger.info("Running Cucumber with arguments: {}", arguments);
        Main.run(arguments.toArray(new String[0]));
    }
    
    /**
     * Find all .feature files in the specified directory and subdirectories.
     * 
     * @param directory Directory to search
     * @return List of feature file paths
     * @throws IOException If directory cannot be read
     */
    private static List<String> findFeatureFiles(String directory) throws IOException {
        Path featuresDir = Paths.get(directory);
        if (!Files.exists(featuresDir)) {
            logger.warn("Features directory not found: {}", directory);
            return new ArrayList<>();
        }
        
        return Files.walk(featuresDir)
                .filter(path -> path.toString().endsWith(".feature"))
                .map(Path::toString)
                .collect(Collectors.toList());
    }
    
    /**
     * Create a runner instance with custom configuration.
     * 
     * @param featuresPath Path to feature files
     * @param gluePackage Package with step definitions
     * @param tagsExpression Tags expression for filtering
     * @return Configured CSBDDRunner instance
     */
    public static CSBDDRunner createRunner(String featuresPath, String gluePackage, String tagsExpression) {
        CSBDDRunner runner = new CSBDDRunner();
        
        if (featuresPath != null && !featuresPath.isEmpty()) {
            runner.featuresPath = featuresPath;
        }
        
        if (gluePackage != null && !gluePackage.isEmpty()) {
            runner.gluePackage = gluePackage;
        }
        
        if (tagsExpression != null && !tagsExpression.isEmpty()) {
            System.setProperty("cucumber.filter.tags", tagsExpression);
        }
        
        return runner;
    }
} 