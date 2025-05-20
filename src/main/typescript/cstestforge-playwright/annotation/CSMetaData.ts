/**
 * Metadata options for a test method
 */
export interface CSMetaDataOptions {
    /** Test case ID (e.g., from test management system) */
    id?: string;
    /** Test case title */
    title?: string;
    /** Test description */
    description?: string;
    /** Test priority (P0, P1, P2, etc.) */
    priority?: string;
    /** Test severity (Critical, High, Medium, Low) */
    severity?: string;
    /** Test labels/tags */
    labels?: string[];
    /** Test category/group */
    category?: string;
    /** Test owner/author */
    owner?: string;
    /** Linked requirements */
    requirements?: string[];
    /** Test automation status */
    automationStatus?: 'Automated' | 'Manual' | 'Partial' | 'Not Applicable';
    /** Custom metadata fields */
    [key: string]: any;
}

/**
 * Stores metadata for all decorated methods
 */
export class MetadataRegistry {
    private static registry: Map<string, CSMetaDataOptions> = new Map();

    /**
     * Register metadata for a test method
     */
    public static register(target: Object, propertyKey: string, metadata: CSMetaDataOptions): void {
        const key = this.getKey(target, propertyKey);
        this.registry.set(key, metadata);
    }

    /**
     * Get metadata for a test method
     */
    public static getMetadata(target: Object, propertyKey: string): CSMetaDataOptions | undefined {
        const key = this.getKey(target, propertyKey);
        return this.registry.get(key);
    }

    /**
     * Get a unique key for the class method
     */
    private static getKey(target: Object, propertyKey: string): string {
        return `${target.constructor.name}.${propertyKey}`;
    }
}

/**
 * Decorator for providing metadata about a test method.
 * Used for integration with reporting and test management systems like ADO.
 * 
 * @example
 * ```typescript
 * @CSMetaData({
 *     id: 'TC-123',
 *     title: 'Verify login functionality',
 *     description: 'Test that users can login with valid credentials',
 *     priority: 'P0',
 *     severity: 'Critical',
 *     owner: 'John Doe',
 *     labels: ['Regression', 'Smoke'],
 *     requirements: ['REQ-001', 'REQ-002']
 * })
 * public async testLogin() {
 *     // Test code
 * }
 * ```
 */
export function CSMetaData(options: CSMetaDataOptions) {
    return function (target: Object, propertyKey: string, descriptor: PropertyDescriptor) {
        // Store the metadata in the registry
        MetadataRegistry.register(target, propertyKey, options);

        // Return the original method descriptor
        return descriptor;
    };
}

/**
 * Utility functions for accessing metadata during test execution
 */
export class CSMetaDataUtil {
    /**
     * Get metadata for the current test method
     * 
     * @param target Class instance
     * @param methodName Method name
     * @returns Metadata options
     */
    public static getTestMetadata(target: Object, methodName: string): CSMetaDataOptions | undefined {
        return MetadataRegistry.getMetadata(target, methodName);
    }

    /**
     * Convert metadata to a map for reporting
     * 
     * @param metadata Metadata options
     * @returns Map of string key-value pairs
     */
    public static toReportingMap(metadata?: CSMetaDataOptions): Record<string, string> {
        if (!metadata) {
            return {};
        }

        const result: Record<string, string> = {};
        
        // Convert each metadata field to string
        for (const [key, value] of Object.entries(metadata)) {
            if (value !== undefined) {
                if (Array.isArray(value)) {
                    result[key] = value.join(', ');
                } else {
                    result[key] = String(value);
                }
            }
        }
        
        return result;
    }
} 