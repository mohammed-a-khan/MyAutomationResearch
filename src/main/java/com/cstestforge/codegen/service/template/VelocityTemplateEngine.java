package com.cstestforge.codegen.service.template;

import com.cstestforge.project.storage.FileStorageService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple implementation of the TemplateEngine interface
 */
@Service
public class VelocityTemplateEngine implements TemplateEngine {
    
    private final FileStorageService storageService;
    
    @Autowired
    public VelocityTemplateEngine(FileStorageService storageService) {
        this.storageService = storageService;
    }
    
    @Override
    public String render(String template, Map<String, Object> context) {
        // Simple variable replacement using ${variable} syntax
        String result = template;
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(result);
        
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String variable = matcher.group(1);
            Object value = context.get(variable);
            String replacement = (value != null) ? value.toString() : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }
    
    @Override
    public String renderFile(String templatePath, Map<String, Object> context) {
        String template = loadTemplate(templatePath);
        return render(template, context);
    }
    
    @Override
    public String loadTemplate(String templatePath) {
        try {
            // Read the template file as a string
            byte[] bytes = Files.readAllBytes(Paths.get(
                    storageService.getAbsolutePath(templatePath)));
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + templatePath, e);
        }
    }
    
    @Override
    public void saveTemplate(String templatePath, String template) {
        try {
            // Create parent directories if they don't exist
            String parentDir = templatePath.substring(0, templatePath.lastIndexOf('/'));
            storageService.createDirectoryIfNotExists(parentDir);
            
            // Write the template to the file
            Files.write(
                    Paths.get(storageService.getAbsolutePath(templatePath)), 
                    template.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save template: " + templatePath, e);
        }
    }
    
    @Override
    public boolean templateExists(String templatePath) {
        return storageService.fileExists(templatePath);
    }
} 