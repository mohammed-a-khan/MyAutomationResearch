package com.cstestforge.framework.selenium.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Excel data reader for test data management.
 * Supports both .xls and .xlsx formats.
 */
public class CSExcelReader {
    private static final Logger logger = LoggerFactory.getLogger(CSExcelReader.class);
    
    private final String filePath;
    private final Map<String, List<Map<String, Object>>> sheetData = new HashMap<>();
    
    /**
     * Constructor that loads the Excel file
     * 
     * @param filePath Path to the Excel file
     * @throws IOException If file cannot be read
     */
    public CSExcelReader(String filePath) throws IOException {
        this.filePath = filePath;
        loadExcelFile();
    }
    
    /**
     * Load Excel file and parse all sheets
     * 
     * @throws IOException If file cannot be read
     */
    private void loadExcelFile() throws IOException {
        logger.debug("Loading Excel file: {}", filePath);
        
        try (FileInputStream fis = new FileInputStream(new File(filePath));
             Workbook workbook = createWorkbook(fis, filePath)) {
            
            // Process each sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                logger.debug("Processing sheet: {}", sheetName);
                sheetData.put(sheetName, readSheet(sheet));
            }
            
            logger.debug("Excel file loaded successfully");
        } catch (IOException e) {
            logger.error("Failed to load Excel file: {}", filePath, e);
            throw e;
        }
    }
    
    /**
     * Create workbook based on file extension
     * 
     * @param fis FileInputStream for the Excel file
     * @param filePath Path to the Excel file
     * @return Workbook instance
     * @throws IOException If file cannot be read
     */
    private Workbook createWorkbook(FileInputStream fis, String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IOException("Unsupported file format. Only .xls and .xlsx are supported.");
        }
    }
    
    /**
     * Read a single sheet and convert to list of maps
     * 
     * @param sheet Sheet to read
     * @return List of data rows as maps
     */
    private List<Map<String, Object>> readSheet(Sheet sheet) {
        List<Map<String, Object>> data = new ArrayList<>();
        
        // Get header row for column names
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            logger.warn("Sheet {} has no header row", sheet.getSheetName());
            return data;
        }
        
        // Get column names from header row
        List<String> headers = new ArrayList<>();
        for (Cell cell : headerRow) {
            headers.add(getCellValueAsString(cell));
        }
        
        // Process data rows
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            Map<String, Object> rowData = new HashMap<>();
            boolean hasData = false;
            
            // Read each cell in the row
            for (int j = 0; j < headers.size(); j++) {
                Cell cell = row.getCell(j);
                if (cell != null) {
                    Object value = getCellValue(cell);
                    rowData.put(headers.get(j), value);
                    if (value != null && !value.toString().trim().isEmpty()) {
                        hasData = true;
                    }
                }
            }
            
            // Only add rows with data
            if (hasData) {
                data.add(rowData);
            }
        }
        
        return data;
    }
    
    /**
     * Get cell value as appropriate type
     * 
     * @param cell Cell to get value from
     * @return Cell value as Object
     */
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    // Check if it's an integer value
                    double numValue = cell.getNumericCellValue();
                    if (numValue == Math.floor(numValue)) {
                        return (long) numValue;
                    }
                    return numValue;
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                try {
                    return cell.getNumericCellValue();
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e2) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return "";
            default:
                return null;
        }
    }
    
    /**
     * Get cell value as string
     * 
     * @param cell Cell to get value from
     * @return Cell value as String
     */
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value == null ? "" : value.toString();
    }
    
    /**
     * Get all data from a specific sheet
     * 
     * @param sheetName Name of the sheet
     * @return List of data rows as maps
     */
    public List<Map<String, Object>> getSheetData(String sheetName) {
        return sheetData.getOrDefault(sheetName, new ArrayList<>());
    }
    
    /**
     * Get filtered data from a sheet
     * 
     * @param sheetName Name of the sheet
     * @param filterExpression Simple filter expression (e.g., "column=value")
     * @return Filtered list of data
     */
    public List<Map<String, Object>> getFilteredData(String sheetName, String filterExpression) {
        List<Map<String, Object>> allData = getSheetData(sheetName);
        
        if (filterExpression == null || filterExpression.trim().isEmpty()) {
            return allData;
        }
        
        logger.debug("Filtering data with expression: {}", filterExpression);
        
        // Parse filter expression
        String[] parts = filterExpression.split("(=|>|<|!=|>=|<=)");
        if (parts.length != 2) {
            logger.warn("Invalid filter expression: {}", filterExpression);
            return allData;
        }
        
        String column = parts[0].trim();
        String value = parts[1].trim();
        String operator = filterExpression.substring(
                column.length(), 
                filterExpression.length() - value.length()
        ).trim();
        
        // Apply filter
        return allData.stream()
                .filter(row -> evaluateFilter(row, column, operator, value))
                .collect(Collectors.toList());
    }
    
    /**
     * Evaluate a filter condition on a data row
     * 
     * @param row Data row
     * @param column Column name
     * @param operator Comparison operator
     * @param value Value to compare
     * @return True if condition is met
     */
    private boolean evaluateFilter(Map<String, Object> row, String column, String operator, String value) {
        if (!row.containsKey(column)) {
            return false;
        }
        
        Object cellValue = row.get(column);
        if (cellValue == null) {
            return "=".equals(operator) && "null".equals(value);
        }
        
        // Handle numeric comparisons
        if (cellValue instanceof Number && isNumeric(value)) {
            double numValue = ((Number) cellValue).doubleValue();
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
        
        // Handle string comparisons
        String stringValue = cellValue.toString();
        switch (operator) {
            case "=":  return stringValue.equals(value);
            case "!=": return !stringValue.equals(value);
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
     * Get the names of all sheets in the Excel file
     * 
     * @return List of sheet names
     */
    public List<String> getSheetNames() {
        return new ArrayList<>(sheetData.keySet());
    }
} 