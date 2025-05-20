package com.cstestforge.framework.selenium.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JSON data reader for test data management.
 * Supports both array-based and object-based JSON structures.
 */
public class CSJsonReader {
    private static final Logger logger = LoggerFactory.getLogger(CSJsonReader.class);
    
    private final String filePath;
    private final ObjectMapper mapper;
    private JsonNode rootNode;
    
    /**
     * Constructor that loads the JSON file
     * 
     * @param filePath Path to the JSON file
     * @throws IOException If file cannot be read
     */
    public CSJsonReader(String filePath) throws IOException {
        this.filePath = filePath;
        this.mapper = new ObjectMapper();
        loadJsonFile();
    }
    
    /**
     * Load JSON file
     * 
     * @throws IOException If file cannot be read
     */
    private void loadJsonFile() throws IOException {
        logger.debug("Loading JSON file: {}", filePath);
        
        try {
            byte[] jsonData = Files.readAllBytes(Paths.get(filePath));
            rootNode = mapper.readTree(jsonData);
            logger.debug("JSON file loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load JSON file: {}", filePath, e);
            throw e;
        }
    }
    
    /**
     * Get all data as a list of maps
     * 
     * @return List of data objects as maps
     * @throws IOException If JSON cannot be processed
     */
    public List<Map<String, Object>> getData() throws IOException {
        if (rootNode == null) {
            return new ArrayList<>();
        }
        
        // Handle array of objects
        if (rootNode.isArray()) {
            return mapper.convertValue(
                rootNode,
                new TypeReference<List<Map<String, Object>>>() {}
            );
        }
        
        // Handle object with array property
        for (String fieldName : getFieldNames()) {
            JsonNode node = rootNode.get(fieldName);
            if (node != null && node.isArray()) {
                return mapper.convertValue(
                    node,
                    new TypeReference<List<Map<String, Object>>>() {}
                );
            }
        }
        
        // Handle a single object
        if (rootNode.isObject()) {
            List<Map<String, Object>> result = new ArrayList<>();
            result.add(mapper.convertValue(
                rootNode,
                new TypeReference<Map<String, Object>>() {}
            ));
            return result;
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Get data filtered by a simple expression
     * 
     * @param filterExpression Simple filter expression (e.g., "field=value")
     * @return Filtered list of data
     * @throws IOException If JSON cannot be processed
     */
    public List<Map<String, Object>> getFilteredData(String filterExpression) throws IOException {
        List<Map<String, Object>> allData = getData();
        
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return allData;
        }
        
        logger.debug("Filtering data with expression: {}", filterExpression);
        
        // Support for complex AND/OR expressions
        if (filterExpression.contains(" AND ") || filterExpression.contains(" OR ")) {
            return evaluateComplexFilter(allData, filterExpression);
        }
        
        // Simple expression
        String[] parts = filterExpression.split("(=|>|<|!=|>=|<=)");
        if (parts.length != 2) {
            logger.warn("Invalid filter expression: {}", filterExpression);
            return allData;
        }
        
        String field = parts[0].trim();
        String value = parts[1].trim();
        String operator = filterExpression.substring(
                field.length(), 
                filterExpression.length() - value.length()
        ).trim();
        
        return allData.stream()
                .filter(row -> evaluateFilter(row, field, operator, value))
                .collect(Collectors.toList());
    }
    
    /**
     * Evaluate a complex filter with AND/OR operators
     * 
     * @param data Data to filter
     * @param expression Complex filter expression
     * @return Filtered data
     */
    private List<Map<String, Object>> evaluateComplexFilter(List<Map<String, Object>> data, String expression) {
        // Split by OR first (lower precedence)
        if (expression.contains(" OR ")) {
            String[] orExpressions = expression.split(" OR ");
            List<Map<String, Object>> result = new ArrayList<>();
            
            for (String orExpression : orExpressions) {
                result.addAll(evaluateComplexFilter(data, orExpression));
            }
            
            return result.stream().distinct().collect(Collectors.toList());
        }
        
        // Then process AND expressions
        if (expression.contains(" AND ")) {
            String[] andExpressions = expression.split(" AND ");
            List<Map<String, Object>> result = data;
            
            for (String andExpression : andExpressions) {
                result = evaluateComplexFilter(result, andExpression);
            }
            
            return result;
        }
        
        // Base case: simple expression
        String[] parts = expression.split("(=|>|<|!=|>=|<=)");
        if (parts.length != 2) {
            logger.warn("Invalid filter expression part: {}", expression);
            return data;
        }
        
        String field = parts[0].trim();
        String value = parts[1].trim();
        String operator = expression.substring(
                field.length(), 
                expression.length() - value.length()
        ).trim();
        
        return data.stream()
                .filter(row -> evaluateFilter(row, field, operator, value))
                .collect(Collectors.toList());
    }
    
    /**
     * Evaluate a simple filter condition
     * 
     * @param row Data map
     * @param field Field name
     * @param operator Comparison operator
     * @param value Value to compare
     * @return True if condition is met
     */
    private boolean evaluateFilter(Map<String, Object> row, String field, String operator, String value) {
        if (!row.containsKey(field)) {
            return false;
        }
        
        Object fieldValue = row.get(field);
        if (fieldValue == null) {
            return "=".equals(operator) && "null".equals(value);
        }
        
        // Handle numeric comparisons
        if (fieldValue instanceof Number && isNumeric(value)) {
            double numValue = ((Number) fieldValue).doubleValue();
            double compareValue = Double.parseDouble(value);
            
            switch (operator) {
                case "=":  return numValue == compareValue;
                case "!=": return numValue != compareValue;
                case ">":  return numValue > compareValue;
                case "<":  return numValue < compareValue;
                case ">=": return numValue >= compareValue;
                case "<=": return numValue <= compareValue;
                default:   return false;
            }
        }
        
        // Handle boolean comparisons
        if (fieldValue instanceof Boolean && ("true".equals(value) || "false".equals(value))) {
            boolean boolValue = (Boolean) fieldValue;
            boolean compareValue = Boolean.parseBoolean(value);
            
            switch (operator) {
                case "=":  return boolValue == compareValue;
                case "!=": return boolValue != compareValue;
                default:   return false;
            }
        }
        
        // Default string comparison
        String stringValue = fieldValue.toString();
        switch (operator) {
            case "=":  return stringValue.equals(value);
            case "!=": return !stringValue.equals(value);
            case ">":  return stringValue.compareTo(value) > 0;
            case "<":  return stringValue.compareTo(value) < 0;
            case ">=": return stringValue.compareTo(value) >= 0;
            case "<=": return stringValue.compareTo(value) <= 0;
            default:   return false;
        }
    }
    
    /**
     * Get a specific object by ID or index
     * 
     * @param idOrIndex ID field value or array index
     * @param idField Field name to use as ID (defaults to "id")
     * @return Data object as map
     * @throws IOException If JSON cannot be processed
     */
    public Map<String, Object> getObjectById(String idOrIndex, String idField) throws IOException {
        List<Map<String, Object>> data = getData();
        
        // Handle numeric index
        if (isNumeric(idOrIndex)) {
            int index = Integer.parseInt(idOrIndex);
            if (index >= 0 && index < data.size()) {
                return data.get(index);
            }
        }
        
        // Handle id field matching
        idField = idField != null ? idField : "id";
        for (Map<String, Object> item : data) {
            if (item.containsKey(idField) && idOrIndex.equals(item.get(idField).toString())) {
                return item;
            }
        }
        
        return new HashMap<>();
    }
    
    /**
     * Get a specific object by ID, using "id" as the default ID field
     * 
     * @param idOrIndex ID field value or array index
     * @return Data object as map
     * @throws IOException If JSON cannot be processed
     */
    public Map<String, Object> getObjectById(String idOrIndex) throws IOException {
        return getObjectById(idOrIndex, "id");
    }
    
    /**
     * Get field names from the root object
     * 
     * @return List of field names
     */
    public List<String> getFieldNames() {
        List<String> fieldNames = new ArrayList<>();
        if (rootNode != null && rootNode.isObject()) {
            rootNode.fieldNames().forEachRemaining(fieldNames::add);
        }
        return fieldNames;
    }
    
    /**
     * Check if a string can be parsed as a number
     * 
     * @param str String to check
     * @return True if the string is a valid number
     */
    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Read a JSON file and return data as a list of maps
     * 
     * @param jsonFile JSON file to read
     * @return List of data objects
     * @throws IOException If file cannot be read
     */
    public static List<Map<String, Object>> readJsonFile(File jsonFile) throws IOException {
        CSJsonReader reader = new CSJsonReader(jsonFile.getAbsolutePath());
        return reader.getData();
    }
    
    /**
     * Create a data provider array for TestNG
     * 
     * @return Object array for TestNG data provider
     * @throws IOException If JSON cannot be processed
     */
    public Object[][] createDataProvider() throws IOException {
        List<Map<String, Object>> data = getData();
        Object[][] result = new Object[data.size()][1];
        
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        
        return result;
    }
    
    /**
     * Create a data provider array for TestNG, with filtering
     * 
     * @param filterExpression Filter expression
     * @return Object array for TestNG data provider
     * @throws IOException If JSON cannot be processed
     */
    public Object[][] createDataProvider(String filterExpression) throws IOException {
        List<Map<String, Object>> data = getFilteredData(filterExpression);
        Object[][] result = new Object[data.size()][1];
        
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = data.get(i);
        }
        
        return result;
    }
} 