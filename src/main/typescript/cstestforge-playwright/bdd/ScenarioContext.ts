import { Page } from 'playwright';

/**
 * Context class for BDD scenario execution.
 * 
 * This class holds state that can be shared between step definitions
 * within a single scenario.
 */
export class ScenarioContext {
    /** The Playwright page object */
    public readonly page: Page;
    
    /** Storage for scenario data */
    private scenarioData: Map<string, any> = new Map();
    
    /**
     * Create a new scenario context
     * 
     * @param page Playwright page object
     */
    constructor(page: Page) {
        this.page = page;
    }
    
    /**
     * Store data in the scenario context
     * 
     * @param key Data key
     * @param value Data value
     */
    public set<T>(key: string, value: T): void {
        this.scenarioData.set(key, value);
    }
    
    /**
     * Retrieve data from the scenario context
     * 
     * @param key Data key
     * @returns The stored value or undefined if not found
     */
    public get<T>(key: string): T | undefined {
        return this.scenarioData.get(key) as T;
    }
    
    /**
     * Check if a key exists in the scenario context
     * 
     * @param key Data key
     * @returns True if the key exists, false otherwise
     */
    public has(key: string): boolean {
        return this.scenarioData.has(key);
    }
    
    /**
     * Remove data from the scenario context
     * 
     * @param key Data key
     * @returns True if the key was removed, false otherwise
     */
    public delete(key: string): boolean {
        return this.scenarioData.delete(key);
    }
    
    /**
     * Clear all data from the scenario context
     */
    public clear(): void {
        this.scenarioData.clear();
    }
} 