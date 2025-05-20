package com.cstestforge.framework.selenium.element;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cstestforge.framework.selenium.core.CSDriverManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Self-healing element locator that adapts to UI changes.
 * Implements robust element location with history tracking and proximity-based location.
 */
public class CSSelfHealing {
    private static final Logger logger = LoggerFactory.getLogger(CSSelfHealing.class);
    private static final int DEFAULT_TIMEOUT_SECONDS = 10;
    private static final double MINIMUM_SIMILARITY_THRESHOLD = 0.7;
    private static final int MAX_ALTERNATE_LOCATORS = 5;
    private static final int MAX_HISTORY_SIZE = 100;
    
    // Thread-safe cache of element history
    private static final Map<String, ElementHistory> elementHistoryCache = new ConcurrentHashMap<>();
    
    private final int timeoutSeconds;
    
    /**
     * Constructor with default timeout.
     */
    public CSSelfHealing() {
        this(DEFAULT_TIMEOUT_SECONDS);
    }
    
    /**
     * Constructor with custom timeout.
     * 
     * @param timeoutSeconds Timeout in seconds
     */
    public CSSelfHealing(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
    
    /**
     * Gets the WebDriver instance from CSDriverManager
     * 
     * @return WebDriver instance
     */
    private WebDriver getDriver() {
        return CSDriverManager.getDriver();
    }
    
    /**
     * Tracks the history of an element's successful location.
     */
    public static class ElementHistory {
        private final String elementId;
        private final List<LocatorEntry> locatorHistory;
        private int successCount;
        
        /**
         * Creates a new element history.
         * 
         * @param elementId Unique element ID
         */
        public ElementHistory(String elementId) {
            this.elementId = elementId;
            this.locatorHistory = Collections.synchronizedList(new ArrayList<>());
            this.successCount = 0;
        }
        
        /**
         * Adds a successful locator to history.
         * 
         * @param locator Locator that successfully found the element
         * @param attributes Element attributes at time of location
         */
        public void addSuccessfulLocator(By locator, Map<String, String> attributes) {
            // Check if this locator already exists in history
            Optional<LocatorEntry> existingEntry = locatorHistory.stream()
                    .filter(entry -> entry.locator.toString().equals(locator.toString()))
                    .findFirst();
            
            if (existingEntry.isPresent()) {
                // Update existing entry's success count and attributes
                LocatorEntry entry = existingEntry.get();
                entry.incrementSuccessCount();
                entry.updateAttributes(attributes);
            } else {
                // Add new entry
                locatorHistory.add(new LocatorEntry(locator, attributes));
                
                // Trim history if needed
                if (locatorHistory.size() > MAX_HISTORY_SIZE) {
                    // Remove locators with lowest success count
                    locatorHistory.sort(Comparator.comparingInt(LocatorEntry::getSuccessCount));
                    locatorHistory.remove(0);
                }
            }
            
            this.successCount++;
        }
        
        /**
         * Gets alternate locators sorted by success rate.
         * 
         * @param primaryLocator Primary locator to exclude
         * @return Sorted list of alternate locators
         */
        public List<By> getAlternateLocators(By primaryLocator) {
            String primaryLocatorStr = primaryLocator.toString();
            return locatorHistory.stream()
                    .filter(entry -> !entry.locator.toString().equals(primaryLocatorStr))
                    .sorted(Comparator.comparingInt(LocatorEntry::getSuccessCount).reversed())
                    .limit(MAX_ALTERNATE_LOCATORS)
                    .map(entry -> entry.locator)
                    .collect(Collectors.toList());
        }
        
        /**
         * Gets all attributes from the most successful locator.
         * 
         * @return Map of element attributes
         */
        public Map<String, String> getMostSuccessfulAttributes() {
            return locatorHistory.stream()
                    .max(Comparator.comparingInt(LocatorEntry::getSuccessCount))
                    .map(entry -> entry.attributes)
                    .orElse(new HashMap<>());
        }
        
        /**
         * Gets the element ID.
         * 
         * @return Element ID
         */
        public String getElementId() {
            return elementId;
        }
        
        /**
         * Gets the total success count for this element.
         * 
         * @return Success count
         */
        public int getSuccessCount() {
            return successCount;
        }
    }
    
    /**
     * Entry in the locator history.
     */
    private static class LocatorEntry {
        private final By locator;
        private Map<String, String> attributes;
        private int successCount;
        
        /**
         * Creates a new locator entry.
         * 
         * @param locator Locator
         * @param attributes Element attributes
         */
        public LocatorEntry(By locator, Map<String, String> attributes) {
            this.locator = locator;
            this.attributes = new HashMap<>(attributes);
            this.successCount = 1;
        }
        
        /**
         * Increments the success count for this locator.
         */
        public void incrementSuccessCount() {
            this.successCount++;
        }
        
        /**
         * Updates attributes for this locator.
         * 
         * @param newAttributes New attributes
         */
        public void updateAttributes(Map<String, String> newAttributes) {
            this.attributes = new HashMap<>(newAttributes);
        }
        
        /**
         * Gets the success count for this locator.
         * 
         * @return Success count
         */
        public int getSuccessCount() {
            return successCount;
        }
    }
    
    /**
     * Find an element with self-healing capability.
     * 
     * @param locator Primary locator to use
     * @param elementId Unique element ID
     * @return WebElement or null if not found
     */
    public WebElement findElement(By locator, String elementId) {
        WebElement element = findElementWithHealing(locator, elementId);
        if (element != null) {
            // Extract attributes and add to history
            Map<String, String> attributes = extractElementAttributes(element);
            addToHistory(elementId, locator, attributes);
        }
        return element;
    }
    
    /**
     * Find an element with self-healing capability.
     * 
     * @param locator Primary locator to use
     * @return WebElement or null if not found
     */
    public WebElement findElement(By locator) {
        // Generate ID from locator if not provided
        String elementId = "auto_" + locator.toString().hashCode();
        return findElement(locator, elementId);
    }
    
    /**
     * Find an element with healing strategies.
     * 
     * @param primaryLocator Primary locator
     * @param elementId Element ID
     * @return WebElement or null if not found
     */
    private WebElement findElementWithHealing(By primaryLocator, String elementId) {
        // First try with the primary locator
        try {
            WebElement element = waitForElement(primaryLocator);
            if (element != null) {
                return element;
            }
        } catch (Exception e) {
            logger.debug("Primary locator failed: {} - {}", primaryLocator, e.getMessage());
        }
        
        // If primary failed, try alternate locators if available
        ElementHistory history = elementHistoryCache.get(elementId);
        if (history != null) {
            List<By> alternateLocators = history.getAlternateLocators(primaryLocator);
            for (By alternateLocator : alternateLocators) {
                try {
                    WebElement element = waitForElement(alternateLocator);
                    if (element != null) {
                        logger.info("Healed element {} using alternate locator: {}", elementId, alternateLocator);
                        return element;
                    }
                } catch (Exception e) {
                    logger.debug("Alternate locator failed: {} - {}", alternateLocator, e.getMessage());
                }
            }
            
            // If alternate locators failed, try proximity-based healing
            return findWithProximityHealing(primaryLocator, history);
        }
        
        return null;
    }
    
    /**
     * Find an element using proximity-based healing.
     * 
     * @param locator Original locator
     * @param history Element history
     * @return WebElement or null if not found
     */
    private WebElement findWithProximityHealing(By locator, ElementHistory history) {
        Map<String, String> targetAttributes = history.getMostSuccessfulAttributes();
        if (targetAttributes.isEmpty()) {
            return null;
        }
        
        try {
            // Use JavaScript to find elements with similar attributes
            String tagName = targetAttributes.getOrDefault("tagName", "*").toLowerCase();
            
            // Build JavaScript to find all elements of this tag type
            String jsQuery = "return Array.from(document.getElementsByTagName('" + tagName + "'))";
            
            JavascriptExecutor js = (JavascriptExecutor) getDriver();
            List<WebElement> candidates = (List<WebElement>) js.executeScript(jsQuery);
            
            if (candidates.isEmpty()) {
                return null;
            }
            
            // Calculate similarity scores
            List<ScoredElement> scoredElements = candidates.stream()
                    .map(element -> {
                        Map<String, String> attributes = extractElementAttributes(element);
                        double score = calculateSimilarity(targetAttributes, attributes);
                        return new ScoredElement(element, score);
                    })
                    .filter(scored -> scored.score >= MINIMUM_SIMILARITY_THRESHOLD)
                    .sorted(Comparator.comparingDouble(se -> -se.score)) // highest score first
                    .collect(Collectors.toList());
            
            if (!scoredElements.isEmpty()) {
                WebElement bestMatch = scoredElements.get(0).element;
                // Generate a new XPath locator for this element for future use
                String newXPath = generateXPath(bestMatch);
                By newLocator = By.xpath(newXPath);
                
                // Add this new locator to history
                Map<String, String> newAttributes = extractElementAttributes(bestMatch);
                addToHistory(history.getElementId(), newLocator, newAttributes);
                
                logger.info("Healed element {} using proximity matching, new locator: {}", 
                        history.getElementId(), newLocator);
                
                return bestMatch;
            }
        } catch (Exception e) {
            logger.debug("Proximity healing failed: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Class for scoring elements by similarity.
     */
    private static class ScoredElement {
        WebElement element;
        double score;
        
        public ScoredElement(WebElement element, double score) {
            this.element = element;
            this.score = score;
        }
    }
    
    /**
     * Calculate similarity between two sets of attributes.
     * 
     * @param target Target attributes
     * @param candidate Candidate attributes
     * @return Similarity score between 0 and 1
     */
    private double calculateSimilarity(Map<String, String> target, Map<String, String> candidate) {
        if (target.isEmpty() || candidate.isEmpty()) {
            return 0;
        }
        
        // Priority attributes have higher weight
        final List<String> priorityAttributes = Arrays.asList("id", "name", "class", "title", "text");
        
        double totalScore = 0;
        double maxPossibleScore = 0;
        
        // Compare all target attributes
        for (Map.Entry<String, String> entry : target.entrySet()) {
            String key = entry.getKey();
            String targetValue = entry.getValue();
            String candidateValue = candidate.get(key);
            
            // Skip null values
            if (targetValue == null) {
                continue;
            }
            
            // Determine attribute weight
            double weight = priorityAttributes.contains(key) ? 3.0 : 1.0;
            maxPossibleScore += weight;
            
            // Calculate attribute similarity
            if (candidateValue != null) {
                double attributeScore;
                if (key.equals("class")) {
                    // Special handling for CSS classes
                    attributeScore = calculateClassSimilarity(targetValue, candidateValue);
                } else {
                    // Other attributes
                    attributeScore = calculateStringSimilarity(targetValue, candidateValue);
                }
                
                totalScore += attributeScore * weight;
            }
        }
        
        return maxPossibleScore > 0 ? totalScore / maxPossibleScore : 0;
    }
    
    /**
     * Calculate similarity for CSS classes.
     * 
     * @param target Target class string
     * @param candidate Candidate class string
     * @return Similarity score between 0 and 1
     */
    private double calculateClassSimilarity(String target, String candidate) {
        // Split class strings into sets
        List<String> targetClasses = Arrays.stream(target.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        List<String> candidateClasses = Arrays.stream(candidate.split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        if (targetClasses.isEmpty() || candidateClasses.isEmpty()) {
            return 0;
        }
        
        // Count matching classes
        long matchingClasses = targetClasses.stream()
                .filter(candidateClasses::contains)
                .count();
        
        // Calculate Jaccard similarity (intersection over union)
        int unionSize = (int) (targetClasses.size() + candidateClasses.size() - matchingClasses);
        return unionSize > 0 ? (double) matchingClasses / unionSize : 0;
    }
    
    /**
     * Calculate similarity between two strings.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity score between 0 and 1
     */
    private double calculateStringSimilarity(String s1, String s2) {
        if (s1.equals(s2)) {
            return 1.0;
        }
        
        // Case insensitive equality
        if (s1.equalsIgnoreCase(s2)) {
            return 0.9;
        }
        
        // One contains the other
        if (s1.contains(s2) || s2.contains(s1)) {
            return 0.7;
        }
        
        // Calculate Levenshtein distance for approximate matching
        int distance = levenshteinDistance(s1, s2);
        int maxLength = Math.max(s1.length(), s2.length());
        
        return maxLength > 0 ? 1.0 - ((double) distance / maxLength) : 0;
    }
    
    /**
     * Calculate Levenshtein distance between two strings.
     * 
     * @param s1 First string
     * @param s2 Second string
     * @return Edit distance
     */
    private int levenshteinDistance(String s1, String s2) {
        int m = s1.length();
        int n = s2.length();
        
        int[][] dp = new int[m + 1][n + 1];
        
        for (int i = 0; i <= m; i++) {
            dp[i][0] = i;
        }
        
        for (int j = 0; j <= n; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }
        
        return dp[m][n];
    }
    
    /**
     * Wait for an element to be present.
     * 
     * @param locator Element locator
     * @return WebElement or null if not found
     */
    private WebElement waitForElement(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds));
            return wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Add a locator to the element history.
     * 
     * @param elementId Element ID
     * @param locator Successful locator
     * @param attributes Element attributes
     */
    public void addToHistory(String elementId, By locator, Map<String, String> attributes) {
        ElementHistory history = elementHistoryCache.computeIfAbsent(elementId, id -> new ElementHistory(id));
        history.addSuccessfulLocator(locator, attributes);
    }
    
    /**
     * Extract important attributes from an element.
     * 
     * @param element WebElement
     * @return Map of attributes
     */
    public Map<String, String> extractElementAttributes(WebElement element) {
        Map<String, String> attributes = new HashMap<>();
        
        try {
            // Extract basic attributes
            attributes.put("tagName", element.getTagName());
            
            // Common identifying attributes
            addAttributeIfPresent(element, attributes, "id");
            addAttributeIfPresent(element, attributes, "name");
            addAttributeIfPresent(element, attributes, "class");
            addAttributeIfPresent(element, attributes, "type");
            addAttributeIfPresent(element, attributes, "href");
            addAttributeIfPresent(element, attributes, "src");
            addAttributeIfPresent(element, attributes, "title");
            addAttributeIfPresent(element, attributes, "alt");
            addAttributeIfPresent(element, attributes, "placeholder");
            addAttributeIfPresent(element, attributes, "role");
            addAttributeIfPresent(element, attributes, "data-testid");
            addAttributeIfPresent(element, attributes, "data-id");
            
            // Get text content
            String text = element.getText();
            if (text != null && !text.isEmpty()) {
                attributes.put("text", text);
            }
            
            // Get position information
            try {
                attributes.put("x", String.valueOf(element.getLocation().getX()));
                attributes.put("y", String.valueOf(element.getLocation().getY()));
                attributes.put("width", String.valueOf(element.getSize().getWidth()));
                attributes.put("height", String.valueOf(element.getSize().getHeight()));
            } catch (Exception e) {
                // Ignore position errors
            }
        } catch (Exception e) {
            logger.debug("Error extracting element attributes: {}", e.getMessage());
        }
        
        return attributes;
    }
    
    /**
     * Add an attribute to the map if it exists.
     * 
     * @param element WebElement
     * @param attributes Map to add to
     * @param attributeName Attribute name
     */
    private void addAttributeIfPresent(WebElement element, Map<String, String> attributes, String attributeName) {
        try {
            String value = element.getAttribute(attributeName);
            if (value != null && !value.isEmpty()) {
                attributes.put(attributeName, value);
            }
        } catch (Exception e) {
            // Ignore attribute errors
        }
    }
    
    /**
     * Generate an XPath for an element.
     * 
     * @param element WebElement
     * @return XPath string
     */
    private String generateXPath(WebElement element) {
        // Try to execute JavaScript to generate a unique XPath
        try {
            return (String) ((JavascriptExecutor) getDriver()).executeScript(
                "function getPathTo(element) {" +
                "    if (element.id !== '') return '//*[@id=\"' + element.id + '\"]';" +
                "    if (element === document.body) return '/html/body';" +
                "    var index = 1;" +
                "    var siblings = element.parentNode.childNodes;" +
                "    for (var i = 0; i < siblings.length; i++) {" +
                "        var sibling = siblings[i];" +
                "        if (sibling === element) {" +
                "            var tagName = element.tagName.toLowerCase();" +
                "            if (element.className) {" +
                "                return getPathTo(element.parentNode) + '/' + tagName + '[@class=\"' + element.className + '\"]';" +
                "            }" +
                "            if (element.name) {" +
                "                return getPathTo(element.parentNode) + '/' + tagName + '[@name=\"' + element.name + '\"]';" +
                "            }" +
                "            return getPathTo(element.parentNode) + '/' + tagName;" +
                "        }" +
                "        if (sibling.nodeType === 1 && sibling.tagName === element.tagName) {" +
                "            index++;" +
                "        }" +
                "    }" +
                "    return getPathTo(element.parentNode) + '/' + element.tagName.toLowerCase() + '[' + index + ']';" +
                "}" +
                "return getPathTo(arguments[0]);",
                element
            );
        } catch (Exception e) {
            // Fallback - create a simple XPath based on what we know
            Map<String, String> attributes = extractElementAttributes(element);
            String tagName = attributes.getOrDefault("tagName", "*").toLowerCase();
            
            StringBuilder xpath = new StringBuilder("//" + tagName);
            
            // Add any unique identifiers we have
            if (attributes.containsKey("id")) {
                return "//" + tagName + "[@id='" + attributes.get("id") + "']";
            }
            
            boolean addedAttribute = false;
            for (String attr : Arrays.asList("name", "data-testid", "data-id", "title", "href", "src")) {
                if (attributes.containsKey(attr)) {
                    xpath.append("[@").append(attr).append("='").append(attributes.get(attr)).append("']");
                    addedAttribute = true;
                    break;
                }
            }
            
            if (!addedAttribute && attributes.containsKey("class")) {
                xpath.append("[contains(@class,'").append(attributes.get("class").split("\\s+")[0]).append("')]");
            }
            
            return xpath.toString();
        }
    }
    
    /**
     * Get the element history for analysis or reporting.
     * 
     * @param elementId Element ID
     * @return Element history or null if not found
     */
    public ElementHistory getElementHistory(String elementId) {
        return elementHistoryCache.get(elementId);
    }
    
    /**
     * Clear the element history cache.
     */
    public static void clearCache() {
        elementHistoryCache.clear();
    }
} 