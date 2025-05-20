package com.cstestforge.framework.selenium.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for providing test data from various data sources.
 * Supports CSV, JSON, Excel data sources with filtering capabilities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface CSDataProvider {
    /**
     * Source file name relative to the test data directory.
     * 
     * @return Source file name
     */
    String source() default "";
    
    /**
     * Type of data source.
     * 
     * @return Source type
     */
    SourceType sourceType() default SourceType.CSV;
    
    /**
     * Filter expression to filter data records.
     * Examples:
     * - "name=John"
     * - "age>18"
     * - "status=active AND country=US"
     * - "role=admin OR role=manager"
     * 
     * @return Filter expression
     */
    String filter() default "";
    
    /**
     * Sheet name for Excel data sources.
     * 
     * @return Sheet name
     */
    String sheetName() default "Sheet1";
    
    /**
     * Types of supported data sources.
     */
    enum SourceType {
        /** CSV file source */
        CSV,
        
        /** JSON file source */
        JSON,
        
        /** Excel file source */
        EXCEL
    }
} 