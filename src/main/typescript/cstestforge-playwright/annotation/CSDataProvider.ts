import * as fs from 'fs';
import * as path from 'path';
import * as xlsx from 'xlsx';
import csvParser from 'csv-parser';

/**
 * Data provider configuration
 */
export interface CSDataProviderConfig {
    /** Data source name (e.g., 'userCredentials', 'productData') */
    name: string;
    /** Type of data source */
    type: 'excel' | 'csv' | 'json' | 'database';
    /** Path to the data file (for excel, csv, and json) */
    filePath?: string;
    /** Sheet name (for excel) */
    sheetName?: string;
    /** Key or row index to use (for excel/csv) */
    key?: string | number;
    /** Database connection details (for database type) */
    database?: {
        /** Database name */
        name: string;
        /** SQL query */
        query: string;
        /** Connection parameters */
        connection?: Record<string, any>;
    };
    /** Optional data transformation function */
    transform?: (data: any) => any;
}

// Define a type for Excel/CSV row data
interface DataRow {
    [key: string]: any;
}

/**
 * Registry of data providers and their data
 */
export class DataProviderRegistry {
    private static registry: Map<string, any[]> = new Map();
    private static configRegistry: Map<string, CSDataProviderConfig> = new Map();

    /**
     * Register a data provider
     */
    public static register(name: string, config: CSDataProviderConfig): void {
        this.configRegistry.set(name, config);
        // Data will be loaded on demand
    }

    /**
     * Get data for a data provider
     */
    public static async getData(name: string): Promise<any[]> {
        // Check if data is already loaded
        if (this.registry.has(name)) {
            return this.registry.get(name) || [];
        }

        // Get configuration
        const config = this.configRegistry.get(name);
        if (!config) {
            throw new Error(`Data provider "${name}" not found`);
        }

        // Load data based on configuration
        const data = await this.loadData(config);
        
        // Apply optional transformation
        let transformedData = data;
        if (config.transform) {
            transformedData = Array.isArray(data) 
                ? data.map(config.transform)
                : config.transform(data);
            
            if (!Array.isArray(transformedData)) {
                transformedData = [transformedData];
            }
        }
        
        // Store in registry
        this.registry.set(name, transformedData);
        
        return transformedData;
    }

    /**
     * Load data based on configuration
     */
    private static async loadData(config: CSDataProviderConfig): Promise<any[]> {
        switch (config.type) {
            case 'excel':
                return await this.loadExcelData(config);
            case 'csv':
                return await this.loadCsvData(config);
            case 'json':
                return this.loadJsonData(config);
            case 'database':
                return await this.loadDatabaseData(config);
            default:
                throw new Error(`Unsupported data provider type: ${(config as any).type}`);
        }
    }

    /**
     * Load data from Excel file
     */
    private static async loadExcelData(config: CSDataProviderConfig): Promise<DataRow[]> {
        if (!config.filePath) {
            throw new Error('File path is required for Excel data provider');
        }

        // Read Excel file
        const workbook = xlsx.readFile(config.filePath);
        
        // Get sheet
        const sheetName = config.sheetName || workbook.SheetNames[0];
        const sheet = workbook.Sheets[sheetName];
        
        if (!sheet) {
            throw new Error(`Sheet "${sheetName}" not found in Excel file`);
        }
        
        // Convert to JSON
        const data = xlsx.utils.sheet_to_json<DataRow>(sheet);
        
        // Filter by key if provided
        if (config.key !== undefined) {
            if (typeof config.key === 'number') {
                // Return specific row by index
                if (config.key >= 0 && config.key < data.length) {
                    return [data[config.key]];
                }
                throw new Error(`Row index ${config.key} out of bounds`);
            } else if (typeof config.key === 'string') {
                // Return rows that match a certain value in a column
                const [column, value] = config.key.split('=');
                if (column && value) {
                    return data.filter(row => String(row[column.trim()]) === value.trim());
                }
            }
        }
        
        return data;
    }

