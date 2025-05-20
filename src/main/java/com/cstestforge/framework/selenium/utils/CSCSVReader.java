package com.cstestforge.framework.selenium.utils;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CSV data reader for test data management.
 * Supports CSV files with headers and filtering capabilities.
 */
public class CSCSVReader {
    private static final Logger logger = LoggerFactory.getLogger(CSCSVReader.class);
    
    private final String filePath;
    private final List<Map<String, String>> data = new ArrayList<>();
    private final char separator;
    private final boolean hasHeader;
    private final int skipLines;
    
    /**
     * Constructor with default settings
     * 
     * @param filePath Path to the CSV file
     * @throws IOException If file cannot be read
     */
    public CSCSVReader(String filePath) throws IOException {
        this(filePath, ',', true, 0);
    }
    
    /**
     * Constructor with custom settings
     * 
     * @param filePath Path to the CSV file
     * @param separator CSV separator character
     * @param hasHeader Whether the CSV has a header row
     * @param skipLines Number of lines to skip before header
     * @throws IOException If file cannot be read
     */
    public CSCSVReader(String filePath, char separator, boolean hasHeader, int skipLines) throws IOException {
        this.filePath = filePath;
        this.separator = separator;
        this.hasHeader = hasHeader;
        this.skipLines = skipLines;
        loadCsvFile();
    }
    
    /**
     * Load and parse the CSV file
     * 
     * @throws IOException If file cannot be read
     */
    private void loadCsvFile() throws IOException {
        logger.debug("Loading CSV file: {}", filePath);
        
        try (Reader reader = new FileReader(filePath);
             CSVReader csvReader = new CSVReaderBuilder(reader)
                 .withSkipLines(skipLines)
                 .build()) {
            
            List<String[]> records = csvReader.readAll();
            
            if (records.isEmpty()) {
                logger.warn("CSV file is empty: {}", filePath);
                return;
            }
            
            if (hasHeader) {
                String[] headers = records.get(0);
                for (int i = 1; i < records.size(); i++) {
                    String[] record = records.get(i);
                    Map<String, String> rowData = new HashMap<>();
                    boolean hasData = false;
                    
                    for (int j = 0; j < Math.min(headers.length, record.length); j++) {
                        String value = record[j];
                        rowData.put(headers[j], value);
                        if (value != null && !value.trim().isEmpty()) {
                            hasData = true;
                        }
                    }
                    
                    if (hasData) {
                        data.add(rowData);
                    }
                }
            } else {
                // No header - use column indices as keys
                for (String[] record : records) {
                    Map<String, String> rowData = new HashMap<>();
                    boolean hasData = false;
                    
                    for (int j = 0; j < record.length; j++) {
                        String value = record[j];
                        rowData.put("Column" + (j + 1), value);
                        if (value != null && !value.trim().isEmpty()) {
                            hasData = true;
                        }
                    }
                    
                    if (hasData) {
                        data.add(rowData);
                    }
                }
            }
            
            logger.debug("CSV file loaded successfully with {} records", data.size());
        } catch (CsvException e) {
            logger.error("Failed to parse CSV file: {}", filePath, e);
            throw new IOException("Failed to parse CSV file", e);
        }
    }
    
    /**
     * Get all data from the CSV file
     * 
     * @return List of data rows as maps
     */
    public List<Map<String, String>> getData() {
        return new ArrayList<>(data);
    }
    
    /**
     * Get filtered data from the CSV file
     * 
     * @param filterExpression Simple filter expression (e.g., "column=value")
     * @return Filtered list of data
     */
    public List<Map<String, String>> getFilteredData(String filterExpression) {
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return getData();
        }
        
        logger.debug("Filtering data with expression: {}", filterExpression);
        
        // Support for complex AND/OR expressions
        if (filterExpression.contains(" AND ") || filterExpression.contains(" OR ")) {
            return evaluateComplexFilter(data, filterExpression);
        }
        
        // Simple expression
        String[] parts = filterExpression.split("(=|>|<|!=|>=|<=)");
        if (parts.length != 2) {
            logger.warn("Invalid filter expression: {}", filterExpression);
            return data;
        }
        
        String column = parts[0].trim();
        String value = parts[1].trim();
        String operator = filterExpression.substring(
                column.length(), 
                filterExpression.length() - value.length()
        ).trim();
        
