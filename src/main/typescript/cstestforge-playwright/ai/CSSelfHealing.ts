import { Locator, Page, ElementHandle } from 'playwright';
import { CSPlaywrightDriver } from '../core/CSPlaywrightDriver';

/**
 * Self-healing element location strategies to handle dynamic elements and DOM changes.
 * This provides AI-driven element location for reliable test automation.
 */
export class CSSelfHealing {
    private page: Page | null = null;
    private driver: CSPlaywrightDriver;
    private static instance: CSSelfHealing;
    
    // Weights for different attributes when calculating similarity
    private readonly WEIGHTS = {
        id: 0.35,
        name: 0.15,
        className: 0.15,
        tagName: 0.10,
        text: 0.10,
        position: 0.05,
        size: 0.05,
        attributes: 0.05
    };
    
    // Confidence threshold for element match
    private readonly CONFIDENCE_THRESHOLD = 0.7;
    
    // Cache to store previously found elements for quick lookup
    private readonly elementCache = new Map<string, {
        originalSelector: string;
        attributes: Record<string, string>;
        position: { x: number; y: number; } | null;
        size: { width: number; height: number; } | null;
    }>();
    
    // Private constructor for singleton pattern
    private constructor() {
        this.driver = new CSPlaywrightDriver();
    }
    
    /**
     * Get singleton instance
     */
    public static getInstance(): CSSelfHealing {
        if (!CSSelfHealing.instance) {
            CSSelfHealing.instance = new CSSelfHealing();
        }
        return CSSelfHealing.instance;
    }
    
    /**
     * Get the current page
     */
    private async getPage(): Promise<Page> {
        if (!this.page) {
            this.page = await this.driver.getPage();
        }
        return this.page;
    }
    
    /**
     * Find element with self-healing capabilities.
     * If the primary selector fails, try alternative strategies.
     * 
     * @param selector Primary selector to locate element
     * @param friendlyName Element description for logging
     * @returns Playwright Locator
     */
    public async findElement(selector: string, friendlyName: string = ''): Promise<Locator> {
        const page = await this.getPage();
        
        try {
            // Try the original selector first
            const locator = page.locator(selector);
            
            // Check if element exists
            const count = await locator.count();
            if (count > 0) {
                // Element found with original selector, store its attributes for future healing
                await this.cacheElementAttributes(selector, locator);
                return locator;
            }
            
            // Original selector failed, try self-healing
            console.log(`Element not found with selector "${selector}", attempting self-healing...`);
            return await this.healElement(selector, friendlyName);
        } catch (error) {
            console.error(`Failed to find element "${friendlyName || selector}":`, error);
            throw new Error(`Self-healing failed for element "${friendlyName || selector}": ${error}`);
        }
    }
    
    /**
     * Store element attributes in cache for future healing
     * 
     * @param selector Original selector
     * @param locator Element locator
     */
    private async cacheElementAttributes(selector: string, locator: Locator): Promise<void> {
        try {
            const page = await this.getPage();
            const elementHandle = await locator.elementHandle();
            
            if (!elementHandle) {
                return;
            }
            
            // Get all attributes
            const attributes = await page.evaluate((el) => {
                const attrs: Record<string, string> = {};
                
                for (let i = 0; i < el.attributes.length; i++) {
                    const attribute = el.attributes[i];
                    attrs[attribute.name] = attribute.value;
                }
                
                attrs['innerText'] = el.innerText || '';
                attrs['tagName'] = el.tagName.toLowerCase();
                
                return attrs;
            }, elementHandle as any);
            
            // Get element position and size
            const boundingBox = await elementHandle.boundingBox();
            
            // Store in cache with selector as key
            this.elementCache.set(selector, {
                originalSelector: selector,
                attributes,
                position: boundingBox ? { x: boundingBox.x, y: boundingBox.y } : null,
                size: boundingBox ? { width: boundingBox.width, height: boundingBox.height } : null
            });
            
            elementHandle.dispose();
        } catch (error) {
            console.debug(`Failed to cache element attributes:`, error);
        }
    }
    
