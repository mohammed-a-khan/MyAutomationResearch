package com.cstestforge.framework.selenium.annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Processor for the CSDataProvider annotation.
 * Provides data for tests from various sources (CSV, Excel, JSON, etc.)
 */
public class CSDataProviderProcessor {
    private static final Logger logger = LoggerFactory.getLogger(CSDataProviderProcessor.class);
    private static final String DATA_DIR = "src/test/resources/testdata/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Data provider method used by TestNG to supply test data
     * 
     * @param method Test method
     * @return Iterator of objects for test data
     */
    @DataProvider(name = "CSDataProvider")
    public static Iterator<Object[]> provideData(Method method) {
        logger.info("Processing data provider for method: {}", method.getName());
        
        CSDataProvider annotation = method.getAnnotation(CSDataProvider.class);
        if (annotation == null) {
            logger.warn("CSDataProvider annotation not found on method: {}", method.getName());
            return List.<Object[]>of(new Object[]{}).iterator();
        }
        
        String source = annotation.source();
        CSDataProvider.SourceType sourceType = annotation.sourceType();
        
        switch (sourceType) {
            case CSV:
                return readCsvData(source, annotation.filter()).iterator();
            case JSON:
                return readJsonData(source, annotation.filter()).iterator();
            case EXCEL:
                return readExcelData(source, annotation.filter(), annotation.sheetName()).iterator();
            default:
                logger.error("Unsupported data source type: {}", sourceType);
                return List.<Object[]>of(new Object[]{}).iterator();
        }
    }
    
    /**
     * Read CSV data from file
     * 
     * @param fileName CSV file name
     * @param filter Filter expression
     * @return List of test data objects
     */
    private static List<Object[]> readCsvData(String fileName, String filter) {
        List<Object[]> testData = new ArrayList<>();
        Path filePath = Paths.get(DATA_DIR, fileName);
        
        try (Reader reader = new FileReader(filePath.toFile());
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            
            List<CSVRecord> records = csvParser.getRecords();
            logger.info("Read {} records from CSV file: {}", records.size(), fileName);
            
            for (CSVRecord record : records) {
                Map<String, String> dataMap = new HashMap<>();
                csvParser.getHeaderNames().forEach(header -> 
                    dataMap.put(header, record.get(header)));
                
                // Apply filter if specified
                if (filter == null || filter.isEmpty() || evaluateFilter(dataMap, filter)) {
                    testData.add(new Object[]{dataMap});
                }
            }
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", fileName, e);
        }
        
        return testData;
    }
    
    /**
     * Read JSON data from file
     * 
     * @param fileName JSON file name
     * @param filter Filter expression
     * @return List of test data objects
     */
    private static List<Object[]> readJsonData(String fileName, String filter) {
        List<Object[]> testData = new ArrayList<>();
        Path filePath = Paths.get(DATA_DIR, fileName);
        
        try {
            // Parse the JSON content
            List<Map<String, Object>> jsonData;
            
            // Handle both array and single object JSON formats
            if (Files.readString(filePath).trim().startsWith("[")) {
                // JSON array format
                jsonData = objectMapper.readValue(filePath.toFile(), 
                        new TypeReference<List<Map<String, Object>>>() {});
            } else {
                // Single object format
                Map<String, Object> singleData = objectMapper.readValue(filePath.toFile(), 
                        new TypeReference<Map<String, Object>>() {});
                jsonData = List.of(singleData);
            }
            
            logger.info("Read {} records from JSON file: {}", jsonData.size(), fileName);
            
            for (Map<String, Object> item : jsonData) {
                // Convert all values to strings for consistent filtering
                Map<String, String> stringDataMap = new HashMap<>();
                item.forEach((k, v) -> stringDataMap.put(k, v == null ? "" : v.toString()));
                
                // Apply filter if specified
                if (filter == null || filter.isEmpty() || evaluateFilter(stringDataMap, filter)) {
                    testData.add(new Object[]{item});
                }
            }
        } catch (IOException e) {
            logger.error("Error reading JSON file: {}", fileName, e);
        }
        
        return testData;
    }
    
    /**
     * Read Excel data from file
     * 
     * @param fileName Excel file name
     * @param filter Filter expression
     * @param sheetName Sheet name
     * @return List of test data objects
     */
    private static List<Object[]> readExcelData(String fileName, String filter, String sheetName) {
        List<Object[]> testData = new ArrayList<>();
        Path filePath = Paths.get(DATA_DIR, fileName);
        
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            Workbook workbook;
            
            // Determine workbook type by file extension
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IOException("Unsupported Excel file format: " + fileName);
            }
            