        return data.stream()
                .filter(row -> evaluateFilter(row, column, operator, value))
                .collect(Collectors.toList());
    }
    
    /**
     * Evaluate a complex filter with AND/OR operators
     * 
     * @param data Data to filter
     * @param expression Complex filter expression
     * @return Filtered data
     */
    private List<Map<String, String>> evaluateComplexFilter(List<Map<String, String>> data, String expression) {
        // Split by OR first (lower precedence)
        if (expression.contains(" OR ")) {
            String[] orExpressions = expression.split(" OR ");
            List<Map<String, String>> result = new ArrayList<>();
            
            for (String orExpression : orExpressions) {
                result.addAll(evaluateComplexFilter(data, orExpression));
            }
            
            return result.stream().distinct().collect(Collectors.toList());
        }
        
        // Then process AND expressions
        if (expression.contains(" AND ")) {
            String[] andExpressions = expression.split(" AND ");
            List<Map<String, String>> result = data;
            
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
        
        String column = parts[0].trim();
        String value = parts[1].trim();
        String operator = expression.substring(
                column.length(), 
                expression.length() - value.length()
        ).trim();
        
        return data.stream()
                .filter(row -> evaluateFilter(row, column, operator, value))
                .collect(Collectors.toList());
    }
    
    /**
     * Evaluate a simple filter condition
     * 
     * @param row Data map
     * @param column Column name
     * @param operator Comparison operator
     * @param value Value to compare
     * @return True if condition is met
     */
    private boolean evaluateFilter(Map<String, String> row, String column, String operator, String value) {
        if (!row.containsKey(column)) {
            return false;
        }
        
        String cellValue = row.get(column);
        if (cellValue == null) {
            return "=".equals(operator) && "null".equals(value);
        }
        
        // Try numeric comparison if both values are numeric
        if (isNumeric(cellValue) && isNumeric(value)) {
            double numValue = Double.parseDouble(cellValue);
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
        
        // String comparison
        switch (operator) {
            case "=":  return cellValue.equals(value);
            case "!=": return !cellValue.equals(value);
            case ">":  return cellValue.compareTo(value) > 0;
            case "<":  return cellValue.compareTo(value) < 0;
            case ">=": return cellValue.compareTo(value) >= 0;
            case "<=": return cellValue.compareTo(value) <= 0;
            default:   return false;
        }
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
     * Get column names from the CSV file
     * 
     * @return List of column names
     */
    public List<String> getColumnNames() {
        if (data.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(data.get(0).keySet());
    }
    
    /**
     * Get a specific row by its index
     * 
     * @param index Row index (0-based)
     * @return Row data as map
     */
    public Map<String, String> getRow(int index) {
        if (index < 0 || index >= data.size()) {
            return new HashMap<>();
        }
        return new HashMap<>(data.get(index));
    }
    
    /**
     * Create a data provider array for TestNG
     * 
     * @return Object array for TestNG data provider
     */
    public Object[][] createDataProvider() {
        Object[][] result = new Object[data.size()][1];
        
        for (int i = 0; i < data.size(); i++) {
            result[i][0] = new HashMap<>(data.get(i));
        }
        
        return result;
    }
    
    /**
     * Create a data provider array for TestNG, with filtering
     * 
     * @param filterExpression Filter expression
     * @return Object array for TestNG data provider
     */
    public Object[][] createDataProvider(String filterExpression) {
        List<Map<String, String>> filteredData = getFilteredData(filterExpression);
        Object[][] result = new Object[filteredData.size()][1];
        
        for (int i = 0; i < filteredData.size(); i++) {
            result[i][0] = filteredData.get(i);
        }
        
        return result;
    }
    
    /**
     * Get data as a typed object list
     * 
     * @param <T> Type of objects to create
     * @param mapper Function to map row data to object
     * @return List of typed objects
     */
    public <T> List<T> getDataAsList(CSVRowMapper<T> mapper) {
        return data.stream()
                .map(mapper::mapRow)
                .collect(Collectors.toList());
    }
    
    /**
     * Interface for mapping CSV rows to typed objects
     * 
     * @param <T> Type of object to create
     */
    public interface CSVRowMapper<T> {
        /**
         * Map a row of CSV data to an object
         * 
         * @param row Row data
         * @return Created object
         */
        T mapRow(Map<String, String> row);
    }
} 