    /**
     * Attempt to heal/find an element that has changed
     * 
     * @param originalSelector Original selector that failed
     * @param friendlyName Element description for logging
     * @returns Playwright Locator for the healed element
     */
    private async healElement(originalSelector: string, friendlyName: string): Promise<Locator> {
        const page = await this.getPage();
        const cachedElement = this.elementCache.get(originalSelector);
        
        // If no cached info, we can't heal
        if (!cachedElement) {
            throw new Error(`No cached information available for element "${friendlyName || originalSelector}"`);
        }
        
        // Try alternative locating strategies
        
        // 1. Try healing by ID if available
        if (cachedElement.attributes.id) {
            const idLocator = page.locator(`#${cachedElement.attributes.id}`);
            const idCount = await idLocator.count();
            if (idCount > 0) {
                console.log(`Element healed using ID: #${cachedElement.attributes.id}`);
                return idLocator;
            }
        }
        
        // 2. Try healing by name if available
        if (cachedElement.attributes.name) {
            const nameLocator = page.locator(`[name="${cachedElement.attributes.name}"]`);
            const nameCount = await nameLocator.count();
            if (nameCount > 0) {
                console.log(`Element healed using name: [name="${cachedElement.attributes.name}"]`);
                return nameLocator;
            }
        }
        
        // 3. Try combining tag and class if available
        if (cachedElement.attributes.tagName && cachedElement.attributes.class) {
            const classSelector = `${cachedElement.attributes.tagName}.${cachedElement.attributes.class.replace(/\s+/g, '.')}`;
            const classLocator = page.locator(classSelector);
            const classCount = await classLocator.count();
            if (classCount > 0) {
                console.log(`Element healed using tag and class: ${classSelector}`);
                return classLocator;
            }
        }
        
        // 4. Try using text content if available
        if (cachedElement.attributes.innerText) {
            const text = cachedElement.attributes.innerText.trim();
            if (text) {
                const textLocator = page.locator(`text=${text}`);
                const textCount = await textLocator.count();
                if (textCount > 0) {
                    console.log(`Element healed using text content: text=${text}`);
                    return textLocator;
                }
            }
        }
        
        // 5. Use AI-based similarity matching
        return await this.findSimilarElement(cachedElement, friendlyName);
    }
    
    /**
     * Find similar element using advanced similarity calculation
     * 
     * @param cachedElement Cached element information
     * @param friendlyName Element description for logging
     * @returns Playwright Locator for the most similar element
     */
    private async findSimilarElement(
        cachedElement: {
            originalSelector: string;
            attributes: Record<string, string>;
            position: { x: number; y: number; } | null;
            size: { width: number; height: number; } | null;
        },
        friendlyName: string
    ): Promise<Locator> {
        const page = await this.getPage();
        const tagName = cachedElement.attributes.tagName || 'div';
        
        // Get all elements of the same tag
        const allElements = page.locator(tagName);
        const count = await allElements.count();
        
        if (count === 0) {
            throw new Error(`No ${tagName} elements found in the page`);
        }
        
        // Calculate similarity for each element
        let bestMatch: { element: Locator; score: number } = { element: allElements.first(), score: 0 };
        
        for (let i = 0; i < count; i++) {
            const element = allElements.nth(i);
            const elementHandle = await element.elementHandle();
            
            if (!elementHandle) continue;
            
            const similarity = await this.calculateSimilarity(elementHandle, cachedElement);
            
            if (similarity > bestMatch.score) {
                bestMatch = { element, score: similarity };
            }
            
            await elementHandle.dispose();
            
            // If we found a very good match, stop searching
            if (bestMatch.score > 0.9) {
                break;
            }
        }
        
        // Check if we have a good enough match
        if (bestMatch.score >= this.CONFIDENCE_THRESHOLD) {
            console.log(`Element "${friendlyName || cachedElement.originalSelector}" healed with similarity score ${bestMatch.score.toFixed(2)}`);
            return bestMatch.element;
        }
        
        // Last resort: try to find something similar to the original selector
        throw new Error(`Could not find similar element for "${friendlyName || cachedElement.originalSelector}" with sufficient confidence`);
    }
    