            // Get the specified sheet or first sheet if not specified
            Sheet sheet;
            if (sheetName != null && !sheetName.isEmpty()) {
                sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    logger.warn("Sheet {} not found in workbook. Using first sheet.", sheetName);
                    sheet = workbook.getSheetAt(0);
                }
            } else {
                sheet = workbook.getSheetAt(0);
            }
            
            // Get header row and validate
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                logger.error("Header row not found in Excel file: {}", fileName);
                return testData;
            }
            
            // Extract header names
            List<String> headers = StreamSupport.stream(headerRow.spliterator(), false)
                .map(cell -> cell.getStringCellValue().trim())
                .collect(Collectors.toList());
            
            // Process data rows
            int rowCount = sheet.getPhysicalNumberOfRows();
            logger.info("Processing {} rows from Excel file: {}", rowCount - 1, fileName);
            
            for (int i = 1; i < rowCount; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                Map<String, String> dataMap = new HashMap<>();
                
                // Map each cell to its header
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = extractCellValue(cell);
                    dataMap.put(headers.get(j), value);
                }
                
                // Apply filter if specified
                if (filter == null || filter.isEmpty() || evaluateFilter(dataMap, filter)) {
                    testData.add(new Object[]{dataMap});
                }
            }
            
            workbook.close();
        } catch (IOException e) {
            logger.error("Error reading Excel file: {}", fileName, e);
        }
        
        return testData;
    }
    
    /**
     * Extract string value from Excel cell accounting for different cell types
     * 
     * @param cell Excel cell
     * @return String value of cell contents
     */
    private static String extractCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                // Check if date
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                // Prevent scientific notation and trailing zeros
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
                return "";
            default:
                return "";
        }
    }
    
    /**
     * Evaluate filter expression against data
     * 
     * @param data Map of data values
     * @param filter Filter expression
     * @return True if data matches filter
     */
    private static boolean evaluateFilter(Map<String, String> data, String filter) {
        if (filter == null || filter.isEmpty()) {
            return true;
        }
        
        // Support complex expressions with AND, OR
        if (filter.contains(" AND ")) {
            String[] conditions = filter.split(" AND ");
            for (String condition : conditions) {
                if (!evaluateSingleCondition(data, condition.trim())) {
                    return false;
                }
            }
            return true;
        } else if (filter.contains(" OR ")) {
            String[] conditions = filter.split(" OR ");
            for (String condition : conditions) {
                if (evaluateSingleCondition(data, condition.trim())) {
                    return true;
                }
            }
            return false;
        } else {
            return evaluateSingleCondition(data, filter);
        }
    }
    
    /**
     * Evaluate a single filter condition
     * 
     * @param data Map of data values
     * @param condition Filter condition
     * @return True if data matches condition
     */
    private static boolean evaluateSingleCondition(Map<String, String> data, String condition) {
        // Support various comparison operations
        Pattern pattern = Pattern.compile("(.+?)(=|!=|>|<|>=|<=|CONTAINS|STARTSWITH|ENDSWITH|MATCHES)(.+)");
        Matcher matcher = pattern.matcher(condition);
        
        if (matcher.find()) {
            String field = matcher.group(1).trim();
            String operator = matcher.group(2).trim();
            String expectedValue = matcher.group(3).trim();
            
            // Remove quotes if present
            if (expectedValue.startsWith("'") && expectedValue.endsWith("'") ||
                expectedValue.startsWith("\"") && expectedValue.endsWith("\"")) {
                expectedValue = expectedValue.substring(1, expectedValue.length() - 1);
            }
            
            if (data.containsKey(field)) {
                String actualValue = data.get(field);
                
                switch (operator) {
                    case "=":
                        return actualValue.equals(expectedValue);
                    case "!=":
                        return !actualValue.equals(expectedValue);
                    case ">":
                        try {
                            return Double.parseDouble(actualValue) > Double.parseDouble(expectedValue);
                        } catch (NumberFormatException e) {
                            return actualValue.compareTo(expectedValue) > 0;
                        }
                    case "<":
                        try {
                            return Double.parseDouble(actualValue) < Double.parseDouble(expectedValue);
                        } catch (NumberFormatException e) {
                            return actualValue.compareTo(expectedValue) < 0;
                        }
                    case ">=":
                        try {
                            return Double.parseDouble(actualValue) >= Double.parseDouble(expectedValue);
                        } catch (NumberFormatException e) {
                            return actualValue.compareTo(expectedValue) >= 0;
                        }
                    case "<=":
                        try {
                            return Double.parseDouble(actualValue) <= Double.parseDouble(expectedValue);
                        } catch (NumberFormatException e) {
                            return actualValue.compareTo(expectedValue) <= 0;
                        }
                    case "CONTAINS":
                        return actualValue.contains(expectedValue);
                    case "STARTSWITH":
                        return actualValue.startsWith(expectedValue);
                    case "ENDSWITH":
                        return actualValue.endsWith(expectedValue);
                    case "MATCHES":
                        return actualValue.matches(expectedValue);
                    default:
                        return false;
                }
            }
        }
        
        return false;
    }
} 