    /**
     * Load data from CSV file
     */
    private static async loadCsvData(config: CSDataProviderConfig): Promise<DataRow[]> {
        if (!config.filePath) {
            throw new Error('File path is required for CSV data provider');
        }

        return new Promise((resolve, reject) => {
            const results: DataRow[] = [];
            
            fs.createReadStream(config.filePath!)
                .pipe(csvParser())
                .on('data', (data: DataRow) => results.push(data))
                .on('end', () => {
                    // Filter by key if provided
                    if (config.key !== undefined) {
                        if (typeof config.key === 'number') {
                            // Return specific row by index
                            if (config.key >= 0 && config.key < results.length) {
                                resolve([results[config.key]]);
                                return;
                            }
                            reject(new Error(`Row index ${config.key} out of bounds`));
                            return;
                        } else if (typeof config.key === 'string') {
                            // Return rows that match a certain value in a column
                            const [column, value] = config.key.split('=');
                            if (column && value) {
                                resolve(results.filter(row => String(row[column.trim()]) === value.trim()));
                                return;
                            }
                        }
                    }
                    
                    resolve(results);
                })
                .on('error', (error: Error) => reject(error));
        });
    }

    /**
     * Load data from JSON file
     */
    private static loadJsonData(config: CSDataProviderConfig): any[] {
        if (!config.filePath) {
            throw new Error('File path is required for JSON data provider');
        }

        // Read JSON file
        const jsonData = JSON.parse(fs.readFileSync(config.filePath, 'utf8'));
        
        // Convert to array if it's not already
        const data = Array.isArray(jsonData) ? jsonData : [jsonData];
        
        // Filter by key if provided
        if (config.key !== undefined) {
            if (typeof config.key === 'number') {
                // Return specific item by index
                if (config.key >= 0 && config.key < data.length) {
                    return [data[config.key]];
                }
                throw new Error(`Index ${config.key} out of bounds`);
            } else if (typeof config.key === 'string') {
                // Return items that match a certain value in a property
                const [property, value] = config.key.split('=');
                if (property && value) {
                    return data.filter(item => String(item[property.trim()]) === value.trim());
                }
            }
        }
        
        return data;
    }

    /**
     * Load data from database
     */
    private static async loadDatabaseData(config: CSDataProviderConfig): Promise<any[]> {
        if (!config.database) {
            throw new Error('Database configuration is required for database data provider');
        }
        
        if (!config.database.query) {
            throw new Error('SQL query is required for database data provider');
        }
        
        // Get database configuration
        const dbConfig = await this.getDatabaseConfig(config.database.name);
        if (!dbConfig) {
            throw new Error(`Database configuration not found for: ${config.database.name}`);
        }
        
        // Execute query using the appropriate database driver
        try {
            switch (dbConfig.type) {
                case 'sqlite':
                    return await this.executeSqliteQuery(dbConfig.path, config.database.query);
                case 'mysql':
                    return await this.executeMysqlQuery(dbConfig, config.database.query);
                case 'postgres':
                    return await this.executePostgresQuery(dbConfig, config.database.query);
                default:
                    throw new Error(`Unsupported database type: ${dbConfig.type}`);
            }
        } catch (error) {
            console.error(`Error executing database query:`, error);
            throw error;
        }
    }
    