    /**
     * Calculate similarity between a candidate element and cached element
     * 
     * @param elementHandle Handle to candidate element
     * @param cachedElement Cached element information
     * @returns Similarity score between 0 and 1
     */
    private async calculateSimilarity(
        elementHandle: ElementHandle,
        cachedElement: {
            originalSelector: string;
            attributes: Record<string, string>;
            position: { x: number; y: number; } | null;
            size: { width: number; height: number; } | null;
        }
    ): Promise<number> {
        const page = await this.getPage();
        
        // Get current element attributes
        const currentAttributes = await page.evaluate((el) => {
            const attrs: Record<string, string> = {};
            
            for (let i = 0; i < el.attributes.length; i++) {
                const attribute = el.attributes[i];
                attrs[attribute.name] = attribute.value;
            }
            
            attrs['innerText'] = el.innerText || '';
            attrs['tagName'] = el.tagName.toLowerCase();
            
            return attrs;
        }, elementHandle as any);
        
        // Get current element position and size
        const boundingBox = await elementHandle.boundingBox();
        const currentPosition = boundingBox ? { x: boundingBox.x, y: boundingBox.y } : null;
        const currentSize = boundingBox ? { width: boundingBox.width, height: boundingBox.height } : null;
        
        // Calculate individual similarity scores
        let idScore = 0;
        let nameScore = 0;
        let classScore = 0;
        let tagScore = 0;
        let textScore = 0;
        let attributesScore = 0;
        let positionScore = 0;
        let sizeScore = 0;
        
        // ID similarity (exact match)
        if (cachedElement.attributes.id && currentAttributes.id) {
            idScore = cachedElement.attributes.id === currentAttributes.id ? 1 : 0;
        }
        
        // Name similarity (exact match)
        if (cachedElement.attributes.name && currentAttributes.name) {
            nameScore = cachedElement.attributes.name === currentAttributes.name ? 1 : 0;
        }
        
        // Class similarity (partial match)
        if (cachedElement.attributes.class && currentAttributes.class) {
            const cachedClasses = new Set(cachedElement.attributes.class.split(/\s+/));
            const currentClasses = new Set(currentAttributes.class.split(/\s+/));
            
            const intersection = new Set([...cachedClasses].filter(x => currentClasses.has(x)));
            const union = new Set([...cachedClasses, ...currentClasses]);
            
            classScore = intersection.size / union.size;
        }
        
        // Tag similarity (exact match)
        tagScore = cachedElement.attributes.tagName === currentAttributes.tagName ? 1 : 0;
        
        // Text similarity (fuzzy match)
        if (cachedElement.attributes.innerText && currentAttributes.innerText) {
            const cachedText = cachedElement.attributes.innerText.trim().toLowerCase();
            const currentText = currentAttributes.innerText.trim().toLowerCase();
            
            if (cachedText.length > 0 && currentText.length > 0) {
                // Simple fuzzy match logic
                if (cachedText === currentText) {
                    textScore = 1;
                } else if (cachedText.includes(currentText) || currentText.includes(cachedText)) {
                    textScore = 0.7;
                } else {
                    // Calculate Levenshtein distance-based similarity
                    const distance = this.levenshteinDistance(cachedText, currentText);
                    const maxLength = Math.max(cachedText.length, currentText.length);
                    textScore = maxLength > 0 ? 1 - (distance / maxLength) : 0;
                }
            }
        }
        
        // Other attributes similarity
        const cachedAttrs = Object.keys(cachedElement.attributes).filter(k => 
            !['id', 'name', 'class', 'tagName', 'innerText'].includes(k));
        
        const currentAttrs = Object.keys(currentAttributes).filter(k => 
            !['id', 'name', 'class', 'tagName', 'innerText'].includes(k));
        
        if (cachedAttrs.length > 0) {
            let matches = 0;
            
            for (const attr of cachedAttrs) {
                if (currentAttributes[attr] === cachedElement.attributes[attr]) {
                    matches++;
                }
            }
            
            attributesScore = matches / cachedAttrs.length;
        }
        
        // Position similarity
        if (cachedElement.position && currentPosition) {
            const xDiff = Math.abs(cachedElement.position.x - currentPosition.x);
            const yDiff = Math.abs(cachedElement.position.y - currentPosition.y);
            
            // Consider positions similar if they're within 20px
            positionScore = 1 - Math.min(1, (xDiff + yDiff) / 40);
        }
        
        // Size similarity
        if (cachedElement.size && currentSize) {
            const widthDiff = Math.abs(cachedElement.size.width - currentSize.width);
            const heightDiff = Math.abs(cachedElement.size.height - currentSize.height);
            
            // Consider sizes similar if they're within 20px total difference
            sizeScore = 1 - Math.min(1, (widthDiff + heightDiff) / 40);
        }
        
        // Calculate weighted score
        const weightedScore = 
            (idScore * this.WEIGHTS.id) +
            (nameScore * this.WEIGHTS.name) +
            (classScore * this.WEIGHTS.className) +
            (tagScore * this.WEIGHTS.tagName) +
            (textScore * this.WEIGHTS.text) +
            (positionScore * this.WEIGHTS.position) +
            (sizeScore * this.WEIGHTS.size) +
            (attributesScore * this.WEIGHTS.attributes);
        
        return weightedScore;
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     * Used for calculating text similarity
     * 
     * @param a First string
     * @param b Second string
     * @returns Levenshtein distance
     */
    private levenshteinDistance(a: string, b: string): number {
        if (a.length === 0) return b.length;
        if (b.length === 0) return a.length;
      
        const matrix: number[][] = [];
      
        // Initialize matrix
        for (let i = 0; i <= b.length; i++) {
            matrix[i] = [i];
        }
      
        for (let j = 0; j <= a.length; j++) {
            matrix[0][j] = j;
        }
      
        // Fill matrix
        for (let i = 1; i <= b.length; i++) {
            for (let j = 1; j <= a.length; j++) {
                const cost = a[j - 1] === b[i - 1] ? 0 : 1;
                matrix[i][j] = Math.min(
                    matrix[i - 1][j] + 1,     // deletion
                    matrix[i][j - 1] + 1,     // insertion
                    matrix[i - 1][j - 1] + cost  // substitution
                );
            }
        }
      
        return matrix[b.length][a.length];
    }
    
    /**
     * Create a self-healing locator that will use AI to find elements
     * when standard selectors fail
     * 
     * @param selector Primary selector to use
     * @param friendlyName Optional name for logging
     * @returns A function that returns a Locator
     */
    public static createSelfHealingLocator(selector: string, friendlyName?: string) {
        return async (): Promise<Locator> => {
            return await CSSelfHealing.getInstance().findElement(selector, friendlyName);
        };
    }
} 