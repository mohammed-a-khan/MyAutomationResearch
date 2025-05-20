package com.cstestforge.framework.selenium.core;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.io.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Handles screenshot operations for WebDriver.
 * Provides utilities for taking full page, element, and viewport screenshots.
 */
public class CSScreenshotHandler {
    private static final Logger logger = LoggerFactory.getLogger(CSScreenshotHandler.class);
    
    // Default directory for storing screenshots
    private static final String SCREENSHOT_DIR = "test-output/screenshots";
    
    // Date format for screenshot filenames
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss");
    
    // Private constructor to prevent instantiation
    private CSScreenshotHandler() {
        // Utility class should not be instantiated
    }
    
    /**
     * Take a screenshot of the current browser window
     * 
     * @param driver WebDriver instance
     * @param name Screenshot name
     * @return Path to the saved screenshot or null if failed
     */
    public static String takeScreenshot(WebDriver driver, String name) {
        if (!(driver instanceof TakesScreenshot)) {
            logger.warn("Driver does not support taking screenshots");
            return null;
        }
        
        ensureDirectoryExists();
        
        String timestamp = DATE_FORMAT.format(new Date());
        String sanitizedName = sanitizeFilename(name);
        String filename = String.format("%s_%s.png", timestamp, sanitizedName);
        String filePath = Paths.get(SCREENSHOT_DIR, filename).toString();
        
        try {
            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileHandler.copy(screenshotFile, new File(filePath));
            logger.debug("Screenshot saved to: {}", filePath);
            return filePath;
        } catch (IOException e) {
            logger.error("Failed to save screenshot: {}", name, e);
            return null;
        }
    }
    
    /**
     * Take a screenshot of a specific element
     * 
     * @param driver WebDriver instance
     * @param element WebElement to capture
     * @param name Screenshot name
     * @return Path to the saved screenshot or null if failed
     */
    public static String takeElementScreenshot(WebDriver driver, WebElement element, String name) {
        if (!(driver instanceof TakesScreenshot)) {
            logger.warn("Driver does not support taking screenshots");
            return null;
        }
        
        ensureDirectoryExists();
        
        String timestamp = DATE_FORMAT.format(new Date());
        String sanitizedName = sanitizeFilename(name);
        String filename = String.format("%s_%s_element.png", timestamp, sanitizedName);
        String filePath = Paths.get(SCREENSHOT_DIR, filename).toString();
        
        try {
            // Scroll element into view
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({behavior: 'smooth', block: 'center'});", element);
            
            // Wait for scrolling to complete
            Thread.sleep(500);
            
            // Take full screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            
            // Get element dimensions
            org.openqa.selenium.Rectangle elementRect = element.getRect();
            
            // Load the full screenshot
            BufferedImage fullImg = ImageIO.read(screenshot);
            
            // Get the element's location relative to the current viewport
            int scrollY = ((Number) ((JavascriptExecutor) driver).executeScript("return window.pageYOffset")).intValue();
            int scrollX = ((Number) ((JavascriptExecutor) driver).executeScript("return window.pageXOffset")).intValue();
            
            // Adjust coordinates relative to viewport
            int x = elementRect.getX() - scrollX;
            int y = elementRect.getY() - scrollY;
            
            // Crop the image to just the element
            BufferedImage elementImg = fullImg.getSubimage(
                Math.max(0, x),
                Math.max(0, y),
                Math.min(elementRect.getWidth(), fullImg.getWidth() - x),
                Math.min(elementRect.getHeight(), fullImg.getHeight() - y)
            );
            
            // Save the element image
            ImageIO.write(elementImg, "png", new File(filePath));
            
            logger.debug("Element screenshot saved to: {}", filePath);
            return filePath;
        } catch (Exception e) {
            logger.error("Failed to save element screenshot: {}", name, e);
            return null;
        }
    }
    
