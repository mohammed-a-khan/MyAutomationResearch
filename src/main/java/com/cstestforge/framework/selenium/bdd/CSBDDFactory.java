package com.cstestforge.framework.selenium.bdd;

import io.cucumber.core.gherkin.Feature;
import io.cucumber.core.feature.FeatureParser;
import io.cucumber.core.resource.Resource;
import io.cucumber.core.resource.ResourceScanner;
import io.cucumber.plugin.event.TestSourceRead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Factory for creating and managing BDD test components.
 * Provides utility methods for working with Cucumber features and steps.
 */
public class CSBDDFactory {
    private static final Logger logger = LoggerFactory.getLogger(CSBDDFactory.class);
    
    // Maps to store registered step definitions and hooks
    private static final Map<String, Object> stepDefinitions = new HashMap<>();
    private static final Map<String, Object> hooks = new HashMap<>();
    
    /**
     * Private constructor to prevent instantiation
     */
    private CSBDDFactory() {
        // Factory class should not be instantiated
    }
    
    /**
     * Register a step definition class instance
     * 
     * @param name Name to register with
     * @param instance Step definition instance
     */
    public static void registerStepDefinition(String name, Object instance) {
        stepDefinitions.put(name, instance);
        logger.debug("Registered step definition: {}", name);
    }
    
    /**
     * Get a registered step definition
     * 
     * @param <T> Step definition type
     * @param name Step definition name
     * @param type Step definition class
     * @return Step definition instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getStepDefinition(String name, Class<T> type) {
        Object stepDef = stepDefinitions.get(name);
        if (stepDef != null && type.isInstance(stepDef)) {
            return (T) stepDef;
        }
        
        return null;
    }
    
    /**
     * Register a hook instance
     * 
     * @param name Name to register with
     * @param instance Hook instance
     */
    public static void registerHook(String name, Object instance) {
        hooks.put(name, instance);
        logger.debug("Registered hook: {}", name);
    }
    
    /**
     * Get a registered hook
     * 
     * @param <T> Hook type
     * @param name Hook name
     * @param type Hook class
     * @return Hook instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T getHook(String name, Class<T> type) {
        Object hook = hooks.get(name);
        if (hook != null && type.isInstance(hook)) {
            return (T) hook;
        }
        
        return null;
    }
    
    /**
     * Load all Cucumber feature files from a directory
     * 
     * @param featureDirectory Directory containing feature files
     * @return Map of feature file paths to feature file contents
     * @throws IOException If directory cannot be read
     */
    public static Map<String, String> loadFeatureFiles(String featureDirectory) throws IOException {
        Map<String, String> features = new HashMap<>();
        Path featuresDir = Paths.get(featureDirectory);
        
        if (!Files.exists(featuresDir)) {
            logger.warn("Features directory not found: {}", featureDirectory);
            return features;
        }
        
        Files.walk(featuresDir)
                .filter(path -> path.toString().endsWith(".feature"))
                .forEach(path -> {
                    try {
                        String content = new String(Files.readAllBytes(path));
                        features.put(path.toString(), content);
                    } catch (IOException e) {
                        logger.error("Failed to read feature file: {}", path, e);
                    }
                });
        
        return features;
    }
    
    /**
     * Create a scenario context
     * 
     * @return New scenario context instance
     */
    public static CSScenarioContext createScenarioContext() {
        return new CSScenarioContext();
    }
    
    /**
     * Create a BDD runner with default configuration
     * 
     * @return BDD runner instance
     */
    public static CSBDDRunner createBDDRunner() {
        return new CSBDDRunner();
    }
    
    /**
     * Create a BDD runner with custom configuration
     * 
     * @param featuresPath Feature files path
     * @param gluePackage Glue package for step definitions
     * @return BDD runner instance
     */
    public static CSBDDRunner createBDDRunner(String featuresPath, String gluePackage) {
        return CSBDDRunner.createRunner(featuresPath, gluePackage, null);
    }
    
    /**
     * Create a BDD runner with custom configuration including tags
     * 
     * @param featuresPath Feature files path
     * @param gluePackage Glue package for step definitions
     * @param tagsExpression Tags expression for filtering
     * @return BDD runner instance
     */
    public static CSBDDRunner createBDDRunner(String featuresPath, String gluePackage, String tagsExpression) {
        return CSBDDRunner.createRunner(featuresPath, gluePackage, tagsExpression);
    }
    
    /**
     * Utility class for creating step definition instances
     * 
     * @param <T> Step definition type
     */
    public static class StepDefinitionBuilder<T> {
        private final Class<T> stepDefClass;
        private final Map<String, Object> dependencies = new HashMap<>();
        
        /**
         * Create a builder for a step definition class
         * 
         * @param stepDefClass Step definition class
         */
        private StepDefinitionBuilder(Class<T> stepDefClass) {
            this.stepDefClass = stepDefClass;
        }
        
        /**
         * Add a dependency
         * 
         * @param <D> Dependency type
         * @param name Dependency name
         * @param dependency Dependency instance
         * @return This builder
         */
        public <D> StepDefinitionBuilder<T> withDependency(String name, D dependency) {
            dependencies.put(name, dependency);
            return this;
        }
        
        /**
         * Add a dependency supplier
         * 
         * @param <D> Dependency type
         * @param name Dependency name
         * @param supplier Dependency supplier
         * @return This builder
         */
        public <D> StepDefinitionBuilder<T> withDependencySupplier(String name, Supplier<D> supplier) {
            dependencies.put(name, supplier.get());
            return this;
        }
        
        /**
         * Build the step definition instance
         * 
         * @return Step definition instance
         */
        public T build() {
            try {
                // Try to create instance using reflection
                T instance = stepDefClass.getDeclaredConstructor().newInstance();
                
                // Register it
                registerStepDefinition(stepDefClass.getSimpleName(), instance);
                
                return instance;
            } catch (Exception e) {
                logger.error("Failed to create step definition instance: {}", stepDefClass.getName(), e);
                throw new RuntimeException("Failed to create step definition instance: " + stepDefClass.getName(), e);
            }
        }
    }
    
    /**
     * Create a step definition builder
     * 
     * @param <T> Step definition type
     * @param stepDefClass Step definition class
     * @return Step definition builder
     */
    public static <T> StepDefinitionBuilder<T> createStepDefinition(Class<T> stepDefClass) {
        return new StepDefinitionBuilder<>(stepDefClass);
    }
    
    /**
     * Create a base step definition instance
     * 
     * @param <T> Step definition type
     * @param stepDefClass Step definition class that extends CSBaseStepDefinition
     * @return Step definition instance
     */
    @SuppressWarnings("unchecked")
    public static <T extends CSBaseStepDefinition> T createBaseStepDefinition(Class<T> stepDefClass) {
        try {
            T instance = stepDefClass.getDeclaredConstructor().newInstance();
            registerStepDefinition(stepDefClass.getSimpleName(), instance);
            return instance;
        } catch (Exception e) {
            logger.error("Failed to create base step definition instance: {}", stepDefClass.getName(), e);
            throw new RuntimeException("Failed to create base step definition instance: " + stepDefClass.getName(), e);
        }
    }
} 