    /**
     * Get database configuration from environment or config file
     */
    private static async getDatabaseConfig(databaseName: string): Promise<any> {
        // Priority order:
        // 1. Environment variables
        // 2. Configuration file
        
        // Check environment variables
        const envPrefix = `DB_${databaseName.toUpperCase()}_`;
        const dbTypeEnv = process.env[`${envPrefix}TYPE`];
        
        if (dbTypeEnv) {
            // Found database config in environment variables
            return {
                type: dbTypeEnv.toLowerCase(),
                host: process.env[`${envPrefix}HOST`],
                port: process.env[`${envPrefix}PORT`],
                user: process.env[`${envPrefix}USER`],
                password: process.env[`${envPrefix}PASSWORD`],
                database: process.env[`${envPrefix}DATABASE`],
                path: process.env[`${envPrefix}PATH`], // For SQLite
            };
        }
        
        // Check for config file
        try {
            const configPaths = [
                path.resolve('cstestforge.config.json'),
                path.resolve('config', 'databases.json')
            ];
            
            for (const configPath of configPaths) {
                if (fs.existsSync(configPath)) {
                    const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));
                    if (config.databases && config.databases[databaseName]) {
                        return config.databases[databaseName];
                    }
                }
            }
        } catch (error) {
            console.warn(`Error reading database configuration:`, error);
        }
        
        return null;
    }
    
    /**
     * Execute a query against a SQLite database
     */
    private static async executeSqliteQuery(dbPath: string, query: string): Promise<any[]> {
        return new Promise((resolve, reject) => {
            try {
                const sqlite3 = require('sqlite3').verbose();
                const db = new sqlite3.Database(dbPath);
                
                // Determine if it's a SELECT query
                const isSelect = query.trim().toUpperCase().startsWith('SELECT');
                
                if (isSelect) {
                    db.all(query, [], (err: any, rows: any[]) => {
                        db.close();
                        
                        if (err) {
                            reject(err);
                        } else {
                            resolve(rows);
                        }
                    });
                } else {
                    db.run(query, [], function(this: { changes: number, lastID: number }, err: any) {
                        db.close();
                        
                        if (err) {
                            reject(err);
                        } else {
                            // For non-SELECT queries, return the changes
                            resolve([{ changes: this.changes, lastID: this.lastID }]);
                        }
                    });
                }
            } catch (error) {
                reject(error);
            }
        });
    }
    
    /**
     * Execute a query against a MySQL database
     */
    private static async executeMysqlQuery(dbConfig: any, query: string): Promise<any[]> {
        try {
            const mysql = require('mysql2/promise');
            const connection = await mysql.createConnection({
                host: dbConfig.host,
                port: dbConfig.port,
                user: dbConfig.user,
                password: dbConfig.password,
                database: dbConfig.database
            });
            
            const [rows] = await connection.query(query);
            await connection.end();
            return rows;
        } catch (error) {
            console.error('MySQL query error:', error);
            throw error;
        }
    }
    
    /**
     * Execute a query against a PostgreSQL database
     */
    private static async executePostgresQuery(dbConfig: any, query: string): Promise<any[]> {
        let client;
        try {
            const { Client } = require('pg');
            client = new Client({
                host: dbConfig.host,
                port: dbConfig.port,
                user: dbConfig.user,
                password: dbConfig.password,
                database: dbConfig.database
            });
            
            await client.connect();
            const result = await client.query(query);
            return result.rows;
        } catch (error) {
            console.error('PostgreSQL query error:', error);
            throw error;
        } finally {
            if (client) {
                await client.end();
            }
        }
    }
}

/**
 * Decorator for configuring data sources for test methods.
 * Supports Excel, CSV, JSON, and database data sources.
 * 
 * @example
 * ```typescript
 * // Excel data provider
 * @CSDataProvider({
 *     name: 'loginData',
 *     type: 'excel',
 *     filePath: './data/testdata.xlsx',
 *     sheetName: 'LoginCredentials'
 * })
 * public async testLogin(data: any) {
 *     // Test code using data
 *     await loginPage.login(data.username, data.password);
 * }
 * 
 * // CSV data provider with specific selection
 * @CSDataProvider({
 *     name: 'productData',
 *     type: 'csv',
 *     filePath: './data/products.csv',
 *     key: 'category=electronics'
 * })
 * public async testProductSearch(data: any) {
 *     // Test code using data
 * }
 * 
 * // Database data provider
 * @CSDataProvider({
 *     name: 'userAccounts',
 *     type: 'database',
 *     database: {
 *         name: 'testdb',
 *         query: 'SELECT * FROM users WHERE status = "active"'
 *     }
 * })
 * public async testUserAccounts(data: any) {
 *     // Test code using data
 * }
 * ```
 */
export function CSDataProvider(config: CSDataProviderConfig) {
    return function (target: Object, propertyKey: string, descriptor: PropertyDescriptor) {
        // Register the data provider
        DataProviderRegistry.register(config.name, config);
        
        // Store the original method
        const originalMethod = descriptor.value;
        
        // Replace the method with data provider wrapper
        descriptor.value = async function(...args: any[]) {
            // Load data
            const testData = await DataProviderRegistry.getData(config.name);
            
            // Run the test once for each data item
            const results = [];
            for (const data of testData) {
                // Call the original method with data and original arguments
                results.push(await originalMethod.apply(this, [data, ...args]));
            }
            
            return results;
        };
        
        return descriptor;
    };
}

/**
 * Utility to get test data without using the decorator
 */
export class CSDataUtil {
    /**
     * Get data from a registered data provider
     * 
     * @param name Data provider name
     * @returns Test data array
     */
    public static async getTestData(name: string): Promise<any[]> {
        return DataProviderRegistry.getData(name);
    }
} 