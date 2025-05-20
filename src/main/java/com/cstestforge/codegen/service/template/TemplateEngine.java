package com.cstestforge.codegen.service.template;

import java.util.Map;

/**
 * Interface for template engines that render code templates with context variables
 */
public interface TemplateEngine {
    /**
     * Render a template with context variables
     * 
     * @param template the template string to render
     * @param context the context variables to use in rendering
     * @return the rendered template as a string
     */
    String render(String template, Map<String, Object> context);
    
    /**
     * Render a template file with context variables
     * 
     * @param templatePath the path to the template file
     * @param context the context variables to use in rendering
     * @return the rendered template as a string
     */
    String renderFile(String templatePath, Map<String, Object> context);
    
    /**
     * Load a template from a file
     * 
     * @param templatePath the path to the template file
     * @return the template content as a string
     */
    String loadTemplate(String templatePath);
    
    /**
     * Save a template to a file
     * 
     * @param templatePath the path to save the template to
     * @param template the template content to save
     */
    void saveTemplate(String templatePath, String template);
    
    /**
     * Check if a template exists
     * 
     * @param templatePath the path to check
     * @return true if the template exists, false otherwise
     */
    boolean templateExists(String templatePath);
} 