    /**
     * Take a full-page screenshot (beyond visible viewport)
     * Uses JavaScript to stitch together a full-page screenshot
     * 
     * @param driver WebDriver instance
     * @param name Screenshot name
     * @return Path to the saved screenshot or null if failed
     */
    public static String takeFullPageScreenshot(WebDriver driver, String name) {
        if (!(driver instanceof JavascriptExecutor)) {
            logger.warn("Driver does not support JavaScript execution for full page screenshot");
            return null;
        }
        
        ensureDirectoryExists();
        
        String timestamp = DATE_FORMAT.format(new Date());
        String sanitizedName = sanitizeFilename(name);
        String filename = String.format("%s_%s_fullpage.png", timestamp, sanitizedName);
        String filePath = Paths.get(SCREENSHOT_DIR, filename).toString();
        
        try {
            // Get the total height and width of the page
            long totalWidth = (long) ((JavascriptExecutor) driver).executeScript("return document.body.offsetWidth");
            long totalHeight = (long) ((JavascriptExecutor) driver).executeScript(
                "return Math.max(document.body.scrollHeight, document.body.offsetHeight, document.documentElement.clientHeight, document.documentElement.scrollHeight, document.documentElement.offsetHeight);");
            
            // Get the size of the viewport
            long viewportWidth = (long) ((JavascriptExecutor) driver).executeScript("return document.documentElement.clientWidth");
            long viewportHeight = (long) ((JavascriptExecutor) driver).executeScript("return document.documentElement.clientHeight");
            
            // Create a new BufferedImage with the full dimensions
            BufferedImage fullImage = new BufferedImage((int) totalWidth, (int) totalHeight, BufferedImage.TYPE_INT_RGB);
            
            // Calculate the number of snapshots needed (vertically and horizontally)
            int verticalSnapshots = (int) Math.ceil((double) totalHeight / viewportHeight);
            int horizontalSnapshots = (int) Math.ceil((double) totalWidth / viewportWidth);
            
            // Keep track of the current scroll position
            int currentYPosition = 0;
            
            // For each vertical section
            for (int y = 0; y < verticalSnapshots; y++) {
                int currentXPosition = 0;
                
                // For each horizontal section
                for (int x = 0; x < horizontalSnapshots; x++) {
                    // Scroll to the correct position
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(" + currentXPosition + ", " + currentYPosition + ")");
                    
                    // Wait for scrolling and content to stabilize
                    Thread.sleep(100);
                    
                    // Take a screenshot
                    File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                    BufferedImage viewportImage = ImageIO.read(screenshot);
                    
                    // Calculate the clip region
                    int clipWidth = Math.min(viewportImage.getWidth(), (int) totalWidth - currentXPosition);
                    int clipHeight = Math.min(viewportImage.getHeight(), (int) totalHeight - currentYPosition);
                    
                    // Copy the screenshot to the full image
                    BufferedImage clipImage = viewportImage.getSubimage(0, 0, clipWidth, clipHeight);
                    fullImage.createGraphics().drawImage(clipImage, currentXPosition, currentYPosition, null);
                    
                    // Move to the next horizontal section
                    currentXPosition += viewportWidth;
                }
                
                // Move to the next vertical section
                currentYPosition += viewportHeight;
            }
            
            // Save the full image
            ImageIO.write(fullImage, "png", new File(filePath));
            
            // Return to the top of the page
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0)");
            
            logger.debug("Full page screenshot saved to: {}", filePath);
            return filePath;
        } catch (Exception e) {
            logger.error("Failed to save full page screenshot: {}", name, e);
            return null;
        }
    }
    
    /**
     * Ensure the screenshot directory exists
     */
    private static void ensureDirectoryExists() {
        Path path = Paths.get(SCREENSHOT_DIR);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                logger.debug("Created screenshot directory: {}", SCREENSHOT_DIR);
            } catch (IOException e) {
                logger.error("Failed to create screenshot directory: {}", SCREENSHOT_DIR, e);
            }
        }
    }
    
    /**
     * Sanitize the filename to remove invalid characters
     * 
     * @param filename Filename to sanitize
     * @return Sanitized filename
     */
    private static String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "screenshot_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // Replace invalid file characters with underscores
        String sanitized = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
        
        // Limit the length of the filename
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized;
    }
} 