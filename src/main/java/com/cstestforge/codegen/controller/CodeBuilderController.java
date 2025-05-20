package com.cstestforge.codegen.controller;

import com.cstestforge.codegen.model.*;
import com.cstestforge.codegen.service.CodeGenerationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for code generation operations
 */
@RestController
@RequestMapping("/api/codebuilder")
public class CodeBuilderController {
    
    private final CodeGenerationService codeGenerationService;
    
    @Autowired
    public CodeBuilderController(CodeGenerationService codeGenerationService) {
        this.codeGenerationService = codeGenerationService;
    }
    
    /**
     * Get all available code templates
     */
    @GetMapping("/templates")
    public ResponseEntity<List<CodeTemplate>> getAllTemplates(
            @RequestParam(required = false) String framework,
            @RequestParam(required = false) String language) {
        return ResponseEntity.ok(codeGenerationService.getTemplates(framework, language));
    }
    
    /**
     * Get a specific template by ID
     */
    @GetMapping("/templates/{id}")
    public ResponseEntity<CodeTemplate> getTemplateById(@PathVariable String id) {
        CodeTemplate template = codeGenerationService.getTemplateById(id);
        if (template != null) {
            return ResponseEntity.ok(template);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Generate code from a test definition
     */
    @PostMapping("/generate")
    public ResponseEntity<GeneratedCode> generateCode(@RequestBody CodeGenerationRequest request) {
        GeneratedCode generatedCode;
        
        if (request.getSourceId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        // Determine if this is from a test or recording based on the source ID pattern
        if (request.getSourceId().startsWith("test_")) {
            generatedCode = codeGenerationService.generateFromTest(request.getSourceId(), 
                request.getFramework(), request.getLanguage());
        } else if (request.getSourceId().startsWith("rec_")) {
            generatedCode = codeGenerationService.generateFromRecording(request.getSourceId(), 
                request.getFramework(), request.getLanguage());
        } else {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(generatedCode);
    }
    
    /**
     * Generate code from a recording session
     */
    @PostMapping("/generate/recording/{sessionId}")
    public ResponseEntity<GeneratedCode> generateCodeFromRecording(
            @PathVariable String sessionId,
            @RequestParam String framework,
            @RequestParam String language,
            @RequestParam(required = false) String templateId) {
        
        GeneratedCode generatedCode = codeGenerationService.generateFromRecording(
                sessionId, framework, language);
        
        return ResponseEntity.ok(generatedCode);
    }
    
    /**
     * Generate code from a test definition
     */
    @PostMapping("/generate/test/{testId}")
    public ResponseEntity<GeneratedCode> generateCodeFromTest(
            @PathVariable String testId,
            @RequestParam String framework,
            @RequestParam String language,
            @RequestParam(required = false) String templateId) {
        
        GeneratedCode generatedCode = codeGenerationService.generateFromTest(
                testId, framework, language);
        
        return ResponseEntity.ok(generatedCode);
    }
    
    /**
     * Get supported frameworks
     */
    @GetMapping("/frameworks")
    public ResponseEntity<List<FrameworkDefinition>> getSupportedFrameworks() {
        return ResponseEntity.ok(codeGenerationService.getSupportedFrameworks());
    }
    
    /**
     * Validate generated code
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateCode(
            @RequestBody Map<String, String> request) {
        
        String code = request.get("code");
        String framework = request.get("framework");
        String language = request.get("language");
        
        if (code == null || framework == null || language == null) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean isValid = codeGenerationService.validateCode(code, framework, language);
        Map<String, Object> response = Map.of(
            "valid", isValid,
            "message", isValid ? "Code successfully validated" : "Code validation failed"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Save code generation customization
     */
    @PostMapping("/customization")
    public ResponseEntity<CodeGenerationConfig> saveCustomization(
            @RequestBody CodeGenerationConfig config) {
        
        if (config.getProjectId() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        CodeGenerationConfig savedConfig = codeGenerationService.saveCustomization(
                config.getProjectId(), config);
        
        return ResponseEntity.ok(savedConfig);
    }
    
    /**
     * Export framework with generated tests
     */
    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> exportFramework(
            @RequestBody ExportRequest request) {
        
        if (request.getProjectId() == null || request.getFramework() == null || 
            request.getDestinationPath() == null) {
            return ResponseEntity.badRequest().build();
        }
        
        String exportPath = codeGenerationService.exportFramework(request, request.getDestinationPath());
        Map<String, Object> response = Map.of(
            "success", true,
            "exportPath", exportPath
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Regenerate code with customization
     */
    @PostMapping("/regenerate/{codeId}")
    public ResponseEntity<GeneratedCode> regenerateWithCustomization(
            @PathVariable String codeId,
            @RequestBody CodeGenerationConfig config) {
        
        GeneratedCode regeneratedCode = codeGenerationService.regenerateWithCustomization(codeId, config);
        
        if (regeneratedCode != null) {
            return ResponseEntity.ok(regeneratedCode);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get code generation configuration for a project
     */
    @GetMapping("/customization/{projectId}")
    public ResponseEntity<CodeGenerationConfig> getCustomization(@PathVariable String projectId) {
        CodeGenerationConfig config = codeGenerationService.getCustomization(projectId);
        
        if (config != null) {
            return ResponseEntity.ok(config);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
} 