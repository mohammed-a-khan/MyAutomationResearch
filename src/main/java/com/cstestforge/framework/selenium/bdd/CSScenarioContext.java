package com.cstestforge.framework.selenium.bdd;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides a context for sharing data between BDD steps.
 * Allows storing and retrieving values during test execution.
 */
public class CSScenarioContext {
    
    /**
     * Different types of context scopes
     */
    public enum ContextScope {
        /** Scenario level scope - cleared after each scenario */
        SCENARIO,
        
        /** Feature level scope - persists across scenarios in a feature */
        FEATURE,
        
        /** Suite level scope - persists across features in a test suite */
        SUITE,
        
        /** Global scope - persists across all test executions until explicitly cleared */
        GLOBAL
    }
    
    // Thread-safe storage for context values across different scopes
    private static final Map<ContextScope, Map<String, Object>> contextStore = new ConcurrentHashMap<>();
    
    // Initialize the context maps for each scope
    static {
        for (ContextScope scope : ContextScope.values()) {
            contextStore.put(scope, new ConcurrentHashMap<>());
        }
    }
    
    /**
     * Store a value in the context with a given key and scope.
     * 
     * @param <T> Type of the value
     * @param key Key to store the value under
     * @param value Value to store
     * @param scope Scope in which to store the value
     */
    public <T> void set(String key, T value, ContextScope scope) {
        if (key == null) {
            throw new IllegalArgumentException("Context key cannot be null");
        }
        
        Map<String, Object> scopeMap = contextStore.get(scope);
        scopeMap.put(key, value);
    }
    
    /**
     * Store a value in the scenario scope (default).
     * 
     * @param <T> Type of the value
     * @param key Key to store the value under
     * @param value Value to store
     */
    public <T> void set(String key, T value) {
        set(key, value, ContextScope.SCENARIO);
    }
    
    /**
     * Get a typed value from the context. Will look through all scopes in order
     * of precedence: SCENARIO, FEATURE, SUITE, GLOBAL.
     * 
     * @param <T> Expected type of the value
     * @param key Key to look up
     * @param type Class of the expected type
     * @return The value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        if (key == null) {
            throw new IllegalArgumentException("Context key cannot be null");
        }
        
        // Look through scopes in order of precedence
        for (ContextScope scope : ContextScope.values()) {
            Map<String, Object> scopeMap = contextStore.get(scope);
            if (scopeMap.containsKey(key)) {
                Object value = scopeMap.get(key);
                if (value == null || type.isInstance(value)) {
                    return (T) value;
                } else {
                    throw new ClassCastException(
                            "Value for key '" + key + "' is of type " + 
                            value.getClass().getName() + " but requested as " + 
                            type.getName());
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get a value from a specific scope.
     * 
     * @param <T> Expected type of the value
     * @param key Key to look up
     * @param type Class of the expected type
     * @param scope Scope to look in
     * @return The value or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, ContextScope scope) {
        if (key == null) {
            throw new IllegalArgumentException("Context key cannot be null");
        }
        
        Map<String, Object> scopeMap = contextStore.get(scope);
        if (scopeMap.containsKey(key)) {
            Object value = scopeMap.get(key);
            if (value == null || type.isInstance(value)) {
                return (T) value;
            } else {
                throw new ClassCastException(
                        "Value for key '" + key + "' is of type " + 
                        value.getClass().getName() + " but requested as " + 
                        type.getName());
            }
        }
        
        return null;
    }
    
    /**
     * Get value with default fallback.
     * 
     * @param <T> Expected type of the value
     * @param key Key to look up
     * @param type Class of the expected type
     * @param defaultValue Default value if key not found
     * @return The value or defaultValue if not found
     */
    public <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Check if a key exists in any scope.
     * 
     * @param key Key to look up
     * @return True if the key exists in any scope
     */
    public boolean containsKey(String key) {
        if (key == null) {
            return false;
        }
        
        for (ContextScope scope : ContextScope.values()) {
            Map<String, Object> scopeMap = contextStore.get(scope);
            if (scopeMap.containsKey(key)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if a key exists in a specific scope.
     * 
     * @param key Key to look up
     * @param scope Scope to look in
     * @return True if the key exists in the scope
     */
    public boolean containsKey(String key, ContextScope scope) {
        if (key == null) {
            return false;
        }
        
        Map<String, Object> scopeMap = contextStore.get(scope);
        return scopeMap.containsKey(key);
    }
    
    /**
     * Remove a value from all scopes.
     * 
     * @param key Key to remove
     */
    public void remove(String key) {
        if (key == null) {
            return;
        }
        
        for (ContextScope scope : ContextScope.values()) {
            Map<String, Object> scopeMap = contextStore.get(scope);
            scopeMap.remove(key);
        }
    }
    
    /**
     * Remove a value from a specific scope.
     * 
     * @param key Key to remove
     * @param scope Scope to remove from
     */
    public void remove(String key, ContextScope scope) {
        if (key == null) {
            return;
        }
        
        Map<String, Object> scopeMap = contextStore.get(scope);
        scopeMap.remove(key);
    }
    
    /**
     * Clear all values in the scenario scope.
     * To be called after each scenario.
     */
    public void clearScenarioScope() {
        contextStore.get(ContextScope.SCENARIO).clear();
    }
    
    /**
     * Clear all values in the feature scope.
     * To be called after each feature.
     */
    public void clearFeatureScope() {
        contextStore.get(ContextScope.FEATURE).clear();
    }
    
    /**
     * Clear all values in the suite scope.
     * To be called after the test suite.
     */
    public void clearSuiteScope() {
        contextStore.get(ContextScope.SUITE).clear();
    }
    
    /**
     * Clear all values in all scopes.
     * Use with caution as it will clear global scope as well.
     */
    public void clearAll() {
        for (ContextScope scope : ContextScope.values()) {
            contextStore.get(scope).clear();
        }
    }
    
    /**
     * Get a snapshot of all values in a specific scope.
     * 
     * @param scope Scope to get values from
     * @return Map of all values in the scope
     */
    public Map<String, Object> getScopeSnapshot(ContextScope scope) {
        return new HashMap<>(contextStore.get(scope));
    }
} 